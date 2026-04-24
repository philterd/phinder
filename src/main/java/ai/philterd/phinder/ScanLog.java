package ai.philterd.phinder;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a log of a scan to track processed files and their xxHash64 hashes using a MongoDB database.
 */
public class ScanLog implements AutoCloseable {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> scannedPathsCollection;
    private final MongoCollection<Document> fileHashesCollection;
    private final MongoCollection<Document> reportsCollection;

    public ScanLog(final File databaseFile) throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (final ClassNotFoundException e) {
            throw new SQLException("H2 driver not found", e);
        }
        final String url = "jdbc:h2:" + databaseFile.getAbsolutePath();
        this.connection = DriverManager.getConnection(url, "sa", "");
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        try (final Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS scanned_paths (path VARCHAR(4096) PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS file_hashes (file_path VARCHAR(4096) PRIMARY KEY, hash VARCHAR(64))");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_scanned_paths_path ON scanned_paths(path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_file_hashes_file_path ON file_hashes(file_path)");
        }

        ConnectionString connectionString = new ConnectionString(mongoDbUrl);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(DATABASE_NAME);
        this.scannedPathsCollection = database.getCollection(SCANNED_PATHS_COLLECTION);
        this.fileHashesCollection = database.getCollection(FILE_HASHES_COLLECTION);
        this.reportsCollection = database.getCollection(REPORTS_COLLECTION);
    }

    public List<String> getScannedPaths() throws SQLException {
        final List<String> paths = new ArrayList<>();
        try (final Statement stmt = connection.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT path FROM scanned_paths")) {
            while (rs.next()) {
                paths.add(rs.getString("path"));
            }
        }
        return paths;
    }

    public void addScannedPath(final String path) throws SQLException {
        final String sql = "MERGE INTO scanned_paths (path) KEY (path) VALUES (?)";
        try (final PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.executeUpdate();
        }
    }

    public void putFileHash(final String filePath, final String hash) throws SQLException {
        final String sql = "MERGE INTO file_hashes (file_path, hash) KEY (file_path) VALUES (?, ?)";
        try (final PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
        }
    }

    public void clean() throws SQLException {
        try (final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE scanned_paths");
            stmt.execute("TRUNCATE TABLE file_hashes");
        }
    }

    public String getFileHash(final String filePath) throws SQLException {
        final String sql = "SELECT hash FROM file_hashes WHERE file_path = ?";
        try (final PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            try (final ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("hash");
                }
            }
        }
        return null;
    }

    public void saveReport(PhinderReport report) {
        Document doc = new Document();
        doc.append("timestamp", new Date());
        doc.append("skipped_files", report.getSkippedFiles());
        doc.append("aggregate_magnitude_score", report.getAggregateMagnitudeScore());
        doc.append("aggregate_density_score", report.getAggregateDensityScore());

        Document aggregateCountsDoc = new Document();
        for (Map.Entry<String, Integer> entry : report.getAggregateCounts().entrySet()) {
            aggregateCountsDoc.append(entry.getKey(), entry.getValue());
        }
        doc.append("aggregate_counts", aggregateCountsDoc);

        Document aggregateConfidenceDoc = new Document();
        for (Map.Entry<String, PhinderReport.ConfidenceStats> entry : report.getAggregateConfidence().entrySet()) {
            Document statsDoc = new Document()
                    .append("min", entry.getValue().getMin())
                    .append("max", entry.getValue().getMax())
                    .append("average", entry.getValue().getAverage())
                    .append("count", entry.getValue().getCount());
            aggregateConfidenceDoc.append(entry.getKey(), statsDoc);
        }
        doc.append("aggregate_confidence", aggregateConfidenceDoc);

        List<Document> perFileResults = new ArrayList<>();
        for (String filePath : report.getPerFileCounts().keySet()) {
            Document fileDoc = new Document();
            fileDoc.append("file_path", filePath);
            fileDoc.append("magnitude_score", report.getFileMagnitudeScore(filePath));
            fileDoc.append("density_score", report.getFileDensityScore(filePath));

            Document countsDoc = new Document();
            for (Map.Entry<String, Integer> entry : report.getPerFileCounts().get(filePath).entrySet()) {
                countsDoc.append(entry.getKey(), entry.getValue());
            }
            fileDoc.append("counts", countsDoc);

            Document confidenceDoc = new Document();
            Map<String, PhinderReport.ConfidenceStats> fileConf = report.getPerFileConfidence().get(filePath);
            if (fileConf != null) {
                for (Map.Entry<String, PhinderReport.ConfidenceStats> entry : fileConf.entrySet()) {
                    Document statsDoc = new Document()
                            .append("min", entry.getValue().getMin())
                            .append("max", entry.getValue().getMax())
                            .append("average", entry.getValue().getAverage())
                            .append("count", entry.getValue().getCount());
                    confidenceDoc.append(entry.getKey(), statsDoc);
                }
            }
            fileDoc.append("confidence", confidenceDoc);

            perFileResults.add(fileDoc);
        }
        doc.append("per_file_results", perFileResults);

        reportsCollection.insertOne(doc);
    }

    public long getReportCount() {
        return reportsCollection.countDocuments();
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
