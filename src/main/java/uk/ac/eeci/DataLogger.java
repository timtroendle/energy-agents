package uk.ac.eeci;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DataLogger {

    private final Set<DataPointReference> dataPoints;

    /** Logs data points during the simulation. */
    public DataLogger(Collection<DataPointReference> dataPoints) {
        this.dataPoints = new HashSet<>(dataPoints);
    }

    public void step() {
        CompletableFuture<Void>[] steps = new CompletableFuture[this.dataPoints.size()];
        int i = 0;
        for (DataPointReference dataPoint : this.dataPoints) {
            steps[i] = dataPoint.step();
            i++;
        }
        try {
            CompletableFuture.allOf(steps).get();
        } catch (InterruptedException e) {
            e.printStackTrace(); // FIXME proper handling
        } catch (ExecutionException e) {
            e.printStackTrace(); // FIXME proper handling
        }
    }

}
