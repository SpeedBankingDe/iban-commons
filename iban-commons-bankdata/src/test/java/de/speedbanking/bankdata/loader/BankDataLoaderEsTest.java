package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.spi.BankDataParseException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link BankDataLoaderEs}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderEsTest {

    private static final String HEADER = "RIAD_CODE\tBIC\tCOUNTRY_OF_REGISTRATION\tNAME\tBOX\tADDRESS\tPOSTAL\tCITY\t"
        + "CATEGORY\tHEAD_COUNTRY_OF_REGISTRATION\tHEAD_NAME\tHEAD_RIAD_CODE\tRESERVE\tEXEMPT\r\n";

    private final BankDataLoaderEs loader = new BankDataLoaderEs();

    @Test
    void countryCode_returnsES() {
        assertThat(loader.getCountryCode()).isEqualTo("ES");
    }

    @Test
    void remoteSourceUri_returnsDefaultSourceUri() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderEs.DEFAULT_SOURCE_URI);
    }

    @Test
    void remoteSourceCharset_returnsUtf16() {
        assertThat(loader.getRemoteSourceCharset()).isEqualTo(StandardCharsets.UTF_16);
    }

    @Test
    void resolveActualSourceUri_landingPageHtml_extractsCurrentCsvLinkNotGzipVariant() {
        String html = "<html><body>"
            + "<a href=\"/stats/money/mfi/general/html/dla/mfi_mrr_MID/fi_mrr_csv_260831.csv.gz\">CSV.GZ</a>"
            + "<a href=\"/stats/money/mfi/general/html/dla/mfi_mrr_MID/fi_mrr_csv_260831.csv\">CSV</a>"
            + "</body></html>";
        URI landingPage = URI.create("https://www.ecb.europa.eu/stats/financial_corporations/"
            + "list_of_financial_institutions/html/monthly_list-MID.en.html");

        Optional<URI> resolved = loader.resolveActualSourceUri(landingPage, html.getBytes(StandardCharsets.UTF_8));

        assertThat(resolved).contains(URI.create(
            "https://www.ecb.europa.eu/stats/money/mfi/general/html/dla/mfi_mrr_MID/fi_mrr_csv_260831.csv"));
    }

    @Test
    void resolveActualSourceUri_htmlWithoutMatchingLink_returnsEmpty() {
        String html = "<html><body>No matching link here.</body></html>";
        URI landingPage = URI.create("https://www.ecb.europa.eu/stats/financial_corporations/"
            + "list_of_financial_institutions/html/monthly_list-MID.en.html");

        Optional<URI> resolved = loader.resolveActualSourceUri(landingPage, html.getBytes(StandardCharsets.UTF_8));

        assertThat(resolved).isEmpty();
    }

    @Test
    void parse_rowWithTooFewColumns_throwsBankDataParseException() {
        String content = HEADER + "ES0049\tBSCHESMMXXX\tES\n"; // far fewer than the required columns
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_16));

        assertThatThrownBy(() -> loader.parse(in, "test-version"))
            .isInstanceOf(BankDataParseException.class)
            .hasMessageContaining("Malformed ECB financial institutions row");
    }

    @Test
    void parse_multiCountryFile_filtersToOwnCountryAndStripsCountryPrefixFromBankCode() throws Exception {
        String content = HEADER
            + "ES0049\tBSCHESMMXXX\tES\tBanco Santander, S.A.\t\tPs de Pereda, 9-12\t39004\tSantander\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"
            + "ES2100\tCAIXESBBXXX\tES\tCaixabank, S.A.\t\tCl Pintor Sorolla 2-4\t46002\tValencia\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n"
            + "FR30004\tBNPAFRPPXXX\tFR\tBNP Paribas\t\t16 Boulevard des Italiens\t75009\tParis\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n" // different country, must be filtered out
            + "DE00001\tDEUTDEFFXXX\tDE\tDeutsche Bank Aktiengesellschaft\t\tTaunusanlage 12\t60325\t"
            + "Frankfurt am Main\tCredit institution S122\t\t\t\tY\tN\r\n"; // different country, must be filtered out
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_16));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).extracting(BankData::getBankCode).containsExactlyInAnyOrder("0049", "2100");
        BankData santander = records.stream().filter(r -> r.getBankCode().equals("0049")).findFirst().get();
        assertThat(santander.getBankName()).isEqualTo("Banco Santander, S.A.");
        assertThat(santander.getBic().toString()).isEqualTo("BSCHESMMXXX");
        assertThat(santander.getPostalCode()).isEqualTo("39004");
        assertThat(santander.getCity()).isEqualTo("Santander");
        assertThat(records).allMatch(r -> r.getSourceVersion().equals("test-version"));
    }

    @Test
    void parse_rowWithRiadCodeNotPrefixedByOwnCountry_isSkipped() throws Exception {
        // COUNTRY_OF_REGISTRATION says ES, but RIAD_CODE carries a different country's prefix (e.g.
        // a relocated/merged institution keeping a legacy RIAD_CODE) - must not derive a bogus bank
        // code by blindly stripping the country-code length off it
        String content = HEADER
            + "IE1234\tSOMEBIC1XXX\tES\tRelocated Institution\t\tCl Example 1\t28001\tMadrid\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_16));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).isEmpty();
    }

    @Test
    void parse_rowWithEmptyBic_recordIncludedWithNullBic() throws Exception {
        String content = HEADER
            + "ES1492\t\tES\tBNP Paribas Lease Group S.A. Sucursal en España\t\tCl Example 1\t28001\tMadrid\t"
            + "Credit institution S122\t\t\t\tY\tN\r\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_16));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat((Object) records.get(0).getBic()).isNull();
        assertThat(records.get(0).getBankCode()).isEqualTo("1492");
    }

}
