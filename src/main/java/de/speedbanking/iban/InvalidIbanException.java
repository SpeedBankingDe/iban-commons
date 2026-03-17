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

import de.speedbanking.iban.util.InvalidBaseException;

/**
 * Exception thrown when a string fails IBAN validation in the strict {@code Iban.of()} method.
 * <p>
 * Instances are typically created via the static factory methods {@link #of(IbanValidationError)}
 * or {@link #of(IbanValidationError, CharSequence)} rather than directly via constructors.
 *
 * @since 1.8.0
 */
public class InvalidIbanException extends InvalidBaseException {

    /** Serial version UID. */
    private static final long serialVersionUID = 42L;

    /**
     * Constructs a new exception with the specified validation failure reason and the erroneous input.
     * The exception message is derived from the reason's text.
     *
     * @param reason the specific {@code IbanValidationError} that occurred, must not be {@code null}
     * @param input  the IBAN input string that caused the error, may be {@code null};
     *               blank values are normalized to {@code null}
     */
    InvalidIbanException(IbanValidationError reason, CharSequence input) {
        super(reason, input);
    }

    /**
     * Static factory method to create an {@code InvalidIbanException} with the specified reason.
     *
     * @param reason the specific {@code IbanValidationError} that occurred, must not be {@code null}
     * @return a new {@code InvalidIbanException} instance
     */
    public static InvalidIbanException of(IbanValidationError reason) {
        return new InvalidIbanException(reason, null);
    }

    /**
     * Static factory method to create an {@code InvalidIbanException} with the specified reason
     * and the erroneous IBAN input.
     *
     * @param reason the specific {@code IbanValidationError} that occurred, must not be {@code null}
     * @param input  the IBAN input string that caused the error, may be {@code null};
     *               blank values are normalized to {@code null}
     * @return a new {@code InvalidIbanException} instance
     */
    public static InvalidIbanException of(IbanValidationError reason, CharSequence input) {
        return new InvalidIbanException(reason, input);
    }

    /**
     * Returns the specific reason for the IBAN validation failure.
     *
     * @return the validation error reason, never {@code null}
     */
    @Override
    public IbanValidationError getReason() {
        return (IbanValidationError) super.getReason();
    }

}
