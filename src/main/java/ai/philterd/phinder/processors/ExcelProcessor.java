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

public class ExcelProcessor implements DocumentProcessor {

    @Override
    public String extractText(File file) throws IOException {
        String name = file.getName().toLowerCase();
        try (FileInputStream fis = new FileInputStream(file)) {
            try (Workbook workbook = WorkbookFactory.create(fis)) {
                if (workbook instanceof HSSFWorkbook) {
                    try (ExcelExtractor extractor = new ExcelExtractor((HSSFWorkbook) workbook)) {
                        return extractor.getText();
                    }
                } else if (workbook instanceof XSSFWorkbook) {
                    try (XSSFExcelExtractor extractor = new XSSFExcelExtractor((XSSFWorkbook) workbook)) {
                        return extractor.getText();
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to extract text from Excel file: " + file.getName(), e);
        }
        return null;
    }

    @Override
    public boolean supports(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".xlsx") || name.endsWith(".xls");
    }

}
