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
1. **Aggregate Magnitude Score**: Total magnitude across all files.
2. **Aggregate Density Score**: Magnitude score divided by total word count across all files.
3. **Aggregate PII Counts**: Total number of occurrences for each PII type.
4. **Files Skipped**: The number of files skipped because they hadn't changed since the last scan.
5. **PII Weights**: The weights used for each PII type (if custom weights were provided).
6. **Best Candidates for Redaction Testing**: A table of the top 20 files with the most PII variety and highest scores (HTML format only).
7. **Per-file Details**: For each file, the Magnitude and Density Scores, and the counts of each PII type found.
