package uk.ac.eeci.strategy;

import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.PersonReference;

import java.util.Set;

/**
 * A heating control strategy that does its best to participate to climate change.
 *
 * Good luck strategy, you can make it!
 */
public class ClimateChangingControlStrategy implements HeatingControlStrategy {

    private final double heatingSetPoint;
    private final double coolingSetPoint;

    /**
     *
     * @param heatingSetPoint The constant heating set point.
     * @param coolingSetPoint The constant cooling set point.
     */
    public ClimateChangingControlStrategy(double heatingSetPoint, double coolingSetPoint) {
        this.heatingSetPoint = heatingSetPoint;
        this.coolingSetPoint = coolingSetPoint;
    }

    @Override
    public double heatingSetPoint(Set<PersonReference> peopleInDwelling) {
        return this.heatingSetPoint;
    }

    @Override
    public double coolingSetPoint(Set<PersonReference> peopleInDwelling) {
        return this.coolingSetPoint;
    }
}
