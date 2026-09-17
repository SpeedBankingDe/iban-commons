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

import de.speedbanking.bankdata.refresh.CountryDataCache;
import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanPlusKey;

import java.util.Optional;
import java.util.Set;

/**
 * Public entry point to look up {@link BankData} (BIC, bank name, address) by IBAN or by a
 * country-specific bank code, optionally combined with a branch code.
 * <p>
 * A bank code alone only identifies a single institution in countries whose BBAN has no branch
 * code, or where the branch code is not needed to disambiguate institutions. For a country whose
 * BBAN does carry a branch code that matters (see {@link BankData#getKey()}), only {@link
 * #find(Iban)}/{@link #find(String)} (which derive the same {@link IbanPlusKey} the SWIFT IBAN
 * Plus service itself uses) or {@link #findByBankCode(String, String, String)} with an explicit
 * branch code are guaranteed to resolve a single, correct record.
 * <p>
 * Stateless, static-methods-only utility, in the style of {@code IbanPlusKey}. Never throws for
 * "not found" / "country not supported" / "network unreachable", these conditions all result in
 * {@link Optional#empty()}. Each successful or unsuccessful lookup also triggers a non-blocking
 * staleness check that may schedule a background refresh for the looked-up country.
 *
 * @since 1.8.11
 */
public final class BankDataLookup {

    private BankDataLookup() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Looks up the {@link BankData} for the institution identified by the given IBAN's routing
     * prefix (bank code, and branch code/national check digit where relevant), the same key {@link
     * IbanPlusKey#of(Iban)} derives.
     *
     * @param iban the IBAN to resolve; must not be {@code null}
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if the
     *         institution is unknown, the country is unsupported, or no data could be loaded
     */
    public static Optional<BankData> find(Iban iban) {
        if (iban == null) {
            return Optional.empty();
        }
        return find(iban.getCountryCode(), IbanPlusKey.of(iban));
    }

    /**
     * Parses the given IBAN string and looks up its {@link BankData}.
     *
     * @param ibanStr the IBAN string to parse
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if parsing
     *         failed or the institution could not be resolved
     */
    public static Optional<BankData> find(String ibanStr) {
        return Iban.tryParse(ibanStr).flatMap(BankDataLookup::find);
    }

    /**
     * Looks up the {@link BankData} for the given country and country-specific bank code directly
     * (e.g. an 8-digit German BLZ), without going through an IBAN.
     * <p>
     * Only resolves a single, correct record if the bank code alone is unique in this country; use
     * {@link #findByBankCode(String, String, String)} for a country whose BBAN carries a branch code
     * that matters.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param bankCode    the country-specific bank code
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if not found
     */
    public static Optional<BankData> findByBankCode(String countryCode, String bankCode) {
        return findByBankCode(countryCode, bankCode, null);
    }

    /**
     * Looks up the {@link BankData} for the given country, bank code and branch code directly,
     * without going through an IBAN. Equivalent to looking up the same key {@link IbanPlusKey#of(Iban)}
     * would derive for a real IBAN routed to bank code + branch code.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param bankCode    the country-specific bank code
     * @param branchCode  the country-specific branch code, or {@code null} if this country's BBAN has
     *                    no branch code component
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if not found
     */
    public static Optional<BankData> findByBankCode(String countryCode, String bankCode, String branchCode) {
        return find(countryCode, branchCode != null ? bankCode + branchCode : bankCode);
    }

    private static Optional<BankData> find(String countryCode, String key) {
        if (key == null || !BankDataRegistry.isCountrySupported(countryCode)) {
            return Optional.empty();
        }
        CountryDataCache cache = BankDataRegistry.getCache(countryCode);
        BankData result = cache.getOrLoadSync().get(key);
        cache.triggerAsyncRefreshIfStale();
        return Optional.ofNullable(result);
    }

    /**
     * Returns whether the given country is supported by a registered bank data loader.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @return {@code true} if the country is supported
     */
    public static boolean isCountrySupported(String countryCode) {
        return BankDataRegistry.isCountrySupported(countryCode);
    }

    /**
     * Returns the set of ISO 3166-1 alpha-2 country codes for which a bank data loader is registered.
     *
     * @return an immutable set of supported country codes
     */
    public static Set<String> getSupportedCountryCodes() {
        return BankDataRegistry.getSupportedCountryCodes();
    }

}
