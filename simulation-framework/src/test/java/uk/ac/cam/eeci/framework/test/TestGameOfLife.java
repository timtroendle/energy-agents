package uk.ac.cam.eeci.framework.test;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.eeci.framework.Conductor;

public class TestGameOfLife {

    public Conductor conductor;

    @Before
    public void setUp() {
        this.conductor = new Conductor(new Simulation());
    }

    @Test
    public void testConductorRuns() {
        this.conductor.run();
    }

}
