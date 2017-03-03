package uk.ac.eeci;

import java.time.*;
import java.util.HashSet;
import java.util.Set;

/**
 * The model of a citizen making choices on activities and locations.
 */
public class Person {

    /**
     * Activities of citizens.
     */
    public enum Activity {
        HOME, SLEEP_AT_HOME, OTHER_HOME, SLEEP_AT_OTHER_HOME, NOT_AT_HOME;
    }

    private final static Set<Activity> HOME_ACTIVITIES;

    static {
        HOME_ACTIVITIES = new HashSet<>();
        HOME_ACTIVITIES.add(Activity.HOME);
        HOME_ACTIVITIES.add(Activity.SLEEP_AT_HOME);
    }

    private final HeterogeneousMarkovChain<Activity> markovChain;
    private final Duration timeStepSize;
    private PersonReference reference;
    private final DwellingReference home;
    private ZonedDateTime currentTime;
    private Activity currentActivity;
    private boolean atHome;


    /**
     *
     * @param markovChain The {@link HeterogeneousMarkovChain} that determines follow up Person activities.
     * @param initialActivity The {@link Activity} at startup.
     * @param initialDateTime The date time at startup.
     * @param timeStepSize The time step size for the simulation. Must be consistent with the time step size
     *                     of the markov chain.
     * @param home A {@link DwellingReference} to this person's home.
     */
    public Person(HeterogeneousMarkovChain<Activity> markovChain, Activity initialActivity,
                  ZonedDateTime initialDateTime, Duration timeStepSize, DwellingReference home) {
        this.markovChain = markovChain;
        this.currentActivity = initialActivity;
        this.currentTime = initialDateTime;
        this.timeStepSize = timeStepSize;
        this.reference = new PersonReference(this);
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
        this.currentActivity = this.markovChain.move(this.currentActivity, this.currentTime);
        this.updateLocation();
        this.currentTime = this.currentTime.plus(this.timeStepSize);
    }

    public Activity getCurrentActivity() {
        return this.currentActivity;
    }

    private void updateLocation() {
        if (this.atHome && !HOME_ACTIVITIES.contains(this.currentActivity)) {
            this.atHome = false;
            this.home.leave(this.reference);
        } else if (!this.atHome && HOME_ACTIVITIES.contains(this.currentActivity)) {
            this.atHome = true;
            this.home.enter(this.reference);
        }

    }

}
