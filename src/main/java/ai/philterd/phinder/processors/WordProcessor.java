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

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WordProcessor implements DocumentProcessor {

    @Override
    public String extractText(File file) throws IOException {
        String name = file.getName().toLowerCase();
        try (FileInputStream fis = new FileInputStream(file)) {
            if (name.endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(fis)) {
                    try (XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                        return extractor.getText();
                    }
                }
            } else if (name.endsWith(".doc")) {
                try (HWPFDocument doc = new HWPFDocument(fis)) {
                    try (WordExtractor extractor = new WordExtractor(doc)) {
                        return extractor.getText();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean supports(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".docx") || name.endsWith(".doc");
    }

}
