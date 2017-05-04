package uk.ac.cam.eeci.energyagents.strategy;

import uk.ac.cam.eeci.energyagents.PersonReference;
import uk.ac.cam.eeci.energyagents.HeatingControlStrategy;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NoHeatingStrategy extends HeatingControlStrategy {


    @Override
    public CompletableFuture<Optional<Double>> heatingSetPoint(ZonedDateTime timeStamp,
                                                               Set<PersonReference> peopleInDwelling) {
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
