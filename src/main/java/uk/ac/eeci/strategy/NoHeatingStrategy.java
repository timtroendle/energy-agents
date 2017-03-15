package uk.ac.eeci.strategy;

import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.PersonReference;

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
