package uk.ac.eeci;

import junit.framework.TestCase;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

public class TestDwelling extends TestCase {

    public double EPSILON = 0.0001;
    public double INITIAL_DWELLING_TEMPERATURE = 22;
    public Dwelling dwelling;

    public void setUp() {
        double conditionedFloorArea = 100;
        this.dwelling = new Dwelling(165000 * conditionedFloorArea, 200, Double.NEGATIVE_INFINITY,
                                     Double.POSITIVE_INFINITY, INITIAL_DWELLING_TEMPERATURE,
                                     conditionedFloorArea, 60 * 60);
    }

    public void testDwellingTemperatureRemainsConstantWithSameTemperature() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE, 21.9, 26);
        assertThat(this.dwelling.getTemperature(), is(closeTo(INITIAL_DWELLING_TEMPERATURE, EPSILON)));
    }

    public void testDwellingTemperatureRisesWhenWarmerOutside() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE + 1, 21.9, 26);
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    public void testDwellingTemperatureSinksWhenColderOutside() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE - 1, 21.9, 26);
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
    }

    public void testDwellingGetsHeatedWhenBelowHeatingSetPoint() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE, 23, 26);
        assertThat(this.dwelling.getTemperature(), is(greaterThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(lessThanOrEqualTo(23.0)));
    }

    public void testDwellingGetsCooledWhenAboveCoolingSetPoint() {
        this.dwelling.step(INITIAL_DWELLING_TEMPERATURE, 20, 21);
        assertThat(this.dwelling.getTemperature(), is(lessThan(INITIAL_DWELLING_TEMPERATURE)));
        assertThat(this.dwelling.getTemperature(), is(greaterThanOrEqualTo(21.0)));
    }

}
