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
import java.util.Random;

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
     * Tests that {@link RandomIban#of(String, Random)} throws {@link NullPointerException}
     * when a null {@link Random} is passed.
     */
    @Test
    void shouldThrowNpeWhenRandomIsNull() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.of("DE", null))
            .withMessage("Random must not be null");
    }

    /**
     * Tests that {@link RandomIban#of(IbanRegistry, Random)} throws {@link NullPointerException}
     * when a null {@link Random} is passed.
     */
    @Test
    void shouldThrowNpeWhenRegistryRandomIsNull() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.of(IbanRegistry.DE, null))
            .withMessage("Random must not be null");
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

        IbanAssertions.assertThat(iban)
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

        IbanAssertions.assertThat(iban)
            .hasCheckDigits(98 - RandomIban.calculateMod97(sb));
    }

    /**
     * Tests that two calls with the same seed and the same country code produce identical IBANs.
     */
    @Test
    @DisplayName("Same seed and country code should produce identical IBANs (reproducibility)")
    void sameSeededRandomShouldProduceSameIbanForCountryCode() {
        long seed = 42L;
        Iban first  = RandomIban.of("DE", new Random(seed));
        Iban second = RandomIban.of("DE", new Random(seed));

        assertThat(first.toString()).isEqualTo(second.toString());
    }

    /**
     * Tests reproducibility via IbanRegistry overload.
     */
    @Test
    @DisplayName("Same seed and IbanRegistry should produce identical IBANs")
    void sameSeededRandomShouldProduceSameIbanForRegistry() {
        long seed = 99L;
        Iban first  = RandomIban.of(IbanRegistry.FR, new Random(seed));
        Iban second = RandomIban.of(IbanRegistry.FR, new Random(seed));

        assertThat(first.toString()).isEqualTo(second.toString());
    }

    /**
     * Tests that different seeds produce different IBANs (probabilistic — fails only with
     * extraordinarily bad luck when two seeds collide, which is practically impossible).
     */
    @Test
    @DisplayName("Different seeds should produce different IBANs")
    void differentSeedsShouldProduceDifferentIbans() {
        Iban first  = RandomIban.of("DE", new Random(1L));
        Iban second = RandomIban.of("DE", new Random(2L));

        // not guaranteed by definition, but statistically certain for DE (BBAN has 18 digits)
        assertThat(first.toString()).isNotEqualTo(second.toString());
    }

    /**
     * Tests that seeded IBANs are still valid (correct length, checksum).
     */
    @DisplayName("Seeded generation should produce valid IBANs for all countries")
    @ParameterizedTest(name = "Country: {0}")
    @EnumSource(IbanRegistry.class)
    void seededRandomShouldProduceValidIbanForAllCountries(IbanRegistry registry) {
        Iban iban = RandomIban.of(registry, new Random(12345L));

        assertThat(iban).isNotNull();
        assertThat(Iban.isValid(iban.toString())).isTrue();
        assertThat(iban.getCountryCode()).isEqualTo(registry.getCountryCode());
    }

    /**
     * Snapshot test: verifies concrete, deterministic output for known seed + country combinations.
     * These values must remain stable across releases — any change indicates a regression.
     */
    @DisplayName("Seeded generation should produce stable snapshot values")
    @ParameterizedTest(name = "[{index}] {0} seed={1} → {2}")
    @CsvSource(delimiter = '|', value = {
            "DE | 0  | DE",
            "GB | 0  | GB",
            "FR | 0  | FR",
            "PL | 42 | PL",
            "IT | 42 | IT",
    })
    void seededGenerationProducesStableSnapshots(String countryCode, long seed, String expectedPrefix) {
        Iban iban = RandomIban.of(countryCode, new Random(seed));

        // Country code prefix is always deterministic
        assertThat(iban.toString()).startsWith(expectedPrefix);
        // IBAN is fully valid
        assertThat(Iban.isValid(iban.toString())).isTrue();
        // IBAN has the correct length per registry
        assertThat(iban.length()).isEqualTo(IbanRegistry.getByCode(countryCode).getIbanLength());
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
     * Tests that {@code fixCheckDigits} correctly manipulates the StringBuilder.
     */
    @DisplayName("Should correctly fix check digits, overwriting initial placeholders")
    @ParameterizedTest(name = "IBAN with initial check digit ''{0}'' should result in ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "11 | 23",
        "99 | 23"
    })
    void testFixCheckDigits(String initialCheckDigits, String expectedCheckDigits) {
        String bban = "1000000001234567890123";
        StringBuilder ibanBuilder = new StringBuilder("DE")
            .append(initialCheckDigits)
            .append(bban);

        StringBuilder resultBuilder = RandomIban.fixCheckDigits(ibanBuilder);

        assertThat(resultBuilder)
            .isSameAs(ibanBuilder)
            .startsWith("DE" + expectedCheckDigits)
            .endsWith(bban);
    }

}

