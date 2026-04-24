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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogProcessor implements DocumentProcessor {

    private static final List<String> ACCEPTABLE_MIME_TYPES = Arrays.asList(
            "text/x-log",
            "application/x-log"
    );

    @Override
    public String extractText(final File file) throws IOException {
        // We are overriding process() directly for log files to avoid loading the whole file into memory.
        // This method is not used for log files, but we must implement it to satisfy the interface.
        throw new UnsupportedOperationException("LogProcessor does not support extractText() to avoid memory issues.");
    }

    @Override
    public List<Span> process(final File file, final Policy policy, final Phinder phinder) throws Exception {
        final Policy effectivePolicy = (policy != null) ? policy : phinder.createDefaultPolicy();
        final List<Span> allSpans = new ArrayList<>();

        // First, check the filename itself for PII.
        allSpans.addAll(phinder.findPii(file.getName(), effectivePolicy));

        // Use a LineIterator to process the file line-by-line, avoiding loading it all into memory.
        int characterOffset = 0;
        try (final LineIterator it = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name())) {
            while (it.hasNext()) {
                final String line = it.next();
                final List<Span> lineSpans = phinder.findPii(line, effectivePolicy);

                // Shift spans by the current character offset
                for (final Span span : lineSpans) {
                    span.setCharacterStart(span.getCharacterStart() + characterOffset);
                    span.setCharacterEnd(span.getCharacterEnd() + characterOffset);
                    allSpans.add(span);
                }

                // Increment offset by line length plus 1 for the newline character
                characterOffset += line.length() + 1;
            }
        }

        return allSpans;
    }

    @Override
    public long getWordCount(final File file) throws IOException {
        long wordCount = 0;
        try (final LineIterator it = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name())) {
            while (it.hasNext()) {
                wordCount += countWords(it.next());
            }
        }
        return wordCount;
    }

    @Override
    public boolean supports(final String mimeType, final String fileName) {
        if ("text/plain".equalsIgnoreCase(mimeType) && fileName != null && fileName.toLowerCase().endsWith(".log")) {
            return true;
        }

        return supports(mimeType);
    }

    @Override
    public boolean supports(final String mimeType) {
        return mimeType != null && ACCEPTABLE_MIME_TYPES.stream().anyMatch(mimeType::equalsIgnoreCase);
    }

}
