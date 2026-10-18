package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.spi.BankDataParseException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Unit tests for {@link BankDataLoaderAt}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderAtTest {

    private final BankDataLoaderAt loader = new BankDataLoaderAt();

    @Test
    void countryCode_returnsAT() {
        assertThat(loader.getCountryCode()).isEqualTo("AT");
    }

    @Test
    void parse_sampleFile_returnsAllRecords_andHandlesMissingBic() throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/raw/AT-sample.txt")) {
            records = loader.parse(in, "test-version");
        }

        assertThat(records).hasSize(3);
        assertThat(records).extracting(BankData::getBankCode).containsExactly("12000", "18170", "34923");

        BankData withoutBic = records.stream().filter(r -> r.getBankCode().equals("18170")).findFirst().orElseThrow(AssertionError::new);
        assertThat((Object) withoutBic.getBic()).isNull();

        BankData headOffice = records.stream().filter(r -> r.getBankCode().equals("12000")).findFirst().orElseThrow(AssertionError::new);
        assertThat(headOffice.getBankName()).isEqualTo("UniCredit Bank Austria AG");
        assertThat((Object) headOffice.getBic())
            .isNotNull()
            .hasToString("BKAUATWWXXX");
        assertThat(headOffice.getPostalCode()).isEqualTo("1020");
        assertThat(headOffice.getCity()).isEqualTo("Wien");
    }

    @Test
    void parse_shortRowsAndRowWithEmptyBankCode_areSkipped() throws Exception {
        String content = "Kennzeichen;Identnummer;Bankleitzahl;Institutsart;Sektor;Firmenbuchnummer;Bankenname;Strasse;PLZ;Ort;"
            + "Postadresse / Strasse;Postadresse / PLZ;Postadresse / Ort;Postfach;Bundesland;Telefon;Fax;E-Mail;SWIFT-Code\n"
            + "Hauptanstalt;1;;KI;;;Ohne Bankleitzahl;;1010;Wien;;;;;Wien;;;;\n"
            + "Hauptanstalt;2;12000;KI;;;UniCredit Bank Austria AG;;1020;Wien;;;;;Wien;;;;BKAUATWWXXX\n"
            + "Ende der Liste\n"
            + "\n";
        InputStream in = new ByteArrayInputStream(content.getBytes(ISO_8859_1));

        List<BankData> records = loader.parse(in, "test-version");

        assertThat(records).extracting(BankData::getBankCode).containsExactly("12000");
    }

    @Test
    void parse_sourceWithoutHeaderRow_throwsBankDataParseException() {
        // e.g. an HTML error page delivered instead of the CSV export
        InputStream in = new ByteArrayInputStream("<html><body>Wartungsarbeiten</body></html>\n".getBytes(ISO_8859_1));

        assertThatThrownBy(() -> loader.parse(in, "test-version"))
            .isInstanceOf(BankDataParseException.class)
            .hasMessageContaining("header row with a 'Bankleitzahl' column not found");
    }

}
