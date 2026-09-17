package de.speedbanking.bankdata.tool;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.bankdata.BankDataRegistry;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/**
 * Unit tests for {@link BundledDataRefreshTool}'s argument handling (usage/help output, country
 * code resolution), which does not require any network access. {@code main(String[])} itself is
 * not exercised here, since a real run downloads live upstream data and overwrites the bundled
 * resource files, unsuitable for an automated unit test.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BundledDataRefreshToolTest {

    @Test
    void isHelpRequested_shortFlag_returnsTrue() {
        assertThat(Arrays.stream(new String[] {"-h"}).anyMatch(arg -> "-h".equals(arg) || "--help".equals(arg))).isTrue();
    }

    @Test
    void isHelpRequested_longFlag_returnsTrue() {
        assertThat(Arrays.stream(new String[] {"--help"}).anyMatch(arg -> "-h".equals(arg) || "--help".equals(arg))).isTrue();
    }

    @Test
    void isHelpRequested_countryCodesOnly_returnsFalse() {
        assertThat(Arrays.stream(new String[] {"CZ", "BE"}).anyMatch(arg -> "-h".equals(arg) || "--help".equals(arg))).isFalse();
    }

    @Test
    void isHelpRequested_noArgs_returnsFalse() {
        assertThat(Arrays.stream(new String[0]).anyMatch(arg -> "-h".equals(arg) || "--help".equals(arg))).isFalse();
    }

    @Test
    void printUsage_mentionsInvocationAndSupportedCountries() throws UnsupportedEncodingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        BundledDataRefreshTool.printUsage(new PrintStream(buffer, true, StandardCharsets.UTF_8.name()));

        String usage = buffer.toString(StandardCharsets.UTF_8.name());
        assertThat(usage).contains("Usage: BundledDataRefreshTool")
                          .contains("exec:java@refresh-bundled-bankdata")
                          .contains("DE");
    }

    @Test
    void resolveCountryCodes_noArgs_returnsAllSupportedCountriesSorted() {
        Set<String> countryCodes = BundledDataRefreshTool.resolveCountryCodes(new String[0]);

        assertThat(countryCodes).containsExactlyElementsOf(BankDataRegistry.getSupportedCountryCodes());
    }

    @Test
    void resolveCountryCodes_validLowercaseCodes_returnsUppercased() {
        Set<String> countryCodes = BundledDataRefreshTool.resolveCountryCodes(new String[] {"cz", "be"});

        assertThat(countryCodes).containsExactly("CZ", "BE");
    }

    @Test
    void resolveCountryCodes_unknownCode_returnsNull() {
        Set<String> countryCodes = BundledDataRefreshTool.resolveCountryCodes(new String[] {"CZ", "XX"});

        assertThat(countryCodes).isEmpty();
    }

}
