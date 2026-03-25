# 🏦 IBAN Commons

> Ultra-fast, zero-dependency IBAN & BIC toolkit for Java 8+

[![Maven Central Version](https://img.shields.io/maven-central/v/de.speedbanking/iban-commons?label=Maven%20Central)](https://central.sonatype.com/artifact/de.speedbanking/iban-commons)
[![Maven Central Last Update](https://img.shields.io/maven-central/last-update/de.speedbanking/iban-commons?label=Last%20Update)](https://central.sonatype.com/artifact/de.speedbanking/iban-commons)
[![Build (JDK 17 Linux)](https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk17_ubuntu.yml?label=Build%20(JDK%2017%20Linux))](https://github.com/SpeedBankingDe/iban-commons/actions)
[![Build (JDK 17 Win)](https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk17_win.yml?label=Build%20(JDK%2017%20Win))](https://github.com/SpeedBankingDe/iban-commons/actions)
[![GitHub Repo stars](https://img.shields.io/github/stars/SpeedBankingDe/iban-commons)](https://github.com/SpeedBankingDe/iban-commons/stargazers)

**[🚀 Quick Start](#-quick-start) • [📖 Examples](#-code-examples) • [📊 Benchmarks](#-performance-benchmarks) • [📚 Javadoc](https://javadoc.io/doc/de.speedbanking/iban-commons/latest/) • [💬 Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)**

`iban-commons` is a Java IBAN validation library and BIC validator for Java 8+, providing fast and reliable parsing, validation, and formatting of International Bank Account Numbers (IBAN) and Business Identifier Codes (BIC/SWIFT codes).
Designed for high-performance enterprise applications, it covers 120 countries, is Android-compatible (API 21+), and has zero compile or runtime dependencies outside the Java Standard Library.

## Why IBAN Commons?

| Feature                    | iban-commons  | Apache Commons |  iban4j   | garvelink java-iban |
|----------------------------|:-------------:|:--------------:|:---------:|:-------------------:|
| **Throughput (ops/s)**     | **7,700,000** |   4,100,000    | 1,800,000 |      1,600,000      |
| **Memory (B/op)**          |    **106**    |      319       |   1,114   |         882         |
| **Dependencies**           |       0       |       5        |     0     |          0          |
| **Java Version**           |      8+       |       8+       |    11+    |         8+          |
| **Android (API 21+)**      |       ✅       |       ?        |     ?     |          ✅          |
| **Countries**              |      120      |  regex-based   |    111    |         111         |

-----

## 🚀 Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons</artifactId>
    <version>1.8.4</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'de.speedbanking:iban-commons:1.8.4'
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

* **Zero Dependencies**

  Keep your build clean and avoid dependency conflicts

* **High Performance**

  Optimized for throughput and minimal memory footprint. Validates 7,700,000 IBANs per second — **4.3x faster** than iban4j, **10x lower** memory allocation (106 B/op vs. 1,114 B/op).

* **Small Footprint**

  ~100 kB JAR

* **Simple, intuitive API**

  Intuitive factory methods (`Iban.of()`, `Bic.tryParse()`), clear component accessors

* **Immutability**

  Both `Iban` and `Bic` classes are immutable and thread-safe

* **Java 8 compatibility**

  Built with and compiled for Java 8 for maximum reach, tested on recent LTS versions

* **Comprehensive Coverage**

  Full support for IBAN and BIC validation per ISO 13616 and ISO 9362. Covers **120 countries** including all from the SWIFT IBAN Registry — more than any comparable Java IBAN validation library.

* **Rich Metadata**

  Access country names, flags, bank codes, SEPA status, and regulatory organization details

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

Iban iban     = RandomIban.of();         // random country
Iban deIban   = RandomIban.of("DE");     // specific country
Iban sepaIban = RandomIban.ofSepa();     // random SEPA country
```

**Reproducible generation with a seeded `Random`:**

Pass a seeded `java.util.Random` to get deterministic output — the same seed always produces the same IBAN for the same country, which is useful for unit tests and snapshot tests.

```java
import java.util.Random;

// Always produces the same IBAN for seed 42 + country "DE"
Iban iban = RandomIban.of("DE", new Random(42L));

// Reproducible across any supported country
Iban anyIban = RandomIban.of(new Random(42L));

// Reproducible SEPA IBAN
Iban sepaIban = RandomIban.ofSepa(new Random(42L));
```

**Using an `IbanRegistry` entry directly:**
```java
import de.speedbanking.iban.IbanRegistry;

Iban iban = RandomIban.of(IbanRegistry.DE);
Iban reproducible = RandomIban.of(IbanRegistry.FR, new Random(123L));
```

> **Note:** Generated IBANs are syntactically and structurally valid but do **not** correspond to real bank accounts. Do not use them for actual financial transactions.

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

We use the Java Microbenchmark Harness (**JMH**) to compare the throughput of `iban-commons` against popular Java IBAN libraries: [iban4j](http://www.iban4j.org/), [Apache Commons Validator](https://commons.apache.org/proper/commons-validator/), and [garvelink java-iban](https://github.com/barend/java-iban).

Measured on **Intel Core i7-1165G7 @ 2.80GHz**, OpenJDK 21.0.7, Linux, single core (`taskset -c 0`),
Generational ZGC, `-XX:-StackTraceInThrowable`.
30 measurement iterations (3 forks × 10 iterations × 2 s each).

### Valid IBANs (accept path)

| Library                 | Throughput (ops/s) | Memory (B/op) | vs. iban-commons |
|:------------------------|-------------------:|--------------:|:----------------:|
| 🌟 **iban-commons**     |      **7,700,000** |       **106** | baseline         |
| Apache Commons          |          4,100,000 |           319 | 1.9× slower      |
| iban4j                  |          1,800,000 |         1,114 | 4.3× slower      |
| garvelink java-iban     |          1,600,000 |           882 | 4.8× slower      |

### Invalid IBANs (rejection path)

| Library                 | Throughput (ops/s) | Memory (B/op) | vs. iban-commons |
|:------------------------|-------------------:|--------------:|:----------------:|
| 🌟 **iban-commons**     |     **11,000,000** |        **78** | baseline         |
| Apache Commons          |          9,200,000 |           165 | 1.2× slower      |
| garvelink java-iban     |          1,700,000 |           689 | 6.4× slower      |
| iban4j                  |          1,500,000 |           999 | 7.3× slower      |

### Key Observations

1. **Leading Throughput:** `iban-commons` is the fastest across both valid and invalid input — reaching **7,700,000 ops/s** on the accept path and **11,000,000 ops/s** on early rejection.
2. **Fast Rejection:** Invalid IBANs are rejected faster than valid ones because many fail length or country-code checks before the full Mod-97 computation is reached.
3. **Minimal GC Pressure:** Memory allocation is **3×–10× lower** than competing libraries, thanks to an ASCII-math Mod-97 approach that avoids intermediate `String` and `BigInteger` allocations.

> **Note on `-XX:-StackTraceInThrowable`:** All forks run with this JVM flag, which suppresses stack trace generation to isolate pure algorithmic cost. This makes the comparison fair for libraries using exceptions for control flow (notably `iban4j`). For production-realistic measurements, remove the flag and re-run.

### Benchmark Suite Repository

All performance tests are fully open and available in the [SpeedBankingDe/iban-commons-benchmarks](https://github.com/SpeedBankingDe/iban-commons-benchmarks) repository.

-----

## ❓ FAQ

<details>
<summary><b>Which countries are supported?</summary>

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
<summary><b>Where can I find the full API documentation?</b></summary>

The complete Javadoc is hosted on [javadoc.io](https://javadoc.io/doc/de.speedbanking/iban-commons/latest/).
It covers all public classes and methods including `Iban`, `Bic`, `IbanRegistry`, and the validation API.
</details>

<details>
<summary><b>Can I use this library on Android?</b></summary>

Yes. `iban-commons` is Android-compatible from API level 21 (Android 5.0) onwards.

The primary API (`Iban.tryParse()`, `Bic.tryParse()`) uses `java.util.Optional`, which requires API 24+.
For projects targeting API 21–23, use the Android-safe alternatives introduced in v1.8.3:

```java
// Android-safe (API 21+) — returns null instead of Optional
Iban iban = Iban.tryParseOrNull("DE91100000000123456789");
Bic  bic  = Bic.tryParseOrNull("MARKDEFF");
```

The library has zero compile or runtime dependencies and does not use `java.time` types in its public API.
</details>

<details>
<summary><b>Is the library thread-safe?</b></summary>

Yes. Both `Iban` and `Bic` are immutable value objects — all fields are `final` and set in the constructor.
They can be safely shared across threads without synchronization.
The validation and registry classes are stateless and equally safe for concurrent use.
</details>



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

**Please star this repo if you find it useful!** ⭐

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

-----

<!-- SEO: common search terms for this project -->
<!-- java iban validation | iban validator java | bic validation java | swift code validator java -->
<!-- iban4j alternative | apache commons iban | sepa validation java | java iban library -->
<!-- android iban validation | iban parser java 8 | iso 13616 java | mod97 java -->
