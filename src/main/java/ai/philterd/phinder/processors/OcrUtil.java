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

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class OcrUtil {

    private static Tesseract tesseract;

    public static synchronized Tesseract getTesseract() {
        if (tesseract == null) {
            tesseract = new Tesseract();
            final String datapath = System.getenv("TESSDATA_PREFIX");
            if (datapath != null) {
                tesseract.setDatapath(datapath);
            }
        }
        return tesseract;
    }

    public static String extractText(final File file) throws IOException {
        try {
            return getTesseract().doOCR(file);
        } catch (final TesseractException e) {
            throw new IOException("Failed to extract text via OCR from file: " + file.getName(), e);
        }
    }

    public static String extractText(final BufferedImage image) throws IOException {
        try {
            return getTesseract().doOCR(image);
        } catch (final TesseractException e) {
            throw new IOException("Failed to extract text via OCR from image.", e);
        }
    }
}
