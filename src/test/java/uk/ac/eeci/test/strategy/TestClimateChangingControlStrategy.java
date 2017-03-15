package uk.ac.eeci.test.strategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.eeci.strategy.ClimateChangingControlStrategy;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.*;
import java.util.concurrent.ExecutionException;


@RunWith(Parameterized.class)
public class TestClimateChangingControlStrategy extends StrategyTestBase {

    private final static double HEATING_SET_POINT = 26.0;
    private final static double EPSILON = 0.0001;

    @Before
    public void setUp() {
        super.setUp();
        this.strategy = new ClimateChangingControlStrategy(HEATING_SET_POINT);
    }

    @Test
    public void returnsConstantHeatingSetPointWhenEmpty() throws ExecutionException, InterruptedException {
        double heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, new HashSet<>()).get().get();
        assertThat(heatingSetPoint, is(closeTo(HEATING_SET_POINT, EPSILON)));
    }

    @Test
    public void returnsConstantHeatingSetPointNoMatterTheActivity() throws ExecutionException, InterruptedException {
        double heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, this.people).get().get();
        assertThat(heatingSetPoint, is(closeTo(HEATING_SET_POINT, EPSILON)));
    }

}
