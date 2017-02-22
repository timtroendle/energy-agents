package uk.ac.eeci.test.integration;

import io.improbable.scienceos.EndSimulationException;
import uk.ac.eeci.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * A subclass of CitySimulation that stops simulation after a predefined amount of time steps.
 */
class ShortSimulation extends CitySimulation {

    private int remainingSteps;

    public ShortSimulation(Collection<DwellingReference> dwellings, Collection<PersonReference> people,
                           EnvironmentReference environment,
                           DataLoggerReference dataLoggerReference, ZonedDateTime initialTime,
                           Duration timeStepSize, int numberSteps) {
        super(dwellings, people, environment, dataLoggerReference, initialTime, timeStepSize);
        this.remainingSteps = numberSteps;
    }

    public void step() throws InterruptedException, ExecutionException, EndSimulationException {
        if (this.remainingSteps > 0) {
            super.step();
            this.remainingSteps -= 1;
        } else {
            throw new EndSimulationException();
        }
    }
}
