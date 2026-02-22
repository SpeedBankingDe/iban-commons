package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThatInvalidIbanException;
import static de.speedbanking.iban.IbanValidationError.EMPTY;
import static de.speedbanking.iban.IbanValidationError.ILLEGAL_CHARACTERS;
import static de.speedbanking.iban.IbanValidationError.INCORRECT_LENGTH;
import static de.speedbanking.iban.IbanValidationError.INCORRECT_LENGTH_COUNTRY;
import static de.speedbanking.iban.IbanValidationError.INVALID_CHECKSUM;
import static de.speedbanking.iban.IbanValidationError.INVALID_CHECK_DIGITS;
import static de.speedbanking.iban.IbanValidationError.INVALID_COUNTRY;
import static de.speedbanking.iban.IbanValidationError.INVALID_STRUCTURE;
import static de.speedbanking.iban.IbanValidationError.UNSUPPORTED_COUNTRY;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

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

    @DisplayName("Private constructor should throw UnsupportedOperationException to prevent instantiation")
    @Test
    void privateConstructor_shouldThrowException() throws Exception {
        Constructor<IbanValidator> constructor = IbanValidator.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(constructor::newInstance)
            .withCauseInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getMessage)
            .isEqualTo("Utility class " + IbanValidator.class.getSimpleName() + " cannot be instantiated");
    }

    @DisplayName("validateRaw should return success object for valid raw IBAN")
    @Test
    void validateRaw_shouldBeValid() {
        IbanValidationSuccess result = IbanValidator.validateRaw(VALID_RAW_DE);
        assertThat(result)
            .isNotNull()
            .extracting(IbanValidationSuccess::getNormIbanArr, IbanValidationSuccess::getCountryData)
            .containsExactly(VALID_NORM_DE.toCharArray(), IbanRegistry.DE);
        assertThat(IbanValidator.getLastReason()).isNull();
    }

    @DisplayName("validateRaw should return null and set error on null or empty input")
    @ParameterizedTest(name = "Input: ''{0}'' -> Expected: EMPTY")
    @NullAndEmptySource
    void validateRaw_shouldFailOnNullOrEmpty(String input) {
        IbanValidationSuccess result = IbanValidator.validateRaw(input);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(EMPTY);
    }

    @DisplayName("validateRaw should return null and set error on unsupported country code (e.g., AA)")
    @Test
    void validateRaw_shouldFailOnUnsupportedCountry() {
        IbanValidationSuccess result = IbanValidator.validateRaw("AA99AA9999999999999999");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(UNSUPPORTED_COUNTRY);
    }

    @DisplayName("validateRaw should return null and set error on invalid country code (lowercase)")
    @Test
    void validateRaw_shouldFailOnInvalidCountryCode() {
        IbanValidationSuccess result = IbanValidator.validateRaw("de91100000000123456789");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_COUNTRY);
    }

    @DisplayName("validateRaw should return null and set error on illegal characters in BBAN")
    @Test
    void validateRaw_shouldFailOnIllegalCharactersInBBAN() {
        IbanValidationSuccess result1 = IbanValidator.validateRaw("DE91.100000000123456789");
        assertThat(result1).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(ILLEGAL_CHARACTERS);

        // Reset for second test
        IbanValidator.setLastReason(null);

        // test that forces an illegal character check after the first 4 chars
        IbanValidationSuccess result2 = IbanValidator.validateRaw("DE51XXXXXXXXXXXXXXXXXX");
        // This is caught by BBAN structure check if X is allowed char but not in structure
        // But here X is caught as ILLEGAL_CHARACTERS in validateRaw loop L141 as it's not a digit
        // for BBAN part of DE
        assertThat(result2).isNull();
        // Since X is an uppercase char, it passes L141 but fails L277 (INVALID_STRUCTURE)
        IbanValidator.setLastReason(null);
        IbanValidationSuccess result3 = IbanValidator.validateRaw("DE5110000000012345678!");
        assertThat(result3).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(ILLEGAL_CHARACTERS);
    }

    @DisplayName("validateRaw should fail on non-digit check digits")
    @Test
    void validateRaw_shouldFailOnInvalidCheckDigits() {
        // 'A' instead of a digit at position 4 (check digit), containing spaces (L161)
        IbanValidationSuccess result = IbanValidator.validateRaw("DE9A 1000 0000 0123 4567 89");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_CHECK_DIGITS);
    }

    @DisplayName("validateRaw should fail if length less than min iban length")
    @Test
    void validateRaw_shouldFailOnMaxIbanLengthExceeded() {
        IbanValidationSuccess result = IbanValidator.validateRaw("DE73");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH);
    }

    @DisplayName("validateNormalized should return success object for valid normalized IBAN (DE)")
    @Test
    void validateNormalized_shouldBeValid_DE() {
        IbanValidationSuccess result = IbanValidator.validateNormalized(VALID_NORM_DE);
        assertThat(result).isNotNull();
        assertThat(Arrays.toString(result.normIbanArr)).isEqualTo(Arrays.toString(VALID_NORM_DE.toCharArray()));
        assertThat(IbanValidator.getLastReason()).isNull();
    }

    @DisplayName("validateNormalized should return success object for valid normalized IBAN (LU)")
    @Test
    void validateNormalized_shouldBeValid_LU() {
        IbanValidationSuccess result = IbanValidator.validateNormalized(VALID_NORM_LU);
        assertThat(result).isNotNull();
    }

    @DisplayName("validateNormalized should return null and set error on null input")
    @Test
    void validateNormalized_shouldFailOnNull() {
        IbanValidationSuccess result = IbanValidator.validateNormalized(null);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(EMPTY);
    }

    @DisplayName("validateNormalized should return null and set error on short length")
    @Test
    void validateNormalized_shouldFailOnTooShort() {
        // < MIN_IBAN_LENGTH (5 chars)
        assertThat(IbanValidator.validateNormalized("DE9")).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH);

        IbanValidator.setLastReason(null);

        // < country specific length (22 chars)
        assertThat(IbanValidator.validateNormalized("DE9110000000000000000")).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH_COUNTRY);
    }

    @DisplayName("validateNormalized should return null and set error on invalid country code (lowercase)")
    @Test
    void validateNormalized_shouldFailOnInvalidCountryCode() {
        // dE...
        IbanValidationSuccess result = IbanValidator.validateNormalized("dE91100000000123456789");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_COUNTRY);
    }

    @DisplayName("validateNormalized should return null and set error on unsupported country (e.g., AA)")
    @Test
    void validateNormalized_shouldFailOnUnsupportedCountry() {
        // AA does not exist in IbanRegistry
        IbanValidationSuccess result = IbanValidator.validateNormalized("AA99AA9999999999999999");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(UNSUPPORTED_COUNTRY);
    }

    @DisplayName("validateNormalized should return null and set error on incorrect length for country (e.g., DE with 21 chars)")
    @Test
    void validateNormalized_shouldFailOnIncorrectLengthForCountry() {
        // DE expects 22 characters, here 21
        IbanValidationSuccess result = IbanValidator.validateNormalized("DE9110000000012345678");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INCORRECT_LENGTH_COUNTRY);
    }

    @DisplayName("validateNormalized should return null and set error on non-digit check digits")
    @ParameterizedTest(name = "Input: ''{0}''")
    @ValueSource(strings = {"DE9A100000000123456789", "DEA9100000000123456789", "DEAA100000000123456789"})
    void validateNormalized_shouldFailOnInvalidCheckDigits(String input) {
        IbanValidationSuccess result = IbanValidator.validateNormalized(input);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_CHECK_DIGITS);
    }

    @DisplayName("validateNormalized should return null and set error on illegal characters in BBAN (e.g., lowercase)")
    @Test
    void validateNormalized_shouldFailOnIllegalCharactersInBBAN() {
        // lowercase in BBAN
        IbanValidationSuccess result = IbanValidator.validateNormalized("DE9110000000012345678b");
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(ILLEGAL_CHARACTERS);
    }

    @DisplayName("validateCommon should return null and set error on invalid BBAN structure (country specific)")
    @Test
    void validateCommon_shouldFailOnInvalidStructure() {
        // DE BBAN (Bank Code and Account Number) must only contain digits
        String invalidStructure = "DE91ABCDEFGHIJKLMNOPQR";
        IbanValidationSuccess result = IbanValidator.validateNormalized(invalidStructure);

        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_STRUCTURE);
    }

    @DisplayName("validateCommon should return null and set error on invalid Mod 97 checksum")
    @Test
    void validateCommon_shouldFailOnInvalidChecksum() {
        IbanValidationSuccess result = IbanValidator.validateRaw(INVALID_CHECKSUM_DE);
        assertThat(result).isNull();
        assertThat(IbanValidator.getLastReason()).isEqualTo(INVALID_CHECKSUM);
    }

    /**
     * Tests {@link IbanValidator#isMod97Valid(char[])} using various IBAN strings.
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
        "'' | false", // empty string
        "'  ' | false" // whitespace
    })
    void isMod97Valid_shouldHandleAllCases(String ibanString, boolean expectedValidity) {
        // handle the CsvSource empty string case
        char[] ibanArr = ibanString == null
                         ? null
                         : ibanString.toCharArray();

        boolean isValid = IbanValidator.isMod97Valid(ibanArr);

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

    /**
     * Tests that {@code setLastReason(null)} is called upon successful validation.
     */
    @DisplayName("validateCommon should reset last reason on success")
    @Test
    void validateCommon_shouldResetLastReasonOnSuccess() {
        // 1. Trigger an error to set LAST_REASON
        IbanValidator.validateRaw(INVALID_CHECKSUM_DE);
        assertThat(IbanValidator.getLastReason()).isNotNull();

        // 2. Perform a successful validation call. This internally calls validateCommon(),
        // which must execute setLastReason(null).
        IbanValidationSuccess result = IbanValidator.validateRaw(VALID_RAW_DE);
        assertThat(result).isNotNull();

        // 3. Assert that LAST_REASON has been reset to null.
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
     * Tests that the calculation method correctly throws an {@code InvalidIbanException}
     * when encountering illegal characters (those not in A-Z or 0-9) during the numeric conversion.
     *
     * @param ibanInput the input string containing illegal characters
     */
    @ParameterizedTest(name = "[{index}] Invalid character in ''{0}'' throws ILLEGAL_CHARACTERS")
    @ValueSource(strings = {
        "DE0010000000012345678/", // Forward slash
        "DE0010000000012345678-", // Hyphen
        "DE0010000000012345678 ", // Space (assuming input is normalized, but guards against it)
        "DE0010000000012345678ß"  // German specific non-alphanumeric character
    })
    void testCalculateMod97WithIllegalCharactersShouldThrowException(String ibanInput) {
        assertThatInvalidIbanException()
            .isThrownBy(() -> IbanValidator.calculateMod97(ibanInput))
            .extracting("reason")
            .isEqualTo(ILLEGAL_CHARACTERS);
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
    void testFixCheckDigits(String countryCode, String initialCheckDigits, String bban, String expectedCheckDigits) {
        StringBuilder ibanBuilder = new StringBuilder(countryCode)
            .append(initialCheckDigits)
            .append(bban);

        StringBuilder resultBuilder = IbanValidator.fixCheckDigits(ibanBuilder);

        assertThat(resultBuilder)
            .isSameAs(ibanBuilder)
            .startsWith("DE" + expectedCheckDigits)
            .endsWith(bban);
    }

}
