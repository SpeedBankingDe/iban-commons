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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.io.MinimalXlsxReader;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bic.Bic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loader for the National Bank of Belgium (Nationale Bank van Belgie / Banque Nationale de
 * Belgique, NBB/BNB) bank identification code directory.
 * <p>
 * The NBB/BNB publishes this directory as an XLSX spreadsheet in two variants: a
 * {@code grouped_list_current.xlsx} that expresses runs of consecutive codes assigned to the same
 * institution as {@code From}/{@code To} ranges, and a {@code full_list_current.xlsx} with one row
 * per individual code. This loader deliberately downloads the latter to avoid having to expand
 * numeric ranges itself.
 * <p>
 * <strong>Verified raw format</strong> (checked against a live download of the {@code full_list}
 * variant): a single 3-digit Belgian bank code in the first column, followed by:
 * <pre>
 * code;Biccode;Name Dutch;Name French;Name German;Name English
 * </pre>
 * The sheet starts with a single-cell title row (e.g. {@code "Version 01/09/2026"}) above the
 * actual column-header row; any row whose first column is not a purely numeric bank code, not just
 * the very first row, is treated as a non-data row and skipped, which covers both in one pass. The
 * BIC column may hold the literal placeholder {@code "VRIJ"} ("free"/unassigned, Dutch) for a code
 * not currently assigned to any institution, which is not a valid BIC and is correctly dropped by
 * {@link Bic#tryParse(String)}. A real BIC value in this source is published with embedded spaces
 * (e.g. {@code "GEBA BE BB"} for what is actually {@code GEBABEBB}), so those are stripped before
 * parsing. The institution name is taken from the first populated column among English, Dutch,
 * French, German, in that order: most rows only populate Dutch (Flemish institutes) or leave
 * Dutch/English both blank with only French populated (Walloon-only institutes, e.g. code 171
 * "Banque CPH"), so a Dutch-or-English-only fallback silently drops those names. The spreadsheet
 * does not publish a postal code or city, so {@link BankData#getPostalCode()} and
 * {@link BankData#getCity()} are always {@code null}, the same convention used by
 * {@link BankDataLoaderCh} for the Swiss source.
 *
 * @since 1.8.11
 */
public final class BankDataLoaderBe extends AbstractCountryBankDataLoader<BankDataLoaderBe.Column> {

    /**
     * Default remote source URI: the NBB/BNB's single-code ("full list") bank code XLSX download,
     * no login required.
     */
    public static final URI DEFAULT_SOURCE_URI = URI
        .create("https://www.nbb.be/doc/be/be/protocol/full_list_current.xlsx");

    enum Column implements ColumnDefinition {

        CODE(0),
        BIC(1),
        NAME_DUTCH(2),
        NAME_FRENCH(3),
        NAME_GERMAN(4),
        NAME_ENGLISH(5);

        private final int index;

        Column(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

    }

    private static final Column[] PREFERRED_NAME_COLUMNS = {
        Column.NAME_ENGLISH,
        Column.NAME_DUTCH,
        Column.NAME_FRENCH,
        Column.NAME_GERMAN
    };

    public BankDataLoaderBe() {
        super(Column.class);
    }

    @Override
    public URI getRemoteSourceUri() {
        return DEFAULT_SOURCE_URI;
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<String[]> rows;
        try {
            rows = MinimalXlsxReader.read(rawSource);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read Belgian NBB/BNB XLSX raw source", ex);
        }

        // the raw format does not distinguish a head-office from a branch record, so deduplication
        // by bank code keeps the first occurrence (deterministic, documented tie-break)
        Map<String, BankData> byBankCode = new LinkedHashMap<>();

        for (String[] arrayFields : rows) {
            List<String> fields = arrayFields != null ? asList(arrayFields) : emptyList();
            String bankCode = getColumns().getOrEmpty(Column.CODE, fields);
            if (bankCode.isEmpty() || !bankCode.chars().allMatch(Character::isDigit)) {
                continue; // blank row, or a non-data row (title row, column-header row)
            }
            byBankCode.computeIfAbsent(bankCode, k -> toBankData(fields, k, sourceVersion));
        }

        return new ArrayList<>(byBankCode.values());
    }

    BankData toBankData(List<String> fields, String bankCode, String sourceVersion) {
        String bicRaw = getColumns().getOrEmpty(Column.BIC, fields);
        String bic = bicRaw.replace(" ", "");
        return new BankData(getCountryCode(), bankCode, Bic.tryParse(bic).orElse(null), bankName(fields), sourceVersion);
    }

    private String bankName(List<String> fields) {
        for (Column column : PREFERRED_NAME_COLUMNS) {
            String name = getColumns().getOrEmpty(column, fields);
            if (!name.isEmpty()) {
                return name;
            }
        }
        return "";
    }

}
