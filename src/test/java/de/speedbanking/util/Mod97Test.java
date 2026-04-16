package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.iban.IbanRegistry;
import de.speedbanking.iban.IbanRegistrySource;
import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link Mod97}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
class Mod97Test {

    private static final String VALID_DE_IBAN    = "DE91100000000123456789";
    private static final String TAMPERED_DE_IBAN = "DE91100000000123456780";

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

    @Test
    void calculate_charSequence_null_returnsInvalidRemainder() {
        assertThat(Mod97.calculate((CharSequence) null)).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @ParameterizedTest
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
    void calculateRange_validRange_returnsCorrectRemainder() {
        char[] iban = VALID_DE_IBAN.toCharArray();
        // testing BBAN part
        int result = Mod97.calculateRange(iban, 4, iban.length - 4);
        assertThat(result).isBetween(0, 96);
    }

    @Test
    void calculateRange_nullData_throwsNullPointerException() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> Mod97.calculateRange((char[]) null, 0, 1));
    }

    @Test
    void calculateRange_consistencyBetweenTypes() {
        char[] data = "DE123456789".toCharArray();
        CharArrayWrapper wrapper = new CharArrayWrapper(data);

        int resultArr = Mod97.calculateRange(data, 2, 5);
        int resultWrapper = Mod97.calculateRange(wrapper, 2, 5);

        assertThat(resultWrapper).isEqualTo(resultArr);
    }

    @Test
    void calculateRange_invalidRange_throwsException() {
        String data = "123";
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> Mod97.calculateRange(data, 0, 4))
            .withMessageContaining("Invalid range");
    }

    @ParameterizedTest
    @IbanRegistrySource
    void isValid_charSequence_validIbans_returnTrue(IbanRegistry countryData) {
        assertThat(Mod97.isValid(countryData.getIbanExample())).isTrue();
    }

    @Test
    void isValid_charArray_consistency() {
        char[] valid = VALID_DE_IBAN.toCharArray();
        char[] invalid = TAMPERED_DE_IBAN.toCharArray();

        assertThat(Mod97.isValid(valid)).isTrue();
        assertThat(Mod97.isValid(invalid)).isFalse();
    }

    @Test
    void calculate_largeInput_staysInValidRange() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("1");
        }
        String largeInput = sb.toString();

        assertThat(Mod97.calculate(largeInput)).isBetween(0, 96);
    }

    @Test
    void calculate_charArray_edgeCases() {
        assertThatNullPointerException()
            .isThrownBy(() -> Mod97.calculate((char[]) null));
        assertThat(Mod97.calculate(new char[0])).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @Test
    void calculate_triggerOverflowGuard() {
        String longInput = "999999999999999999999999"; // 24 mal '9'
        int result = Mod97.calculate(longInput);

        assertThat(result).isBetween(0, 96);
        assertThat(Mod97.calculate(longInput.toCharArray()))
            .isEqualTo(result);
    }

    @Test
    void calculateRange_charArray_null_throwsException() {
        assertThatNullPointerException()
            .isThrownBy(() -> Mod97.calculateRange((char[]) null, 0, 1));
    }

    @Test
    void calculateRange_charArray_invalidChars() {
        char[] data = {'1', '2', '_', 'A'};
        int result = Mod97.calculateRange(data, 0, 4);

        assertThat(result).isEqualTo(Mod97.INVALID_REMAINDER);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "123 | -1 |  2", // negative offset
        "123 |  5 |  2", // offset too large
        "123 |  0 |  4"  // length too long
    })
    void calculateRange_charArray_invalidRange_throwsException(String input, int offset, int len) {
        char[] data = input.toCharArray();
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> Mod97.calculateRange(data, offset, len));
    }

}
