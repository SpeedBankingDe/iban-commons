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

import static de.speedbanking.iban.IbanRegistry.INDEX_BBAN;
import static de.speedbanking.iban.IbanRegistry.INDEX_CHECK_DIGITS;

import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;

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

    private static final String DIGITS       = "0123456789";
    private static final String LETTERS      = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = DIGITS + LETTERS;

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private RandomIban() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Generates a random, valid IBAN for the country specified by the given two-letter country code.
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryCode the two-letter country code (e.g., "DE")
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     * @throws NullPointerException if the {@code countryCode} is {@code null} or not supported
     */
    public static Iban of(String countryCode) {
        return of(countryCode, ThreadLocalRandom.current());
    }

    /**
     * Generates a random, valid IBAN based on the specifications of the provided {@code IbanRegistry}.
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param ibanRegistry the registry entry defining the country's IBAN structure
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     * @throws NullPointerException if {@code ibanRegistry} is {@code null}
     */
    public static Iban of(IbanRegistry ibanRegistry) {
        return of(ibanRegistry, ThreadLocalRandom.current());
    }

    // -------------------------------------------------------------------------
    // Public API — with explicit Random (reproducible / seeded)
    // -------------------------------------------------------------------------

    /**
     * Generates a random, valid IBAN for the country specified by the given two-letter country code,
     * using the provided {@link Random} instance as the source of randomness.
     * <p>
     * Passing a seeded {@code Random} (e.g. {@code new Random(42L)}) makes generation
     * fully reproducible — the same seed always produces the same IBAN for the same country.
     *
     * @param countryCode the two-letter country code (e.g., "DE")
     * @param random      the {@link Random} instance to use; must not be {@code null}
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     * @throws NullPointerException if {@code countryCode} is not supported or {@code random} is {@code null}
     */
    public static Iban of(String countryCode, Random random) {
        Objects.requireNonNull(random, "Random must not be null");
        IbanRegistry ibanRegistry = Objects.requireNonNull(IbanRegistry.getByCode(countryCode),
                "Supported country code required");
        return of(ibanRegistry, random);
    }

    /**
     * Generates a random, valid IBAN based on the specifications of the provided {@code IbanRegistry},
     * using the provided {@link Random} instance as the source of randomness.
     * <p>
     * The generation process involves:
     * <ul>
     *   <li>Constructing the base IBAN string (Country Code + {@code "00"} placeholder + random BBAN).</li>
     *   <li>Calculating the correct ISO 7064 Mod 97-10 check digits using {@link #fixCheckDigits}.</li>
     *   <li>Applying the calculated check digits to the IBAN string.</li>
     * </ul>
     * <p>
     * Passing a seeded {@code Random} (e.g. {@code new Random(42L)}) makes generation
     * fully reproducible — the same seed always produces the same IBAN for the same country.
     *
     * @param ibanRegistry the registry entry defining the country's IBAN structure
     * @param random       the {@link Random} instance to use; must not be {@code null}
     * @return a valid, randomly generated IBAN as an {@code Iban} object
     * @throws NullPointerException if {@code ibanRegistry} or {@code random} is {@code null}
     */
    public static Iban of(IbanRegistry ibanRegistry, Random random) {
        Objects.requireNonNull(ibanRegistry, "IbanRegistry must not be null");
        Objects.requireNonNull(random, "Random must not be null");

        // 1. Start with CC + Check Digits Placeholders ("00")
        StringBuilder ibanBuilder = new StringBuilder()
            .append(ibanRegistry.getCountryCode())
            .append("00") // placeholder for check digits
            .append(generateRandomBban(ibanRegistry.getBbanPatternStr(), random));

        // 2. Calculate and apply the correct check digits
        fixCheckDigits(ibanBuilder);

        // 3. Return the validated result
        return Iban.of(ibanBuilder.toString());
    }

    /**
     * Calculates the correct ISO 7064 Mod 97-10 check digits for the given IBAN string
     * and overwrites the placeholders (usually "00") at the check digit positions (index 2 and 3).
     * <p>
     * This method temporarily sets the check digits to "00" to calculate the required remainder $R$,
     * then determines the final check digits $CD = 98 - R$.
     *
     * @param ibanBuilder the IBAN string builder (must already be of the full IBAN length,
     * with placeholders at the check digit position).
     * @return the same {@code StringBuilder} instance with the correct check digits applied
     */
    static StringBuilder fixCheckDigits(StringBuilder ibanBuilder) {
        // 1. Set placeholders to "00" (crucial for correct calculation context)
        ibanBuilder.setCharAt(INDEX_CHECK_DIGITS, '0');
        ibanBuilder.setCharAt(INDEX_CHECK_DIGITS + 1, '0');

        // 2. Calculate the required check digits value (98 - Modulo result)
        int checkDigitsValue = 98 - calculateMod97(ibanBuilder);

        // 3. Format the result to a zero-padded 2-digit String, e.g. 5 -> "05", 91 -> "91"
        String checkDigitsStr = String.format("%02d", checkDigitsValue);

        // 4. Overwrite the placeholders with the calculated digits
        ibanBuilder.setCharAt(INDEX_CHECK_DIGITS, checkDigitsStr.charAt(0));
        ibanBuilder.setCharAt(INDEX_CHECK_DIGITS + 1, checkDigitsStr.charAt(1));

        return ibanBuilder;
    }

    /**
     * Performs the core ISO 7064 Mod 97-10 calculation on the restructured IBAN string.
     * <p>
     * The input {@code CharSequence} is rotated by moving the first 4 characters (CC + CD) to the end.
     * Characters are converted to numerical values (A=10, B=11, ..., Z=35).
     * The result is the remainder of the overall number when divided by 97.
     * Intermediate modulo operations are performed to prevent {@code long} overflow.
     *
     * @param cs the full IBAN string (normalized, with placeholder check digits)
     * @return the Mod 97 remainder (R) of the restructured IBAN value
     * @throws InvalidIbanException if the input contains non-alphanumeric characters (outside A-Z, 0-9)
     */
    static int calculateMod97(final CharSequence cs) {
        StringBuilder ibanBuilder = new StringBuilder()
            .append(cs.subSequence(INDEX_BBAN, cs.length()))
            .append(cs.subSequence(0, INDEX_BBAN));

        long total = 0;
        for (int i = 0; i < ibanBuilder.length(); i++) {
            final int numericValue = Character.getNumericValue(ibanBuilder.charAt(i));
            if (numericValue < 0 || numericValue > 35) {
                throw new InvalidIbanException(IbanValidationError.ILLEGAL_CHARACTERS);
            }
            total = (numericValue > 9 ? total * 100 : total * 10) + numericValue;

            if (total > IbanValidator.MAX) {
                total = (total % IbanValidator.MOD);
            }
        }
        return (int) (total % IbanValidator.MOD);
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

}
