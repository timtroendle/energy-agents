package uk.ac.eeci.test;

import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.HeterogeneousMarkovChain;
import uk.ac.eeci.MarkovChainReader;
import uk.ac.eeci.Person;

import java.io.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static uk.ac.eeci.Person.Activity.NOT_AT_HOME;

public class TestMarkovFromCSV {

    private final static int NUMBER_EXECUTIONS = 2000; // FIXME the seed mechanism doesnt work as expected
    private final static long SEED = 123456789L;
    private final static ZoneId TIME_ZONE = ZoneId.of("Europe/London");
    private final static Duration TIME_STEP_SIZE = Duration.ofMinutes(10);
    private final static ZonedDateTime MIDNIGHT_WEEKEND = ZonedDateTime.of(2017, 2, 11, 0, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime MIDNIGHT_WEEKDAY = ZonedDateTime.of(2017, 2, 13, 0, 0, 0, 0, TIME_ZONE);
    private HeterogeneousMarkovChain<Person.Activity> markovChain;

    @Before
    public void setUp() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test-markov-chain.csv");
        Reader in = new InputStreamReader(is);
        this.markovChain = MarkovChainReader.readMarkovChainFromFile(in, TIME_STEP_SIZE, SEED, TIME_ZONE);
    }

    private double frequency(Person.Activity from, ZonedDateTime dateTime, Person.Activity to) {
        List<Person.Activity> chosenStates = new ArrayList<>();
        for (int i = 0; i < NUMBER_EXECUTIONS; i++) {
            chosenStates.add(this.markovChain.move(from, dateTime));
        }
        return (double)chosenStates.stream().filter(state -> state == to).count() / (double)NUMBER_EXECUTIONS;
    }

    @Test
    public void testCorrectProbabilitiesOnWeekday() {
        double frequency = this.frequency(NOT_AT_HOME, MIDNIGHT_WEEKDAY, NOT_AT_HOME);
        assertThat(frequency, is(both(greaterThan(0.85)).and(lessThan(0.90))));
    }

    @Test
    public void testCorrectProbabilitiesOnWeekend() {
        double frequency = this.frequency(NOT_AT_HOME, MIDNIGHT_WEEKEND, NOT_AT_HOME);
        assertThat(frequency, is(both(greaterThan(0.80)).and(lessThan(0.85))));
    }
}
