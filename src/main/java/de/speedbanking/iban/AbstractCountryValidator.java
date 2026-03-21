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
 * Abstract base class for all country-specific IBAN validators.
 *
 * <h3>Design</h3>
 * Every generated {@code CountryValidator} inner class extends this base instead of
 * implementing {@link CountryValidator} directly. Centralising shared behaviour here
 * keeps the generated subclasses minimal: they only contain a {@code validateIban}
 * implementation and nothing else.
 *
 * <h3>toString</h3>
 * A final {@link #toString()} is provided that formats the validator as
 * {@code "XX[Country Name]"}, derived from the {@link IbanRegistry} entry whose key
 * equals the simple class name. Subclasses must not override it.
 *
 * <h3>Inheritance contract</h3>
 * Subclasses must implement {@link CountryValidator#validateIban(char[])}.
 * Their simple class name must match a valid {@link IbanRegistry} constant
 * (e.g. class {@code DE} → {@code IbanRegistry.DE}) so that {@link #toString()}
 * and any registry lookups work correctly.
 *
 * @since 1.8.5
 *
 * @see CountryValidator
 * @see AbstractNcdCountryValidator
 * @see IbanRegistry
 */
abstract class AbstractCountryValidator implements CountryValidator {

    /** Country-specific registry data resolved at construction time. */
    private final IbanRegistry countryData;

    /**
     * Package-private constructor — only subclasses within this package may extend
     * this class.
     */
    AbstractCountryValidator() {
        String clazzName = getClass().getSimpleName();
        countryData = IbanRegistry.getByCode(clazzName);

        if (countryData == null) {
            throw new ExceptionInInitializerError("'" + clazzName + "' is not a supported IBAN country code");
        }
    }

    /**
     * Provides access to country-specific registry data of the calculator, i.e. the enum entry in {@link IbanRegistry}.
     *
     * @return country-specific registry data
     *
     * @see IbanRegistry
     */
    final IbanRegistry getCountryData() {
        return countryData;
    }

    /**
     * Returns a human-readable representation of this validator.
     * <p>
     * Format: {@code "XX[Country Name]"} where {@code XX} is the ISO 3166-1 alpha-2
     * country code (the simple class name) and {@code Country Name} is the full name
     * from the {@link IbanRegistry}.
     * <p>
     * Example: {@code "DE[Germany]"}
     *
     * @return a non-{@code null} string identifying this validator
     */
    @Override
    public final String toString() {
        return getClass().getSimpleName() + '[' + countryData.getCountryName() + ']';
    }

}
