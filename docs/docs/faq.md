# Frequently Asked Questions

### What is Phinder?

Phinder is a command-line application that identifies Personally Identifiable Information (PII) in various file formats, such as PDF, Word, Excel, and more. It uses the [Phileas](https://github.com/philterd/philterd/phileas) library for PII detection.

### Which file formats are supported?

Phinder supports a wide range of formats including:
- Plain Text (`.txt`, `.log`)
- Adobe PDF (`.pdf`)
- Microsoft Word (`.doc`, `.docx`)
- Microsoft Excel (`.xls`, `.xlsx`)
- Microsoft PowerPoint (`.ppt`, `.pptx`)
- Comma-Separated Values (`.csv`)
- Rich Text Format (`.rtf`)
- Email Messages (`.eml`, `.msg`)
- Images (`.png`, `.jpg`, `.tiff`) via OCR

### How do I define what PII to look for?

You can provide a Phileas policy file in JSON format using the `-p` or `--policy` option. This policy defines the types of PII (e.g., names, SSNs, credit card numbers) to detect.

### Can I scan entire directories?

Yes, Phinder can scan a single file or an entire directory. Use the `-R` or `--recursive` flag to scan subdirectories as well.

### How does Phinder handle large files?

Phinder is designed for high performance. Large log files are processed line-by-line to minimize memory usage, and other formats are handled efficiently using specialized processors.

### What are Magnitude and Density scores?

- **Magnitude Score**: A weighted score representing the total amount of PII found in a document. You can customize the weights for different PII types.
- **Density Score**: The ratio of PII findings to the total amount of text in the document.

### How can I speed up subsequent scans?

You can use the `--log` and `--skip-unchanged` options. Phinder will maintain a log (in a MongoDB database) of the files it has already scanned and their hashes. On subsequent runs, it will skip files that haven't changed. You must provide the MongoDB connection string via the `PHINDER_MONGODB_URL` environment variable.

### In what formats are the reports generated?

Phinder always generates both HTML (`.html`) and JSON (`.json`) reports.

### How is OCR handled for images?

Phinder uses Tesseract for OCR. You may need to set the `TESSDATA_PREFIX` environment variable to the directory containing your Tesseract language data files (e.g., `eng.traineddata`).

### Does Phinder require an internet connection?

No, Phinder performs all PII detection locally using the Phileas library and does not send your data to any external services.

### Does Phinder redact the PII?

No, Phinder is designed for **identification and reporting** of PII. If you need to redact or anonymize PII, consider using [Philter](https://www.philterd.ai).
