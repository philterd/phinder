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
        sb.append(String.format("Aggregate Density Score: %.4f\n\n", report.getAggregateDensityScore()));

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
}
