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
package de.speedbanking.bic;

import de.speedbanking.util.CountryUtil;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a valid, immutable Business Identifier Code (BIC), also known as SWIFT Code (ISO 9362).
 * <p>
 * Creation is done exclusively via static factory methods after successful validation.
 *
 * @since 1.8.0
 */
public final class Bic implements Serializable, CharSequence, Comparable<Bic> {

    private static final long         serialVersionUID    = 42L;

    /** The mandatory "{@code XXX}" suffix for an 8-character BIC to become an 11-character BIC. */
    private static final String       HEAD_OFFICE_SUFFIX  = "XXX";

    /** The fixed length of a BIC without branch code (8 characters). */
    static final int                  BIC8_LENGTH         = 8;

    /** The fixed length of a BIC with branch code (11 characters). */
    static final int                  BIC11_LENGTH        = BIC8_LENGTH + HEAD_OFFICE_SUFFIX.length();

    /** Indices for the BIC components (ISO 9362). */
    static final int                  BANK_CODE_START     = 0;
    static final int                  COUNTRY_CODE_START  = 4;
    static final int                  LOCATION_CODE_START = 6;
    static final int                  BRANCH_CODE_START   = 8; // only for BIC-11

    /** The raw, normalized BIC character array. */
    private final char[]              bicArr;

    /** {@code true} if the object represents a BIC-8. */
    private final boolean             isBic8;

    /** The normalized BIC-8 string. */
    private final String              bic8;
    /** The normalized BIC-11 string. */
    private final String              bic11;

    // cached components
    private transient volatile String bankCode;
    private transient volatile String countryCode;
    private transient volatile String locationCode;
    private transient volatile String branchCode; // null if BIC-8

    /**
     * Package-private constructor.
     * <p>
     * Construction is restricted to {@link BicValidator} which guarantees
     * the input {@code bicArr} is valid, normalized, and correctly sized (8 or 11).
     *
     * @param bicArr The normalized, validated BIC characters.
     */
    Bic(final char[] bicArr) {
        this.bicArr = bicArr;
        this.isBic8 = bicArr.length == BIC8_LENGTH;
        this.bic8 = new String(bicArr, 0, BIC8_LENGTH);
        this.bic11 = isBic8 ? bic8 + HEAD_OFFICE_SUFFIX : new String(bicArr);
    }

    /**
     * Parses and validates the input character sequence, throwing {@link InvalidBicException} if validation fails.
     *
     * @param bic The BIC character sequence.
     * @return    A valid, immutable {@code Bic} instance.
     * @throws InvalidBicException if the BIC is invalid.
     *
     * @since 1.8.0
     */
    public static Bic of(CharSequence bic) {
        BicValidationResult result = BicValidator.validate(bic);
        return result.getBic().orElseThrow(() -> InvalidBicException.of(result.getError().get()));
    }

    /**
     * Attempts to parse and validate the input character sequence safely.
     *
     * @param bic The BIC character sequence.
     * @return    An {@link Optional} containing the {@link Bic} instance if valid, or empty if invalid.
     *
     * @since 1.8.0
     */
    public static Optional<Bic> tryParse(CharSequence bic) {
        return BicValidator.validate(bic).getBic();
    }

    /**
     * Performs a full BIC validation and returns {@code true} if successful.
     *
     * @param bic The BIC character sequence to validate.
     * @return    {@code true} if the BIC is valid, {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public static boolean isValid(final CharSequence bic) {
        return BicValidator.validate(bic).isValid();
    }

    /**
     * Checks if this BIC is 8 characters long (without explicit branch code).
     *
     * @return {@code true} if BIC-8, {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public boolean isBic8() {
        return isBic8;
    }

    /**
     * Checks if this BIC is 11 characters long (with explicit branch code).
     *
     * @return {@code true} if BIC-11, {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public boolean isBic11() {
        return !isBic8;
    }

    /**
     * Returns the Institution Code (Bank Code), which are the BIC's first 4 characters.
     * <p>
     * Example: The bank code for Varengold Bank AG (Hamburg) is {@code VGAG}.
     *
     * @return The Bank Code (e.g., {@code "VGAG"}).
     *
     * @since 1.8.0
     */
    public String getBankCode() {
        if (bankCode == null) {
            bankCode = new String(bicArr, BANK_CODE_START, COUNTRY_CODE_START - BANK_CODE_START).intern();
        }
        return bankCode;
    }

    /**
     * Returns the two-letter ISO country code (positions 5 and 6).
     * <p>
     * This is the <strong>ISO 3166-1 Alpha-2</strong> country code, which identifies the country of the bank.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "EC"} for Ecuador</li>
     *   <li>{@code "KE"} for Kenya</li>
     *   <li>{@code "FI"} for Finland</li>
     * </ul>
     * <p>
     * For more information, see:
     * <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166-1 Alpha-2 Specification</a>
     *
     * @return The Country Code (e.g., {@code "EC"}).
     *
     * @since 1.8.0
     */
    public String getCountryCode() {
        if (countryCode == null) {
            countryCode = new String(bicArr, COUNTRY_CODE_START, LOCATION_CODE_START - COUNTRY_CODE_START).intern();
        }
        return countryCode;
    }

    /**
     * Returns the two-character country flag emoji corresponding to the BIC's
     * country code, based on the <strong>ISO 3166-1 Alpha-2</strong> standard.
     * <p>
     * Examples:
     * <ul>
     *   <li>For country code {@code "BT"}: 🇧🇹 (Bhutan)</li>
     *   <li>For country code {@code "VN"}: 🇻🇳 (Vietnam)</li>
     *   <li>For country code {@code "HR"}: 🇭🇷 (Croatia)</li>
     * </ul>
     *
     * @return The country flag emoji string.
     *
     * @since 1.8.0
     */
    public String getCountryFlag() {
        return CountryUtil.createFlagEmoji(getCountryCode()).intern();
    }

    /**
     * Returns the Location Code (City Code), characters 7 and 8.
     * <p>
     * The location code identifies the city or geographical area of the institution,
     * such as {@code "FF"} for Frankfurt.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "FF"} (Frankfurt)</li>
     *   <li>{@code "HH"} (Hamburg)</li>
     *   <li>{@code "22"} (Non-bank financial institution or test BIC)</li>
     * </ul>
     *
     * @return The Location Code (e.g., {@code "FF"}).
     *
     * @since 1.8.0
     */
    public String getLocationCode() {
        if (locationCode == null) {
            locationCode = new String(bicArr, LOCATION_CODE_START, BRANCH_CODE_START - LOCATION_CODE_START);
        }
        return locationCode;
    }

    /**
     * Returns the Branch Code (characters 9-11).
     * <p>
     * Examples:
     * <ul>
     *   <li>For BIC {@code BHLSDEMMXXX}: Branch Code is {@code XXX} (Head Office of Bankhaus Ludwig Sperrer KG)</li>
     *   <li>For BIC {@code DEUTDEFFXXX}: Branch Code is {@code XXX} (Head Office of Deutsche Bank AG)</li>
     *   <li>For BIC {@code DEUTDEFF444}: Branch Code is {@code 500} (Specific Branch of Deutsche Bank AG Frankfurt am Main)</li>
     * </ul>
     *
     * @return The Branch Code (e.g., {@code "XXX"}), or {@code null} if the BIC is 8 characters long.
     *
     * @since 1.8.0
     */
    public String getBranchCode() {
        if (isBic8) {
            return null;
        } else if (branchCode == null) {
            branchCode = new String(bicArr, BRANCH_CODE_START, BIC11_LENGTH - BRANCH_CODE_START);
        }
        return branchCode;
    }

    /**
     * Returns the BIC in its 8-character format. If the original BIC is 11 characters,
     * the branch code is truncated.
     *
     * @return The BIC-8 string.
     *
     * @since 1.8.0
     */
    public String toBic8() {
        return bic8;
    }

    /**
     * Returns the BIC in its 11-character format. If the original BIC is 8 characters,
     * the <strong>{@code "XXX"}</strong> (head office) suffix is appended.
     *
     * @return The BIC-11 string.
     *
     * @since 1.8.0
     */
    public String toBic11() {
        return bic11;
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index The index of the character to return.
     * @return      The character at the specified index.
     * @throws IndexOutOfBoundsException if the {@code index} is negative or greater than or equal to {@code length()}.
     */
    @Override
    public char charAt(int index) {
        if (index < 0 || index >= bicArr.length) {
            throw new IndexOutOfBoundsException(String
                .format("Index should be between 0 (inclusive) and %d (exclusive), but was %d", bicArr.length, index));
        }
        return bicArr[index];
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     *
     * @param start The start index, inclusive.
     * @param end   The end index, exclusive.
     * @return      The specified subsequence as a new {@link String} instance.
     * @throws IndexOutOfBoundsException if {@code start} or {@code end} are out of bounds
     * (negative, greater than length(), or {@code start > end}).
     */
    @Override
    public String subSequence(int start, int end) {
        if (start < 0 || end > bicArr.length || start > end) {
            throw new IndexOutOfBoundsException(
                String.format("Start index should be >= %d (was: %d) and end index (exclusive) <= %d (was: %d)", 0,
                    start, bicArr.length, end));
        }
        return new String(bicArr, start, end - start);
    }

    /**
     * Returns the length of the BIC (8 or 11 characters).
     *
     * @return The length of the BIC character array.
     */
    @Override
    public int length() {
        return bicArr.length;
    }

    /**
     * Compares this BIC with the specified BIC for order.
     * The comparison is based on the lexicographical order of the normalized 11-character BIC strings.
     *
     * @param other The BIC to be compared.
     * @return      A negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     *
     * @since 1.8.0
     */
    @Override
    public int compareTo(Bic other) {
        Objects.requireNonNull(other, "Cannot compare Bic to null");
        // delegate comparison to the 11-character String representation
        return bic11.compareTo(other.bic11);
    }

    /**
     * Returns the raw, unformatted, normalized BIC string.
     *
     * @return The normalized BIC string (8 or 11 characters).
     */
    @Override
    public String toString() {
        return isBic8 ? bic8 : bic11;
    }

    /**
     * Compares this BIC to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code Bic} object that represents
     * the same normalized 11-character BIC string.
     *
     * @param o The object to compare with.
     * @return  {@code true} if the objects are the same; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Bic other = (Bic) o;
        // comparison is based on the normalized 11-character representation
        return bic11.equals(other.bic11);
    }

    /**
     * Returns a hash code for this BIC.
     * The hash code is based on the normalized 11-character BIC string.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        // hash code based on the normalized 11-character representation
        return Objects.hash(bic11);
    }

}
