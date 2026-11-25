# Security Policy

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/tag-dark.svg"><img src=".github/icons/tag.svg" height="20" alt=""></picture> Supported Versions

Security fixes go into the latest release only. Older versions do not receive backported patches.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/shield-lock-dark.svg"><img src=".github/icons/shield-lock.svg" height="20" alt=""></picture> Reporting a Vulnerability

Please do not report security vulnerabilities through public GitHub issues. Use GitHub's private reporting instead (Security tab of the repository, then "Report a vulnerability"), or send an email to the maintainer address listed in `pom.xml`.

A good report contains a description of the vulnerability and its impact, the steps to reproduce it or a minimal proof of concept, and the affected versions. You can expect an acknowledgement within 5 business days and a status update within 14 days.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/shield-check-dark.svg"><img src=".github/icons/shield-check.svg" height="20" alt=""></picture> Security Design

### Input Validation

All public factory methods (`Iban.of()`, `Iban.parse()`, `Bic.of()` etc.) validate their input against the library's IBAN registry, which is based on the SWIFT IBAN Registry, before they create an object. Invalid input throws `InvalidIbanException` or `InvalidBicException`, it never results in a malformed instance.

### Serialization

`Iban` and `Bic` are immutable and implement `java.io.Serializable` using the serialization proxy pattern:

- `writeReplace()` replaces each instance with a small `Memento` proxy, so only the normalized string value is written to the stream.
- `readResolve()` in `Memento` validates that value again before it creates the object: structure and Mod 97-10 check digits for an `Iban` (national check digits are left out, so the result does not depend on the configuration of the reading JVM), the full `Bic.parse()` for a `Bic`.
- `readObject()` and `readObjectNoData()` on `Iban` and `Bic` always throw `InvalidObjectException`, so a raw instance cannot be deserialized directly, not even from a crafted byte stream.
- The `Memento` stream starts with a version number. A version mismatch causes an `InvalidObjectException`.

As a result, a manipulated stream cannot produce an `Iban` with an invalid check digit, an unknown country code or a malformed BBAN, nor an invalid `Bic`.

### Network Access

The core module, `iban-commons-validation` and `iban-commons-de-checkdigit` perform no network I/O. All their data is compiled into the library.

`iban-commons-bankdata` downloads bank directories from the official sources of each country and caches them locally (by default in `~/.iban-commons/bankdata`). All bundled source URLs use HTTPS, and a redirect from HTTPS to plain HTTP is rejected. Responses and unpacked XLSX parts are limited to 64 MB, and the XML parser rejects DTDs. If a download fails, the module keeps using its cached or bundled data. Setting `disableNetwork = true` in the configuration turns off all downloads. The source URLs can be overridden by a configuration file, which is therefore treated as trusted input.

### Dependencies

The core module has no runtime dependencies beyond the Java standard library. `iban-commons-validation` needs the Jakarta Validation API, which the application provides. `iban-commons-bankdata` only depends on the other modules of this project.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/circle-off-dark.svg"><img src=".github/icons/circle-off.svg" height="20" alt=""></picture> Out of Scope

The following is not considered a security vulnerability of this library:

- An IBAN or BIC that is syntactically valid but belongs to an account or bank that does not exist. The library validates formats and check digits, not existence.
- Countries or formats that are not listed in the SWIFT IBAN Registry and not maintained in the library's own registry.
- Outdated or wrong entries in the bank directories that `iban-commons-bankdata` takes from its upstream sources.
- The behavior of third-party code that wraps or re-serializes instances created by this library.
