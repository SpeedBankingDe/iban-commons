# 🏦 IBAN Commons

> Ultra-fast, zero-dependency IBAN & BIC toolkit for Java 8+

[![Maven Central Version](https://img.shields.io/maven-central/v/de.speedbanking/iban-commons?label=Maven%20Central)](https://central.sonatype.com/artifact/de.speedbanking/iban-commons)
[![Maven Central Last Update](https://img.shields.io/maven-central/last-update/de.speedbanking/iban-commons?label=Last%20Update)](https://central.sonatype.com/artifact/de.speedbanking/iban-commons)
[![Build (JDK 11 Linux)](https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk11_ubuntu.yml?label=Build%20(JDK%2011%20Linux))](https://github.com/SpeedBankingDe/iban-commons/actions)
[![Build (JDK 11 Win)](https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk11_win.yml?label=Build%20(JDK%2011%20Win))](https://github.com/SpeedBankingDe/iban-commons/actions)
[![GitHub Repo stars](https://img.shields.io/github/stars/SpeedBankingDe/iban-commons)](https://github.com/SpeedBankingDe/iban-commons/stargazers)

**[🚀 Quick Start](#-quick-start) • [📖 Examples](#-code-examples) • [📊 Benchmarks](#-performance-benchmarks) • [💬 Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)**

The `iban-commons` library provides simple, fast, and reliable validation and decomposition of International Bank Account Numbers (IBAN) and Business Identifier Codes (BIC).
Designed for high-performance enterprise applications, it intentionally has zero compile or runtime dependencies outside the Java Standard Library.

## Why IBAN Commons?

| Feature            | iban-commons    | Apache Commons | iban4j      | garvelink java-iban |
|--------------------|-----------------|----------------|-------------|---------------------|
| **Performance**    | **5,257 ops/s** | 4,786 ops/s    | 814 ops/s   | 863 ops/s           |
| **Dependencies**   | 0               | 5              | 0           | 0                   |
| **Memory**         | **137 MB/op**   | 327 MB/op      | 1,627 MB/op | 1,126 MB/op         |
| **Java Version**   | 8+              | 8+             | 11+         | 8+                  |

-----

## 🚀 Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons</artifactId>
    <version>1.8.1</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'de.speedbanking:iban-commons:1.8.1'
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

  Optimized for execution speed and minimum memory footprint. **6.4x faster** than iban4j, **11.9x less memory** allocation

* **Small Footprint**

  ~100 kB JAR

* **Simple, intuitive API**

  Intuitive factory methods (`Iban.of()`, `Bic.tryParse()`), clear component accessors

* **Immutability**

  Both `Iban` and `Bic` classes are immutable and thread-safe

* **Java 8 compatibility**

  Built with and compiled for Java 8 for maximum reach, tested on recent LTS versions

* **Comprehensive Coverage**

  Full support for IBAN and BIC validation according to ISO 13616 and ISO 9362 standards. Supports **95+ countries** from the SWIFT IBAN Registry

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

-----

### BIC

The `Bic` class implements `Serializable`, `CharSequence`, and `Comparable<Bic>`.
A BIC comparison is always based on the 11-character representation (`toBic11()`), meaning BIC-8 and its BIC-11 equivalent are considered equal.

#### 1\. Quick Validation (throws exception on failure)

```java
import de.speedbanking.bic.Bic;

// BIC-11
Bic bic11 = Bic.of("PALSPS22XXX");                    // Bank of Palestine P.S.C.

System.out.println("Bank Code    : " + bic11.getBankCode());     // PALS
System.out.println("Country Code : " + bic11.getCountryCode());  // PS
System.out.println("Location Code: " + bic11.getLocationCode()); // 22
System.out.println("Branch Code  : " + bic11.getBranchCode());   // XXX
System.out.println("is BIC-11    : " + bic11.isBic11());         // true
System.out.println("is BIC-8     : " + bic11.isBic8());          // false
System.out.println("to BIC-8     : " + bic11.toBic8());          // PALSPS22

// BIC-8
Bic bic8 = Bic.of("MARKDEFF");                        // Deutsche Bundesbank, Zentrale

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

### Validation Throughput Comparison

The results below demonstrate the significant **throughput** advantage (operations per second) of `iban-commons`.

| Benchmark Operation                         | iban-commons (ops/s) | Apache Commons (ops/s) | garvelink java-iban (ops/s) | iban4j (ops/s) | Speedup vs. iban4j  |
|---------------------------------------------|----------------------|------------------------|-----------------------------|----------------|---------------------|
| **Pure Validation** (`.isValid()`)          | **5257**             | 4786                   | n/a                         | 814            | **~6.4x faster**    |
| **Object Creation** (`.tryParse()`/`.of()`) | **2993**             | n/a                    | 863                         | 754            | **~3.9x faster**    |

### Memory Footprint Comparison

These results, derived from the JMH **GC Profiler**, quantify the **memory allocation overhead** per operation (B/op). Lower values indicate less work for the Garbage Collector (GC).

| Benchmark Operation                         | iban-commons (B/op) | Apache Commons (B/op) | garvelink java-iban (B/op) | iban4j (B/op) | Allocation Efficiency         |
|---------------------------------------------|---------------------|-----------------------|----------------------------|---------------|-------------------------------|
| **Pure Validation** (`.isValid()`)          | **136.9M**          | 326.8M                | n/a                        | 1627.3M       | **~11.9x smaller vs. iban4j** |
| **Object Creation** (`.tryParse()`/`.of()`) | **253.9M**          | n/a                   | 1126.1M                    | 1633.7M       | **~6.4x smaller vs. iban4j**  |

*Note on Memory Units: The B/op values represent normalized allocations per benchmark operation. The **relative difference** confirms the high allocation efficiency of iban-commons.*

### Key Observations

1. **Leading Throughput:**       `iban-commons` outperforms all tested libraries, reaching **5257 operations per second** for validation.
2. **Superior Object Creation:** Compared to `garvelink java-iban`, `iban-commons` is about **3.4x faster** and uses **4.4x less memory** when creating immutable objects.
3. **Minimal GC Pressure:**      With only **136.9M B/op** for validation, the library requires significantly less allocation than Apache Commons (~2.4x) and iban4j (~11.9x).

### Execution Environment Details

The benchmarks were executed under the following configuration:

* Intel Core i7-1165G7 @ 2.80GHz (4 Cores, 8 Threads), 32 GiB RAM
* Ubuntu 24.04.3 LTS (Noble Numbat)
* OpenJDK 21.0.7 LTS (`Temurin-21.0.7+6`), 64-Bit Server VM
* JMH Version 1.37 (using Compiler Blackholes)

### Benchmark Suite Repository

All performance tests are fully open and available in the [SpeedBankingDe/iban-commons-benchmarks](https://github.com/SpeedBankingDe/iban-commons-benchmarks) repository.

-----

## ❓ FAQ

<details>
<summary><b>Which countries are supported?</summary>

All 95+ countries from the SWIFT IBAN Registry Release 100 (October 2025), including:
- All SEPA countries
- Major economies: UK, Switzerland, Norway, etc.
- Full list available in the [source code](src/main/java/de/speedbanking/iban/IbanRegistry.java)
</details>

<details>
<summary>Is this library production-ready?</summary>

Yes, and already in use in critical production systems. Features:
- Comprehensive test suite (>95% coverage)
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
The library automatically handles whitespace and case normalization.
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
```bash
git clone https://github.com/SpeedBankingDe/iban-commons.git
cd iban-commons
mvn clean verify
```

-----

## 💬 Support & Community

- 💡 Questions? Ask in [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)
- 🐛 Found a bug? Open an [Issue](https://github.com/SpeedBankingDe/iban-commons/issues)
- 🌐 Website: [www.speedbanking.de](https://www.speedbanking.de/)

**Star this repo if you find it useful!** ⭐

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

