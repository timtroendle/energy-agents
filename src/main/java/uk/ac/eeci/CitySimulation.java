package uk.ac.eeci;

import io.improbable.science.ISteppable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Defines an entire simulation run.
 */
public class CitySimulation implements ISteppable {

    private final Set<DwellingReference> dwellings;
    private final Set<PersonReference> people;
    private final double outdoorTemperature;

    /**
     * @param dwellings The set of all dwellings in the city.
     * @param people The set of all people in the city.
     * @param outdoorTemperature The constant outdoor temperature in the city.
     */
    public CitySimulation(Set<DwellingReference> dwellings, Set<PersonReference> people, double outdoorTemperature) {
        this.dwellings = dwellings;
        this.people = people;
        this.outdoorTemperature = outdoorTemperature;
    }

    @Override
    public CompletableFuture<Void> step() {
        List<CompletableFuture<Void>> peopleSteps = new ArrayList<>();
        for (PersonReference person : this.people) {
            peopleSteps.add(person.step());
        }
        CompletableFuture<Void>[] array = new CompletableFuture[peopleSteps.size()];
        array = peopleSteps.toArray(array);

        return CompletableFuture.allOf(array).thenCompose((unused) -> {
            List<CompletableFuture<Void>> dwellingSteps = new ArrayList<>();
            for (DwellingReference dwelling : this.dwellings) {
                dwellingSteps.add(dwelling.step(this.outdoorTemperature));
            }
            CompletableFuture<Void>[] dStepsArray = new CompletableFuture[dwellingSteps.size()];
            dStepsArray = dwellingSteps.toArray(dStepsArray);
            return CompletableFuture.allOf(dStepsArray);
        });
    }
}
