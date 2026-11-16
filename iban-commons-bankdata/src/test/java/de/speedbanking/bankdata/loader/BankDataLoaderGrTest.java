package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_16;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Unit tests for {@link BankDataLoaderGr}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class BankDataLoaderGrTest {

    private static final String HEADER = "RIAD_CODE\tBIC\tCOUNTRY_OF_REGISTRATION\tNAME\tBOX\tADDRESS\tPOSTAL\tCITY\t"
        + "CATEGORY\tHEAD_COUNTRY_OF_REGISTRATION\tHEAD_NAME\tHEAD_RIAD_CODE\tRESERVE\tEXEMPT\r\n";

    private final BankDataLoaderGr loader = new BankDataLoaderGr();

    @Test
    void countryCode_returnsGR() {
        assertThat(loader.getCountryCode()).isEqualTo("GR");
    }

    @Test
    void remoteSourceUri_sameAsOtherEcbLoaders() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderEs.DEFAULT_SOURCE_URI);
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "011    | 011",
        "014N   | 014",
        "1234   | (null)",
        "01     | (null)",
        "014X   | (null)",
    })
    void toBankCode_riadSuffix_returnsThreeDigitCodeOrNull(String riadSuffix, String expected) {
        assertThat(loader.toBankCode(riadSuffix)).isEqualTo(expected);
    }

    @Test
    void parse_multiCountryFile_keepsGreekRowsWithUsableCode() throws Exception {
        String content = HEADER
            + "GR011\tETHNGRAA\tGR\tNational Bank of Greece, S.A.\t\t86 Eolou Str.\t10559\tAthens\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"
            + "GR017N\tPIRBGRAA\tGR\tPIRAEUS BANK SA\t\t4 Amerikis Str.\t10564\tAthens\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"
            + "GR9999\t\tGR\tUnusable Code Bank\t\tExample Str. 1\t10000\tAthens\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n" // not a 3-digit code, must be skipped
            + "PT35\tCGDIPTPLXXX\tPT\tCaixa Geral de Depositos, S.A.\t\tAv. Joao XXI, 63\t1000-300\tLisboa\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"; // different country, must be filtered out
        InputStream in = new ByteArrayInputStream(content.getBytes(UTF_16));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).extracting(BankData::getBankCode).containsExactly("011", "017");
        assertThat(records.get(1).getBankName()).isEqualTo("PIRAEUS BANK SA");
        assertThat((Object) records.get(1).getBic()).hasToString("PIRBGRAA");
    }

}
