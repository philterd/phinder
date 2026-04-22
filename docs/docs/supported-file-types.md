# Supported File Types

Phinder identifies PII in both the file contents and the file name.

| Category | Extensions | Notes |
|----------|------------|-------|
| **Plain Text** | `.txt`, `.md` | Markdown is supported. |
| **Documents** | `.pdf`, `.docx`, `.doc`, `.rtf` | |
| **Spreadsheets** | `.xlsx`, `.xls`, `.csv` | CSV delimiter and quoting can be customized. |
| **Presentations** | `.pptx`, `.ppt` | Extracts from slides, notes, and comments. |
| **Email** | `.eml`, `.msg` | Extracts subject, sender, and body. |
| **Logs** | `.log` | Processed line-by-line for memory efficiency. |
| **Images** | `.png`, `.jpg`, `.jpeg` | Requires Tesseract OCR installed. |

!!! note
    Processing images requires `tesseract-ocr` to be installed on your system.
