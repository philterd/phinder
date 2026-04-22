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
        PhinderReport report = new PhinderReport();
        Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        Span span2 = Span.make(15, 25, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        
        report.addFileResult("file1.txt", List.of(span1, span2), 100);
        
        assertEquals(2, report.getAggregateCounts().get("email-address"));
        assertEquals(2, report.getPerFileCounts().get("file1.txt").get("email-address"));
        assertEquals(2.0, report.getAggregateMagnitudeScore());
        assertEquals(2.0 / 100.0, report.getAggregateDensityScore());
    }

    @Test
    public void testWeights() {
        PhinderReport report = new PhinderReport();
        report.setWeight("email-address", 5.0);
        
        Span span = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        report.addFileResult("file1.txt", List.of(span), 10);
        
        assertEquals(5.0, report.getAggregateMagnitudeScore());
        assertEquals(0.5, report.getAggregateDensityScore());
    }

    @Test
    public void testAggregateMultipleFiles() {
        PhinderReport report = new PhinderReport();
        Span span1 = Span.make(0, 10, FilterType.EMAIL_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        Span span2 = Span.make(0, 10, FilterType.IP_ADDRESS, "context", 0.9, "replacement", "salt", "window", true, true, new String[]{"test"}, 0);
        
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
        PhinderReport report = new PhinderReport();
        report.setSkippedFiles(5);
        assertEquals(5, report.getSkippedFiles());
    }
}
