package uk.ac.eeci;

import java.util.List;

public interface HeatingControlStrategy {

    double heatingSetPoint(List<PersonReference> peopleInDwelling);

    double coolingSetPoint(List<PersonReference> peopleInDwelling);
}
