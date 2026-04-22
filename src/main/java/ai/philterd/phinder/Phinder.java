/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phinder;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import ai.philterd.phinder.processors.*;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "phinder", mixinStandardHelpOptions = true, version = "phinder 1.0.0",
        description = "Identifies PII in text using Phileas.")
public class Phinder implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, description = "The input file(s) or directorie(s) (text, Markdown, PDF, Word, Excel, PowerPoint, CSV, RTF, PNG, JPG/JPEG, EML, MSG, or LOG).", required = true)
    private List<File> inputFiles;

    @Option(names = {"-R", "--recursive"}, description = "Recursively traverse directories.")
    private boolean recursive;

    @Option(names = {"-p", "--policy"}, description = "The Phileas policy (JSON file).")
    private File policyFile;

    @Option(names = {"--csv-delimiter"}, description = "The CSV delimiter character (default: ,).", defaultValue = ",")
    private char csvDelimiter;

    @Option(names = {"--csv-quote"}, description = "The CSV quote character (default: \").", defaultValue = "\"")
    private char csvQuote;

    @Option(names = {"-r", "--report"}, description = "The report file path. If not specified, a text report will be saved to 'report.txt'.")
    private File reportFile;

    @Option(names = {"-f", "--format"}, description = "The report format: text, pdf, json, html. Default is text.", defaultValue = "text")
    private String reportFormat;

    @Option(names = {"-w", "--weights"}, description = "The PII weights (JSON file).")
    private File weightsFile;

    @Option(names = {"-l", "--log"}, description = "Keep a log of the scan using an H2 database (default: scan.db).", arity = "0..1", fallbackValue = "scan")
    private File logFile;

    @Option(names = {"-s", "--skip-unchanged"}, description = "Skip scanning files that haven't changed since the last scan log.")
    private boolean skipUnchanged;

    @Option(names = {"--clean"}, description = "Truncate the scan log database.")
    private boolean clean;

    private PlainTextFilterService filterService;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Phinder()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Policy policy;
        if (policyFile != null) {
            if (!policyFile.exists()) {
                System.err.println("Policy file does not exist: " + policyFile.getAbsolutePath());
                return 1;
            }
            String policyJson = FileUtils.readFileToString(policyFile, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            policy = gson.fromJson(policyJson, Policy.class);
        } else {
            policy = createDefaultPolicy();
        }

        // Initialize FilterService once
        Properties properties = new Properties();
        PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);
        this.filterService = new PlainTextFilterService(
                phileasConfiguration,
                new DefaultContextService(),
                new InMemoryVectorService(),
                HttpClients.createDefault()
        );

        List<DocumentProcessor> processors = List.of(
                new PlainTextProcessor(),
                new PdfProcessor(),
                new WordProcessor(),
                new ExcelProcessor(),
                new PowerPointProcessor(),
                new CsvProcessor(csvDelimiter, csvQuote),
                new RtfProcessor(),
                new ImageProcessor(),
                new EmailProcessor(),
                new LogProcessor()
        );

        int exitCode = 0;
        int skippedCount = 0;
        PhinderReport report = new PhinderReport();

        ScanLog scanLog = null;
        if (logFile != null || skipUnchanged || clean) {
            if (logFile == null) {
                logFile = new File("scan");
            }
            scanLog = new ScanLog(logFile);
        }

        try {
            if (clean && scanLog != null) {
                scanLog.clean();
                System.out.println("Scan log cleaned.");
            }
            if (weightsFile != null) {
                if (!weightsFile.exists()) {
                    System.err.println("Weights file does not exist: " + weightsFile.getAbsolutePath());
                    return 1;
                }
                String weightsJson = FileUtils.readFileToString(weightsFile, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                Map<String, Object> weightsMap = gson.fromJson(weightsJson, Map.class);
                if (weightsMap != null) {
                    for (Map.Entry<String, Object> entry : weightsMap.entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        Double value = Double.valueOf(String.valueOf(entry.getValue()));
                        report.setWeight(key, value);
                    }
                }
            }

            for (File inputFile : inputFiles) {
                if (!inputFile.exists()) {
                    System.err.println("Input file/directory does not exist: " + inputFile.getAbsolutePath());
                    exitCode = 1;
                    continue;
                }

                if (scanLog != null) {
                    scanLog.addScannedPath(inputFile.getAbsolutePath());
                }

                if (inputFile.isDirectory()) {
                    try (Stream<Path> stream = recursive ? Files.walk(inputFile.toPath()) : Files.walk(inputFile.toPath(), 1)) {
                        // Process files directly from the stream to handle millions of files without memory issues
                        Iterable<Path> iterable = stream::iterator;
                        for (Path path : iterable) {
                            if (Files.isRegularFile(path)) {
                                File file = path.toFile();
                                if (processFileWithCheck(file, processors, policy, report, scanLog)) {
                                    exitCode = 1;
                                } else if (skipUnchanged && scanLog != null) {
                                    String hash = getFileHash(file);
                                    String previousHash = scanLog.getFileHash(file.getAbsolutePath());
                                    if (hash.equals(previousHash)) {
                                        skippedCount++;
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to walk directory: " + inputFile.getAbsolutePath() + " - " + e.getMessage());
                        exitCode = 1;
                    }
                } else {
                    if (processFileWithCheck(inputFile, processors, policy, report, scanLog)) {
                        exitCode = 1;
                    } else if (skipUnchanged && scanLog != null) {
                        String hash = getFileHash(inputFile);
                        String previousHash = scanLog.getFileHash(inputFile.getAbsolutePath());
                        if (hash.equals(previousHash)) {
                            skippedCount++;
                        }
                    }
                }
            }

            report.setSkippedFiles(skippedCount);
            generateReport(report);

            return exitCode;
        } finally {
            if (scanLog != null) {
                scanLog.close();
                System.out.println("Scan log saved to " + logFile.getAbsolutePath() + ".mv.db");
            }
        }
    }

private boolean processFileWithCheck(File inputFile, List<DocumentProcessor> processors, Policy policy, PhinderReport report, ScanLog scanLog) throws SQLException {
    String hash = getFileHash(inputFile);

    if (skipUnchanged && scanLog != null) {
        String previousHash = scanLog.getFileHash(inputFile.getAbsolutePath());
        if (hash != null && hash.equals(previousHash)) {
            System.out.println("Skipping unchanged file: " + inputFile.getName());
            return false;
        }
    }

    if (scanLog != null && hash != null) {
        scanLog.putFileHash(inputFile.getAbsolutePath(), hash);
    }

    return processFile(inputFile, processors, policy, report);
}

    private String getFileHash(File file) {
        try {
            XXHashFactory factory = XXHashFactory.fastestInstance();
            StreamingXXHash64 hasher = factory.newStreamingHash64(0); // Using seed 0
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    hasher.update(buffer, 0, read);
                }
            }
            return String.format("%016x", hasher.getValue());
        } catch (IOException e) {
            System.err.println("Error calculating xxHash64 hash for " + file.getAbsolutePath() + ": " + e.getMessage());
            return "";
        }
    }

    private boolean processFile(File inputFile, List<DocumentProcessor> processors, Policy policy, PhinderReport report) {
        DocumentProcessor processor = processors.stream()
                .filter(p -> p.supports(inputFile))
                .findFirst()
                .orElse(null);

        if (processor == null) {
            System.err.println("Unsupported file type: " + inputFile.getName());
            return true;
        }

        try {
            List<Span> spans;
            long wordCount;

            if (processor instanceof LogProcessor || processor instanceof CsvProcessor) {
                spans = processor.process(inputFile, policy, this);
                wordCount = processor.getWordCount(inputFile);
            } else {
                String text = processor.extractText(inputFile);
                if (text == null) text = "";

                String combinedText = inputFile.getName() + "\n" + text;
                spans = findPii(combinedText, policy);
                wordCount = processor.countWords(text);
            }

            report.addFileResult(inputFile.getAbsolutePath(), spans, wordCount);

            System.out.println("PII found in " + inputFile.getName() + ":");
            if (spans.isEmpty()) {
                System.out.println(" - No PII found.");
            } else {
                for (Span span : spans) {
                    System.out.printf(" - %s (type: %s, confidence: %.2f)\n",
                            span.getText(), span.getFilterType(), span.getConfidence());
                }
                System.out.printf("Magnitude Score: %.2f\n", report.getFileMagnitudeScore(inputFile.getAbsolutePath()));
                System.out.printf("Density Score: %.4f\n", report.getFileDensityScore(inputFile.getAbsolutePath()));
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error processing file " + inputFile.getAbsolutePath() + ": " + e.getMessage());
            return true;
        }
        return false;
    }

    private void generateReport(PhinderReport report) throws Exception {
        ReportBuilder reportBuilder = new ReportBuilder(reportFile, reportFormat);
        reportBuilder.build(report);
    }

    public Policy createDefaultPolicy() {

        // TODO: Build out the full policy.

        Policy policy = new Policy();
        Identifiers identifiers = new Identifiers();

        EmailAddress emailAddress = new EmailAddress();
        EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();
        emailAddressFilterStrategy.setStrategy("REDACT");
        emailAddress.setEmailAddressFilterStrategies(Collections.singletonList(emailAddressFilterStrategy));
        identifiers.setEmailAddress(emailAddress);

        Ssn ssn = new Ssn();
        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();
        ssnFilterStrategy.setStrategy("REDACT");
        ssn.setSsnFilterStrategies(Collections.singletonList(ssnFilterStrategy));
        identifiers.setSsn(ssn);

        policy.setIdentifiers(identifiers);

        return policy;

    }

    public List<Span> findPii(String text, Policy policy) throws Exception {
        if (filterService == null) {
            // Lazy initialization for cases like unit tests where call() isn't called
            Properties properties = new Properties();
            PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);
            this.filterService = new PlainTextFilterService(
                    phileasConfiguration,
                    new DefaultContextService(),
                    new InMemoryVectorService(),
                    HttpClients.createDefault()
            );
        }

        // Filter the text.
        TextFilterResult response = filterService.filter(policy, "context", text);

        return response.getExplanation().appliedSpans();
    }

    // Keep the old method for backward compatibility if needed, or remove it.
    // I'll update PhinderTest to use the new method.
    public List<Span> findPii(String text) throws Exception {
        return findPii(text, createDefaultPolicy());
    }

}
