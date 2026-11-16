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
import de.speedbanking.bankdata.io.ColumnDefinition;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bic.Bic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared base for {@link BankDataLoaderCh} and {@link BankDataLoaderLi}, both of which read the
 * SIX Interbank Clearing "Bank Master" directory (IID / bank clearing numbers, formerly known as
 * BC-Nummer). Swiss and Liechtenstein banks share one IID number space and one file, told apart
 * by its {@code Country} column, see {@link #includesCountry(String)}.
 * <p>
 * Checked against a live download of the v3 Bank Master CSV plus its official record description:
 * semicolon-separated, one record per line, UTF-8, header row with 21 named columns, in order:
 * <pre>
 * IID/QR-IID;Valid on;Concatenation;New IID/QR-IID;SIC IID;Headquarters;IID type;
 * QR-IID allocation;Name of bank/institution;Street Name;Building Number;Post Code;Town Name;
 * Country;BIC;SIC participation;RTGS customer payments, CHF;IP customer payments, CHF;
 * euroSIC participation;LSV+/BDD, CHF;LSV+/BDD, EUR
 * </pre>
 * The header carries a 22nd token (a generation timestamp) that data rows don't have, so header
 * validation just checks the first 21 column names by position and ignores the rest. Data rows are
 * exactly 21 fields.
 * <p>
 * Haven't seen a duplicate IID in the live source, but we still dedup (first occurrence wins) in
 * case that changes.
 * <p>
 * Unlike the other loaders in this package, this module does <strong>not</strong> ship a bundled
 * {@code /bankdata/CH.csv} or {@code /bankdata/LI.csv} offline fallback: SIX Interbank Clearing's
 * terms of use prohibit reproducing or reusing website content, including for commercial purposes,
 * without an exception carved out for this dataset, enforced via {@link #allowsBundledSnapshot()}
 * returning {@code false}. The loaders themselves work normally either way (live download plus
 * local cache), they just cannot answer a lookup with zero network access before the first
 * successful download. See the module README's "Data licensing" section for the full assessment.
 *
 * @since 1.8.15
 */
abstract class AbstractBankDataLoaderSix extends AbstractCountryBankDataLoader<AbstractBankDataLoaderSix.Column> {

    private static final char   FIELD_SEPARATOR  = ';';
    private static final String CONCATENATED     = "Y";
    private static final int    MIN_COLUMN_COUNT = 21;

    enum Column implements ColumnDefinition {

        IID(0),
        CONCATENATION(2),
        NAME(8),
        POST_CODE(11),
        TOWN_NAME(12),
        COUNTRY(13),
        BIC(14);

        private final int index;

        Column(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

    }

    /**
     * Creates a loader for the given default remote source.
     *
     * @param defaultSourceUri the SIX Bank Master CSV to download
     */
    protected AbstractBankDataLoaderSix(URI defaultSourceUri) {
        super(Column.class, defaultSourceUri);
    }

    @Override
    public boolean allowsBundledSnapshot() {
        return false; // SIX Interbank Clearing's terms of use prohibit redistribution; see class Javadoc
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, getRemoteSourceCharset()), FIELD_SEPARATOR);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read SIX Bank Master raw source", ex);
        }

        Map<String, BankData> byBankCode = new LinkedHashMap<>();
        // rowIndex starts at 1: row 0 is always the header (carries an extra trailing timestamp token, ignored)
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            BankData entry = tryParseRow(rows.get(rowIndex), rowIndex, sourceVersion);
            if (entry != null) {
                byBankCode.putIfAbsent(entry.getBankCode(), entry);
            }
        }
        return new ArrayList<>(byBankCode.values());
    }

    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    private BankData tryParseRow(List<String> fields, int rowIndex, String sourceVersion) throws BankDataParseException {
        if (fields.size() == 1 && fields.get(0).trim().isEmpty()) {
            return null; // blank trailing line
        }
        if (fields.size() < MIN_COLUMN_COUNT) {
            throw new BankDataParseException(String.format("Malformed SIX Bank Master row %s: expected at least %s semicolon-separated fields, got %s",
                rowIndex + 1, MIN_COLUMN_COUNT, fields.size()));
        }
        // Concatenation = Y means this IID has been merged into another one and is no longer valid
        if (CONCATENATED.equalsIgnoreCase(getColumns().getOrEmpty(Column.CONCATENATION, fields))) {
            return null;
        }
        if (!includesCountry(getColumns().getOrEmpty(Column.COUNTRY, fields))) {
            return null;
        }

        String iidRaw = getColumns().getOrEmpty(Column.IID, fields);
        if (iidRaw.isEmpty()) {
            return null;
        }
        String bankCode;
        try {
            bankCode = String.format("%05d", Integer.parseInt(iidRaw));
        } catch (NumberFormatException ex) {
            throw new BankDataParseException(String.format("Malformed SIX Bank Master IID '%s': expected a number", iidRaw), ex);
        }
        String bicRaw = getColumns().getOrEmpty(Column.BIC, fields);
        Bic bic = Bic.tryParse(bicRaw).orElse(null);
        String bankName = getColumns().getOrEmpty(Column.NAME, fields);
        String postalCode = getColumns().getOrNull(Column.POST_CODE, fields);
        String city = getColumns().getOrNull(Column.TOWN_NAME, fields);

        return new BankData(getCountryCode(), bankCode, bic, bankName, postalCode, city, sourceVersion);
    }

    /**
     * Tells whether a row with the given {@code Country} column belongs to this loader.
     *
     * @param country the row's {@code Country} column, possibly empty
     * @return {@code true} to include the row
     */
    protected abstract boolean includesCountry(String country);

}
