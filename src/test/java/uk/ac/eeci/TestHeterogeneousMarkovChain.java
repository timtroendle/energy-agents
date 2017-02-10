package uk.ac.eeci;

import org.javatuples.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.*;
import java.lang.IllegalArgumentException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.*;

@RunWith(Parameterized.class)
public class TestHeterogeneousMarkovChain {

    private int NUMBER_EXECUTIONS = 1000;
    private long SEED = 24124123111L;

    private HeterogeneousMarkovChain<State> chain;

    private enum State {
        A, B, C;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { State.A }, { State.B }, { State.C }
        });
    }

    @Parameterized.Parameter
    public State startState;

    @Before
    public void setUp() {
        Map<Pair<State, State>, Double> workTimeProbabilities = new HashMap<>();
        workTimeProbabilities.put(new Pair<>(State.A, State.A), 1.0);
        workTimeProbabilities.put(new Pair<>(State.B, State.B), 1.0);
        workTimeProbabilities.put(new Pair<>(State.C, State.C), 1.0);
        Map<Pair<State, State>, Double> spareTimeProbabilities = new HashMap<>();
        spareTimeProbabilities.put(new Pair<>(State.A, State.B), 1.0);
        spareTimeProbabilities.put(new Pair<>(State.B, State.A), 1.0);
        spareTimeProbabilities.put(new Pair<>(State.C, State.B), 1.0);
        Map<OffsetTime, HeterogeneousMarkovChain.MarkovChain<State>> weekdayChain = new HashMap<>();
        Map<OffsetTime, HeterogeneousMarkovChain.MarkovChain<State>> weekendChain = new HashMap<>();
        LocalDate date = LocalDate.of(2017, 1, 1); // arbitrary date
        LocalDateTime tsp = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        do {
            if (tsp.getHour() >= 9 && tsp.getHour() < 17) {
                weekdayChain.put(tsp.toLocalTime().atOffset(ZoneOffset.UTC), new HeterogeneousMarkovChain.MarkovChain<>(workTimeProbabilities, SEED));
            } else {
                weekdayChain.put(tsp.toLocalTime().atOffset(ZoneOffset.UTC), new HeterogeneousMarkovChain.MarkovChain<>(spareTimeProbabilities, SEED));
            }
            weekendChain.put(tsp.toLocalTime().atOffset(ZoneOffset.UTC), new HeterogeneousMarkovChain.MarkovChain<>(spareTimeProbabilities, SEED));
            tsp = tsp.plus(Duration.ofMinutes(10));
        } while (date.equals(tsp.toLocalDate()));
        this.chain = new HeterogeneousMarkovChain<>(weekdayChain, weekendChain);
    }

    private double frequency(State from, OffsetDateTime dateTime, State to) {
        List<State> chosenStates = new ArrayList<>();
        for (int i = 0; i < NUMBER_EXECUTIONS; i++) {
            chosenStates.add(this.chain.move(from, dateTime));
        }
        return (double)chosenStates.stream().filter(state -> state == to).count() / (double)NUMBER_EXECUTIONS;
    }

    @Test
    public void testDoesNotChangeStateDuringWorktime() {
        double frequency = this.frequency(this.startState, OffsetDateTime.of(2017, 02, 10, 15, 20, 0, 0, ZoneOffset.UTC), this.startState);
        assertThat(frequency, is(equalTo(1.0)));
    }

    @Test
    public void testAlwaysChangeStateDuringEvenings() {
        double frequency = this.frequency(this.startState, OffsetDateTime.of(2017, 02, 10, 18, 40, 0, 0, ZoneOffset.UTC), this.startState);
        assertThat(frequency, is(equalTo(0.0)));
    }

    @Test
    public void testAlwaysChangeStateDuringWeekend() {
        double frequency = this.frequency(this.startState, OffsetDateTime.of(2017, 02, 11, 15, 20, 0, 0, ZoneOffset.UTC), this.startState);
        assertThat(frequency, is(equalTo(0.0)));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFailsWithInvalidTimeStamp() {
        // time stamps must be full 10 minute
        OffsetDateTime invalid = OffsetDateTime.of(2017, 02, 10, 15, 21, 0, 0, ZoneOffset.UTC);
        this.chain.move(startState, invalid);
    }
}
