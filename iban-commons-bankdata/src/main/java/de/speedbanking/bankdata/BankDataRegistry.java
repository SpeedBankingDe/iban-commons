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

import de.speedbanking.bankdata.io.Downloader;
import de.speedbanking.bankdata.log.NaiveLogger;
import de.speedbanking.bankdata.refresh.CountryDataCache;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
import de.speedbanking.iban.IbanRegistry;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry for all available {@link CountryBankDataLoader} implementations.
 * <p>
 * Loaders are discovered dynamically at class initialization by querying {@link IbanRegistry#getSepaCountries()}
 * and attempting to load corresponding loader classes following standard naming conventions in package
 * {@code de.speedbanking.bankdata.loader}.
 *
 * @since 1.8.11
 */
public final class BankDataRegistry {

    private static final NaiveLogger LOGGER = NaiveLogger.of(BankDataRegistry.class);

    private static final String LOADER_PACKAGE = de.speedbanking.bankdata.loader.BankDataLoaderDe.class.getPackage().getName();

    private static final Map<String, CountryBankDataLoader> LOADERS = unmodifiableMap(discoverLoaders());

    /** Lazily created, one {@link CountryDataCache} per supported country. */
    private static final Map<String, CountryDataCache> CACHES = new ConcurrentHashMap<>();

    private static volatile Supplier<Downloader> downloaderFactory = Downloader::createDefault;

    private BankDataRegistry() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    private static Map<String, CountryBankDataLoader> discoverLoaders() {
        Map<String, CountryBankDataLoader> loaders = new LinkedHashMap<>();

        for (IbanRegistry countryData : IbanRegistry.getSepaCountries()) {
            CountryBankDataLoader loader = tryLoadForCountry(countryData.getCountryCode());
            if (loader != null) {
                loaders.put(loader.getCountryCode(), loader);
            }
        }

        return loaders;
    }

    private static CountryBankDataLoader tryLoadForCountry(String countryCode) {
        String loaderClassName = LOADER_PACKAGE
            + '.'
            + "BankDataLoader"
            + Character.toUpperCase(countryCode.charAt(0))
            + Character.toLowerCase(countryCode.charAt(1));

        try {
            Class<?> clazz = Class.forName(loaderClassName);
            if (CountryBankDataLoader.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                return (CountryBankDataLoader) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (ClassNotFoundException ignored) {
            // expected for SEPA countries without a dedicated loader implementation
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("Failed to instantiate bank data loader class {0}", ex, loaderClassName);
        } catch (LinkageError ex) {
            // e.g. ExceptionInInitializerError from a loader's own static initializer (a packaging
            // bug in just that one loader); must not escape and take every other country's loader
            // down with it
            LOGGER.warn("Failed to load bank data loader class {0}", ex, loaderClassName);
        }
        return null;
    }

    public static Optional<CountryBankDataLoader> getLoader(String countryCode) {
        return Optional.ofNullable(LOADERS.get(countryCode));
    }

    /**
     * Returns the set of ISO 3166-1 alpha-2 country codes supported by this module.
     *
     * @return an immutable set of supported country codes
     */
    public static Set<String> getSupportedCountryCodes() {
        return new TreeSet<>(LOADERS.keySet());
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
