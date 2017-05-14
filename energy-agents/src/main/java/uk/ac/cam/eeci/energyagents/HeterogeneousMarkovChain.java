package uk.ac.cam.eeci.energyagents;

import org.javatuples.Pair;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A time heterogeneous Markov chain for with different probabilities for one week.
 *
 * @param <T> The type of the states of the Markov chain.
 */
public class HeterogeneousMarkovChain<T> {

    private final Map<LocalTime, MarkovChain<T>> weekdayChain;
    private final Map<LocalTime, MarkovChain<T>> weekendChain;
    private final ZoneId timeZone;

    /**
     *
     * @param weekdayChain markov chain for each time of the weekday
     * @param weekendChain markov chains for each time of the weekend day
     * @param timeZone the time zone
     */
    public HeterogeneousMarkovChain(Map<LocalTime, MarkovChain<T>> weekdayChain,
                                    Map<LocalTime, MarkovChain<T>> weekendChain,
                                    ZoneId timeZone) {
        this.weekdayChain = weekdayChain;
        this.weekendChain = weekendChain;
        this.timeZone = timeZone;
    }

    /**
     * Move to the next Markov state.
     *
     * @param currentState The current state of the Markov chain.
     * @param dateTime the current time
     * @param randomNumberGenerator an object that returns a random number between 0 and 1
     * @return the next state of the Markov chain
     */
    public T move(T currentState, ZonedDateTime dateTime, Random randomNumberGenerator) {
        Map<LocalTime, MarkovChain<T>> dayChain = null;
        switch(dateTime.getDayOfWeek()) {
            case MONDAY:
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
            case FRIDAY:
                dayChain = this.weekdayChain;
                break;
            case SATURDAY:
            case SUNDAY:
                dayChain = this.weekendChain;
                break;
        }
        T nextState = null;
        try {
            nextState = dayChain.get(dateTime.withZoneSameInstant(this.timeZone).toLocalTime())
                    .move(currentState, randomNumberGenerator);
        } catch (NullPointerException npe) {
            String msg = String.format("%s is not a valid date time for this markov chain.", dateTime);
            throw new IllegalArgumentException(msg);
        }
        return nextState;
    }


    /**
     * A time invariant first order Markov chain.
     * @param <T> The type of the states.
     */
    public static class MarkovChain<T> {

        private final Map<Pair<T, T>, Double> probabilities;

        /**
         *
         * @param probabilities transition probabilities between states
         */
        public MarkovChain(Map<Pair<T, T>, Double> probabilities) {
            this.probabilities = probabilities;
            this.validateChain();
        }

        /**
         * Move to the next state.
         *
         * @param currentState the current state of the Markov chain
         * @param randomNumberGenerator an object that returns a random number between 0 and 1
         * @return the next state of the Markov chain
         */
        public T move(T currentState, Random randomNumberGenerator) {
            List<Pair<T, T>> possibleTransitions = this.possibleTransitions(currentState)
                .collect(Collectors.toList());
            double randomNumber = randomNumberGenerator.nextDouble();
            T nextState = null;
            double summedProbabilities = 0;
            for (Pair<T, T> statePair : possibleTransitions) {
                double thisProbability = this.probabilities.get(statePair);
                if (randomNumber < summedProbabilities + thisProbability) {
                    nextState = statePair.getValue1();
                    break;
                }
                else {
                    summedProbabilities += thisProbability;
                }
            }
            if (nextState == null) {
                throw new IllegalStateException("Could not determine next state. Markov chain is invalid.");
            }
            return nextState;
        }

        private Stream<Pair<T, T>> possibleTransitions(T fromState) {
            return probabilities.keySet().stream().filter(statePair -> statePair.getValue0() == fromState);
        }

        private void validateChain() {
            Set<T> startStates = new HashSet<>();
            for (Pair<T, T> statePair : this.probabilities.keySet()) {
                startStates.add(statePair.getValue0());
            }
            for (T startState : startStates) {
                double summedProbabilities = this.possibleTransitions(startState)
                        .mapToDouble(this.probabilities::get)
                        .sum();
                assert Math.abs(summedProbabilities - 1.0) < 0.001;
            }
        }
    }
}
