package uk.ac.eeci;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

public class TestFullyDampedDwelling {

    private double EPSILON = 0.0001;
    private double INITIAL_DWELLING_TEMPERATURE = 22;
    private Dwelling dwelling;
    private HeatingControlStrategy controlStrategy = mock(HeatingControlStrategy.class);

    @Before
    public void setUp() {
        double conditionedFloorArea = 1;
        this.dwelling = new Dwelling(3600 * conditionedFloorArea, 0, -1,
                +1, INITIAL_DWELLING_TEMPERATURE,
                conditionedFloorArea, 60 * 60, this.controlStrategy);
    }

    @Test
    public void testDwellingGetsHeatedWithMaxPowerWhenTooCold() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(23.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(26.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(closeTo(23, EPSILON)));
    }

    @Test
    public void testDwellingDoesNotExceedMaxHeatingPower() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(24.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(26.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(closeTo(23, EPSILON)));
    }

    @Test
    public void testDwellingGetsCooledWithMaxPowerWhenTooWarm() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(18.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(21.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(closeTo(21, EPSILON)));
    }

    @Test
    public void testDwellingDoesNotExceedMaxCoolingPower() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(18.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(20.0);
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE);
        assertThat(this.dwelling.getTemperature(), is(closeTo(21, EPSILON)));
    }

}
