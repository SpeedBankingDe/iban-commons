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
package de.speedbanking.bankdata.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bic.Bic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader/writer for this module's internal, normalized bank data file format: one, fully
 * RFC-4180-compatible CSV file per country.
 * <p>
 * UTF-8, comma-separated, {@code \n} line endings (no CRLF, so the bundled fallback files diff
 * cleanly in Git), with a single header row identical for every country:
 * <pre>
 * bankCode,bic,bankName,postalCode,city,flags
 * </pre>
 * A field containing a comma, a double quote, or a line break is enclosed in double quotes, with
 * any double quote it contains doubled, exactly as required by RFC 4180. The {@code flags} column
 * is reserved for future use (e.g. an inactive-institute marker) and is always empty in this
 * version, keeping it present from the start avoids a format migration on the first extension.
 * Nullable fields ({@code bic}, {@code postalCode}, {@code city}) are written as empty strings.
 * <p>
 * This format is independent of any specific country: it is what a {@code CountryBankDataLoader}
 * produces after parsing a country's raw source, and what {@code CountryDataCache} reads back from
 * the local cache directory or the bundled classpath fallback. Consolidating what used to be a
 * {@code .dat} data file plus a {@code .meta} sidecar into this single file eliminates a
 * write-time inconsistency race that existed between two separate atomic renames.
 *
 * @since 1.8.11
 */
public final class BankDataFormat {

    private static final char FIELD_SEPARATOR = ',';
    private static final char QUOTE           = '"';
    private static final char LINE_FEED       = '\n';
    private static final char CARRIAGE_RETURN = '\r';

    private static final List<String> HEADER  = asList(
        "bankCode", "bic", "bankName", "postalCode", "city", "flags");
    private static final int          HEADER_MIN_LEN = HEADER.size() - 1;

    /**
     * The exact CSV header line every bank data file starts with, identical for every country.
     * Exposed so callers (e.g. {@code CountryDataCache}) can cheaply sanity-check a file without
     * fully parsing it.
     */
    public static final String HEADER_LINE = String.join(Character.toString(FIELD_SEPARATOR), HEADER);

    private BankDataFormat() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Reads bank data records for a single country from an input stream.
     *
     * @param in            the input stream, not closed by this method
     * @param charset       the charset to decode the stream with (always UTF-8 for this internal format)
     * @param countryCode   the ISO 3166-1 alpha-2 country code all records belong to
     * @param sourceVersion the source version identifier to stamp onto every record
     * @return the parsed records, in file order
     * @throws IOException if the stream cannot be read, or a data row is malformed
     */
    public static List<BankData> read(InputStream in, Charset charset, String countryCode, String sourceVersion) throws IOException {
        List<List<String>> rows = parseQuotedFields(new InputStreamReader(in, charset), FIELD_SEPARATOR);
        List<BankData> result = new ArrayList<>();
        boolean headerSkipped = false;
        int rowNumber = 0;
        for (List<String> row : rows) {
            rowNumber++;
            if (!headerSkipped) {
                headerSkipped = true;
                continue; // first row is always the header
            }
            if (isBlankRow(row)) {
                continue;
            }
            result.add(toBankData(row, countryCode, sourceVersion, rowNumber));
        }
        return result;
    }

    private static boolean isBlankRow(List<String> row) {
        return row.size() == 1 && row.get(0).isEmpty();
    }

    private static BankData toBankData(List<String> fields, String countryCode, String sourceVersion, int rowNumber) throws IOException {
        if (fields.size() < HEADER_MIN_LEN) {
            throw new IOException("Malformed bank data CSV row " + rowNumber + ": expected at least " + HEADER_MIN_LEN + " fields, got " + fields.size());
        }

        String bankCode = fields.get(0);
        String bicField = fields.get(1);
        String bankName = fields.get(2);
        String postalCode = emptyToNull(fields.get(3));
        String city = emptyToNull(fields.get(4));

        Bic bic = bicField.isEmpty() ? null : Bic.tryParse(bicField).orElse(null);

        return new BankData(countryCode, bankCode, bic, bankName, postalCode, city, sourceVersion);
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    /**
     * Writes bank data records to an output stream in this module's normalized CSV format,
     * including the header row.
     *
     * @param out     the output stream, not closed by this method
     * @param records the records to write
     * @throws IOException if the stream cannot be written
     */
    public static void write(OutputStream out, List<BankData> records) throws IOException {
        Writer writer = new OutputStreamWriter(out, UTF_8);
        writeRow(writer, HEADER);
        for (BankData record : records) {
            writeRow(writer, asList(
                record.getBankCode(),
                record.getBic() != null ? record.getBic().toString() : "",
                record.getBankName(),
                record.getPostalCode() != null ? record.getPostalCode() : "",
                record.getCity() != null ? record.getCity() : "",
                "" // reserved flags column, always empty in this version
            ));
        }
        writer.flush();
    }

    private static void writeRow(Writer writer, List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                writer.write(FIELD_SEPARATOR);
            }
            writer.write(csvField(fields.get(i)));
        }
        writer.write(LINE_FEED);
    }

    private static String csvField(String value) {
        String safe = value != null ? value : "";
        boolean needsQuoting = safe.indexOf(FIELD_SEPARATOR) >= 0 || safe.indexOf(QUOTE) >= 0
            || safe.indexOf(LINE_FEED) >= 0 || safe.indexOf(CARRIAGE_RETURN) >= 0;
        if (!needsQuoting) {
            return safe;
        }
        return QUOTE + safe.replace("\"", "\"\"") + QUOTE;
    }

    /**
     * Parses RFC-4180-style, quote-aware delimited content into rows of fields, handling quoted
     * fields (including embedded separators, double quotes, and line breaks) with a plain,
     * character-by-character state machine, deliberately not {@code String.split}, which cannot
     * honor quoting.
     * <p>
     * Exposed as a reusable building block for {@code CountryBankDataLoader} implementations whose
     * raw source uses a different separator than this module's own comma-based normalized format
     * (e.g. semicolon-separated national bank directories) but is still quoted in the same
     * RFC-4180 style.
     *
     * @param source    the character source, not closed by this method
     * @param separator the field separator character (this module's own format uses {@code ,};
     *                  callers parsing a raw national source typically pass {@code ;})
     * @return the parsed rows, in file order; a completely empty source yields an empty list, and
     *     a trailing line terminator does not produce a spurious empty trailing row
     * @throws IOException if the source cannot be read
     */
    public static List<List<String>> parseQuotedFields(Reader source, char separator) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        PushbackReader reader = new PushbackReader(source);
        List<String> currentRow = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean rowPending = false;

        int read;
        while ((read = reader.read()) != -1) {
            char ch = (char) read;
            rowPending = true;

            if (inQuotes) {
                if (ch == QUOTE) {
                    int next = reader.read();
                    if (next == QUOTE) {
                        field.append(QUOTE);
                    } else {
                        inQuotes = false;
                        if (next != -1) {
                            reader.unread(next);
                        }
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }

            if (ch == QUOTE) {
                inQuotes = true;
            } else if (ch == separator) {
                currentRow.add(field.toString());
                field.setLength(0);
            } else if (ch == CARRIAGE_RETURN || ch == LINE_FEED) {
                if (ch == CARRIAGE_RETURN) {
                    int next = reader.read();
                    if (next != -1 && next != LINE_FEED) {
                        reader.unread(next);
                    }
                }
                currentRow.add(field.toString());
                field.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                rowPending = false;
            } else {
                field.append(ch);
            }
        }

        if (rowPending) {
            currentRow.add(field.toString());
            rows.add(currentRow);
        }

        return rows;
    }

}
