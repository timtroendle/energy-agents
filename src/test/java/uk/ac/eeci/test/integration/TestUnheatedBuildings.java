package uk.ac.eeci.test.integration;

import io.improbable.scienceos.Conductor;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import uk.ac.eeci.*;
import uk.ac.eeci.strategy.ClimateChangingControlStrategy;
import uk.ac.eeci.Person.Activity;

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
    private final static Duration TIME_STEP_SIZE = Duration.ofMinutes(10);
    private final static ZoneId TIME_ZONE = ZoneOffset.UTC;
    private final static ZonedDateTime INITIAL_TIME = ZonedDateTime.of(2017, 2, 13, 15, 20, 0, 0, TIME_ZONE);
    private final static double INITIAL_DWELLING_TEMPERATURE = 22.0;
    private final static double CONSTANT_OUTDOOR_TEMPERATURE = 24.0;
    private final static double EPSILON = 0.1;
    private Conductor conductor;
    private List<Dwelling> dwellings;
    private List<DwellingReference> dwellingReferences;
    private List<PersonReference> peopleReferences;

    @Before
    public void setUp() throws IOException, ExecutionException, InterruptedException {
        this.dwellings = this.createDwellings();
        this.dwellingReferences = this.dwellings
                .stream()
                .map(DwellingReference::new)
                .collect(Collectors.toList());
        this.peopleReferences = this.createPeopleReferences(this.dwellingReferences);

        this.conductor = new Conductor(new CitySimulation(this.dwellingReferences,
                                                          new HashSet<>(this.peopleReferences),
                                                          CONSTANT_OUTDOOR_TEMPERATURE,
                                                          null, INITIAL_TIME, TIME_STEP_SIZE) {
        });
    }

    private List<Dwelling> createDwellings() {
        List<Dwelling> dwellings = new ArrayList<>();
        double conditionedFloorArea = 100;
        for (int i = 0; i < 10; i++) {
            Dwelling d = new Dwelling(165000 * conditionedFloorArea, 2000, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, INITIAL_DWELLING_TEMPERATURE,
                    conditionedFloorArea, TIME_STEP_SIZE, new ClimateChangingControlStrategy(0, 100));
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
            Person p = new Person(MarkovChainReader.readMarkovChainFromFile(in, TIME_STEP_SIZE, SEED, TIME_ZONE),
                    Activity.NOT_AT_HOME, INITIAL_TIME, TIME_STEP_SIZE, null,
                    this.dwellingReferences.get(dwellingReference));
            PersonReference pRef = new PersonReference(p);
            p.setPersonReference(pRef); // FIXME noooooo!
            people.add(pRef);
        }
        return people;
    }

    /**
     * Conductor currently runs 100 steps, i.e. 16.6h.
     * Dwellings are not heated and have a high heat transmission, hence it is expected
     * that their internal temperature equals the outdoor temperature after 16.6h.
     */
    @Test
    public void testDwellingTemperatureApproachesOutdoorTemperature() {
        this.conductor.run();
        assertThat(this.dwellings.get(0).getTemperature(), is(closeTo(CONSTANT_OUTDOOR_TEMPERATURE, EPSILON)));
    }

}
