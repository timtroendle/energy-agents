package uk.ac.cam.eeci.energyagents.strategy;

import uk.ac.cam.eeci.energyagents.HeatingControlStrategy;
import uk.ac.cam.eeci.energyagents.PersonReference;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A heating control strategy that does its best to participate to climate change.
 *
 * Good luck strategy, you can make it!
 */
public class ClimateChangingControlStrategy extends HeatingControlStrategy {

    private final double heatingSetPoint;

    /**
     *
     * @param heatingSetPoint The constant heating set point.
     */
    public ClimateChangingControlStrategy(double heatingSetPoint) {
        this.heatingSetPoint = heatingSetPoint;
    }

    @Override
    public CompletableFuture<Optional<Double>> heatingSetPoint(ZonedDateTime timeStamp,
                                                               Set<PersonReference> peopleInDwelling) {
        return CompletableFuture.completedFuture(Optional.of(this.heatingSetPoint));
    }

}
