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
3. **Aggregate Risk Score and Risk Level**: How sensitive the PII across all files is. See [Risk Scoring](configuration/risk-scoring.md).
4. **Aggregate Magnitude Score**: Total magnitude across all files.
5. **Aggregate Density Score**: Magnitude score divided by total word count across all files.
6. **Aggregate PII Counts**: Total number of occurrences for each PII type.
7. **Files Skipped**: The number of files skipped because they hadn't changed since the last scan.
8. **PII Weights and Severities**: The weights used for the Magnitude Score, and the severity used for each detected PII type in the Risk Score.
9. **Best Candidates for Redaction Testing**: A table of the highest-scoring files, capped by `--top` (HTML format only).
10. **Per-file Details**: For each file, the Risk Score and Risk Level, the Magnitude and Density Scores, and the counts of each PII type found.

## Ordering and Filtering

The per-file sections are ordered by Risk Score by default, so the files worth remediating first appear first. Use `--sort-by risk|magnitude|density|name` to change the ordering, `--min-risk` to leave low-risk files out, and `--top` to size the candidates table:

```bash
java -jar phinder.jar -i my_docs/ -R --sort-by risk --min-risk 50 --top 10
```

`--min-risk` changes which files are listed, not which files were scanned. Aggregate scores and counts always cover the whole scan, and the JSON report records both `filesReported` and `filesScanned`.

## Report History

If the `--mongodb` option is used, the report will also be stored in the specified MongoDB database.

```bash
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt --mongodb "mongodb://localhost:27017/phinder"
```
