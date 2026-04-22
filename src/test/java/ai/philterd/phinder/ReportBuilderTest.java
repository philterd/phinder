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
    public void testTextReport() throws Exception {
        PhinderReport report = createTestReport();
        File reportFile = tempDir.resolve("report.txt").toFile();
        
        ReportBuilder.generateTextReport(report, reportFile);
        
        String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("Phinder PII Report"), "Report should contain title");
        assertTrue(content.contains("email-address: 2"), "Report should contain count");
        assertTrue(content.contains("Magnitude Score: 2.00"), "Report should contain score");
    }

    @Test
    public void testJsonReport() throws Exception {
        PhinderReport report = createTestReport();
        File reportFile = tempDir.resolve("report.json").toFile();
        
        ReportBuilder.generateJsonReport(report, reportFile);
        
        String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"aggregateCounts\""));
        assertTrue(content.contains("\"email-address\": 2"));
    }

    @Test
    public void testHtmlReport() throws Exception {
        PhinderReport report = createTestReport();
        File reportFile = tempDir.resolve("report.html").toFile();
        
        ReportBuilder.generateHtmlReport(report, reportFile);
        
        String content = FileUtils.readFileToString(reportFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("email-address"));
        assertTrue(content.contains("https://www.philterd.ai"));
        assertTrue(content.contains("Confidence Interval"));
        assertTrue(content.contains("0.90 - 0.90 (avg: 0.90)"));
        assertTrue(content.contains("Best Candidates for Redaction Testing"), "Should contain best candidates section");
        assertTrue(content.contains("PII Variety"), "Should contain variety column");
    }

    @Test
    public void testPdfReport() throws Exception {
        PhinderReport report = createTestReport();
        File reportFile = tempDir.resolve("report.pdf").toFile();
        
        ReportBuilder.generatePdfReport(report, reportFile);
        
        assertTrue(reportFile.exists());
        assertTrue(reportFile.length() > 0);
    }

    private PhinderReport createTestReport() {
        PhinderReport report = new PhinderReport();
        Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        Span span2 = Span.make(15, 25, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("test.txt", List.of(span1, span2), 100);
        return report;
    }
}
