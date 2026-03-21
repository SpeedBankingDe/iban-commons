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

import de.speedbanking.util.IndexRange;

/**
 * Strategy interface for country-specific <strong>National Check Digit (NCD)</strong>
 * computation and verification within an IBAN.
 *
 * <h3>Background</h3>
 * Several national IBAN formats embed a country-specific check digit (or check-digit pair)
 * inside the BBAN, in addition to the two ISO 7064 Mod 97-10 check digits at positions 3–4
 * of the full IBAN.  The position of this NCD is recorded in
 * {@link IbanRegistry.StructureData#nationalCheckDigitIndexRange()} and marked with
 * {@code withNationalCheckDigit(…)} in the registry builder.
 *
 * <h3>Usage</h3>
 * <ul>
 *   <li><strong>Validation</strong> – {@link CountryValidator} implementations call
 *       {@link #validateNationalCheckDigit(char[])} to verify an existing IBAN.</li>
 *   <li><strong>Generation</strong> – {@link RandomIban} calls
 *       {@link #calculateNationalCheckDigit(char[])} after producing a random BBAN, then
 *       writes the result back into the {@code char[]} at the index range defined by the
 *       registry.</li>
 * </ul>
 *
 * <h3>Implementation contract</h3>
 * <ul>
 *   <li>The {@code iban} parameter is always a fully assembled, normalised {@code char[]}
 *       containing the country code + placeholder check digits + BBAN.  All characters are
 *       guaranteed to be digits or uppercase ASCII letters.</li>
 *   <li>{@link #calculateNationalCheckDigit(char[])} returns the NCD as a {@code String}
 *       whose length equals the width of
 *       {@link IbanRegistry.StructureData#nationalCheckDigitIndexRange()}.</li>
 *   <li>{@link #validateNationalCheckDigit(char[])} computes the expected NCD and compares
 *       it with the value already present in {@code iban}.</li>
 * </ul>
 *
 * <h3>Implementations</h3>
 * All country-specific implementations live in {@link NationalCheckDigitCalculators} and
 * are maintained there by hand.  They are not intended to be referenced directly — use
 * {@link IbanRegistry#getNcdCalculator()} to obtain the calculator for a specific country.
 *
 * @since 1.8.5
 *
 * @see NationalCheckDigitCalculators
 * @see CountryValidator
 * @see RandomIban
 */
interface NationalCheckDigitCalculator {

    /**
     * Computes the expected National Check Digit for the given IBAN.
     *
     * @param iban the normalised IBAN as a {@code char[]};
     *             must not be {@code null} and is guaranteed to contain only
     *             digits and uppercase ASCII letters
     * @return the computed NCD as a char array (length matches the NCD field width)
     */
    char[] calculateNationalCheckDigit(char[] iban);

    /**
     * Validates the National Check Digit already present in the given IBAN.
     * <p>
     * Implementations use their own {@code COUNTRY} constant to obtain the NCD
     * {@link IndexRange} and compare the value already present in {@code iban}
     * against the value computed by {@link #calculateNationalCheckDigit(char[])}.
     *
     * @param iban the normalised IBAN as a {@code char[]};
     *             must not be {@code null}
     * @return {@code true} if the NCD present in {@code iban} matches the computed value,
     *         {@code false} otherwise
     */
    boolean validateNationalCheckDigit(char[] iban);

}
