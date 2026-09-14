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
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
import de.speedbanking.bic.Bic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loader for the Czech National Bank (Ceska narodni banka, CNB) bank code (kod banky) directory.
 * <p>
 * <strong>Assumed raw format (TODO: re-verify the exact current column order and encoding against
 * the live CNB download before release, only the URL and general CSV shape below were actually
 * researched):</strong> UTF-8, semicolon-separated CSV with a header row that must be skipped,
 * columns in this order:
 * <pre>
 * Kod platebniho styku;Poskytovatel platebnich sluzeb;BIC kod (SWIFT);System CERTIS
 * </pre>
 * i.e. bank code, institution name, BIC, and a payment-system participation flag that is not part
 * of this module's normalized {@link BankData} format and is therefore ignored. {@code bankCode}
 * is the 4-digit Czech bank code; the BIC column may be empty for some institutions. This source
 * does not publish a postal code or city column, so {@link BankData#getPostalCode()} and
 * {@link BankData#getCity()} are always {@code null}, the same convention used by
 * {@link ChBankDataLoader} for the Swiss source.
 *
 * @since 1.8.11
 */
public final class CzBankDataLoader implements CountryBankDataLoader {

    private static final String COUNTRY_CODE = "CZ";

    /**
     * Default remote source URI: the Czech National Bank's bank code list CSV download, no login
     * required.
     */
    public static final URI     DEFAULT_SOURCE_URI = URI.create("https://www.cnb.cz/cs/platebni-styk/.galleries/ucty_kody_bank/download/kody_bank_CR.csv");

    private static final Charset SOURCE_CHARSET  = UTF_8;
    private static final String  FIELD_SEPARATOR = ";";
    private static final int     HEADER_ROW_COUNT = 1;

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
        // the raw format does not distinguish a head-office from a branch record, so deduplication
        // by bank code keeps the first occurrence (deterministic, documented tie-break)
        Map<String, BankData> byBankCode = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(rawSource, SOURCE_CHARSET))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber <= HEADER_ROW_COUNT) {
                    continue; // skip the CSV header row
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                BankData record = parseLine(line, lineNumber, sourceVersion);
                byBankCode.putIfAbsent(record.getBankCode(), record);
            }
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read Czech CNB bank code raw source", ex);
        }

        return new ArrayList<>(byBankCode.values());
    }

    private BankData parseLine(String line, int lineNumber, String sourceVersion) throws BankDataParseException {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length < 3) {
            throw new BankDataParseException("Malformed Czech CNB bank code line " + lineNumber + ": expected at least 3 semicolon-separated fields, got " + fields.length);
        }

        String bankCode = fields[0].trim();
        String bankName = fields[1].trim();
        String bicRaw    = fields[2].trim();

        Bic bic = bicRaw.isEmpty() ? null : Bic.tryParse(bicRaw).orElse(null);

        return new BankData(COUNTRY_CODE, bankCode, bic, bankName, null, null, sourceVersion);
    }

}
