# IBAN Commons Bank Data

> Bank master data (BIC, bank name, address) lookup by IBAN or bank code, for Java 8+

<div align="center">
  <a href="https://central.sonatype.com/artifact/de.speedbanking/iban-commons-bankdata"><img src="https://img.shields.io/maven-central/v/de.speedbanking/iban-commons-bankdata?label=Maven%20Central&style=flat-square" alt="Maven Central Version"></a>
  <a href="https://javadoc.io/doc/de.speedbanking/iban-commons-bankdata"><img src="https://javadoc.io/badge2/de.speedbanking/iban-commons-bankdata/javadoc.svg?style=flat-square" alt="Javadoc"></a>
</div>

`iban-commons-bankdata` resolves the BIC and bank name of the institution behind an IBAN or a raw
country-specific bank code (German BLZ, Austrian Bankleitzahl, Swiss BC-Nummer, Czech kod banky,
Belgian bank code), self-hosted and open-source.

Initial country coverage: **Germany (DE), Austria (AT), Switzerland (CH), Czech Republic (CZ),
Belgium (BE)**. The architecture is built to be extensible to any ISO country code; only the
concrete per-country loaders are limited to these five in this release.

This module depends on [`iban-commons`](../README.md) (it reuses `Iban` and `Bic`).

-----

## Quick Start

**Maven:**
```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons-bankdata</artifactId>
    <version>1.8.11-SNAPSHOT</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'de.speedbanking:iban-commons-bankdata:1.8.11-SNAPSHOT'
```

```java
import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.BankDataLookup;
import de.speedbanking.iban.Iban;

Optional<BankData> bank = BankDataLookup.find(Iban.of("DE89370400440532013000"));

bank.ifPresent(b -> {
    System.out.println(b.getBankName());       // "Commerzbank AG" (once real data is loaded)
    System.out.println(b.getBic());             // e.g. "COBADEFFXXX"
    System.out.println(b.getCity());             // e.g. "Frankfurt am Main"
});

// or look up directly by country-specific bank code, without an IBAN:
Optional<BankData> byBankCode = BankDataLookup.findByBankCode("DE", "37040044");
```

`BankDataLookup` never throws for "not found", "country not supported", or "network unreachable" -
all three simply produce `Optional.empty()`.

A small runnable sample (`BankDataLookupSample`) generates a few random, valid IBANs and prints
each institution's resolved BIC and bank name:
```bash
cd iban-commons-bankdata
mvn test-compile exec:java@bank-data-lookup-sample
```

## How data is loaded

1. **Offline fallback ("Urladung").** Each supported country ships a bundled classpath snapshot
   (`/bankdata/{DE,AT,CH,CZ,BE}.csv`), a real download from the live source at the time of release,
   so lookups work immediately, with zero network access, right after adding the dependency.
2. **Local cache.** Once refreshed at least once, data is persisted as a single CSV file per
   country to a local cache directory (`${user.home}/.iban-commons/bankdata` by default; see
   [`BankDataConfig`](#configuration)) and survives JVM restarts. Persisting a refresh writes to a
   temporary file first and then atomically renames it into place, so a crash mid-write can never
   leave a partially written file at the final path.
3. **Lazy, asynchronous refresh.** After every `BankDataLookup` call, a non-blocking staleness
   check may schedule a background download of fresh data for that country (default threshold: 90
   days). Staleness is always based on the actual age of the currently loaded data: the local cache
   file's last-modified timestamp, or the bundled classpath resource's last-modified timestamp when
   no local cache file is present yet (so a freshly rebuilt bundled resource, e.g. right after a
   release, is not immediately flagged stale). Before parsing a local cache file, it is sanity
   checked (non-empty, starts with the expected CSV header); a file that fails this check is
   treated as if it were absent and the bundled fallback data is used instead. The synchronous
   lookup itself is never blocked by network I/O; a failed refresh simply keeps the last known good
   data and logs a warning, including a short diagnosis for common network failure causes (proxy
   authentication, DNS resolution, a blocking firewall/proxy, or a timeout).

## Configuration

`BankDataConfig` follows the same initialize-once pattern as `iban-commons`'s `IbanConfig`:

```java
BankDataConfig.configure(BankDataConfig.builder()
    .cacheDirectory(Paths.get("/var/cache/myapp/bankdata"))
    .staleThreshold(Duration.ofDays(30))
    .connectTimeout(Duration.ofSeconds(3))
    .readTimeout(Duration.ofSeconds(15))
    .disableNetwork(false)
    .build());
```

Must be called before the first `BankDataLookup`/`BankDataConfig.get()` invocation. If never
called, the default cache directory is resolved from, in order: the
`de.speedbanking.bankdata.cacheDir` system property, the `IBAN_COMMONS_BANKDATA_CACHE_DIR`
environment variable, or `${user.home}/.iban-commons/bankdata`.

Network access (downloads and background refreshes) can be disabled entirely, e.g. for
environments with no outbound network access at all, via `Builder.disableNetwork(true)`, or by
setting the `de.speedbanking.bankdata.disableNetwork` system property or the
`IBAN_COMMONS_BANKDATA_DISABLE_NETWORK` environment variable to `true` (same resolution order as
the cache directory). When disabled, `triggerAsyncRefreshIfStale()` is a complete no-op and lookups
are served exclusively from the local cache and bundled fallback data.

## Logging

This module logs via plain `java.util.logging` (no new dependency), so it never imposes any
logging configuration on the host application. Its default output is the JDK's verbose two-line
`SimpleFormatter` format; if you'd rather have a single, grep-friendly line with a UTC ISO-8601
timestamp, opt in explicitly with `CompactLogFormatter`, either declaratively via a
`logging.properties` file passed as `-Djava.util.logging.config.file=...`:
```properties
handlers = java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.formatter = de.speedbanking.bankdata.log.CompactLogFormatter
```
or programmatically, which also works when running in a build tool's in-process JVM (e.g.
`exec-maven-plugin`'s `java` goal, as used by `BundledDataRefreshTool` and
`BankDataLookupSample` above):
```java
CompactLogFormatter.installOnRootLogger();
```

## Extending to another country

Implement `de.speedbanking.bankdata.spi.CountryBankDataLoader` for the new country's raw source
format. This module wires loaders through an internal, fixed registry (`BankDataRegistry`) rather
than `ServiceLoader`/`META-INF/services` discovery, consistent with `IbanRegistry`/
`CountryValidators` elsewhere in this project. Third-party pluggability beyond the loaders shipped
with this module (i.e. registering your own `CountryBankDataLoader` without forking this module)
is **not part of this release**.

## Regenerating bundled data

The bundled "Urladung" CSV files under `src/main/resources/bankdata` can be regenerated from each
country's live upstream source with a maintainer-only tool, `BundledDataRefreshTool`
(`src/test/java/de/speedbanking/bankdata/tool`, test-scope only, not part of the published jar).
Run it with the module directory (not the repository root) as the actual working directory, since
the tool writes to a path relative to the current directory:

```shell
cd iban-commons-bankdata
mvn test-compile exec:java@refresh-bundled-bankdata
```

Running it via `mvn -pl iban-commons-bankdata ...` from the repository root does **not** work: the
`exec-maven-plugin` `java` goal runs in-process and inherits whatever directory `mvn` itself was
started from, not the module's directory. The tool detects this (the expected
`src/main/resources/bankdata` directory would not be found relative to the wrong working directory)
and aborts immediately without writing anything, rather than silently creating a stray directory
tree in the wrong place.

It downloads and re-parses each registered country's source and overwrites `{CC}.csv` on success;
a failure for one country is reported and leaves that country's existing file untouched, the tool
then continues with the next country. This is a best-effort maintainer convenience, typically run
once before cutting a release, not a CI gate, since it depends on outbound network access and on
each loader's upstream source URL/format still being accurate. Review the resulting diff (in
particular record counts and a few spot-checked entries) before committing.

## ⚠️ Data accuracy disclaimer

**This module is provided on a best-effort basis and comes with no guarantee of correctness,
completeness, or timeliness of the bank data it returns.** `BankDataLookup` results are derived
from third-party national bank registries (Deutsche Bundesbank, OeNB, SIX Interbank Clearing, Ceska
narodni banka, Nationale Bank van Belgie / Banque Nationale de Belgique) that publish and amend
their data on their own schedule, outside this project's control. A result may be
outdated between refreshes (background refresh only runs after the configured staleness threshold
is reached, see [How data is loaded](#how-data-is-loaded)), and a failed refresh silently continues
serving the last known good, potentially stale, data rather than failing the lookup. Do not treat
a `BankData` record as authoritative for payment routing, compliance, or other decisions where
incorrect or outdated bank data could cause harm; always validate against an authoritative source
for such use cases, and use `BankData.getSourceVersion()` to check the vintage of a given result.

In addition, at the time of this release the download URLs and raw column layouts in
`DeBundesbankLoader`, `AtBankDataLoader`, and `ChBankDataLoader` have been **verified against a
live download** of each institution's current, official source. `CzBankDataLoader`'s source URL
and general CSV shape were researched against the live CNB download page, but the exact current
column order and encoding are still marked `// TODO:` for re-verification before release.
`BeBankDataLoader`'s source URL was researched against the live NBB/BNB download page and its
`full_list_current.xlsx` column layout has since been verified too. The bundled
`/bankdata/{DE,AT,CH,CZ,BE}.csv` fallback files are, for every country, a real snapshot downloaded
from the live source with the maintainer tool below, not hand-curated examples, so the module is
fully usable offline out of the box; this also means their record counts and file sizes are
non-trivial (the largest, `DE.csv`, is a few hundred KB with several thousand records) and change
on every refresh as the upstream sources are amended. `CZ`'s bundled data is still subject to the
same column-layout caution as its loader above.

## Notable implementation choices

- **`src/main/resources` / `src/test/resources`** are used in this module to ship the bundled
  offline fallback data and raw-format test fixtures, respectively. This is new to the
  `iban-commons` project (no other module previously used non-Java resources) and is a deliberate,
  scoped exception for this module's "Urladung" and fixture needs.
- **HTTP transport** uses `java.net.HttpURLConnection` exclusively, no new runtime dependency,
  consistent with the zero-dependency philosophy of the rest of the project, including manual
  single-hop redirect following across protocol changes (`http` → `https`), since
  `HttpURLConnection`'s built-in redirect handling does not cover that case.
- **Logging** goes through a small internal `BankDataLog` facade over `java.util.logging.Logger`
  (available since Java 1.4) rather than `java.lang.System.Logger` (Java 9+), since this module
  targets `releaseJavaVersion=8` like the rest of the project.
- **`BeBankDataLoader`'s source is an XLSX file**, not a delimited text file like the other
  countries. Rather than adding Apache POI (or any other library) as a runtime dependency, this
  module ships its own minimal, internal `MinimalXlsxReader` (package `io`) that reads the first
  worksheet of an XLSX using only JDK-provided facilities: `java.util.zip.ZipInputStream` to
  unpack the underlying ZIP container and `javax.xml.parsers` (DOM) to parse the two relevant
  XML parts (`xl/sharedStrings.xml`, `xl/worksheets/sheet1.xml`). It intentionally supports only
  what this module needs, no formulas, no cell styles, no multiple sheets, not a general-purpose
  spreadsheet library.

## License

This project is licensed under the **Apache License, Version 2.0**, see [`../LICENSE.txt`](../LICENSE.txt).

-----

Part of the [`iban-commons`](../README.md) project.
