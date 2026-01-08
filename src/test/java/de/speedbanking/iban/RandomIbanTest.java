package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;
import static de.speedbanking.iban.IbanAssertions.assertThatInvalidIbanException;
import static de.speedbanking.iban.IbanRegistry.INDEX_CHECK_DIGITS;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * JUnit test class for the {@link RandomIban}.
 */
class RandomIbanTest {

    @Test
    void privateConstructorShouldThrowException() throws Exception {
        Constructor<RandomIban> constructor = RandomIban.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(constructor::newInstance)
            .withCauseInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getMessage)
            .isEqualTo("Utility class " + RandomIban.class.getSimpleName() + " cannot be instantiated");
    }

    /**
     * Tests that {@link RandomIban#of(String)} throws a {@link NullPointerException}
     * when the input country code is null, empty, or the country is not supported.
     */
    @ParameterizedTest(name = "Country code: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "XX", "de", "12"})
    void shouldThrowNpeWhenCountryCodeIsInvalid(String countryCode) {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.of(countryCode))
            .withCause(null)
            .withMessage("Supported country code required");
    }

    /**
     * Tests that for every country defined in the {@link IbanRegistry}.
     */
    @DisplayName("Should generate a valid IBAN for every supported country")
    @ParameterizedTest(name = "Generation for country: {0}")
    @EnumSource(IbanRegistry.class)
    void shouldGenerateValidIbanForCountry(IbanRegistry registry) {
        IbanValidator.setLastReason(null);
        Iban iban = RandomIban.of(registry.getCountryCode());

        assertThat(iban)
            .isNotNull()
            .hasCountryCode(registry.getCountryCode())
            .hasCountryFlag(registry.getCountryFlag())
            .hasCountryName(registry.getCountryName())
            .hasOrganisation(registry.getOrganisation());

        assertThat(IbanValidator.getLastReason()).isNull();

        String ibanStr = iban.toString();
        assertThat(Iban.isValid(ibanStr)).isTrue();
        assertThat(IbanValidator.getLastReason()).isNull();

        StringBuilder sb = new StringBuilder(ibanStr);
        sb.setCharAt(INDEX_CHECK_DIGITS, '0');
        sb.setCharAt(INDEX_CHECK_DIGITS + 1, '0');

        assertThat(iban)
            .hasCheckDigits(98 - RandomIban.calculateMod97(sb));
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
        int mod97 = RandomIban.calculateMod97(ibanWithZeroCheckDigits);
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
            .isThrownBy(() -> RandomIban.calculateMod97(ibanInput))
            .extracting("reason")
            .isEqualTo(IbanValidationError.ILLEGAL_CHARACTERS);
    }

    /**
     * Tests that {@code fixCheckDigits} correctly manipulates the StringBuilder:
     * 1. Sets the check digits placeholders to '00' internally (eliminates mutation in lines 106, 107).
     * 2. Calculates the correct check digits (98 - Mod97).
     * 3. Overwrites the placeholders with the correct check digits.
     * 4. Returns the modified StringBuilder (eliminates mutation in line 119).
     *
     * @param initialCheckDigits  the non-'00' initial placeholder digits (e.g., "11", "99")
     * @param expectedCheckDigits the correctly calculated check digits (CD = 98 - R)
     */
    @DisplayName("Should correctly fix check digits, overwriting initial placeholders")
    @ParameterizedTest(name = "IBAN with initial check digit ''{0}'' should result in ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // Known structure (DE BBAN) + non-standard initial CD
        "11 | 23", // DE11... -> DE00... (R=9) -> DE23...
        "99 | 23"  // DE99... -> DE00... (R=9) -> DE23...
    })
    void testFixCheckDigits(String initialCheckDigits, String expectedCheckDigits) {
        // DE BBAN pattern: 8!n4!n12!c (8+4+12 = 24 chars) -> IBAN total length 28
        // Base structure: CC + CD + BBAN -> "DE" + CD + "1000000001234567890123"
        String bban = "1000000001234567890123";
        StringBuilder ibanBuilder = new StringBuilder("DE")
            .append(initialCheckDigits)
            .append(bban);

        // This call mutates the StringBuilder and is expected to return it.
        // It must pass through lines 106/107, set chars to '0', calculate Mod97,
        // set chars to the final value, and return the non-null builder.
        StringBuilder resultBuilder = RandomIban.fixCheckDigits(ibanBuilder);

        assertThat(resultBuilder)
             // 1. assert return value is not null
            .isSameAs(ibanBuilder)
             // 2. Assert the final result is correct (calculation is only correct after setting to '00')
            .startsWith("DE" + expectedCheckDigits)
            .endsWith(bban);
    }

}
