# Phinder

A Java application that uses the [Phileas](https://github.com/philterd/phileas) library to identify PII (Personally Identifiable Information) in text across a wide variety of file formats.

Types of PII are scored by magnitude, density, and confidence. A list of files suggested for redaction testing will be generated. 

The goal of Phinder is to provide a comprehensive analysis of PII to help you take the next step to redact it with [Philter](https://github.com/philterd/philter).

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

For more examples and detailed usage, please refer to the [Usage Guide](docs/docs/usage.md).

## License

Copyright 2026 Philterd, LLC.

This project is licensed under the Apache License 2.0.
