package uk.ac.eeci.test;

import io.improbable.scienceos.EndSimulationException;
import io.improbable.scienceos.Reference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.eeci.*;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class TestCitySimulation {

    private final static ZonedDateTime INITIAL_DATETIME = ZonedDateTime.of(2017, 2, 22, 11, 0, 0, 0, ZoneOffset.UTC);
    private final static Duration TIME_STEP_SIZE = Duration.ofMinutes(10);
    private Dwelling dwelling = mock(Dwelling.class);
    private Person person = mock(Person.class);
    private Environment environment = mock(Environment.class);
    private DataLogger dataLogger = mock(DataLogger.class);
    private CitySimulation citySimulation;

    @Before
    public void setUp() {
        when(this.dwelling.step()).thenReturn(CompletableFuture.completedFuture(null));
        when(this.dataLogger.step(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(this.dataLogger.write(any())).thenReturn(CompletableFuture.completedFuture(null));
        this.citySimulation = new CitySimulation(
                Arrays.asList(new DwellingReference(this.dwelling)),
                Arrays.asList(new PersonReference(this.person)),
                new EnvironmentReference(this.environment),
                new DataLoggerReference(this.dataLogger),
                INITIAL_DATETIME,
                TIME_STEP_SIZE,
                100);
        Reference.pool.setCurrentExecutor(Reference.pool.main);
    }

    @Test
    public void stepsDwelling() throws InterruptedException, ExecutionException, EndSimulationException {
        this.citySimulation.step();
        verify(this.dwelling, times(1)).step();
    }

    @Test
    public void stepsPerson() throws InterruptedException, ExecutionException, EndSimulationException {
        this.citySimulation.step();
        verify(this.person, times(1)).step();
    }

    @Test
    public void stepsDataLogger() throws InterruptedException, ExecutionException, EndSimulationException {
        this.citySimulation.step();
        verify(this.dataLogger, times(1)).step(any());
    }

    @Test
    public void stepsEnvironment() throws InterruptedException, ExecutionException, EndSimulationException {
        this.citySimulation.step();
        verify(this.environment, times(1)).step();
    }

    @Test
    public void commandsWriteOfSimulationDataOnStop() throws InterruptedException, ExecutionException, EndSimulationException {
        this.citySimulation.step();
        verify(this.dataLogger, never()).write(any());
        this.citySimulation.stop();
        verify(this.dataLogger, times(1)).write(any());
    }

    @Test
    public void gathersSimulationMetadata() throws InterruptedException, ExecutionException, EndSimulationException {
        this.citySimulation.step(); // necessary to 'start' simulation as there is no startup hook
        this.citySimulation.stop();
        ArgumentCaptor<HashMap<String, String>> argument = ArgumentCaptor.forClass(HashMap.class);
        verify(this.dataLogger).write(argument.capture());
        assertThat(argument.getValue().keySet(), containsInAnyOrder(
                CitySimulation.METADATA_KEY_SIM_START,
                CitySimulation.METADATA_KEY_SIM_END,
                CitySimulation.METADATA_KEY_SIM_DURATION,
                CitySimulation.METADATA_KEY_MODEL_VERSION
        ));
    }
}
