package uk.ac.eeci.test.integration;

import io.improbable.scienceos.Conductor;
import org.hamcrest.Matchers;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.ac.eeci.CitySimulation;
import uk.ac.eeci.ScenarioBuilder;
import uk.ac.eeci.TimeSeries;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static uk.ac.eeci.test.utils.Utils.resetScienceOS;

@Category(IntegrationTest.class)
public class TestScenarioBuilder {

    // constants from the input file
    private final static String INPUT_PATH = "test-scenario.db";
    private final static int NUMBER_DWELLINGS = 100;
    private final static int NUMBER_PEOPLE = 200;
    private final static int NUMBER_TIME_STEPS = 90;
    private final static List<Integer> DWELLING_INDICES;
    private final static List<Integer> PEOPLE_INDICES;
    private final static ZonedDateTime INITIAL_DATE_TIME = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final static Duration TIME_STEPS_SIZE = Duration.ofHours(12);
    private final static ZonedDateTime[] TIME_INDEX;

    static {
        DWELLING_INDICES = new ArrayList<>();
        PEOPLE_INDICES = new ArrayList<>();
        for (int i = 0; i < NUMBER_DWELLINGS; i++) {
            DWELLING_INDICES.add(104 + i);
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

    private File tempOutPutFile;
    private URL inputURL;
    private CitySimulation citySimulation;

    @Before
    public void setUp() throws IOException {
        resetScienceOS();
        this.tempOutPutFile = File.createTempFile("energy-agents-test-scenario", ".db");
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        this.inputURL = classloader.getResource(INPUT_PATH);
    }

    @After
    public void tearDown() {
        this.tempOutPutFile.deleteOnExit();
    }

    @Test
    public void resultContainsValidTemperatureRecord() throws ClassNotFoundException, SQLException, IOException {
        this.citySimulation = ScenarioBuilder.readScenario(this.inputURL.getPath(), this.tempOutPutFile.getCanonicalPath());
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
    public void resultContainsValidThermalPowerRecord() throws ClassNotFoundException, SQLException, IOException {
        this.citySimulation = ScenarioBuilder.readScenario(this.inputURL.getPath(), this.tempOutPutFile.getCanonicalPath());
        new Conductor(this.citySimulation).run();

        Map<Integer, TimeSeries<Double>> powerTimeSeries = readThermalPowerRecordFromDB();

        assertThat(powerTimeSeries.size(), is(equalTo(NUMBER_DWELLINGS)));
        for (TimeSeries<Double> timeSeries : powerTimeSeries.values()) {
            assertThat(timeSeries.getIndex(), Matchers.contains(TIME_INDEX));
        }
        List<Integer> sortedIndexSet = new ArrayList<>(powerTimeSeries.keySet());
        Collections.sort(sortedIndexSet);
        assertThat(sortedIndexSet, is(equalTo(DWELLING_INDICES)));
    }

    @Test
    public void resultContainsValidActivityRecord() throws ClassNotFoundException, SQLException, IOException {
        this.citySimulation = ScenarioBuilder.readScenario(this.inputURL.getPath(), this.tempOutPutFile.getCanonicalPath());
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
        this.citySimulation = ScenarioBuilder.readScenario(this.inputURL.getPath(), outputPath);
        new Conductor(this.citySimulation).run();

        List<String> tableNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", outputPath))) {
            try (Statement stat = conn.createStatement()){
                try (ResultSet rs = stat.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")) {
                    while (rs.next()) {
                        tableNames.add(rs.getString(1));
                    }
                }
            }
        }

        assertThat(tableNames, hasItems(
                ScenarioBuilder.SQL_TABLES_DWELLINGS, ScenarioBuilder.SQL_TABLES_PEOPLE,
                ScenarioBuilder.SQL_TABLES_ENVIRONMENT, ScenarioBuilder.SQL_TABLES_MARKOV_CHAINS,
                ScenarioBuilder.SQL_TABLES_PARAMETERS
                )
        );
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
                indexTimeSeries.add(timeSeriesEntry);
            }
        }
        return timeSeries;
    }

    private Map<Integer, TimeSeries<Double>> readThermalPowerRecordFromDB()
            throws IOException, SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempOutPutFile.getCanonicalPath()));
        Statement stat = conn.createStatement();

        List<Triplet<Integer, ZonedDateTime, Double>> entries = new ArrayList<>();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", ScenarioBuilder.THERMAL_POWER_DATA_POINT_NAME));
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
                indexTimeSeries.add(timeSeriesEntry);
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
                indexTimeSeries.add(timeSeriesEntry);
            }
        }
        return timeSeries;
    }

}
