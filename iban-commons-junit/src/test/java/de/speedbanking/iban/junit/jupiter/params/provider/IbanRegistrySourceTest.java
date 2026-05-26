package de.speedbanking.iban.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

import de.speedbanking.iban.IbanRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * Unit tests for {@link IbanRegistrySource}.
 * <p>
 * The provider is tested both via direct {@link ParameterizedTest} usage (exercising the
 * full JUnit integration path) and by invoking the inner
 * {@link IbanRegistrySource.IbanRegistryArgumentsProvider} directly (exercising edge-cases
 * and the {@link IbanRegistrySource.IbanRegistryArgumentsProvider#describe} helper).
 */
@SuppressWarnings("checkstyle:MethodName")
final class IbanRegistrySourceTest {

    private static final int TOTAL = IbanRegistry.values().length;

    // =========================================================================
    // Default (no filter) — all entries
    // =========================================================================

    @ParameterizedTest
    @IbanRegistrySource
    void noFilter_yieldsEveryRegistryEntry(IbanRegistry entry) {
        assertThat(entry).isNotNull();
    }

    @Test
    void noFilter_countMatchesTotalRegistrySize() {
        assertThat(countArguments(IbanRegistrySourceNoFilter.class)).isEqualTo(TOTAL);
    }

    @IbanRegistrySource
    private @interface IbanRegistrySourceNoFilter {}

    // =========================================================================
    // value() — explicit include list
    // =========================================================================

    @ParameterizedTest
    @IbanRegistrySource({IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH})
    void value_yieldsOnlySpecifiedEntries(IbanRegistry entry) {
        assertThat(entry).isIn(IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH);
    }

    @Test
    void value_countMatchesSpecifiedSize() {
        assertThat(countArguments(IbanRegistrySourceDeAtCh.class)).isEqualTo(3);
    }

    @IbanRegistrySource({IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH})
    private @interface IbanRegistrySourceDeAtCh {}

    // =========================================================================
    // exclude()
    // =========================================================================

    @Test
    void exclude_countIsReducedByExcludedSize() {
        assertThat(countArguments(IbanRegistrySourceExcludeDeAt.class)).isEqualTo(TOTAL - 2);
    }

    @IbanRegistrySource(exclude = {IbanRegistry.DE, IbanRegistry.AT})
    private @interface IbanRegistrySourceExcludeDeAt {}

    @ParameterizedTest
    @IbanRegistrySource(exclude = {IbanRegistry.DE, IbanRegistry.AT})
    void exclude_excludedEntriesAreAbsent(IbanRegistry entry) {
        assertThat(entry).isNotIn(IbanRegistry.DE, IbanRegistry.AT);
    }

    // =========================================================================
    // countryType filter
    // =========================================================================

    @ParameterizedTest
    @IbanRegistrySource(countryType = IbanRegistrySource.CountryType.BASE)
    void countryType_BASE_yieldsOnlyBaseCountries(IbanRegistry entry) {
        assertThat(entry.isBaseCountry()).isTrue();
    }

    @ParameterizedTest
    @IbanRegistrySource(countryType = IbanRegistrySource.CountryType.DERIVED)
    void countryType_DERIVED_yieldsOnlyDerivedCountries(IbanRegistry entry) {
        assertThat(entry.isDerivedCountry()).isTrue();
    }

    @Test
    void countryType_BASE_and_DERIVED_partitionAllEntries() {
        long base    = countArguments(IbanRegistrySourceBase.class);
        long derived = countArguments(IbanRegistrySourceDerived.class);
        assertThat(base + derived).isEqualTo(TOTAL);
    }

    @IbanRegistrySource(countryType = IbanRegistrySource.CountryType.BASE)
    private @interface IbanRegistrySourceBase {}

    @IbanRegistrySource(countryType = IbanRegistrySource.CountryType.DERIVED)
    private @interface IbanRegistrySourceDerived {}

    // =========================================================================
    // sepa filter
    // =========================================================================

    @ParameterizedTest
    @IbanRegistrySource(sepa = IbanRegistrySource.Sepa.YES)
    void sepa_YES_yieldsOnlySepaEntries(IbanRegistry entry) {
        assertThat(entry.isSepa()).isTrue();
    }

    @ParameterizedTest
    @IbanRegistrySource(sepa = IbanRegistrySource.Sepa.NO)
    void sepa_NO_yieldsOnlyNonSepaEntries(IbanRegistry entry) {
        assertThat(entry.isSepa()).isFalse();
    }

    @Test
    void sepa_YES_and_NO_partitionAllEntries() {
        long sepa    = countArguments(IbanRegistrySourceSepaYes.class);
        long nonSepa = countArguments(IbanRegistrySourceSepaNo.class);
        assertThat(sepa + nonSepa).isEqualTo(TOTAL);
    }

    @IbanRegistrySource(sepa = IbanRegistrySource.Sepa.YES)
    private @interface IbanRegistrySourceSepaYes {}

    @IbanRegistrySource(sepa = IbanRegistrySource.Sepa.NO)
    private @interface IbanRegistrySourceSepaNo {}

    // =========================================================================
    // minIbanLength / maxIbanLength filter
    // =========================================================================

    @ParameterizedTest
    @IbanRegistrySource(minIbanLength = 22)
    void minIbanLength_yieldsOnlyEntriesAtOrAboveMinimum(IbanRegistry entry) {
        assertThat(entry.getIbanLength()).isGreaterThanOrEqualTo(22);
    }

    @ParameterizedTest
    @IbanRegistrySource(maxIbanLength = 20)
    void maxIbanLength_yieldsOnlyEntriesAtOrBelowMaximum(IbanRegistry entry) {
        assertThat(entry.getIbanLength()).isLessThanOrEqualTo(20);
    }

    @ParameterizedTest
    @IbanRegistrySource(minIbanLength = 16, maxIbanLength = 20)
    void ibanLengthRange_yieldsOnlyEntriesWithinRange(IbanRegistry entry) {
        assertThat(entry.getIbanLength()).isBetween(16, 20);
    }

    @Test
    void ibanLengthRange_countIsPositive() {
        assertThat(countArguments(IbanRegistrySourceLength16to20.class)).isPositive();
    }

    @IbanRegistrySource(minIbanLength = 16, maxIbanLength = 20)
    private @interface IbanRegistrySourceLength16to20 {}

    // =========================================================================
    // currency filter
    // =========================================================================

    @ParameterizedTest
    @IbanRegistrySource(currency = "EUR")
    void currency_EUR_yieldsOnlyEuroEntries(IbanRegistry entry) {
        // verify that each returned entry's country code resolves to EUR
        de.speedbanking.util.Country iso = de.speedbanking.util.Country.fromCode(entry.name());
        assertThat(iso).isNotNull();
        assertThat(iso.getCurrency().getAlphaCode()).isEqualTo("EUR");
    }

    @Test
    void currency_EUR_countIsPositive() {
        assertThat(countArguments(IbanRegistrySourceEur.class)).isPositive();
    }

    @IbanRegistrySource(currency = "EUR")
    private @interface IbanRegistrySourceEur {}

    @Test
    void currency_multipleValues_yieldsEntriesMatchingAny() {
        long eurGbp  = countArguments(IbanRegistrySourceEurGbp.class);
        long eur     = countArguments(IbanRegistrySourceEur.class);
        long gbp     = countArguments(IbanRegistrySourceGbp.class);
        // EUR ∪ GBP (no overlap between EUR and GBP countries)
        assertThat(eurGbp).isEqualTo(eur + gbp);
    }

    @IbanRegistrySource(currency = {"EUR", "GBP"})
    private @interface IbanRegistrySourceEurGbp {}

    @IbanRegistrySource(currency = "GBP")
    private @interface IbanRegistrySourceGbp {}

    // =========================================================================
    // filterMode — AND vs. OR semantics
    // =========================================================================

    /**
     * AND (ALL): sepa=YES AND currency=EUR → intersection (Eurozone members only).
     * OR  (ANY): sepa=YES OR  currency=EUR → union (SEPA members ∪ EUR countries).
     * The union must be at least as large as either set.
     */
    @Test
    void filterMode_ANY_producesUnionLargerThanIntersection() {
        long andCount = countArguments(IbanRegistrySourceSepaYesEurAnd.class);
        long orCount  = countArguments(IbanRegistrySourceSepaYesEurOr.class);
        assertThat(orCount).isGreaterThanOrEqualTo(andCount);
    }

    @IbanRegistrySource(sepa = IbanRegistrySource.Sepa.YES, currency = "EUR",
                        filterMode = IbanRegistrySource.FilterMode.ALL)
    private @interface IbanRegistrySourceSepaYesEurAnd {}

    @IbanRegistrySource(sepa = IbanRegistrySource.Sepa.YES, currency = "EUR",
                        filterMode = IbanRegistrySource.FilterMode.ANY)
    private @interface IbanRegistrySourceSepaYesEurOr {}

    @ParameterizedTest
    @IbanRegistrySource(sepa = IbanRegistrySource.Sepa.YES, currency = "EUR",
                        filterMode = IbanRegistrySource.FilterMode.ALL)
    void filterMode_ALL_sepaAndEur_yieldsOnlyEurozone(IbanRegistry entry) {
        assertThat(entry.isSepa()).isTrue();
        de.speedbanking.util.Country iso = de.speedbanking.util.Country.fromCode(entry.name());
        assertThat(iso).isNotNull();
        assertThat(iso.getCurrency().getAlphaCode()).isEqualTo("EUR");
    }

    // =========================================================================
    // value() + attribute filter combined
    // =========================================================================

    /**
     * {@code value} restricts the candidate set; the attribute filter is then applied to
     * that restricted set. DE is SEPA, AE is not — only DE should survive.
     */
    @Test
    void value_combinedWithSepaFilter_yieldsIntersection() {
        assertThat(countArguments(IbanRegistrySourceDeAeSepaYes.class)).isEqualTo(1);
    }

    @IbanRegistrySource(value = {IbanRegistry.DE, IbanRegistry.AE},
                        sepa = IbanRegistrySource.Sepa.YES)
    private @interface IbanRegistrySourceDeAeSepaYes {}

    // =========================================================================
    // value() + exclude() combined
    // =========================================================================

    @Test
    void value_andExclude_combined_yieldsCorrectSubset() {
        // include DE, AT, CH; exclude CH → 2
        assertThat(countArguments(IbanRegistrySourceDeAtChExcludeCh.class)).isEqualTo(2);
    }

    @IbanRegistrySource(value = {IbanRegistry.DE, IbanRegistry.AT, IbanRegistry.CH},
                        exclude = {IbanRegistry.CH})
    private @interface IbanRegistrySourceDeAtChExcludeCh {}

    // =========================================================================
    // Empty result → IllegalStateException
    // =========================================================================

    /**
     * A filter combination that cannot match any entry must throw
     * {@link IllegalStateException} with a useful message.
     */
    @Test
    void emptyResult_throwsIllegalStateException() {
        // DE is a SEPA country — sepa=NO with value={DE} can never match
        assertThatIllegalStateException()
            .isThrownBy(() -> countArguments(IbanRegistrySourceDeSepaNo.class))
            .withMessageContaining("No IbanRegistry entries matched");
    }

    @IbanRegistrySource(value = {IbanRegistry.DE}, sepa = IbanRegistrySource.Sepa.NO)
    private @interface IbanRegistrySourceDeSepaNo {}

    // =========================================================================
    // describe() helper
    // =========================================================================

    @Test
    void describe_noFilter_containsWildcardsAndDefaults() {
        IbanRegistrySource src = IbanRegistrySourceNoFilter.class.getAnnotation(IbanRegistrySource.class);
        String desc = IbanRegistrySource.IbanRegistryArgumentsProvider.describe(src);

        assertThat(desc)
            .contains("IbanRegistrySource")
            .contains("value=*")
            .contains("exclude=-")
            .contains("countryType=ANY")
            .contains("sepa=ANY")
            .contains("currency=*")
            .contains("filterMode=ALL");
    }

    @Test
    void describe_withFilters_containsFilterValues() {
        IbanRegistrySource src = IbanRegistrySourceDeAtCh.class.getAnnotation(IbanRegistrySource.class);
        String desc = IbanRegistrySource.IbanRegistryArgumentsProvider.describe(src);

        assertThat(desc).contains("DE").contains("AT").contains("CH");
    }

    @Test
    void describe_withExclude_containsExcludedCodes() {
        IbanRegistrySource src = IbanRegistrySourceExcludeDeAt.class.getAnnotation(IbanRegistrySource.class);
        String desc = IbanRegistrySource.IbanRegistryArgumentsProvider.describe(src);

        assertThat(desc).contains("exclude=").contains("DE").contains("AT");
    }

    @Test
    void describe_withLengthAndCurrency_containsValues() {
        IbanRegistrySource src = IbanRegistrySourceLength16to20.class.getAnnotation(IbanRegistrySource.class);
        String desc = IbanRegistrySource.IbanRegistryArgumentsProvider.describe(src);

        assertThat(desc).contains("minIbanLength=16").contains("maxIbanLength=20");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Counts arguments produced by driving the provider with the {@link IbanRegistrySource}
     * annotation found on {@code annotationType}.
     */
    private static long countArguments(Class<?> annotationType) {
        IbanRegistrySource src = annotationType.getAnnotation(IbanRegistrySource.class);
        assertThat(src).as("annotationType must carry @IbanRegistrySource").isNotNull();

        IbanRegistrySource.IbanRegistryArgumentsProvider provider = new IbanRegistrySource.IbanRegistryArgumentsProvider();
        provider.accept(src);

        ExtensionContext ctx = mock();

        return provider.provideArguments(null, ctx).count();
    }
}
