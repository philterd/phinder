# Phinder

Phinder is a Java application that uses the [Phileas](https://github.com/philterd/phileas) library to identify PII (Personally Identifiable Information) in text across a wide variety of file formats.

## Key Features

- **Multi-format Support**: Process Plain Text, PDF, Word, Excel, PowerPoint, CSV, RTF, Emails, Log files, and Images.
- **Recursive Processing**: Scan entire directories and their subdirectories.
- **Customizable Policies**: Use Phileas policies to define what PII to look for.
- **Risk Scoring**: Rank files by how sensitive their PII is, not just how much of it there is, so the riskiest data gets remediated first.
- **Magnitude & Density Scoring**: Calculate scores based on the types, counts, and concentration of PII found.
- **Reporting**: Always generates HTML and JSON reports.
- **High Performance**: Optimized for handling large files and millions of documents.

## How it Works

Phinder extracts text from various file formats and uses Phileas's `PlainTextFilterService` to identify PII spans. It also checks file names for PII. Results are aggregated and can be exported as reports.
