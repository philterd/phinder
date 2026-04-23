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

import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phinder.Phinder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        final CsvProcessor processor = new CsvProcessor(',', '"');
        assertTrue(processor.supports("text/csv"));
        assertFalse(processor.supports("text/plain"));
    }

    @Test
    public void testProcessDefault() throws Exception {
        final File csvFile = tempDir.resolve("test.csv").toFile();
        final String content = "name,email\nJohn Doe,john@example.com";
        FileUtils.writeStringToFile(csvFile, content, StandardCharsets.UTF_8);

        final CsvProcessor processor = new CsvProcessor(',', '"');
        final List<Span> spans = processor.process(csvFile, null, new Phinder());

        assertFalse(spans.isEmpty());
        final boolean found = spans.stream().anyMatch(s -> s.getText().equals("john@example.com"));
        assertTrue(found);
    }

    @Test
    public void testProcessCustomDelimiter() throws Exception {
        final File csvFile = tempDir.resolve("test.csv").toFile();
        final String content = "name|email\nJohn Doe|john@example.com";
        FileUtils.writeStringToFile(csvFile, content, StandardCharsets.UTF_8);

        final CsvProcessor processor = new CsvProcessor('|', '"');
        final List<Span> spans = processor.process(csvFile, null, new Phinder());

        assertFalse(spans.isEmpty());
        final boolean found = spans.stream().anyMatch(s -> s.getText().equals("john@example.com"));
        assertTrue(found);
    }

    @Test
    public void testWordCount() throws Exception {
        final File csvFile = tempDir.resolve("test.csv").toFile();
        final String content = "col1,col2,col3\nval1,val2,val3\nval4,val5,val6";
        FileUtils.writeStringToFile(csvFile, content, StandardCharsets.UTF_8);

        final CsvProcessor processor = new CsvProcessor(',', '"');
        assertEquals(9, processor.getWordCount(csvFile));
    }
}
