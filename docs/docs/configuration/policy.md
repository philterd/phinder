# Phileas Policy

Phinder uses the Phileas library to perform PII detection. You can provide a custom Phileas policy in JSON format using the `-p` or `--policy` option.

## Example Policy

A policy defines which identifiers (PII types) to enable.

```json
{
  "identifiers": {
    "emailAddress": {
      "emailAddressFilterStrategies": [
        {
          "strategy": "REDACT",
          "redaction": "[[EMAIL]]"
        }
      ]
    },
    "ssn": {
      "ssnFilterStrategies": [
        {
          "strategy": "REDACT",
          "redaction": "[[SSN]]"
        }
      ]
    }
  }
}
```

## Applying the Policy

```bash
java -jar phinder.jar -i input.txt -p my-policy.json
```

If no policy is provided, Phinder uses a default policy that looks for email addresses.
