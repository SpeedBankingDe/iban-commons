# IBAN Commons Bank Data

> Bank master data (BIC, bank name, address) lookup by IBAN or bank code, for Java 8+

<div align="center">
  <a href="https://central.sonatype.com/artifact/de.speedbanking/iban-commons-bankdata"><img src="https://img.shields.io/maven-central/v/de.speedbanking/iban-commons-bankdata?label=Maven%20Central&style=flat-square" alt="Maven Central Version"></a>
  <a href="https://javadoc.io/doc/de.speedbanking/iban-commons-bankdata"><img src="https://javadoc.io/badge2/de.speedbanking/iban-commons-bankdata/javadoc.svg?style=flat-square" alt="Javadoc"></a>
</div>

`iban-commons-bankdata` resolves the BIC and bank name of the institution behind an IBAN or a raw
country-specific bank code (German BLZ, Austrian Bankleitzahl, Swiss BC-Nummer, Czech kod banky,
Belgian bank code, Polish numer rozliczeniowy, Dutch 4-letter bank identifier, Spanish código de
entidad, French code banque), self-hosted and open-source.

Country coverage: Germany (DE), Austria (AT), Switzerland (CH), Czech Republic (CZ), Belgium (BE), Poland (PL), Netherlands (NL), Spain (ES), France (FR). The architecture is built to be extensible to any ISO country code.

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

Optional<BankData> bank = BankDataLookup.byIban(Iban.of("DE89370400440532013000"));

bank.ifPresent(b -> {
    System.out.println(b.getBankName());  // "Commerzbank AG" (once real data is loaded)
    System.out.println(b.getBic());       // e.g. "COBADEFFXXX"
    System.out.println(b.getCity());      // e.g. "Frankfurt am Main"
});

// or look up directly by country-specific bank code, without an IBAN:
Optional<BankData> byBankCode = BankDataLookup.byBankCode("DE", "37040044");

// or the other way round: bank name/code by BIC
Optional<BankData> byBic = BankDataLookup.byBic("COBADEFFXXX");
```

`BankDataLookup` never throws for "not found", "country not supported", or "network unreachable" - all three simply produce an `Optional.empty()`.

A small runnable sample (`BankDataLookupSample`) generates a few random, valid IBANs and prints each institution's resolved BIC and bank name:
```bash
cd iban-commons-bankdata
mvn test-compile exec:java@bank-data-lookup-sample
```

## How data is loaded

1. **Offline fallback ("Urladung").** Most supported countries ship a bundled classpath snapshot
   (`/bankdata/{DE,AT,CZ,BE,PL,NL,ES,FR}.csv`), a real download from the live source at the time of
   release, so lookups work immediately, with zero network access, right after adding the
   dependency. `CH` is the exception - see [Data licensing](#data-licensing) - its loader works the
   same way otherwise, just without a bundled offline snapshot: the first lookup requires network
   access.
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

Must be called before the first `BankDataLookup`/`BankDataConfig.get()` invocation. Any `Builder`
setting left unset falls back to the external config file below, then to the hardcoded default
shown above.

Network access (downloads and background refreshes) can be disabled entirely, e.g. for environments with no outbound network access at all, via `Builder.disableNetwork(true)` or the `disableNetwork` config file entry below. When disabled, `triggerAsyncRefreshIfStale()` is a complete no-op and lookups are served exclusively from the local cache and bundled fallback data.

### External config file

A single optional properties file can override the cache directory, network disabling, timeouts, and any loader's source URL without recompiling or waiting for a new library release, e.g. to route around a source that has permanently moved. Point the `de.speedbanking.bankdata.configFile` system property, or the `IBAN_COMMONS_BANKDATA_CONFIG_FILE` environment variable, at its path. The
file only needs to contain the keys you actually want to change; anything it omits keeps using its hardcoded default:

```properties
cacheDir       = /var/cache/myapp/bankdata
disableNetwork = false
staleThreshold = P30D
connectTimeout = PT3S
readTimeout    = PT15S
loader.DE.url  = https://mirror.example.com/blz-aktuell-csv-data.csv
```

The bundled classpath resource `/bankdata/_bankdata.properties` is the single source of truth for every hardcoded default shown above except `cacheDir` (which has none, see [How data is loaded](#how-data-is-loaded)), and uses this exact same key shape, so it doubles as a template:
copy it, then edit only the keys that need fixing. A `Builder` value explicitly set in code always wins over the config file, which in turn wins over the bundled default. An unreadable or unparsable config file, or an invalid individual entry, is ignored with a logged warning rather than failing startup; the affected setting simply falls back to its next-lower-priority value.

## Logging

This module logs via plain `java.util.logging` (no new dependency), so it never imposes any logging configuration on the host application.

## Extending to another country

Implement `de.speedbanking.bankdata.spi.CountryBankDataLoader` for the new country's raw source format. This module wires loaders through an internal, fixed registry (`BankDataRegistry`) rather than `ServiceLoader`/`META-INF/services` discovery, consistent with `IbanRegistry`/
`CountryValidators` elsewhere in this project. Third-party pluggability beyond the loaders shipped with this module (i.e. registering your own `CountryBankDataLoader` without forking this module) is not part of this release.

## Regenerating bundled data

The bundled "Urladung" CSV files under `src/main/resources/bankdata` can be regenerated from each country's live upstream source with a maintainer-only tool, `BundledDataRefreshTool` (`src/test/java/de/speedbanking/bankdata/tool`, test-scope only, not part of the published jar).
Run it with the module directory (not the repository root) as the actual working directory, since the tool writes to a path relative to the current directory:

```shell
cd iban-commons-bankdata
mvn test-compile exec:java@refresh-bundled-bankdata
```

Running it via `mvn -pl iban-commons-bankdata ...` from the repository root does **not** work: the `exec-maven-plugin` `java` goal runs in-process and inherits whatever directory `mvn` itself was started from, not the module's directory. The tool detects this (the expected `src/main/resources/bankdata` directory would not be found relative to the wrong working directory)
and aborts immediately without writing anything, rather than silently creating a stray directory tree in the wrong place.

It downloads and re-parses each registered country's source and overwrites `{CC}.csv` on success;
a failure for one country is reported and leaves that country's existing file untouched, the tool then continues with the next country. This is a best-effort maintainer convenience, typically run once before cutting a release, not a CI gate, since it depends on outbound network access and on
each loader's upstream source URL/format still being accurate. Review the resulting diff (in particular record counts and a few spot-checked entries) before committing.

A country whose loader's `CountryBankDataLoader.allowsBundledSnapshot()` returns `false`
(currently only `CH`, see [Data licensing](#data-licensing)) is always skipped and never written to
disk, whether it was selected implicitly (no arguments) or named explicitly on the command line -
this is enforced in code, not just documented, so there is no way to accidentally regenerate
`CH.csv` by running the tool without arguments.

## ⚠️ Data accuracy disclaimer

**This module is provided on a best-effort basis and comes with no guarantee of correctness, completeness, or timeliness of the bank data it returns.** `BankDataLookup` results are derived from third-party national bank registries and, for `ES`/`FR`, the European Central Bank (Deutsche Bundesbank, OeNB, SIX Interbank Clearing, Ceska narodni banka, Nationale Bank van Belgie / Banque Nationale de Belgique, Narodowy Bank Polski, Betaalvereniging Nederland, European Central Bank) that publish and amend their data on their own schedule, outside this project's control. A result may be outdated between refreshes (background refresh only runs after the configured staleness threshold is reached, see [How data is loaded](#how-data-is-loaded)), and a failed refresh silently continues serving the last known good, potentially stale, data rather than failing the lookup. Do not treat a `BankData` record as authoritative for payment routing, compliance, or other decisions where
incorrect or outdated bank data could cause harm; always validate against an authoritative source for such use cases, and use `BankData.getSourceVersion()` to check the vintage of a given result.

In addition, at the time of this release the download URLs and raw column layouts in `BankDataLoaderDe`, `BankDataLoaderAt`, `BankDataLoaderCh`, `BankDataLoaderCz`, `BankDataLoaderBe`, `BankDataLoaderPl`, `BankDataLoaderNl`, `BankDataLoaderEs`, and `BankDataLoaderFr` have all been **verified against a live download** of each institution's current, official source. The bundled `/bankdata/{DE,AT,CZ,BE,PL,NL,ES,FR}.csv` fallback files are, for every one of those eight countries, a real snapshot downloaded from the live source with the maintainer tool below, not hand-curated examples, so the module is fully usable offline out of the box for those countries; this also means their record counts and file sizes are non-trivial (the largest, `DE.csv`, is a few hundred KB with several thousand records) and change on every refresh as the upstream sources are amended. `CH` is verified and fully functional the same way, just not bundled - see [Data licensing](#data-licensing).

`ES` and `FR` are both sourced from the same ECB file (see `AbstractBankDataLoaderEcb`'s class Javadoc), which lists every euro-area credit institution the ECB tracks, keyed by an internal `RIAD_CODE` registration identifier. For most countries in that file `RIAD_CODE` bears no relation to the country's actual IBAN bank code, but for Spain and France specifically it was verified, against known real-world bank codes, to equal the actual national bank code (`código de entidad` / `code banque`) - this is why only these two countries use this source, not every country the file happens to list.

## ⚠️ Data licensing

Every source this module downloads from publishes its own terms of use, separate from and independent of the Apache-2.0 license this module itself is published under; downloading, caching, and returning `BankData` records derived from that source at runtime is covered by "personal or business use" language common to most of them, but **bundling a snapshot of the raw data inside this module's own jar and redistributing it via Maven Central to every consumer, including for commercial use, is a materially different act** and was evaluated per source before this release:

| Country | Source                                                   | Bundled offline snapshot | Why                                                                                                                                                                                                                                                                                                                                                    |
|---------|----------------------------------------------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DE      | Deutsche Bundesbank                                      | ✅ Yes                    | Redistribution/reproduction for personal or business use is explicitly permitted, conditional on citing "Quelle: Deutsche Bundesbank" as the source (see [Notable implementation choices](#notable-implementation-choices) for where that attribution lives)                                                                                           |
| AT      | OeNB                                                     | ✅ Yes                    | Published under CC BY 4.0, commercial and non-commercial reuse both explicitly permitted, attribution required                                                                                                                                                                                                                                         |
| CZ      | Ceska narodni banka                                      | ✅ Yes                    | Published as open data under Czech government regulation 425/2016 Sb.; the exact license variant could not be fully pinned down, treated as low-risk given the explicit open-data framing                                                                                                                                                              |
| BE      | Nationale Bank van Belgie / Banque Nationale de Belgique | ✅ Yes                    | No explicit redistribution restriction found for this dataset                                                                                                                                                                                                                                                                                          |
| PL      | Narodowy Bank Polski                                     | ✅ Yes                    | No explicit redistribution restriction found for this dataset; NBP is the statutory registrar for these identifiers under Polish law, published for exactly this kind of consumption                                                                                                                                                                   |
| CH      | SIX Interbank Clearing                                   | ❌ No                     | Terms of use explicitly prohibit reproducing or reusing website content "in any way" or "for commercial purposes"; the wording does not carve out an exception for the bank master data file. Permission has been requested (see below); until granted, this module downloads live and caches locally, but does not ship a bundled snapshot in the jar |
| NL      | Betaalvereniging Nederland                               | ✅ Yes                    | Written permission granted 2026-09-22 by the Dutch Payments Association's press office: free of charge, without a formal licence agreement, conditional on citing "Betaalvereniging Nederland" as the source and disclaiming that a Dutch BIC or bank code may change, be revoked, or be added at any time (see [Notable implementation choices](#notable-implementation-choices) for where that attribution lives)                |
| ES      | European Central Bank                                    | ✅ Yes                    | ECB's website disclaimer permits reproduction/redistribution, including where the information is incorporated into a commercially sold product, conditional on citing the ECB as the source and, for a sold product, disclosing that the information is available free of charge on the ECB's website (see [Notable implementation choices](#notable-implementation-choices) for where that attribution lives)                        |
| FR      | European Central Bank                                    | ✅ Yes                    | Same ECB source and terms as `ES` above                                                                                                                                                                                                                                                                                                                |

For `CH`, `BankDataLookup` still works exactly the same way, it just cannot answer a lookup with zero network access on first use the way the other six countries can (see [How data is loaded](#how-data-is-loaded)); once a live download has succeeded once, the local cache file takes over and offline lookups work from then on, same as any other country.

**This assessment is the result of reading each source's publicly posted terms, not legal advice.**
If your use of this module is licensing-sensitive, verify the current terms yourself. Written permission has been requested from SIX Interbank Clearing to bundle their data the same way the other six sources already are; this section will be updated (and the snapshot added) if and when that permission is granted. If you represent SIX and can help move that along, please open an issue.

## Notable implementation choices

- **Attribution: Quelle: Deutsche Bundesbank.** The bundled `DE.csv` offline snapshot and any   `BankData` this module resolves for Germany are derived from the Deutsche Bundesbank's
  Bankleitzahlen directory; its terms of use require this citation whenever the data is redistributed or reproduced, which bundling it into this module's jar does.
- **Attribution: Source: Betaalvereniging Nederland.** The bundled `NL.csv` offline snapshot and any `BankData` this module resolves for the Netherlands are derived from the Dutch Payments
  Association's BIC directory. Per written permission granted 2026-09-22 (press office, by e-mail), free use requires citing "Betaalvereniging Nederland" as the source and a disclaimer
  that a Dutch BIC or bank code may change, be revoked, or be added at any time - both satisfied by this section together with the accuracy disclaimer above.
- **Attribution: Source: European Central Bank.** The bundled `ES.csv`/`FR.csv` offline snapshots and any `BankData` this module resolves for Spain and France are derived from the ECB's
  monthly list of financial institutions. Its terms of use require citing the ECB as the source when the data is redistributed or reproduced, which bundling it into this module's jar does.
  No other source used by this module currently requires a specific citation string (see [Data licensing](#data-licensing) for the per-source assessment).
- **`BankDataLoaderEs` and `BankDataLoaderFr` share a base class, `AbstractBankDataLoaderEcb`**,
  since both read the exact same ECB source file and differ only in which country they filter rows
  down to (a byproduct of `AbstractCountryBankDataLoader` deriving the country code from the
  concrete class name). The source file is published as UTF-16 despite its `.csv` extension - verified against a live download - and a landing page has to be scraped for the current
  month's file URL the same way `BankDataLoaderDe` and `BankDataLoaderNl` already do for their own sources.
- **`src/main/resources` / `src/test/resources`** are used in this module to ship the bundled
  offline fallback data and raw-format test fixtures, respectively. This is new to the
  `iban-commons` project (no other module previously used non-Java resources) and is a deliberate,
  scoped exception for this module's "Urladung" and fixture needs.
- **HTTP transport** uses `java.net.HttpURLConnection` exclusively, no new runtime dependency,
  consistent with the zero-dependency philosophy of the rest of the project, including manual
  single-hop redirect following across protocol changes (`http` → `https`), since
  `HttpURLConnection`'s built-in redirect handling does not cover that case.
- **Requests send a real browser `User-Agent`** (and matching `Accept`/`Sec-Fetch-*` headers)
  instead of Java's default. Several sources block or degrade a request that identifies itself as
  a bot or a bare HTTP client, even though the exact same file is served to an ordinary browser
  visitor without restriction and is published specifically for automated, machine-readable
  consumption. See `HttpDownloader`'s class Javadoc for the full rationale and the concrete limits
  this stays within (one request at a time, no crawling, rate-limited by the staleness threshold).
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
