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

import static java.util.Objects.requireNonNull;

import java.util.function.Function;

/**
 * Encapsulates structural components of an IBAN, their validation error mapping,
 * and the extraction logic for their expected pattern.
 *
 * @since 1.8.9
 */
@SuppressWarnings("ImmutableEnumChecker")
enum IbanComponent {

    /** BBAN (Basic Bank Account Number) component. */
    BBAN(
        IbanValidationError.INVALID_BBAN,
        IbanRegistry::getBbanPattern),

    /** Bank code component. */
    BANK_CODE(
        IbanValidationError.INVALID_BANK_CODE,
        IbanRegistry::getBankCodePattern),

    /** Branch code component. */
    BRANCH_CODE(
        IbanValidationError.INVALID_BRANCH_CODE,
        IbanRegistry::getBranchCodePattern),

    /** Account number component. */
    ACCOUNT_NUMBER(
        IbanValidationError.INVALID_ACCOUNT_NUMBER,
        IbanRegistry::getAccountNumberPattern);

    private final IbanValidationError            validationError;
    private final Function<IbanRegistry, String> patternExtractor;

    IbanComponent(IbanValidationError validationError, Function<IbanRegistry, String> patternExtractor) {
        this.validationError = requireNonNull(validationError, "validationError must not be null");
        this.patternExtractor = requireNonNull(patternExtractor, "patternExtractor must not be null");
    }

    /**
     * Returns the validation error associated with this component.
     *
     * @return the validation error, never null
     */
    IbanValidationError getValidationError() {
        return validationError;
    }

    /**
     * Extracts the required pattern string for this component from the given country registry.
     *
     * @param registry the country registry entry, must not be null
     * @return the pattern string, or null if not defined
     */
    String getPattern(IbanRegistry registry) {
        requireNonNull(registry, "registry must not be null");
        return patternExtractor.apply(registry);
    }

}
