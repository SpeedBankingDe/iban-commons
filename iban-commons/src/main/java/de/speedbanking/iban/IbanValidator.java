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
import static de.speedbanking.util.CharUtil.isDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isLowerCase;
import static de.speedbanking.util.CharUtil.isNotDigit;

import de.speedbanking.util.Mod97;

import java.util.Arrays;

/**
 * The core engine for **International Bank Account Number (IBAN)** validation.
 *
 * <h2>Validation pipeline</h2>
 * Every validation call follows a strict fail-fast sequence that aborts at the
 * first error detected:
 * <ol>
 *   <li><strong>Normalization</strong> — raw input is stripped of spaces and/or
 *       converted to uppercase into a thread-local buffer (no heap allocation).</li>
 *   <li><strong>Length check</strong> — overall length against ISO 13616 bounds.</li>
 *   <li><strong>Country lookup</strong> — two-letter code resolved via
 *       {@link IbanRegistry#getBaseEntryByCode(char, char)} using an O(1) array index.</li>
 *   <li><strong>Country length check</strong> — normalized length against the
 *       country-specific expected length.</li>
 *   <li><strong>BBAN structure check</strong> — country-specific digit/letter pattern
 *       via {@link CountryValidator#validateIban(char[])}.</li>
 *   <li><strong>ISO 7064 Mod 97-10</strong> — check digit verification, performed last
 *       as it is the most expensive step.</li>
 * </ol>
 *
 * <h2>Performance design</h2>
 * The hot validation path ({@link #isValid(CharSequence)}) is deliberately separated
 * from the diagnostic path ({@link #validate(CharSequence)}) to avoid {@link ThreadLocal}
 * writes and object allocations on the boolean fast-path.
 * <p>
 * Normalization writes directly into a pre-allocated {@link ThreadLocal} {@code char[]}
 * buffer ({@link #VALIDATION_BUFFER}), eliminating {@code char[]} allocations and GC
 * pressure even under high concurrency.
 * <p>
 * {@link String} inputs take a dedicated
 * {@link #normalize(String, int, char[], boolean, boolean)} overload that uses
 * {@link String#getChars(int, int, char[], int)} — a JVM intrinsic backed by
 * {@code System.arraycopy} — instead of per-character virtual {@code charAt()} dispatch.
 * The Java compiler resolves this overload statically via the {@link #isValid(String)}
 * and {@link #validate(String)} entry points, so callers passing {@link String} literals
 * or variables pay no runtime dispatch cost.
 * <p>
 * All internal loops in {@link CountryValidator} implementations and {@link Mod97} operate
 * directly on {@code char[]} arrays, keeping every hot callsite monomorphic and allowing
 * the JIT to inline and auto-vectorize aggressively.
 *
 * <h2>Thread safety</h2>
 * This class is thread-safe. The {@link #VALIDATION_BUFFER} and {@link #LAST_REASON}
 * thread-locals isolate mutable state per thread. All other fields are effectively immutable.
 *
 * @since 1.8.0
 */
public final class IbanValidator {

    /** Sentinel value indicating a normalization or validation error. */
    static final int                                      INVALID_INPUT     = -1;

    /**
     * Return value for a failed Mod 97-10 calculation.
     * <p>
     * Delegates to {@link Mod97#INVALID_REMAINDER} for a consistent sentinel across the API.
     */
    static final int                                      INVALID_MOD97     = Mod97.INVALID_REMAINDER;

    /**
     * Internal cache of all country-specific validators, indexed by the ordinal
     * of the {@link IbanRegistry} enumeration.
     * <p>
     * This field is thread-safe as the array and the contained {@link CountryValidator}
     * instances are effectively immutable.
     */
    private static final CountryValidator[]               VALIDATORS    =
        Arrays.stream(IbanRegistry.values())
            .map(IbanRegistry::name)
            .map(IbanValidator::loadCountryValidator)
            .toArray(CountryValidator[]::new);

    /**
     * Internal thread-local buffer used to perform IBAN normalization and validation
     * without heap allocations.
     * <p>
     * To maximize throughput in high-concurrency or batch-processing scenarios, this
     * buffer allows the validator to copy and transform raw input (e.g., stripping spaces
     * or upper-casing) within the same memory area. Using a {@link ThreadLocal} ensures
     * thread-safety while avoiding the overhead of frequent {@code char[]} allocations
     * and the resulting garbage collection pressure.
     * <p>
     * The capacity is set to {@code MAX_IBAN_LENGTH}, the longst possible unformatted IBAN.
     */
    @SuppressWarnings("java:S5164") // ThreadLocal used as a tiny persistent buffer to avoid GC pressure
    private static final ThreadLocal<char[]>              VALIDATION_BUFFER = ThreadLocal
        .withInitial(() -> new char[MAX_IBAN_LENGTH]);

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
     * Dynamically loads and instantiates the country-specific IBAN validator
     * using reflection based on the two-letter country code.
     * <p>
     * This method expects the validator implementation to be defined as a public
     * nested static class within the {@code CountryValidator} interface,
     * named after the country code (e.g., {@code CountryValidator.AD} for "AD").
     * <p>
     * <strong>Note:</strong> This method uses reflection and may fail silently
     * if the validator class is not found.
     *
     * @param countryCode the two-letter country code (e.g., "DE", "AD")
     * @return the instantiated {@link CountryValidator} for the given country,
     *         or {@code null} if the validator class cannot be found or instantiated
     *
     * @see CountryValidator
     * @see CountryValidators
     */
    static CountryValidator loadCountryValidator(final String countryCode) {
        String className = CountryValidators.class.getName() + '$' + countryCode;
        try {
            Class<?> cls = Class.forName(className);
            return (CountryValidator) cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not instantiate class '" + className + "': " + ex);
        }
    }

    @SuppressWarnings("EnumOrdinal")
    static CountryValidator getCountryValidator(IbanRegistry countryData) {
        return VALIDATORS[countryData.ordinal()];
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
     * @see IbanConfig#isAllowSpace()
     * @see IbanConfig#isAllowLowercase()
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

        // normalize to shared char[] once — all downstream calls use direct array access
        char[] normIban = VALIDATION_BUFFER.get();

        len = normalize(iban, len,
            normIban,
            IbanConfig.isAllowSpace(),
            IbanConfig.isAllowLowercase());

        if (len < MIN_IBAN_LENGTH) { // MIN_IBAN_LENGTH includes check for invalid input
            return false;
        }

        // country code: lookup ensures it consists of 2 uppercase letters
        IbanRegistry countryData = IbanRegistry.getBaseEntryByCode(normIban[0], normIban[1]);

        return countryData != null
            && len == countryData.getIbanLength()
            // BBAN structure check (country-specific) — wrap once so CountryValidators see a monomorphic type
            && getCountryValidator(countryData).validateIban(normIban)
            && Mod97.isValid(normIban, len);
    }

    /**
     * {@link String}-optimized overload of {@link #isValid(CharSequence)}.
     * <p>
     * Resolved statically by the Java compiler when the caller passes a {@link String},
     * routing normalization through {@link #normalize(String, int, char[], boolean, boolean)},
     * which uses {@link String#getChars(int, int, char[], int)} — a JVM intrinsic backed
     * by {@code System.arraycopy} — instead of per-character virtual {@code charAt()} dispatch.
     *
     * @param iban the IBAN string to validate (may be unnormalized
     *             depending on {@link IbanConfig} settings)
     * @return {@code true} if the IBAN is valid according to all criteria, {@code false} otherwise
     * @see #isValid(CharSequence)
     * @since 1.8.5
     */
    public static boolean isValid(final String iban) {
        if (iban == null) {
            return false;
        }

        int len = iban.length();
        if (len < MIN_IBAN_LENGTH) {
            return false;
        }

        char[] normIban = VALIDATION_BUFFER.get();

        len = normalize(iban, len,
            normIban,
            IbanConfig.isAllowSpace(),
            IbanConfig.isAllowLowercase());

        if (len < MIN_IBAN_LENGTH || len > MAX_IBAN_LENGTH) {
            return false;
        }

        IbanRegistry countryData = IbanRegistry.getBaseEntryByCode(normIban[0], normIban[1]);

        return countryData != null
            && len == countryData.getIbanLength()
            && getCountryValidator(countryData).validateIban(normIban)
            && Mod97.isValid(normIban, len);
    }

    /**
     * Normalizes a {@link CharSequence} into the provided output buffer.
     * <p>
     * Performs two optional transformations: stripping space characters and converting
     * lowercase ASCII letters to uppercase. By writing directly into a pre-allocated
     * buffer, this method avoids heap allocations in the validation hot-path.
     * <p>
     * Characters are read via {@code charAt()} — virtual dispatch whose cost depends on
     * the concrete type at the callsite. Prefer the {@link String} overload
     * ({@link #normalize(String, int, char[], boolean, boolean)}) wherever possible.
     *
     * @param input      the raw character sequence to normalize; must not be {@code null}
     * @param inputLen   the number of characters to process from the input
     * @param output     the destination array; must hold at least {@code inputLen} characters
     * @param allowSpace if {@code true}, space characters are silently omitted from the output
     * @param allowLower if {@code true}, lowercase ASCII {@code 'a'–'z'} are converted to uppercase
     * @return the number of characters written to {@code outputBuffer},
     *         or {@value #INVALID_INPUT} if an illegal character was encountered
     *
     * @since 1.8.5
     */
    static int normalize(final CharSequence input, final int inputLen,
        final char[] output,
        final boolean allowSpace, final boolean allowLower) {

        if (!allowSpace && !allowLower) {
            if (inputLen > MAX_IBAN_LENGTH) {
                return INVALID_INPUT;
            }
            for (int i = 0; i < inputLen; i++) {
                output[i] = input.charAt(i);
                if (!isDigitOrUpperCase(output[i])) {
                    return INVALID_INPUT;
                }
            }
            return inputLen;
        }

        int targetIdx = 0;
        for (int i = 0; i < inputLen; i++) {
            char c = input.charAt(i);
            if (isDigitOrUpperCase(c)) {
                output[targetIdx++] = c;
            } else if (isLowerCase(c)) {
                if (!allowLower) {
                    return INVALID_INPUT;
                }
                output[targetIdx++] = (char) (c - 32);
            } else if (c != ' ' || !allowSpace) {
                return INVALID_INPUT;
            }
            if (targetIdx >= MAX_IBAN_LENGTH) {
                return INVALID_INPUT;
            }
        }
        return targetIdx;
    }

    /**
     * {@link String}-optimized overload of {@link #normalize(CharSequence, int, char[], boolean, boolean)}.
     * <p>
     * Uses {@link String#getChars(int, int, char[], int)} to bulk-copy the entire input into
     * {@code outputBuffer} in a single JVM-intrinsic call (backed by {@code System.arraycopy}),
     * replacing {@code inputLen} virtual {@code charAt()} dispatches with one native memory copy.
     * The buffer is then validated — and transformed if necessary — entirely via direct array access.
     * <p>
     * In the transformation path ({@code allowSpace} or {@code allowLower}), in-place editing
     * is safe because the write index {@code targetIdx} is always {@code ≤ i}, so no unread
     * position is ever overwritten.
     *
     * @param input      the raw {@link String} to normalize; must not be {@code null}
     * @param inputLen   the number of characters to process from the input
     * @param output     the destination array; must hold at least {@code inputLen} characters
     * @param allowSpace if {@code true}, space characters are silently omitted from the output
     * @param allowLower if {@code true}, lowercase ASCII {@code 'a'–'z'} are converted to uppercase
     * @return the number of characters written to {@code outputBuffer},
     *         or {@value #INVALID_INPUT} if an illegal character was encountered
     *
     * @since 1.8.5
     */
    static int normalize(final String input, final int inputLen,
        final char[] output,
        final boolean allowSpace, final boolean allowLower) {

        if (!allowSpace && !allowLower) {
            if (inputLen > MAX_IBAN_LENGTH) {
                return INVALID_INPUT;
            }

            // Fast path: one combined pass — validate and copy simultaneously.
            // The JIT inlines String.charAt() to direct char[] array access,
            // and early exit on the first invalid character avoids processing
            // the rest of the input (critical for invalid-IBAN throughput).
            for (int i = 0; i < inputLen; i++) {
                output[i] = input.charAt(i);
                if (!isDigitOrUpperCase(output[i])) {
                    return INVALID_INPUT;
                }
            }
            return inputLen;
        }

        // Transformation path: spaces stripped and/or lowercase converted.
        // getChars() uses System.arraycopy (SIMD intrinsic) to bulk-copy the
        // entire input first; in-place editing is then safe because targetIdx <= i
        // always holds, so no unread position is ever overwritten.
        int targetIdx = 0;
        for (int i = 0; i < inputLen; i++) {
            char c = input.charAt(i);
            if (isDigitOrUpperCase(c)) {
                output[targetIdx++] = c;
            } else if (isLowerCase(c)) {
                if (!allowLower) {
                    return INVALID_INPUT;
                }
                output[targetIdx++] = (char) (c - 32);
            } else if (c != ' ' || !allowSpace) {
                return INVALID_INPUT;
            }
            if (targetIdx >= MAX_IBAN_LENGTH) {
                return INVALID_INPUT;
            }
        }
        return targetIdx;
    }

    /**
     * Validates the given IBAN using the default space configuration.
     * <p>
     * This is a convenience method that delegates to {@link #validate(CharSequence, boolean)}
     * using {@link IbanConfig#isAllowSpace()}.
     *
     * @param rawIban the IBAN character sequence to validate
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     * @see #validate(CharSequence, boolean)
     * @since 1.8.0
     */
    static IbanValidationSuccess validate(final CharSequence rawIban) {
        return validate(rawIban, IbanConfig.isAllowSpace());
    }

    /**
     * Performs a full IBAN validation and returns the required data for IBAN object creation.
     * <p>
     * This is the preferred method for factory methods (like {@code Iban.of()})
     * as it aborts validation on failure and provides the normalized data on success.
     * <p>
     * In case of failure, the specific reason is stored in a {@link ThreadLocal}
     * and can be retrieved via {@link #getLastReason()}.
     *
     * @param rawIban    the IBAN character sequence to validate, potentially containing spaces
     * @param allowSpace whether to allow spaces during validation
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     * @see #getLastReason()
     *
     * @since 1.8.5
     */
    static IbanValidationSuccess validate(final CharSequence rawIban, boolean allowSpace) {
        if (rawIban == null) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        int rawLen = rawIban.length();
        if (rawLen == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        // normalize to char[] once — all downstream calls use direct array access
        char[] normIban = VALIDATION_BUFFER.get();

        int normLen = normalize(rawIban, rawLen,
            normIban,
            allowSpace, IbanConfig.isAllowLowercase());

        if (normLen == INVALID_INPUT) {
            return validationFailed(IbanValidationError.ILLEGAL_CHARACTERS);
        }

        // check min/max lengths
        if (normLen < MIN_IBAN_LENGTH || normLen > MAX_IBAN_LENGTH) {
            return normLen == 0
                ? validationFailed(IbanValidationError.EMPTY)
                : validationFailed(IbanValidationError.INCORRECT_LENGTH);
        }

        // country code: lookup ensures it consists of 2 uppercase letters
        IbanRegistry countryData = IbanRegistry.getBaseEntryByCode(normIban[0], normIban[1]);

        if (countryData == null) {
            return validationFailed(IbanValidationError.INVALID_COUNTRY);
        }

        if (normLen != countryData.getIbanLength()) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH_COUNTRY);
        }

        if (isNotDigit(normIban[IbanRegistry.INDEX_CHECK_DIGIT1])
         || isNotDigit(normIban[IbanRegistry.INDEX_CHECK_DIGIT2])) {
            return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
        }

        // check BBAN structure (country-specific)
        CountryValidator countryValidator = getCountryValidator(countryData);
        if (countryValidator != null && !countryValidator.validateIban(normIban)) {
            return validationFailed(IbanValidationError.INVALID_STRUCTURE);
        }

        // check Mod 97 (most expensive operation — performed last)
        if (!Mod97.isValid(normIban, normLen)) {
            return validationFailed(IbanValidationError.INVALID_CHECKSUM);
        }

        // success: reset thread-local error state and return carrier object
        LAST_REASON.remove();

        /*
         * Create the success result.
         * Reuse the original input only if it was already normalized
         * (matching length and no lowercase conversion).
         * Otherwise, MUST create a stable copy from the transient validation buffer.
         */
        return new IbanValidationSuccess(
            !allowSpace && !IbanConfig.isAllowLowercase()
                ? rawIban
                : String.valueOf(normIban, 0, normLen),
            countryData);
    }

    /**
     * {@link String}-optimized overload of {@link #validate(CharSequence)}.
     * <p>
     * Resolved statically by the Java compiler when the caller passes a {@link String},
     * routing normalization through {@link #normalize(String, int, char[], boolean, boolean)}.
     *
     * @param rawIban the IBAN string to validate
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     * @see #validate(CharSequence)
     * @since 1.8.5
     */
    static IbanValidationSuccess validate(final String rawIban) {
        return validate(rawIban, IbanConfig.isAllowSpace());
    }

    /**
     * {@link String}-optimized overload of {@link #validate(CharSequence, boolean)}.
     * <p>
     * Resolved statically by the Java compiler when the caller passes a {@link String},
     * routing normalization through {@link #normalize(String, int, char[], boolean, boolean)}.
     *
     * @param rawIban    the IBAN string to validate, potentially containing spaces
     * @param allowSpace whether to allow spaces during validation
     * @return the {@link IbanValidationSuccess} data if valid, or {@code null} if validation failed
     * @see #validate(CharSequence, boolean)
     * @since 1.8.5
     */
    static IbanValidationSuccess validate(final String rawIban, final boolean allowSpace) {
        if (rawIban == null) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        int rawLen = rawIban.length();
        if (rawLen == 0) {
            return validationFailed(IbanValidationError.EMPTY);
        }

        char[] normIban = VALIDATION_BUFFER.get();

        int normLen = normalize(rawIban, rawLen,
            normIban,
            allowSpace, IbanConfig.isAllowLowercase());

        if (normLen == INVALID_INPUT) {
            return validationFailed(IbanValidationError.ILLEGAL_CHARACTERS);
        }

        if (normLen < MIN_IBAN_LENGTH || normLen > MAX_IBAN_LENGTH) {
            return normLen == 0
                ? validationFailed(IbanValidationError.EMPTY)
                : validationFailed(IbanValidationError.INCORRECT_LENGTH);
        }

        IbanRegistry countryData = IbanRegistry.getBaseEntryByCode(normIban[0], normIban[1]);

        if (countryData == null) {
            return validationFailed(IbanValidationError.INVALID_COUNTRY);
        }

        if (normLen != countryData.getIbanLength()) {
            return validationFailed(IbanValidationError.INCORRECT_LENGTH_COUNTRY);
        }

        if (isNotDigit(normIban[IbanRegistry.INDEX_CHECK_DIGIT1])
         || isNotDigit(normIban[IbanRegistry.INDEX_CHECK_DIGIT2])) {
            return validationFailed(IbanValidationError.INVALID_CHECK_DIGITS);
        }

        CountryValidator countryValidator = getCountryValidator(countryData);
        if (countryValidator != null && !countryValidator.validateIban(normIban)) {
            return validationFailed(IbanValidationError.INVALID_STRUCTURE);
        }

        if (!Mod97.isValid(normIban, normLen)) {
            return validationFailed(IbanValidationError.INVALID_CHECKSUM);
        }

        LAST_REASON.remove();

        /*
         * Create the success result.
         * Reuse the original String only if no transformation was applied
         * (no space stripping, no lowercase conversion).
         * Otherwise, MUST create a stable copy from the transient validation buffer.
         */
        return new IbanValidationSuccess(
            !allowSpace && !IbanConfig.isAllowLowercase()
                ? rawIban
                : String.valueOf(normIban, 0, normLen),
            countryData);
    }

    /**
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
