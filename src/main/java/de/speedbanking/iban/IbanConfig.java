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

import java.util.Objects;
import java.util.function.Function;

/**
 * Global configuration settings for the IBAN engine.
 * <p>
 * Each enum constant represents a configuration property that can be controlled
 * via System Properties or programmatically.
 * <p>
 * This enum is thread-safe as the underlying values are marked {@code volatile}.
 *
 * @since 1.8.5
 */
@SuppressWarnings({"ImmutableEnumChecker", "TypeParameterUnusedInFormals"}) // state is intentionally mutable via volatile field 'value'
public enum IbanConfig {

    /**
     * Whether to validate National Check Digits (NCD) for supported countries.
     */
    NCD_VALIDATE("iban.ncd.validate", Boolean::parseBoolean, false),

    /**
     * Whether to calculate National Check Digits (NCD) when generating IBANs.
     */
    NCD_CALCULATE("iban.ncd.calculate", Boolean::parseBoolean, false),

    /**
     * Whether to allow spaces during validation.
     * <p>
     * If enabled, the validator will internally remove spaces from the input.
     */
    ALLOW_SPACE("iban.allow.space", Boolean::parseBoolean, false),

    /**
     * Whether to allow lowercase characters during validation.
     * <p>
     * If enabled, the validator will internally treat the input as uppercase.
     */
    ALLOW_LOWERCASE("iban.allow.lowercase", Boolean::parseBoolean, false);

    private final String              systemProperty;
    private final Function<String, ?> parser;
    private final Object              defaultValue;
    private volatile Object           value;

    <T> IbanConfig(final String systemProperty, final Function<String, T> parser, final T defaultValue) {
        this.systemProperty = Objects.requireNonNull(systemProperty);
        this.parser = Objects.requireNonNull(parser);
        this.defaultValue = defaultValue;
        this.value = readSystemProperty();
    }

    /**
     * Resets the configuration value to its initial state.
     * <p>
     * Re-reads the System Property or falls back to the hardcoded default.
     */
    public void reset() {
        this.value = readSystemProperty();
    }

    /**
     * Returns the current value of this configuration in a type-safe manner.
     * <p>
     * Note: Use this method when the expected type is known at the call site.
     *
     * @param <T> the expected return type
     * @return the current value
     */
    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) value;
    }

    /**
     * Checks if the configuration is enabled (true).
     * <p>
     * This is a convenience method for boolean properties to avoid type casting or inference issues.
     *
     * @return true if the value is {@link Boolean#TRUE}
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(value);
    }

    /**
     * Checks if the configuration is disabled (false).
     *
     * @return true if the value is {@link Boolean#FALSE}
     */
    public boolean isDisabled() {
        return Boolean.FALSE.equals(value);
    }

    /**
     * Programmatically overrides the configuration value.
     * <p>
     * The value is updated immediately for all threads.
     *
     * @param <T>      the type of the value
     * @param newValue the new value to set
     */
    public <T> void set(final T newValue) {
        this.value = newValue;
    }

    /**
     * Enables this configuration setting if it is of type {@link Boolean}.
     *
     * @throws UnsupportedOperationException if the configuration is not a boolean type
     */
    public void enable() {
        if (!(defaultValue instanceof Boolean)) {
            throw new UnsupportedOperationException("Method enable() is only supported for boolean properties");
        }
        set(true);
    }

    /**
     * Disables this configuration setting if it is of type {@link Boolean}.
     *
     * @throws UnsupportedOperationException if the configuration is not a boolean type
     */
    public void disable() {
        if (!(defaultValue instanceof Boolean)) {
            throw new UnsupportedOperationException("Method disable() is only supported for boolean properties");
        }
        set(false);
    }

    /**
     * Reads the value from the configured System Property.
     *
     * @return the parsed value from the system property, or the default value if not set
     */
    private Object readSystemProperty() {
        final String sysProp = System.getProperty(systemProperty);
        return sysProp == null ? defaultValue : parser.apply(sysProp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name() + "=" + value + ']';
    }

    /**
     * Resets all configuration constants to their default state.
     * <p>
     * Useful for clearing state in test suites.
     */
    public static void resetAll() {
        for (IbanConfig config : values()) {
            config.reset();
        }
    }

}
