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
package ai.philterd.phinder.processors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PdfProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        final PdfProcessor processor = new PdfProcessor();
        assertTrue(processor.supports("application/pdf"));
        assertFalse(processor.supports("text/plain"));
    }

    @Test
    public void testExtractText() throws Exception {
        final File pdfFile = tempDir.resolve("test.pdf").toFile();
        final String content = "Email: test-pdf@example.com";

        try (final PDDocument document = new PDDocument()) {
            final PDPage page = new PDPage();
            document.addPage(page);
            try (final PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(content);
                contentStream.endText();
            }
            document.save(pdfFile);
        }

        final PdfProcessor processor = new PdfProcessor();
        final String extracted = processor.extractText(pdfFile);
        assertTrue(extracted.contains(content));
    }

    @Test
    public void testWordCount() throws Exception {
        final File pdfFile = tempDir.resolve("test.pdf").toFile();
        try (final PDDocument document = new PDDocument()) {
            final PDPage page = new PDPage();
            document.addPage(page);
            try (final PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("One two three four five.");
                contentStream.endText();
            }
            document.save(pdfFile);
        }

        final PdfProcessor processor = new PdfProcessor();
        assertEquals(5, processor.getWordCount(pdfFile));
    }
}
