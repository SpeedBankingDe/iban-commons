package de.speedbanking.iban;

import static de.speedbanking.iban.IbanValidationError.EMPTY;
import static de.speedbanking.iban.IbanValidationError.ILLEGAL_CHARACTERS;
import static de.speedbanking.iban.IbanValidationError.INCORRECT_LENGTH;
import static de.speedbanking.iban.IbanValidationError.INCORRECT_LENGTH_COUNTRY;
import static de.speedbanking.iban.IbanValidationError.INVALID_CHECKSUM;
import static de.speedbanking.iban.IbanValidationError.INVALID_CHECK_DIGITS;
import static de.speedbanking.iban.IbanValidationError.INVALID_COUNTRY;
import static de.speedbanking.iban.IbanValidationError.INVALID_STRUCTURE;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatInvalidIbanException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

/**
 * JUnit test class for {@link IbanValidator}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class IbanValidatorTest {

    // A valid German IBAN for positive tests
    private static final String VALID_RAW_DE        = "DE91 1000 0000 0123 4567 89";
    private static final String VALID_NORM_DE       = "DE91100000000123456789";

    // A valid Luxembourgish IBAN (20 chars, contains letters in BBAN)
    private static final String VALID_NORM_LU       = "LU280019400644750000";

    // An invalid IBAN with a wrong checksum (Mod 97)
    private static final String INVALID_CHECKSUM_DE = "DE90100000000123456789"; // DE91 -> DE90

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructor_shouldThrowException_whenInstantiated() {
        TestUtil.assertConstructorIsPrivate(IbanValidator.class);
    }

    @DisplayName("Should load country validator when valid country code is provided")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"DE", "AT", "CH"})
    void loadCountryValidator_ShouldReturnValidator_WhenCountryCodeIsValid(String countryCode) {
        CountryValidator validator = IbanValidator.loadCountryValidator(countryCode);

        assertThat(validator).isNotNull();
    }

    @DisplayName("Should throw exception when country code is unknown")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"XX", "47", "  ", " ", ""})
    void loadCountryValidator_ShouldThrowException_WhenCountryCodeIsUnknown(String countryCode) {
        assertThatThrownBy(() -> IbanValidator.loadCountryValidator(countryCode))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Could not instantiate class '%s': java.lang.ClassNotFoundException", CountryValidators.class.getName() + '$' + countryCode);
    }

    @DisplayName("validate should return valid result for valid raw IBAN")
    @Test
    void validate_shouldReturnSuccess_whenIbanIsValidAndRaw() {
        IbanValidationResult result = IbanValidator.validate(VALID_RAW_DE, true);

        assertThat(result.isValid()).isTrue();
        assertThat(result.normIban).isEqualTo(VALID_NORM_DE);
        assertThat(result.countryData).isEqualTo(IbanRegistry.DE);
    }

    @DisplayName("validate should return invalid result on null or empty input")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    void validate_shouldReturnInvalid_whenInputIsNullOrEmpty(String input) {
        IbanValidationResult result = IbanValidator.validate(input, false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(EMPTY);
    }

    @DisplayName("validate should return invalid result on unsupported country code (e.g., AA)")
    @Test
    void validate_shouldReturnInvalid_whenCountryIsUnsupported() {
        IbanValidationResult result = IbanValidator.validate("AA99AA9999999999999999", false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INVALID_COUNTRY);
    }

    @DisplayName("validate should return invalid result on lowercase country code")
    @Test
    void validate_shouldReturnInvalid_whenCountryCodeIsLowercase() {
        IbanValidationResult result = IbanValidator.validate("de91100000000123456789", false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(ILLEGAL_CHARACTERS);
    }

    @DisplayName("validate should return invalid result on illegal characters in BBAN")
    @Test
    void validate_shouldReturnInvalid_whenBbanContainsIllegalCharacters() {
        IbanValidationResult result1 = IbanValidator.validate("DE91.100000000123456789", false);
        assertThat(result1.isValid()).isFalse();
        assertThat(result1.error).isEqualTo(ILLEGAL_CHARACTERS);

        IbanValidationResult result2 = IbanValidator.validate("DE51XXXXXXXXXXXXXXXXXX", false);
        assertThat(result2.isValid()).isFalse();
        assertThat(result2.error).isEqualTo(INVALID_STRUCTURE);

        IbanValidationResult result3 = IbanValidator.validate("DE5110000000012345678!", false);
        assertThat(result3.isValid()).isFalse();
        assertThat(result3.error).isEqualTo(ILLEGAL_CHARACTERS);
    }

    @DisplayName("validate should fail on non-digit check digits")
    @Test
    void validate_shouldReturnInvalid_whenCheckDigitsAreNonDigit() {
        // 'A' instead of a digit at position 4 (check digit), containing spaces
        IbanValidationResult result = IbanValidator.validate("DE9A 1000 0000 0123 4567 89", true);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INVALID_CHECK_DIGITS);
    }

    @DisplayName("validate should fail if length less than min IBAN length")
    @Test
    void validate_shouldReturnInvalid_whenLengthIsBelowMinimum() {
        IbanValidationResult result = IbanValidator.validate("DE73", false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INCORRECT_LENGTH);
    }

    @DisplayName("validate should return valid result for valid normalized IBAN (DE)")
    @Test
    void validate_shouldReturnSuccess_whenIbanIsValidDe() {
        IbanValidationResult result = IbanValidator.validate(VALID_NORM_DE);

        assertThat(result.isValid()).isTrue();
        assertThat(result.normIban).isEqualTo(VALID_NORM_DE);
    }

    @DisplayName("validate should return valid result for valid normalized IBAN (LU)")
    @Test
    void validate_shouldReturnSuccess_whenIbanIsValidLu() {
        IbanValidationResult result = IbanValidator.validate(VALID_NORM_LU);

        assertThat(result.isValid()).isTrue();
        assertThat(result.normIban).isEqualTo(VALID_NORM_LU);
    }

    @DisplayName("validate should return invalid result on null input")
    @Test
    void validate_shouldReturnInvalid_whenInputIsNull() {
        assertThat(IbanValidator.validate((String) null).error).isEqualTo(EMPTY);
        assertThat(IbanValidator.validate((CharSequence) null).error).isEqualTo(EMPTY);
    }

    @DisplayName("validate should return invalid result on short length")
    @Test
    void validate_shouldReturnInvalid_whenIbanIsTooShort() {
        // < MIN_IBAN_LENGTH (5 chars)
        IbanValidationResult result1 = IbanValidator.validate("DE9");
        assertThat(result1.isValid()).isFalse();
        assertThat(result1.error).isEqualTo(INCORRECT_LENGTH);

        // < country-specific length (22 chars for DE)
        IbanValidationResult result2 = IbanValidator.validate("DE9110000000000000000");
        assertThat(result2.isValid()).isFalse();
        assertThat(result2.error).isEqualTo(INCORRECT_LENGTH_COUNTRY);
    }

    @DisplayName("validate should return invalid result on incorrect length for country (e.g., DE with 21 chars)")
    @Test
    void validate_shouldReturnInvalid_whenLengthIsIncorrectForCountry() {
        // DE expects 22 characters, here 21
        String iban = "DE9110000000012345678";

        IbanValidationResult result1 = IbanValidator.validate(iban);
        assertThat(result1.isValid()).isFalse();
        assertThat(result1.error).isEqualTo(INCORRECT_LENGTH_COUNTRY);

        IbanValidationResult result2 = IbanValidator.validate((CharSequence) iban);
        assertThat(result2.isValid()).isFalse();
        assertThat(result2.error).isEqualTo(INCORRECT_LENGTH_COUNTRY);
    }

    @DisplayName("validate should return invalid result on non-digit check digits")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"DE9A100000000123456789", "DEA9100000000123456789", "DEAA100000000123456789"})
    void validate_shouldReturnInvalid_whenCheckDigitsAreInvalid(String input) {
        assertThat(IbanValidator.validate(input).error).isEqualTo(INVALID_CHECK_DIGITS);
        assertThat(IbanValidator.validate((CharSequence) input).error).isEqualTo(INVALID_CHECK_DIGITS);
    }

    @DisplayName("validate should return invalid result on invalid BBAN structure (country specific)")
    @Test
    void validate_shouldReturnInvalid_whenBbanStructureIsInvalid() {
        // DE BBAN (Bank Code and Account Number) must only contain digits
        String invalidStructure = "DE91ABCDEFGHIJKLMNOPQR";
        IbanValidationResult result = IbanValidator.validate(invalidStructure, false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INVALID_STRUCTURE);
    }

    @DisplayName("validate should return invalid result on invalid Mod 97 checksum")
    @Test
    void validate_shouldReturnInvalid_whenChecksumIsInvalid() {
        IbanValidationResult result = IbanValidator.validate(INVALID_CHECKSUM_DE, false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INVALID_CHECKSUM);
    }

    /**
     * Tests {@link IbanValidator#isMod97Valid(CharSequence)} using various IBAN strings.
     */
    @DisplayName("Modulo 97 Validation")
    @ParameterizedTest(name = "[{index}] ''{0}'' -> expected ''{1}''")
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
        "''     | false", // empty string
        "'  '   | false"  // whitespace
    })
    void isMod97Valid_shouldReturnCorrectResult_whenVariousInputsAreGiven(String ibanString, boolean expectedValidity) {
        boolean isValid = IbanValidator.isMod97Valid(ibanString);

        assertThat(isValid)
            .withFailMessage("Validation failed for IBAN: %s (expected: %s, actual: %s)",
                             ibanString, expectedValidity, isValid)
            .isEqualTo(expectedValidity);
    }

    @DisplayName("isValid should return true for valid IBAN")
    @Test
    void isValid_shouldReturnTrue_whenIbanIsValid() {
        assertThat(IbanValidator.isValid(VALID_NORM_DE)).isTrue();
        assertThat(IbanValidator.isValid((CharSequence) VALID_NORM_DE)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "DE91 1000 0000 0123 4567 89",
        "GB29 NWBK 6016 1331 9268 19",
        "PL 61 10 90 10 14 00 00 07 12 19 81 28 74"
    })
    @DisplayName("isValid should return true for valid formatted (spaced) IBANs")
    @ResourceLock(IbanConfigTest.RESOURCE_NAME)
    void isValid_shouldReturnTrue_whenIbanIsFormattedWithSpaces(String formattedIban) {
        IbanConfig ibanConfig = IbanConfig.get();

        try {
            IbanConfig.reset(IbanConfig.builder()
                .allowSpace(true)
                .allowLowercase(true)
                .build());

            assertThat(IbanValidator.isValid(formattedIban))
                .as("IBAN %s should be recognized as valid", formattedIban)
                .isTrue();
            assertThat(IbanValidator.isValid((CharSequence) formattedIban)).isTrue();

            String lowerCaseIban = formattedIban.toLowerCase(Locale.ROOT);

            assertThat(IbanValidator.isValid(lowerCaseIban))
                .as("IBAN %s should be recognized as valid", lowerCaseIban)
                .isTrue();
            assertThat(IbanValidator.isValid((CharSequence) lowerCaseIban)).isTrue();

        } finally {
            IbanConfig.reset(ibanConfig);
        }
    }

    /**
     * Tests {@link IbanValidator#calculateMod97(CharSequence)} with known IBAN structures
     * (containing "00" placeholders for check digits) to ensure the calculated remainder
     * matches the required value R, where the actual check digits CD = 98 - R.
     *
     * @param ibanWithZeroCheckDigits the IBAN with "00" in the check digit position
     * @param expectedRemainder       the remainder R that must be produced by the modulo 97 calculation
     */
    @ParameterizedTest(name = "[{index}] ''{0}'' -> {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // IBAN (00 CD)              | Expected Remainder R (for CD = 98 - R)
        "DE00370400440532013000      | 9",  // result 9 -> CD 91 (DE91...)
        "NL00ABNA0417164300          | 7",  // result 7 -> CD 97 (NL91...)
        "FR0020041010050500013M02606 | 84", // result 84 -> CD 15 (FR14...)
        "TR000006100519786457841326  | 65"  // longer IBAN structure (implicitly tests intermediate modulo)
    })
    @DisplayName("calculateMod97 should return correct remainder for valid IBAN formats")
    void calculateMod97_shouldReturnCorrectRemainder(CharSequence ibanWithZeroCheckDigits, int expectedRemainder) {
        int mod97 = IbanValidator.calculateMod97(ibanWithZeroCheckDigits);
        assertThat(mod97).isEqualTo(expectedRemainder);
    }

    /**
     * Tests that the calculation method correctly returns {@value IbanValidator#INVALID_MOD97}
     * when encountering illegal characters (those not in A-Z or 0-9) during the numeric conversion.
     *
     * @param ibanInput the input string containing illegal characters
     */
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "DE0010000000012345678/", // forward slash
        "DE0010000000012345678-", // hyphen
        "DE0010000000012345678 ", // space (assuming input is normalized, but guards against it)
        "DE0010000000012345678ß"  // German specific non-alphanumeric character
    })
    @DisplayName("calculateMod97 should return INVALID_MOD97 on illegal characters")
    void calculateMod97_shouldReturnInvalidOnIllegalCharacters(String ibanInput) {
        assertThat(IbanValidator.calculateMod97(ibanInput)).isEqualTo(IbanValidator.INVALID_MOD97);
    }

    @DisplayName("isValid: fail on non-digit at check digit position or illegal BBAN on spaced IBANs")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @CsvSource(delimiter = '|', value = {
        "DE A1 1000 0000 0123 4567 89",       // non-digit at pos 3
        "DE9A 1000 0000 0123 4567 89",        // non-digit at pos 4
        "DE91 1000 !000 0123 4567 89",        // illegal char in BBAN
        "DE91 1000 0000 0123 4567 8",         // length check fail via registry
        "GT00 TRAJ 0102 0000 0012 1002 9690", // no country validator & invalid check digits
        "DE00 4444 7777 1111 1111 4747 1111 0047 119" // too long (and invalid)
    })
    @NullAndEmptySource
    void isValid_shouldReturnFalse_whenInputIsInvalidWithSpacing(String input) {
        IbanConfig ibanConfig = IbanConfig.get();

        try {
            IbanConfig.reset(IbanConfig.builder().allowSpace(true).build());

            assertThat(IbanValidator.isValid(input)).isFalse();
            assertThat(IbanValidator.isValid((CharSequence) input)).isFalse();

        } finally {
            IbanConfig.reset(ibanConfig);
        }
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' ({1})")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "(null)                       | null input",
        "''                           | empty input",
        "DE91                         | short mixed input",
        "123                          | numeric short input",
        "ABCDE                        | non-numeric short input",
        "XX61________________________ | invalid IBAN chars"
    })
    @DisplayName("calculateMod97 should return INVALID_MOD97 for invalid inputs")
    void calculateMod97_shouldReturnInvalid_whenInputIsInvalid(String input, String description) {
        assertThat(IbanValidator.calculateMod97(input))
            .as("Check failed for: %s", description)
            .isEqualTo(IbanValidator.INVALID_MOD97);
    }

    @DisplayName("fixCheckDigits: should throw InvalidIbanException when input is null")
    @Test
    void fixCheckDigits_shouldThrowException_whenInputIsNull() {
        assertThatInvalidIbanException()
            .isThrownBy(() -> IbanValidator.fixCheckDigits(null))
            .withCause(null)
            .withMessage("%s (%s)",
                EMPTY.getText(), EMPTY)
            .hasFieldOrPropertyWithValue("reason", EMPTY);
    }

    @DisplayName("fixCheckDigits: should throw IllegalArgumentException when length is out of bounds")
    @ParameterizedTest
    @ValueSource(strings = {
        "DE91", // too short (below MIN_IBAN_LENGTH)
        "DE0044447777111111114747111100471191234" // too long (above MAX_IBAN_LENGTH)
    })
    void fixCheckDigits_shouldThrowException_whenLengthIsOutOfBounds(String invalidIban) {
        assertThatInvalidIbanException()
            .isThrownBy(() -> IbanValidator.fixCheckDigits(invalidIban))
            .withCause(null)
            .withMessage("%s (%s): '%s'",
                INCORRECT_LENGTH.getText(), INCORRECT_LENGTH, invalidIban)
            .hasFieldOrPropertyWithValue("reason", INCORRECT_LENGTH);
    }

    @DisplayName("fixCheckDigits: should reuse existing StringBuilder instance to prevent re-allocation")
    @Test
    @SuppressWarnings("UnnecessaryStringBuilder")
    void fixCheckDigits_shouldReuseInstance_whenInputIsStringBuilder() {
        StringBuilder inputBuffer = new StringBuilder("DE00370400440532013000");
        StringBuilder resultBuffer = IbanValidator.fixCheckDigits(inputBuffer);

        assertThat(resultBuffer)
            .as("The returned StringBuilder must be identical to the input instance")
            .isSameAs(inputBuffer);

        assertThat(resultBuffer.toString())
            .as("The check digits must be correctly computed and mutated inside the buffer")
            .isEqualTo("DE89370400440532013000");
    }

}
