package uk.ac.eeci.test;

import org.junit.Before;

import java.time.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import uk.ac.eeci.DwellingReference;
import uk.ac.eeci.HeterogeneousMarkovChain;
import uk.ac.eeci.Person;
import uk.ac.eeci.Person.Activity;
import uk.ac.eeci.PersonReference;

public class TestPerson {

    private static final Duration TIME_STEP_SIZE = Duration.ofMinutes(10);
    private static final Activity INITIAL_ACTIVITY = Activity.NOT_AT_HOME;
    private static final ZonedDateTime INITIAL_DATETIME = ZonedDateTime.of(2017, 02, 11, 16, 20, 0, 0, ZoneOffset.UTC);
    private HeterogeneousMarkovChain<Activity> markovChain = mock(HeterogeneousMarkovChain.class);
    private DwellingReference home = mock(DwellingReference.class);
    private Person person;
    private PersonReference personReference = mock(PersonReference.class);

    @Before
    public void setUp() {
        this.person = new Person(this.markovChain, INITIAL_ACTIVITY, INITIAL_DATETIME, TIME_STEP_SIZE,
                this.personReference, home);
    }

    @Test
    public void testStartsUpWithInitialActivity() {
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

    @Test
    public void testEntersHomeWhenStartingBeingAtHome() {
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.HOME);
        person.step();
        verify(this.home).enter(this.personReference);
    }

    @Test
    public void testDoesNotEnterHomeTwice() {
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.HOME);
        person.step();
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.SLEEP_AT_HOME);
        person.step();
        verify(this.home, times(1)).enter(this.personReference);
    }

    @Test
    public void testLeavesHomeWhenStartingToNotBeThere() {
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.HOME);
        person.step();
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.NOT_AT_HOME);
        person.step();
        verify(this.home).leave(this.personReference);
    }

    @Test
    public void testDoesNotLeaveHomeTwice() {
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.HOME);
        person.step();
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.NOT_AT_HOME);
        person.step();
        when(this.markovChain.move(INITIAL_ACTIVITY, INITIAL_DATETIME)).thenReturn(Activity.OTHER_HOME);
        person.step();
        verify(this.home, times(1)).leave(this.personReference);
    }
}
