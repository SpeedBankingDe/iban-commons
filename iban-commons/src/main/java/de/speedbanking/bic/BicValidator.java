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

import static de.speedbanking.util.CharUtil.isDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isNotUpperCase;

import de.speedbanking.util.Country;

import java.nio.CharBuffer;

/**
 * The core engine for BIC validation.
 * <p>
 * Validation is designed to abort at the first error (fail-fast).
 *
 * @since 1.8.0
 */
public final class BicValidator {

    /**
     * Internal thread-local buffer used to perform BIC validation without heap allocations.
     * <p>
     * To maximize throughput in high-concurrency or batch-processing scenarios, this
     * buffer allows the validator to copy and transform raw input within the same memory area.
     * Using a {@link ThreadLocal} ensures thread-safety while avoiding the overhead of frequent
     * {@code char[]} allocations and the resulting garbage collection pressure.
     * <p>
     * The capacity is set to {@code BIC11_LENGTH}, the longest possible BIC.
     */
    private static final ThreadLocal<char[]> VALIDATION_BUFFER = ThreadLocal
        .withInitial(() -> new char[Bic.BIC11_LENGTH]);

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private BicValidator() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Performs a fast, allocation-free check whether the given BIC is valid.
     *
     * @param bic the BIC character sequence to validate
     * @return {@code true} if the BIC is valid, {@code false} otherwise
     *
     * @since 1.8.8
     */
    public static boolean isValid(final CharSequence bic) {
        if (bic == null) {
            return false;
        }

        int len = bic.length();
        if (len != Bic.BIC8_LENGTH && len != Bic.BIC11_LENGTH) {
            return false;
        }

        char[] buffer = copyToBuffer(bic, len, VALIDATION_BUFFER.get());

        return isValidInternal(buffer, len);
    }

    /**
     * String-optimized overload of {@link #isValid(CharSequence)}.
     * <p>
     * Resolved statically by the compiler to bypass virtual dispatch overhead of CharSequence.
     *
     * @param bic the BIC string to validate
     * @return {@code true} if the BIC is valid, {@code false} otherwise
     *
     * @since 1.8.8
     */
    public static boolean isValid(final String bic) {
        if (bic == null) {
            return false;
        }

        int len = bic.length();
        if (len != Bic.BIC8_LENGTH && len != Bic.BIC11_LENGTH) {
            return false;
        }

        char[] buffer = VALIDATION_BUFFER.get();
        bic.getChars(0, len, buffer, 0);

        return isValidInternal(buffer, len);
    }

    /**
     * Performs a full BIC validation on an input character sequence
     * and returns a {@link BicValidationResult}.
     *
     * @param bic the BIC character sequence to validate
     * @return the validation result
     *
     * @since 1.8.0
     */
    static BicValidationResult validate(final CharSequence bic) {
        if (bic == null) {
            return BicValidationResult.invalid(BicValidationError.EMPTY);
        }

        int len = bic.length();

        if (len == 0) {
            return BicValidationResult.invalid(BicValidationError.EMPTY);
        } else if (len != Bic.BIC8_LENGTH && len != Bic.BIC11_LENGTH) {
            return BicValidationResult.invalid(BicValidationError.INCORRECT_LENGTH);
        }

        char[] buffer = copyToBuffer(bic, len, VALIDATION_BUFFER.get());

        BicValidationError error = validateInternal(buffer, len);
        return error == null ? BicValidationResult.valid(bic) : BicValidationResult.invalid(error);
    }

    /**
     * Internal character-by-character validation based on position-specific requirements.
     * <p>
     * This method assumes that null-checks and length-checks have already been performed.
     * It evaluates bank code, country code (ISO 3166), and location/branch codes.
     *
     * @since 1.8.5
     *
     * @param buffer the character array to validate
     * @param len the pre-calculated length of the sequence
     * @return the first encountered {@link BicValidationError}, or {@code null} if all characters are valid
     */
    static BicValidationError validateInternal(final char[] buffer, final int len) {
        for (int i = 0; i < len; i++) {
            char c = buffer[i];

            // Strategy: Guard clauses or dedicated methods for position-based logic
            if (i < Bic.COUNTRY_CODE_START) { // <4
                if (isNotUpperCase(c)) {
                    return BicValidationError.INVALID_BANK_CODE;
                }
            } else if (i == Bic.COUNTRY_CODE_START) { // ==4 (country code part 1)
                if (isNotUpperCase(c)) {
                    return BicValidationError.INVALID_COUNTRY;
                }
            } else if (i == Bic.COUNTRY_CODE_START + 1) { // country code part 2 & full ISO check
                if (!Country.isAssigned(buffer[i - 1], c)) {
                    return BicValidationError.INVALID_COUNTRY;
                }
            } else if (!isDigitOrUpperCase(c)) { // location & branch Code
                return BicValidationError.ILLEGAL_CHARACTERS;
            }
        }
        return null;
    }

    /**
     * Core validation logic operating directly on a char array to guarantee high performance.
     * <p>
     * This fast-path method yields a boolean result without producing diagnostic error objects.
     *
     * @since 1.8.8
     *
     * @param bic the character array containing the normalized BIC characters
     * @param len the pre-calculated length of the BIC (8 or 11)
     * @return {@code true} if all positional and structural constraints match, {@code false} otherwise
     */
    static boolean isValidInternal(final char[] bic, final int len) {
        for (int i = 0; i < len; i++) {
            if (i < Bic.COUNTRY_CODE_START) {
                if (isNotUpperCase(bic[i])) {
                    return false;
                }
            } else if (i == Bic.COUNTRY_CODE_START) {
                if (isNotUpperCase(bic[i])) {
                    return false;
                }
            } else if (i == Bic.COUNTRY_CODE_START + 1) {
                if (!Country.isAssigned(bic[i - 1], bic[i])) {
                    return false;
                }
            } else if (!isDigitOrUpperCase(bic[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies characters from a {@link CharSequence} into the target buffer.
     * <p>
     * This method optimizes the copying process by checking for common concrete
     * types like {@link String} and {@link StringBuilder} to leverage fast,
     * hardware-accelerated block copying via {@code getChars()}. It falls back
     * to a sequential loop for other character sequence implementations.
     *
     * @param source the source character sequence to copy from
     * @param sourceLen the number of characters to copy
     * @param target the destination array
     * @return the destination array
     */
    static char[] copyToBuffer(final CharSequence source, final int sourceLen, final char[] target) {
        if (source instanceof String) {
            ((String) source).getChars(0, sourceLen, target, 0);
        } else if (source instanceof StringBuilder) {
            ((StringBuilder) source).getChars(0, sourceLen, target, 0);
        } else if (source instanceof CharBuffer) {
            ((CharBuffer) source).duplicate().get(target, 0, sourceLen);
        } else {
            for (int i = 0; i < sourceLen; i++) {
                target[i] = source.charAt(i);
            }
        }
        return target;
    }

}
