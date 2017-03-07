package uk.ac.eeci;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Dwelling {

    private double currentTemperature;
    private double currentThermalPower;
    private final double heatMassCapacity;
    private final double heatTransmission;
    private final double maximumCoolingPower;
    private final double maximumHeatingPower;
    private final double conditionedFloorArea;
    private final HeatingControlStrategy heatingControlStrategy;
    private final EnvironmentReference environmentReference;
    private final Set<PersonReference> peopleInDwelling;
    private final Duration timeStepSize;

    /**
     * A simple energy model of a dwelling.
     *
     * Consisting of one thermal capacity and one resistance, this model is derived from the
     * hourly dynamic model of the ISO 13790. It models heating and cooling energy demand only.
     *
     *
     * @param heatMassCapacity capacity of the dwelling's heat mass [J/K]
     * @param heatTransmission heat transmission to the outside [W/K]
     * @param maximumCoolingPower [W] (<= 0)
     * @param maximumHeatingPower [W] (>= 0)
     * @param initialDwellingTemperature dwelling temperature at start time [℃]
     * @param conditionedFloorArea [m**2]
     * @param controlStrategy the heating control strategy applied in this dwelling
     * @param timeStepSize the time step size of the dwelling simulation
     * @param environmentReference
     */
    public Dwelling(double heatMassCapacity, double heatTransmission, double maximumCoolingPower,
                    double maximumHeatingPower, double initialDwellingTemperature,
                    double conditionedFloorArea, Duration timeStepSize,
                    HeatingControlStrategy controlStrategy, EnvironmentReference environmentReference) {
        assert maximumCoolingPower <= 0;
        assert maximumHeatingPower >= 0;
        this.currentTemperature = initialDwellingTemperature;
        this.currentThermalPower = 0;
        this.heatMassCapacity = heatMassCapacity;
        this.heatTransmission = heatTransmission;
        this.maximumCoolingPower = maximumCoolingPower;
        this.maximumHeatingPower = maximumHeatingPower;
        this.conditionedFloorArea = conditionedFloorArea;
        this.heatingControlStrategy = controlStrategy;
        this.timeStepSize = timeStepSize;
        this.peopleInDwelling = new HashSet<>();
        this.environmentReference = environmentReference;
    }

    /**
     * Performs dwelling simulation for the next time step.
     */
    public CompletableFuture<Void> step() {
        return this.environmentReference.getCurrentTemperature().thenAccept(this::step);
    }

    private void step(double outsideTemperature) {
        double heatingSetPoint = this.heatingControlStrategy.heatingSetPoint(this.peopleInDwelling);
        double coolingSetPoint = this.heatingControlStrategy.coolingSetPoint(this.peopleInDwelling);
        Function<Double, Double> nextTemperature = thermalPower ->
                this.nextTemperature(outsideTemperature, thermalPower);
        double noPower = 0.0;
        double nextTemperatureNoPower = nextTemperature.apply(noPower);
        if (nextTemperatureNoPower >= heatingSetPoint && nextTemperatureNoPower <= coolingSetPoint) {
            this.currentTemperature = nextTemperatureNoPower;
            this.currentThermalPower = noPower;
        }
        else {
            double setPoint;
            double maxPower;
            if (nextTemperatureNoPower < heatingSetPoint) {
                setPoint = heatingSetPoint;
                maxPower = this.maximumHeatingPower;
            }
            else {
                setPoint = coolingSetPoint;
                maxPower = this.maximumCoolingPower;
            }
            double tenWattPowerSquareMeterPower = 10 * this.conditionedFloorArea;
            double nextTemperaturePower10 = nextTemperature.apply(tenWattPowerSquareMeterPower);
            double unrestrictedPower = (tenWattPowerSquareMeterPower *
                    (setPoint - nextTemperatureNoPower) /
                    (nextTemperaturePower10 - nextTemperatureNoPower));
            double thermalPower;
            if (Math.abs(unrestrictedPower) <= Math.abs(maxPower)) {
                thermalPower = unrestrictedPower;
            }
            else {
                thermalPower = maxPower;
            }
            this.currentTemperature = nextTemperature.apply(thermalPower);
            this.currentThermalPower = thermalPower;
        }
    }

    public double getTemperature() {
        return this.currentTemperature;
    }

    public double getThermalPower(){
        return this.currentThermalPower;
    }

    public void enter(PersonReference person) {
        this.peopleInDwelling.add(person);
    }

    public void leave(PersonReference person) {
        this.peopleInDwelling.remove(person);
    }

    private double nextTemperature(double outsideTemperature, double thermalPower) {
        double dt_by_cm = this.timeStepSize.toMillis() / 1000.0 / this.heatMassCapacity;
        return (this.currentTemperature * (1 - dt_by_cm * this.heatTransmission) +
                dt_by_cm * (thermalPower + this.heatTransmission * outsideTemperature));

    }
}
