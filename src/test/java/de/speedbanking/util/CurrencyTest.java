package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JUnit tests for {@link Currency}.
 * <p>
 * Coverage map:
 * <ul>
 *   <li>{@link Currency#getAlphaCode()}    — {@code getAlphaCode_*}</li>
 *   <li>{@link Currency#getCurrencyName()} — {@code getCurrencyName_*}</li>
 *   <li>{@link Currency#getNumericCode()}  — {@code getNumericCode_*}</li>
 *   <li>{@link Currency#toString()}        — {@code toString_*}</li>
 *   <li>Enum mechanics                     — {@code enum_*}</li>
 *   <li>Invariants                         — {@code invariant_*}</li>
 * </ul>
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@DisplayName("Currency")
class CurrencyTest {

    // =========================================================================
    // getAlphaCode()
    // =========================================================================

    @DisplayName("getAlphaCode() equals name() for all constants")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Currency.class)
    void getAlphaCode_equalsName_forAllConstants(Currency c) {
        assertThat(c.getAlphaCode())
            .as("%s.getAlphaCode() must equal name()", c.name())
            .isEqualTo(c.name());
    }

    @DisplayName("getAlphaCode() consists of exactly 3 uppercase ASCII letters")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Currency.class)
    void getAlphaCode_matchesIso4217AlphaFormat(Currency c) {
        assertThat(c.getAlphaCode())
            .as("%s.getAlphaCode() must be 3 uppercase ASCII letters", c.name())
            .matches("^[A-Z]{3}$");
    }

    // =========================================================================
    // getCurrencyName()
    // =========================================================================

    @DisplayName("getCurrencyName() is non-null and non-blank for all constants")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Currency.class)
    void getCurrencyName_neverNullOrBlank(Currency c) {
        assertThat(c.getCurrencyName())
            .as("%s.getCurrencyName()", c.name())
            .isNotNull()
            .isNotBlank();
    }

    @DisplayName("getCurrencyName() returns the correct English name")
    @ParameterizedTest(name = "[{index}] {0} → ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "EUR | Euro",
        "USD | US Dollar",
        "GBP | British Pound Sterling",
        "CHF | Swiss Franc",
        "JPY | Japanese Yen",
        "SEK | Swedish Krona",
        "NOK | Norwegian Krone",
        "DKK | Danish Krone",
        "PLN | Polish Złoty",
        "CZK | Czech Koruna",
        "HUF | Hungarian Forint",
        "BGN | Bulgarian Lev",
        "RON | Romanian Leu",
        "ISK | Icelandic Króna",
        "XAF | CFA Franc BEAC",
        "XOF | CFA Franc BCEAO",
        "XCD | East Caribbean Dollar",
        "XPF | CFP Franc",
        "BYN | Belarusian Ruble",
        "VES | Venezuelan Bolívar Soberano",
        "SLE | Sierra Leonean Leone",
        "MRU | Mauritanian Ouguiya",
        "STN | São Tomé and Príncipe Dobra"
    })
    void getCurrencyName_knownConstants_returnsCorrectName(String code, String expectedName) {
        assertThat(Currency.valueOf(code).getCurrencyName())
            .isEqualTo(expectedName);
    }

    // =========================================================================
    // getNumericCode()
    // =========================================================================

    @DisplayName("getNumericCode() returns the correct ISO 4217 numeric code")
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', value = {
        // Major currencies
        "EUR | 978",
        "USD | 840",
        "GBP | 826",
        "CHF | 756",
        "JPY | 392",
        "AUD | 36",
        "CAD | 124",
        "NZD | 554",
        "SGD | 702",
        "HKD | 344",
        // European non-Euro
        "SEK | 752",
        "NOK | 578",
        "DKK | 208",
        "PLN | 985",
        "CZK | 203",
        "HUF | 348",
        "BGN | 975",
        "RON | 946",
        "ISK | 352",
        "RSD | 941",
        // Small numeric codes (formerly leading zeros — now plain int)
        "ALL |   8",
        "AMD |  51",
        "ARS |  32",
        "BHD |  48",
        "BND |  96",
        "BOB |  68",
        "BSD |  44",
        "BTN |  64",
        "BWP |  72",
        "BZD |  84",
        "DZD |  12",
        "SBD |  90",
        // Supranational
        "XAF | 950",
        "XOF | 952",
        "XCD | 951",
        "XPF | 953",
        // Updated codes
        "BYN | 933",
        "VES | 928",
        "SLE | 925",
        "MRU | 929",
        "STN | 930"
    })
    void getNumericCode_knownConstants_returnsCorrectCode(String alphaCode, int expectedNumeric) {
        assertThat(Currency.valueOf(alphaCode).getNumericCode())
            .as("%s.getNumericCode()", alphaCode)
            .isEqualTo(expectedNumeric);
    }

    @DisplayName("getNumericCode() is positive for all constants")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Currency.class)
    void getNumericCode_isPositive(Currency c) {
        assertThat(c.getNumericCode())
            .as("%s.getNumericCode() must be positive", c.name())
            .isPositive();
    }

    @DisplayName("getNumericCode() is at most 999 (ISO 4217 three-digit constraint)")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Currency.class)
    void getNumericCode_isAtMost999(Currency c) {
        assertThat(c.getNumericCode())
            .as("%s.getNumericCode() must be ≤ 999", c.name())
            .isLessThanOrEqualTo(999);
    }

    // =========================================================================
    // toString()
    // =========================================================================

    @DisplayName("toString() follows \"<CODE> (<currencyName>)\" pattern for all constants")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Currency.class)
    void toString_allConstants_followsPattern(Currency c) {
        assertThat(c.toString())
            .as("%s.toString()", c.name())
            .startsWith(c.name() + " (")
            .endsWith(")");
    }

    @DisplayName("toString() returns correct formatted string for selected constants")
    @ParameterizedTest(name = "[{index}] {0} → ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "EUR | EUR (Euro)",
        "GBP | GBP (British Pound Sterling)",
        "USD | USD (US Dollar)",
        "CHF | CHF (Swiss Franc)",
        "XAF | XAF (CFA Franc BEAC)"
    })
    void toString_knownConstants_returnsFormattedString(String code, String expected) {
        assertThat(Currency.valueOf(code)).hasToString(expected);
    }

    // =========================================================================
    // Enum mechanics
    // =========================================================================

    @DisplayName("values() contains exactly 153 constants")
    @Test
    void enum_values_contains153Constants() {
        assertThat(Currency.values()).hasSize(153);
    }

    @DisplayName("valueOf() resolves every constant from its alpha code string")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Currency.class)
    void enum_valueOf_resolvesEveryConstant(Currency c) {
        assertThat(Currency.valueOf(c.name()))
            .as("valueOf(\"%s\")", c.name())
            .isSameAs(c);
    }

    @DisplayName("valueOf() throws IllegalArgumentException for an unknown code")
    @Test
    void enum_valueOf_unknownCode_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Currency.valueOf("XXX"));
    }

    // =========================================================================
    // Invariants
    // =========================================================================

    @DisplayName("No duplicate numeric codes exist across all constants")
    @Test
    void invariant_allNumericCodes_areUnique() {
        Set<Integer> seen = new LinkedHashSet<>();
        for (Currency c : Currency.values()) {
            assertThat(seen.add(c.getNumericCode()))
                .as("Duplicate numeric code %d detected on %s", c.getNumericCode(), c.name())
                .isTrue();
        }
    }

    @DisplayName("No duplicate alpha codes exist across all constants")
    @Test
    void invariant_allAlphaCodes_areUnique() {
        Set<String> seen = new LinkedHashSet<>();
        for (Currency c : Currency.values()) {
            assertThat(seen.add(c.getAlphaCode()))
                .as("Duplicate alpha code '%s' detected", c.getAlphaCode())
                .isTrue();
        }
    }

    @DisplayName("No duplicate currency names exist across all constants")
    @Test
    void invariant_allCurrencyNames_areUnique() {
        Set<String> seen = new LinkedHashSet<>();
        for (Currency c : Currency.values()) {
            assertThat(seen.add(c.getCurrencyName()))
                .as("Duplicate currency name '%s' detected on %s", c.getCurrencyName(), c.name())
                .isTrue();
        }
    }

    @DisplayName("Every constant in Iso3166Alpha2 with a non-null currency resolves to a valid Currency constant")
    @Test
    void invariant_iso3166Alpha2_currencyRoundTrip() {
        for (Iso3166Alpha2 country : Iso3166Alpha2.values()) {
            Currency currency = country.getCurrency();
            if (currency != null) {
                // Round-trip: Currency.valueOf(currency.name()) must return the same instance
                assertThat(Currency.valueOf(currency.name()))
                    .as("Currency round-trip failed for country %s (currency %s)",
                        country.name(), currency.name())
                    .isSameAs(currency);
            }
        }
    }

    @DisplayName("Exactly one Iso3166Alpha2 constant (AQ) has a null currency")
    @Test
    void invariant_exactlyOneIso3166Alpha2Entry_hasNullCurrency() {
        long nullCount = Arrays.stream(Iso3166Alpha2.values())
            .filter(c -> c.getCurrency() == null)
            .count();
        assertThat(nullCount)
            .as("Exactly one Iso3166Alpha2 constant should have a null Currency")
            .isEqualTo(1L);
        assertThat(Iso3166Alpha2.AQ.getCurrency()).isNull();
    }

}
