package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.stream.Stream;

/**
 * JUnit tests for {@link Country} and its relation to {@link Continent}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class CountryTest {

    @DisplayName("getCode() returns the two-letter enum name")
    @ParameterizedTest(name = "[{index}] ''{0}'' -> ''{1}''")
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
        assertThat(Country.valueOf(code).getCode()).isEqualTo(expected);
    }

    @DisplayName("getCode() is identical to name() for all constants")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void getCode_equalsName_forAllConstants(Country c) {
        assertThat(c.getCode())
            .as("%s.getCode() must equal name()", c.name())
            .isEqualTo(c.name());
    }

    @DisplayName("getCountryName() returns the correct English country name")
    @ParameterizedTest(name = "[{index}] ''{0}'' -> ''{1}''")
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
        assertThat(Country.valueOf(code).getCountryName())
            .isEqualTo(expectedName);
    }

    @DisplayName("getCountryName() is non-null and non-blank for every constant")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void getCountryName_allConstants_neverNullOrBlank(Country c) {
        assertThat(c.getCountryName())
            .as("%s.getCountryName()", c.name())
            .isNotNull()
            .isNotBlank();
    }

    @DisplayName("getCurrencyCode() returns the correct ISO 4217 currency code")
    @ParameterizedTest(name = "[{index}] ''{0}'' -> ''{1}''")
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
    void getCurrencyCode_knownConstants_returnsCorrectCode(Country code, String expectedCurrency) {
        assertThat(code.getCurrency().getAlphaCode())
            .as("%s.getCurrency().getAlphaCode()", code)
            .isEqualTo(expectedCurrency);
    }

    @DisplayName("getCurrencyCode() returns null for Antarctica (AQ) — the only currency-less entry")
    @Test
    void getCurrencyCode_antarctica_returnsNull() {
        assertThat(Country.AQ.getCurrency()).isNull();
    }

    @DisplayName("getCurrencyCode() is non-null and non-blank for every constant except AQ")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(value = Country.class, names = "AQ", mode = EnumSource.Mode.EXCLUDE)
    void getCurrencyCode_allConstantsExceptAQ_neverNullOrBlank(Country c) {
        assertThat(c.getCurrency().getAlphaCode())
            .as("%s.getCurrency().getAlphaCode()", c.name())
            .isNotNull()
            .isNotBlank();
    }

    @DisplayName("getCurrencyCode() returns a 3-letter uppercase alphabetic code when non-null")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void getCurrencyCode_whenNonNull_matchesIso4217Format(Country c) {
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
        for (Country c : Country.values()) {
            Currency cur = c.getCurrency();
            if (cur != null && "EUR".equals(cur.getAlphaCode())) {
                eurCodes.add(c.getCode());
            }
        }
        // at minimum the 20 Eurozone member states plus the 6 micro-states/territories
        // that formally use EUR must be present
        assertThat(eurCodes)
            .as("EUR zone should contain at least 26 entries (20 EU Eurozone + 6 non-EU)")
            .hasSizeGreaterThanOrEqualTo(26);

        // non-Eurozone European countries must NOT be in the EUR set
        for (String nonEurCode : Arrays.asList("GB", "CH", "DK", "NO", "SE", "IS", "PL",
                                               "CZ", "HU", "RO", "BG", "LI")) {
            assertThat(eurCodes)
                .as("'%s' must not be in the EUR zone", nonEurCode)
                .doesNotContain(nonEurCode);
        }
    }

    @DisplayName("getContinent() returns the correct Continent instance for verified key countries")
    @ParameterizedTest(name = "[{index}] ''{0}'' -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "DE | EUROPE",
        "FR | EUROPE",
        "JP | ASIA",
        "CN | ASIA",
        "US | NORTH_AMERICA",
        "CA | NORTH_AMERICA",
        "BR | SOUTH_AMERICA",
        "AR | SOUTH_AMERICA",
        "ZA | AFRICA",
        "EG | AFRICA",
        "AU | OCEANIA",
        "NZ | OCEANIA",
        "AQ | ANTARCTICA"
    })
    void getContinent_knownConstants_returnsCorrectContinent(Country country, Continent expectedContinent) {
        assertThat(country.getContinent())
            .as("%s.getContinent()", country.name())
            .isEqualTo(expectedContinent);
    }

    @DisplayName("Every assigned Country has a Continent reference set")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void getContinent_allConstants_neverNull(Country c) {
        assertThat(c.getContinent())
            .as("Continent mapping for %s must not be null", c.name())
            .isNotNull();
    }

    @DisplayName("Continent.fromCode() resolves the correct continent instance from its code")
    @ParameterizedTest(name = "[{index}] ''{0}'' -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "EU | EUROPE",
        "AS | ASIA",
        "NA | NORTH_AMERICA",
        "SA | SOUTH_AMERICA",
        "AF | AFRICA",
        "OC | OCEANIA",
        "AN | ANTARCTICA"
    })
    void continentFromCode_validCodes_returnsCorrectConstant(String code, Continent expected) {
        assertThat(Continent.fromCode(code)).isSameAs(expected);
    }

    @DisplayName("Continent.fromCode() returns null for unknown, empty, or null inputs")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"XX", "eu", "EU "})
    void continentFromCode_invalidCodes_returnsNull(String code) {
        assertThat(Continent.fromCode(code)).isNull();
    }

    @DisplayName("Continent structural properties are healthy")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Continent.class)
    void continent_properties_areValidAndConsistent(Continent continent) {
        assertThat(continent.getCode()).matches("^[A-Z]{2}$");
        assertThat(continent.getContinentName()).isNotBlank();
        assertThat(continent).hasToString("%s[%s, code=%s, continentName=%s]",
            continent.getDeclaringClass().getSimpleName(), continent.name(), continent.getCode(), continent.getContinentName());
    }

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
        "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK", "ML", "MM", "MN", "MO",
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
        assertThat(Country.fromCode(code))
            .as("fromCode(\"%s\")", code)
            .isNotNull()
            .extracting(Country::getCode)
            .isEqualTo(code);
    }

    @DisplayName("fromCode() returns null for null or empty input")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    void fromCode_null_returnsNull(String c) {
        assertThat(Country.fromCode(c))
            .isNull();
    }

    @DisplayName("fromCode() returns null for strings with wrong length")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"D", "DEU", "DEUS"})
    void fromCode_wrongLength_returnsNull(String code) {
        assertThat(Country.fromCode(code))
            .as("fromCode(\"%s\") with wrong length", code)
            .isNull();
    }

    @DisplayName("fromCode() returns null for whitespace-padded two-character inputs")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {" D", "D "})
    void fromCode_whitespacePadded_returnsNull(String code) {
        assertThat(Country.fromCode(code))
            .as("fromCode(\"%s\") with whitespace padding", code)
            .isNull();
    }

    @DisplayName("fromCode() returns null for syntactically plausible but unassigned codes")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "AA", // user-assigned range
        "QM", // user-assigned range
        "XA", // user-assigned range (XK is the only X* exception)
        "ZZ", // user-assigned
        "CS", // deleted (former Serbia and Montenegro)
        "AN"  // deleted (former Netherlands Antilles)
    })
    void fromCode_unassignedCode_returnsNull(String code) {
        assertThat(Country.fromCode(code))
            .as("'%s' is not assigned", code)
            .isNull();
    }

    @DisplayName("fromCode() is case-sensitive — lowercase inputs return null")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"ni", "cu", "Ni", "cU"})
    void fromCode_lowercase_returnsNull(String code) {
        assertThat(Country.fromCode(code))
            .as("fromCode(\"%s\") must be case-sensitive", code)
            .isNull();
    }

    @DisplayName("fromCode() returns null for codes containing digits or special characters")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"D1", "1D", "$$", "D-"})
    void fromCode_nonAlphabeticCode_returnsNull(String code) {
        assertThat(Country.fromCode(code))
            .as("fromCode(\"%s\") with non-alphabetic chars", code)
            .isNull();
    }

    @DisplayName("fromCode() accepts StringBuilder and returns the correct constant")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @CsvSource(delimiter = '|', value = {
        "DE | DE",
        "FR | FR",
        "XK | XK",
        "ZA | ZA"
    })
    @SuppressWarnings("UnnecessaryStringBuilder")
    void fromCode_stringBuilder_returnsCorrectConstant(String code, Country country) {
        assertThat(Country.fromCode(new StringBuilder(code))).isSameAs(country);
    }

    @DisplayName("fromCode() accepts StringBuffer and returns the correct constant")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @CsvSource(delimiter = '|', value = {
        "GB | GB",
        "JP | JP"
    })
    @SuppressWarnings("JdkObsolete")
    void fromCode_stringBuffer_returnsCorrectConstant(String code, Country country) {
        assertThat(Country.fromCode(new StringBuffer(code))).isSameAs(country);
    }

    @DisplayName("fromCode() returns null for null or wrong-length StringBuilder/StringBuffer inputs")
    @Test
    @SuppressWarnings({"UnnecessaryStringBuilder", "JdkObsolete"})
    void fromCode_charSequenceEdgeCases_returnsNull() {
        assertThat(Country.fromCode((CharSequence) null)).isNull();
        assertThat(Country.fromCode(new StringBuilder())).isNull();           // empty
        assertThat(Country.fromCode(new StringBuilder("D"))).isNull();        // too short
        assertThat(Country.fromCode(new StringBuilder("DEU"))).isNull();      // too long
        assertThat(Country.fromCode(new StringBuilder("de"))).isNull();       // lowercase
        assertThat(Country.fromCode(new StringBuffer(" D"))).isNull();        // whitespace
    }

    @DisplayName("isAssigned() returns true for officially assigned codes")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"KP", "SY", "IR", "CU", "VE", "RU", "NI", "BY", "DZ", "JP", "AF", "RE", "CW"})
    void isAssigned_assignedCodes_returnsTrue(CharSequence code) {
        assertThat(Country.isAssigned(code))
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
        assertThat(Country.isAssigned(code))
            .as("'%s' should not be assigned", code)
            .isFalse();
    }

    @DisplayName("isAssigned() accepts StringBuilder and StringBuffer")
    @Test
    @SuppressWarnings({"UnnecessaryStringBuilder", "JdkObsolete"})
    void isAssigned_charSequenceTypes_workCorrectly() {
        assertThat(Country.isAssigned(new StringBuilder("DE"))).isTrue();
        assertThat(Country.isAssigned(new StringBuilder("AA"))).isFalse();
        assertThat(Country.isAssigned(new StringBuffer("FR"))).isTrue();
        assertThat(Country.isAssigned(new StringBuffer("ZZ"))).isFalse();
        assertThat(Country.isAssigned((CharSequence) null)).isFalse();
    }

    @DisplayName("isAssigned(char, char) returns true for valid pairs and false for invalid ones")
    @ParameterizedTest(name = "[{index}] ''{0}'', ''{1}'' -> {2}")
    @CsvSource(delimiter = '|', value = {
        "D | E | true",
        "F | R | true",
        "d | e | false",
        "A | A | false",
        "A | 1 | false",
        "1 | A | false",
        "0 | 0 | false",
        "_ | _ | false"
    })
    void isAssigned_primitiveChars_worksCorrectly(char c1, char c2, boolean expectedResult) {
        assertThat(Country.isAssigned(c1, c2)).isEqualTo(expectedResult);
    }

    @DisplayName("toString() follows 'getClass().getSimpleName()[NAME, countryName=COUNTRY_NAME]' pattern for all constants")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void toString_allConstants_followsPattern(Country code) {
        assertThat(code)
            .as("%s.toString()", code.name())
            .hasToString("%s[%s, name=%s]", Country.class.getSimpleName(), code.name(), code.getCountryName());
    }

    @DisplayName("values() contains exactly 249 constants")
    @Test
    void enum_values_contains249Constants() {
        assertThat(Country.values()).hasSize(249);
    }

    @DisplayName("valueOf() resolves every constant from its code string")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void enum_valueOf_resolvesEveryConstant(Country c) {
        assertThat(Country.valueOf(c.name()))
            .as("valueOf(\"%s\")", c.name())
            .isSameAs(c);
    }

    @DisplayName("valueOf() throws IllegalArgumentException for an unknown name")
    @Test
    void enum_valueOf_unknownName_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Country.valueOf("XX"));
    }

    @DisplayName("Every code is exactly 2 characters long")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void invariant_allCodes_areExactlyTwoChars(Country c) {
        assertThat(c.getCode())
            .as("%s.getCode()", c.name())
            .hasSize(2);
    }

    @DisplayName("Every code consists of two uppercase ASCII letters (A–Z)")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void invariant_allCodes_areUpperCaseAsciiLetters(Country c) {
        assertThat(c.getCode())
            .as("%s.getCode() must be two uppercase ASCII letters", c.name())
            .matches("^[A-Z]{2}$");
    }

    @DisplayName("No duplicate codes exist across all constants")
    @Test
    void invariant_allCodes_areUnique() {
        Set<String> seen = new LinkedHashSet<>();
        for (Country c : Country.values()) {
            assertThat(seen.add(c.getCode()))
                .as("Duplicate code detected: %s", c.getCode())
                .isTrue();
        }
    }

    @DisplayName("Lookup round-trip: fromCode(c.getCode()) returns the identical instance")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Country.class)
    void invariant_lookupRoundTrip_returnsIdenticalInstance(Country c) {
        assertThat(Country.fromCode(c.getCode()))
            .as("fromCode(\"%s\") round-trip", c.getCode())
            .isSameAs(c);
    }

    @DisplayName("XK (Kosovo) is included as the only non-standard code and is correctly labelled")
    @Test
    void invariant_xk_isIncludedAndCorrectlyLabelled() {
        assertThat(Country.fromCode("XK"))
            .isNotNull()
            .extracting(Country::getCode, Country::getCountryName)
            .containsExactly("XK", "Kosovo");
    }

    @DisplayName("Deleted and reserved codes are not present in the enum")
    @Test
    void invariant_deletedOrReservedCodes_areAbsent() {
        for (String code : Arrays.asList("AA", "ZZ", "CS", "AN", "QM", "XA", "EU", "UN")) {
            assertThat(Country.fromCode(code))
                .as("'%s' must not be in the enum", code)
                .isNull();
        }
    }

    @DisplayName("AQ is the only constant with a null currency code")
    @Test
    void invariant_onlyAQ_hasNullCurrencyCode() {
        long nullCount = Stream.of(Country.values())
            .filter(c -> c.getCurrency() == null)
            .count();
        assertThat(nullCount)
            .as("Exactly one constant (AQ) should have a null currency code")
            .isEqualTo(1L);
        assertThat(Country.AQ.getCurrency()).isNull();
    }

    @DisplayName("No two constants share an identical country name")
    @Test
    void invariant_allCountryNames_areUnique() {
        Set<String> seen = new LinkedHashSet<>();
        for (Country c : Country.values()) {
            assertThat(seen.add(c.getCountryName()))
                .as("Duplicate country name detected: '%s' (%s)", c.getCountryName(), c.getCode())
                .isTrue();
        }
    }

    @DisplayName("createFlagEmoji() converts assigned country codes to their flag emoji")
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "DE | 🇩🇪",
        "US | 🇺🇸",
        "CH | 🇨🇭",
        "PS | 🇵🇸",
        "GB | 🇬🇧",
        "FR | 🇫🇷",
        "JP | 🇯🇵",
        "ZA | 🇿🇦",
        "XK | 🇽🇰",
        "AU | 🇦🇺"
    })
    void createFlagEmoji_validCodes_returnsCorrectEmoji(String countryCode, String expectedEmoji) {
        assertThat(Country.createFlagEmoji(countryCode))
            .as("Emoji conversion of '%s' failed", countryCode)
            .isEqualTo(expectedEmoji);
    }

    @DisplayName("createFlagEmoji() throws IllegalArgumentException for null or empty input")
    @ParameterizedTest(name = "[{index}] null/empty input")
    @NullAndEmptySource
    void createFlagEmoji_nullOrEmpty_throwsIllegalArgumentException(String countryCode) {
        assertThatThrownBy(() -> Country.createFlagEmoji(countryCode))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("createFlagEmoji() throws IllegalArgumentException for invalid codes")
    @ParameterizedTest(name = "[{index}] ''{0}'' is invalid")
    @ValueSource(strings = {
        "D",   // too short
        "USA", // too long
        "de",  // lowercase
        "D1",  // contains digit
        "?!"   // non-alphabetic
    })
    void createFlagEmoji_invalidCodes_throwsIllegalArgumentException(String countryCode) {
        assertThatThrownBy(() -> Country.createFlagEmoji(countryCode))
            .as("createFlagEmoji(\"%s\") should throw IllegalArgumentException", countryCode)
            .isInstanceOf(IllegalArgumentException.class);
    }

}
