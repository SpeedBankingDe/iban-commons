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

import static de.speedbanking.util.CharUtil.isDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isNotUpperCase;

import de.speedbanking.util.Country;

/**
 * The core engine for BIC validation.
 * <p>
 * Validation is designed to abort at the first error (fail-fast).
 *
 * @since 1.8.0
 */
public final class BicValidator {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private BicValidator() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Performs a full BIC validation on an input character sequence
     * and returns a {@link BicValidationResult}.
     *
     * @param rawBic the BIC character sequence to validate
     * @return the validation result
     *
     * @since 1.8.0
     */
    static BicValidationResult validate(final CharSequence rawBic) {
        if (rawBic == null || rawBic.length() == 0) {
            return BicValidationResult.invalid(BicValidationError.EMPTY);
        }

        int len = rawBic.length();

        if (len != Bic.BIC8_LENGTH && len != Bic.BIC11_LENGTH) {
            return BicValidationResult.invalid(BicValidationError.INCORRECT_LENGTH);
        }

        BicValidationError error = validateCharacters(rawBic, len);
        return error == null ? BicValidationResult.valid(rawBic) : BicValidationResult.invalid(error);
    }

    /**
     * Internal character-by-character validation based on position-specific requirements.
     * <p>
     * This method assumes that null-checks and length-checks have already been performed.
     * It evaluates bank code, country code (ISO 3166), and location/branch codes.
     *
     * @since 1.8.5
     *
     * @param rawBic the character sequence to validate
     * @param len the pre-calculated length of the sequence
     * @return the first encountered {@link BicValidationError}, or {@code null} if all characters are valid
     */
    static BicValidationError validateCharacters(final CharSequence rawBic, final int len) {
        for (int i = 0; i < len; i++) {
            char c = rawBic.charAt(i);

            // Strategy: Guard clauses or dedicated methods for position-based logic
            if (i < Bic.COUNTRY_CODE_START) { // <4
                if (isNotUpperCase(c)) {
                    return BicValidationError.INVALID_BANK_CODE;
                }
            } else if (i == Bic.COUNTRY_CODE_START) { // ==4 (country code part 1)
                if (isNotUpperCase(c)) {
                    return BicValidationError.INVALID_COUNTRY;
                }
            } else if (i == Bic.COUNTRY_CODE_START + 1) { // country code part 2 & full ISO check
                if (!Country.isAssigned(rawBic.charAt(i - 1), c)) {
                    return BicValidationError.INVALID_COUNTRY;
                }
            } else if (!isDigitOrUpperCase(c)) { // Location & Branch Code
                return BicValidationError.ILLEGAL_CHARACTERS;
            }
        }
        return null;
    }

}
