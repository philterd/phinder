package ai.philterd.phinder;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
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
        final File dbFile = tempDir.resolve("scan").toFile();
        final ScanLog scanLog = new ScanLog(dbFile);
        
        try {
            final String filePath = "/path/to/file.txt";
            final String hash = "hash123";
            
            assertNull(scanLog.getFileHash(filePath));
            
            scanLog.putFileHash(filePath, hash);
            assertEquals(hash, scanLog.getFileHash(filePath));
            
            scanLog.addScannedPath("/some/dir");
            assertTrue(scanLog.getScannedPaths().contains("/some/dir"));
            
            scanLog.clean();
            assertNull(scanLog.getFileHash(filePath));
            assertTrue(scanLog.getScannedPaths().isEmpty());
        } finally {
            scanLog.close();
        }
    }
}
