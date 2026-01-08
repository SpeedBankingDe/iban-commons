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
import static de.speedbanking.util.CharUtil.isUpperCase;

/**
 * The core engine for BIC validation.
 * <p>
 * Validation is designed to abort at the first error (fail-fast).
 *
 * @since 1.8.0
 */
public final class BicValidator {

    private BicValidator() {
        throw new UnsupportedOperationException(
            "Utility class " + BicValidator.class.getSimpleName() + " cannot be instantiated");
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
    public static BicValidationResult validate(final CharSequence rawBic) {

        if (rawBic == null || rawBic.length() == 0) {
            return validationFailed(BicValidationError.EMPTY);
        }

        final int len = rawBic.length();

        if (len != Bic.BIC8_LENGTH && len != Bic.BIC11_LENGTH) {
            return validationFailed(BicValidationError.INCORRECT_LENGTH);
        }

        char[] bicArr = new char[len];

        // 1. Character Set Check
        // BIC must only contain uppercase ASCII letters (A-Z) and digits (0-9)
        for (int i = 0; i < len; i++) {
            char c = rawBic.charAt(i);

            // BIC should be all uppercase and contain only digits or letters.
            if (!isDigitOrUpperCase(c)) {
                return validationFailed(BicValidationError.ILLEGAL_CHARACTERS);
            }
            bicArr[i] = c;
        }

        // 2. Country Code Check (positions 5 and 6, indices 4 and 5)
        // ISO 9362 requires the Country Code to be 2 alphabetic characters (A-Z)
        if (!isUpperCase(bicArr[Bic.COUNTRY_CODE_START]) || !isUpperCase(bicArr[Bic.COUNTRY_CODE_START + 1])) {
            return validationFailed(BicValidationError.INVALID_COUNTRY);
        }

        Bic bic = new Bic(bicArr);
        return BicValidationResult.valid(bic);
    }

    /**
     * Finalizes an invalid validation result by storing the reason and creating the result object.
     */
    static BicValidationResult validationFailed(final BicValidationError reason) {
        return BicValidationResult.invalid(reason);
    }

}
