package uk.ac.eeci.strategy;

import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.PersonReference;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

public class NoHeatingStrategy implements HeatingControlStrategy {


    @Override
    public Optional<Double> heatingSetPoint(ZonedDateTime timeStamp, Set<PersonReference> peopleInDwelling) {
        return Optional.empty();
    }
}
