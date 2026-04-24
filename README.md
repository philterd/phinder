# Phinder

A Java application that uses the [Phileas](https://github.com/philterd/phileas) library to identify PII (Personally Identifiable Information) in text across a wide variety of file formats. Types of PII are scored by magnitude, density, and confidence. A list of files suggested for redaction testing will be generated. The goal of Phinder is to provide a comprehensive analysis of PII to help you take the next step to redact it with [Philter](https://github.com/philterd/philter).

Visit http://philterd.github.io/phinder for documentation and more information.

![Phinder](docs/report.png)

## Quick Start

### Build the project

```bash
mvn clean install
```

### Run Phinder

```bash
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt
```

To process a directory recursively:
 
 ```bash
 java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/ -R
 ```
 
 ### Use MongoDB to skip unchanged files
 
 Using a MongoDB database allows Phinder to track scanned files and skip those that haven't changed in subsequent runs.
 
 ```bash
 export PHINDER_MONGODB_URL="mongodb://localhost:27017"
 java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/ --log --skip-unchanged
 ```
 
 For more examples and detailed usage, please refer to the [documentation](http://philterd.github.io/phinder).

## License

Copyright 2026 Philterd, LLC.

This project is licensed under the Apache License 2.0.
