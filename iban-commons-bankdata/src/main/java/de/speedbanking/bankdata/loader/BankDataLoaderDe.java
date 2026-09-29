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
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Loader for the Deutsche Bundesbank's German BLZ (Bankleitzahl) directory.
 * <p>
 * Checked against a live download and the official record description: semicolon-separated,
 * RFC-4180-quoted, one record per line, header row, 13 columns in order:
 * <pre>
 * Bankleitzahl;Merkmal;Bezeichnung;PLZ;Ort;Kurzbezeichnung;PAN;BIC;Pruefzifferberechnungsmethode;
 * Datensatznummer;Aenderungskennzeichen;Bankleitzahlloeschung;Nachfolge-Bankleitzahl
 * </pre>
 * Most fields are individually double-quoted, but an empty field can show up as a bare, unquoted
 * token between two separators, and at least one real field (Kurzbezeichnung) had a comma inside
 * its quotes, so this uses {@link BankDataFormat#parseQuotedFields(java.io.Reader, char)} instead
 * of a naive split.
 * <p>
 * The export is actually ISO-8859-1/Windows-1252, not UTF-8 - I checked the raw bytes of a live
 * download. The {@code Content-Type} header claims UTF-8, which is just wrong, so don't trust it.
 * <p>
 * A Bankleitzahl isn't always unique to one row: Bundesbank publishes a separate record for the
 * {@code Hauptstelle} (Merkmal {@code "1"}) and one per dependent {@code Zweigstelle} under the
 * same BLZ, and only the head office record reliably has a BIC. So we dedup by bank code,
 * preferring the head office (a branch record already kept for a BLZ gets replaced if the head
 * office turns up later in the file).
 *
 * @since 1.8.12
 */
public final class BankDataLoaderDe extends AbstractCountryBankDataLoader<BankDataLoaderDe.Column> {

    /**
     * Default remote source URI: the Bundesbank's BLZ download landing page, not the CSV itself.
     * <p>
     * The actual CSV URL rotates every quarter (a blob ID and hash,
     * {@code .../resource/blob/926192/2feadedd.../blz-aktuell-csv-data.csv}), so it can't be a
     * fixed constant. The landing page has stayed put across several releases though, so
     * {@link #resolveActualSourceUri(URI, byte[])} downloads that and scrapes the current CSV link
     * back out of the HTML (only the filename is stable, the path before it isn't).
     *
     * @see <a href="https://www.bundesbank.de/de/aufgaben/unbarer-zahlungsverkehr/serviceangebot/bankleitzahlen/download-bankleitzahlen-602592">Bundesbank Bankleitzahlen download page</a>
     */
    public static final URI     DEFAULT_SOURCE_URI              = BankDataLoaderDefaults.sourceUri("DE");

    /** The CSV filename is stable even though the blob ID/hash before it isn't. */
    private static final Pattern CSV_LINK_PATTERN                = Pattern.compile("href=\"([^\"]*blz-aktuell-csv-data\\.csv)\"");

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
        super(Column.class, DEFAULT_SOURCE_URI);
    }

    @Override
    public Optional<URI> resolveActualSourceUri(URI downloadedFrom, byte[] downloadedBytes) {
        return resolveLinkFromLandingPage(downloadedFrom, downloadedBytes, CSV_LINK_PATTERN);
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return Charset.forName("windows-1252");
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<List<String>> rows;
        try {
            rows = BankDataFormat.parseQuotedFields(new InputStreamReader(rawSource, getRemoteSourceCharset()), FIELD_SEPARATOR);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read German BLZ raw source", ex);
        }

        Map<String, BankData> byBankCode = new LinkedHashMap<>();
        // rowIndex starts at 1: row 0 is always the header, column names already verified against the live source
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            mergeRow(rows.get(rowIndex), rowIndex, sourceVersion, byBankCode);
        }
        return new ArrayList<>(byBankCode.values());
    }

    private void mergeRow(List<String> fields, int rowIndex, String sourceVersion, Map<String, BankData> byBankCode) throws BankDataParseException {
        if (isBlankTrailingLine(fields)) {
            return;
        }
        requireMinColumns(fields, rowIndex + 1, "German BLZ", "semicolon");

        String aenderungskennzeichen = getColumns().getOrEmpty(Column.AENDERUNGSKENNZEICHEN, fields);
        String merkmal = getColumns().getOrEmpty(Column.MERKMAL, fields);
        // per the Bundesbank's own guidance, a record is deleted when Aenderungskennzeichen = "D"
        // and Merkmal = "1", drop it rather than treating it as an active institution
        if (AENDERUNGSKENNZEICHEN_GELOESCHT.equalsIgnoreCase(aenderungskennzeichen) && MERKMAL_HAUPTSTELLE.equals(merkmal)) {
            return;
        }

        String bankCode = getColumns().getOrEmpty(Column.BANKLEITZAHL, fields);
        if (bankCode.isEmpty()) {
            return;
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
        byBankCode.merge(bankCode, bankData, (existing, candidate) -> MERKMAL_HAUPTSTELLE.equals(merkmal) ? candidate : existing);
    }

}
