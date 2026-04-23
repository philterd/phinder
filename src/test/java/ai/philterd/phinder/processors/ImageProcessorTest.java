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
import static org.junit.jupiter.api.Assertions.*;

public class ImageProcessorTest {

    @Test
    public void testSupports() {
        final ImageProcessor processor = new ImageProcessor();
        assertTrue(processor.supports("image/png", "test.png"));
        assertTrue(processor.supports("image/jpeg", "test.jpg"));
        assertTrue(processor.supports("image/jpeg", "test.jpeg"));
        assertFalse(processor.supports("image/gif", "test.gif"));
        assertFalse(processor.supports("image/png", "test.bmp"));
        assertFalse(processor.supports("text/plain", "test.txt"));
    }

    // Actual OCR test might fail if Tesseract is not installed in the environment.
    // The previous sessions showed it's not installed locally.
}
