# Getting Started

## Requirements

- Java 21 or higher
- Maven 3.x
- Tesseract OCR (optional, required for processing images)

## Installation

Clone the repository and build the project using Maven:

```bash
mvn clean install
```

This will create a runnable shaded JAR in the `target/` directory.

## Quick Start

To scan a single text file:

```bash
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i input.txt
```

To scan a directory:

```bash
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i /path/to/my/files/
```
