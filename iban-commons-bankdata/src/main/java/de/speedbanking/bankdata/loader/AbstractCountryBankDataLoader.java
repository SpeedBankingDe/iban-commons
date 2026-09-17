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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import de.speedbanking.bankdata.BankDataConfig;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
import de.speedbanking.util.Country;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Abstract base implementation for country-specific bank data loaders using column definitions.
 *
 * @param <C> enum type representing column definitions
 * @since 1.8.11
 */
public abstract class AbstractCountryBankDataLoader<C extends Enum<C> & ColumnDefinition> implements CountryBankDataLoader {

    private final Columns<C> columns;
    private final String     countryCode;

    /**
     * Constructs a loader instance with the specified column enum class.
     * <p>
     * Derives the ISO country code from the concrete subclass name prefix (e.g. {@code "DE"} from {@code DeBankDataLoader}).
     *
     * @param columnClass enum class defining column mapping, must not be null
     */
    protected AbstractCountryBankDataLoader(Class<C> columnClass) {
        requireNonNull(columnClass, "columnClass must not be null");
        this.columns = Columns.of(columnClass);
        this.countryCode = resolveCountryCode(getClass());
    }

    private static String resolveCountryCode(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (simpleName.length() < 2) {
            throw new IllegalStateException("Class name '" + simpleName + "' is too short to extract ISO country code");
        }
        Country country = Country.fromCode(simpleName.substring(simpleName.length() - 2, simpleName.length()).toUpperCase(Locale.ROOT));
        if (country == null) {
            throw new IllegalStateException("Cannot derive country from class name '" + simpleName + "'");
        }
        return country.getCode();
    }

    /**
     * Gets the column wrapper instance.
     *
     * @return columns instance
     */
    protected final Columns<C> getColumns() {
        return columns;
    }

    @Override
    public final String getCountryCode() {
        return countryCode;
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return UTF_8;
    }

    @Override
    public Duration getRecommendedRefreshThreshold() {
        return BankDataConfig.get().getStaleThreshold();
    }

    @Override
    public final String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add("countryCode=" + countryCode)
            .add("remoteSourceUri=" + getRemoteSourceUri())
            .add("remoteSourceCharset=" + getRemoteSourceCharset())
            .add("columns=" + columns)
            .toString();
    }

}
