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
 * Unit tests for {@link BankDataLoaderPt}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class BankDataLoaderPtTest {

    private static final String HEADER = "RIAD_CODE\tBIC\tCOUNTRY_OF_REGISTRATION\tNAME\tBOX\tADDRESS\tPOSTAL\tCITY\t"
        + "CATEGORY\tHEAD_COUNTRY_OF_REGISTRATION\tHEAD_NAME\tHEAD_RIAD_CODE\tRESERVE\tEXEMPT\r\n";

    private final BankDataLoaderPt loader = new BankDataLoaderPt();

    @Test
    void countryCode_returnsPT() {
        assertThat(loader.getCountryCode()).isEqualTo("PT");
    }

    @Test
    void remoteSourceUri_sameAsOtherEcbLoaders() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderEs.DEFAULT_SOURCE_URI);
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "7         | 0007",
        "35        | 0035",
        "160       | 0160",
        "5340      | 5340",
        "980752019 | (null)",
        "12A       | (null)",
    })
    void toBankCode_riadSuffix_returnsPaddedCodeOrNull(String riadSuffix, String expected) {
        assertThat(loader.toBankCode(riadSuffix)).isEqualTo(expected);
    }

    @Test
    void parse_multiCountryFile_keepsPortugueseRowsWithUsableCode() throws Exception {
        String content = HEADER
            + "PT35\tCGDIPTPLXXX\tPT\tCaixa Geral de Depositos, S.A.\t\tAv. Joao XXI, 63\t1000-300\tLisboa\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"
            + "PT7\tBESCPTPLXXX\tPT\tNOVO BANCO, S.A.\t\tAv. da Liberdade, 195\t1250-142\tLisboa\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"
            + "PT980752019\tREVOPTP2XXX\tPT\tREVOLUT BANK UAB - SUCURSAL EM PORTUGAL\t\tExample 1\t1000-000\tLisboa\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n" // tax number instead of a bank code, must be skipped
            + "GR011\tETHNGRAA\tGR\tNational Bank of Greece, S.A.\t\t86 Eolou Str.\t10559\tAthens\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"; // different country, must be filtered out
        InputStream in = new ByteArrayInputStream(content.getBytes(UTF_16));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).extracting(BankData::getBankCode).containsExactly("0035", "0007");
        assertThat(records.get(1).getBankName()).isEqualTo("NOVO BANCO, S.A.");
        assertThat((Object) records.get(1).getBic()).hasToString("BESCPTPLXXX");
    }

}
