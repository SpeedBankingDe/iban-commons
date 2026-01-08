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

/**
 * Data container for the successful result of an IBAN validation.
 * It holds the two artifacts required for constructing the final {@link Iban} object.
 *
 * @since 1.8.0
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
final class IbanValidationSuccess {

    final char[]       normIbanArr;
    final IbanRegistry countryData;

    /**
     * Constructs a success data object.
     *
     * @param normIbanArr the validated, normalized IBAN characters
     * @param countryData the metadata for the country code (format, structure)
     */
    IbanValidationSuccess(final char[] normIbanArr, final IbanRegistry countryData) {
        this.normIbanArr = normIbanArr;
        this.countryData = countryData;
    }

    char[] getNormIbanArr() {
        return normIbanArr;
    }

    IbanRegistry getCountryData() {
        return countryData;
    }

}
