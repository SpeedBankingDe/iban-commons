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
package de.speedbanking.bic;

import de.speedbanking.util.Iso3166Alpha2;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating random Business Identifier Codes (BICs) / SWIFT Codes
 * that are syntactically valid according to ISO 9362.
 * <p>
 * Every generated BIC satisfies all constraints enforced by {@link BicValidator}:
 * <ul>
 *   <li>Length of exactly 8 ({@link Bic#BIC8_LENGTH}) or 11 ({@link Bic#BIC11_LENGTH}) characters</li>
 *   <li>Only uppercase ASCII letters (A–Z) and digits (0–9)</li>
 *   <li>A valid, officially assigned ISO 3166-1 Alpha-2 country code at positions 5–6</li>
 * </ul>
 * <p>
 * <strong>BIC structure (ISO 9362):</strong>
 * <pre>
 *   ┌────────────┬─────────────┬───────────────┬───────────────────────────┐
 *   │ Bank Code  │ Country Code│ Location Code │ Branch Code (BIC-11 only) │
 *   │ 4 letters  │ 2 letters   │ 2 alphanumeric│ 3 alphanumeric            │
 *   └────────────┴─────────────┴───────────────┴───────────────────────────┘
 * </pre>
 * <p>
 * <strong>Reproducible generation:</strong> Every {@code of(...)} and {@code ofBic11(...)} method
 * accepts an optional {@link Random} parameter. Passing a seeded {@code Random} instance produces
 * deterministic output, which is useful for unit tests and snapshot tests. If no {@code Random} is
 * supplied, the thread-local default ({@link ThreadLocalRandom#current()}) is used.
 *
 * <pre>{@code
 * // Reproducible: always produces the same BIC-8 for a given seed and country
 * Bic bic = RandomBic.of("DE", new Random(42L));
 *
 * // Reproducible BIC-11
 * Bic bic11 = RandomBic.ofBic11("DE", new Random(42L));
 *
 * // Non-reproducible: uses ThreadLocalRandom internally
 * Bic bic = RandomBic.of();
 * }</pre>
 *
 * @since 1.8.5
 */
public final class RandomBic {

    /** Source characters for the Bank Code (positions 1–4): uppercase letters only. */
    private static final String          LETTERS       = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Source characters for the Location Code and Branch Code: uppercase letters and digits. */
    private static final String          ALPHANUMERIC  = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Cached array of all assigned ISO 3166-1 Alpha-2 codes to avoid repeated {@code values()} allocations. */
    private static final Iso3166Alpha2[] ALL_COUNTRIES = Iso3166Alpha2.values();

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private RandomBic() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    // -------------------------------------------------------------------------
    // Public API — BIC-8 without explicit Random (uses ThreadLocalRandom)
    // -------------------------------------------------------------------------

    /**
     * Generates a random, valid BIC-8 for any assigned ISO 3166-1 country.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @return a valid, randomly generated {@link Bic} in BIC-8 format
     */
    public static Bic of() {
        return of((Random) null);
    }

    /**
     * Generates a random, valid BIC-8 for the country specified by the given
     * two-letter ISO 3166-1 Alpha-2 country code.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryCode the two-letter ISO 3166-1 Alpha-2 country code (e.g., {@code "DE"})
     * @return a valid, randomly generated {@link Bic} in BIC-8 format
     * @throws NullPointerException if {@code countryCode} is {@code null} or not an officially
     *                              assigned ISO 3166-1 Alpha-2 code
     */
    public static Bic of(String countryCode) {
        return of(countryCode, null);
    }

    // -------------------------------------------------------------------------
    // Public API — BIC-8 with explicit Random (reproducible / seeded)
    // -------------------------------------------------------------------------

    /**
     * Generates a random, valid BIC-8 for any assigned ISO 3166-1 country using the
     * provided {@link Random} instance.
     * <p>
     * Passing a seeded {@code Random} makes both the country selection and the BIC content
     * deterministic.
     *
     * @param random the {@link Random} instance to use; {@code null} falls back to
     *               {@link ThreadLocalRandom#current()}
     * @return a valid, randomly generated {@link Bic} in BIC-8 format
     */
    public static Bic of(Random random) {
        return of((String) null, random);
    }

    /**
     * Generates a random, valid BIC-8 for the country specified by the given two-letter
     * ISO 3166-1 Alpha-2 country code, using the provided {@link Random} instance as the
     * source of randomness.
     * <p>
     * Passing a seeded {@code Random} (e.g. {@code new Random(42L)}) makes generation
     * fully reproducible — the same seed always produces the same BIC for the same country.
     * Parameter {@code random} defaults to {@link ThreadLocalRandom} unless provided.
     *
     * @param countryCode the two-letter ISO 3166-1 Alpha-2 country code (e.g., {@code "DE"}),
     *                    or {@code null} to select a country at random
     * @param random      the {@link Random} instance to use; {@code null} falls back to
     *                    {@link ThreadLocalRandom#current()}
     * @return a valid, randomly generated {@link Bic} in BIC-8 format
     * @throws NullPointerException if {@code countryCode} is not {@code null} but is not an
     *                              officially assigned ISO 3166-1 Alpha-2 code
     */
    public static Bic of(String countryCode, Random random) {
        Random rd = random != null ? random : ThreadLocalRandom.current();
        String cc = resolveCountryCode(countryCode, rd);
        return Bic.of(generateBic8String(cc, rd));
    }

    // -------------------------------------------------------------------------
    // Public API — BIC-11 without explicit Random (uses ThreadLocalRandom)
    // -------------------------------------------------------------------------

    /**
     * Generates a random, valid BIC-11 for any assigned ISO 3166-1 country.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @return a valid, randomly generated {@link Bic} in BIC-11 format
     */
    public static Bic ofBic11() {
        return ofBic11((Random) null);
    }

    /**
     * Generates a random, valid BIC-11 for the country specified by the given
     * two-letter ISO 3166-1 Alpha-2 country code.
     * <p>
     * Uses {@link ThreadLocalRandom} as the source of randomness.
     *
     * @param countryCode the two-letter ISO 3166-1 Alpha-2 country code (e.g., {@code "DE"})
     * @return a valid, randomly generated {@link Bic} in BIC-11 format
     * @throws NullPointerException if {@code countryCode} is {@code null} or not an officially
     *                              assigned ISO 3166-1 Alpha-2 code
     */
    public static Bic ofBic11(String countryCode) {
        return ofBic11(countryCode, null);
    }

    // -------------------------------------------------------------------------
    // Public API — BIC-11 with explicit Random (reproducible / seeded)
    // -------------------------------------------------------------------------

    /**
     * Generates a random, valid BIC-11 for any assigned ISO 3166-1 country using the
     * provided {@link Random} instance.
     * <p>
     * Passing a seeded {@code Random} makes both the country selection and the BIC content
     * deterministic.
     *
     * @param random the {@link Random} instance to use; {@code null} falls back to
     *               {@link ThreadLocalRandom#current()}
     * @return a valid, randomly generated {@link Bic} in BIC-11 format
     */
    public static Bic ofBic11(Random random) {
        return ofBic11((String) null, random);
    }

    /**
     * Generates a random, valid BIC-11 for the country specified by the given two-letter
     * ISO 3166-1 Alpha-2 country code, using the provided {@link Random} instance as the
     * source of randomness.
     * <p>
     * Passing a seeded {@code Random} (e.g. {@code new Random(42L)}) makes generation
     * fully reproducible — the same seed always produces the same BIC for the same country.
     * Parameter {@code random} defaults to {@link ThreadLocalRandom} unless provided.
     *
     * @param countryCode the two-letter ISO 3166-1 Alpha-2 country code (e.g., {@code "DE"}),
     *                    or {@code null} to select a country at random
     * @param random      the {@link Random} instance to use; {@code null} falls back to
     *                    {@link ThreadLocalRandom#current()}
     * @return a valid, randomly generated {@link Bic} in BIC-11 format
     * @throws NullPointerException if {@code countryCode} is not {@code null} but is not an
     *                              officially assigned ISO 3166-1 Alpha-2 code
     */
    public static Bic ofBic11(String countryCode, Random random) {
        Random rd = random != null ? random : ThreadLocalRandom.current();
        String cc = resolveCountryCode(countryCode, rd);
        return Bic.of(generateBic11String(cc, rd));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the effective country code for generation.
     * <p>
     * If {@code countryCode} is {@code null}, a random country is selected from
     * {@link Iso3166Alpha2#values()}. Otherwise the given code is validated and returned
     * as-is (case-sensitive: only uppercase codes are accepted).
     *
     * @param countryCode the caller-supplied country code, or {@code null}
     * @param random      the {@link Random} instance to use for random country selection
     * @return a valid, uppercase, two-letter ISO 3166-1 Alpha-2 code
     * @throws NullPointerException if {@code countryCode} is non-{@code null} but invalid
     */
    private static String resolveCountryCode(String countryCode, Random random) {
        if (countryCode == null) {
            return ALL_COUNTRIES[random.nextInt(ALL_COUNTRIES.length)].name();
        }
        Objects.requireNonNull(Iso3166Alpha2.fromCode(countryCode),
            "Supported ISO 3166-1 Alpha-2 country code required");
        return countryCode;
    }

    /**
     * Assembles a random BIC-8 string from the given country code.
     * <p>
     * The resulting string has the structure:
     * {@code [4 letters][2-letter country code][2 alphanumeric]}
     *
     * @param countryCode a valid, uppercase two-letter ISO 3166-1 Alpha-2 code
     * @param random      the {@link Random} instance to use
     * @return a BIC-8 string (8 characters)
     */
    static String generateBic8String(String countryCode, Random random) {
        return generateBankCode(random)
            + countryCode
            + generateLocationCode(random);
    }

    /**
     * Assembles a random BIC-11 string from the given country code.
     * <p>
     * The resulting string has the structure:
     * {@code [4 letters][2-letter country code][2 alphanumeric][3 alphanumeric]}
     *
     * @param countryCode a valid, uppercase two-letter ISO 3166-1 Alpha-2 code
     * @param random      the {@link Random} instance to use
     * @return a BIC-11 string (11 characters)
     */
    static String generateBic11String(String countryCode, Random random) {
        return generateBic8String(countryCode, random)
            + generateBranchCode(random);
    }

    /**
     * Generates a random Bank Code: 4 uppercase letters (A–Z).
     *
     * @param random the {@link Random} instance to use
     * @return a 4-character string containing only uppercase letters
     */
    static String generateBankCode(Random random) {
        return randomChars(LETTERS, 4, random);
    }

    /**
     * Generates a random Location Code: 2 alphanumeric characters (A–Z, 0–9).
     *
     * @param random the {@link Random} instance to use
     * @return a 2-character alphanumeric string
     */
    static String generateLocationCode(Random random) {
        return randomChars(ALPHANUMERIC, 2, random);
    }

    /**
     * Generates a random Branch Code: 3 alphanumeric characters (A–Z, 0–9).
     *
     * @param random the {@link Random} instance to use
     * @return a 3-character alphanumeric string
     */
    static String generateBranchCode(Random random) {
        return random.nextBoolean()
            ? Bic.HEAD_OFFICE_SUFFIX
            : randomChars(ALPHANUMERIC, 3, random);
    }

    /**
     * Builds a random string of the requested {@code length} by sampling characters
     * uniformly at random from {@code sourceChars}.
     *
     * @param sourceChars the character pool to draw from
     * @param length      the number of characters to generate
     * @param random      the {@link Random} instance to use
     * @return a randomly assembled string
     */
    private static String randomChars(String sourceChars, int length, Random random) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(sourceChars.charAt(random.nextInt(sourceChars.length())));
        }
        return sb.toString();
    }

}
