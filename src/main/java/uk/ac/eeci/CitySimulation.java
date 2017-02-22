package uk.ac.eeci;

import io.improbable.scienceos.EndSimulationException;
import io.improbable.scienceos.ISimulation;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Defines an entire simulation run.
 */
public class CitySimulation implements ISimulation {

    private final Set<DwellingReference> dwellings;
    private final Set<PersonReference> people;
    private final EnvironmentReference environment;
    private final DataLoggerReference dataLoggerReference;
    private final Duration timeStepSize;
    private ZonedDateTime currentTime;
    private int remainingSteps;

    /**
     * @param dwellings The set of all dwellings in the city.
     * @param people The set of all people in the city.
     */
    public CitySimulation(Collection<DwellingReference> dwellings, Collection<PersonReference> people,
                          EnvironmentReference environment, DataLoggerReference dataLoggerReference,
                          ZonedDateTime startTime, Duration timeStepSize, int numberSteps) {
        this.dwellings = new HashSet<>(dwellings);
        this.people = new HashSet<>(people);
        this.environment = environment;
        this.dataLoggerReference = dataLoggerReference;
        this.currentTime = startTime;
        this.timeStepSize = timeStepSize;
        this.remainingSteps = numberSteps;
    }

    @Override
    public void step() throws InterruptedException, ExecutionException, EndSimulationException {
        if (this.remainingSteps > 0) {
            this.performStep();
            this.remainingSteps -= 1;
        } else {
            throw new EndSimulationException();
        }
    }

    private void performStep() throws ExecutionException, InterruptedException, EndSimulationException {
        List<CompletableFuture<Void>> peopleSteps = new ArrayList<>();
        for (PersonReference person : this.people) {
            peopleSteps.add(person.step());
        }
        CompletableFuture<Void>[] array = new CompletableFuture[peopleSteps.size()];
        array = peopleSteps.toArray(array);

        CompletableFuture.allOf(array).get();

        List<CompletableFuture<Void>> dwellingSteps = new ArrayList<>();
        for (DwellingReference dwelling : this.dwellings) {
            dwellingSteps.add(dwelling.step());
        }
        CompletableFuture<Void>[] dStepsArray = new CompletableFuture[dwellingSteps.size()];
        dStepsArray = dwellingSteps.toArray(dStepsArray);
        CompletableFuture.allOf(dStepsArray).get();

        this.environment.step().get();
        this.currentTime = this.currentTime.plus(this.timeStepSize);
        if (this.dataLoggerReference != null) {
            this.dataLoggerReference.step(this.currentTime).get();
        }
    }

    @Override
    public void stop() {
        if (this.dataLoggerReference != null) {
            try {
                this.dataLoggerReference.write().get();
            } catch (InterruptedException|ExecutionException e) {
                e.printStackTrace(); // FIXME proper error handling
            }
        }
    }
}
