---
name: iban-commons
description: A zero-dependency Java toolkit for IBAN/BIC validation, formatting, and bank-data lookup
---

# IBAN Commons

## Project Overview

**IBAN Commons** is an ultra-fast, allocation-free, zero-dependency Java toolkit for **IBAN** (ISO 13616) and **BIC/SWIFT** (ISO 9362) validation, parsing, and formatting. Supports 127 countries, Java 8+, Android-compatible (API 21+).

### Key Features

- Structural IBAN/BIC validation and formatting, no runtime dependencies in the core module
- Per-country national check digit (NCD) calculators
- Optional Jakarta Bean Validation constraints (`@ValidIban`, `@ValidBic`)
- Optional German domestic account check digit verification (Bankleitzahl-based)
- Optional bank-data lookup (BIC/name by IBAN or bank code) with a bundled offline fallback and lazy background refresh
- JUnit 5 test extensions (`@IbanCountrySource`, `@RandomIbanSource`, `@IbanRegistrySource`)

### Modules

| Module                        | Package root                                      | Description                                                                      |
|--------------------------------|---------------------------------------------------|-----------------------------------------------------------------------------------|
| `iban-commons`                 | `de.speedbanking.iban`, `.bic`, `.util`, `.about` | Core: `Iban`/`Bic` models, validators, registries, country/currency data |
| `iban-commons-validation`      | `de.speedbanking.validation`                      | Jakarta Bean Validation constraints (`@ValidIban`, `@ValidBic`)                   |
| `iban-commons-de-checkdigit`   | `de.speedbanking.checkdigit.de`                   | German Kontonummer check digit methods (standalone, **no dependency on `iban-commons`**) |
| `iban-commons-bankdata`        | `de.speedbanking.bankdata`                        | BIC/bank-name lookup by IBAN or bank code, with offline fallback + refresh         |
| `iban-commons-junit`           | `de.speedbanking.iban.junit`                      | JUnit 5 `@ParameterizedTest` source extensions for IBAN test data                  |

Only `iban-commons-de-checkdigit` has zero internal dependencies; every other module depends on `iban-commons` (core).

## Architecture

```
iban-commons/
├── iban-commons/
│   └── src/main/java/de/speedbanking/
│       ├── iban/             # Iban, IbanBuilder, IbanRegistry, IbanValidator, NationalCheckDigitCalculators
│       │   └── util/         # IbanPatternConverter (structure-notation → regex)
│       ├── bic/              # Bic, BicValidator, RandomBic
│       ├── util/             # CharUtil, Mod97, Country/Currency/Continent enums, UtilityClasses
│       └── about/            # ThisLib (manifest-driven CLI metadata entry point)
├── iban-commons-validation/
│   └── src/main/java/de/speedbanking/validation/     # IbanConstraintValidator, ValidIban, ValidBic
├── iban-commons-de-checkdigit/
│   └── src/main/java/de/speedbanking/checkdigit/de/  # GermanAccountCheckDigit
├── iban-commons-bankdata/
│   └── src/main/java/de/speedbanking/bankdata/
│       ├── loader/           # per-country CountryBankDataLoader implementations
│       ├── refresh/          # CountryDataCache, RefreshExecutors (background refresh)
│       ├── io/               # Downloader, BankDataFormat, MinimalXlsxReader
│       ├── log/              # NaiveLogger / NaiveLogFormatter (no logging dependency)
│       └── spi/              # CountryBankDataLoader SPI, BankDataParseException
└── iban-commons-junit/
    └── src/main/java/de/speedbanking/iban/junit/jupiter/params/provider/
```

## Key Conventions

These are established, non-obvious conventions - follow them rather than defaulting to generic Java style:

- **Private-constructor guard for utility classes**: call `UtilityClasses.cannotInstantiate(getClass())` (in `de.speedbanking.util`, `iban-commons` core) instead of repeating the message. Exception: `iban-commons-de-checkdigit` has no dependency on `iban-commons`, so its one utility class (`GermanAccountCheckDigit`) inlines `String.format("Utility class %s cannot be instantiated", getClass().getSimpleName())` instead. Test-source utility classes also inline this (they don't pull in the main-source helper).
- **No `final` on local variables or method parameters**.
- **Tabular alignment of members** but not local variables.
- **Exception/log messages use `String.format` with `%s`-style placeholders**, not string concatenation. Logger messages specifically (via the project's own minimal logging, e.g. `NaiveLogger` in `bankdata`) use `{0}`-style `MessageFormat` placeholders instead — the two message styles are not interchangeable, match whichever the surrounding class already uses.
- **Static-import `java.nio.charset.StandardCharsets.UTF_8`/`UTF_16`** rather than qualifying `StandardCharsets.UTF_8` inline — this is enforced indirectly by error-prone's `StaticImport` check.
- **Checkstyle import order** (`build/checkstyle-rules.xml`, `ImportOrder` module): groups are, in order — `de.speedbanking.*` statics, other statics (e.g. `org.assertj.*`), `jakarta`/`java`/`javafx`/`javax` statics; then the same grouping for regular imports. Each group is blank-line-separated and alphabetically sorted within itself. When adding a static import from `java.*`, it goes *after* non-`de.speedbanking`/non-`java` statics like `org.assertj`, not before.
- **`package-info.java` carries no license header** — `license-maven-plugin` excludes `**/package-info.java`.
- **No separate `package-info.java` in `src/test`** when the corresponding main package already has one — avoids a split-package duplicate `package-info.class` on the classpath.
- **Logger field is named `LOGGER`** (not `log`/`logger`).
- Avoid 'em dashes' and curly quotes in Javadoc, Markdown, and commit messages — this codebase's prose stays plain-ASCII punctuation.

## Testing Conventions

- **JUnit 5** throughout; AssertJ for assertions (`assertThat(...)`), not Hamcrest or JUnit's own assertions.
- **Test method naming**: `methodUnderTest_condition_expectedOutcome()` — compact, no `should`/`when` filler words. Example:
  ```java
  void resolveActualSourceUri_htmlWithoutMatchingLink_returnsEmpty() { ... }
  void parse_rowWithTooFewColumns_throwsBankDataParseException() { ... }
  ```
  Some older tests in the `iban`/`bic`/`util` packages still use a verbose `method_shouldOutcome_whenCondition()` style — don't copy that pattern into new tests, prefer the compact form above.
- **Prefer `@ParameterizedTest` with `@CsvSource`** over separate single-assertion test methods when covering several input/output combinations of the same behavior. Typical shape:
  ```java
  @ParameterizedTest(name = "[{index}] offset={0}, length={1} ({2})")
  @CsvSource(delimiter = '|', value = {
      "-1 |  5 | negative offset",
      " 0 | -1 | negative length",
  })
  void constructor_invalidRange_throwsIndexOutOfBoundsException(int offset, int length, String reason) { ... }
  ```
  Add `@DisplayName` for the group when the parameterized intent isn't obvious from the method name alone.
- **No `@Nested` test classes**, as a rule — keep test classes flat. (One pre-existing exception in `IbanComponentTest`; don't follow it as a model for new tests.)
- Favor the project's own JUnit 5 source extensions (`@IbanCountrySource`, `@RandomIbanSource`, `@IbanRegistrySource` from `iban-commons-junit`) over hand-rolled `@MethodSource` providers when iterating over countries or random IBANs.

## Build & Test

```bash
mvn clean verify                               # full build: compile, checkstyle, pmd, tests, jacoco
mvn -pl iban-commons-bankdata -am clean verify # single module + its dependencies
```

Default goal is `clean verify` (see root `pom.xml`). Checkstyle and PMD run as part of `verify` and fail the build on violations — there is no separate lint step to remember.

- **Java**: compiled targeting Java 8 (`releaseJavaVersion`) for source compatibility, built with Java 17 (`buildJavaVersion`)
- **Git branch convention**: feature branches for non-trivial work; commit messages stay concise, present-tense, capability-first (describe what a squashed commit adds/does, not the bugs it fixed along the way)

## Release Process

Releases are driven **locally** via `maven-release-plugin`, not by a tag-push CI workflow:

```bash
mvn release:prepare -DreleaseVersion=<X.Y.Z> -DdevelopmentVersion=<next>-SNAPSHOT -Dtag=iban-commons-<X.Y.Z>
git push --follow-tags
mvn release:perform
```

`pushChanges` is `false` in the plugin config, so the `git push --follow-tags` step is always explicit and manual. Release candidates use a `-RCn` version suffix (e.g. `1.8.12-RC1`) with the development version left as the current `-SNAPSHOT` (no bump) until the RC is validated; only the final release bumps to the next `-SNAPSHOT`. Deploys go through the `release` Maven profile (GPG-signs artifacts, publishes via the Central Publishing Portal) — Central deployments require a manual "publish" click at https://central.sonatype.com/publishing/deployments even after a successful `release:perform`.

## External Resources

- **Maven Central**: https://central.sonatype.com/artifact/de.speedbanking/iban-commons
- **Javadoc**:       https://javadoc.io/doc/de.speedbanking/iban-commons
- **GitHub**:        https://github.com/SpeedBankingDe/iban-commons
- **Issues / Discussions**: via the GitHub repo above

## Sibling Repositories

Other repos under the same organization depend on `iban-commons` via Maven (`de.speedbanking:iban-commons`): `iban-commons-generator` (code generator), `iban-commons-benchmarks` (JMH benchmarks), `iban-commons-site` (Website). When bumping the `iban-commons` version or changing public API, check whether these need a corresponding dependency bump.

## License

Apache License, Version 2.0
