package uk.ac.cam.eeci.energyagents.test.utils;

import io.improbable.scienceos.Reference;
import io.improbable.scienceos.WorkerPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    public static void resetScienceOS() {
        Reference.pool = new WorkerPool(4);
        Reference.pool.setCurrentExecutor(Reference.pool.main);
    }

    /**
     * Creates a cartesian product of test parameters as expected by JUnit.
     *
     * @param parameterArrays one Object[] for each test parameter containing
     *                        all the values of that parameter
     * @return the cartesian product of the test parameters as expected by
     *         JUnit.
     */
    public static List<Object[]> cartesian(Object[]... parameterArrays) {

        BinaryOperator<Object[]> aggregator = (a, b) -> {
            ArrayList<Object> asList = new ArrayList<>(Arrays.asList(a));
            asList.addAll(Arrays.asList(b));
            return asList.toArray(new Object[asList.size()]);
        };
        List<Supplier<Stream<Object[]>>> suppliers = new ArrayList<>();
        for (Object[] parameterArray : parameterArrays){

            Object[][] parameter = new Object[parameterArray.length][1];
            for (int i = 0; i < parameterArray.length; i++) {
                parameter[i][0] = parameterArray[i];
            }
            suppliers.add(() -> Arrays.stream(parameter));
        }
        return suppliers.stream()
                .reduce((s1, s2) ->
                        () -> s1.get().flatMap(t1 -> s2.get().map(t2 -> aggregator.apply(t1, t2))))
                .orElse(Stream::empty).get()
                .collect(Collectors.toList());
    }
}
