package uk.ac.eeci.strategy;

import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.PersonReference;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * A heating control strategy that does its best to participate to climate change.
 *
 * Good luck strategy, you can make it!
 */
public class ClimateChangingControlStrategy implements HeatingControlStrategy {

    private final double heatingSetPoint;

    /**
     *
     * @param heatingSetPoint The constant heating set point.
     */
    public ClimateChangingControlStrategy(double heatingSetPoint) {
        this.heatingSetPoint = heatingSetPoint;
    }

    @Override
    public Optional<Double> heatingSetPoint(ZonedDateTime timeStamp, Set<PersonReference> peopleInDwelling) {
        return Optional.of(this.heatingSetPoint);
    }

}
