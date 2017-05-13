package uk.ac.cam.eeci.energyagents.test.integration;

import uk.ac.cam.eeci.framework.Conductor;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import uk.ac.cam.eeci.energyagents.*;
import uk.ac.cam.eeci.energyagents.strategy.ClimateChangingControlStrategy;
import uk.ac.cam.eeci.energyagents.test.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Category(IntegrationTest.class)
public class TestUnheatedBuildings {

    private final static long SEED = 123456789L;
    private final static String MARKOV_CSV_FILE_NAME = "test-markov-chain.csv";
    private final static Duration TIME_STEP_SIZE = Duration.ofHours(12);
    private final static ZoneId TIME_ZONE = ZoneOffset.UTC;
    private final static ZonedDateTime INITIAL_TIME = ZonedDateTime.of(2017, 2, 13, 0, 0, 0, 0, TIME_ZONE);
    private final static int NUMBER_TIME_STEPS = 8;
    private final static double INITIAL_DWELLING_TEMPERATURE = 10.0;
    private final static double CONSTANT_OUTDOOR_TEMPERATURE = 24.0;
    private final static double EPSILON = 0.1;
    private final static double ACTIVE_METABOLIC_RATE = 100;
    private final static double PASSIVE_METABOLIC_RATE = 30;
    private Conductor conductor;
    private List<Dwelling> dwellings;
    private List<DwellingReference> dwellingReferences;
    private List<PersonReference> peopleReferences;
    private Environment environment = mock(Environment.class);
    private EnvironmentReference environmentReference;

    @Before
    public void setUp() throws IOException, ExecutionException, InterruptedException {
        Utils.resetScienceOS();
        when(this.environment.getCurrentTemperature())
                .thenReturn(CONSTANT_OUTDOOR_TEMPERATURE);
        this.environmentReference = new EnvironmentReference(this.environment);
        this.dwellings = this.createDwellings();
        this.dwellingReferences = this.dwellings
                .stream()
                .map(DwellingReference::new)
                .collect(Collectors.toList());
        this.peopleReferences = this.createPeopleReferences(this.dwellingReferences);

        this.conductor = new Conductor(new CitySimulation(
                this.dwellingReferences,
                new HashSet<>(this.peopleReferences),
                new EnvironmentReference(this.environment),
                null,
                INITIAL_TIME,
                TIME_STEP_SIZE,
                NUMBER_TIME_STEPS) {
        });
    }

    private List<Dwelling> createDwellings() {
        List<Dwelling> dwellings = new ArrayList<>();
        double floorArea = 100;
        for (int i = 0; i < 10; i++) {
            Dwelling d = new Dwelling(165000 * floorArea, 2.5 * floorArea, floorArea,
                3, 0.19, 0.26, 0.12, 0.40, 1.95,
                0.91, 0.65, 0,
                INITIAL_DWELLING_TEMPERATURE, INITIAL_TIME, TIME_STEP_SIZE,
                new HeatingControlStrategyReference(new ClimateChangingControlStrategy(Double.POSITIVE_INFINITY)),
                this.environmentReference);
            dwellings.add(d);
        }
        return dwellings;
    }

    private List<PersonReference> createPeopleReferences(List<DwellingReference> dwellings) throws IOException {
        List<PersonReference> people = new ArrayList<>();
        Random randomNumberGenerator = new Random(SEED);
        for (int i = 0; i < 20; i++) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream(MARKOV_CSV_FILE_NAME);
            Reader in = new InputStreamReader(is);
            int dwellingReference = randomNumberGenerator.nextInt(dwellings.size());
            Person p = new Person(MarkovChainReader.readMarkovChainFromFile(in, TIME_STEP_SIZE, TIME_ZONE),
                    ACTIVE_METABOLIC_RATE, PASSIVE_METABOLIC_RATE, Person.Activity.NOT_AT_HOME, INITIAL_TIME,
                    TIME_STEP_SIZE, this.dwellingReferences.get(dwellingReference), new Random(SEED));
            PersonReference pRef = new PersonReference(p);
            people.add(pRef);
        }
        return people;
    }

    /**
     * Dwellings are not heated and hence it is expected that their internal temperature equals the
     * outdoor temperature after the short simulation.
     */
    @Test
    public void testDwellingTemperatureApproachesOutdoorTemperature() {
        this.conductor.run();
        assertThat(this.dwellings.get(0).getCurrentAirTemperature(), is(closeTo(CONSTANT_OUTDOOR_TEMPERATURE, EPSILON)));
    }

}
