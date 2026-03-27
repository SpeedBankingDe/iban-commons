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

import de.speedbanking.util.ValidationError;

import java.util.Objects;

/**
 * Specifies the reasons why a BIC string failed validation.
 *
 * @since 1.8.0
 */
public enum BicValidationError implements ValidationError {

    /** The input string for the BIC was {@code null} or contained no characters. */
    EMPTY("BIC is null or empty"),

    /** The BIC string does not have the mandatory length of 8 or 11 characters (ISO 9362 requires 8 or 11). */
    INCORRECT_LENGTH("BIC has incorrect length"),

    /** The institution code (characters 1 - 4) within the BIC is not alphabetic. */
    INVALID_BANK_CODE("Invalid bank code"),

    /** The country code (characters 5 and 6) within the BIC is not a valid ISO 3166-1 Alpha-2 code. */
    INVALID_COUNTRY("BIC has invalid country code"),

    /** The BIC contains characters that are not allowed (only upper-case letters and digits are permitted). */
    ILLEGAL_CHARACTERS("BIC contains illegal character(s)");

    private final String errorText;

    /**
     * Private constructor.
     * @param errorText the descriptive English error message
     */
    BicValidationError(String errorText) {
        this.errorText = Objects.requireNonNull(errorText, "Error text required");
    }

    /**
     * Retrieves the descriptive English reason for the validation error.
     * @return the failure text
     */
    @Override
    public String getText() {
        return errorText;
    }

    /**
     * Returns a detailed string representation including the enum class name,
     * the enum constant name, and the descriptive error text.
     *
     * @return a long string representation of the validation error
     */
    public String toLongString() {
        return getClass().getSimpleName()
            + '['
            + name() + ": " + getText()
            + ']';
    }

}
