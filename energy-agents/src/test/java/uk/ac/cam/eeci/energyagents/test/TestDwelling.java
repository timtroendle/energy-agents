package uk.ac.cam.eeci.energyagents.test;

import uk.ac.cam.eeci.framework.Reference;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.eeci.energyagents.*;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

public class TestDwelling {

    private final static double EPSILON = 0.0001;
    private final static double INITIAL_DWELLING_TEMPERATURE = 22;
    private final static ZonedDateTime INITIAL_TIME = ZonedDateTime.of(2017, 3, 13, 17, 40, 0, 0, ZoneId.of("Europe/Paris"));
    private final static Duration TIME_STEP_SIZE = Duration.ofHours(1);
    private Dwelling dwelling;
    private DwellingReference dwellingReference;
    private HeatingControlStrategyReference controlStrategy = mock(HeatingControlStrategyReference.class);
    private EnvironmentReference environment = mock(EnvironmentReference.class);
    private PersonReference person = mock(PersonReference.class);
    private Set<PersonReference> personInSet;

    @Before
    public void setUp() {
        this.personInSet = new HashSet<>();
        this.personInSet.add(this.person);
        when(this.controlStrategy.heatingSetPoint(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(21.9)));
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE));
        when(this.person.getCurrentMetabolicRate())
                .thenReturn(CompletableFuture.completedFuture(2.0));
        double conditionedFloorArea = 100;
        this.dwelling = new Dwelling(165000 * conditionedFloorArea, 200,
                                     Double.POSITIVE_INFINITY, INITIAL_DWELLING_TEMPERATURE,
                                     conditionedFloorArea, INITIAL_TIME, TIME_STEP_SIZE, this.controlStrategy,
                                     this.environment);
        this.dwellingReference = new DwellingReference(this.dwelling);
    }

    @Test
    public void testDwellingAsksControlStrategyForSetPoints() {
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(eq(INITIAL_TIME), any());
    }

    @Test
    public void testWhenPersonEntersDwellingItIsHandedOverToControlStrategy() {
        this.dwelling.enter(this.person);
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(INITIAL_TIME, this.personInSet);
    }

    @Test
    public void testDwellingIsEmptyAtStartup() {
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(INITIAL_TIME, new HashSet<>());
    }

    @Test
    public void testWhenPersonLeavesItIsNotHandedOverToControlStrategy() {
        this.dwelling.enter(this.person);
        this.dwelling.leave(this.person);
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(INITIAL_TIME, new HashSet<>());
    }

    @Test
    public void testDwellingTemperatureRemainsConstantWithSameTemperature() {
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(closeTo(INITIAL_DWELLING_TEMPERATURE, EPSILON)));
    }

    @Test
    public void testDwellingTemperatureRisesWhenWarmerOutside() {
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE + 1));
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    @Test
    public void testDwellingTemperatureSinksWhenColderOutside() {
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE - 1));
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    @Test
    public void testNoThermalPowerAboveHeatingSetPoint() {
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE + 1));
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentThermalPower(), is(equalTo(0.0)));
    }

    @Test
    public void testDwellingGetsHeatedWhenBelowHeatingSetPoint() {
        when(this.controlStrategy.heatingSetPoint(eq(INITIAL_TIME), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(23.0)));
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getCurrentTemperature(), is(lessThanOrEqualTo(23.0)));
        assertThat(this.dwelling.getCurrentThermalPower(), is(greaterThan(0.0)));
    }

    @Test
    public void switchesOffHeatingSystemWithoutHeatingSetPoint() {
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(0.0)); // it's cold outside!
        when(this.controlStrategy.heatingSetPoint(eq(INITIAL_TIME), any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentThermalPower(), is(closeTo(0.0, EPSILON)));
    }

    @Test
    public void advancesTime() {
        this.dwelling.step();
        this.dwelling.step();
        verify(this.controlStrategy, times(1)).heatingSetPoint(
                INITIAL_TIME.plus(TIME_STEP_SIZE),
                new HashSet<>()
        );
    }

    @Test
    public void canAccessTemperatureThroughReference() throws ExecutionException, InterruptedException {
        Reference.pool.setCurrentExecutor(Reference.pool.main);
        double temp = this.dwellingReference.getCurrentTemperature().get();
        assertThat(temp, is(closeTo(INITIAL_DWELLING_TEMPERATURE, EPSILON)));
    }

}
