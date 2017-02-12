package uk.ac.eeci;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class TestDwelling {

    private double EPSILON = 0.0001;
    private double INITIAL_DWELLING_TEMPERATURE = 22;
    private Dwelling dwelling;
    private HeatingControlStrategy controlStrategy = mock(HeatingControlStrategy.class);

    @Before
    public void setUp() {
        when(this.controlStrategy.heatingSetPoint()).thenReturn(21.9);
        when(this.controlStrategy.coolingSetPoint()).thenReturn(26.0);
        double conditionedFloorArea = 100;
        this.dwelling = new Dwelling(165000 * conditionedFloorArea, 200, Double.NEGATIVE_INFINITY,
                                     Double.POSITIVE_INFINITY, INITIAL_DWELLING_TEMPERATURE,
                                     conditionedFloorArea, 60 * 60, this.controlStrategy);
    }

    @Test
    public void testDwellingAsksControlStrategyForSetPoints() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        verify(this.controlStrategy, atLeastOnce()).coolingSetPoint();
        verify(this.controlStrategy, atLeastOnce()).heatingSetPoint();
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
        when(this.controlStrategy.heatingSetPoint()).thenReturn(23.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(lessThanOrEqualTo(23.0)));
    }

    @Test
    public void testDwellingGetsCooledWhenAboveCoolingSetPoint() {
        when(this.controlStrategy.heatingSetPoint()).thenReturn(20.0);
        when(this.controlStrategy.coolingSetPoint()).thenReturn(21.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(greaterThanOrEqualTo(21.0)));
    }

}
