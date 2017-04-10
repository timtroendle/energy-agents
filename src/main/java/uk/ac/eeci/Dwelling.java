package uk.ac.eeci;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.DoubleStream;

public class Dwelling {

    private double currentTemperature;
    private double currentThermalPower;
    private double currentMetabolicHeatGain;
    private ZonedDateTime currentTime;
    private final double heatMassCapacity;
    private final double heatTransmission;
    private final double maximumHeatingPower;
    private final double conditionedFloorArea;
    private final HeatingControlStrategyReference heatingControlStrategy;
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
     * @param maximumHeatingPower [W] (>= 0)
     * @param initialDwellingTemperature dwelling temperature at start time [℃]
     * @param conditionedFloorArea [m**2]
     * @param controlStrategyReference the heating control strategy applied in this dwelling
     * @param initialTime the initial time of the simulation
     * @param timeStepSize the time step size of the dwelling simulation
     * @param environmentReference
     */
    public Dwelling(double heatMassCapacity, double heatTransmission,
                    double maximumHeatingPower, double initialDwellingTemperature,
                    double conditionedFloorArea, ZonedDateTime initialTime, Duration timeStepSize,
                    HeatingControlStrategyReference controlStrategyReference,
                    EnvironmentReference environmentReference) {
        assert maximumHeatingPower >= 0;
        this.currentTemperature = initialDwellingTemperature;
        this.currentThermalPower = 0;
        this.heatMassCapacity = heatMassCapacity;
        this.heatTransmission = heatTransmission;
        this.maximumHeatingPower = maximumHeatingPower;
        this.conditionedFloorArea = conditionedFloorArea;
        this.heatingControlStrategy = controlStrategyReference;
        this.timeStepSize = timeStepSize;
        this.currentTime = initialTime;
        this.peopleInDwelling = new HashSet<>();
        this.environmentReference = environmentReference;
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

    private void step(Optional<Double> heatingSetPoint, double outsideTemperature) {
        Function<Double, Double> nextTemperature = thermalPower ->
                this.nextTemperature(outsideTemperature, thermalPower, this.currentMetabolicHeatGain);
        double noPower = 0.0;
        double nextTemperatureNoPower = nextTemperature.apply(noPower);
        if (!heatingSetPoint.isPresent() || nextTemperatureNoPower >= heatingSetPoint.get()) {
            this.currentTemperature = nextTemperatureNoPower;
            this.currentThermalPower = noPower;
        }
        else {
            double tenWattPowerSquareMeterPower = 10 * this.conditionedFloorArea;
            double nextTemperaturePower10 = nextTemperature.apply(tenWattPowerSquareMeterPower);
            double unrestrictedPower = (tenWattPowerSquareMeterPower *
                    (heatingSetPoint.get() - nextTemperatureNoPower) /
                    (nextTemperaturePower10 - nextTemperatureNoPower));
            double thermalPower;
            if (Math.abs(unrestrictedPower) <= Math.abs(this.maximumHeatingPower)) {
                thermalPower = unrestrictedPower;
            }
            else {
                thermalPower = this.maximumHeatingPower;
            }
            this.currentTemperature = nextTemperature.apply(thermalPower);
            this.currentThermalPower = thermalPower;
        }
        this.currentTime = this.currentTime.plus(this.timeStepSize);
    }

    public double getCurrentTemperature() {
        return this.currentTemperature;
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

    private double nextTemperature(double outsideTemperature, double thermalPower, double heatGains) {
        double dt_by_cm = this.timeStepSize.toMillis() / 1000.0 / this.heatMassCapacity;
        return (this.currentTemperature * (1 - dt_by_cm * this.heatTransmission) +
                dt_by_cm * (thermalPower + this.heatTransmission * outsideTemperature + heatGains));

    }
}
