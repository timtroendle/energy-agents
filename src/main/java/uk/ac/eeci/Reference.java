package uk.ac.eeci;

import java.util.concurrent.ExecutorService;

public class Reference<T> {
    static PoolPool pool = new PoolPool(4);
    ExecutorService executor;
    T               referent;

    public Reference(T referent) {
        executor = pool.executorFor(referent);
        this.referent = referent;
    }

}
