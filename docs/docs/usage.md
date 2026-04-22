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

## Large File Support

Phinder is designed to handle very large files efficiently. Specifically, `.log` files are processed line-by-line to minimize heap usage.

## Scan Logging and Skipping

To speed up subsequent scans, Phinder can maintain a log of scanned files and their xxHash64 hashes.

### Generating a Scan Log

By default, using `--log` or `--skip-unchanged` will use `scan.json`.

```bash
java -jar phinder.jar -i /path/to/data --log
```

### Skipping Unchanged Files

On the next run, use `--skip-unchanged` to skip any file that hasn't changed (based on its xxHash64 hash):

```bash
java -jar phinder.jar -i /path/to/data --skip-unchanged
```

If you have a specific scan log from a previous session, you can provide it using `--log`:

```bash
java -jar phinder.jar -i /path/to/data --log previous_scan.json --skip-unchanged
```

Phinder will compare the file names and hash values in `previous_scan.json` to determine if a file can be skipped. The count of skipped files will be included in the reports.
