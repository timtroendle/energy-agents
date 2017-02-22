package uk.ac.eeci.test.integration;

import io.improbable.scienceos.Reference;
import io.improbable.scienceos.WorkerPool;

public class Utils {

    public static void resetScienceOS() {
        Reference.pool = new WorkerPool(4);
        Reference.pool.setCurrentExecutor(Reference.pool.main);
    }
}
