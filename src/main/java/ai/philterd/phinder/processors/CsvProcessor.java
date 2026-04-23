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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvProcessor implements DocumentProcessor {

    private static final List<String> ACCEPTABLE_MIME_TYPES = Arrays.asList("text/csv");

    private final char delimiter;
    private final char quote;

    public CsvProcessor() {
        this(',', '"');
    }

    public CsvProcessor(final char delimiter, final char quote) {
        this.delimiter = delimiter;
        this.quote = quote;
    }

    @Override
    public String extractText(final File file) throws IOException {
        throw new UnsupportedOperationException("extractText is not supported for CSV files as it may lead to high memory usage. Use process() instead.");
    }

    @Override
    public List<Span> process(final File file, final Policy policy, final Phinder phinder) throws Exception {
        final Policy effectivePolicy = (policy != null) ? policy : phinder.createDefaultPolicy();
        final List<Span> allSpans = new ArrayList<>();

        // Check filename for PII
        allSpans.addAll(phinder.findPii(file.getName(), effectivePolicy));

        final CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setQuote(quote)
                .build();

        try (final Reader reader = new FileReader(file, StandardCharsets.UTF_8);
             final CSVParser csvParser = new CSVParser(reader, format)) {

            for (final CSVRecord csvRecord : csvParser) {
                final String lineText = String.join(" ", csvRecord.toList());
                if (!lineText.trim().isEmpty()) {
                    final List<Span> lineSpans = phinder.findPii(lineText, effectivePolicy);
                    allSpans.addAll(lineSpans);
                }
            }
        }

        return allSpans;
    }

    @Override
    public long getWordCount(final File file) throws IOException {
        long wordCount = 0;
        final CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setQuote(quote)
                .build();

        try (final Reader reader = new FileReader(file, StandardCharsets.UTF_8);
             final CSVParser csvParser = new CSVParser(reader, format)) {
            for (final CSVRecord csvRecord : csvParser) {
                for (final String field : csvRecord) {
                    wordCount += countWords(field);
                }
            }
        }
        return wordCount;
    }

    @Override
    public boolean supports(final String mimeType) {
        return mimeType != null && ACCEPTABLE_MIME_TYPES.stream().anyMatch(mimeType::equalsIgnoreCase);
    }

}
