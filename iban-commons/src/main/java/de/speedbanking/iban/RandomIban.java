/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.iban;

import static java.util.Objects.requireNonNull;

import de.speedbanking.iban.util.IbanCharType;
import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.util.Country;
import de.speedbanking.util.IndexRange;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating random International Bank Account Numbers (IBANs)
 * based on the country-specific Basic Bank Account Number (BBAN) structure defined
 * in the {@link IbanRegistry}.
 * <p>
 * This class ensures that the generated IBAN is syntactically correct, matches the
 * national BBAN structure, and has a valid check digit calculated according to the
 * ISO 7064 Mod 97-10 algorithm.
 * <p>
 * <strong>Preferred API — fluent builder:</strong>
 * <pre>{@code
 * // any country, non-reproducible
 * Iban iban = RandomIban.builder().build();
 *
 * // fixed country, deterministic via seed
 * Iban iban = RandomIban.builder()
 *     .country("DE")
 *     .seed(42L)
 *     .build();
 *
 * // random SEPA country, deterministic via seed
 * Iban iban = RandomIban.builder()
 *     .sepaOnly()
 *     .seed(42L)
 *     .build();
 * }</pre>
 * <p>
 * <strong>Convenience static methods</strong> for simple, non-reproducible generation:
 * <pre>{@code
 * Iban iban = RandomIban.of("IT");
 * Iban iban = RandomIban.of(IbanRegistry.PL);
 * Iban iban = RandomIban.ofSepa();
 * }</pre>
 * <p>
 * <strong>Invalid IBAN strings</strong> (strings only — not {@link Iban} objects, which are
 * always valid by definition) can be produced via:
 * <pre>{@code
 * String invalid = RandomIban.invalidString();
 * String invalid = RandomIban.invalidString("DE");
 * String invalid = RandomIban.invalidString(IbanRegistry.DE);
 * String invalid = RandomIban.invalidString(validIbanString, random);
 * }</pre>
 */
public final class RandomIban {

    private static final String         DIGITS         = "0123456789";
    private static final String         LETTERS        = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String         ALPHANUMERIC   = DIGITS + LETTERS;

    /**
     * Cached array of SEPA-country registry entries.
     * <p>
     * {@link IbanRegistry#getSepaCountries()} builds a new list on every call; caching the
     * result as an array avoids repeated allocation in {@link Builder#resolveCountry(Random)}
     * and {@link #ofSepa()}.
     */
    private static final IbanRegistry[] SEPA_COUNTRIES = IbanRegistry.getSepaCountries().toArray(new IbanRegistry[0]);

    /**
     * Strategies for intentional IBAN corruption used to generate negative test data.
     * <p>
     * Each constant represents a specific way to violate IBAN structural or logical rules.
     */
    private enum SabotageStrategy {

        /** Increments a check digit to cause a Mod-97 validation failure. */
        CORRUPT_CHECK_DIGIT,

        /** Replaces the country code with an undefined ISO code (e.g., "XY"). */
        INVALID_COUNTRY_CODE,

        /** Replaces the country code with a valid but mismatched ISO code from another country. */
        MISMATCHED_COUNTRY_CODE,

        /** Injects an alphabetic character into a strictly numeric BBAN section. */
        INJECT_LETTER_INTO_BBAN,

        /** Swaps two adjacent characters (transposition error). */
        TRANSPOSE_CHARACTERS,

        /** Truncates the IBAN to a length below the minimum required 15 characters. */
        TRUNCATE_IBAN;

        /** Cached array of values to avoid repeated heap allocation by {@code values()}. */
        private static final SabotageStrategy[] VALUES = values();

        /**
         * Picks a random sabotage strategy using the provided random generator.
         *
         * @param random the random generator to use; must not be null
         * @return a random strategy
         */
        static SabotageStrategy getRandom(Random random) {
            return VALUES[random.nextInt(VALUES.length)];
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private RandomIban() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    // -------------------------------------------------------------------------
    // Valid IBAN generation
    // -------------------------------------------------------------------------

    /**
     * Generates a random, valid IBAN for the country identified by the given two-letter code.
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryCode the two-letter country code (e.g., {@code "DE"})
     * @return a valid, randomly generated IBAN
     * @throws IllegalArgumentException if {@code countryCode} is not supported
     */
    public static Iban of(String countryCode) {
        return builder().country(countryCode).build();
    }

    /**
     * Generates a random, valid IBAN based on the given {@link IbanRegistry} entry.
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryData the registry entry defining the country's IBAN structure;
     *                    must not be {@code null}
     * @return a valid, randomly generated IBAN
     */
    public static Iban of(IbanRegistry countryData) {
        return builder().country(countryData).build();
    }

    /**
     * Generates a random, valid IBAN from a randomly selected country in the registry.
     * <p>
     * This method serves as a convenience wrapper for the builder, picking a random
     * country supported by the underlying {@link IbanRegistry}. It uses
     * {@link java.util.concurrent.ThreadLocalRandom} for efficient, thread-safe
     * generation of the random components.
     *
     * @return a valid, randomly generated IBAN from any supported country
     */
    public static Iban any() {
        return builder().build();
    }

    /**
     * Generates a random, valid IBAN for any supported SEPA country.
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @return a valid, randomly generated SEPA IBAN
     */
    public static Iban ofSepa() {
        return builder().sepaOnly().build();
    }

    /**
     * Returns a new {@link Builder} for fluent IBAN construction.
     * <p>
     * Defaults: random country, {@link ThreadLocalRandom} as randomness source.
     *
     * @return a new {@code Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Invalid IBAN string generation
    // -------------------------------------------------------------------------

    /**
     * Generates a deliberately invalid IBAN string from a randomly selected country.
     * <p>
     * Starts with a valid IBAN and corrupts it using a randomly chosen sabotage strategy
     * (see {@link #sabotageIban(StringBuilder, Random)}) until validation fails.
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     * <p>
     * The return type is {@link String} rather than {@link Iban}: {@code Iban} objects are
     * always valid by definition; only plain strings may represent invalid IBANs.
     *
     * @return a string that fails {@link IbanValidator#isValid(String)} validation
     *
     * @since 1.8.6
     */
    public static String invalidString() {
        return invalidString(ThreadLocalRandom.current());
    }

    /**
     * Generates a deliberately invalid IBAN string for the country identified by the given
     * two-letter country code.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryCode the two-letter country code (e.g., {@code "DE"})
     * @return a string that fails {@link IbanValidator#isValid(String)} validation
     * @throws IllegalArgumentException if {@code countryCode} is not supported
     *
     * @since 1.8.6
     */
    public static String invalidString(String countryCode) {
        IbanRegistry country = IbanRegistry.getByCode(countryCode);
        if (country == null) {
            throw new IllegalArgumentException("Unsupported country code: " + countryCode);
        }
        return invalidString(country);
    }

    /**
     * Generates a deliberately invalid IBAN string for the given {@link IbanRegistry} entry.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryData the registry entry; must not be {@code null}
     * @return a string that fails {@link IbanValidator#isValid(String)} validation
     *
     * @since 1.8.6
     */
    public static String invalidString(IbanRegistry countryData) {
        return invalidString(countryData, ThreadLocalRandom.current());
    }

    /**
     * Generates a deliberately invalid IBAN string for the given {@link IbanRegistry} entry,
     * using the provided {@link Random} source.
     *
     * @param countryData the registry entry; must not be {@code null}
     * @param random      the source of randomness; must not be {@code null}
     * @return a string that fails {@link IbanValidator#isValid(String)} validation
     *
     * @since 1.8.6
     */
    public static String invalidString(IbanRegistry countryData, Random random) {
        requireNonNull(countryData, "countryData must not be null");
        requireRandom(random);
        String valid = generate(countryData, random).toString();
        return sabotageUntilInvalid(valid, random);
    }

    /**
     * Corrupts the given valid IBAN string until it fails validation.
     * <p>
     * Uses the provided {@link Random} instance; the sabotage strategy is chosen randomly
     * via {@link #sabotageIban(StringBuilder, Random)}.
     *
     * @param validIban a syntactically and checksum-valid IBAN string; must not be {@code null}
     * @param random    the source of randomness; must not be {@code null}
     * @return a string derived from {@code validIban} that fails {@link IbanValidator#isValid(String)}
     *
     * @since 1.8.6
     */
    public static String invalidString(String validIban, Random random) {
        requireNonNull(validIban, "validIban must not be null");
        requireRandom(random);
        return sabotageUntilInvalid(validIban, random);
    }

    /**
     * Generates a deliberately invalid IBAN string from a randomly selected country,
     * using the provided {@link Random} source.
     *
     * @param random the source of randomness; must not be {@code null}
     * @return a string that fails {@link IbanValidator#isValid(String)} validation
     *
     * @since 1.8.6
     */
    static String invalidString(Random random) {
        requireRandom(random);
        IbanRegistry country = IbanRegistry.ALL_COUNTRIES[random.nextInt(IbanRegistry.ALL_COUNTRIES.length)];
        return invalidString(country, random);
    }

    /**
     * Applies {@link #sabotageIban(StringBuilder, Random)} in a loop until
     * {@link IbanValidator#isValid(String)} returns {@code false}.
     * <p>
     * Most sabotage strategies produce an invalid result on the first attempt; the loop
     * guards against the rare case where a mutation happens to preserve validity (e.g.
     * a transposition that does not change the checksum outcome).
     *
     * @param validIban a valid IBAN string; must not be {@code null}
     * @param random    the source of randomness; must not be {@code null}
     * @return a string derived from {@code validIban} that fails IBAN validation
     *
     * @since 1.8.6
     */
    static String sabotageUntilInvalid(String validIban, Random random) {
        StringBuilder sb = new StringBuilder(validIban);
        do {
            // reset to original before each attempt so cumulative mutations don't
            // accidentally cancel each other out (e.g. two transpositions undoing each other)
            sb.replace(0, sb.length(), validIban);
            sabotageIban(sb, random);
        } while (IbanValidator.isValid(sb.toString()));
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Sabotage
    // -------------------------------------------------------------------------

    /**
     * Corrupts the given IBAN in-place using a randomly selected sabotage strategy.
     * <p>
     * The method modifies the {@link StringBuilder} <em>in-place</em> and returns it.
     * Six strategies are applied with equal probability (1/6 each):
     * <ol>
     *   <li><strong>Tamper check digit</strong> – one of the two check digits (position 3 or 4)
     *       is incremented by 1 (wrapping: {@code '9'} → {@code '0'}) to trigger a Mod-97
     *       checksum failure.</li>
     *   <li><strong>Invalid country code</strong> – the first two characters are replaced with
     *       {@code "XY"}, a non-registered ISO 3166 Alpha-2 code.</li>
     *   <li><strong>Mismatched ISO code</strong> – the first two characters are replaced with a
     *       valid but randomly chosen {@link Country} code that does not match the
     *       existing BBAN format.</li>
     *   <li><strong>Structural violation in BBAN</strong> – a letter ({@code 'A'}) is injected
     *       at a random position within the BBAN section to break the expected character
     *       pattern.</li>
     *   <li><strong>Transposition error</strong> – two adjacent characters are swapped,
     *       simulating a classic human keying mistake.</li>
     *   <li><strong>Illegal length</strong> – the string is truncated to below
     *       {@link IbanRegistry#MIN_IBAN_BASE_LENGTH}, making it structurally invalid.</li>
     * </ol>
     *
     * @param iban   the IBAN to corrupt; modified in-place; must not be {@code null}
     * @param random the source of randomness; must not be {@code null}
     * @return the same {@link StringBuilder} instance after mutation
     * @throws IllegalArgumentException if {@code iban} or {@code random} is {@code null}
     *
     * @since 1.8.6
     */
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    static StringBuilder sabotageIban(StringBuilder iban, Random random) {
        requireNonNull(iban, "iban must not be null");
        if (iban.length() < IbanRegistry.MIN_IBAN_LENGTH) {
            throw new IllegalArgumentException("IBAN length must be at least " + IbanRegistry.MIN_IBAN_LENGTH);
        }
        requireRandom(random);

        SabotageStrategy strategy = SabotageStrategy.getRandom(random);
        switch (strategy) {
            case CORRUPT_CHECK_DIGIT:
                // increment one check digit (position 3 or 4) to trigger a Mod-97 failure
                int cdIdx = random.nextBoolean() ? IbanRegistry.INDEX_CHECK_DIGIT1 : IbanRegistry.INDEX_CHECK_DIGIT2;
                char c = iban.charAt(cdIdx);
                iban.setCharAt(cdIdx, c == '9' ? '0' : (char) (c + 1));
                break;
            case INVALID_COUNTRY_CODE:
                iban.setCharAt(0, 'X');
                iban.setCharAt(1, 'Y');
                break;
            case MISMATCHED_COUNTRY_CODE:
                Country[] countries = Country.values();
                String randomIso = countries[random.nextInt(countries.length)].name();
                iban.setCharAt(0, randomIso.charAt(0));
                iban.setCharAt(1, randomIso.charAt(1));
                break;
            case INJECT_LETTER_INTO_BBAN:
                int pos = IbanRegistry.INDEX_BBAN + random.nextInt(iban.length() - IbanRegistry.INDEX_BBAN);
                iban.setCharAt(pos, 'A');
                break;
            case TRANSPOSE_CHARACTERS:
                int p = random.nextInt(iban.length() - 1);
                char tmp = iban.charAt(p);
                iban.setCharAt(p, iban.charAt(p + 1));
                iban.setCharAt(p + 1, tmp);
                break;
            case TRUNCATE_IBAN:
                iban.setLength(iban.length() - 1);
                break;
        }
        return iban;
    }

    /**
     * Ensures that the provided Random instance is not null.
     *
     * @param random the Random instance to check
     * @return the non-null Random instance
     * @throws NullPointerException if random is null
     */
    private static Random requireRandom(Random random) {
        return requireNonNull(random, "random must not be null");
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Fluent builder for generating random, valid IBANs.
     * <p>
     * Instances are <em>not</em> thread-safe; create one builder per thread
     * (or per generation request).
     *
     * <pre>{@code
     * // any country, non-reproducible
     * Iban iban = RandomIban.builder().build();
     *
     * // fixed country, deterministic
     * Iban iban = RandomIban.builder()
     *     .country("DE")
     *     .seed(42L)
     *     .build();
     *
     * // random SEPA country
     * Iban iban = RandomIban.builder()
     *     .sepaOnly()
     *     .build();
     * }</pre>
     *
     * @since 1.8.5
     */
    public static final class Builder {

        private IbanRegistry countryData = null; // null = pick randomly at build time
        private boolean      sepaOnly    = false;
        private Random       random      = null; // null = ThreadLocalRandom at build time

        /** Package-private — use {@link RandomIban#builder()}. */
        Builder() {}

        /**
         * Restricts generation to the country identified by the given two-letter ISO code.
         * <p>
         * Clears any previous {@link #sepaOnly()} constraint.
         *
         * @param countryCode two-letter country code (e.g., {@code "DE"})
         * @return this builder
         * @throws IllegalArgumentException if {@code countryCode} is not supported
         */
        public Builder country(String countryCode) {
            return country(requireSupportedCode(countryCode));
        }

        /**
         * Restricts generation to the given registry entry.
         * <p>
         * Clears any previous {@link #sepaOnly()} constraint.
         *
         * @param registry the registry entry; must not be {@code null}
         * @return this builder
         */
        public Builder country(IbanRegistry registry) {
            this.countryData = requireNonNull(registry, "registry must not be null");
            this.sepaOnly = false;
            return this;
        }

        /**
         * Restricts generation to SEPA countries (randomly chosen at {@link #build()} time).
         * <p>
         * Clears any previously set fixed country.
         *
         * @return this builder
         */
        public Builder sepaOnly() {
            this.sepaOnly = true;
            this.countryData = null;
            return this;
        }

        /**
         * Uses the given {@link Random} instance as the source of randomness.
         * <p>
         * For reproducible output pass a seeded instance:
         * <pre>{@code builder.random(new Random(42L)) }</pre>
         *
         * @param random the {@link Random} instance; must not be {@code null}
         * @return this builder
         */
        public Builder random(Random random) {
            this.random = requireRandom(random);
            return this;
        }

        /**
         * Convenience shorthand for {@code random(new Random(seed))}.
         * <p>
         * The same seed always produces the same IBAN for the same country configuration,
         * <em>provided</em> neither {@link #random(Random)} nor {@link #seed(long)} is called
         * again afterwards — the last call always wins.
         *
         * @param seed the seed value
         * @return this builder
         */
        public Builder seed(long seed) {
            return random(new Random(seed));
        }

        /**
         * Generates and returns a random, valid IBAN according to the current builder state.
         *
         * @return a valid, randomly generated {@link Iban}
         */
        public Iban build() {
            Random rnd = random != null ? random : ThreadLocalRandom.current();
            IbanRegistry cd = resolveCountry(rnd);
            return generate(cd, rnd);
        }

        /**
         * Looks up the {@link IbanRegistry} entry for the given country code and throws
         * an {@link IllegalArgumentException} if the code is not supported.
         *
         * @param countryCode the two-letter country code to look up
         * @return the corresponding {@link IbanRegistry} entry; never {@code null}
         * @throws IllegalArgumentException if {@code countryCode} is not a supported country code
         */
        private static IbanRegistry requireSupportedCode(String countryCode) {
            IbanRegistry entry = IbanRegistry.getByCode(countryCode);
            if (entry == null) {
                throw new IllegalArgumentException("Unsupported country code: " + countryCode);
            }
            return entry;
        }

        /**
         * Resolves the country to be used for IBAN generation.
         * <p>
         * If a specific country was set, it is returned. Otherwise, a random country
         * is selected from either the SEPA-only list or all available countries.
         *
         * @param rnd the random number generator to use
         * @return a non-null {@link IbanRegistry} entry
         */
        private IbanRegistry resolveCountry(Random rnd) {
            if (countryData != null) {
                return countryData;
            } else if (sepaOnly) {
                return SEPA_COUNTRIES[rnd.nextInt(SEPA_COUNTRIES.length)];
            }
            return IbanRegistry.ALL_COUNTRIES[rnd.nextInt(IbanRegistry.ALL_COUNTRIES.length)];
        }
    }

    // -------------------------------------------------------------------------
    // Internal generation helpers
    // -------------------------------------------------------------------------

    /**
     * Core IBAN generation method. All public entry points (builder and static methods)
     * delegate here.
     * <p>
     * The generation process:
     * <ol>
     *   <li>Constructs the base IBAN string (country code + {@code "00"} placeholder + random BBAN).</li>
     *   <li>Computes and writes the National Check Digit (NCD) if the country defines one
     *       and its {@link CountryValidator} implements {@link NationalCheckDigitCalculator}.</li>
     *   <li>Calculates the correct ISO 7064 Mod 97-10 check digits via
     *       {@link IbanValidator#fixCheckDigits}.</li>
     *   <li>Returns the validated {@link Iban}.</li>
     * </ol>
     *
     * @param countryData the non-{@code null} registry entry for the target country
     * @param random      the non-{@code null} {@link Random} instance to use
     * @return a valid, randomly generated IBAN
     */
    static Iban generate(IbanRegistry countryData, Random random) {

        String countryCode = countryData.isBaseCountry()
            ? countryData.getCountryCode()
            : countryData.getBaseCountry().getCountryCode();

        // 1. CC + "00" ISO check-digit placeholder + random BBAN
        StringBuilder ibanBuilder = new StringBuilder()
            .append(countryCode)
            .append("00")
            .append(generateRandomBban(countryData.getBbanPatternStr(), random));

        // 2. Compute and write the National Check Digit (NCD) where applicable.
        //    This MUST happen before fixCheckDigits(), because the ISO Mod-97 check
        //    covers the complete IBAN including the NCD field.
        if (IbanConfig.isCalculateNcd()) {
            fixNationalCheckDigit(countryData, ibanBuilder);
        }

        // 3. Calculate and apply the correct ISO 7064 Mod 97-10 check digits
        IbanValidator.fixCheckDigits(ibanBuilder);

        // 4. Return the validated result
        return Iban.of(ibanBuilder.toString());
    }

    /**
     * Parses the BBAN pattern string (e.g., {@code "4!n4!n12!c"}) and generates a random BBAN
     * string that matches the required length and character types for the national structure,
     * using the provided {@link Random} instance.
     * <p>
     * The pattern components are:
     * <ul>
     *   <li>{@code n}: digits (numeric characters 0–9)</li>
     *   <li>{@code a}: upper-case letters (alphabetic characters A–Z)</li>
     *   <li>{@code c}: upper-case letters and digits (alphanumeric characters A–Z, 0–9)</li>
     *   <li>{@code !}: indicates fixed length (e.g., {@code 4!n})</li>
     * </ul>
     *
     * @param patternNotation the BBAN structure pattern string from {@link IbanRegistry}
     * @param random          the {@link Random} instance to use for character selection
     * @return a randomly generated string matching the BBAN structure
     */
    static String generateRandomBban(String patternNotation, Random random) {

        List<Segment> segments = IbanPatternConverter.aggregateSegments(
            IbanPatternConverter.parseSegments(patternNotation));

        StringBuilder bbanBuilder = new StringBuilder();
        for (Segment segment : segments) {
            bbanBuilder.append(generateRandomSegment(segment, random));
        }
        return bbanBuilder.toString();
    }

    /**
     * Generates a random string for a single BBAN {@link Segment}.
     * <p>
     * The character set is chosen by the segment's {@code CharType}:
     * <ul>
     *   <li>{@code NUMERIC}      → digits {@code 0–9}</li>
     *   <li>{@code ALPHABETIC}   → upper-case letters {@code A–Z}</li>
     *   <li>{@code ALPHANUMERIC} → digits and upper-case letters {@code 0–9, A–Z}</li>
     * </ul>
     *
     * @param segment the pattern segment specifying the character type and length
     * @param random  the {@link Random} instance to use for character selection
     * @return a randomly generated string of {@link Segment#getLength()} characters
     * @throws IllegalStateException if an unrecognised {@code CharType} is encountered
     */
    static String generateRandomSegment(Segment segment, Random random) {
        requireNonNull(segment, "segment must not be null");
        requireRandom(random);

        String sourceChars;
        if (IbanCharType.NUMERIC == segment.getCharType()) {
            sourceChars = DIGITS;
        } else if (IbanCharType.ALPHABETIC == segment.getCharType()) {
            sourceChars = LETTERS;
        } else if (IbanCharType.ALPHANUMERIC == segment.getCharType()) {
            sourceChars = ALPHANUMERIC;
        } else {
            throw new IllegalStateException("Unrecognised IbanCharType: " + segment.getCharType());
        }

        int segmentLen = segment.getLength();
        StringBuilder sb = new StringBuilder(segmentLen);
        for (int i = 0; i < segmentLen; i++) {
            sb.append(sourceChars.charAt(random.nextInt(sourceChars.length())));
        }
        return sb.toString();
    }

    /**
     * Overwrites the National Check Digit (NCD) field in {@code ibanBuilder} with the value
     * computed by the country's {@link NationalCheckDigitCalculator}, if one is available.
     * <p>
     * This method is a no-op when:
     * <ul>
     *   <li>the country has no NCD field
     *       ({@link IbanRegistry#getNationalCheckDigitIndexRange()} returns {@code null}), or</li>
     *   <li>the country's {@link CountryValidator} does not implement
     *       {@link NationalCheckDigitCalculator}.</li>
     * </ul>
     *
     * @param countryData the registry entry for the country, may not be {@code null}
     * @param ibanBuilder the mutable IBAN string with {@code "00"} ISO check-digit placeholders
     *                    and a randomly generated BBAN; modified in-place; may not be {@code null}
     * @return {@code ibanBuilder}, unmodified if no NCD field is defined for the country
     *         or if the {@link CountryValidator} does not implement
     *         {@link NationalCheckDigitCalculator}; otherwise the NCD field is
     *         overwritten in-place and the same instance is returned
     */
    static StringBuilder fixNationalCheckDigit(IbanRegistry countryData, StringBuilder ibanBuilder) {
        requireNonNull(countryData, "countryData must not be null");
        requireNonNull(ibanBuilder, "ibanBuilder must not be null");

        if (ibanBuilder.length() != countryData.getIbanLength()) {
            throw InvalidIbanException.of(IbanValidationError.INCORRECT_LENGTH_COUNTRY, ibanBuilder);
        }

        // only countries with a registered NCD field are relevant
        IndexRange ncdRange = countryData.getNationalCheckDigitIndexRange();
        if (ncdRange == null) {
            return ibanBuilder;
        }

        // countries with NCD must have a NationalCheckDigitCalculator
        NationalCheckDigitCalculator calc = (NationalCheckDigitCalculator) IbanValidator.getCountryValidator(countryData);
        char[] ncd = calc.calculateNationalCheckDigit(ibanBuilder);

        // write the computed NCD into the StringBuilder
        for (int idxIban = ncdRange.getBegin(), idxNcd = 0; idxIban < ncdRange.getEnd(); idxIban++, idxNcd++) {
            ibanBuilder.setCharAt(idxIban, ncd[idxNcd]);
        }

        return ibanBuilder;
    }

}
