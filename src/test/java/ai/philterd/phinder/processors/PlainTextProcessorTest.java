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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PlainTextProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        PlainTextProcessor processor = new PlainTextProcessor();
        assertTrue(processor.supports(new File("test.txt")));
        assertTrue(processor.supports(new File("test.md")));
        assertFalse(processor.supports(new File("test.pdf")));
        assertFalse(processor.supports(new File("test.DOCX")));
    }

    @Test
    public void testExtractTextTxt() throws Exception {
        File txtFile = tempDir.resolve("test.txt").toFile();
        String content = "Email: test@example.com";
        FileUtils.writeStringToFile(txtFile, content, StandardCharsets.UTF_8);

        PlainTextProcessor processor = new PlainTextProcessor();
        String extracted = processor.extractText(txtFile);
        assertEquals(content, extracted.trim());
    }

    @Test
    public void testExtractTextMd() throws Exception {
        File mdFile = tempDir.resolve("test.md").toFile();
        String content = "# Markdown\nEmail: test@example.md";
        FileUtils.writeStringToFile(mdFile, content, StandardCharsets.UTF_8);

        PlainTextProcessor processor = new PlainTextProcessor();
        String extracted = processor.extractText(mdFile);
        assertEquals(content, extracted.trim());
    }

    @Test
    public void testWordCount() throws Exception {
        File txtFile = tempDir.resolve("test.txt").toFile();
        String content = "One two three four.";
        FileUtils.writeStringToFile(txtFile, content, StandardCharsets.UTF_8);

        PlainTextProcessor processor = new PlainTextProcessor();
        assertEquals(4, processor.getWordCount(txtFile));
    }
}
