# Security Policy

## Supported Versions

Security fixes are applied to the **latest stable release** only.
Older versions do not receive backported patches.

| Version | Supported |
|---------|-----------|
| latest  | ✅         |
| older   | ❌         |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Report vulnerabilities privately via GitHub's built-in mechanism:
**Security → Report a vulnerability** (in the repository's Security tab).

Alternatively, send an email to the maintainer address listed in `pom.xml`.
Please include:

- A description of the vulnerability and its potential impact
- Steps to reproduce or a minimal proof-of-concept
- The affected version(s)

You can expect an acknowledgement within **5 business days** and a status update
within **14 days**.

## Security Design

### Serialization (IBAN and BIC)

`Iban` and `Bic` are immutable value objects that implement `java.io.Serializable`.
Both classes use the **Serialization Proxy Pattern** to defend against
byte-stream injection attacks:

- **`writeReplace()`** substitutes each instance with a lightweight `Memento` proxy
  before it is written to a stream. Only the normalized string value crosses the
  serialization boundary.
- **`readResolve()`** in `Memento` reconstructs the object exclusively through the
  public factory method (`Iban.parse()` / `Bic.of()`), which runs the full
  validation pipeline (length, structure, ISO 7064 Mod 97-10 check digits).
- **`readObject()`** and **`readObjectNoData()`** on `Iban` and `Bic` unconditionally
  throw `InvalidObjectException`, blocking any attempt to deserialize a raw instance
  directly — even from a crafted byte stream.
- The `Memento` stream format includes an explicit **version `long`** written before
  the value string. A version mismatch causes `InvalidObjectException` during
  deserialization, enabling forward-compatible format evolution.

Consequence: it is not possible to deserialize an `Iban` or `Bic` object that would
fail validation. An attacker cannot inject an instance with an invalid check digit,
an unsupported country code, or a malformed BBAN via a manipulated stream.

### Input Validation

All public factory methods (`Iban.of()`, `Iban.parse()`, `Bic.of()`, etc.) validate
their input against the project's IBAN Registry (based on SWIFT IBAN Registry) 
before constructing an object.
Invalid input throws `InvalidIbanException` or `InvalidBicException` respectively;
it never silently produces a malformed instance.

### No Network Access

This library performs no network I/O. All country and registry data is compiled
into the library at build time.

### Dependencies

The library has **no runtime dependencies** beyond the Java standard library.
The attack surface from transitive dependencies is therefore zero.

### Android Compatibility Notes

`java.util.Optional` (API 24+) and `java.time.YearMonth` (API 26+) are used
internally. Null-returning alternatives (`tryParseOrNull()`, `getLastUpdateYear()`,
`getLastUpdateMonth()`) are provided for Android projects that cannot enable
core library desugaring. These carry the same validation guarantees as their
`Optional`-returning counterparts.

## Out of Scope

The following are **not** considered security vulnerabilities for this library:

- IBAN or BIC values that are syntactically valid but belong to non-existent accounts
  (this library validates format and check digits, not account existence)
- Countries or formats not yet listed in the SWIFT IBAN Registry or not 'manually'
  maintained in the library's own registry
- Behavior of third-party code that wraps or re-serializes instances produced by
  this library
