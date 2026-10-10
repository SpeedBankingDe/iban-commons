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
package de.speedbanking.bankdata.loader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import de.speedbanking.bankdata.BankDataConfig;
import de.speedbanking.bankdata.io.ColumnDefinition;
import de.speedbanking.bankdata.io.Columns;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
import de.speedbanking.util.Country;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base implementation for country-specific bank data loaders using column definitions.
 *
 * @param <C> enum type representing column definitions
 * @since 1.8.12
 */
public abstract class AbstractCountryBankDataLoader<C extends Enum<C> & ColumnDefinition> implements CountryBankDataLoader {

    private final Columns<C> columns;
    private final String     countryCode;
    private final URI        defaultSourceUri;

    /**
     * Constructs a loader instance with the specified column enum class.
     * <p>
     * Derives the ISO country code from the concrete subclass name prefix (e.g. {@code "DE"} from {@code DeBankDataLoader}).
     *
     * @param columnClass      enum class defining column mapping, must not be null
     * @param defaultSourceUri the compiled-in default remote source URI, used unless
     *                         {@link BankDataConfig} carries a runtime override for this country;
     *                         must not be null
     */
    protected AbstractCountryBankDataLoader(Class<C> columnClass, URI defaultSourceUri) {
        requireNonNull(columnClass, "columnClass must not be null");
        this.columns = Columns.of(columnClass);
        this.countryCode = resolveCountryCode(getClass());
        this.defaultSourceUri = requireNonNull(defaultSourceUri, "defaultSourceUri must not be null");
    }

    private static String resolveCountryCode(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (simpleName.length() < 2) {
            throw new IllegalStateException("Class name '" + simpleName + "' is too short to extract ISO country code");
        }
        Country country = Country.fromCode(simpleName.substring(simpleName.length() - 2, simpleName.length()).toUpperCase(Locale.ROOT));
        if (country == null) {
            throw new IllegalStateException("Cannot derive country from class name '" + simpleName + "'");
        }
        return country.getCode();
    }

    /**
     * Gets the column wrapper instance.
     *
     * @return columns instance
     */
    protected final Columns<C> getColumns() {
        return columns;
    }

    @Override
    public final String getCountryCode() {
        return countryCode;
    }

    /**
     * Returns the effective remote source URI: the {@link BankDataConfig} runtime override for
     * this country if one is configured, otherwise the compiled-in default passed to the
     * constructor.
     */
    @Override
    public final URI getRemoteSourceUri() {
        return BankDataConfig.get().getSourceUriOverride(countryCode).orElse(defaultSourceUri);
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return UTF_8;
    }

    @Override
    public Duration getRecommendedRefreshThreshold() {
        return BankDataConfig.get().getStaleThreshold();
    }

    /**
     * Shared building block for a loader whose {@link #resolveActualSourceUri(URI, byte[])}
     * resolves a rotating data-file URL by scraping a stable link out of a downloaded landing
     * page's HTML: decodes {@code downloadedBytes} as UTF-8, finds the first match of
     * {@code linkPattern}'s first capturing group (expected to be an {@code href} attribute value),
     * and resolves it against {@code downloadedFrom}.
     * <p>
     * Used by {@code BankDataLoaderDe}, {@code BankDataLoaderNl}, and {@code
     * AbstractBankDataLoaderEcb}, each of which downloads a source whose actual data-file URL
     * shifts (a blob hash, an upload-month path segment, a release-date-stamped filename) while a
     * landing page linking to it stays put.
     *
     * @param downloadedFrom the landing page's own URL, used to resolve a relative {@code href}
     * @param downloadedBytes the landing page's raw downloaded bytes (UTF-8 HTML)
     * @param linkPattern     a pattern whose first capturing group is the {@code href} value to
     *                        extract, e.g. {@code href="([^"]*some-stable-filename\.csv)"}
     * @return the resolved absolute URI, or empty if {@code linkPattern} found no match
     */
    protected static Optional<URI> resolveLinkFromLandingPage(URI downloadedFrom, byte[] downloadedBytes, Pattern linkPattern) {
        String html = new String(downloadedBytes, UTF_8);
        Matcher matcher = linkPattern.matcher(html);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(downloadedFrom.resolve(matcher.group(1)));
    }

    /**
     * Returns {@code true} if {@code fields} is the sole, harmless shape a blank trailing line in a
     * delimited raw source parses into: a single empty (whitespace-only) field.
     * <p>
     * Shared by a loader whose {@link #parse(java.io.InputStream, String)} reads pre-split rows
     * (via {@code BankDataFormat.parseQuotedFields}) and needs to distinguish that from a
     * genuinely malformed, too-short data row.
     *
     * @param fields one already-parsed row's fields
     * @return {@code true} if this row is a blank trailing line
     */
    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    protected static boolean isBlankTrailingLine(List<String> fields) {
        return fields.size() == 1 && fields.get(0).trim().isEmpty();
    }

    /**
     * Throws a {@link BankDataParseException} if {@code fields} has fewer fields than {@link
     * #getColumns()}'s {@link Columns#getMinColumnCount()} requires, in the wording shared by
     * {@code BankDataLoaderDe} and {@code AbstractBankDataLoaderEcb}.
     *
     * @param fields               one already-parsed row's fields
     * @param rowNumber            this row's 1-based line/row number, for the exception message
     * @param sourceLabel          a short label identifying the raw source, e.g. {@code "German BLZ"}
     * @param separatorDescription the field separator's name, e.g. {@code "semicolon"} or {@code "tab"}
     * @throws BankDataParseException if {@code fields} is too short
     */
    protected final void requireMinColumns(List<String> fields, int rowNumber, String sourceLabel, String separatorDescription)
        throws BankDataParseException {
        if (fields.size() < columns.getMinColumnCount()) {
            throw new BankDataParseException("Malformed " + sourceLabel + " row " + rowNumber
                + ": expected at least " + columns.getMinColumnCount() + " " + separatorDescription + "-separated fields, got " + fields.size());
        }
    }

    @Override
    public final String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add("countryCode=" + countryCode)
            .add("remoteSourceUri=" + getRemoteSourceUri())
            .add("remoteSourceCharset=" + getRemoteSourceCharset())
            .add("columns=" + columns)
            .toString();
    }

}
