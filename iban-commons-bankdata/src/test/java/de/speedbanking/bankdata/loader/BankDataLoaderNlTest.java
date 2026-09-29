package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Unit tests for {@link BankDataLoaderNl}, run against a minimal XLSX package built at test time
 * (mirroring the real "BIC-lijst-NL.xlsx" layout: BIC;Identifier;Name) rather than a committed
 * binary fixture.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderNlTest {

    private final BankDataLoaderNl loader = new BankDataLoaderNl();

    @Test
    void countryCode_returnsNL() {
        assertThat(loader.getCountryCode()).isEqualTo("NL");
    }

    @Test
    void remoteSourceUri_returnsDefaultSourceUri() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderNl.DEFAULT_SOURCE_URI);
    }

    @Test
    void resolveActualSourceUri_landingPageHtml_extractsCurrentXlsxLink() {
        String html = "<html><body><a href=\"https://www.betaalvereniging.nl/wp-content/uploads/2025/11/"
            + "BIC-lijst-NL.xlsx\">Download</a></body></html>";
        URI landingPage = URI.create("https://www.betaalvereniging.nl/kennisbank/iban-en-bic/bic-code/");

        Optional<URI> resolved = loader.resolveActualSourceUri(landingPage, html.getBytes(UTF_8));

        assertThat(resolved).contains(URI.create("https://www.betaalvereniging.nl/wp-content/uploads/2025/11/BIC-lijst-NL.xlsx"));
    }

    @Test
    void resolveActualSourceUri_htmlWithoutMatchingLink_returnsEmpty() {
        String html = "<html><body>No matching link here.</body></html>";
        URI landingPage = URI.create("https://www.betaalvereniging.nl/kennisbank/iban-en-bic/bic-code/");

        Optional<URI> resolved = loader.resolveActualSourceUri(landingPage, html.getBytes(UTF_8));

        assertThat(resolved).isEmpty();
    }

    @Test
    void parse_sampleXlsx_skipsTitleAndHeaderRows_usesIdentifierAsBankCode_andTrimsTrailingSpaceInName() throws Exception {
        List<BankData> records;
        try (InputStream in = new ByteArrayInputStream(buildSampleXlsx())) {
            records = loader.parse(in, "test-version");
        }

        assertThat(records)
            .hasSize(3)
            .extracting(BankData::getBankCode).containsExactly("ABNA", "AEGO", "ANDL");

        BankData first = records.get(0);
        assertThat(first.getBic().toString()).isEqualTo("ABNANL2A");
        assertThat(first.getBankName()).isEqualTo("ABN AMRO BANK N.V.");
        assertThat(first.getPostalCode()).isNull();
        assertThat(first.getCity()).isNull();

        BankData trailingSpaceName = records.get(1);
        assertThat(trailingSpaceName.getBankName()).isEqualTo("AEGON BANK"); // trailing space in source, trimmed

        BankData noBic = records.get(2);
        assertThat((Object) noBic.getBic()).isNull(); // blank BIC cell is not a valid BIC
    }

    /**
     * Builds a minimal XLSX mirroring the real {@code BIC-lijst-NL.xlsx}: a single-cell title row,
     * then a header row, then data rows: BIC;Identifier;Name.
     */
    private static byte[] buildSampleXlsx() throws IOException {
        String sharedStringsXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"14\" uniqueCount=\"14\">"
            + "<si><t>BIC-lijst-NL | BIC-list-NL (Laatste update | last update 02-09-2026)</t></si>" // 0 (title row)
            + "<si><t>BIC</t></si>"                    // 1
            + "<si><t>Identifier</t></si>"             // 2
            + "<si><t>Betaaldienstverlener / Payment Service Provider</t></si>" // 3
            + "<si><t>ABNANL2A</t></si>"                // 4
            + "<si><t>ABNA</t></si>"                    // 5
            + "<si><t>ABN AMRO BANK N.V.</t></si>"      // 6
            + "<si><t>AEGONL2U</t></si>"                // 7
            + "<si><t>AEGO</t></si>"                    // 8
            + "<si><t>AEGON BANK </t></si>"              // 9 (trailing space, as published)
            + "<si><t>ANDLNL2A</t></si>"                // 10
            + "<si><t>ANDL</t></si>"                    // 11
            + "<si><t>ANADOLUBANK</t></si>"             // 12
            + "</sst>";

        String sheetXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
            + "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c></row>"
            + "<row r=\"2\">"
            + "<c r=\"A2\" t=\"s\"><v>1</v></c><c r=\"B2\" t=\"s\"><v>2</v></c><c r=\"C2\" t=\"s\"><v>3</v></c>"
            + "</row>"
            + "<row r=\"3\">"
            + "<c r=\"A3\" t=\"s\"><v>4</v></c><c r=\"B3\" t=\"s\"><v>5</v></c><c r=\"C3\" t=\"s\"><v>6</v></c>"
            + "</row>"
            + "<row r=\"4\">"
            + "<c r=\"A4\" t=\"s\"><v>7</v></c><c r=\"B4\" t=\"s\"><v>8</v></c><c r=\"C4\" t=\"s\"><v>9</v></c>"
            + "</row>"
            + "<row r=\"5\">"
            + "<c r=\"B5\" t=\"s\"><v>11</v></c><c r=\"C5\" t=\"s\"><v>12</v></c>"
            + "</row>"
            + "</sheetData></worksheet>";

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zip.write(sharedStringsXml.getBytes(UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            zip.write(sheetXml.getBytes(UTF_8));
            zip.closeEntry();
        }
        return buffer.toByteArray();
    }

}
