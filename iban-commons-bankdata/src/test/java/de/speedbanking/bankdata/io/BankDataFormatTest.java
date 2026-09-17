package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bic.Bic;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link BankDataFormat}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataFormatTest {

    @Test
    void writeThenRead_roundTrips() throws Exception {
        BankData withBic = new BankData("DE", "10000000", Bic.of("MARKDEF1100"), "Bundesbank Berlin", "10117", "Berlin", "v1");
        BankData withoutOptionalFields = new BankData("DE", "20000000", null, "Musterbank", null, null, "v1");
        List<BankData> records = Arrays.asList(withBic, withoutOptionalFields);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BankDataFormat.write(out, records);

        List<BankData> readBack = BankDataFormat.read(new ByteArrayInputStream(out.toByteArray()), UTF_8, "DE", "v1");

        assertThat(readBack).containsExactly(withBic, withoutOptionalFields);
    }

    @Test
    void write_alwaysIncludesHeaderRow() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BankDataFormat.write(out, emptyList());

        String content = new String(out.toByteArray(), UTF_8);

        assertThat(content).isEqualTo("%s\n", BankDataFormat.HEADER_LINE);
    }

    @Test
    void read_emptyStream_returnsEmptyList() throws Exception {
        List<BankData> records = BankDataFormat.read(
            new ByteArrayInputStream(new byte[0]), UTF_8, "DE", "v1");

        assertThat(records).isEmpty();
    }

    @Test
    void read_skipsHeaderRow() throws Exception {
        String content = BankDataFormat.HEADER_LINE + "\n"
            + "10000000;;MARKDEF1100;Bundesbank;10117;Berlin;\n";

        List<BankData> records = BankDataFormat.read(
            new ByteArrayInputStream(content.getBytes(UTF_8)), UTF_8, "DE", "v1");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBankCode()).isEqualTo("10000000");
    }

    @Test
    void writeThenRead_branchCode_roundTrips() throws Exception {
        BankData withBranch = new BankData("PL", "101", "0000", Bic.of("NBPLPLPWXXX"), "NBP", null, null, "v1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BankDataFormat.write(out, singletonList(withBranch));

        List<BankData> readBack = BankDataFormat.read(
            new ByteArrayInputStream(out.toByteArray()), UTF_8, "PL", "v1");

        assertThat(readBack).containsExactly(withBranch);
        assertThat(readBack.get(0).getBranchCode()).isEqualTo("0000");
        assertThat(readBack.get(0).getKey()).isEqualTo("1010000");
    }

    @Test
    void writeThenRead_bankNameContainingComma_notQuoted() throws Exception {
        // a comma is not this format's separator (semicolon is), so it never forces quoting
        BankData record = new BankData("DE", "30000000", null, "Musterbank, AG", null, null, "v1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BankDataFormat.write(out, singletonList(record));
        String content = new String(out.toByteArray(), UTF_8);

        assertThat(content).contains("Musterbank, AG").doesNotContain("\"Musterbank, AG\"");

        List<BankData> readBack = BankDataFormat.read(
            new ByteArrayInputStream(out.toByteArray()), UTF_8, "DE", "v1");

        assertThat(readBack).containsExactly(record);
        assertThat(readBack.get(0).getBankName()).isEqualTo("Musterbank, AG");
    }

    @Test
    void writeThenRead_bankNameContainingSemicolon_quotedAndRoundTrips() throws Exception {
        BankData record = new BankData("DE", "31000000", null, "Musterbank; Zweigstelle Nord", null, null, "v1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BankDataFormat.write(out, singletonList(record));
        String content = new String(out.toByteArray(), UTF_8);

        assertThat(content).contains("\"Musterbank; Zweigstelle Nord\"");

        List<BankData> readBack = BankDataFormat.read(
            new ByteArrayInputStream(out.toByteArray()), UTF_8, "DE", "v1");

        assertThat(readBack).containsExactly(record);
        assertThat(readBack.get(0).getBankName()).isEqualTo("Musterbank; Zweigstelle Nord");
    }

    @Test
    void writeThenRead_bankNameContainingQuoteAndNewline_roundTrips() throws Exception {
        BankData record = new BankData("DE", "40000000", null, "Muster\"bank\"\nAG", null, null, "v1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BankDataFormat.write(out, singletonList(record));

        List<BankData> readBack = BankDataFormat.read(
            new ByteArrayInputStream(out.toByteArray()), UTF_8, "DE", "v1");

        assertThat(readBack).containsExactly(record);
    }

}
