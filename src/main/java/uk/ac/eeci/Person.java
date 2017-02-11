package uk.ac.eeci;

import java.time.Duration;
import java.time.*;

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

    private final HeterogeneousMarkovChain<Activity> markovChain;
    private final Duration timeStepSize;
    private ZonedDateTime currentTime;
    private Activity currentActivity;

    /**
     *
     * @param markovChain The {@link HeterogeneousMarkovChain} that determines follow up Person activities.
     * @param initialActivity The {@link Activity} at startup.
     * @param initialDateTime The date time at startup.
     * @param timeStepSize The time step size for the simulation. Must be consistent with the time step size
     *                     of the markov chain.
     */
    public Person(HeterogeneousMarkovChain<Activity> markovChain, Activity initialActivity,
                  ZonedDateTime initialDateTime, Duration timeStepSize) {
        this.markovChain = markovChain;
        this.currentActivity = initialActivity;
        this.currentTime = initialDateTime;
        this.timeStepSize = timeStepSize;
    }

    /**
     * Run simulation for one time step.
     *
     * Chooses new activity.
     * Updates internal time by time step.
     */
    public void step() {
        this.currentActivity = this.markovChain.move(this.currentActivity, this.currentTime);
        this.currentTime = this.currentTime.plus(this.timeStepSize);
    }

    public Activity getCurrentActivity() {
        return this.currentActivity;
    }

}
