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
 * <h3>IBAN-style calculation (with rearrangement)</h3>
 * {@link #calculate(CharSequence)} and {@link #calculate(char[])} implement
 * the <em>rearrangement step</em> mandated by ISO 13616: the four-character
 * header (country code + check digits at positions 0–3) is processed <em>last</em>,
 * after the BBAN (positions 4 onwards).  A valid IBAN yields a remainder of exactly
 * {@link #VALID_REMAINDER} ({@value #VALID_REMAINDER}).
 *
 * <h3>General-purpose calculation (no rearrangement)</h3>
 * {@link #calculateRange(char[], int, int)} processes a contiguous sub-range of a
 * character array without any rearrangement.  This variant is used internally for
 * National Check Digit (NCD) calculations that operate on BBAN sub-sequences.
 *
 * <h3>Return value on invalid input</h3>
 * Both {@code calculate} variants return {@link #INVALID_REMAINDER}
 * ({@value #INVALID_REMAINDER}) when the input is {@code null} or contains a
 * character that is neither a digit {@code '0'–'9'} nor an uppercase letter
 * {@code 'A'–'Z'}.  Callers must treat this sentinel as a validation failure.
 *
 * <h3>Usage examples</h3>
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
    public static final int   MODULUS           = 97;

    /**
     * The remainder that indicates a valid IBAN under ISO 7064 Mod 97-10 ({@value}).
     * <p>
     * A correctly constructed IBAN, when processed by {@link #calculate(CharSequence)},
     * always produces this value.
     */
    public static final int   VALID_REMAINDER   = 1;

    /**
     * Sentinel value returned by {@link #calculate} variants when the input is
     * {@code null} or contains an illegal character ({@value}).
     * <p>
     * This value is outside the valid remainder range {@code [0, 96]} and can therefore
     * never arise from a legitimate calculation.  Callers should treat it as a
     * hard validation failure without further inspection.
     */
    public static final int   INVALID_REMAINDER = -1;

    /**
     * The number of characters in the IBAN header (country code + check digits)
     * that are moved to the end during the rearrangement step ({@value}).
     */
    private static final int  HEADER_LENGTH     = 4;

    /**
     * Overflow guard for the accumulator.
     * <p>
     * The accumulator is reduced modulo {@link #MODULUS} whenever it reaches this
     * threshold, preventing {@code long} overflow.  Safe upper bound:
     * {@code 10^15 × 100 + 35 < Long.MAX_VALUE}.
     */
    private static final long OVERFLOW_GUARD    = 1_000_000_000_000_000L;

    /**
     * Private constructor — utility class, not instantiable.
     * @throws UnsupportedOperationException always
     */
    private Mod97() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    // -------------------------------------------------------------------------
    // IBAN-style calculation (with 4-character header rearrangement)
    // -------------------------------------------------------------------------

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for the given IBAN character sequence,
     * applying the standard <em>rearrangement step</em>: the first {@value #HEADER_LENGTH}
     * characters (country code and check digits) are processed after the BBAN.
     * <p>
     * This is the primary method for IBAN check-digit validation.  A valid IBAN always
     * produces {@link #VALID_REMAINDER}.
     * <p>
     * The method operates directly on the {@link CharSequence} — zero heap allocations
     * for {@link String} and {@link StringBuilder} inputs.
     *
     * @param iban the normalized IBAN character sequence (uppercase, no spaces);
     *             may be {@code null}
     * @return the remainder in the range {@code [0, 96]}, or {@link #INVALID_REMAINDER}
     *         if the input is {@code null} or contains a character that is neither
     *         a digit ({@code '0'–'9'}) nor an uppercase letter ({@code 'A'–'Z'})
     *
     * @since 1.8.5
     */
    @SuppressWarnings("PMD.UselessParentheses")
    public static int calculate(final CharSequence iban) {
        if (iban == null) {
            return INVALID_REMAINDER;
        }

        final int len = iban.length();
        if (len == 0) {
            return INVALID_REMAINDER;
        }

        long total = 0;

        for (int i = 0; i < len; i++) {
            // rearrangement: BBAN first (indices HEADER_LENGTH…end), then header (0…HEADER_LENGTH-1)
            // idx %= len handles all lengths correctly, including inputs shorter than HEADER_LENGTH
            final int idx = (i + HEADER_LENGTH) % len;
            final char c = iban.charAt(idx);

            if (CharUtil.isDigit(c)) {
                total = total * 10 + (c - '0');
            } else if (CharUtil.isUpperCase(c)) {
                total = total * 100 + (c - 'A' + 10);
            } else {
                return INVALID_REMAINDER;
            }

            if (total >= OVERFLOW_GUARD) {
                total %= MODULUS;
            }
        }
        return (int) (total % MODULUS);
    }

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for the given IBAN character array,
     * applying the standard <em>rearrangement step</em> (first {@value #HEADER_LENGTH}
     * characters processed last).
     * <p>
     * Delegates to {@link #calculate(CharSequence)} via an internal {@code CharArrayWrapper};
     * semantically equivalent and allocation-free for callers.
     *
     * @param iban the normalized IBAN character array (uppercase, no spaces);
     *             may be {@code null}
     * @return the remainder in the range {@code [0, 96]}, or {@link #INVALID_REMAINDER}
     *         if the input is {@code null} or contains an illegal character
     *
     * @since 1.8.5
     */
    public static int calculate(final char[] iban) {
        if (iban == null || iban.length == 0) {
            return INVALID_REMAINDER;
        }

        return calculate(new CharArrayWrapper(iban));
    }

    // -------------------------------------------------------------------------
    // General-purpose calculation (straight range, no rearrangement)
    // -------------------------------------------------------------------------

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
            if (CharUtil.isDigit(c)) {
                remainder = (remainder * 10 + (c - '0')) % MODULUS;
            } else if (CharUtil.isUpperCase(c)) {
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
     * character array, <em>without</em> any rearrangement step.
     * <p>
     * This variant is useful for computing the remainder of a BBAN sub-sequence
     * (e.g. for National Check Digit calculation), where the caller already controls
     * which characters to process and in which order.
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

        return calculateRange(new CharArrayWrapper(data), offset, length);
    }

    // -------------------------------------------------------------------------
    // Convenience boolean wrappers
    // -------------------------------------------------------------------------

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

    /**
     * Returns {@code true} if the ISO 7064 Mod 97-10 remainder of the given IBAN
     * character array equals {@link #VALID_REMAINDER} ({@value #VALID_REMAINDER}).
     * <p>
     * Equivalent to {@code calculate(iban) == VALID_REMAINDER}.
     *
     * @param iban the normalized IBAN character array (uppercase, no spaces)
     * @return {@code true} if the checksum is valid
     *
     * @since 1.8.5
     */
    public static boolean isValid(final char[] iban) {
        return calculate(iban) == VALID_REMAINDER;
    }

}
