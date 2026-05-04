package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit tests for {@link CountryUtil}.
 * <p>
 * Note: {@link CountryUtil#isValidCountryCode(CharSequence)} now delegates to
 * {@link Iso3166Alpha2#isAssigned(CharSequence)} and therefore performs a full
 * registry lookup — not merely a format check.
 */
@SuppressWarnings("checkstyle:MethodName")
@DisplayName("CountryUtil")
final class CountryUtilTest {

    @DisplayName("isValidCountryCode() returns true for officially assigned ISO 3166-1 codes")
    @ParameterizedTest(name = "[{index}] ''{0}'' is assigned")
    @ValueSource(strings = {
        "TV", // Tuvalu
        "NR", // Nauru
        "PW", // Palau
        "KM", // Comoros
        "LS", // Lesotho
        "GQ", // Equatorial Guinea
        "TL", // Timor-Leste
        "BT", // Bhutan
        "KI", // Kiribati
        "ST", // São Tomé and Príncipe
        "VU", // Vanuatu
        "ER", // Eritrea
        "DJ", // Djibouti
        "WS", // Samoa
        "TO", // Tonga
        "XK", // Kosovo — user-assigned but included in the registry
        "VA", // Holy See
        "AX"  // Åland Islands
    })
    void isValidCountryCode_assignedCodes_returnsTrue(String countryCode) {
        assertThat(CountryUtil.isValidCountryCode(countryCode))
            .as("'%s' is an assigned code and should be valid", countryCode)
            .isTrue();
    }

    @DisplayName("isValidCountryCode() returns false for null or empty input")
    @ParameterizedTest(name = "[{index}] null/empty input")
    @NullAndEmptySource
    void isValidCountryCode_nullOrEmpty_returnsFalse(String countryCode) {
        assertThat(CountryUtil.isValidCountryCode(countryCode)).isFalse();
    }

    @DisplayName("isValidCountryCode() returns false for codes with wrong length")
    @ParameterizedTest(name = "[{index}] ''{0}'' has wrong length")
    @ValueSource(strings = {
        "D",   // too short
        "USA", // too long
        " D",  // leading space
        "D ",  // trailing space
        "D\t"  // tab character
    })
    void isValidCountryCode_wrongLength_returnsFalse(String countryCode) {
        assertThat(CountryUtil.isValidCountryCode(countryCode))
            .as("'%s' has the wrong length and should be invalid", countryCode)
            .isFalse();
    }

    @DisplayName("isValidCountryCode() returns false for wrong-case codes")
    @ParameterizedTest(name = "[{index}] ''{0}'' is not uppercase")
    @ValueSource(strings = {"de", "Us", "dE"})
    void isValidCountryCode_wrongCase_returnsFalse(String countryCode) {
        assertThat(CountryUtil.isValidCountryCode(countryCode))
            .as("\"%s\" is not uppercase and should be invalid", countryCode)
            .isFalse();
    }

    @DisplayName("isValidCountryCode() returns false for codes with digits or special characters")
    @ParameterizedTest(name = "[{index}] ''{0}'' contains non-alphabetic chars")
    @ValueSource(strings = {"D1", "1D", "$$", "AA "})
    void isValidCountryCode_nonAlphabeticChars_returnsFalse(String countryCode) {
        assertThat(CountryUtil.isValidCountryCode(countryCode))
            .as("'%s' contains non-alphabetic characters and should be invalid", countryCode)
            .isFalse();
    }

    @DisplayName("isValidCountryCode() returns false for syntactically valid but unassigned codes")
    @ParameterizedTest(name = "[{index}] ''{0}'' is unassigned")
    @ValueSource(strings = {
        "AA", // user-assigned range
        "ZZ", // user-assigned range
        "QQ", // not assigned
        "CS", // deleted (former Serbia and Montenegro)
        "AN"  // deleted (former Netherlands Antilles)
    })
    void isValidCountryCode_unassignedCodes_returnsFalse(String countryCode) {
        assertThat(CountryUtil.isValidCountryCode(countryCode))
            .as("\"%s\" is not an assigned ISO 3166-1 code and should be invalid", countryCode)
            .isFalse();
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
        assertThat(CountryUtil.createFlagEmoji(countryCode))
            .as("Emoji conversion of '%s' failed", countryCode)
            .isEqualTo(expectedEmoji);
    }

    @DisplayName("createFlagEmoji() throws IllegalArgumentException for null or empty input")
    @ParameterizedTest(name = "[{index}] null/empty input")
    @NullAndEmptySource
    void createFlagEmoji_nullOrEmpty_throwsIllegalArgumentException(String countryCode) {
        assertThatThrownBy(() -> CountryUtil.createFlagEmoji(countryCode))
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
        assertThatThrownBy(() -> CountryUtil.createFlagEmoji(countryCode))
            .as("createFlagEmoji(\"%s\") should throw IllegalArgumentException", countryCode)
            .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Private constructor throws UnsupportedOperationException (utility class guard)")
    @Test
    void constructor_reflectiveInstantiation_throwsUnsupportedOperationException() {
        TestUtil.assertConstructorIsPrivate(CountryUtil.class);
    }

}

