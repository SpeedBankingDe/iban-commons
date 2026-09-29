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
package de.speedbanking.bankdata.loader;

import de.speedbanking.bankdata.BankDataConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * Reads the bundled classpath resource {@code /bankdata/_bankdata.properties}, the single source
 * of truth for each loader's compiled-in {@code DEFAULT_SOURCE_URI} constant.
 * <p>
 * Keeping these defaults in a properties file rather than hardcoded {@link URI#create(String)}
 * literals means the shipped file is also the template a consumer copies and edits to override a
 * source URL at runtime via {@code BankDataConfig}, without risking the two ever drifting apart.
 *
 * @since 1.8.11
 */
final class BankDataLoaderDefaults {

    private static final Properties PROPERTIES = load();

    private BankDataLoaderDefaults() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = BankDataLoaderDefaults.class.getResourceAsStream(BankDataConfig.BUNDLED_RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Bundled resource '" + BankDataConfig.BUNDLED_RESOURCE_PATH + "' not found on classpath");
            }
            properties.load(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load bundled resource '" + BankDataConfig.BUNDLED_RESOURCE_PATH + "'", ex);
        }
        return properties;
    }

    /**
     * Returns the compiled-in default source {@link URI} for the given country's loader.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code, e.g. {@code "DE"}
     * @return the default source URI
     * @throws IllegalStateException if the bundled resource has no entry for this country
     */
    static URI sourceUri(String countryCode) {
        String key = "loader." + countryCode + ".url";
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Bundled resource '" + BankDataConfig.BUNDLED_RESOURCE_PATH + "' has no '" + key + "' entry");
        }
        return URI.create(value.trim());
    }

}
