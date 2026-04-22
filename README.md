# Phinder

A Java application that uses the [Phileas](https://github.com/philterd/phileas) library to identify PII (Personally Identifiable Information) in text.

## Getting Started

### Build the project

```bash
mvn clean install
```

### Run the application

First, build the project to create the runnable JAR:

```bash
mvn clean package
```

Then, run the application using `java -jar`:

```bash
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i input.txt
```

To provide a custom Phileas policy (JSON):

```bash
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i input.txt -p policy.json
```

## CLI Options

- `-i, --input=<inputFile>`: The input text file (required).
- `-p, --policy=<policyFile>`: The Phileas policy (JSON file).
- `-h, --help`: Show help message.
- `-V, --version`: Print version information.

## Example Script

An `example.sh` script is provided to demonstrate the CLI usage.

```bash
chmod +x example.sh
./example.sh
```

## Usage

The `Phinder` class provides a `findPii` method that returns a list of identified spans:

```java
Phinder phinder = new Phinder();
List<Span> spans = phinder.findPii("Contact me at test@example.com");
```
