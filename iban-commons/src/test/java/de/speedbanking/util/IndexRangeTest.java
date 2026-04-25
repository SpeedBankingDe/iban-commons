package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.TypedArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit test class for {@link IndexRange}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IndexRangeTest {

    @DisplayName("Should create IndexRange and check getters for valid indices")
    @Test
    void of_shouldCreateRangeWithCorrectGetters_whenIndicesAreValid() {
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
    void of_shouldThrowException_whenBeginIsNegative(int negativeBegin) {
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
    void of_shouldThrowException_whenEndIsLessThanBegin(int begin, int end) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IndexRange.of(begin, end))
            .withMessageContaining("must be greater than or equal to 'begin'");
    }

    @DisplayName("Should allow zero-length range (begin == end)")
    @Test
    void of_shouldCreateZeroLengthRange_whenBeginEqualsEnd() {
        IndexRange range = IndexRange.of(5, 5);
        assertThat(range)
            .isNotNull()
            .extracting(IndexRange::length, IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(0, 5, 5);
    }

    @DisplayName("Should return correct length (end - begin)")
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        " 0,  5 |  5",
        "10, 10 |  0",
        " 5, 15 | 10",
        " 0,  0 |  0"
    })
    void length_shouldReturnCorrectLength_whenRangeIsGiven(@ConvertWith(IndexRangeConverter.class) IndexRange range, int expectedLength) {
        assertThat(range.length()).isEqualTo(expectedLength);
    }

    @DisplayName("Should extract correct substring with applyTo(CharSequence)")
    @Test
    void applyTo_shouldExtractCorrectSubstring_whenCharSequenceIsGiven() {
        final String sequence = "ABCDEFGHIJ"; // 10 chars, indices 0 to 9
        IndexRange range = IndexRange.of(3, 7); // indices 3 (incl.) to 7 (excl.) -> DEFG

        CharSequence result = range.applyTo(sequence);

        assertThat(result).hasToString("DEFG");
    }

    @DisplayName("Should throw IndexOutOfBoundsException when sequence is too short for applyTo(CharSequence)")
    @Test
    void applyTo_shouldThrowException_whenCharSequenceIsTooShort() {
        final String sequence = "ABC"; // Length 3, indices 0, 1, 2
        IndexRange range = IndexRange.of(1, 5); // needs index 5 (exclusive)

        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> range.applyTo(sequence));
    }

    @DisplayName("Should extract correct string from char array with applyTo(char[])")
    @Test
    void applyTo_shouldExtractCorrectSubstring_whenCharArrayIsGiven() {
        final char[] sequence = "KLMNOPQRSTUVWXYZ".toCharArray();
        IndexRange range = IndexRange.of(4, 10); // indices 4 (incl.) to 10 (excl.) -> OPQRST

        char[] result = range.applyTo(sequence);

        assertThat(new String(result)).isEqualTo("OPQRST");
    }

    @DisplayName("Should throw IndexOutOfBoundsException when char array is too short for applyTo(char[])")
    @Test
    void applyTo_shouldThrowException_whenCharArrayIsTooShort() {
        final char[] sequence = "K".toCharArray(); // length 1
        IndexRange range = IndexRange.of(0, 5); // needs index 5 (exclusive)

        // the underlying String constructor throws an IndexOutOfBoundsException
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
            .isThrownBy(() -> range.applyTo(sequence));
    }

    @DisplayName("Should satisfy compareTo contract")
    @ParameterizedTest(name = "Comparing {0} with {1} should return sign {2}")
    @CsvSource(delimiter = '|', value = {
        " 1, 5 | 1, 5 |  0", // equal
        " 1, 5 | 2, 5 | -1", // begin is less
        " 2, 5 | 1, 5 |  1", // begin is greater
        " 1, 5 | 1, 6 | -1", // begin equal, end is less
        " 1, 6 | 1, 5 |  1"  // begin equal, end is greater
    })
    void compareTo_shouldSatisfyContract_whenRangesAreCompared(@ConvertWith(IndexRangeConverter.class) IndexRange range1, @ConvertWith(IndexRangeConverter.class) IndexRange range2, int expectedSign) {
        int result = range1.compareTo(range2);

        if (expectedSign == 0) {
            assertThat(result).isZero();
        } else if (expectedSign < 0) {
            assertThat(result).isNegative();
        } else {
            assertThat(result).isPositive();
        }
    }

    @DisplayName("Should throw NullPointerException when compareTo is called with null")
    @Test
    void compareTo_shouldThrowException_whenArgumentIsNull() {
        IndexRange range = IndexRange.of(1, 5);

        assertThatNullPointerException()
            .isThrownBy(() -> range.compareTo(null))
            .withMessage("Comparison object must not be null");
    }

    @DisplayName("Should satisfy equals and hashCode contract, including null names")
    @Test
    @SuppressWarnings("SelfAssertion")
    void equalsAndHashCode_shouldSatisfyContract_whenRangesAreCompared() {
        IndexRange range1 = IndexRange.of(1, 5);
        IndexRange range2 = IndexRange.of(1, 5);
        IndexRange rangeDifferentBegin = IndexRange.of(0, 5);

        // test with names
        assertThat(range1)
            .isEqualTo(range1)
            .isEqualTo(range2)
            .hasSameHashCodeAs(range2)
            .isNotEqualTo(rangeDifferentBegin)
            .isNotEqualTo(new Object());
    }

    @DisplayName("Should return correct and descriptive toString format, handling null name")
    @Test
    void toString_shouldReturnCorrectFormat_whenRangeIsGiven() {
         // Length 4. End-1 = 4
        assertThat(IndexRange.of(1, 5)).hasToString("IndexRange[1-4 (4)]");
    }

    /**
     * Converter to transform a CSV string "begin, end" into an {@link IndexRange} instance.
     */
    static class IndexRangeConverter extends TypedArgumentConverter<String, IndexRange> {

        protected IndexRangeConverter() {
            super(String.class, IndexRange.class);
        }

        @Override
        @SuppressWarnings("StringSplitter")
        protected IndexRange convert(String source) {
            String[] parts = source.split("\\s*,\\s*");
            return IndexRange.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

}

