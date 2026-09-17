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
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
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
 * Loader for the Deutsche Bundesbank's German BLZ (Bankleitzahl) directory.
 * <p>
 * <strong>Verified raw format</strong> (checked against a live download and the official record
 * description): semicolon-separated, RFC-4180-quoted, one record per line, with a header row and
 * 13 columns in this order:
 * <pre>
 * Bankleitzahl;Merkmal;Bezeichnung;PLZ;Ort;Kurzbezeichnung;PAN;BIC;Pruefzifferberechnungsmethode;
 * Datensatznummer;Aenderungskennzeichen;Bankleitzahlloeschung;Nachfolge-Bankleitzahl
 * </pre>
 * Most fields are individually double-quoted, but an empty field can appear as a bare, unquoted
 * empty token between two separators, and at least one real-world field (Kurzbezeichnung) is known
 * to contain an embedded comma inside its quotes, so this loader parses with
 * {@link BankDataFormat#parseQuotedFields(java.io.Reader, char)} rather than a naive split.
 * <p>
 * The real Bundesbank export is published in ISO-8859-1/Windows-1252, not UTF-8, confirmed by
 * inspecting the raw bytes of a live download: the HTTP response's {@code Content-Type} header
 * claims UTF-8, which is simply wrong, this is exactly the kind of situation
 * {@link CountryBankDataLoader#getRemoteSourceCharset()} exists to guard against, decoding must
 * never be based on a trusted {@code Content-Type}.
 * <p>
 * A Bankleitzahl is not always unique to a single row: the Bundesbank publishes a separate
 * {@code Datensatz} for a {@code Hauptstelle} (Merkmal {@code "1"}) and, under the same BLZ, one
 * per dependent {@code Zweigstelle} (branch), only the former reliably carries a BIC. Records are
 * therefore deduplicated by bank code, preferring a head-office record over a branch record
 * (upgrading a branch record already kept for a BLZ if a head-office record for the same BLZ is
 * found later in the file), the same tie-break convention documented on {@code
 * CountryBankDataLoader#parse}.
 *
 * @since 1.8.11
 */
public final class BankDataLoaderDe extends AbstractCountryBankDataLoader<BankDataLoaderDe.Column> {

    /**
     * Default remote source URI.
     * <p>
     * <strong>This URL is not stable long-term.</strong> It points at a specific Bundesbank
     * quarterly BLZ release and is only valid for that release's publication window (at the time
     * this was verified, the download page stated the file is valid from 2026-09-07 to
     * 2026-12-06). Unlike a typical stable API endpoint, this URL is expected to eventually 404
     * once the next quarterly release replaces it, and the new URL must be looked up again from
     * the Bundesbank download page below, this is a structural limitation of this data source, not
     * a one-off TODO to verify once before a release.
     *
     * @see <a href="https://www.bundesbank.de/de/aufgaben/unbarer-zahlungsverkehr/serviceangebot/bankleitzahlen/download-bankleitzahlen-602592">Bundesbank Bankleitzahlen download page</a>
     */
    public static final URI     DEFAULT_SOURCE_URI              = URI.create(
        "https://www.bundesbank.de/resource/blob/926192/2feadedd079726ffbe1eedaf6c74ff76/472B63F073F071307366337C94F8C870/blz-aktuell-csv-data.csv");

    private static final char   FIELD_SEPARATOR                 = ';';

    private static final String MERKMAL_HAUPTSTELLE             = "1";
    private static final String AENDERUNGSKENNZEICHEN_GELOESCHT = "D";

    enum Column implements ColumnDefinition {

        BANKLEITZAHL(0),
        MERKMAL(1),
        BEZEICHNUNG(2),
        PLZ(3),
        ORT(4),
        BIC(7),
        AENDERUNGSKENNZEICHEN(10),
        NACHFOLGE_BANKLEITZAHL(12);

        private final int index;

        Column(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

    }

    public BankDataLoaderDe() {
        super(Column.class);
    }

    @Override
    public URI getRemoteSourceUri() {
        return DEFAULT_SOURCE_URI;
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return Charset.forName("windows-1252");
    }

    @Override
    @SuppressWarnings("PMD.InefficientEmptyStringCheck")
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, getRemoteSourceCharset()), FIELD_SEPARATOR);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read German BLZ raw source", ex);
        }

        Map<String, BankData> byBankCode = new LinkedHashMap<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            if (rowIndex == 0) {
                continue; // header row, column names already verified against the live source
            }
            List<String> fields = rows.get(rowIndex);
            if (fields.size() == 1 && fields.get(0).trim().isEmpty()) {
                continue; // blank trailing line
            }
            if (fields.size() < getColumns().getMinColumnCount()) {
                throw new BankDataParseException("Malformed German BLZ row " + (rowIndex + 1)
                    + ": expected at least " + getColumns().getMinColumnCount() + " semicolon-separated fields, got " + fields.size());
            }

            String aenderungskennzeichen = getColumns().getOrEmpty(Column.AENDERUNGSKENNZEICHEN, fields);
            String merkmal = getColumns().getOrEmpty(Column.MERKMAL, fields);
            // per the Bundesbank's own guidance, a record is deleted when Aenderungskennzeichen = "D"
            // and Merkmal = "1", drop it rather than treating it as an active institution
            if (AENDERUNGSKENNZEICHEN_GELOESCHT.equals(aenderungskennzeichen) && MERKMAL_HAUPTSTELLE.equals(merkmal)) {
                continue;
            }

            String bankCode = getColumns().getOrEmpty(Column.BANKLEITZAHL, fields);
            if (bankCode.isEmpty()) {
                continue;
            }
            String bicRaw = getColumns().getOrEmpty(Column.BIC, fields);

            Bic bic = Bic.tryParse(bicRaw).orElse(null);
            String bankName = getColumns().getOrEmpty(Column.BEZEICHNUNG, fields);
            String postalCode = getColumns().getOrNull(Column.PLZ, fields);
            String city = getColumns().getOrNull(Column.ORT, fields);

            BankData bankData = new BankData(getCountryCode(), bankCode, bic, bankName, postalCode, city, sourceVersion);
            // a head-office record always wins (and overwrites an earlier branch record already
            // kept for this BLZ), since only the head-office record reliably carries a BIC (see
            // class Javadoc); a branch record only fills in if nothing has been kept for this BLZ yet
            if (MERKMAL_HAUPTSTELLE.equals(merkmal) || !byBankCode.containsKey(bankCode)) {
                byBankCode.put(bankCode, bankData);
            }
        }
        return new ArrayList<>(byBankCode.values());
    }

}
