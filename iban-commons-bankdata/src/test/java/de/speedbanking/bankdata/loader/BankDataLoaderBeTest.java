package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Unit tests for {@link BankDataLoaderBe}, run against a minimal XLSX package built at test time
 * (mirroring the "full_list_current.xlsx" layout: code;Biccode;Name Dutch;Name French;Name German;
 * Name English) rather than a committed binary fixture.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderBeTest {

    private final BankDataLoaderBe loader = new BankDataLoaderBe();

    @Test
    void countryCode_returnsBE() {
        assertThat(loader.getCountryCode()).isEqualTo("BE");
    }

    @Test
    void parse_sampleXlsx_skipsTitleAndHeaderRows_prefersEnglishName_stripsBicSpaces_andHandlesUnassignedCode() throws Exception {
        List<BankData> records;
        try (InputStream in = new ByteArrayInputStream(buildSampleXlsx())) {
            records = loader.parse(in, "test-version");
        }

        assertThat(records)
            .hasSize(4)
            .extracting(BankData::getBankCode).containsExactly("001", "068", "171", "999");

        BankData first = records.get(0);
        assertThat(first.getBic().toString()).isEqualTo("GEBABEBBXXX"); // raw source value has embedded spaces, stripped
        assertThat(first.getBankName()).isEqualTo("Musterbank Brussels NV"); // English name preferred
        assertThat(first.getPostalCode()).isNull();
        assertThat(first.getCity()).isNull();

        BankData dutchFallback = records.get(1);
        assertThat(dutchFallback.getBankName()).isEqualTo("Musterbank Gent NV"); // no English name: falls back to Dutch

        BankData frenchOnly = records.stream().filter(r -> r.getBankCode().equals("171")).findFirst().orElseThrow(AssertionError::new);
        assertThat(frenchOnly.getBankName()).isEqualTo("Banque CPH"); // Dutch and English both blank: falls back to French

        BankData unassigned = records.stream().filter(r -> r.getBankCode().equals("999")).findFirst().orElseThrow(AssertionError::new);
        assertThat((Object) unassigned.getBic()).isNull(); // "VRIJ" placeholder for an unassigned code is not a valid BIC
    }

    /**
     * Builds a minimal XLSX mirroring the real {@code full_list_current.xlsx}: a single-cell title
     * row, then a header row, then data rows: code;Biccode;Name Dutch;Name French;Name German;Name
     * English.
     */
    private static byte[] buildSampleXlsx() throws IOException {
        String sharedStringsXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"18\" uniqueCount=\"18\">"
            + "<si><t>Version 01/09/2026</t></si>" // 0 (title row)
            + "<si><t>Code</t></si>"          // 1
            + "<si><t>Biccode</t></si>"       // 2
            + "<si><t>Name Dutch</t></si>"    // 3
            + "<si><t>Name French</t></si>"   // 4
            + "<si><t>Name German</t></si>"   // 5
            + "<si><t>Name English</t></si>"  // 6
            + "<si><t>GEBA BEBB XXX</t></si>" // 7 (BIC with embedded spaces, as published)
            + "<si><t>Musterbank Brussel NV</t></si>"   // 8 (Dutch)
            + "<si><t>Musterbank Brussels NV</t></si>"  // 9 (English)
            + "<si><t>GKCCBEBBXXX</t></si>"   // 10
            + "<si><t>Musterbank Gent NV</t></si>"      // 11 (Dutch, no English)
            + "<si><t>VRIJ</t></si>"          // 12 ("free"/unassigned placeholder, not a real BIC)
            + "<si><t>CPHB BE 75</t></si>"    // 13 (BIC with embedded spaces)
            + "<si><t>Banque CPH</t></si>"    // 14 (French only, no Dutch/English)
            + "</sst>";

        String sheetXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
            + "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c></row>"
            + "<row r=\"2\">"
            + "<c r=\"A2\" t=\"s\"><v>1</v></c><c r=\"B2\" t=\"s\"><v>2</v></c><c r=\"C2\" t=\"s\"><v>3</v></c>"
            + "<c r=\"D2\" t=\"s\"><v>4</v></c><c r=\"E2\" t=\"s\"><v>5</v></c><c r=\"F2\" t=\"s\"><v>6</v></c>"
            + "</row>"
            + "<row r=\"3\">"
            + "<c r=\"A3\"><v>001</v></c><c r=\"B3\" t=\"s\"><v>7</v></c><c r=\"C3\" t=\"s\"><v>8</v></c>"
            + "<c r=\"F3\" t=\"s\"><v>9</v></c>"
            + "</row>"
            + "<row r=\"4\">"
            + "<c r=\"A4\"><v>068</v></c><c r=\"B4\" t=\"s\"><v>10</v></c><c r=\"C4\" t=\"s\"><v>11</v></c>"
            + "</row>"
            + "<row r=\"5\">"
            + "<c r=\"A5\"><v>171</v></c><c r=\"B5\" t=\"s\"><v>13</v></c><c r=\"D5\" t=\"s\"><v>14</v></c>"
            + "</row>"
            + "<row r=\"6\">"
            + "<c r=\"A6\"><v>999</v></c><c r=\"B6\" t=\"s\"><v>12</v></c><c r=\"C6\" t=\"s\"><v>12</v></c>"
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
