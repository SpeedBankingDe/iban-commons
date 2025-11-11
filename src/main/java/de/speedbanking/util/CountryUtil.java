/*
 * Copyright © 2025 Markus Spann, SpeedBankingDe
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
package de.speedbanking.util;

/**
 * Utility class providing functionality regarding country codes and countries.
 *
 * @since 1.8.0
 */
public final class CountryUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CountryUtil() {
        throw new UnsupportedOperationException(
            "Utility class " + CountryUtil.class.getSimpleName() + " cannot be instantiated");
    }

    /**
     * Checks if the given string is a valid two-letter country code according to
     * the basic format requirements of ISO 3166-1 Alpha-2.
     * <p>
     * A country code is considered valid if it is not null, has a length of exactly two,
     * and both characters are uppercase ASCII letters ('A' through 'Z').
     * <p>
     * This method only checks the format and does not validate if the code
     * actually represents a currently assigned ISO country.
     *
     * @param countryCode The string to validate (e.g., "US", "CH").
     * @return {@code true} if the string is a valid two-letter uppercase code; {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public static boolean isValidCountryCode(final String countryCode) {
        return countryCode != null && countryCode.length() == 2
            && countryCode.charAt(0) >= 'A' && countryCode.charAt(0) <= 'Z'
            && countryCode.charAt(1) >= 'A' && countryCode.charAt(1) <= 'Z';
    }

    /**
     * Converts a two-letter ISO 3166-1 Alpha-2 country code (e.g., "DE", "PS")
     * into its corresponding flag emoji representation (e.g., 🇩🇪, 🇵🇸).
     * <p>
     * This conversion relies on the Unicode Regional Indicator Symbol Letters (RIS)
     * which are calculated by adding an offset to the uppercase ASCII value of the letters.
     *
     * @param countryCode The two-letter Alpha-2 country code (case-insensitive).
     * @return The flag emoji string, or an empty string if the input code is invalid or null.
     * @throws IllegalArgumentException if the provided {@code countryCode} is not a valid two-letter uppercase code (as checked by {@link #isValidCountryCode(String)}).
     *
     * @since 1.8.0
     */
    public static String createFlagEmoji(final String countryCode) {
        if (!isValidCountryCode(countryCode)) {
            throw new IllegalArgumentException("Valid country code required");
        }

        // the base Unicode value for the Regional Indicator Symbol Letter 'A'
        final int regionalIndicatorOffset = 0x1F1E6;

        return new StringBuilder()
            // convert the first letter: ('D' - 'A' + Offset) -> RIS 'D'
            .appendCodePoint(countryCode.charAt(0) - 'A' + regionalIndicatorOffset)
            // convert the second letter: ('E' - 'A' + Offset) -> RIS 'E'
            .appendCodePoint(countryCode.charAt(1) - 'A' + regionalIndicatorOffset)
            .toString();
    }

}
