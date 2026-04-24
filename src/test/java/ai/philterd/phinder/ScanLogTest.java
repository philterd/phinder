package ai.philterd.phinder;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScanLogTest {

    private MongoServer server;
    private ScanLog scanLog;
    private String connectionString;

    @BeforeEach
    public void setUp() {
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = server.bind();
        connectionString = "mongodb://" + serverAddress.getHostName() + ":" + serverAddress.getPort();
        scanLog = new ScanLog(connectionString);
    }

    @AfterEach
    public void tearDown() {
        scanLog.close();
        server.shutdown();
    }

    @Test
    public void testScanLogLifecycle() throws Exception {
        String filePath = "/path/to/file.txt";
        String hash = "hash123";

        assertNull(scanLog.getFileHash(filePath));

        scanLog.putFileHash(filePath, hash);
        assertEquals(hash, scanLog.getFileHash(filePath));

        scanLog.addScannedPath("/some/dir");
        assertTrue(scanLog.getScannedPaths().contains("/some/dir"));

        scanLog.clean();
        assertNull(scanLog.getFileHash(filePath));
        assertTrue(scanLog.getScannedPaths().isEmpty());
    }

    @Test
    public void testSaveReport() {
        PhinderReport report = new PhinderReport();
        report.setSkippedFiles(5);
        report.addFileResult("file1.txt", Collections.emptyList(), 100);

        // This should not throw an exception
        scanLog.saveReport(report);

        // Verify that the report was saved
        assertEquals(1, scanLog.getReportCount());

        // Verify that the report ID was saved
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            MongoCollection<Document> reportsCollection = mongoClient.getDatabase("phinder").getCollection("reports");
            Document doc = reportsCollection.find().first();
            assertNotNull(doc);
            assertEquals(report.getReportId(), doc.getString("report_id"));
            assertNotNull(doc.get("timestamp"));
        }
    }
}
