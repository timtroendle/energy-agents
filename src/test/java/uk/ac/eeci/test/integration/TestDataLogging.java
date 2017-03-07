package uk.ac.eeci.test.integration;

import io.improbable.scienceos.Conductor;
import org.hamcrest.core.Every;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.ac.eeci.*;
import uk.ac.eeci.Person.Activity;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.eeci.test.integration.Utils.resetScienceOS;

@Category(IntegrationTest.class)
public class TestDataLogging {

    private final static int NUMBER_STEPS = 5;
    private final static ZonedDateTime INITIAL_TIME = ZonedDateTime.of(2017, 2, 21, 10, 10, 0, 0, ZoneId.of("Europe/Paris"));
    private final static Duration TIME_STEP_SIZE = Duration.ofMinutes(10);
    private Conductor conductor;
    private Dwelling dwelling1 = mock(Dwelling.class);
    private Dwelling dwelling2 = mock(Dwelling.class);
    private Person person1 = mock(Person.class);
    private Person person2 = mock(Person.class);
    private Person person3 = mock(Person.class);
    private Environment environment = mock(Environment.class);
    private DataPoint<DwellingReference, Double> temperatureDataPoint;
    private DataPoint<DwellingReference, Double> thermalPowerDataPoint;
    private DataPoint<PersonReference, Person.Activity> activityDataPoint;
    private File tempFile;
    private List<ZonedDateTime> timeIndex;
    private List<ZonedDateTime> timeIndexInUTC;


    @Before
    public void setUp() throws IOException, ExecutionException, InterruptedException {
        resetScienceOS();
        this.timeIndex = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            this.timeIndex.add(INITIAL_TIME.plusSeconds(i * TIME_STEP_SIZE.toMinutes() * 60));
        }
        this.timeIndexInUTC = this.timeIndex
                .stream()
                .map(timeStamp -> timeStamp.withZoneSameInstant(ZoneOffset.UTC))
                .collect(Collectors.toList());
        this.initDwellings();
        this.initPeople();
        List<DwellingReference> dwellingReferences = Stream.of(this.dwelling1, this.dwelling2)
                .map(DwellingReference::new)
                .collect(Collectors.toList());
        List<PersonReference> peopleReferences = Stream.of(this.person1, this.person2, this.person3)
                .map(PersonReference::new)
                .collect(Collectors.toList());
        Map<Integer, DwellingReference> indexedDwellings = new HashMap<>();
        for (int i = 0; i < dwellingReferences.size(); i++) {
            indexedDwellings.put(i, dwellingReferences.get(i));
        }
        this.temperatureDataPoint =  new DataPoint<>(
                "temperature",
                indexedDwellings,
                (DwellingReference::getTemperature)
        );
        this.thermalPowerDataPoint =  new DataPoint<>(
                "thermalPower",
                indexedDwellings,
                (DwellingReference::getThermalPower)
        );
        Map<Integer, PersonReference> indexedPeople = new HashMap<>();
        for (int i = 0; i < peopleReferences.size(); i++) {
            indexedPeople.put(i, peopleReferences.get(i));
        }
        this.activityDataPoint = new DataPoint<>(
                "activity",
                indexedPeople,
                (PersonReference::getCurrentActivity)
        );
        this.tempFile = File.createTempFile("energy-agents", ".db");
        DataLoggerReference dataLoggerReference = new DataLoggerReference(new DataLogger(
                Stream.of(this.temperatureDataPoint, this.activityDataPoint, this.thermalPowerDataPoint)
                        .map(DataPointReference::new)
                        .collect(Collectors.toList()),
                null,
                this.tempFile.getCanonicalPath()
        ));

        this.conductor = new Conductor(new CitySimulation(dwellingReferences,
                new HashSet<>(peopleReferences),
                new EnvironmentReference(this.environment),
                dataLoggerReference,
                INITIAL_TIME,
                TIME_STEP_SIZE,
                NUMBER_STEPS) {
        });
    }

    @After
    public void tearDown() {
        this.tempFile.deleteOnExit();
    }

    private void initDwellings() {
        when(this.dwelling1.step()).thenReturn(CompletableFuture.completedFuture(null));
        when(this.dwelling2.step()).thenReturn(CompletableFuture.completedFuture(null));
        when(this.dwelling1.getTemperature()).thenReturn(20.0);
        when(this.dwelling2.getTemperature()).thenReturn(30.0);
        when(this.dwelling1.getThermalPower()).thenReturn(100.1);
        when(this.dwelling2.getThermalPower()).thenReturn(-87.2);
    }

    private void initPeople() {
        when(this.person1.getCurrentActivity()).thenReturn(Activity.HOME);
        when(this.person2.getCurrentActivity()).thenReturn(Activity.NOT_AT_HOME);
        when(this.person3.getCurrentActivity()).thenReturn(Activity.SLEEP_AT_HOME);
    }

    @Test
    public void testLogsDwellingTemperature() {
        this.conductor.run();
        Map<Integer, TimeSeries<Double>> temperatures = this.temperatureDataPoint.getRecord();
        assertThat(temperatures.get(0).getValues(), (Every.everyItem(is(equalTo(20.0)))));
        assertThat(temperatures.get(1).getValues(), (Every.everyItem(is(equalTo(30.0)))));

        assertThat(temperatures.get(0).getIndex(), is(equalTo(this.timeIndex)));
        assertThat(temperatures.get(1).getIndex(), is(equalTo(this.timeIndex)));
    }

    @Test
    public void testLogsDwellingThermalPower() {
        this.conductor.run();
        Map<Integer, TimeSeries<Double>> temperatures = this.thermalPowerDataPoint.getRecord();
        assertThat(temperatures.get(0).getValues(), (Every.everyItem(is(equalTo(100.1)))));
        assertThat(temperatures.get(1).getValues(), (Every.everyItem(is(equalTo(-87.2)))));

        assertThat(temperatures.get(0).getIndex(), is(equalTo(this.timeIndex)));
        assertThat(temperatures.get(1).getIndex(), is(equalTo(this.timeIndex)));
    }

    @Test
    public void testLogsPeopleActivity() {
        this.conductor.run();
        Map<Integer, TimeSeries<Activity>> activities = this.activityDataPoint.getRecord();
        assertThat(activities.get(0).getValues(), (Every.everyItem(is(equalTo(Activity.HOME)))));
        assertThat(activities.get(1).getValues(), (Every.everyItem(is(equalTo(Activity.NOT_AT_HOME)))));
        assertThat(activities.get(2).getValues(), (Every.everyItem(is(equalTo(Activity.SLEEP_AT_HOME)))));

        assertThat(activities.get(0).getIndex(), is(equalTo(this.timeIndex)));
        assertThat(activities.get(1).getIndex(), is(equalTo(this.timeIndex)));
    }

    @Test
    public void writesTemperatureToDatabase() throws IOException, SQLException, ClassNotFoundException {
        this.conductor.run();
        String filename = this.tempFile.getCanonicalPath();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
        Statement stat = conn.createStatement();

        Map<Integer, TimeSeries<Double>> values = new HashMap<>();
        values.put(0, new TimeSeries<>());
        values.put(1, new TimeSeries<>());
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", this.temperatureDataPoint.getName()));
        while (rs.next()) {
            values
                    .get(rs.getInt(2))
                    .add(Instant.ofEpochMilli(rs.getLong(1)).atZone(ZoneOffset.UTC), rs.getDouble(3));
        }
        rs.close();
        conn.close();

        assertThat(values.get(0).getValues(), (Every.everyItem(is(equalTo(20.0)))));
        assertThat(values.get(1).getValues(), (Every.everyItem(is(equalTo(30.0)))));

        assertThat(values.get(0).getIndex(), is(equalTo(this.timeIndexInUTC)));
        assertThat(values.get(1).getIndex(), is(equalTo(this.timeIndexInUTC)));
    }

    @Test
    public void writesThermalPowerToDatabase() throws IOException, SQLException, ClassNotFoundException {
        this.conductor.run();
        String filename = this.tempFile.getCanonicalPath();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
        Statement stat = conn.createStatement();

        Map<Integer, TimeSeries<Double>> values = new HashMap<>();
        values.put(0, new TimeSeries<>());
        values.put(1, new TimeSeries<>());
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", this.thermalPowerDataPoint.getName()));
        while (rs.next()) {
            values
                    .get(rs.getInt(2))
                    .add(Instant.ofEpochMilli(rs.getLong(1)).atZone(ZoneOffset.UTC), rs.getDouble(3));
        }
        rs.close();
        conn.close();

        assertThat(values.get(0).getValues(), (Every.everyItem(is(equalTo(100.1)))));
        assertThat(values.get(1).getValues(), (Every.everyItem(is(equalTo(-87.2)))));

        assertThat(values.get(0).getIndex(), is(equalTo(this.timeIndexInUTC)));
        assertThat(values.get(1).getIndex(), is(equalTo(this.timeIndexInUTC)));
    }

    @Test
    public void writesActivityToDatabase() throws IOException, SQLException, ClassNotFoundException {
        this.conductor.run();
        String filename = this.tempFile.getCanonicalPath();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
        Statement stat = conn.createStatement();

        Map<Integer, TimeSeries<String>> values = new HashMap<>();
        values.put(0, new TimeSeries<>());
        values.put(1, new TimeSeries<>());
        values.put(2, new TimeSeries<>());
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", this.activityDataPoint.getName()));
        while (rs.next()) {
            values
                    .get(rs.getInt(2))
                    .add(Instant.ofEpochMilli(rs.getLong(1)).atZone(ZoneOffset.UTC), rs.getString(3));
        }
        rs.close();
        conn.close();

        assertThat(values.get(0).getValues(), (Every.everyItem(is(equalTo("HOME")))));
        assertThat(values.get(1).getValues(), (Every.everyItem(is(equalTo("NOT_AT_HOME")))));
        assertThat(values.get(2).getValues(), (Every.everyItem(is(equalTo("SLEEP_AT_HOME")))));

        assertThat(values.get(0).getIndex(), is(equalTo(this.timeIndexInUTC)));
        assertThat(values.get(1).getIndex(), is(equalTo(this.timeIndexInUTC)));
        assertThat(values.get(2).getIndex(), is(equalTo(this.timeIndexInUTC)));
    }

    @Test
    public void writesMetadataToDatabase() throws IOException, ClassNotFoundException, SQLException {
        this.conductor.run();
        String filename = this.tempFile.getCanonicalPath();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
        Statement stat = conn.createStatement();

        Map<String, String> metadata = new HashMap<>();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", DataLogger.METADATA_TABLE_NAME));
        while (rs.next()) {
            metadata.put(rs.getString(1), rs.getString(2));
        }
        rs.close();
        conn.close();

        assertThat(metadata.keySet(), containsInAnyOrder(
                CitySimulation.METADATA_KEY_SIM_START,
                CitySimulation.METADATA_KEY_SIM_END,
                CitySimulation.METADATA_KEY_SIM_DURATION,
                CitySimulation.METADATA_KEY_MODEL_VERSION
        ));
    }
}
