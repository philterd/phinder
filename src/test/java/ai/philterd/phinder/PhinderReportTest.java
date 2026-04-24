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

import java.util.List;

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
}
