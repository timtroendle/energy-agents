package uk.ac.eeci;

import java.util.Set;

public interface HeatingControlStrategy {

    double heatingSetPoint(Set<PersonReference> peopleInDwelling);

    double coolingSetPoint(Set<PersonReference> peopleInDwelling);
}
