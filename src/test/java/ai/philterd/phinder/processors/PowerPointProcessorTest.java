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

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PowerPointProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        final PowerPointProcessor processor = new PowerPointProcessor();
        assertTrue(processor.supports("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        assertTrue(processor.supports("application/vnd.ms-powerpoint"));
        assertFalse(processor.supports("text/plain"));
    }

    @Test
    public void testExtractTextPpptx() throws Exception {
        final File pptxFile = tempDir.resolve("test.pptx").toFile();

        try (final XMLSlideShow ppt = new XMLSlideShow();
             final FileOutputStream out = new FileOutputStream(pptxFile)) {
            final XSLFSlide slide = ppt.createSlide();
            final XSLFTextBox shape = slide.createTextBox();
            shape.setText("Email: ppt@example.com");
            ppt.write(out);
        }

        final PowerPointProcessor processor = new PowerPointProcessor();
        final String extracted = processor.extractText(pptxFile);
        assertTrue(extracted.contains("ppt@example.com"));
    }

    @Test
    public void testWordCount() throws Exception {
        final File pptxFile = tempDir.resolve("test.pptx").toFile();
        try (final XMLSlideShow ppt = new XMLSlideShow();
             final FileOutputStream out = new FileOutputStream(pptxFile)) {
            final XSLFSlide slide = ppt.createSlide();
            final XSLFTextBox shape = slide.createTextBox();
            shape.setText("This is a PowerPoint slide.");
            ppt.write(out);
        }

        final PowerPointProcessor processor = new PowerPointProcessor();
        assertEquals(5, processor.getWordCount(pptxFile));
    }
}
