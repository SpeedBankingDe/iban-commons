package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.iban.IbanRegistry;
import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link Mod97}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class Mod97Test {

    private static final String VALID_DE_IBAN    = "DE91100000000123456789";
    private static final String TAMPERED_DE_IBAN = "DE91100000000123456780";

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    @Test
    void constants_modulus() {
        assertThat(Mod97.MODULUS).isEqualTo(97);
    }

    @Test
    void constants_validRemainder() {
        assertThat(Mod97.VALID_REMAINDER).isEqualTo(1);
    }

    @Test
    void constants_invalidRemainder() {
        assertThat(Mod97.INVALID_REMAINDER).isEqualTo(-1);
    }

    @Test
    void privateConstructor_shouldThrowException() {
        TestUtil.assertConstructorIsPrivate(Mod97.class);
    }

    // -------------------------------------------------------------------------
    // calculate(CharSequence)
    // -------------------------------------------------------------------------

    @Test
    void calculate_charSequence_null_returnsInvalidRemainder() {
        assertThat(Mod97.calculate((CharSequence) null)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        "DE91 1000 0000 0123 4567 89", // spaces
        "de91100000000123456789",      // lowercase
        "DE91100000000123456789!"      // special chars
    })
    @NullAndEmptySource
    void calculate_charSequence_invalidInput_returnsInvalidRemainder(String input) {
        assertThat(Mod97.calculate(input)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @ParameterizedTest(name = "calculate({0}) should be {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "DE03869337585814897021       |  1",
        "BY27H03U2623LQ9QV1JOGK5DG7SZ |  1",
        "CH8173414K9E1UUCEF0K3        |  1",
        "CR51102873298304277251       |  1",
        "CV0949318910112F4MZAZJIBN    |  1",
        "CY38785624228KNFCI0QQVCG9465 |  1",
        "0 | -1",
        "1 | -1",
        "A | -1",
        "Z | -1"
    })
    void calculate_charSequence_knownValues(String input, int expected) {
        assertThat(Mod97.calculate(input)).isEqualTo(expected);
    }

    @Test
    void calculate_charSequence_gbIban_returnsOne() {
        // GB-IBANs start with two letters, so this exercises the letter-to-digit
        // conversion in both the BBAN and header pass
        assertThat(Mod97.calculate("GB29NWBK60161331926819")).isEqualTo(Mod97.VALID_REMAINDER);
    }

    @Test
    void calculate_charSequence_illegalCharInBban_returnsInvalidRemainder() {
        assertThat(Mod97.calculate("DE00 00000000123456789")).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_charSequence_illegalCharInHeader_returnsInvalidRemainder() {
        // space at position 1 — header chars are processed in the second pass,
        // so the BBAN needs to be clean for this to reach that code path
        assertThat(Mod97.calculate("D 91100000000123456789")).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    // -------------------------------------------------------------------------
    // calculate(char[])
    // -------------------------------------------------------------------------

    @Test
    void calculate_charArray_null_returnsInvalidRemainder() {
        assertThat(Mod97.calculate((char[]) null)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_charArray_empty_returnsInvalidRemainder() {
        assertThat(Mod97.calculate(new char[0])).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_charArray_validIban_returnsOne() {
        assertThat(Mod97.calculate(VALID_DE_IBAN.toCharArray())).isEqualTo(Mod97.VALID_REMAINDER);
    }

    @Test
    void calculate_charArray_tamperedIban_doesNotReturnOne() {
        assertThat(Mod97.calculate(TAMPERED_DE_IBAN.toCharArray()))
            .isNotEqualTo(Mod97.VALID_REMAINDER);
    }

    @Test
    void calculate_charArray_sameResultAsCharSequence() {
        String iban = "GB29NWBK60161331926819";
        assertThat(Mod97.calculate(iban.toCharArray())).isEqualTo(Mod97.calculate(iban));
    }

    // -------------------------------------------------------------------------
    // calculate(char[], int)
    // -------------------------------------------------------------------------

    @Test
    void calculate_charArrayWithLen_null_returnsInvalidRemainder() {
        assertThat(Mod97.calculate((char[]) null, 5)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_charArrayWithLen_tooShort_returnsInvalidRemainder() {
        assertThat(Mod97.calculate(VALID_DE_IBAN.toCharArray(), 3)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_charArrayWithLen_zeroLen_fallsBackToArrayLength() {
        // inputLen <= 0 means "use array length" — handy for callers that
        // don't know the length up front
        char[] data = VALID_DE_IBAN.toCharArray();
        assertThat(Mod97.calculate(data, 0)).isEqualTo(Mod97.VALID_REMAINDER);
        assertThat(Mod97.calculate(data, -1)).isEqualTo(Mod97.VALID_REMAINDER);
    }

    @Test
    void calculate_charArrayWithLen_explicitLen_limitsProcessing() {
        // the array is longer than inputLen — trailing chars must be ignored
        char[] data = "GB29NWBK60161331926819GARBAGE".toCharArray();
        assertThat(Mod97.calculate(data, 22)).isEqualTo(Mod97.VALID_REMAINDER);
    }

    @Test
    void calculate_charArrayWithLen_illegalCharInBban_returnsInvalidRemainder() {
        assertThat(Mod97.calculate("DE00 00000000000000000".toCharArray(), 22))
            .isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_charArrayWithLen_illegalCharInHeader_returnsInvalidRemainder() {
        assertThat(Mod97.calculate("D 91100000000123456789".toCharArray(), 22))
            .isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_charArrayWithLen_allDigitHeader_isAccepted() {
        // headers aren't always two letters — some synthetic NCD inputs are all-numeric
        char[] data = "123456789012345678".toCharArray();
        assertThat(Mod97.calculate(data, data.length)).isBetween(0, 96);
    }

    @Test
    void calculate_triggerOverflowGuard() {
        // without intermediate mod steps a 24-digit accumulator would overflow int
        String input = "999999999999999999999999";
        int result = Mod97.calculate(input);
        assertThat(result).isBetween(0, 96);
        assertThat(Mod97.calculate(input.toCharArray())).isEqualTo(result);
    }

    // -------------------------------------------------------------------------
    // calculateRange(char[], int, int)
    // -------------------------------------------------------------------------

    @Test
    void calculateRange_charArray_null_throwsNullPointerException() {
        assertThatNullPointerException()
            .isThrownBy(() -> Mod97.calculateRange((char[]) null, 0, 1));
    }

    @Test
    void calculateRange_charArray_illegalChar_returnsInvalidRemainder() {
        // space is a realistic mistake — e.g. a formatted IBAN passed by accident
        char[] data = "100 0".toCharArray();
        assertThat(Mod97.calculateRange(data, 0, data.length)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @ParameterizedTest(name = "[{index}] input={0} offset={1} len={2}")
    @CsvSource(delimiter = '|', value = {
        "123 | -1 |  2", // negative offset
        "123 |  5 |  2", // offset beyond array length
        "123 |  0 |  4"  // length exceeds remaining elements
    })
    void calculateRange_charArray_invalidRange_throwsIndexOutOfBoundsException(
            String input, int offset, int len) {
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> Mod97.calculateRange(input.toCharArray(), offset, len));
    }

    @Test
    void calculateRange_charArray_negativeLength_throwsIndexOutOfBoundsException() {
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> Mod97.calculateRange("ABC".toCharArray(), 0, -1));
    }

    @Test
    void calculateRange_charArray_bbanOfValidIban_returnsValueInRange() {
        char[] iban = VALID_DE_IBAN.toCharArray();
        assertThat(Mod97.calculateRange(iban, 4, iban.length - 4)).isBetween(0, 96);
    }

    // -------------------------------------------------------------------------
    // calculateRange(CharSequence, int, int)
    // -------------------------------------------------------------------------

    @Test
    void calculateRange_charSequence_null_throwsNullPointerException() {
        assertThatNullPointerException()
            .isThrownBy(() -> Mod97.calculateRange((CharSequence) null, 0, 1));
    }

    @Test
    void calculateRange_charSequence_negativeOffset_throwsIndexOutOfBoundsException() {
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> Mod97.calculateRange("ABC", -1, 2));
    }

    @Test
    void calculateRange_charSequence_negativeLength_throwsIndexOutOfBoundsException() {
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> Mod97.calculateRange("ABC", 0, -1));
    }

    @Test
    void calculateRange_charSequence_offsetPlusLengthExceedsBounds_throwsIndexOutOfBoundsException() {
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> Mod97.calculateRange("AB", 1, 3))
            .withMessageContaining("Invalid range");
    }

    @Test
    void calculateRange_charSequence_lengthExceedsBounds_throwsIndexOutOfBoundsException() {
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> Mod97.calculateRange("123", 0, 4))
            .withMessageContaining("Invalid range");
    }

    @Test
    void calculateRange_charSequence_emptyRange_returnsZero() {
        assertThat(Mod97.calculateRange("ABC123", 0, 0)).isEqualTo(0);
    }

    @Test
    void calculateRange_charSequence_digitsAndLetters() {
        assertThat(Mod97.calculateRange("100000", 0, 6)).isBetween(0, 96);
        assertThat(Mod97.calculateRange("NWBK60", 0, 6)).isBetween(0, 96);
    }

    @Test
    void calculateRange_charSequence_illegalChar_returnsInvalidRemainder() {
        assertThat(Mod97.calculateRange("DE 123", 0, 6)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculateRange_charSequence_offset_limitsRange() {
        // "45" from "12345" — deliberately short so we can compute the expected value by hand:
        // 4 → total = 4; 5 → total = (4 * 10 + 5) % 97 = 45
        assertThat(Mod97.calculateRange("12345", 3, 2)).isEqualTo(45);
    }

    @Test
    void calculateRange_charSequence_exactUpperBoundary_doesNotThrow() {
        // offset == length, len == 0: valid but vacuous
        assertThat(Mod97.calculateRange("123", 3, 0)).isEqualTo(0);
    }

    @Test
    void calculateRange_sameResultForCharArrayAndCharSequence() {
        char[] data = "DE123456789".toCharArray();
        CharArrayWrapper wrapper = new CharArrayWrapper(data);
        assertThat(Mod97.calculateRange(wrapper, 2, 5))
            .isEqualTo(Mod97.calculateRange(data, 2, 5));
    }

    // -------------------------------------------------------------------------
    // isValid
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void isValid_charSequence_validIbans_returnTrue(IbanRegistry countryData) {
        assertThat(Mod97.isValid(countryData.getIbanExample())).isTrue();
    }

    @Test
    void isValid_charSequence_nullOrEmpty_returnsFalse() {
        assertThat(Mod97.isValid((CharSequence) null)).isFalse();
        assertThat(Mod97.isValid("")).isFalse();
    }

    @Test
    void isValid_charSequence_validAndTampered() {
        assertThat(Mod97.isValid(VALID_DE_IBAN)).isTrue();
        assertThat(Mod97.isValid(TAMPERED_DE_IBAN)).isFalse();
    }

    @Test
    void isValid_charArray_validAndTampered() {
        assertThat(Mod97.isValid(VALID_DE_IBAN.toCharArray())).isTrue();
        assertThat(Mod97.isValid(TAMPERED_DE_IBAN.toCharArray())).isFalse();
    }

    @Test
    void isValid_charArray_nullOrEmpty_returnsFalse() {
        assertThat(Mod97.isValid((char[]) null)).isFalse();
        assertThat(Mod97.isValid(new char[0])).isFalse();
    }

    @Test
    void isValid_charArrayWithLen_valid() {
        char[] data = VALID_DE_IBAN.toCharArray();
        assertThat(Mod97.isValid(data, data.length)).isTrue();
        assertThat(Mod97.isValid(data, 3)).isFalse(); // too short
    }

    @Test
    void isValid_charArrayWithLen_tampered() {
        char[] data = TAMPERED_DE_IBAN.toCharArray();
        assertThat(Mod97.isValid(data, data.length)).isFalse();
    }

}
