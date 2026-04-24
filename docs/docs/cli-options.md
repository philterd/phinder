# CLI Options

Phinder is a command-line application. Below is a list of all available options.

| Option | Long Option        | Description                                                                            |
|--------|--------------------|----------------------------------------------------------------------------------------|
| `-i`   | `--input`          | **Required.** The input file(s) or directory/directories to scan. Repeat for multiple. |
| `-R`   | `--recursive`      | Recursively traverse subdirectories if directories are provided.                       |
| `-p`   | `--policy`         | Path to a custom Phileas policy (JSON file).                                           |
| `-w`   | `--weights`        | Path to custom PII weights (JSON file).                                                |
| `-l`   | `--log`            | Enable the scan log using a MongoDB database.                                         |
| `-s`   | `--skip-unchanged` | Skip scanning files that haven't changed since the last scan log.                      |
|        | `--clean`          | Truncate the scan log database.                                                        |
|        | `--csv-delimiter`  | Custom CSV delimiter character. (Default: `,`)                                         |
|        | `--csv-quote`      | Custom CSV quote character. (Default: `"`)                                             |
| `-h`   | `--help`           | Show the help message and exit.                                                        |
| `-V`   | `--version`        | Show the application version and exit.                                                 |

## Examples

### Scan multiple files
    
```bash
java -jar phinder.jar -i file1.txt -i file2.docx
```

### Scan a directory recursively with custom weights

```bash
java -jar phinder.jar -i my_docs/ -R -w weights.json
```
