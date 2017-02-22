package uk.ac.eeci;

import io.improbable.scienceos.Reference;

import java.util.concurrent.CompletableFuture;

public class EnvironmentReference extends Reference<Environment> {

    public EnvironmentReference(Environment referent) {
        super(referent);
    }

    public CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(this.referent::step, this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Double> getCurrentTemperature() {
        return CompletableFuture.supplyAsync(this.referent::getCurrentTemperature, this.executor)
                .thenApplyAsync(i -> i, pool.currentExecutor());
    }
}
