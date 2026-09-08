package de.speedbanking.checkdigit.de;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Unit tests for {@link GermanAccountCheckDigit}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class GermanAccountCheckDigitTest {

    @Test
    void verify_validAccount_returnsValid() {
        CheckDigitResult result = GermanAccountCheckDigit.verify("00", "10000000", "1234567897");

        assertThat(result.isChecked()).isTrue();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void verify_invalidAccount_returnsInvalid() {
        CheckDigitResult result = GermanAccountCheckDigit.verify("00", "10000000", "1234567890");

        assertThat(result.isChecked()).isTrue();
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void verify_shorterAccountNumber_isZeroPaddedOnTheLeft() {
        // "1" alone pads to "0000000001": weighted sum over "000000000" is 0,
        // so method 00 expects check digit 0 - but the actual (padded) check digit is 1.
        CheckDigitResult padded = GermanAccountCheckDigit.verify("00", "10000000", "1");
        CheckDigitResult explicit = GermanAccountCheckDigit.verify("00", "10000000", "0000000001");

        assertThat(padded).isEqualTo(explicit);
        assertThat(padded.isValid()).isFalse();
    }

    @Test
    void verify_whitespaceInInput_isStripped() {
        CheckDigitResult withSpaces = GermanAccountCheckDigit.verify("00", "1000 0000", "1234 5678 97");
        CheckDigitResult withoutSpaces = GermanAccountCheckDigit.verify("00", "10000000", "1234567897");

        assertThat(withSpaces).isEqualTo(withoutSpaces);
        assertThat(withSpaces.isValid()).isTrue();
    }

    @Test
    void verify_methodWithNoCheck_alwaysReturnsNotCheckedValid() {
        CheckDigitResult result = GermanAccountCheckDigit.verify("09", "10000000", "0000000000");

        assertThat(result.isChecked()).isFalse();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void verify_unknownMethodCode_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("XX", "10000000", "1234567897"));
    }

    @Test
    void verify_blzTooLong_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("00", "123456789", "1234567897"));
    }

    @Test
    void verify_blzNonNumeric_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("00", "1000000A", "1234567897"));
    }

    @Test
    void verify_blzContainsCharacterBelowDigitRange_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("00", "1000000/", "1234567897"));
    }

    @Test
    void verify_accountNumberTooLong_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("00", "10000000", "12345678900"));
    }

    @Test
    void verify_accountNumberEmpty_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("00", "10000000", ""));
    }

    @Test
    void verify_nullMethodCode_throwsNullPointerException() {
        assertThatNullPointerException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify(null, "10000000", "1234567897"));
    }

    @Test
    void verify_nullBlz_throwsNullPointerException() {
        assertThatNullPointerException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("00", null, "1234567897"));
    }

    @Test
    void verify_nullAccountNumber_throwsNullPointerException() {
        assertThatNullPointerException()
            .isThrownBy(() -> GermanAccountCheckDigit.verify("00", "10000000", null));
    }

    @Test
    void privateConstructor_shouldThrowException() throws Exception {
        Constructor<GermanAccountCheckDigit> constructor = GermanAccountCheckDigit.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();

        constructor.setAccessible(true);
        assertThat(catchInvocationTargetCause(constructor)).isInstanceOf(UnsupportedOperationException.class);
    }

    private static Throwable catchInvocationTargetCause(Constructor<?> constructor) {
        try {
            constructor.newInstance();
            return null;
        } catch (InvocationTargetException ex) {
            return ex.getCause();
        } catch (ReflectiveOperationException ex) {
            return ex;
        }
    }

}
