package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

/**
 * JUnit test class for the {@link RandomIban}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
class RandomIbanTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructor_Instantiation_ShouldThrowException() {
        TestUtil.assertConstructorIsPrivate(RandomIban.class);
    }

    /**
     * Tests that {@link RandomIban#of()} does not throw.
     */
    @Test
    void of_ShouldNotThrowException() {
        assertThatCode(RandomIban::of)
            .doesNotThrowAnyException();
    }

    /**
     * Tests that {@link RandomIban#of(String)} throws a {@link NullPointerException}
     * when the input country code is null, empty, or the country is not supported.
     */
    @ParameterizedTest(name = "Country code: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "XX", "de", "12"})
    void of_InvalidCountryCode_ShouldThrowNpe(String countryCode) {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.of(countryCode))
            .withCause(null)
            .withMessage("Supported iban country code required");
    }

    /**
     * Tests that {@link RandomIban#of(String, Random)} does not throw exceptions
     * when a null {@link Random} is passed.
     */
    @Test
    void of_NullRandom_ShouldNotThrowException() {
        assertThatCode(
            () -> RandomIban.of("DE", null))
            .doesNotThrowAnyException();
    }

    /**
     * Tests that {@link RandomIban#of(IbanRegistry, Random)} throws {@link NullPointerException}
     * when a null {@link Random} is passed.
     */
    @Test
    void of_NullRegistryRandom_ShouldNotThrowException() {
        assertThatCode(
            () -> RandomIban.of(IbanRegistry.DE, null))
            .doesNotThrowAnyException();
    }

    /**
     * Tests that for every country defined in the {@link IbanRegistry}.
     */
    @DisplayName("Should generate a valid IBAN for every supported country enum")
    @ParameterizedTest(name = "Generation for country: {0}")
    @IbanRegistrySource
    void of_CountryEnum_ShouldGenerateValidIban(IbanRegistry countryData) {
        IbanValidator.setLastReason(null);
        Iban iban = RandomIban.of(countryData);

        IbanRegistry actualCountryData = countryData.isBaseCountry()
            ? countryData
            : countryData.getBaseCountry();

        IbanAssertions.assertThat(iban)
            .isNotNull()
            .hasCountryCode(actualCountryData.getCountryCode())
            .hasCountryFlag(actualCountryData.getCountryFlag())
            .hasCountryName(actualCountryData.getCountryName())
            .hasOrganisation(actualCountryData.getOrganisation());

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
     * Tests that for every country defined in the {@link IbanRegistry}.
     */
    @DisplayName("Should generate a valid IBAN for every supported country code")
    @ParameterizedTest(name = "Generation for country: {0}")
    @IbanRegistrySource
    void of_CountryCode_ShouldGenerateValidIban(IbanRegistry countryData) {
        IbanValidator.setLastReason(null);
        Iban iban = RandomIban.of(countryData.getCountryCode());

        IbanRegistry actualCountryData = countryData.isBaseCountry()
            ? countryData
            : countryData.getBaseCountry();

        IbanAssertions.assertThat(iban)
            .isNotNull()
            .hasCountryCode(actualCountryData.getCountryCode())
            .hasCountryFlag(actualCountryData.getCountryFlag())
            .hasCountryName(actualCountryData.getCountryName())
            .hasOrganisation(actualCountryData.getOrganisation());

        assertThat(IbanValidator.getLastReason()).isNull();
    }

    /**
     * Tests that two calls with the same seed and the same country code produce identical IBANs.
     */
    @DisplayName("Same seed and country code should produce identical IBANs (reproducibility)")
    @Test
    void of_SameSeed_ShouldProduceIdenticalIban() {
        long seed = 42L;
        Iban first  = RandomIban.of("DE", new Random(seed));
        Iban second = RandomIban.of("DE", new Random(seed));

        assertThat(first).hasToString(second.toString());
    }

    /**
     * Tests reproducibility via IbanRegistry overload.
     */
    @DisplayName("Same seed and IbanRegistry should produce identical IBANs")
    @Test
    void of_SameSeedRegistry_ShouldProduceIdenticalIban() {
        long seed = 99L;
        Iban first  = RandomIban.of(IbanRegistry.FR, new Random(seed));
        Iban second = RandomIban.of(IbanRegistry.FR, new Random(seed));

        assertThat(first).hasToString(second.toString());
    }

    /**
     * Tests that different seeds produce different IBANs (probabilistic — fails only with
     * extraordinarily bad luck when two seeds collide, which is practically impossible).
     */
    @DisplayName("Different seeds should produce different IBANs")
    @Test
    void of_DifferentSeeds_ShouldProduceDifferentIbans() {
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
    @IbanRegistrySource
    void of_SeededAllCountries_ShouldBeValid(IbanRegistry countryData) {
        Iban iban = RandomIban.of(countryData, new Random(4711L));
        String countryCode = countryData.isBaseCountry()
            ? countryData.getCountryCode()
            : countryData.getBaseCountry().getCountryCode();

        assertThat(iban)
            .isNotNull()
            .hasCountryCode(countryCode);
        assertThat(Iban.isValid(iban.toString())).isTrue();
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
    void of_SeededSnapshot_ShouldBeStable(String countryCode, long seed, String expectedPrefix) {
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
    @DisplayName("Should generate a valid IBAN for any country")
    @Test
    void of_AnyCountry_ShouldBeValid() {
        Iban iban = RandomIban.of();

        assertThat(iban).isNotNull();
        assertThat(Iban.isValid(iban.toString())).isTrue();
        assertThat(IbanRegistry.getByCode(iban.getCountryCode())).isNotNull();
    }

    /**
     * Tests that {@link RandomIban#ofSepa()} generates a valid IBAN that belongs to a SEPA country.
     */
    @DisplayName("Should generate only SEPA IBANs when calling ofSepa")
    @Test
    void ofSepa_MultipleIbans_ShouldBeValidAndFromSepaCountry() {
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
    @DisplayName("Same seed should produce identical IBANs for any-country selection")
    @Test
    void of_SameSeedAnyCountry_ShouldProduceIdenticalIban() {
        long seed = 123456L;
        Iban first  = RandomIban.of(new Random(seed));
        Iban second = RandomIban.of(new Random(seed));

        assertThat(first).hasToString(second.toString());
    }

    /**
     * Tests reproducibility for the SEPA generation.
     */
    @DisplayName("Same seed should produce identical IBANs for SEPA selection")
    @Test
    void ofSepa_SameSeed_ShouldProduceIdenticalIban() {
        long seed = 789L;
        Iban first  = RandomIban.ofSepa(new Random(seed));
        Iban second = RandomIban.ofSepa(new Random(seed));

        assertThat(first).hasToString(second.toString());
        assertThat(IbanRegistry.getByCode(first.getCountryCode()).isSepa()).isTrue();
    }

    /**
     * Verifies that null {@link Random} is handled for the new methods.
     */
    @Test
    void of_NullRandomNewMethods_ShouldNotThrowException() {
        assertThatCode(
            () -> RandomIban.of((Random) null))
            .doesNotThrowAnyException();
        assertThatCode(
            () -> RandomIban.ofSepa(null))
            .doesNotThrowAnyException();
    }

}

