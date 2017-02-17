package uk.ac.eeci;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.improbable.scienceos.Reference;

public class DwellingSetReference extends Reference<DwellingSet> {

    public DwellingSetReference(DwellingSet referent) {
        super(referent);
    }

    public CompletableFuture<List<DwellingReference>> getDwellings() {
        return CompletableFuture.supplyAsync(() -> this.referent.getDwellings(), this.executor);
    }

    public CompletableFuture<List<Double>> getTemperatures() throws InterruptedException, ExecutionException {
        return CompletableFuture.supplyAsync(() -> this.referent.getTemperatures(), this.executor);
    }
}
