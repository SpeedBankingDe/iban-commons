package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.spi.BankDataParseException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link BankDataLoaderDe}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderDeTest {

    private final BankDataLoaderDe loader = new BankDataLoaderDe();

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    @Test
    void countryCode_returnsDE() {
        assertThat(loader.getCountryCode()).isEqualTo("DE");
    }

    @Test
    void remoteSourceUri_returnsDefaultSourceUri() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderDe.DEFAULT_SOURCE_URI);
    }

    @Test
    void resolveActualSourceUri_landingPageHtml_extractsCurrentCsvLink() {
        String html = "<html><body><a href=\"/resource/blob/926192/2feadedd079726ffbe1eedaf6c74ff76/"
            + "472B63F073F071307366337C94F8C870/blz-aktuell-csv-data.csv\" target=\"_blank\">CSV</a></body></html>";
        URI landingPage = URI.create("https://www.bundesbank.de/de/aufgaben/unbarer-zahlungsverkehr/serviceangebot/"
            + "bankleitzahlen/download-bankleitzahlen-602592");

        Optional<URI> resolved = loader.resolveActualSourceUri(landingPage, html.getBytes(UTF_8));

        assertThat(resolved).contains(URI.create("https://www.bundesbank.de/resource/blob/926192/"
            + "2feadedd079726ffbe1eedaf6c74ff76/472B63F073F071307366337C94F8C870/blz-aktuell-csv-data.csv"));
    }

    @Test
    void resolveActualSourceUri_htmlWithoutMatchingLink_returnsEmpty() {
        String html = "<html><body>No matching link here.</body></html>";
        URI landingPage = URI.create("https://www.bundesbank.de/de/aufgaben/unbarer-zahlungsverkehr/serviceangebot/"
            + "bankleitzahlen/download-bankleitzahlen-602592");

        Optional<URI> resolved = loader.resolveActualSourceUri(landingPage, html.getBytes(UTF_8));

        assertThat(resolved).isEmpty();
    }

    @Test
    void remoteSourceCharset_returnsWindows1252() {
        assertThat(loader.getRemoteSourceCharset()).isEqualTo(WINDOWS_1252);
    }

    @Test
    void parse_rowWithTooFewColumns_throwsBankDataParseException() {
        String content = "Bankleitzahl;Merkmal;Bezeichnung;PLZ;Ort;Kurzbezeichnung;PAN;BIC;Pruefzifferberechnungsmethode;"
            + "Datensatznummer;Aenderungskennzeichen;Bankleitzahlloeschung;Nachfolge-Bankleitzahl\n"
            + "\"10000000\";\"1\";\"Bundesbank Berlin\"\n"; // far fewer than the required 13 fields
        InputStream in = new ByteArrayInputStream(content.getBytes(WINDOWS_1252));

        assertThatThrownBy(() -> loader.parse(in, "test-version"))
            .isInstanceOf(BankDataParseException.class)
            .hasMessageContaining("Malformed German BLZ row");
    }

    @Test
    void parse_bicPlaceholder_recordIncludedWithNullBic() throws Exception {
        String content = "Bankleitzahl;Merkmal;Bezeichnung;PLZ;Ort;Kurzbezeichnung;PAN;BIC;Pruefzifferberechnungsmethode;"
            + "Datensatznummer;Aenderungskennzeichen;Bankleitzahlloeschung;Nachfolge-Bankleitzahl\n"
            + "\"10000000\";\"1\";\"Bundesbank Berlin\";\"10117\";\"Berlin\";\"BBk Berlin\";\"20100\";"
            + "\"XXXXXXXX\";\"09\";\"011380\";\"U\";\"0\";\"00000000\"\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(WINDOWS_1252));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat((Object) records.get(0).getBic()).isNull();
    }

    @Test
    void parse_unparsableBic_recordIncludedWithNullBic() throws Exception {
        String content = "Bankleitzahl;Merkmal;Bezeichnung;PLZ;Ort;Kurzbezeichnung;PAN;BIC;Pruefzifferberechnungsmethode;"
            + "Datensatznummer;Aenderungskennzeichen;Bankleitzahlloeschung;Nachfolge-Bankleitzahl\n"
            + "\"10000000\";\"1\";\"Bundesbank Berlin\";\"10117\";\"Berlin\";\"BBk Berlin\";\"20100\";"
            + "\"NOT-A-VALID-BIC\";\"09\";\"011380\";\"U\";\"0\";\"00000000\"\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(WINDOWS_1252));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).hasSize(1);
        assertThat((Object) records.get(0).getBic()).isNull();
    }

    @Test
    void parse_sampleFile_skipsHeaderDeletedAndBiclessRows_andHandlesQuotingAndUmlauts() throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/raw/DE-sample.txt")) {
            records = loader.parse(in, "test-version");
        }

        // header row, the empty-BIC row (20000000), and the deleted row (25000000) are all excluded
        assertThat(records).extracting(BankData::getBankCode)
            .containsExactlyInAnyOrder("10000000", "10010123", "20000000", "30000000");

        BankData mainOffice = findByBankCode(records, "10000000");
        assertThat(mainOffice.getBankName()).isEqualTo("Bundesbank Berlin");
        assertThat(mainOffice.getPostalCode()).isEqualTo("10117");
        assertThat(mainOffice.getCity()).isEqualTo("Berlin");

        BankData embeddedComma = findByBankCode(records, "10010123");
        assertThat(embeddedComma.getBankName()).isEqualTo("Qonto Zweigniederlassung");
        assertThat((Object) embeddedComma.getBic()).isNotNull();
        assertThat(embeddedComma.getBic().toString()).isEqualTo("QNTODEB2XXX");

        BankData withUmlaut = findByBankCode(records, "30000000");
        assertThat(withUmlaut.getBankName()).isEqualTo("Müllerbank München AG");
        assertThat(withUmlaut.getCity()).isEqualTo("München");

        assertThat(records).allMatch(r -> r.getSourceVersion().equals("test-version"));
    }

    private static BankData findByBankCode(List<BankData> records, String bankCode) {
        return records.stream()
            .filter(r -> r.getBankCode().equals(bankCode))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No record found for bank code " + bankCode));
    }

}
