package de.speedbanking.iban;

import de.speedbanking.util.Iso3166Alpha2;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JUnit 5 annotation for supplying {@link IbanRegistry} entries as parameterized-test arguments.
 * <p>
 * This annotation serves as a specialized, filterable replacement for
 * {@code @EnumSource(IbanRegistry.class)}:
 * <p>
 * {@code @EnumSource(IbanRegistry.class)} ➔ {@code @IbanRegistrySource}
 * <p>
 * Multiple attribute filters ({@link #countryType()}, {@link #sepa()}, {@link #currency()},
 * {@link #minIbanLength()}, {@link #maxIbanLength()}) are by default combined with AND semantics.
 * This can be changed to OR semantics via {@link #filterMode()}.
 * <p>
 * {@link #value()} always acts as an explicit include list and is applied independently
 * (before the attribute filters). {@link #exclude()} always removes entries and is applied last,
 * regardless of {@link #filterMode()}.
 * <p>
 * Example usages:
 * <pre>{@code
 * // All IbanRegistry entries
 * @IbanRegistrySource
 *
 * // Only Germany and Austria
 * @IbanRegistrySource({IbanRegistry.DE, IbanRegistry.AT})
 *
 * // All SEPA base countries
 * @IbanRegistrySource(countryType = CountryType.BASE, sepa = Sepa.YES)
 *
 * // All Eurozone countries (EUR currency, SEPA member) — AND semantics (default)
 * @IbanRegistrySource(sepa = Sepa.YES, currency = "EUR")
 *
 * // SEPA members OR EUR countries — OR semantics
 * @IbanRegistrySource(sepa = Sepa.YES, currency = "EUR", filterMode = FilterMode.ANY)
 *
 * // All short IBANs (16–20 chars)
 * @IbanRegistrySource(minIbanLength = 16, maxIbanLength = 20)
 *
 * // All EUR or GBP countries
 * @IbanRegistrySource(currency = {"EUR", "GBP"})
 *
 * // All entries except DE and AT
 * @IbanRegistrySource(exclude = {IbanRegistry.DE, IbanRegistry.AT})
 * }</pre>
 *
 * @since 1.8.5
 *
 * @see IbanRegistry
 * @see org.junit.jupiter.params.provider.EnumSource
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(IbanRegistrySource.IbanRegistryArgumentsProvider.class)
public @interface IbanRegistrySource {

    /**
     * Explicit {@link IbanRegistry} entries to include (e.g., {@code {IbanRegistry.DE, IbanRegistry.AT}}).
     * <p>
     * Type-safe shorthand: invalid enum constants are caught at compile time, not at runtime.
     * <p>
     * When combined with attribute filters ({@link #countryType()}, {@link #sepa()},
     * {@link #currency()}, {@link #minIbanLength()}, {@link #maxIbanLength()}), those filters
     * are applied to this restricted set. The combination mode is controlled by {@link #filterMode()}.
     * An empty array (the default) means no restriction by country — all entries are candidates.
     */
    IbanRegistry[] value() default {};

    /**
     * Explicit {@link IbanRegistry} entries to exclude.
     * <p>
     * Entries listed here are removed from the result set after all other filters have been applied.
     * Exclusion is always applied unconditionally and is not affected by {@link #filterMode()}.
     * <p>
     * Useful when almost all entries are needed except a few known outliers:
     * <pre>{@code
     * @IbanRegistrySource(exclude = {IbanRegistry.DE, IbanRegistry.AT})
     * }</pre>
     */
    IbanRegistry[] exclude() default {};

    /**
     * Filter by the hierarchy type of the country (base vs. derived).<br>
     * Defaults to {@link CountryType#ANY} (no restriction).
     */
    CountryType countryType() default CountryType.ANY;

    /**
     * Filter by SEPA (Single Euro Payments Area) membership.<br>
     * Defaults to {@link Sepa#ANY} (no restriction).
     */
    Sepa sepa() default Sepa.ANY;

    /**
     * Filter by ISO 4217 currency code(s) (e.g., {@code "EUR"}, {@code {"EUR", "GBP"}}).
     * <p>
     * The currency code is resolved via {@link Iso3166Alpha2#getCurrency()}, keyed by the
     * entry's two-letter country code. Entries whose country code cannot be resolved in
     * {@link Iso3166Alpha2} (e.g. non-standard territory codes) are excluded when this filter
     * is active.
     * <p>
     * An empty array (the default) means no restriction by currency.
     */
    String[] currency() default {};

    /**
     * Minimum IBAN length filter (inclusive).
     * <p>
     * Only entries with {@code getIbanLength() >= minIbanLength} are included.
     * A value of {@code 0} (the default) disables this lower bound.
     */
    int minIbanLength() default 0;

    /**
     * Maximum IBAN length filter (inclusive).
     * <p>
     * Only entries with {@code getIbanLength() <= maxIbanLength} are included.
     * A value of {@code 0} (the default) disables this upper bound.
     */
    int maxIbanLength() default 0;

    /**
     * Controls how multiple attribute filters ({@link #countryType()}, {@link #sepa()},
     * {@link #currency()}, {@link #minIbanLength()}, {@link #maxIbanLength()}) are combined.
     * <p>
     * Defaults to {@link FilterMode#ALL} (AND semantics): an entry must satisfy every
     * active attribute filter to be included.
     * <p>
     * {@link FilterMode#ANY} (OR semantics): an entry is included if it satisfies at
     * least one active attribute filter.
     * <p>
     * Note: {@link #value()} (include list) and {@link #exclude()} are not affected by
     * this setting; they are always applied unconditionally.
     */
    FilterMode filterMode() default FilterMode.ALL;

    // -------------------------------------------------------------------------
    // Nested enums
    // -------------------------------------------------------------------------

    enum CountryType {
        /** Include only base countries. */
        BASE,
        /** Include only derived countries. */
        DERIVED,
        /** Include both base and derived countries (no restriction). */
        ANY
    }

    enum Sepa {
        /** Include only SEPA members. */
        YES,
        /** Include only non-SEPA members. */
        NO,
        /** Include both SEPA and non-SEPA members (no restriction). */
        ANY
    }

    enum FilterMode {
        /**
         * An entry must satisfy <em>all</em> active attribute filters (AND semantics).
         * This is the default.
         */
        ALL,
        /**
         * An entry is included if it satisfies <em>at least one</em> active attribute filter
         * (OR semantics).
         */
        ANY
    }

    // -------------------------------------------------------------------------
    // Provider implementation
    // -------------------------------------------------------------------------

    /**
     * Implementation of {@link ArgumentsProvider} for {@link IbanRegistrySource}.
     */
    class IbanRegistryArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<IbanRegistrySource> {

        private IbanRegistrySource config;

        /**
         * Receives the annotation instance and stores it as the active configuration.
         *
         * @param annotation the {@link IbanRegistrySource} annotation on the test method
         */
        @Override
        public void accept(IbanRegistrySource annotation) {
            this.config = annotation;
        }

        /**
         * Provides the filtered stream of {@link IbanRegistry} entries as test arguments.
         * <p>
         * Implements the JUnit 5.12+ two-parameter overload of {@link ArgumentsProvider}.
         * {@code parameters} is not used by this provider.
         *
         * @param parameters parameter declarations of the test method (unused)
         * @param context    the current extension context
         * @return a non-empty stream of {@link Arguments}, each wrapping one {@link IbanRegistry} entry
         * @throws IllegalStateException if the configured filters match no entries
         */
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {

            // Step 1: build include / exclude sets — O(1) lookup via EnumSet
            // No upfront name validation needed: the compiler already rejects unknown enum constants.
            Set<IbanRegistry> includeFilter = config.value().length > 0
                ? EnumSet.copyOf(Arrays.asList(config.value()))
                : null;

            Set<IbanRegistry> excludeFilter = config.exclude().length > 0
                ? EnumSet.copyOf(Arrays.asList(config.exclude()))
                : null;

            // Step 2: build attribute predicates
            List<Predicate<IbanRegistry>> predicates = buildAttributePredicates();

            // Step 3: compose the combined attribute predicate (AND vs. OR)
            Predicate<IbanRegistry> attributePredicate = predicates.isEmpty()
                ? null
                : config.filterMode() == FilterMode.ALL
                    ? predicates.stream().reduce(e -> true,  Predicate::and)
                    : predicates.stream().reduce(e -> false, Predicate::or);

            // Step 4: apply all filters
            Stream<IbanRegistry> stream = Arrays.stream(IbanRegistry.values());

            if (includeFilter != null) {
                stream = stream.filter(includeFilter::contains);
            }
            if (attributePredicate != null) {
                stream = stream.filter(attributePredicate);
            }
            if (excludeFilter != null) {
                stream = stream.filter(e -> !excludeFilter.contains(e));
            }

            List<IbanRegistry> result = stream.collect(Collectors.toList());

            // Step 5: safety check — an empty result means the filters excluded everything.
            // All enum constants are valid by definition, so this can only happen when
            // the filter combination is too restrictive.
            if (result.isEmpty()) {
                throw new IllegalStateException(
                    "No IbanRegistry entries matched " + describe(config)
                    + ". Check that the filter combination is not contradictory "
                    + "(e.g. sepa=YES with a non-SEPA currency) or overly restrictive.");
            }

            return result.stream().map(Arguments::of);
        }

        /**
         * Builds the list of attribute predicates from the annotation configuration.
         * Each active attribute filter produces exactly one predicate.
         *
         * @return a mutable list of predicates; may be empty if no attribute filter is active
         */
        private List<Predicate<IbanRegistry>> buildAttributePredicates() {
            List<Predicate<IbanRegistry>> predicates = new ArrayList<>();

            // country hierarchy type
            if (config.countryType() == CountryType.BASE) {
                predicates.add(IbanRegistry::isBaseCountry);
            } else if (config.countryType() == CountryType.DERIVED) {
                predicates.add(IbanRegistry::isDerivedCountry);
            }

            // SEPA membership
            if (config.sepa() == Sepa.YES) {
                predicates.add(IbanRegistry::isSepa);
            } else if (config.sepa() == Sepa.NO) {
                predicates.add(IbanRegistry::isNotSepa);
            }

            // IBAN length range
            if (config.minIbanLength() > 0) {
                int min = config.minIbanLength();
                predicates.add(e -> e.getIbanLength() >= min);
            }
            if (config.maxIbanLength() > 0) {
                int max = config.maxIbanLength();
                predicates.add(e -> e.getIbanLength() <= max);
            }

            // currency (ISO 4217) — resolved via Iso3166Alpha2
            if (config.currency().length > 0) {
                Set<String> currencyCodes = new LinkedHashSet<>(Arrays.asList(config.currency()));
                predicates.add(e -> {
                    String code = resolveCurrencyCode(e);
                    return code != null && currencyCodes.contains(code);
                });
            }

            return predicates;
        }

        /**
         * Resolves the ISO 4217 currency code for a given registry entry via {@link Iso3166Alpha2}.
         * <p>
         * Returns {@code null} if the entry's country code is not present in {@link Iso3166Alpha2}
         * (e.g., non-standard territory codes that are not ISO 3166-1 members).
         *
         * @param entry the registry entry to resolve
         * @return the ISO 4217 currency code, or {@code null} if unresolvable
         */
        private static String resolveCurrencyCode(IbanRegistry entry) {
            Iso3166Alpha2 countryCode = Iso3166Alpha2.fromCode(entry.name());
            return countryCode == null ? null : countryCode.getCurrency().getAlphaCode();
        }

        /**
         * Returns a human-readable description of the {@link IbanRegistrySource} configuration,
         * used in error messages.
         *
         * @param config the annotation instance
         * @return a string of the form
         *         {@code IbanRegistrySource[value=*, exclude=-, countryType=ANY, ...]},
         *         where {@code *} denotes an unrestricted filter and {@code -} an empty exclude list
         */
        static String describe(IbanRegistrySource config) {
            return String.format(
                "%s[value=%s, exclude=%s, countryType=%s, sepa=%s, currency=%s, "
                + "minIbanLength=%s, maxIbanLength=%s, filterMode=%s]",
                IbanRegistrySource.class.getSimpleName(),
                config.value().length == 0
                    ? "*"
                    : Arrays.stream(config.value()).map(IbanRegistry::getCountryCode).collect(Collectors.joining(", ")),
                config.exclude().length == 0
                    ? "-"
                    : Arrays.stream(config.exclude()).map(IbanRegistry::getCountryCode).collect(Collectors.joining(", ")),
                config.countryType(),
                config.sepa(),
                config.currency().length == 0
                    ? "*"
                    : String.join(", ", config.currency()),
                config.minIbanLength() > 0 ? config.minIbanLength() : "*",
                config.maxIbanLength() > 0 ? config.maxIbanLength() : "*",
                config.filterMode());
        }
    }
}
