package uk.ac.eeci;

import io.improbable.science.PoolPool;
import io.improbable.science.Reference;

import java.util.concurrent.CompletableFuture;

public class CitySimulationReference extends Reference<CitySimulation> {

    public CitySimulationReference(CitySimulation referent) {
        super(referent);
    }

    CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(() -> referent.step(), executor).thenRunAsync(() ->{}, PoolPool.currentExecutor());
    }
}
