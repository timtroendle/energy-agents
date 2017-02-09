package uk.ac.eeci;

import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;

public class Main {
    public static Simulation sim = new Simulation();

    public static void main(String[] args) {
        try {
            int i,p;
            for(i=0; i<400; ++i) { // FIXME change to actual simulation length
                Reference.pool.mainExecutor().submit(() -> {
                    sim.step();
                }).get();
            }
            sleep(200);
            Reference.pool.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }
}
