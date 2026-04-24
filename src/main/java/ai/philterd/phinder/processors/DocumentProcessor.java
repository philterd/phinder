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
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phinder.Phinder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface DocumentProcessor {

    String extractText(final File file) throws IOException;

    default List<Span> process(final File file, final Policy policy, final Phinder phinder) throws Exception {
        final String text = extractText(file);

        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        // Include the file name in the text to be searched for PII.
        final String combinedText = file.getName() + "\n" + text;

        return phinder.findPii(combinedText, policy);
    }

    default long getWordCount(final File file) throws IOException {
        final String text = extractText(file);
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return countWords(text);
    }

    default long countWords(final String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    default boolean supports(final String mimeType, final String fileName) {
        return supports(mimeType);
    }

    boolean supports(final String mimeType);

}
