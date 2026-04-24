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

/**
 * Represents a log of a scan to track processed files and their xxHash64 hashes using a MongoDB database.
 */
public class ScanLog implements AutoCloseable {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> scannedPathsCollection;
    private final MongoCollection<Document> fileHashesCollection;
    private final MongoCollection<Document> reportsCollection;

    private static final String DATABASE_NAME = "phinder";
    private static final String SCANNED_PATHS_COLLECTION = "scanned_paths";
    private static final String FILE_HASHES_COLLECTION = "file_hashes";
    private static final String REPORTS_COLLECTION = "reports";

    public ScanLog(String mongoDbUri) {
        if (mongoDbUri == null || mongoDbUri.isEmpty()) {
            throw new RuntimeException("MongoDB URI is not set. Use --mongodb to specify the URI.");
        }

        ConnectionString connectionString = new ConnectionString(mongoDbUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(DATABASE_NAME);
        this.scannedPathsCollection = database.getCollection(SCANNED_PATHS_COLLECTION);
        this.fileHashesCollection = database.getCollection(FILE_HASHES_COLLECTION);
        this.reportsCollection = database.getCollection(REPORTS_COLLECTION);
    }

    public List<String> getScannedPaths() {
        List<String> paths = new ArrayList<>();
        for (Document doc : scannedPathsCollection.find()) {
            paths.add(doc.getString("path"));
        }
        return paths;
    }

    public void addScannedPath(String path) {
        scannedPathsCollection.updateOne(
                Filters.eq("path", path),
                Updates.set("path", path),
                new UpdateOptions().upsert(true)
        );
    }

    public void putFileHash(String filePath, String hash) {
        fileHashesCollection.updateOne(
                Filters.eq("file_path", filePath),
                Updates.set("hash", hash),
                new UpdateOptions().upsert(true)
        );
    }

    public void clean() {
        scannedPathsCollection.deleteMany(new Document());
        fileHashesCollection.deleteMany(new Document());
        reportsCollection.deleteMany(new Document());
    }

    public String getFileHash(String filePath) {
        Document doc = fileHashesCollection.find(Filters.eq("file_path", filePath)).first();
        if (doc != null) {
            return doc.getString("hash");
        }
        return null;
    }

    public void saveReport(PhinderReport report) {
        Document doc = new Document();
        doc.append("report_id", report.getReportId());
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
