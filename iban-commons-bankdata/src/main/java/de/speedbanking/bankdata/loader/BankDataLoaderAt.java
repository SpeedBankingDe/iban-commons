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

import static java.nio.charset.StandardCharsets.ISO_8859_1;

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
 * Loader for the Austrian OeNB (Oesterreichische Nationalbank) SEPA-Zahlungsverkehrs-Verzeichnis.
 * <p>
 * <strong>Verified raw format</strong> (checked against a live download): semicolon-separated,
 * one record per line, published in ISO-8859-1 (the {@code Content-Type} response header claims
 * plain {@code text/csv} with no charset, so this cannot be trusted, exactly the situation
 * {@link CountryBankDataLoader#getRemoteSourceCharset()} exists to guard against). The file opens
 * with a few free-text disclaimer/title lines before the actual header row, so this loader skips
 * everything up to and including the first row whose {@code Bankleitzahl} column literally reads
 * {@code "Bankleitzahl"}, rather than assuming a fixed number of preamble lines. The header row
 * declares 22 columns:
 * <pre>
 * Kennzeichen;Identnummer;Bankleitzahl;Institutsart;Sektor;Firmenbuchnummer;Bankenname;Straße;PLZ;
 * Ort;Postadresse / Straße;Postadresse / PLZ;Postadresse / Ort;Postfach;Bundesland;Telefon;Fax;
 * E-Mail;SWIFT-Code;Homepage;Gründungsdatum;LEI
 * </pre>
 * {@code Kennzeichen} is {@code "Hauptanstalt"} for a head office, {@code "Filiale"} for a
 * dependent branch of a foreign institute, or blank for a subsidiary institute that nonetheless
 * has its own {@code Bankleitzahl}; unlike the German Bundesbank source, every row already carries
 * a distinct {@code Bankleitzahl}, so no branch-vs-head-office collapsing by bank code is needed,
 * a defensive dedup (keeping the first occurrence) is still applied in case a future export
 * changes that. {@code SWIFT-Code} may be empty for a small number of institutes (observed for
 * some institutes in wind-down); such records are kept with a {@code null} BIC rather than
 * dropped.
 *
 * @see <a href="https://www.oenb.at/Statistik/Klassifikationen/SEPA-Zahlungsverkehrs-Verzeichnis.html">OeNB SEPA-Zahlungsverkehrs-Verzeichnis</a>
 * @since 1.8.11
 */
public final class BankDataLoaderAt extends AbstractCountryBankDataLoader<BankDataLoaderAt.Column> {

    /** Default remote source URI: the OeNB's full SEPA payment directory CSV, no login required. */
    public static final URI DEFAULT_SOURCE_URI = URI
        .create("https://www.oenb.at/docroot/downloads_observ/sepa-zv-vz_gesamt.csv");

    private static final char   FIELD_SEPARATOR    = ';';

    private static final String HEADER_MARKER      = "Bankleitzahl";

    enum Column implements ColumnDefinition {

        BANKLEITZAHL(2),
        BANKENNAME(6),
        PLZ(8),
        ORT(9),
        SWIFT_CODE(18);

        private final int index;

        Column(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

    }

    public BankDataLoaderAt() {
        super(Column.class);
    }

    @Override
    public URI getRemoteSourceUri() {
        return DEFAULT_SOURCE_URI;
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return ISO_8859_1;
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, getRemoteSourceCharset()), FIELD_SEPARATOR);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read Austrian OeNB raw source", ex);
        }

        // the raw format does not distinguish a head-office from a branch record by bank code (every
        // row already has its own Bankleitzahl), but a defensive dedup keeps the first occurrence
        // (deterministic, documented tie-break) in case a future export changes that
        Map<String, BankData> byBankCode = new LinkedHashMap<>();

        boolean headerSeen = false;
        for (List<String> fields : rows) {
            if (!headerSeen) {
                headerSeen = HEADER_MARKER.equals(getColumns().getOrEmpty(Column.BANKLEITZAHL, fields));
                continue;
            }
            if (fields.size() < getColumns().getMinColumnCount()) {
                continue; // blank trailing line
            }
            String bankCode = getColumns().getOrEmpty(Column.BANKLEITZAHL, fields);
            if (bankCode.isEmpty()) {
                continue;
            }
            String bicRaw = getColumns().getOrEmpty(Column.SWIFT_CODE, fields);
            Bic bic = Bic.tryParse(bicRaw).orElse(null);
            String bankName = getColumns().getOrEmpty(Column.BANKENNAME, fields);
            String postalCode = getColumns().getOrNull(Column.PLZ, fields);
            String city = getColumns().getOrNull(Column.ORT, fields);

            BankData record = new BankData(getCountryCode(), bankCode, bic, bankName, postalCode, city, sourceVersion);
            byBankCode.putIfAbsent(record.getBankCode(), record);
        }

        if (!headerSeen) {
            throw new BankDataParseException("Malformed Austrian OeNB raw source: header row with a '" + HEADER_MARKER + "' column not found");
        }

        return new ArrayList<>(byBankCode.values());
    }

}
