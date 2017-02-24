package uk.ac.eeci.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.eeci.DataLogger;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class TestDataLogger {

    private final static String MOCK_SQL_INPUT_TABLE_NAME_1 = "table1";
    private final static String MOCK_SQL_INPUT_TABLE_NAME_2 = "table2";

    private File tempOutputFile;
    private File tempInputFile;
    private DataLogger dataLogger;

    @Before
    public void setUp() throws IOException, SQLException, ClassNotFoundException {
        this.tempOutputFile = File.createTempFile("energy-agents-data-logger", ".db");
        this.tempInputFile = File.createTempFile("energy-agents-input-mock", ".db");
        this.createInputDatabaseMock();

        this.dataLogger = new DataLogger(
                new HashSet<>(),
                this.tempInputFile.getAbsolutePath(),
                this.tempOutputFile.getCanonicalPath()
        );
    }

    @After
    public void tearDown() {
        this.tempOutputFile.deleteOnExit();
        this.tempInputFile.deleteOnExit();
    }

    private void createInputDatabaseMock() throws SQLException, ClassNotFoundException, IOException {
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.tempInputFile.getCanonicalPath()))) {
            this.writeTable(conn, MOCK_SQL_INPUT_TABLE_NAME_1);
            this.writeTable(conn, MOCK_SQL_INPUT_TABLE_NAME_2);
        }
    }

    private void writeTable(Connection conn, String tableName) throws SQLException {

        try (Statement stat = conn.createStatement()) {
            stat.executeUpdate(String.format("drop table if exists %s;", tableName));
            stat.executeUpdate(String.format(
                    "create table %s (timestamp TIMESTAMP, id INTEGER, value DOUBLE);", tableName));
        }
    }

    @Test
    public void writesMetadata() throws IOException, ClassNotFoundException, SQLException, ExecutionException, InterruptedException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");
        this.dataLogger.write(metadata).get();

        String filename = this.tempOutputFile.getCanonicalPath();

        Map<String, String> writtenMetadata = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
             Statement stat = conn.createStatement();
             ResultSet rs = stat.executeQuery(String.format("select * from %s;", DataLogger.METADATA_TABLE_NAME));) {
            while (rs.next()) {
                writtenMetadata.put(rs.getString(1), rs.getString(2));
            }
        }
        assertThat(writtenMetadata, is(equalTo(metadata)));
    }

    @Test
    public void copiesInputDataBaseIntoOutput() throws SQLException, IOException, ClassNotFoundException,
            ExecutionException, InterruptedException {
        this.dataLogger.write(new HashMap<>()).get();

        String filename = this.tempOutputFile.getCanonicalPath();

        List<String> tableNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
             Statement stat = conn.createStatement();
             ResultSet rs = stat.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");) {
            while (rs.next()) {
                tableNames.add(rs.getString(1));
            }
        }
        assertThat(tableNames, hasItems(MOCK_SQL_INPUT_TABLE_NAME_1, MOCK_SQL_INPUT_TABLE_NAME_2));
    }


}
