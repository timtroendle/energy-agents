package uk.ac.eeci;

import java.time.Duration;
import java.util.Optional;

public class Environment {

    private final TimeSeries<Double> temperatureTimeSeries;
    private int index;
    private double currentTemperature;

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
        this.currentTemperature = this.temperatureTimeSeries.get(this.index).getValue1();

    }

    public void step() {
        this.index += 1;
        this.currentTemperature = this.temperatureTimeSeries.get(this.index).getValue1();
    }

    public double getCurrentTemperature() {
        return this.currentTemperature;
    }
}
