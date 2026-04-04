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
 * <strong>Reproducible generation:</strong> Every {@code of(...)} method accepts an optional
 * {@link Random} parameter. Passing a seeded {@code Random} instance produces deterministic
 * output, which is useful for unit tests and snapshot tests. If no {@code Random} is supplied,
 * the thread-local default ({@link ThreadLocalRandom#current()}) is used.
 *
 * <pre>{@code
 * // Reproducible: always produces the same IBAN for a given seed
 * Iban iban = RandomIban.of("DE", new Random(42L));
 *
 * // Non-reproducible: uses ThreadLocalRandom internally
 * Iban iban = RandomIban.of("DE");
 * }</pre>
 */
public final class RandomIban {

    private static final String         DIGITS        = "0123456789";
    private static final String         LETTERS       = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String         ALPHANUMERIC  = DIGITS + LETTERS;

    /** Cached array of all registry entries to avoid repeated .values() allocations. */
    private static final IbanRegistry[] ALL_COUNTRIES = IbanRegistry.values();

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private RandomIban() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Generates a random, valid IBAN for any supported country.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     */
    public static Iban of() {
        return of((Random) null);
    }

    /**
     * Generates a random, valid IBAN for any supported SEPA country.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @return a valid, randomly generated IBAN from a SEPA country
     */
    public static Iban ofSepa() {
        return ofSepa(null);
    }

    /**
     * Generates a random, valid IBAN for the country specified by the given two-letter country code.
     *
     * @param countryCode the two-letter country code (e.g., "DE")
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     * @throws NullPointerException if the {@code countryCode} is {@code null} or not supported
     */
    public static Iban of(String countryCode) {
        return of(countryCode, null);
    }

    /**
     * Generates a random, valid IBAN based on the specifications of the provided {@code IbanRegistry}.
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryData the registry entry defining the country's IBAN structure
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     */
    public static Iban of(IbanRegistry countryData) {
        return of(countryData, null);
    }

    // -------------------------------------------------------------------------
    // Public API — with explicit Random (reproducible / seeded)
    // -------------------------------------------------------------------------

    /**
     * Generates a random, valid IBAN for any supported country using the provided {@link Random} instance.
     * <p>
     * Passing a seeded {@code Random} makes the selection of the country and the BBAN deterministic.
     *
     * @param random the {@link Random} instance to use
     * @return a valid, randomly generated IBAN
     */
    public static Iban of(Random random) {
        return of((IbanRegistry) null, random);
    }

    /**
     * Generates a random, valid IBAN for any supported SEPA country using the provided {@link Random} instance.
     * <p>
     * Passing a seeded {@code Random} makes the selection of the SEPA country and the BBAN deterministic.
     *
     * @param random the {@link Random} instance to use
     * @return a valid, randomly generated SEPA IBAN
     */
    public static Iban ofSepa(Random random) {
        Random rd = random != null ? random : ThreadLocalRandom.current();
        List<IbanRegistry> sepaCountries = IbanRegistry.getSepaCountries();
        return of(sepaCountries.get(rd.nextInt(sepaCountries.size())), rd);
    }

    /**
     * Generates a random, valid IBAN for the country specified by the given two-letter country code,
     * using the provided {@link Random} instance as the source of randomness.
     * <p>
     * Passing a seeded {@code Random} (e.g. {@code new Random(42L)}) makes generation
     * fully reproducible — the same seed always produces the same IBAN for the same country.<br>
     * Parameter {@code random} defaults to {@link ThreadLocalRandom} unless provided.
     *
     * @param countryCode the two-letter country code (e.g., "DE")
     * @param random      the {@link Random} instance to use
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     * @throws NullPointerException if {@code countryCode} is not supported
     */
    public static Iban of(String countryCode, Random random) {
        IbanRegistry countryData = Objects.requireNonNull(IbanRegistry.getByCode(countryCode),
                "Supported iban country code required");
        return of(countryData, random);
    }

    /**
     * Generates a random, valid IBAN based on the specifications of the provided {@code IbanRegistry},
     * using the provided {@link Random} instance as the source of randomness.
     * <p>
     * The generation process involves:
     * <ol>
     *   <li>Constructing the base IBAN string (Country Code + {@code "00"} placeholder + random BBAN).</li>
     *   <li>Computing and writing the National Check Digit (NCD) if the country defines one
     *       and its {@link CountryValidator} implements {@link NationalCheckDigitCalculator}.</li>
     *   <li>Calculating the correct ISO 7064 Mod 97-10 check digits using {@link IbanValidator#fixCheckDigits}.</li>
     *   <li>Returning the validated {@link Iban}.</li>
     * </ol>
     * <p>
     * Passing a seeded {@code Random} (e.g. {@code new Random(42L)}) makes generation
     * fully reproducible — the same seed always produces the same IBAN for the same country.
     *
     * @param countryData the registry entry defining the country's IBAN structure
     * @param random      the {@link Random} instance to use
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     */
    public static Iban of(IbanRegistry countryData, Random random) {
        Random rd = random != null ? random : ThreadLocalRandom.current();
        final IbanRegistry cd = countryData != null ? countryData : ALL_COUNTRIES[rd.nextInt(ALL_COUNTRIES.length)];

        String countryCode = cd.isBaseCountry()
            ? cd.getCountryCode()
            : cd.getBaseCountry().getCountryCode();

        // 1. Start with CC + Check Digits Placeholders ("00") + random BBAN
        StringBuilder ibanBuilder = new StringBuilder()
            .append(countryCode)
            .append("00") // placeholder for ISO check digits
            .append(generateRandomBban(cd.getBbanPatternStr(), rd));

        // 2. Compute and write the National Check Digit (NCD) where applicable.
        //    This MUST happen before fixCheckDigits(), because the ISO Mod-97 check
        //    covers the complete IBAN including the NCD field.
        if (IbanConfig.NCD_CALCULATE.isEnabled()) {
            fixNationalCheckDigit(cd, ibanBuilder);
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
     *   <li>{@code n}: digits (numeric characters 0-9)</li>
     *   <li>{@code a}: upper-case letters (alphabetic characters A-Z)</li>
     *   <li>{@code c}: upper-case letters and digits (alphanumeric characters A-Z, 0-9)</li>
     *   <li>{@code !}: indicates fixed length (e.g., {@code 4!n})</li>
     * </ul>
     *
     * @param patternNotation the BBAN structure pattern string provided by {@link IbanRegistry}
     * @param random          the {@link Random} instance to use for character selection
     * @return a randomly generated string matching the BBAN structure
     */
    static String generateRandomBban(String patternNotation, Random random) {

        List<Segment> segments = IbanPatternConverter.aggregateSegments(
            IbanPatternConverter.parseSegments(patternNotation));

        String sourceChars;
        StringBuilder bbanBuilder = new StringBuilder();

        for (Segment segment : segments) {
            switch (segment.getCharType()) {
                case NUMERIC:
                    sourceChars = DIGITS;
                    break;
                case ALPHABETIC:
                    sourceChars = LETTERS;
                    break;
                default:
                    sourceChars = ALPHANUMERIC;
                    break;
            }

            // append random characters of the correct type
            for (int i = 0; i < segment.getLength(); i++) {
                int randomIdx = random.nextInt(sourceChars.length());
                bbanBuilder.append(sourceChars.charAt(randomIdx));
            }
        }

        return bbanBuilder.toString();
    }

    /**
     * Overwrites the National Check Digit field in {@code ibanBuilder} with the value
     * computed by the country's {@link NationalCheckDigitCalculator}, if one is available.
     *
     * <p>The method is a no-op for countries whose {@link CountryValidator} does not also
     * implement {@link NationalCheckDigitCalculator}, or for countries that have no NCD
     * field ({@link IbanRegistry#getNationalCheckDigitIndexRange()} returns {@code null}).
     *
     * @param countryData the registry entry for the country
     * @param ibanBuilder the mutable IBAN string with {@code "00"} ISO check-digit placeholders
     *                    and a randomly generated BBAN; modified in-place
     */
    static StringBuilder fixNationalCheckDigit(IbanRegistry countryData, StringBuilder ibanBuilder) {

        // only countries with a registered NCD field are relevant
        IndexRange ncdRange = countryData.getNationalCheckDigitIndexRange();
        if (ncdRange == null) {
            return ibanBuilder;
        }

        // the CountryValidator must also implement NationalCheckDigitCalculator
        CountryValidator cv = IbanValidator.getCountryValidator(countryData);

        NationalCheckDigitCalculator calc = NationalCheckDigitCalculator.class.cast(cv);

        CharSequence ncd = calc.calculateNationalCheckDigit(ibanBuilder);

        // write the computed NCD into the StringBuilder
        for (int idxIban = ncdRange.getBegin(), idxNcd = 0; idxIban < ncdRange.getEnd(); idxIban++, idxNcd++) {
            ibanBuilder.setCharAt(idxIban, ncd.charAt(idxNcd));
        }

        return ibanBuilder;
    }

}
