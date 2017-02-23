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
        return CompletableFuture.completedFuture(null).thenComposeAsync((p) -> this.referent.step(currentTime), this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

    public CompletableFuture<Void> write(HashMap<String, String> metadata) {
        return CompletableFuture.completedFuture(null).thenComposeAsync((p) -> this.referent.write(metadata), this.executor)
                .thenRunAsync(() ->{}, pool.currentExecutor());
    }

}
