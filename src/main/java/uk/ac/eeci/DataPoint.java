package uk.ac.eeci;

import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class DataPoint<K, T> {

    private final List<T> values;
    private final List<K> dataPointSources;
    private final Function<K, CompletableFuture<T>> valueSupplier;
    private final String name;

    public DataPoint(String name, List<K> dataPointSources, Function<K, CompletableFuture<T>> valueSupplier) {
        this.name = name;
        this.values = new ArrayList<T>();
        this.valueSupplier = valueSupplier;
        this.dataPointSources = dataPointSources;
    }

    public String getName() {
        return this.name;
    }

    public void step() {
        Map<K, T> values = new ConcurrentHashMap<>();
        CompletableFuture<Void>[] updates = new CompletableFuture[this.dataPointSources.size()];
        for (int i = 0; i < this.dataPointSources.size(); ++i) {
            updates[i] = this.getValue(this.dataPointSources.get(i))
                    .thenAccept(pair -> values.put(pair.getValue0(), pair.getValue1()));
        }
        try {
            CompletableFuture.allOf(updates).get();
        } catch (InterruptedException e) {
            e.printStackTrace(); // FIXME proper exception handling necessary
        } catch (ExecutionException e) {
            e.printStackTrace(); // FIXME proper exception handling necessary
        }
        this.values.addAll(this.mapToSortedList(values));
    }

    public Map<Integer, List<T>> getRecord(){
        List<List<T>> temperatureRecord = new ArrayList<>();
        for (int i = 0; i < this.dataPointSources.size(); i++) {
            temperatureRecord.add(i, new ArrayList<>());
        }
        for (int i=0; i < this.values.size(); i++) {
            int listIndex = i % this.dataPointSources.size();
            temperatureRecord.get(listIndex).add(this.values.get(i));
        }
        Map<Integer, List<T>> temperatureRecordMap = new HashMap<>();
        for (int i = 0; i < this.dataPointSources.size(); i++) {
            temperatureRecordMap.put(i, temperatureRecord.get(i));
        }
        return temperatureRecordMap;
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
