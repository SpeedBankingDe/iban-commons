package de.speedbanking.iban.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.speedbanking.iban.IbanRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Optional;

/**
 * Unit tests for {@link IbanCountrySource}.
 * <p>
 * Because {@link IbanCountrySource} is a meta-annotation backed by an inner
 * {@link org.junit.jupiter.params.provider.ArgumentsProvider}, the most natural way
 * to test it is to use it directly as a {@link ParameterizedTest} source and assert
 * on the arguments it provides.  Edge-cases that cannot be triggered via a normal
 * annotation (e.g. missing annotation on the element) are tested via the provider
 * directly.
 */
@SuppressWarnings("checkstyle:MethodName")
final class IbanCountrySourceTest {

    // =========================================================================
    // Default (no filter) — all countries
    // =========================================================================

    /** Total number of {@link IbanRegistry} constants. */
    private static final int TOTAL_COUNTRIES = IbanRegistry.values().length;

    /**
     * {@code @IbanCountrySource} with no arguments must yield every
     * {@link IbanRegistry} entry exactly once.
     */
    @ParameterizedTest
    @IbanCountrySource
    void noFilter_yieldsAllCountries(String countryCode, String countryName) {
        assertThat(countryCode).isNotBlank();
        assertThat(countryName).isNotBlank();
        // the registry entry must exist for the given code
        assertThat(IbanRegistry.getByCode(countryCode)).isNotNull();
    }

    @Test
    void noFilter_countMatchesTotalRegistrySize() {
        long count = countArguments(IbanCountrySourceNoFilter.class);
        assertThat(count).isEqualTo(TOTAL_COUNTRIES);
    }

    /** Marker interface used to count arguments produced by a bare @IbanCountrySource. */
    @IbanCountrySource
    private @interface IbanCountrySourceNoFilter {}

    // =========================================================================
    // includeCountries
    // =========================================================================

    @ParameterizedTest
    @IbanCountrySource(includeCountries = {IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH})
    void includeCountries_yieldsOnlySpecifiedEntries(String countryCode, String countryName) {
        assertThat(countryCode).isIn("DE", "AT", "CH");
        assertThat(countryName).isNotBlank();
    }

    @Test
    void includeCountries_countMatchesSpecifiedSize() {
        long count = countArguments(IbanCountrySourceDeAtCh.class);
        assertThat(count).isEqualTo(3);
    }

    @IbanCountrySource(includeCountries = {IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH})
    private @interface IbanCountrySourceDeAtCh {}

    @ParameterizedTest
    @IbanCountrySource(includeCountries = {IbanRegistry.GB})
    void includeCountries_singleEntry_yieldsExactlyThatEntry(String countryCode, String countryName) {
        assertThat(countryCode).isEqualTo("GB");
        assertThat(countryName).isNotBlank();
    }

    // =========================================================================
    // excludeCountries
    // =========================================================================

    @Test
    void excludeCountries_countIsReducedByExcludedSize() {
        long count = countArguments(IbanCountrySourceExcludeDeAt.class);
        assertThat(count).isEqualTo(TOTAL_COUNTRIES - 2);
    }

    @IbanCountrySource(excludeCountries = {IbanRegistry.DE, IbanRegistry.AT})
    private @interface IbanCountrySourceExcludeDeAt {}

    @ParameterizedTest
    @IbanCountrySource(excludeCountries = {IbanRegistry.DE, IbanRegistry.AT})
    void excludeCountries_excludedEntriesAreAbsent(String countryCode, String countryName) {
        assertThat(countryCode).isNotIn("DE", "AT");
    }

    // =========================================================================
    // includeCountries + excludeCountries combined
    // =========================================================================

    @Test
    void includeAndExclude_combined_yieldsCorrectSubset() {
        long count = countArguments(IbanCountrySourceIncludeDeAtChExcludeCh.class);
        // DE, AT included; CH excluded → 2
        assertThat(count).isEqualTo(2);
    }

    @IbanCountrySource(includeCountries = {IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH},
                       excludeCountries = {IbanRegistry.CH})
    private @interface IbanCountrySourceIncludeDeAtChExcludeCh {}

    @ParameterizedTest
    @IbanCountrySource(includeCountries = {IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH},
                       excludeCountries = {IbanRegistry.CH})
    void includeAndExclude_combined_excludedEntryAbsent(String countryCode, String countryName) {
        assertThat(countryCode).isIn("DE", "AT")
                               .isNotEqualTo("CH");
    }

    // =========================================================================
    // Argument content — countryCode and countryName values
    // =========================================================================

    @ParameterizedTest
    @IbanCountrySource(includeCountries = {IbanRegistry.DE})
    void singleCountry_argumentsMatchRegistryValues(String countryCode, String countryName) {
        assertThat(countryCode).isEqualTo(IbanRegistry.DE.getCountryCode());
        assertThat(countryName).isEqualTo(IbanRegistry.DE.getCountryName());
    }

    // =========================================================================
    // Missing annotation guard (provider used without @IbanCountrySource)
    // =========================================================================

    /**
     * Directly invokes the provider without an annotation present on the element
     * to verify the {@link IllegalStateException} guard.
     */
    @Test
    void provider_missingAnnotation_throwsIllegalStateException() throws Exception {
        IbanCountrySource.IbanCountryArgumentsProvider provider =
            new IbanCountrySource.IbanCountryArgumentsProvider();

        // Create a minimal ExtensionContext whose element has no @IbanCountrySource
        ExtensionContext ctx = mock();
        when(ctx.getElement()).thenReturn(Optional.of(
                IbanCountrySourceTest.class.getDeclaredMethod("provider_missingAnnotation_throwsIllegalStateException")));

        assertThatIllegalStateException()
            .isThrownBy(() -> provider.provideArguments(null, ctx))
            .withMessageContaining("@IbanCountrySource annotation not found");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Counts the number of argument tuples that a given {@code @IbanCountrySource}-annotated
     * annotation type would produce, by synthesising the annotation and invoking the provider.
     */
    private static long countArguments(Class<?> annotationType) {
        IbanCountrySource src = annotationType.getAnnotation(IbanCountrySource.class);
        assertThat(src).as("annotationType must carry @IbanCountrySource").isNotNull();

        IbanCountrySource.IbanCountryArgumentsProvider provider =
            new IbanCountrySource.IbanCountryArgumentsProvider();

        ExtensionContext ctx = mock();
        when(ctx.getElement()).thenReturn(Optional.of(annotationType));
        // AnnotationSupport.findAnnotation looks on the element; meta-annotation on the
        // marker interface is found automatically via JUnit's support utilities.

        return provider.provideArguments(null, ctx).count();
    }

}
