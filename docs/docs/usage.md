# Usage Guide

## Processing Files

You can specify one or more files using the `-i` or `--input` option:

```bash
java -jar phinder.jar -i file1.txt -i file2.pdf
```

## Processing Directories

To process all supported files in a directory:

```bash
java -jar phinder.jar -i /path/to/directory
```

### Recursive Search

Use the `-R` or `--recursive` flag to traverse subdirectories:

```bash
java -jar phinder.jar -i /path/to/directory -R
```

## CSV Customization

When processing CSV files, you can specify the delimiter and quote character:

```bash
java -jar phinder.jar -i data.csv --csv-delimiter=";" --csv-quote="'"
```

## Generating a Starter Redaction Policy

Phinder can turn a scan into a starter [Philter](https://www.philterd.ai/philter/) / [Phileas](https://www.philterd.ai/phileas/) redaction policy, so discovery and redaction become one workflow. Add `--emit-policy` with an output file:

```bash
java -jar phinder.jar -i my_docs/ -R --emit-policy starter-policy.json
```

The flow is scan, then policy, then apply:

1. **Scan.** Phinder finds the entity types present across the inputs.
2. **Policy.** It writes redaction-policy JSON that enables each detected type with a `REDACT` strategy. The output loads unchanged into Philter or Phileas. Types that need a supplied custom policy to detect (custom identifiers, sections, or PhEye name detection) are reported as skipped rather than guessed at.
3. **Apply.** Use the policy with Philter or Phileas directly, or tune it first (change strategies, add conditions, ignore terms) and re-run.

This is a starting point to review, tune, and measure (for example with [Philter Scope](https://www.philterd.ai/philter-scope/)) before you rely on it. Redaction is probabilistic: it reduces how much sensitive data gets through, it does not catch every instance, and you are responsible for validating the policy against your own data.

## Large File Support

Phinder is designed to handle very large files efficiently. Specifically, `.log` files are processed line-by-line to minimize heap usage.

## Scan Logging and Skipping

To speed up subsequent scans, Phinder can maintain a log of scanned files and their xxHash64 hashes using a MongoDB database.

### MongoDB Configuration

To use the scan log, you must provide a MongoDB connection string via the `PHINDER_MONGODB_URI` environment variable:

```bash
export PHINDER_MONGODB_URI="mongodb://localhost:27017"
```

### Generating a Scan Log

By default, using `--log` or `--skip-unchanged` will enable the scan log using the MongoDB database specified in `PHINDER_MONGODB_URI`.

```bash
java -jar phinder.jar -i /path/to/data --log
```

### Skipping Unchanged Files

On the next run, use `--skip-unchanged` to skip any file that hasn't changed (based on its xxHash64 hash):

```bash
java -jar phinder.jar -i /path/to/data --skip-unchanged
```

Phinder will compare the file names and hash values in the MongoDB database to determine if a file can be skipped. The count of skipped files will be included in the reports.

When using the scan log, Phinder also stores a timestamped copy of the scan results in a `reports` collection in the MongoDB database. This allows for historical tracking of scan results.

### Cleaning the Scan Log

To clear the recorded hashes and paths from the scan log database, use the `--clean` option:

```bash
java -jar phinder.jar --clean
```

This will delete the entries in the scan log collections, forcing all files to be re-scanned on the next run with `--skip-unchanged`.
