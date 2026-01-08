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

import java.util.Objects;

/**
 * Exception thrown when a string fails BIC validation in the strict {@link Bic#of(CharSequence)} method.
 *
 * @since 1.8.0
 */
public class InvalidBicException extends RuntimeException {

    /** Serial version UID. */
    private static final long        serialVersionUID = 42L;

    /** The specific reason why BIC validation failed. */
    private final BicValidationError reason;

    /**
     * Constructs a new exception with the specified validation failure reason.
     * The exception message is derived from the reason's text.
     *
     * @param reason the specific {@code BicValidationError} that occurred, must not be {@code null}
     */
    InvalidBicException(BicValidationError reason) {
        super(Objects.requireNonNull(reason, "reason required").getText());
        this.reason = reason;
    }

    /**
     * Static factory method to create an {@code InvalidBicException} instance.
     *
     * @param reason the specific {@code BicValidationError} that occurred
     * @return a new {@code InvalidBicException} instance
     */
    public static InvalidBicException of(BicValidationError reason) {
        return new InvalidBicException(reason);
    }

    /**
     * Returns the specific reason for the validation failure.
     *
     * @return the validation error reason
     */
    public BicValidationError getReason() {
        return reason;
    }

    /**
     * {@inheritDoc}
     * <p>
     * More specifically, returns a string representation including the class name, the localized message,
     * and the reason if available.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        String str = getClass().getSimpleName();
        String message = getLocalizedMessage();
        if (message != null) {
            str += ": " + message;
        }
        if (reason != null) {
            str += " (" + reason + ")";
        }
        return str;
    }

}

