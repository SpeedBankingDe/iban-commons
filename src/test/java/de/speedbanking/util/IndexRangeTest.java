package de.speedbanking.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit test class for {@link IndexRange}.
 */
class IndexRangeTest extends org.assertj.core.api.Assertions {

    @DisplayName("Should create IndexRange and check getters for valid indices")
    @Test
    void shouldCreateAndCheckGetters() {
        final int begin = 5;
        final int end = 10;

        IndexRange range = IndexRange.of(begin, end);

        assertThat(range)
            .isNotNull()
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(begin, end);
    }

    @DisplayName("Should throw IllegalArgumentException if begin index is negative")
    @ParameterizedTest
    @ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
    void shouldThrowIAEWhenBeginIsNegative(int negativeBegin) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IndexRange.of(negativeBegin, 1))
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
            .isThrownBy(() -> IndexRange.of(begin, end))
            .withMessageContaining("must be greater than or equal to 'begin'");
    }

    @DisplayName("Should allow zero-length range (begin == end)")
    @Test
    void shouldAllowZeroLengthRange() {
        IndexRange range = IndexRange.of(5, 5);
        assertThat(range)
            .isNotNull()
            .extracting(IndexRange::length, IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(0, 5, 5);
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
        IndexRange range = IndexRange.of(begin, end);
        assertThat(range.length()).isEqualTo(expectedLength);
    }

    @DisplayName("Should extract correct substring with applyTo(CharSequence)")
    @Test
    void shouldExtractCorrectSubstringFromCharSequence() {
        final String sequence = "ABCDEFGHIJ"; // 10 chars, indices 0 to 9
        IndexRange range = IndexRange.of(3, 7); // indices 3 (incl.) to 7 (excl.) -> DEFG

        CharSequence result = range.applyTo(sequence);

        assertThat(result).hasToString("DEFG");
    }

    @DisplayName("Should throw IndexOutOfBoundsException when sequence is too short for applyTo(CharSequence)")
    @Test
    void shouldThrowIOOBEWhenSequenceIsTooShortForCharSequence() {
        final String sequence = "ABC"; // Length 3, indices 0, 1, 2
        IndexRange range = IndexRange.of(1, 5); // needs index 5 (exclusive)

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> range.applyTo(sequence));
    }

    @DisplayName("Should extract correct string from char array with applyTo(char[])")
    @Test
    void shouldExtractCorrectStringFromCharArray() {
        final char[] sequence = "KLMNOPQRSTUVWXYZ".toCharArray();
        IndexRange range = IndexRange.of(4, 10); // indices 4 (incl.) to 10 (excl.) -> OPQRST

        String result = range.applyTo(sequence);

        assertThat(result).isEqualTo("OPQRST");
    }

    @DisplayName("Should throw IndexOutOfBoundsException when char array is too short for applyTo(char[])")
    @Test
    void shouldThrowIOOBEWhenCharArrayIsTooShort() {
        final char[] sequence = "K".toCharArray(); // Length 1
        IndexRange range = IndexRange.of(0, 5); // needs index 5 (exclusive)

        // The underlying String constructor throws an IndexOutOfBoundsException
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> range.applyTo(sequence));
    }

    @DisplayName("Should satisfy equals and hashCode contract, including null names")
    @Test
    void shouldSatisfyEqualsAndHashCodeContract() {
        IndexRange range1 = IndexRange.of(1, 5);
        IndexRange range2 = IndexRange.of(1, 5);
        IndexRange rangeDifferentBegin = IndexRange.of(0, 5);

        // Test with names
        assertThat(range1)
            .isEqualTo(range1)
            .isEqualTo(range2)
            .hasSameHashCodeAs(range2)
            .isNotEqualTo(rangeDifferentBegin);

        // Test against null and different object type
        assertThat(range1)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());
    }

    @DisplayName("Should return correct and descriptive toString format, handling null name")
    @Test
    void shouldReturnCorrectToString() {
         // Length 4. End-1 = 4
        assertThat(IndexRange.of(1, 5).toString()).isEqualTo("IndexRange[1-4 (4)]");
    }
}
