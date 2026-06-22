# Release Notes

Notable changes to Phinder, most recent first.

## 1.0.0 - Unreleased

### Added

- **Starter redaction policy generation.** A new `--emit-policy <file>` option turns a scan into a starter [Philter](https://www.philterd.ai/philter/) / [Phileas](https://www.philterd.ai/phileas/) redaction policy: it enables each detected entity type with a `REDACT` strategy, so discovery and redaction become one workflow. It writes JSON by default, or [PhiSQL](https://github.com/philterd/phisql) when the output file ends in `.phisql` (validated against the PhiSQL compiler before it is written). The output loads unchanged into Philter or Phileas. Types that need a supplied custom policy to detect (custom identifiers, sections, or PhEye name detection) are reported as skipped rather than guessed at. The generated policy is a starting point to review, tune, and measure (for example with Philter Scope), not a guarantee; redaction is probabilistic and you are responsible for validating the policy against your own data.

### Changed

- **Now built on Phileas 4.1.0 and Java 25** (previously Phileas 3.3.0 and Java 21), and depends on PhiSQL 1.1.0 for the PhiSQL policy output.
