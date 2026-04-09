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

import java.util.Optional;

/**
 * An immutable container that holds the result of an BIC validation attempt.
 * <p>
 * The object contains either a valid BIC or a {@link BicValidationError}, never both.
 *
 * @since 1.8.0
 */
public final class BicValidationResult {

    private final CharSequence       bic;
    private final BicValidationError validationError;

    /**
     * Private constructor: enforce creation through static factory methods
     *
     * @param bic             the successfully validated BIC (must not be {@code null})
     * @param validationError the single reason for validation failure (must not be {@code null})
     */
    private BicValidationResult(CharSequence bic, BicValidationError validationError) {
        // sanity check to ensure the "either-or" invariant is maintained
        if (bic == null && validationError == null) {
            throw new IllegalArgumentException("BIC result must contain either a valid BIC or a validation error");
        } else if (bic != null && validationError != null) {
            throw new IllegalArgumentException("BIC result cannot contain both a valid BIC and a validation error");
        }

        this.bic = bic;
        this.validationError = validationError;
    }

    /**
     * Factory method for a successful validation.
     *
     * @param bic the successfully validated BIC (must not be {@code null})
     * @return a valid result has the BIC present and no error reason
     */
    static BicValidationResult valid(CharSequence bic) {
        if (bic == null) {
            throw new IllegalArgumentException("Valid result requires a BIC");
        }
        // valid result: BIC present, validationError is null
        return new BicValidationResult(bic, null);
    }

    /**
     * Factory method for a validation error.
     *
     * @param validationError the single reason for validation failure (must not be {@code null})
     * @return an invalid result has no BIC and the specific error reason present
     */
    static BicValidationResult invalid(BicValidationError validationError) {
        if (validationError == null) {
            throw new IllegalArgumentException("Invalid result requires a validation error object");
        }
        return new BicValidationResult(null, validationError);
    }

    /**
     * Returns true if the validation was successful.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean isValid() {
        return bic != null;
    }

    /**
     * Returns a validated BIC if available.
     *
     * @return an {@code Optional} containing the BIC, or empty if validation failed
     */
    public Optional<CharSequence> getBic() {
        return Optional.ofNullable(bic);
    }

    /**
     * Returns the single reason why validation failed.
     *
     * @return an {@code Optional} containing the {@link BicValidationError}, or empty if validation was successful
     */
    public Optional<BicValidationError> getError() {
        return Optional.ofNullable(validationError);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + '['
            + (bic == null ? "invalid: " + validationError.getText() : "valid: " + bic)
            + ']';
    }

}
