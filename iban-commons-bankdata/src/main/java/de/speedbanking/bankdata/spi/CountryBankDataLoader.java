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
 * <p>
 * Implementations of this interface must be thread-safe and immutable.
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
     * @return the remote source URI; never {@code null}
     */
    URI getRemoteSourceUri();

    /**
     * Returns the character encoding the remote source is published in.
     * <p>
     * The raw bytes downloaded by a {@code Downloader} are never decoded based on an HTTP
     * {@code Content-Type} header; only the loader decides how its own source is encoded.
     *
     * @return the source charset; never {@code null}
     */
    Charset getRemoteSourceCharset();

    /**
     * Parses a raw source stream into normalized {@link BankData} records.
     * <p>
     * This is the only place in the module with knowledge of a country's raw column layout.
     * Implementations are expected to trim whitespace, validate/skip malformed BIC values (rather
     * than fail the whole parse), and deduplicate records sharing the same {@link BankData#getKey()
     * lookup key} (preferring a main/head-office record over a branch record where the source
     * distinguishes the two). A country whose BBAN carries a branch code that is actually needed to
     * identify a single institution (i.e. the bank code alone is ambiguous) must populate
     * {@link BankData#getBranchCode()} so that records are keyed at branch granularity instead of
     * being conflated under one bank code.
     *
     * @param rawSource     the raw source bytes, not yet decoded; must not be {@code null}
     * @param sourceVersion an identifier for the vintage/version of this particular download,
     *                      propagated into every returned {@link BankData#getSourceVersion()}
     * @return the parsed, normalized bank records; never {@code null}
     * @throws BankDataParseException if the raw source cannot be parsed
     * @throws NullPointerException if {@code rawSource} is {@code null}
     */
    List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException;

    /**
     * Returns the recommended staleness threshold after which cached data for this country
     * should be refreshed in the background.
     *
     * @return the recommended refresh threshold duration; never {@code null}
     */
    Duration getRecommendedRefreshThreshold();

}
