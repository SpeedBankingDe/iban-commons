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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Immutable global configuration for the bank data engine.
 * <p>
 * Configuration follows an <em>initialize-once</em> pattern:
 * <ol>
 *   <li>Optionally call {@link #configure(BankDataConfig)} once at application startup to supply a
 *       custom configuration built via {@link Builder}.</li>
 *   <li>The first call to {@link #get()} freezes the configuration permanently. Any subsequent
 *       call to {@link #configure(BankDataConfig)} throws an {@link IllegalStateException}.</li>
 * </ol>
 * <p>
 * If {@link #configure(BankDataConfig)} is never called, the {@link #DEFAULT} instance is used
 * automatically.
 * <p>
 * This class is thread-safe.
 *
 * @since 1.8.11
 */
public final class BankDataConfig {

    /** System property overriding the default cache directory. */
    public static final String                  CACHE_DIR_SYSTEM_PROPERTY       = "de.speedbanking.bankdata.cacheDir";

    /** Environment variable overriding the default cache directory. */
    public static final String                  CACHE_DIR_ENV_VARIABLE          = "IBAN_COMMONS_BANKDATA_CACHE_DIR";

    /** System property disabling all network access (downloads/background refreshes) entirely. */
    public static final String                  DISABLE_NETWORK_SYSTEM_PROPERTY
                                                                                = "de.speedbanking.bankdata.disableNetwork";

    /** Environment variable disabling all network access (downloads/background refreshes) entirely. */
    public static final String                  DISABLE_NETWORK_ENV_VARIABLE    = "IBAN_COMMONS_BANKDATA_DISABLE_NETWORK";

    private static final Duration               DEFAULT_STALE_THRESHOLD         = Duration.ofDays(90);
    private static final Duration               DEFAULT_CONNECT_TIMEOUT         = Duration.ofSeconds(5);
    private static final Duration               DEFAULT_READ_TIMEOUT            = Duration.ofSeconds(30);

    /**
     * Default configuration.
     */
    public static final BankDataConfig          DEFAULT                         = new BankDataConfig(new Builder());

    private static final AtomicReference<State> STATE                           = new AtomicReference<>(
        new State(DEFAULT, false));

    private static volatile BankDataConfig      activeConfig;

    private final Path                          cacheDirectory;
    private final Duration                      staleThreshold;
    private final Duration                      connectTimeout;
    private final Duration                      readTimeout;
    private final boolean                       networkDisabled;

    private BankDataConfig(Builder builder) {
        this.cacheDirectory  = builder.cacheDirectory != null ? builder.cacheDirectory : resolveDefaultCacheDirectory();
        this.staleThreshold  = builder.staleThreshold;
        this.connectTimeout  = builder.connectTimeout;
        this.readTimeout     = builder.readTimeout;
        this.networkDisabled = builder.networkDisabled != null ? builder.networkDisabled : resolveDefaultNetworkDisabled();
    }

    private static Path resolveDefaultCacheDirectory() {
        String systemProperty = System.getProperty(CACHE_DIR_SYSTEM_PROPERTY);
        if (systemProperty != null && !systemProperty.isEmpty()) {
            return Paths.get(systemProperty);
        }
        String envVariable = System.getenv(CACHE_DIR_ENV_VARIABLE);
        if (envVariable != null && !envVariable.isEmpty()) {
            return Paths.get(envVariable);
        }
        return Paths.get(System.getProperty("user.home"), ".iban-commons", "bankdata");
    }

    private static boolean resolveDefaultNetworkDisabled() {
        String systemProperty = System.getProperty(DISABLE_NETWORK_SYSTEM_PROPERTY);
        if (systemProperty != null && !systemProperty.isEmpty()) {
            return Boolean.parseBoolean(systemProperty);
        }
        String envVariable = System.getenv(DISABLE_NETWORK_ENV_VARIABLE);
        if (envVariable != null && !envVariable.isEmpty()) {
            return Boolean.parseBoolean(envVariable);
        }
        return false;
    }

    /**
     * Returns the cache directory used to persist downloaded bank directories across JVM restarts.
     * <p>
     * The directory is <strong>not</strong> created eagerly when the configuration is frozen; it is
     * created lazily on the first write attempt.
     *
     * @return the cache directory
     */
    public Path getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Returns the default duration after which cached country data is considered stale and
     * eligible for an asynchronous background refresh.
     *
     * @return the stale threshold, default 90 days
     */
    public Duration getStaleThreshold() {
        return staleThreshold;
    }

    /**
     * Returns the connect timeout used by {@code HttpDownloader}.
     *
     * @return the connect timeout, default 5 seconds
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Returns the read timeout used by {@code HttpDownloader}.
     *
     * @return the read timeout, default 30 seconds
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * Returns whether all network access (downloads and background refreshes) is disabled.
     * <p>
     * When {@code true}, {@code CountryDataCache.triggerAsyncRefreshIfStale()} becomes a complete
     * no-op: lookups are served exclusively from the local cache directory and the bundled
     * classpath fallback data, and no outbound connection is ever attempted.
     *
     * @return {@code true} if network access is disabled, default {@code false}
     */
    public boolean isNetworkDisabled() {
        return networkDisabled;
    }

    /**
     * Installs a custom global configuration.
     * <p>
     * Must be called before the first invocation of {@link #get()}. Once the configuration is
     * frozen, this method throws an {@link IllegalStateException}.
     *
     * @param config the configuration to install; must not be {@code null}
     * @return the newly installed configuration instance
     * @throws IllegalStateException if the configuration has already been frozen
     */
    public static BankDataConfig configure(BankDataConfig config) {
        requireNonNull(config, "Config must not be null");

        BankDataConfig result = STATE.updateAndGet(current -> {
            if (current.frozen) {
                throw new IllegalStateException(
                    BankDataConfig.class.getSimpleName() + " is already in use and cannot be changed - "
                  + "call configure() before the first get() invocation");
            }
            return new State(config, false);
        }).config;

        activeConfig = null; // invalidate fast-path cache
        return result;
    }

    /**
     * Returns the global configuration instance and freezes it.
     *
     * @return the active {@link BankDataConfig} instance; never {@code null}
     */
    public static BankDataConfig get() {
        BankDataConfig cfg = activeConfig;
        if (cfg != null) {
            return cfg;
        }

        cfg = STATE.updateAndGet(current -> current.frozen
                ? current
                : new State(current.config, true)).config;
        activeConfig = cfg;
        return cfg;
    }

    /**
     * Resets the global configuration to {@link #DEFAULT} and clears the frozen flag.
     * <p>
     * Intended exclusively for test tear-down.
     *
     * @return the newly installed configuration instance
     */
    static BankDataConfig reset() {
        return reset(DEFAULT);
    }

    /**
     * Resets the global configuration to the specified instance and clears the frozen flag.
     * <p>
     * Intended exclusively for test tear-down.
     *
     * @param config the configuration instance to install; must not be {@code null}
     * @return the newly installed configuration instance
     */
    static BankDataConfig reset(BankDataConfig config) {
        requireNonNull(config, "Config must not be null");
        activeConfig = null;
        return STATE.updateAndGet(current -> new State(config, false)).config;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + '['
               + "cacheDirectory=" + cacheDirectory + ", "
               + "staleThreshold=" + staleThreshold + ", "
               + "connectTimeout=" + connectTimeout + ", "
               + "readTimeout="    + readTimeout + ", "
               + "networkDisabled=" + networkDisabled
               + ']';
    }

    /**
     * Returns a new {@link Builder} with all options set to their defaults.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BankDataConfig}.
     */
    public static final class Builder {

        private Path     cacheDirectory;
        private Duration staleThreshold = DEFAULT_STALE_THRESHOLD;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout    = DEFAULT_READ_TIMEOUT;
        private Boolean  networkDisabled;

        private Builder() {
        }

        /**
         * Sets the cache directory used to persist downloaded bank directories.
         *
         * @param value the cache directory; if not set, resolved from system property, env
         *              variable, or {@code ${user.home}/.iban-commons/bankdata}, in that order
         * @return this builder
         */
        public Builder cacheDirectory(Path value) {
            this.cacheDirectory = value;
            return this;
        }

        /**
         * Sets the duration after which cached country data is considered stale.
         *
         * @param value the stale threshold
         * @return this builder
         */
        public Builder staleThreshold(Duration value) {
            this.staleThreshold = requireNonNull(value, "staleThreshold must not be null");
            return this;
        }

        /**
         * Sets the HTTP connect timeout.
         *
         * @param value the connect timeout
         * @return this builder
         */
        public Builder connectTimeout(Duration value) {
            this.connectTimeout = requireNonNull(value, "connectTimeout must not be null");
            return this;
        }

        /**
         * Sets the HTTP read timeout.
         *
         * @param value the read timeout
         * @return this builder
         */
        public Builder readTimeout(Duration value) {
            this.readTimeout = requireNonNull(value, "readTimeout must not be null");
            return this;
        }

        /**
         * Disables (or re-enables) all network access.
         *
         * @param value {@code true} to disable all downloads/background refreshes; if not set,
         *              resolved from the {@value BankDataConfig#DISABLE_NETWORK_SYSTEM_PROPERTY}
         *              system property, the {@value BankDataConfig#DISABLE_NETWORK_ENV_VARIABLE}
         *              environment variable, or {@code false}, in that order
         * @return this builder
         */
        public Builder disableNetwork(boolean value) {
            this.networkDisabled = value;
            return this;
        }

        /**
         * Builds and returns the configured {@link BankDataConfig} instance.
         *
         * @return a new immutable {@link BankDataConfig}
         */
        public BankDataConfig build() {
            return new BankDataConfig(this);
        }
    }

    /**
     * Internal state container to ensure atomic updates of config and frozen flag.
     */
    private static final class State {
        private final BankDataConfig config;
        private final boolean        frozen;

        private State(BankDataConfig config, boolean frozen) {
            this.config = config;
            this.frozen = frozen;
        }
    }

}
