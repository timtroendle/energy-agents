package uk.ac.eeci.test.strategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.eeci.HeatingControlStrategy;
import static uk.ac.eeci.Person.Activity.*;
import uk.ac.eeci.PersonReference;
import uk.ac.eeci.strategy.PresenceBasedStrategy;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.eeci.test.utils.Utils.cartesian;

@RunWith(Parameterized.class)
public class TestPresenceBasedStrategy {

    private final static double SET_POINT_WHILE_ACTIVE_AT_HOME = 26;
    private final static double SET_POINT_WHILE_SLEEPING_AT_HOME = 18;
    private final static ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
    private final static ZonedDateTime MONDAY_AM = ZonedDateTime.of(2017, 3, 13, 10, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime TUESDAY_PM = ZonedDateTime.of(2017, 3, 14, 16, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SATURDAY_AM = ZonedDateTime.of(2017, 3, 11, 9, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SUNDAY_PM = ZonedDateTime.of(2017, 3, 12, 15, 0, 0, 0, TIME_ZONE);

    HeatingControlStrategy strategy;
    PersonReference person1 = mock(PersonReference.class);
    PersonReference person2 = mock(PersonReference.class);
    Set<PersonReference> people;


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[] timeStamps = new Object[]{MONDAY_AM, TUESDAY_PM, SATURDAY_AM, SUNDAY_PM};
        return cartesian(timeStamps);
    }

    @Parameterized.Parameter(0)
    public ZonedDateTime timeStamp;

    @Before
    public void setUp() {
        this.people = new HashSet<>();
        this.people.add(person1);
        this.people.add(person2);
        this.strategy = new PresenceBasedStrategy(SET_POINT_WHILE_ACTIVE_AT_HOME, SET_POINT_WHILE_SLEEPING_AT_HOME);
    }

    @Test
    public void noSetPointWhenNoOneAtHome() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(this.timeStamp, new HashSet<>()).get().isPresent(), is(equalTo(false)));
    }

    @Test(expected=IllegalStateException.class)
    public void raisesErrorWithPeopleAtHomeThatAreNotAtHome() throws Throwable {
        when(this.person1.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(NOT_AT_HOME));
        when(this.person2.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(HOME));
        try {
            this.strategy.heatingSetPoint(this.timeStamp, this.people).join();
        }
        catch (CompletionException ce) {
            throw ce.getCause();
        }
    }

    @Test
    public void activeSetPointWhenOnePersonHomeAndActive() throws ExecutionException, InterruptedException {
        when(this.person1.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(HOME));
        this.people.remove(this.person2);
        assertThat(this.strategy.heatingSetPoint(this.timeStamp, this.people).get().get(),
                is(equalTo(SET_POINT_WHILE_ACTIVE_AT_HOME)));
    }

    @Test
    public void activeSetPointWhenTwoPersonHome() throws ExecutionException, InterruptedException {
        when(this.person1.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(HOME));
        when(this.person2.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(HOME));
        assertThat(this.strategy.heatingSetPoint(this.timeStamp, this.people).get().get(),
                is(equalTo(SET_POINT_WHILE_ACTIVE_AT_HOME)));
    }

    @Test
    public void activeSetPointWhenOneForeignPersonHome() throws ExecutionException, InterruptedException {
        when(this.person1.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(OTHER_HOME));
        this.people.remove(this.person2);
        assertThat(this.strategy.heatingSetPoint(this.timeStamp, this.people).get().get(),
                is(equalTo(SET_POINT_WHILE_ACTIVE_AT_HOME)));
    }

    @Test
    public void activeSetPointEvenWhenSomePersonSleep() throws ExecutionException, InterruptedException {
        when(this.person1.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(SLEEP_AT_HOME));
        when(this.person2.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(HOME));
        assertThat(this.strategy.heatingSetPoint(this.timeStamp, this.people).get().get(),
                is(equalTo(SET_POINT_WHILE_ACTIVE_AT_HOME)));
    }

    @Test
    public void sleepSetPointWhenAllPersonSleep() throws ExecutionException, InterruptedException {
        when(this.person1.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(SLEEP_AT_HOME));
        when(this.person2.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(SLEEP_AT_HOME));
        assertThat(this.strategy.heatingSetPoint(this.timeStamp, this.people).get().get(),
                is(equalTo(SET_POINT_WHILE_SLEEPING_AT_HOME)));
    }

    @Test
    public void sleepSetPointWhenAllPersonIncludingForeignSleep() throws ExecutionException, InterruptedException {
        when(this.person1.getCurrentActivity()).thenReturn(CompletableFuture.completedFuture(SLEEP_AT_OTHER_HOME));
        this.people.remove(this.person2);
        assertThat(this.strategy.heatingSetPoint(this.timeStamp, this.people).get().get(),
                is(equalTo(SET_POINT_WHILE_SLEEPING_AT_HOME)));
    }

}
