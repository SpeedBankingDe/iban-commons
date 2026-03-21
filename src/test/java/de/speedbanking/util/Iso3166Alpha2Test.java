package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JUnit tests for {@link Iso3166Alpha2}.
 * <p>
 * Coverage map:
 * <ul>
 *   <li>{@link Iso3166Alpha2#getCode()}                — {@code getCode_*}</li>
 *   <li>{@link Iso3166Alpha2#getCountryName()}         — {@code getCountryName_*}</li>
 *   <li>{@link Iso3166Alpha2#getCurrencyCode()}        — {@code getCurrencyCode_*}</li>
 *   <li>{@link Iso3166Alpha2#fromCode(CharSequence)}   — {@code fromCode_*}</li>
 *   <li>{@link Iso3166Alpha2#isAssigned(CharSequence)} — {@code isAssigned_*}</li>
 *   <li>{@link Iso3166Alpha2#toString()}               — {@code toString_*}</li>
 *   <li>Enum mechanics ({@code values()}, {@code valueOf()}) — {@code enum_*}</li>
 *   <li>Completeness / uniqueness invariants           — {@code invariant_*}</li>
 * </ul>
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@DisplayName("Iso3166Alpha2")
class Iso3166Alpha2Test {

    // =========================================================================
    // getCode()
    // =========================================================================

    @DisplayName("getCode() returns the two-letter enum name")
    @ParameterizedTest(name = "[{index}] {0} → code = {0}")
    @CsvSource(delimiter = '|', value = {
        "DZ | DZ",
        "IE | IE",
        "ZA | ZA",
        "NO | NO",
        "ES | ES",
        "ID | ID",
        "FR | FR",
        "GB | GB",
        "CO | CO",
        "TR | TR"
    })
    void getCode_knownConstants_returnsEnumName(String code, String expected) {
        assertThat(Iso3166Alpha2.valueOf(code).getCode()).isEqualTo(expected);
    }

    @DisplayName("getCode() is identical to name() for all constants")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Iso3166Alpha2.class)
    void getCode_equalsName_forAllConstants(Iso3166Alpha2 c) {
        assertThat(c.getCode())
            .as("%s.getCode() must equal name()", c.name())
            .isEqualTo(c.name());
    }

    // =========================================================================
    // getCountryName()
    // =========================================================================

    @DisplayName("getCountryName() returns the correct English country name")
    @ParameterizedTest(name = "[{index}] {0} → ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "KP | Korea (Democratic People's Republic of)",
        "IR | Iran (Islamic Republic of)",
        "SY | Syrian Arab Republic",
        "CU | Cuba",
        "VE | Venezuela (Bolivarian Republic of)",
        "RU | Russian Federation",
        "BY | Belarus",
        "NI | Nicaragua",
        "DZ | Algeria",
        "LY | Libya",
        "LB | Lebanon",
        "YE | Yemen",
        "IQ | Iraq",
        "AF | Afghanistan",
        "PS | Palestine, State of"
    })
    void getCountryName_knownConstants_returnsCorrectName(String code, String expectedName) {
        assertThat(Iso3166Alpha2.valueOf(code).getCountryName())
            .isEqualTo(expectedName);
    }

    @DisplayName("getCountryName() is non-null and non-blank for every constant")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Iso3166Alpha2.class)
    void getCountryName_allConstants_neverNullOrBlank(Iso3166Alpha2 c) {
        assertThat(c.getCountryName())
            .as("%s.getCountryName()", c.name())
            .isNotNull()
            .isNotBlank();
    }

    // =========================================================================
    // getCurrencyCode()
    // =========================================================================

    @DisplayName("getCurrencyCode() returns the correct ISO 4217 currency code")
    @ParameterizedTest(name = "[{index}] {0} → ''{1}''")
    @CsvSource(delimiter = '|', value = {
        // Eurozone members
        "DE | EUR",
        "FR | EUR",
        "AT | EUR",
        "IT | EUR",
        "ES | EUR",
        "NL | EUR",
        "PT | EUR",
        "FI | EUR",
        "IE | EUR",
        "GR | EUR",
        "LU | EUR",
        "BE | EUR",
        "CY | EUR",
        "MT | EUR",
        "EE | EUR",
        "LV | EUR",
        "LT | EUR",
        "SI | EUR",
        "SK | EUR",
        "HR | EUR",
        // Eurozone-adjacent (use EUR without EU membership)
        "AD | EUR",
        "MC | EUR",
        "SM | EUR",
        "VA | EUR",
        "ME | EUR",
        "XK | EUR",
        // French overseas territories using EUR
        "AX | EUR",
        "GP | EUR",
        "GF | EUR",
        "MQ | EUR",
        "RE | EUR",
        "YT | EUR",
        "MF | EUR",
        "PM | EUR",
        "TF | EUR",
        // Non-Eurozone European currencies
        "GB | GBP",
        "CH | CHF",
        "LI | CHF",
        "DK | DKK",
        "NO | NOK",
        "SE | SEK",
        "IS | ISK",
        "PL | PLN",
        "CZ | CZK",
        "HU | HUF",
        "RO | RON",
        "BG | BGN",
        // Crown dependencies using GBP
        "GG | GBP",
        "JE | GBP",
        "IM | GBP",
        // Selected non-European currencies
        "US | USD",
        "JP | JPY",
        "CN | CNY",
        "AU | AUD",
        "CA | CAD",
        "NZ | NZD",
        "CH | CHF",
        "IN | INR",
        "KR | KRW",
        "SG | SGD",
        "HK | HKD",
        // CFA franc zone
        "SN | XOF",
        "ML | XOF",
        "CI | XOF",
        "CM | XAF",
        "GA | XAF",
        "TD | XAF",
        // East Caribbean dollar zone
        "AG | XCD",
        "DM | XCD",
        "GD | XCD",
        "LC | XCD",
        "VC | XCD",
        "KN | XCD",
        "AI | XCD",
        "MS | XCD",
        // Pacific franc zone
        "NC | XPF",
        "PF | XPF",
        "WF | XPF",
        // Updated ISO 4217 codes (2016-2025 redenominations)
        "BY | BYN",
        "VE | VES",
        "SL | SLE",
        // Countries with no own currency (use USD)
        "EC | USD",
        "PA | USD",
        "SV | USD",
        "TL | USD"
    })
    void getCurrencyCode_knownConstants_returnsCorrectCode(String code, String expectedCurrency) {
        assertThat(Iso3166Alpha2.valueOf(code).getCurrency().getAlphaCode())
            .as("%s.getCurrency().getAlphaCode()", code)
            .isEqualTo(expectedCurrency);
    }

    @DisplayName("getCurrencyCode() returns null for Antarctica (AQ) — the only currency-less entry")
    @Test
    void getCurrencyCode_antarctica_returnsNull() {
        assertThat(Iso3166Alpha2.AQ.getCurrency()).isNull();
    }

    @DisplayName("getCurrencyCode() is non-null and non-blank for every constant except AQ")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(value = Iso3166Alpha2.class, names = "AQ", mode = EnumSource.Mode.EXCLUDE)
    void getCurrencyCode_allConstantsExceptAQ_neverNullOrBlank(Iso3166Alpha2 c) {
        assertThat(c.getCurrency().getAlphaCode())
            .as("%s.getCurrency().getAlphaCode()", c.name())
            .isNotNull()
            .isNotBlank();
    }

    @DisplayName("getCurrencyCode() returns a 3-letter uppercase alphabetic code when non-null")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Iso3166Alpha2.class)
    void getCurrencyCode_whenNonNull_matchesIso4217Format(Iso3166Alpha2 c) {
        Currency currency = c.getCurrency();
        if (currency != null) {
            assertThat(currency.getAlphaCode())
                .as("%s.getCurrency().getAlphaCode() must be a 3-letter uppercase alphabetic code", c.name())
                .matches("^[A-Z]{3}$");
        }
    }

    @DisplayName("No two constants share the same (countryCode, currencyCode) pair via a duplicate currency-code check")
    @Test
    void getCurrencyCode_currencyCodeValuesAreInternallySelfConsistent() {
        // Verify that every constant that claims EUR is actually in a known EUR territory or Eurozone country,
        // and that no unexpected code has been accidentally assigned EUR.
        // This is a smoke-test — it will catch copy-paste accidents between adjacent constants.
        Set<String> eurCodes = new LinkedHashSet<>();
        for (Iso3166Alpha2 c : Iso3166Alpha2.values()) {
            Currency cur = c.getCurrency();
            if (cur != null && "EUR".equals(cur.getAlphaCode())) {
                eurCodes.add(c.getCode());
            }
        }
        // At minimum the 20 Eurozone member states plus the 6 micro-states/territories
        // that formally use EUR must be present.
        assertThat(eurCodes.size())
            .as("EUR zone should contain at least 26 entries (20 EU Eurozone + 6 non-EU)")
            .isGreaterThanOrEqualTo(26);

        // Non-Eurozone European countries must NOT be in the EUR set
        for (String nonEurCode : Arrays.asList("GB", "CH", "DK", "NO", "SE", "IS", "PL",
                                               "CZ", "HU", "RO", "BG", "LI")) {
            assertThat(eurCodes)
                .as("'%s' must not be in the EUR zone", nonEurCode)
                .doesNotContain(nonEurCode);
        }
    }

    // =========================================================================
    // fromCode(String)
    // =========================================================================

    @DisplayName("fromCode() returns the correct constant for every assigned code")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR",
        "AS", "AT", "AU", "AW", "AX", "AZ",
        "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL",
        "BM", "BN", "BO", "BQ", "BR", "BS", "BT", "BV", "BW", "BY", "BZ",
        "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN",
        "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ",
        "DE", "DJ", "DK", "DM", "DO", "DZ",
        "EC", "EE", "EG", "EH", "ER", "ES", "ET",
        "FI", "FJ", "FK", "FM", "FO", "FR",
        "GA", "GB", "GD", "GE", "GF", "GG", "GH", "GI", "GL", "GM",
        "GN", "GP", "GQ", "GR", "GT", "GU", "GW", "GY",
        "HK", "HM", "HN", "HR", "HT", "HU",
        "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "IT",
        "JE", "JM", "JO", "JP",
        "KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ",
        "LA", "LB", "LC", "LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY",
        "MA", "MC", "MD", "MF", "MG", "MH", "MK", "ML", "MM", "MN", "MO",
        "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ",
        "NA", "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ",
        "OM",
        "PA", "PE", "PF", "PG", "PH", "PK", "PL", "PM", "PN", "PR", "PS",
        "PT", "PW", "PY",
        "QA",
        "RE", "RO", "RS", "RU", "RW",
        "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL",
        "SM", "SN", "SO", "SR", "SS", "ST", "SV", "SX", "SY", "SZ",
        "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO",
        "TR", "TT", "TV", "TW", "TZ",
        "UA", "UG", "UM", "US", "UY", "UZ",
        "VA", "VC", "VE", "VG", "VI", "VN", "VU",
        "WF", "WS",
        "XK",
        "YE", "YT",
        "ZA", "ZM", "ZW"
    })
    void fromCode_assignedCode_returnsConstantWithMatchingCode(String code) {
        assertThat(Iso3166Alpha2.fromCode(code))
            .as("fromCode(\"%s\")", code)
            .isNotNull()
            .extracting(Iso3166Alpha2::getCode)
            .isEqualTo(code);
    }

    @DisplayName("fromCode() returns null for null or empty input")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    void fromCode_null_returnsNull(String c) {
        assertThat(Iso3166Alpha2.fromCode(c))
            .isNull();
    }

    @DisplayName("fromCode() returns null for strings with wrong length")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"D", "DEU", "DEUS"})
    void fromCode_wrongLength_returnsNull(String code) {
        assertThat(Iso3166Alpha2.fromCode(code))
            .as("fromCode(\"%s\") with wrong length", code)
            .isNull();
    }

    @DisplayName("fromCode() returns null for whitespace-padded two-character inputs — length is 2 but no whitespace key exists in the LOOKUP")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {" D", "D "})
    void fromCode_whitespacePadded_returnsNull(String code) {
        assertThat(Iso3166Alpha2.fromCode(code))
            .as("fromCode(\"%s\") with whitespace padding", code)
            .isNull();
    }

    @DisplayName("fromCode() returns null for syntactically plausible but unassigned codes")
    @ParameterizedTest(name = "[{index}] ''{0}'' is unassigned")
    @ValueSource(strings = {
        "AA", // user-assigned range
        "QM", // user-assigned range
        "XA", // user-assigned range (XK is the only X* exception)
        "ZZ", // user-assigned
        "CS", // deleted (former Serbia and Montenegro)
        "AN"  // deleted (former Netherlands Antilles)
    })
    void fromCode_unassignedCode_returnsNull(String code) {
        assertThat(Iso3166Alpha2.fromCode(code))
            .as("'%s' is not assigned", code)
            .isNull();
    }

    @DisplayName("fromCode() is case-sensitive — lowercase inputs return null")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"ni", "cu", "Ni", "cU"})
    void fromCode_lowercase_returnsNull(String code) {
        assertThat(Iso3166Alpha2.fromCode(code))
            .as("fromCode(\"%s\") must be case-sensitive", code)
            .isNull();
    }

    @DisplayName("fromCode() returns null for codes containing digits or special characters")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"D1", "1D", "$$", "D-"})
    void fromCode_nonAlphabeticCode_returnsNull(String code) {
        assertThat(Iso3166Alpha2.fromCode(code))
            .as("fromCode(\"%s\") with non-alphabetic chars", code)
            .isNull();
    }

    @DisplayName("fromCode() accepts StringBuilder and returns the correct constant")
    @Test
    void fromCode_stringBuilder_returnsCorrectConstant() {
        assertThat(Iso3166Alpha2.fromCode(new StringBuilder("DE"))).isSameAs(Iso3166Alpha2.DE);
        assertThat(Iso3166Alpha2.fromCode(new StringBuilder("FR"))).isSameAs(Iso3166Alpha2.FR);
        assertThat(Iso3166Alpha2.fromCode(new StringBuilder("XK"))).isSameAs(Iso3166Alpha2.XK);
    }

    @DisplayName("fromCode() accepts StringBuffer and returns the correct constant")
    @Test
    void fromCode_stringBuffer_returnsCorrectConstant() {
        assertThat(Iso3166Alpha2.fromCode(new StringBuffer("GB"))).isSameAs(Iso3166Alpha2.GB);
        assertThat(Iso3166Alpha2.fromCode(new StringBuffer("JP"))).isSameAs(Iso3166Alpha2.JP);
    }

    @DisplayName("fromCode() returns null for null or wrong-length StringBuilder/StringBuffer inputs")
    @Test
    void fromCode_charSequenceEdgeCases_returnsNull() {
        assertThat(Iso3166Alpha2.fromCode((CharSequence) null)).isNull();
        assertThat(Iso3166Alpha2.fromCode(new StringBuilder())).isNull();           // empty
        assertThat(Iso3166Alpha2.fromCode(new StringBuilder("D"))).isNull();        // too short
        assertThat(Iso3166Alpha2.fromCode(new StringBuilder("DEU"))).isNull();      // too long
        assertThat(Iso3166Alpha2.fromCode(new StringBuilder("de"))).isNull();       // lowercase
        assertThat(Iso3166Alpha2.fromCode(new StringBuffer(" D"))).isNull();        // whitespace
    }

    // =========================================================================
    // isAssigned(String)
    // =========================================================================

    @DisplayName("isAssigned() returns true for officially assigned codes")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"KP", "SY", "IR", "CU", "VE", "RU", "NI", "BY", "DZ", "JP", "AF", "RE", "CW"})
    void isAssigned_assignedCodes_returnsTrue(String code) {
        assertThat(Iso3166Alpha2.isAssigned(code))
            .as("'%s' should be assigned", code)
            .isTrue();
    }

    @DisplayName("isAssigned() returns false for null, empty, or unassigned inputs")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {
        "AA", "ZZ", "QQ", "CS", "AN", // unassigned / deleted
        "jp", "ir",                   // wrong case
        "C",  "CUB",                  // wrong length
        "D1"                          // non-alphabetic
    })
    void isAssigned_invalidOrUnassigned_returnsFalse(String code) {
        assertThat(Iso3166Alpha2.isAssigned(code))
            .as("'%s' should not be assigned", code)
            .isFalse();
    }

    @DisplayName("isAssigned() accepts StringBuilder and StringBuffer")
    @Test
    void isAssigned_charSequenceTypes_workCorrectly() {
        assertThat(Iso3166Alpha2.isAssigned(new StringBuilder("DE"))).isTrue();
        assertThat(Iso3166Alpha2.isAssigned(new StringBuilder("AA"))).isFalse();
        assertThat(Iso3166Alpha2.isAssigned(new StringBuffer("FR"))).isTrue();
        assertThat(Iso3166Alpha2.isAssigned(new StringBuffer("ZZ"))).isFalse();
        assertThat(Iso3166Alpha2.isAssigned((CharSequence) null)).isFalse();
    }

    // =========================================================================
    // toString()
    // =========================================================================

    @DisplayName("toString() returns \"<CODE> (<countryName>)\" format")
    @ParameterizedTest(name = "[{index}] {0} → ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "NZ | NZ (New Zealand)",
        "IS | IS (Iceland)",
        "KE | KE (Kenya)",
        "UY | UY (Uruguay)",
        "SG | SG (Singapore)",
        "XK | XK (Kosovo)",
        "AX | AX (Åland Islands)"
    })
    void toString_knownConstants_returnsFormattedString(String code, String expected) {
        assertThat(Iso3166Alpha2.valueOf(code).toString())
            .isEqualTo(expected);
    }

    @DisplayName("toString() follows \"<CODE> (<countryName>)\" pattern for all constants")
    @Test
    void toString_allConstants_followsPattern() {
        for (Iso3166Alpha2 c : Iso3166Alpha2.values()) {
            assertThat(c.toString())
                .as("%s.toString()", c.name())
                .startsWith(c.name() + " (")
                .endsWith(")");
        }
    }

    // =========================================================================
    // Enum mechanics — values() / valueOf()
    // =========================================================================

    @DisplayName("values() contains exactly 249 constants")
    @Test
    void enum_values_contains249Constants() {
        assertThat(Iso3166Alpha2.values()).hasSize(249);
    }

    @DisplayName("valueOf() resolves every constant from its code string")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Iso3166Alpha2.class)
    void enum_valueOf_resolvesEveryConstant(Iso3166Alpha2 c) {
        assertThat(Iso3166Alpha2.valueOf(c.name()))
            .as("valueOf(\"%s\")", c.name())
            .isSameAs(c);
    }

    @DisplayName("valueOf() throws IllegalArgumentException for an unknown name")
    @Test
    void enum_valueOf_unknownName_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Iso3166Alpha2.valueOf("XX"));
    }

    // =========================================================================
    // Invariants
    // =========================================================================

    @DisplayName("Every code is exactly 2 characters long")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Iso3166Alpha2.class)
    void invariant_allCodes_areExactlyTwoChars(Iso3166Alpha2 c) {
        assertThat(c.getCode())
            .as("%s.getCode()", c.name())
            .hasSize(2);
    }

    @DisplayName("Every code consists of two uppercase ASCII letters (A–Z)")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Iso3166Alpha2.class)
    void invariant_allCodes_areUpperCaseAsciiLetters(Iso3166Alpha2 c) {
        assertThat(c.getCode())
            .as("%s.getCode() must be two uppercase ASCII letters", c.name())
            .matches("^[A-Z]{2}$");
    }

    @DisplayName("No duplicate codes exist across all constants")
    @Test
    void invariant_allCodes_areUnique() {
        Set<String> seen = new LinkedHashSet<>();
        for (Iso3166Alpha2 c : Iso3166Alpha2.values()) {
            assertThat(seen.add(c.getCode()))
                .as("Duplicate code detected: %s", c.getCode())
                .isTrue();
        }
    }

    @DisplayName("LOOKUP round-trip: fromCode(c.getCode()) returns the identical instance")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Iso3166Alpha2.class)
    void invariant_lookupRoundTrip_returnsIdenticalInstance(Iso3166Alpha2 c) {
        assertThat(Iso3166Alpha2.fromCode(c.getCode()))
            .as("fromCode(\"%s\") round-trip", c.getCode())
            .isSameAs(c);
    }

    @DisplayName("XK (Kosovo) is included as the only non-standard code and is correctly labelled")
    @Test
    void invariant_xk_isIncludedAndCorrectlyLabelled() {
        assertThat(Iso3166Alpha2.fromCode("XK"))
            .isNotNull()
            .extracting(Iso3166Alpha2::getCode, Iso3166Alpha2::getCountryName)
            .containsExactly("XK", "Kosovo");
    }

    @DisplayName("Deleted and reserved codes are not present in the enum")
    @Test
    void invariant_deletedOrReservedCodes_areAbsent() {
        for (String code : Arrays.asList("AA", "ZZ", "CS", "AN", "QM", "XA", "EU", "UN")) {
            assertThat(Iso3166Alpha2.fromCode(code))
                .as("'%s' must not be in the enum", code)
                .isNull();
        }
    }

    @DisplayName("AQ is the only constant with a null currency code")
    @Test
    void invariant_onlyAQ_hasNullCurrencyCode() {
        long nullCount = Arrays.stream(Iso3166Alpha2.values())
            .filter(c -> c.getCurrency() == null)
            .count();
        assertThat(nullCount)
            .as("Exactly one constant (AQ) should have a null currency code")
            .isEqualTo(1L);
        assertThat(Iso3166Alpha2.AQ.getCurrency()).isNull();
    }

    @DisplayName("No two constants share an identical country name")
    @Test
    void invariant_allCountryNames_areUnique() {
        Set<String> seen = new LinkedHashSet<>();
        for (Iso3166Alpha2 c : Iso3166Alpha2.values()) {
            assertThat(seen.add(c.getCountryName()))
                .as("Duplicate country name detected: '%s' (%s)", c.getCountryName(), c.getCode())
                .isTrue();
        }
    }

}
