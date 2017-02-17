package uk.ac.eeci.test.integration;

import io.improbable.scienceos.Conductor;
import io.improbable.scienceos.EndSimulationException;
import org.hamcrest.core.Every;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.ac.eeci.*;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    private final static ZoneId TIME_ZONE = ZoneOffset.UTC;
    private final static double CONSTANT_OUTDOOR_TEMPERATURE = 24.0;
    private Conductor conductor;
    private Dwelling dwelling1 = mock(Dwelling.class);
    private Dwelling dwelling2 = mock(Dwelling.class);
    private Person person1 = mock(Person.class);
    private Person person2 = mock(Person.class);
    private Person person3 = mock(Person.class);
    private DataLogger dataLogger;


    private static class ShortSimulation extends CitySimulation {

        private int remainingSteps;

        public ShortSimulation(DwellingSetReference dwellings, Set<PersonReference> people,
                               DataLoggerReference dataLogger, double outdoorTemperature, int numberSteps) {
            super(dwellings, people, outdoorTemperature, dataLogger);
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
        DwellingSet dwellings = new DwellingSet(dwellingReferences);
        DwellingSetReference dwellingSetReference = new DwellingSetReference(dwellings);
        this.dataLogger = new DataLogger(dwellingSetReference);
        DataLoggerReference dataLoggerReference = new DataLoggerReference(this.dataLogger);

        this.conductor = new Conductor(new ShortSimulation(dwellingSetReference,
                new HashSet<>(peopleReferences), dataLoggerReference,
                CONSTANT_OUTDOOR_TEMPERATURE,
                NUMBER_STEPS) {
        });
    }

    private void initDwellings() {
        when(this.dwelling1.getTemperature()).thenReturn(20.0);
        when(this.dwelling2.getTemperature()).thenReturn(30.0);
    }

    private void initPeople() {
        when(this.person1.getCurrentActivity()).thenReturn(Person.Activity.HOME);
        when(this.person2.getCurrentActivity()).thenReturn(Person.Activity.NOT_AT_HOME);
        when(this.person3.getCurrentActivity()).thenReturn(Person.Activity.SLEEP_AT_HOME);
    }

    @Test
    public void testLogsDwellingTemperature() {
        this.conductor.run();
        Map<DwellingReference, List<Double>> temperatures = this.dataLogger.getTemperatureRecord();
        assertThat(temperatures.get(new DwellingReference(this.dwelling1)), (Every.everyItem(is(equalTo(20.0)))));
        assertThat(temperatures.get(new DwellingReference(this.dwelling2)), (Every.everyItem(is(equalTo(30.0)))));
    }
}
