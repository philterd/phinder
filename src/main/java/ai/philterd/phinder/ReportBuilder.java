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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportBuilder {

    public ReportBuilder() {
    }

    public void build(PhinderReport report) throws Exception {
        // Always generate the HTML report.
        File htmlReportFile = new File("report.html");

        // Always generate the JSON report.
        File jsonReportFile = new File("report.json");

        generateHtmlReport(report, htmlReportFile);
        System.out.println("HTML report generated: " + htmlReportFile.getAbsolutePath());

        generateJsonReport(report, jsonReportFile);
        System.out.println("JSON report generated: " + jsonReportFile.getAbsolutePath());
    }

    public static void generateJsonReport(PhinderReport report, File file) throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String readableTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(report.getTimestamp()), ZoneId.systemDefault()).format(formatter);

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", readableTimestamp);
        data.put("weights", report.getWeights());
        data.put("aggregateMagnitudeScore", report.getAggregateMagnitudeScore());
        data.put("aggregateDensityScore", report.getAggregateDensityScore());
        data.put("skippedFiles", report.getSkippedFiles());
        data.put("aggregateCounts", report.getAggregateCounts());

        Map<String, Object> perFileDetails = new HashMap<>();
        report.getPerFileCounts().forEach((fileName, counts) -> {
            Map<String, Object> fileDetail = new HashMap<>();
            fileDetail.put("magnitudeScore", report.getFileMagnitudeScore(fileName));
            fileDetail.put("densityScore", report.getFileDensityScore(fileName));
            fileDetail.put("counts", counts);
            perFileDetails.put(fileName, fileDetail);
        });
        data.put("perFileDetails", perFileDetails);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(data);

        FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
    }

    public static void generateHtmlReport(PhinderReport report, File file) throws Exception {
        StringBuilder sb = new StringBuilder();
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
        sb.append("            <p class=\"text-lg text-gray-600\">Personally Identifiable Information (PII) detection summary.</p>\n");
        sb.append(String.format("            <p class=\"text-sm text-gray-400 mt-2\">Report generated on %s</p>\n",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(report.getTimestamp()), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        sb.append("        </header>\n");

        // Aggregate Summary Cards
        sb.append("        <section class=\"grid grid-cols-1 md:grid-cols-3 gap-6 mb-12\">\n");
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

        // Best Candidates for Redaction Testing
        List<String> files = new ArrayList<>(report.getPerFileCounts().keySet());
        if (!files.isEmpty()) {
            // Sort by Magnitude Score (desc) then by Variety (desc) then by Density Score (desc)
            files.sort((f1, f2) -> {
                int compare = Double.compare(report.getFileMagnitudeScore(f2), report.getFileMagnitudeScore(f1));
                if (compare == 0) {
                    compare = Integer.compare(report.getPerFileCounts().get(f2).size(), report.getPerFileCounts().get(f1).size());
                }
                if (compare == 0) {
                    compare = Double.compare(report.getFileDensityScore(f2), report.getFileDensityScore(f1));
                }
                return compare;
            });

            List<String> bestCandidates = files.subList(0, Math.min(20, files.size()));

            sb.append("        <section class=\"mb-12\">\n");
            sb.append("            <h2 class=\"text-2xl font-bold text-gray-800 mb-6\">Best Candidates for Redaction Testing</h2>\n");
            sb.append("            <div class=\"bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden\">\n");
            sb.append("                <table class=\"min-w-full divide-y divide-gray-200\">\n");
            sb.append("                    <thead class=\"bg-gray-50\">\n");
            sb.append("                        <tr>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">File Name</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">PII Variety</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Magnitude Score</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Density Score</th>\n");
            sb.append("                        </tr>\n");
            sb.append("                    </thead>\n");
            sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");

            for (String fileName : bestCandidates) {
                sb.append("                        <tr>\n");
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900 break-all\">%s</td>\n", fileName));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", report.getPerFileCounts().get(fileName).size()));
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
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Weight</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Magnitude</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Confidence Interval</th>\n");
        sb.append("                        </tr>\n");
        sb.append("                    </thead>\n");
        sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");

        Map<String, Integer> aggregate = report.getAggregateCounts();
        Map<String, Double> weights = report.getWeights();
        Map<String, PhinderReport.ConfidenceStats> aggregateConfidence = report.getAggregateConfidence();

        if (aggregate.isEmpty()) {
            sb.append("                        <tr>\n");
            sb.append("                            <td colspan=\"5\" class=\"px-6 py-4 text-sm text-gray-500 italic\">No PII detected.</td>\n");
            sb.append("                        </tr>\n");
        } else {
            List<String> sortedTypes = new ArrayList<>(aggregate.keySet());
            Collections.sort(sortedTypes);

            for (String type : sortedTypes) {
                int count = aggregate.get(type);
                double weight = weights.getOrDefault(type, 1.0);
                double magnitude = count * weight;
                PhinderReport.ConfidenceStats stats = aggregateConfidence.get(type);
                String confidenceInterval = stats != null ?
                        String.format("%.2f - %.2f (avg: %.2f)", stats.getMin(), stats.getMax(), stats.getAverage()) : "N/A";

                sb.append("                        <tr>\n");
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900\">%s</td>\n", type));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", count));
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

        List<String> sortedFiles = new ArrayList<>(report.getPerFileCounts().keySet());
        Collections.sort(sortedFiles);

        for (String fileName : sortedFiles) {
            Map<String, Integer> counts = report.getPerFileCounts().get(fileName);
            sb.append("            <div class=\"bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-8\">\n");
            sb.append("                <div class=\"bg-gray-50 px-6 py-4 border-b border-gray-100 flex flex-wrap justify-between items-center\">\n");
            sb.append(String.format("                    <h3 class=\"text-lg font-semibold text-gray-800 break-all mr-4\">%s</h3>\n", fileName));
            sb.append("                    <div class=\"flex space-x-4\">\n");
            sb.append(String.format("                        <span class=\"px-3 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded-full\">Magnitude: %.2f</span>\n", report.getFileMagnitudeScore(fileName)));
            sb.append(String.format("                        <span class=\"px-3 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full\">Density: %.4f</span>\n", report.getFileDensityScore(fileName)));
            sb.append("                    </div>\n");
            sb.append("                </div>\n");
            sb.append("                <table class=\"min-w-full divide-y divide-gray-200\">\n");
            sb.append("                    <thead class=\"bg-gray-50\">\n");
            sb.append("                        <tr>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">PII Type</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Count</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Weight</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Magnitude</th>\n");
            sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Confidence Interval</th>\n");
            sb.append("                        </tr>\n");
            sb.append("                    </thead>\n");
            sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");

            if (counts.isEmpty()) {
                sb.append("                        <tr>\n");
                sb.append("                            <td colspan=\"5\" class=\"px-6 py-4 text-sm text-gray-500 italic\">No PII detected.</td>\n");
                sb.append("                        </tr>\n");
            } else {
                List<String> sortedTypes = new ArrayList<>(counts.keySet());
                Collections.sort(sortedTypes);
                Map<String, PhinderReport.ConfidenceStats> fileConfStats = report.getPerFileConfidence().get(fileName);

                for (String type : sortedTypes) {
                    int count = counts.get(type);
                    double weight = weights.getOrDefault(type, 1.0);
                    double magnitude = count * weight;
                    PhinderReport.ConfidenceStats stats = fileConfStats != null ? fileConfStats.get(type) : null;
                    String confidenceInterval = stats != null ?
                            String.format("%.2f - %.2f (avg: %.2f)", stats.getMin(), stats.getMax(), stats.getAverage()) : "N/A";

                    sb.append("                        <tr>\n");
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900\">%s</td>\n", type));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", count));
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
}
