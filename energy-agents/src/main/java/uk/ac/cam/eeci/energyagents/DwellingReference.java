package uk.ac.cam.eeci.energyagents;

import uk.ac.cam.eeci.framework.Reference;

import java.util.concurrent.CompletableFuture;

public class DwellingReference extends Reference<Dwelling> {

    public DwellingReference(Dwelling referent) {
        super(referent);
    }

    public CompletableFuture<Void> step() {
        return CompletableFuture.completedFuture(null).thenComposeAsync((p) -> this.referent.step(), this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Double> getCurrentAirTemperature() {
        return CompletableFuture.supplyAsync(this.referent::getCurrentAirTemperature, this.executor)
                .thenApplyAsync(i -> i, pool.currentExecutor());
    }

    public CompletableFuture<Double> getCurrentThermalPower() {
        return CompletableFuture.supplyAsync(this.referent::getCurrentThermalPower, this.executor)
                .thenApplyAsync(i -> i, pool.currentExecutor());
    }

    public CompletableFuture<Void> enter(PersonReference person) {
        return CompletableFuture.runAsync(() -> this.referent.enter(person), this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Void> leave(PersonReference person) {
        return CompletableFuture.runAsync(() -> this.referent.leave(person), this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }
}
