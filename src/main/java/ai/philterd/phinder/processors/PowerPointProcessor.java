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

import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PowerPointProcessor implements DocumentProcessor {

    private static final List<String> ACCEPTABLE_MIME_TYPES = Arrays.asList(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint"
    );

    @Override
    public String extractText(final File file) throws IOException {
        try (final FileInputStream fis = new FileInputStream(file)) {
            try (final SlideShow<?, ?> slideshow = SlideShowFactory.create(fis)) {
                try (final SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideshow)) {
                    extractor.setCommentsByDefault(true);
                    extractor.setNotesByDefault(true);
                    return extractor.getText();
                }
            }
        } catch (final Exception e) {
            throw new IOException("Failed to extract text from PowerPoint file: " + file.getName(), e);
        }
    }

    @Override
    public boolean supports(final String mimeType) {
        return mimeType != null && ACCEPTABLE_MIME_TYPES.stream().anyMatch(mimeType::equalsIgnoreCase);
    }

}
