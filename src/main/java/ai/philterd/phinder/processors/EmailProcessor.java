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

import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;

import java.io.File;
import java.io.IOException;

public class EmailProcessor implements DocumentProcessor {

    @Override
    public String extractText(File file) throws IOException {
        String name = file.getName().toLowerCase();
        Email email;

        if (name.endsWith(".eml")) {
            email = EmailConverter.emlToEmail(file);
        } else if (name.endsWith(".msg")) {
            email = EmailConverter.outlookMsgToEmail(file);
        } else {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (email.getSubject() != null) {
            sb.append("Subject: ").append(email.getSubject()).append("\n");
        }
        if (email.getFromRecipient() != null) {
            sb.append("From: ").append(email.getFromRecipient().getName())
                    .append(" <").append(email.getFromRecipient().getAddress()).append(">\n");
        }
        if (email.getPlainText() != null) {
            sb.append("\n").append(email.getPlainText());
        } else if (email.getHTMLText() != null) {
            // Very basic HTML to text conversion if plain text is missing
            sb.append("\n").append(email.getHTMLText().replaceAll("<[^>]*>", " "));
        }

        return sb.toString();
    }

    @Override
    public boolean supports(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".eml") || name.endsWith(".msg");
    }

}
