package uk.ac.cam.eeci.energyagents;

import org.javatuples.Pair;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A data point defines a time varying data source to be logged.
 *
 * @param <K> The type from which data shall be logged.
 * @param <T> Data type to be logged.
 */
public class DataPoint<K, T> {

    private final List<T> values;
    private final List<ZonedDateTime> index;
    private final List<K> dataPointSources;
    private final Map<K, Integer> indexOfDataPointSources;
    private final Function<K, CompletableFuture<T>> valueSupplier;
    private final String name;

    /**
     *
     * @param name name of the data point
     * @param dataPointSources the data point sources, a map from unique ids to data point sources
     * @param valueSupplier a function through which the current value of the data point can be accessed.
     */
    public DataPoint(String name, Map<Integer, K> dataPointSources, Function<K, CompletableFuture<T>> valueSupplier) {
        this.name = name;
        this.values = new ArrayList<>();
        this.index = new ArrayList<>();
        this.valueSupplier = valueSupplier;
        this.dataPointSources = new ArrayList<>(dataPointSources.values());
        this.indexOfDataPointSources = dataPointSources.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public String getName() {
        return this.name;
    }

    public CompletableFuture<Void> step(ZonedDateTime currentTime) {
        Map<K, T> values = new ConcurrentHashMap<>();
        CompletableFuture<Void>[] updates = new CompletableFuture[this.dataPointSources.size()];
        for (int i = 0; i < this.dataPointSources.size(); ++i) {
            updates[i] = this.getValue(this.dataPointSources.get(i))
                    .thenAccept(pair -> values.put(pair.getValue0(), pair.getValue1()));
        }
        return CompletableFuture.allOf(updates)
                .thenRun(() -> this.values.addAll(this.mapToSortedList(values)))
                .thenRun(() -> this.index.add(currentTime));
    }

    /**
     *
     * @return the complete record of historic values of the data point
     */
    public Map<Integer, TimeSeries<T>> getRecord(){
        List<TimeSeries<T>> timeSeries = new ArrayList<>();
        for (int i = 0; i < this.dataPointSources.size(); i++) {
            timeSeries.add(i, new TimeSeries<>());
        }
        for (int i=0; i < this.values.size(); i++) {
            int listIndex = i % this.dataPointSources.size();
            int timeStepIndex = Math.floorDiv(i, this.dataPointSources.size());
            timeSeries.get(listIndex).add(this.index.get(timeStepIndex), this.values.get(i));
        }
        Map<Integer, TimeSeries<T>> timeSeriesMap = new HashMap<>();
        for (K source : this.dataPointSources) {
            int internalIndex = this.dataPointSources.indexOf(source);
            int externalIndex = this.indexOfDataPointSources.get(source);
            timeSeriesMap.put(externalIndex, timeSeries.get(internalIndex));
        }
        return timeSeriesMap;
    }

    private CompletableFuture<Pair<K, T>> getValue(K dataPointSource) {
        return this.valueSupplier.apply(dataPointSource).thenApplyAsync(temp -> new Pair<>(dataPointSource, temp));
    }

    private List<T> mapToSortedList(Map<K, T> valueMap) {
        List<T> values = new ArrayList<>(this.dataPointSources.size());
        for (K dataPointSource : this.dataPointSources) {
            values.add(valueMap.get(dataPointSource));
        }
        return values;
    }

}
