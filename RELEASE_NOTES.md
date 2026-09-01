# Release Notes

Notable changes to Phinder, most recent first.

## 1.0.0 - Unreleased

### Added

- **Risk scoring for discovered PII.** Phinder now scores every file, and the scan as a whole, by how sensitive the PII in it is rather than only by how much of it there is: `Risk Score = Sum(severity * count * confidenceFactor)`, reported alongside a risk level from `NONE` to `CRITICAL`. Each entity type has a built-in severity (a Social Security number outranks a city name out of the box), overridable per type with `--severities <file>`. The per-file sections of both reports are ordered by Risk Score by default, with `--sort-by risk|magnitude|density|name` to reorder, `--min-risk` to omit low-risk files from the listing, and `--top` to size the candidates table. Risk figures and the severities in effect are written to `report.json`, `report.html`, and the MongoDB report history. The Magnitude and Density Scores are unchanged: weights still default to 1.0 and tune those scores only, so existing reports keep their existing numbers. The Risk Score is a prioritization aid, not a compliance determination or a measure of regulatory exposure.
- **Starter redaction policy generation.** A new `--emit-policy <file>` option turns a scan into a starter [Philter](https://www.philterd.ai/philter/) / [Phileas](https://www.philterd.ai/phileas/) redaction policy: it enables each detected entity type with a `REDACT` strategy, so discovery and redaction become one workflow. It writes JSON by default, or [PhiSQL](https://github.com/philterd/phisql) when the output file ends in `.phisql` (validated against the PhiSQL compiler before it is written). The output loads unchanged into Philter or Phileas. Types that need a supplied custom policy to detect (custom identifiers, sections, or PhEye name detection) are reported as skipped rather than guessed at. The generated policy is a starting point to review, tune, and measure (for example with Philter Scope), not a guarantee; redaction is probabilistic and you are responsible for validating the policy against your own data.

### Changed

- **Now built on Phileas 4.2.0 and Java 25** (previously Phileas 3.3.0 and Java 21), and depends on PhiSQL 1.3.0 for the PhiSQL policy output.
- **Dependencies updated.** Apache Tika 4.0.0, the MongoDB driver 5.11.0, simple-java-mail 9.3.2, and JUnit Jupiter 6.1.3 are major-version upgrades; Apache POI 5.5.1, Tess4J 5.20.0, commons-csv 1.14.1, commons-io 2.22.0, PDFBox 3.0.8, picocli 4.7.7, lz4-java 1.11.2, and mongo-java-server 1.47.0 (test) are minor or patch upgrades. Build plugins moved to maven-compiler 3.15.0, maven-shade 3.6.2, and central-publishing 0.11.0. Log4j is deliberately held at 2.25.4: the only newer release is 3.0.0-beta3, and a beta does not belong in a published build.
