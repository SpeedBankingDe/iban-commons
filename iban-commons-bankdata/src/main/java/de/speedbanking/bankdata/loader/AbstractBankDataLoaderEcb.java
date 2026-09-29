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

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.io.BankDataFormat;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bic.Bic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Shared base for {@link BankDataLoaderEs} and {@link BankDataLoaderFr}, both of which read the
 * European Central Bank's monthly "list of financial institutions" ({@code fi_mrr_csv}), rather
 * than a national source.
 * <p>
 * That file lists every euro-area credit institution the ECB tracks for minimum-reserve
 * requirements, tab-separated, one row per institution, with a stable 14-column layout:
 * <pre>
 * RIAD_CODE  BIC  COUNTRY_OF_REGISTRATION  NAME  BOX  ADDRESS  POSTAL  CITY  CATEGORY
 * HEAD_COUNTRY_OF_REGISTRATION  HEAD_NAME  HEAD_RIAD_CODE  RESERVE  EXEMPT
 * </pre>
 * {@code RIAD_CODE} is the ECB's own registration identifier, a 2-letter country prefix followed
 * by digits, and for most countries in the file bears no relation at all to that country's actual
 * IBAN bank code (verified against a live download: for Germany, {@code DE00001} is Deutsche
 * Bank's RIAD_CODE, but its real BLZ is {@code 50070010}). For Spain and France specifically,
 * however, the national central bank appears to have reused its existing bank-code register as
 * the RIAD numbering, so the digits after the country prefix are verified to equal the real
 * national bank code (checked against known codes: {@code ES0049} is Banco Santander's actual
 * {@code código de entidad} {@code 0049}; {@code FR30004} is BNP Paribas's actual {@code code
 * banque} {@code 30004}). This is why this source is only used for {@code ES} and {@code FR} and
 * not offered as a generic fallback for every country the file happens to list.
 * <p>
 * The file is published as UTF-16 (with a byte-order mark) despite the {@code .csv} extension -
 * checked against a live download - so {@link #getRemoteSourceCharset()} returns generic
 * {@link StandardCharsets#UTF_16}, which detects the BOM's byte order rather than assuming one.
 * A handful of fields contain a literal double quote, so this uses
 * {@link BankDataFormat#parseQuotedFields(java.io.Reader, char)} instead of a naive split.
 * {@code RIAD_CODE} was verified unique per country in a live download, so no dedup by bank code
 * is needed beyond the defensive {@code putIfAbsent} below.
 *
 * @see <a href="https://www.ecb.europa.eu/stats/financial_corporations/list_of_financial_institutions/html/monthly_list-MID.en.html">ECB list of financial institutions</a>
 * @since 1.8.12
 */
abstract class AbstractBankDataLoaderEcb extends AbstractCountryBankDataLoader<AbstractBankDataLoaderEcb.Column> {

    /** The filename is stable ({@code fi_mrr_csv_YYMMDD.csv}), even though the release month isn't. */
    private static final Pattern CSV_LINK_PATTERN = Pattern.compile("href=\"([^\"]*fi_mrr_csv_\\d{6}\\.csv)\"");

    enum Column implements ColumnDefinition {

        RIAD_CODE(0),
        BIC(1),
        COUNTRY_OF_REGISTRATION(2),
        NAME(3),
        POSTAL(6),
        CITY(7);

        private final int index;

        Column(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

    }

    protected AbstractBankDataLoaderEcb(URI defaultSourceUri) {
        super(Column.class, defaultSourceUri);
    }

    @Override
    public Optional<URI> resolveActualSourceUri(URI downloadedFrom, byte[] downloadedBytes) {
        return resolveLinkFromLandingPage(downloadedFrom, downloadedBytes, CSV_LINK_PATTERN);
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return StandardCharsets.UTF_16;
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, getRemoteSourceCharset()), '\t');
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read ECB financial institutions raw source", ex);
        }

        Map<String, BankData> byBankCode = new LinkedHashMap<>();
        // rowIndex starts at 1: row 0 is always the header, column names already verified against the live source
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            BankData entry = tryParseRow(rows.get(rowIndex), rowIndex, sourceVersion);
            if (entry != null) {
                byBankCode.putIfAbsent(entry.getKey(), entry); // defensive; RIAD_CODE verified unique per country
            }
        }
        return new ArrayList<>(byBankCode.values());
    }

    /**
     * Parses a single data row, or returns {@code null} if the row should be skipped: a blank
     * trailing line, a row for a different country (this source lists every euro-area country), or
     * one whose RIAD_CODE is malformed/not prefixed with its own country code.
     */
    private BankData tryParseRow(List<String> fields, int rowIndex, String sourceVersion) throws BankDataParseException {
        if (isBlankTrailingLine(fields)) {
            return null;
        }
        requireMinColumns(fields, rowIndex + 1, "ECB financial institutions", "tab");

        String country = getColumns().getOrEmpty(Column.COUNTRY_OF_REGISTRATION, fields);
        if (!getCountryCode().equals(country)) {
            return null;
        }

        String riadCode = getColumns().getOrEmpty(Column.RIAD_CODE, fields);
        if (!riadCode.startsWith(country) || riadCode.length() <= country.length()) {
            // malformed/blank RIAD_CODE, or one not actually prefixed with this row's own
            // COUNTRY_OF_REGISTRATION (e.g. a relocated/merged institution keeping a legacy
            // RIAD_CODE from another country) - do not silently derive a wrong bank code from it
            return null;
        }
        String bankCode = riadCode.substring(country.length());

        String bicRaw = getColumns().getOrEmpty(Column.BIC, fields);
        Bic bic = Bic.tryParse(bicRaw).orElse(null);
        String bankName = getColumns().getOrEmpty(Column.NAME, fields);
        String postalCode = getColumns().getOrNull(Column.POSTAL, fields);
        String city = getColumns().getOrNull(Column.CITY, fields);

        return new BankData(getCountryCode(), bankCode, bic, bankName, postalCode, city, sourceVersion);
    }

}
