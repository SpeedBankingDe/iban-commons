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
import static java.util.stream.Collectors.toList;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bic.Bic;
import de.speedbanking.util.UtilityClasses;

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
import java.util.Arrays;
import java.util.List;

/**
 * Reader/writer for this module's internal, normalized bank data file format: one, fully
 * RFC-4180-compatible CSV file per country.
 * <p>
 * UTF-8, semicolon-separated, {@code \n} line endings (no CRLF, so the bundled fallback files diff
 * cleanly in Git), with a single header row identical for every country:
 * <pre>
 * bankCode;branchCode;bic;bankName;postalCode;city;flags;checkDigitMethod
 * </pre>
 * Semicolon rather than comma, the same convention every raw national source this module parses
 * already uses, and it keeps quoting rare: a comma inside a bank name (common, e.g. {@code "Muster
 * GmbH, Zweigstelle Nord"}) never forces quoting, only a semicolon, quote, or line break does.
 * {@code branchCode} is written as an empty string for a record with no branch code
 * ({@link BankData#getBranchCode()} is {@code null}), which is the common case: it is only
 * populated for a country whose BBAN carries a branch code that is actually needed to identify a
 * single institution (see {@link BankData#getKey()}).
 * A field containing the separator, a double quote, or a line break is enclosed in double quotes,
 * with any double quote it contains doubled, exactly as required by RFC 4180. The {@code flags} column
 * is reserved for future use (e.g. an inactive-institute marker) and is always empty in this
 * version, keeping it present from the start avoids a format migration on the first extension.
 * The {@code checkDigitMethod} column carries {@link BankData#getCheckDigitMethod()}, populated only
 * for countries whose source publishes a national account check digit method (currently Germany).
 * Nullable fields ({@code bic}, {@code postalCode}, {@code city}, {@code checkDigitMethod}) are
 * written as empty strings.
 * <p>
 * The writer always writes every column. The reader is lenient about trailing columns: a row may
 * end after {@code city}, {@code flags}, or {@code checkDigitMethod}, and a missing column reads as
 * empty.
 * <p>
 * This format is independent of any specific country: it is what a {@code CountryBankDataLoader}
 * produces after parsing a country's raw source, and what {@code CountryDataCache} reads back from
 * the local cache directory or the bundled classpath fallback. Consolidating what used to be a
 * {@code .dat} data file plus a {@code .meta} sidecar into this single file eliminates a
 * write-time inconsistency race that existed between two separate atomic renames.
 *
 * @since 1.8.12
 */
public final class BankDataFormat {

    private static final char FIELD_SEPARATOR = ';';
    private static final char QUOTE           = '"';
    private static final char LINE_FEED       = '\n';
    private static final char CARRIAGE_RETURN = '\r';

    @SuppressWarnings("InlineTrivialConstant")
    private static final String EMPTY = "";

    private static final Columns<Column> COLUMNS = Columns.of(Column.class);
    private static final List<String>    HEADER  = Arrays.stream(Column.values()).map(Column::getCamelCaseName).collect(toList());

    /**
     * The exact CSV header line every bank data file starts with, identical for every country.
     * Exposed so callers (e.g. {@code CountryDataCache}) can cheaply sanity-check a file without
     * fully parsing it.
     */
    public static final String HEADER_LINE = String.join(Character.toString(FIELD_SEPARATOR), HEADER);

    private BankDataFormat() {
        throw UtilityClasses.cannotInstantiate(getClass());
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
                headerSkipped = true; // first row is always the header
            } else if (!isBlankRow(row)) {
                result.add(toBankData(row, countryCode, sourceVersion, rowNumber));
            }
        }
        return result;
    }

    /**
     * Returns whether the given row has no content, i.e. no field or a single empty field.
     */
    private static boolean isBlankRow(List<String> row) {
        return row.isEmpty() || (row.size() == 1 && row.get(0).isEmpty());
    }

    /**
     * Maps one data row to a {@link BankData} record, rejecting a row that is too short or lacks a
     * required field.
     */
    private static BankData toBankData(List<String> fields, String countryCode, String sourceVersion, int rowNumber) throws IOException {
        if (fields.size() < COLUMNS.getMinColumnCount()) {
            throw new IOException(String.format("Malformed bank data CSV row %s: expected at least %s fields, got %s",
                rowNumber, COLUMNS.getMinColumnCount(), fields.size()));
        }

        String bankCode = COLUMNS.getOrEmpty(Column.BANK_CODE, fields);
        String branchCode = COLUMNS.getOrNull(Column.BRANCH_CODE, fields);
        String bicField = COLUMNS.getOrEmpty(Column.BIC, fields);
        String bankName = COLUMNS.getOrEmpty(Column.BANK_NAME, fields);
        String postalCode = COLUMNS.getOrNull(Column.POSTAL_CODE, fields);
        String city = COLUMNS.getOrNull(Column.CITY, fields);
        String checkDigitMethod = COLUMNS.getOrNull(Column.CHECK_DIGIT_METHOD, fields);

        // the field-count check above only guards against a short/truncated row; it says nothing
        // about a row that has the right shape but an empty required field (e.g. a stray leading
        // separator), which BankData's constructor itself would happily accept (it only rejects
        // null, not "")
        if (bankCode.isEmpty() || bankName.isEmpty()) {
            throw new IOException(String.format("Malformed bank data CSV row %s: %s and %s must not be empty",
                rowNumber, Column.BANK_CODE.getCamelCaseName(), Column.BANK_NAME.getCamelCaseName()));
        }

        Bic bic = bicField.isEmpty() ? null : Bic.tryParse(bicField).orElse(null);

        return BankData.builder(countryCode, bankCode, bankName, sourceVersion)
            .branchCode(branchCode)
            .bic(bic)
            .postalCode(postalCode)
            .city(city)
            .checkDigitMethod(checkDigitMethod)
            .build();
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
        for (BankData entry : records) {
            writeRow(writer, asList(
                entry.getBankCode(),
                entry.branchCode().orElse(EMPTY),
                entry.bic().map(Bic::toString).orElse(EMPTY),
                entry.getBankName(),
                entry.postalCode().orElse(EMPTY),
                entry.city().orElse(EMPTY),
                EMPTY, // reserved flags column, always empty in this version
                entry.checkDigitMethod().orElse(EMPTY)
            ));
        }
        writer.flush();
    }

    /**
     * Writes one row of fields, separated and quoted as needed, followed by a line feed.
     */
    private static void writeRow(Writer writer, List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                writer.write(FIELD_SEPARATOR);
            }
            writer.write(csvField(fields.get(i)));
        }
        writer.write(LINE_FEED);
    }

    /**
     * Returns the given value as a CSV field, quoted if it contains a separator, quote or line break.
     */
    private static String csvField(String value) {
        String safe = value != null ? value : EMPTY;
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
     * raw source uses a different separator than this module's own normalized format but is still
     * quoted in the same RFC-4180 style.
     *
     * @param source    the character source, not closed by this method
     * @param separator the field separator character (this module's own format, and most raw
     *                  national sources it parses, use {@code ;})
     * @return the parsed rows, in file order; a completely empty source yields an empty list, and
     *     a trailing line terminator does not produce a spurious empty trailing row
     * @throws IOException if the source cannot be read
     */
    public static List<List<String>> parseQuotedFields(Reader source, char separator) throws IOException {
        PushbackReader reader = new PushbackReader(source);
        RowParser parser = new RowParser(separator);

        int read;
        while ((read = reader.read()) != -1) {
            parser.consume((char) read, reader);
        }
        parser.finish();

        return parser.rows;
    }

    /**
     * Mutable state machine backing {@link #parseQuotedFields(Reader, char)}, split out of that
     * method purely to keep each branch of the RFC-4180 quoting logic in its own small method
     * instead of one large one.
     */
    private static final class RowParser {
        private final char               separator;
        private final List<List<String>> rows       = new ArrayList<>();
        private final StringBuilder      field      = new StringBuilder();
        private List<String>             currentRow = new ArrayList<>();
        private boolean                  inQuotes;
        private boolean                  rowPending;

        private RowParser(char separator) {
            this.separator = separator;
        }

        /**
         * Consumes one character, inside or outside a quoted field.
         */
        private void consume(char ch, PushbackReader reader) throws IOException {
            rowPending = true;
            if (inQuotes) {
                consumeQuoted(ch, reader);
            } else {
                consumeUnquoted(ch, reader);
            }
        }

        /**
         * Consumes one character inside a quoted field, where a doubled quote is a literal quote.
         */
        private void consumeQuoted(char ch, PushbackReader reader) throws IOException {
            if (ch != QUOTE) {
                field.append(ch);
                return;
            }
            int next = reader.read();
            if (next == QUOTE) {
                field.append(QUOTE);
            } else {
                inQuotes = false;
                if (next != -1) {
                    reader.unread(next);
                }
            }
        }

        /**
         * Consumes one character outside a quoted field.
         */
        private void consumeUnquoted(char ch, PushbackReader reader) throws IOException {
            if (ch == QUOTE) {
                inQuotes = true;
            } else if (ch == separator) {
                endField();
            } else if (ch == CARRIAGE_RETURN || ch == LINE_FEED) {
                consumeLineBreak(ch, reader);
            } else {
                field.append(ch);
            }
        }

        /**
         * Ends the current row at a line break; a CR LF pair counts as one line break.
         */
        private void consumeLineBreak(char ch, PushbackReader reader) throws IOException {
            if (ch == CARRIAGE_RETURN) {
                int next = reader.read();
                if (next != -1 && next != LINE_FEED) {
                    reader.unread(next);
                }
            }
            endField();
            rows.add(currentRow);
            currentRow = new ArrayList<>();
            rowPending = false;
        }

        /**
         * Adds the current field to the current row and starts a new field.
         */
        private void endField() {
            currentRow.add(field.toString());
            field.setLength(0);
        }

        /**
         * Adds a last row that is not terminated by a line break.
         */
        private void finish() {
            if (rowPending) {
                endField();
                rows.add(currentRow);
            }
        }
    }

    /**
     * The columns of the format, declared in file order. The header names are the camel case
     * names of the constants.
     */
    enum Column implements ColumnDefinition {

        BANK_CODE(0),
        BRANCH_CODE(1),
        BIC(2),
        BANK_NAME(3),
        POSTAL_CODE(4),
        CITY(5),
        FLAGS(6, true),
        CHECK_DIGIT_METHOD(7, true);

        private final int     index;
        private final boolean optional;

        Column(int index) {
            this(index, false);
        }

        Column(int index, boolean optional) {
            this.index = index;
            this.optional = optional;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isOptional() {
            return optional;
        }

    }

}
