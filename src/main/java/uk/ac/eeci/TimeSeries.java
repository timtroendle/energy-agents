package uk.ac.eeci;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TimeSeries<K> {

    private List<ZonedDateTime> index;
    private List<K> values;

    public TimeSeries() {
        this.index = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    public void add(ZonedDateTime timeStamp, K value) {
        this.index.add(timeStamp);
        this.values.add(value);
    }

    public List<K> getValues() {
        return this.values;
    }

    public List<ZonedDateTime> getIndex() {
        return this.index;
    }

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

    public int size() {
        return this.index.size();
    }

}
