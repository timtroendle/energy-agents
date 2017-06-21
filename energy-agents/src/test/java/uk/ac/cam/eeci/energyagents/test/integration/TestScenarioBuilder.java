package uk.ac.cam.eeci.energyagents.test.integration;

import uk.ac.cam.eeci.framework.Conductor;
import org.hamcrest.Matchers;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.ac.cam.eeci.energyagents.CitySimulation;
import uk.ac.cam.eeci.energyagents.ScenarioBuilder;
import uk.ac.cam.eeci.energyagents.TimeSeries;
import uk.ac.cam.eeci.energyagents.test.utils.Utils;

import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

@Category(IntegrationTest.class)
public class TestScenarioBuilder {

    // constants from the input file
    private final static String INPUT_PATH = "test-scenario.db";
    private final static int NUMBER_DWELLINGS = 100;
    private final static int NUMBER_DISTRICTS = 10;
    private final static int NUMBER_PEOPLE = 200;
    private final static int NUMBER_TIME_STEPS = 90;
    private final static List<Integer> DWELLING_INDICES;
    private final static List<Integer> DISTRICT_INDICES;
    private final static List<Integer> PEOPLE_INDICES;
    private final static ZonedDateTime INITIAL_DATE_TIME = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final static Duration TIME_STEPS_SIZE = Duration.ofHours(12);
    private final static ZonedDateTime[] TIME_INDEX;

    static {
        DWELLING_INDICES = new ArrayList<>();
        DISTRICT_INDICES = new ArrayList<>();
        PEOPLE_INDICES = new ArrayList<>();
        for (int i = 0; i < NUMBER_DWELLINGS; i++) {
            DWELLING_INDICES.add(104 + i);
        }
        for (int i = 0; i < NUMBER_DISTRICTS; i++) {
            DISTRICT_INDICES.add(i);
        }
        for (int i = 1; i < NUMBER_PEOPLE * 2; i += 2) {
            PEOPLE_INDICES.add(i);
        }
        assertThat(DWELLING_INDICES.size(), is(equalTo(NUMBER_DWELLINGS)));
        assertThat(PEOPLE_INDICES.size(), is(equalTo(NUMBER_PEOPLE)));

        TIME_INDEX = new ZonedDateTime[NUMBER_TIME_STEPS];
        for (int i = 1; i <= NUMBER_TIME_STEPS; i++) {
            TIME_INDEX[i - 1] = INITIAL_DATE_TIME.plus(TIME_STEPS_SIZE.multipliedBy(i));
        }
    }

    private File tempInputFile;
    private File tempOutPutFile;
    private CitySimulation citySimulation;

    @Before
    public void setUp() throws IOException {
        Utils.resetScienceOS();
        this.tempInputFile = File.createTempFile("energy-agents-test-scenario-input", ".db");
        this.tempOutPutFile = File.createTempFile("energy-agents-test-scenario", ".db");
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Path src = FileSystems.getDefault().getPath(classloader.getResource(INPUT_PATH).getPath());
        Path dest = this.tempInputFile.toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    @After
    public void tearDown() {
        this.tempOutPutFile.deleteOnExit();
    }

    private void demandAggregatedResults() throws IOException, ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempInputFile.getCanonicalPath()));
        Statement stat = conn.createStatement();

        stat.executeUpdate(String.format("update %s set %s = 1;", ScenarioBuilder.SQL_TABLES_PARAMETERS,
                ScenarioBuilder.SQL_COLUMNS_PAR_LOG_AGGREGATED));
        conn.close();
    }


    @Test(expected = IOException.class)
    public void throwsIOExceptionWhenInputFileDoesNotExist() throws IOException {
        ScenarioBuilder.readScenario(this.tempInputFile.getPath() + "invalid", this.tempOutPutFile.getCanonicalPath());
    }

    @Test
    public void resultContainsValidTemperatureRecord() throws ClassNotFoundException, SQLException, IOException {
        this.citySimulation = ScenarioBuilder.readScenario(this.tempInputFile.getPath(), this.tempOutPutFile.getCanonicalPath());
        new Conductor(this.citySimulation).run();

        Map<Integer, TimeSeries<Double>> temperatureTimeSeries = readTemperatureRecordFromDB();

        assertThat(temperatureTimeSeries.size(), is(equalTo(NUMBER_DWELLINGS)));
        for (TimeSeries<Double> timeSeries : temperatureTimeSeries.values()) {
            assertThat(timeSeries.getIndex(), Matchers.contains(TIME_INDEX));
        }
        List<Integer> sortedIndexSet = new ArrayList<>(temperatureTimeSeries.keySet());
        Collections.sort(sortedIndexSet);
        assertThat(sortedIndexSet, is(equalTo(DWELLING_INDICES)));
    }

    @Test
    public void resultDoesNotContainThermalPowerRecord() throws ClassNotFoundException, SQLException, IOException {
        String outputPath = this.tempOutPutFile.getCanonicalPath();
        this.citySimulation = ScenarioBuilder.readScenario(this.tempInputFile.getPath(), this.tempOutPutFile.getCanonicalPath());
        new Conductor(this.citySimulation).run();

        List<String> tableNames = getTableNames(outputPath);

        assertThat(tableNames, not(hasItems(
                ScenarioBuilder.THERMAL_POWER_DATA_POINT_NAME
                ))
        );
    }

    @Test
    public void resultContainsValidActivityRecord() throws ClassNotFoundException, SQLException, IOException {
        this.citySimulation = ScenarioBuilder.readScenario(this.tempInputFile.getPath(), this.tempOutPutFile.getCanonicalPath());
        new Conductor(this.citySimulation).run();

        Map<Integer, TimeSeries<String>> activityTimeSeries = readActivityRecordFromDB();

        assertThat(activityTimeSeries.size(), is(equalTo(NUMBER_PEOPLE)));
        for (TimeSeries<String> timeSeries : activityTimeSeries.values()) {
            assertThat(timeSeries.getIndex(), Matchers.contains(TIME_INDEX));
        }
        List<Integer> sortedIndexSet = new ArrayList<>(activityTimeSeries.keySet());
        Collections.sort(sortedIndexSet);
        assertThat(sortedIndexSet, is(equalTo(PEOPLE_INDICES)));
    }

    @Test
    public void resultContainsInput() throws SQLException, IOException {
        String outputPath = this.tempOutPutFile.getCanonicalPath();
        this.citySimulation = ScenarioBuilder.readScenario(this.tempInputFile.getPath(), outputPath);
        new Conductor(this.citySimulation).run();

        List<String> tableNames = getTableNames(outputPath);

        assertThat(tableNames, hasItems(
                ScenarioBuilder.SQL_TABLES_DWELLINGS, ScenarioBuilder.SQL_TABLES_PEOPLE,
                ScenarioBuilder.SQL_TABLES_ENVIRONMENT, ScenarioBuilder.SQL_TABLES_MARKOV_CHAINS,
                ScenarioBuilder.SQL_TABLES_PARAMETERS
                )
        );
    }

    @Test
    public void resultsAreReproducible() throws IOException, SQLException, ClassNotFoundException {
        // run once
        String outputPath = this.tempOutPutFile.getCanonicalPath();
        this.citySimulation = ScenarioBuilder.readScenario(this.tempInputFile.getPath(), outputPath);
        new Conductor(this.citySimulation).run();

        Map<Integer, TimeSeries<String>> activityTimeSeries1 = readActivityRecordFromDB();
        Map<Integer, TimeSeries<Double>> temperatureTimeSeries1 = readTemperatureRecordFromDB();

        // ... and run again
        Utils.resetScienceOS();
        this.citySimulation = ScenarioBuilder.readScenario(this.tempInputFile.getPath(), outputPath);
        new Conductor(this.citySimulation).run();

        Map<Integer, TimeSeries<String>> activityTimeSeries2 = readActivityRecordFromDB();
        Map<Integer, TimeSeries<Double>> temperatureTimeSeries2 = readTemperatureRecordFromDB();

        assertThat(activityTimeSeries1, is(equalTo(activityTimeSeries2)));
        assertThat(temperatureTimeSeries1, is(equalTo(temperatureTimeSeries2)));
    }

    @Test
    public void temperatureAverageExistsWhenAggregatedResultsDemanded() throws IOException, SQLException, ClassNotFoundException {
        this.demandAggregatedResults();
        this.citySimulation = ScenarioBuilder.readScenario(this.tempInputFile.getPath(), this.tempOutPutFile.getCanonicalPath());
        new Conductor(this.citySimulation).run();

        Map<Integer, TimeSeries<Double>> averageTimeSeries = readAverageTemperatureRecordFromDB();

        assertThat(averageTimeSeries.size(), is(equalTo(NUMBER_DISTRICTS)));
        for (TimeSeries<Double> timeSeries : averageTimeSeries.values()) {
            assertThat(timeSeries.getIndex(), Matchers.contains(TIME_INDEX));
        }
        List<Integer> sortedIndexSet = new ArrayList<>(averageTimeSeries.keySet());
        Collections.sort(sortedIndexSet);
        assertThat(sortedIndexSet, is(equalTo(DISTRICT_INDICES)));
    }

    private List<String> getTableNames(String outputPath) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", outputPath))) {
            try (Statement stat = conn.createStatement()) {
                try (ResultSet rs = stat.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")) {
                    while (rs.next()) {
                        tableNames.add(rs.getString(1));
                    }
                }
            }
        }
        return tableNames;
    }

    private Map<Integer, TimeSeries<Double>> readTemperatureRecordFromDB()
            throws IOException, SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempOutPutFile.getCanonicalPath()));
        Statement stat = conn.createStatement();

        List<Triplet<Integer, ZonedDateTime, Double>> entries = new ArrayList<>();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", ScenarioBuilder.TEMPERATURE_DATA_POINT_NAME));
        while (rs.next()) {
            entries.add(new Triplet<>(
                    rs.getInt(2),
                    Instant.ofEpochMilli(rs.getLong(1)).atZone(ZoneOffset.UTC),
                    rs.getDouble(3)
            ));
        }
        rs.close();
        conn.close();

        Map<Integer, TimeSeries<Double>> timeSeries = new HashMap<>();
        List<Integer> indices = entries.stream().map(Triplet::getValue0).collect(Collectors.toList());
        for (Integer index : indices) {
            TimeSeries<Double> indexTimeSeries = new TimeSeries<>();
            timeSeries.put(index, indexTimeSeries);
            List<Pair<ZonedDateTime, Double>> timeSeriesEntries = entries.stream()
                    .filter(entry -> entry.getValue0().equals(index))
                    .map(entry -> new Pair<>(entry.getValue1(), entry.getValue2()))
                    .collect(Collectors.toList());
            for (Pair<ZonedDateTime, Double> timeSeriesEntry : timeSeriesEntries) {
                indexTimeSeries.add(timeSeriesEntry.getValue0(), timeSeriesEntry.getValue1());
            }
        }
        return timeSeries;
    }

    private Map<Integer, TimeSeries<Double>> readAverageTemperatureRecordFromDB()
            throws IOException, SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempOutPutFile.getCanonicalPath()));
        Statement stat = conn.createStatement();

        List<Triplet<Integer, ZonedDateTime, Double>> entries = new ArrayList<>();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", ScenarioBuilder.AVERAGE_TEMPERATURE_DATA_POINT_NAME));
        while (rs.next()) {
            entries.add(new Triplet<>(
                    rs.getInt(2),
                    Instant.ofEpochMilli(rs.getLong(1)).atZone(ZoneOffset.UTC),
                    rs.getDouble(3)
            ));
        }
        rs.close();
        conn.close();

        Map<Integer, TimeSeries<Double>> timeSeries = new HashMap<>();
        List<Integer> indices = entries.stream().map(Triplet::getValue0).collect(Collectors.toList());
        for (Integer index : indices) {
            TimeSeries<Double> indexTimeSeries = new TimeSeries<>();
            timeSeries.put(index, indexTimeSeries);
            List<Pair<ZonedDateTime, Double>> timeSeriesEntries = entries.stream()
                    .filter(entry -> entry.getValue0().equals(index))
                    .map(entry -> new Pair<>(entry.getValue1(), entry.getValue2()))
                    .collect(Collectors.toList());
            for (Pair<ZonedDateTime, Double> timeSeriesEntry : timeSeriesEntries) {
                indexTimeSeries.add(timeSeriesEntry.getValue0(), timeSeriesEntry.getValue1());
            }
        }
        return timeSeries;
    }

    private Map<Integer, TimeSeries<String>> readActivityRecordFromDB()
            throws IOException, SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempOutPutFile.getCanonicalPath()));
        Statement stat = conn.createStatement();

        List<Triplet<Integer, ZonedDateTime, String>> entries = new ArrayList<>();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", ScenarioBuilder.ACTIVITY_DATA_POINT_NAME));
        while (rs.next()) {
            entries.add(new Triplet<>(
                    rs.getInt(2),
                    Instant.ofEpochMilli(rs.getLong(1)).atZone(ZoneOffset.UTC),
                    rs.getString(3)
            ));
        }
        rs.close();
        conn.close();

        Map<Integer, TimeSeries<String>> timeSeries = new HashMap<>();
        List<Integer> indices = entries.stream().map(Triplet::getValue0).collect(Collectors.toList());
        for (Integer index : indices) {
            TimeSeries<String> indexTimeSeries = new TimeSeries<>();
            timeSeries.put(index, indexTimeSeries);
            List<Pair<ZonedDateTime, String>> timeSeriesEntries = entries.stream()
                    .filter(entry -> entry.getValue0().equals(index))
                    .map(entry -> new Pair<>(entry.getValue1(), entry.getValue2()))
                    .collect(Collectors.toList());
            for (Pair<ZonedDateTime, String> timeSeriesEntry : timeSeriesEntries) {
                indexTimeSeries.add(timeSeriesEntry.getValue0(), timeSeriesEntry.getValue1());
            }
        }
        return timeSeries;
    }

}
