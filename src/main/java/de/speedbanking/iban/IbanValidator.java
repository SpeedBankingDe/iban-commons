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

import static de.speedbanking.iban.IbanRegistry.MAX_IBAN_LENGTH;
import static de.speedbanking.iban.IbanRegistry.MIN_IBAN_LENGTH;
import static de.speedbanking.util.CharUtil.isAllDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isLowerCase;
import static de.speedbanking.util.CharUtil.isNotDigit;

import de.speedbanking.util.CharArrayWrapper;
import de.speedbanking.util.Mod97;

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
     * Return value for a failed Mod 97-10 calculation.
     * <p>
     * Delegates to {@link Mod97#INVALID_REMAINDER} for a consistent sentinel across the API.
     */
    public static final int                               INVALID_MOD97 = Mod97.INVALID_REMAINDER;

    /**
     * Simple thread-local holder for the last failure reason for the {@link Iban#of(CharSequence)} simplicity.
     * <p>
     * Ensures that the reason for failure is correctly associated with the calling thread
     * when using a simplified API that doesn't return the full result object.
     * <p>
     * Only written on the {@link #validate} path.<br>
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
     * Performs a full IBAN validation and returns {@code true} if successful.
     * <p>
     * The validation process includes:
     * <ul>
     *   <li>Normalization based on {@link IbanConfig} (handling of spaces and casing)</li>
     *   <li>Basic length and format checks</li>
     *   <li>Country-specific length and BBAN structure validation via {@link IbanRegistry}</li>
     *   <li>ISO 7064 Mod 97-10 check digit verification</li>
     * </ul>
     * <p>
     * Unlike the {@code validate} methods, this path is optimized for speed and
     * does not write to the {@link ThreadLocal} error state. It always performs
     * a full check against the {@link IbanRegistry} to ensure the country code
     * and length are valid.
     *
     * @param iban the IBAN character sequence to validate (may be unnormalized
     *             depending on {@link IbanConfig} settings)
     * @return {@code true} if the IBAN is valid according to all criteria, {@code false} otherwise
     * @see IbanConfig#ALLOW_SPACE
     * @see IbanConfig#ALLOW_LOWERCASE
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

        // convert input to normalized char array based on library configuration
        CharSequence normIban = normalize(iban, len,
            IbanConfig.ALLOW_SPACE.isEnabled(),
            IbanConfig.ALLOW_LOWERCASE.isEnabled());

        if (normIban == null) {
            return false;
        }

        len = normIban.length();
        if (len < MIN_IBAN_LENGTH || len > MAX_IBAN_LENGTH) {
            return false;
        }

        // country code: lookup ensures it consists of 2 uppercase letters
        IbanRegistry countryData = IbanRegistry.getByCode(normIban.charAt(0), normIban.charAt(1));

        return countryData != null
            && len == countryData.getIbanLength()
            // BBAN structure check (country-specific) and Mod 97
            && countryData.getCountryValidator().validateIban(normIban)
            && calculateMod97(normIban) == 1;
    }

    /**
     * Normalizes the input sequence by optionally removing spaces and converting
     * lowercase characters to uppercase.
     * <p>
     * This method follows a dual-path strategy: it returns the original input
     * immediately if it is already normalized (zero allocation). Otherwise,
     * it performs a single-pass transformation into a {@link CharArrayWrapper}.
     * <p>
     * If an invalid character is encountered (not a digit, not a letter, or
     * a space when {@code allowSpace} is {@code false}), the method returns
     * {@code null} immediately.
     *
     * @param input      the raw character sequence to normalize, may be {@code null}
     * @param inputLen   the length of the input sequence to process
     * @param allowSpace whether to ignore and strip space characters (' ')
     * @param allowLower whether to accept and convert lowercase ASCII characters (a-z)
     * @return a normalized character sequence, or {@code null} if an invalid character was found
     *
     * @since 1.8.5
     */
    static CharSequence normalize(final CharSequence input, final int inputLen,
                                  final boolean allowSpace, final boolean allowLower) {

        if (input == null || inputLen == 0 || isAllDigitOrUpperCase(input, 0, inputLen)) {
            // fast path: return input if no normalization is required to avoid allocations
            return input;
        }

        // transformation required: allocation is now unavoidable
        return normalizeImpl(input, inputLen, allowSpace, allowLower);
    }

    /**
     * Internal implementation for the transformation path of normalization.
     * <p>
     * Iterates through the input and builds a new character array while
     * applying case conversion and space filtering.
     *
     * @param input      the sequence to transform
     * @param len        the length of the sequence
     * @param allowSpace whether spaces are allowed (and thus ignored)
     * @param allowLower whether lowercase letters are allowed (and thus converted)
     * @return a new {@link CharArrayWrapper} containing the normalized data, or {@code null}
     *
     * @since 1.8.5
     */
    private static CharSequence normalizeImpl(final CharSequence input, final int len,
                                              final boolean allowSpace, final boolean allowLower) {
        final char[] arr = new char[len];
        int targetIdx = 0;

        for (int i = 0; i < len; i++) {
            final char c = input.charAt(i);

            if (isLowerCase(c)) {
                if (!allowLower) {
                    return null; // lowercase not permitted
                }
                arr[targetIdx++] = (char) (c - 32); // fast ascii uppercase conversion
            } else if (isDigitOrUpperCase(c)) {
                arr[targetIdx++] = c;
            } else if (c != ' ' || !allowSpace) {
                return null; // illegal character or disallowed space
            }
        }

        return new CharArrayWrapper(arr, 0, targetIdx);
    }

    /**
     * Validates the given IBAN using the default space configuration.
     * <p>
     * This is a convenience method that delegates to {@link #validate(CharSequence, boolean)}
     * using {@link IbanConfig#ALLOW_SPACE}.
     *
     * @param rawIban the IBAN character sequence to validate
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     * @see #validate(CharSequence, boolean)
     * @since 1.8.0
     */
    static IbanValidationSuccess validate(final CharSequence rawIban) {
        return validate(rawIban, IbanConfig.ALLOW_SPACE.isEnabled());
    }

    /**
     * Performs a full IBAN validation and returns the required data for IBAN object creation.
     * <p>
     * This is the preferred method for factory methods (like {@code Iban.of()})
     * as it aborts validation on failure and provides the normalized data on success.
     *
     * @param rawIban    the IBAN character sequence to validate, potentially containing spaces
     * @param allowSpace whether to allow spaces during validation
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     *
     * @since 1.8.5
     */
    static IbanValidationSuccess validate(final CharSequence rawIban, boolean allowSpace) {
        if (rawIban == null) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        int len = rawIban.length();
        if (len == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        // convert input to normalized char array based on library configuration
        CharSequence normIban = normalize(rawIban, len,
                                          allowSpace, IbanConfig.ALLOW_LOWERCASE.isEnabled());

        if (normIban == null) {
            return validationFailed(IbanValidationError.ILLEGAL_CHARACTERS);
        }

        len = normIban.length();
        // check min/max lengths
        if (len < MIN_IBAN_LENGTH || len > MAX_IBAN_LENGTH) {
            return len == 0
                ? validationFailed(IbanValidationError.EMPTY)
                : validationFailed(IbanValidationError.INCORRECT_LENGTH);
        }

        // country code: lookup ensures it consists of 2 uppercase letters
        IbanRegistry countryData = IbanRegistry.getByCode(normIban.charAt(0), normIban.charAt(1));

        if (countryData == null) {
            return validationFailed(IbanValidationError.INVALID_COUNTRY);
        }

        if (len != countryData.getIbanLength()) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH_COUNTRY);
        }

        if (isNotDigit(normIban.charAt(IbanRegistry.INDEX_CHECK_DIGIT1))
         || isNotDigit(normIban.charAt(IbanRegistry.INDEX_CHECK_DIGIT2))) {
            return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
        }

        // check BBAN structure (country-specific)
        CountryValidator countryValidator = countryData.getCountryValidator();
        if (countryValidator != null && !countryValidator.validateIban(normIban)) {
            return validationFailed(IbanValidationError.INVALID_STRUCTURE);
        }

        // check Mod 97 (most expensive operation — performed last)
        if (!isMod97Valid(normIban)) {
            return validationFailed(IbanValidationError.INVALID_CHECKSUM);
        }

        // success: reset thread-local error state and return carrier object
        LAST_REASON.remove();

        return new IbanValidationSuccess(normIban, countryData);
    }

    /**
     * Returns {@code true} if the ISO 7064 Mod 97-10 remainder of the given IBAN equals {@code 1}.
     * <p>
     * Delegates to {@link Mod97#isValid(CharSequence)}.
     *
     * @param iban the normalized IBAN {@link CharSequence} (uppercase, no spaces)
     * @return {@code true} if the checksum is valid
     */
    public static boolean isMod97Valid(final CharSequence iban) {
        return Mod97.isValid(iban);
    }

    /**
     * Calculates the ISO 7064 Mod 97-10 remainder for a normalized IBAN {@link CharSequence},
     * applying the standard rearrangement step (BBAN first, then the 4-character header).
     * <p>
     * Operates directly on the {@link CharSequence} — zero heap allocations for
     * {@link String} and {@link StringBuilder} inputs.
     * Delegates to {@link Mod97#calculate(CharSequence)}.
     *
     * @param iban the normalized IBAN {@link CharSequence} (uppercase, no spaces)
     * @return the remainder (0–96), or {@link #INVALID_MOD97} on invalid input
     */
    @SuppressWarnings("PMD.UselessParentheses")
    public static int calculateMod97(final CharSequence iban) {
        if (iban == null) {
            return INVALID_MOD97;
        }

        final int len = iban.length();

        if (len < MIN_IBAN_LENGTH || len > MAX_IBAN_LENGTH) {
            return INVALID_MOD97;
        }

        int result = Mod97.calculate(iban);
        return result == Mod97.INVALID_REMAINDER ? INVALID_MOD97 : result;
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
        final int checkDigitsValue = 98 - Mod97.calculate(sb);

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
