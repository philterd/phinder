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
        PowerPointProcessor processor = new PowerPointProcessor();
        assertTrue(processor.supports(new File("test.pptx")));
        assertTrue(processor.supports(new File("test.ppt")));
        assertFalse(processor.supports(new File("test.txt")));
    }

    @Test
    public void testExtractTextPpptx() throws Exception {
        File pptxFile = tempDir.resolve("test.pptx").toFile();

        try (XMLSlideShow ppt = new XMLSlideShow();
             FileOutputStream out = new FileOutputStream(pptxFile)) {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox shape = slide.createTextBox();
            shape.setText("Email: ppt@example.com");
            ppt.write(out);
        }

        PowerPointProcessor processor = new PowerPointProcessor();
        String extracted = processor.extractText(pptxFile);
        assertTrue(extracted.contains("ppt@example.com"));
    }

    @Test
    public void testWordCount() throws Exception {
        File pptxFile = tempDir.resolve("test.pptx").toFile();
        try (XMLSlideShow ppt = new XMLSlideShow();
             FileOutputStream out = new FileOutputStream(pptxFile)) {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox shape = slide.createTextBox();
            shape.setText("This is a PowerPoint slide.");
            ppt.write(out);
        }

        PowerPointProcessor processor = new PowerPointProcessor();
        assertEquals(5, processor.getWordCount(pptxFile));
    }
}
