package uk.ac.eeci;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private HeatingControlStrategy controlStrategy = mock(HeatingControlStrategy.class);
    private PersonReference person = mock(PersonReference.class);
    private List<PersonReference> personInList;

    @Before
    public void setUp() {
        this.personInList = new LinkedList<>();
        this.personInList.add(this.person);
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(21.9);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(26.0);
        double conditionedFloorArea = 100;
        this.dwelling = new Dwelling(165000 * conditionedFloorArea, 200, Double.NEGATIVE_INFINITY,
                                     Double.POSITIVE_INFINITY, INITIAL_DWELLING_TEMPERATURE,
                                     conditionedFloorArea, 60 * 60, this.controlStrategy);
    }

    @Test
    public void testDwellingAsksControlStrategyForSetPoints() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(any());
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(any());
    }

    @Test
    public void testWhenPersonEntersDwellingItIsHandedOverToControlStrategy() {
        this.dwelling.enter(this.person);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(this.personInList);
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(this.personInList);
    }

    @Test
    public void testDwellingIsEmptyAtStartup() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(new ArrayList<>());
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(new ArrayList<>());
    }

    @Test
    public void testWhenPersonLeavesItIsNotHandedOverToControlStrategy() {
        this.dwelling.enter(this.person);
        this.dwelling.leave(this.person);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint(new ArrayList<>());
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint(new ArrayList<>());
    }

    @Test
    public void testDwellingTemperatureRemainsConstantWithSameTemperature() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(closeTo(INITIAL_DWELLING_TEMPERATURE, EPSILON)));
    }

    @Test
    public void testDwellingTemperatureRisesWhenWarmerOutside() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE + 1);
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    @Test
    public void testDwellingTemperatureSinksWhenColderOutside() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE - 1);
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    @Test
    public void testDwellingGetsHeatedWhenBelowHeatingSetPoint() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(23.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(lessThanOrEqualTo(23.0)));
    }

    @Test
    public void testDwellingGetsCooledWhenAboveCoolingSetPoint() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(20.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(21.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(greaterThanOrEqualTo(21.0)));
    }

}
