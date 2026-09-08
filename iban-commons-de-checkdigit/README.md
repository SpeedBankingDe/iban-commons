# IBAN Commons DE Check Digit

> Standalone, zero-dependency German bank account ("Kontonummer") check digit verification for Java 8+

<div align="center">
  <a href="https://central.sonatype.com/artifact/de.speedbanking/iban-commons-de-checkdigit"><img src="https://img.shields.io/maven-central/v/de.speedbanking/iban-commons-de-checkdigit?label=Maven%20Central&style=flat-square" alt="Maven Central Version"></a>
  <a href="https://javadoc.io/doc/de.speedbanking/iban-commons-de-checkdigit"><img src="https://javadoc.io/badge2/de.speedbanking/iban-commons-de-checkdigit/javadoc.svg?style=flat-square" alt="Javadoc"></a>
</div>

`iban-commons-de-checkdigit` implements the Deutsche Bundesbank's
[*"Prüfzifferberechnungsmethoden in der deutschen Kreditwirtschaft"*](https://www.bundesbank.de/de/startseite/pruefzifferberechnungsmethoden-603320) —
the catalogue of check digit algorithms (methods `00`–`99`, `A0`–`D9`) used to verify a German
domestic account number ("Kontonummer") against a Bankleitzahl (BLZ). **122 of the 127 assignable
method codes** are implemented; see [Coverage](#coverage) below for what's left out and why.

This module is part of the [`iban-commons`](../README.md) project but is **deliberately standalone**:
it has zero dependency on `iban-commons` (or anything else) and can be used entirely on its own by
callers who only have a BLZ, a Kontonummer, and a method code — typically from legacy domestic
payment processing. It does not select a method; it only executes one you already know.

-----

## Quick Start

**Maven:**
```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons-de-checkdigit</artifactId>
    <version>1.8.10-SNAPSHOT</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'de.speedbanking:iban-commons-de-checkdigit:1.8.10-SNAPSHOT'
```

```java
import de.speedbanking.checkdigit.de.CheckDigitResult;
import de.speedbanking.checkdigit.de.GermanAccountCheckDigit;

// method code "00", BLZ "10000000", Kontonummer "0532013000"
CheckDigitResult result = GermanAccountCheckDigit.verify("00", "10000000", "0532013000");

if (result.isValid()) {
    // check digit is internally consistent — or the method performs no verification at all
} else {
    // check digit mismatch
}
```

> **Note:** A positive `isValid()` result means the check digit is internally consistent with the
> method used — it does **not** confirm that the account actually exists at the bank. Some methods
> perform no verification by design (e.g. `09`); use `result.isChecked()` to distinguish an actual
> pass from a no-op.

## Where does the method code come from?

The method code is bank-specific and this module does not look it up — it must be obtained from
elsewhere, most commonly the Bundesbank's public BLZ master-data file (each record lists the
`Prüfzifferberechnungsmethode` for that Bankleitzahl). If you already resolve BLZ master data in
your application, feed that method code straight into `GermanAccountCheckDigit.verify(...)`.

## Coverage

Of the 128 method codes defined by the reference source, `12` is explicitly marked as unassigned
("12 is not used") and is not registered. Of the remaining 127, **122 are implemented**; five are
deliberately left out because the risk of a silent transcription error outweighs the value of a
formula nobody can cross-check without the authoritative Bundesbank text:

| Code(s)      | Reason                                                                                                  |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `52`, `53`   | Build a combined "ESER-Nummer" interleaving specific BLZ digits with the account number at computed offsets, then search for a matching factor via a modular loop. |
| `87`         | A stateful digit-substitution algorithm with running parity/carry variables threaded across the whole account number.            |
| `B6`, `C0`   | Delegate part of their logic to `53` and `52` respectively, so they're incomplete without them.                                   |

All 122 implemented methods are exercised by the test suite against real Bundesbank test account
numbers where the reference source provides them. `GermanAccountCheckDigit.verify(...)` throws
`IllegalArgumentException` for any of the codes above (or any other unknown code) — see the
[`GermanCheckDigitMethod`](src/main/java/de/speedbanking/checkdigit/de/GermanCheckDigitMethod.java)
class javadoc for the full per-method breakdown, including a few implemented "with reservation"
where the reference source itself is ambiguous.

## License

This project is licensed under the **Apache License, Version 2.0** — see [`../LICENSE.txt`](../LICENSE.txt).

-----

Part of the [`iban-commons`](../README.md) project.
