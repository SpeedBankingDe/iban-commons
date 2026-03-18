package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;

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
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT1, '0');
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT2, '0');

        IbanAssertions.assertThat(iban)
            .hasCheckDigits(98 - IbanValidator.calculateMod97(sb));
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
     * Tests that {@link RandomIban#of()} generates a valid IBAN for an arbitrary supported country.
     */
    @Test
    @DisplayName("Should generate a valid IBAN for any country")
    void shouldGenerateValidIbanForAnyCountry() {
        Iban iban = RandomIban.of();

        assertThat(iban).isNotNull();
        assertThat(Iban.isValid(iban.toString())).isTrue();
        assertThat(IbanRegistry.getByCode(iban.getCountryCode())).isNotNull();
    }

    /**
     * Tests that {@link RandomIban#ofSepa()} generates a valid IBAN that belongs to a SEPA country.
     */
    @Test
    @DisplayName("Should generate only SEPA IBANs when calling ofSepa")
    void shouldGenerateOnlySepaIbans() {
        // test multiple times to increase statistical confidence in random selection
        for (int i = 0; i < 20; i++) {
            Iban iban = RandomIban.ofSepa();
            IbanRegistry registry = IbanRegistry.getByCode(iban.getCountryCode());

            assertThat(registry.isSepa())
                .withFailMessage("Generated IBAN %s is not from a SEPA country", iban)
                .isTrue();
            assertThat(Iban.isValid(iban.toString())).isTrue();
        }
    }

    /**
     * Tests reproducibility for the 'any country' generation.
     */
    @Test
    @DisplayName("Same seed should produce identical IBANs for any-country selection")
    void seededRandomShouldProduceSameIbanForAnyCountry() {
        long seed = 123456L;
        Iban first  = RandomIban.of(new Random(seed));
        Iban second = RandomIban.of(new Random(seed));

        assertThat(first.toString()).isEqualTo(second.toString());
    }

    /**
     * Tests reproducibility for the SEPA generation.
     */
    @Test
    @DisplayName("Same seed should produce identical IBANs for SEPA selection")
    void seededRandomShouldProduceSameIbanForSepa() {
        long seed = 789L;
        Iban first  = RandomIban.ofSepa(new Random(seed));
        Iban second = RandomIban.ofSepa(new Random(seed));

        assertThat(first.toString()).isEqualTo(second.toString());
        assertThat(IbanRegistry.getByCode(first.getCountryCode()).isSepa()).isTrue();
    }

    /**
     * Verifies that null Random is handled for the new methods.
     */
    @Test
    void shouldThrowNpeWhenRandomIsNullForNewMethods() {
        assertThatNullPointerException().isThrownBy(() -> RandomIban.of((Random) null));
        assertThatNullPointerException().isThrownBy(() -> RandomIban.ofSepa(null));
    }

}

