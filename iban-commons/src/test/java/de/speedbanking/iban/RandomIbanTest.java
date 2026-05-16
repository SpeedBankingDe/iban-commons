package de.speedbanking.iban;

import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatInvalidIbanException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import de.speedbanking.iban.junit.jupiter.api.IbanAssertions;
import de.speedbanking.iban.util.IbanCharType;
import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Unit tests for {@link RandomIban}, covering both valid IBAN generation and the
 * invalid-IBAN-string API ({@code invalidString} / {@code sabotageIban}).
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:LeftCurly"})
@ResourceLock(IbanConfigTest.RESOURCE_NAME)
final class RandomIbanTest {

    @BeforeAll
    static void prepareConfig() {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).calculateNcd(true).build());
    }

    @AfterAll
    static void resetConfig() {
        IbanConfig.reset();
    }

    /**
     * Verifies that {@link RandomIban} cannot be instantiated even via reflection.
     */
    @Test
    void constructor_isPrivateAndThrows() throws Exception {
        Constructor<RandomIban> ctor = RandomIban.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (java.lang.reflect.InvocationTargetException ex) {
            assertThat(ex.getCause()).isInstanceOf(UnsupportedOperationException.class);
            return;
        }
        fail("Expected UnsupportedOperationException to be thrown");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"DE", "GB", "FR", "NL", "AT", "CH", "PL", "IT", "ES", "SE"})
    void of_shouldReturnValidIban_whenInvokedByCountryCode(String countryCode) {
        Iban iban = RandomIban.of(countryCode);

        IbanAssertions.assertThat(iban)
            .isNotNull()
            .hasCountryCode(countryCode);

        assertThat(IbanValidator.isValid(iban.toString()))
            .as("Expected valid IBAN for country %s but got: %s", countryCode, iban)
            .isTrue();
    }

    @Test
    void of_byCountryCode_unsupportedCode_throwsIllegalArgument() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> RandomIban.of("XX"))
            .withMessageContaining("XX");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = IbanRegistry.class, names = {"DE", "GB", "FR", "NL", "AT"})
    void of_byRegistry_returnsValidIban(IbanRegistry country) {
        Iban iban = RandomIban.of(country);

        IbanAssertions.assertThat(iban)
            .isNotNull();

        assertThat(IbanValidator.isValid(iban.toString()))
            .as("Expected valid IBAN for %s but got: %s", country, iban)
            .isTrue();
    }

    @Test
    void ofSepa_returnsValidIbanFromSepaCountry() {
        Set<IbanRegistry> sepaCountries = EnumSet.copyOf(IbanRegistry.getSepaCountries());

        for (int i = 0; i < 20; i++) {
            Iban iban = RandomIban.ofSepa();

            IbanAssertions.assertThat(iban).isNotNull();

            assertThat(IbanValidator.isValid(iban.toString()))
                .as("ofSepa() returned an invalid IBAN: %s", iban)
                .isTrue();

            IbanRegistry entry = IbanRegistry.getByCode(iban.getCountryCode());
            assertThat(sepaCountries)
                .as("ofSepa() returned IBAN for non-SEPA country %s", iban.getCountryCode())
                .contains(entry);
        }
    }

    @Test
    void any_returnsValidIban() {
        Iban iban = RandomIban.any();

        IbanAssertions.assertThat(iban)
            .isNotNull();
        assertThat(IbanValidator.isValid(iban.toString())).isTrue();
        assertThat(IbanValidator.isValid(iban)).isTrue();

        IbanAssertions.assertThatIbanIsValid(iban.toString());

    }

    @Test
    void builder_country_byString_buildsValidIban() {
        Iban iban = RandomIban.builder().country("DE").build();

        IbanAssertions.assertThat(iban)
            .isNotNull()
            .hasCountryCode("DE");
        assertThat(IbanValidator.isValid(iban.toString())).isTrue();
    }

    @Test
    void builder_country_byString_unsupportedCode_throwsIllegalArgument() {
        // exercises Builder.requireSupportedCode() including null-result branch
        assertThatIllegalArgumentException()
            .isThrownBy(() -> RandomIban.builder().country("XX").build())
            .withMessageContaining("XX");
    }

    @Test
    void builder_country_byRegistry_buildsValidIban() {
        Iban iban = RandomIban.builder().country(IbanRegistry.FR).build();

        IbanAssertions.assertThat(iban)
            .isNotNull()
            .hasCountryCode("FR");
        assertThat(IbanValidator.isValid(iban.toString())).isTrue();
    }

    @Test
    void builder_country_nullRegistry_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.builder().country((IbanRegistry) null));
    }

    @Test
    void builder_sepaOnly_resolvesToSepaCountry() {
        // exercises the sepaOnly() method and the SEPA branch in resolveCountry()
        Set<IbanRegistry> sepaCountries = EnumSet.copyOf(IbanRegistry.getSepaCountries());

        for (int i = 0; i < 20; i++) {
            Iban iban = RandomIban.builder().sepaOnly().build();

            IbanAssertions.assertThat(iban).isNotNull();
            assertThat(IbanValidator.isValid(iban.toString())).isTrue();
            IbanRegistry entry = IbanRegistry.getByCode(iban.getCountryCode());
            assertThat(sepaCountries)
                .as("builder().sepaOnly() returned IBAN for non-SEPA country %s", iban.getCountryCode())
                .contains(entry);
        }
    }

    /** {@code sepaOnly()} called after {@code country()} must clear the fixed-country flag. */
    @Test
    void builder_sepaOnly_afterCountry_clearsFixedCountry() {
        Set<IbanRegistry> sepaCountries = EnumSet.copyOf(IbanRegistry.getSepaCountries());
        Iban iban = RandomIban.builder().country("DE").sepaOnly().build();
        IbanRegistry entry = IbanRegistry.getByCode(iban.getCountryCode());
        assertThat(sepaCountries).contains(entry);
    }

    /** {@code country()} called after {@code sepaOnly()} must clear the SEPA flag. */
    @Test
    void builder_country_afterSepaOnly_clearsSEPAFlag() {
        for (int i = 0; i < 10; i++) {
            Iban iban = RandomIban.builder().sepaOnly().country("PL").build();
            assertThat(iban.getCountryCode()).isEqualTo("PL");
        }
    }

    @Test
    void builder_seed_isDeterministic() {
        Iban first  = RandomIban.builder().country("DE").seed(7L).build();
        Iban second = RandomIban.builder().country("DE").seed(7L).build();

        assertThat(first.toString()).isEqualTo(second.toString());
    }

    @Test
    void builder_random_null_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.builder().random(null));
    }

    /**
     * Countries with a National Check Digit (e.g. NO, BE, IT) exercise the
     * {@code fixNationalCheckDigit()} branch inside {@link RandomIban#generate}.
     * Enables NCD calculation, generates IBANs for all NCD countries, and confirms validity.
     * Covers lines 584–585 and the entire {@code fixNationalCheckDigit} method.
     */
    @Test
    void generate_withNcdEnabled_producesValidIban() {
        try {
            IbanRegistry[] ncdCountries = Arrays.stream(IbanRegistry.ALL_COUNTRIES)
                .filter(r -> r.getNationalCheckDigitIndexRange() != null)
                .toArray(IbanRegistry[]::new);

            assertThat(ncdCountries).as("expected at least one NCD country in registry").isNotEmpty();

            Random rnd = new Random(42L);
            for (IbanRegistry country : ncdCountries) {
                Iban iban = RandomIban.generate(country, rnd);
                assertThat(IbanValidator.isValid(iban.toString()))
                    .as("NCD-enabled generation produced invalid IBAN for %s: %s", country, iban)
                    .isTrue();
            }
        } finally {
            IbanConfig.reset(IbanConfig.DEFAULT);
        }
    }

    /** Countries without an NCD field must return the StringBuilder unchanged. */
    @Test
    void fixNationalCheckDigit_noNcdField_returnsUnchanged() {
        assertThat(IbanRegistry.DE.getNationalCheckDigitIndexRange()).isNull();

        StringBuilder sb = new StringBuilder("DE89370400440532013000");
        StringBuilder result = RandomIban.fixNationalCheckDigit(IbanRegistry.DE, sb);

        assertThat(result).isSameAs(sb);
        assertThat(result.toString()).isEqualTo("DE89370400440532013000");
    }

    /** A length mismatch must throw. */
    @Test
    void fixNationalCheckDigit_wrongLength_throws() {
        assertThatInvalidIbanException()
            .isThrownBy(() -> RandomIban.fixNationalCheckDigit(IbanRegistry.DE, new StringBuilder("DE00")))
            .withMessageStartingWith("IBAN has incorrect length for specified country");
    }

    @Test
    void fixNationalCheckDigit_nullCountryData_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.fixNationalCheckDigit(null, new StringBuilder()));
    }

    @Test
    void fixNationalCheckDigit_nullIbanBuilder_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.fixNationalCheckDigit(IbanRegistry.DE, null));
    }

    /**
     * For countries that actually have an NCD field, {@code fixNationalCheckDigit} must write
     * the computed value and the result must pass validation.
     * This exercises lines 697–703 (the NCD write loop).
     */
    @Test
    void fixNationalCheckDigit_ncdCountry_writesCheckDigit() {
        try {
            IbanRegistry[] ncdCountries = Arrays.stream(IbanRegistry.ALL_COUNTRIES)
                .filter(r -> r.getNationalCheckDigitIndexRange() != null)
                .toArray(IbanRegistry[]::new);
            assertThat(ncdCountries).isNotEmpty();

            Random rnd = new Random(1L);
            for (IbanRegistry country : ncdCountries) {
                // Build an IBAN with placeholder NCD ("00" at NCD position)
                StringBuilder sb = new StringBuilder(country.getCountryCode())
                    .append("00")
                    .append(RandomIban.generateRandomBban(country.getBbanPatternStr(), rnd));

                RandomIban.fixNationalCheckDigit(country, sb);

                // The result must be the correct length and ultimately produce a valid IBAN
                assertThat(sb.length()).isEqualTo(country.getIbanLength());
            }
        } finally {
            IbanConfig.reset(IbanConfig.DEFAULT);
        }
    }

    /**
     * The {@code else} branch in {@link RandomIban#generateRandomSegment} throws
     * {@link IllegalStateException} for any {@link IbanCharType} outside the three known values.
     * Passes a {@code Segment} with a {@code null} CharType (simulating an unrecognised type).
     */
    @Test
    void generateRandomSegment_unknownCharType_throwsIllegalStateException() {
        IbanPatternConverter.Segment segment = Segment.of(null, 5);

        assertThatThrownBy(() -> RandomIban.generateRandomSegment(segment, new Random(0)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("null");
    }

    @Test
    void generateRandomSegment_numericType_producesOnlyDigits() {
        IbanPatternConverter.Segment segment = Segment.of(IbanCharType.NUMERIC, 20);
        String result = RandomIban.generateRandomSegment(segment, new Random(0));

        assertThat(result).hasSize(20).matches("[0-9]+");
    }

    @Test
    void generateRandomSegment_alphabeticType_producesOnlyLetters() {
        IbanPatternConverter.Segment segment = Segment.of(IbanCharType.ALPHABETIC, 10);
        String result = RandomIban.generateRandomSegment(segment, new Random(0));

        assertThat(result).hasSize(10).matches("[A-Z]+");
    }

    @Test
    void generateRandomSegment_alphanumericType_producesAlphanumeric() {
        IbanPatternConverter.Segment segment = Segment.of(IbanCharType.ALPHANUMERIC, 15);
        String result = RandomIban.generateRandomSegment(segment, new Random(0));

        assertThat(result).hasSize(15).matches("[A-Z0-9]+");
    }

    @Test
    void generateRandomSegment_nullSegment_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.generateRandomSegment(null, new Random()));
    }

    @Test
    void generateRandomSegment_nullRandom_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.generateRandomSegment(
                Segment.of(IbanCharType.NUMERIC, 4), null));
    }

    /**
     * Strategy 5 fallback: when the IBAN string is already at exactly {@code MIN_IBAN_BASE_LENGTH},
     * truncation is skipped and the first check digit is corrupted instead (lines 386–388).
     */
    @Test
    void sabotageIban_strategy5_alreadyShort_corruptsCheckDigit() {
        int minLen = IbanRegistry.MIN_IBAN_LENGTH;

        // exactly minLen characters - length guard prevents truncation
        char[] zeros = new char[minLen - 4];
        Arrays.fill(zeros, '0');
        String shortIban = "DE00" + new String(zeros);
        assertThat(shortIban).hasSize(minLen);

        Random rnd = findSeedForStrategy(5, 6);

        StringBuilder sb = new StringBuilder(shortIban);
        String before = sb.toString();

        RandomIban.sabotageIban(sb, rnd);

        // length unchanged — fallback must not truncate
        assertThat(sb.length()).isLessThan(shortIban.length());
        // but the content must differ (check digit was incremented)
        assertThat(sb.toString()).isNotEqualTo(before);
    }

    /**
     * Strategy 5 normal path: a longer IBAN is truncated below {@code MIN_IBAN_BASE_LENGTH}.
     */
    @Test
    void sabotageIban_strategy5_longIban_truncates() {
        int minLen = IbanRegistry.MIN_IBAN_BASE_LENGTH;

        String validIban = RandomIban.any().toString();
        assertThat(validIban.length()).isGreaterThan(minLen);

        Random rnd = findSeedForStrategy(5, 6);

        StringBuilder sb = new StringBuilder(validIban);
        RandomIban.sabotageIban(sb, rnd);

        assertThat(sb.length()).isLessThan(validIban.length());
    }

    @Test
    void sabotageIban_nullIban_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.sabotageIban(null, ThreadLocalRandom.current()));
    }

    @Test
    void sabotageIban_shortIban_throwsIAE() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> RandomIban.sabotageIban(new StringBuilder("FOO"), ThreadLocalRandom.current()));
    }

    @Test
    void sabotageIban_nullRandom_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.sabotageIban(new StringBuilder("DE89370400440532013000"), null));
    }

    /**
     * Runs 600 sabotage calls (one per valid IBAN, seeded for reproducibility) and verifies
     * that at least 98 % produce an invalid result.
     */
    @Test
    void sabotageIban_allStrategiesProduceInvalidOutput() {
        Random random = new Random(0L);
        int invalidCount = 0;

        for (int i = 0; i < 600; i++) {
            String valid = RandomIban.builder().random(new Random(i)).build().toString();
            StringBuilder sb = new StringBuilder(valid);
            RandomIban.sabotageIban(sb, random);

            if (!IbanValidator.isValid(sb.toString())) {
                invalidCount++;
            }
        }

        assertThat(invalidCount)
            .as("at least 98%% of sabotaged IBANs must be invalid")
            .isGreaterThanOrEqualTo(588);
    }

    /**
     * Verifies that at least three distinct corrupted outputs emerge from the same IBAN
     * over 60 iterations, confirming strategy diversity.
     */
    @Test
    void sabotageIban_producesVariedCorruptions() {
        String valid = RandomIban.builder().country(IbanRegistry.DE).seed(1L).build().toString();
        Set<String> corruptions = new HashSet<>();
        Random random = new Random(99L);

        for (int i = 0; i < 60; i++) {
            StringBuilder sb = new StringBuilder(valid);
            RandomIban.sabotageIban(sb, random);
            corruptions.add(sb.toString());
        }

        assertThat(corruptions)
            .as("sabotage must produce varied corruptions (expected >= 3 distinct variants)")
            .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void invalidString_noArgs_returnsInvalidIban() {
        String invalid = RandomIban.invalidString();

        assertThat(invalid)
            .as("invalidString() must not return null")
            .isNotNull();

        assertThat(IbanValidator.isValid(invalid))
            .as("invalidString() must return a string that fails IBAN validation, but got: %s", invalid)
            .isFalse();
    }

    @RepeatedTest(100)
    void invalidString_shouldAlwaysBeInvalid_whenInvokedWithoutArgs() {
        assertThat(IbanValidator.isValid(RandomIban.invalidString()))
            .as("RandomIban.invalidString() should never produce a valid IBAN")
            .isFalse();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"DE", "GB", "FR", "NL", "AT", "CH", "PL", "IT", "ES", "SE"})
    void invalidString_shouldReturnInvalidIban_whenInvokedByCountryCode(String countryCode) {
        String invalid = RandomIban.invalidString(countryCode);

        assertThat(invalid)
            .as("Result for country code %s must not be null", countryCode)
            .isNotNull();

        assertThat(IbanValidator.isValid(invalid))
            .as("Expected invalid IBAN for country %s but got: %s", countryCode, invalid)
            .isFalse();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = IbanRegistry.class, names = {"DE", "GB", "FR", "NL", "AT"})
    void invalidString_shouldReturnInvalidIban_whenInvokedByRegistry(IbanRegistry country) {
        String invalid = RandomIban.invalidString(country);

        assertThat(invalid)
            .as("Result for registry entry %s must not be null", country)
            .isNotNull();

        assertThat(IbanValidator.isValid(invalid))
            .as("Expected invalid IBAN for %s but got: %s", country, invalid)
            .isFalse();
    }

    @Test
    void invalidString_shouldReturnInvalidIban_whenCreatedFromValidIbanString() {
        String valid = RandomIban.any().toString();
        String invalid = RandomIban.invalidString(valid, ThreadLocalRandom.current());

        assertThat(invalid)
            .as("Resulting invalid string must not be null")
            .isNotNull();

        assertThat(IbanValidator.isValid(invalid))
            .as("invalidString(String, Random) should produce an invalid IBAN from: %s, but got: %s", valid, invalid)
            .isFalse();
    }

    @RepeatedTest(200)
    void invalidString_shouldAlwaysBeInvalid_whenCreatedFromValidIbanString() {
        String valid = RandomIban.any().toString();
        String invalid = RandomIban.invalidString(valid, ThreadLocalRandom.current());

        assertThat(IbanValidator.isValid(invalid))
            .as("Repeated check: generated IBAN %s must be invalid", invalid)
            .isFalse();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void invalidString_shouldReturnInvalidIban_forAllRegistryCountries(IbanRegistry country) {
        String invalid = RandomIban.invalidString(country);

        assertThat(invalid)
            .as("Invalid IBAN for country %s must not be null", country.getCountryCode())
            .isNotNull();

        assertThat(IbanValidator.isValid(invalid))
            .as("Expected invalid IBAN for country %s but got: %s", country.getCountryCode(), invalid)
            .isFalse();
    }

    // -------------------------------------------------------------------------
    // invalidString() — determinism with a fixed seed
    // -------------------------------------------------------------------------

    @Test
    void invalidString_deterministicWithFixedSeed() {
        Random r1 = new Random(42L);
        Random r2 = new Random(42L);

        String first  = RandomIban.invalidString(IbanRegistry.DE, r1);
        String second = RandomIban.invalidString(IbanRegistry.DE, r2);

        assertThat(first).isEqualTo(second);
    }

    // -------------------------------------------------------------------------
    // invalidString() — null / bad input guards
    // -------------------------------------------------------------------------

    @Test
    void invalidString_nullCountryData_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.invalidString((IbanRegistry) null));
    }

    @Test
    void invalidString_unsupportedCountryCode_throwsIllegalArgument() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> RandomIban.invalidString("XX"));
    }

    @Test
    void invalidString_nullIbanString_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.invalidString((String) null, ThreadLocalRandom.current()));
    }

    @Test
    void invalidString_nullRandom_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> RandomIban.invalidString(IbanRegistry.DE, null));
    }

    // -------------------------------------------------------------------------
    // sabotageIban() — strategy coverage
    // -------------------------------------------------------------------------

    /**
     * The {@code invalidString} family must guarantee an invalid result regardless of the
     * sabotage strategy. Exercises the retry loop at scale.
     */
    @RepeatedTest(100)
    void invalidString_largeRandomBatch_neverValid() {
        Random random = new Random();
        for (int i = 0; i < 500; i++) {
            String invalid = RandomIban.invalidString(random);
            assertThat(IbanValidator.isValid(invalid))
                .as("Found a valid IBAN returned by invalidString(): %s", invalid)
                .isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Brute-forces a {@link Random} seed whose first {@code nextInt(bound)} call returns
     * {@code target}. Used to pin a specific sabotage strategy in deterministic tests.
     */
    private static Random findSeedForStrategy(int target, int bound) {
        for (long seed = 0; seed < 1_000_000; seed++) {
            if (new Random(seed).nextInt(bound) == target) {
                return new Random(seed);
            }
        }
        throw new IllegalStateException(
            "Could not find a seed producing nextInt(" + bound + ") == " + target);
    }
}

