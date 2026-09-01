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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportBuilderTest {

    @TempDir
    Path tempDir;


    @Test
    public void testJsonReport() throws Exception {
        final PhinderReport report = createTestReport();
        report.setWeight("email-address", 2.0);
        final File reportFile = tempDir.resolve("report.json").toFile();
        
        ReportBuilder.generateJsonReport(report, reportFile);
        
        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"aggregateCounts\""));
        assertTrue(content.contains("\"email-address\": 2"));
        // Matches yyyy-MM-dd HH:mm:ss
        assertTrue(content.matches("(?s).*\"timestamp\": \"\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\".*"));
        assertTrue(content.contains("\"weights\":"));
        assertTrue(content.contains("\"email-address\": 2.0"));
        assertTrue(content.contains("\"magnitudeScore\": 4.0"));
        assertTrue(content.contains("\"densityScore\": 0.04"));
    }

    @Test
    public void testAlwaysGenerateReports() throws Exception {
        final PhinderReport report = createTestReport();
        
        final ReportBuilder builder = new ReportBuilder();
        builder.build(report, null);
        
        final File htmlReport = new File("report.html");
        final File jsonReport = new File("report.json");
        
        assertTrue(htmlReport.exists(), "Default HTML report should always be generated");
        assertTrue(jsonReport.exists(), "Default JSON report should always be generated");
        
        htmlReport.delete();
        jsonReport.delete();
    }

    @Test
    public void testHtmlReport() throws Exception {
        final PhinderReport report = createTestReport();
        final File reportFile = tempDir.resolve("report.html").toFile();
        
        ReportBuilder.generateHtmlReport(report, reportFile);
        
        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("email-address"));
        assertTrue(content.contains("https://www.philterd.ai"));
        assertTrue(content.contains("Confidence Interval"));
        assertTrue(content.contains("0.90 - 0.90 (avg: 0.90)"));
        assertTrue(content.contains("Best Candidates for Redaction Testing"), "Should contain best candidates section");
        assertTrue(content.contains("PII Variety"), "Should contain variety column");
    }


    @Test
    public void testJsonReportIncludesRisk() throws Exception {
        final PhinderReport report = createTestReport();
        final File reportFile = tempDir.resolve("report.json").toFile();

        ReportBuilder.generateJsonReport(report, reportFile);

        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        // Two email addresses at severity 5.0 and confidence 0.9: 5.0 * 2 * 0.95 = 9.5.
        assertTrue(content.contains("\"aggregateRiskScore\": 9.5"), content);
        assertTrue(content.contains("\"aggregateRiskLevel\": \"LOW\""), content);
        assertTrue(content.contains("\"riskScore\": 9.5"), content);
        assertTrue(content.contains("\"riskLevel\": \"LOW\""), content);
        assertTrue(content.contains("\"severities\""), content);
        assertTrue(content.contains("\"sortedBy\": \"risk\""), content);
        assertTrue(content.contains("\"filesReported\": 1"), content);
        assertTrue(content.contains("\"filesScanned\": 1"), content);
    }

    @Test
    public void testJsonReportOrdersFilesByRisk() throws Exception {
        final PhinderReport report = createTwoFileReport();
        final File reportFile = tempDir.resolve("report.json").toFile();

        ReportBuilder.generateJsonReport(report, reportFile, ReportOptions.defaults());

        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.indexOf("high.txt") < content.indexOf("low.txt"),
                "The higher-risk file should be listed first");
    }

    @Test
    public void testJsonReportHonorsMinRisk() throws Exception {
        final PhinderReport report = createTwoFileReport();
        final File reportFile = tempDir.resolve("report.json").toFile();

        ReportBuilder.generateJsonReport(report, reportFile,
                new ReportOptions(ReportOptions.SortBy.RISK, 5.0, ReportOptions.DEFAULT_TOP));

        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("high.txt"));
        assertFalse(content.contains("\"low.txt\""), "The below-threshold file should not be listed");
        assertTrue(content.contains("\"filesReported\": 1"), content);
        // The aggregate still covers the whole scan, not just the reported files.
        assertTrue(content.contains("\"filesScanned\": 2"), content);
        assertTrue(content.contains("\"minRisk\": 5.0"), content);
    }

    @Test
    public void testHtmlReportIncludesRisk() throws Exception {
        final PhinderReport report = createTestReport();
        final File reportFile = tempDir.resolve("report.html").toFile();

        ReportBuilder.generateHtmlReport(report, reportFile);

        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("Aggregate Risk Score"));
        assertTrue(content.contains("Risk Score"), "Candidates table should have a risk column");
        assertTrue(content.contains("Risk Level"));
        assertTrue(content.contains("Severity"), "Type tables should show the severity used");
        assertTrue(content.contains("Risk: 9.50 (LOW)"));
        assertTrue(content.contains("sorted by risk"));
    }

    @Test
    public void testHtmlReportHonorsTopAndMinRisk() throws Exception {
        final PhinderReport report = createTwoFileReport();
        final File reportFile = tempDir.resolve("report.html").toFile();

        ReportBuilder.generateHtmlReport(report, reportFile,
                new ReportOptions(ReportOptions.SortBy.RISK, 5.0, 1));

        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("Showing 1 of 2 scanned file(s)"), content);
        assertTrue(content.contains("risk score 5.00 and above"), content);
        assertTrue(content.contains("high.txt"));
        assertFalse(content.contains("low.txt"));
    }

    @Test
    public void testHtmlReportWithNoFiles() throws Exception {
        final PhinderReport report = new PhinderReport();
        final File reportFile = tempDir.resolve("report.html").toFile();

        ReportBuilder.generateHtmlReport(report, reportFile);

        final String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("No PII detected."));
        assertTrue(content.contains("Showing 0 of 0 scanned file(s)"));
    }

    // One SSN outranks a single city name on risk.
    private PhinderReport createTwoFileReport() {
        final PhinderReport report = new PhinderReport();
        report.addFileResult("high.txt", List.of(
                Span.make(0, 10, FilterType.SSN, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0)), 100);
        report.addFileResult("low.txt", List.of(
                Span.make(0, 10, FilterType.LOCATION_CITY, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0)), 100);
        return report;
    }

    private PhinderReport createTestReport() {
        final PhinderReport report = new PhinderReport();
        final Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        final Span span2 = Span.make(15, 25, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("test.txt", List.of(span1, span2), 100);
        return report;
    }
}
