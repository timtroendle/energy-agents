package uk.ac.eeci;

import org.junit.Before;

import java.time.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import uk.ac.eeci.Person.Activity;

public class TestPerson {

    private static final Duration TIME_STEP_SIZE = Duration.ofMinutes(10);
    private static final Activity INITIAL_ACTIVITY = Activity.HOME;
    private static final OffsetDateTime INITIAL_DATETIME = OffsetDateTime.of(2017, 02, 11, 16, 20, 0, 0, ZoneOffset.UTC);
    private HeterogeneousMarkovChain<Activity> markovChain = mock(HeterogeneousMarkovChain.class);
    private Person person;

    @Before
    public void setUp() {
        this.person = new Person(this.markovChain, INITIAL_ACTIVITY, INITIAL_DATETIME, TIME_STEP_SIZE);
    }

    @Test
    public void testStartsUpwithInitialActivity() {
        assertThat(person.getCurrentActivity(), is(equalTo(INITIAL_ACTIVITY)));
    }

    @Test
    public void testUpdatesStateAccordingToMarkovChainDuringStep() {
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.SLEEP_AT_OTHER_HOME);
        person.step();
        assertThat(person.getCurrentActivity(), is(equalTo(Activity.SLEEP_AT_OTHER_HOME)));
    }

    @Test
    public void testUpdatesTimeDuringStep() {
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.OTHER_HOME);
        person.step();
        reset(this.markovChain);
        person.step();
        verify(this.markovChain).move(Activity.OTHER_HOME, INITIAL_DATETIME.plus(TIME_STEP_SIZE));
    }
}
