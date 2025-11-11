package de.speedbanking.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * JUnit test class for {@link CharUtil}.
 */
@SuppressWarnings({"checkstyle:MethodName"})
class CharUtilTest extends Assertions {

    @Test
    void privateConstructor_shouldThrowException() throws Exception {
        Constructor<CharUtil> constructor = CharUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(constructor::newInstance)
            .withCauseInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getMessage)
            .isEqualTo("Utility class " + CharUtil.class.getSimpleName() + " cannot be instantiated");
    }

    @ParameterizedTest(name = "Char ''{0}'' should be a digit")
    @ValueSource(chars = {'0', '5', '9'})
    void isDigit_ShouldReturnTrueForDigits(char c) {
        assertThat(CharUtil.isDigit(c)).isTrue();
    }

    @ParameterizedTest(name = "Char ''{0}'' should not be a digit")
    @ValueSource(chars = {'A', 'a', '-', ' ', '/', ':', ':', '9' + 1})
    void isDigit_ShouldReturnFalseForNonDigits(char c) {
        assertThat(CharUtil.isDigit(c)).isFalse();
    }

    @Test
    void isAllDigits_ShouldReturnTrueForValidRanges() {
        char[] arr = {'1', '2', 'A', '4', '5'};
        // full array (when valid)
        assertThat(CharUtil.isAllDigits(new char[]{'1', '2', '3'}, 0, 3)).isTrue();
        // sub-range
        assertThat(CharUtil.isAllDigits(arr, 0, 2)).isTrue(); // '1', '2'
        // empty range
        assertThat(CharUtil.isAllDigits(arr, 2, 2)).isTrue();
    }

    @Test
    void isAllDigits_ShouldReturnFalseWhenNonDigitFound() {
        char[] arr = {'1', '2', 'A', '4'};
        // in the middle
        assertThat(CharUtil.isAllDigits(arr, 0, 4)).isFalse(); // 'A'
        // at the boundary (end)
        assertThat(CharUtil.isAllDigits(arr, 2, 4)).isFalse(); // 'A', '4'
        // non-digit at start
        assertThat(CharUtil.isAllDigits(arr, 2, 3)).isFalse(); // 'A'
    }

    @Test
    void isAllDigits_ShouldThrowExceptionForInvalidInput() {
        // null array check
        assertThatNullPointerException()
            .isThrownBy(() -> CharUtil.isAllDigits(null, 0, 1))
            .withMessage("Character array cannot be null");

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
    }

    @ParameterizedTest(name = "Char ''{0}'' should not be uppercase")
    @ValueSource(chars = {'a', '5', '!', '@', '[', '`', '{'})
    void isUpperCase_ShouldReturnFalseForNonUpperCase(char c) {
        assertThat(CharUtil.isUpperCase(c)).isFalse();
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
            .isThrownBy(() -> CharUtil.isAllUpperCase(null, 0, 1))
            .withMessage("Character array cannot be null");

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
        char[] arr = {'1', 'A', 'b', 'Z', '*'};
        // Contains 'b' (lowercase)
        assertThat(CharUtil.isAllDigitOrUpperCase(arr, 0, 4)).isFalse();
        // Contains '*'
        assertThat(CharUtil.isAllDigitOrUpperCase(arr, 0, 5)).isFalse();
        // Invalid at start
        assertThat(CharUtil.isAllDigitOrUpperCase(new char[] {'@', 'A'}, 0, 2)).isFalse();
    }

    @Test
    void isAllDigitOrUpperCase_ShouldThrowExceptionForInvalidInput() {
        // Null array check
        assertThatNullPointerException()
            .isThrownBy(() -> CharUtil.isAllDigitOrUpperCase(null, 0, 1))
            .withMessage("Character array cannot be null");

        char[] arr = new char[] {'1', 'A'};
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllDigitOrUpperCase(arr, -1, 1))
            .withMessage("Invalid range (-1, 1) specified, valid range (0, 2)");
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> CharUtil.isAllDigitOrUpperCase(arr, 0, 3))
            .withMessage("Invalid range (0, 3) specified, valid range (0, 2)");
    }

}
