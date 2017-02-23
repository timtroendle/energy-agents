package uk.ac.eeci.test;

import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.*;

import java.time.Duration;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDwellingWithBrokenHVAC {

    private final static double EPSILON = 0.0001;
    private final static double INITIAL_DWELLING_TEMPERATURE = 16;
    private final static Double MAX_HEATING_POWER = 0.0;
    private final static Double MAX_COOLING_POWER = 0.0;
    private final static Duration TIME_STEP_SIZE = Duration.ofHours(1);
    private HeatingControlStrategy controlStrategy = mock(HeatingControlStrategy.class);
    private Environment environment = mock(Environment.class);
    private Dwelling dwelling;

    @Before
    public void setUp() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(20.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(26.0);
        when(this.environment.getCurrentTemperature()).thenReturn(INITIAL_DWELLING_TEMPERATURE);
        double conditionedFloorArea = 100;
        this.dwelling = new Dwelling(
                165000 * conditionedFloorArea,
                200,
                MAX_HEATING_POWER,
                MAX_COOLING_POWER,
                INITIAL_DWELLING_TEMPERATURE,
                conditionedFloorArea,
                TIME_STEP_SIZE,
                this.controlStrategy,
                new EnvironmentReference(this.environment));
    }

    @Test
    public void dwellingStaysColdWhenSameTempOutside() {
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(closeTo(INITIAL_DWELLING_TEMPERATURE, EPSILON)));
    }

    @Test
    public void dwellingGetsColderWhenColderOutside() {
        when(this.environment.getCurrentTemperature()).thenReturn(INITIAL_DWELLING_TEMPERATURE - 5);
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    @Test
    public void dwellingGetsWarmerWhenWarmerOutside() {
        when(this.environment.getCurrentTemperature()).thenReturn(INITIAL_DWELLING_TEMPERATURE + 5);
        this.dwelling.step();
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
    }
}
