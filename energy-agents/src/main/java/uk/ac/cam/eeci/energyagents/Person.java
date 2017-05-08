package uk.ac.cam.eeci.energyagents;

import java.time.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * The model of a citizen making choices on activities and locations.
 */
public class Person {

    /**
     * Activities of citizens.
     */
    public enum Activity {
        HOME, SLEEP_AT_HOME, NOT_AT_HOME
    }

    public final static Set<Activity> HOME_ACTIVITIES;
    public final static Set<Activity> SLEEP_ACTIVITIES;
    private final static Set<Activity> OWN_HOME_ACTIVITIES;

    static {
        OWN_HOME_ACTIVITIES = new HashSet<>();
        OWN_HOME_ACTIVITIES.add(Activity.HOME);
        OWN_HOME_ACTIVITIES.add(Activity.SLEEP_AT_HOME);
        SLEEP_ACTIVITIES = new HashSet<>();
        SLEEP_ACTIVITIES.add(Activity.SLEEP_AT_HOME);
        HOME_ACTIVITIES = new HashSet<>(OWN_HOME_ACTIVITIES);
    }

    private final HeterogeneousMarkovChain<Activity> markovChain;
    private final double activeMetabolicRate;
    private final double passiveMetabolicRate;
    private final Random randomNumberGenerator;
    private final Duration timeStepSize;
    private PersonReference reference;
    private final DwellingReference home;
    private ZonedDateTime currentTime;
    private Activity currentActivity;
    private boolean atHome;

    /**
     *
     * @param markovChain The {@link HeterogeneousMarkovChain} that determines follow up Person activities.
     * @param activeMetabolicRate The metabolic rate while active [W].
     * @param passiveMetabolicRate The metabolic rate while asleep [W].
     * @param initialActivity The {@link Activity} at startup.
     * @param initialDateTime The date time at startup.
     * @param timeStepSize The time step size for the simulation. Must be consistent with the time step size
     *                     of the markov chain.
     * @param home A {@link DwellingReference} to this person's home.
     * @param randomNumberGenerator A {@link Random} instance that creates random numbers for this person.
     *                              Important for reproducibility of results.
     */
    public Person(HeterogeneousMarkovChain<Activity> markovChain, double activeMetabolicRate, double passiveMetabolicRate,
                  Activity initialActivity, ZonedDateTime initialDateTime, Duration timeStepSize,
                  DwellingReference home, Random randomNumberGenerator) {
        this.markovChain = markovChain;
        this.activeMetabolicRate = activeMetabolicRate;
        this.passiveMetabolicRate = passiveMetabolicRate;
        this.currentActivity = initialActivity;
        this.currentTime = initialDateTime;
        this.timeStepSize = timeStepSize;
        this.reference = new PersonReference(this);
        this.randomNumberGenerator = randomNumberGenerator;
        this.home = home;
        this.atHome = false;
        this.updateLocation();
    }

    /**
     * Run simulation for one time step.
     *
     * Chooses new activity.
     * Updates internal time by time step.
     */
    public void step() {
        this.currentActivity = this.markovChain.move(this.currentActivity, this.currentTime,
                this.randomNumberGenerator);
        this.updateLocation();
        this.currentTime = this.currentTime.plus(this.timeStepSize);
    }

    public Activity getCurrentActivity() {
        return this.currentActivity;
    }

    public double getCurrentMetabolicRate() {
        if (SLEEP_ACTIVITIES.contains(this.currentActivity)) {
            return this.passiveMetabolicRate;
        } else {
            return this.activeMetabolicRate;
        }
    }

    private void updateLocation() {
        if (this.atHome && !OWN_HOME_ACTIVITIES.contains(this.currentActivity)) {
            this.atHome = false;
            this.home.leave(this.reference);
        } else if (!this.atHome && OWN_HOME_ACTIVITIES.contains(this.currentActivity)) {
            this.atHome = true;
            this.home.enter(this.reference);
        }

    }

}
