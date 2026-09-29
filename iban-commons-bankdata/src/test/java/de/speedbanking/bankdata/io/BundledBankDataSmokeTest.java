package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Smoke test guarding the bundled classpath "Urladung" fallback resources: verifies that
 * {@code /bankdata/{DE,AT,CZ,BE,PL}.csv} are present, start with the expected CSV header,
 * non-empty, and parse without error, so a release can never accidentally ship a broken or missing
 * offline fallback data set.
 * <p>
 * {@code CH} and {@code NL} are deliberately excluded here: their upstream sources' terms of use
 * are restrictive about redistribution, so this module does not bundle an offline snapshot for
 * them pending clarification (see the module README's "Data licensing" section); both loaders
 * still work normally via a live download, just without the zero-network fallback.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BundledBankDataSmokeTest {

    @ParameterizedTest
    @ValueSource(strings = {"DE", "AT", "CZ", "BE", "PL"})
    void bundledCsvFile_existsAndParsesToNonEmptyRecords(String countryCode) throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/" + countryCode + ".csv")) {
            assertThat(in).as("bundled resource /bankdata/%s.csv", countryCode).isNotNull();
            records = BankDataFormat.read(in, UTF_8, countryCode, "smoke-test");
        }

        assertThat(records).isNotEmpty();
        assertThat(records).allMatch(r -> r.getCountryCode().equals(countryCode));
        assertThat(records).allMatch(r -> !r.getBankCode().isEmpty());
        assertThat(records).allMatch(r -> !r.getBankName().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DE", "AT", "CZ", "BE", "PL"})
    void bundledCsvFile_startsWithExpectedHeader(String countryCode) throws Exception {
        String firstLine;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/" + countryCode + ".csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8))) {
            firstLine = reader.readLine();
        }

        assertThat(firstLine).isEqualTo(BankDataFormat.HEADER_LINE);
    }

    @Test
    void deCsv_containsTheDocumentedSmokeExample() throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/DE.csv")) {
            records = BankDataFormat.read(in, UTF_8, "DE", "smoke-test");
        }

        assertThat(records).anyMatch(r -> r.getBankCode().equals("37040044"));
    }

}
