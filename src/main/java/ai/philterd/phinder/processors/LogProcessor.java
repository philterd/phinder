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
import java.util.List;

public class LogProcessor implements DocumentProcessor {

    @Override
    public String extractText(File file) throws IOException {
        // We are overriding process() directly for log files to avoid loading the whole file into memory.
        // This method is not used for log files, but we must implement it to satisfy the interface.
        throw new UnsupportedOperationException("LogProcessor does not support extractText() to avoid memory issues.");
    }

    @Override
    public List<Span> process(File file, Policy policy, Phinder phinder) throws Exception {
        final Policy effectivePolicy = (policy != null) ? policy : phinder.createDefaultPolicy();
        List<Span> allSpans = new ArrayList<>();

        // First, check the filename itself for PII.
        allSpans.addAll(phinder.findPii(file.getName(), effectivePolicy));

        // Use a LineIterator to process the file line-by-line, avoiding loading it all into memory.
        int characterOffset = 0;
        try (LineIterator it = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name())) {
            while (it.hasNext()) {
                String line = it.next();
                List<Span> lineSpans = phinder.findPii(line, effectivePolicy);

                // Shift spans by the current character offset
                for (Span span : lineSpans) {
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
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".log");
    }

}
