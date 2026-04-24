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

import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ExcelProcessor implements DocumentProcessor {

    private static final List<String> ACCEPTABLE_MIME_TYPES = Arrays.asList(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel"
    );

    @Override
    public String extractText(final File file) throws IOException {
        final String name = file.getName().toLowerCase();
        try (final FileInputStream fis = new FileInputStream(file)) {
            try (final Workbook workbook = WorkbookFactory.create(fis)) {
                if (workbook instanceof HSSFWorkbook) {
                    try (final ExcelExtractor extractor = new ExcelExtractor((HSSFWorkbook) workbook)) {
                        return extractor.getText();
                    }
                } else if (workbook instanceof XSSFWorkbook) {
                    try (final XSSFExcelExtractor extractor = new XSSFExcelExtractor((XSSFWorkbook) workbook)) {
                        return extractor.getText();
                    }
                }
            }
        } catch (final Exception e) {
            throw new IOException("Failed to extract text from Excel file: " + file.getName(), e);
        }
        return null;
    }

    @Override
    public boolean supports(final String mimeType) {
        return mimeType != null && ACCEPTABLE_MIME_TYPES.stream().anyMatch(mimeType::equalsIgnoreCase);
    }

}
