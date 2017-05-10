package uk.ac.cam.eeci.energyagents;

import uk.ac.cam.eeci.framework.Reference;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class HeatingControlStrategyReference extends Reference<HeatingControlStrategy> {

    public HeatingControlStrategyReference(HeatingControlStrategy referent) {
        super(referent);
    }

    public CompletableFuture<Optional<Double>> heatingSetPoint(ZonedDateTime timeStamp, Set<PersonReference> peopleInDwelling) {
        return this.referent.heatingSetPoint(timeStamp, peopleInDwelling)
                .thenApplyAsync((setPoint) -> setPoint, pool.currentExecutor());
    }
}
