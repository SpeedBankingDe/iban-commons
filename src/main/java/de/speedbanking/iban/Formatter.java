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
package de.speedbanking.iban;

/**
 * Utility class for formatting a normalized IBAN sequence (no spaces)
 * into the standard display format (groups of characters separated by spaces).
 *
 * @since 1.8.0
 */
public final class Formatter {

    /** Standard group size for display formatting (as recommended by ISO) */
    static final int DEFAULT_GROUP_SIZE = 4;

    private Formatter() {
        throw new UnsupportedOperationException(
            "Utility class " + Formatter.class.getSimpleName() + " cannot be instantiated");
    }

    /**
     * Formats a raw, normalized IBAN character sequence into the standard, spaced display format,
     * using the default group size of 4.
     * <p>
     * Example: "DE91100000000123456789" {@code ->} "DE91 1000 0000 0123 4567 89"
     *
     * @param normalizedIban The raw, unformatted IBAN character sequence.
     * @return The formatted IBAN string with spaces.
     *
     * @since 1.8.0
     */
    public static String format(final CharSequence normalizedIban) {
        return format(normalizedIban, DEFAULT_GROUP_SIZE);
    }

    /**
     * Formats a raw, normalized IBAN character sequence into the standard, spaced display format,
     * using a custom group size.
     *
     * @param inputIban The raw, unformatted IBAN character sequence.
     * @param groupSize The number of characters per group (must be greater than zero).
     * @return The formatted IBAN string with spaces.
     *
     * @since 1.8.0
     */
    public static String format(final CharSequence inputIban, final int groupSize) {
        if (groupSize <= 0) {
            throw new IllegalArgumentException("Group size must be a positive integer");
        }

        if (inputIban == null) {
            return null;
        }

        int ibanLen = inputIban.length();

        if (ibanLen == 0) {
            return null;
        }

        // pre-allocate StringBuilder with sufficient capacity to avoid resizing
        final StringBuilder formatted = new StringBuilder(2 * ibanLen);

        // iterate through the CharSequence, remove spaces (if any) and insert a space
        // after every configured groupSize characters but at the end
        for (int i = 0, outCount = 0; i < ibanLen; i++) {
            char c = inputIban.charAt(i);
            if (c != ' ') {
                formatted.append(c);
                if (i + 1 < ibanLen && ++outCount % groupSize == 0) {
                    formatted.append(' ');
                }
            }
        }

        return formatted.length() == 0 ? null : formatted.toString();
    }

}
