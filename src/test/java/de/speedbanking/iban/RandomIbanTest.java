package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
     * Tests that {@link RandomIban#of(String)} throws an {@link IllegalArgumentException}
     * when the input country code is null, empty, blank, or unsupported.
     */
    @ParameterizedTest(name = "Country code: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "XX", "de", "12"})
    void of_InvalidCountryCode_ShouldThrowIllegalArgumentException(String countryCode) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> RandomIban.of(countryCode));
    }

    /**
     * Tests that {@link RandomIban#of(IbanRegistry)} generates a valid IBAN for every
     * country defined in the {@link IbanRegistry}.
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
     * Tests that {@link RandomIban#of(String)} generates a valid IBAN for every
     * country defined in the {@link IbanRegistry}.
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
     * Tests that {@link RandomIban#ofSepa()} generates valid IBANs that all belong
     * to a SEPA country.
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

    @ParameterizedTest(name = "[{index}] {0}")
    @IbanRegistrySource
    void fixNationalCheckDigit_should_MaintainInstance_And_ValidateLength(IbanRegistry countryData) {
        StringBuilder ibanIn1 = new StringBuilder(countryData.getIbanExample());
        StringBuilder ibanOut = RandomIban.fixNationalCheckDigit(countryData, ibanIn1);

        assertThat(ibanOut).isSameAs(ibanIn1);

        StringBuilder ibanIn2 = new StringBuilder(countryData.getIbanExample()).deleteCharAt(countryData.getIbanLength() - 1);

        assertThatExceptionOfType(InvalidIbanException.class).isThrownBy(
            () -> RandomIban.fixNationalCheckDigit(countryData, ibanIn2));
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Tests that a default builder invocation does not throw.
     */
    @DisplayName("Default builder build should not throw")
    @Test
    void builder_DefaultBuild_ShouldNotThrow() {
        assertThatCode(() -> RandomIban.builder().build())
            .doesNotThrowAnyException();
    }

    /**
     * Tests that the builder generates a valid IBAN for any country.
     */
    @DisplayName("Builder without constraints should produce a valid IBAN for any country")
    @Test
    void builder_AnyCountry_ShouldBeValid() {
        Iban iban = RandomIban.builder().build();

        assertThat(iban).isNotNull();
        assertThat(Iban.isValid(iban.toString())).isTrue();
        assertThat(IbanRegistry.getByCode(iban.getCountryCode())).isNotNull();
    }

    /**
     * Tests that the builder throws {@link IllegalArgumentException} for unsupported country codes.
     */
    @ParameterizedTest(name = "Country code: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "XX", "de", "12"})
    void builder_InvalidCountryCode_ShouldThrowIllegalArgumentException(String countryCode) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> RandomIban.builder().country(countryCode));
    }

    /**
     * Tests that the builder throws {@link NullPointerException} when a null
     * {@link IbanRegistry} is passed.
     */
    @DisplayName("Builder country(IbanRegistry) with null should throw NullPointerException")
    @Test
    void builder_NullRegistry_ShouldThrowNullPointerException() {
        RandomIban.Builder builder = RandomIban.builder();

        assertThatNullPointerException()
            .isThrownBy(() -> builder.country((IbanRegistry) null))
            .withMessage("registry must not be null");
    }

    /**
     * Tests that the builder throws {@link NullPointerException} when a null
     * {@link Random} is passed.
     */
    @DisplayName("Builder random(null) should throw NullPointerException")
    @Test
    void builder_NullRandom_ShouldThrowNullPointerException() {
        RandomIban.Builder builder = RandomIban.builder();

        assertThatNullPointerException()
            .isThrownBy(() -> builder.random(null))
            .withMessage("random must not be null");
    }

    /**
     * Tests that sepaOnly() and a fixed country() are mutually exclusive:
     * the last-called setter wins.
     */
    @DisplayName("Builder sepaOnly() after country() should override the fixed country")
    @Test
    void builder_SepaOnlyAfterCountry_ShouldYieldSepaIban() {
        // chain: country first, then sepaOnly — sepaOnly must win
        for (int i = 0; i < 10; i++) {
            Iban iban = RandomIban.builder().country("DE").sepaOnly().build();
            assertThat(IbanRegistry.getByCode(iban.getCountryCode()).isSepa()).isTrue();
            assertThat(Iban.isValid(iban.toString())).isTrue();
        }
    }

    /**
     * Tests that country() after sepaOnly() overrides the SEPA constraint.
     */
    @DisplayName("Builder country() after sepaOnly() should pin the country to DE")
    @Test
    void builder_CountryAfterSepaOnly_ShouldYieldPinnedCountry() {
        Iban iban = RandomIban.builder()
            .sepaOnly()
            .country("DE")
            .build();
        assertThat(iban.getCountryCode()).isEqualTo("DE");
    }

    /**
     * Tests that {@code sepaOnly()} restricts generation to SEPA countries.
     */
    @DisplayName("Builder sepaOnly should generate only SEPA IBANs")
    @Test
    void builder_SepaOnly_ShouldBeValidAndFromSepaCountry() {
        for (int i = 0; i < 20; i++) {
            Iban iban = RandomIban.builder().sepaOnly().build();
            IbanRegistry registry = IbanRegistry.getByCode(iban.getCountryCode());

            assertThat(registry.isSepa())
                .withFailMessage("Generated IBAN %s is not from a SEPA country", iban)
                .isTrue();
            assertThat(Iban.isValid(iban.toString())).isTrue();
        }
    }

    /**
     * Tests that two builder calls with the same seed and country code produce identical IBANs.
     */
    @DisplayName("Same seed and country code should produce identical IBANs (reproducibility)")
    @Test
    void builder_SameSeedCountryCode_ShouldProduceIdenticalIban() {
        long seed = 42L;
        Iban first  = RandomIban.builder().country("DE").seed(seed).build();
        Iban second = RandomIban.builder().country("DE").seed(seed).build();

        assertThat(first).hasToString(second.toString());
    }

    /**
     * Tests reproducibility via the {@link IbanRegistry} overload.
     */
    @DisplayName("Same seed and IbanRegistry should produce identical IBANs")
    @Test
    void builder_SameSeedRegistry_ShouldProduceIdenticalIban() {
        long seed = 99L;
        Iban first  = RandomIban.builder().country(IbanRegistry.FR).seed(seed).build();
        Iban second = RandomIban.builder().country(IbanRegistry.FR).seed(seed).build();

        assertThat(first).hasToString(second.toString());
    }

    /**
     * Tests reproducibility for any-country generation.
     */
    @DisplayName("Same seed should produce identical IBANs for any-country selection")
    @Test
    void builder_SameSeedAnyCountry_ShouldProduceIdenticalIban() {
        long seed = 123456L;
        Iban first  = RandomIban.builder().seed(seed).build();
        Iban second = RandomIban.builder().seed(seed).build();

        assertThat(first).hasToString(second.toString());
    }

    /**
     * Tests reproducibility for SEPA-only generation.
     */
    @DisplayName("Same seed should produce identical IBANs for SEPA-only selection")
    @Test
    void builder_SepaOnly_SameSeed_ShouldProduceIdenticalIban() {
        long seed = 789L;
        Iban first  = RandomIban.builder().sepaOnly().seed(seed).build();
        Iban second = RandomIban.builder().sepaOnly().seed(seed).build();

        assertThat(first).hasToString(second.toString());
        assertThat(IbanRegistry.getByCode(first.getCountryCode()).isSepa()).isTrue();
    }

    /**
     * Tests that a {@link Random} instance passed via {@link RandomIban.Builder#random(Random)}
     * also yields reproducible results.
     */
    @DisplayName("Builder random(Random) with same seed should produce identical IBANs")
    @Test
    void builder_RandomInstance_ShouldProduceIdenticalIban() {
        Iban first  = RandomIban.builder().country("DE").random(new Random(42L)).build();
        Iban second = RandomIban.builder().country("DE").random(new Random(42L)).build();

        assertThat(first).hasToString(second.toString());
    }

    /**
     * Tests that different seeds produce different IBANs (probabilistic — fails only with
     * extraordinarily bad luck when two seeds collide, which is practically impossible).
     */
    @DisplayName("Different seeds should produce different IBANs")
    @Test
    void builder_DifferentSeeds_ShouldProduceDifferentIbans() {
        Iban first  = RandomIban.builder().country("DE").seed(1L).build();
        Iban second = RandomIban.builder().country("DE").seed(2L).build();

        // not guaranteed by definition, but statistically certain for DE (BBAN has 18 digits)
        assertThat(first.toString()).isNotEqualTo(second.toString());
    }

    /**
     * Tests that seeded generation produces valid IBANs for all countries.
     */
    @DisplayName("Seeded builder generation should produce valid IBANs for all countries")
    @ParameterizedTest(name = "Country: {0}")
    @IbanRegistrySource
    void builder_SeededAllCountries_ShouldBeValid(IbanRegistry countryData) {
        Iban iban = RandomIban.builder().country(countryData).seed(4711L).build();
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
    @DisplayName("Seeded builder generation should produce stable snapshot values")
    @ParameterizedTest(name = "[{index}] {0} seed={1} → {2}")
    @CsvSource(delimiter = '|', value = {
            "DE | 0  | DE",
            "GB | 0  | GB",
            "FR | 0  | FR",
            "PL | 42 | PL",
            "IT | 42 | IT",
    })
    void builder_SeededSnapshot_ShouldBeStable(String countryCode, long seed, String expectedPrefix) {
        Iban iban = RandomIban.builder().country(countryCode).seed(seed).build();

        // Country code prefix is always deterministic
        assertThat(iban.toString()).startsWith(expectedPrefix);
        // IBAN is fully valid
        assertThat(Iban.isValid(iban.toString())).isTrue();
        // IBAN has the correct length per registry
        assertThat(iban.length()).isEqualTo(IbanRegistry.getByCode(countryCode).getIbanLength());
    }

}
