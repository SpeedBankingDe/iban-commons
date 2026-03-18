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
import static de.speedbanking.iban.IbanRegistry.MAX_IBAN_LENGTH;
import static de.speedbanking.iban.IbanRegistry.MIN_IBAN_LENGTH;
import static de.speedbanking.util.CharUtil.isDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isNotDigit;
import static de.speedbanking.util.CharUtil.isNotUpperCase;

/**
 * The core engine for **International Bank Account Number (IBAN)** validation.
 * <p>
 * Validation is designed to fail fast, aborting at the first error detected.<br>
 * This class is only responsible for validation and does not create {@link Iban} objects.
 *
 * @since 1.8.0
 */
public final class IbanValidator {

    /**
     * The modulus used in the ISO 7064 Mod 97-10 check (97).
     */
    private static final int                              MOD97         = 97;

    /**
     * Return value for a failed Mod 97-10 calculation.
     */
    static final int                                      INVALID_MOD97 = -1;

    /**
     * A limit used to trigger the intermediate modulo operation during the
     * Mod 97-10 calculation to prevent {@code long} overflow, set to 10^15.
     * <p>
     * Safe upper bound: the next iteration multiplies by at most 100 and adds at most 35,
     * so {@code 10^15 * 100 + 35 < Long.MAX_VALUE}.
     */
    private static final long                             MAX           = 1_000_000_000_000_000L;

    /**
     * Simple thread-local holder for the last failure reason for the {@link Iban#of(CharSequence)} simplicity.
     * <p>
     * Ensures that the reason for failure is correctly associated with the calling thread
     * when using a simplified API that doesn't return the full result object.
     * <p>
     * Only written on the {@link #validate}/{@link #validateRaw} path.
     * {@link #isValid} never touches this field, avoiding unnecessary ThreadLocal overhead
     * on the hot validation-only path.
     */
    private static final ThreadLocal<IbanValidationError> LAST_REASON   = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private IbanValidator() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Performs a full IBAN validation and returns the required data for IBAN object creation.
     * <p>
     * This is the preferred method for factory methods (like {@code Iban.of()})
     * as it aborts validation on failure and provides the normalized data on success.
     *
     * @param rawIban the IBAN character sequence to validate, potentially containing spaces
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     *
     * @since 1.8.0
     */
    static IbanValidationSuccess validate(final CharSequence rawIban) {
        return validateRaw(rawIban);
    }

    /**
     * Performs a full IBAN validation and returns {@code true} if successful,
     * or {@code false} if any validation step fails.
     * <p>
     * This method is optimized for the common case of normalized input (no spaces):
     * it performs <strong>zero heap allocations</strong> by running the Mod 97-10 check
     * directly on the input {@link CharSequence}.
     * <p>
     * For formatted input containing spaces, a single {@code char[]} is allocated for
     * normalization. The {@link ThreadLocal} error state is intentionally never written
     * on this path (avoiding unnecessary memory-barrier overhead).
     *
     * @param iban the IBAN character sequence to validate (may contain spaces)
     * @return {@code true} if the IBAN is valid, {@code false} otherwise
     *
     * @since 1.8.0
     */
    public static boolean isValid(final CharSequence iban) {
        if (iban == null) {
            return false;
        }

        int len = iban.length();
        if (len < MIN_IBAN_LENGTH) {
            return false;
        }

        // country code: must be exactly 2 uppercase letters
        final char c1 = iban.charAt(0);
        if (isNotUpperCase(c1)) {
            return false;
        }
        final char c2 = iban.charAt(1);
        if (isNotUpperCase(c2)) {
            return false;
        }

        final IbanRegistry countryData = IbanRegistry.getByCode(c1, c2);
        if (countryData == null) {
            return false;
        }

        // count spaces starting at index 2 (country code already validated above)
        int spaces = 0;
        for (int i = 2; i < len; i++) {
            if (iban.charAt(i) == ' ') {
                spaces++;
            }
        }
        if ((len - spaces) != countryData.getIbanLength()) {
            return false;
        }

        if (spaces == 0) {
            // Fast path: zero allocations, no ThreadLocal writes
            // calculateMod97(CharSequence) uses in-place index rotation — no intermediate array.
            return calculateMod97(iban) == 1;
        }

        // Slow path: formatted input — normalize into char[] without touching ThreadLocal
        return isValidSpaced(iban, countryData);
    }

    /**
     * Normalizes a space-containing IBAN into a {@code char[]} and validates it
     * without writing to the {@link #LAST_REASON} thread-local.
     * <p>
     * Called exclusively from {@link #isValid} when the input contains spaces.
     * The loop iterates over the <em>full input length</em> (not the normalized IBAN length)
     * so that all characters, including those after the first group of spaces, are processed.
     *
     * @param iban        the raw IBAN containing spaces
     * @param countryData the already-resolved registry entry
     * @return {@code true} if the normalized IBAN passes all validation checks
     */
    static boolean isValidSpaced(final CharSequence iban, final IbanRegistry countryData) {
        if (iban == null || countryData == null) {
            return false;
        }

        int ibanLength = countryData.getIbanLength();
        final char[] normArr = new char[ibanLength];
        int normIdx = 0;

        for (int i = 0, len = iban.length(); i < len; i++) {
            final char c = iban.charAt(i);

            if (c == ' ') {
                continue;
            }

            if (normIdx <= 1) {
                // country code: already validated before entering this method
                normArr[normIdx++] = c;
            } else if (normIdx <= 3) {
                // check digits: must be numeric
                if (isNotDigit(c)) {
                    return false;
                }
                normArr[normIdx++] = c;
            } else {
                // BBAN: digits and uppercase letters only
                if (!isDigitOrUpperCase(c)) {
                    return false;
                } else if (normIdx >= ibanLength) {
                    return false; // too many non-space characters
                }
                normArr[normIdx++] = c;
            }
        }

        // BBAN structure check (country-specific)
        final CountryValidator countryValidator = countryData.getCountryValidator();
        if (countryValidator != null && !countryValidator.validateIban(normArr)) {
            return false;
        }

        return calculateMod97(normArr) == 1;
    }

    /**
     * Performs a full IBAN validation on an **unnormalized** IBAN (i.e., may contain spaces)
     * and returns the success data or {@code null} on failure.
     *
     * @param rawIban the IBAN character sequence to validate, potentially containing spaces
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     *
     * @since 1.8.0
     */
    static IbanValidationSuccess validateRaw(final CharSequence rawIban) {

        if (rawIban == null) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        final int len = rawIban.length();
        if (len == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        // overall length of the normalized sequence
        int normLen = 0;

        // char array holding normalized (space-free) IBAN, size determined by country-specific length
        char[] normIbanArr = null;

        // first non-space character (country code position 1)
        char c1 = 0;
        char c;

        IbanRegistry countryData = null;

        // Normalization and character set check (combined single pass)
        for (int i = 0; i < len; i++) {
            c = rawIban.charAt(i);

            if (c == ' ') {
                continue; // skip space
            }

            normLen++;

            if (normLen <= 2) {

                // country code: two uppercase letters
                if (isNotUpperCase(c)) {
                    return validationFailed(IbanValidationError.INVALID_COUNTRY);
                }

                if (normLen == 1) {

                    c1 = c;

                } else {
                    // normLen == 2: country code complete, look up registry
                    countryData = IbanRegistry.getByCode(c1, c);

                    if (countryData == null) {
                        return validationFailed(IbanValidationError.UNSUPPORTED_COUNTRY);
                    }

                    normIbanArr = new char[countryData.getIbanLength()];
                    normIbanArr[0] = c1;
                    normIbanArr[1] = c;
                }

                continue;

            } else if (normLen <= 4) {

                // check digits: must be numeric
                if (isNotDigit(c)) {
                    return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
                }

            } else if (!isDigitOrUpperCase(c)) {

                // BBAN: uppercase ASCII letters (A-Z) and digits (0-9) only
                return validationFailed(IbanValidationError.ILLEGAL_CHARACTERS);

            }

            if (normLen > normIbanArr.length) {
                return validationFailed(IbanValidationError.INCORRECT_LENGTH_COUNTRY);
            }

            normIbanArr[normLen - 1] = c;

        }

        // post-loop length checks
        if (normLen == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        // check min/max lengths
        if (normLen < MIN_IBAN_LENGTH || normLen > MAX_IBAN_LENGTH) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH);
        }

        // check specific length for country
        if (normLen != countryData.getIbanLength()) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH_COUNTRY);
        }

        // shared logic validation steps
        return validateCommon(normIbanArr, countryData);
    }

    /**
     * Performs a full IBAN validation on an **already normalized** IBAN (must not contain spaces)
     * and returns the success data or {@code null} on failure.
     *
     * @param normalizedIban the normalized IBAN character sequence to validate (no spaces)
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     *
     * @since 1.8.0
     */
    static IbanValidationSuccess validateNormalized(final CharSequence normalizedIban) {

        if (normalizedIban == null) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        int len = normalizedIban.length();

        if (len == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        } else if (len < MIN_IBAN_LENGTH || len > MAX_IBAN_LENGTH) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH);
        }

        // check country code and registry (first 2 chars)
        char c1 = normalizedIban.charAt(0);
        if (isNotUpperCase(c1)) {
            return validationFailed(IbanValidationError.INVALID_COUNTRY);
        }
        char c2 = normalizedIban.charAt(1);
        if (isNotUpperCase(c2)) {
            return validationFailed(IbanValidationError.INVALID_COUNTRY);
        }

        IbanRegistry countryData = IbanRegistry.getByCode(c1, c2);

        if (countryData == null) {
            return validationFailed(IbanValidationError.UNSUPPORTED_COUNTRY);
        }

        // check specific length for country
        if (len != countryData.getIbanLength()) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH_COUNTRY);
        }

        // check digit characters must be numeric
        char c3 = normalizedIban.charAt(2);
        if (isNotDigit(c3)) {
            return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
        }
        char c4 = normalizedIban.charAt(3);
        if (isNotDigit(c4)) {
            return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
        }

        char[] normIbanArr = new char[len];
        normIbanArr[0] = c1;
        normIbanArr[1] = c2;
        normIbanArr[2] = c3;
        normIbanArr[3] = c4;

        // BBAN part (from index 4 onwards): digits or uppercase only
        for (int i = INDEX_BBAN; i < len; i++) {
            char ci = normalizedIban.charAt(i);
            if (!isDigitOrUpperCase(ci)) {
                return validationFailed(IbanValidationError.ILLEGAL_CHARACTERS);
            }
            normIbanArr[i] = ci;
        }

        // shared logic validation steps
        return validateCommon(normIbanArr, countryData);
    }

    /**
     * Final validation steps shared by all full-validation paths: BBAN structure check and Mod 97.
     * <p>
     * On success, clears the thread-local error state and returns a success carrier object.
     * On failure, stores the reason in the thread-local and returns {@code null}.
     *
     * @param normIbanArr the normalized IBAN as a char array
     * @param countryData the {@link IbanRegistry} data
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     */
    static IbanValidationSuccess validateCommon(final char[] normIbanArr, final IbanRegistry countryData) {

        // check BBAN structure (country-specific)
        CountryValidator countryValidator = countryData.getCountryValidator();
        if (countryValidator != null && !countryValidator.validateIban(normIbanArr)) {
            return validationFailed(IbanValidationError.INVALID_STRUCTURE);
        }

        // check Mod 97 (most expensive operation — performed last)
        if (!isMod97Valid(normIbanArr)) {
            return validationFailed(IbanValidationError.INVALID_CHECKSUM);
        }

        // success: reset thread-local error state and return carrier object
        LAST_REASON.remove();

        return new IbanValidationSuccess(normIbanArr, countryData);
    }

    /**
     * Returns {@code true} if the ISO 7064 Mod 97-10 remainder of the given IBAN equals {@code 1}.
     *
     * @param iban the normalized IBAN char array (uppercase, no spaces)
     * @return {@code true} if the checksum is valid
     */
    static boolean isMod97Valid(final char[] iban) {
        return calculateMod97(iban) == 1;
    }

    /**
     * Returns {@code true} if the ISO 7064 Mod 97-10 remainder of the given IBAN equals {@code 1}.
     *
     * @param iban the normalized IBAN {@link CharSequence} (uppercase, no spaces)
     * @return {@code true} if the checksum is valid
     */
    static boolean isMod97Valid(final CharSequence iban) {
        return calculateMod97(iban) == 1;
    }

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder using in-place index rotation on a {@code char[]}.
     * <p>
     * Processes the BBAN part first (indices 4 to end) then the header (indices 0 to 3) in a single
     * loop using manual index arithmetic — faster than {@code (i + INDEX_BBAN) % len} and avoids
     * any object allocation. Intermediate modulo operations are performed only when
     * {@code total >= MAX} to minimize expensive divisions.
     *
     * @param iban the normalized IBAN array (uppercase, no spaces)
     * @return the remainder (0–96), or {@link #INVALID_MOD97} on {@code null},
     *         too-short input, or illegal characters
     */
    @SuppressWarnings("PMD.UselessParentheses")
    static int calculateMod97(final char[] iban) {
        if (iban == null) {
            return INVALID_MOD97;
        }

        final int len = iban.length;

        if (len < MIN_IBAN_LENGTH || len > MAX_IBAN_LENGTH) {
            return INVALID_MOD97;
        }

        long total = 0;

        for (int i = 0; i < len; i++) {
            // in-place BBAN-first rotation: faster than (i + INDEX_BBAN) % len
            int idx = i + INDEX_BBAN;
            if (idx >= len) {
                idx -= len;
            }
            final char c = iban[idx];

            if (c >= '0' && c <= '9') {
                total = total * 10 + (c - '0');
            } else if (c >= 'A' && c <= 'Z') {
                total = total * 100 + (c - 'A' + 10);
            } else {
                return INVALID_MOD97;
            }

            // intermediate modulo only when necessary to prevent long overflow
            if (total >= MAX) {
                total %= MOD97;
            }
        }
        return (int) (total % MOD97);
    }

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder using in-place index rotation on a {@link CharSequence}.
     * <p>
     * Operates directly on the {@link CharSequence} — supports {@link String}, {@link StringBuilder},
     * and any other implementation without requiring an intermediate {@code char[]}.
     * This enables the zero-allocation fast path in {@link #isValid}.
     *
     * @param iban the normalized IBAN {@link CharSequence} (uppercase, no spaces)
     * @return the remainder (0–96), or {@link #INVALID_MOD97} on {@code null},
     *         too-short input, or illegal characters
     */
    @SuppressWarnings("PMD.UselessParentheses")
    static int calculateMod97(final CharSequence iban) {
        if (iban == null) {
            return INVALID_MOD97;
        }

        final int len = iban.length();

        if (len < MIN_IBAN_LENGTH || len > MAX_IBAN_LENGTH) {
            return INVALID_MOD97;
        }

        long total = 0;

        for (int i = 0; i < len; i++) {
            // in-place BBAN-first rotation: faster than (i + INDEX_BBAN) % len
            int idx = i + INDEX_BBAN;
            if (idx >= len) {
                idx -= len;
            }
            final char c = iban.charAt(idx);

            // fast value conversion and incremental total calculation
            if (c >= '0' && c <= '9') {
                total = total * 10 + (c - '0');
            } else if (c >= 'A' && c <= 'Z') {
                total = total * 100 + (c - 'A' + 10);
            } else {
                return INVALID_MOD97;
            }

            // intermediate modulo only when necessary to prevent long overflow
            if (total >= MAX) {
                total %= MOD97;
            }
        }
        return (int) (total % MOD97);
    }

    /**
     * Calculates the correct ISO 7064 Mod 97-10 check digits for the given IBAN string
     * and overwrites the placeholders (usually {@code "00"}) at the check digit positions
     * (index 2 and 3).
     * <p>
     * This method temporarily sets the check digits to {@code "00"} to calculate the
     * required remainder {@code R}, then determines the final check digits {@code CD = 98 - R}.
     *
     * @param iban the IBAN character sequence (must already be of the full IBAN length,
     *             with placeholders at the check digit position); if a {@code StringBuilder}
     *             is passed, it is mutated in place, otherwise a copy is created
     * @return the same {@code StringBuilder} instance with the correct check digits applied
     */
    public static StringBuilder fixCheckDigits(final CharSequence iban) {
        final StringBuilder sb = iban instanceof StringBuilder
            ? (StringBuilder) iban
            : new StringBuilder(iban);

        // set placeholders to "00" (required for correct calculation context)
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT1, '0');
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT2, '0');

        // calculate the required check digits value (98 - modulo result)
        final int checkDigitsValue = 98 - calculateMod97(sb);

        // manual zero-padding: faster than String.format
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT1, (char) ('0' + (checkDigitsValue / 10)));
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT2, (char) ('0' + (checkDigitsValue % 10)));

        return sb;
    }

    /**
     * Retrieves the last single reason for a validation failure, for use by {@link Iban#of(CharSequence)}.
     *
     * @return the last validation failure
     *
     * @since 1.8.0
     */
    public static IbanValidationError getLastReason() {
        return LAST_REASON.get();
    }

    static void setLastReason(final IbanValidationError reason) {
        LAST_REASON.remove();
        LAST_REASON.set(reason);
    }

    /**
     * Finalizes an invalid validation result by storing the reason in the thread-local
     * and returning {@code null}.
     *
     * @param reason the reason for the validation failure
     * @return always {@code null}
     */
    static IbanValidationSuccess validationFailed(final IbanValidationError reason) {
        setLastReason(reason);
        return null;
    }

}
