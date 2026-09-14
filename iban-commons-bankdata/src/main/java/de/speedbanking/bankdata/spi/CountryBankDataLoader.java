/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.bankdata.spi;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.BankDataConfig;
import de.speedbanking.iban.Iban;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;

/**
 * Extension contract for a single country's bank directory ("BLZ file", "Bankleitzahlenverzeichnis",
 * BC-Nummer directory, etc.).
 * <p>
 * A loader is the only place with knowledge of a country's raw source format: it knows where to
 * download the directory from, in what character encoding it is published, and how to turn its
 * raw bytes into a {@link List} of normalized {@link BankData} records.
 * <p>
 * Implementations are registered in a fixed, internally wired table (see
 * {@code de.speedbanking.bankdata.BankDataRegistry}), this module does not use
 * {@link java.util.ServiceLoader}/{@code META-INF/services} discovery. Third-party pluggability
 * beyond the countries shipped with this module is a deliberately deferred design question, not
 * part of this iteration.
 *
 * @since 1.8.11
 */
public interface CountryBankDataLoader {

    /**
     * Returns the ISO 3166-1 alpha-2 country code this loader is responsible for.
     *
     * @return the country code, e.g. {@code "DE"}
     */
    String getCountryCode();

    /**
     * Returns the default remote location this loader downloads its raw source data from.
     * <p>
     * Implementations expose this as a {@code public static final URI DEFAULT_SOURCE_URI}
     * constant on the concrete class as well.
     *
     * @return the remote source URI
     */
    URI getRemoteSourceUri();

    /**
     * Returns the character encoding the remote source is published in.
     * <p>
     * The raw bytes downloaded by a {@code Downloader} are never decoded based on an HTTP
     * {@code Content-Type} header; only the loader decides how its own source is encoded.
     *
     * @return the source charset
     */
    Charset getRemoteSourceCharset();

    /**
     * Parses a raw source stream into normalized {@link BankData} records.
     * <p>
     * This is the only place in the module with knowledge of a country's raw column layout.
     * Implementations are expected to trim whitespace, validate/skip malformed BIC values (rather
     * than fail the whole parse), and deduplicate records sharing the same bank code (preferring
     * a main/head-office record over a branch record where the source distinguishes the two).
     *
     * @param rawSource     the raw source bytes, not yet decoded
     * @param sourceVersion an identifier for the vintage/version of this particular download,
     *                      propagated into every returned {@link BankData#getSourceVersion()}
     * @return the parsed, normalized bank records; never {@code null}
     * @throws BankDataParseException if the raw source cannot be parsed
     */
    List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException;

    /**
     * Extracts the country-specific bank code lookup key from an IBAN.
     * <p>
     * Default implementation returns {@link Iban#getBankCode()}. Deliberately decoupled from
     * {@code IbanPlusKey}: that class derives a bank+branch+NCD routing key for SWIFT IBAN Plus,
     * a different concept from the plain bank-code registry key used here.
     *
     * @param iban the IBAN to extract the bank code from
     * @return the bank code to look up in this country's registry
     */
    default String extractBankCode(Iban iban) {
        return iban.getBankCode();
    }

    /**
     * Returns the recommended staleness threshold after which cached data for this country
     * should be refreshed in the background.
     * <p>
     * Default implementation returns {@link BankDataConfig#getStaleThreshold()}; individual
     * loaders may override this to reflect how frequently their upstream source actually changes.
     *
     * @return the recommended refresh threshold
     */
    default Duration getRecommendedRefreshThreshold() {
        return BankDataConfig.get().getStaleThreshold();
    }

}
