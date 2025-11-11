/*
 * Copyright © 2025 Markus Spann, SpeedBankingDe
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
 * Specifies the reasons why an IBAN string failed validation.
 *
 * @since 1.8.0
 */
public enum IbanValidationError {

    /** The IBAN input string is null or empty. */
    EMPTY("IBAN is null or empty"),

    /** The overall length of the IBAN string is incorrect (not between 15 and 34 characters). */
    INCORRECT_LENGTH("IBAN has incorrect length"),

    /** The IBAN contains characters that are not digits (0-9) or uppercase letters (A-Z). */
    ILLEGAL_CHARACTERS("IBAN contains illegal character(s)"),

    /** The country code (first two letters) is syntactically invalid or does not correspond to a known country. */
    INVALID_COUNTRY("IBAN has invalid country code"),

    /** The country code is valid but is not supported by the current IBAN registry. */
    UNSUPPORTED_COUNTRY("IBAN has unsupported country code"),

    /** The length of the IBAN is correct overall, but incorrect for the specific country code provided. */
    INCORRECT_LENGTH_COUNTRY("IBAN has incorrect length for specified country"),

    /** The check digits (positions 3 and 4) fail the initial verification (e.g., they are not digits). */
    INVALID_CHECK_DIGITS("IBAN has invalid check digits"),

    /** The structure of the BBAN (country code specific part) does not match the expected format (e.g., incorrect length of bank code). */
    INVALID_STRUCTURE("IBAN violates country-specific structure rules"),

    /** The ISO 7064 Modulo 97-10 checksum calculation fails. */
    INVALID_CHECKSUM("IBAN violates ISO 7064 Mod 97-10 checksum check");

    private final String errorText;

    /**
     * Private constructor.
     * @param errorText The descriptive English error message.
     */
    IbanValidationError(String errorText) {
        this.errorText = Objects.requireNonNull(errorText, "Error text required");
    }

    /**
     * Retrieves the descriptive English reason for the validation error.
     * @return The failure text.
     */
    public String getText() {
        return errorText;
    }

    /**
     * Returns a detailed string representation including the enum class name,
     * the enum constant name, and the descriptive error text.
     *
     * @return A long string representation of the validation error.
     */
    public String toLongString() {
        return getClass().getSimpleName()
            + '['
            + name() + ": " + getText()
            + ']';
    }

}
