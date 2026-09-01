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

import static org.junit.jupiter.api.Assertions.*;

public class ReportOptionsTest {

    // One SSN outranks a pile of city names on risk, but loses on magnitude and density, so these
    // three files separate the orderings from each other.
    private PhinderReport createReport() {

        final PhinderReport report = new PhinderReport();

        report.addFileResult("ssn.txt", List.of(span(FilterType.SSN, 1.0)), 1000);

        final List<Span> cities = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            cities.add(span(FilterType.LOCATION_CITY, 1.0));
        }
        report.addFileResult("cities.txt", cities, 1000);

        report.addFileResult("empty.txt", List.of(), 1000);

        return report;
    }

    private static Span span(final FilterType filterType, final double confidence) {
        return Span.make(0, 10, filterType, "context", confidence, "replacement", "salt", "window",
                true, true, new String[]{"test"}, 0);
    }

    @Test
    public void testSortByRisk() {
        final PhinderReport report = createReport();
        final ReportOptions options = new ReportOptions(ReportOptions.SortBy.RISK, 0, ReportOptions.DEFAULT_TOP);
        // ssn.txt: 10.0. cities.txt: 8.0. empty.txt: 0.
        assertEquals(List.of("ssn.txt", "cities.txt", "empty.txt"), options.selectFiles(report));
    }

    @Test
    public void testSortByMagnitude() {
        final PhinderReport report = createReport();
        final ReportOptions options = new ReportOptions(ReportOptions.SortBy.MAGNITUDE, 0, ReportOptions.DEFAULT_TOP);
        // Magnitude counts occurrences at weight 1.0, so the eight cities come first.
        assertEquals(List.of("cities.txt", "ssn.txt", "empty.txt"), options.selectFiles(report));
    }

    @Test
    public void testSortByDensity() {
        final PhinderReport report = createReport();
        final ReportOptions options = new ReportOptions(ReportOptions.SortBy.DENSITY, 0, ReportOptions.DEFAULT_TOP);
        assertEquals(List.of("cities.txt", "ssn.txt", "empty.txt"), options.selectFiles(report));
    }

    @Test
    public void testSortByName() {
        final PhinderReport report = createReport();
        final ReportOptions options = new ReportOptions(ReportOptions.SortBy.NAME, 0, ReportOptions.DEFAULT_TOP);
        assertEquals(List.of("cities.txt", "empty.txt", "ssn.txt"), options.selectFiles(report));
    }

    @Test
    public void testMinRiskFiltersFiles() {
        final PhinderReport report = createReport();
        final ReportOptions options = new ReportOptions(ReportOptions.SortBy.RISK, 9.0, ReportOptions.DEFAULT_TOP);

        assertEquals(List.of("ssn.txt"), options.selectFiles(report));

        // Filtering is a view over the report, not over the scan: the totals still cover everything.
        assertEquals(9.0, report.getAggregateMagnitudeScore(), 0.0001);
        assertEquals(3, report.getPerFileCounts().size());
    }

    @Test
    public void testMinRiskOfZeroKeepsFilesWithNoPii() {
        final PhinderReport report = createReport();
        final ReportOptions options = ReportOptions.defaults();
        assertTrue(options.selectFiles(report).contains("empty.txt"));
    }

    @Test
    public void testDefaults() {
        final ReportOptions options = ReportOptions.defaults();
        assertEquals(ReportOptions.SortBy.RISK, options.getSortBy());
        assertEquals(0, options.getMinRisk());
        assertEquals(ReportOptions.DEFAULT_TOP, options.getTop());
    }

    @Test
    public void testNullSortByFallsBackToRisk() {
        assertEquals(ReportOptions.SortBy.RISK, new ReportOptions(null, 0, 20).getSortBy());
    }

}
