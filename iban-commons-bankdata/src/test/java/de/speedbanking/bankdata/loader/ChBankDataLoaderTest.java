package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.spi.BankDataParseException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Unit tests for {@link ChBankDataLoader}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class ChBankDataLoaderTest {

    private final ChBankDataLoader loader = new ChBankDataLoader();

    @Test
    void countryCode_returnsCH() {
        assertThat(loader.getCountryCode()).isEqualTo("CH");
    }

    @Test
    void remoteSourceUri_returnsDefaultSourceUri() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(ChBankDataLoader.DEFAULT_SOURCE_URI);
    }

    @Test
    void remoteSourceCharset_returnsUtf8() {
        assertThat(loader.getRemoteSourceCharset()).isEqualTo(UTF_8);
    }

    @Test
    void parse_rowWithTooFewColumns_throwsBankDataParseException() {
        String header = "IID/QR-IID;Valid on;Concatenation;New IID/QR-IID;SIC IID;Headquarters;IID type;"
            + "QR-IID allocation;Name of bank/institution;Street Name;Building Number;Post Code;Town Name;"
            + "Country;BIC;SIC participation;RTGS customer payments, CHF;IP customer payments, CHF;"
            + "euroSIC participation;LSV+/BDD, CHF;LSV+/BDD, EUR;20260101000000\n"
            + "700;2026-09-15;N;;070008\n"; // far fewer than the required 21 fields
        InputStream in = new ByteArrayInputStream(header.getBytes(UTF_8));

        assertThatThrownBy(() -> loader.parse(in, "test-version"))
            .isInstanceOf(BankDataParseException.class)
            .hasMessageContaining("Malformed Swiss Bank Master row");
    }

    @Test
    void parse_nonNumericIid_throwsBankDataParseException() {
        String header = "IID/QR-IID;Valid on;Concatenation;New IID/QR-IID;SIC IID;Headquarters;IID type;"
            + "QR-IID allocation;Name of bank/institution;Street Name;Building Number;Post Code;Town Name;"
            + "Country;BIC;SIC participation;RTGS customer payments, CHF;IP customer payments, CHF;"
            + "euroSIC participation;LSV+/BDD, CHF;LSV+/BDD, EUR;20260101000000\n"
            + "not-a-number;2026-09-15;N;;070008;700;1;;Musterbank;Bahnhofstrasse;1;8001;Zuerich;CH;"
            + "UBSWCHZHXXX;Y;Y;Y;Y;Y;N\n";
        InputStream in = new ByteArrayInputStream(header.getBytes(UTF_8));

        assertThatThrownBy(() -> loader.parse(in, "test-version"))
            .isInstanceOf(BankDataParseException.class)
            .hasMessageContaining("Malformed Swiss Bank Master IID");
    }

    @Test
    void parse_unparsableBic_recordIncludedWithNullBic() throws Exception {
        String header = "IID/QR-IID;Valid on;Concatenation;New IID/QR-IID;SIC IID;Headquarters;IID type;"
            + "QR-IID allocation;Name of bank/institution;Street Name;Building Number;Post Code;Town Name;"
            + "Country;BIC;SIC participation;RTGS customer payments, CHF;IP customer payments, CHF;"
            + "euroSIC participation;LSV+/BDD, CHF;LSV+/BDD, EUR;20260101000000\n"
            + "700;2026-09-15;N;;070008;700;1;;Musterbank;Bahnhofstrasse;1;8001;Zuerich;CH;"
            + "NOT-A-VALID-BIC;Y;Y;Y;Y;Y;N\n";
        InputStream in = new ByteArrayInputStream(header.getBytes(UTF_8));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat((Object) records.get(0).getBic()).isNull();
    }

    @Test
    void parse_sampleFile_skipsHeaderConcatenatedAndBiclessRows_padsIidAndPopulatesPostalCodeAndCity() throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/raw/CH-sample.txt")) {
            records = loader.parse(in, "test-version");
        }

        // the header row (with its extra trailing timestamp token), the BIC-less row (781), and the
        // concatenated/merged row (750) are all excluded
        assertThat(records).extracting(BankData::getBankCode)
            .containsExactlyInAnyOrder("00700", "00760");

        BankData zuerich = findByBankCode(records, "00700");
        assertThat(zuerich.getBankName()).isEqualTo("Musterbank Zuerich AG");
        assertThat((Object) zuerich.getBic()).isNotNull();
        assertThat(zuerich.getBic().toString()).isEqualTo("UBSWCHZHXXX");
        assertThat(zuerich.getPostalCode()).isEqualTo("8001");
        assertThat(zuerich.getCity()).isEqualTo("Zuerich");

        BankData postfinance = findByBankCode(records, "00760");
        assertThat(postfinance.getPostalCode()).isEqualTo("3030");
        assertThat(postfinance.getCity()).isEqualTo("Bern");

        assertThat(records).allMatch(r -> r.getSourceVersion().equals("test-version"));
    }

    private static BankData findByBankCode(List<BankData> records, String bankCode) {
        return records.stream()
            .filter(r -> r.getBankCode().equals(bankCode))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No record found for bank code " + bankCode));
    }

}
