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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSupports() {
        final ExcelProcessor processor = new ExcelProcessor();
        assertTrue(processor.supports("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertTrue(processor.supports("application/vnd.ms-excel"));
        assertFalse(processor.supports("text/plain"));
    }

    @Test
    public void testExtractTextXlsx() throws Exception {
        final File xlsxFile = tempDir.resolve("test.xlsx").toFile();

        try (final Workbook workbook = new XSSFWorkbook();
             final FileOutputStream out = new FileOutputStream(xlsxFile)) {
            final Sheet sheet = workbook.createSheet("Sheet1");
            final Row row = sheet.createRow(0);
            final Cell cell = row.createCell(0);
            cell.setCellValue("Email: excel@example.com");
            workbook.write(out);
        }

        final ExcelProcessor processor = new ExcelProcessor();
        final String extracted = processor.extractText(xlsxFile);
        assertTrue(extracted.contains("excel@example.com"));
    }

    @Test
    public void testWordCount() throws Exception {
        final File xlsxFile = tempDir.resolve("test.xlsx").toFile();
        try (final Workbook workbook = new XSSFWorkbook();
             final FileOutputStream out = new FileOutputStream(xlsxFile)) {
            final Sheet sheet = workbook.createSheet("Sheet1");
            final Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("OneWord");
            workbook.write(out);
        }

        final ExcelProcessor processor = new ExcelProcessor();
        final long count = processor.getWordCount(xlsxFile);
        assertTrue(count >= 1);
    }
}
