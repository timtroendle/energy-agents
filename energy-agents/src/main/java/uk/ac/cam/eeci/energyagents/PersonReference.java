package uk.ac.cam.eeci.energyagents;

import uk.ac.cam.eeci.framework.Reference;

import java.util.concurrent.CompletableFuture;

public class PersonReference extends Reference<Person> {

    public PersonReference(Person referent) {
        super(referent);
    }

    public CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(this.referent::step, this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Person.Activity> getCurrentActivity() {
        return CompletableFuture.supplyAsync(this.referent::getCurrentActivity, this.executor)
                .thenApplyAsync(i -> i, pool.currentExecutor());
    }

    public CompletableFuture<Double> getCurrentMetabolicRate() {
        return CompletableFuture.supplyAsync(this.referent::getCurrentMetabolicRate, this.executor)
                .thenApplyAsync(i -> i, pool.currentExecutor());
    }
}
