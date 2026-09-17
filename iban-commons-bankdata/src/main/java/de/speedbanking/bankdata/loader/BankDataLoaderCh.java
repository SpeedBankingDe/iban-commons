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
import java.util.ArrayList;
import java.util.List;

/**
 * Loader for the SIX Interbank Clearing "Bank Master" directory (Swiss IID / bank clearing
 * numbers, formerly known as BC-Nummer).
 * <p>
 * <strong>Verified raw format</strong> (checked against a live download of the v3 Bank Master
 * CSV and its official record description): semicolon-separated, one record per line, UTF-8, with
 * a header row of 21 named columns, in this order:
 * <pre>
 * IID/QR-IID;Valid on;Concatenation;New IID/QR-IID;SIC IID;Headquarters;IID type;
 * QR-IID allocation;Name of bank/institution;Street Name;Building Number;Post Code;Town Name;
 * Country;BIC;SIC participation;RTGS customer payments, CHF;IP customer payments, CHF;
 * euroSIC participation;LSV+/BDD, CHF;LSV+/BDD, EUR
 * </pre>
 * The header row itself carries one additional, 22nd token (a generation timestamp) not present in
 * data rows, so header validation only checks the first 21 expected column names by position and
 * ignores anything beyond that. Data rows contain exactly 21 fields.
 *
 * @since 1.8.11
 */
public final class BankDataLoaderCh extends AbstractCountryBankDataLoader<BankDataLoaderCh.Column> {

    /**
     * Default remote source URI: the SIX Bank Master v3 CSV, a stable, versioned REST endpoint
     * (no login required).
     */
    public static final URI     DEFAULT_SOURCE_URI = URI
        .create("https://api.six-group.com/api/epcd/bankmaster/v3/bankmaster_V3.csv");

    private static final char   FIELD_SEPARATOR    = ';';
    private static final String CONCATENATED       = "Y";
    private static final int    MIN_COLUMN_COUNT   = 21;

    enum Column implements ColumnDefinition {

        IID(0),
        CONCATENATION(2),
        NAME(8),
        POST_CODE(11),
        TOWN_NAME(12),
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

    public BankDataLoaderCh() {
        super(Column.class);
    }

    @Override
    public URI getRemoteSourceUri() {
        return DEFAULT_SOURCE_URI;
    }

    @Override
    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, getRemoteSourceCharset()), FIELD_SEPARATOR);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read Swiss Bank Master raw source", ex);
        }

        List<BankData> result = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            if (rowIndex == 0) {
                continue; // header row (carries an extra trailing timestamp token, ignored)
            }
            List<String> fields = rows.get(rowIndex);
            if (fields.size() == 1 && fields.get(0).trim().isEmpty()) {
                continue; // blank trailing line
            }
            if (fields.size() < MIN_COLUMN_COUNT) {
                throw new BankDataParseException("Malformed Swiss Bank Master row " + (rowIndex + 1)
                    + ": expected at least " + MIN_COLUMN_COUNT + " semicolon-separated fields, got " + fields.size());
            }
            // Concatenation = Y means this IID has been merged into another one and is no longer valid
            if (CONCATENATED.equalsIgnoreCase(getColumns().getOrEmpty(Column.CONCATENATION, fields))) {
                continue;
            }

            String iidRaw = getColumns().getOrEmpty(Column.IID, fields);
            if (iidRaw.isEmpty()) {
                continue;
            }
            String bankCode;
            try {
                bankCode = String.format("%05d", Integer.parseInt(iidRaw));
            } catch (NumberFormatException ex) {
                throw new BankDataParseException("Malformed Swiss Bank Master IID '" + iidRaw + "': expected a number", ex);
            }
            String bicRaw = getColumns().getOrEmpty(Column.BIC, fields);
            Bic bic = Bic.tryParse(bicRaw).orElse(null);
            String bankName = getColumns().getOrEmpty(Column.NAME, fields);
            String postalCode = getColumns().getOrNull(Column.POST_CODE, fields);
            String city = getColumns().getOrNull(Column.TOWN_NAME, fields);

            BankData bankData = new BankData(getCountryCode(), bankCode, bic, bankName, postalCode, city, sourceVersion);
            result.add(bankData);
        }
        return result;
    }

}
