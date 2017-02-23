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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TestDataLogger {

    private File tempFile;
    private DataLogger dataLogger;

    @Before
    public void setUp() throws IOException {
        this.tempFile = File.createTempFile("energy-agents-data-logger", ".db");
        this.dataLogger = new DataLogger(new HashSet<>(), this.tempFile.getCanonicalPath());
    }

    @After
    public void tearDown() {
        this.tempFile.deleteOnExit();
    }

    @Test
    public void writesMetadata() throws IOException, ClassNotFoundException, SQLException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");
        this.dataLogger.write(metadata);

        String filename = this.tempFile.getCanonicalPath();

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", filename));
        Statement stat = conn.createStatement();

        Map<String, String> writtenMetadata = new HashMap<>();
        ResultSet rs = stat.executeQuery(String.format("select * from %s;", DataLogger.METADATA_TABLE_NAME));
        while (rs.next()) {
            writtenMetadata.put(rs.getString(1), rs.getString(2));
        }
        rs.close();
        conn.close();

        assertThat(writtenMetadata, is(equalTo(metadata)));
    }


}
