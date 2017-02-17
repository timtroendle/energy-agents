package uk.ac.eeci;

import io.improbable.scienceos.Reference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DataLoggerReference extends Reference<DataLogger> {

    public DataLoggerReference(DataLogger dataLogger) {
        super(dataLogger);
    }

    public CompletableFuture<Void> step() {
        return CompletableFuture.runAsync(this.referent::step, this.executor);
    }

    public CompletableFuture<Map<DwellingReference, List<Double>>> getTemperatureRecord() {
        return CompletableFuture.supplyAsync(this.referent::getTemperatureRecord, this.executor);
    }

}
