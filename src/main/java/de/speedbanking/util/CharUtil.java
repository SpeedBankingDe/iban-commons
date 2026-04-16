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
package de.speedbanking.util;

import java.util.Objects;

/**
 * Utility class providing common character and character array validation methods.
 * <p>
 * Focuses primarily on basic ASCII checks (digits, uppercase letters) for banking data validation.
 * All methods are optimized for performance by avoiding heavy regex or locale-dependent checks.
 *
 * @since 1.8.0
 */
public final class CharUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private CharUtil() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Checks if a character is a numeric digit ('0'-'9').
     *
     * @param c the character to check
     * @return {@code true} if the character is a digit, {@code false} otherwise
     *
     * @since 1.8.0
     */
    public static boolean isDigit(final char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Checks if a character is NOT a numeric digit ('0'-'9').
     *
     * @param c the character to check
     * @return {@code true} if the character is not a digit, {@code false} otherwise
     *
     * @since 1.8.2
     */
    public static boolean isNotDigit(final char c) {
        return c < '0' || c > '9';
    }

    /**
     * Checks if all characters within the specified range of the array are numeric digits ('0'-'9').
     *
     * @param chars      the character array to check
     * @param beginIndex the start index (inclusive)
     * @param endIndex   the end index (exclusive)
     * @return {@code true} if all characters in the range are digits, {@code false} otherwise
     * @throws NullPointerException if {@code chars} is {@code null}
     * @throws IndexOutOfBoundsException if the range is invalid for the given array
     *
     * @since 1.8.0
     */
    public static boolean isAllDigits(final char[] chars, final int beginIndex, final int endIndex) {
        requireInput(chars);
        requireValidIndices(beginIndex, endIndex, 0, chars.length);

        for (int i = beginIndex; i < endIndex; i++) {
            if (chars[i] < '0' || chars[i] > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all characters within the specified range of the sequence are numeric digits ('0'-'9').
     *
     * @param chars      the character sequence to check
     * @param beginIndex the start index (inclusive)
     * @param endIndex   the end index (exclusive)
     * @return {@code true} if all characters in the range are digits, {@code false} otherwise
     * @throws NullPointerException if {@code chars} is {@code null}
     * @throws IndexOutOfBoundsException if the range is invalid
     *
     * @since 1.8.5
     */
    public static boolean isAllDigits(final CharSequence chars, final int beginIndex, final int endIndex) {
        requireInput(chars);
        requireValidIndices(beginIndex, endIndex, 0, chars.length());

        for (int i = beginIndex; i < endIndex; i++) {
            char c = chars.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllDigits(final CharSequence chars) {
        requireInput(chars);
        return isAllDigits(chars, 0, chars.length());
    }

    /**
     * Checks if a character is a lowercase ASCII letter ('a'-'z').
     *
     * @param c the character to check
     * @return {@code true} if the character is a lowercase letter, {@code false} otherwise
     *
     * @since 1.8.5
     */
    public static boolean isLowerCase(final char c) {
        return c >= 'a' && c <= 'z';
    }

    /**
     * Checks if a character is NOT a lowercase ASCII letter ('a'-'z').
     *
     * @param c the character to check
     * @return {@code true} if the character is not a lowercase letter, {@code false} otherwise
     *
     * @since 1.8.5
     */
    public static boolean isNotLowerCase(final char c) {
        return c < 'a' || c > 'z';
    }

    /**
     * Checks if a character is an uppercase ASCII letter ('A'-'Z').
     *
     * @param c the character to check
     * @return {@code true} if the character is an uppercase letter, {@code false} otherwise
     *
     * @since 1.8.0
     */
    public static boolean isUpperCase(final char c) {
        return c >= 'A' && c <= 'Z';
    }

    /**
     * Checks if a character is NOT an uppercase ASCII letter ('A'-'Z').
     *
     * @param c the character to check
     * @return {@code true} if the character is not an uppercase letter, {@code false} otherwise
     *
     * @since 1.8.2
     */
    public static boolean isNotUpperCase(final char c) {
        return c < 'A' || c > 'Z';
    }

    /**
     * Checks if all characters within the specified range of the array are uppercase ASCII letters ('A'-'Z').
     *
     * @param chars      the character array to check
     * @param beginIndex the start index (inclusive)
     * @param endIndex   the end index (exclusive)
     * @return {@code true} if all characters in the range are uppercase letters, {@code false} otherwise
     * @throws NullPointerException if {@code chars} is {@code null}
     * @throws IndexOutOfBoundsException if the range is invalid for the given array
     *
     * @since 1.8.0
     */
    public static boolean isAllUpperCase(final char[] chars, final int beginIndex, final int endIndex) {
        requireInput(chars);
        requireValidIndices(beginIndex, endIndex, 0, chars.length);

        for (int i = beginIndex; i < endIndex; i++) {
            if (chars[i] < 'A' || chars[i] > 'Z') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all characters within the specified range of the sequence are uppercase ASCII letters ('A'-'Z').
     *
     * @param chars      the character sequence to check
     * @param beginIndex the start index (inclusive)
     * @param endIndex   the end index (exclusive)
     * @return {@code true} if all characters in the range are uppercase letters, {@code false} otherwise
     * @throws NullPointerException if {@code chars} is {@code null}
     * @throws IndexOutOfBoundsException if the range is invalid
     *
     * @since 1.8.5
     */
    public static boolean isAllUpperCase(final CharSequence chars, final int beginIndex, final int endIndex) {
        requireInput(chars);
        requireValidIndices(beginIndex, endIndex, 0, chars.length());

        for (int i = beginIndex; i < endIndex; i++) {
            char c = chars.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllUpperCase(final CharSequence chars) {
        requireInput(chars);
        return isAllUpperCase(chars, 0, chars.length());
    }

    /**
     * Checks if a character is either a numeric digit ('0'-'9') or an uppercase ASCII letter ('A'-'Z').
     *
     * @param c the character to check
     * @return {@code true} if the character is an alphanumeric uppercase character, {@code false} otherwise
     *
     * @since 1.8.0
     */
    public static boolean isDigitOrUpperCase(final char c) {
        return (c >= '0' && c <= '9')
            || (c >= 'A' && c <= 'Z');
    }

    /**
     * Checks if all characters within the specified range of the array are alphanumeric uppercase characters.
     *
     * @param chars      the character array to check
     * @param beginIndex the start index (inclusive)
     * @param endIndex   the end index (exclusive)
     * @return {@code true} if all characters in the range are digits or uppercase letters, {@code false} otherwise
     * @throws NullPointerException if {@code chars} is {@code null}
     * @throws IndexOutOfBoundsException if the range is invalid
     *
     * @since 1.8.0
     */
    public static boolean isAllDigitOrUpperCase(final char[] chars, final int beginIndex, final int endIndex) {
        requireInput(chars);
        requireValidIndices(beginIndex, endIndex, 0, chars.length);

        for (int i = beginIndex; i < endIndex; i++) {
            char c = chars[i];
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all characters within the specified range of the sequence are alphanumeric uppercase characters.
     *
     * @param chars      the character sequence to check
     * @param beginIndex the start index (inclusive)
     * @param endIndex   the end index (exclusive)
     * @return {@code true} if all characters in the range are digits or uppercase letters, {@code false} otherwise
     * @throws NullPointerException if {@code chars} is {@code null}
     * @throws IndexOutOfBoundsException if the range is invalid
     *
     * @since 1.8.5
     */
    public static boolean isAllDigitOrUpperCase(final CharSequence chars, final int beginIndex, final int endIndex) {
        requireInput(chars);
        requireValidIndices(beginIndex, endIndex, 0, chars.length());

        for (int i = beginIndex; i < endIndex; i++) {
            if (!isDigitOrUpperCase(chars.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all characters of the sequence are alphanumeric uppercase characters.
     *
     * @param chars the character sequence to check
     * @return {@code true} if all characters are digits or uppercase letters, {@code false} otherwise
     * @throws NullPointerException if {@code chars} is {@code null}
     *
     * @since 1.8.5
     */
    public static boolean isAllDigitOrUpperCase(final CharSequence chars) {
        requireInput(chars);
        return isAllDigitOrUpperCase(chars, 0, chars.length());
    }

    /**
     * Converts a {@link CharSequence} to a primitive character array.
     * <p>
     * This method is a performance-optimized alternative to {@code toString().toCharArray()},
     * as it avoids the creation of an intermediate {@code String} object and its
     * internal array copy.
     * <p>
     * If the specified length is negative, the full length of the sequence is used.
     *
     * @param chars the character sequence to convert, must not be null
     * @param charsLen the number of characters to copy, or -1 to copy the entire sequence
     * @return a new character array containing the characters
     * @throws IllegalArgumentException if {@code chars} is null
     */
    public static char[] toCharArray(final CharSequence chars, final int charsLen) {
        requireInput(chars);
        final int len = charsLen < 0 ? chars.length() : charsLen;
        final char[] arr = new char[len];
        for (int i = 0; i < len; i++) {
            arr[i] = chars.charAt(i);
        }
        return arr;
    }

    /**
     * Converts the entire {@link CharSequence} to a primitive character array.
     * <p>
     * Effectively a shorthand for {@code toCharArray(chars, -1)}.
     *
     * @param chars the character sequence to convert, must not be null
     * @return a new character array containing all characters
     * @throws IllegalArgumentException if {@code chars} is null
     */
    public static char[] toCharArray(final CharSequence chars) {
        return toCharArray(chars, -1);
    }

    /**
     * Checks that the specified input is not {@code null} and
     * throws a customized {@link NullPointerException} if it is.
     *
     * @param input   the input to check for nullity
     * @param <T> the type of the reference
     * @return {@code input} if not {@code null}
     * @throws NullPointerException if {@code input} is {@code null}
     */
    private static <T> T requireInput(final T input) {
        return Objects.requireNonNull(input, "Input cannot be null");
    }

    /**
     * Validates that the specified indices define a valid range within given bounds.
     *
     * @param beginIndex    the start index to check
     * @param endIndex      the end index to check
     * @param minBeginIndex the minimum allowed start index
     * @param maxEndIndex   the maximum allowed end index
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    private static void requireValidIndices(final int beginIndex, final int endIndex, final int minBeginIndex, final int maxEndIndex) {
        if (beginIndex < minBeginIndex || endIndex > maxEndIndex || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException(String.format("Invalid range (%d, %d) specified, valid range (%d, %d)",
                beginIndex, endIndex, minBeginIndex, maxEndIndex));
        }
    }

}

