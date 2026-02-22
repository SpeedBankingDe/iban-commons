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
import static de.speedbanking.iban.IbanRegistry.INDEX_CHECK_DIGITS;
import static de.speedbanking.iban.IbanRegistry.MIN_IBAN_LENGTH;
import static de.speedbanking.util.CharUtil.isDigit;
import static de.speedbanking.util.CharUtil.isDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isUpperCase;

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
    private static final int                              MOD         = 97;

    /**
     * A limit used to trigger the intermediate modulo operation during the
     * Mod 97-10 calculation to prevent {@code long} overflow, set to 999999999.
     */
    private static final long                             MAX         = 999999999;

    /**
     * Simple thread-local holder for the last failure reason for the {@link Iban#of(CharSequence)} simplicity.
     * <p>
     * Ensures that the reason for failure is correctly associated with the calling thread
     * when using a simplified API that doesn't return the full result object.
     */
    private static final ThreadLocal<IbanValidationError> LAST_REASON = new ThreadLocal<>();

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

        final int len = iban.length();

        if (len < MIN_IBAN_LENGTH) {
            return false;
        }

        IbanRegistry countryData = IbanRegistry.getByCode(iban.subSequence(0, 2));

        return countryData != null
            && len == countryData.getIbanLength()
            && countryData.getIbanRegex().matcher(iban).matches()
            && isMod97Valid(iban);
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

        // the first two non-space characters (should be country code)
        char c1 = 0;
        char c2;

        IbanRegistry countryData = null;

        // 1. Normalization and Character Set Check (combined loop)

        for (int i = 0; i < len; i++) {
            char c = rawIban.charAt(i);

            if (c == ' ') {
                continue; // skip space
            }

            normLen++;

            if (normLen <= 2) {

                // two uppercase country code characters

                // check country code
                if (!isUpperCase(c)) {
                    return validationFailed(IbanValidationError.INVALID_COUNTRY);
                }

                if (normLen == 1) {

                    c1 = c;

                } else if (normLen == 2) {

                    c2 = c;

                    // check registry
                    countryData = IbanRegistry.getByCode(c1, c2);

                    if (countryData == null) {
                        return validationFailed(IbanValidationError.UNSUPPORTED_COUNTRY);
                    }

                    // initialize the array
                    normIbanArr = new char[countryData.getIbanLength()];
                    normIbanArr[0] = c1;
                    normIbanArr[1] = c2;
                }

                continue;

            } else if (normLen <= 4) {

                // check two check digit characters
                if (!isDigit(c)) {
                    return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
                }

            } else if (!isDigitOrUpperCase(c)) {

                // check IBANs must only contain uppercase ASCII letters (A-Z) and digits (0-9)
                return validationFailed(IbanValidationError.ILLEGAL_CHARACTERS);

            }

            if (normLen > normIbanArr.length) {
                return validationFailed(IbanValidationError.INCORRECT_LENGTH_COUNTRY);
            }

            normIbanArr[normLen - 1] = c;

        }

        // check general length checks
        if (normLen == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        // check minimum length
        if (normLen < MIN_IBAN_LENGTH) {
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

        final int len = normalizedIban.length();

        if (len == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        } else if (len < MIN_IBAN_LENGTH) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH);
        }

        // check country code and registry (first 2 chars)
        char c1 = normalizedIban.charAt(0);
        char c2 = normalizedIban.charAt(1);
        if (!isUpperCase(c1) || !isUpperCase(c2)) {
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

        // check two check digit characters
        char c3 = normalizedIban.charAt(2);
        char c4 = normalizedIban.charAt(3);
        if (!isDigit(c3) || !isDigit(c4)) {
            return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
        }

        char[] normIbanArr = new char[len];
        normIbanArr[0] = c1;
        normIbanArr[1] = c2;
        normIbanArr[2] = c3;
        normIbanArr[3] = c4;

        // character set check for the rest
        // BBAN part (from index 4 onwards) must be digits or uppercase
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
     * Final validation steps (BBAN structure and Mod 97).
     *
     * @param normIbanArr the normalized IBAN as a char array
     * @param countryData the {@link IbanRegistry} data
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     */
    static IbanValidationSuccess validateCommon(final char[] normIbanArr, final IbanRegistry countryData) {

        // check bban structure
        CountryValidator countryValidator = countryData.getCountryValidator();
        if (countryValidator != null && !countryValidator.validateIban(normIbanArr)) {
            return validationFailed(IbanValidationError.INVALID_STRUCTURE);
        }

        // check mod 97 (the most expensive operation)
        if (!isMod97Valid(normIbanArr)) {
            return validationFailed(IbanValidationError.INVALID_CHECKSUM);
        }

        // if successful, return the required data for object creation and reset the last error
        LAST_REASON.remove();

        return new IbanValidationSuccess(normIbanArr, countryData);
    }

    /**
     * Implementation of the highly optimized ISO 7064 Mod 97-10 check.
     * <p>
     * Checks the validity of the IBAN check digit using an optimized Modulo 97 calculation strategy.
     * It processes the BBAN part (indices 4 to end) and the rotated part (indices 0 to 3)
     * in a single loop using the modulo rotation technique.
     *
     * @param iban the IBAN char array, assumed to be already normalized (uppercase, no spaces)
     * @return {@code true} if the Modulo 97 remainder is {@code 1}, {@code false} otherwise
     */
    static boolean isMod97Valid(final char[] iban) {
        if (iban == null) {
            return false;
        }

        // length of the normalized IBAN array
        final int len = iban.length;

        if (len < 1) {
            return false;
        }

        long total = 0;
        int value; // used for the ISO 7064 value (0-35)

        // The point where the rotation starts: index 4 (start of BBAN part)
        // This ensures the BBAN part is processed first (indices 4, 5, ..., len-1),
        // followed by the rotated part (indices 0, 1, 2, 3).
        final int rotationStart = INDEX_BBAN;

        // single loop iterating over the full length of the IBAN.
        for (int i = 0; i < len; i++) {

            // calculate the rotated index:
            // the result ensures the processing order is: 4, 5, ..., len-1, then 0, 1, 2, 3.
            int idx = (i + rotationStart) % len;

            char c = iban[idx];

            // 1. character-to-value conversion (0-9 or 10-35)
            if (isDigit(c)) {
                value = c - '0';
            } else if (isUpperCase(c)) {
                value = c - 'A' + 10;
            } else {
                // failsafe check: Should not happen if the input was truly normalized
                return false;
            }

            // 2. incremental Modulo-97 calculation
            // total = (total * 10^n + value) where n is 2 for letters, 1 for digits
            total = (value > 9 ? total * 100 : total * 10) + value;

            // 3. modulo block check (crucial to prevent long overflow)
            if (total > MAX) { // MAX = 999999999
                total = (total % MOD); // MOD = 97
            }
        }

        // 4. final modulo check: the IBAN is valid if the remainder is 1 (ISO 7064 Mod 97-10 standard)
        return 1 == (total % MOD);
    }

    /**
     * Checks if the given normalized IBAN (International Bank Account Number)
     * passes the ISO 7064 Mod 97-10 check.
     * <p>
     * The input must be a normalized IBAN consisting only of digits (0-9)
     * and uppercase letters (A-Z).
     *
     * @param iban the normalized IBAN as a CharSequence (e.g., {@link String} or {@link StringBuilder})
     * @return {@code true} if the IBAN is valid according to Mod 97, otherwise {@code false}
     */
    static boolean isMod97Valid(final CharSequence iban) {

        long total = 0;
        int value; // used for the ISO 7064 value (0-35)

        // length of the normalized IBAN CharSequence
        final int len = iban.length();

        // The point where the rotation starts: index 4 (start of BBAN part)
        // The constant INDEX_BBAN must be available here.
        final int rotationStart = INDEX_BBAN;

        // single loop iterating over the full length of the IBAN.
        for (int i = 0; i < len; i++) {

            // calculate the rotated index:
            // the result ensures the processing order is: 4, 5, ..., len-1, then 0, 1, 2, 3.
            int idx = (i + rotationStart) % len;

            // Use charAt(idx) instead of array access iban[idx]
            char c = iban.charAt(idx);

            // 1. character-to-value conversion (0-9 or 10-35)
            if (isDigit(c)) {
                value = c - '0';
            } else if (isUpperCase(c)) {
                value = c - 'A' + 10;
            } else {
                // failsafe check: Should not happen if the input was truly normalized
                return false;
            }

            // 2. incremental Modulo-97 calculation
            // total = (total * 10^n + value) where n is 2 for letters, 1 for digits
            // Note: Characters 'A'-'Z' map to values 10-35, so value > 9 for letters.
            total = (value > 9 ? total * 100 : total * 10) + value;

            // 3. modulo block check (crucial to prevent long overflow)
            // Using MAX = 999999999 ensures 'total' never requires more than 10 digits
            // and keeps the intermediate result high enough for precision.
            if (total > MAX) {
                total = (total % MOD); // MOD = 97
            }
        }

        // 4. final modulo check: the IBAN is valid if the remainder is 1
        return 1 == (total % MOD);
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
     * Finalizes an invalid validation result by storing the reason and returning {@code null}.
     *
     * @param reason the reason for the validation failure
     * @return always {@code null}
     */
    static IbanValidationSuccess validationFailed(final IbanValidationError reason) {
        // store the single error for the simple API to retrieve via getLastReason()
        setLastReason(reason);
        return null;
    }

    /**
     * Performs the core ISO 7064 Mod 97-10 calculation on the restructured IBAN string.
     * <p>
     * The input {@code CharSequence} is rotated by moving the first 4 characters (CC + CD) to the end.
     * Characters are converted to numerical values (A=10, B=11, ..., Z=35).
     * The result is the remainder of the overall number when divided by 97.
     * Intermediate modulo operations are performed to prevent {@code long} overflow.
     *
     * @param cs the full IBAN string (normalized, with placeholder check digits)
     * @return the Mod 97 remainder (R) of the restructured IBAN value
     * @throws InvalidIbanException if the input contains non-alphanumeric characters (outside A-Z, 0-9)
     */
    static int calculateMod97(CharSequence cs) {
        StringBuilder ibanBuilder = new StringBuilder()
            .append(cs.subSequence(INDEX_BBAN, cs.length()))
            .append(cs.subSequence(0, INDEX_BBAN));

        long total = 0;
        for (int i = 0; i < ibanBuilder.length(); i++) {
            final int numericValue = Character.getNumericValue(ibanBuilder.charAt(i));
            if (numericValue < 0 || numericValue > 35) {
                throw new InvalidIbanException(IbanValidationError.ILLEGAL_CHARACTERS);
            }
            total = (numericValue > 9 ? total * 100 : total * 10) + numericValue;

            if (total > MAX) {
                total = (total % MOD);
            }
        }
        return (int) (total % MOD);
    }

    /**
     * Calculates the correct ISO 7064 Mod 97-10 check digits for the given IBAN string
     * and overwrites the placeholders (usually "00") at the check digit positions (index 2 and 3).
     * <p>
     * This method temporarily sets the check digits to "00" to calculate the required remainder $R$,
     * then determines the final check digits $CD = 98 - R$.
     *
     * @param iban the IBAN character sequence (must already be of the full IBAN length,
     *             with placeholders at the check digit position); if a {@code StringBuilder}
     *             is passed, it is mutated in place, otherwise a copy is created
     * @return the same {@code StringBuilder} instance with the correct check digits applied
     */
    public static StringBuilder fixCheckDigits(CharSequence iban) {
        StringBuilder sb = (iban instanceof StringBuilder)
            ? (StringBuilder) iban
            : new StringBuilder(iban);

        // 1. Set placeholders to "00" (crucial for correct calculation context)
        sb.setCharAt(INDEX_CHECK_DIGITS, '0');
        sb.setCharAt(INDEX_CHECK_DIGITS + 1, '0');

        // 2. Calculate the required check digits value (98 - Modulo result)
        int checkDigitsValue = 98 - calculateMod97(sb);

        // 3. Format the result to a zero-padded 2-digit String, e.g. 5 -> "05", 91 -> "91"
        String checkDigitsStr = String.format("%02d", checkDigitsValue);

        // 4. Overwrite the placeholders with the calculated digits
        sb.setCharAt(INDEX_CHECK_DIGITS, checkDigitsStr.charAt(0));
        sb.setCharAt(INDEX_CHECK_DIGITS + 1, checkDigitsStr.charAt(1));

        return sb;
    }

}
