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
package de.speedbanking.iban;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Immutable global configuration for the IBAN engine.
 * <p>
 * Configuration follows an <em>initialize-once</em> pattern:
 * <ol>
 *   <li>Optionally call {@link #configure(IbanConfig)} once at application startup
 *       (e.g. in a {@code @PostConstruct} method or {@code main()}) to supply a
 *       custom configuration built via {@link Builder}.</li>
 *   <li>The first call to {@link #get()} or any of the static accessors (e.g., {@link #isAllowSpace()})
 *       freezes the configuration permanently.
 *       Any subsequent call to {@link #configure(IbanConfig)} will throw an
 *       {@link IllegalStateException}.</li>
 * </ol>
 * <p>
 * If {@link #configure(IbanConfig)} is never called, the {@link #DEFAULT} instance
 * is used automatically.
 * <p>
 * This class is thread-safe.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // at startup — before any IBAN operation:
 * IbanConfig.configure(IbanConfig.builder()
 *     .allowSpace(true)
 *     .allowLowercase(true)
 *     .build());
 *
 * // anywhere in the application:
 * if (IbanConfig.get().isAllowLowercase()) { ... }
 * }</pre>
 *
 * @since 1.8.5
 */
public final class IbanConfig {

    /**
     * Default configuration with all options <strong>disabled</strong>.
     */
    public static final IbanConfig              DEFAULT = new IbanConfig(new Builder());

    private static final AtomicReference<State> STATE   = new AtomicReference<>(new State(DEFAULT, false));

    /**
     * Fast-path cache: populated the first time the configuration is frozen.
     * <p>
     * A plain volatile read is an order of magnitude cheaper than an
     * {@link AtomicReference#updateAndGet} CAS loop, so this field allows
     * {@link #get()} — which is called on every IBAN operation — to bypass
     * the slow path entirely once the configuration has been frozen.
     * <p>
     * {@code null} means "not yet frozen"; any non-null value is the active,
     * immutable {@link IbanConfig} instance. Concurrent writes are safe because
     * the value is always the same immutable object (idempotent assignment).
     */
    private static volatile IbanConfig          activeConfig;

    private final boolean                       validateNcd;
    private final boolean                       calculateNcd;
    private final boolean                       allowSpace;
    private final boolean                       allowLowercase;

    private IbanConfig(final Builder builder) {
        this.validateNcd    = builder.validateNcd;
        this.calculateNcd   = builder.calculateNcd;
        this.allowSpace     = builder.allowSpace;
        this.allowLowercase = builder.allowLowercase;
    }

    /**
     * Returns whether National Check Digit (NCD) validation is enabled in the global configuration.
     * <p>
     * Calling this method freezes the configuration.
     *
     * @return {@code true} if enabled
     */
    public static boolean isValidateNcd() {
        return get().validateNcd;
    }

    /**
     * Returns whether National Check Digit (NCD) calculation is enabled in the global configuration.
     * <p>
     * Calling this method freezes the configuration.
     *
     * @return {@code true} if enabled
     */
    public static boolean isCalculateNcd() {
        return get().calculateNcd;
    }

    /**
     * Returns whether space-tolerance is enabled in the global configuration.
     * <p>
     * Calling this method freezes the configuration.
     *
     * @return {@code true} if enabled
     */
    public static boolean isAllowSpace() {
        return get().allowSpace;
    }

    /**
     * Returns whether case-insensitivity is enabled in the global configuration.
     * <p>
     * Calling this method freezes the configuration.
     *
     * @return {@code true} if enabled
     */
    public static boolean isAllowLowercase() {
        return get().allowLowercase;
    }

    /**
     * Installs a custom global configuration.
     * <p>
     * Must be called before the first invocation of {@link #get()} or any static
     * accessor. Once the configuration is frozen, this method throws an
     * {@link IllegalStateException}.
     *
     * @param config the configuration to install; must not be {@code null}
     * @return the newly installed configuration instance
     * @throws IllegalStateException if the configuration has already been frozen
     */
    public static IbanConfig configure(final IbanConfig config) {
        requireNonNull(config, "Config must not be null");

        IbanConfig result = STATE.updateAndGet(current -> {
            if (current.frozen) {
                throw new IllegalStateException(
                    IbanConfig.class.getSimpleName() + " is already in use and cannot be changed - "
                  + "call configure() before the first get() or accessor invocation");
            }
            return new State(config, false);
        }).config;

        activeConfig = null; // invalidate fast-path cache
        return result;
    }

    /**
     * Returns the global configuration instance and freezes it.
     * <p>
     * Uses a volatile fast-path to avoid a CAS operation on every call once the
     * configuration has been frozen. After this method has been called, any
     * subsequent call to {@link #configure(IbanConfig)} will throw an
     * {@link IllegalStateException}.
     *
     * @return the active {@link IbanConfig} instance; never {@code null}
     */
    static IbanConfig get() {
        // fast path: single volatile read — no CAS, no lambda, no allocation
        // this is the common case for every IBAN operation after startup
        IbanConfig cfg = activeConfig;
        if (cfg != null) {
            return cfg;
        }

        // Slow path: freeze the config via CAS, then promote it to the fast-path
        // cache. Safe without further synchronization: the assigned value is always
        // the same immutable object, so concurrent writes are idempotent.
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
    static IbanConfig reset() {
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
    static IbanConfig reset(final IbanConfig config) {
        requireNonNull(config, "Config must not be null");
        activeConfig = null; // invalidate fast-path cache
        return STATE.updateAndGet(current -> new State(config, false)).config;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + '['
               + "validateNcd="    + validateNcd + ", "
               + "calculateNcd="   + calculateNcd + ", "
               + "allowSpace="     + allowSpace + ", "
               + "allowLowercase=" + allowLowercase
               + ']';
    }

    /**
     * Returns a new {@link Builder} with all options set to their defaults ({@code false}).
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link IbanConfig}.
     * <p>
     * All options default to {@code false}. Only properties that deviate from
     * the default need to be set explicitly.
     */
    public static final class Builder {

        private boolean validateNcd;
        private boolean calculateNcd;
        private boolean allowSpace;
        private boolean allowLowercase;

        private Builder() {
        }

        /**
         * Enables or disables National Check Digit (NCD) validation.
         *
         * @param value {@code true} to enable
         * @return this builder
         */
        public Builder validateNcd(final boolean value) {
            this.validateNcd = value;
            return this;
        }

        /**
         * Enables or disables National Check Digit (NCD) calculation.
         *
         * @param value {@code true} to enable
         * @return this builder
         */
        public Builder calculateNcd(final boolean value) {
            this.calculateNcd = value;
            return this;
        }

        /**
         * Enables or disables space-tolerant IBAN validation.
         *
         * @param value {@code true} to enable
         * @return this builder
         */
        public Builder allowSpace(final boolean value) {
            this.allowSpace = value;
            return this;
        }

        /**
         * Enables or disables case-insensitive IBAN validation.
         *
         * @param value {@code true} to enable
         * @return this builder
         */
        public Builder allowLowercase(final boolean value) {
            this.allowLowercase = value;
            return this;
        }

        /**
         * Builds and returns the configured {@link IbanConfig} instance.
         *
         * @return a new immutable {@link IbanConfig}
         */
        public IbanConfig build() {
            return new IbanConfig(this);
        }
    }

    /**
     * Internal state container to ensure atomic updates of config and frozen flag.
     */
    private static final class State {
        private final IbanConfig config;
        private final boolean    frozen;

        private State(final IbanConfig config, final boolean frozen) {
            this.config = config;
            this.frozen = frozen;
        }
    }

}

