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

public class LogProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        final LogProcessor processor = new LogProcessor();
        assertTrue(processor.supports("text/x-log"));
        assertFalse(processor.supports("text/plain"));
        assertTrue(processor.supports("text/plain", "test.log"));
        assertFalse(processor.supports("text/plain", "test.txt"));
    }

    @Test
    public void testProcess() throws Exception {
        final File logFile = tempDir.resolve("test.log").toFile();
        final String content = "2026-04-22 INFO User test@example.com logged in\n" +
                         "2026-04-22 ERROR Failed for admin@example.com";
        FileUtils.writeStringToFile(logFile, content, StandardCharsets.UTF_8);

        final LogProcessor processor = new LogProcessor();
        final List<Span> spans = processor.process(logFile, null, new Phinder());

        assertFalse(spans.isEmpty());
        assertTrue(spans.stream().anyMatch(s -> s.getText().equals("test@example.com")));
        assertTrue(spans.stream().anyMatch(s -> s.getText().equals("admin@example.com")));
    }

    @Test
    public void testWordCount() throws Exception {
        final File logFile = tempDir.resolve("test.log").toFile();
        final String content = "Line one\nLine two with five words";
        FileUtils.writeStringToFile(logFile, content, StandardCharsets.UTF_8);

        final LogProcessor processor = new LogProcessor();
        assertEquals(7, processor.getWordCount(logFile));
    }
}
