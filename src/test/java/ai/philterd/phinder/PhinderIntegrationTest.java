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
package ai.philterd.phinder;

import ai.philterd.phileas.model.filtering.Span;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PhinderIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    public void testFindPii() throws Exception {
        final Phinder phinder = new Phinder();
        final String text = "Contact me at test@example.com";
        final List<Span> spans = phinder.findPii(text);

        assertFalse(spans.isEmpty(), "Should have found at least one PII span");
        assertTrue(spans.stream().anyMatch(s -> s.getText().equals("test@example.com")));
    }

    @Test
    public void testCliHelp() {
        final Phinder phinder = new Phinder();
        final CommandLine cmd = new CommandLine(phinder);
        final int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
    }

    @Test
    public void testMultipleFilesIntegration() throws Exception {
        final File file1 = tempDir.resolve("file1.txt").toFile();
        final File file2 = tempDir.resolve("file2.txt").toFile();
        FileUtils.writeStringToFile(file1, "Email: one@example.com", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(file2, "Email: two@example.com", StandardCharsets.UTF_8);

        final Phinder phinder = new Phinder();
        final CommandLine cmd = new CommandLine(phinder);
        final int exitCode = cmd.execute("-i", file1.getAbsolutePath(), "-i", file2.getAbsolutePath());
        
        assertEquals(0, exitCode);
    }

    @Test
    public void testDirectoryProcessingRecursive() throws Exception {
        final Path subDir = tempDir.resolve("subdir");
        subDir.toFile().mkdir();
        final File file1 = tempDir.resolve("file1.txt").toFile();
        final File file2 = subDir.resolve("file2.txt").toFile();
        FileUtils.writeStringToFile(file1, "Email: one@example.com", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(file2, "Email: two@example.com", StandardCharsets.UTF_8);

        final Phinder phinder = new Phinder();
        final CommandLine cmd = new CommandLine(phinder);
        final int exitCode = cmd.execute("-i", tempDir.toFile().getAbsolutePath(), "--recursive");
        
        assertEquals(0, exitCode);
    }

    @Test
    public void testCustomWeightsFromFile() throws Exception {
        final File weightsFile = tempDir.resolve("weights.json").toFile();
        FileUtils.writeStringToFile(weightsFile, "{\"email-address\": 10.0}", StandardCharsets.UTF_8);
        
        final File inputFile = tempDir.resolve("input.txt").toFile();
        FileUtils.writeStringToFile(inputFile, "test@example.com", StandardCharsets.UTF_8);

        final Phinder phinder = new Phinder();
        final CommandLine cmd = new CommandLine(phinder);
        final int exitCode = cmd.execute("-i", inputFile.getAbsolutePath(), "-w", weightsFile.getAbsolutePath());
        
        assertEquals(0, exitCode);
    }
}
