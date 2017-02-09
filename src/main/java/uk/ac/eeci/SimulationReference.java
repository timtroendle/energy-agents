package uk.ac.eeci;

import java.util.concurrent.CompletableFuture;

public class SimulationReference extends Reference<Simulation> {
    public SimulationReference(Simulation referent) {
        super(referent);
    }

    CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(() -> referent.step(), executor).thenRunAsync(() ->{}, PoolPool.currentExecutor());
    }

}
