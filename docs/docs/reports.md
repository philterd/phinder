# Reports

At the end of a scan, Phinder generates a report summarizing the findings.

## Report Formats

Phinder supports four report formats:

| Format | Option | Description |
|--------|--------|-------------|
| **Text** | `text` | A human-readable text file (default). |
| **PDF** | `pdf` | A structured PDF document. |
| **JSON** | `json` | A machine-readable JSON file. |
| **HTML** | `html` | A modern, visually attractive HTML report styled with Tailwind CSS. |

## Generating a Report

Use the `-r` or `--report` option to specify the output path, and `-f` or `--format` to specify the format.

### Examples

**Default (Text report to `report.txt`):**
```bash
java -jar phinder.jar -i input.txt
```

**Custom PDF Report:**
```bash
java -jar phinder.jar -i documents/ -r results.pdf -f pdf
```

**Machine-readable JSON Report:**
```bash
java -jar phinder.jar -i documents/ -r results.json -f json
```

**Modern HTML Report:**
```bash
java -jar phinder.jar -i documents/ -r results.html -f html
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
