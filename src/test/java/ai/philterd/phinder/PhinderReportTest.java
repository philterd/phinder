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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PhinderReportTest {

    @Test
    public void testAddFileResult() {
        final PhinderReport report = new PhinderReport();
        final Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        final Span span2 = Span.make(15, 25, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        
        report.addFileResult("file1.txt", List.of(span1, span2), 100);
        
        assertEquals(2, report.getAggregateCounts().get("email-address"));
        assertEquals(2, report.getPerFileCounts().get("file1.txt").get("email-address"));
        assertEquals(2.0, report.getAggregateMagnitudeScore());
        assertEquals(2.0 / 100.0, report.getAggregateDensityScore());
    }

    @Test
    public void testWeights() {
        final PhinderReport report = new PhinderReport();
        report.setWeight("email-address", 5.0);
        
        final Span span = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("file1.txt", List.of(span), 10);
        
        assertEquals(5.0, report.getAggregateMagnitudeScore());
        assertEquals(0.5, report.getAggregateDensityScore());
    }

    @Test
    public void testAggregateMultipleFiles() {
        final PhinderReport report = new PhinderReport();
        final Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        final Span span2 = Span.make(0, 10, FilterType.IP_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        
        report.addFileResult("file1.txt", List.of(span1), 10);
        report.addFileResult("file2.txt", List.of(span2), 20);
        
        assertEquals(2, report.getAggregateCounts().size());
        assertEquals(1, report.getAggregateCounts().get("email-address"));
        assertEquals(1, report.getAggregateCounts().get("ip-address"));
        assertEquals(2.0, report.getAggregateMagnitudeScore());
        assertEquals(2.0 / 30.0, report.getAggregateDensityScore());
    }

    @Test
    public void testSkippedFiles() {
        final PhinderReport report = new PhinderReport();
        report.setSkippedFiles(5);
        assertEquals(5, report.getSkippedFiles());
    }

    @Test
    public void testReportId() {
        final PhinderReport report = new PhinderReport();
        assertNotNull(report.getReportId());
        assertFalse(report.getReportId().isEmpty());
        // Verify it's a UUID (at least roughly)
        assertTrue(report.getReportId().matches("^[0-9a-fA-F-]{36}$"));
    }

    @Test
    public void testConfidenceStats() {
        final PhinderReport report = new PhinderReport();
        final Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.5, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        final Span span2 = Span.make(15, 25, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        final Span span3 = Span.make(30, 40, FilterType.EMAIL_ADDRESS, "context", 0.7, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);

        report.addFileResult("file1.txt", List.of(span1, span2), 100);
        report.addFileResult("file2.txt", List.of(span3), 100);

        final PhinderReport.ConfidenceStats aggregateStats = report.getAggregateConfidence().get("email-address");
        assertNotNull(aggregateStats);
        assertEquals(0.5, aggregateStats.getMin(), 0.001);
        assertEquals(0.9, aggregateStats.getMax(), 0.001);
        assertEquals(0.7, aggregateStats.getAverage(), 0.001);
        assertEquals(3, aggregateStats.getCount());

        final PhinderReport.ConfidenceStats file1Stats = report.getPerFileConfidence().get("file1.txt").get("email-address");
        assertEquals(0.5, file1Stats.getMin(), 0.001);
        assertEquals(0.9, file1Stats.getMax(), 0.001);
        assertEquals(0.7, file1Stats.getAverage(), 0.001);
        assertEquals(2, file1Stats.getCount());

        final PhinderReport.ConfidenceStats file2Stats = report.getPerFileConfidence().get("file2.txt").get("email-address");
        assertEquals(0.7, file2Stats.getMin(), 0.001);
        assertEquals(0.7, file2Stats.getMax(), 0.001);
        assertEquals(0.7, file2Stats.getAverage(), 0.001);
        assertEquals(1, file2Stats.getCount());
    }

    @Test
    public void testRiskScoreRanksSensitivityAboveVolume() {
        // The case from the issue: a pile of first names must not outrank a pile of SSNs.
        final PhinderReport report = new PhinderReport();

        final List<Span> ssns = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            ssns.add(Span.make(0, 10, FilterType.SSN, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0));
        }
        report.addFileResult("ssns.txt", ssns, 5000);

        final List<Span> names = List.of(
                Span.make(0, 10, FilterType.FIRST_NAME, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0),
                Span.make(15, 25, FilterType.FIRST_NAME, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0));
        report.addFileResult("names.txt", names, 5000);

        assertEquals(5000.0, report.getFileRiskScore("ssns.txt"), 0.0001);
        assertEquals(6.0, report.getFileRiskScore("names.txt"), 0.0001);
        assertEquals(RiskScorer.RiskLevel.CRITICAL, report.getFileRiskLevel("ssns.txt"));
        assertEquals(RiskScorer.RiskLevel.LOW, report.getFileRiskLevel("names.txt"));

        assertEquals(5006.0, report.getAggregateRiskScore(), 0.0001);
        assertEquals(RiskScorer.RiskLevel.CRITICAL, report.getAggregateRiskLevel());
    }

    @Test
    public void testRiskScoreDiscountsLowConfidence() {
        final PhinderReport report = new PhinderReport();
        final Span certain = Span.make(0, 10, FilterType.SSN, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        final Span unsure = Span.make(0, 10, FilterType.SSN, "context", 0.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);

        report.addFileResult("certain.txt", List.of(certain), 100);
        report.addFileResult("unsure.txt", List.of(unsure), 100);

        assertEquals(10.0, report.getFileRiskScore("certain.txt"), 0.0001);
        // Half credit rather than none: a low-confidence hit is still worth looking at.
        assertEquals(5.0, report.getFileRiskScore("unsure.txt"), 0.0001);
    }

    @Test
    public void testSeverityOverride() {
        final PhinderReport report = new PhinderReport();
        final Span span = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("file1.txt", List.of(span), 10);

        assertEquals(RiskScorer.defaultSeverity("email-address"), report.getSeverity("email-address"));

        report.setSeverity("email-address", 20.0);

        assertEquals(20.0, report.getSeverity("email-address"));
        assertEquals(20.0, report.getFileRiskScore("file1.txt"), 0.0001);
        assertEquals(Map.of("email-address", 20.0), report.getSeverityOverrides());
        assertEquals(Map.of("email-address", 20.0), report.getEffectiveSeverities());
    }

    @Test
    public void testEffectiveSeveritiesCoverOnlyDetectedTypes() {
        final PhinderReport report = new PhinderReport();
        final Span span = Span.make(0, 10, FilterType.SSN, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("file1.txt", List.of(span), 10);

        assertEquals(Map.of("ssn", RiskScorer.defaultSeverity("ssn")), report.getEffectiveSeverities());
        assertTrue(report.getSeverityOverrides().isEmpty());
    }

    @Test
    public void testRiskScoreIsIndependentOfMagnitudeWeights() {
        // Weights tune the magnitude score only, so an existing weights file cannot move the risk score.
        final PhinderReport report = new PhinderReport();
        report.setWeight("ssn", 100.0);
        final Span span = Span.make(0, 10, FilterType.SSN, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("file1.txt", List.of(span), 10);

        assertEquals(100.0, report.getAggregateMagnitudeScore(), 0.0001);
        assertEquals(10.0, report.getAggregateRiskScore(), 0.0001);
    }

    @Test
    public void testAggregateRiskIsTheSumOfTheFileRisks() {
        // A reader adding up the per-file Risk Scores must land on the aggregate the report shows,
        // even though each file averages its own confidence separately.
        final PhinderReport report = new PhinderReport();
        report.addFileResult("a.txt", List.of(
                Span.make(0, 10, FilterType.SSN, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0),
                Span.make(15, 25, FilterType.LOCATION_CITY, "context", 1.0, "replacement", "salt", "window", true, true, new String[]{"test"}, 0)), 200);
        report.addFileResult("b.txt", List.of(
                Span.make(0, 10, FilterType.SSN, "context", 0.3, "replacement", "salt", "window", true, true, new String[]{"test"}, 0)), 200);
        report.addFileResult("c.txt", List.of(), 200);

        final double sumOfFiles = report.getFileRiskScore("a.txt")
                + report.getFileRiskScore("b.txt")
                + report.getFileRiskScore("c.txt");

        assertEquals(sumOfFiles, report.getAggregateRiskScore(), 0.0001);
    }

    @Test
    public void testRiskScoreOfAnEmptyReport() {
        final PhinderReport report = new PhinderReport();
        assertEquals(0.0, report.getAggregateRiskScore(), 0.0001);
        assertEquals(RiskScorer.RiskLevel.NONE, report.getAggregateRiskLevel());
        assertEquals(0.0, report.getFileRiskScore("never-scanned.txt"), 0.0001);
        assertEquals(RiskScorer.RiskLevel.NONE, report.getFileRiskLevel("never-scanned.txt"));
    }
}
