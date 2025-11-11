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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a valid, immutable **International Bank Account Number (IBAN)**,
 * structured according to the **ISO 13616** standard.
 * <p>
 * The validity is guaranteed by successful checks on length, country-specific
 * structure (BBAN), and the **ISO 7064 Mod 97-10** checksum.
 * <p>
 * The IBAN format consists of:
 * <ul>
 *   <li>**Country Code** (2 letters, ISO 3166-1 alpha-2)</li>
 *   <li>**Check Digits** (2 numbers, calculated using ISO 7064 Mod 97-10)</li>
 *   <li>**Basic Bank Account Number (BBAN)** (country-specific length and structure)</li>
 * </ul>
 * Creation is done exclusively via static factory methods after successful validation.
 *
 * @since 1.8.0
 */
public final class Iban implements Serializable, CharSequence, Comparable<Iban> {

    private static final long           serialVersionUID = 42L;

    /** The starting index of the Basic Bank Account Number (BBAN) within the normalized IBAN (always 4). */
    private static final int            INDEX_BBAN       = 4;

    private static final Optional<Iban> EMPTY_IBAN       = Optional.empty();

    /** The raw, normalized IBAN character array (e.g., "DE91100000000123456789"). */
    private final char[]                ibanArr;

    /**
     * The raw, normalized IBAN string, created once in the constructor.<br>
     * This field is part of the object's essential state and is therefore NOT transient.
     */
    private final String                ibanStr;

    /** Reference to the applicable {@link IbanRegistry} entry holding country meta data and BBAN structure details. */
    private final IbanRegistry          countryData;

    /**
     * Caches the two IBAN check digits (positions 3 and 4) upon first access.
     * Marked as {@code transient} for serialization and {@code volatile}
     * to ensure correct lazy initialization across multiple threads.
     */
    private transient volatile String checkDigits;

    /** Caches the Basic Bank Account Number (BBAN) part upon first access. */
    private transient volatile String bban;

    /** Caches the Bank Code part of the BBAN upon first access. Can be null if the country's BBAN structure doesn't define it. */
    private transient volatile String bankCode;

    /** Caches the Branch Code part of the BBAN upon first access. Can be null if the country's BBAN structure doesn't define it. */
    private transient volatile String branchCode;

    /** Caches the Account Number part of the BBAN upon first access. */
    private transient volatile String accountNumber;

    /**
     * Package-private constructor.
     * <p>
     * Construction is restricted to static factory methods after validation and guarantees
     * the input {@code ibanArr} is valid, normalized, and correctly sized.
     *
     * @param ibanArr     The normalized, validated IBAN characters.
     * @param countryData The metadata for the country code (format, structure).
     */
    Iban(final char[] ibanArr, final IbanRegistry countryData) {
        this.ibanArr = ibanArr;
        this.ibanStr = new String(ibanArr);
        this.countryData = countryData;
    }

    /**
     * Parses and validates the input character sequence, throwing {@link InvalidIbanException} if validation fails.
     * <p>
     * Implementation note: validation logic is delegated to the separate Validator class {@link IbanValidator}.
     * <p>
     * For more information, see:
     * <a href="https://www.swift.com/de/node/301396">Official SWIFT IBAN Resources</a>
     *
     * @param iban The IBAN character sequence, may include spaces (as used in IBAN formatting) but no other whitespace or non-IBAN characters.
     * @return A valid, immutable {@code Iban} instance.
     * @throws InvalidIbanException if the IBAN is invalid for any reason (e.g., invalid length, bad format, or incorrect check digits).
     *
     * @since 1.8.0
     */
    public static Iban of(CharSequence iban) {
        return parse(iban);
    }

    /**
     * Parses and validates the input character sequence that is assumed to be normalized (no spaces, all uppercase).
     * <p>
     * This method skips the initial normalization step but performs full validation.
     *
     * @param iban The normalized IBAN character sequence.
     * @return A valid, immutable {@code Iban} instance.
     * @throws InvalidIbanException if the IBAN is invalid for any reason.
     *
     * @since 1.8.0
     */
    public static Iban ofNormalized(CharSequence iban) {
        IbanValidationSuccess success = IbanValidator.validateNormalized(iban);

        if (success == null) {
            IbanValidationError error = IbanValidator.getLastReason();
            throw InvalidIbanException.of(error);
        }

        return new Iban(success.normIbanArr, success.countryData);
    }

    /**
     * Parses and validates the input character sequence.
     *
     * @param iban The IBAN character sequence, may include spaces but no other non-IBAN characters.
     * @return A valid, immutable {@code Iban} instance.
     * @throws InvalidIbanException if the IBAN is invalid for any reason.
     *
     * @since 1.8.0
     */
    public static Iban parse(CharSequence iban) {
        IbanValidationSuccess success = IbanValidator.validate(iban);

        if (success == null) {
            IbanValidationError error = IbanValidator.getLastReason();
            throw InvalidIbanException.of(error);
        }

        return new Iban(success.normIbanArr, success.countryData);
    }

    /**
     * Attempts to parse and validate the input character sequence safely.
     *
     * @param iban The IBAN character sequence, may include spaces but no other non-IBAN characters.
     * @return An {@link Optional} containing the {@link Iban} instance if valid, or empty if invalid.
     *
     * @since 1.8.0
     */
    public static Optional<Iban> tryParse(CharSequence iban) {
        IbanValidationSuccess success = IbanValidator.validate(iban);

        return success == null
            ? EMPTY_IBAN
            : Optional.of(new Iban(success.normIbanArr, success.countryData));
    }

    /**
     * Performs a full IBAN validation and returns {@code true} if successful,
     * or {@code false} if any validation step fails.
     *
     * @param iban The IBAN character sequence to validate.
     * @return {@code true} if the IBAN is valid, {@code false} otherwise.
     *
     * @since 1.8.0
     */
    public static boolean isValid(final CharSequence iban) {
        return IbanValidator.isValid(iban);
    }

    /**
     * Returns the two-letter ISO country code (positions 1 and 2).
     * <p>
     * This is the {@code <strong>ISO 3166-1 Alpha-2</strong>} country code, which identifies the country of the bank.
     * This code forms the initial part of every IBAN.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "CR"} for Costa Rica</li>
     *   <li>{@code "DE"} for Germany</li>
     *   <li>{@code "PL"} for Poland</li>
     * </ul>
     * <p>
     * For more information, see:
     * <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166-1 Alpha-2 Specification</a>
     *
     * @return The country code (e.g., {@code "DE"}).
     *
     * @since 1.8.0
     */
    public String getCountryCode() {
        return countryData.getCountryCode();
    }

    /**
     * Returns the full English country name associated with the IBAN's country code.
     * <p>
     * Examples:
     * <ul>
     *   <li>For country code {@code "DZ"}: Algeria</li>
     *   <li>For country code {@code "PS"}: Palestine</li>
     *   <li>For country code {@code "TL"}: Timor-Leste</li>
     * </ul>
     *
     * @return The country name (e.g., {@code "Palestine"}).
     *
     * @since 1.8.0
     */
    public String getCountryName() {
        return countryData.getCountryName();
    }

    /**
     * Returns the two-character country flag emoji corresponding to the IBAN's
     * country code, based on the ISO 3166-1 Alpha-2 standard.
     * <p>
     * Examples:
     * <ul>
     *   <li>For country code {@code "AD"}: 🇦🇩 (Andorra)</li>
     *   <li>For country code {@code "IQ"}: 🇮🇶 (Iraq)</li>
     *   <li>For country code {@code "TD"}: 🇹🇩 (Chad)</li>
     * </ul>
     *
     * @return The country flag emoji string.
     *
     * @since 1.8.0
     */
    public String getCountryFlag() {
        return countryData.getCountryFlag();
    }

    /**
     * Returns the two numeric IBAN check digits (positions 3 and 4).
     * <p>
     * These digits serve as the primary checksum for IBAN validation. They are calculated using
     * the {@code MOD 97-10} algorithm and ensure that the IBAN is correctly constructed and not
     * corrupted by simple input errors.
     *
     * @return The check digits (e.g., {@code "91"}).
     *
     * @since 1.8.0
     */
    public String getCheckDigits() {
        if (checkDigits == null) {
            // lazy initialization of the check digits string
            checkDigits = new String(ibanArr, 2, 2);
        }
        return checkDigits;
    }

    /**
     * Returns the Basic Bank Account Number (BBAN), the part of the IBAN
     * that follows the country code and check digits.
     * <p>
     * The BBAN comprises the entire remainder of the IBAN. It holds all national
     * account information, typically including the Bank Code, the Branch Code (if applicable),
     * the Account Number, and in some countries, a National Check Digit (NCD), such as in Poland.
     *
     * @return The BBAN part of the IBAN.
     *
     * @since 1.8.0
     */
    public String getBban() {
        if (bban == null) {
            // lazy initialization of the BBAN string
            bban = new String(ibanArr, INDEX_BBAN, ibanArr.length - INDEX_BBAN);
        }
        return bban;
    }

    /**
     * Returns the Bank Identifier Code (Bank Code) based on the country's BBAN structure.
     * <p>
     * The Bank Code is the part of the BBAN that uniquely identifies the financial institution.
     * <ul>
     *   <li>IBAN {@code DE91100000000123456789}: Bank Code is {@code 10000000}</li>
     *   <li>IBAN {@code FR763000600001123456789018}: Bank Code is {@code 30006}</li>
     * </ul>
     *
     * @return The bank code string.
     *
     * @since 1.8.0
     */
    public String getBankCode() {
        if (bankCode == null && countryData.getBankCodeIndexRange() != null) {
            // lazy initialization based on metadata indices
            bankCode = countryData.getBankCodeIndexRange().applyTo(ibanArr);
        }
        return bankCode;
    }

    /**
     * Returns the Branch Identifier Code (Branch Code)
     * based on the country's BBAN structure.
     *
     * @return The branch code string, or {@code null} if the country does not define a separate branch code part.
     *
     * @since 1.8.0
     */
    public String getBranchCode() {
        if (branchCode == null && countryData.getBranchCodeIndexRange() != null) {
            // lazy initialization based on metadata indices
            branchCode = countryData.getBranchCodeIndexRange().applyTo(ibanArr);
        }
        return branchCode;
    }

    /**
     * Returns the combination of Bank Identifier Code (Bank Code) and Branch Identifier Code (Branch Code)
     * based on the country's BBAN structure.
     *
     * @return The bank code string including branch code.
     *
     * @since 1.8.0
     */
    public String getBankAndBranchCode() {
        return getBranchCode() == null ? getBankCode() : getBankCode() + getBranchCode();
    }

    /**
     * Returns the national account number part of the BBAN.
     * <p>
     * The Account Number is the part of the BBAN that identifies the customer's account.
     * <ul>
     *   <li>IBAN {@code DE91100000000123456789}: Account Number is {@code 0123456789} (for DE)</li>
     *   <li>IBAN {@code GB29NWBK60161331926819}: Account Number is {@code 31926819} (for GB)</li>
     * </ul>
     *
     * @return The account number string.
     *
     * @since 1.8.0
     */
    public String getAccountNumber() {
        if (accountNumber == null) {
            // lazy initialization based on metadata indices
            accountNumber = countryData.getAccountNumberIndexRange().applyTo(ibanArr);
        }
        return accountNumber;
    }

    /**
     * Returns the name of the organization or body responsible for IBAN
     * issuance and registration in the IBAN's country.
     * <p>
     * Examples:
     * <ul>
     *   <li>For country code {@code "DE"}: "Bundesverband deutscher Banken"</li>
     *   <li>For country code {@code "EG"}: "Central Bank of Egypt"</li>
     * </ul>
     * <p>
     * Note: This value may be {@code null} if the organization is not defined in the metadata.
     *
     * @return The organization's name as a string, or {@code null}.
     *
     * @since 1.8.0
     */
    public String getOrganisation() {
        return countryData.getOrganisation();
    }

    /**
     * Returns the IBAN formatted for display in groups of four characters (per ISO standard).
     * <p>
     * Formatting makes the IBAN easier to read for humans, often referred to as the **"Paper Format"** or **"Printed Form"**.
     * <p>
     * Example:
     * <pre>
     * Normalized IBAN: {@code "DE91100000000123456789"}
     * Formatted IBAN:  {@code "DE91 1000 0000 0123 4567 89"}
     * </pre>
     *
     * @return The formatted IBAN string (e.g., {@code "DE91 1000 0000 0123 4567 89"}).
     *
     * @since 1.8.0
     */
    public String toFormattedString() {
        // delegate formatting to external Formatter
        return Formatter.format(toString());
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index The index of the character to return.
     * @return      The character at the specified index.
     * @throws IndexOutOfBoundsException if the {@code index} is negative or greater than or equal to {@code length()}.
     *
     * @since 1.8.0
     */
    @Override
    public char charAt(int index) {
        if (index < 0 || index >= ibanArr.length) {
            throw new IndexOutOfBoundsException(
                String.format("Index should be between 0 (inclusive) and %d (exclusive), but was %d", ibanArr.length, index));
        }
        return ibanArr[index];
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     * <p>
     * The subsequence starts at the specified index {@code start} (inclusive) and extends to the
     * character at index {@code end - 1} (exclusive).
     * <p>
     * Note that this method provides a covariant return of {@code String} instead of {@code CharSequence}.
     *
     * @param start The start index, inclusive.
     * @param end   The end index, exclusive.
     * @return      The specified subsequence as a new {@link String} instance.
     * @throws IndexOutOfBoundsException if {@code start} or {@code end} are out of bounds
     *                                   (negative, greater than length(), or {@code start > end}).
     *
     * @since 1.8.0
     */
    @Override
    public String subSequence(int start, int end) {
        if (start < 0 || end > ibanArr.length || start > end) {
            throw new IndexOutOfBoundsException(String.format(
                "Start index should be >= %d (was: %d) and end index (exclusive) <= %d (was: %d)", 0, start, ibanArr.length, end));
        }
        return new String(ibanArr, start, end - start);
    }

    /**
     * Returns the length of the normalized IBAN (excluding spaces).
     *
     * @return The length of the IBAN character array.
     *
     * @since 1.8.0
     */
    @Override
    public int length() {
        return ibanArr.length;
    }

    /**
     * Compares this IBAN with the specified IBAN for order.
     * The comparison is based on the lexicographical order of the raw, unformatted IBAN strings.
     *
     * @param other The IBAN to be compared.
     * @return      A negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     *
     * @since 1.8.0
     */
    @Override
    public int compareTo(Iban other) {
        Objects.requireNonNull(other, "Cannot compare Iban to null");
        // delegate comparison to the String representation of the normalized IBAN
        return ibanStr.compareTo(other.ibanStr);
    }

    /**
     * Returns the raw, unformatted, normalized IBAN string.
     * <p>
     * <strong>Normalized</strong> means the IBAN consists only of uppercase letters and digits,
     * <strong>without spaces</strong> or other formatting characters.
     * <p>
     * Example: {@code "DE91100000000123456789"}
     *
     * @return The normalized IBAN string (e.g., {@code "DE91100000000123456789"}).
     */
    @Override
    public String toString() {
        return ibanStr;
    }

    /**
     * Compares this IBAN to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is an {@code Iban} object that represents
     * the same normalized IBAN string.
     *
     * @param o The object to compare with.
     * @return {@code true} if the objects are the same; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Iban other = (Iban) o;
        return ibanStr.equals(other.ibanStr);
    }

    /**
     * Returns the hash code for this IBAN.
     * <p>
     * The hash code is based on the normalized IBAN string.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return ibanStr.hashCode();
    }

}
