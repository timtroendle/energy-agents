package uk.ac.eeci.test;

import org.javatuples.Pair;
import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.HeterogeneousMarkovChain;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.*;

public class TestMarkovChain
{
    private int NUMBER_EXECUTIONS = 1000;
    private long SEED = 24124123111L;

    private HeterogeneousMarkovChain.MarkovChain<State> chain;

    private enum State {
        A, B, C;
    }

    @Before
    public void setUp() {
        Map<Pair<State, State>, Double> probabilities = new HashMap<>();
        probabilities.put(new Pair<>(State.A, State.A), 0.50);
        probabilities.put(new Pair<>(State.A, State.B), 0.40);
        probabilities.put(new Pair<>(State.A, State.C), 0.10);
        probabilities.put(new Pair<>(State.B, State.B), 1.0);
        probabilities.put(new Pair<>(State.C, State.C), 1.0);
        this.chain = new HeterogeneousMarkovChain.MarkovChain<>(probabilities, SEED);
    }

    private double frequency(State from, State to) {
        List<State> chosenStates = new ArrayList<>();
        for (int i = 0; i < NUMBER_EXECUTIONS; i++) {
            chosenStates.add(this.chain.move(from));
        }
        return (double)chosenStates.stream().filter(state -> state == to).count() / (double)NUMBER_EXECUTIONS;
    }

    @Test
    public void testStaysInStateA() {
        double frequency = this.frequency(State.A, State.A);
        assertThat(frequency, is(both(greaterThan(0.45)).and(lessThan(0.55))));
    }

    @Test
    public void testStaysInStateB() {
        double frequency = this.frequency(State.B, State.B);
        assertThat(frequency, is(equalTo(1.0)));
    }

    @Test
    public void testStaysInStateC() {
        double frequency = this.frequency(State.C, State.C);
        assertThat(frequency, is(equalTo(1.0)));
    }

    @Test
    public void testChangesToStateB() {
        double frequency = this.frequency(State.A, State.B);
        assertThat(frequency, is(both(greaterThan(0.35)).and(lessThan(0.45))));
    }

    @Test
    public void testChangesToStateC() {
        double frequency = this.frequency(State.A, State.C);
        assertThat(frequency, is(both(greaterThan(0.05)).and(lessThan(0.15))));
    }

    @Test
    public void testIsDeterministic() {
        List<State> chosenStates = new ArrayList<>();
        for (int i = 0; i < NUMBER_EXECUTIONS; i++) {
            Map<Pair<State, State>, Double> probabilities = new HashMap<>();
            probabilities.put(new Pair<>(State.A, State.A), 0.75);
            probabilities.put(new Pair<>(State.A, State.B), 0.25);
            this.chain = new HeterogeneousMarkovChain.MarkovChain<>(probabilities, SEED);
            chosenStates.add(this.chain.move(State.A));
        }

        assertThat(chosenStates.stream().distinct().count(), is(equalTo(1L)));
    }

    @Test(expected=AssertionError.class)
    public void testFailsWithTotalProbabilityViolated() {
        Map<Pair<State, State>, Double> probabilities = new HashMap<>();
        probabilities.put(new Pair<>(State.A, State.A), 0.75);
        probabilities.put(new Pair<>(State.A, State.B), 0.05);
        this.chain = new HeterogeneousMarkovChain.MarkovChain<>(probabilities, SEED);
    }
}
