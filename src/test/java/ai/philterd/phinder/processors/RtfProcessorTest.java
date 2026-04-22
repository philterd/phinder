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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.text.rtf.RTFEditorKit;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RtfProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        RtfProcessor processor = new RtfProcessor();
        assertTrue(processor.supports(new File("test.rtf")));
        assertFalse(processor.supports(new File("test.txt")));
    }

    @Test
    public void testExtractText() throws Exception {
        File rtfFile = tempDir.resolve("test.rtf").toFile();
        String content = "{\\rtf1\\ansi Email: rtf@example.com}";

        try (FileOutputStream out = new FileOutputStream(rtfFile)) {
            out.write(content.getBytes());
        }

        RtfProcessor processor = new RtfProcessor();
        String extracted = processor.extractText(rtfFile);
        assertTrue(extracted.contains("rtf@example.com"));
    }

    @Test
    public void testWordCount() throws Exception {
        File rtfFile = tempDir.resolve("test.rtf").toFile();
        String content = "{\\rtf1\\ansi One two three four five six.}";
        try (FileOutputStream out = new FileOutputStream(rtfFile)) {
            out.write(content.getBytes());
        }

        RtfProcessor processor = new RtfProcessor();
        assertEquals(6, processor.getWordCount(rtfFile));
    }
}
