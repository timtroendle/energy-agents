package uk.ac.eeci.test.strategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.PersonReference;
import uk.ac.eeci.Person.Activity;
import uk.ac.eeci.strategy.ClimateChangingControlStrategy;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.eeci.test.utils.Utils.cartesian;

@RunWith(Parameterized.class)
public class TestClimateChangingControlStrategy {

    private final static double HEATING_SET_POINT = 26.0;
    private final static double EPSILON = 0.0001;
    private final static ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
    private final static ZonedDateTime MONDAY_AM = ZonedDateTime.of(2017, 3, 13, 10, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime TUESDAY_PM = ZonedDateTime.of(2017, 3, 14, 16, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SATURDAY_AM = ZonedDateTime.of(2017, 3, 11, 9, 0, 0, 0, TIME_ZONE);
    private final static ZonedDateTime SUNDAY_PM = ZonedDateTime.of(2017, 3, 12, 15, 0, 0, 0, TIME_ZONE);

    private HeatingControlStrategy strategy;
    private PersonReference person1 = mock(PersonReference.class);
    private PersonReference person2 = mock(PersonReference.class);
    private Set<PersonReference> people;


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[] activities = Activity.values();
        Object[] timeStamps = new Object[]{MONDAY_AM, TUESDAY_PM, SATURDAY_AM, SUNDAY_PM};
        return cartesian(activities, activities, timeStamps);
    }

    @Parameterized.Parameter
    public Activity activityPerson1;

    @Parameterized.Parameter(1)
    public Activity activityPerson2;

    @Parameterized.Parameter(2)
    public ZonedDateTime timeStamp;


    @Before
    public void setUp() {
        this.strategy = new ClimateChangingControlStrategy(HEATING_SET_POINT);
        this.people = new HashSet<>();
        this.people.add(person1);
        this.people.add(person2);
        CompletableFuture<Activity> activityPerson1 = new CompletableFuture<>();
        CompletableFuture<Activity> activityPerson2 = new CompletableFuture<>();
        activityPerson1.complete(this.activityPerson1);
        activityPerson2.complete(this.activityPerson2);
        when(this.person1.getCurrentActivity()).thenReturn(activityPerson1);
        when(this.person2.getCurrentActivity()).thenReturn(activityPerson2);
    }

    @Test
    public void returnsConstantHeatingSetPointWhenEmpty() {
        double heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, new HashSet<>()).get();
        assertThat(heatingSetPoint, is(closeTo(HEATING_SET_POINT, EPSILON)));
    }

    @Test
    public void returnsConstantHeatingSetPointNoMatterTheActivity() {
        double heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, this.people).get();
        assertThat(heatingSetPoint, is(closeTo(HEATING_SET_POINT, EPSILON)));
    }

}
