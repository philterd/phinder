# Reports

At the end of a scan, Phinder generates a report summarizing the findings.

## Report Formats

Phinder supports three report formats:

| Format | Option | Description |
|--------|--------|-------------|
| **Text** | `text` | A human-readable text file (default). |
| **PDF** | `pdf` | A structured PDF document with tables. |
| **JSON** | `json` | A machine-readable JSON file. |

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

## Report Content

Every report includes:
1. **Aggregate Magnitude Score**: Total magnitude across all files.
2. **Aggregate Density Score**: Magnitude score divided by total word count across all files.
3. **Aggregate PII Counts**: Total number of occurrences for each PII type.
4. **Per-file Details**: For each file, the Magnitude and Density Scores, and the counts of each PII type found.
