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

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.io.BankDataFormat;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
import de.speedbanking.bic.Bic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
public final class ChBankDataLoader implements CountryBankDataLoader {

    private static final String COUNTRY_CODE = "CH";

    /**
     * Default remote source URI: the SIX Bank Master v3 CSV, a stable, versioned REST endpoint
     * (no login required).
     */
    public static final URI     DEFAULT_SOURCE_URI = URI.create("https://api.six-group.com/api/epcd/bankmaster/v3/bankmaster_V3.csv");

    private static final Charset SOURCE_CHARSET  = UTF_8;
    private static final char    FIELD_SEPARATOR = ';';
    private static final String  CONCATENATED    = "Y";

    private static final int COL_IID             = 0;
    private static final int COL_CONCATENATION   = 2;
    private static final int COL_NAME            = 8;
    private static final int COL_POST_CODE       = 11;
    private static final int COL_TOWN_NAME       = 12;
    private static final int COL_BIC             = 14;
    private static final int MIN_COLUMN_COUNT    = 21;

    @Override
    public String getCountryCode() {
        return COUNTRY_CODE;
    }

    @Override
    public URI getRemoteSourceUri() {
        return DEFAULT_SOURCE_URI;
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return SOURCE_CHARSET;
    }

    @Override
    @SuppressWarnings("PMD.InefficientEmptyStringCheck") // trim().isEmpty() is a one-off blank-line check, not a hot path
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, SOURCE_CHARSET), FIELD_SEPARATOR);
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
            toBankData(fields, sourceVersion).ifPresent(result::add);
        }
        return result;
    }

    private Optional<BankData> toBankData(List<String> fields, String sourceVersion) throws BankDataParseException {
        String iidRaw = field(fields, COL_IID);
        String bicRaw = field(fields, COL_BIC);
        if (iidRaw.isEmpty() || bicRaw.isEmpty()) {
            return Optional.empty();
        }
        // Concatenation = Y means this IID has been merged into another one and is no longer valid
        if (CONCATENATED.equalsIgnoreCase(field(fields, COL_CONCATENATION))) {
            return Optional.empty();
        }

        String bankCode;
        try {
            bankCode = String.format("%05d", Integer.parseInt(iidRaw));
        } catch (NumberFormatException ex) {
            throw new BankDataParseException("Malformed Swiss Bank Master IID '" + iidRaw + "': expected a number", ex);
        }

        Bic bic = Bic.tryParse(bicRaw).orElse(null);
        String bankName   = field(fields, COL_NAME);
        String postalCode = emptyToNull(field(fields, COL_POST_CODE));
        String city        = emptyToNull(field(fields, COL_TOWN_NAME));

        return Optional.of(new BankData(COUNTRY_CODE, bankCode, bic, bankName, postalCode, city, sourceVersion));
    }

    private static String field(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index).trim() : "";
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

}
