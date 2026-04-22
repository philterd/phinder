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

import java.io.File;
import java.io.IOException;

public class ImageProcessor implements DocumentProcessor {

    @Override
    public String extractText(File file) throws IOException {
        Tesseract tesseract = new Tesseract();
        
        // In many environments, the tessdata is in a standard location.
        // For Tess4J on some systems, it might be bundled or need a path.
        // We'll assume it can find it or is configured via environment.
        // In the Dockerfile, we will ensure it's in a known place.
        String datapath = System.getenv("TESSDATA_PREFIX");
        if (datapath != null) {
            tesseract.setDatapath(datapath);
        }

        try {
            return tesseract.doOCR(file);
        } catch (TesseractException e) {
            throw new IOException("Failed to extract text from image: " + file.getName(), e);
        }
    }

    @Override
    public boolean supports(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

}
