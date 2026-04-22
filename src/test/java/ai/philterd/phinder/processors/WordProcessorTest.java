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

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class WordProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        WordProcessor processor = new WordProcessor();
        assertTrue(processor.supports(new File("test.docx")));
        assertTrue(processor.supports(new File("test.doc")));
        assertFalse(processor.supports(new File("test.txt")));
    }

    @Test
    public void testExtractTextDocx() throws Exception {
        File docxFile = tempDir.resolve("test.docx").toFile();
        String content = "Word document content with test@example.com";

        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(docxFile)) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(content);
            document.write(out);
        }

        WordProcessor processor = new WordProcessor();
        String extracted = processor.extractText(docxFile);
        assertTrue(extracted.contains("test@example.com"));
    }

    @Test
    public void testWordCount() throws Exception {
        File docxFile = tempDir.resolve("test.docx").toFile();
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(docxFile)) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("This is a six word sentence.");
            document.write(out);
        }

        WordProcessor processor = new WordProcessor();
        assertEquals(6, processor.getWordCount(docxFile));
    }
}
