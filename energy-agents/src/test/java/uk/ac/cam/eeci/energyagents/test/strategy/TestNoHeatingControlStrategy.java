package uk.ac.cam.eeci.energyagents.test.strategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.cam.eeci.energyagents.strategy.NoHeatingStrategy;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
    public void returnsNoHeatingSetPointWhenEmpty() throws ExecutionException, InterruptedException {
        Optional<Double> heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, new HashSet<>()).get();
        assertThat(heatingSetPoint.isPresent(), is(equalTo(false)));
    }

    @Test
    public void returnsNoHeatingSetPointNoMatterTheActivity() throws ExecutionException, InterruptedException {
        Optional<Double> heatingSetPoint = this.strategy.heatingSetPoint(this.timeStamp, this.people).get();
        assertThat(heatingSetPoint.isPresent(), is(equalTo(false)));
    }

}
