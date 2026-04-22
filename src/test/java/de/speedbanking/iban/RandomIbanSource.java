package de.speedbanking.iban;

import static java.util.stream.Collectors.toList;

import de.speedbanking.test.TestUtil;

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
 * valid or intentionally corrupted, depending on {@link #invalidIbanPercentage()}.
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
 * @RandomIbanSource(value = {IbanRegistry.DE}, ibanCount = 500)
 *
 * // 200 IBANs, 20 % intentionally invalid, all countries except Kosovo and Kosovo-derived entries
 * @RandomIbanSource(excludeCountries = {IbanRegistry.XK}, ibanCount = 200, invalidIbanPercentage = 20)
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
     * {@link IllegalStateException} at test discovery time.
     * <p>
     * Example: {@code @RandomIbanSource({IbanRegistry.DE, IbanRegistry.AT})}
     */
    IbanRegistry[] value() default {};

    /**
     * Number of IBAN strings to generate. Must be positive. Defaults to 1 000.
     */
    int ibanCount() default 1000;

    /**
     * Percentage of generated IBANs that will be deliberately corrupted (two characters swapped).
     * <p>
     * Must be between 0 (default, all valid) and 100 (all invalid).
     */
    int invalidIbanPercentage() default 0;

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
         *         the stream has exactly {@link #ibanCount()} elements
         * @throws IllegalArgumentException if {@link #ibanCount()} is not positive or
         *                                  {@link #invalidIbanPercentage()} is outside 0–100
         * @throws IllegalStateException    if {@link #value()} and {@link #includeCountries()} are
         *                                  both non-empty, or if the country pool is empty after filtering
         */
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            validate(config);

            List<IbanRegistry> pool = buildPool(config);
            ThreadLocalRandom random = ThreadLocalRandom.current();

            return IntStream.range(0, config.ibanCount())
                .mapToObj(i -> generateIban(pool, config.invalidIbanPercentage(), random))
                .map(Arguments::of);
        }

        /**
         * Validates the annotation configuration and throws a descriptive exception on the first
         * violation found.
         *
         * @param cfg the annotation configuration to validate
         * @throws IllegalStateException    if {@link #value()} and {@link #includeCountries()} are
         *                                  both non-empty
         * @throws IllegalArgumentException if {@link #ibanCount()} is not positive or
         *                                  {@link #invalidIbanPercentage()} is outside 0–100
         */
        private static void validate(RandomIbanSource cfg) {
            // value() and includeCountries() must not both be specified
            if (cfg.value().length > 0 && cfg.includeCountries().length > 0) {
                throw new IllegalStateException(
                    "@" + RandomIbanSource.class.getSimpleName() + ": 'value' and 'includeCountries' must not be used simultaneously. "
                    + "Use 'value' as the shorthand and leave 'includeCountries' empty.");
            }

            if (cfg.ibanCount() <= 0) {
                throw new IllegalArgumentException(
                    "@" + RandomIbanSource.class.getSimpleName() + ": ibanCount must be positive, but was " + cfg.ibanCount());
            }
            if (cfg.invalidIbanPercentage() < 0 || cfg.invalidIbanPercentage() > 100) {
                throw new IllegalArgumentException(
                    "@" + RandomIbanSource.class.getSimpleName() + ": invalidIbanPercentage must be 0–100, but was " + cfg.invalidIbanPercentage());
            }
        }

        /**
         * Builds the pool of {@link IbanRegistry} entries from which IBANs will be generated.
         * <p>
         * The effective include list is determined by {@link #value()} (if non-empty) or
         * {@link #includeCountries()}, falling back to all registered countries if both are empty.
         * {@link #excludeCountries()} is then applied on top. An overlap between the include and
         * exclude lists is treated as a configuration error.
         *
         * @param cfg the annotation configuration
         * @return a non-empty list of countries eligible for IBAN generation
         * @throws IllegalStateException if the include and exclude lists overlap, or if the
         *                               resulting pool is empty
         */
        private static List<IbanRegistry> buildPool(RandomIbanSource cfg) {
            // resolve effective include list (value() takes precedence)
            IbanRegistry[] includes = cfg.value().length > 0 ? cfg.value() : cfg.includeCountries();

            Stream<IbanRegistry> stream = includes.length == 0
                ? Arrays.stream(IbanRegistry.values())
                : Arrays.stream(includes);

            // apply exclude filter using EnumSet for O(1) lookup
            if (cfg.excludeCountries().length > 0) {
                Set<IbanRegistry> excludeSet = EnumSet.copyOf(Arrays.asList(cfg.excludeCountries()));

                // detect entries that are both included and excluded
                if (includes.length > 0) {
                    List<IbanRegistry> overlap = Arrays.stream(includes)
                        .filter(excludeSet::contains)
                        .collect(toList());
                    if (!overlap.isEmpty()) {
                        throw new IllegalStateException(
                            "@" + RandomIbanSource.class.getSimpleName() + ": the following countries appear in both include and exclude: " + overlap);
                    }
                }

                stream = stream.filter(c -> !excludeSet.contains(c));
            }

            List<IbanRegistry> pool = stream.collect(toList());

            if (pool.isEmpty()) {
                throw new IllegalStateException(
                    "@" + RandomIbanSource.class.getSimpleName() + ": the country pool is empty after applying the configured filters. "
                    + "Check that includeCountries/value and excludeCountries do not cancel each other out.");
            }

            return pool;
        }

        /**
         * Generates a single IBAN string for a randomly selected country from the pool.
         * <p>
         * With probability {@code invalidPercent / 100}, the generated IBAN is deliberately
         * corrupted by swapping two randomly chosen characters via {@link TestUtil#swapRandomChars}.
         *
         * @param pool           non-empty list of eligible countries
         * @param invalidPercent percentage chance (0–100) that the IBAN will be corrupted
         * @param random         the random source to use for country selection and corruption
         * @return a valid or deliberately corrupted IBAN string
         */
        private static String generateIban(List<IbanRegistry> pool, int invalidPercent, ThreadLocalRandom random) {
            IbanRegistry country = pool.get(random.nextInt(pool.size()));
            String iban = RandomIban.builder()
                .country(country)
                .random(random)
                .build().toString();

            // corrupt the IBAN with the requested probability
            if (invalidPercent > 0 && random.nextInt(100) < invalidPercent) {
                iban = TestUtil.swapRandomChars(iban);
            }
            return iban;
        }
    }

}
