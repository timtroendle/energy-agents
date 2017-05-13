package uk.ac.cam.eeci.energyagents;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Dwelling {

    // Nomenclature of internal variables and parameters derived from the ISO 13790 standard
    // and not self explanatory. See the standard for further details.

    private static final double HEAT_TRANSMISSION_COEFFICIENT_AIR_TO_SURFACE = 3.45; // ISO 13790 [W/(m^2*K)]
    private static final double HEAT_TRANSMISSION_COEFFICIENT_MASS_TO_SURFACE = 9.1; // ISO 13790 [W/(m^2*K)]
    private static final double SURFACE_TO_FLOOR_RATIO = 4.5; // ISO 13790 [-]
    private static final double HEAT_CAPACITY_AIR = 1200; // ISO 13790 [J/(m3·K)]
    private static final double TIME_FRACTION_NATURAL_VENTILATION = 1; // constant natural ventilation
    private static final double SOLAR_HEAT_GAIN = 0.0; // solar gain currently not considered

    private final HeatingControlStrategyReference heatingControlStrategy;
    private final EnvironmentReference environmentReference;
    private final Set<PersonReference> peopleInDwelling;
    private final Duration timeStepSize;

    private final double A_f;
    private final double A_t;
    private final double C_m;
    private final double A_m;
    private final double H_tr_w;
    private final double H_tr_ms;
    private final double H_tr_em;
    private final double H_tr_is;
    private final double H_tr_ve;
    private final double H_tr_1;
    private final double H_tr_2;
    private final double H_tr_3;
    private final double maximumHeatingPower;

    private double currentMassTemperature;
    private double currentAirTemperature;
    private double currentMetabolicHeatGain;
    private double currentThermalPower;
    private ZonedDateTime currentTime;

    /**
     * A simple energy model of a dwelling.
     *
     * Consisting of one thermal capacity and five resistances, this model is derived from the
     * simple hourly dynamic model of the ISO 13790. It models heating energy demand only.
     *
     * The dwelling model is a one zone mode with a squared floor area. The entire floor area is
     * supposed to be heated. The dwelling consists of only one storey. Ventilation and infiltration
     * is limited to constant natural ventilation, and heat gains are limited to metabolic heat
     * gains.
     *
     * @param thermalMassCapacity capacity of the dwelling's thermal mass [J/K]
     * @param thermalMassArea area of the dwelling's thermal mass [m^2]
     * @param floorArea floor area of the squared dwelling [m^2]
     * @param roomHeight height of the one-storey dwelling [m]
     * @param windowToWallRatio the ratio between window area and wall area [-]
     * @param uWall thermal transmittance of the walls [W/(m^2*K)]
     * @param uRoof thermal transmittance of the roof [W/(m^2*K)]
     * @param uFloor thermal transmittance of the floor [W/(m^2*K)]
     * @param uWindow thermal transmittance of the windows [W/(m^2*K)]
     * @param transmissionAdjustmentGround adjustment factor for the heat transmission to ground due to different
     *                                     temperature in the ground [-]
     * @param naturalVentilationRate the air flow rate of natural ventilation [l/(s*m^2)]
     * @param maximumHeatingPower [W] (>= 0)
     * @param initialDwellingTemperature dwelling air temperature at start time [℃]
     * @param initialTime the initial time of the simulation
     * @param timeStepSize the time step size of the dwelling simulation
     * @param controlStrategyReference the heating control strategy applied in this dwelling
     * @param environmentReference the object from which environmental variables are obtained
     */
    public Dwelling(double thermalMassCapacity, double thermalMassArea,
                    double floorArea, double roomHeight, double windowToWallRatio,
                    double uWall, double uRoof, double uFloor, double uWindow,
                    double transmissionAdjustmentGround, double naturalVentilationRate,
                    double maximumHeatingPower, double initialDwellingTemperature,
                    ZonedDateTime initialTime, Duration timeStepSize,
                    HeatingControlStrategyReference controlStrategyReference,
                    EnvironmentReference environmentReference) {
        assert maximumHeatingPower >= 0;
        this.currentMassTemperature = initialDwellingTemperature;
        this.currentAirTemperature = initialDwellingTemperature;
        // FIXME The initial current air temperature is wrong. To determine it one would need to know the
        // environmental conditions from the environment reference. The simulation framework prohibits
        // that at the moment though.
        this.currentThermalPower = 0;
        this.heatingControlStrategy = controlStrategyReference;
        this.timeStepSize = timeStepSize;
        this.currentTime = initialTime;
        this.peopleInDwelling = new HashSet<>();
        this.environmentReference = environmentReference;
        this.maximumHeatingPower = maximumHeatingPower;

        double windowAndWallArea = roomHeight * floorArea * 4;
        double windowArea = windowAndWallArea * windowToWallRatio;
        double wallArea = windowAndWallArea - windowArea;
        double A_op = wallArea + 2 * floorArea;
        double U_op = (uWall * wallArea + uRoof * floorArea + transmissionAdjustmentGround * uFloor * floorArea) / A_op;
        double H_tr_op = A_op * U_op;

        this.A_f = floorArea;
        this.A_t = SURFACE_TO_FLOOR_RATIO * this.A_f;
        this.A_m = thermalMassArea;
        this.C_m = thermalMassCapacity;
        this.H_tr_w = windowArea * uWindow;
        this.H_tr_is = HEAT_TRANSMISSION_COEFFICIENT_AIR_TO_SURFACE * this.A_t;
        double q_ve = (0.04 + 0.06 * naturalVentilationRate) * floorArea / 1000;
        this.H_tr_ve = HEAT_CAPACITY_AIR * TIME_FRACTION_NATURAL_VENTILATION * q_ve;
        this.H_tr_ms = HEAT_TRANSMISSION_COEFFICIENT_MASS_TO_SURFACE * A_m;
        this.H_tr_em = 1 / ((1/H_tr_op) - (1/this.H_tr_ms));
        this.H_tr_1 = 1 / ((1/this.H_tr_ve) + (1/this.H_tr_is));
        this.H_tr_2 = H_tr_1 + this.H_tr_w;
        this.H_tr_3 = 1 / ((1/H_tr_2) + (1/this.H_tr_ms));
    }

    /**
     * Performs dwelling simulation for the next time step.
     */
    public CompletableFuture<Void> step() {
        CompletableFuture<Double>[] steps = new CompletableFuture[this.peopleInDwelling.size()];
        int i = 0;
        for (PersonReference person : this.peopleInDwelling) {
            steps[i] = person.getCurrentMetabolicRate();
            i++;
        }
        return CompletableFuture.allOf(steps).thenAccept(v ->
                    this.currentMetabolicHeatGain = Arrays.stream(steps)
                            .map(CompletableFuture::join)
                            .mapToDouble(Double::doubleValue).sum())
                .thenCombine(this.environmentReference.getCurrentTemperature(), (v, temp) -> temp)
                .thenAcceptBoth(this.heatingControlStrategy.heatingSetPoint(this.currentTime, this.peopleInDwelling),
                (temp, setPoint) -> this.step(setPoint, temp));
    }

    private void step(Optional<Double> heatingSetPoint, double externalTemperature) {
        double internalHeatGain = this.currentMetabolicHeatGain;
        double solarHeatGain = SOLAR_HEAT_GAIN;
        Function<Double, Double> nextMassTemperature = thermalPower ->
                this.nextTemperature(externalTemperature, thermalPower, internalHeatGain, solarHeatGain);
        double noPower = 0.0;
        double nextMassTemperatureNoPower = nextMassTemperature.apply(noPower);
        double nextAirTemperatureNoPower = this.massToAirTemperature(nextMassTemperatureNoPower, externalTemperature,
                noPower, internalHeatGain, solarHeatGain);
        if (!heatingSetPoint.isPresent() || nextAirTemperatureNoPower >= heatingSetPoint.get()) {
            this.currentMassTemperature = nextMassTemperatureNoPower;
            this.currentThermalPower = noPower;
            this.currentAirTemperature = nextAirTemperatureNoPower;
        }
        else {
            double tenWattPowerSquareMeterPower = 10 * this.A_f;
            double nextMassTemperaturePower10 = nextMassTemperature.apply(tenWattPowerSquareMeterPower);
            double nextAirTemperaturePower10 = this.massToAirTemperature(nextMassTemperaturePower10, externalTemperature,
                    tenWattPowerSquareMeterPower, internalHeatGain, solarHeatGain);
            double unrestrictedPower = (tenWattPowerSquareMeterPower *
                    (heatingSetPoint.get() - nextAirTemperatureNoPower) /
                    (nextAirTemperaturePower10 - nextAirTemperatureNoPower));
            double thermalPower;
            if (Math.abs(unrestrictedPower) <= Math.abs(this.maximumHeatingPower)) {
                thermalPower = unrestrictedPower;
            }
            else {
                thermalPower = this.maximumHeatingPower;
            }
            this.currentMassTemperature = nextMassTemperature.apply(thermalPower);
            this.currentThermalPower = thermalPower;
            this.currentAirTemperature = this.massToAirTemperature(this.currentMassTemperature, externalTemperature,
                    thermalPower, internalHeatGain, solarHeatGain);
        }
        this.currentTime = this.currentTime.plus(this.timeStepSize);
    }

    public double getCurrentAirTemperature() {
        return this.currentAirTemperature;
    }

    public double getCurrentThermalPower(){
        return this.currentThermalPower;
    }

    public void enter(PersonReference person) {
        this.peopleInDwelling.add(person);
    }

    public void leave(PersonReference person) {
        this.peopleInDwelling.remove(person);
    }

    private double massToAirTemperature(double massTemperature, double externalTemperature,
                                        double thermalPower, double internalGain, double solarGain) {
        double phi_ia = heatGainOnAirNode(internalGain);
        double phi_st = heatGainOnSurfaceNode(internalGain, solarGain);

        double theta_m = massTemperature;
        double theta_e = externalTemperature;
        double theta_sup = externalTemperature;
        double theta_s_nom = this.H_tr_ms * theta_m + phi_st + H_tr_w * theta_e + H_tr_1 * (theta_sup + (phi_ia + thermalPower) / this.H_tr_ve);
        double theta_s_denom = this.H_tr_ms + this.H_tr_w + this.H_tr_1;
        double theta_s = theta_s_nom / theta_s_denom;
        double theta_a_nom = this.H_tr_is * theta_s + this.H_tr_ve * theta_sup + phi_ia + thermalPower;
        double theta_a_denom = this.H_tr_is + this.H_tr_ve;
        return theta_a_nom / theta_a_denom;
    }

    private double heatGainOnAirNode(double internalGain){
        return 0.5 * internalGain;
    }

    private double heatGainOnSurfaceNode(double internalGain, double solarGain) {
        return (1 - this.A_m / this.A_t - this.H_tr_w / (9.1 * this.A_t)) * (0.5 * internalGain + solarGain);
    }

    private double heatGainOnMassNode(double internalGain, double solarGain) {
        return this.A_m / this.A_t * (0.5 * internalGain + solarGain);
    }

    private double nextTemperature(double externalTemperature, double thermalPower,
                                   double internalGain, double solarGain) {
        double phi_ia = this.heatGainOnAirNode(internalGain);
        double phi_st = this.heatGainOnSurfaceNode(internalGain, solarGain);
        double phi_m = this.heatGainOnMassNode(internalGain, solarGain);

        double theta_e = externalTemperature;
        double theta_sup = externalTemperature;
        double phi_2_3 = phi_st + this.H_tr_w * theta_e + this.H_tr_1 * ((phi_ia + thermalPower) / this.H_tr_ve + theta_sup);
        double phi_tot = phi_m + this.H_tr_em * theta_e + this.H_tr_3 / this.H_tr_2 * phi_2_3;

        double Cm_by_dt = this.C_m / (this.timeStepSize.toMillis() / 1000.0);

        double theta_m_nom = this.currentMassTemperature * (Cm_by_dt - 0.5 * (this.H_tr_3 + this.H_tr_em)) + phi_tot;
        double theta_m_denom = Cm_by_dt + 0.5 * (this.H_tr_3 + this.H_tr_em);

        return theta_m_nom / theta_m_denom;

    }
}
