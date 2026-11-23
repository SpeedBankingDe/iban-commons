# Changelog

Release notes of earlier versions of IBAN Commons. The notes of the current release are in the
[README](README.md), every release is also listed on the
[GitHub releases page](https://github.com/SpeedBankingDe/iban-commons/releases).

## 1.8.13

### 42% Smaller Jar
The core jar shrinks from 252 kB in 1.8.12 to 145 kB, smaller than 1.8.4 despite everything added since. The 112 generated country validator classes are replaced by a single `CountryValidator` that derives its checks from the BBAN pattern in `IbanRegistry`. National check digit algorithms are plain methods shared by all countries using them. The jar no longer carries local variable tables, `package-info` classes or synthetic accessor classes.

### Faster Validation
IBAN validation is about 6% faster on valid and 9% faster on invalid IBANs than in 1.8.12, measured with JMH across all countries. Every country is now checked by the same `CountryValidator` class, so the JIT sees a single call target instead of one per country. Allocation stays at 0 bytes per validation.

### Less Memory for Bank Data
`iban-commons-bankdata` shares equal values such as bank names, BICs and cities among the records of a country. With all bundled countries loaded, the bank data takes 3.6 instead of 5.4 MB of heap.

### Reproducible Builds
Building the same commit yields byte-identical jars: archive entries carry a fixed timestamp, and the manifests no longer contain machine-specific entries.

### Breaking Changes
A few rarely used public types were removed or moved:
* `IndexRange`, `RegexSimplifier` and `CharArrayWrapper` in `de.speedbanking.util` are removed, the library no longer used them. For a `CharSequence` view on a `char[]`, use `java.nio.CharBuffer.wrap(char[])`.
* `InvalidBaseException`, the common base of `InvalidIbanException` and `InvalidBicException`, moved from `de.speedbanking.iban.util` to `de.speedbanking.util`.
* `IbanPatternConverter.parseSegments()` returns the new `Segments` type instead of `List<Segment>`. The static helpers `aggregateSegments()`, `buildRegex()`, `allMatch()`, `isAllNumeric()` and `calculateTotalLength(Iterable)` are replaced by instance methods of `Segments`.

## 1.8.12

### Bank Data Lookup Module
New optional [`iban-commons-bankdata`](README.md#bank-data-lookup-iban-commons-bankdata) module resolving BIC and bank name from an IBAN or a country-specific bank code (DE, AT, CH, CZ, BE, PL, NL, ES, FR), with a bundled offline fallback dataset and a lazy, non-blocking background refresh against each country's official bank directory. For Germany, each record also carries the check digit method the Bundesbank publishes for the bank. Depends on `iban-commons`.

### German IBANs with Account Check
New `GermanIban` in `iban-commons-bankdata` is a validated German IBAN whose `getAccountCheckResult()` checks the account number against the check digit method of the bank, using the bank data above and `iban-commons-de-checkdigit` as an optional dependency.

### Complete German Check Digit Methods
`iban-commons-de-checkdigit` now implements all 144 assigned Bundesbank check digit methods (`00` to `99`, `A0` to `E4`), up from 122. New are the ESER methods `52` and `53` with `B6` and `C0` built on them, `72`, `75`, `77`, `79`, `80`, `83`, `87`, `89`, `97`, `D4`, `D5`, `D7`, `D8` and `E0` to `E4`. Methods `13`, `35`, `50`, `57`, `66`, `69`, `71`, `73`, `76`, `84`, `86`, `90`, `B7`, `B9` and `D1` were corrected where they deviated from the specification, and the test suite now checks every test account number listed in the specification.

### Faster IbanBuilder
`IbanBuilder` checks component values character by character against their pattern instead of using a regex, generates random characters only for BBAN positions no component sets, and works on a reused per-thread buffer. Building a DE IBAN from bank code and account number is about 2.6 times as fast as in 1.8.11 and allocates 120 instead of 564 bytes, compared to 1,604 bytes for iban4j, which it now outperforms by a factor of 2.4.

### Smaller Iban Objects
The parts that `Iban` caches for its getters (`getBankCode()`, `getAccountNumber()`, `toFormattedString()` and others) now live in a holder created on first access. An `Iban` whose parts are never requested takes 24 instead of 48 bytes on the heap.

### Build Requirement: JDK 21
The project now requires JDK 21 to build. The compiled artifacts remain binary-compatible with Java 8, and CI runs the test suite on Java 8 as well.

## 1.8.11

### IBAN Registry Release 103
`IbanRegistry` is now cross-checked against SWIFT IBAN Registry Release 103 (Sep 2026), up from Release 100. The only structural change: Brazil's bank identifier and account-type/control digits are now alphanumeric instead of numeric/alpha-only (`8!c...1!c1!c` instead of `8!n...1!a1!c`).

### Check Digit Range Enforcement
A genuinely generated IBAN check digit is always in `[02, 98]` - `00`, `01`, and `99` can't occur. `Mod97.isValid()`, and everything built on it (`IbanValidator`, `IbanBuilder`), now rejects those three values outright instead of trusting the raw mod-97 remainder alone, which some BBANs could coincidentally still satisfy.

### Library Metadata on Direct JAR Execution
Running the JAR directly (`java -jar iban-commons.jar`) now prints its title, version, vendor, and build info from the manifest, with a note that it's a library and not meant for CLI use, instead of exiting silently.

### Test Suite Hardening
Line and mutation coverage (via pitest) were pushed further across `iban-commons-de-checkdigit`, `iban-commons-junit`, and `iban-commons-validation`, closing gaps mutation testing had exposed.

## 1.8.10

### German Account Check Digit Module
New standalone [`iban-commons-de-checkdigit`](README.md#german-account-check-digit-iban-commons-de-checkdigit) module implementing 122 of the Deutsche Bundesbank's "Prüfzifferberechnungsmethoden in der deutschen Kreditwirtschaft" (methods `00`-`99`, `A0`-`D9`) for verifying a German domestic account number against a Bankleitzahl. Zero dependency on `iban-commons` itself.

## 1.8.9

### IbanBuilder
New `IbanBuilder` API (obtained via `IbanRegistry#builder()`) generates structurally valid, correctly checksummed IBANs from explicit or randomly filled components - useful for tests and fixtures.

### 7 New Countries
Added CG (Congo-Brazzaville), CI (Côte d'Ivoire), GW (Guinea-Bissau), MG (Madagascar), ML (Mali), NE (Niger), and TD (Chad) to `IbanRegistry`, bringing total country coverage to 127.

### Regex & Component Utilities
Added `PatternCache` (thread-safe compiled-`Pattern` cache) and `RegexSimplifier` (consolidates consecutive same-class regex blocks), plus new `IndexRange` offset methods and gap-range support in component string formatting.

### Internal Consolidation
`CountryUtil` was merged into `Country` (including `createFlagEmoji`), and several country data entries were corrected (Bolivia name, Tonga continent, ZWL->ZWG rename, KGS typo).

## 1.8.8

### Optimized BIC Validation Pipeline
Extended the zero-allocation principles fully to the BIC parsing and validation pipeline.

### Updated Performance Benchmarks
Integrated the latest JMH benchmark results (June 2026) for both the IBAN and BIC validation paths, confirming zero-allocation (`0 B/op`) performance and leading execution speeds.

### Internal Quality Refinements
Fine-tuned internal validation rules and alignment with Error Prone guidelines.

## 1.8.7

### Zero Allocation on the Hot Path
`IbanValidationResult` now caches one singleton per `IbanValidationError` constant at class-load time - failed validations no longer allocate on the rejection path.

### Cleaner validate() API
New allocation-free `Iban.validate()` and `Bic.validate()` methods throw on invalid input without constructing an instance.

### Internal Refactoring & Correctness
`Objects.hash()` replaced with the classic Bloch prime-accumulation pattern throughout - no vararg array, no autoboxing of primitives.

## 1.8.6

### Invalid IBAN Generator
New utilities for generating structurally invalid IBAN strings with controlled sabotage strategies.

### Error-Prone Integration
The build now integrates [Error Prone](https://errorprone.info/) with [`error‑prone‑support`](https://github.com/PicnicSupermarket/error-prone-support), catching bug patterns at compile time that unit tests alone cannot surface.

### Dedicated JUnit Extensions Module
JUnit 5 extensions (enhanced IBAN and BIC assertions, parameterized test support) have been extracted into a standalone `iban-commons-junit` sub-module.

## 1.8.5

### Zero-Allocation Validation Pipeline
The internal validation pipeline was migrated from `CharSequence` to `char[]` throughout - across all 80+ country validators, `NationalCheckDigitCalculator`, and `AbstractNcdCountryValidator`. This eliminates virtual dispatch on `charAt()` and reduces heap allocation to exactly 0 B/op.

> **Migration note (SPI implementors):** Custom `NationalCheckDigitCalculator` implementations must update their method signatures from `CharSequence` to `char[]`.

### Expanded Public API
Previously internal classes are now part of the stable public API: `Bic`, `IbanRegistry`, `RandomBic`, and `Formatter`.

### Immutable `IbanConfig` via Freeze Pattern
`IbanConfig` redesigned from a mutable enum to an immutable class. Configuration is set once and then locked - concurrent reads require no synchronization.

> **Migration note:** Update existing code to the new `IbanConfig.freeze()` approach.

### `RandomIban` Fluent Builder
Overloaded `RandomIban.of(...)` factory methods replaced by a fluent builder:

```java
Iban iban = RandomIban.builder().country("DE").random(new Random(42L)).build();
```

> **Migration note:** Migrate calls to the old `of(String)` / `of(IbanRegistry)` overloads to the builder.

### Build Requirement: JDK 17+
The project now requires JDK 17+ to build. The compiled artifact remains fully binary-compatible with Java 8.
