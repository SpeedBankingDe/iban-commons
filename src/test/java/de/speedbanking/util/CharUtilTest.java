package de.speedbanking.util;

import de.speedbanking.test.TestUtil;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit test class for {@link CharUtil}.
 */
@SuppressWarnings({"checkstyle:MethodName"})
class CharUtilTest extends Assertions {

    @Test
    void privateConstructor_shouldThrowException() {
        TestUtil.assertConstructorIsPrivate(CharUtil.class);
    }

    @ParameterizedTest(name = "Char ''{0}'' should be a digit")
    @ValueSource(chars = {'0', '5', '9'})
    void isDigit_ShouldReturnTrueForDigits(char c) {
        assertThat(CharUtil.isDigit(c)).isTrue();
        assertThat(CharUtil.isNotDigit(c)).isFalse();
    }

    @ParameterizedTest(name = "Char ''{0}'' should not be a digit")
    @ValueSource(chars = {'A', 'a', '-', ' ', '/', ':', ':', '9' + 1})
    void isDigit_ShouldReturnFalseForNonDigits(char c) {
        assertThat(CharUtil.isDigit(c)).isFalse();
        assertThat(CharUtil.isNotDigit(c)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        // input, beginIndex, endIndex, expected result
        "'123',   0, 3, true",  // full array (0 to 3 exclusive)
        "'12A45', 0, 2, true",  // sub-range '1', '2'
        "'12A45', 2, 2, true",  // empty range (2 to 2)
        "'12A4',  0, 4, false", // 'A' in the middle (index 2)
        "'12A4',  2, 4, false", // 'A', '4' (boundary/end)
        "'12A4',  2, 3, false"  // just 'A' (index 2 to 3 exclusive)
    })
    void isAllDigits_ShouldValidateDigitRanges(String input, int beginIndex, int endIndex, boolean expected) {
        char[] arr = input.toCharArray();
        boolean result = CharUtil.isAllDigits(arr, beginIndex, endIndex);

        assertThat(result)
            .as("Checking if range [%d, %d) in '%s' is digits: %b", beginIndex, endIndex, input, expected)
            .isEqualTo(expected);
    }

    @Test
    void isAllDigits_ShouldThrowExceptionForInvalidInput() {
        // null array check
        assertThatNullPointerException()
            .isThrownBy(() -> CharUtil.isAllDigits((char[]) null, 0, 1))
            .withMessage("Input cannot be null");

        // invalid index ranges check (for increased coverage)
        char[] arr = new char[] {'1', '2'};
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllDigits(arr, -1, 1))
            .withMessage("Invalid range (-1, 1) specified, valid range (0, 2)");
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllDigits(arr, 0, 3))
            .withMessage("Invalid range (0, 3) specified, valid range (0, 2)");
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllDigits(arr, 3, 3))
            .withMessage("Invalid range (3, 3) specified, valid range (0, 2)");
    }

    @ParameterizedTest(name = "Char ''{0}'' should be uppercase")
    @ValueSource(chars = {'A', 'M', 'Z'})
    void isUpperCase_ShouldReturnTrueForUpperCaseLetters(char c) {
        assertThat(CharUtil.isUpperCase(c)).isTrue();
        assertThat(CharUtil.isNotUpperCase(c)).isFalse();

        assertThat(CharUtil.isLowerCase(c)).isFalse();
        assertThat(CharUtil.isNotLowerCase(c)).isTrue();
    }

    @ParameterizedTest(name = "Char ''{0}'' should not be uppercase")
    @ValueSource(chars = {'a', '5', '!', '@', '[', '`', '{'})
    void isUpperCase_ShouldReturnFalseForNonUpperCase(char c) {
        assertThat(CharUtil.isUpperCase(c)).isFalse();
        assertThat(CharUtil.isNotUpperCase(c)).isTrue();
    }

    @ParameterizedTest(name = "Char ''{0}'' should be lowercase")
    @ValueSource(chars = {'j', 'a', 'p', 'a', 'n'})
    void isLowerCase_ShouldReturnTrueForLowerCaseLetters(char c) {
        assertThat(CharUtil.isLowerCase(c)).isTrue();
        assertThat(CharUtil.isNotLowerCase(c)).isFalse();
    }

    @ParameterizedTest(name = "Char ''{0}'' should not be uppercase")
    @ValueSource(chars = {'A', '5', '!', '@', '[', '`', '{'})
    void isLowerCase_ShouldReturnFalseForNonLowerCaseLetters(char c) {
        assertThat(CharUtil.isLowerCase(c)).isFalse();
        assertThat(CharUtil.isNotLowerCase(c)).isTrue();
    }

    @Test
    void isAllUpperCase_ShouldReturnTrueForValidRanges() {
        char[] arr = {'A', 'B', 'c', 'D', 'E'};
        assertThat(CharUtil.isAllUpperCase(new char[]{'A', 'B', 'C'}, 0, 3)).isTrue();
        assertThat(CharUtil.isAllUpperCase(arr, 0, 2)).isTrue(); // 'A', 'B'
        assertThat(CharUtil.isAllUpperCase(arr, 2, 2)).isTrue(); // Empty range
    }

    @Test
    void isAllUpperCase_ShouldReturnFalseWhenNonUpperCaseFound() {
        char[] arr = {'A', '1', 'C', 'd'};
        assertThat(CharUtil.isAllUpperCase(arr, 0, 4)).isFalse(); // contains '1' and 'd'
        assertThat(CharUtil.isAllUpperCase(arr, 3, 4)).isFalse(); // contains 'd'
        assertThat(CharUtil.isAllUpperCase(arr, 1, 3)).isFalse(); // contains '1'
    }

    @Test
    void isAllUpperCase_ShouldThrowExceptionForInvalidInput() {
        // null array check
        assertThatNullPointerException()
            .isThrownBy(() -> CharUtil.isAllUpperCase((char[]) null, 0, 1))
            .withMessage("Input cannot be null");

        // Invalid index ranges check (for increased coverage)
        char[] arr = new char[] {'A', 'B'};
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllUpperCase(arr, -1, 1))
            .withMessage("Invalid range (-1, 1) specified, valid range (0, 2)");
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllUpperCase(arr, 0, 3))
            .withMessage("Invalid range (0, 3) specified, valid range (0, 2)");
    }

    @ParameterizedTest(name = "Char ''{0}'' should be digit or uppercase")
    @ValueSource(chars = {'5', 'A', 'Z', '0', '9'})
    void isDigitOrUpperCase_ShouldReturnTrueForValidChars(char c) {
        assertThat(CharUtil.isDigitOrUpperCase(c)).isTrue();
    }

    @ParameterizedTest(name = "Char ''{0}'' should not be digit or uppercase")
    @ValueSource(chars = {'a', '!', ' ', '\0', ':', '@', '[', '`', '{'})
    void isDigitOrUpperCase_ShouldReturnFalseForInvalidChars(char c) {
        assertThat(CharUtil.isDigitOrUpperCase(c)).isFalse();
    }

    @Test
    void isAllDigitOrUpperCase_ShouldReturnTrueForMixedValidChars() {
        char[] arr = {'1', 'A', '5', 'Z'};
        assertThat(CharUtil.isAllDigitOrUpperCase(arr, 0, arr.length)).isTrue();
        // sub-range
        assertThat(CharUtil.isAllDigitOrUpperCase(new char[]{'1', 'a', 'Z'}, 0, 1)).isTrue(); // Only '1'
        assertThat(CharUtil.isAllDigitOrUpperCase(arr, 1, 4)).isTrue(); // 'A', '5', 'Z'
        assertThat(CharUtil.isAllDigitOrUpperCase(arr, 2, 2)).isTrue(); // Empty range
    }

    @Test
    void isAllDigitOrUpperCase_ShouldReturnFalseWhenInvalidCharFound() {
        char[] arr1 = {'1', 'A', 'b', 'Z', '*'};
        // contains 'b' (lowercase)
        assertThat(CharUtil.isAllDigitOrUpperCase(arr1, 0, 4)).isFalse();
        assertThat(CharUtil.isAllDigitOrUpperCase(String.valueOf(arr1, 0, 4))).isFalse();
        // contains '*'
        assertThat(CharUtil.isAllDigitOrUpperCase(arr1, 0, 5)).isFalse();
        assertThat(CharUtil.isAllDigitOrUpperCase(String.valueOf(arr1, 0, 5))).isFalse();
        // invalid at start
        char[] arr2 = new char[] {'@', 'A'};
        assertThat(CharUtil.isAllDigitOrUpperCase(arr2, 0, 2)).isFalse();
        assertThat(CharUtil.isAllDigitOrUpperCase(String.valueOf(arr2, 0, 2))).isFalse();
    }

    @Test
    void isAllDigitOrUpperCase_ShouldThrowExceptionForInvalidInput() {
        // Null array check
        assertThatNullPointerException()
            .isThrownBy(() -> CharUtil.isAllDigitOrUpperCase((char[]) null, 0, 1))
            .withMessage("Input cannot be null");

        char[] arr = new char[] {'1', 'A'};
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllDigitOrUpperCase(arr, -1, 1))
            .withMessage("Invalid range (-1, 1) specified, valid range (0, 2)");
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllDigitOrUpperCase(arr, 0, 3))
            .withMessage("Invalid range (0, 3) specified, valid range (0, 2)");
    }

}
