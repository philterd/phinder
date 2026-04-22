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

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RtfProcessor implements DocumentProcessor {

    @Override
    public String extractText(File file) throws IOException {
        RTFEditorKit rtfKit = new RTFEditorKit();
        Document doc = new DefaultStyledDocument();
        try (InputStream is = new FileInputStream(file)) {
            rtfKit.read(is, doc, 0);
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            throw new IOException("Failed to extract text from RTF file: " + file.getName(), e);
        }
    }

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".rtf");
    }

}
