package de.speedbanking.iban;

import static de.speedbanking.iban.IbanValidationError.EMPTY;
import static de.speedbanking.iban.IbanValidationError.ILLEGAL_CHARACTERS;
import static de.speedbanking.iban.IbanValidationError.INCORRECT_LENGTH;
import static de.speedbanking.iban.IbanValidationError.INCORRECT_LENGTH_COUNTRY;
import static de.speedbanking.iban.IbanValidationError.INVALID_CHECKSUM;
import static de.speedbanking.iban.IbanValidationError.INVALID_CHECK_DIGITS;
import static de.speedbanking.iban.IbanValidationError.INVALID_COUNTRY;
import static de.speedbanking.iban.IbanValidationError.INVALID_STRUCTURE;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

/**
 * JUnit test class for {@link IbanValidator}.
 */
@SuppressWarnings("checkstyle:MethodName")
class IbanValidatorTest extends org.assertj.core.api.Assertions {

    // A valid German IBAN for positive tests
    private static final String VALID_RAW_DE        = "DE91 1000 0000 0123 4567 89";
    private static final String VALID_NORM_DE       = "DE91100000000123456789";

    // A valid Luxembourgish IBAN (20 chars, contains letters in BBAN)
    private static final String VALID_NORM_LU       = "LU280019400644750000";

    // An invalid IBAN with a wrong checksum (Mod 97)
    private static final String INVALID_CHECKSUM_DE = "DE90100000000123456789"; // DE91 -> DE90

    @BeforeEach
    void setup() {
        // ensures the ThreadLocal error reason is reset before each test
        IbanValidator.setLastReason(null);
    }

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructor_shouldThrowException() {
        TestUtil.assertConstructorIsPrivate(IbanValidator.class);
    }

    @DisplayName("validate should return success object for valid raw IBAN")
    @Test
    void validate_shouldBeValid() {
        IbanValidationSuccess result = IbanValidator.validate(VALID_RAW_DE, true);
        assertThat(result)
            .isNotNull()
            .extracting(t -> t.normIban, t -> t.countryData)
            .containsExactly(VALID_NORM_DE, IbanRegistry.DE);
        assertThat(IbanValidator.getLastReason()).isNull();
    }

    @DisplayName("validate should return null and set error on null or empty input")
    @ParameterizedTest(name = "Input: ''{0}'' -> Expected: EMPTY")
    @NullAndEmptySource
    void validate_shouldFailOnNullOrEmpty(String input) {
        IbanValidationSuccess result = IbanValidator.validate(input, false);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(EMPTY);
    }

    @DisplayName("validate should return null and set error on unsupported country code (e.g., AA)")
    @Test
    void validate_shouldFailOnUnsupportedCountry() {
        IbanValidationSuccess result = IbanValidator.validate("AA99AA9999999999999999", false);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_COUNTRY);
    }

    @DisplayName("validate should return null and set error on invalid country code (lowercase)")
    @Test
    void validate_shouldFailOnInvalidCountryCode() {
        IbanValidationSuccess result = IbanValidator.validate("de91100000000123456789", false);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(ILLEGAL_CHARACTERS);
    }

    @DisplayName("validate should return null and set error on illegal characters in BBAN")
    @Test
    void validate_shouldFailOnIllegalCharactersInBBAN() {
        IbanValidationSuccess result1 = IbanValidator.validate("DE91.100000000123456789", false);
        assertThat(result1).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(ILLEGAL_CHARACTERS);

        // Reset for second test
        IbanValidator.setLastReason(null);

        IbanValidationSuccess result2 = IbanValidator.validate("DE51XXXXXXXXXXXXXXXXXX", false);
        assertThat(result2).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_STRUCTURE);

        IbanValidator.setLastReason(null);

        IbanValidationSuccess result3 = IbanValidator.validate("DE5110000000012345678!", false);
        assertThat(result3).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(ILLEGAL_CHARACTERS);
    }

    @DisplayName("validate should fail on non-digit check digits")
    @Test
    void validate_shouldFailOnInvalidCheckDigits() {
        // 'A' instead of a digit at position 4 (check digit), containing spaces (L161)
        IbanValidationSuccess result = IbanValidator.validate("DE9A 1000 0000 0123 4567 89", true);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_CHECK_DIGITS);
    }

    @DisplayName("validate should fail if length less than min iban length")
    @Test
    void validate_shouldFailOnMaxIbanLengthExceeded() {
        IbanValidationSuccess result = IbanValidator.validate("DE73", false);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH);
    }

    @DisplayName("validate should return success object for valid normalized IBAN (DE)")
    @Test
    void validate_shouldBeValid_DE() {
        IbanValidationSuccess result = IbanValidator.validate(VALID_NORM_DE);
        assertThat(result).isNotNull();
        assertThat(result.normIban).isEqualTo(VALID_NORM_DE);
        assertThat(IbanValidator.getLastReason()).isNull();
    }

    @DisplayName("validate should return success object for valid normalized IBAN (LU)")
    @Test
    void validate_shouldBeValid_LU() {
        IbanValidationSuccess result = IbanValidator.validate(VALID_NORM_LU);
        assertThat(result).isNotNull();
    }

    @DisplayName("validate should return null and set error on null input")
    @Test
    void validate_shouldFailOnNull() {
        IbanValidationSuccess result = IbanValidator.validate(null);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(EMPTY);
    }

    @DisplayName("validate should return null and set error on short length")
    @Test
    void validate_shouldFailOnTooShort() {
        // < MIN_IBAN_LENGTH (5 chars)
        assertThat(IbanValidator.validate("DE9")).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH);

        IbanValidator.setLastReason(null);

        // < country specific length (22 chars)
        assertThat(IbanValidator.validate("DE9110000000000000000")).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH_COUNTRY);
    }

    @DisplayName("validate should return null and set error on incorrect length for country (e.g., DE with 21 chars)")
    @Test
    void validate_shouldFailOnIncorrectLengthForCountry() {
        // DE expects 22 characters, here 21
        IbanValidationSuccess result = IbanValidator.validate("DE9110000000012345678");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH_COUNTRY);
    }

    @DisplayName("validate should return null and set error on non-digit check digits")
    @ParameterizedTest(name = "Input: ''{0}''")
    @ValueSource(strings = {"DE9A100000000123456789", "DEA9100000000123456789", "DEAA100000000123456789"})
    void validate_shouldFailOnInvalidCheckDigits(String input) {
        IbanValidationSuccess result = IbanValidator.validate(input);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_CHECK_DIGITS);
    }

    @DisplayName("validate should return null and set error on invalid BBAN structure (country specific)")
    @Test
    void validate_shouldFailOnInvalidStructure() {
        // DE BBAN (Bank Code and Account Number) must only contain digits
        String invalidStructure = "DE91ABCDEFGHIJKLMNOPQR";
        IbanValidationSuccess result = IbanValidator.validate(invalidStructure, false);

        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_STRUCTURE);
    }

    @DisplayName("validate should return null and set error on invalid Mod 97 checksum")
    @Test
    void validate_shouldFailOnInvalidChecksum() {
        IbanValidationSuccess result = IbanValidator.validate(INVALID_CHECKSUM_DE, false);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_CHECKSUM);
    }

    /**
     * Tests {@link IbanValidator#isMod97Valid(CharSequence)} using various IBAN strings.
     */
    @DisplayName("Modulo 97 Validation")
    @ParameterizedTest(name = "[{index}] IBAN: {0} -> expected: {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // Valid case (long string, forces intermediate modulo); uses a known, valid SA IBAN structure/value
        "SA0320000001000201861989 | true",

        // Invalid Checksum case (long string, forces intermediate modulo)
        // Checksum SA44 is replaced by SA00 (invalid)
        "SA0020000001000201861989 | false",

        // Invalid Checksum case (intentionally modified original example)
        "SA00112233445566778899AABBCCDD | false",

        // Valid case (standard length, DE example)
        "DE91100000000123456789 | true",

        // invalid Checksum (DE example, Checksum DE91 replaced by DE00)
        "DE00100000000123456789 | false",

        // invalid length/format (does not pass the check)
        "SA00SHORT | false",

        // invalid characters
        "invalid_chars | false",

        // Null/Empty handling (assuming isMod97Valid handles null/empty gracefully or fails early)
        "(null) | false",
        "'' | false",  // empty string
        "'  ' | false" // whitespace
    })
    void isMod97Valid_shouldHandleAllCases(String ibanString, boolean expectedValidity) {
        boolean isValid = IbanValidator.isMod97Valid(ibanString);

        assertThat(isValid)
            .withFailMessage("Validation failed for IBAN: %s (expected: %s, actual: %s)",
                             ibanString, expectedValidity, isValid)
            .isEqualTo(expectedValidity);
    }

    @DisplayName("isValid should return true for valid IBAN")
    @Test
    void isValid_shouldReturnTrue() {
        // uses the simple API, must not set any error reason
        assertThat(IbanValidator.isValid(VALID_NORM_DE)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] Validating formatted IBAN: {0}")
    @ValueSource(strings = {
        "DE91 1000 0000 0123 4567 89",
        "GB29 NWBK 6016 1331 9268 19",
        "PL 61 10 90 10 14 00 00 07 12 19 81 28 74"
    })
    @DisplayName("isValid should return true for valid formatted (spaced) IBANs")
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES)
    void isValid_shouldReturnTrueForFormattedIban(String formattedIban) {
        try {
            IbanConfig.ALLOW_SPACE.enable();

            assertThat(IbanValidator.isValid(formattedIban))
                .as("IBAN %s should be recognized as valid", formattedIban)
                .isTrue();

            IbanConfig.ALLOW_LOWERCASE.enable();

            String lowercaseIban = formattedIban.toLowerCase(Locale.ROOT);

            assertThat(IbanValidator.isValid(lowercaseIban))
                .as("IBAN %s should be recognized as valid", lowercaseIban)
                .isTrue();

        } finally {
            IbanConfig.ALLOW_SPACE.reset();
            IbanConfig.ALLOW_LOWERCASE.reset();
        }
    }

    /**
     * Tests that {@code setLastReason(null)} is called upon successful validation.
     */
    @DisplayName("validate should reset last reason on success")
    @Test
    void validate_shouldResetLastReasonOnSuccess() {
        // 1. Trigger an error to set LAST_REASON
        IbanValidator.validate(INVALID_CHECKSUM_DE, false);
        assertThat(IbanValidator.getLastReason()).isNotNull();

        // 2. Perform a successful validation call, which internally executes setLastReason(null)
        IbanValidationSuccess result = IbanValidator.validate(VALID_RAW_DE, true);
        assertThat(result).isNotNull();

        // 3. Assert that LAST_REASON has been reset to null
        assertThat(IbanValidator.getLastReason()).isNull();
    }

    /**
     * Tests the {@code calculateMod97} method with known IBAN structures (containing "00" placeholders
     * for check digits) to ensure the calculated remainder matches the required value R, where
     * the actual check digits CD = 98 - R.
     *
     * @param ibanWithZeroCheckDigits the IBAN with "00" in the check digit position
     * @param expectedRemainder       the remainder R that must be produced by the modulo 97 calculation
     */
    @ParameterizedTest(name = "[{index}] IBAN ''{0}'' should yield Mod 97 Remainder {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // IBAN (00 CD)              | Expected Remainder R (for CD = 98 - R)
        "DE00370400440532013000      | 9",  // result 9 -> CD 91 (DE91...)
        "NL00ABNA0417164300          | 7",  // result 7 -> CD 97 (NL91...)
        "FR0020041010050500013M02606 | 84", // result 84 -> CD 15 (FR14...)
        "TR000006100519786457841326  | 65"  // longer IBAN structure (implicitly tests intermediate modulo)
    })
    void testCalculateMod97ValidIbanFormat(CharSequence ibanWithZeroCheckDigits, int expectedRemainder) {
        int mod97 = IbanValidator.calculateMod97(ibanWithZeroCheckDigits);
        assertThat(mod97).isEqualTo(expectedRemainder);
    }

    /**
     * Tests that the calculation method correctly return {@value IbanValidator#INVALID_MOD97}
     * when encountering illegal characters (those not in A-Z or 0-9) during the numeric conversion.
     *
     * @param ibanInput the input string containing illegal characters
     */
    @ParameterizedTest(name = "[{index}] Invalid character in ''{0}'' returns " + IbanValidator.INVALID_MOD97)
    @ValueSource(strings = {
        "DE0010000000012345678/", // forward slash
        "DE0010000000012345678-", // hyphen
        "DE0010000000012345678 ", // space (assuming input is normalized, but guards against it)
        "DE0010000000012345678ß"  // German specific non-alphanumeric character
    })
    void testCalculateMod97WithIllegalCharactersShouldReturnNegativeOne(String ibanInput) {
        assertThat(IbanValidator.calculateMod97(ibanInput)).isEqualTo(IbanValidator.INVALID_MOD97);
    }

    /**
     * Tests that {@code fixCheckDigits} correctly manipulates the StringBuilder.
     */
    @DisplayName("Should correctly fix check digits, overwriting initial placeholders")
    @ParameterizedTest(name = "IBAN with initial check digit ''{0}'' should result in ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "DE | 11 | 1000000001234567890123 | 23",
        "DE | 99 | 1000000001234567890123 | 23"
    })
    @SuppressWarnings("UnnecessaryStringBuilder")
    void testFixCheckDigits(String countryCode, String initialCheckDigits, String bban, String expectedCheckDigits) {
        StringBuilder ibanBuilder = new StringBuilder(countryCode);
        ibanBuilder.append(initialCheckDigits);
        ibanBuilder.append(bban);

        StringBuilder resultBuilder1 = IbanValidator.fixCheckDigits(ibanBuilder);

        assertThat(resultBuilder1)
            .isSameAs(ibanBuilder)
            .startsWith("DE" + expectedCheckDigits)
            .endsWith(bban);

        StringBuilder resultBuilder2 = IbanValidator.fixCheckDigits(ibanBuilder.toString());

        assertThat(resultBuilder2)
            .hasToString(ibanBuilder.toString())
            .asString()
            .startsWith("DE" + expectedCheckDigits)
            .endsWith(bban);
    }

    @DisplayName("isValid: fail on non-digit at check digit position or illegal BBAN on spaced IBANs")
    @ParameterizedTest(name = "Input: {0}")
    @CsvSource(delimiter = '|', value = {
        "DE A1 1000 0000 0123 4567 89",       // non-digit at pos 3
        "DE9A 1000 0000 0123 4567 89",        // non-digit at pos 4
        "DE91 1000 !000 0123 4567 89",        // illegal char in BBAN
        "DE91 1000 0000 0123 4567 8",         // length check fail via registry
        "GT00 TRAJ 0102 0000 0012 1002 9690", // no country validator & invalid check digits
    })
    @NullAndEmptySource
    void isValidSpaced_shouldReturnFalseForInvalidCasesWithSpacing(String input) {
        try {
            IbanConfig.ALLOW_SPACE.enable();

            assertThat(IbanValidator.isValid(input)).isFalse();

        } finally {
            IbanConfig.ALLOW_SPACE.reset();
        }
    }

    @ParameterizedTest(name = "[{index}] input: {0} ({1})")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "(null) | null input",
        "''     | empty input",
        "DE91   | short mixed input",
        "123    | numeric short input",
        "ABCDE  | non-numeric short input",
        "XX61________________________ | invalid IBAN chars"
    })
    @DisplayName("calculateMod97 should return INVALID_MOD97 for invalid inputs")
    void calculateMod97_shouldHandleInvalidInputs(String input, String description) {
        assertThat(IbanValidator.calculateMod97(input))
            .as("Check failed for: %s", description)
            .isEqualTo(IbanValidator.INVALID_MOD97);
    }

}
