# Magnitude Score Weights

Phinder calculates a Magnitude Score for each file and an aggregate score for the entire scan. By default, every PII occurrence has a weight of 1.0.

!!! note
    The Magnitude Score measures **how much** PII a file holds, not how sensitive it is. To rank files by sensitivity instead, see [Risk Scoring](risk-scoring.md), which is calculated separately and configured separately. Weights tune the Magnitude Score only and do not affect the Risk Score.

## Custom Weights

You can specify custom weights for different PII types using a `weights.json` file. This allows you to prioritize certain types of PII over others (e.g., a SSN might have a higher magnitude than an email address).

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

The Magnitude Score is calculated as:
`Sum(PII Type Count * PII Type Weight)`

### Density Score

Phinder also calculates a **Density Score**, which is the Magnitude Score divided by the word count of the document:
`Density Score = Magnitude Score / Word Count`

This score helps identify documents with a high concentration of PII relative to their size.

## Weights and Severities

Weights and severities look alike and are easy to confuse:

| | Weight | Severity |
|---|---|---|
| Feeds | Magnitude Score, Density Score | [Risk Score](risk-scoring.md) |
| Default | 1.0 for every type | Per type, from the built-in severity table |
| Option | `--weights` | `--severities` |

Setting one has no effect on the other, so you can tune each for what it measures.
