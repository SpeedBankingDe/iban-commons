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
package de.speedbanking.iban.util;

import de.speedbanking.util.ValidationError;

import java.util.Objects;

/**
 * Abstract base class for exceptions thrown when a financial identifier string
 * fails validation.
 * <p>
 * Concrete subclasses store a specific {@link ValidationError} enum constant as
 * the failure reason and override {@link #getReason()} with a covariant return
 * type to expose the concrete enum type to callers without casting.
 * All shared logic — input normalisation, {@code equals}, {@code hashCode}, and
 * {@code toString} — is implemented here exactly once.
 *
 * @since 1.8.5
 *
 * @see de.speedbanking.iban.InvalidIbanException
 * @see de.speedbanking.bic.InvalidBicException
 */
public abstract class InvalidBaseException extends RuntimeException {

    /** Serial version UID. */
    private static final long     serialVersionUID = 42L;

    /** The specific reason why validation failed. */
    private final ValidationError reason;

    /** The optional input found to be erroneous, trimmed to {@code null} if blank. */
    private final String          input;

    /**
     * Constructs a new exception with the specified validation failure reason and the erroneous input.
     * The exception message is derived from the reason's text.
     *
     * @param reason the specific {@code ValidationError} that occurred, must not be {@code null}
     * @param input  the input string that caused the error, may be {@code null}
     */
    protected InvalidBaseException(ValidationError reason, CharSequence input) {
        super(Objects.requireNonNull(Objects.requireNonNull(reason, "reason required").getText(), "reason text required"));
        this.reason = reason;
        this.input = input == null ? null : input.toString();
    }

    /**
     * Returns the specific reason for the validation failure.
     * <p>
     * Concrete subclasses should override this method with a covariant return
     * type to expose the concrete {@link ValidationError} enum type directly.
     *
     * @return the validation error reason, never {@code null}
     */
    public ValidationError getReason() {
        return reason;
    }

    /**
     * Returns the erroneous input that triggered this exception, if available.
     * The value is trimmed; blank input is normalized to {@code null}.
     *
     * @return the invalid input, or {@code null} if not set or blank
     */
    public final CharSequence getInput() {
        return input;
    }

    /**
     * Indicates whether some other object is equal to this exception.
     * Two instances are considered equal if they are of the same concrete class
     * and have the same {@code reason} and {@code input}.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is the same as {@code obj}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof InvalidBaseException)) {
            return false;
        }
        InvalidBaseException other = (InvalidBaseException) obj;
        return reason == other.reason
            && Objects.equals(input, other.input);
    }

    /**
     * Returns a hash code value for this exception based on {@code reason} and {@code input}.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(reason, input);
    }

    /**
     * Returns the detailed error message string of this exception.
     * <p>
     * The message includes the base error text from the underlying {@link ValidationError},
     * the specific reason constant in parentheses, and the erroneous input if present.
     *
     * @return the detailed error message
     */
    @Override
    public final String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage())
            .append(' ')
            .append('(').append(reason).append(')');
        if (input != null && !input.isEmpty()) {
            sb.append(": ").append('\'').append(input).append('\'');
        }
        return sb.toString();
    }

    /**
     * Returns a short string representation of this exception for debugging purposes.
     * <p>
     * Follows the format {@code SimpleClassName[field=value, ...]}.
     *
     * @return a string representation of the exception
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[reason=" + reason + ", input=" + input + ']';
    }

}
