# 🏦 IBAN Commons

> Ultra-fast, allocation-free, zero-dependency IBAN & BIC toolkit for Java 8+

<div align="center">
  <a href="https://central.sonatype.com/artifact/de.speedbanking/iban-commons"><img src="https://img.shields.io/maven-central/v/de.speedbanking/iban-commons?label=Maven%20Central&style=flat-square" alt="Maven Central Version"></a>
  <img src="https://img.shields.io/maven-central/last-update/de.speedbanking/iban-commons?label=Updated&style=flat-square&color=blue" alt="Maven Central Last Update">
  <a href="https://github.com/SpeedBankingDe/iban-commons/stargazers"><img src="https://img.shields.io/github/stars/SpeedBankingDe/iban-commons?logo=github&label=&logoColor=white&labelColor=555555&color=007ec6&style=flat-square" alt="GitHub Stars"></a>
  <br>
  <img src="https://img.shields.io/badge/coverage-100%25-brightgreen?style=flat-square" alt="Test Coverage">
  <a href="https://github.com/SpeedBankingDe/iban-commons/actions/workflows/ci_jdk17_ubuntu.yml"><img src="https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk17_ubuntu.yml?label=Build%20(JDK%2011%20Linux)&style=flat-square" alt="GitHub Actions Workflow Status"></a>
  <a href="https://github.com/SpeedBankingDe/iban-commons/actions/workflows/ci_jdk17_win.yml"><img src="https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk17_win.yml?label=Build%20(JDK%2011%20Win)&style=flat-square" alt="GitHub Actions Workflow Status"></a>
  <a href="https://javadoc.io/doc/de.speedbanking/iban-commons"><img src="https://javadoc.io/badge2/de.speedbanking/iban-commons/javadoc.svg?style=flat-square" alt="Javadoc"></a>
  <a href="https://apidia.net/mvn/de.speedbanking/iban-commons"><img src="https://apidia.net/mvn/de.speedbanking/iban-commons/badge_flat_square.svg" alt="APIdia"></a>
</div>

**[🚀 Quick Start](#-quick-start) • [📖 Examples](#-code-examples) • [📊 Benchmarks](#-performance-benchmarks) • [📚 Javadoc](https://javadoc.io/doc/de.speedbanking/iban-commons/latest/) • [💬 Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)**

`iban-commons` is a Java IBAN validation library and BIC validator for Java 8+, providing fast and reliable parsing, validation, and formatting of International Bank Account Numbers (IBAN) and Business Identifier Codes (BIC/SWIFT codes).
Designed for high-performance enterprise applications, it covers 120 countries, is Android-compatible (API 21+), and has zero compile or runtime dependencies outside the Java Standard Library.

## Why IBAN Commons?

| Feature                    | iban-commons   | jbanking  | Apache Commons |  iban4j   | garvelink |
|----------------------------|:--------------:|:---------:|:--------------:|:---------:|:---------:|
| **Throughput (ops/s)**     | **10,740,280** | 7,880,633 |   6,531,841    | 1,801,759 | 2,250,043 |
| **Memory (B/op)**          |      **0**     |    154    |      247       |   1,156   |    647    |
| **Dependencies**           |        0       |     0     |       5        |     0     |     0     |
| **Java Version**           |       8+       |    8+     |       8+       |    11+    |    8+     |
| **Android (API 21+)**      |        ✅       |     ?     |       ?        |     ?     |     ✅     |
| **Countries**              |       120      |    82     |      n/a       |    111    |    111    |

> Throughput: rejection path (invalid IBANs) — JMH · OpenJDK 21.0.7 · Linux · single core · ParallelGC · `-XX:-StackTraceInThrowable` · 2026-04-19

-----

## 🚀 Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons</artifactId>
    <version>1.8.8</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'de.speedbanking:iban-commons:1.8.8'
```

### 2. Validate & Parse

```java
import de.speedbanking.iban.Iban;
// throws InvalidIbanException if invalid
Iban iban = Iban.of("DE91100000000123456789");

// returns Optional<Iban>, does not throw
Optional<Iban> maybeIban = Iban.tryParse("GB82WEST12345698765432");

// validate without parsing
boolean valid = Iban.isValid("FR1420041010050500013M02606");
```

### 3. Extract Components

```java
Iban iban = Iban.of("IT60X0542811101000000123456");

iban.getCountryCode();    // "IT"
iban.getCountryName();    // "Italy"
iban.getCountryFlag();    // "🇮🇹"
iban.getBankCode();       // "05428"
iban.getBranchCode();     // "11101"
iban.getAccountNumber();  // "000000123456"
iban.toFormattedString(); // "IT60 X054 2811 1010 0000 0123 456"
```

-----

## ✨ Key Features

* **Zero Dependencies & Small Footprint**

  Keep your build clean and avoid dependency conflicts

* **High Performance**

  Zero-allocation validation pipeline — `char[]`-based processing eliminates heap pressure throughout.
  Validates **5,591,566 IBANs/s** on the accept path and **10,740,280 IBANs/s** on early rejection;
  allocation is exactly **0 B/op**, against 154–1,156 B/op for competing libraries.

* **Simple, intuitive API**

  Intuitive factory methods (`Iban.of()`, `Bic.tryParse()`), Android-compatible

* **Immutability**

  `Iban`, `Bic`, `IbanRegistry` classes are immutable and thread-safe

* **Java 8 compatibility**

  Targeting Java 8 for maximum reach, built and verified using JDK 17+ ensuring full binary compatibility

* **Comprehensive Coverage**

  Full support for IBAN and BIC validation per ISO 13616 and ISO 9362.
  Covers **120 countries** including all from the SWIFT IBAN Registry — more than any comparable Java IBAN validation library.

* **Rich Metadata**

  Access country names, flags, bank codes, SEPA status, currency information, and regulatory organization details

-----

## 💡 Code Examples

The API is designed for simplicity, focusing on two main ways to create a valid object: a throwing factory method for quick use and a safe parsing method using `Optional`.

### IBAN

The `Iban` class implements `Serializable`, `CharSequence`, and `Comparable<Iban>`.

#### 1\. Quick Validation (throws exception on failure)

Use `Iban.of()` or `Iban.parse()` when you prefer an exception for validation failures.

```java
import de.speedbanking.iban.Iban;
import de.speedbanking.iban.InvalidIbanException;

String ibanInput = "DE91100000000123456789";

try {
    Iban iban = Iban.of(ibanInput);

    // getters
    System.out.println("Country Code: " + iban.getCountryCode());    // DE
    System.out.println("Check Digits: " + iban.getCheckDigits());    // 91
    System.out.println("BBAN        : " + iban.getBban());           // 100000000123456789
    System.out.println("Bank Code   : " + iban.getBankCode());       // 10000000
    System.out.println("Account No  : " + iban.getAccountNumber());  // 0123456789

    // output
    System.out.println("Normalized  : " + iban.toString());          // DE91100000000123456789
    System.out.println("Formatted   : " + iban.toFormattedString()); // DE91 1000 0000 0123 4567 89
} catch (InvalidIbanException ex) {
    System.out.println("IBAN validation failed: " + ex.getMessage());
}
```

#### 2\. Safe Parsing (returns Optional)

Use `Iban.tryParse()` when dealing with external or uncertain input and to avoid exceptions for control flow.

```java
import de.speedbanking.iban.Iban;
import java.util.Optional;

Optional<Iban> optionalIban = Iban.tryParse("PS92PALS000000000400123456702");

optionalIban.ifPresent(iban -> {
    println("Country Code: " + iban.getCountryCode());  // PS
    println("Country Name: " + iban.getCountryName());  // Palestine
    println("Country Flag: " + iban.getCountryFlag());  // 🇵🇸
    println("Organisation: " + iban.getOrganisation()); // Palestine Monetary Authority
});
```

#### 3\. Advanced Usage

**Batch Processing:**
```java
List<String> ibanStrings = loadFromDatabase();

List<Iban> validIbans = ibanStrings.stream()
    .map(Iban::tryParse)
    .flatMap(Optional::stream)
    .collect(Collectors.toList());
```

**SEPA Filtering:**
```java
Iban iban = Iban.of("CH9300762011623852957");
if (iban.isSepa()) {
    processSepaPayment(iban);
} else {
    processCrossBorderPayment(iban);
}
```

#### 4\. Random IBAN Generation

Use `RandomIban` to generate syntactically correct, structurally valid IBANs with proper ISO 7064 Mod 97-10 check digits — ideal for testing, seeding demo data, or populating sandboxes.

**Any supported country (non-deterministic):**
```java
import de.speedbanking.iban.RandomIban;

Iban rdIban   = RandomIban.of();     // random country
Iban deIban   = RandomIban.of("DE"); // specific country
Iban sepaIban = RandomIban.ofSepa(); // random SEPA country
```

**Reproducible generation with a seeded `Random`:**

Pass a seeded `java.util.Random` to get deterministic output — the same seed always produces the same IBAN for the same country, which is useful for unit tests and snapshot tests.

```java
import java.util.Random;

// Always produces the same IBAN for seed 42 + country "IT"
Iban itIban = RandomIban.builder()
    .country("IT")
    .random(new Random(42L))
    .build();

// Reproducible SEPA IBAN
Iban sepaIban = RandomIban.builder()
    .sepaOnly() // random SEPA country
    .random(new Random(42L))
    .build();
```

> **Note:** Generated IBANs are syntactically and structurally valid but do **not** correspond to real bank accounts. 
>           Do not use them for actual financial transactions.

-----

### BIC

The `Bic` class implements `Serializable`, `CharSequence`, and `Comparable<Bic>`.
A BIC comparison is always based on the 11-character representation (`toBic11()`), meaning BIC-8 and its BIC-11 equivalent are considered equal.

#### 1\. Quick Validation (throws exception on failure)

```java
import de.speedbanking.bic.Bic;

// BIC-11
Bic bic11 = Bic.of("PALSPS22XXX"); // Bank of Palestine P.S.C.

System.out.println("Bank Code    : " + bic11.getBankCode());     // PALS
System.out.println("Country Code : " + bic11.getCountryCode());  // PS
System.out.println("Location Code: " + bic11.getLocationCode()); // 22
System.out.println("Branch Code  : " + bic11.getBranchCode());   // XXX
System.out.println("is BIC-11    : " + bic11.isBic11());         // true
System.out.println("is BIC-8     : " + bic11.isBic8());          // false
System.out.println("to BIC-8     : " + bic11.toBic8());          // PALSPS22

// BIC-8
Bic bic8 = Bic.of("MARKDEFF"); // Deutsche Bundesbank, Zentrale

System.out.println("is BIC-8     : " + bic8.isBic8());           // true
System.out.println("is BIC-11    : " + bic8.isBic11());          // false
System.out.println("to BIC-11    : " + bic8.toBic11());          // MARKDEFFXXX
```

#### 2\. Safe Parsing (returns Optional)

```java
import de.speedbanking.bic.Bic;

Bic.tryParse("INVALIDBIC").ifPresentOrElse(
    bic -> System.out.println("Valid BIC: " + bic),
    ()  -> System.err.println("Invalid BIC")
);
```

-----

## 🔄 Migrating from Other Libraries

### From iban4j

```java
// Before (iban4j)
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.iban4j.IbanFormatException;

try {
    Iban iban = new Iban.Builder()
    .countryCode(CountryCode.DE)
    .bankCode("37040044")
    .accountNumber("0532013000")
    .build();
} catch (IbanFormatException ex) { }

// After (iban-commons)
import de.speedbanking.iban.Iban;

Iban iban = Iban.of("DE89370400440532013000");
```

### From Apache Commons Validator

```java
// Before (Apache Commons Validator)
import org.apache.commons.validator.routines.IBANValidator;

IBANValidator validator = IBANValidator.getInstance();
boolean valid = validator.isValid("GB82WEST12345698765432");

// After (iban-commons)
import de.speedbanking.iban.Iban;

boolean valid = Iban.isValid("GB82WEST12345698765432");
// or parse for component access
Iban iban = Iban.of("GB82WEST12345698765432");
```

-----

## ⏱️ Performance Benchmarks

`iban-commons` is designed for **high throughput** and **minimal overhead**.

We use the Java Microbenchmark Harness (**JMH**) to compare the throughput of `iban-commons` against popular Java IBAN libraries: [iban4j](http://www.iban4j.org/), [Apache Commons Validator](https://commons.apache.org/proper/commons-validator/), [garvelink java-iban](https://github.com/barend/java-iban), and [jbanking](https://github.com/marcwrobel/jbanking).

Measured on **Intel Core i7-1165G7 @ 2.80GHz**, OpenJDK 21.0.7, Linux, single core (`taskset -c 0`),
ParallelGC (`-XX:+UseParallelGC`), `-XX:-StackTraceInThrowable`, `-Xms2G -Xmx2G`.
5 warmup iterations × 2 s, then 2 forks × 4–5 measurement iterations × 2 s — run 2026-04-19.

### Valid IBANs (accept path)

| Library                 | Throughput (ops/s) | Memory (B/op) | vs. iban-commons |
|:------------------------|-------------------:|--------------:|:----------------:|
| 🌟 **iban-commons**     |      **5,591,566** |       **0**   | baseline         |
| jbanking                |          4,278,475 |           298 | 1.3× slower      |
| Apache Commons          |          3,208,846 |           442 | 1.7× slower      |
| iban4j                  |          2,870,107 |         1,133 | 1.9× slower      |
| garvelink java-iban     |          2,269,465 |           867 | 2.5× slower      |

### Invalid IBANs (rejection path)

| Library                 | Throughput (ops/s) | Memory (B/op) | vs. iban-commons |
|:------------------------|-------------------:|--------------:|:----------------:|
| 🌟 **iban-commons**     |     **10,740,280** |       **0**   | baseline         |
| jbanking                |          7,880,633 |           154 | 1.4× slower      |
| Apache Commons          |          6,531,841 |           247 | 1.6× slower      |
| garvelink java-iban     |          2,250,043 |           647 | 4.8× slower      |
| iban4j                  |          1,801,759 |         1,156 | 6.0× slower      |

Each invalid IBAN is derived from a valid one by applying one of six sabotage strategies with equal probability: incrementing a check digit (triggering a Mod-97 failure), replacing the country code with the non-registered code `XY`, substituting a valid but mismatched ISO 3166 country code, injecting a letter into the numeric BBAN section, swapping two adjacent characters (transposition), or truncating the string below the minimum structural length.

### Key Observations

1. **Leading Throughput:** `iban-commons` is the fastest across both valid and invalid input — reaching **5,591,566 ops/s** on the accept path and **10,740,280 ops/s** on early rejection.
2. **Fast Rejection:** Invalid IBANs are rejected faster than valid ones because many fail length or country-code checks before the full Mod-97 computation is reached.
3. **Zero Allocation:** Memory allocation is **0 B/op** on both paths. Competing libraries allocate 154–1,156 B/op.

> **Note on `-XX:-StackTraceInThrowable`:** All forks run with this JVM flag, which suppresses stack trace generation to isolate pure algorithmic cost. This makes the comparison fair for libraries using exceptions for control flow (notably `iban4j`). For production-realistic measurements, remove the flag and re-run.

### Benchmark Suite Repository

All performance tests are fully open and available in the [SpeedBankingDe/iban-commons-benchmarks](https://github.com/SpeedBankingDe/iban-commons-benchmarks) repository.

-----

## 🆕 What's New in 1.8.8

### Optimized BIC Validation Pipeline
Extended the zero-allocation principles fully to the BIC parsing and validation pipeline.

### Updated Performance Benchmarks
Integrated the latest JMH benchmark results (June 2026) for both the IBAN and BIC validation paths, confirming zero-allocation (`0 B/op`) performance and leading execution speeds.

### Internal Quality Refinements
Fine-tuned internal validation rules and alignment with Error Prone guidelines.

-----

## 🆕 What's New in 1.8.7

### Zero Allocation on the Hot Path
`IbanValidationResult` now caches one singleton per `IbanValidationError` constant at class-load time — failed validations no longer allocate on the rejection path.

### Cleaner validate() API
New allocation-free `Iban.validate()` and `Bic.validate()` methods throw on invalid input without constructing an instance.

### Internal Refactoring & Correctness
`Objects.hash()` replaced with the classic Bloch prime-accumulation pattern throughout — no vararg array, no autoboxing of primitives.

-----

## 🆕 What's New in 1.8.6

### Invalid IBAN Generator
New utilities for generating structurally invalid IBAN strings with controlled sabotage strategies.

### Error-Prone Integration
The build now integrates [Error Prone](https://errorprone.info/) with [`error‑prone‑support`](https://github.com/PicnicSupermarket/error-prone-support), catching bug patterns at compile time that unit tests alone cannot surface.

### Dedicated JUnit Extensions Module
JUnit 5 extensions (enhanced IBAN and BIC assertions, parameterized test support) have been extracted into a standalone `iban-commons-junit` sub-module.

-----

## 🆕 What's New in 1.8.5

### Zero-Allocation Validation Pipeline
The internal validation pipeline was migrated from `CharSequence` to `char[]` throughout — across all 80+ country validators, `NationalCheckDigitCalculator`, and `AbstractNcdCountryValidator`. This eliminates virtual dispatch on `charAt()` and reduces heap allocation to exactly 0 B/op.

> **Migration note (SPI implementors):** Custom `NationalCheckDigitCalculator` implementations must update their method signatures from `CharSequence` to `char[]`.

### Expanded Public API
Previously internal classes are now part of the stable public API: `Bic`, `IbanRegistry`, `RandomBic`, and `Formatter`.

### Immutable `IbanConfig` via Freeze Pattern
`IbanConfig` redesigned from a mutable enum to an immutable class. Configuration is set once and then locked — concurrent reads require no synchronization.

> **Migration note:** Update existing code to the new `IbanConfig.freeze()` approach.

### `RandomIban` Fluent Builder
Overloaded `RandomIban.of(…)` factory methods replaced by a fluent builder:

```java
Iban iban = RandomIban.builder().country("DE").random(new Random(42L)).build();
```

> **Migration note:** Migrate calls to the old `of(String)` / `of(IbanRegistry)` overloads to the builder.

### Build Requirement: JDK 17+
The project now requires JDK 17+ to build. The compiled artifact remains fully binary-compatible with Java 8.

-----

## ❓ FAQ

<details>
<summary>Which countries are supported?</summary>

120 countries including all from the SWIFT IBAN Registry Release 100 (October 2025):
- All SEPA countries including major economies: Germany, UK, Switzerland, Norway, etc.
- All known non-SEPA countries that support IBAN
- Full list available in the [source code](src/main/java/de/speedbanking/iban/IbanRegistry.java)
</details>

<details>
<summary>Is this library production-ready?</summary>

Yes, and already in use in production systems. Features:
- Comprehensive test suite (>99% coverage)
- Zero runtime dependencies
- Immutable, thread-safe design
- Regular updates with SWIFT registry
</details>

<details>
<summary>How do I validate IBANs from user input?</summary>

```java
String userInput = "  de91 1000 0000 0123 4567 89  ";

// Option 1: Safe parsing (recommended for user input)
Optional<Iban> maybeIban = Iban.tryParse(userInput);
maybeIban.ifPresent(iban -> processPayment(iban));

// Option 2: Quick validation
if (Iban.isValid(userInput)) {
    Iban iban = Iban.of(userInput);
}
```
</details>

<details>
<summary>Can I use this with Spring Boot?</summary>

Yes! Works seamlessly with Spring Boot. For Jakarta Bean Validation integration.

Example custom validator:
```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IbanValidator.class)
public @interface ValidIban {
    String message() default "Invalid IBAN";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```
</details>

<details>
<summary>Where can I find the full API documentation?</summary>

The complete Javadoc is hosted on [javadoc.io](https://javadoc.io/doc/de.speedbanking/iban-commons/latest/).
It covers all public classes and methods including `Iban`, `Bic`, `IbanRegistry`, and the validation API.
</details>

<details>
<summary>Can I use this library on Android?</summary>

Yes. `iban-commons` is Android-compatible from API level 21 (Android 5.0) onwards.
</details>

<details>
<summary>Is the library thread-safe?</summary>

Yes. Both `Iban` and `Bic` are immutable value objects — all fields are `final` and set in the constructor.
They can be safely shared across threads without synchronization.
The validation and registry classes are stateless and equally safe for concurrent use.
</details>

<details>
<summary>What's the difference between IBAN validation and checksum validation?</summary>

`iban-commons` performs **complete validation**:
1. Format validation: Country code, length, character set
2. Structural validation: Country-specific BBAN pattern
3. Checksum validation: MOD-97 algorithm (ISO 13616)
4. Country-specific rules: When available via `CountryValidator`

Simple checksum validation alone is insufficient for real-world use.
</details>

-----

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

**Quick Ways to Contribute:**
- Report bugs via [Issues](https://github.com/SpeedBankingDe/iban-commons/issues)
- Suggest features in [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)
- Improve documentation
- Submit pull requests

**Development Setup:**

* **JDK 17** or higher required to build the project
* Maven 3.9+ required

```bash
git clone https://github.com/SpeedBankingDe/iban-commons.git
cd iban-commons
mvn clean verify
```

-----

## 💬 Support & Community

- 💡 Questions? Ask in [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)
- 🐛 Found a bug? Open an [Issue](https://github.com/SpeedBankingDe/iban-commons/issues)
- 📚 API Reference: [Javadoc on javadoc.io](https://javadoc.io/doc/de.speedbanking/iban-commons/latest/)
- 🌐 Website: [www.speedbanking.de](https://www.speedbanking.de/)

** Please star this repo if you find it useful! **

-----

## 🔒 Security

To report a vulnerability, please use GitHub's private
[Security Advisory](https://github.com/SpeedBankingDe/iban-commons/security/advisories/new)
mechanism rather than opening a public issue.

For details on the project's security policy and the serialization hardening built
into `Iban` and `Bic`, see [SECURITY.md](SECURITY.md).

-----

## ⚖️ License

This project is licensed under the **Apache License, Version 2.0**. You can find the full text of the license [here](https://www.apache.org/licenses/LICENSE-2.0).

<p style="height: 20px;">&nbsp;</p>

<div align="center">
<table style="border-collapse: collapse;">
  <tr>
    <td style="padding: 40px; border: 2px solid #3a82c2;">
      <strong>Enjoying 
        <a href="https://github.com/SpeedBankingDe/iban-commons" style="color: #3a82c2; text-decoration: none;">iban-commons</a>?
      </strong><br>
      <strong>Please leave a 🌟 to support the project!</strong><br>
      <small>Your stars help to keep open source development visible and going.</small>
    </td>
  </tr>
</table>
</div>

<!-- SEO: common search terms for this project -->
<!-- java iban validation | iban validator java | bic validation java | swift code validator java -->
<!-- iban4j alternative | apache commons iban | sepa validation java | java iban library -->
<!-- android iban validation | iban parser java 8 | iso 13616 java | mod97 java -->
