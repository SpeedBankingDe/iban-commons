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

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import de.speedbanking.util.CountryUtil;
import de.speedbanking.util.Currency;
import de.speedbanking.util.Iso3166Alpha2;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Optional;

/**
 * Represents a valid, immutable Business Identifier Code (BIC), also known as SWIFT Code (ISO 9362).
 * <p>
 * Creation is done exclusively via static factory methods after successful validation.
 * <p>
 * Internally, only the normalized BIC-8 string is stored eagerly. The BIC-11 string and
 * all component strings (bank code, country code, location code, branch code) are derived
 * lazily on first access and then cached.
 * <p>
 * <strong>BIC-8 and BIC-11 equality:</strong> Two {@code Bic} instances are considered equal
 * when their 11-character representations match. This means {@code Bic.of("DEUTDEFF")} and
 * {@code Bic.of("DEUTDEFFXXX")} are equal and produce the same {@link #hashCode()}.
 * Consequently, a BIC-8 and a BIC-11 with the {@code XXX} head-office suffix have different
 * {@link #length()} values but the same {@link #equals} and {@link #hashCode} result.
 * <p>
 * For more information, see:
 * <a href="https://en.wikipedia.org/wiki/ISO_9362">Wikipedia: ISO 9362</a>
 *
 * @since 1.8.0
 */
public final class Bic implements Serializable, CharSequence, Comparable<Bic> {

    private static final long         serialVersionUID    = 42L;

    /** The mandatory {@code "XXX"} suffix appended to a BIC-8 to form a BIC-11. */
    public static final String        HEAD_OFFICE_SUFFIX  = "XXX";

    /** The fixed length of a BIC without branch code. */
    public static final int           BIC8_LENGTH         = 8;

    /** The fixed length of a BIC with branch code. */
    public static final int           BIC11_LENGTH        = BIC8_LENGTH + HEAD_OFFICE_SUFFIX.length();

    /** Start index of the Institution Code (Bank Code) within a BIC string ({@value}). */
    public static final int           BANK_CODE_START     = 0;

    /** Start index of the Country Code within a BIC string ({@value}). */
    public static final int           COUNTRY_CODE_START  = 4;

    /** Start index of the Location Code within a BIC string ({@value}). */
    public static final int           LOCATION_CODE_START = 6;

    /** Start index of the Branch Code within a BIC-11 string ({@value}). */
    public static final int           BRANCH_CODE_START   = 8;

    /**
     * The normalized BIC-8 string — the single source of truth for this instance.
     * <p>
     * All other string representations and component extractions are derived from
     * this field on demand.
     */
    private final String              bic8;

    /** {@code true} if the object represents a BIC-8. */
    private final boolean             isBic8;

    /**
     * The normalized BIC-11 string.
     */
    private final String              bic11;

    // lazily derived strings — transient so they are re-derived after deserialization
    private transient volatile String bankCode;
    private transient volatile String countryCode;
    private transient volatile String locationCode;

    /**
     * Branch code, only present on BIC-11, {@code null} for BIC-8.
     */
    private final String              branchCode;

    /**
     * Package-private constructor.
     * <p>
     * Construction is restricted to {@link BicValidator} which guarantees
     * the input character sequence {@code csBic} is valid, normalized,
     * and correctly sized (8 or 11 characters long).
     * <p>
     * For BIC-11 input the branch code is stored eagerly so that {@link #toBic11()}
     * can reconstruct the full string without retaining the original {@code char[]}.
     *
     * @param bicInput the normalized, validated sequence of BIC characters
     */
    Bic(CharSequence bicInput) {
        this.isBic8 = bicInput.length() == BIC8_LENGTH;

        if (this.isBic8) {
            this.bic8 = bicInput.toString();
            this.bic11 = null;
            this.branchCode = null;
        } else {
            this.bic8 = bicInput.subSequence(0, BIC8_LENGTH).toString();
            this.bic11 = bicInput.toString();
            this.branchCode = bicInput.subSequence(BRANCH_CODE_START, BIC11_LENGTH).toString();
        }
    }

    /**
     * Parses and validates the input character sequence, throwing {@link InvalidBicException} if validation fails.
     *
     * @param bic the BIC character sequence
     * @return a valid, immutable {@code Bic} instance
     * @throws InvalidBicException if the BIC is invalid
     *
     * @since 1.8.0
     */
    public static Bic of(CharSequence bic) throws InvalidBicException {
        return parse(bic);
    }

    /**
     * Parses and validates the input character sequence, throwing {@link InvalidBicException} if validation fails.
     *
     * @param bic the BIC character sequence
     * @return a valid, immutable {@code Bic} instance
     * @throws InvalidBicException if the BIC is invalid
     *
     * @since 1.8.6
     */
    public static Bic parse(CharSequence bic) throws InvalidBicException {
        BicValidationResult result = BicValidator.validate(bic);
        return result.getBic().map(Bic::new)
            .orElseThrow(() -> InvalidBicException.of(result.getError().orElse(null), bic));
    }

    /**
     * Attempts to parse and validate the input character sequence safely.
     *
     * @param bic the BIC character sequence
     * @return an {@link Optional} containing the {@link Bic} instance if valid, or empty if invalid
     *
     * @since 1.8.0
     */
    public static Optional<Bic> tryParse(CharSequence bic) {
        return BicValidator.validate(bic).getBic().map(Bic::new);
    }

    /**
     * Performs a full BIC validation and returns {@code true} if successful.
     *
     * @param bic the BIC character sequence to validate
     * @return {@code true} if the BIC is valid, {@code false} otherwise
     *
     * @since 1.8.0
     */
    public static boolean isValid(final CharSequence bic) {
        return BicValidator.validate(bic).isValid();
    }

    /**
     * Checks if this BIC is 8 characters long (without explicit branch code).
     *
     * @return {@code true} if BIC-8, {@code false} otherwise
     *
     * @since 1.8.0
     */
    public boolean isBic8() {
        return isBic8;
    }

    /**
     * Checks if this BIC is 11 characters long (with explicit branch code).
     *
     * @return {@code true} if BIC-11, {@code false} otherwise
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
     * @return the Bank Code (e.g., {@code "VGAG"})
     *
     * @since 1.8.0
     */
    public String getBankCode() {
        if (bankCode == null) {
            bankCode = bic8.substring(BANK_CODE_START, COUNTRY_CODE_START);
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
     * @return the Country Code (e.g., {@code "EC"})
     *
     * @since 1.8.0
     */
    public String getCountryCode() {
        if (countryCode == null) {
            countryCode = bic8.substring(COUNTRY_CODE_START, LOCATION_CODE_START);
        }
        return countryCode;
    }

    /**
     * Returns the full English country name associated with this BIC's country code.
     * <p>
     * Resolved via {@link Iso3166Alpha2#getCountryName()}.
     * <p>
     * Examples:
     * <ul>
     *   <li>For country code {@code "DE"}: {@code "Germany"}</li>
     *   <li>For country code {@code "JP"}: {@code "Japan"}</li>
     * </ul>
     *
     * @return the country name
     *
     * @since 1.8.5
     */
    public String getCountryName() {
        return Iso3166Alpha2.fromCode(getCountryCode()).getCountryName();
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
     * @return the country flag emoji string
     *
     * @since 1.8.0
     */
    public String getCountryFlag() {
        return CountryUtil.createFlagEmoji(getCountryCode());
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
     * @return the Location Code (e.g., {@code "FF"})
     *
     * @since 1.8.0
     */
    public String getLocationCode() {
        if (locationCode == null) {
            locationCode = bic8.substring(LOCATION_CODE_START, BRANCH_CODE_START);
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
     * @return the Branch Code (e.g., {@code "XXX"}), or {@code null} if the BIC is a BIC-8 (8 characters long)
     *
     * @since 1.8.0
     */
    public String getBranchCode() {
        return branchCode;
    }

    /**
     * Returns the primary {@link Currency} used in the country
     * associated with this BIC.
     * <p>
     * Resolved via {@link Iso3166Alpha2#getCurrency()}.
     *
     * @return the {@link Currency} constant for this country
     *
     * @since 1.8.5
     */
    public Currency getCurrency() {
        return Iso3166Alpha2.fromCode(getCountryCode()).getCurrency();
    }

    /**
     * Returns the ISO 4217 three-letter currency code for the country associated with this BIC
     * (e.g., {@code "EUR"}, {@code "GBP"}).
     * <p>
     * Convenience shorthand for {@code getCurrency().getAlphaCode()}.
     *
     * @return the currency code string
     *
     * @since 1.8.5
     */
    public String getCurrencyCode() {
        return getCurrency().getAlphaCode();
    }

    /**
     * Returns the BIC in its 8-character format. If the original BIC is 11 characters,
     * the branch code is truncated.
     *
     * @return the BIC-8 string
     *
     * @since 1.8.0
     */
    public String toBic8() {
        return bic8;
    }

    /**
     * Returns the BIC in its 11-character format.<br>
     * If the original BIC is 8 characters, the <strong>{@code "XXX"}</strong>
     * (head office) suffix is appended.
     *
     * @return the BIC-11 string
     *
     * @since 1.8.0
     */
    public String toBic11() {
        return this.isBic8 ? bic8 + HEAD_OFFICE_SUFFIX : bic11;
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index the index of the character to return
     * @return the character at the specified index
     * @throws IndexOutOfBoundsException if the {@code index} is negative or greater than or equal to {@code length()}
     */
    @Override
    public char charAt(int index) {
        // delegate to bic8 for indices 0–7; for BIC-11 indices 8–10 read from bic11
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException(String
                .format("Index should be between 0 (inclusive) and %d (exclusive), but was %d", length(), index));
        }
        return index < BIC8_LENGTH ? bic8.charAt(index) : toBic11().charAt(index);
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     *
     * @param start the start index, inclusive
     * @param end   the end index, exclusive
     * @return the specified subsequence as a new {@link String} instance
     * @throws IndexOutOfBoundsException if {@code start} or {@code end} are out of bounds
     *         (negative, greater than length(), or {@code start > end})
     */
    @Override
    public String subSequence(int start, int end) {
        final int len = length();
        if (start < 0 || end > len || start > end) {
            throw new IndexOutOfBoundsException(
                String.format("Start index should be >= %d (was: %d) and end index (exclusive) <= %d (was: %d)", 0,
                    start, len, end));
        }
        // fast path: range falls entirely within bic8
        if (end <= BIC8_LENGTH) {
            return bic8.substring(start, end);
        }
        return toBic11().substring(start, end);
    }

    /**
     * Returns the length of the BIC (8 or 11 characters).
     *
     * @return the length of the BIC string
     */
    @Override
    public int length() {
        return isBic8 ? BIC8_LENGTH : BIC11_LENGTH;
    }

    /**
     * Compares this BIC with the specified BIC for order.
     * The comparison is based on the lexicographical order of the normalized 11-character BIC strings.
     *
     * @param other the BIC to be compared
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object
     *
     * @since 1.8.0
     */
    @Override
    public int compareTo(Bic other) {
        requireNonNull(other, "Cannot compare Bic to null");
        // delegate comparison to the 11-character String representation
        return toBic11().compareTo(other.toBic11());
    }

    /**
     * Returns the raw, unformatted, normalized BIC string.
     *
     * @return the normalized BIC string (8 or 11 characters)
     */
    @Override
    public String toString() {
        return isBic8 ? bic8 : toBic11();
    }

    /**
     * Compares this BIC to the specified object. The result is {@code true} if and only if
     * the argument is not {@code null} and is a {@code Bic} object that represents
     * the same normalized 11-character BIC string.
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
        // comparison is based on the normalized 11-character representation
        return toBic11().equals(((Bic) o).toBic11());
    }

    /**
     * Returns a hash code for this BIC.
     * The hash code is based on the normalized 11-character BIC string.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return hash(toBic11());
    }

    // -------------------------------------------------------------------------
    // Serialization - Memento Pattern
    // -------------------------------------------------------------------------

    /**
     * Substitutes this instance with a lightweight {@link Memento} during Java serialization.
     * <p>
     * This ensures that only the normalized BIC string crosses the serialization boundary,
     * and that deserialization always re-validates through {@link #of(CharSequence)}.
     * No public no-arg constructor or mutable fields are required on this class.
     *
     * @return a {@link Memento} carrying the normalized BIC string
     * @throws ObjectStreamException never
     *
     * @since 1.8.3
     */
    private Object writeReplace() throws ObjectStreamException {
        return new Memento(toString());
    }

    /**
     * Blocks direct deserialization of {@code Bic} instances.
     * <p>
     * {@code Bic} objects are never written directly to a stream -- only their
     * {@link Memento} proxy is. If a raw {@code Bic} is encountered in a stream
     * (e.g., from a manipulated byte payload), deserialization is rejected.
     *
     * @param stream ignored
     * @throws InvalidObjectException always
     *
     * @since 1.8.3
     */
    private void readObject(final ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException(
            Bic.class.getSimpleName() + " must be deserialized via its " + Memento.class.getSimpleName() + " proxy");
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
            Bic.class.getSimpleName() + " must be deserialized via its " + Memento.class.getSimpleName() + " proxy");
    }

    /**
     * Serialization proxy for {@link Bic}.
     * <p>
     * Implements the Serialization Proxy Pattern: during serialization,
     * {@link Bic#writeReplace()} substitutes the {@code Bic} instance with this
     * lightweight carrier. During deserialization, {@link #readResolve()} reconstructs
     * the {@code Bic} by calling {@link Bic#of(CharSequence)}, which runs the full
     * validation pipeline. This guarantees that:
     * <ul>
     *   <li>No invalid {@code Bic} object can be injected via a crafted byte stream.</li>
     *   <li>The {@code Bic} class needs neither a public no-arg constructor nor mutable fields.</li>
     *   <li>The serialized form remains stable across library versions (only the BIC string is stored).</li>
     * </ul>
     * <p>
     * A custom {@code writeObject}/{@code readObject} pair is used deliberately:
     * an explicit stream-version {@code long} is written before the BIC string,
     * enabling future format evolution while maintaining backward-compatible deserialization.
     * <p>
     * Note: {@code toString()} returns the original input form (BIC-8 or BIC-11). The restored
     * {@code Bic} instance therefore preserves whether the original was a BIC-8 or BIC-11.
     * <p>
     * Clients should never reference or instantiate this class directly.
     *
     * @since 1.8.3
     */
    static final class Memento implements Serializable {

        private static final long serialVersionUID = 1L;

        /** The single supported stream format version. */
        private static final long STREAM_VERSION   = 1L;

        /** The normalized BIC string carried across the serialization boundary. */
        private String            value;

        /** No-arg constructor required by Java serialization. */
        Memento() {
        }

        Memento(final String value) {
            this();
            this.value = value;
        }

        /**
         * Writes the stream-format version followed by the normalized BIC string.
         *
         * @param out the object output stream
         * @throws IOException if an I/O error occurs
         */
        private void writeObject(final ObjectOutputStream out) throws IOException {
            out.writeLong(STREAM_VERSION);
            out.writeUTF(value);
        }

        /**
         * Reads the stream-format version and the normalized BIC string.
         * Rejects any stream whose version does not match {@link #STREAM_VERSION}.
         *
         * @param in the object input stream
         * @throws InvalidObjectException if the version is unsupported
         * @throws IOException            if an I/O error occurs
         */
        private void readObject(final ObjectInputStream in) throws IOException {
            final long version = in.readLong();
            if (version != STREAM_VERSION) {
                throw new InvalidObjectException("Unsupported Bic Memento stream version: " + version);
            }
            this.value = in.readUTF();
        }

        /**
         * Reconstructs the {@link Bic} instance after deserialization by running full validation.
         *
         * @return the validated, immutable {@link Bic} instance
         * @throws InvalidObjectException if the stored BIC string fails validation
         */
        private Object readResolve() throws InvalidObjectException {
            try {
                return of(this.value);
            } catch (final RuntimeException ex) {
                final InvalidObjectException ioe = new InvalidObjectException(
                    "Cannot restore " + Bic.class.getSimpleName() + " from serialized form: " + ex.getMessage());
                ioe.initCause(ex);
                throw ioe;
            }
        }
    }

}
