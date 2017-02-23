package uk.ac.eeci;

import io.improbable.scienceos.Reference;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DataPointReference<K, T> extends Reference<DataPoint<K, T>> {

    public DataPointReference(DataPoint referent) {
        super(referent);
    }

    public CompletableFuture<String> getName() {
        return CompletableFuture.supplyAsync(this.referent::getName, this.executor)
                .thenApplyAsync(i -> i, pool.currentExecutor());
    }

    public CompletableFuture<Void> step(ZonedDateTime currentTime) {
        return CompletableFuture.completedFuture(null).thenComposeAsync((p) -> this.referent.step(currentTime), this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Map<Integer, TimeSeries<T>>> getRecord() {
        return CompletableFuture.supplyAsync(this.referent::getRecord, this.executor)
                .thenApplyAsync(i -> i, pool.currentExecutor());
    }
}
