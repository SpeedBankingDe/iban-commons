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
 * Utility class to derive a lookup key compatible with the <strong>SWIFT IBAN Plus</strong> service.
 * <p>
 * The IBAN Plus directory is used to facilitate BIC derivation from an IBAN by identifying
 * the routing-relevant parts of the BBAN.<br>
 * This key typically consists of the bank code, an optional branch code, and in
 * specific countries, a national check digit (NCD).
 *
 * @author Markus Spann
 * @since 1.8.1
 */
public final class IbanPlusKey {

    private IbanPlusKey() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Generates an IBAN Plus lookup key from an {@link Iban} instance.
     * <p>
     * The generated key is used to query the SWIFT database for BIC information.
     *
     * @param iban the IBAN instance
     * @return the concatenated lookup code (Bank + Branch + NCD), or {@code null} if no routing data is present
     */
    public static String of(final Iban iban) {
        if (iban == null) {
            return null;
        }

        IbanRegistry countryData = IbanRegistry.valueOf(iban.getCountryCode());

        StringBuilder sb = new StringBuilder(14)
            .append(iban.getBankCode());

        if (countryData.hasBranchCode()) {
            sb.append(iban.getBranchCode());
        }
        if (isNcdRoutingRelevant(countryData)) {
            sb.append(iban.getNationalCheckDigit());
        }

        return sb.toString();
    }

    /**
     * Parses the given IBAN string and generates the lookup key.
     *
     * @param ibanStr the IBAN string to parse
     * @return the lookup code, or {@code null} if parsing fails or input is invalid
     */
    public static String of(final String ibanStr) {
        return Iban.tryParse(ibanStr).map(IbanPlusKey::of).orElse(null);
    }

    /**
     * Checks whether the national check digit (NCD) belongs to the IBAN Plus key for the given country.
     * <p>
     * This is the case only if the NCD sits directly next to the bank code or branch code in the BBAN,
     * i.e. no other component (such as the account number) lies between them.
     *
     * @param countryData the country's IBAN registry entry
     * @return {@code true} if the country has an NCD and it directly follows the bank code or branch code
     */
    private static boolean isNcdRoutingRelevant(IbanRegistry countryData) {
        IbanComponent ncdComponent = countryData.getNationalCheckDigitComponent();
        if (ncdComponent == null) {
            return false;
        }
        IbanComponent branchComponent = countryData.getBranchCodeComponent();
        return ncdComponent.getBeginIndex() == countryData.getBankCodeComponent().getEndIndex()
            || (countryData.hasBranchCode() && ncdComponent.getBeginIndex() == branchComponent.getEndIndex());
    }

}

