package uk.ac.eeci;

import io.improbable.science.PoolPool;
import io.improbable.science.Reference;

import java.util.concurrent.CompletableFuture;

public class DwellingReference extends Reference<Dwelling> {

    public DwellingReference(Dwelling referent) {
        super(referent);
    }

    public CompletableFuture<Void> step(double outsideTemperature) {
        return CompletableFuture.runAsync(() -> referent.step(outsideTemperature), executor).thenRunAsync(() ->{}, PoolPool.currentExecutor());
    }

    CompletableFuture<Double> getTemperature() {
        return CompletableFuture.supplyAsync(() -> referent.getTemperature(), executor).thenApplyAsync((i) ->{return(i);}, PoolPool.currentExecutor());
    }

    public CompletableFuture<Void> enter(PersonReference person) {
        return CompletableFuture.runAsync(() -> referent.enter(person), executor).thenRunAsync(() ->{}, PoolPool.currentExecutor());
    }

    public CompletableFuture<Void> leave(PersonReference person) {
        return CompletableFuture.runAsync(() -> referent.leave(person), executor).thenRunAsync(() ->{}, PoolPool.currentExecutor());
    }
}
