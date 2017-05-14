package uk.ac.cam.eeci.energyagents;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * A time series of values.
 *
 * @param <K> the type of the values of the time series
 */
public class TimeSeries<K> {

    private List<ZonedDateTime> index;
    private List<K> values;

    public TimeSeries() {
        this.index = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    /**
     * Adds a new value to the end of the time series.
     *
     * @param timeStamp the time stamp of the new entry
     * @param value the value of the new entry
     */
    public void add(ZonedDateTime timeStamp, K value) {
        this.index.add(timeStamp);
        this.values.add(value);
    }

    /**
     *
     * @return all values in the time series
     */
    public List<K> getValues() {
        return this.values;
    }

    /**
     *
     * @return the time index of the time series
     */
    public List<ZonedDateTime> getIndex() {
        return this.index;
    }

    /**
     *
     * @return the constant time step size if time step size is constant
     */
    public Optional<Duration> getConstantTimeStepSize() {
        List<Duration> timeStepSizes = new ArrayList<>();
        List<ZonedDateTime> index = this.getIndex();
        for (int i = 0; i < this.size() - 1; i++) {
            timeStepSizes.add(Duration.ofSeconds(index.get(i).until(index.get(i + 1), ChronoUnit.SECONDS)));
        }
        Duration reference = timeStepSizes.get(0);
        for (Duration timeStepSize : timeStepSizes) {
            if (!timeStepSize.equals(reference)) {
                return Optional.empty();
            }
        }
        return Optional.of(reference);
    }

    /**
     *
     * @return the lenght of the time series
     */
    public int size() {
        return this.index.size();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TimeSeries))
            return false;
        if (other == this)
            return true;

        TimeSeries rhs = (TimeSeries) other;
        return this.index.equals(rhs.index) & this.values.equals(rhs.values);
    }

    @Override
    public int hashCode() {
        return this.index.hashCode() ^ this.values.hashCode();
    }
}
