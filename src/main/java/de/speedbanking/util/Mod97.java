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
 * Utility class implementing the <strong>ISO 7064 Mod 97-10</strong> checksum algorithm.
 * <p>
 * This algorithm is used by the <strong>ISO 13616 IBAN standard</strong> and by
 * other financial identifiers such as the SEPA Creditor Identifier (CI).
 * The core computation replaces each uppercase letter {@code A–Z} with its two-digit
 * numeric equivalent ({@code A=10, B=11, …, Z=35}) and then computes the remainder
 * of the resulting integer modulo {@value #MODULUS}.
 *
 * <h2>IBAN-style calculation (with rearrangement)</h2>
 * {@link #calculate(CharSequence)} and {@link #calculate(char[])} implement
 * the <em>rearrangement step</em> mandated by ISO 13616: the four-character
 * header (country code + check digits at positions 0–3) is processed <em>last</em>,
 * after the BBAN (positions 4 onwards).  A valid IBAN yields a remainder of exactly
 * {@link #VALID_REMAINDER} ({@value #VALID_REMAINDER}).
 *
 * <h2>General-purpose calculation (no rearrangement)</h2>
 * {@link #calculateRange(char[], int, int)} processes a contiguous sub-range of a
 * character array without any rearrangement.  This variant is used internally for
 * National Check Digit (NCD) calculations that operate on BBAN sub-sequences.
 *
 * <h2>Return value on invalid input</h2>
 * Both {@code calculate} variants return {@link #INVALID_REMAINDER}
 * ({@value #INVALID_REMAINDER}) when the input is {@code null} or contains a
 * character that is neither a digit {@code '0'–'9'} nor an uppercase letter
 * {@code 'A'–'Z'}.  Callers must treat this sentinel as a validation failure.
 *
 * <h2>Usage examples</h2>
 * <pre>{@code
 * // Validate an IBAN checksum
 * boolean valid = Mod97.isValid("DE91100000000123456789");
 *
 * // Compute the raw remainder for diagnostics
 * int remainder = Mod97.calculate("DE91100000000123456789"); // → 1
 *
 * // Calculate the remainder of a BBAN sub-sequence (e.g. for NCD computation)
 * char[] iban = "DE91100000000123456789".toCharArray();
 * int r = Mod97.calculateRange(iban, 4, iban.length - 4);
 * }</pre>
 *
 * @since 1.8.5
 *
 * @see <a href="https://www.iso.org/standard/31531.html">ISO 7064:2003</a>
 * @see <a href="https://www.swift.com/standards/data-standards/iban-international-bank-account-number">SWIFT IBAN</a>
 */
public final class Mod97 {

    /**
     * The modulus used in the ISO 7064 Mod 97-10 algorithm ({@value}).
     */
    public static final int  MODULUS           = 97;

    /**
     * The remainder that indicates a valid IBAN under ISO 7064 Mod 97-10 ({@value}).
     * <p>
     * A correctly constructed IBAN, when processed by {@link #calculate(CharSequence)},
     * always produces this value.
     */
    public static final int  VALID_REMAINDER   = 1;

    /**
     * Sentinel value returned by {@link #calculate} variants when the input is
     * {@code null} or contains an illegal character ({@value}).
     * <p>
     * This value is outside the valid remainder range {@code [0, 96]} and can therefore
     * never arise from a legitimate calculation.  Callers should treat it as a
     * hard validation failure without further inspection.
     */
    public static final int  INVALID_REMAINDER = -1;

    /**
     * The number of characters in the IBAN header (country code + check digits)
     * that are moved to the end during the rearrangement step ({@value}).
     */
    private static final int HEADER_LENGTH     = 4;

    /**
     * Private constructor — utility class, not instantiable.
     * @throws UnsupportedOperationException always
     */
    private Mod97() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    // -------------------------------------------------------------------------
    // IBAN-style calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for a normalized IBAN character array,
     * applying the standard rearrangement step defined by ISO 13616.
     * <p>
     * The first four characters (country code and check digits) are processed last.
     * Internally, this is implemented as a two-pass iteration:
     * <ol>
     *   <li>BBAN part (index {@value #HEADER_LENGTH} to end)</li>
     *   <li>Header (index {@code 0} to {@value #HEADER_LENGTH}-1)</li>
     * </ol>
     *
     * <p><b>Input requirements:</b>
     * <ul>
     *   <li>Uppercase letters only ({@code A–Z})</li>
     *   <li>Digits ({@code 0–9})</li>
     *   <li>No whitespace or separators</li>
     * </ul>
     *
     * <p><b>Performance characteristics:</b>
     * <ul>
     *   <li>No intermediate allocations</li>
     *   <li>Linear memory access</li>
     *   <li>No modulo operation for index calculation</li>
     * </ul>
     *
     * @param input    the character array containing the normalized IBAN;
     *                 may be {@code null}
     * @param inputLen the number of characters to process; if {@code <= 0},
     *                 {@code input.length} is used
     *
     * @return the remainder in the range {@code [0, 96]}, or
     *         {@link #INVALID_REMAINDER} if:
     *         <ul>
     *           <li>{@code input} is {@code null}</li>
     *           <li>length is less than {@value #HEADER_LENGTH}</li>
     *           <li>an illegal character is encountered</li>
     *         </ul>
     *
     * @since 1.8.5
     */
    public static int calculate(final char[] input, final int inputLen) {
        if (input == null) {
            return INVALID_REMAINDER;
        }

        final int len = inputLen > 0 ? inputLen : input.length;
        if (len < HEADER_LENGTH) {
            return INVALID_REMAINDER;
        }

        int total = 0;

        // 1. BBAN (index 4..end)
        for (int i = HEADER_LENGTH; i < len; i++) {
            final char c = input[i];

            if (c >= '0' && c <= '9') {
                total = (total * 10 + (c - '0')) % MODULUS;
            } else if (c >= 'A' && c <= 'Z') {
                total = (total * 100 + (c - 'A' + 10)) % MODULUS;
            } else {
                return INVALID_REMAINDER;
            }
        }

        // 2. Header (index 0..3)
        for (int i = 0; i < HEADER_LENGTH; i++) {
            final char c = input[i];

            if (c >= '0' && c <= '9') {
                total = (total * 10 + (c - '0')) % MODULUS;
            } else if (c >= 'A' && c <= 'Z') {
                total = (total * 100 + (c - 'A' + 10)) % MODULUS;
            } else {
                return INVALID_REMAINDER;
            }
        }

        return total;
    }

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for a normalized IBAN character array.
     * <p>
     * This is a convenience wrapper for {@link #calculate(char[], int)} using the
     * full array length.
     *
     * @param input the normalized IBAN character array; may be {@code null}
     * @return the remainder in the range {@code [0, 96]}, or
     *         {@link #INVALID_REMAINDER} on invalid input
     *
     * @see #calculate(char[], int)
     */
    public static int calculate(final char[] input) {
        return calculate(input, input.length);
    }

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for a normalized IBAN character sequence,
     * applying the standard rearrangement step defined by ISO 13616.
     * <p>
     * Semantically equivalent to {@link #calculate(char[], int)}, but operates on a
     * {@link CharSequence} to avoid copying when working with {@link String} or
     * {@link StringBuilder}.
     *
     * <p><b>Input requirements:</b>
     * <ul>
     *   <li>Uppercase letters only ({@code A–Z})</li>
     *   <li>Digits ({@code 0–9})</li>
     *   <li>No whitespace or separators</li>
     * </ul>
     *
     * @param iban the normalized IBAN character sequence; may be {@code null}
     * @return the remainder in the range {@code [0, 96]}, or
     *         {@link #INVALID_REMAINDER} if the input is invalid
     *
     * @since 1.8.5
     */
    public static int calculate(final CharSequence iban) {
        if (iban == null) {
            return INVALID_REMAINDER;
        }

        final int len = iban.length();
        if (len < HEADER_LENGTH) {
            return INVALID_REMAINDER;
        }

        int total = 0;

        // 1. BBAN
        for (int i = HEADER_LENGTH; i < len; i++) {
            final char c = iban.charAt(i);

            if (c >= '0' && c <= '9') {
                total = (total * 10 + (c - '0')) % MODULUS;
            } else if (c >= 'A' && c <= 'Z') {
                total = (total * 100 + (c - 'A' + 10)) % MODULUS;
            } else {
                return INVALID_REMAINDER;
            }
        }

        // 2. Header
        for (int i = 0; i < HEADER_LENGTH; i++) {
            final char c = iban.charAt(i);

            if (c >= '0' && c <= '9') {
                total = (total * 10 + (c - '0')) % MODULUS;
            } else if (c >= 'A' && c <= 'Z') {
                total = (total * 100 + (c - 'A' + 10)) % MODULUS;
            } else {
                return INVALID_REMAINDER;
            }
        }

        return total;
    }

    // -------------------------------------------------------------------------
    // General-purpose calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for a contiguous sub-range of a
     * character array, <em>without</em> any rearrangement step.
     * <p>
     * Operates directly on the {@code char[]} — no wrapper allocation, no virtual dispatch.
     * Semantically equivalent to {@link #calculateRange(CharSequence, int, int)} but
     * faster on paths where the data is already available as an array.
     *
     * @param data   the source character array; must not be {@code null}
     * @param offset start index within {@code data} (inclusive)
     * @param length number of characters to process
     * @return the remainder in the range {@code [0, 96]};
     *         or {@link #INVALID_REMAINDER} if an illegal character is encountered
     * @throws NullPointerException      if {@code data} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} or {@code offset + length}
     *                                   are outside the array bounds
     *
     * @since 1.8.5
     */
    public static int calculateRange(final char[] data, final int offset, final int length) {
        Objects.requireNonNull(data, "Data must not be null");

        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IndexOutOfBoundsException(
                String.format("Invalid range (offset: %d, length: %d) for sequence length %d",
                    offset, length, data.length));
        }

        int remainder = 0;
        for (int i = 0; i < length; i++) {
            final char c = data[offset + i];
            if (c >= '0' && c <= '9') {
                remainder = (remainder * 10 + (c - '0')) % MODULUS;
            } else if (c >= 'A' && c <= 'Z') {
                // two-digit letter expansion: A=10 … Z=35
                remainder = (remainder * 100 + (c - 'A' + 10)) % MODULUS;
            } else {
                return INVALID_REMAINDER;
            }
        }
        return remainder;
    }

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for a contiguous sub-range of a
     * character sequence, <em>without</em> any rearrangement step.
     * <p>
     * This variant is useful for computing the remainder of a BBAN sub-sequence
     * (e.g. for National Check Digit calculation).
     *
     * @param data   the source character sequence; must not be {@code null}
     * @param offset start index within {@code data} (inclusive)
     * @param length number of characters to process
     * @return the remainder in the range {@code [0, 96]};
     *         or {@link #INVALID_REMAINDER} if an illegal character is encountered
     * @throws NullPointerException      if {@code data} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} or {@code offset + length}
     *                                   are outside the sequence bounds
     *
     * @since 1.8.5
     */
    public static int calculateRange(final CharSequence data, final int offset, final int length) {
        Objects.requireNonNull(data, "Data must not be null");

        if (offset < 0 || length < 0 || offset + length > data.length()) {
            throw new IndexOutOfBoundsException(
                String.format("Invalid range (offset: %d, length: %d) for sequence length %d",
                    offset, length, data.length()));
        }

        int remainder = 0;
        for (int i = 0; i < length; i++) {
            final char c = data.charAt(offset + i);
            if (c >= '0' && c <= '9') {
                remainder = (remainder * 10 + (c - '0')) % MODULUS;
            } else if (c >= 'A' && c <= 'Z') {
                // two-digit letter expansion: A=10 … Z=35
                remainder = (remainder * 100 + (c - 'A' + 10)) % MODULUS;
            } else {
                return INVALID_REMAINDER;
            }
        }
        return remainder;
    }

    // -------------------------------------------------------------------------
    // Convenience boolean wrappers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the ISO 7064 Mod 97-10 remainder of the given IBAN
     * character array equals {@link #VALID_REMAINDER} ({@value #VALID_REMAINDER}).
     * <p>
     * This is equivalent to:
     * <pre>{@code
     * calculate(iban) == VALID_REMAINDER
     * }</pre>
     *
     *
     * @param iban the normalized IBAN character array (uppercase, no spaces)
     * @return {@code true} if the checksum is valid
     *
     * @since 1.8.5
     */
    public static boolean isValid(final char[] iban) {
        return calculate(iban) == VALID_REMAINDER;
    }

    /**
     * Returns {@code true} if the ISO 7064 Mod 97-10 remainder of the given IBAN
     * character array segment equals {@link #VALID_REMAINDER} ({@value #VALID_REMAINDER}).
     * <p>
     * This variant is optimized for use with shared or pooled buffers where only
     * a specific portion of the array contains the IBAN to be validated.
     *
     * @param iban   the normalized IBAN character array
     * @param length the number of characters to process from the array
     * @return {@code true} if the checksum is valid, {@code false} otherwise
     *
     * @since 1.8.5
     */
    public static boolean isValid(final char[] iban, final int length) {
        return calculate(iban, length) == VALID_REMAINDER;
    }

    /**
     * Returns {@code true} if the ISO 7064 Mod 97-10 remainder of the given IBAN
     * character sequence equals {@link #VALID_REMAINDER} ({@value #VALID_REMAINDER}).
     * <p>
     * Equivalent to {@code calculate(iban) == VALID_REMAINDER}.
     *
     * @param iban the normalized IBAN character sequence (uppercase, no spaces)
     * @return {@code true} if the checksum is valid
     *
     * @since 1.8.5
     */
    public static boolean isValid(final CharSequence iban) {
        return calculate(iban) == VALID_REMAINDER;
    }

}
