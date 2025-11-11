package de.speedbanking.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * JUnit test class for {@link CountryUtil}.
 */
@SuppressWarnings({"checkstyle:MethodName"})
class CountryUtilTest extends Assertions {

    @DisplayName("isValidCountryCode should return true for valid two-letter uppercase codes")
    @ParameterizedTest
    @ValueSource(strings = {
        "DE",
        "US",
        "CH",
        "ZZ", // Valid format, even if not a real ISO code (only format check)
        "AA"
    })
    void isValidCountryCode_validCodes_shouldReturnTrue(String countryCode) {
        assertTrue(CountryUtil.isValidCountryCode(countryCode),
            () -> "Code '" + countryCode + "' should be considered valid");
    }

    /**
     * Tests invalid country codes using CsvSource.
     */
    @DisplayName("isValidCountryCode should return false for invalid codes")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        // Invalid Length
        "D",
        "USA",
        // Invalid Case
        "de",
        "Us",
        "dE",
        // Contains numbers/special chars
        "D1",
        "$$",
        "AA ", // Space is invalid
        " A"
    })
    void isValidCountryCode_invalidCodes_shouldReturnFalse(String countryCode) {
        assertFalse(CountryUtil.isValidCountryCode(countryCode),
            () -> "Code '" + countryCode + "' should be considered invalid");
    }

    /**
     * Tests the null case explicitly.
     */
    @DisplayName("isValidCountryCode should return false for null input")
    @Test
    void isValidCountryCode_null_shouldReturnFalse() {
        assertFalse(CountryUtil.isValidCountryCode(null));
    }

    @DisplayName("createFlagEmoji should correctly convert valid codes to their emoji representation")
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "DE | 🇩🇪",
        "US | 🇺🇸",
        "CH | 🇨🇭",
        "PS | 🇵🇸",
        "GB | 🇬🇧"
    })
    void createFlagEmoji_validCodes_shouldReturnEmoji(String countryCode, String expectedEmoji) {
        assertEquals(expectedEmoji, CountryUtil.createFlagEmoji(countryCode),
            () -> "Conversion of '" + countryCode + "' failed");
    }

    @DisplayName("createFlagEmoji should throw IllegalArgumentException for invalid codes")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "D",    // Too short
        "USA",  // Too long
        "de",   // Lowercase
        "D1",   // Contains number
        ""      // Empty string
    })
    void createFlagEmoji_invalidCodes_shouldThrowException(String countryCode) {
        assertThrows(IllegalArgumentException.class,
            () -> CountryUtil.createFlagEmoji(countryCode),
            () -> "Calling createFlagEmoji with '" + countryCode + "' should throw an exception");
    }

    /**
     * Tests that the private constructor throws an UnsupportedOperationException
     * to prevent instantiation of the utility class.
     */
    @DisplayName("Constructor should throw UnsupportedOperationException")
    @Test
    void constructor_shouldThrowException() throws NoSuchMethodException {
        Constructor<CountryUtil> constructor = CountryUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance,
            "Attempting to instantiate CountryUtil should throw UnsupportedOperationException");
        assertInstanceOf(UnsupportedOperationException.class, ex.getCause());
    }

}
