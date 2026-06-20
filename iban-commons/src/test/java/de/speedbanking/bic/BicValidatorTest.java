package de.speedbanking.bic;

import static de.speedbanking.bic.BicValidationError.EMPTY;
import static de.speedbanking.bic.BicValidationError.INCORRECT_LENGTH;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.CharBuffer;

/**
 * JUnit test class for {@link BicValidator}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BicValidatorTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructor_shouldThrowException_whenInstantiated() {
        TestUtil.assertConstructorIsPrivate(BicValidator.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ROTACHZZ",
        "PALSPS22",
        "ROTACHZZXXX",
        "PALSPS22XXX",
        "ROTACHZZ123",
        "PALSPS22RAM"
    })
    @DisplayName("isValid: should return true for valid BICs via String and CharSequence paths")
    @SuppressWarnings("UnnecessaryStringBuilder")
    void isValid_shouldReturnTrue_forValidBic(String validBic) {
        assertThat(BicValidator.isValid(validBic)).isTrue();
        assertThat(BicValidator.isValid((CharSequence) validBic)).isTrue();
        assertThat(BicValidator.isValid(new StringBuilder(validBic))).isTrue();
        assertThat(BicValidator.isValid(CharBuffer.wrap(validBic))).isTrue();
    }

    @DisplayName("validate: should return false and appropriate error for null or empty input")
    @ParameterizedTest
    @NullAndEmptySource
    void validate_shouldReturnError_whenInputIsNullOrEmpty(String input) {
        BicValidationResult result = BicValidator.validate(input);
        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isIn(EMPTY, INCORRECT_LENGTH);
    }

    @DisplayName("validate: should return INCORRECT_LENGTH for lengths other than 8 or 11")
    @ParameterizedTest
    @ValueSource(strings = {
        "YAMAJP",      // too short (6 chars)
        "YAMAJPJTXX",  // invalid 10-char length
        "YAMAJPJTXXXX" // too long (12 chars)
    })
    void validate_shouldReturnIncorrectLength_whenLengthIsInvalid(String invalidLengthBic) {
        BicValidationResult result = BicValidator.validate(invalidLengthBic);
        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INCORRECT_LENGTH);
    }

    @DisplayName("validate: should return specific structural error types based on positional constraints")
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "banaESMM | INVALID_BANK_CODE",
        "AIbKie2D | INVALID_BANK_CODE",
        "DNBNO123 | INVALID_COUNTRY",
        "DNBNXX23 | INVALID_COUNTRY",
        "AIBKIE2! | ILLEGAL_CHARACTERS"
    })
    void validate_shouldReturnSpecificError_whenStructureIsInvalid(String invalidBic, String expectedErrorStr) {
        BicValidationError expectedError = BicValidationError.valueOf(expectedErrorStr);
        BicValidationResult result = BicValidator.validate(invalidBic);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(expectedError);
    }

    @DisplayName("validate: should return valid result without error for flawless BIC")
    @Test
    void validate_shouldReturnValidResult_whenBicIsFlawless() {
        String bic = "NBPLPLPWXXX";
        BicValidationResult result = BicValidator.validate(bic);

        assertThat(result.isValid()).isTrue();
        assertThat(result.error).isNull();
        assertThat(result.bic).hasToString(bic);
    }

    @DisplayName("copyToBuffer: should handle generic CharSequence fallback correctly")
    @Test
    void copyToBuffer_shouldFallbackToSequentialLoop_whenTypeIsGenericCharSequence() {
        String sourceStr = "DEUTDED1";
        CharSequence genericSeq = CharBuffer.wrap(sourceStr);
        char[] target = new char[Bic.BIC11_LENGTH];

        char[] result = BicValidator.copyToBuffer(genericSeq, sourceStr.length(), target);

        assertThat(new String(result, 0, sourceStr.length())).isEqualTo(sourceStr);
    }

    @DisplayName("validate: should return INCORRECT_LENGTH for boundary violations")
    @ParameterizedTest
    @ValueSource(strings = {
        "BCCIL",       // too short
        "BCCILULLXXXX" // too long
    })
    void validate_shouldReturnIncorrectLength_whenLengthIsOutsideBounds(String invalidLengthBic) {
        BicValidationResult result = BicValidator.validate(invalidLengthBic);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INCORRECT_LENGTH);
    }

    @DisplayName("validate: should catch invalid lengths that are neither 8 nor 11")
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "WDABDE2    | 7",
        "LEHMUS33X  | 9",
        "BARYGB2LXX | 10"
    })
    void validate_shouldRejectSpecificInvalidLengths(String bic, int length) {
        BicValidationResult result = BicValidator.validate(bic);

        assertThat(result.isValid()).isFalse();
        assertThat(result.error).isEqualTo(INCORRECT_LENGTH);
    }

    @DisplayName("copyToBuffer: should cover precise array bounds for String source")
    @Test
    void copyToBuffer_shouldCopyCorrectly_whenSourceIsString() {
        String sourceStr = "BARYGB2L";
        char[] target = new char[Bic.BIC8_LENGTH];

        char[] result = BicValidator.copyToBuffer(sourceStr, sourceStr.length(), target);

        assertThat(new String(result)).isEqualTo(sourceStr);
    }

    @DisplayName("copyToBuffer: should use fast path when source is a String")
    @Test
    void copyToBuffer_shouldUseFastPath_whenSourceIsString() {
        String source = "WDABDE21XXX";
        char[] target = new char[11];

        char[] result = BicValidator.copyToBuffer(source, source.length(), target);

        assertThat(result).containsExactly('W', 'D', 'A', 'B', 'D', 'E', '2', '1', 'X', 'X', 'X');
    }

    @DisplayName("copyToBuffer: should use sequential loop when source is a generic CharSequence")
    @Test
    void copyToBuffer_shouldUseSequentialLoop_whenSourceIsGenericCharSequence() {
        // using a custom CharSequence implementation to force execution of the fallback loop
        CharSequence customSeq = new CharSequence() {
            private final String data = "ABCDEFGH";

            @Override
            public int length() {
                return data.length();
            }

            @Override
            public char charAt(int index) {
                return data.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return data.subSequence(start, end);
            }

            @Override
            public String toString() {
                return data;
            }
        };
        char[] target = new char[11];

        char[] result = BicValidator.copyToBuffer(customSeq, customSeq.length(), target);

        // verify that the first 8 characters are copied correctly into the buffer
        assertThat(new String(result, 0, customSeq.length())).isEqualTo("ABCDEFGH");
    }

}
