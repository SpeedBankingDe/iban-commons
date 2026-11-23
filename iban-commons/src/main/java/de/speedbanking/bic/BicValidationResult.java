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

/**
 * An immutable container that holds the result of a BIC validation attempt.
 * <p>
 * The object contains either the validated BIC (on success), or a
 * {@link BicValidationError} (on failure) - never both, never neither.
 * <p>
 * Intentionally avoids {@link java.util.Optional} to keep the internal validation
 * path allocation-free on the failure side.
 *
 * @since 1.8.0
 */
@SuppressWarnings("VisibilityModifier")
final class BicValidationResult {

    /** The validated, normalized BIC character sequence; {@code null} on failure. */
    final CharSequence       bic;

    /** The validation error; {@code null} on success. */
    final BicValidationError error;

    /**
     * Private constructor: enforce creation through static factory methods.
     */
    private BicValidationResult(CharSequence bic, BicValidationError error) {
        this.bic   = bic;
        this.error = error;
    }

    /**
     * Factory method for a successful validation result.
     *
     * @param bic the validated, normalized BIC (must not be {@code null})
     * @return a result carrying the validated BIC
     */
    static BicValidationResult valid(CharSequence bic) {
        return new BicValidationResult(bic, null);
    }

    /**
     * Factory method for a failed validation result.
     *
     * @param error the reason for the validation failure (must not be {@code null})
     * @return a result carrying the error reason
     */
    static BicValidationResult invalid(BicValidationError error) {
        return new BicValidationResult(null, error);
    }

    /**
     * Returns {@code true} if validation succeeded.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    boolean isValid() {
        return bic != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + '['
            + (isValid() ? "valid: " + bic : "invalid: " + error.getText())
            + ']';
    }

}
