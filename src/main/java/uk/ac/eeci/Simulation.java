package uk.ac.eeci;

import java.util.concurrent.CompletableFuture;

public class Simulation implements ISteppable {

    public Simulation() {
    }

    public CompletableFuture<Void> step() {
        CompletableFuture<Void> aFuture = new CompletableFuture<>();
        aFuture.complete(null);
        return aFuture;
    }

}
