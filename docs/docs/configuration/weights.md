# Risk Score Weights

Phinder calculates a Risk Score for each file and an aggregate score for the entire scan. By default, every PII occurrence has a weight of 1.0.

## Custom Weights

You can specify custom weights for different PII types using a `weights.json` file. This allows you to prioritize certain types of PII over others (e.g., a SSN is riskier than an email address).

### `weights.json` Format

The file should be a JSON object mapping PII types (as identified by Phileas) to numeric weights.

```json
{
  "email-address": 2.0,
  "ssn": 10.0,
  "phone-number": 5.0
}
```

## Using Weights

Specify the weights file with the `-w` or `--weights` option:

```bash
java -jar phinder.jar -i documents/ -w my-weights.json
```

## Calculation

The Risk Score is calculated as:
`Sum(PII Type Count * PII Type Weight)`
