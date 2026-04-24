# Reports

Phinder always generates an HTML and JSON report (`report.html` and `report.json`).

## Report Formats

| Format | File | Description |
|--------|--------|-------------|
| **HTML** | `report.html` | A modern, visually attractive HTML report. |
| **JSON** | `report.json` | A machine-readable JSON file. |

## Generating a Report

Reports are always generated at the end of a scan. 

### Example

```bash
java -jar phinder.jar -i input.txt
```

## Report Content

Every report includes:
1. **Report ID**: A unique UUID for the scan.
2. **Timestamp**: The date and time the scan was completed.
3. **Aggregate Magnitude Score**: Total magnitude across all files.
4. **Aggregate Density Score**: Magnitude score divided by total word count across all files.
5. **Aggregate PII Counts**: Total number of occurrences for each PII type.
6. **Files Skipped**: The number of files skipped because they hadn't changed since the last scan.
7. **PII Weights**: The weights used for each PII type (if custom weights were provided).
8. **Best Candidates for Redaction Testing**: A table of the top 20 files with the most PII variety and highest scores (HTML format only).
9. **Per-file Details**: For each file, the Magnitude and Density Scores, and the counts of each PII type found.

## Report History

If the `--mongodb` option is used, the report will also be stored in the specified MongoDB database.

```bash
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt --mongodb "mongodb://localhost:27017/phinder"
```
