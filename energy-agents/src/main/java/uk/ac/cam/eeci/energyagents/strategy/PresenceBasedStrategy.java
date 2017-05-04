package uk.ac.cam.eeci.energyagents.strategy;

import uk.ac.cam.eeci.energyagents.Person;
import uk.ac.cam.eeci.energyagents.PersonReference;
import uk.ac.cam.eeci.energyagents.HeatingControlStrategy;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PresenceBasedStrategy extends HeatingControlStrategy {

    private final double setPointWhileActiveAtHome;
    private final double setPointWhileSleepingAtHome;

    public PresenceBasedStrategy(double setPointWhileActiveAtHome, double setPointWhileSleepingAtHome) {
        this.setPointWhileActiveAtHome = setPointWhileActiveAtHome;
        this.setPointWhileSleepingAtHome = setPointWhileSleepingAtHome;
    }

    @Override
    public CompletableFuture<Optional<Double>> heatingSetPoint(ZonedDateTime timeStamp, Set<PersonReference> peopleInDwelling) {

        Map<PersonReference, Person.Activity> activities = new ConcurrentHashMap<>();
        CompletableFuture<Void>[] updates = new CompletableFuture[peopleInDwelling.size()];
        int i = 0;
        for (PersonReference person : peopleInDwelling) {
            updates[i] = person.getCurrentActivity()
                    .thenAccept(activity -> activities.put(person, activity));
            i++;
        }
        return CompletableFuture.allOf(updates)
                .thenApply((a) -> this.determineSetPoint(activities.values()));
    }

    private Optional<Double> determineSetPoint(Collection<Person.Activity> activities) {
        boolean someOneNotAtHomeWhileBeingHome = activities.stream()
                .anyMatch((act) -> !Person.HOME_ACTIVITIES.contains(act));
        if  (someOneNotAtHomeWhileBeingHome) {
            String msg = "At least one person was at a dwelling while being in a non-dwelling markov chain state. " +
                    "This should never happen.";
            throw new IllegalStateException(msg);
        }
        boolean someOneHome = !activities.isEmpty();
        boolean allAsleep = activities.stream().allMatch(Person.SLEEP_ACTIVITIES::contains);
        if (someOneHome && allAsleep)
            return Optional.of(this.setPointWhileSleepingAtHome);
        else if (someOneHome)
            return Optional.of(this.setPointWhileActiveAtHome);
        else
            return Optional.empty();
    }
}
