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
import java.util.List;

public class CsvProcessor implements DocumentProcessor {

    private final char delimiter;
    private final char quote;

    public CsvProcessor() {
        this(',', '"');
    }

    public CsvProcessor(char delimiter, char quote) {
        this.delimiter = delimiter;
        this.quote = quote;
    }

    @Override
    public String extractText(File file) throws IOException {
        throw new UnsupportedOperationException("extractText is not supported for CSV files as it may lead to high memory usage. Use process() instead.");
    }

    @Override
    public List<Span> process(File file, Policy policy, Phinder phinder) throws Exception {
        final Policy effectivePolicy = (policy != null) ? policy : phinder.createDefaultPolicy();
        List<Span> allSpans = new ArrayList<>();

        // Check filename for PII
        allSpans.addAll(phinder.findPii(file.getName(), effectivePolicy));

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setQuote(quote)
                .build();

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, format)) {

            for (CSVRecord csvRecord : csvParser) {
                String lineText = String.join(" ", csvRecord.toList());
                if (!lineText.trim().isEmpty()) {
                    List<Span> lineSpans = phinder.findPii(lineText, effectivePolicy);
                    allSpans.addAll(lineSpans);
                }
            }
        }

        return allSpans;
    }

    @Override
    public long getWordCount(File file) throws IOException {
        long wordCount = 0;
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setQuote(quote)
                .build();

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, format)) {
            for (CSVRecord csvRecord : csvParser) {
                for (String field : csvRecord) {
                    wordCount += countWords(field);
                }
            }
        }
        return wordCount;
    }

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".csv");
    }

}
