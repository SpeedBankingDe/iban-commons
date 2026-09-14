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
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
import de.speedbanking.iban.Iban;

import java.util.Optional;
import java.util.Set;

/**
 * Public entry point to look up {@link BankData} (BIC, bank name, address) by IBAN or by a
 * country-specific bank code.
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
     * Looks up the {@link BankData} for the institution identified by the given IBAN's bank code.
     *
     * @param iban the IBAN to resolve; must not be {@code null}
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if the
     *         institution is unknown, the country is unsupported, or no data could be loaded
     */
    public static Optional<BankData> find(Iban iban) {
        if (iban == null) {
            return Optional.empty();
        }
        return BankDataRegistry.getLoader(iban.getCountryCode())
            .flatMap(loader -> find(loader, loader.extractBankCode(iban)));
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
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param bankCode    the country-specific bank code
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if not found
     */
    public static Optional<BankData> findByBankCode(String countryCode, String bankCode) {
        return BankDataRegistry.getLoader(countryCode).flatMap(loader -> find(loader, bankCode));
    }

    private static Optional<BankData> find(CountryBankDataLoader loader, String bankCode) {
        CountryDataCache cache = BankDataRegistry.getCache(loader.getCountryCode());
        BankData result = cache.getOrLoadSync().get(bankCode);
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
