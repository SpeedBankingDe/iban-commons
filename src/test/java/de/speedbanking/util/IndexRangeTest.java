package de.speedbanking.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit test class for {@link IndexRange}.
 */
class IndexRangeTest extends org.assertj.core.api.Assertions {

    private static final String DEFAULT_NAME = "TestRange";

    @DisplayName("Should create IndexRange and check getters for valid indices")
    @Test
    void shouldCreateAndCheckGetters() {
        final int begin = 5;
        final int end = 10;

        IndexRange range = IndexRange.of(DEFAULT_NAME, begin, end);

        assertThat(range).isNotNull();
        assertThat(range.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(range.getBegin()).isEqualTo(begin);
        assertThat(range.getEnd()).isEqualTo(end);
    }

    @DisplayName("Should handle various names correctly including empty string")
    @ParameterizedTest
    @ValueSource(strings = {"Valid Name", "", "a"})
    void shouldHandleVariousNames(String name) {
        IndexRange range = IndexRange.of(name, 0, 1);
        assertThat(range.getName()).isEqualTo(name);
    }

    @DisplayName("Should throw NullPointerException if name is null")
    @ParameterizedTest
    @NullSource
    void shouldThrowNPEWhenNameIsNull(String nullName) {
        assertThatNullPointerException()
            .isThrownBy(() -> IndexRange.of(nullName, 0, 1))
            .withMessage("Name must not be null");
    }

    @DisplayName("Should throw IllegalArgumentException if begin index is negative")
    @ParameterizedTest
    @ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
    void shouldThrowIAEWhenBeginIsNegative(int negativeBegin) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IndexRange.of(DEFAULT_NAME, negativeBegin, 1))
            .withMessageContaining("must be non-negative");
    }

    @DisplayName("Should throw IllegalArgumentException if end index is less than begin index")
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        " 5 | 4", // end < begin
        "10 | 0",
        " 1 | 0"
    })
    void shouldThrowIAEWhenEndIsLessThanBegin(int begin, int end) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IndexRange.of(DEFAULT_NAME, begin, end))
            .withMessageContaining("must be greater than or equal to 'begin'");
    }

    @DisplayName("Should allow zero-length range (begin == end)")
    @Test
    void shouldAllowZeroLengthRange() {
        IndexRange range = IndexRange.of(DEFAULT_NAME, 5, 5);
        assertThat(range.length()).isZero();
        assertThat(range.getBegin()).isEqualTo(5);
        assertThat(range.getEnd()).isEqualTo(5);
    }

    @DisplayName("Should return correct length (end - begin)")
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        " 0 |  5 |  5",
        "10 | 10 |  0",
        " 5 | 15 | 10",
        " 0 |  0 |  0"
    })
    void shouldReturnCorrectLength(int begin, int end, int expectedLength) {
        IndexRange range = IndexRange.of(DEFAULT_NAME, begin, end);
        assertThat(range.length()).isEqualTo(expectedLength);
    }

    @DisplayName("Should extract correct substring with applyTo(CharSequence)")
    @Test
    void shouldExtractCorrectSubstringFromCharSequence() {
        final String sequence = "ABCDEFGHIJ"; // 10 chars, indices 0 to 9
        IndexRange range = IndexRange.of("Segment", 3, 7); // indices 3 (incl.) to 7 (excl.) -> DEFG

        CharSequence result = range.applyTo(sequence);

        assertThat(result).hasToString("DEFG");
    }

    @DisplayName("Should throw IndexOutOfBoundsException when sequence is too short for applyTo(CharSequence)")
    @Test
    void shouldThrowIOOBEWhenSequenceIsTooShortForCharSequence() {
        final String sequence = "ABC"; // Length 3, indices 0, 1, 2
        IndexRange range = IndexRange.of("LongRange", 1, 5); // needs index 5 (exclusive)

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> range.applyTo(sequence));
    }

    @DisplayName("Should extract correct string from char array with applyTo(char[])")
    @Test
    void shouldExtractCorrectStringFromCharArray() {
        final char[] sequence = "KLMNOPQRSTUVWXYZ".toCharArray();
        IndexRange range = IndexRange.of("MidSection", 4, 10); // indices 4 (incl.) to 10 (excl.) -> OPQRST

        String result = range.applyTo(sequence);

        assertThat(result).isEqualTo("OPQRST");
    }

    @DisplayName("Should throw IndexOutOfBoundsException when char array is too short for applyTo(char[])")
    @Test
    void shouldThrowIOOBEWhenCharArrayIsTooShort() {
        final char[] sequence = "K".toCharArray(); // Length 1
        IndexRange range = IndexRange.of("LongRange", 0, 5); // needs index 5 (exclusive)

        // The underlying String constructor throws an IndexOutOfBoundsException
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> range.applyTo(sequence));
    }

    @DisplayName("Should satisfy equals and hashCode contract")
    @Test
    void shouldSatisfyEqualsAndHashCodeContract() {
        IndexRange range1 = IndexRange.of(DEFAULT_NAME, 1, 5);
        IndexRange range2 = IndexRange.of(DEFAULT_NAME, 1, 5);
        IndexRange rangeDifferentName = IndexRange.of("OtherName", 1, 5);
        IndexRange rangeDifferentBegin = IndexRange.of(DEFAULT_NAME, 0, 5);

        assertThat(range1)
            .isEqualTo(range1)
            .isEqualTo(range2)
            .hasSameHashCodeAs(range2)
            .isNotEqualTo(rangeDifferentName)
            .isNotEqualTo(rangeDifferentBegin)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());
    }

    @DisplayName("Should return correct and descriptive toString format")
    @Test
    void shouldReturnCorrectToString() {
        IndexRange range = IndexRange.of("Bank Code", 4, 8); // Length 4. End-1 = 7

        String result = range.toString();

        assertThat(result).isEqualTo("IndexRange[Bank Code: 4-7 (4)]");
    }
}
