package uk.ac.eeci.strategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.eeci.HeatingControlStrategy;
import uk.ac.eeci.PersonReference;
import uk.ac.eeci.Person.Activity;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TestClimateChangingControlStrategy {

    private final static double HEATING_SET_POINT = 26.0;
    private final static double COOLING_SET_POINT = 20.0;
    private final static double EPSILON = 0.0001;

    private HeatingControlStrategy strategy;
    private PersonReference person1 = mock(PersonReference.class);
    private PersonReference person2 = mock(PersonReference.class);
    private Set<PersonReference> people;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> activityCrossProduct;
        activityCrossProduct = new ArrayList<>();

        Activity[] act = Activity.values();

        Consumer<Activity> consumer = (Activity a1) -> activityCrossProduct.addAll(Arrays.stream(act)
                .map(a2 -> new Activity[]{a1, a2})
                .collect(Collectors.toList()));
        Arrays.stream(act).forEach(consumer);
        return activityCrossProduct;
    }

    @Parameterized.Parameter
    public Activity activityPerson1;

    @Parameterized.Parameter(1)
    public Activity activityPerson2;


    @Before
    public void setUp() {
        this.strategy = new ClimateChangingControlStrategy(HEATING_SET_POINT, COOLING_SET_POINT);
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
        double heatingSetPoint = this.strategy.heatingSetPoint(new HashSet<>());
        assertThat(heatingSetPoint, is(closeTo(HEATING_SET_POINT, EPSILON)));
    }

    @Test
    public void returnsConstantCoolingSetPointWhenEmpty() {
        double coolingSetPoint = this.strategy.coolingSetPoint(new HashSet<>());
        assertThat(coolingSetPoint, is(closeTo(COOLING_SET_POINT, EPSILON)));
    }

    @Test
    public void returnsConstantHeatingSetPointNoMatterTheActivity() {
        double heatingSetPoint = this.strategy.heatingSetPoint(this.people);
        assertThat(heatingSetPoint, is(closeTo(HEATING_SET_POINT, EPSILON)));
    }

    @Test
    public void returnsConstantCoolingSetPointNoMatterTheActivity() {
        double coolingSetPoint = this.strategy.coolingSetPoint(this.people);
        assertThat(coolingSetPoint, is(closeTo(COOLING_SET_POINT, EPSILON)));
    }

}
