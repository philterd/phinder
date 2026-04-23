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
        builder.build(report);
        
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


    private PhinderReport createTestReport() {
        final PhinderReport report = new PhinderReport();
        final Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        final Span span2 = Span.make(15, 25, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("test.txt", List.of(span1, span2), 100);
        return report;
    }
}
