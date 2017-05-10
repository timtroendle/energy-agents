package uk.ac.cam.eeci.energyagents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.cam.eeci.energyagents.strategy.HeatingControlStrategyFactory;

import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ScenarioBuilder {

    private final static Logger LOGGER = LogManager.getLogger(ScenarioBuilder.class.getName());

    public final static String SQL_TABLES_PARAMETERS = "parameters";
    public final static String SQL_TABLES_ENVIRONMENT = "environment";
    public final static String SQL_TABLES_DWELLINGS = "dwellings";
    public final static String SQL_TABLES_MARKOV_CHAINS = "markovChains";
    public final static String SQL_TABLES_PEOPLE = "people";

    public final static String SQL_COLUMNS_PAR_INITIAL_DATETIME = "initialDateTime";
    public final static String SQL_COLUMNS_PAR_TIME_STEP_SIZE = "timeStepSize_in_min";
    public final static String SQL_COLUMNS_PAR_NUMBER_TIME_STEPS = "numberTimeSteps";
    public final static String SQL_COLUMNS_PAR_LOG_THERMAL_POWER = "logThermalPower";
    public final static String SQL_COLUMNS_PAR_LOG_TEMPERATURE = "logTemperature";
    public final static String SQL_COLUMNS_PAR_LOG_ACTIVITY = "logActivity";
    public final static String SQL_COLUMNS_PAR_SET_POINT_WHILE_HOME = "setPointWhileHome";
    public final static String SQL_COLUMNS_PAR_SET_POINT_WHILE_ASLEEP = "setPointWhileAsleep";
    public final static String SQL_COLUMNS_PAR_WAKE_UP_TIME = "wakeUpTime";
    public final static String SQL_COLUMNS_PAR_LEAVE_HOME_TIME = "leaveHomeTime";
    public final static String SQL_COLUMNS_PAR_COME_HOME_TIME = "comeHomeTime";
    public final static String SQL_COLUMNS_PAR_BED_TIME = "bedTime";
    public final static String SQL_COLUMNS_ENV_INDEX = "index";
    public final static String SQL_COLUMNS_ENV_TEMPERATURE = "temperature";
    public final static String SQL_COLUMNS_DW_INDEX = "index";
    public final static String SQL_COLUMNS_DW_HEAT_MASS_CAPACITY = "heatMassCapacity";
    public final static String SQL_COLUMNS_DW_HEAT_TRANSMISSION = "heatTransmission";
    public final static String SQL_COLUMNS_DW_INITIAL_TEMPERATURE = "initialTemperature";
    public final static String SQL_COLUMNS_DW_MAX_HEATING_POWER = "maxHeatingPower";
    public final static String SQL_COLUMNS_DW_CONDITIONED_FLOOR_AREA = "conditionedFloorArea";
    public final static String SQL_COLUMNS_DW_HEATING_CONTROL_STRATEGY = "heatingControlStrategy";
    public final static String SQL_COLUMNS_PPL_DWELLING_ID = "dwellingId";
    public final static String SQL_COLUMNS_PPL_MARKOV_ID = "markovChainId";
    public final static String SQL_COLUMNS_PPL_INITIAL_ACTIVITY = "initialActivity";
    public final static String SQL_COLUMNS_PPL_INDEX = "index";
    public final static String SQL_COLUMNS_PPL_RANDOM_SEED = "randomSeed";
    public final static String SQL_COLUMNS_PPL_ACTIVE_METABOLIC_RATE = "activeMetabolicRate";
    public final static String SQL_COLUMNS_PPL_PASSIVE_METABOLIC_RATE = "passiveMetabolicRate";
    public final static String SQL_COLUMNS_MARKOVS_INDEX = "index";
    public final static String SQL_COLUMNS_MARKOVS_TABLENAME = "tablename";
    public final static String SQL_COLUMNS_MARKOV_DAY = "day";
    public final static String SQL_COLUMNS_MARKOV_TIME_OF_DAY = "time";
    public final static String SQL_COLUMNS_MARKOV_FROM = "fromActivity";
    public final static String SQL_COLUMNS_MARKOV_TO = "toActivity";
    public final static String SQL_COLUMNS_MARKOV_PROBABILITY = "probability";

    public final static String TEMPERATURE_DATA_POINT_NAME = "temperature";
    public final static String ACTIVITY_DATA_POINT_NAME = "activity";
    public final static String THERMAL_POWER_DATA_POINT_NAME = "thermalPower";

    public final static ZoneOffset TIME_ZONE = ZoneOffset.UTC;

    private static class SimulationParameter {

        private final ZonedDateTime initialTime;
        private final Duration timeStepSize;
        private final int numberTimeSteps;
        private final boolean logThermalPower;
        private final boolean logTemperature;
        private final boolean logActivity;

        private SimulationParameter(ZonedDateTime initialTime, Duration timeStepSize, int numberTimeSteps,
                                    boolean logThermalPower, boolean logTemperature, boolean logActivity) {
            this.initialTime = initialTime;
            this.timeStepSize = timeStepSize;
            this.numberTimeSteps = numberTimeSteps;
            this.logThermalPower = logThermalPower;
            this.logTemperature = logTemperature;
            this.logActivity = logActivity;
        }
    }

    public static CitySimulation readScenario(String databasePath, String outputPath) throws IOException {
        CitySimulation simulation = null;
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", databasePath));
            simulation = readScenario(conn, databasePath, outputPath);
        } catch (ClassNotFoundException|SQLException|IOException ex) {
            LOGGER.error(String.format("Failed to read scenario from %s.", databasePath), ex);
            throw new IOException("Failed to read scenario");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return simulation;
    }

    private static CitySimulation readScenario(Connection con, String inputPath, String outputPath)
            throws SQLException, IOException {
        SimulationParameter parameters = readSimulationParameters(con);
        HeatingControlStrategyFactory heatingControlStrategyFactory = readHeatingControlStrategyFactory(con);
        EnvironmentReference environmentReference = readEnvironment(con, parameters.timeStepSize);
        Map<Integer, DwellingReference> dwellingReferences = readDwellings(con, parameters, environmentReference,
                heatingControlStrategyFactory);
        Map<Integer, PersonReference> peopleReferences = readPeople(con, dwellingReferences, parameters);
        DataLoggerReference dataLoggerReference = createDataLogger(dwellingReferences, peopleReferences, parameters,
                inputPath, outputPath);
        return new CitySimulation(
                dwellingReferences.values(),
                peopleReferences.values(),
                environmentReference,
                dataLoggerReference,
                parameters.initialTime,
                parameters.timeStepSize,
                parameters.numberTimeSteps
        );
    }

    private static ZonedDateTime readTimeStamp(ResultSet rs, String columnName) throws SQLException {
        return rs.getTimestamp(columnName, Calendar.getInstance(TimeZone.getTimeZone("UTC")))
                .toInstant()
                .atZone(TIME_ZONE);
    }

    private static LocalTime readLocalTime(ResultSet rs, String columnName) throws SQLException, IOException {
        try {
            return LocalTime.parse(rs.getString(columnName), DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException e) {
            throw new IOException(e);
        }
    }

    private static HeatingControlStrategyFactory.ControlStrategyType readControlStrategyType(ResultSet rs, String columnName) throws SQLException {
        return HeatingControlStrategyFactory.ControlStrategyType.valueOf(rs.getString(columnName));
    }

    private static EnvironmentReference readEnvironment(Connection conn, Duration timeStepSize) throws SQLException {
        TimeSeries<Double> temperatureTimeSeries = new TimeSeries<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_ENVIRONMENT));
        while (rs.next()) {
            ZonedDateTime timeStamp = readTimeStamp(rs, SQL_COLUMNS_ENV_INDEX);
            Double value = rs.getDouble(SQL_COLUMNS_ENV_TEMPERATURE);
            temperatureTimeSeries.add(timeStamp, value);
        }
        rs.close();
        Environment env = new Environment(temperatureTimeSeries, timeStepSize);
        return new EnvironmentReference(env);
    }

    private static Map<Integer, DwellingReference> readDwellings(Connection conn, SimulationParameter parameters,
                                                                 EnvironmentReference env,
                                                                 HeatingControlStrategyFactory controlStrategyFactory)
            throws SQLException {
        Map<Integer, DwellingReference> dwellings = new HashMap<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_DWELLINGS));
        while (rs.next()) {
            dwellings.put(
                    rs.getInt(SQL_COLUMNS_DW_INDEX),
                    new DwellingReference(new Dwelling(
                        rs.getDouble(SQL_COLUMNS_DW_HEAT_MASS_CAPACITY),
                        rs.getDouble(SQL_COLUMNS_DW_HEAT_TRANSMISSION),
                        rs.getDouble(SQL_COLUMNS_DW_MAX_HEATING_POWER),
                        rs.getDouble(SQL_COLUMNS_DW_INITIAL_TEMPERATURE),
                        rs.getDouble(SQL_COLUMNS_DW_CONDITIONED_FLOOR_AREA),
                        parameters.initialTime,
                        parameters.timeStepSize,
                        new HeatingControlStrategyReference(controlStrategyFactory.build(
                                readControlStrategyType(rs, SQL_COLUMNS_DW_HEATING_CONTROL_STRATEGY))
                        ),
                        env
                    ))
            );
        }
        rs.close();
        return dwellings;
    }

    private static Map<Integer, PersonReference> readPeople(Connection conn, Map<Integer, DwellingReference> dwellings,
                                                    SimulationParameter parameters) throws SQLException, IOException {
        Map<Integer, HeterogeneousMarkovChain<Person.Activity>> markovChains = readMarkovChains(conn, parameters);
        Map<Integer, Person> people = new HashMap<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_PEOPLE));
        while (rs.next()) {
            int personId = rs.getInt(SQL_COLUMNS_PPL_INDEX);
            int homeId = rs.getInt(SQL_COLUMNS_PPL_DWELLING_ID);
            int markovChainId = rs.getInt(SQL_COLUMNS_PPL_MARKOV_ID);
            int randomSeed = rs.getInt(SQL_COLUMNS_PPL_RANDOM_SEED);
            int activeMetabolicRate = rs.getInt(SQL_COLUMNS_PPL_ACTIVE_METABOLIC_RATE);
            int passiveMetabolicRate = rs.getInt(SQL_COLUMNS_PPL_PASSIVE_METABOLIC_RATE);
            Person.Activity initialActivity = Person.Activity.valueOf(rs.getString(SQL_COLUMNS_PPL_INITIAL_ACTIVITY));
            people.put(
                    personId,
                    new Person(
                        markovChains.get(markovChainId),
                        activeMetabolicRate,
                        passiveMetabolicRate,
                        initialActivity,
                        parameters.initialTime,
                        parameters.timeStepSize,
                        dwellings.get(homeId),
                        new Random(randomSeed)
            ));
        }
        rs.close();
        Map<Integer, PersonReference> peopleReference = new HashMap<>();
        for (Map.Entry<Integer, Person> entry : people.entrySet()) {
            Person person = entry.getValue();
            PersonReference ref = new PersonReference(person);
            peopleReference.put(entry.getKey(), ref);
        }
        return peopleReference;
    }

    private static Map<Integer, HeterogeneousMarkovChain<Person.Activity>> readMarkovChains(Connection conn,
                                                                                            SimulationParameter parameters)
            throws SQLException, IOException {
        Map<Integer, String> markovChainTableNames = new HashMap<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_MARKOV_CHAINS));
        while (rs.next()) {
            markovChainTableNames.put(
                    rs.getInt(SQL_COLUMNS_MARKOVS_INDEX),
                    rs.getString(SQL_COLUMNS_MARKOVS_TABLENAME));
        }
        rs.close();
        Map<Integer, HeterogeneousMarkovChain<Person.Activity>> markovChains = new HashMap<>();
        for (Map.Entry<Integer, String> entry : markovChainTableNames.entrySet()) {
            markovChains.put(
                    entry.getKey(),
                    readMarkovChain(conn, entry.getValue(), parameters
                    )
            );
        }
        return markovChains;
    }

    private static HeterogeneousMarkovChain<Person.Activity> readMarkovChain(Connection conn, String tablename,
                                                                             SimulationParameter parameters)
            throws SQLException, IOException {
        List<MarkovChainReader.MarkovChainEntry> entries = new ArrayList<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", tablename));
        while (rs.next()) {
            entries.add(new MarkovChainReader.MarkovChainEntry(
                    rs.getString(SQL_COLUMNS_MARKOV_DAY),
                    readLocalTime(rs, SQL_COLUMNS_MARKOV_TIME_OF_DAY),
                    Person.Activity.valueOf(rs.getString(SQL_COLUMNS_MARKOV_FROM)),
                    Person.Activity.valueOf(rs.getString(SQL_COLUMNS_MARKOV_TO)),
                    rs.getDouble(SQL_COLUMNS_MARKOV_PROBABILITY)
            ));
        }
        rs.close();
        return MarkovChainReader.buildMarkovChainFromEntries(entries, parameters.timeStepSize,
                TIME_ZONE);
    }

    private static SimulationParameter readSimulationParameters(Connection conn) throws SQLException {
        List<SimulationParameter> parameters = new ArrayList<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_PARAMETERS));
        while (rs.next()) {
            parameters.add(new SimulationParameter(
                    readTimeStamp(rs, SQL_COLUMNS_PAR_INITIAL_DATETIME),
                    Duration.ofMinutes(rs.getInt(SQL_COLUMNS_PAR_TIME_STEP_SIZE)),
                    rs.getInt(SQL_COLUMNS_PAR_NUMBER_TIME_STEPS),
                    rs.getBoolean(SQL_COLUMNS_PAR_LOG_THERMAL_POWER),
                    rs.getBoolean(SQL_COLUMNS_PAR_LOG_TEMPERATURE),
                    rs.getBoolean(SQL_COLUMNS_PAR_LOG_ACTIVITY)
            ));
        }
        rs.close();
        if (parameters.size() < 1) {
            String msg = String.format("Simulation parameter missing in table %s.", SQL_TABLES_PARAMETERS);
            throw new SQLException(msg);
        }
        return parameters.get(0); // there could be more, but at the moment don't care
    }

    private static HeatingControlStrategyFactory readHeatingControlStrategyFactory(Connection conn)
            throws SQLException, IOException {
        List<HeatingControlStrategyFactory> factories = new ArrayList<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_PARAMETERS));
        while (rs.next()) {
            factories.add(new HeatingControlStrategyFactory(
                    rs.getDouble(SQL_COLUMNS_PAR_SET_POINT_WHILE_HOME),
                    rs.getDouble(SQL_COLUMNS_PAR_SET_POINT_WHILE_ASLEEP),
                    readLocalTime(rs, SQL_COLUMNS_PAR_WAKE_UP_TIME),
                    readLocalTime(rs, SQL_COLUMNS_PAR_LEAVE_HOME_TIME),
                    readLocalTime(rs, SQL_COLUMNS_PAR_COME_HOME_TIME),
                    readLocalTime(rs, SQL_COLUMNS_PAR_BED_TIME),
                    TIME_ZONE
            ));
        }
        rs.close();
        if (factories.size() < 1) {
            String msg = String.format("Simulation parameter missing in table %s.", SQL_TABLES_PARAMETERS);
            throw new SQLException(msg);
        }
        return factories.get(0); // there could be more, but at the moment don't care
    }

    private static DataLoggerReference createDataLogger(Map<Integer, DwellingReference> dwellings,
                                                        Map<Integer, PersonReference> people,
                                                        SimulationParameter parameters,
                                                        String inputPath, String outputPath) {
        Set<DataPoint> dataPoints = new HashSet<>();
        if (parameters.logTemperature) {
            dataPoints.add(new DataPoint<>(
                    TEMPERATURE_DATA_POINT_NAME,
                    dwellings,
                    (DwellingReference::getCurrentTemperature)
            ));
        }
        if (parameters.logThermalPower) {
            dataPoints.add(new DataPoint<>(
                    THERMAL_POWER_DATA_POINT_NAME,
                    dwellings,
                    (DwellingReference::getCurrentThermalPower)
            ));
        }
        if (parameters.logActivity) {
            dataPoints.add(new DataPoint<>(
                    ACTIVITY_DATA_POINT_NAME,
                    people,
                    (PersonReference::getCurrentActivity)
            ));
        }
        DataLogger dataLogger = new DataLogger(
                dataPoints.stream().map(DataPointReference::new).collect(Collectors.toSet()),
                inputPath,
                outputPath
        );
        return new DataLoggerReference(dataLogger);
    }
}
