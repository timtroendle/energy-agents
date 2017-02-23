package uk.ac.eeci;

import io.improbable.scienceos.Reference;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class DataLoggerReference extends Reference<DataLogger> {

    public DataLoggerReference(DataLogger dataLogger) {
        super(dataLogger);
    }

    public CompletableFuture<Void> step(ZonedDateTime currentTime) {
        return this.referent.step(currentTime).thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Void> write(HashMap<String, String> metadata) {
        return this.referent.write(metadata).thenRunAsync(() ->{}, pool.currentExecutor());
    }

}
