package uk.ac.eeci;

import io.improbable.science.PoolPool;
import io.improbable.science.Reference;

import java.util.concurrent.CompletableFuture;

public class PersonReference extends Reference<Person> {

    public PersonReference(Person referent) {
        super(referent);
    }

    public CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(() -> referent.step(), executor).thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Person.Activity> getCurrentActivity() {
        return CompletableFuture.supplyAsync(() -> referent.getCurrentActivity(), executor).thenApplyAsync((i) ->{return(i);}, PoolPool.currentExecutor());
    }
}
