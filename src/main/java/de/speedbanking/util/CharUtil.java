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

import java.util.Objects;

/**
 * Utility class providing common character and character array validation methods.
 * <p>
 * Focuses primarily on basic ASCII checks (digits, uppercase letters) for banking data validation.
 *
 * @since 1.8.0
 */
public final class CharUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CharUtil() {
        throw new UnsupportedOperationException(
            "Utility class " + CharUtil.class.getSimpleName() + " cannot be instantiated");
    }

    /**
     * Checks if a character is a numeric digit ('0'-'9').
     *
     * @param c The character to check.
     * @return {@code true} if the character is a digit, {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public static boolean isDigit(final char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Checks if all characters within the specified range of the array are numeric digits ('0'-'9').
     *
     * @param chars      The character array to check.
     * @param beginIndex The start index (inclusive).
     * @param endIndex   The end index (exclusive).
     * @return {@code true} if all characters are digits, {@code false} otherwise.
     * @throws NullPointerException if {@code chars} is {@code null}.
     * @throws IndexOutOfBoundsException if the range is invalid.
     *
     * @since 1.8.0
     */
    public static boolean isAllDigits(final char[] chars, final int beginIndex, final int endIndex) {
        Objects.requireNonNull(chars, "Character array cannot be null");
        requireValidIndices(beginIndex, endIndex, 0, chars.length);

        for (int i = beginIndex; i < endIndex; i++) {
            if (chars[i] < '0' || chars[i] > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a character is an uppercase ASCII letter ('A'-'Z').
     *
     * @param c The character to check.
     * @return {@code true} if the character is uppercase, {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public static boolean isUpperCase(final char c) {
        return c >= 'A' && c <= 'Z';
    }

    /**
     * Checks if all characters within the specified range of the array are uppercase ASCII letters ('A'-'Z').
     *
     * @param chars      The character array to check.
     * @param beginIndex The start index (inclusive).
     * @param endIndex   The end index (exclusive).
     * @return {@code true} if all characters are uppercase letters, {@code false} otherwise.
     * @throws NullPointerException if {@code chars} is {@code null}.
     * @throws IndexOutOfBoundsException if the range is invalid.
     *
     * @since 1.8.0
     */
    public static boolean isAllUpperCase(final char[] chars, final int beginIndex, final int endIndex) {
        Objects.requireNonNull(chars, "Character array cannot be null");
        requireValidIndices(beginIndex, endIndex, 0, chars.length);

        for (int i = beginIndex; i < endIndex; i++) {
            if (chars[i] < 'A' || chars[i] > 'Z') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a character is either a numeric digit ('0'-'9') or an uppercase ASCII letter ('A'-'Z').
     *
     * @param c The character to check.
     * @return {@code true} if the character is alphanumeric and uppercase, {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public static boolean isDigitOrUpperCase(final char c) {
        return c >= '0' && c <= '9'
            || c >= 'A' && c <= 'Z';
    }

    /**
     * Checks if all characters within the specified range of the array are either numeric digits or uppercase ASCII letters.
     *
     * @param chars      The character array to check.
     * @param beginIndex The start index (inclusive).
     * @param endIndex   The end index (exclusive).
     * @return {@code true} if all characters are digits or uppercase letters, {@code false} otherwise.
     * @throws NullPointerException if {@code chars} is {@code null}.
     * @throws IndexOutOfBoundsException if the range is invalid.
     *
     * @since 1.8.0
     */
    public static boolean isAllDigitOrUpperCase(final char[] chars, final int beginIndex, final int endIndex) {
        Objects.requireNonNull(chars, "Character array cannot be null");
        requireValidIndices(beginIndex, endIndex, 0, chars.length);

        for (int i = beginIndex; i < endIndex; i++) {
            final char c = chars[i];
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'Z')) {
                return false;
            }
        }
        return true;
    }

    private static void requireValidIndices(final int beginIndex, final int endIndex, int minBeginIndex, int maxEndIndex) {
        if (beginIndex < minBeginIndex || endIndex > maxEndIndex || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException(String.format("Invalid range (%d, %d) specified, valid range (%d, %d)",
                beginIndex, endIndex, minBeginIndex, maxEndIndex));
        }
    }

}
