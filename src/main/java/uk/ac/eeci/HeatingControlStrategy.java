package uk.ac.eeci;

import java.util.Optional;
import java.util.Set;

/**
 * A control strategy for the heating system of dwellings.
 *
 * A control strategy is a -- potentially dynamic -- controller that decides
 * on switch states of the heating system (on/off) and heating set points. It
 * can base its decisions on the people that occupy the dwelling at the current
 * moment, but doesn't have to.
 *
 */
public interface HeatingControlStrategy {

    /**
     * Determines the current heating set point for the heating system of a dwelling.
     *
     * @param peopleInDwelling The people that currently occupy the dwelling.
     * @return the heating set point for the heating system; can be empty in
     *         which case a switch off of the heating system is demanded,
     *         should it be turned on.
     */
    Optional<Double> heatingSetPoint(Set<PersonReference> peopleInDwelling);

}
