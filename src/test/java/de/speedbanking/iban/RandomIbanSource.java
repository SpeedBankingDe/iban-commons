package de.speedbanking.iban;

import de.speedbanking.iban.RandomIbanSource.RandomIbanArgumentsProvider;
import de.speedbanking.util.TestUtil;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * {@code RandomIbanSource} is a JUnit Jupiter {@link ArgumentsSource} used on parameterized test methods
 * to provide a stream of randomly generated IBAN Strings.
 * <p>
 * Each argument stream yields an {@link Arguments} instance containing
 * a randomly generated IBAN String (valid or invalid, based on {@link #invalidIbanPercentage()}).
 * <p>
 * The source of valid IBAN country codes can be filtered using the {@link #includeCountries()} and {@link #excludeCountries()} parameters.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(RandomIbanArgumentsProvider.class)
public @interface RandomIbanSource {

    int ibanCount() default 1000;

    /**
     * Percentage of generated IBANs that should be made invalid by swapping two random characters.
     * Value must be between 0 (default) and 100.
     */
    int invalidIbanPercentage() default 0;

    /**
     * Optional {@link IbanRegistry} constants to include for IBAN generation.
     * <p>
     * If specified, only the country codes listed here will be considered. If not specified (default),
     * all available country codes are initially taken into consideration before applying {@link #excludeCountries()}.
     */
    IbanRegistry[] includeCountries() default {};

    /**
     * Optional {@link IbanRegistry} constants to exclude from IBAN generation.
     * <p>
     * Country codes listed here will be filtered out from the final list of countries used.
     */
    IbanRegistry[] excludeCountries() default {};

    class RandomIbanArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // find the annotation
            RandomIbanSource src = context.getElement()
                .flatMap(elem -> AnnotationSupport.findAnnotation(elem, RandomIbanSource.class))
                .orElseThrow(() -> new IllegalStateException("@RandomIbanSource annotation not found on the test element"));

            // Parameter validation
            if (src.ibanCount() <= 0) {
                throw new IllegalArgumentException("ibanCount must be a positive number, but was " + src.ibanCount());
            }
            if (src.invalidIbanPercentage() < 0 || src.invalidIbanPercentage() > 100) {
                throw new IllegalArgumentException("invalidIbanPercentage must be between 0 and 100, but was " + src.invalidIbanPercentage());
            }

            // determine the final list of included IbanRegistry entries
            List<IbanRegistry> includeCountries = new ArrayList<>(Arrays.asList(src.includeCountries()));
            if (includeCountries.isEmpty()) {
                includeCountries.addAll(Arrays.asList(IbanRegistry.values()));
            }

            includeCountries.removeAll(Arrays.asList(src.excludeCountries()));

            List<Arguments> ibans = new ArrayList<>(src.ibanCount());

            for (int i = 0; i < src.ibanCount(); i++) {
                String iban;
                if (includeCountries.isEmpty()) {
                    iban = RandomIban.of().toString();
                } else {
                    int randomCcIdx = random.nextInt(includeCountries.size());
                    IbanRegistry randomCountry = includeCountries.get(randomCcIdx);
                    iban = RandomIban.of(randomCountry).toString();
                }

                // Die invalidIbanPercentage gibt die Wahrscheinlichkeit an, dass ein IBAN ungültig ist.
                // Ein Zufallswert von 1 bis 100 wird mit dem Prozentsatz verglichen.
                if (random.nextInt(100) + 1 <= src.invalidIbanPercentage()) {
                    iban = TestUtil.swapRandomChars(iban);
                }
                ibans.add(Arguments.of(iban));
            }

            return ibans.stream();
        }

    }

}
