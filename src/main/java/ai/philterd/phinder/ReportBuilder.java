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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportBuilder {

    public ReportBuilder() {
    }

    public void build(final PhinderReport report, final String mongoDbUri) throws Exception {
        build(report, mongoDbUri, ReportOptions.defaults());
    }

    public void build(final PhinderReport report, final String mongoDbUri, final ReportOptions options) throws Exception {
        // Always generate the HTML report.
        final File htmlReportFile = new File("report.html");

        // Always generate the JSON report.
        final File jsonReportFile = new File("report.json");

        generateHtmlReport(report, htmlReportFile, options);
        System.out.println("HTML report generated: " + htmlReportFile.getAbsolutePath());

        generateJsonReport(report, jsonReportFile, options);
        System.out.println("JSON report generated: " + jsonReportFile.getAbsolutePath());

        if (mongoDbUri != null && !mongoDbUri.isEmpty()) {
            try (ScanLog scanLog = new ScanLog(mongoDbUri)) {
                scanLog.saveReport(report);
                System.out.println("Report stored in MongoDB.");
            } catch (Exception e) {
                System.err.println("Error storing report in MongoDB: " + e.getMessage());
            }
        }
    }

    public static void generateJsonReport(final PhinderReport report, final File file) throws Exception {
        generateJsonReport(report, file, ReportOptions.defaults());
    }

    public static void generateJsonReport(final PhinderReport report, final File file, final ReportOptions options) throws Exception {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final String readableTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(report.getTimestamp()), ZoneId.systemDefault()).format(formatter);

        // A LinkedHashMap so the emitted order matches the requested sort rather than hash order.
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", readableTimestamp);
        data.put("weights", report.getWeights());
        data.put("severities", report.getEffectiveSeverities());
        data.put("sortedBy", options.getSortBy().name().toLowerCase(Locale.ROOT));
        data.put("minRisk", options.getMinRisk());
        data.put("aggregateRiskScore", report.getAggregateRiskScore());
        data.put("aggregateRiskLevel", report.getAggregateRiskLevel().name());
        data.put("aggregateMagnitudeScore", report.getAggregateMagnitudeScore());
        data.put("aggregateDensityScore", report.getAggregateDensityScore());
        data.put("skippedFiles", report.getSkippedFiles());
        data.put("aggregateCounts", report.getAggregateCounts());

        // The aggregate figures above cover the whole scan; only the per-file listing honors the
        // minimum-risk filter, so a filtered report still reports the full scan totals.
        final List<String> files = options.selectFiles(report);
        data.put("filesReported", files.size());
        data.put("filesScanned", report.getPerFileCounts().size());

        final Map<String, Object> perFileDetails = new LinkedHashMap<>();
        for (final String fileName : files) {
            final Map<String, Object> fileDetail = new LinkedHashMap<>();
            fileDetail.put("riskScore", report.getFileRiskScore(fileName));
            fileDetail.put("riskLevel", report.getFileRiskLevel(fileName).name());
            fileDetail.put("magnitudeScore", report.getFileMagnitudeScore(fileName));
            fileDetail.put("densityScore", report.getFileDensityScore(fileName));
            fileDetail.put("counts", report.getPerFileCounts().get(fileName));
            perFileDetails.put(fileName, fileDetail);
        }
        data.put("perFileDetails", perFileDetails);

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String json = gson.toJson(data);

        FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
    }

    // Tailwind classes for each risk band, so a reader can pick out the worst files at a glance.
    private static String riskBadgeClasses(final RiskScorer.RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "bg-red-100 text-red-800";
            case HIGH -> "bg-orange-100 text-orange-800";
            case MEDIUM -> "bg-yellow-100 text-yellow-800";
            case LOW -> "bg-blue-100 text-blue-800";
            case NONE -> "bg-gray-100 text-gray-600";
        };
    }

    private static String riskTextClasses(final RiskScorer.RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "text-red-600";
            case HIGH -> "text-orange-600";
            case MEDIUM -> "text-yellow-600";
            case LOW -> "text-blue-600";
            case NONE -> "text-gray-500";
        };
    }

    public static void generateHtmlReport(final PhinderReport report, final File file) throws Exception {
        generateHtmlReport(report, file, ReportOptions.defaults());
    }

    public static void generateHtmlReport(final PhinderReport report, final File file, final ReportOptions options) throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>Phinder PII Report</title>\n");
        sb.append("    <script src=\"https://cdn.tailwindcss.com\"></script>\n");
        sb.append("</head>\n");
        sb.append("<body class=\"bg-gray-50 text-gray-900 font-sans\">\n");
        sb.append("    <div class=\"max-w-6xl mx-auto px-4 py-12\">\n");
        sb.append("        <header class=\"mb-12 border-b border-gray-200 pb-8\">\n");
        sb.append("            <h1 class=\"text-4xl font-extrabold text-blue-800 mb-2\">Phinder PII Report</h1>\n");
        sb.append(String.format("            <p class=\"text-sm text-gray-400\">Report ID: %s</p>\n", report.getReportId()));
        sb.append("            <p class=\"text-lg text-gray-600\">Personally Identifiable Information (PII) detection summary.</p>\n");
        sb.append(String.format("            <p class=\"text-sm text-gray-400 mt-2\">Report generated on %s</p>\n",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(report.getTimestamp()), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        sb.append("        </header>\n");

        // Aggregate Summary Cards
        final RiskScorer.RiskLevel aggregateRiskLevel = report.getAggregateRiskLevel();
        sb.append("        <section class=\"grid grid-cols-1 md:grid-cols-4 gap-6 mb-12\">\n");
        sb.append("            <div class=\"bg-white p-6 rounded-xl shadow-sm border border-gray-100\">\n");
        sb.append("                <h2 class=\"text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2\">Aggregate Risk Score</h2>\n");
        sb.append(String.format("                <p class=\"text-3xl font-bold %s\">%.2f</p>\n",
                riskTextClasses(aggregateRiskLevel), report.getAggregateRiskScore()));
        sb.append(String.format("                <span class=\"inline-block mt-2 px-3 py-1 %s text-xs font-semibold rounded-full\">%s</span>\n",
                riskBadgeClasses(aggregateRiskLevel), aggregateRiskLevel.name()));
        sb.append("            </div>\n");
        sb.append("            <div class=\"bg-white p-6 rounded-xl shadow-sm border border-gray-100\">\n");
        sb.append("                <h2 class=\"text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2\">Aggregate Magnitude Score</h2>\n");
        sb.append(String.format("                <p class=\"text-3xl font-bold text-blue-600\">%.2f</p>\n", report.getAggregateMagnitudeScore()));
        sb.append("            </div>\n");
        sb.append("            <div class=\"bg-white p-6 rounded-xl shadow-sm border border-gray-100\">\n");
        sb.append("                <h2 class=\"text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2\">Aggregate Density Score</h2>\n");
        sb.append(String.format("                <p class=\"text-3xl font-bold text-blue-600\">%.4f</p>\n", report.getAggregateDensityScore()));
        sb.append("            </div>\n");
        sb.append("            <div class=\"bg-white p-6 rounded-xl shadow-sm border border-gray-100\">\n");
        sb.append("                <h2 class=\"text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2\">Files Skipped</h2>\n");
        sb.append(String.format("                <p class=\"text-3xl font-bold text-blue-600\">%d</p>\n", report.getSkippedFiles()));
        sb.append("            </div>\n");
        sb.append("        </section>\n");

        // The per-file sections below share one ordering and one minimum-risk filter. The aggregate
        // cards above always cover the whole scan.
        final List<String> files = options.selectFiles(report);

        sb.append(String.format("        <p class=\"text-sm text-gray-500 mb-8\">Showing %d of %d scanned file(s), sorted by %s%s.</p>\n",
                files.size(), report.getPerFileCounts().size(), options.getSortBy().name().toLowerCase(Locale.ROOT),
                options.getMinRisk() > 0 ? String.format(", risk score %.2f and above", options.getMinRisk()) : ""));

        // Best Candidates for Redaction Testing
        if (!files.isEmpty()) {

            final List<String> bestCandidates = files.subList(0, Math.min(Math.max(options.getTop(), 0), files.size()));

            sb.append("        <section class=\"mb-12\">\n");
            sb.append("            <h2 class=\"text-2xl font-bold text-gray-800 mb-6\">Best Candidates for Redaction Testing</h2>\n");
            sb.append("            <div class=\"bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden\">\n");
            sb.append("                <table class=\"min-w-full divide-y divide-gray-200\">\n");
            sb.append("                    <thead class=\"bg-gray-50\">\n");
            sb.append("                        <tr>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">File Name</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">PII Variety</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Risk Score</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Risk Level</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Magnitude Score</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Density Score</th>\n");
            sb.append("                        </tr>\n");
            sb.append("                    </thead>\n");
            sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");

            for (final String fileName : bestCandidates) {
                final RiskScorer.RiskLevel level = report.getFileRiskLevel(fileName);
                sb.append("                        <tr>\n");
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900 break-all\">%s</td>\n", fileName));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", report.getPerFileCounts().get(fileName).size()));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm %s font-semibold\">%.2f</td>\n", riskTextClasses(level), report.getFileRiskScore(fileName)));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm\"><span class=\"px-3 py-1 %s text-xs font-semibold rounded-full\">%s</span></td>\n", riskBadgeClasses(level), level.name()));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-blue-600 font-semibold\">%.2f</td>\n", report.getFileMagnitudeScore(fileName)));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-green-600 font-semibold\">%.4f</td>\n", report.getFileDensityScore(fileName)));
                sb.append("                        </tr>\n");
            }

            sb.append("                    </tbody>\n");
            sb.append("                </table>\n");
            sb.append("            </div>\n");
            sb.append("        </section>\n");
        }

        // Aggregate Counts Table
        sb.append("        <section class=\"mb-12\">\n");
        sb.append("            <h2 class=\"text-2xl font-bold text-gray-800 mb-6\">Aggregate PII Counts</h2>\n");
        sb.append("            <div class=\"bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden\">\n");
        sb.append("                <table class=\"min-w-full divide-y divide-gray-200\">\n");
        sb.append("                    <thead class=\"bg-gray-50\">\n");
        sb.append("                        <tr>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">PII Type</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Count</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Severity</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Risk</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Weight</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Magnitude</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Confidence Interval</th>\n");
        sb.append("                        </tr>\n");
        sb.append("                    </thead>\n");
        sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");

        final Map<String, Integer> aggregate = report.getAggregateCounts();
        final Map<String, Double> weights = report.getWeights();
        final Map<String, PhinderReport.ConfidenceStats> aggregateConfidence = report.getAggregateConfidence();

        if (aggregate.isEmpty()) {
            sb.append("                        <tr>\n");
            sb.append("                            <td colspan=\"7\" class=\"px-6 py-4 text-sm text-gray-500 italic\">No PII detected.</td>\n");
            sb.append("                        </tr>\n");
        } else {
            // Highest-risk types first, so the reader sees what matters before what is merely common.
            final List<String> sortedTypes = new ArrayList<>(aggregate.keySet());
            sortedTypes.sort(Comparator
                    .comparingDouble((String type) -> typeRisk(report, type, aggregate, aggregateConfidence)).reversed()
                    .thenComparing(Comparator.naturalOrder()));

            for (final String type : sortedTypes) {
                final int count = aggregate.get(type);
                final double weight = weights.getOrDefault(type, 1.0);
                final double magnitude = count * weight;
                final PhinderReport.ConfidenceStats stats = aggregateConfidence.get(type);
                final String confidenceInterval = stats != null ?
                        String.format("%.2f - %.2f (avg: %.2f)", stats.getMin(), stats.getMax(), stats.getAverage()) : "N/A";

                sb.append("                        <tr>\n");
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900\">%s</td>\n", type));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", count));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%.2f</td>\n", report.getSeverity(type)));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-red-600\">%.2f</td>\n", typeRisk(report, type, aggregate, aggregateConfidence)));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%.2f</td>\n", weight));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-blue-600\">%.2f</td>\n", magnitude));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%s</td>\n", confidenceInterval));
                sb.append("                        </tr>\n");
            }
        }

        sb.append("                    </tbody>\n");
        sb.append("                </table>\n");
        sb.append("            </div>\n");
        sb.append("        </section>\n");


        // Per-File Details
        sb.append("        <section>\n");
        sb.append("            <h2 class=\"text-2xl font-bold text-gray-800 mb-6\">Per-File Details</h2>\n");

        for (final String fileName : files) {
            final Map<String, Integer> counts = report.getPerFileCounts().get(fileName);
            final RiskScorer.RiskLevel level = report.getFileRiskLevel(fileName);
            sb.append("            <div class=\"bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-8\">\n");
            sb.append("                <div class=\"bg-gray-50 px-6 py-4 border-b border-gray-100 flex flex-wrap justify-between items-center\">\n");
            sb.append(String.format("                    <h3 class=\"text-lg font-semibold text-gray-800 break-all mr-4\">%s</h3>\n", fileName));
            sb.append("                    <div class=\"flex space-x-4\">\n");
            sb.append(String.format("                        <span class=\"px-3 py-1 %s text-xs font-semibold rounded-full\">Risk: %.2f (%s)</span>\n", riskBadgeClasses(level), report.getFileRiskScore(fileName), level.name()));
            sb.append(String.format("                        <span class=\"px-3 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded-full\">Magnitude: %.2f</span>\n", report.getFileMagnitudeScore(fileName)));
            sb.append(String.format("                        <span class=\"px-3 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full\">Density: %.4f</span>\n", report.getFileDensityScore(fileName)));
            sb.append("                    </div>\n");
            sb.append("                </div>\n");
            sb.append("                <table class=\"min-w-full divide-y divide-gray-200\">\n");
            sb.append("                    <thead class=\"bg-gray-50\">\n");
            sb.append("                        <tr>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">PII Type</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Count</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Severity</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Risk</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Weight</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Magnitude</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Confidence Interval</th>\n");
            sb.append("                        </tr>\n");
            sb.append("                    </thead>\n");
            sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");

            final Map<String, PhinderReport.ConfidenceStats> fileConfStats = report.getPerFileConfidence().get(fileName);

            if (counts.isEmpty()) {
                sb.append("                        <tr>\n");
                sb.append("                            <td colspan=\"7\" class=\"px-6 py-4 text-sm text-gray-500 italic\">No PII detected.</td>\n");
                sb.append("                        </tr>\n");
            } else {
                final List<String> sortedTypes = new ArrayList<>(counts.keySet());
                sortedTypes.sort(Comparator
                        .comparingDouble((String type) -> typeRisk(report, type, counts, fileConfStats)).reversed()
                        .thenComparing(Comparator.naturalOrder()));

                for (final String type : sortedTypes) {
                    final int count = counts.get(type);
                    final double weight = weights.getOrDefault(type, 1.0);
                    final double magnitude = count * weight;
                    final PhinderReport.ConfidenceStats stats = fileConfStats != null ? fileConfStats.get(type) : null;
                    final String confidenceInterval = stats != null ?
                            String.format("%.2f - %.2f (avg: %.2f)", stats.getMin(), stats.getMax(), stats.getAverage()) : "N/A";

                    sb.append("                        <tr>\n");
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900\">%s</td>\n", type));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", count));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%.2f</td>\n", report.getSeverity(type)));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-red-600\">%.2f</td>\n", typeRisk(report, type, counts, fileConfStats)));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%.2f</td>\n", weight));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-blue-600\">%.2f</td>\n", magnitude));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%s</td>\n", confidenceInterval));
                    sb.append("                        </tr>\n");
                }
            }
            sb.append("                    </tbody>\n");
            sb.append("                </table>\n");
            sb.append("            </div>\n");
        }

        sb.append("        </section>\n");
        sb.append("        <footer class=\"mt-12 text-center text-gray-400 text-sm border-t border-gray-200 pt-8\">\n");
        sb.append("            Generated by Phinder - Copyright 2026 <a href=\"https://www.philterd.ai\" class=\"text-blue-500 hover:underline\">Philterd, LLC</a>\n");
        sb.append("        </footer>\n");
        sb.append("    </div>\n");
        sb.append("</body>\n");
        sb.append("</html>");

        FileUtils.writeStringToFile(file, sb.toString(), StandardCharsets.UTF_8);
    }

    // How much a single entity type contributes to a Risk Score, so a table row can show where the
    // score came from and rows can be ordered by it.
    private static double typeRisk(final PhinderReport report, final String type,
                                   final Map<String, Integer> counts,
                                   final Map<String, PhinderReport.ConfidenceStats> confidence) {
        return RiskScorer.score(
                counts.getOrDefault(type, 0),
                report.getSeverity(type),
                confidence == null ? null : confidence.get(type));
    }

}
