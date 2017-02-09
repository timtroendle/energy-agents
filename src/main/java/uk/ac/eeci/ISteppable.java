package uk.ac.eeci;

import java.util.concurrent.CompletableFuture;

public interface ISteppable {
    CompletableFuture<Void> step();
}
