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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loader for the Polish NBP (Narodowy Bank Polski) EWIB directory.
 * <p>
 * Evaluates the official NBP text/CSV export. Uses CP852 charset encoding as published
 * by NBP. The 8-digit settlement number ({@code numer rozliczeniowy}) is split into the
 * {@code bankCode} (first 3 digits) and {@code branchCode} (next 4 digits) components of the
 * Polish BBAN structure ({@code 3!n} bank code + {@code 4!n} branch code + {@code 1!n} national
 * code); the trailing 8th digit is that national code, an internal control digit that is not
 * part of the SWIFT IBAN Plus routing key (see {@code IbanPlusKey}) and is therefore not used
 * here. The institution number published alongside each record ({@code NrInstytucji}) is only an
 * administrative identifier and does not appear in the IBAN, so it is not used here either.
 * <p>
 * Polish settlement number prefixes have been reassigned across institutions over decades of bank
 * mergers, so the 3-digit bank code alone isn't always unique to one institution in this dataset.
 * The 7-digit bank+branch code is, so we dedup on the combined {@link BankData#getKey()} instead
 * of the bank code alone (first occurrence wins).
 *
 * @see <a href="https://ewib.nbp.pl/faces/pages/daneDoPobrania.xhtml">NBP EWIB Downloads</a>
 * @since 1.8.11
 */
public final class BankDataLoaderPl extends AbstractCountryBankDataLoader<BankDataLoaderPl.Column> {

    /** Default remote source URI for the Polish NBP bank registry (TXT export). */
    public static final URI     DEFAULT_SOURCE_URI = BankDataLoaderDefaults.sourceUri("PL");

    private static final char   FIELD_SEPARATOR    = '\t'; // NBP uses tab-separated fields in TXT

    private static final Charset CP852              = Charset.forName("CP852");

    enum Column implements ColumnDefinition {

        SETTLEMENT_CODE(4),
        BANK_NAME(1),
        POSTAL_CODE(9),
        CITY(10),
        BIC(19);

        private final int index;

        Column(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

    }

    public BankDataLoaderPl() {
        super(Column.class, DEFAULT_SOURCE_URI);
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return CP852;
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, getRemoteSourceCharset()), FIELD_SEPARATOR);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read Polish NBP raw source", ex);
        }

        Map<String, BankData> byKey = new LinkedHashMap<>();

        for (List<String> fields : rows) {
            if (fields.isEmpty()) {
                continue;
            }

            String settlementCode = getColumns().getOrEmpty(Column.SETTLEMENT_CODE, fields);
            if (settlementCode.length() < 7 || !settlementCode.chars().allMatch(Character::isDigit)) {
                continue; // header or invalid row
            }

            // Polish BBAN: bank code = first 3 digits, branch code = next 4 digits of the settlement number
            String bankCode = settlementCode.substring(0, 3);
            String branchCode = settlementCode.substring(3, 7);

            String bankName = getColumns().getOrEmpty(Column.BANK_NAME, fields);
            if (bankName.isEmpty()) {
                continue;
            }
            String bicRaw = getColumns().getOrEmpty(Column.BIC, fields);
            Bic bic = Bic.tryParse(bicRaw).orElse(null);

            String postalCode = getColumns().getOrNull(Column.POSTAL_CODE, fields);
            String city = getColumns().getOrNull(Column.CITY, fields);

            BankData record = new BankData(getCountryCode(), bankCode, branchCode, bic, bankName, postalCode, city, sourceVersion);
            // keep first occurrence per bank+branch key (see class Javadoc: bank code alone can be ambiguous)
            byKey.putIfAbsent(record.getKey(), record);
        }

        return new ArrayList<>(byKey.values());
    }

}
