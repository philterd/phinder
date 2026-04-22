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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ReportBuilder {

    private final File reportFile;
    private final String reportFormat;

    public ReportBuilder(File reportFile, String reportFormat) {
        this.reportFile = reportFile != null ? reportFile : new File("report.txt");
        this.reportFormat = reportFormat != null ? reportFormat : "text";
    }

    public void build(PhinderReport report) throws Exception {
        if ("pdf".equalsIgnoreCase(reportFormat)) {
            generatePdfReport(report, reportFile);
        } else if ("json".equalsIgnoreCase(reportFormat)) {
            generateJsonReport(report, reportFile);
        } else if ("html".equalsIgnoreCase(reportFormat)) {
            generateHtmlReport(report, reportFile);
        } else {
            generateTextReport(report, reportFile);
        }

        System.out.println("Report generated: " + reportFile.getAbsolutePath());
    }

    private void generateTextReport(PhinderReport report, File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Phinder PII Report\n");
        sb.append("==================\n\n");

        sb.append(String.format("Aggregate Magnitude Score: %.2f\n", report.getAggregateMagnitudeScore()));
        sb.append(String.format("Aggregate Density Score: %.4f\n", report.getAggregateDensityScore()));
        sb.append(String.format("Files Skipped: %d\n\n", report.getSkippedFiles()));

        sb.append("Aggregate Counts:\n");
        Map<String, Integer> aggregate = report.getAggregateCounts();
        if (aggregate.isEmpty()) {
            sb.append(" - No PII detected.\n");
        } else {
            aggregate.forEach((type, count) -> sb.append(String.format(" - %s: %d\n", type, count)));
        }

        sb.append("\nPer-File Details:\n");
        report.getPerFileCounts().forEach((fileName, counts) -> {
            sb.append(String.format(" - %s (Magnitude Score: %.2f, Density Score: %.4f):\n",
                    fileName, report.getFileMagnitudeScore(fileName), report.getFileDensityScore(fileName)));
            if (counts.isEmpty()) {
                sb.append("   - No PII detected.\n");
            } else {
                counts.forEach((type, count) -> sb.append(String.format("   - %s: %d\n", type, count)));
            }
        });

        FileUtils.writeStringToFile(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private void generateJsonReport(PhinderReport report, File file) throws Exception {
        Map<String, Object> data = new HashMap<>();
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

    private void generatePdfReport(PhinderReport report, File file) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Phinder PII Report");
                contentStream.endText();

                int y = 720;
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText(String.format("Aggregate Magnitude Score: %.2f", report.getAggregateMagnitudeScore()));
                contentStream.endText();
                y -= 20;

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText(String.format("Aggregate Density Score: %.4f", report.getAggregateDensityScore()));
                contentStream.endText();
                y -= 20;

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText(String.format("Files Skipped: %d", report.getSkippedFiles()));
                contentStream.endText();
                y -= 30;

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Aggregate Counts:");
                contentStream.endText();
                y -= 20;

                Map<String, Integer> aggregate = report.getAggregateCounts();
                if (aggregate.isEmpty()) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(60, y);
                    contentStream.showText("- No PII detected.");
                    contentStream.endText();
                    y -= 20;
                } else {
                    for (Map.Entry<String, Integer> entry : aggregate.entrySet()) {
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        contentStream.newLineAtOffset(60, y);
                        contentStream.showText(String.format("- %s: %d", entry.getKey(), entry.getValue()));
                        contentStream.endText();
                        y -= 20;
                    }
                }

                y -= 10;
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Per-File Details:");
                contentStream.endText();
                y -= 20;

                for (Map.Entry<String, Map<String, Integer>> fileEntry : report.getPerFileCounts().entrySet()) {
                    String fileName = fileEntry.getKey();
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(60, y);
                    contentStream.showText(String.format("- %s", fileName));
                    contentStream.endText();
                    y -= 15;

                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    contentStream.newLineAtOffset(70, y);
                    contentStream.showText(String.format("Magnitude Score: %.2f, Density Score: %.4f",
                            report.getFileMagnitudeScore(fileName), report.getFileDensityScore(fileName)));
                    contentStream.endText();
                    y -= 15;

                    Map<String, Integer> counts = fileEntry.getValue();
                    if (counts.isEmpty()) {
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        contentStream.newLineAtOffset(80, y);
                        contentStream.showText("No PII detected.");
                        contentStream.endText();
                        y -= 15;
                    } else {
                        for (Map.Entry<String, Integer> countEntry : counts.entrySet()) {
                            contentStream.beginText();
                            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                            contentStream.newLineAtOffset(80, y);
                            contentStream.showText(String.format("%s: %d", countEntry.getKey(), countEntry.getValue()));
                            contentStream.endText();
                            y -= 15;
                        }
                    }

                    if (y < 50) {
                        break;
                    }
                }
            }

            document.save(file);
        }
    }

    private void generateHtmlReport(PhinderReport report, File file) throws Exception {
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

        // Aggregate Counts Table
        sb.append("        <section class=\"mb-12\">\n");
        sb.append("            <h2 class=\"text-2xl font-bold text-gray-800 mb-6\">Aggregate PII Counts</h2>\n");
        sb.append("            <div class=\"bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden\">\n");
        sb.append("                <table class=\"min-w-full divide-y divide-gray-200\">\n");
        sb.append("                    <thead class=\"bg-gray-50\">\n");
        sb.append("                        <tr>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">PII Type</th>\n");
        sb.append("                            <th class=\"px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider\">Count</th>\n");
        sb.append("                        </tr>\n");
        sb.append("                    </thead>\n");
        sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");

        Map<String, Integer> aggregate = report.getAggregateCounts();
        if (aggregate.isEmpty()) {
            sb.append("                        <tr>\n");
            sb.append("                            <td colspan=\"2\" class=\"px-6 py-4 text-sm text-gray-500 italic\">No PII detected.</td>\n");
            sb.append("                        </tr>\n");
        } else {
            for (Map.Entry<String, Integer> entry : aggregate.entrySet()) {
                sb.append("                        <tr>\n");
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900\">%s</td>\n", entry.getKey()));
                sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", entry.getValue()));
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
        
        report.getPerFileCounts().forEach((fileName, counts) -> {
            sb.append("            <div class=\"bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-8\">\n");
            sb.append("                <div class=\"bg-gray-50 px-6 py-4 border-b border-gray-100 flex flex-wrap justify-between items-center\">\n");
            sb.append(String.format("                    <h3 class=\"text-lg font-semibold text-gray-800 break-all mr-4\">%s</h3>\n", fileName));
            sb.append("                    <div class=\"flex space-x-4\">\n");
            sb.append(String.format("                        <span class=\"px-3 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded-full\">Magnitude: %.2f</span>\n", report.getFileMagnitudeScore(fileName)));
            sb.append(String.format("                        <span class=\"px-3 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full\">Density: %.4f</span>\n", report.getFileDensityScore(fileName)));
            sb.append("                    </div>\n");
            sb.append("                </div>\n");
            sb.append("                <table class=\"min-w-full divide-y divide-gray-200\">\n");
            sb.append("                    <tbody class=\"divide-y divide-gray-200\">\n");
            
            if (counts.isEmpty()) {
                sb.append("                        <tr>\n");
                sb.append("                            <td class=\"px-6 py-4 text-sm text-gray-500 italic\">No PII detected.</td>\n");
                sb.append("                        </tr>\n");
            } else {
                counts.forEach((type, count) -> {
                    sb.append("                        <tr>\n");
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm font-medium text-gray-900 w-1/2\">%s</td>\n", type));
                    sb.append(String.format("                            <td class=\"px-6 py-4 text-sm text-gray-600\">%d</td>\n", count));
                    sb.append("                        </tr>\n");
                });
            }
            
            sb.append("                    </tbody>\n");
            sb.append("                </table>\n");
            sb.append("            </div>\n");
        });

        sb.append("        </section>\n");
        sb.append("        <footer class=\"mt-12 text-center text-gray-400 text-sm border-t border-gray-200 pt-8\">\n");
        sb.append("            Generated by Phinder - Copyright 2026 Philterd, LLC\n");
        sb.append("        </footer>\n");
        sb.append("    </div>\n");
        sb.append("</body>\n");
        sb.append("</html>");

        FileUtils.writeStringToFile(file, sb.toString(), StandardCharsets.UTF_8);
    }
}
