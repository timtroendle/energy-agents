package uk.ac.eeci.test;

import io.improbable.scienceos.Reference;
import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;

public class TestDwelling {

    private double EPSILON = 0.0001;
    private double INITIAL_DWELLING_TEMPERATURE = 22;
    private Dwelling dwelling;
    private DwellingReference dwellingReference;
    private HeatingControlStrategy controlStrategy = mock(HeatingControlStrategy.class);
    private EnvironmentReference environment = mock(EnvironmentReference.class);
    private PersonReference person = mock(PersonReference.class);
    private Set<PersonReference> personInSet;

    @Before
    public void setUp() {
        this.personInSet = new HashSet<>();
        this.personInSet.add(this.person);
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(21.9);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(26.0);
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE));
        double conditionedFloorArea = 100;
        this.dwelling = new Dwelling(165000 * conditionedFloorArea, 200, Double.NEGATIVE_INFINITY,
                                     Double.POSITIVE_INFINITY, INITIAL_DWELLING_TEMPERATURE,
                                     conditionedFloorArea, Duration.ofHours(1), this.controlStrategy, this.environment);
        this.dwellingReference = new DwellingReference(this.dwelling);
    }

    @Test
    public void testDwellingAsksControlStrategyForSetPoints() {
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(any());
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(any());
    }

    @Test
    public void testWhenPersonEntersDwellingItIsHandedOverToControlStrategy() {
        this.dwelling.enter(this.person);
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(this.personInSet);
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(this.personInSet);
    }

    @Test
    public void testDwellingIsEmptyAtStartup() {
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(new HashSet<>());
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(new HashSet<>());
    }

    @Test
    public void testWhenPersonLeavesItIsNotHandedOverToControlStrategy() {
        this.dwelling.enter(this.person);
        this.dwelling.leave(this.person);
        this.dwelling.step();
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(new HashSet<>());
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(new HashSet<>());
    }

    @Test
    public void testDwellingTemperatureRemainsConstantWithSameTemperature() {
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(closeTo(INITIAL_DWELLING_TEMPERATURE, EPSILON)));
    }

    @Test
    public void testDwellingTemperatureRisesWhenWarmerOutside() {
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE + 1));
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    @Test
    public void testDwellingTemperatureSinksWhenColderOutside() {
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE - 1));
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    @Test
    public void testNoThermalPowerWhenBetweenHeatingAndCoolingSetPoint() {
        when(this.environment.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE + 1));
        this.dwelling.step();
        assertThat(this.dwelling.getThermalPower(), is(equalTo(0.0)));
    }

    @Test
    public void testDwellingGetsHeatedWhenBelowHeatingSetPoint() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(23.0);
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(lessThanOrEqualTo(23.0)));
        assertThat(this.dwelling.getThermalPower(), is(greaterThan(0.0)));
    }

    @Test
    public void testDwellingGetsCooledWhenAboveCoolingSetPoint() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(20.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(21.0);
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(greaterThanOrEqualTo(21.0)));
        assertThat(this.dwelling.getThermalPower(), is(lessThan(0.0)));
    }

    @Test
    public void canAccessTemperatureThroughReference() throws ExecutionException, InterruptedException {
        Reference.pool.setCurrentExecutor(Reference.pool.main);
        double temp = this.dwellingReference.getTemperature().get();
        assertThat(temp, is(closeTo(INITIAL_DWELLING_TEMPERATURE, EPSILON)));
    }

}
