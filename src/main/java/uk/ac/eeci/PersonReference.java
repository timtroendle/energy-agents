package uk.ac.eeci;

import io.improbable.scienceos.Reference;

import java.util.concurrent.CompletableFuture;

public class PersonReference extends Reference<Person> {

    public PersonReference(Person referent) {
        super(referent);
    }

    public CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(() -> this.referent.step(), this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Person.Activity> getCurrentActivity() {
        return CompletableFuture.supplyAsync(() -> this.referent.getCurrentActivity(), this.executor)
                .thenApplyAsync((i) ->i, pool.currentExecutor());
    }
}
