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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loader for the Dutch Betaalvereniging Nederland ("Payments Association Netherlands") BIC
 * directory ("BIC-lijst-NL").
 * <p>
 * Published as a single-sheet XLSX with exactly three columns and no header/data ambiguity beyond
 * two leading non-data rows: a single-cell title row carrying the last-update date (e.g.
 * {@code "BIC-lijst-NL | BIC-list-NL (Laatste update | last update 02-09-2026)"}), then a header
 * row ({@code BIC;Identifier;Betaaldienstverlener / Payment Service Provider}), then one data row
 * per institution:
 * <pre>
 * BIC;Identifier;Name
 * </pre>
 * The {@code Identifier} column is the 4-letter institution code that appears literally as the
 * bank identifier segment (positions 5-8) of a Dutch IBAN (see {@code IbanRegistry.NL}'s
 * {@code 4!a} bank code pattern), so it is used as {@link BankData#getBankCode()} rather than
 * deriving it from the BIC's institution code, keeping the two in sync even on the rare row where
 * they might otherwise diverge. Any row whose {@code Identifier} value is not exactly four
 * uppercase letters is treated as a non-data row (the title and header rows both fail this check)
 * and skipped. A handful of names in the source carry a trailing space (e.g.
 * {@code "AEGON BANK "}), which {@link Columns#getOrEmpty} already trims. The spreadsheet does not
 * publish a postal code or city, so {@link BankData#getPostalCode()} and
 * {@link BankData#getCity()} are always {@code null}, the same convention used by
 * {@link BankDataLoaderBe} and {@link BankDataLoaderCh}.
 * <p>
 * A bundled {@code /bankdata/NL.csv} offline fallback ships with this loader, same as the other
 * loaders in this package. Written permission was granted 2026-09-22 by the Betaalvereniging's
 * press office for free use, conditional on citing "Betaalvereniging Nederland" as the source and
 * disclaiming that a Dutch BIC or bank code may change, be revoked, or be added at any time. See
 * the module README's "Data licensing" section for the full assessment and where that attribution
 * lives.
 *
 * @since 1.8.11
 */
public final class BankDataLoaderNl extends AbstractCountryBankDataLoader<BankDataLoaderNl.Column> {

    /**
     * Default remote source URI: the Betaalvereniging's BIC directory landing page, not the XLSX
     * itself.
     * <p>
     * The actual XLSX URL carries the WordPress media upload month in its path (e.g.
     * {@code .../wp-content/uploads/2025/11/BIC-lijst-NL.xlsx}), which shifts whenever the
     * Betaalvereniging re-uploads a refreshed file, so it can't be a fixed constant. The filename
     * itself has stayed put across releases, so {@link #resolveActualSourceUri(URI, byte[])}
     * downloads the landing page and scrapes the current XLSX link back out of the HTML, the same
     * approach {@link BankDataLoaderDe} uses for the Bundesbank's rotating blob URL.
     *
     * @see <a href="https://www.betaalvereniging.nl/kennisbank/iban-en-bic/bic-code/">Betaalvereniging BIC code page</a>
     */
    public static final URI DEFAULT_SOURCE_URI = BankDataLoaderDefaults.sourceUri("NL");

    /** The XLSX filename is stable even though the WordPress upload-month path before it isn't. */
    private static final Pattern XLSX_LINK_PATTERN = Pattern.compile("href=\"([^\"]*BIC-lijst-NL\\.xlsx)\"");

    enum Column implements ColumnDefinition {

        BIC(0),
        IDENTIFIER(1),
        NAME(2);

        private final int index;

        Column(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

    }

    public BankDataLoaderNl() {
        super(Column.class, DEFAULT_SOURCE_URI);
    }

    @Override
    public Optional<URI> resolveActualSourceUri(URI downloadedFrom, byte[] downloadedBytes) {
        String html = new String(downloadedBytes, UTF_8);
        Matcher matcher = XLSX_LINK_PATTERN.matcher(html);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(downloadedFrom.resolve(matcher.group(1)));
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
        List<String[]> rows;
        try {
            rows = MinimalXlsxReader.read(rawSource);
        } catch (IOException ex) {
            throw new BankDataParseException("Failed to read Dutch Betaalvereniging XLSX raw source", ex);
        }

        // no head-office/branch distinction here, just dedup by bank identifier and keep the first one
        Map<String, BankData> byBankCode = new LinkedHashMap<>();

        for (String[] arrayFields : rows) {
            List<String> fields = arrayFields != null ? asList(arrayFields) : emptyList();
            String identifier = getColumns().getOrEmpty(Column.IDENTIFIER, fields);
            if (!isBankIdentifier(identifier)) {
                continue; // blank row, or a non-data row (title row, column-header row)
            }
            byBankCode.computeIfAbsent(identifier, k -> toBankData(fields, k, sourceVersion));
        }

        return new ArrayList<>(byBankCode.values());
    }

    private static boolean isBankIdentifier(String value) {
        return value.length() == 4 && value.chars().allMatch(c -> c >= 'A' && c <= 'Z');
    }

    private BankData toBankData(List<String> fields, String identifier, String sourceVersion) {
        String bic = getColumns().getOrEmpty(Column.BIC, fields);
        String name = getColumns().getOrEmpty(Column.NAME, fields);
        return new BankData(getCountryCode(), identifier, Bic.tryParse(bic).orElse(null), name, sourceVersion);
    }

}
