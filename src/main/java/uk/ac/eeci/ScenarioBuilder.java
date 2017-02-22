package uk.ac.eeci;

import uk.ac.eeci.strategy.ClimateChangingControlStrategy;
import uk.ac.eeci.Person.Activity;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ScenarioBuilder {

    public final static String SQL_TABLES_PARAMETERS = "parameters";
    public final static String SQL_TABLES_ENVIRONMENT = "environment";
    public final static String SQL_TABLES_DWELLINGS = "dwellings";
    public final static String SQL_TABLES_MARKOV_CHAINS = "markovChains";
    public final static String SQL_TABLES_PEOPLE = "people";

    public final static String SQL_COLUMNS_PAR_INITIAL_DATETIME = "initialDateTime";
    public final static String SQL_COLUMNS_PAR_TIME_STEP_SIZE = "timeStepSize_in_min";
    public final static String SQL_COLUMNS_PAR_NUMBER_TIME_STEPS = "numberTimeSteps";
    public final static String SQL_COLUMNS_PAR_RANDOM_SEED = "randomSeed";
    public final static String SQL_COLUMNS_ENV_INDEX = "index";
    public final static String SQL_COLUMNS_ENV_TEMPERATURE = "temperature";
    public final static String SQL_COLUMNS_DW_INDEX = "index";
    public final static String SQL_COLUMNS_DW_HEAT_MASS_CAPACITY = "heatMassCapacity";
    public final static String SQL_COLUMNS_DW_HEAT_TRANSMISSION = "heatTransmission";
    public final static String SQL_COLUMNS_DW_INITIAL_TEMPERATURE = "initialTemperature";
    public final static String SQL_COLUMNS_DW_MAX_COOLING_POWER = "maxCoolingPower";
    public final static String SQL_COLUMNS_DW_MAX_HEATING_POWER = "maxHeatingPower";
    public final static String SQL_COLUMNS_DW_CONDITIONED_FLOOR_AREA = "conditionedFloorArea";
    public final static String SQL_COLUMNS_PPL_DWELLING_ID = "dwellingId";
    public final static String SQL_COLUMNS_PPL_MARKOV_ID = "markovChainId";
    public final static String SQL_COLUMNS_PPL_INITIAL_ACTIVITY = "initialActivity";
    public final static String SQL_COLUMNS_MARKOVS_INDEX = "index";
    public final static String SQL_COLUMNS_MARKOVS_TABLENAME = "tablename";
    public final static String SQL_COLUMNS_MARKOV_DAY = "day";
    public final static String SQL_COLUMNS_MARKOV_TIME_OF_DAY = "time";
    public final static String SQL_COLUMNS_MARKOV_FROM = "fromActivity";
    public final static String SQL_COLUMNS_MARKOV_TO = "toActivity";
    public final static String SQL_COLUMNS_MARKOV_PROBABILITY = "probability";

    public final static String TEMPERATURE_DATA_POINT_NAME = "temperature";
    public final static String ACTIVITY_DATA_POINT_NAME = "activity";

    public final static ZoneOffset TIME_ZONE = ZoneOffset.UTC;
    public final static HeatingControlStrategy HEATING_CONTROL_STRATEGY = new ClimateChangingControlStrategy(20, 26);
    // TODO heating strategy should come from input

    private static class SimulationParameter {

        private final ZonedDateTime initialTime;
        private final Duration timeStepSize;
        private final int numberTimeSteps;
        private final long randomSeed;

        private SimulationParameter(ZonedDateTime initialTime, Duration timeStepSize, int numberTimeSteps, long randomSeed) {
            this.initialTime = initialTime;
            this.timeStepSize = timeStepSize;
            this.numberTimeSteps = numberTimeSteps;
            this.randomSeed = randomSeed;
        }
    }

    public static CitySimulation readScenario(String databasePath, String outputPath) {
        CitySimulation simulation = null;
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", databasePath));
            simulation = readScenario(conn, outputPath);
        } catch (ClassNotFoundException|SQLException ex) {
            ex.printStackTrace();
            System.out.println(String.format("Failed to read scenario from %s.", databasePath));
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

    private static CitySimulation readScenario(Connection con, String outputPath)
            throws SQLException {
        SimulationParameter parameters = readSimulationParameters(con);
        EnvironmentReference environmentReference = readEnvironment(con, parameters.timeStepSize);
        List<DwellingReference> dwellingReferences = readDwellings(con, parameters.timeStepSize, environmentReference);
        List<PersonReference> peopleReferences = readPeople(con, dwellingReferences, parameters);
        DataLoggerReference dataLoggerReference = createDataLogger(dwellingReferences, peopleReferences, outputPath);
        return new CitySimulation(
                dwellingReferences,
                peopleReferences,
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

    private static LocalTime readLocalTime(ResultSet rs, String columnName) throws SQLException {
        //return rs.getTime(columnName, Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toLocalTime();
        return LocalTime.parse(rs.getString(columnName), DateTimeFormatter.ISO_LOCAL_TIME);
    }

    private static EnvironmentReference readEnvironment(Connection conn, Duration timeStepSize) throws SQLException {
        TimeSeries<Double> temperatureTimeSeries = new TimeSeries<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_ENVIRONMENT));
        List<Instant> timestamps = new ArrayList<>();
        while (rs.next()) {
            timestamps.add(rs.getTimestamp(SQL_COLUMNS_ENV_INDEX).toInstant());
            ZonedDateTime timeStamp = readTimeStamp(rs, SQL_COLUMNS_ENV_INDEX);
            Double value = rs.getDouble(SQL_COLUMNS_ENV_TEMPERATURE);
            temperatureTimeSeries.add(timeStamp, value);
        }
        rs.close();
        Environment env = new Environment(temperatureTimeSeries, timeStepSize);
        return new EnvironmentReference(env);
    }

    private static List<DwellingReference> readDwellings(Connection conn, Duration timeStepSize, EnvironmentReference env)
            throws SQLException {
        List<Dwelling> dwellings = new ArrayList<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_DWELLINGS));
        while (rs.next()) {
            dwellings.add(new Dwelling(
                    rs.getDouble(SQL_COLUMNS_DW_HEAT_MASS_CAPACITY),
                    rs.getDouble(SQL_COLUMNS_DW_HEAT_TRANSMISSION),
                    rs.getDouble(SQL_COLUMNS_DW_MAX_COOLING_POWER),
                    rs.getDouble(SQL_COLUMNS_DW_MAX_HEATING_POWER),
                    rs.getDouble(SQL_COLUMNS_DW_INITIAL_TEMPERATURE),
                    rs.getDouble(SQL_COLUMNS_DW_CONDITIONED_FLOOR_AREA),
                    timeStepSize,
                    HEATING_CONTROL_STRATEGY,
                    env
            ));
        }
        rs.close();
        return dwellings.stream().map(DwellingReference::new).collect(Collectors.toList());
    }

    private static List<PersonReference> readPeople(Connection conn, List<DwellingReference> dwellings,
                                                    SimulationParameter parameters) throws SQLException {
        List<HeterogeneousMarkovChain<Activity>> markovChains = readMarkovChains(conn, parameters);
        List<Person> people = new ArrayList<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_PEOPLE));
        while (rs.next()) {
            int homeId = rs.getInt(SQL_COLUMNS_PPL_DWELLING_ID);
            int markovChainId = rs.getInt((SQL_COLUMNS_PPL_MARKOV_ID));
            Activity initialActivity = Activity.valueOf(rs.getString(SQL_COLUMNS_PPL_INITIAL_ACTIVITY));
            people.add(new Person(
                    markovChains.get(markovChainId - 1),
                    initialActivity,
                    parameters.initialTime,
                    parameters.timeStepSize,
                    null,
                    dwellings.get(homeId - 1)
            ));
        }
        rs.close();
        List<PersonReference> peopleReference = people.stream().map(PersonReference::new).collect(Collectors.toList());
        for (int i = 0; i < people.size(); i++) {
            people.get(i).setPersonReference(peopleReference.get(i));
        }
        return peopleReference;
    }

    private static List<HeterogeneousMarkovChain<Activity>> readMarkovChains(Connection conn,
                                                                             SimulationParameter parameters) throws SQLException {
        List<String> markovChainTableNames = new ArrayList<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", SQL_TABLES_MARKOV_CHAINS));
        while (rs.next()) {
            markovChainTableNames.add(rs.getString(SQL_COLUMNS_MARKOVS_TABLENAME));
        }
        rs.close();
        List<HeterogeneousMarkovChain<Activity>> markovChains = new ArrayList<>();
        for (String markovChainTableName : markovChainTableNames) {
            markovChains.add(readMarkovChain(conn, markovChainTableName, parameters));
        }
        return markovChains;
    }

    private static HeterogeneousMarkovChain<Activity> readMarkovChain(Connection conn, String tablename,
                                                                      SimulationParameter parameters) throws SQLException {
        List<MarkovChainReader.MarkovChainEntry> entries = new ArrayList<>();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", tablename));
        while (rs.next()) {
            entries.add(new MarkovChainReader.MarkovChainEntry(
                    rs.getString(SQL_COLUMNS_MARKOV_DAY),
                    readLocalTime(rs, SQL_COLUMNS_MARKOV_TIME_OF_DAY),
                    Activity.valueOf(rs.getString(SQL_COLUMNS_MARKOV_FROM)),
                    Activity.valueOf(rs.getString(SQL_COLUMNS_MARKOV_TO)),
                    rs.getDouble(SQL_COLUMNS_MARKOV_PROBABILITY)
            ));
        }
        rs.close();



        return MarkovChainReader.buildMarkovChainFromEntries(entries, parameters.timeStepSize,
                parameters.randomSeed, TIME_ZONE);
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
                    rs.getLong(SQL_COLUMNS_PAR_RANDOM_SEED)
            ));
        }
        rs.close();
        if (parameters.size() < 1) {
            String msg = String.format("Simulation parameter missing in table %s.", SQL_TABLES_PARAMETERS);
            throw new SQLException(msg);
        }
        return parameters.get(0); // there could be more, but at the moment don't care
    }

    private static DataLoggerReference createDataLogger(List<DwellingReference> dwellings, List<PersonReference> people,
                                                        String outputPath) {
        Set<DataPoint> dataPoints = new HashSet<>();
        dataPoints.add(new DataPoint<>(
                TEMPERATURE_DATA_POINT_NAME,
                new ArrayList<>(dwellings),
                (DwellingReference::getTemperature)
        ));
        dataPoints.add(new DataPoint<>(
                ACTIVITY_DATA_POINT_NAME,
                new ArrayList<>(people),
                (PersonReference::getCurrentActivity)
        ));
        DataLogger dataLogger = new DataLogger(
                dataPoints.stream().map(DataPointReference::new).collect(Collectors.toSet()),
                outputPath
        );
        return new DataLoggerReference(dataLogger);
    }
}
