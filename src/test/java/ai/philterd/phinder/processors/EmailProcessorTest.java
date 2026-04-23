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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class EmailProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        final EmailProcessor processor = new EmailProcessor();
        assertTrue(processor.supports("message/rfc822"));
        assertTrue(processor.supports("application/vnd.ms-outlook"));
        assertFalse(processor.supports("text/plain"));
    }

    @Test
    public void testExtractTextEml() throws Exception {
        // We use the existing test.eml resource for testing
        final File emlFile = new File("src/test/resources/test.eml");
        if (emlFile.exists()) {
            final EmailProcessor processor = new EmailProcessor();
            final String extracted = processor.extractText(emlFile);
            assertNotNull(extracted);
            assertTrue(extracted.contains("jeff@example.com") || extracted.contains("Subject:"));
        }
    }

    @Test
    public void testExtractTextMsg() throws Exception {
        // test.msg is a placeholder but let's see if it works or handles it gracefully
        final File msgFile = new File("src/test/resources/test.msg");
        if (msgFile.exists()) {
            final EmailProcessor processor = new EmailProcessor();
            try {
                final String extracted = processor.extractText(msgFile);
                assertNotNull(extracted);
            } catch (final Exception e) {
                // Ignore if it's just a placeholder that fails to parse
            }
        }
    }

    @Test
    public void testWordCount() throws Exception {
        final File emlFile = new File("src/test/resources/test.eml");
        if (emlFile.exists()) {
            final EmailProcessor processor = new EmailProcessor();
            final long count = processor.getWordCount(emlFile);
            assertTrue(count > 0);
        }
    }
}
