package uk.ac.cam.eeci.framework;

import java.util.concurrent.ExecutionException;

/**
 * Created by daniel on 09/02/17.
 */
public interface ISteppable {
    void step() throws ExecutionException, InterruptedException, EndSimulationException;
}
