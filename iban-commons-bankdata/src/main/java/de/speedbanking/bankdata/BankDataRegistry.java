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

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

import de.speedbanking.bankdata.io.Downloader;
import de.speedbanking.bankdata.loader.AtBankDataLoader;
import de.speedbanking.bankdata.loader.BeBankDataLoader;
import de.speedbanking.bankdata.loader.ChBankDataLoader;
import de.speedbanking.bankdata.loader.CzBankDataLoader;
import de.speedbanking.bankdata.loader.DeBundesbankLoader;
import de.speedbanking.bankdata.refresh.CountryDataCache;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The internal, fixed registry of all {@link CountryBankDataLoader} implementations shipped with
 * this module.
 * <p>
 * Consistent with {@code IbanRegistry}/{@code CountryValidators} elsewhere in this project: a
 * closed, internally wired map is used instead of {@link java.util.ServiceLoader}/
 * {@code META-INF/services} discovery. Third-party pluggability beyond the countries shipped here
 * is a deliberately deferred design question, not part of this iteration.
 *
 * @since 1.8.11
 */
public final class BankDataRegistry {

    private static final Map<String, CountryBankDataLoader> LOADERS = unmodifiableMap(
        Arrays.<CountryBankDataLoader>asList(
            new DeBundesbankLoader(), new AtBankDataLoader(), new ChBankDataLoader(),
            new CzBankDataLoader(), new BeBankDataLoader())
              .stream()
              .collect(toMap(CountryBankDataLoader::getCountryCode, loader -> loader))
    );

    /** Lazily created, one {@link CountryDataCache} per supported country. */
    private static final Map<String, CountryDataCache> CACHES = new ConcurrentHashMap<>();

    /**
     * Factory for the {@link Downloader} used by newly created caches; overridable for tests so
     * that exercising {@link BankDataLookup} never reaches out to the real network, since the
     * bundled fallback data is always treated as maximally stale and triggers a background
     * refresh after the very first lookup for a country.
     */
    private static volatile Supplier<Downloader> downloaderFactory = Downloader::createDefault;

    private BankDataRegistry() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Returns the loader registered for the given country code, if any.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @return an {@link Optional} containing the loader, or empty if the country is not supported
     */
    public static Optional<CountryBankDataLoader> getLoader(String countryCode) {
        return Optional.ofNullable(LOADERS.get(countryCode));
    }

    /**
     * Returns the set of ISO 3166-1 alpha-2 country codes supported by this module.
     *
     * @return an immutable set of supported country codes
     */
    public static Set<String> getSupportedCountryCodes() {
        return LOADERS.keySet();
    }

    /**
     * Returns whether the given country is supported by a registered loader.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @return {@code true} if a loader is registered for this country
     */
    public static boolean isCountrySupported(String countryCode) {
        return LOADERS.containsKey(countryCode);
    }

    /**
     * Returns the lazily created {@link CountryDataCache} for the given country, creating it (with
     * the default {@link Downloader}) if it does not exist yet.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code; must be supported
     * @return the country's data cache
     * @throws IllegalArgumentException if the country is not supported
     */
    public static CountryDataCache getCache(String countryCode) {
        CountryBankDataLoader loader = LOADERS.get(countryCode);
        if (loader == null) {
            throw new IllegalArgumentException("Unsupported bank data country code: " + countryCode);
        }
        return CACHES.computeIfAbsent(countryCode, code -> new CountryDataCache(loader, downloaderFactory.get()));
    }

    /**
     * Installs a custom {@link Downloader} factory for newly created caches and clears any
     * already-created caches so the next {@link #getCache(String)} call picks it up.
     * <p>
     * Intended exclusively for test setup, to prevent background refreshes from reaching the real
     * network while exercising {@link BankDataLookup}.
     *
     * @param factory the downloader factory to use for subsequently created caches
     */
    static void setDownloaderFactoryForTesting(Supplier<Downloader> factory) {
        downloaderFactory = factory;
        CACHES.clear();
    }

    /**
     * Resets the downloader factory to the production default and clears any created caches.
     * <p>
     * Intended exclusively for test tear-down.
     */
    static void resetForTesting() {
        downloaderFactory = Downloader::createDefault;
        CACHES.clear();
    }

}
