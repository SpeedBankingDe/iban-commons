package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Unit tests for {@link MinimalXlsxReader}, run against a minimal XLSX package built at test time
 * (rather than a committed binary fixture) so the expected cell values are readable directly in
 * the test source.
 */
@SuppressWarnings("checkstyle:MethodName")
final class MinimalXlsxReaderTest {

    private static final String SHEET_XML_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>";
    private static final String SHEET_XML_FOOTER = "</sheetData></worksheet>";

    @Test
    void read_returnsSharedStrings_rawNumericText_andGapFilledColumns() throws Exception {
        String sharedStringsXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"3\" uniqueCount=\"3\">"
            + "<si><t>Code</t></si>"
            + "<si><t>Name</t></si>"
            + "<si><t>Test Bank</t></si>"
            + "</sst>";

        String sheetXml = SHEET_XML_HEADER
            + "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c></row>"
            + "<row r=\"2\"><c r=\"A2\"><v>042</v></c><c r=\"B2\" t=\"s\"><v>2</v></c></row>"
            + "<row r=\"3\"><c r=\"C3\" t=\"s\"><v>2</v></c></row>"
            + SHEET_XML_FOOTER;

        byte[] xlsx = buildXlsx(sharedStringsXml, sheetXml);

        List<String[]> rows;
        try (ByteArrayInputStream in = new ByteArrayInputStream(xlsx)) {
            rows = MinimalXlsxReader.read(in);
        }

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).containsExactly("Code", "Name");
        assertThat(rows.get(1)).containsExactly("042", "Test Bank");
        assertThat(rows.get(2)).containsExactly("", "", "Test Bank");
    }

    @Test
    void read_withoutSharedStringsPart_stillParsesRawCellValues() throws Exception {
        String sheetXml = SHEET_XML_HEADER
            + "<row r=\"1\"><c r=\"A1\"><v>123</v></c></row>"
            + SHEET_XML_FOOTER;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            zip.write(sheetXml.getBytes(UTF_8));
            zip.closeEntry();
        }

        List<String[]> rows;
        try (ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray())) {
            rows = MinimalXlsxReader.read(in);
        }

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("123");
    }

    @Test
    void read_missingSheetPart_throwsIOException() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
            zip.write("<workbook/>".getBytes(UTF_8));
            zip.closeEntry();
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }

        assertThatThrownBy(() -> MinimalXlsxReader.read(new ByteArrayInputStream(buffer.toByteArray())))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("sheet1.xml");
    }

    private static byte[] buildXlsx(String sharedStringsXml, String sheetXml) throws IOException {
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
