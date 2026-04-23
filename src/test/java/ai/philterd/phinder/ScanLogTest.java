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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ScanLogTest {

    @TempDir
    Path tempDir;

    @Test
    public void testScanLogLifecycle() throws Exception {
        final File dbFile = tempDir.resolve("scan").toFile();
        final ScanLog scanLog = new ScanLog(dbFile);
        
        try {
            final String filePath = "/path/to/file.txt";
            final String hash = "hash123";
            
            assertNull(scanLog.getFileHash(filePath));
            
            scanLog.putFileHash(filePath, hash);
            assertEquals(hash, scanLog.getFileHash(filePath));
            
            scanLog.addScannedPath("/some/dir");
            assertTrue(scanLog.getScannedPaths().contains("/some/dir"));
            
            scanLog.clean();
            assertNull(scanLog.getFileHash(filePath));
            assertTrue(scanLog.getScannedPaths().isEmpty());
        } finally {
            scanLog.close();
        }
    }
}
