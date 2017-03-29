package uk.ac.eeci;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DataLogger {

    public final static String METADATA_TABLE_NAME = "metadata";
    private final Set<DataPointReference> dataPoints;
    private final String inputFilename;
    private final String outputFilename;

    /**
     * Logs data points during the simulation.
     */
    public DataLogger(Collection<DataPointReference> dataPoints, String inputFilename, String outputFilename) {
        this.dataPoints = new HashSet<>(dataPoints);
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
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
        steps.thenRun(this::copyInput);

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
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.outputFilename))){
            writeDataPointToDatabase(conn, dp);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println(String.format("Failed to write datapoint %s.", dp.dpName));
        }
    }

    private static void writeDataPointToDatabase(Connection conn, DataPointInternals dp)
            throws SQLException {
        boolean dataPointContainsDoubles = dataPointContainsDoubles(dp.values);
        String valueDataType;
        if (dataPointContainsDoubles) {
            valueDataType = "DOUBLE PRECISION";
        } else {
            valueDataType = "VARCHAR(100)";
        }
        try (Statement stat = conn.createStatement()) {
            stat.executeUpdate(String.format("drop table if exists %s;", dp.dpName));
            stat.executeUpdate(String.format(
                    "create table %s (timestamp TIMESTAMP, id INTEGER, value %s);", dp.dpName, valueDataType));
            PreparedStatement prep = conn.prepareStatement(
                    String.format("insert into %s values (?, ?, ?);", dp.dpName));
            int numberTimeSteps = anyTimeSeries(dp.values).getIndex().size();
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
                conn.setAutoCommit(false);
                prep.executeBatch();
                conn.setAutoCommit(true);
            }
        }
    }

    private static boolean dataPointContainsDoubles(Map<Integer, TimeSeries<Object>> dataPointValues) {
        try { // FIXME ugly duck typing
            Double value = (Double) anyTimeSeries(dataPointValues).getValues().get(0);
            return true;
        } catch (ClassCastException cce) {
            return false;
        }
    }

    private static TimeSeries<Object> anyTimeSeries(Map<Integer, TimeSeries<Object>> timeSeriesMap) {
        int indexOfAnyTimeSeries = new ArrayList<>(timeSeriesMap.keySet()).get(0);
        return timeSeriesMap.get(indexOfAnyTimeSeries);

    }

    private void writeMetadata(HashMap<String, String> metadata) {
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.outputFilename))) {
            writeMetadata(conn, metadata);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("Failed to write metadata to database.");
        }
    }

    private void copyInput() {
        if (this.inputFilename == null) {
            return;
        }
        Path src = FileSystems.getDefault().getPath(this.inputFilename);
        Path dest = FileSystems.getDefault().getPath(this.outputFilename);
        try {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not copy input database into output.");
        }
    }

    private static void writeMetadata(Connection conn, HashMap<String, String> metadata) throws SQLException {
        try (Statement stat = conn.createStatement()) {
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

}
