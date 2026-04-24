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

To speed up subsequent scans, Phinder can maintain a log of scanned files and their xxHash64 hashes using a MongoDB database.

### MongoDB Configuration

To use the scan log, you must provide a MongoDB connection string via the `PHINDER_MONGODB_URL` environment variable:

```bash
export PHINDER_MONGODB_URL="mongodb://localhost:27017"
```

### Generating a Scan Log

By default, using `--log` or `--skip-unchanged` will enable the scan log using the MongoDB database specified in `PHINDER_MONGODB_URL`.

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
