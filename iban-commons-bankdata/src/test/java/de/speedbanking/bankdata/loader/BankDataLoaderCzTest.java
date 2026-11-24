package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Unit tests for {@link BankDataLoaderCz}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderCzTest {

    private final BankDataLoaderCz loader = new BankDataLoaderCz();

    @Test
    void countryCode_returnsCZ() {
        assertThat(loader.getCountryCode()).isEqualTo("CZ");
    }

    @Test
    void parse_sampleFile_skipsHeaderRow_andHandlesMissingBic() throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/raw/CZ-sample.txt")) {
            records = loader.parse(in, "test-version");
        }

        assertThat(records).hasSize(3);
        assertThat(records).extracting(BankData::getBankCode).containsExactly("0100", "0300", "2700");

        BankData withoutBic = records.stream().filter(r -> "2700".equals(r.getBankCode())).findFirst().orElseThrow(AssertionError::new);
        assertThat((Object) withoutBic.getBic()).isNull();

        BankData withBic = records.stream().filter(r -> "0100".equals(r.getBankCode())).findFirst().orElseThrow(AssertionError::new);
        assertThat((Object) withBic.getBic()).hasToString("KOMBCZPPXXX");
        assertThat(withBic.getBankName()).isEqualTo("Musterbanka Praha a.s.");
        assertThat((Object) withBic.getPostalCode()).isNull();
        assertThat((Object) withBic.getCity()).isNull();
    }

    @Test
    void remoteSourceUri_returnsDefaultSourceUri() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderCz.DEFAULT_SOURCE_URI);
    }

    @Test
    void remoteSourceCharset_returnsUtf8() {
        assertThat(loader.getRemoteSourceCharset()).isEqualTo(UTF_8);
    }

    @Test
    void parse_lineWithTooFewFields_isSkipped() throws Exception {
        String content = "header;ignored;ignored\n0100;Musterbanka\n0300;Musterbanka Two;KOMBCZPPXXX;Y\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(UTF_8));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankCode()).isEqualTo("0300");
    }

    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"", ";Musterbanka without code;KOMBCZPPXXX;Y"})
    void parse_blankLineOrLineWithoutBankCode_isSkipped(String skippedLine) throws Exception {
        String content = "header;ignored;ignored\n" + skippedLine + "\n0100;Musterbanka;KOMBCZPPXXX;Y\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(UTF_8));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankCode()).isEqualTo("0100");
    }

    @Test
    void parse_duplicateBankCode_keepsFirstOccurrence() throws Exception {
        String content = "header;ignored;ignored\n"
            + "0100;First Bank;KOMBCZPPXXX;Y\n"
            + "0100;Second Bank;;Y\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(UTF_8));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankName()).isEqualTo("First Bank");
    }

    @Test
    void parse_unparsableBic_recordIncludedWithNullBic() throws Exception {
        String content = "header;ignored;ignored\n0100;Musterbanka;NOT-A-VALID-BIC;Y\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(UTF_8));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat((Object) records.get(0).getBic()).isNull();
    }

}
