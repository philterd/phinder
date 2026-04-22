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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PdfProcessor implements DocumentProcessor {

    @Override
    public String extractText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // If the extracted text is empty or very short, try OCR
            if (text == null || text.trim().length() < 10) {
                StringBuilder sb = new StringBuilder();
                if (text != null) {
                    sb.append(text);
                }

                PDFRenderer renderer = new PDFRenderer(document);
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, 300);
                    String ocrText = OcrUtil.extractText(image);
                    if (ocrText != null) {
                        sb.append(ocrText);
                    }
                }
                text = sb.toString();
            }

            return text;
        }
    }

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".pdf");
    }

}
