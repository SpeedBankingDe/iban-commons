package de.speedbanking.iban.util;

import static de.speedbanking.iban.util.IbanCharType.ALPHABETIC;
import static de.speedbanking.iban.util.IbanCharType.ALPHANUMERIC;
import static de.speedbanking.iban.util.IbanCharType.NUMERIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.iban.util.IbanPatternConverter.Segments;
import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * JUnit test class for {@link IbanPatternConverter}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IbanPatternConverterTest {

    @DisplayName("Valid patterns should convert to correct Regex")
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = ';', nullValues = "(null)", value = {
        "4!a16!c      ; [A-Z]{4}[0-9A-Z]{16}",
        "30!n         ; [0-9]{30}",
        "2!a3!n4!c    ; [A-Z]{2}[0-9]{3}[0-9A-Z]{4}",
        "1!a1!n1!c    ; [A-Z][0-9][0-9A-Z]",
        "4!a2!a3!n    ; [A-Z]{6}[0-9]{3}",           // aggregation (4a + 2a = 6a) then segment change (3n)
        "4!a3!n2!n    ; [A-Z]{4}[0-9]{5}",           // segment change, then aggregation (3n + 2n = 5n)
        "4!a10!a      ; [A-Z]{14}",                  // only two segments of the same type
        "2!c1!a2!a3!n ; [0-9A-Z]{2}[A-Z]{3}[0-9]{3}" // all three types and aggregation
    })
    void convertToRegex_givenValidPattern_shouldReturnCorrectRegex(String input, String expected) {
        String patternInput = (input == null) ? "" : input;
        String actual = IbanPatternConverter.convertToRegex(patternInput);

        assertThat(actual)
            .as("Conversion failed for input: %s", patternInput)
            .isEqualTo(expected);
    }

    @DisplayName("Unsupported character type should throw IllegalArgumentException")
    @ParameterizedTest(name = "Type: {0}")
    @ValueSource(strings = {"x", "A", "!", "1", "@"})
    void convertToRegex_givenUnsupportedCharacterType_shouldThrowException(String invalidType) {
        String input = "4!" + invalidType;
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IbanPatternConverter.convertToRegex(input))
            .withMessage("Unknown character type '%s' at index 0, valid types are: [a, n, c, e]", invalidType);
    }

    @DisplayName("Invalid pattern formats should throw IllegalArgumentException")
    @ParameterizedTest(name = "Format: {0}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "4!a16!cX               | Pattern contains invalid trailing characters starting at index 7: 'X'",
        "4!a16!c4a              | Pattern contains invalid trailing characters starting at index 7: '4a'",
        "X4!a16!c               | Pattern contains invalid characters at index 0: 'X'",
        "4a16!c                 | Pattern contains invalid characters at index 0: '4a'",
        "!a16!c                 | Pattern contains invalid characters at index 0: '!a'",
        "03!n                   | Pattern contains invalid characters at index 0: '0'",
        "-5!c                   | Pattern contains invalid characters at index 0: '-'",
        "99999999999999999999!a | Length value too large at index 0: 99999999999999999999",
        "(null)                 | Pattern notation must not be null",
        "''                     | Pattern notation must not be empty",
        "'  '                   | Pattern contains illegal leading/trailing whitespace"
    })
    void convertToRegex_givenInvalidFormat_shouldThrowException(String input, String expectedErrorSegment) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IbanPatternConverter.convertToRegex(input))
            .withMessage(expectedErrorSegment);
    }

    @DisplayName("isValid should return true for valid and false for invalid patterns")
    @ParameterizedTest(name = "Input: {0}, Expected: {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "4!a16!c  | true",  // valid
        "20!c     | true",  // valid
        "X4!a16!c | false", // invalid char
        "4!a16!cX | false", // invalid trailing
        "(null)   | false", // null
        "''       | false", // empty
        "'  '     | false", // whitespace
        "4!x      | false"  // invalid type
    })
    void isValid_givenVariousInputs_shouldReturnExpectedBoolean(String input, boolean expected) {
        assertThat(IbanPatternConverter.isValid(input))
            .as("isValid failed for input: '%s'", input)
            .isEqualTo(expected);
    }

    @DisplayName("parseSegments: repeated calls with the same pattern return the cached, identical instance")
    @Test
    void parseSegments_calledTwiceWithSamePattern_returnsSameCachedInstance() {
        Segments first = IbanPatternConverter.parseSegments("4!a16!c");
        Segments second = IbanPatternConverter.parseSegments("4!a16!c");

        assertThat(second)
            .as("same pattern notation should yield the cached, identical instance")
            .isSameAs(first);
    }

    @DisplayName("parseSegments: different pattern notations are cached independently")
    @Test
    void parseSegments_differentPatterns_areCachedIndependently() {
        Segments a = IbanPatternConverter.parseSegments("4!a16!c");
        Segments b = IbanPatternConverter.parseSegments("30!n");

        assertThat(a)
            .isNotSameAs(b)
            .containsExactly(Segment.of(ALPHABETIC, 4), Segment.of(ALPHANUMERIC, 16));
        assertThat(b).containsExactly(Segment.of(NUMERIC, 30));
    }

    @Test
    void asList_parsedSegments_isUnmodifiable() {
        List<Segment> segments = IbanPatternConverter.parseSegments("4!a16!c").asList();
        Segment extraSegment = Segment.of(NUMERIC, 1);

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> segments.add(extraSegment));
    }

    @Test
    void aggregate_noSegments_returnsSameInstance() {
        Segments empty = Segments.of();

        assertThat(empty.aggregate()).isSameAs(empty);
    }

    @DisplayName("aggregate merges consecutive segments of the same type")
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "2!a3!n4!a          | 2!a3!n4!a",
        "2!a3!a4!a          | 9!a",
        "1!a1!a1!n2!n3!a4!a | 2!a3!n7!a",
    })
    void aggregate_consecutiveSameType_mergesThem(String pattern, String expectedPattern) {
        assertThat(IbanPatternConverter.parseSegments(pattern).aggregate())
            .isEqualTo(IbanPatternConverter.parseSegments(expectedPattern));
    }

    @DisplayName("Segments.ranges returns begin and end index pairs of all segments of one type")
    @ParameterizedTest(name = "[{index}] {0}, {1}, offset {2} -> {3}")
    @CsvSource(delimiter = '|', value = {
        "4!a2!n3!a | ALPHABETIC   | 0 | 0,4,6,9",
        "4!a2!n3!a | NUMERIC      | 4 | 8,10",
        "4!a2!n3!a | ALPHANUMERIC | 4 | ''",
        "16!c      | ALPHANUMERIC | 4 | 4,20",
    })
    void ranges_charTypeAndOffset_returnsIndexPairs(String pattern, IbanCharType charType, int offset, String expected) {
        int[] expectedRanges = expected.isEmpty()
            ? new int[0]
            : Arrays.stream(expected.split(",")).mapToInt(Integer::parseInt).toArray();

        assertThat(IbanPatternConverter.parseSegments(pattern).ranges(charType, offset)).containsExactly(expectedRanges);
    }

    @ParameterizedTest(name = "[{index}] {0} contains {1}: {2}")
    @CsvSource(delimiter = '|', value = {
        "4!a6!n    | NUMERIC      | true",
        "4!a6!n    | ALPHANUMERIC | false",
        "8!n2!e8!n | SPACE        | true",
    })
    void containsType_charType_returnsWhetherAnySegmentHasIt(String pattern, IbanCharType charType, boolean expected) {
        assertThat(IbanPatternConverter.parseSegments(pattern).containsType(charType)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "4!n4!n12!c | 20",
        "30!n       | 30",
    })
    void totalLength_pattern_returnsSumOfSegmentLengths(String pattern, int expected) {
        assertThat(IbanPatternConverter.parseSegments(pattern).totalLength()).isEqualTo(expected);
        assertThat(IbanPatternConverter.calculateTotalLength(pattern)).isEqualTo(expected);
    }

    @Test
    void totalLength_noSegments_returnsZero() {
        assertThat(Segments.of().totalLength()).isZero();
    }

    @Test
    void allMatch_predicate_returnsWhetherAllSegmentsMatch() {
        Segments segments = Segments.of(Segment.of(NUMERIC, 4), Segment.of(NUMERIC, 2));

        assertThat(segments.allMatch(Segment::isNumeric)).isTrue();
        assertThat(Segments.of(Segment.of(NUMERIC, 4), Segment.of(ALPHABETIC, 4)).allMatch(Segment::isNumeric)).isFalse();
        assertThat(Segments.of().allMatch(Segment::isNumeric)).isTrue();
        assertThatNullPointerException()
            .isThrownBy(() -> segments.allMatch(null))
            .withMessage("predicate must not be null");
    }

    @Test
    void of_nullSegment_throwsNullPointerException() {
        Segment segment = Segment.of(NUMERIC, 4);

        assertThatNullPointerException()
            .isThrownBy(() -> Segments.of(segment, null))
            .withMessage("segment must not be null");
    }

    @Test
    void iterator_parsedSegments_doesNotSupportRemoval() {
        Iterator<Segment> iterator = IbanPatternConverter.parseSegments("4!a16!c").iterator();
        iterator.next();

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(iterator::remove);
    }

    @Test
    void segments_equalsHashCodeAndToString_dependOnSegments() {
        Segments segments = Segments.of(Segment.of(ALPHABETIC, 4), Segment.of(NUMERIC, 6));

        assertThat(segments)
            .isEqualTo(IbanPatternConverter.parseSegments("4!a6!n"))
            .hasSameHashCodeAs(IbanPatternConverter.parseSegments("4!a6!n"))
            .isNotEqualTo(IbanPatternConverter.parseSegments("4!a6!c"))
            .hasToString("Segments[Segment[charType=ALPHABETIC, length=4], Segment[charType=NUMERIC, length=6]]");
        assertThat(segments.size()).isEqualTo(2);
        assertThat(segments.get(1)).isEqualTo(Segment.of(NUMERIC, 6));
    }

    @Test
    void toRegex_segments_concatenatesCharacterClasses() {
        Segments segments = Segments.of(
            Segment.of(ALPHABETIC, 4),
            Segment.of(ALPHANUMERIC, 16),
            Segment.of(ALPHABETIC, 1)
        );

        assertThat(segments.toRegex()).isEqualTo("[A-Z]{4}[0-9A-Z]{16}[A-Z]");
        assertThat(Segments.of().toRegex()).isEmpty();
    }

    @DisplayName("Segment creation should validate parameters")
    @Test
    void segment_construction_shouldValidateInputs() {
        assertThatNullPointerException()
            .isThrownBy(() -> new Segment(null, 5))
            .withMessage("charType must not be null");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Segment(NUMERIC, 0))
            .withMessage("length must be > 0");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> Segment.of(NUMERIC, -1))
            .withMessage("length must be > 0");
    }

    @DisplayName("Segment boolean type inspectors should return correct values")
    @Test
    void segment_typeInspectors_shouldReturnCorrectBooleans() {
        Segment num = Segment.of(NUMERIC, 4);
        Segment alpha = Segment.of(ALPHABETIC, 4);
        Segment alphaNum = Segment.of(ALPHANUMERIC, 4);

        assertThat(num.isNumeric()).isTrue();
        assertThat(num.isAlphabetic()).isFalse();
        assertThat(num.isAlphanumeric()).isFalse();
        assertThat(num.isNumericOrAlphanumeric()).isTrue();

        assertThat(alpha.isNumeric()).isFalse();
        assertThat(alpha.isAlphabetic()).isTrue();
        assertThat(alpha.isAlphanumeric()).isFalse();
        assertThat(alpha.isNumericOrAlphanumeric()).isFalse();

        assertThat(alphaNum.isNumeric()).isFalse();
        assertThat(alphaNum.isAlphabetic()).isFalse();
        assertThat(alphaNum.isAlphanumeric()).isTrue();
        assertThat(alphaNum.isNumericOrAlphanumeric()).isTrue();
    }

    @DisplayName("Segment toPatternNotation should return valid IBAN pattern string")
    @Test
    void toPatternNotation_shouldReturnFormattedPattern() {
        assertThat(Segment.of(ALPHABETIC, 4).toPatternNotation()).isEqualTo("4!a");
        assertThat(Segment.of(NUMERIC, 10).toPatternNotation()).isEqualTo("10!n");
        assertThat(Segment.of(ALPHANUMERIC, 16).toPatternNotation()).isEqualTo("16!c");
    }

    @DisplayName("Segment toString() should return expected format")
    @Test
    void toString_givenSegment_shouldReturnExpectedFormat() {
        Segment segment = Segment.of(ALPHABETIC, 10);
        assertThat(segment).hasToString("Segment[charType=ALPHABETIC, length=10]");
    }

    @DisplayName("Segment addLength should handle non-positive addition")
    @Test
    void addLength_givenNonPositiveValue_shouldReturnSameInstance() {
        Segment segment5 = Segment.of(ALPHABETIC, 5);

        Segment resultSegment0 = segment5.addLength(0);
        assertThat(resultSegment0).isSameAs(segment5);

        Segment resultSegmentNegative = segment5.addLength(-1);
        assertThat(resultSegmentNegative).isSameAs(segment5);

        Segment segmentAdd3 = segment5.addLength(3);
        assertThat(segmentAdd3).isNotSameAs(segment5);
        assertThat(segmentAdd3.getLength()).isEqualTo(8);
    }

    @DisplayName("Segment equals and hashCode must be consistent")
    @Test
    @SuppressWarnings("SelfAssertion")
    void equalsAndHashCode_givenVariousSegments_shouldBeConsistent() {
        Segment segmentA = Segment.of(ALPHABETIC, 10);
        Segment segmentB = Segment.of(ALPHABETIC, 10);
        Segment segmentC = Segment.of(ALPHABETIC, 5);
        Segment segmentD = Segment.of(NUMERIC, 10);

        assertThat(segmentA)
            // identity check
            .isEqualTo(segmentA)

            // check with null and different class
            .isNotNull()
            .isNotEqualTo("Not a Segment")

            // check equality (A vs B)
            .isEqualTo(segmentB)
            .hasSameHashCodeAs(segmentB)

            // check inequality (A vs C - different length)
            .isNotEqualTo(segmentC)

            // check inequality (A vs D - different char type)
            .isNotEqualTo(segmentD);
    }

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void constructor_shouldBePrivate() {
        TestUtil.assertConstructorIsPrivate(IbanPatternConverter.class);
    }

}
