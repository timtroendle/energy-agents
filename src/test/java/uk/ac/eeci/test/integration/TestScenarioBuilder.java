package uk.ac.eeci.test.integration;

import io.improbable.scienceos.Conductor;
import org.hamcrest.core.Every;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.ac.eeci.CitySimulation;
import uk.ac.eeci.ScenarioBuilder;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.ac.eeci.test.integration.Utils.resetScienceOS;

@Category(IntegrationTest.class)
public class TestScenarioBuilder {

    // constants from the input file
    private final static String INPUT_PATH = "test-scenario.db";
    private final static int NUMBER_DWELLINGS = 100;
    private final static int NUMBER_PEOPLE = 200;
    private final static int NUMBER_TIME_STEPS = 90;

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
    public void resultContainsCompleteDwellingTemperatureRecord() throws ClassNotFoundException, SQLException, IOException {
        this.citySimulation = ScenarioBuilder.readScenario(this.inputURL.getPath(), this.tempOutPutFile.getCanonicalPath());
        new Conductor(this.citySimulation).run();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempOutPutFile.getCanonicalPath()));
        Statement stat = conn.createStatement();

        Map<Integer, Integer> values = new HashMap<>();
        for (int i = 0; i < NUMBER_DWELLINGS; i++) {
            values.put(i, 0);
        }
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", ScenarioBuilder.TEMPERATURE_DATA_POINT_NAME));
        while (rs.next()) {
            int dwellingId = rs.getInt("id");
            values.put(dwellingId, values.get(dwellingId) + 1);
        }
        rs.close();
        conn.close();

        assertThat(values.values(), (Every.everyItem(is(equalTo(NUMBER_TIME_STEPS)))));
    }

    @Test
    public void resultContainsCompleteActivityRecord() throws ClassNotFoundException, SQLException, IOException {
        this.citySimulation = ScenarioBuilder.readScenario(this.inputURL.getPath(), this.tempOutPutFile.getCanonicalPath());
        new Conductor(this.citySimulation).run();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempOutPutFile.getCanonicalPath()));
        Statement stat = conn.createStatement();

        Map<Integer, Integer> values = new HashMap<>();
        for (int i = 0; i < NUMBER_PEOPLE; i++) {
            values.put(i, 0);
        }
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", ScenarioBuilder.ACTIVITY_DATA_POINT_NAME));
        while (rs.next()) {
            int personId = rs.getInt("id");
            values.put(personId, values.get(personId) + 1);
        }
        rs.close();
        conn.close();

        assertThat(values.values(), (Every.everyItem(is(equalTo(NUMBER_TIME_STEPS)))));
    }

}
