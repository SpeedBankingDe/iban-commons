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

/**
 * An immutable container that holds the result of an IBAN validation attempt.
 * <p>
 * The object contains either the normalized IBAN and its country data (on success),
 * or an {@link IbanValidationError} (on failure) — never both, never neither.
 * <p>
 * Intentionally avoids {@link java.util.Optional} to keep the internal validation
 * path allocation-free on the failure side. Failure results without country context
 * are pre-allocated at class-load time (one singleton per {@link IbanValidationError} constant)
 * so that {@link #invalid(IbanValidationError)} never allocates on the hot validation path.
 *
 * @since 1.8.6 (previously called IbanValidationSuccess)
 */
@SuppressWarnings("VisibilityModifier")
final class IbanValidationResult {

    /**
     * Pre-allocated failure results, indexed by {@link IbanValidationError#ordinal()}.
     * Populated once at class-load time; never modified afterwards.
     */
    private static final IbanValidationResult[] INVALID_RESULTS;

    static {
        IbanValidationError[] errors = IbanValidationError.values();
        INVALID_RESULTS = new IbanValidationResult[errors.length];
        for (int i = 0; i < errors.length; i++) {
            INVALID_RESULTS[i] = new IbanValidationResult(null, null, errors[i]);
        }
    }

    /** The normalized IBAN string; {@code null} on failure. */
    final CharSequence        normIban;

    /** The country metadata; {@code null} on failure if country unknown. */
    final IbanRegistry        countryData;

    /** The validation error; {@code null} on success. */
    final IbanValidationError error;

    /**
     * Constructs a new {@code IbanValidationResult} instance.<br>
     * Enforces creation through static factory methods.
     *
     * @param normIban    the normalized IBAN, or {@code null} if invalid
     * @param countryData the country registry metadata, or {@code null} if unknown or omitted
     * @param error       the validation error, or {@code null} if valid
     */
    private IbanValidationResult(CharSequence normIban, IbanRegistry countryData, IbanValidationError error) {
        this.normIban    = normIban;
        this.countryData = countryData;
        this.error       = error;
    }

    /**
     * Factory method for a successful validation result.
     *
     * @param normIban    the normalized, validated IBAN characters (must not be {@code null})
     * @param countryData the metadata for the country code (must not be {@code null})
     * @return a result carrying the IBAN and its country data
     */
    static IbanValidationResult valid(CharSequence normIban, IbanRegistry countryData) {
        return new IbanValidationResult(normIban, countryData, null);
    }

    /**
     * Factory method for a failed validation result without country context.
     * <p>
     * Returns a pre-allocated singleton for the given error; never allocates.
     *
     * @param error the reason for the validation failure (must not be {@code null})
     * @return the cached result for the given error constant
     */
    @SuppressWarnings("EnumOrdinal")
    static IbanValidationResult invalid(IbanValidationError error) {
        return INVALID_RESULTS[error.ordinal()];
    }

    /**
     * Factory method for a failed validation result associated with specific country metadata.
     * <p>
     * Allocates a new instance because country metadata is attached to the failure context.
     *
     * @param error       the reason for the validation failure (must not be {@code null})
     * @param countryData the metadata for the country code, if determined (may be {@code null})
     * @return a result carrying the error and its associated country data
     */
    static IbanValidationResult invalid(IbanValidationError error, IbanRegistry countryData) {
        return new IbanValidationResult(null, countryData, error);
    }

    /**
     * Returns {@code true} if validation succeeded.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    boolean isValid() {
        return normIban != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + '['
            + (isValid() ? "valid: " + normIban : "invalid: " + error.getText())
            + ']';
    }

}
