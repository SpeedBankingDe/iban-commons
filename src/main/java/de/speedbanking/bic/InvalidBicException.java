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

import de.speedbanking.iban.util.InvalidBaseException;

/**
 * Exception thrown when a string fails BIC validation in the strict {@link Bic#of(CharSequence)} method.
 * <p>
 * Instances are typically created via the static factory methods {@link #of(BicValidationError)}
 * or {@link #of(BicValidationError, CharSequence)} rather than directly via constructors.
 *
 * @since 1.8.0
 */
public class InvalidBicException extends InvalidBaseException {

    /** Serial version UID. */
    private static final long serialVersionUID = 42L;

    /**
     * Constructs a new exception with the specified validation failure reason and the erroneous input.
     * The exception message is derived from the reason's text.
     *
     * @param reason the specific {@code BicValidationError} that occurred, must not be {@code null}
     * @param input  the BIC input string that caused the error, may be {@code null};
     *               blank values are normalized to {@code null}
     */
    InvalidBicException(BicValidationError reason, CharSequence input) {
        super(reason, input);
    }

    /**
     * Static factory method to create an {@code InvalidBicException} with the specified reason.
     *
     * @param reason the specific {@code BicValidationError} that occurred, must not be {@code null}
     * @return a new {@code InvalidBicException} instance
     */
    public static InvalidBicException of(BicValidationError reason) {
        return new InvalidBicException(reason, null);
    }

    /**
     * Static factory method to create an {@code InvalidBicException} with the specified reason
     * and the erroneous BIC input.
     *
     * @param reason the specific {@code BicValidationError} that occurred, must not be {@code null}
     * @param input  the BIC input string that caused the error, may be {@code null};
     *               blank values are normalized to {@code null}
     * @return a new {@code InvalidBicException} instance
     */
    public static InvalidBicException of(BicValidationError reason, CharSequence input) {
        return new InvalidBicException(reason, input);
    }

    /**
     * Returns the specific reason for the BIC validation failure.
     *
     * @return the validation error reason, never {@code null}
     */
    @Override
    public BicValidationError getReason() {
        return (BicValidationError) super.getReason();
    }

}

