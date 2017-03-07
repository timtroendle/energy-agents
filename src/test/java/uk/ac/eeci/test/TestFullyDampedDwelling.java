package uk.ac.eeci.test;

import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.Dwelling;
import uk.ac.eeci.EnvironmentReference;
import uk.ac.eeci.HeatingControlStrategy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

public class TestFullyDampedDwelling {

    private double EPSILON = 0.0001;
    private double INITIAL_DWELLING_TEMPERATURE = 22;
    private double MAXIMUM_COOLING_POWER = -1;
    private double MAXIMUM_HEATING_POWER = +1;
    private Dwelling dwelling;
    private HeatingControlStrategy controlStrategy = mock(HeatingControlStrategy.class);
    private EnvironmentReference environmentReference = mock(EnvironmentReference.class);

    @Before
    public void setUp() {
        double conditionedFloorArea = 1;
        when(this.environmentReference.getCurrentTemperature())
                .thenReturn(CompletableFuture.completedFuture(INITIAL_DWELLING_TEMPERATURE));
        this.dwelling = new Dwelling(3600 * conditionedFloorArea, 0,
                MAXIMUM_COOLING_POWER, MAXIMUM_HEATING_POWER, INITIAL_DWELLING_TEMPERATURE,
                conditionedFloorArea, Duration.ofHours(1), this.controlStrategy, this.environmentReference);
    }

    @Test
    public void testDwellingGetsHeatedWithMaxPowerWhenTooCold() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(23.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(26.0);
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(closeTo(23, EPSILON)));
        assertThat(this.dwelling.getCurrentThermalPower(), is(closeTo(MAXIMUM_HEATING_POWER, EPSILON)));
    }

    @Test
    public void testDwellingDoesNotExceedMaxHeatingPower() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(24.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(26.0);
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(closeTo(23, EPSILON)));
        assertThat(this.dwelling.getCurrentThermalPower(), is(closeTo(MAXIMUM_HEATING_POWER, EPSILON)));
    }

    @Test
    public void testDwellingGetsCooledWithMaxPowerWhenTooWarm() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(18.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(21.0);
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(closeTo(21, EPSILON)));
        assertThat(this.dwelling.getCurrentThermalPower(), is(closeTo(MAXIMUM_COOLING_POWER, EPSILON)));
    }

    @Test
    public void testDwellingDoesNotExceedMaxCoolingPower() {
        when(this.controlStrategy.heatingSetPoint(any())).thenReturn(18.0);
        when(this.controlStrategy.coolingSetPoint(any())).thenReturn(20.0);
        this.dwelling.step();
        assertThat(this.dwelling.getCurrentTemperature(), is(closeTo(21, EPSILON)));
        assertThat(this.dwelling.getCurrentThermalPower(), is(closeTo(MAXIMUM_COOLING_POWER, EPSILON)));
    }

}
