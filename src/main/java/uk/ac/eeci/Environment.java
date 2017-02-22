package uk.ac.eeci;

import java.time.Duration;
import java.util.Optional;

public class Environment {

    private final TimeSeries<Double> temperatureTimeSeries;
    private int index = 0;

    public Environment(TimeSeries<Double> temperatureTimeSeries, Duration timeStepSize) {
        this.temperatureTimeSeries = temperatureTimeSeries;
        Optional<Duration> inferredTimeStepSize = this.temperatureTimeSeries.getConstantTimeStepSize();
        if (!inferredTimeStepSize.isPresent() || !inferredTimeStepSize.get().equals(timeStepSize)) {
            String msg = "The temperature time series must have a constant time step size.";
            throw new IllegalArgumentException(msg);
        }
    }

    public void step() {
        this.index += 1;
    }

    public double getCurrentTemperature() {
        return temperatureTimeSeries.getValues().get(this.index);
    }
}
