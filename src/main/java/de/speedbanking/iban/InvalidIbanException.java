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

import java.util.Objects;

/**
 * Exception thrown when a string fails IBAN validation in the strict Iban.of() method.
 *
 * @since 1.8.0
 */
public class InvalidIbanException extends RuntimeException {

    /** Serial version UID. */
    private static final long         serialVersionUID = 42L;

    /** The specific reason why IBAN validation failed. */
    private final IbanValidationError reason;

    /**
     * Constructs a new exception with the specified validation failure reason.
     * The exception message is derived from the reason's text.
     *
     * @param reason the specific {@code IbanValidationError} that occurred, must not be null
     */
    InvalidIbanException(IbanValidationError reason) {
        super(Objects.requireNonNull(reason, "reason required").getText());
        this.reason = reason;
    }

    /**
     * Static factory method to create an {@code InvalidIbanException} instance.
     *
     * @param reason the specific {@code IbanValidationError} that occurred
     * @return a new {@code InvalidIbanException} instance
     */
    public static InvalidIbanException of(IbanValidationError reason) {
        return new InvalidIbanException(reason);
    }

    /**
     * Returns the specific reason for the validation failure.
     *
     * @return the validation error reason
     */
    public IbanValidationError getReason() {
        return reason;
    }

    /**
     * Returns a detailed string representation of this exception, including the
     * simple class name, the error message, and the specific validation reason.
     *
     * @return a string representation of the exception
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
