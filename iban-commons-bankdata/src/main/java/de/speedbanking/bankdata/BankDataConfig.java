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
import static java.util.Objects.requireNonNull;

import de.speedbanking.bankdata.log.NaiveLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Immutable global configuration for the bank data engine.
 * <p>
 * Initialize-once, same as {@code IbanConfig} elsewhere in this project: call
 * {@link #configure(BankDataConfig)} once at startup if you want a custom {@link Builder}, then
 * the first {@link #get()} freezes it - calling {@code configure()} again after that throws
 * {@link IllegalStateException}. Never call {@code configure()} at all and {@link #DEFAULT} is
 * used. Any {@link Builder} setting you don't set falls back to the external config file below,
 * then to a hardcoded default.
 * <p>
 * <strong>External config file.</strong> One optional properties file can override the cache
 * directory, network disabling, timeouts, and per-country loader source URIs without a new
 * release - point {@value #CONFIG_FILE_SYSTEM_PROPERTY} (or {@value #CONFIG_FILE_ENV_VARIABLE}) at
 * it. Only put in the keys you actually want to change:
 * <pre>
 *   cacheDir       = /var/cache/myapp/bankdata
 *   disableNetwork = false
 *   staleThreshold = P30D            (ISO-8601 duration, {@link Duration#parse(CharSequence)}; minimum 6 hours)
 *   connectTimeout = PT3S
 *   readTimeout    = PT15S
 *   loader.DE.url  = https://mirror.example.com/blz-aktuell-csv-data.csv
 * </pre>
 * The bundled classpath resource {@code /bankdata/_bankdata.properties} is the single source of
 * truth for every hardcoded default shown above (except {@code cacheDir}, which has none - see its
 * getter) and uses this exact same key shape, so it doubles as a template: copy it, change only
 * the keys that need fixing.
 * <p>
 * This class is thread-safe.
 *
 * @since 1.8.11
 */
public final class BankDataConfig {

    /** System property pointing at an external properties file overriding any setting below. */
    public static final String                  CONFIG_FILE_SYSTEM_PROPERTY = "de.speedbanking.bankdata.configFile";

    /** Environment variable pointing at an external properties file overriding any setting below. */
    public static final String                  CONFIG_FILE_ENV_VARIABLE    = "IBAN_COMMONS_BANKDATA_CONFIG_FILE";

    public static final String                  BUNDLED_RESOURCE_PATH       = "/bankdata/_bankdata.properties";

    private static final String                 KEY_CACHE_DIR               = "cacheDir";
    private static final String                 KEY_DISABLE_NETWORK         = "disableNetwork";
    private static final String                 KEY_STALE_THRESHOLD         = "staleThreshold";
    private static final String                 KEY_CONNECT_TIMEOUT         = "connectTimeout";
    private static final String                 KEY_READ_TIMEOUT            = "readTimeout";
    private static final String                 LOADER_URL_KEY_PREFIX       = "loader.";
    private static final String                 LOADER_URL_KEY_SUFFIX       = ".url";

    private static final NaiveLogger            LOGGER                      = NaiveLogger.of(BankDataConfig.class);

    /**
     * The bundled classpath resource is the single source of truth for every hardcoded default
     * below (except {@code cacheDir}, which has no fixed default and is therefore absent from it);
     * a Java literal is only consulted if that resource is somehow missing the key.
     */
    private static final ExternalProperties      BUNDLED_DEFAULTS                = ExternalProperties.loadBundled();

    /**
     * The lowest {@code staleThreshold} this configuration accepts, matching the fixed backoff
     * {@code CountryDataCache} already applies after a transient refresh failure (connection
     * error, timeout, 5xx, suspect response). Allowing a lower value here would make the
     * "data is fine" staleness check fire more often than the failure path's own cooldown,
     * defeating its purpose of not hammering an upstream source.
     */
    private static final Duration                MIN_STALE_THRESHOLD             = Duration.ofHours(6);

    /**
     * The highest {@code connectTimeout}/{@code readTimeout} this configuration accepts.
     * <p>
     * {@link java.net.HttpURLConnection#setConnectTimeout(int)}/{@code setReadTimeout(int)} take a
     * plain {@code int} milliseconds value; a {@link Duration} beyond this overflows when narrowed
     * via {@link Duration#toMillis()}, silently producing a negative value that then makes every
     * download fail with an {@link IllegalArgumentException} at connection time, for every country,
     * from a single misconfigured value. Validating here instead catches it immediately, at
     * configuration time, with a message that names the actual problem.
     */
    private static final Duration                MAX_TIMEOUT                     = Duration.ofMillis(Integer.MAX_VALUE);

    private static final Duration                DEFAULT_STALE_THRESHOLD         = BUNDLED_DEFAULTS.getDuration(KEY_STALE_THRESHOLD)
        .orElseGet(() -> Duration.ofDays(90));
    private static final Duration                DEFAULT_CONNECT_TIMEOUT         = BUNDLED_DEFAULTS.getDuration(KEY_CONNECT_TIMEOUT)
        .orElseGet(() -> Duration.ofSeconds(5));
    private static final Duration                DEFAULT_READ_TIMEOUT            = BUNDLED_DEFAULTS.getDuration(KEY_READ_TIMEOUT)
        .orElseGet(() -> Duration.ofSeconds(30));
    private static final boolean                 DEFAULT_NETWORK_DISABLED        = BUNDLED_DEFAULTS.getBoolean(KEY_DISABLE_NETWORK)
        .orElse(false);

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
    private final Map<String, URI>              sourceUriOverrides;

    private BankDataConfig(Builder builder) {
        // re-read on every construction (rare: once for DEFAULT, once per configure()/build()
        // call) rather than caching statically, so a system property/env variable/file change
        // made before the next construction is honored - the same "no caching" contract the
        // previous system-property-only resolution already had
        ExternalProperties external = ExternalProperties.load();
        this.cacheDirectory    = builder.cacheDirectory != null ? builder.cacheDirectory : resolveCacheDirectory(external);
        this.staleThreshold    = builder.staleThreshold != null ? builder.staleThreshold
            : resolveExternalStaleThreshold(external);
        this.connectTimeout    = builder.connectTimeout != null ? builder.connectTimeout
            : resolveExternalTimeout(KEY_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT, external);
        this.readTimeout       = builder.readTimeout != null ? builder.readTimeout
            : resolveExternalTimeout(KEY_READ_TIMEOUT, DEFAULT_READ_TIMEOUT, external);
        this.networkDisabled   = builder.networkDisabled != null ? builder.networkDisabled
            : external.getBoolean(KEY_DISABLE_NETWORK).orElse(DEFAULT_NETWORK_DISABLED);
        Map<String, URI> mergedOverrides = new HashMap<>(external.getSourceUriOverrides());
        mergedOverrides.putAll(builder.sourceUriOverrides);
        this.sourceUriOverrides = unmodifiableMap(mergedOverrides);
    }

    private static Path resolveCacheDirectory(ExternalProperties external) {
        // no bundled default: a fixed absolute path baked into the jar cannot be portable across
        // users/machines, so this one setting always falls straight through to the computed default
        return external.getString(KEY_CACHE_DIR)
            .map(Paths::get)
            .orElseGet(() -> Paths.get(System.getProperty("user.home"), ".iban-commons", "bankdata"));
    }

    /**
     * Resolves {@code staleThreshold} from the external config file, enforcing {@link
     * #MIN_STALE_THRESHOLD}. Unlike {@link Builder#staleThreshold(Duration)}, which rejects a too-low
     * value outright, a misconfigured external file falls back to the minimum with a warning instead
     * of crashing the application at startup.
     */
    private static Duration resolveExternalStaleThreshold(ExternalProperties external) {
        Optional<Duration> configured = external.getDuration(KEY_STALE_THRESHOLD);
        if (!configured.isPresent()) {
            return DEFAULT_STALE_THRESHOLD;
        }
        Duration value = configured.get();
        if (value.compareTo(MIN_STALE_THRESHOLD) < 0) {
            LOGGER.warn("Bank data config entry ''staleThreshold={0}'' is below the minimum of {1}, using the minimum instead",
                value, MIN_STALE_THRESHOLD);
            return MIN_STALE_THRESHOLD;
        }
        return value;
    }

    /**
     * Resolves a timeout ({@code connectTimeout}/{@code readTimeout}) from the external config
     * file, enforcing that it is positive and does not exceed {@link #MAX_TIMEOUT}. Unlike {@link
     * Builder#connectTimeout(Duration)}/{@link Builder#readTimeout(Duration)}, which reject an
     * invalid value outright, a misconfigured external file falls back to the given default with a
     * warning instead of crashing the application at startup.
     */
    private static Duration resolveExternalTimeout(String key, Duration defaultValue, ExternalProperties external) {
        Optional<Duration> configured = external.getDuration(key);
        if (!configured.isPresent()) {
            return defaultValue;
        }
        Duration value = configured.get();
        if (value.isZero() || value.isNegative() || value.compareTo(MAX_TIMEOUT) > 0) {
            LOGGER.warn("Bank data config entry ''{0}={1}'' must be positive and at most {2}, using the default of {3} instead",
                key, value, MAX_TIMEOUT, defaultValue);
            return defaultValue;
        }
        return value;
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
     * @return the stale threshold, default 90 days, never below 6 hours
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
     * Returns a runtime override for a loader's remote source URI, if one is configured for the
     * given country via the external config file or {@link Builder#sourceUriOverride(String, URI)}.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code, e.g. {@code "DE"}
     * @return the overriding URI, or empty if the loader's compiled-in default should be used
     */
    public Optional<URI> getSourceUriOverride(String countryCode) {
        return Optional.ofNullable(sourceUriOverrides.get(countryCode));
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
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add("cacheDirectory=" + cacheDirectory)
            .add("staleThreshold=" + staleThreshold)
            .add("connectTimeout=" + connectTimeout)
            .add("readTimeout=" + readTimeout)
            .add("networkDisabled=" + networkDisabled)
            .add("sourceUriOverrides=" + sourceUriOverrides)
            .toString();
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

        private Path              cacheDirectory;
        private Duration          staleThreshold;
        private Duration          connectTimeout;
        private Duration          readTimeout;
        private Boolean           networkDisabled;
        private final Map<String, URI> sourceUriOverrides = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Sets the cache directory used to persist downloaded bank directories.
         *
         * @param value the cache directory; if not set, resolved from the external config file's
         *              {@code cacheDir} entry, or {@code ${user.home}/.iban-commons/bankdata}
         * @return this builder
         */
        public Builder cacheDirectory(Path value) {
            this.cacheDirectory = value;
            return this;
        }

        /**
         * Sets the duration after which cached country data is considered stale.
         *
         * @param value the stale threshold, must be at least 6 hours (see {@link
         *              BankDataConfig#getStaleThreshold()}); if not set, resolved from the external
         *              config file's {@code staleThreshold} entry, or 90 days
         * @return this builder
         * @throws IllegalArgumentException if {@code value} is below the 6-hour minimum
         */
        public Builder staleThreshold(Duration value) {
            requireNonNull(value, "staleThreshold must not be null");
            if (value.compareTo(MIN_STALE_THRESHOLD) < 0) {
                throw new IllegalArgumentException("staleThreshold must be at least " + MIN_STALE_THRESHOLD + ", got " + value);
            }
            this.staleThreshold = value;
            return this;
        }

        /**
         * Sets the HTTP connect timeout.
         *
         * @param value the connect timeout; must be positive and at most {@link BankDataConfig#MAX_TIMEOUT}
         *              (beyond that, narrowing to the {@code int} milliseconds
         *              {@link java.net.HttpURLConnection#setConnectTimeout(int)} takes would
         *              overflow); if not set, resolved from the external config file's
         *              {@code connectTimeout} entry, or 5 seconds
         * @return this builder
         * @throws IllegalArgumentException if {@code value} is not positive or exceeds {@link BankDataConfig#MAX_TIMEOUT}
         */
        public Builder connectTimeout(Duration value) {
            this.connectTimeout = requireValidTimeout("connectTimeout", value);
            return this;
        }

        /**
         * Sets the HTTP read timeout.
         *
         * @param value the read timeout; must be positive and at most {@link BankDataConfig#MAX_TIMEOUT} (beyond
         *              that, narrowing to the {@code int} milliseconds
         *              {@link java.net.HttpURLConnection#setReadTimeout(int)} takes would overflow);
         *              if not set, resolved from the external config file's {@code readTimeout}
         *              entry, or 30 seconds
         * @return this builder
         * @throws IllegalArgumentException if {@code value} is not positive or exceeds {@link BankDataConfig#MAX_TIMEOUT}
         */
        public Builder readTimeout(Duration value) {
            this.readTimeout = requireValidTimeout("readTimeout", value);
            return this;
        }

        private static Duration requireValidTimeout(String name, Duration value) {
            requireNonNull(value, name + " must not be null");
            if (value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive, got " + value);
            }
            if (value.compareTo(MAX_TIMEOUT) > 0) {
                throw new IllegalArgumentException(name + " must not exceed " + MAX_TIMEOUT + ", got " + value);
            }
            return value;
        }

        /**
         * Disables (or re-enables) all network access.
         *
         * @param value {@code true} to disable all downloads/background refreshes; if not set,
         *              resolved from the external config file's {@code disableNetwork} entry, or
         *              {@code false}
         * @return this builder
         */
        public Builder disableNetwork(boolean value) {
            this.networkDisabled = value;
            return this;
        }

        /**
         * Overrides the remote source URI a loader uses for the given country, taking precedence
         * over both its compiled-in default and any {@code loader.<CC>.url} entry in the external
         * config file.
         *
         * @param countryCode the ISO 3166-1 alpha-2 country code, e.g. {@code "DE"}; matched
         *                    case-insensitively, since a loader's own country code (used to look
         *                    this override up) is always the uppercase canonical form
         * @param uri         the overriding source URI
         * @return this builder
         */
        public Builder sourceUriOverride(String countryCode, URI uri) {
            requireNonNull(countryCode, "countryCode must not be null");
            sourceUriOverrides.put(countryCode.toUpperCase(Locale.ROOT), requireNonNull(uri, "uri must not be null"));
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

    /**
     * Loads and exposes the single optional external properties file located via
     * {@link #CONFIG_FILE_SYSTEM_PROPERTY}/{@link #CONFIG_FILE_ENV_VARIABLE}. Absent, unreadable,
     * or unparsable input all resolve to an empty property set (every lookup falls back to the
     * hardcoded default), logging a warning in the latter two cases since those indicate a likely
     * typo rather than a deliberate "no override" choice.
     */
    private static final class ExternalProperties {
        private final Properties properties;

        private ExternalProperties(Properties properties) {
            this.properties = properties;
        }

        /**
         * Loads the bundled classpath resource carrying every hardcoded default (except
         * {@code cacheDir}). Failure to find or parse it is a packaging bug, not a runtime
         * condition to degrade gracefully from, so this throws rather than falling back silently.
         */
        static ExternalProperties loadBundled() {
            Properties properties = new Properties();
            try (InputStream in = BankDataConfig.class.getResourceAsStream(BUNDLED_RESOURCE_PATH)) {
                if (in == null) {
                    throw new IllegalStateException("Bundled resource '" + BUNDLED_RESOURCE_PATH + "' not found on classpath");
                }
                properties.load(in);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load bundled resource '" + BUNDLED_RESOURCE_PATH + "'", ex);
            }
            return new ExternalProperties(properties);
        }

        static ExternalProperties load() {
            String path = resolveConfigFilePath();
            if (path == null) {
                return new ExternalProperties(new Properties());
            }

            Path file = Paths.get(path);
            if (!Files.isReadable(file)) {
                LOGGER.warn("Bank data config file ''{0}'' (from {1}/{2}) does not exist or is not readable, ignoring",
                    path, CONFIG_FILE_SYSTEM_PROPERTY, CONFIG_FILE_ENV_VARIABLE);
                return new ExternalProperties(new Properties());
            }

            Properties loaded = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                loaded.load(in);
                LOGGER.info("Loaded bank data config file ''{0}'' with {1} entr{2}",
                    path, loaded.size(), loaded.size() == 1 ? "y" : "ies");
            } catch (IOException ex) {
                LOGGER.warn("Failed to read bank data config file ''{0}'', ignoring", ex, path);
                return new ExternalProperties(new Properties());
            }
            return new ExternalProperties(loaded);
        }

        private static String resolveConfigFilePath() {
            String systemProperty = System.getProperty(CONFIG_FILE_SYSTEM_PROPERTY);
            if (systemProperty != null && !systemProperty.isEmpty()) {
                return systemProperty;
            }
            String envVariable = System.getenv(CONFIG_FILE_ENV_VARIABLE);
            if (envVariable != null && !envVariable.isEmpty()) {
                return envVariable;
            }
            return null;
        }

        @SuppressWarnings("PMD.InefficientEmptyStringCheck")
        Optional<String> getString(String key) {
            String value = properties.getProperty(key);
            return value == null || value.trim().isEmpty() ? Optional.empty() : Optional.of(value.trim());
        }

        Optional<Boolean> getBoolean(String key) {
            return getString(key).map(Boolean::parseBoolean);
        }

        Optional<Duration> getDuration(String key) {
            Optional<String> value = getString(key);
            if (!value.isPresent()) {
                return Optional.empty();
            }
            try {
                return Optional.of(Duration.parse(value.get()));
            } catch (DateTimeParseException ex) {
                LOGGER.warn("Bank data config entry ''{0}={1}'' is not a valid ISO-8601 duration, ignoring", ex, key, value.get());
                return Optional.empty();
            }
        }

        @SuppressWarnings("PMD.InefficientEmptyStringCheck")
        Map<String, URI> getSourceUriOverrides() {
            Map<String, URI> result = new HashMap<>();
            for (String name : properties.stringPropertyNames()) {
                if (!name.startsWith(LOADER_URL_KEY_PREFIX) || !name.endsWith(LOADER_URL_KEY_SUFFIX)) {
                    continue;
                }
                int countryCodeStart = LOADER_URL_KEY_PREFIX.length();
                int countryCodeEnd = name.length() - LOADER_URL_KEY_SUFFIX.length();
                // a key like "loader.url" (country code segment missing) matches both the prefix
                // and suffix check above via their shared '.', but leaves nothing between them;
                // substring() would throw StringIndexOutOfBoundsException for that (begin > end)
                if (countryCodeEnd <= countryCodeStart) {
                    LOGGER.warn("Bank data config entry ''{0}'' has no country code between ''{1}'' and ''{2}'', ignoring",
                        name, LOADER_URL_KEY_PREFIX, LOADER_URL_KEY_SUFFIX);
                    continue;
                }
                String countryCode = name.substring(countryCodeStart, countryCodeEnd).toUpperCase(Locale.ROOT);
                String value = properties.getProperty(name);
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }
                try {
                    result.put(countryCode, URI.create(value.trim()));
                } catch (IllegalArgumentException ex) {
                    LOGGER.warn("Bank data config entry ''{0}={1}'' is not a valid URI, ignoring", ex, name, value);
                }
            }
            return result;
        }

    }

}
