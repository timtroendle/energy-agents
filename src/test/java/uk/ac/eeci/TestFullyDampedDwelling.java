package uk.ac.eeci;

import junit.framework.TestCase;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class TestFullyDampedDwelling extends TestCase {

    public double EPSILON = 0.0001;
    public double INITIAL_DWELLING_TEMPERATURE = 22;
    public Dwelling dwelling;

    public void setUp() {
        double conditionedFloorArea = 1;
        this.dwelling = new Dwelling(3600 * conditionedFloorArea, 0, -1,
                +1, INITIAL_DWELLING_TEMPERATURE,
                conditionedFloorArea, 60 * 60);
    }

    public void testDwellingGetsHeatedWithMaxPowerWhenTooCold() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE, 23, 26);
        assertThat(this.dwelling.getTemperature(), is(closeTo(23, EPSILON)));
    }

    public void testDwellingDoesNotExceedMaxHeatingPower() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE, 24, 26);
        assertThat(this.dwelling.getTemperature(), is(closeTo(23, EPSILON)));
    }

    public void testDwellingGetsCooledWithMaxPowerWhenTooWarm() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE, 18, 21);
        assertThat(this.dwelling.getTemperature(), is(closeTo(21, EPSILON)));
    }

    public void testDwellingDoesNotExceedMaxCoolingPower() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE, 18, 20);
        assertThat(this.dwelling.getTemperature(), is(closeTo(21, EPSILON)));
    }

}
