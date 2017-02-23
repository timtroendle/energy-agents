package uk.ac.eeci;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DataLogger {

    public final static String METADATA_TABLE_NAME = "metadata";
    private final Set<DataPointReference> dataPoints;
    private final String filename;

    /**
     * Logs data points during the simulation.
     */
    public DataLogger(Collection<DataPointReference> dataPoints, String filename) {
        this.dataPoints = new HashSet<>(dataPoints);
        this.filename = filename;
    }

    public CompletableFuture<Void> step(ZonedDateTime currentTime) {
        CompletableFuture<Void>[] steps = new CompletableFuture[this.dataPoints.size()];
        int i = 0;
        for (DataPointReference dataPoint : this.dataPoints) {
            steps[i] = dataPoint.step(currentTime);
            i++;
        }
        return CompletableFuture.allOf(steps);
    }

    public CompletableFuture<Void> write(HashMap<String, String> metaData) {
        CompletableFuture<Void> steps = CompletableFuture.completedFuture(null);
        for (DataPointReference dp : this.dataPoints) {
            steps = steps
                    .thenCompose(unused -> dp.getName())
                    .thenCompose(name -> dp.getRecord().thenCompose(m -> CompletableFuture.completedFuture(new DataPointInternals(name, m))))
                    .thenAccept(this::writeDataPoint);
        }
        steps = steps.thenAccept(unused -> this.writeMetadata(metaData));
        return steps;
    }

    private static class DataPointInternals {
        private final String dpName;
        private final Map<Integer, TimeSeries<Object>> values;

        public DataPointInternals(Object dpName, Object values) { // FIXME raw type
            this.dpName = dpName.toString();
            this.values = (Map<Integer, TimeSeries<Object>>) values;
        }
    }

    private void writeDataPoint(Object dpAsObject) { // FIXME raw type
        DataPointInternals dp = (DataPointInternals) dpAsObject;
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.filename));
            writeDataPointToDatabase(conn, dp);
        } catch (ClassNotFoundException | SQLException ex) {
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
        boolean dataPointContainsDoubles = dataPointContainsDoubles(dp.values);
        Statement stat = conn.createStatement();
        stat.executeUpdate(String.format("drop table if exists %s;", dp.dpName));
        String valueDataType;
        if (dataPointContainsDoubles) {
            valueDataType = "DOUBLE PRECISION";
        } else {
            valueDataType = "VARCHAR(100)";
        }
        stat.executeUpdate(String.format(
                "create table %s (timestamp TIMESTAMP, id INTEGER, value %s);", dp.dpName, valueDataType));
        PreparedStatement prep = conn.prepareStatement(
                String.format("insert into %s values (?, ?, ?);", dp.dpName));

        int numberTimeSteps = dp.values.get(0).getIndex().size();
        for (int i = 0; i < numberTimeSteps; i++) {
            for (Integer j : dp.values.keySet()) {
                prep.setTimestamp(1, Timestamp.from(dp.values.get(j).getIndex().get(i).toInstant()));
                prep.setInt(2, j);
                if (dataPointContainsDoubles) {
                    Double value = (Double) dp.values.get(j).getValues().get(i);
                    prep.setDouble(3, value);
                } else {
                    String value = dp.values.get(j).getValues().get(i).toString();
                    prep.setString(3, value);
                }
                prep.addBatch();
            }
        }
        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
    }

    private static boolean dataPointContainsDoubles(Map<Integer, TimeSeries<Object>> dataPointValues) {
        try { // FIXME ugly duck typing
            Double value = (Double) dataPointValues.get(0).getValues().get(0);
            return true;
        } catch (ClassCastException cce) {
            return false;
        }
    }

    private void writeMetadata(HashMap<String, String> metadata) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.filename));
            writeMetadata(conn, metadata);
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            System.out.println("Failed to write metadata to database.");
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

    private static void writeMetadata(Connection conn, HashMap<String, String> metadata) throws SQLException {
        Statement stat = conn.createStatement();
        stat.executeUpdate(String.format("drop table if exists %s;", METADATA_TABLE_NAME));
        stat.executeUpdate(String.format(
                "create table %s (key VARCHAR(100), value VARCHAR(100));", METADATA_TABLE_NAME));
        PreparedStatement prep = conn.prepareStatement(
                String.format("insert into %s values (?, ?);", METADATA_TABLE_NAME));

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            prep.setString(1, entry.getKey());
            prep.setString(2, entry.getValue());
            prep.addBatch();
        }
        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
    }

}
