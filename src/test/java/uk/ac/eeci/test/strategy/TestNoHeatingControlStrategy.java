package uk.ac.eeci.test.strategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.eeci.strategy.NoHeatingStrategy;

import java.util.HashSet;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TestNoHeatingControlStrategy extends StrategyTestBase {

    @Before
    public void setUp() {
        super.setUp();
        this.strategy = new NoHeatingStrategy();
    }


    @Test
    public void returnsNoHeatingSetPointWhenEmpty() {
        Optional<Double> heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, new HashSet<>());
        assertThat(heatingSetPoint.isPresent(), is(false));
    }

    @Test
    public void returnsNoHeatingSetPointNoMatterTheActivity() {
        Optional<Double> heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, this.people);
        assertThat(heatingSetPoint.isPresent(), is(false));
    }

}
