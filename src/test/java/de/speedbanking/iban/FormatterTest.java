package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;

/**
 * JUnit test class for {@link Formatter}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class FormatterTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructor_shouldThrowException() {
        TestUtil.assertConstructorIsPrivate(Formatter.class);
    }

    @DisplayName("Default format should correctly insert spaces for various IBAN lengths")
    @ParameterizedTest(name = "Format IBAN ''{0}'' -> ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "DE91100000000123456789          | DE91 1000 0000 0123 4567 89",
        "CH9300762011623852957           | CH93 0076 2011 6238 5295 7",
        "MT31MALT01100000000000000000123 | MT31 MALT 0110 0000 0000 0000 0000 123"
    })
    void format_default_shouldInsertCorrectSpaces(String normalizedIban, String expected) {
        String actual = Formatter.format(normalizedIban);
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Default format should return null for null, empty, or blank input")
    @ParameterizedTest(name = "Input: ''{0}'' -> Expected: ''null''")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "}) // whitespace strings
    void format_default_shouldHandleNullEmptyAndBlank(String input) {
        assertThat(Formatter.format(input)).isNull();
    }

    @DisplayName("Reformatting an already formatted string should return the input unchanged (ignoring surrounding whitespace)")
    @ParameterizedTest(name = "Reformat IBAN ''{0}'' -> ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "DE91 1000 0000 0123 4567 89 | DE91 1000 0000 0123 4567 89",
        "CH93 0076 2011 6238 5295 7 | CH93 0076 2011 6238 5295 7",
        // Test with existing spaces
        "MT84 MALT 0110 0001 2345 MTLC AST0 01S | MT84 MALT 0110 0001 2345 MTLC AST0 01S",
        // Test with leading/trailing spaces
        "' MT84 MALT 0110 0001 2345 MTLC AST0 01S ' | MT84 MALT 0110 0001 2345 MTLC AST0 01S"
    })
    void format_default_reformattedIbanShouldEqual(String input, String expected) {
        assertThat(Formatter.format(input)).isEqualTo(expected);
    }

    @DisplayName("Format simple string to kill infinite loop mutation (i--)")
    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    void format_shouldKillMutationInfiniteLoop() {
        String iban = "12345678901234567890"; // 20 chars
        String expected = "1234 5678 9012 3456 7890";

        assertThat(Formatter.format(iban, 4)).isEqualTo(expected);
    }

    @DisplayName("Format a very long string to kill mutation for Buffer initialization")
    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    void format_shouldKillMutationBufferInit() {
        String longInput = "12345678901234567890123456789012345678901234567890"; // 50 chars
        String expected = "12345 67890 12345 67890 12345 67890 12345 67890 12345 67890";

        assertThat(Formatter.format(longInput, 5)).isEqualTo(expected);
    }

    @DisplayName("Formatter should correctly count characters and group them when group size is changed")
    @Test
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    void format_shouldKillOutCountDecrementMutation() {
        // 12345 67890 12345 67890
        String input = "12345678901234567890";
        int groupSize = 5;

        // Die Mutation (outCount--) würde das erste Leerzeichen (nach '5') falsch setzen,
        // da outCount nach 5 Zeichen nur 3 oder 4 anstatt 5 wäre.
        String expected = "12345 67890 12345 67890";

        String actual = Formatter.format(input, groupSize);
        assertThat(actual).isEqualTo(expected);
    }

    // --- Format with Custom Group Size ---

    @DisplayName("Custom format should use specified group size correctly")
    @ParameterizedTest(name = "Format ''{0}'' with groupSize={1} -> ''{2}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // 5-char groups
        "PS92PALS000000000400123456702 | 5  | PS92P ALS00 00000 00400 12345 6702",
        // 10-char groups
        "IR710570029971601460641001    | 10 | IR71057002 9971601460 641001",
        // 2-char groups
        "MZ59000301080016367102371     | 2  | MZ 59 00 03 01 08 00 16 36 71 02 37 1"
    })
    void format_custom_shouldUseSpecifiedGroupSize(String normalizedIban, int groupSize, String expected) {
        assertThat(Formatter.format(normalizedIban, groupSize)).isEqualTo(expected);
    }

    @DisplayName("Custom format should not add spaces when groupSize is greater than or equal to IBAN length")
    @ParameterizedTest(name = "Format ''{0}'' with groupSize={1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "NO9386011117947 | 15", // groupSize == length (15)
        "NO9386011117947 | 16"  // groupSize > Length (15)
    })
    void format_custom_groupSizeGreaterOrEqualLength_shouldNotAddSpaces(String iban, int groupSize) {
        String actual = Formatter.format(iban, groupSize);
        assertThat(actual).isEqualTo(iban);
    }

    @DisplayName("Custom format should throw IllegalArgumentException on non-positive groupSize (0 or negative)")
    @ParameterizedTest(name = "Group size: {0}")
    @ValueSource(ints = {0, -1, -100})
    void format_custom_shouldThrowOnNonPositiveGroupSize(int invalidGroupSize) {
        String iban = "DE911000";
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Formatter.format(iban, invalidGroupSize))
            .withMessage("Group size must be a positive integer");
    }

}

