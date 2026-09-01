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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * How the per-file sections of a report are ordered and trimmed.
 *
 * <p>These are presentation choices. Filtering with {@link #getMinRisk()} changes which files are
 * listed, not what was scanned: the aggregate scores and counts always cover every file, so a
 * filtered report still reports the full scan totals.
 */
public final class ReportOptions {

    /** The field the per-file sections of a report are ordered by. */
    public enum SortBy {

        RISK,
        MAGNITUDE,
        DENSITY,
        NAME
    }

    /** The default number of files listed as candidates for redaction testing. */
    public static final int DEFAULT_TOP = 20;

    private final SortBy sortBy;
    private final double minRisk;
    private final int top;

    public ReportOptions(final SortBy sortBy, final double minRisk, final int top) {
        this.sortBy = sortBy == null ? SortBy.RISK : sortBy;
        this.minRisk = minRisk;
        this.top = top;
    }

    /** Order by Risk Score, list every file, and cap the candidates table at 20 files. */
    public static ReportOptions defaults() {
        return new ReportOptions(SortBy.RISK, 0, DEFAULT_TOP);
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public double getMinRisk() {
        return minRisk;
    }

    public int getTop() {
        return top;
    }

    /**
     * The scanned files that meet {@link #getMinRisk()}, in {@link #getSortBy()} order.
     *
     * <p>Every ordering other than by name breaks ties on the remaining scores and finally on the
     * file path, so a report of the same scan lists files in the same order every time.
     */
    public List<String> selectFiles(final PhinderReport report) {

        final List<String> files = new ArrayList<>();

        for (final String file : report.getPerFileCounts().keySet()) {
            if (report.getFileRiskScore(file) >= minRisk) {
                files.add(file);
            }
        }

        files.sort(comparator(report));

        return files;
    }

    private Comparator<String> comparator(final PhinderReport report) {

        if (sortBy == SortBy.NAME) {
            return Comparator.naturalOrder();
        }

        final Comparator<String> byRisk = Comparator.comparingDouble(report::getFileRiskScore).reversed();
        final Comparator<String> byMagnitude = Comparator.comparingDouble(report::getFileMagnitudeScore).reversed();
        final Comparator<String> byDensity = Comparator.comparingDouble(report::getFileDensityScore).reversed();
        final Comparator<String> byVariety =
                Comparator.comparingInt((String f) -> report.getPerFileCounts().get(f).size()).reversed();

        final Comparator<String> primary = switch (sortBy) {
            case MAGNITUDE -> byMagnitude.thenComparing(byVariety).thenComparing(byDensity).thenComparing(byRisk);
            case DENSITY -> byDensity.thenComparing(byRisk).thenComparing(byMagnitude).thenComparing(byVariety);
            default -> byRisk.thenComparing(byMagnitude).thenComparing(byVariety).thenComparing(byDensity);
        };

        return primary.thenComparing(Comparator.naturalOrder());
    }

}
