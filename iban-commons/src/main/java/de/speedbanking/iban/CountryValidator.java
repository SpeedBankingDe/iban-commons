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

import static de.speedbanking.iban.util.IbanCharType.ALPHABETIC;
import static de.speedbanking.iban.util.IbanCharType.ALPHANUMERIC;
import static de.speedbanking.iban.util.IbanCharType.NUMERIC;
import static de.speedbanking.iban.util.IbanCharType.SPACE;
import static de.speedbanking.util.CharUtil.isAllDigitOrUpperCase;
import static de.speedbanking.util.CharUtil.isAllDigits;
import static de.speedbanking.util.CharUtil.isAllUpperCase;
import static de.speedbanking.util.CharUtil.toCharArray;

import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segments;
import de.speedbanking.util.CharUtil;

/**
 * Validates the BBAN structure of the IBANs of one country.
 * <p>
 * The checks are derived once from the country's BBAN pattern in {@link IbanRegistry}.
 * Adjacent segments of the same character type are merged, and every numeric or alphabetic
 * segment becomes an index range that must consist of digits or uppercase letters only.
 * Alphanumeric segments need no check of their own, since the validation pipeline only passes
 * on IBANs made of digits and uppercase ASCII letters. A BBAN consisting of a single
 * alphanumeric segment is still checked as a whole.
 * <p>
 * For countries with a National Check Digit (NCD), {@link #validateIban(char[])} also verifies
 * the NCD using the calculator provided by {@link NationalCheckDigitCalculators#forCountry(IbanRegistry)}.
 * <p>
 * Instances are immutable and thread-safe. Use {@link IbanValidator#getCountryValidator(IbanRegistry)}
 * to obtain the validator for a country.
 *
 * @since 1.8.0
 *
 * @see IbanValidator
 * @see NationalCheckDigitCalculator
 */
final class CountryValidator {

    /** Empty range array for character types that need no check. */
    private static final int[]                 NO_RANGES = new int[0];

    /** Country-specific registry data, always a base country. */
    private final IbanRegistry                 countryData;

    /** Begin (inclusive) and end (exclusive) index pairs of the IBAN ranges that must be all digits. */
    private final int[]                        digitRanges;

    /** Begin (inclusive) and end (exclusive) index pairs of the IBAN ranges that must be all uppercase letters. */
    private final int[]                        upperCaseRanges;

    /** Begin (inclusive) and end (exclusive) index pairs of the IBAN ranges that must be all digits or uppercase letters. */
    private final int[]                        digitOrUpperCaseRanges;

    /** The NCD calculator of the country, {@code null} if the country has no NCD. */
    private final NationalCheckDigitCalculator ncdCalculator;

    /**
     * Creates the validator for the given country from its BBAN pattern.
     *
     * @param countryData the registry entry of a base country; must not be {@code null}
     * @throws IllegalArgumentException if the BBAN pattern contains a space segment
     *
     * @since 1.8.13
     */
    CountryValidator(IbanRegistry countryData) {
        this(countryData, countryData.getBbanPattern());
    }

    /**
     * Creates the validator for the given country from the given BBAN pattern.
     * <p>
     * Separate from {@link #CountryValidator(IbanRegistry)} so that tests can derive
     * validators from patterns not present in the registry.
     *
     * @param countryData the registry entry of a base country; must not be {@code null}
     * @param bbanPattern the BBAN pattern in registry notation, e.g. {@code "4!a4!n2!n8!c"}
     * @throws IllegalArgumentException if the BBAN pattern contains a space segment
     *
     * @since 1.8.13
     */
    CountryValidator(IbanRegistry countryData, String bbanPattern) {
        this.countryData = countryData;

        Segments segments = IbanPatternConverter.parseSegments(bbanPattern).aggregate();
        if (segments.containsType(SPACE)) {
            throw new IllegalArgumentException(String.format("Unsupported space segment in BBAN pattern %s of %s",
                bbanPattern, countryData.getCountryCode()));
        }

        digitRanges = segments.ranges(NUMERIC, IbanRegistry.INDEX_BBAN);
        upperCaseRanges = segments.ranges(ALPHABETIC, IbanRegistry.INDEX_BBAN);
        digitOrUpperCaseRanges = segments.size() == 1
            ? segments.ranges(ALPHANUMERIC, IbanRegistry.INDEX_BBAN)
            : NO_RANGES;

        ncdCalculator = countryData.hasNationalCheckDigit()
            ? NationalCheckDigitCalculators.forCountry(countryData)
            : null;
    }

    /**
     * Validates the BBAN structure inside the given IBAN character array.
     * <p>
     * Assumes that elementary validations (overall length, character set) have already
     * been performed upstream. The array is therefore guaranteed to contain only digits
     * and uppercase ASCII letters, no {@code null} check, length check, or
     * {@link CharUtil#isDigitOrUpperCase(char)} check is needed inside this method.
     *
     * @param iban the fully assembled, normalized IBAN as a {@code char[]}
     * @return {@code true} if the IBAN conforms to the country's structural rules
     *         and, if present, its National Check Digit is valid, {@code false} otherwise
     */
    boolean validateIban(char[] iban) {
        return isAllDigitsInRanges(iban, digitRanges)
            && isAllUpperCaseInRanges(iban, upperCaseRanges)
            && isAllDigitOrUpperCaseInRanges(iban, digitOrUpperCaseRanges)
            && validateNationalCheckDigit(iban);
    }

    /**
     * Validates the BBAN structure of the given character sequence.
     * <p>
     * Converts the sequence to a {@code char[]} before calling {@link #validateIban(char[])}.
     * Note that this involves an array allocation.
     *
     * @param iban the IBAN character sequence to validate
     * @return {@code true} if valid, {@code false} otherwise
     */
    boolean validateIban(CharSequence iban) {
        return validateIban(toCharArray(iban));
    }

    /**
     * Validates the National Check Digit (NCD) of the given IBAN.
     *
     * @param iban the normalized IBAN as a {@code char[]}
     * @return {@code true} if the country has no NCD, NCD validation is disabled in
     *         {@link IbanConfig}, or the NCD is valid
     *
     * @since 1.8.13
     */
    boolean validateNationalCheckDigit(char[] iban) {
        return ncdCalculator == null
            || !IbanConfig.isValidateNcd()
            || ncdCalculator.validateNationalCheckDigit(iban);
    }

    /**
     * Computes the National Check Digit (NCD) for the given IBAN.
     * <p>
     * If NCD calculation is disabled in {@link IbanConfig}, the NCD already present in
     * {@code iban} is returned unchanged.
     *
     * @param iban the normalized IBAN as a {@code char[]}
     * @return the NCD as a char array, its length matches the NCD field width
     * @throws IllegalStateException if the country has no NCD
     *
     * @since 1.8.13
     */
    char[] calculateNationalCheckDigit(char[] iban) {
        if (ncdCalculator == null) {
            throw new IllegalStateException(String.format("%s has no national check digit", countryData.getCountryCode()));
        }
        return IbanConfig.isCalculateNcd()
            ? ncdCalculator.calculateNationalCheckDigit(iban)
            : countryData.getNationalCheckDigitComponent().extractFrom(iban);
    }

    /**
     * Returns the registry entry this validator was created for.
     *
     * @return the base country's registry entry, never {@code null}
     *
     * @since 1.8.13
     */
    IbanRegistry getCountryData() {
        return countryData;
    }

    /**
     * Returns the NCD calculator of the country.
     *
     * @return the calculator, {@code null} if the country has no NCD
     *
     * @since 1.8.13
     */
    NationalCheckDigitCalculator getNcdCalculator() {
        return ncdCalculator;
    }

    /**
     * Returns a human-readable representation of this validator in the format
     * {@code "XX[Country Name]"}, e.g. {@code "DE[Germany]"}.
     *
     * @return a non-{@code null} string identifying this validator
     */
    @Override
    public String toString() {
        return String.format("%s[%s]", countryData.getCountryCode(), countryData.getCountryName());
    }

    /**
     * Checks that all given ranges of the IBAN consist of digits only.
     *
     * @param iban   the IBAN
     * @param ranges begin and end index pairs
     * @return {@code true} if every range is all digits
     */
    private static boolean isAllDigitsInRanges(char[] iban, int[] ranges) {
        for (int i = 0; i < ranges.length; i += 2) {
            if (!isAllDigits(iban, ranges[i], ranges[i + 1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that all given ranges of the IBAN consist of uppercase letters only.
     *
     * @param iban   the IBAN
     * @param ranges begin and end index pairs
     * @return {@code true} if every range is all uppercase letters
     */
    private static boolean isAllUpperCaseInRanges(char[] iban, int[] ranges) {
        for (int i = 0; i < ranges.length; i += 2) {
            if (!isAllUpperCase(iban, ranges[i], ranges[i + 1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that all given ranges of the IBAN consist of digits or uppercase letters only.
     *
     * @param iban   the IBAN
     * @param ranges begin and end index pairs
     * @return {@code true} if every range is all digits or uppercase letters
     */
    private static boolean isAllDigitOrUpperCaseInRanges(char[] iban, int[] ranges) {
        for (int i = 0; i < ranges.length; i += 2) {
            if (!isAllDigitOrUpperCase(iban, ranges[i], ranges[i + 1])) {
                return false;
            }
        }
        return true;
    }

}
