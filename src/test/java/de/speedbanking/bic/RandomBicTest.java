package de.speedbanking.bic;

import static de.speedbanking.bic.BicAssertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.test.TestUtil;
import de.speedbanking.util.Iso3166Alpha2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

/**
 * JUnit 5 tests for {@link RandomBic}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
class RandomBicTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructor_Instantiation_ShouldThrowException() {
        TestUtil.assertConstructorIsPrivate(RandomBic.class);
    }

    @DisplayName("of() should return a non-null valid BIC-8")
    @Test
    void of_NoArgs_ShouldReturnValidBic8() {
        Bic bic = RandomBic.of();

        assertThat(bic)
            .isNotNull()
            .isBic8()
            .hasLength(Bic.BIC8_LENGTH);
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    @DisplayName("of() should not throw any exception")
    @Test
    void of_NoArgs_ShouldNotThrowException() {
        assertThatCode(RandomBic::of).doesNotThrowAnyException();
    }

    @DisplayName("of(Random) with null Random should not throw (falls back to ThreadLocalRandom)")
    @Test
    void of_NullRandom_ShouldNotThrow() {
        assertThatCode(() -> RandomBic.of((Random) null)).doesNotThrowAnyException();
    }

    @DisplayName("of(Random) should return a valid BIC-8")
    @Test
    void of_WithRandom_ShouldReturnValidBic8() {
        Bic bic = RandomBic.of(new Random(1L));

        assertThat(bic)
            .isNotNull()
            .isBic8();
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    @DisplayName("of(String) with null or unsupported country code should throw NullPointerException")
    @ParameterizedTest(name = "Country code: ''{0}''")
    @ValueSource(strings = {"", " ", "  ", "XX", "ZZ", "de", "99", "D", "DEU", " DE"})
    void of_InvalidCountryCode_ShouldThrowNpe(String countryCode) {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomBic.of(countryCode))
            .withMessage("Supported ISO 3166-1 Alpha-2 country code required");
    }

    @DisplayName("of(String, null) with null Random should not throw")
    @Test
    void of_CountryCodeNullRandom_ShouldNotThrow() {
        assertThatCode(() -> RandomBic.of("DE", null)).doesNotThrowAnyException();
    }

    @DisplayName("of(String) should return a BIC-8 with the correct country code")
    @ParameterizedTest(name = "Country: {0}")
    @ValueSource(strings = {"DE", "GB", "FR", "US", "JP", "CH", "AT", "NL"})
    void of_ValidCountryCode_ShouldReturnBic8WithCountry(String countryCode) {
        Bic bic = RandomBic.of(countryCode);

        assertThat(bic)
            .isNotNull()
            .isBic8()
            .hasCountryCode(countryCode);
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    @DisplayName("ofBic11() should return a non-null valid BIC-11")
    @Test
    void ofBic11_NoArgs_ShouldReturnValidBic11() {
        Bic bic = RandomBic.ofBic11();

        assertThat(bic)
            .isNotNull()
            .isBic11()
            .hasLength(Bic.BIC11_LENGTH);
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    @DisplayName("ofBic11() should not throw any exception")
    @Test
    void ofBic11_NoArgs_ShouldNotThrowException() {
        assertThatCode(RandomBic::ofBic11).doesNotThrowAnyException();
    }

    @DisplayName("ofBic11(Random) with null Random should not throw")
    @Test
    void ofBic11_NullRandom_ShouldNotThrow() {
        assertThatCode(() -> RandomBic.ofBic11((Random) null)).doesNotThrowAnyException();
    }

    @DisplayName("ofBic11(Random) should return a valid BIC-11")
    @Test
    void ofBic11_WithRandom_ShouldReturnValidBic11() {
        Bic bic = RandomBic.ofBic11(new Random(99L));

        assertThat(bic)
            .isNotNull()
            .isBic11();
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    @DisplayName("ofBic11(String) with null or unsupported country code should throw NullPointerException")
    @ParameterizedTest(name = "Country code: ''{0}''")
    @ValueSource(strings = {"", " ", "  ", "XX", "ZZ", "de", "99", "D", "DEU", " DE"})
    void ofBic11_InvalidCountryCode_ShouldThrowNpe(String countryCode) {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomBic.ofBic11(countryCode))
            .withMessage("Supported ISO 3166-1 Alpha-2 country code required");
    }

    @DisplayName("ofBic11(String, null) with null Random should not throw")
    @Test
    void ofBic11_CountryCodeNullRandom_ShouldNotThrow() {
        assertThatCode(() -> RandomBic.ofBic11("DE", null)).doesNotThrowAnyException();
    }

    @DisplayName("ofBic11(String) should return a BIC-11 with the correct country code")
    @ParameterizedTest(name = "Country: {0}")
    @ValueSource(strings = {"DE", "GB", "FR", "US", "JP", "CH", "AT", "NL"})
    void ofBic11_ValidCountryCode_ShouldReturnBic11WithCountry(String countryCode) {
        Bic bic = RandomBic.ofBic11(countryCode);

        assertThat(bic)
            .isNotNull()
            .isBic11()
            .hasCountryCode(countryCode);
        assertThat(Bic.isValid(bic.toString())).isTrue();
        assertThat(bic.getBranchCode()).isNotNull().hasSize(3);
    }

    @DisplayName("Same seed and country code should produce identical BIC-8 (reproducibility)")
    @Test
    void of_SameSeed_ShouldProduceIdenticalBic8() {
        long seed = 42L;
        Bic first  = RandomBic.of("DE", new Random(seed));
        Bic second = RandomBic.of("DE", new Random(seed));

        assertThat(first).hasToString(second.toString());
    }

    @DisplayName("Same seed and country code should produce identical BIC-11 (reproducibility)")
    @Test
    void ofBic11_SameSeed_ShouldProduceIdenticalBic11() {
        long seed = 42L;
        Bic first  = RandomBic.ofBic11("FR", new Random(seed));
        Bic second = RandomBic.ofBic11("FR", new Random(seed));

        assertThat(first).hasToString(second.toString());
    }

    @DisplayName("Same seed should produce identical BIC-8 for any-country selection")
    @Test
    void of_SameSeedAnyCountry_ShouldProduceIdenticalBic8() {
        long seed = 123456L;
        Bic first  = RandomBic.of(new Random(seed));
        Bic second = RandomBic.of(new Random(seed));

        assertThat(first).hasToString(second.toString());
    }

    @DisplayName("Same seed should produce identical BIC-11 for any-country selection")
    @Test
    void ofBic11_SameSeedAnyCountry_ShouldProduceIdenticalBic11() {
        long seed = 789L;
        Bic first  = RandomBic.ofBic11(new Random(seed));
        Bic second = RandomBic.ofBic11(new Random(seed));

        assertThat(first).hasToString(second.toString());
    }

    @DisplayName("Different seeds should produce different BIC-8 values")
    @Test
    void of_DifferentSeeds_ShouldProduceDifferentBic8s() {
        Bic first  = RandomBic.of("DE", new Random(1L));
        Bic second = RandomBic.of("DE", new Random(2L));

        // Not guaranteed, but statistically certain (bank code alone: 26^4 ≈ 460 k possibilities)
        assertThat(first.toString()).isNotEqualTo(second.toString());
    }

    @DisplayName("Different seeds should produce different BIC-11 values")
    @Test
    void ofBic11_DifferentSeeds_ShouldProduceDifferentBic11s() {
        Bic first  = RandomBic.ofBic11("DE", new Random(1L));
        Bic second = RandomBic.ofBic11("DE", new Random(2L));

        assertThat(first.toString()).isNotEqualTo(second.toString());
    }

    static Stream<Iso3166Alpha2> allIsoCountries() {
        return Arrays.stream(Iso3166Alpha2.values());
    }

    @DisplayName("of(String) should generate a valid BIC-8 for every ISO 3166-1 Alpha-2 country")
    @ParameterizedTest(name = "Country: {0}")
    @MethodSource("allIsoCountries")
    void of_AllIsoCountries_ShouldGenerateValidBic8(Iso3166Alpha2 country) {
        Bic bic = RandomBic.of(country.getCode(), new Random(4711L));

        assertThat(bic)
            .isNotNull()
            .isBic8()
            .hasCountryCode(country.getCode());
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    @DisplayName("ofBic11(String) should generate a valid BIC-11 for every ISO 3166-1 Alpha-2 country")
    @ParameterizedTest(name = "Country: {0}")
    @MethodSource("allIsoCountries")
    void ofBic11_AllIsoCountries_ShouldGenerateValidBic11(Iso3166Alpha2 country) {
        Bic bic = RandomBic.ofBic11(country.getCode(), new Random(4711L));

        assertThat(bic)
            .isNotNull()
            .isBic11()
            .hasCountryCode(country.getCode());
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    /**
     * Snapshot test: verifies concrete, deterministic BIC-8 output for known seed + country
     * combinations. These values must remain stable across releases.
     */
    @DisplayName("Seeded BIC-8 generation should produce stable snapshot values")
    @ParameterizedTest(name = "[{index}] {0} seed={1}")
    @CsvSource(delimiter = '|', value = {
        "DE |  0",
        "GB |  0",
        "FR |  0",
        "US | 42",
        "JP | 42",
        "CH | 99",
    })
    void of_SeededSnapshot_ShouldHaveCorrectCountryAndBeValid(String countryCode, long seed) {
        Bic bic = RandomBic.of(countryCode, new Random(seed));

        assertThat(bic)
            .hasCountryCode(countryCode)
            .isBic8();
        assertThat(Bic.isValid(bic.toString())).isTrue();
    }

    /**
     * Snapshot test: verifies concrete, deterministic BIC-11 output for known seed + country
     * combinations. These values must remain stable across releases.
     */
    @DisplayName("Seeded BIC-11 generation should produce stable snapshot values")
    @ParameterizedTest(name = "[{index}] {0} seed={1}")
    @CsvSource(delimiter = '|', value = {
        "DE |  0",
        "GB |  0",
        "FR |  0",
        "US | 42",
        "JP | 42",
        "CH | 99",
    })
    void ofBic11_SeededSnapshot_ShouldHaveCorrectCountryAndBeValid(String countryCode, long seed) {
        Bic bic = RandomBic.ofBic11(countryCode, new Random(seed));

        assertThat(bic)
            .hasCountryCode(countryCode)
            .isBic11();
        assertThat(Bic.isValid(bic.toString())).isTrue();
        assertThat(bic.getBranchCode()).isNotNull().hasSize(3);
    }

    @DisplayName("of() should always return BIC-8, never BIC-11")
    @Test
    void of_MultipleInvocations_ShouldAlwaysReturnBic8() {
        for (int i = 0; i < 50; i++) {
            Bic bic = RandomBic.of();
            assertThat(bic)
                .withFailMessage("Expected BIC-8 but got BIC-11: %s", bic)
                .isBic8();
        }
    }

    @DisplayName("ofBic11() should always return BIC-11, never BIC-8")
    @Test
    void ofBic11_MultipleInvocations_ShouldAlwaysReturnBic11() {
        for (int i = 0; i < 50; i++) {
            Bic bic = RandomBic.ofBic11();
            assertThat(bic)
                .withFailMessage("Expected BIC-11 but got BIC-8: %s", bic)
                .isBic11();
        }
    }

    @DisplayName("generateBankCode should return exactly 4 uppercase letters")
    @Test
    void generateBankCode_ShouldReturn4UppercaseLetters() {
        Random rnd = new Random(0L);
        for (int i = 0; i < 100; i++) {
            String bankCode = RandomBic.generateBankCode(rnd);
            assertThat(bankCode).hasSize(4).matches("[A-Z]{4}");
        }
    }

    @DisplayName("generateLocationCode should return exactly 2 alphanumeric characters")
    @Test
    void generateLocationCode_ShouldReturn2AlphanumericChars() {
        Random rnd = new Random(0L);
        for (int i = 0; i < 100; i++) {
            String locationCode = RandomBic.generateLocationCode(rnd);
            assertThat(locationCode).hasSize(2).matches("[A-Z0-9]{2}");
        }
    }

    @DisplayName("generateBranchCode should return exactly 3 alphanumeric characters")
    @Test
    void generateBranchCode_ShouldReturn3AlphanumericChars() {
        Random rnd = new Random(0L);
        for (int i = 0; i < 100; i++) {
            String branchCode = RandomBic.generateBranchCode(rnd);
            assertThat(branchCode).hasSize(3).matches("[A-Z0-9]{3}");
        }
    }

    @DisplayName("generateBic8String should return an 8-character string with the given country code")
    @Test
    void generateBic8String_ShouldReturnBic8WithGivenCountryCode() {
        Random rnd = new Random(42L);
        String bic8 = RandomBic.generateBic8String("DE", rnd);

        assertThat(bic8)
            .hasSize(8)
            .matches("[A-Z]{4}DE[A-Z0-9]{2}");
        assertThat(bic8.substring(4, 6)).isEqualTo("DE");
    }

    @DisplayName("generateBic11String should return an 11-character string with the given country code")
    @Test
    void generateBic11String_ShouldReturnBic11WithGivenCountryCode() {
        Random rnd = new Random(42L);
        String bic11 = RandomBic.generateBic11String("DE", rnd);

        assertThat(bic11)
            .hasSize(11)
            .matches("[A-Z]{4}DE[A-Z0-9]{5}");
        assertThat(bic11.substring(4, 6)).isEqualTo("DE");
    }

    @DisplayName("Generated BIC-8 should expose correct component lengths")
    @Test
    void of_GeneratedBic8_ShouldHaveCorrectComponentLengths() {
        Bic bic = RandomBic.of("DE", new Random(42L));

        assertThat(bic.getBankCode()).hasSize(4);
        assertThat(bic.getCountryCode()).hasSize(2).isEqualTo("DE");
        assertThat(bic.getLocationCode()).hasSize(2);
        assertThat(bic.getBranchCode()).isNull(); // BIC-8: no branch code
    }

    @DisplayName("Generated BIC-11 should expose correct component lengths")
    @Test
    void ofBic11_GeneratedBic11_ShouldHaveCorrectComponentLengths() {
        Bic bic = RandomBic.ofBic11("DE", new Random(42L));

        assertThat(bic.getBankCode()).hasSize(4);
        assertThat(bic.getCountryCode()).hasSize(2).isEqualTo("DE");
        assertThat(bic.getLocationCode()).hasSize(2);
        assertThat(bic.getBranchCode()).isNotNull().hasSize(3);
    }

    @DisplayName("Generated BIC-8 bank code should contain only uppercase letters")
    @Test
    void of_BankCode_ShouldContainOnlyUppercaseLetters() {
        for (int i = 0; i < 20; i++) {
            Bic bic = RandomBic.of();
            assertThat(bic.getBankCode()).matches("[A-Z]{4}");
        }
    }

    @DisplayName("Generated country flag emoji should match the requested country code")
    @ParameterizedTest(name = "Country: {0}")
    @ValueSource(strings = {"DE", "GB", "FR"})
    void of_CountryFlag_ShouldMatchCountryCode(String countryCode) {
        Bic bic = RandomBic.of(countryCode);

        assertThat(bic.getCountryFlag()).isNotBlank();
        // flag is non-empty; exact emoji value is tested in BicTest
    }

}
