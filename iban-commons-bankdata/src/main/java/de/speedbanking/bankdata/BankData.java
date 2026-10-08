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
package de.speedbanking.bankdata;

import static java.util.Objects.requireNonNull;

import de.speedbanking.bic.Bic;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Immutable master-data record describing a single bank (credit institution), as identified
 * by its country-specific bank code (e.g. German {@code BLZ}, Austrian {@code Bankleitzahl},
 * or Swiss {@code BC-Nummer}).
 * <p>
 * Instances are produced by a {@link de.speedbanking.bankdata.spi.CountryBankDataLoader} while
 * parsing a country's bank directory, and returned to callers via {@link BankDataLookup}.
 * <p>
 * Not a {@code record} because this module targets Java 8 binary compatibility.
 *
 * @since 1.8.12
 */
public final class BankData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String      countryCode;
    private final String      bankCode;
    private final String      branchCode;
    private final Bic         bic;
    private final String      bankName;
    private final String      postalCode;
    private final String      city;
    private final String      checkDigitMethod;
    private final String      sourceVersion;

    private BankData(Builder builder) {
        this.countryCode      = builder.countryCode;
        this.bankCode         = builder.bankCode;
        this.branchCode       = builder.branchCode;
        this.bic              = builder.bic;
        this.bankName         = builder.bankName;
        this.postalCode       = builder.postalCode;
        this.city             = builder.city;
        this.checkDigitMethod = builder.checkDigitMethod;
        this.sourceVersion    = builder.sourceVersion;
    }

    /**
     * Creates a new immutable bank data record for a country whose BBAN carries a branch code.
     *
     * @param countryCode   the ISO 3166-1 alpha-2 country code; must not be {@code null}
     * @param bankCode      the country-specific bank code (e.g. BLZ); must not be {@code null}
     * @param branchCode    the country-specific branch code, or {@code null} if this record already
     *                      identifies the whole institution regardless of branch
     * @param bic           the BIC of the institution, or {@code null} if the source does not provide one
     * @param bankName      the institution's name; must not be {@code null}
     * @param postalCode    the postal code of the institution's registered address, or {@code null}
     * @param city          the city of the institution's registered address, or {@code null}
     * @param sourceVersion an identifier for the data source's version/vintage; must not be {@code null}
     */
    @SuppressWarnings("java:S107")
    public BankData(String countryCode, String bankCode, String branchCode, Bic bic, String bankName,
                    String postalCode, String city, String sourceVersion) {
        this(builder(countryCode, bankCode, bankName, sourceVersion)
            .branchCode(branchCode)
            .bic(bic)
            .postalCode(postalCode)
            .city(city));
    }

    /**
     * Creates a new immutable bank data record for a country whose BBAN has no branch code component.
     *
     * @param countryCode   the ISO 3166-1 alpha-2 country code; must not be {@code null}
     * @param bankCode      the country-specific bank code (e.g. BLZ); must not be {@code null}
     * @param bic           the BIC of the institution, or {@code null} if the source does not provide one
     * @param bankName      the institution's name; must not be {@code null}
     * @param postalCode    the postal code of the institution's registered address, or {@code null}
     * @param city          the city of the institution's registered address, or {@code null}
     * @param sourceVersion an identifier for the data source's version/vintage; must not be {@code null}
     */
    public BankData(String countryCode, String bankCode, Bic bic, String bankName,
                    String postalCode, String city, String sourceVersion) {
        this(countryCode, bankCode, null, bic, bankName, postalCode, city, sourceVersion);
    }

    public BankData(String countryCode, String bankCode, Bic bic, String bankName, String sourceVersion) {
        this(countryCode, bankCode, null, bic, bankName, null, null, sourceVersion);
    }

    /**
     * Returns a new {@link Builder} for a bank data record with the given required fields; all
     * optional fields start out {@code null}.
     *
     * @param countryCode   the ISO 3166-1 alpha-2 country code; must not be {@code null}
     * @param bankCode      the country-specific bank code (e.g. BLZ); must not be {@code null}
     * @param bankName      the institution's name; must not be {@code null}
     * @param sourceVersion an identifier for the data source's version/vintage; must not be {@code null}
     * @return a new builder
     */
    public static Builder builder(String countryCode, String bankCode, String bankName, String sourceVersion) {
        return new Builder(countryCode, bankCode, bankName, sourceVersion);
    }

    /**
     * Returns the ISO 3166-1 alpha-2 country code of the institution.
     *
     * @return the country code, never {@code null}
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Returns the country-specific bank code (e.g. an 8-digit German {@code BLZ}).
     *
     * @return the bank code, never {@code null}
     */
    public String getBankCode() {
        return bankCode;
    }

    /**
     * Returns the country-specific branch code, if this country's BBAN has one.
     *
     * @return the branch code, or {@code null} if not applicable to this record's country/institution
     */
    public String getBranchCode() {
        return branchCode;
    }

    /**
     * Returns the country-specific branch code as an {@link Optional}.
     *
     * @return an {@link Optional} containing the branch code, or empty if not applicable
     */
    public Optional<String> branchCode() {
        return Optional.ofNullable(branchCode);
    }

    /**
     * Returns the lookup key this record is indexed under, the same key {@code IbanPlusKey.of(iban)}
     * would derive for a real IBAN routed to this institution: the bank code, followed by the branch
     * code if this record has one.
     *
     * @return the lookup key, never {@code null}
     */
    public String getKey() {
        return branchCode != null ? bankCode + branchCode : bankCode;
    }

    /**
     * Returns the BIC of the institution, if known.
     *
     * @return the BIC, or {@code null} if the source did not provide one
     */
    public Bic getBic() {
        return bic;
    }

    /**
     * Returns the BIC of the institution as an {@link Optional}.
     *
     * @return an {@link Optional} containing the BIC, or empty if not known
     */
    public Optional<Bic> bic() {
        return Optional.ofNullable(bic);
    }

    /**
     * Returns the institution's registered name.
     *
     * @return the bank name, never {@code null}
     */
    public String getBankName() {
        return bankName;
    }

    /**
     * Returns {@link #getBankName()} converted to a more readable display form via {@link
     * BankNameCasing#toDisplayCase(String)} - a best-effort heuristic that only changes a name
     * that is entirely upper-case in the source data, leaving a source that already mixes case
     * (and any legal-form abbreviation or run of initials within an all-caps name) untouched.
     * <p>
     * Returns {@link #getBankName()} unchanged instead if display-casing has been disabled for
     * {@link #getCountryCode()} via {@link BankDataConfig#isDisplayCasingEnabled(String)}, since
     * some countries' sources publish an official spelling that is meant to be reproduced verbatim.
     *
     * @return the display-cased bank name, or the original name if display-casing is disabled for
     *         this country; never {@code null}
     */
    public String getDisplayBankName() {
        return BankDataConfig.get().isDisplayCasingEnabled(countryCode) ? BankNameCasing.toDisplayCase(bankName) : bankName;
    }

    /**
     * Returns the postal code of the institution's registered address, if known.
     *
     * @return the postal code, or {@code null} if not known
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Returns the postal code of the institution's registered address as an {@link Optional}.
     *
     * @return an {@link Optional} containing the postal code, or empty if not known
     */
    public Optional<String> postalCode() {
        return Optional.ofNullable(postalCode);
    }

    /**
     * Returns the city of the institution's registered address, if known.
     *
     * @return the city, or {@code null} if not known
     */
    public String getCity() {
        return city;
    }

    /**
     * Returns the city of the institution's registered address as an {@link Optional}.
     *
     * @return an {@link Optional} containing the city, or empty if not known
     */
    public Optional<String> city() {
        return Optional.ofNullable(city);
    }

    /**
     * Returns the identifier of the national account check digit method the institution uses, if
     * the source publishes one. For Germany this is the two-character
     * {@code Pruefzifferberechnungsmethode} from the Bundesbank's BLZ directory (e.g. {@code "09"}
     * or {@code "A4"}); it is {@code null} for every other country.
     *
     * @return the check digit method, or {@code null} if not known
     */
    public String getCheckDigitMethod() {
        return checkDigitMethod;
    }

    /**
     * Returns the identifier of the national account check digit method as an {@link Optional}.
     *
     * @return an {@link Optional} containing the check digit method, or empty if not known
     * @see #getCheckDigitMethod()
     */
    public Optional<String> checkDigitMethod() {
        return Optional.ofNullable(checkDigitMethod);
    }

    /**
     * Returns an identifier for the vintage/version of the underlying data source
     * (e.g. the publication date of the directory this record was parsed from).
     *
     * @return the source version identifier, never {@code null}
     */
    public String getSourceVersion() {
        return sourceVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankData other = (BankData) o;
        return countryCode.equals(other.countryCode)
            && bankCode.equals(other.bankCode)
            && Objects.equals(branchCode, other.branchCode)
            && Objects.equals(bic, other.bic)
            && bankName.equals(other.bankName)
            && Objects.equals(postalCode, other.postalCode)
            && Objects.equals(city, other.city)
            && Objects.equals(checkDigitMethod, other.checkDigitMethod)
            && sourceVersion.equals(other.sourceVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, bankCode, branchCode, bic, bankName, postalCode, city, checkDigitMethod, sourceVersion);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add("countryCode=" + countryCode)
            .add("bankCode=" + bankCode)
            .add("branchCode=" + branchCode)
            .add("bic=" + bic)
            .add("bankName=" + bankName)
            .add("postalCode=" + postalCode)
            .add("city=" + city)
            .add("checkDigitMethod=" + checkDigitMethod)
            .add("sourceVersion=" + sourceVersion)
            .toString();
    }

    /**
     * Builder for {@link BankData}, obtained via {@link BankData#builder(String, String, String, String)}.
     */
    public static final class Builder {

        private final String countryCode;
        private final String bankCode;
        private final String bankName;
        private final String sourceVersion;
        private String       branchCode;
        private Bic          bic;
        private String       postalCode;
        private String       city;
        private String       checkDigitMethod;

        private Builder(String countryCode, String bankCode, String bankName, String sourceVersion) {
            this.countryCode   = requireNonNull(countryCode, "countryCode must not be null");
            this.bankCode      = requireNonNull(bankCode, "bankCode must not be null");
            this.bankName      = requireNonNull(bankName, "bankName must not be null");
            this.sourceVersion = requireNonNull(sourceVersion, "sourceVersion must not be null");
        }

        /**
         * Sets the country-specific branch code.
         *
         * @param value the branch code, or {@code null} if the record identifies the whole
         *              institution regardless of branch
         * @return this builder
         */
        public Builder branchCode(String value) {
            this.branchCode = value;
            return this;
        }

        /**
         * Sets the BIC of the institution.
         *
         * @param value the BIC, or {@code null} if the source does not provide one
         * @return this builder
         */
        public Builder bic(Bic value) {
            this.bic = value;
            return this;
        }

        /**
         * Sets the postal code of the institution's registered address.
         *
         * @param value the postal code, or {@code null}
         * @return this builder
         */
        public Builder postalCode(String value) {
            this.postalCode = value;
            return this;
        }

        /**
         * Sets the city of the institution's registered address.
         *
         * @param value the city, or {@code null}
         * @return this builder
         */
        public Builder city(String value) {
            this.city = value;
            return this;
        }

        /**
         * Sets the identifier of the national account check digit method, e.g. the German
         * {@code Pruefzifferberechnungsmethode} published in the Bundesbank's BLZ directory.
         *
         * @param value the method as published by the source (e.g. {@code "09"} or {@code "A4"} for
         *              Germany), or {@code null} if the source does not provide one
         * @return this builder
         */
        public Builder checkDigitMethod(String value) {
            this.checkDigitMethod = value;
            return this;
        }

        /**
         * Creates the immutable bank data record.
         *
         * @return a new {@link BankData}
         */
        public BankData build() {
            return new BankData(this);
        }

    }

}
