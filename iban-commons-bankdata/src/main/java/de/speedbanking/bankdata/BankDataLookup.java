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
import de.speedbanking.bic.Bic;
import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanPlusKey;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Public entry point to look up {@link BankData} (BIC, bank name, address) by IBAN or by a
 * country-specific bank code, optionally combined with a branch code.
 * <p>
 * A bank code alone only identifies a single institution in countries whose BBAN has no branch
 * code, or where the branch code is not needed to disambiguate institutions. For a country whose
 * BBAN does carry a branch code that matters (see {@link BankData#getKey()}), {@link
 * #byIban(Iban)}/{@link #byIban(String)} (which derive the same {@link IbanPlusKey} the SWIFT IBAN
 * Plus service itself uses) or {@link #byBankCode(String, String, String)} with an explicit
 * branch code are needed to resolve a single, correct record. A lookup that includes a branch code
 * falls back to the bare bank code if no record matches the full key, so a country whose loaded
 * source only distinguishes institutions at the bank level (e.g. {@code ES}/{@code FR}) still
 * resolves correctly even though its BBAN structurally carries a branch code.
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
    public static Optional<BankData> byIban(Iban iban) {
        if (iban == null) {
            return Optional.empty();
        }
        return find(iban.getCountryCode(), IbanPlusKey.of(iban), iban.getBankCode());
    }

    /**
     * Parses the given IBAN string and looks up its {@link BankData}.
     *
     * @param ibanStr the IBAN string to parse
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if parsing
     *         failed or the institution could not be resolved
     */
    public static Optional<BankData> byIban(String ibanStr) {
        return Iban.tryParse(ibanStr).flatMap(BankDataLookup::byIban);
    }

    /**
     * Looks up the {@link BankData} for the given country and country-specific bank code directly
     * (e.g. an 8-digit German BLZ), without going through an IBAN.
     * <p>
     * Only resolves a single, correct record if the bank code alone is unique in this country; use
     * {@link #byBankCode(String, String, String)} for a country whose BBAN carries a branch code
     * that matters.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @param bankCode    the country-specific bank code
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if not found
     */
    public static Optional<BankData> byBankCode(String countryCode, String bankCode) {
        return byBankCode(countryCode, bankCode, null);
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
    public static Optional<BankData> byBankCode(String countryCode, String bankCode, String branchCode) {
        String key = branchCode != null ? bankCode + branchCode : bankCode;
        return find(countryCode, key, bankCode);
    }

    /**
     * Looks up the given key, falling back to the bare bank code if the full key (bank code plus
     * branch code/NCD) isn't found. The fallback matters for a country whose BBAN structurally
     * carries a branch code, but whose loaded source only distinguishes institutions at the bank
     * level (e.g. {@code ES}/{@code FR}, sourced from the ECB's list of financial institutions):
     * such records are stored under their bare bank code, so a full routing key would otherwise
     * never match.
     */
    private static Optional<BankData> find(String countryCode, String key, String bankCode) {
        if (key == null || !BankDataRegistry.isCountrySupported(countryCode)) {
            return Optional.empty();
        }
        CountryDataCache cache = BankDataRegistry.getCache(countryCode);
        Map<String, BankData> records = cache.getOrLoadSync();
        BankData result = records.get(key);
        if (result == null && bankCode != null && !bankCode.equals(key)) {
            result = records.get(bankCode);
        }
        cache.triggerAsyncRefreshIfStale();
        return Optional.ofNullable(result);
    }

    /**
     * Looks up the {@link BankData} for the institution identified by the given BIC, using the
     * BIC's own country code (characters 5-6) to pick the country whose bank data to search - no
     * separate country argument needed.
     * <p>
     * Unlike {@link #byIban(Iban)}/{@link #byBankCode(String, String)}, which resolve their key in
     * constant time, this scans every record loaded for the BIC's country, since none of the
     * per-country loaders publish a BIC-indexed lookup.
     * <p>
     * A BIC is not always unique to a single bank code: several institutions publish the same head
     * office BIC across multiple {@link BankData#getBankCode()} entries (e.g. regional branches of
     * the same bank). In that case an arbitrary one of the matching records is returned; only
     * {@link BankData#getBic()} and {@link BankData#getBankName()} are guaranteed to be correct for
     * the given BIC, not necessarily {@link BankData#getBankCode()}/{@link BankData#getBranchCode()}.
     *
     * @param bic the BIC to resolve; must not be {@code null}
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if no record
     *         for this BIC's country carries this exact BIC, the country is unsupported, or no data
     *         could be loaded
     */
    public static Optional<BankData> byBic(Bic bic) {
        if (bic == null) {
            return Optional.empty();
        }
        String countryCode = bic.getCountryCode();
        if (!BankDataRegistry.isCountrySupported(countryCode)) {
            return Optional.empty();
        }
        CountryDataCache cache = BankDataRegistry.getCache(countryCode);
        Optional<BankData> result = cache.getOrLoadSync().values().stream()
            .filter(bankData -> bic.equals(bankData.getBic()))
            .findFirst();
        cache.triggerAsyncRefreshIfStale();
        return result;
    }

    /**
     * Parses the given BIC string and looks up its {@link BankData}.
     *
     * @param bicStr the BIC string to parse
     * @return an {@link Optional} containing the resolved {@link BankData}, or empty if parsing
     *         failed or no institution could be resolved
     */
    public static Optional<BankData> byBic(String bicStr) {
        return Bic.tryParse(bicStr).flatMap(BankDataLookup::byBic);
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
