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

import de.speedbanking.iban.util.IbanCharType;
import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.util.IndexRange;

import java.util.List;
import java.util.Objects;
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
 */
public final class RandomIban {

    private static final String         DIGITS        = "0123456789";
    private static final String         LETTERS       = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String         ALPHANUMERIC  = DIGITS + LETTERS;

    /**
     * Cached array of all registry entries.
     */
    private static final IbanRegistry[] ALL_COUNTRIES = IbanRegistry.values();

    /**
     * Cached array of SEPA-country registry entries.
     * <p>
     * {@link IbanRegistry#getSepaCountries()} builds a new list on every call; caching the
     * result as an array avoids repeated allocation in {@link Builder#resolveCountry(Random)}
     * and {@link #ofSepa()}.
     */
    private static final IbanRegistry[] SEPA_COUNTRIES =
        IbanRegistry.getSepaCountries().toArray(new IbanRegistry[0]);

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private RandomIban() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

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
            this.countryData = Objects.requireNonNull(registry, "registry must not be null");
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
            this.random = Objects.requireNonNull(random, "random must not be null");
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
            return ALL_COUNTRIES[rnd.nextInt(ALL_COUNTRIES.length)];
        }
    }

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
        Objects.requireNonNull(segment, "segment must not be null");
        Objects.requireNonNull(random, "random must not be null");

        String sourceChars = null;
        if (IbanCharType.NUMERIC == segment.getCharType()) {
            sourceChars = DIGITS;
        } else if (IbanCharType.ALPHABETIC == segment.getCharType()) {
            sourceChars = LETTERS;
        } else if (IbanCharType.ALPHANUMERIC == segment.getCharType()) {
            sourceChars = ALPHANUMERIC;
        } else {
            return null;
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
        Objects.requireNonNull(countryData, "countryData must not be null");
        Objects.requireNonNull(ibanBuilder, "ibanBuilder must not be null");

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
