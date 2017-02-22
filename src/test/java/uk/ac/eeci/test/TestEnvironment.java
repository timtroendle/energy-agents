package uk.ac.eeci.test;

import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.Environment;
import uk.ac.eeci.TimeSeries;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TestEnvironment {

    private final ZonedDateTime INITIAL_DATETIME = ZonedDateTime.of(2017, 2, 22, 9, 50, 0, 0, ZoneOffset.UTC);
    private final Duration TIME_STEP_SIZE = Duration.ofMinutes(10);
    private final Double EPSILON = 0.000001;
    private TimeSeries<Double> temperatureTimeSeries;
    private Environment environment;

    @Before
    public void setUp() {
        this.temperatureTimeSeries = new TimeSeries<>();
        this.temperatureTimeSeries.add(INITIAL_DATETIME, 24.0);
        this.temperatureTimeSeries.add(INITIAL_DATETIME.plus(TIME_STEP_SIZE), 25.0);
        this.environment = new Environment(temperatureTimeSeries, this.TIME_STEP_SIZE);
    }

    @Test
    public void returnsInitialTemperature() {
        assertThat(this.environment.getCurrentTemperature(), is(equalTo(24.0)));
    }

    @Test
    public void returnsNextTemperatureWhenStepped() {
        this.environment.step();
        assertThat(this.environment.getCurrentTemperature(), is(equalTo(25.0)));
    }

    @Test(expected=IllegalArgumentException.class)
    public void validatesTimeStepSize() {
        new Environment(this.temperatureTimeSeries, Duration.ofHours(1));
    }

    @Test(expected=IllegalArgumentException.class)
    public void validatesConstantTimeStepSize() {
        this.temperatureTimeSeries = new TimeSeries<>();
        this.temperatureTimeSeries.add(INITIAL_DATETIME, 24.0);
        this.temperatureTimeSeries.add(INITIAL_DATETIME.plus(TIME_STEP_SIZE), 25.0);
        this.temperatureTimeSeries.add(INITIAL_DATETIME.plus(TIME_STEP_SIZE.multipliedBy(4)), 26.0);
        new Environment(this.temperatureTimeSeries, TIME_STEP_SIZE);
    }
}
