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

import de.speedbanking.util.Country;
import de.speedbanking.util.Currency;
import de.speedbanking.util.IndexRange;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
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

    private static final long         serialVersionUID = 42L;

    /**
     * The raw, normalized IBAN string, created once in the constructor (e.g., "DE91100000000123456789").
     */
    private final String              ibanStr;

    /** Reference to the applicable {@link IbanRegistry} entry holding country metadata and BBAN structure details. */
    private final IbanRegistry        countryData;

    /**
     * Caches the two IBAN check digits (positions 3 and 4) upon first access.<br>
     * Marked {@code transient} so deserialization recomputes on next access,
     * and {@code volatile} to ensure the written value is visible across threads.
     * Multiple threads may compute and write the same value; this is harmless
     * because String assignment is atomic and all threads derive the identical result.
     */
    private transient volatile String checkDigits;

    /** Caches the Basic Bank Account Number (BBAN) part upon first access. */
    private transient volatile String bban;

    /**
     * Caches the Bank Code part of the BBAN upon first access.
     * May remain {@code null} if the country's BBAN structure does not define a bank code.
     */
    private transient volatile String bankCode;

    /**
     * Caches the Branch Code part of the BBAN upon first access.
     * May remain {@code null} if the country's BBAN structure does not define a branch code.
     */
    private transient volatile String branchCode;

    /** Caches the Account Number part of the BBAN upon first access. */
    private transient volatile String accountNumber;

    /**
     * Caches the National Check Digit part of the BBAN upon first access.
     * May remain {@code null} if the country's BBAN structure does not define a national check digit.
     */
    private transient volatile String nationalCheckDigit;

    /**
     * Caches the IBAN formatted for display in groups of four characters (per ISO standard).
     */
    private transient volatile String formattedString;

    /**
     * Package-private constructor.
     * <p>
     * Construction is restricted to static factory methods after validation and guarantees
     * the input {@code ibanArr} is valid, normalized, and correctly sized.
     *
     * @param normIban    the normalized, validated IBAN characters
     * @param countryData the metadata for the country code (format, structure)
     */
    Iban(final CharSequence normIban, final IbanRegistry countryData) {
        this.ibanStr = normIban.toString();
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
     * @param iban the IBAN character sequence, may include spaces (as used in IBAN formatting) but no other whitespace or non-IBAN characters
     * @return a valid, immutable {@code Iban} instance
     * @throws InvalidIbanException if the IBAN is invalid for any reason (e.g., invalid length, bad format, or incorrect check digits)
     *
     * @since 1.8.0
     */
    public static Iban of(CharSequence iban) throws InvalidIbanException {
        return parse(iban);
    }

    /**
     * {@link String}-optimized overload of {@link #of(CharSequence)}.
     *
     * @param iban the IBAN string
     * @return a valid, immutable {@code Iban} instance
     * @throws InvalidIbanException if the IBAN is invalid
     * @see #of(CharSequence)
     * @since 1.8.5
     */
    public static Iban of(final String iban) throws InvalidIbanException {
        return parse(iban);
    }

    /**
     * Parses and validates the input character sequence that is assumed to be normalized (containing no spaces).
     * <p>
     * This method skips the initial normalization step but performs full validation.
     *
     * @param iban the normalized IBAN character sequence
     * @return a valid, immutable {@code Iban} instance
     * @throws InvalidIbanException if the IBAN is invalid for any reason
     *
     * @since 1.8.0
     */
    public static Iban ofNormalized(CharSequence iban) throws InvalidIbanException {
        IbanValidationResult result = IbanValidator.validate(iban, false);

        if (!result.isValid()) {
            throw InvalidIbanException.of(result.error, iban, result.countryData == null ? null : result.countryData.getCountryCode());
        }

        return new Iban(result.normIban, result.countryData);
    }

    /**
     * Parses and validates the input character sequence in order to return an {@code Iban} instance,
     * throwing {@link InvalidIbanException} if validation fails.
     *
     * @param iban the IBAN character sequence, may include spaces but no other non-IBAN characters
     * @return a valid, immutable {@code Iban} instance
     * @throws InvalidIbanException if the IBAN is invalid for any reason
     *
     * @since 1.8.0
     */
    public static Iban parse(CharSequence iban) throws InvalidIbanException {
        IbanValidationResult result = IbanValidator.validate(iban, IbanConfig.isAllowSpace());

        if (!result.isValid()) {
            throw InvalidIbanException.of(result.error, iban, result.countryData == null ? null : result.countryData.getCountryCode());
        }

        return new Iban(result.normIban, result.countryData);
    }

    /**
     * Attempts to parse and validate the input character sequence safely.
     *
     * @param iban the IBAN character sequence, may include spaces but no other non-IBAN characters
     * @return an {@link Optional} containing the {@link Iban} instance if valid, or empty if invalid
     *
     * @since 1.8.0
     */
    public static Optional<Iban> tryParse(CharSequence iban) {
        IbanValidationResult result = IbanValidator.validate(iban, IbanConfig.isAllowSpace());

        return result.isValid()
            ? Optional.of(new Iban(result.normIban, result.countryData))
            : Optional.empty();
    }

    /**
     * Validates the input character sequence, throwing {@link InvalidIbanException} if validation
     * fails, and returning normally if it succeeds.
     * <p>
     * Unlike {@link #parse(CharSequence)}, this method does not allocate an {@code Iban} instance,
     * making it the preferred choice when the validated value is not needed afterwards — for
     * example in Bean Validation constraints or simple guard checks.
     *
     * @param iban the IBAN character sequence, may include spaces if {@link IbanConfig#isAllowSpace()} is {@code true}
     *             but no other non-IBAN characters
     * @throws InvalidIbanException if the IBAN is invalid for any reason
     *
     * @since 1.8.7
     */
    public static void validate(final CharSequence iban) throws InvalidIbanException {
        IbanValidationResult result = IbanValidator.validate(iban, IbanConfig.isAllowSpace());
        if (!result.isValid()) {
            throw InvalidIbanException.of(result.error, iban, result.countryData == null ? null : result.countryData.getCountryCode());
        }
    }

    /**
     * Performs a full IBAN validation and returns {@code true} if successful,
     * or {@code false} if any validation step fails.
     *
     * @param iban the IBAN character sequence to validate
     * @return {@code true} if the IBAN is valid, {@code false} otherwise
     *
     * @since 1.8.0
     */
    public static boolean isValid(final CharSequence iban) {
        return IbanValidator.isValid(iban);
    }

    /**
     * {@link String}-optimized overload of {@link #isValid(CharSequence)}.
     * <p>
     * Resolved statically by the Java compiler when the caller passes a {@link String},
     * routing through {@link IbanValidator#isValid(String)} and its optimized
     * normalization path.
     *
     * @param iban the IBAN string to validate
     * @return {@code true} if the IBAN is valid, {@code false} otherwise
     * @see #isValid(CharSequence)
     * @since 1.8.5
     */
    public static boolean isValid(final String iban) {
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
     * <ul>
     *   <li><a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166-1 Alpha-2 Specification</a></li>
     *   <li><a href="https://en.wikipedia.org/wiki/International_Bank_Account_Number">Wikipedia: International Bank Account Number</a></li>
     * </ul>
     *
     * @return the country code (e.g., {@code "DE"})
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
     * @return the country name (e.g., {@code "Palestine"})
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
     * @return the country flag emoji string
     *
     * @since 1.8.0
     */
    public String getCountryFlag() {
        return countryData.getCountryFlag();
    }

    /**
     * Returns the primary {@link Currency} used in this country.
     * <p>
     * The currency is resolved via {@link Country#getCurrency()}, keyed by this
     * entry's ISO 3166-1 Alpha-2 country code.
     * <p>
     * Returns {@code null} for derived country codes that are not present in
     * {@link Country} (none in the current registry, but defensively handled).
     *
     * @return the {@link Currency} constant for this country, or {@code null} if unresolvable
     *
     * @since 1.8.5
     *
     * @see Country#getCurrency()
     */
    public Currency getCurrency() {
        return countryData.getCurrency();
    }

    /**
     * Returns the ISO 4217 three-letter currency code for this country as a {@code String}
     * (e.g., {@code "EUR"}, {@code "GBP"}).
     * <p>
     * Convenience shorthand for {@code getCurrency().getAlphaCode()}.
     *
     * @return the currency code string
     * @throws NullPointerException if {@link #getCurrency()} returns {@code null}
     *         (see its contract for the conditions under which that can occur)
     *
     * @since 1.8.5
     */
    public String getCurrencyCode() {
        return getCurrency().getAlphaCode();
    }

    /**
     * Checks whether the country associated with this IBAN participates in the Single Euro Payments Area (SEPA).
     *
     * @return {@code true} if the country is in SEPA, {@code false} otherwise
     *
     * @since 1.8.3
     */
    public boolean isSepa() {
        return countryData.isSepa();
    }

    /**
     * Returns the two numeric IBAN check digits (positions 3 and 4).
     * <p>
     * These digits serve as the primary checksum for IBAN validation. They are calculated using
     * the {@code MOD 97-10} algorithm and ensure that the IBAN is correctly constructed and not
     * corrupted by simple input errors.
     *
     * @return the check digits (e.g., {@code "91"})
     *
     * @since 1.8.0
     */
    public String getCheckDigits() {
        if (checkDigits == null) {
            checkDigits = ibanStr.substring(IbanRegistry.INDEX_CHECK_DIGIT1, IbanRegistry.INDEX_BBAN);
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
     * @return the BBAN part of the IBAN
     *
     * @since 1.8.0
     */
    public String getBban() {
        if (bban == null) {
            bban = ibanStr.substring(IbanRegistry.INDEX_BBAN);
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
     * @return the bank code string
     *
     * @since 1.8.0
     */
    public String getBankCode() {
        if (bankCode == null && countryData.getBankCodeIndexRange() != null) {
            bankCode = countryData.getBankCodeIndexRange().applyTo(ibanStr);
        }
        return bankCode;
    }

    /**
     * Returns the Branch Identifier Code (Branch Code) based on the country's BBAN structure, if present, or {@code null}.
     *
     * @return the branch code string, or {@code null} if the country does not define a separate branch code part
     *
     * @since 1.8.0
     */
    public String getBranchCode() {
        if (branchCode == null && countryData.getBranchCodeIndexRange() != null) {
            branchCode = countryData.getBranchCodeIndexRange().applyTo(ibanStr);
        }
        return branchCode;
    }

    /**
     * Returns the combination of Bank Identifier Code (Bank Code) and Branch Identifier Code (Branch Code)
     * based on the country's BBAN structure.
     *
     * @return the bank code string including branch code
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
     * @return the account number string
     *
     * @since 1.8.0
     */
    public String getAccountNumber() {
        if (accountNumber == null) {
            accountNumber = countryData.getAccountNumberIndexRange().applyTo(ibanStr);
        }
        return accountNumber;
    }

    /**
     * Returns the national check digit (NCD) part of the BBAN, if present, or {@code null}.
     *
     * @return the national check digit (NCD) string
     *
     * @since 1.8.1
     */
    public String getNationalCheckDigit() {
        if (nationalCheckDigit == null && countryData.hasNationalCheckDigit()) {
            nationalCheckDigit = countryData.getNationalCheckDigitIndexRange().applyTo(ibanStr);
        }
        return nationalCheckDigit;
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
     * @return the organization's name as a string, or {@code null}
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
     * @return the formatted IBAN string (e.g., {@code "DE91 1000 0000 0123 4567 89"})
     *
     * @since 1.8.0
     */
    public String toFormattedString() {
        if (formattedString == null) {
            // delegate formatting to external Formatter
            formattedString = Formatter.format(toString());
        }
        return formattedString;
    }

    /**
     * Returns the IBAN formatted into its components in order.
     * <p>
     * Gap areas between known components (e.g. unknown sub-components) are
     * identified automatically based on character index ranges and separated accordingly.
     * <p>
     * Example:
     * <pre>
     * IBAN:      {@code "GL8964710001000206"}
     * Formatted: {@code "GL 89 6471 0001000206"}
     *
     * IBAN:      {@code "PL61109010140000071219812874"}
     * Formatted: {@code "PL 61 109 0101 4 0000071219812874"}
     * </pre>
     *
     * @return the IBAN formatted into components (e.g., {@code "AT 61 19043 00234573201"})
     *
     * @since 1.8.5
     */
    public String toComponentString() {
        IndexRange bankRange = countryData.getBankCodeIndexRange();
        IndexRange branchRange = countryData.getBranchCodeIndexRange();
        IndexRange accountRange = countryData.getAccountNumberIndexRange();
        IndexRange ncdRange = countryData.getNationalCheckDigitIndexRange();

        // fixed-size stack array (max 12 entries)
        int[] idx = new int[12];
        int count = 0;
        idx[count++] = 0;
        idx[count++] = IbanRegistry.INDEX_CHECK_DIGIT1;
        idx[count++] = IbanRegistry.INDEX_BBAN;

        idx[count++] = bankRange.getBegin();
        idx[count++] = bankRange.getEnd();

        if (branchRange != null) {
            idx[count++] = branchRange.getBegin();
            idx[count++] = branchRange.getEnd();
        }

        idx[count++] = accountRange.getBegin();
        idx[count++] = accountRange.getEnd();

        if (ncdRange != null) {
            idx[count++] = ncdRange.getBegin();
            idx[count++] = ncdRange.getEnd();
        }

        // insertion sort - optimal for small fixed arrays, avoids Arrays.sort overhead
        for (int i = 1; i < count; i++) {
            int key = idx[i];
            int j = i - 1;
            while (j >= 0 && idx[j] > key) {
                idx[j + 1] = idx[j--];
            }
            idx[j + 1] = key;
        }

        // build result, skipping duplicate cut points inline
        StringBuilder sb = new StringBuilder(ibanStr.length() + count);
        int last = 0;
        int prev = -1;
        for (int i = 0; i < count; i++) {
            int cut = idx[i];
            if (cut <= last || cut == prev) {
                continue;
            }
            if (cut > ibanStr.length()) {
                break;
            }
            sb.append(ibanStr, last, cut).append(' ');
            last = cut;
            prev = cut;
        }

        if (last < ibanStr.length()) {
            sb.append(ibanStr, last, ibanStr.length());
        } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index the index of the character to return
     * @return the character at the specified index
     * @throws IndexOutOfBoundsException if the {@code index} is negative or greater than or equal to {@code length()}
     *
     * @since 1.8.0
     */
    @Override
    public char charAt(int index) {
        // bounds check delegated to String class
        return ibanStr.charAt(index);
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     * <p>
     * The subsequence starts at the specified index {@code start} (inclusive) and extends to the
     * character at index {@code end - 1} (exclusive).
     * <p>
     * Note that this method provides a covariant return of {@code String} instead of {@code CharSequence}.
     *
     * @param start the start index, inclusive
     * @param end   the end index, exclusive
     * @return the specified subsequence as a new {@link String} instance
     * @throws IndexOutOfBoundsException if {@code start} or {@code end} are out of bounds
     *                                   (negative, greater than length(), or {@code start > end})
     *
     * @since 1.8.0
     */
    @Override
    public String subSequence(int start, int end) {
        // bounds check delegated to String class
        return ibanStr.substring(start, end);
    }

    /**
     * Returns the length of the normalized IBAN (excluding spaces).
     *
     * @return the length of the IBAN character array
     *
     * @since 1.8.0
     */
    @Override
    public int length() {
        return countryData.getIbanLength();
    }

    /**
     * Compares this IBAN with the specified IBAN for order.
     * <p>
     * The comparison is based on the lexicographical order of the raw, unformatted IBAN strings.
     *
     * @param other the IBAN to be compared
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object
     *
     * @since 1.8.0
     */
    @Override
    public int compareTo(Iban other) {
        requireNonNull(other, "Cannot compare Iban to null");
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
     * @return the normalized IBAN string (e.g., {@code "DE91100000000123456789"})
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
     * @param o the object to compare with
     * @return {@code true} if the objects are the same; {@code false} otherwise
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
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return ibanStr.hashCode();
    }

    /**
     * Substitutes this instance with a lightweight {@link Memento} during Java serialization.
     * <p>
     * This ensures that the validated, normalized IBAN string is the only data written to
     * the stream, and that deserialization always re-validates through {@link #parse(CharSequence)}.
     * No public no-arg constructor or mutable fields are required on this class.
     *
     * @return a {@link Memento} carrying the normalized IBAN string
     * @throws ObjectStreamException never
     *
     * @since 1.8.3
     */
    private Object writeReplace() throws ObjectStreamException {
        return new Memento(ibanStr);
    }

    /**
     * Blocks direct deserialization of {@code Iban} instances.
     * <p>
     * {@code Iban} objects are never written directly to a stream - only their
     * {@link Memento} proxy is. If a raw {@code Iban} is encountered in a stream
     * (e.g., from a manipulated byte payload), deserialization is rejected.
     *
     * @param stream ignored
     * @throws InvalidObjectException always
     *
     * @since 1.8.3
     */
    private void readObject(final ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException(
            Iban.class.getSimpleName() + " must be deserialized via its " + Memento.class.getSimpleName() + " proxy");
    }

    /**
     * Blocks deserialization when no instance data is present in the stream.
     * <p>
     * Called by the JVM serialization framework; never invoked directly.
     *
     * @throws InvalidObjectException always
     *
     * @since 1.8.3
     */
    @SuppressWarnings("unused") // invoked exclusively by the Java serialization framework
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException(
            Iban.class.getSimpleName() + " must be deserialized via its " + Memento.class.getSimpleName() + " proxy");
    }

    /**
     * Serialization proxy for {@link Iban}.
     * <p>
     * Implements the <em>Serialization Proxy Pattern</em>: during serialization,
     * {@link Iban#writeReplace()} substitutes the {@code Iban} instance with this
     * lightweight carrier. During deserialization, {@link #readResolve()} reconstructs
     * the {@code Iban} by calling {@link Iban#parse(CharSequence)}, which runs the full
     * validation pipeline. This guarantees that:
     * <ul>
     *   <li>No invalid {@code Iban} object can be injected via a crafted byte stream.</li>
     *   <li>The {@code Iban} class needs neither a public no-arg constructor nor mutable fields.</li>
     *   <li>The serialized form remains stable across library versions (only the IBAN string is stored).</li>
     * </ul>
     * <p>
     * A custom {@code writeObject}/{@code readObject} pair is used deliberately:
     * an explicit stream-version {@code long} is written before the IBAN string,
     * enabling future format evolution while maintaining backward-compatible deserialization.
     * <p>
     * Clients should never reference or instantiate this class directly.
     *
     * @since 1.8.3
     */
    static final class Memento implements Serializable {

        private static final long serialVersionUID = 1L;

        /** The single supported stream format version. */
        private static final long STREAM_VERSION   = 1L;

        /** The normalized IBAN string carried across the serialization boundary. */
        private String            value;

        /** No-arg constructor required by Java serialization. */
        Memento() {
        }

        Memento(final String value) {
            this();
            this.value = value;
        }

        /**
         * Writes the stream-format version followed by the normalized IBAN string.
         *
         * @param out the object output stream
         * @throws IOException if an I/O error occurs
         */
        private void writeObject(final ObjectOutputStream out) throws IOException {
            out.writeLong(STREAM_VERSION);
            out.writeUTF(value);
        }

        /**
         * Reads the stream-format version and the normalized IBAN string.
         * Rejects any stream whose version does not match {@link #STREAM_VERSION}.
         *
         * @param in the object input stream
         * @throws InvalidObjectException if an I/O error occurs or the version is unsupported
         */
        private void readObject(final ObjectInputStream in) throws IOException {
            final long version = in.readLong();
            if (version != STREAM_VERSION) {
                throw new InvalidObjectException("Unsupported Iban Memento stream version: " + version);
            }
            this.value = in.readUTF();
        }

        /**
         * Reconstructs the {@link Iban} instance after deserialization by running full validation.
         *
         * @return the validated, immutable {@link Iban} instance
         * @throws InvalidObjectException if the stored IBAN string fails validation
         */
        private Object readResolve() throws InvalidObjectException {
            try {
                return parse(this.value);
            } catch (final RuntimeException ex) {
                final InvalidObjectException ioe =
                    new InvalidObjectException("Cannot restore " + Iban.class.getSimpleName() + " from serialized form: " + ex.getMessage());
                ioe.initCause(ex);
                throw ioe;
            }
        }
    }

}
