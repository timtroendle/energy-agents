package uk.ac.eeci;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DataLogger {

    private final Set<DataPointReference> dataPoints;
    private final String filename;

    /** Logs data points during the simulation. */
    public DataLogger(Collection<DataPointReference> dataPoints, String filename) {
        this.dataPoints = new HashSet<>(dataPoints);
        this.filename = filename;
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
        } catch (InterruptedException|ExecutionException e) {
            e.printStackTrace(); // FIXME proper handling
        }
    }

    public CompletableFuture<Void> write() {
        CompletableFuture<Void> steps = CompletableFuture.completedFuture(null);
        for (DataPointReference dp : this.dataPoints) {
            steps = steps
                    .thenCompose(unused -> dp.getName())
                    .thenCompose(name -> dp.getRecord().thenCompose(m -> CompletableFuture.completedFuture(new DataPointInternals(name, m))))
                    .thenAccept(this::writeDataPoint);
        }
        return steps;
    }

    private static class DataPointInternals {
        private final String dpName;
        private final Map<Integer, List<Object>> values;

        public DataPointInternals(Object dpName, Object values) { // FIXME raw type
            this.dpName = dpName.toString();
            this.values = (Map<Integer, List<Object>>) values;
        }
    }

    private void writeDataPoint(Object dpAsObject) { // FIXME raw type
        DataPointInternals dp = (DataPointInternals) dpAsObject;
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.filename));
            writeDataPointToDatabase(conn, dp);
        } catch (ClassNotFoundException|SQLException ex) {
            ex.printStackTrace();
            System.out.println(String.format("Failed to write datapoint %s.", dp.dpName));
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void writeDataPointToDatabase(Connection conn, DataPointInternals dp)
            throws SQLException {
        Statement stat = conn.createStatement();
        stat.executeUpdate(String.format("drop table if exists %s;", dp.dpName));
        stat.executeUpdate(String.format("create table %s (id, value);", dp.dpName));
        PreparedStatement prep = conn.prepareStatement(
                String.format("insert into %s values (?, ?);", dp.dpName));

        boolean dataPointContainsDoubles = dataPointContainsDoubles(dp.values);
        for (int i = 0; i < 5; i++) {
            for (Integer j : dp.values.keySet()) {
                prep.setInt(1, j);
                if (dataPointContainsDoubles) {
                    Double value = (Double) dp.values.get(j).get(i);
                    prep.setDouble(2, value);
                } else {
                    String value = dp.values.get(j).get(i).toString();
                    prep.setString(2, value);
                }
                prep.addBatch();
            }
        }
        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
    }

    private static boolean dataPointContainsDoubles(Map<Integer, List<Object>> dataPointValues) {
        try { // FIXME ugly duck typing
            Double value = (Double) dataPointValues.get(0).get(0);
            return true;
        } catch (ClassCastException cce) {
            return false;
        }
    }

}
