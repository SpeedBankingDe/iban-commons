package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Unit tests for {@link BankDataLoaderPl}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderPlTest {

    private final BankDataLoaderPl loader = new BankDataLoaderPl();

    private static final Charset CP852 = Charset.forName("CP852");

    @Test
    void countryCode_returnsPL() {
        assertThat(loader.getCountryCode()).isEqualTo("PL");
    }

    @Test
    void remoteSourceUri_returnsDefaultSourceUri() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderPl.DEFAULT_SOURCE_URI);
    }

    @Test
    void remoteSourceCharset_returnsCp852() {
        assertThat(loader.getRemoteSourceCharset()).isEqualTo(CP852);
    }

    @Test
    void parse_bankCodeAndBranchCode_areDerivedFromSettlementNumberNotInstitutionNumber() throws Exception {
        // institution number (field 0) is 102, but the settlement number (field 4) starts with 144,
        // as happens when a bank absorbs another bank's legacy settlement prefix
        String content = "102\tPKO Bank Polski\t\t102\t14400003\tOddzial\tO.\tWarszawa\t\t00-001\tWarszawa\t"
            + "\t\t\t\t\t\t\t\t\tBPKOPLPWXXX\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankCode()).isEqualTo("144");
        assertThat(records.get(0).getBranchCode()).isEqualTo("0000");
        assertThat(records.get(0).getKey()).isEqualTo("1440000");
    }

    @Test
    void parse_differentBranchesOfSameBank_areBothKept() throws Exception {
        // same 3-digit bank code (101), but two distinct 4-digit branch codes: distinct institutions
        // in the BBAN sense, no longer conflated now that the lookup key includes the branch code
        String content = "101\tNarodowy Bank Polski\t\t101\t10100000\tCentrala\tC-la\tWarszawa\t\t00-919\tWarszawa\n"
            + "999\tOther Bank\t\t999\t10199999\tCentrala\tC-la\tKrakow\t\t30-001\tKrakow\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(2);
        assertThat(records).extracting(BankData::getKey).containsExactlyInAnyOrder("1010000", "1019999");
    }

    @Test
    void parse_ambiguousSettlementPrefix_keepsFirstOccurrence() throws Exception {
        // identical bank+branch (101/0000) from two rows: still genuinely ambiguous, first wins
        String content = "101\tNarodowy Bank Polski\t\t101\t10100000\tCentrala\tC-la\tWarszawa\t\t00-919\tWarszawa\n"
            + "999\tOther Bank\t\t999\t10100001\tCentrala\tC-la\tKrakow\t\t30-001\tKrakow\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankCode()).isEqualTo("101");
        assertThat(records.get(0).getBankName()).isEqualTo("Narodowy Bank Polski");
    }

    @Test
    void parse_rowWithNonDigitSettlementCode_isSkipped() throws Exception {
        String content = "101\tNarodowy Bank Polski\t\t101\tXXXXXXXX\tCentrala\tC-la\tWarszawa\n"
            + "102\tPKO Bank Polski\t\t102\t10200000\tCentrala\tC-la\tWarszawa\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankCode()).isEqualTo("102");
    }

    @Test
    void parse_rowWithBlankSettlementCode_isSkipped() throws Exception {
        String content = "101\tNarodowy Bank Polski\t\t101\t\tCentrala\tC-la\tWarszawa\n"
            + "102\tPKO Bank Polski\t\t102\t10200000\tCentrala\tC-la\tWarszawa\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankCode()).isEqualTo("102");
    }

    @Test
    void parse_rowWithEmptyBankName_isSkipped() throws Exception {
        String content = "101\t\t\t101\t10100000\tCentrala\tC-la\tWarszawa\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).isEmpty();
    }

    @Test
    void parse_unparsableBic_recordIncludedWithNullBic() throws Exception {
        String content = "101\tNarodowy Bank Polski\t\t101\t10100000\tCentrala\tC-la\tWarszawa\t\t00-919\tWarszawa\t"
            + "\t\t\t\t\t\t\t\tNOT-A-VALID-BIC\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat((Object) records.get(0).getBic()).isNull();
    }

    @Test
    void parse_fullRow_extractsBankNamePostalCodeCityAndBic() throws Exception {
        String content = "101\tNarodowy Bank Polski\t\t101\t10100000\tCentrala\tC-la\tWarszawa\tul. Swietokrzyska 11/21\t"
            + "00-919\tWarszawa\t1011\t\t\t22 185 10 10\t\t22 185 23 09\t\t1945-01-15\tNBPLPLPWXXX\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(CP852));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        BankData nbp = records.get(0);
        assertThat(nbp.getBankCode()).isEqualTo("101");
        assertThat(nbp.getBankName()).isEqualTo("Narodowy Bank Polski");
        assertThat(nbp.getPostalCode()).isEqualTo("00-919");
        assertThat(nbp.getCity()).isEqualTo("Warszawa");
        assertThat(nbp.getBic().toString()).isEqualTo("NBPLPLPWXXX");
        assertThat(nbp.getSourceVersion()).isEqualTo("test-version");
    }

}
