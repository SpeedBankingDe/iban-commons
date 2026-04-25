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
package de.speedbanking.iban.junit.jupiter.params.provider;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import de.speedbanking.iban.IbanRegistry;
import de.speedbanking.iban.IbanValidator;
import de.speedbanking.iban.RandomIban;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JUnit 5 annotation that provides a stream of randomly generated IBAN strings
 * for parameterized tests.
 * <p>
 * Each yielded {@link Arguments} instance contains a single randomly generated IBAN string —
 * valid or intentionally corrupted, depending on {@link #invalidPercentage()}.
 * <p>
 * The pool of countries used for generation can be narrowed via {@link #value()} or
 * {@link #includeCountries()} (mutually exclusive alternatives) and further reduced via
 * {@link #excludeCountries()}, which is always applied on top of the effective include list.
 * <p>
 * Example usages:
 * <pre>{@code
 * // 1 000 random IBANs from all countries
 * @RandomIbanSource
 *
 * // 500 random IBANs, only German IBANs
 * @RandomIbanSource(value = {IbanRegistry.DE}, count = 500)
 *
 * // 200 IBANs, 20 % intentionally invalid, all countries except Kosovo and Kosovo-derived entries
 * @RandomIbanSource(excludeCountries = {IbanRegistry.XK}, count = 200, invalidPercentage = 20)
 * }</pre>
 *
 * @since 1.8.5
 *
 * @see IbanRegistry
 * @see RandomIban
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(RandomIbanSource.RandomIbanArgumentsProvider.class)
public @interface RandomIbanSource {

    /**
     * Shorthand for {@link #includeCountries()}.
     * <p>
     * Specifying countries here is equivalent to specifying them in {@link #includeCountries()}.
     * Both attributes must not be used simultaneously — doing so causes an
     * {@link IllegalArgumentException} at test discovery time.
     * <p>
     * Example: {@code @RandomIbanSource({IbanRegistry.DE, IbanRegistry.AT})}
     */
    IbanRegistry[] value() default {};

    /**
     * Number of IBAN strings to generate. Must be positive. Defaults to 1 000.
     */
    int count() default 1000;

    /**
     * Percentage of generated IBANs that will be deliberately corrupted.
     * <p>
     * Must be between 0 (default, all valid) and 100 (all invalid).
     * Corruption is performed via {@link RandomIban#invalidString(String, java.util.Random)},
     * which applies a randomly chosen sabotage strategy (check digit tamper, wrong country
     * code, structural BBAN violation, transposition, or illegal length).
     */
    int invalidPercentage() default 0;

    /**
     * {@link IbanRegistry} entries to include in IBAN generation.
     * <p>
     * If non-empty, only the listed countries are considered.
     * If empty (default), all countries are considered before applying {@link #excludeCountries()}.
     * <p>
     * Prefer the shorthand {@link #value()} for inline use; do not combine both.
     */
    IbanRegistry[] includeCountries() default {};

    /**
     * {@link IbanRegistry} entries to exclude from IBAN generation.
     * <p>
     * Applied after the include filter; entries listed here are removed from the final pool.
     */
    IbanRegistry[] excludeCountries() default {};

    /**
     * Filter by SEPA (Single Euro Payments Area) membership.
     * <p>
     * Defaults to {@link Sepa#ANY} (no restriction).
     */
    Sepa sepa() default Sepa.ANY;

    enum Sepa {
        /** Include only SEPA members. */
        YES,
        /** Include only non-SEPA members. */
        NO,
        /** Include both SEPA and non-SEPA members (no restriction). */
        ANY
    }

    /**
     * {@link ArgumentsProvider} backing {@link RandomIbanSource}.
     */
    class RandomIbanArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<RandomIbanSource> {

        private RandomIbanSource config;

        /**
         * Receives the annotation instance and stores it as the active configuration.
         *
         * @param annotation the {@link RandomIbanSource} annotation on the test method
         */
        @Override
        public void accept(RandomIbanSource annotation) {
            this.config = annotation;
        }

        /**
         * Provides the configured number of randomly generated IBAN strings as test arguments.
         * <p>
         * Implements the JUnit 5.12+ two-parameter overload of {@link ArgumentsProvider}.
         * {@code parameters} is not used by this provider.
         *
         * @param parameters parameter declarations of the test method (unused)
         * @param context    the current extension context
         * @return a stream of {@link Arguments}, each wrapping one IBAN string;
         *         the stream has exactly {@link #count()} elements
         * @throws IllegalArgumentException if {@link #count()} is not positive,
         *                                  or {@link #invalidPercentage()} is outside 0–100,
         *                                  or {@link #value()} and {@link #includeCountries()} are both non-empty,
         *                                  or if the country pool is empty after filtering
         */
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            List<IbanRegistry> pool = validateAndBuild(config);
            ThreadLocalRandom random = ThreadLocalRandom.current();

            return IntStream.range(0, config.count())
                .mapToObj(i -> generateIban(pool, config.invalidPercentage(), random))
                .map(Arguments::of);
        }

        /**
         * Validates the annotation configuration, throwing a descriptive exception on the first
         * violation found, and builds the pool of {@link IbanRegistry} entries from which IBANs will be generated.
         * <p>
         * The effective include list is determined by {@link #value()} (if non-empty) or
         * {@link #includeCountries()}, falling back to all registered countries if both are empty.
         * {@link #excludeCountries()} is then applied on top. An overlap between the include and
         * exclude lists is treated as a configuration error.
         *
         * @param config the annotation configuration
         * @return a non-empty list of countries eligible for IBAN generation
         * @throws IllegalArgumentException if {@link #value()} and {@link #includeCountries()} are both non-empty,
         *                                  or {@link #count()} is not positive,
         *                                  or {@link #invalidPercentage()} is outside 0–100,
         *                                  or the include and exclude lists overlap,
         *                                  or if the resulting pool is empty
         */
        private static List<IbanRegistry> validateAndBuild(RandomIbanSource config) {

            if (config.count() <= 0) {
                throwIllegalArgument("count must be positive, but was " + config.count());
            } else if (config.invalidPercentage() < 0 || config.invalidPercentage() > 100) {
                throwIllegalArgument("invalidPercentage must be 0–100, but was " + config.invalidPercentage());
            }

            Set<IbanRegistry> valueCountries = toCountrySet(config.value());
            Set<IbanRegistry> includeCountries = toCountrySet(config.includeCountries());
            Set<IbanRegistry> excludeCountries = toCountrySet(config.excludeCountries());

            // value() and includeCountries() must not both be specified
            if (!valueCountries.isEmpty() && !includeCountries.isEmpty()) {
                throwIllegalArgument(": value and includeCountries must not be used simultaneously");
            }

            // resolve effective include list
            includeCountries.addAll(valueCountries);

            Stream<IbanRegistry> stream = includeCountries.isEmpty()
                ? Arrays.stream(IbanRegistry.values())
                : includeCountries.stream();

            // filter by SEPA
            if (config.sepa() == Sepa.YES) {
                stream = stream.filter(IbanRegistry::isSepa);
            } else if (config.sepa() == Sepa.NO) {
                stream = stream.filter(IbanRegistry::isNotSepa);
            }

            if (!excludeCountries.isEmpty()) {
                // detect entries that are both included and excluded
                if (!includeCountries.isEmpty()) {
                    List<IbanRegistry> overlap = includeCountries.stream()
                        .filter(excludeCountries::contains)
                        .collect(toList());
                    if (!overlap.isEmpty()) {
                        throwIllegalArgument("countries appear in both include and exclude: "
                            + overlap.stream().map(IbanRegistry::getCountryCode).collect(joining(", ")));
                    }
                }

                stream = stream.filter(c -> !excludeCountries.contains(c));
            }

            List<IbanRegistry> pool = stream.collect(toList());

            if (pool.isEmpty()) {
                throwIllegalArgument("country pool empty after applying the configured filters");
            }

            return pool;
        }

        /**
         * Converts the registry country array into a modifiable set.
         * <p>
         * returns an empty set if the input array is null or empty.
         *
         * @param countries the array to convert
         * @return a modifiable set of registries
         */
        private static Set<IbanRegistry> toCountrySet(IbanRegistry[] countries) {
            Set<IbanRegistry> set = EnumSet.noneOf(IbanRegistry.class);
            if (countries != null && countries.length > 0) {
                set.addAll(Arrays.asList(countries));
            }
            return set;
        }

        private static void throwIllegalArgument(String message) {
            throw new IllegalArgumentException("@" + RandomIbanSource.class.getSimpleName() + ": " + message);
        }

        /**
         * Generates a single IBAN string for a randomly selected country from the pool.
         * <p>
         * With probability {@code invalidPercent / 100}, the generated IBAN is deliberately
         * corrupted via {@link RandomIban#invalidString(String, java.util.Random)}, which applies
         * a randomly chosen sabotage strategy that reliably produces a string that fails
         * {@link IbanValidator#isValid(String)}.
         *
         * @param countries         non-empty list of eligible countries
         * @param invalidPercentage percentage chance (0–100) that the IBAN will be corrupted
         * @param random            the random source to use for country selection and corruption
         * @return a valid or deliberately corrupted IBAN string
         */
        private static String generateIban(List<IbanRegistry> countries, int invalidPercentage, ThreadLocalRandom random) {
            IbanRegistry country = countries.get(random.nextInt(countries.size()));
            String iban = RandomIban.builder()
                .country(country)
                .random(random)
                .build().toString();

            if (invalidPercentage > 0 && random.nextInt(100) < invalidPercentage) {
                iban = RandomIban.invalidString(iban, random);
            }
            return iban;
        }
    }

}

