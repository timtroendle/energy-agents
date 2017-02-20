package uk.ac.eeci.test.integration;

import io.improbable.scienceos.Conductor;
import io.improbable.scienceos.EndSimulationException;
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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(IntegrationTest.class)
public class TestDataLogging {

    private final static int NUMBER_STEPS = 5;
    private final static double CONSTANT_OUTDOOR_TEMPERATURE = 24.0;
    private Conductor conductor;
    private Dwelling dwelling1 = mock(Dwelling.class);
    private Dwelling dwelling2 = mock(Dwelling.class);
    private Person person1 = mock(Person.class);
    private Person person2 = mock(Person.class);
    private Person person3 = mock(Person.class);
    private DataPoint<DwellingReference, Double> temperatureDataPoint;
    private DataPoint<PersonReference, Person.Activity> activityDataPoint;
    private File tempFile;


    private static class ShortSimulation extends CitySimulation {

        private int remainingSteps;

        public ShortSimulation(Collection<DwellingReference> dwellings, Collection<PersonReference> people,
                               DataLoggerReference dataLoggerReference, double outdoorTemperature, int numberSteps) {
            super(dwellings, people, outdoorTemperature, dataLoggerReference);
            this.remainingSteps = numberSteps;
        }

        public void step() throws InterruptedException, ExecutionException, EndSimulationException {
            if (this.remainingSteps > 0) {
                super.step();
                this.remainingSteps -= 1;
            } else {
                throw new EndSimulationException();
            }
        }
    }

    @Before
    public void setUp() throws IOException, ExecutionException, InterruptedException {
        this.initDwellings();
        this.initPeople();
        List<DwellingReference> dwellingReferences = Stream.of(this.dwelling1, this.dwelling2)
                .map(DwellingReference::new)
                .collect(Collectors.toList());
        List<PersonReference> peopleReferences = Stream.of(this.person1, this.person2, this.person3)
                .map(PersonReference::new)
                .collect(Collectors.toList());
        this.temperatureDataPoint =  new DataPoint<>(
                "temperature",
                dwellingReferences,
                (DwellingReference::getTemperature)
        );
        this.activityDataPoint = new DataPoint<>(
                "activity",
                peopleReferences,
                (PersonReference::getCurrentActivity)
        );
        this.tempFile = File.createTempFile("energy-agents", ".db");
        DataLoggerReference dataLoggerReference = new DataLoggerReference(new DataLogger(
                Stream.of(this.temperatureDataPoint, this.activityDataPoint)
                        .map(DataPointReference::new)
                        .collect(Collectors.toList()),
                this.tempFile.getCanonicalPath()
        ));

        this.conductor = new Conductor(new ShortSimulation(dwellingReferences,
                new HashSet<>(peopleReferences),
                dataLoggerReference,
                CONSTANT_OUTDOOR_TEMPERATURE,
                NUMBER_STEPS) {
        });
    }

    @After
    public void tearDown() {
        this.tempFile.deleteOnExit();
    }

    private void initDwellings() {
        when(this.dwelling1.getTemperature()).thenReturn(20.0);
        when(this.dwelling2.getTemperature()).thenReturn(30.0);
    }

    private void initPeople() {
        when(this.person1.getCurrentActivity()).thenReturn(Activity.HOME);
        when(this.person2.getCurrentActivity()).thenReturn(Activity.NOT_AT_HOME);
        when(this.person3.getCurrentActivity()).thenReturn(Activity.SLEEP_AT_HOME);
    }

    @Test
    public void testLogsDwellingTemperature() {
        this.conductor.run();
        Map<Integer, List<Double>> temperatures = this.temperatureDataPoint.getRecord();
        for (List<Double> dwellingTimeSeries : temperatures.values()) {
            assertThat(dwellingTimeSeries, hasSize(NUMBER_STEPS));
        }
        assertThat(temperatures.get(0), (Every.everyItem(is(equalTo(20.0)))));
        assertThat(temperatures.get(1), (Every.everyItem(is(equalTo(30.0)))));
    }

    @Test
    public void testLogsPeopleActivity() {
        this.conductor.run();
        Map<Integer, List<Activity>> activities = this.activityDataPoint.getRecord();
        for (List<Activity> personTimeSeries : activities.values()) {
            assertThat(personTimeSeries, hasSize(NUMBER_STEPS));
        }
        assertThat(activities.get(0), (Every.everyItem(is(equalTo(Activity.HOME)))));
        assertThat(activities.get(1), (Every.everyItem(is(equalTo(Activity.NOT_AT_HOME)))));
        assertThat(activities.get(2), (Every.everyItem(is(equalTo(Activity.SLEEP_AT_HOME)))));
    }

    @Test
    public void writesTemperatureToDatabase() throws IOException, SQLException, ClassNotFoundException {
        this.conductor.run();
        String filename = this.tempFile.getCanonicalPath();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
        Statement stat = conn.createStatement();

        Map<Integer, List<Double>> values = new HashMap<>();
        values.put(0, new ArrayList<>());
        values.put(1, new ArrayList<>());
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", this.temperatureDataPoint.getName()));
        while (rs.next()) {
            values.get(rs.getInt(1)).add(rs.getDouble(2));
        }
        rs.close();
        conn.close();

        assertThat(values.get(0), (Every.everyItem(is(equalTo(20.0)))));
        assertThat(values.get(1), (Every.everyItem(is(equalTo(30.0)))));
    }

    @Test
    public void writesActivityToDatabase() throws IOException, SQLException, ClassNotFoundException {
        this.conductor.run();
        String filename = this.tempFile.getCanonicalPath();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
        Statement stat = conn.createStatement();

        Map<Integer, List<String>> values = new HashMap<>();
        values.put(0, new ArrayList<>());
        values.put(1, new ArrayList<>());
        values.put(2, new ArrayList<>());
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", this.activityDataPoint.getName()));
        while (rs.next()) {
            values.get(rs.getInt(1)).add(rs.getString(2));
        }
        rs.close();
        conn.close();

        assertThat(values.get(0), (Every.everyItem(is(equalTo("HOME")))));
        assertThat(values.get(1), (Every.everyItem(is(equalTo("NOT_AT_HOME")))));
        assertThat(values.get(2), (Every.everyItem(is(equalTo("SLEEP_AT_HOME")))));
    }
}
