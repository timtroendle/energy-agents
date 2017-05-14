package uk.ac.cam.eeci.energyagents;

import java.time.Duration;
import java.util.Optional;

/**
 * A simulation-global class representing environmental variables.
 * <br><br>
 * No differentiation is been made on the location, i.e. variables values are valid globally.
 */
public class Environment {

    private final TimeSeries<Double> temperatureTimeSeries;
    private int index;
    private double currentTemperature;

    /**
     *
     * @param temperatureTimeSeries time series of all temperature values for the simulation
     * @param timeStepSize time step size of the time series
     */
    public Environment(TimeSeries<Double> temperatureTimeSeries, Duration timeStepSize) {
        this.temperatureTimeSeries = temperatureTimeSeries;
        Optional<Duration> inferredTimeStepSize = this.temperatureTimeSeries.getConstantTimeStepSize();
        if (!inferredTimeStepSize.isPresent() || !inferredTimeStepSize.get().equals(timeStepSize)) {
            String msg = String.format(
                    "The temperature time series must have a constant time step size of size %s.",
                    timeStepSize
            );
            throw new IllegalArgumentException(msg);
        }
        this.index = 0;
        this.currentTemperature = this.temperatureTimeSeries.getValues().get(this.index);

    }

    public void step() {
        this.index += 1;
        this.currentTemperature = this.temperatureTimeSeries.getValues().get(this.index);
    }

    /**
     *
     * @return current city-wide temperature
     */
    public double getCurrentTemperature() {
        return this.currentTemperature;
    }
}
