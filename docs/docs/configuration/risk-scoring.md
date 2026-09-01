# Risk Scoring

Phinder assigns a **Risk Score** to each file and to the scan as a whole, so you can decide what to remediate first. Unlike the [Magnitude Score](weights.md), which counts occurrences, the Risk Score accounts for how sensitive each entity type is. A file with two Social Security numbers ranks above a file with fifty city names, even though the second file has far more PII in it.

## Calculation

For each entity type found, Phinder multiplies three things and sums the result:

```
Risk Score = Sum over types of ( severity * count * confidenceFactor )
```

| Term | Meaning |
|------|---------|
| `severity` | How damaging one leaked value of that type is. See the table below. |
| `count` | How many occurrences of that type were found. |
| `confidenceFactor` | `0.5 + (0.5 * average detection confidence)`, so a value between 0.5 and 1.0. |

Confidence discounts a finding, it never erases it. A detector that is unsure still found something worth a look, so a zero-confidence hit counts half as much as a certain one rather than nothing at all.

## Risk Levels

Each score is reported alongside a band, so a report is readable without doing arithmetic:

| Level | Risk Score |
|-------|-----------|
| `NONE` | 0 |
| `LOW` | Above 0 and below 10 |
| `MEDIUM` | 10 to below 50 |
| `HIGH` | 50 to below 250 |
| `CRITICAL` | 250 and above |

## Default Severities

Severities are ordinal judgements about how much damage one leaked value does, not measurements. They are the starting point Phinder ships with so that a Social Security number does not rank alongside a city name out of the box. Tune them for your own data and threat model.

| Severity | Entity types |
|----------|--------------|
| 10.0 | `bank-routing-number`, `credit-card`, `drivers-license-number`, `iban-code`, `passport-number`, `ssn` |
| 8.0 | `bitcoin-address`, `medical-condition` |
| 5.0 | `email-address`, `id`, `person`, `pheye`, `phone-number`, `physician-name`, `street-address`, `vin` |
| 3.0 | `age`, `custom-dictionary`, `first-name`, `ip-address`, `surname` |
| 2.0 | `date`, `hospital`, `hospital-abbreviation`, `mac-address`, `phone-number-extension`, `tracking-number`, `zip-code` |
| 1.0 | `city`, `county`, `currency`, `other`, `section`, `state`, `state-abbreviation`, `url` |

An entity type that is not in this table, such as one produced by a custom identifier in your own policy, gets a severity of 1.0.

## Custom Severities

Pass a JSON object mapping entity types to severities with `--severities`. Types you do not name keep their default.

```json
{
  "medical-condition": 10.0,
  "first-name": 5.0,
  "url": 0.5
}
```

```bash
java -jar phinder.jar -i documents/ -R --severities my-severities.json
```

The severities in effect for the types a scan found are written into `report.json` and the HTML report, so a report always records the scoring it was produced with.

## Sorting and Filtering

The per-file sections of a report are ordered by Risk Score by default. Change the ordering with `--sort-by`, drop low-risk files with `--min-risk`, and set how many files appear in the "Best Candidates for Redaction Testing" table with `--top`:

```bash
java -jar phinder.jar -i documents/ -R --sort-by risk --min-risk 50 --top 10
```

`--min-risk` changes which files are **listed**, not which files were scanned. The aggregate scores and counts always cover the whole scan, and `report.json` records both `filesReported` and `filesScanned` so the difference is never hidden.

## Risk Score and Magnitude Score

The two scores answer different questions and are calculated independently:

| | Magnitude Score | Risk Score |
|---|---|---|
| Question | How much PII is in this file? | How sensitive is the PII in this file? |
| Per-type factor | Weight, default 1.0 for every type | Severity, from the table above |
| Uses confidence | No | Yes |
| Configured with | `--weights` | `--severities` |

Because they are independent, a `weights.json` file tunes the Magnitude Score only and cannot move the Risk Score, and vice versa.

## What the Risk Score Is Not

The Risk Score is a prioritization aid built from the entity types a scan found. It is not a measure of regulatory exposure or a compliance determination. It does not know how a file is stored, who can read it, how the values combine across records, or whether the detections are correct in the first place: detection is probabilistic, and a scan may both miss PII and report text that is not PII. Review the findings against your own data before acting on the ranking, and treat the score as a way to order your work rather than as a verdict on it.
