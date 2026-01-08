**Welcome to IBAN Commons!**

IBAN Commons is our zero-dependency, ultra-fast, low-memory IBAN and BIC toolkit with a small, concise API.

[](https://www.apache.org/licenses/LICENSE-2.0)
[](https://www.google.com/search?q=https://central.sonatype.com/artifact/de.speedbanking/iban-commons)
[](https://www.google.com/search?q=pom.xml)

The `iban-commons` library provides simple, fast, and reliable validation and decomposition of International Bank Account Numbers (**IBAN**) and Business Identifier Codes (**BIC**). Designed for high-performance enterprise applications, it intentionally has **zero compile or runtime dependencies** outside the Java Standard Library.

![Maven Central Version](https://img.shields.io/maven-central/v/de.speedbanking/iban-commons?label=Maven%20Central)
![Maven Central Last Update](https://img.shields.io/maven-central/last-update/de.speedbanking/iban-commons?label=Last%20Update)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk11_ubuntu.yml?label=Build%20(JDK%2011%20Linux))
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/SpeedBankingDe/iban-commons/ci_jdk11_win.yml?label=Build%20(JDK%2011%20Win))
![GitHub License](https://img.shields.io/github/license/SpeedBankingDe/iban-commons)
![GitHub Repo stars](https://img.shields.io/github/stars/SpeedBankingDe/iban-commons?logoColor=%233a82c2)

-----

## 🚀 Key Features

  * **Zero Dependencies**

    Keep your build clean and avoid dependency conflicts

  * **High Performance**

    Optimized for execution speed and minimum memory footprint

  * **Small Footprint**

    ~100 kB JAR

  * **Simple, intuitive API**

    Intuitive factory methods (`Iban.of()`, `Bic.tryParse()`), clear component accessors

  * **Immutability**

    Both `Iban` and `Bic` classes are immutable and thread-safe

  * **Java 8 compatibility**
  
    Built with and compiled for Java 8 for maximum reach, tested on recent LTS versions

  * **Comprehensive**

    Full support for IBAN and BIC validation according to ISO standards

-----

## 🛠️ Usage

### Maven Dependency

Add the following to your project's `pom.xml`:

```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons</artifactId>
    <version>1.8.1</version>
</dependency>
```

### Gradle Dependency

Add this line to your project's `build.gradle` or `build.gradle.kts`:

```
implementation 'de.speedbanking:iban-commons:1.8.1'
```

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
    println("Country Code: " + iban.getCountryCode());    // DE
    println("Check Digits: " + iban.getCheckDigits());    // 91
    println("BBAN        : " + iban.getBban());           // 100000000123456789
    println("Bank Code   : " + iban.getBankCode());       // 10000000
    println("Account No  : " + iban.getAccountNumber());  // 0123456789

    // output
    println("Normalized  : " + iban.toString());          // DE91100000000123456789
    println("Formatted   : " + iban.toFormattedString()); // DE91 1000 0000 0123 4567 89

} catch (InvalidIbanException ex) {
    println("IBAN validation failed: " + ex.getMessage());
}

static void println(String s) { System.out.println(s); }

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

-----

### BIC

The `Bic` class implements `Serializable`, `CharSequence`, and `Comparable<Bic>`.
A BIC comparison is always based on the 11-character representation (`toBic11()`), meaning BIC-8 and its BIC-11 equivalent are considered equal.

#### 1\. Quick Validation (throws exception on failure)

```java
import de.speedbanking.bic.Bic;

// BIC-11
Bic bic11 = Bic.of("PALSPS22XXX");                    // Bank of Palestine P.S.C.

println("Bank Code    : " + bic11.getBankCode());     // PALS
println("Country Code : " + bic11.getCountryCode());  // PS
println("Location Code: " + bic11.getLocationCode()); // 22
println("Branch Code  : " + bic11.getBranchCode());   // XXX
println("is BIC-11    : " + bic11.isBic11());         // true
println("is BIC-8     : " + bic11.isBic8());          // false
println("to BIC-8     : " + bic11.toBic8());          // PALSPS22

// BIC-8
Bic bic8 = Bic.of("MARKDEFF");                        // Deutsche Bundesbank, Zentrale

println("is BIC-8     : " + bic8.isBic8());           // true
println("is BIC-11    : " + bic8.isBic11());          // false
println("to BIC-11    : " + bic8.toBic11());          // MARKDEFFXXX
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

## ⏱️ Performance Benchmarks

`iban-commons` is designed for **high throughput** and **minimal overhead**.

We use the Java Microbenchmark Harness (**JMH**) to compare the throughput of `iban-commons` against popular Java IBAN libraries: [iban4j](http://www.iban4j.org/), [Apache Commons Validator](https://commons.apache.org/proper/commons-validator/), and [garvelink-iban](https://github.com/barend/java-iban).

### Validation Throughput Comparison

The results below demonstrate the significant **throughput** advantage (operations per second) of `iban-commons`.

| Benchmark Operation                         | iban-commons (ops/s) | Apache Commons (ops/s) | garvelink-iban (ops/s) | iban4j (ops/s) | Speedup vs. iban4j |
|---------------------------------------------|----------------------|------------------------|------------------------|----------------|--------------------|
| **Pure Validation** (`.isValid()`)          | **5257**             | 4786                   | n/a                    | 814            | **~6.4x faster**   |
| **Object Creation** (`.tryParse()`/`.of()`) | **2993**             | n/a                    | 863                    | 754            | **~3.9x faster**   |

### Memory Footprint Comparison

These results, derived from the JMH **GC Profiler**, quantify the **memory allocation overhead** per operation (B/op). Lower values indicate less work for the Garbage Collector (GC).

| Benchmark Operation                         | iban-commons (B/op) | Apache Commons (B/op) | garvelink-iban (B/op) | iban4j (B/op) | Allocation Efficiency         |
|---------------------------------------------|---------------------|-----------------------|-----------------------|---------------|-------------------------------|
| **Pure Validation** (`.isValid()`)          | **136.9M**          | 326.8M                | n/a                   | 1627.3M       | **~11.9x smaller vs. iban4j** |
| **Object Creation** (`.tryParse()`/`.of()`) | **253.9M**          | n/a                   | 1126.1M               | 1633.7M       | **~6.4x smaller vs. iban4j**  |

*Note on Memory Units: The B/op values represent normalized allocations per benchmark operation. The **relative difference** confirms the high allocation efficiency of iban-commons.*

### Key Observations

1. **Leading Throughput:**       `iban-commons` outperforms all tested libraries, reaching **5257 operations per second** for validation.
2. **Superior Object Creation:** Compared to `garvelink-iban`, `iban-commons` is about **3.4x faster** and uses **4.4x less memory** when creating immutable objects.
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

## ⚖️ License

This project is licensed under the **Apache License, Version 2.0**. You can find the full text of the license [here](https://www.apache.org/licenses/LICENSE-2.0).

```
Copyright © 2025 Markus Spann, SpeedBankingDe

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

-----
