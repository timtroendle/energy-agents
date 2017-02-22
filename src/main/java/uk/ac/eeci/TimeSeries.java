package uk.ac.eeci;

import org.javatuples.Pair;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TimeSeries<K> extends ArrayList<Pair<ZonedDateTime, K>> {

    public void add(ZonedDateTime timeStamp, K value) {
        this.add(new Pair<>(timeStamp, value));
    }

    public List<K> getValues() {
        return this.stream()
                .map(Pair<ZonedDateTime, K>::getValue1)
                .collect(Collectors.toList());
    }

    public List<ZonedDateTime> getIndex() {
        return this.stream()
                .map(Pair<ZonedDateTime, K>::getValue0)
                .collect(Collectors.toList());
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
}
