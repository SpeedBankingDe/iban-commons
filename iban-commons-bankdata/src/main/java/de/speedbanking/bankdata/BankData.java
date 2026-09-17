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
 * @since 1.8.11
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
    private final String      sourceVersion;

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
    public BankData(String countryCode, String bankCode, String branchCode, Bic bic, String bankName,
                    String postalCode, String city, String sourceVersion) {
        this.countryCode   = requireNonNull(countryCode, "countryCode must not be null");
        this.bankCode      = requireNonNull(bankCode, "bankCode must not be null");
        this.branchCode    = branchCode;
        this.bic           = bic;
        this.bankName      = requireNonNull(bankName, "bankName must not be null");
        this.postalCode    = postalCode;
        this.city          = city;
        this.sourceVersion = requireNonNull(sourceVersion, "sourceVersion must not be null");
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
            && sourceVersion.equals(other.sourceVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, bankCode, branchCode, bic, bankName, postalCode, city, sourceVersion);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + '['
               + "countryCode="   + countryCode + ", "
               + "bankCode="      + bankCode + ", "
               + "branchCode="    + branchCode + ", "
               + "bic="           + bic + ", "
               + "bankName="      + bankName + ", "
               + "postalCode="    + postalCode + ", "
               + "city="          + city + ", "
               + "sourceVersion=" + sourceVersion
               + ']';
    }

}
