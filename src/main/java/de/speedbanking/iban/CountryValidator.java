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

import de.speedbanking.util.CharUtil;

/**
 * Functional interface for country-specific IBAN structure validation.
 * <p>
 * Each supported country provides a concrete implementation in {@link CountryValidators}
 * that validates the BBAN portion of the IBAN according to its national format rules.
 *
 * <h3>National Check Digit validation</h3>
 * Validators for countries that embed a National Check Digit (NCD) in their BBAN extend
 * {@link AbstractNcdCountryValidator}, which additionally implements
 * {@link NationalCheckDigitCalculator}.  The {@code validateIban} implementation of such
 * validators calls {@link NationalCheckDigitCalculator#validateNationalCheckDigit(char[])}
 * as part of its boolean expression.
 *
 * <h3>Implementations</h3>
 * All country-specific implementations live in {@link CountryValidators} and are generated
 * by {@code CountryValidatorCodeGenerator}.  They are not intended to be referenced
 * directly — use {@link IbanRegistry#getCountryValidator()} to obtain the validator for
 * a specific country.
 *
 * @since 1.8.0
 *
 * @see CountryValidators
 * @see AbstractCountryValidator
 * @see AbstractNcdCountryValidator
 * @see NationalCheckDigitCalculator
 */
@FunctionalInterface
interface CountryValidator {

    /**
     * Validates the BBAN structure inside the given IBAN character array.
     * <p>
     * Assumes that elementary validations (overall length, character set) have already
     * been performed upstream.  The array is therefore guaranteed to contain only digits
     * and uppercase ASCII letters — no {@code null} check, length check, or
     * {@link CharUtil#isDigitOrUpperCase(char)} check is needed inside this method.
     *
     * @param iban the fully assembled, normalised IBAN as a {@code char[]}
     * @return {@code true} if the IBAN conforms to the country's structural rules,
     *         {@code false} otherwise
     */
    boolean validateIban(char[] iban);
}
