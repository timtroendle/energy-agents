package uk.ac.eeci;

import io.improbable.scienceos.Reference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DataPointReference<K, T> extends Reference<DataPoint<K, T>> {

    public DataPointReference(DataPoint referent) {
        super(referent);
    }

    public CompletableFuture<String> getName() {
        return CompletableFuture.supplyAsync(this.referent::getName, this.executor);
    }

    public CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(this.referent::step, this.executor);
    }

    public CompletableFuture<Map<Integer, List<T>>> getRecord() {
        return CompletableFuture.supplyAsync(this.referent::getRecord, this.executor);
    }
}
