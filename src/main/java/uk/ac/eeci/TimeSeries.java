package uk.ac.eeci;

import org.javatuples.Pair;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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
}
