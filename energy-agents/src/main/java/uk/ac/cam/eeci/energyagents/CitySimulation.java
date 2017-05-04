package uk.ac.cam.eeci.energyagents;

import io.improbable.scienceos.EndSimulationException;
import io.improbable.scienceos.ISimulation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Defines an entire simulation run.
 */
public class CitySimulation implements ISimulation {

    private final static Logger logger = LogManager.getLogger(CitySimulation.class.getName());

    public final static String METADATA_KEY_SIM_START = "startOfSimulation";
    public final static String METADATA_KEY_SIM_END = "endOfSimulation";
    public final static String METADATA_KEY_SIM_DURATION = "durationOfSimulation";
    public final static String METADATA_KEY_MODEL_VERSION = "modelVersion";
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    private final static String METADATA_FILE_NAME = "/metadata.properties";
    private final static String MODEL_VERSION_KEY = "model.version";

    private final Set<DwellingReference> dwellings;
    private final Set<PersonReference> people;
    private final EnvironmentReference environment;
    private final DataLoggerReference dataLoggerReference;
    private final Duration timeStepSize;
    private ZonedDateTime currentTime;
    private int remainingSteps;
    private LocalDateTime simulationStartTime = LocalDateTime.MIN;

    /**
     * @param dwellings The set of all dwellings in the city.
     * @param people The set of all people in the city.
     */
    public CitySimulation(Collection<DwellingReference> dwellings, Collection<PersonReference> people,
                          EnvironmentReference environment, DataLoggerReference dataLoggerReference,
                          ZonedDateTime startTime, Duration timeStepSize, int numberSteps) {
        this.dwellings = new HashSet<>(dwellings);
        this.people = new HashSet<>(people);
        this.environment = environment;
        this.dataLoggerReference = dataLoggerReference;
        this.currentTime = startTime;
        this.timeStepSize = timeStepSize;
        this.remainingSteps = numberSteps;
    }

    @Override
    public void step() throws InterruptedException, ExecutionException, EndSimulationException {
        if (simulationStartTime == LocalDateTime.MIN) // FIXME should be done in a currently non-existing startup hook
            this.simulationStartTime = LocalDateTime.now();
        if (this.remainingSteps > 0) {
            this.performStep();
            this.remainingSteps -= 1;
        } else {
            throw new EndSimulationException();
        }
    }

    private void performStep() throws ExecutionException, InterruptedException, EndSimulationException {
        logger.debug(String.format("Simulating step at time %s.", this.currentTime));
        List<CompletableFuture<Void>> peopleSteps = new ArrayList<>();
        for (PersonReference person : this.people) {
            peopleSteps.add(person.step());
        }
        CompletableFuture<Void>[] array = new CompletableFuture[peopleSteps.size()];
        array = peopleSteps.toArray(array);

        CompletableFuture.allOf(array).get();

        List<CompletableFuture<Void>> dwellingSteps = new ArrayList<>();
        for (DwellingReference dwelling : this.dwellings) {
            dwellingSteps.add(dwelling.step());
        }
        CompletableFuture<Void>[] dStepsArray = new CompletableFuture[dwellingSteps.size()];
        dStepsArray = dwellingSteps.toArray(dStepsArray);
        CompletableFuture.allOf(dStepsArray).get();

        this.environment.step().get();
        this.currentTime = this.currentTime.plus(this.timeStepSize);
        if (this.dataLoggerReference != null) {
            this.dataLoggerReference.step(this.currentTime).get();
        }
    }

    @Override
    public void stop() {
        if (this.dataLoggerReference != null) {
            try {
                logger.info("Attempting to write results to disk.");
                this.dataLoggerReference.write(this.collectMetadata()).get();
            } catch (InterruptedException|ExecutionException e) {
                e.printStackTrace(); // FIXME proper error handling
            }
        }
    }

    private HashMap<String, String> collectMetadata() {
        LocalDateTime simEndTime = LocalDateTime.now();
        Duration simDuration = Duration.ofSeconds(this.simulationStartTime.until(simEndTime, ChronoUnit.SECONDS));

        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_KEY_SIM_START, this.simulationStartTime.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER));
        metadata.put(METADATA_KEY_SIM_END, simEndTime.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER));
        metadata.put(METADATA_KEY_SIM_DURATION, simDuration.toString());
        metadata.put(METADATA_KEY_MODEL_VERSION, inferModelVersion());

        return metadata;
    }

    public static String inferModelVersion() {
        final Properties properties = new Properties();
        try (final InputStream stream = CitySimulation.class.getResourceAsStream(METADATA_FILE_NAME)) {
            properties.load(stream);
            return properties.get(MODEL_VERSION_KEY).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
