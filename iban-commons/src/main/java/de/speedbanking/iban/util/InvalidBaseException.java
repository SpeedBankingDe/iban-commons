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

import static java.util.Objects.requireNonNull;

import de.speedbanking.util.ValidationError;

import java.util.Locale;
import java.util.Objects;

/**
 * Abstract base class for exceptions thrown when a financial identifier string
 * fails validation.
 * <p>
 * Concrete subclasses store a specific {@link ValidationError} enum constant as
 * the failure reason and override {@link #getReason()} with a covariant return
 * type to expose the concrete enum type to callers without casting.
 * All shared logic — input normalization, country code handling, {@code equals},
 * {@code hashCode}, and {@code toString} — is implemented here exactly once.
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

    /** The optional input found to be erroneous, stored verbatim (untrimmed); {@code null} only if the input itself was {@code null}. */
    private final String          input;

    /** The optional ISO country code associated with the validation failure, trimmed and upper-cased to {@code null} if blank. */
    private final String          countryCode;

    /**
     * Constructs a new exception with the specified validation failure reason, erroneous input, and optional country code.
     * The exception message is derived from the reason's text.
     *
     * @param reason      the specific {@code ValidationError} that occurred, must not be {@code null}
     * @param input       the input string that caused the error, may be {@code null}
     * @param countryCode the ISO country code context, may be {@code null}
     */
    protected InvalidBaseException(ValidationError reason, CharSequence input, String countryCode) {
        super(requireNonNull(requireNonNull(reason, "reason required").getText(), "reason text required"));
        this.reason = reason;
        this.input = input == null ? null : input.toString();
        this.countryCode = isBlank(countryCode) ? null : countryCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Checks whether a character sequence is {@code null}, empty, or contains only whitespace characters.
     * <p>
     * Emulates {@code String.isBlank()} for compatibility with Java 8.
     *
     * @param cs the character sequence to check, may be {@code null}
     * @return {@code true} if the character sequence is {@code null}, empty, or whitespace-only
     */
    @SuppressWarnings("InnerAssignment")
    private static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Constructs a new exception with the specified validation failure reason and the erroneous input.
     *
     * @param reason the specific {@code ValidationError} that occurred, must not be {@code null}
     * @param input  the input string that caused the error, may be {@code null}
     */
    protected InvalidBaseException(ValidationError reason, CharSequence input) {
        this(reason, input, null);
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
     * The value is stored verbatim (not trimmed); it is {@code null} only if the original
     * input itself was {@code null}.
     *
     * @return the invalid input, or {@code null} if not set
     */
    public final CharSequence getInput() {
        return input;
    }

    /**
     * Returns the ISO country code associated with this validation failure, if available.
     * The value is trimmed and converted to upper case; blank input is normalized to {@code null}.
     *
     * @return the ISO country code, or {@code null} if not set or blank
     */
    public final String getCountryCode() {
        return countryCode;
    }

    /**
     * Indicates whether some other object is equal to this exception.
     * Two instances are considered equal if they are both {@code InvalidBaseException}
     * (or a subclass thereof) and have the same {@code reason}, {@code input}, and {@code countryCode}.
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
            && Objects.equals(input, other.input)
            && Objects.equals(countryCode, other.countryCode);
    }

    /**
     * Returns a hash code value for this exception based on {@code reason}, {@code input}, and {@code countryCode}.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(reason, input, countryCode);
    }

    /**
     * Returns the detailed error message string of this exception.
     * <p>
     * The message includes the base error text from the underlying {@link ValidationError},
     * the specific reason constant in parentheses, the country code if present, and the erroneous input if present.
     *
     * @return the detailed error message
     */
    @Override
    public final String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage())
            .append(' ')
            .append('(').append(reason).append(')');
        if (countryCode != null) {
            sb.append(", country ").append(countryCode);
        }
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
        return getClass().getSimpleName()
            + '['
            + "reason=" + reason
            + (countryCode == null ? "" : ", country=" + countryCode)
            + ", input='" + input + "'"
            + ']';
    }

}
