package de.speedbanking.iban.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.stream.Collectors.toList;

import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;

/**
 * Tests for the {@link RandomIbanSource} and its provider.
 */
@SuppressWarnings("checkstyle:MethodName")
final class RandomIbanSourceTest {

    private final RandomIbanSource.RandomIbanArgumentsProvider provider = new RandomIbanSource.RandomIbanArgumentsProvider();

    @ParameterizedTest
    @RandomIbanSource(count = 5, value = {IbanRegistry.DE})
    void provideArguments_count_generatesRequestedNumberOfIbans(String iban) {
        assertThat(iban).startsWith("DE")
                        .hasSizeBetween(15, 34);
    }

    @Test
    void provideArguments_negativeCount_throwsException() {
        RandomIbanSource mockAnnotation = createAnnotation(null, 0, 0, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count must be positive");
    }

    @Test
    void provideArguments_percentageTooHigh_throwsException() {
        RandomIbanSource mockAnnotation = createAnnotation(null, 10, 101, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalidPercentage must be 0-100");
    }

    @Test
    void provideArguments_negativePercentage_throwsException() {
        RandomIbanSource mockAnnotation = createAnnotation(null, 10, -1, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalidPercentage must be 0-100");
    }

    @Test
    void validateAndBuild_valueAndIncludeOverlap_throwsException() {
        RandomIbanSource mockAnnotation = createAnnotation(
            new IbanRegistry[] {IbanRegistry.DE}, 10, 0,
            new IbanRegistry[] {IbanRegistry.AT},
            null,
            RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be used simultaneously");
    }

    @Test
    void validateAndBuild_emptyPoolAfterFiltering_throwsException() {
        // only DE included, but only non-SEPA
        RandomIbanSource mockAnnotation = createAnnotation(
            new IbanRegistry[] {IbanRegistry.DE}, 10, 0,
            null, null, RandomIbanSource.Sepa.NO);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("@RandomIbanSource: country pool empty after applying the configured filters");
    }

    @Test
    void validateAndBuild_includedCountryExcluded_throwsException() {
        // base is AT and DE, but DE excluded
        RandomIbanSource mockAnnotation = createAnnotation(
            null, 10, 0,
            new IbanRegistry[] {IbanRegistry.AT, IbanRegistry.DE},
            new IbanRegistry[]{IbanRegistry.DE},
            RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("@RandomIbanSource: countries appear in both include and exclude: DE");
    }

    @Test
    void validateAndBuild_excludedCountries_omitsThem() {
        // excludeCountries actually removing entries from the pool - not just the
        // include/exclude-overlap error path exercised elsewhere in this class. A large
        // count against the ~127-entry full registry makes the odds of never drawing DE
        // by chance alone (were the exclude filter a no-op) astronomically small.
        RandomIbanSource mockAnnotation = createAnnotation(
            null, 5000, 0,
            null, new IbanRegistry[] {IbanRegistry.DE},
            RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        Stream<? extends Arguments> args = provider.provideArguments(null, null);
        assertThat(args)
            .hasSize(5000)
            .allSatisfy(arg -> {
                String iban = (String) arg.get()[0];
                assertThat(iban).doesNotStartWith("DE");
            });
    }

    @Test
    void provideArguments_disjointExcludeAndHalfInvalid_mixesValidAndInvalidIbans() {
        // with 200 draws at 50 percent, getting only valid or only invalid IBANs is practically impossible
        RandomIbanSource mockAnnotation = createAnnotation(
            null, 200, 50,
            new IbanRegistry[] {IbanRegistry.AT, IbanRegistry.DE},
            new IbanRegistry[] {IbanRegistry.CH},
            RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        List<String> ibans = provider.provideArguments(null, null)
            .map(arg -> (String) arg.get()[0])
            .collect(toList());

        // a corrupted IBAN may also have a corrupted country code, so only the valid ones are checked for it
        assertThat(ibans).hasSize(200).anyMatch(iban -> !Iban.isValid(iban));
        assertThat(ibans.stream().filter(Iban::isValid)).isNotEmpty().allMatch(iban -> iban.matches("(AT|DE).*"));
    }

    @Test
    void validateAndBuild_sepaYes_filtersBySepa() {
        // LC (Saint Lucia) is non-SEPA, DE is SEPA
        RandomIbanSource mockAnnotation = createAnnotation(
            new IbanRegistry[]{IbanRegistry.DE, IbanRegistry.LC}, 10, 0,
            null, null, RandomIbanSource.Sepa.YES);
        provider.accept(mockAnnotation);

        Stream<? extends Arguments> args = provider.provideArguments(null, null);
        assertThat(args)
            .isNotEmpty()
            .allSatisfy(arg -> {
                String iban = (String) arg.get()[0];
                assertThat(Iban.of(iban).isSepa()).isTrue();
            });
    }

    @Test
    void validateAndBuild_sepaNo_filtersByNonSepa() {
        RandomIbanSource mockAnnotation = createAnnotation(
            new IbanRegistry[]{IbanRegistry.DE, IbanRegistry.LC}, 10, 0,
            null, null, RandomIbanSource.Sepa.NO);
        provider.accept(mockAnnotation);

        Stream<? extends Arguments> args = provider.provideArguments(null, null);
        assertThat(args)
            .isNotEmpty()
            .allSatisfy(arg -> {
                String iban = (String) arg.get()[0];
                assertThat(Iban.of(iban).isSepa()).isFalse();
            });
    }

    @Test
    void provideArguments_sepaAny_generatesIbans() {
        // Should not throw and provide both SEPA and non-SEPA if in registry
        RandomIbanSource mockAnnotation = createAnnotation(null, 5, 0, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThat(provider.provideArguments(null, null)).hasSize(5);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @RandomIbanSource(value = {IbanRegistry.DE}, count = 10, invalidPercentage = 100)
    void generateIban_percentageHundred_producesCorruptedIban(String iban) {
        // at 100%, every IBAN should fail validation
        assertThat(Iban.isValid(iban))
            .as("Validation should fail for iban '%s'", iban)
            .isFalse();
    }

    /**
     * Helper to create a proxy or mock of the annotation for unit testing the provider logic.
     */
    @SuppressWarnings("BadAnnotationImplementation")
    private RandomIbanSource createAnnotation(IbanRegistry[] value, int count, int invalidPercentage,
                                              IbanRegistry[] includeCountries, IbanRegistry[] excludeCountries,
                                              RandomIbanSource.Sepa sepa) {
        return new RandomIbanSource() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RandomIbanSource.class;
            }

            @Override
            public IbanRegistry[] value() {
                return value == null ? new IbanRegistry[0] : value;
            }

            @Override
            public int count() {
                return count;
            }

            @Override
            public int invalidPercentage() {
                return invalidPercentage;
            }

            @Override
            public IbanRegistry[] includeCountries() {
                return includeCountries == null ? new IbanRegistry[0] : includeCountries;
            }

            @Override
            public IbanRegistry[] excludeCountries() {
                return excludeCountries == null ? new IbanRegistry[0] : excludeCountries;
            }

            @Override
            public RandomIbanSource.Sepa sepa() {
                return sepa == null ? RandomIbanSource.Sepa.ANY : sepa;
            }
        };
    }

}

