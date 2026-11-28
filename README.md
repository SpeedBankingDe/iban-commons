# IBAN Commons

**Ultra-fast, allocation-free, zero-dependency IBAN & BIC toolkit for Java 8+**

<div align="center">
  <a href="https://central.sonatype.com/artifact/de.speedbanking/iban-commons"><img src="https://img.shields.io/maven-central/v/de.speedbanking/iban-commons?label=Maven%20Central&style=flat-square" alt="Maven Central Version"></a>
  <img src="https://img.shields.io/maven-central/last-update/de.speedbanking/iban-commons?label=Updated&style=flat-square&color=blue" alt="Maven Central Last Update">
  <a href="https://github.com/SpeedBankingDe/iban-commons/stargazers"><img src="https://img.shields.io/github/stars/SpeedBankingDe/iban-commons?logo=github&label=&logoColor=white&labelColor=555555&color=007ec6&style=flat-square" alt="GitHub Stars"></a>
  <br>
  <a href="https://sonarcloud.io/summary/overall?id=SpeedBankingDe_iban-commons"><img src="https://img.shields.io/sonar/coverage/SpeedBankingDe_iban-commons?server=https%3A%2F%2Fsonarcloud.io&label=coverage&style=flat-square" alt="Test Coverage"></a>
  <a href="https://github.com/SpeedBankingDe/iban-commons/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci.yml?label=Build%20(Linux%2FWin%2C%20JDK%208%2F21%2F25)&style=flat-square" alt="GitHub Actions Workflow Status"></a>
  <a href="https://javadoc.io/doc/de.speedbanking/iban-commons"><img src="https://javadoc.io/badge2/de.speedbanking/iban-commons/javadoc.svg?style=flat-square" alt="Javadoc"></a>
  <a href="https://apidia.net/mvn/de.speedbanking/iban-commons"><img src="https://apidia.net/mvn/de.speedbanking/iban-commons/badge_flat_square.svg" alt="APIdia"></a>
</div>

<div align="center">

**<picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/rocket-dark.svg"><img src=".github/icons/rocket.svg" height="16" alt=""></picture> [Quick Start](#-quick-start)&emsp;<picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/code-dark.svg"><img src=".github/icons/code.svg" height="16" alt=""></picture> [Examples](#-code-examples)&emsp;<picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/chart-bar-dark.svg"><img src=".github/icons/chart-bar.svg" height="16" alt=""></picture> [Benchmarks](#-performance-benchmarks)&emsp;<picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/history-dark.svg"><img src=".github/icons/history.svg" height="16" alt=""></picture> [Changelog](CHANGELOG.md)&emsp;<picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/book-dark.svg"><img src=".github/icons/book.svg" height="16" alt=""></picture> [Javadoc](https://javadoc.io/doc/de.speedbanking/iban-commons/latest/)&emsp;<picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/messages-dark.svg"><img src=".github/icons/messages.svg" height="16" alt=""></picture> [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)**

</div>

`iban-commons` is a Java IBAN validation library and BIC validator for Java 8+, providing fast and reliable parsing, validation, and formatting of International Bank Account Numbers (IBAN) and Business Identifier Codes (BIC/SWIFT codes).
Designed for high-performance enterprise applications, it covers 127 countries, is Android-compatible (API 24+, or 21+ with core library desugaring), and has zero compile or runtime dependencies outside the Java Standard Library.

An optional [`iban-commons-de-checkdigit`](#german-account-check-digit-iban-commons-de-checkdigit) module is available for verifying German domestic account number ("Kontonummer") check digits against a Bankleitzahl (BLZ) - standalone and independent of `iban-commons`.

An optional [`iban-commons-bankdata`](#bank-data-lookup-iban-commons-bankdata) module resolves the BIC and bank name behind an IBAN or a country-specific bank code (DE, AT, CH, LI, CZ, BE, PL, NL, ES, FR, GR, PT), with a bundled offline fallback dataset and lazy background refresh.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/bolt-dark.svg"><img src=".github/icons/bolt.svg" height="20" alt=""></picture> Why IBAN Commons?

| Feature                    | iban-commons   | jbanking  | Apache Commons |  iban4j   | garvelink |
|----------------------------|:--------------:|:---------:|:--------------:|:---------:|:---------:|
| **Throughput (ops/s)**     | **11,217,693** | 7,761,777 |   6,417,898    | 2,338,722 | 2,206,021 |
| **Memory (B/op)**          |      **0**     |    179    |      210       |    967    |    559    |
| **Dependencies**           |        0       |     0     |       4        |     0     |     0     |
| **Java Version**           |       8+       |    8+     |       8+       |    11+    |    8+     |
| **Countries**              |       127      |    98     |      105       |    106    |    111    |
| **National Check Digits**  |       43       |     0     |       0        |     15    |     0     |

> Throughput: rejection path (invalid IBANs) · JMH · OpenJDK 25.0.3 · Linux · single core · SerialGC · `-XX:-StackTraceInThrowable` · 2026-11-27<br>
> Compared versions: jbanking 4.3.0 · Apache Commons Validator 1.11.0 · iban4j 3.2.14 · garvelink iban 1.17.0. Countries count every country code accepted, including territories using another country's format. National Check Digits count the country codes whose national check digit can be verified, an opt-in in iban-commons and iban4j.

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/rocket-dark.svg"><img src=".github/icons/rocket.svg" height="20" alt=""></picture> Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons</artifactId>
    <version>1.8.14</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'de.speedbanking:iban-commons:1.8.14'
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

iban.getCountryCode();    // IT
iban.getCountryName();    // Italy
iban.getCountryFlag();    // 🇮🇹
iban.getBankCode();       // 05428
iban.getBranchCode();     // 11101
iban.getAccountNumber();  // 000000123456
iban.toFormattedString(); // IT60 X054 2811 1010 0000 0123 456
```

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/list-check-dark.svg"><img src=".github/icons/list-check.svg" height="20" alt=""></picture> Key Features

* **Zero Dependencies & Small Footprint**

  Keep your build clean and avoid dependency conflicts

* **High Performance**

  Zero-allocation validation pipeline - `char[]`-based processing eliminates heap pressure throughout.
  Validates **5,910,767 IBANs/s** on the accept path and **11,217,693 IBANs/s** on early rejection;
  allocation is exactly **0 B/op**, against 179 to 967 B/op for competing libraries.

* **Simple, intuitive API**

  Intuitive factory methods (`Iban.of()`, `Bic.tryParse()`), Android-compatible

* **Immutability**

  `Iban`, `Bic`, `IbanRegistry` classes are immutable and thread-safe

* **Java 8 compatibility**

  Targeting Java 8 for maximum reach, built with JDK 21 and tested on Windows and Linux with JDK 8, 21, 25 and all upcoming LTS versions

* **Comprehensive Coverage**

  Full support for IBAN and BIC validation per ISO 13616 and ISO 9362.
  Covers **127 countries** including all from the SWIFT IBAN Registry - more than any comparable Java IBAN validation library.

* **Rich Metadata**

  Access country names, flags, bank codes, SEPA status, currency information, and regulatory organization details

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/code-dark.svg"><img src=".github/icons/code.svg" height="20" alt=""></picture> Code Examples

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

Use `RandomIban` to generate syntactically correct, structurally valid IBANs with proper ISO 7064 Mod 97-10 check digits - ideal for testing, seeding demo data, or populating sandboxes.

**Any supported country (non-deterministic):**
```java
import de.speedbanking.iban.RandomIban;

Iban rdIban   = RandomIban.of();     // random country
Iban deIban   = RandomIban.of("DE"); // specific country
Iban sepaIban = RandomIban.ofSepa(); // random SEPA country
```

**Reproducible generation with a seeded `Random`:**

Pass a seeded `java.util.Random` to get deterministic output - the same seed always produces the same IBAN for the same country, which is useful for unit tests and snapshot tests.

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

### German Account Check Digit (`iban-commons-de-checkdigit`)

An optional, standalone module implementing the Deutsche Bundesbank's
[*"Prüfzifferberechnungsmethoden in der deutschen Kreditwirtschaft"*](https://www.bundesbank.de/de/startseite/pruefzifferberechnungsmethoden-603320) -
all 144 assigned check digit methods (`00`-`99`, `A0`-`E4`) used to verify a German domestic
account number ("Kontonummer") against a Bankleitzahl (BLZ).

It operates purely on BLZ + Kontonummer + method code and has **zero dependency on `iban-commons`**
(or any other library), so it can be used on its own by callers who only have those three values -
typically from legacy domestic payment processing - and have obtained the applicable method code from
elsewhere, e.g. the Bundesbank's BLZ master-data file. This module does not select a method, only executes one.

See the module's own **[README](iban-commons-de-checkdigit/README.md)** for installation, usage
examples, and full method coverage details.

-----

### Bank Data Lookup (`iban-commons-bankdata`)

An optional module resolving the BIC and bank name of the institution behind an IBAN or a raw
country-specific bank code (German BLZ, Austrian Bankleitzahl, Swiss and Liechtenstein IID, Czech
kod banky, Belgian bank code, Polish numer rozliczeniowy, Dutch bank identifier, Spanish codigo de
entidad, French code banque, Greek bank code, Portuguese codigo de banco), self-hosted and
open-source. Country coverage: **DE, AT, CH, LI, CZ, BE, PL, NL, ES, FR, GR, PT**. For German
IBANs, `GermanIban` additionally checks the account number against the check digit method the
Bundesbank publishes for the bank.

Ships a bundled offline fallback dataset per country so lookups work immediately without
network access, then lazily refreshes itself in the background against each country's official
bank directory, persisting the result to a local cache directory that survives JVM restarts.
Depends on `iban-commons` (reuses `Iban` and `Bic`).

See the module's own **[README](iban-commons-bankdata/README.md)** for installation, usage
examples, configuration, and a data-accuracy disclaimer: this module is provided on a
**best-effort basis** with no guarantee of correctness, completeness, or timeliness of the bank
data it returns.

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/arrows-exchange-dark.svg"><img src=".github/icons/arrows-exchange.svg" height="20" alt=""></picture> Migrating from Other Libraries

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

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/chart-bar-dark.svg"><img src=".github/icons/chart-bar.svg" height="20" alt=""></picture> Performance Benchmarks

`iban-commons` is designed for **high throughput** and **minimal overhead**.

We use the Java Microbenchmark Harness (**JMH**) to compare the throughput of `iban-commons` against popular Java IBAN libraries: [iban4j](https://www.iban4j.org/), [Apache Commons Validator](https://commons.apache.org/proper/commons-validator/), [garvelink java-iban](https://github.com/barend/java-iban), and [jbanking](https://github.com/marcwrobel/jbanking).

Measured on **Intel Core i7-1165G7 @ 2.80GHz**, OpenJDK 25.0.3, Linux, single core (`taskset -c 0`),
SerialGC (`-XX:+UseSerialGC`), `-XX:-StackTraceInThrowable`, `-Xms2G -Xmx2G`.
3 forks × 5 warmup and 5 measurement iterations × 2 s each, run 2026-11-27 with iban-commons 1.8.14.

### Valid IBANs (accept path)

| Library                 | Throughput (ops/s) | Memory (B/op) | vs. iban-commons |
|:------------------------|-------------------:|--------------:|:----------------:|
| **iban-commons**        |      **5,910,767** |       **0**   | baseline         |
| jbanking                |          4,325,613 |           325 | 1.4× slower      |
| iban4j                  |          4,273,620 |           505 | 1.4× slower      |
| Apache Commons          |          3,078,120 |           398 | 1.9× slower      |
| garvelink java-iban     |          1,908,918 |           724 | 3.1× slower      |

### Invalid IBANs (rejection path)

| Library                 | Throughput (ops/s) | Memory (B/op) | vs. iban-commons |
|:------------------------|-------------------:|--------------:|:----------------:|
| **iban-commons**        |     **11,217,693** |       **0**   | baseline         |
| jbanking                |          7,761,777 |           179 | 1.4× slower      |
| Apache Commons          |          6,417,898 |           210 | 1.7× slower      |
| iban4j                  |          2,338,722 |           967 | 4.8× slower      |
| garvelink java-iban     |          2,206,021 |           559 | 5.1× slower      |

Each invalid IBAN is derived from a valid one by applying one of six sabotage strategies with equal probability: incrementing a check digit (triggering a Mod-97 failure), replacing the country code with the non-registered code `XY`, substituting a valid but mismatched ISO 3166 country code, injecting a letter into the numeric BBAN section, swapping two adjacent characters (transposition), or truncating the string below the minimum structural length.

### Key Observations

1. **Leading Throughput:** `iban-commons` is the fastest across both valid and invalid input - reaching **5,910,767 ops/s** on the accept path and **11,217,693 ops/s** on early rejection.
2. **Fast Rejection:** Invalid IBANs are rejected faster than valid ones because many fail length or country-code checks before the full Mod-97 computation is reached.
3. **Zero Allocation:** Memory allocation is **0 B/op** on both paths. Competing libraries allocate 179 to 967 B/op.

> **Note on `-XX:-StackTraceInThrowable`:** All forks run with this JVM flag, which suppresses stack trace generation to isolate pure algorithmic cost. This makes the comparison fair for libraries using exceptions for control flow (notably `iban4j`). For production-realistic measurements, remove the flag and re-run.

### Benchmark Suite Repository

All performance tests are fully open and available in the [SpeedBankingDe/iban-commons-benchmarks](https://github.com/SpeedBankingDe/iban-commons-benchmarks) repository.

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/sparkles-dark.svg"><img src=".github/icons/sparkles.svg" height="20" alt=""></picture> What's New in 1.8.15

### Bank Data for LI, GR and PT
`iban-commons-bankdata` now also covers Liechtenstein, Greece and Portugal, 12 countries in total. LI is read from the same SIX Bank Master file as CH and, like CH, ships without a bundled snapshot, since SIX has not granted permission to redistribute its data. GR and PT come from the ECB list that already serves ES and FR, with bundled snapshots. The snapshots of AT, ES, FR, NL and PL are refreshed.

### Safer Bank Data Downloads
Downloads and unpacked XLSX parts are capped at 64 MB, so an oversized response or a zip bomb only makes the refresh fail and the lookup keeps using the data it has. A redirect from HTTPS to plain HTTP is no longer followed.

### Iban Deserialization Fix
An `Iban` created with national check digit validation turned off could not be deserialized in a JVM that has it turned on. Deserialization now checks length, country, BBAN pattern and checksum, but not the national check digit, so such values can be read back regardless of the `IbanConfig` in effect.

### National Check Digit Fixes
With national check digit validation turned on, valid IBANs from several countries were rejected:
* Angola: the check digits are 98 rather than 00 when bank, branch and account number together form a multiple of 97, as ISO 7064 MOD 97-10 requires.
* Estonia: the check digit is weighted right to left over the account number only, without the bank code.
* Hungary: the check digit is the complement of the weighted sum rather than its last digit.
* Italy and San Marino: the CIN values letters at even positions from 0 to 25 rather than from 10 to 35.

Norwegian account numbers whose MOD 11 result is 10 admit no check digit, banks do not issue them. They are now rejected, before any check digit passed.

### IbanBuilder Fixes
`IbanBuilder`, and with it `RandomIban` and `@RandomIbanSource`, now always calculates the national check digit. Before, it did so only with `calculateNcd(true)` in `IbanConfig` and otherwise left a random value in place, so most IBANs built for countries with a national check digit failed its validation. The builder also no longer produces Norwegian account numbers without a valid check digit.

IBANs built for a territory such as `AX` now report the country code of the base country (`FI`), like a parsed one.

### Tested on JDK 25
CI now runs the tests on JDK 8, 21 and 25, on Linux and Windows.

### Breaking Changes
The `calculateNcd` setting is removed from `IbanConfig`, together with `IbanConfig.isCalculateNcd()` and `IbanConfig.Builder.calculateNcd(boolean)`, as the national check digit is now always calculated. Drop the call when updating.

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/help-circle-dark.svg"><img src=".github/icons/help-circle.svg" height="20" alt=""></picture> FAQ

<details>
<summary>Which countries are supported?</summary>

127 countries including all from the SWIFT IBAN Registry Release 103 (September 2026):
- All SEPA countries including major economies: Germany, UK, Switzerland, Norway, etc.
- All known non-SEPA countries that support IBAN
- Full list available in the [source code](iban-commons/src/main/java/de/speedbanking/iban/IbanRegistry.java)
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

Yes. `iban-commons` runs on Android from API level 24 (Android 7.0) onwards, which the build
verifies against the Android API signatures. The only exception is `IbanRegistry.getLastUpdate()`,
which returns a `java.time.YearMonth` and therefore needs API level 26; `getLastUpdateYear()` and
`getLastUpdateMonth()` work on all levels.

On API levels 21 to 23 it needs
[core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring)
enabled in the app, since it uses `java.util.Optional`, `java.util.stream` and `java.util.function`.
With desugaring, `getLastUpdate()` works as well. This was checked for 1.8.14 against the API 21
signatures including the desugared library, but unlike API 24 it is not part of the build.
</details>

<details>
<summary>Is the library thread-safe?</summary>

Yes. Both `Iban` and `Bic` are immutable value objects - all fields are `final` and set in the constructor.
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

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/git-pull-request-dark.svg"><img src=".github/icons/git-pull-request.svg" height="20" alt=""></picture> Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

**Quick Ways to Contribute:**
- Report bugs via [Issues](https://github.com/SpeedBankingDe/iban-commons/issues)
- Suggest features in [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)
- Improve documentation
- Submit pull requests

**Development Setup:**

* **JDK 21** or higher required to build the project
* Maven 3.9+ required

```bash
git clone https://github.com/SpeedBankingDe/iban-commons.git
cd iban-commons
mvn clean verify
```

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/message-circle-dark.svg"><img src=".github/icons/message-circle.svg" height="20" alt=""></picture> Support & Community

- Questions? Ask in [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)
- Found a bug or missing a feature? Open an [Issue](https://github.com/SpeedBankingDe/iban-commons/issues)
- API Reference: [Javadoc on javadoc.io](https://javadoc.io/doc/de.speedbanking/iban-commons/latest/)
- Website: [www.speedbanking.de](https://www.speedbanking.de/)

**Please star this repo if you find it useful.**

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/shield-lock-dark.svg"><img src=".github/icons/shield-lock.svg" height="20" alt=""></picture> Security

To report a vulnerability, please use GitHub's private
[Security Advisory](https://github.com/SpeedBankingDe/iban-commons/security/advisories/new)
mechanism rather than opening a public issue.

For details on the project's security policy and the serialization hardening built
into `Iban` and `Bic`, see [SECURITY.md](SECURITY.md).

-----

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/scale-dark.svg"><img src=".github/icons/scale.svg" height="20" alt=""></picture> License

This project is licensed under the **Apache License, Version 2.0**. You can find the full text of the license [here](https://www.apache.org/licenses/LICENSE-2.0).

<p style="height: 20px;">&nbsp;</p>

<div align="center">
<table style="border-collapse: collapse;">
  <tr>
    <td style="padding: 40px; border: 2px solid #3a82c2;">
      <strong>Enjoying 
        <a href="https://github.com/SpeedBankingDe/iban-commons" style="color: #3a82c2; text-decoration: none;">iban-commons</a>?
      </strong><br>
      <strong>Please leave a star to support the project!</strong><br>
      <small>Your stars help to keep open source development visible and going.</small>
    </td>
  </tr>
</table>
</div>

<!-- SEO: common search terms for this project -->
<!-- java iban validation | iban validator java | bic validation java | swift code validator java -->
<!-- iban4j alternative | apache commons iban | sepa validation java | java iban library -->
<!-- android iban validation | iban parser java 8 | iso 13616 java | mod97 java -->
