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
     * **Note:** The names must match existing enum constants, otherwise an {@link IllegalArgumentException} is thrown
     * by the calling code.
     */
    IbanRegistry[] includeCountries() default {};

    /**
     * Optional names of {@link IbanRegistry} enum constants to exclude.
     * <p>
     * Constants listed here will be filtered out from the final list of arguments.
     * <p>
     * **Note:** The names must match existing enum constants, otherwise an {@link IllegalArgumentException} is thrown
     * by the calling code.
     */
    IbanRegistry[] excludeCountries() default {};

    class IbanCountryArgumentsProvider implements ArgumentsProvider {

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

            // filter the included stream by the excluded set and map to Arguments
            return includeCountries
                .filter(registry -> !excludedCountries.contains(registry))
                .map(registry -> Arguments.of(registry.getCountryCode(), registry.getCountryName()));
        }

    }

}
