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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import de.speedbanking.iban.IbanRegistry;
import de.speedbanking.iban.junit.jupiter.params.provider.IbanCountrySource.IbanCountryArgumentsProvider;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code IbanCountrySource} is a JUnit Jupiter {@link ArgumentsSource} used on parameterized test methods
 * to provide arguments based on {@link IbanRegistry} enum constants.
 * <p>
 * Each argument stream yields an {@link Arguments} instance containing:
 * <ul>
 *   <li>The country code (e.g., "DE")</li>
 *   <li>The country name (e.g., "Germany")</li>
 * </ul>
 * The list of provided IBAN registries can be filtered using the {@link #includeCountries()} and {@link #excludeCountries()} parameters.
 *
 * @since 1.8.6
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(IbanCountryArgumentsProvider.class)
public @interface IbanCountrySource {

    /**
     * Optional names of {@link IbanRegistry} enum constants to include.
     * <p>
     * If specified, only the constants listed here will be considered. If not specified (default),
     * all enum constants are initially taken into consideration before applying {@link #excludeCountries()}.
     * <p>
     * <strong>Note:</strong> entries are type-safe {@link IbanRegistry} enum constants, so an invalid
     * name is already rejected at compile time, not at runtime.
     */
    IbanRegistry[] includeCountries() default {};

    /**
     * Optional names of {@link IbanRegistry} enum constants to exclude.
     * <p>
     * Constants listed here will be filtered out from the final list of arguments.
     * <p>
     * <strong>Note:</strong> entries are type-safe {@link IbanRegistry} enum constants, so an invalid
     * name is already rejected at compile time, not at runtime.
     */
    IbanRegistry[] excludeCountries() default {};

    /**
     * Implementation of {@link ArgumentsProvider} for {@link IbanCountrySource}.
     */
    class IbanCountryArgumentsProvider implements ArgumentsProvider {

        /**
         * Provides the filtered stream of {@link IbanRegistry} entries as test arguments.
         *
         * @param parameters parameter declarations of the test method (unused)
         * @param context    the current extension context
         * @return a non-empty stream of {@link Arguments}, each wrapping a country code and country name
         * @throws IllegalStateException if the {@code @IbanCountrySource} annotation is not present on the
         *                                test element, or if the configured filters match no entries
         */
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            // find the annotation on the element (usually a method) or throw if not present
            IbanCountrySource src = context.getElement()
                .flatMap(elem -> AnnotationSupport.findAnnotation(elem, IbanCountrySource.class))
                .orElseThrow(() -> new IllegalStateException("@IbanCountrySource annotation not found on the test element"));

            // determine the initial stream of included IbanRegistry entries
            Stream<IbanRegistry> includeCountries = src.includeCountries().length == 0
                ? Arrays.stream(IbanRegistry.values())
                : Arrays.stream(src.includeCountries());

            // convert excluded list to a Set for efficient filtering (O(1) lookup)
            Set<IbanRegistry> excludedCountries = Arrays.stream(src.excludeCountries())
                .collect(toSet());

            // filter the included stream by the excluded set
            List<IbanRegistry> result = includeCountries
                .filter(registry -> !excludedCountries.contains(registry))
                .collect(toList());

            if (result.isEmpty()) {
                throw new IllegalStateException(
                    "No " + IbanRegistry.class.getSimpleName() + " entries matched the configured "
                    + "includeCountries/excludeCountries filter of @" + IbanCountrySource.class.getSimpleName() + ".");
            }

            return result.stream().map(registry -> Arguments.of(registry.getCountryCode(), registry.getCountryName()));
        }

    }

}
