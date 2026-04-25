package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Tests for the {@link RandomIbanSource} and its provider.
 */
@SuppressWarnings({"checkstyle:MethodName", "checkstyle:LeftCurly"})
final class RandomIbanSourceTest {

    private final RandomIbanSource.RandomIbanArgumentsProvider provider = new RandomIbanSource.RandomIbanArgumentsProvider();

    @ParameterizedTest
    @RandomIbanSource(count = 5, value = {IbanRegistry.DE})
    void provideArguments_shouldGenerateRequestedNumber_ofIbans(String iban) {
        assertThat(iban).startsWith("DE");
        assertThat(iban).hasSizeBetween(15, 34);
    }

    @Test
    void provideArguments_shouldThrowException_whenCountIsNegative() {
        RandomIbanSource mockAnnotation = createAnnotation(null, 0, 0, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count must be positive");
    }

    @Test
    void provideArguments_shouldThrowException_whenPercentageIsTooHigh() {
        RandomIbanSource mockAnnotation = createAnnotation(null, 10, 101, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalidPercentage must be 0–100");
    }

    @Test
    void provideArguments_shouldThrowException_whenPercentageIsNegative() {
        RandomIbanSource mockAnnotation = createAnnotation(null, 10, -1, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalidPercentage must be 0–100");
    }

    @Test
    void validateAndBuild_shouldThrowException_whenValueAndIncludeOverlap() {
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
    void validateAndBuild_shouldThrowException_whenPoolIsEmptyAfterFiltering() {
        // only DE included, but only non-SEPA
        RandomIbanSource mockAnnotation = createAnnotation(
            new IbanRegistry[] {IbanRegistry.DE}, 10, 0,
            null, null, RandomIbanSource.Sepa.NO);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("the country pool is empty");
    }

    @Test
    void validateAndBuild_shouldThrowException_whenIncludedCountryIsExcluded() {
        // base is AT and DE, but DE excluded
        RandomIbanSource mockAnnotation = createAnnotation(
            null, 10, 0,
            new IbanRegistry[] {IbanRegistry.AT, IbanRegistry.DE},
            new IbanRegistry[]{IbanRegistry.DE},
            RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThatThrownBy(() -> provider.provideArguments(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("@RandomIbanSource: Country 'DE' appears in both include and exclude");
    }

    @Test
    void validateAndBuild_shouldFilterBySepa_whenSepaIsYes() {
        // LC (Saint Lucia) is non-SEPA, DE is SEPA
        RandomIbanSource mockAnnotation = createAnnotation(
            new IbanRegistry[]{IbanRegistry.DE, IbanRegistry.LC}, 10, 0,
            null, null, RandomIbanSource.Sepa.YES);
        provider.accept(mockAnnotation);

        Stream<? extends Arguments> args = provider.provideArguments(null, null);
        assertThat(args).allSatisfy(arg -> {
            String iban = (String) arg.get()[0];
            assertThat(Iban.of(iban).isSepa()).isTrue();
        });
    }

    @Test
    void validateAndBuild_shouldFilterBySepa_whenSepaIsNo() {
        RandomIbanSource mockAnnotation = createAnnotation(
            new IbanRegistry[]{IbanRegistry.DE, IbanRegistry.LC}, 10, 0,
            null, null, RandomIbanSource.Sepa.NO);
        provider.accept(mockAnnotation);

        Stream<? extends Arguments> args = provider.provideArguments(null, null);
        assertThat(args).allSatisfy(arg -> {
            String iban = (String) arg.get()[0];
            assertThat(Iban.of(iban).isSepa()).isFalse();
        });
    }

    @Test
    void provideArguments_shouldWork_whenSepaIsAny() {
        // Should not throw and provide both SEPA and non-SEPA if in registry
        RandomIbanSource mockAnnotation = createAnnotation(null, 5, 0, null, null, RandomIbanSource.Sepa.ANY);
        provider.accept(mockAnnotation);

        assertThat(provider.provideArguments(null, null)).hasSize(5);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @RandomIbanSource(value = {IbanRegistry.DE}, count = 10, invalidPercentage = 100)
    void generateIban_shouldProduceCorruptedIban_whenPercentageIsHundred(String iban) {
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
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RandomIbanSource.class; }
            @Override public IbanRegistry[] value() { return value == null ? new IbanRegistry[0] : value; }
            @Override public int count() { return count; }
            @Override public int invalidPercentage() { return invalidPercentage; }
            @Override public IbanRegistry[] includeCountries() { return includeCountries == null ? new IbanRegistry[0] : includeCountries; }
            @Override public IbanRegistry[] excludeCountries() { return excludeCountries == null ? new IbanRegistry[0] : excludeCountries; }
            @Override public RandomIbanSource.Sepa sepa() { return sepa == null ? RandomIbanSource.Sepa.ANY : sepa; }
        };
    }

}

