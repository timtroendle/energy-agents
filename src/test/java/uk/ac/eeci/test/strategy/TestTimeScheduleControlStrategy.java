package uk.ac.eeci.test.strategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.Person;
import uk.ac.eeci.PersonReference;
import uk.ac.eeci.strategy.TimeScheduleControlStrategy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.eeci.strategy.TimeScheduleControlStrategy.DayType.*;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static uk.ac.eeci.test.utils.Utils.cartesian;

@RunWith(Parameterized.class)
public class TestTimeScheduleControlStrategy {

    private final static ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
    private final static ZonedDateTime MONDAY_AM = ZonedDateTime.of(2017, 3, 13, 10, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime MONDAY_NOON = ZonedDateTime.of(2017, 3, 13, 12, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime MONDAY_PM = ZonedDateTime.of(2017, 3, 13, 16, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SATURDAY_AM = ZonedDateTime.of(2017, 3, 11, 9, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SATURDAY_PM = ZonedDateTime.of(2017, 3, 11, 15, 0, 0, 0, TIME_ZONE);
    private final static double WEEKDAY_AM_SET_POINT = 6.0;
    private final static double WEEKDAY_PM_SET_POINT = 12.0;
    private final static double WEEKEND_SET_POINT = 18.0;

    private HeatingControlStrategy strategy;
    private PersonReference person1 = mock(PersonReference.class);
    private PersonReference person2 = mock(PersonReference.class);
    private Set<PersonReference> people;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[] activities = Person.Activity.values();
        return cartesian(activities, activities);
    }

    @Parameterized.Parameter
    public Person.Activity activityPerson1;

    @Parameterized.Parameter(1)
    public Person.Activity activityPerson2;

    @Before
    public void setUp() {
        this.setUpPeopleAndTheirActivities();

        List<TimeScheduleControlStrategy.TimeSlot> timeSlots = new ArrayList<>();

        timeSlots.add(new TimeScheduleControlStrategy.TimeSlot(
                WEEKDAY, LocalTime.MIDNIGHT, LocalTime.of(11, 0), WEEKDAY_AM_SET_POINT
        ));
        timeSlots.add(new TimeScheduleControlStrategy.TimeSlot(
                WEEKDAY, LocalTime.of(13, 0), LocalTime.MIDNIGHT, WEEKDAY_PM_SET_POINT
        ));
        timeSlots.add(new TimeScheduleControlStrategy.TimeSlot(
                WEEKEND, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, WEEKEND_SET_POINT
        ));
        this.strategy = new TimeScheduleControlStrategy(timeSlots, TIME_ZONE);
    }

    private void setUpPeopleAndTheirActivities() {
        this.people = new HashSet<>();
        this.people.add(person1);
        this.people.add(person2);
        CompletableFuture<Person.Activity> activityPerson1 = CompletableFuture.completedFuture(this.activityPerson1);
        CompletableFuture<Person.Activity> activityPerson2 = CompletableFuture.completedFuture(this.activityPerson2);
        when(this.person1.getCurrentActivity()).thenReturn(activityPerson1);
        when(this.person2.getCurrentActivity()).thenReturn(activityPerson2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidTimeSlotsAreRejected() {
        new TimeScheduleControlStrategy.TimeSlot(
                WEEKDAY, LocalTime.NOON, LocalTime.NOON.minusHours(1), WEEKDAY_PM_SET_POINT
        );
    }

    @Test(expected=IllegalArgumentException.class)
    public void overlappingTimeSlotsAreRejected() {
        List<TimeScheduleControlStrategy.TimeSlot> timeSlots = new ArrayList<>();
        timeSlots.add(new TimeScheduleControlStrategy.TimeSlot(
                WEEKDAY, LocalTime.MIDNIGHT, LocalTime.NOON, WEEKDAY_AM_SET_POINT
        ));
        timeSlots.add(new TimeScheduleControlStrategy.TimeSlot(
                WEEKDAY, LocalTime.of(11, 0), LocalTime.MIDNIGHT, WEEKDAY_PM_SET_POINT
        ));
        this.strategy = new TimeScheduleControlStrategy(timeSlots, TIME_ZONE);
    }

    @Test
    public void demandsSwitchOffWithoutTimeSlots() throws ExecutionException, InterruptedException {
        this.strategy = new TimeScheduleControlStrategy(new ArrayList<>(), TIME_ZONE);
        assertThat(this.strategy.heatingSetPoint(MONDAY_AM, this.people).get(), is(equalTo(Optional.empty())));
    }

    @Test
    public void setPointMondayAM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(MONDAY_AM, this.people).get(),
                is(equalTo(Optional.of(WEEKDAY_AM_SET_POINT))));
    }

    @Test
    public void noSetPointMondayNoon() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(MONDAY_NOON, this.people).get(), is(equalTo(Optional.empty())));
    }

    @Test
    public void setPointMondayPM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(MONDAY_PM, this.people).get(),
                is(equalTo(Optional.of(WEEKDAY_PM_SET_POINT))));
    }

    @Test
    public void setPointWednesdayAM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(MONDAY_AM.plus(Duration.ofDays(2)), this.people).get(),
                is(equalTo(Optional.of(WEEKDAY_AM_SET_POINT))));
    }

    @Test
    public void setPointThursdayPM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(MONDAY_PM.plus(Duration.ofDays(3)), this.people).get(),
                is(equalTo(Optional.of(WEEKDAY_PM_SET_POINT))));
    }

    @Test
    public void noSetPointFridayNoon() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(MONDAY_NOON.plus(Duration.ofDays(4)), this.people).get(),
                is(equalTo(Optional.empty())));
    }

    @Test
    public void setPointSaturdayAM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(SATURDAY_AM, this.people).get(),
                is(equalTo(Optional.of(WEEKEND_SET_POINT))));
    }

    @Test
    public void setPointSaturdayPM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(SATURDAY_PM, this.people).get(),
                is(equalTo(Optional.of(WEEKEND_SET_POINT))));
    }

    @Test
    public void setPointSundayAM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(SATURDAY_AM.plus(Duration.ofDays(1)), this.people).get(),
                is(equalTo(Optional.of(WEEKEND_SET_POINT))));
    }

    @Test
    public void setPointSundayPM() throws ExecutionException, InterruptedException {
        assertThat(this.strategy.heatingSetPoint(SATURDAY_PM.plus(Duration.ofDays(1)), this.people).get(),
                is(equalTo(Optional.of(WEEKEND_SET_POINT))));
    }
}
