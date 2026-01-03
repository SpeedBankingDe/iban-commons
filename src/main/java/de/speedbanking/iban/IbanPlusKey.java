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

import de.speedbanking.util.IndexRange;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class to derive a lookup key compatible with the <b>SWIFT IBAN Plus</b> service.
 * <p>
 * The IBAN Plus directory is used to facilitate BIC derivation from an IBAN by identifying
 * the routing-relevant parts of the BBAN.<br>
 * This key typically consists of the bank code, an optional branch code, and in
 * specific countries, a national check digit (NCD).
 * <p>
 * This implementation uses a pre-calculated strategy cache to determine the
 * construction rules for each country at class-loading time, ensuring maximum
 * performance during runtime by avoiding redundant metadata lookups.
 *
 * @author Markus Spann
 * @since 1.8.1
 */
public final class IbanPlusKey {

    /** Cache for pre-calculated extraction strategies per country code. */
    private static final Map<String, Strategy> STRATEGY_CACHE = Collections.unmodifiableMap(
        Arrays.stream(IbanRegistry.values())
              .collect(Collectors.toMap(
                  IbanRegistry::getCountryCode,
                  Strategy::new
              ))
    );

    private IbanPlusKey() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Generates an IBAN Plus lookup key from an {@link Iban} instance.
     * <p>
     * The generated key is used to query the SWIFT database for BIC information.
     * </p>
     *
     * @param iban the IBAN instance, must not be null
     * @return the concatenated lookup code (Bank + Branch + NCD), or null if no routing data is present
     * @throws NullPointerException if iban is null
     */
    public static String of(final Iban iban) {
        Objects.requireNonNull(iban, "IBAN required");

        Strategy strategy = STRATEGY_CACHE.get(iban.getCountryCode());
        if (strategy == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (strategy.useBankCode) {
            sb.append(iban.getBankCode());
        }
        if (strategy.useBranchCode) {
            sb.append(iban.getBranchCode());
        }
        if (strategy.useNcd) {
            sb.append(iban.getNationalCheckDigit());
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Parses the given IBAN string and generates the lookup key.
     *
     * @param ibanStr the IBAN string to parse
     * @return the lookup code, or {@code null} if parsing fails or input is invalid
     */
    public static String of(final String ibanStr) {
        return Iban.tryParse(ibanStr)
            .map(IbanPlusKey::of)
            .orElse(null);
    }

    /**
     * Internal representation of a country-specific extraction rule.<br>
     * Decisions are made based on the connectivity of BBAN segments.
     */
    private static final class Strategy {
        private final boolean useBankCode;
        private final boolean useBranchCode;
        private final boolean useNcd;

        private Strategy(final IbanRegistry registry) {
            IndexRange bankRange = registry.getBankCodeIndexRange();
            IndexRange branchRange = registry.getBranchCodeIndexRange();
            IndexRange ncdRange = registry.getNationalCheckDigitIndexRange();

            this.useBankCode = bankRange != null;
            this.useBranchCode = branchRange != null;
            this.useNcd = isNcdRoutingRelevant(bankRange, branchRange, ncdRange);
        }

        private static boolean isNcdRoutingRelevant(IndexRange bankRange, IndexRange branchRange, IndexRange ncdRange) {
            if (ncdRange == null) {
                return false;
            }
            int ncdBegin = ncdRange.getBegin();
            return (bankRange != null && ncdBegin == bankRange.getEnd())
                    || (branchRange != null && ncdBegin == branchRange.getEnd());
        }
    }

}

