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
