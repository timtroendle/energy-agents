package uk.ac.eeci;

import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeterogeneousMarkovChain {


    public static class MarkovChain<T> {

        Map<Pair<T, T>, Double> probabilities;
        Random randomNumberGenerator;

        public MarkovChain(Map<Pair<T, T>, Double> probabilities, long seed) {
            this.probabilities = probabilities;
            this.randomNumberGenerator = new Random(seed);
            this.validateChain();
        }

        public T move(T currentState) {
            List<Pair<T, T>> possibleTransitions = this.possibleTransitions(currentState)
                .collect(Collectors.toList());
            double randomNumber = this.randomNumberGenerator.nextDouble();
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
                        .mapToDouble(statePair -> this.probabilities.get(statePair))
                        .sum();
                assert Math.abs(summedProbabilities - 1.0) < 0.001;
            }
        }
    }
}
