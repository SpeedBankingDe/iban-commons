package de.speedbanking.iban.util;

import static de.speedbanking.iban.util.IbanCharType.ALPHABETIC;
import static de.speedbanking.iban.util.IbanCharType.ALPHANUMERIC;
import static de.speedbanking.iban.util.IbanCharType.NUMERIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
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

    @DisplayName("parseSegments: repeated calls with the same pattern return the cached, identical list instance")
    @Test
    void parseSegments_calledTwiceWithSamePattern_returnsSameCachedInstance() {
        List<Segment> first = IbanPatternConverter.parseSegments("4!a16!c");
        List<Segment> second = IbanPatternConverter.parseSegments("4!a16!c");

        assertThat(second)
            .as("same pattern notation should yield the cached, identical list instance")
            .isSameAs(first);
    }

    @DisplayName("parseSegments: different pattern notations are cached independently")
    @Test
    void parseSegments_differentPatterns_areCachedIndependently() {
        List<Segment> a = IbanPatternConverter.parseSegments("4!a16!c");
        List<Segment> b = IbanPatternConverter.parseSegments("30!n");

        assertThat(a).isNotSameAs(b);
        assertThat(a).containsExactly(Segment.of(ALPHABETIC, 4), Segment.of(ALPHANUMERIC, 16));
        assertThat(b).containsExactly(Segment.of(NUMERIC, 30));
    }

    @DisplayName("parseSegments: returned list is unmodifiable")
    @Test
    void parseSegments_returnedList_isUnmodifiable() {
        List<Segment> segments = IbanPatternConverter.parseSegments("4!a16!c");

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> segments.add(Segment.of(NUMERIC, 1)));
    }

    @DisplayName("Should merge consecutive segments of the same type")
    @Test
    void aggregateSegments_givenConsecutiveSegmentsSameType_shouldMergeThem() {
        // 1. Case: Null or Empty input
        assertThat(IbanPatternConverter.aggregateSegments(null)).isEmpty();
        assertThat(IbanPatternConverter.aggregateSegments(emptyList())).isEmpty();

        // 2. Case: No aggregation needed (alternating types)
        List<Segment> listA = Arrays.asList(
            Segment.of(ALPHABETIC, 2),
            Segment.of(NUMERIC, 3),
            Segment.of(ALPHABETIC, 4)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listA)).isEqualTo(listA);

        // 3. Case: Full aggregation
        List<Segment> listB = Arrays.asList(
            Segment.of(ALPHABETIC, 2),
            Segment.of(ALPHABETIC, 3),
            Segment.of(ALPHABETIC, 4)
        );
        List<Segment> expectedB = singletonList(
            Segment.of(ALPHABETIC, 9)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listB)).isEqualTo(expectedB);

        // 4. Case: Partial aggregation (start, middle, end)
        List<Segment> listC = Arrays.asList(
            Segment.of(ALPHABETIC, 1),
            Segment.of(ALPHABETIC, 1),
            Segment.of(NUMERIC, 1),
            Segment.of(NUMERIC, 2),
            Segment.of(ALPHABETIC, 3),
            Segment.of(ALPHABETIC, 4)
        );
        List<Segment> expectedC = Arrays.asList(
            Segment.of(ALPHABETIC, 2),
            Segment.of(NUMERIC, 3),
            Segment.of(ALPHABETIC, 7)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listC)).isEqualTo(expectedC);
    }

    @DisplayName("buildRegex should correctly concatenate segment patterns")
    @Test
    void buildRegex_givenSegmentList_shouldConcatenatePatterns() {
        assertThat(IbanPatternConverter.buildRegex(null)).isNull();
        assertThat(IbanPatternConverter.buildRegex(emptyList())).isNull();

        List<Segment> segments = Arrays.asList(
            Segment.of(ALPHABETIC, 4),
            Segment.of(ALPHANUMERIC, 16),
            Segment.of(ALPHABETIC, 1)
        );

        String expected = "[A-Z]{4}[0-9A-Z]{16}[A-Z]";

        assertThat(IbanPatternConverter.buildRegex(segments)).isEqualTo(expected);
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

    @DisplayName("IbanPatternConverter.isAllNumeric should correctly identify all-numeric lists")
    @Test
    void isAllNumeric_shouldIdentifyNumericLists() {
        Segment num1 = Segment.of(NUMERIC, 4);
        Segment num2 = Segment.of(NUMERIC, 2);
        Segment alpha = Segment.of(ALPHABETIC, 4);

        assertThat(IbanPatternConverter.isAllNumeric(emptyList())).isTrue();
        assertThat(IbanPatternConverter.isAllNumeric(Arrays.asList(num1, num2))).isTrue();
        assertThat(IbanPatternConverter.isAllNumeric(Arrays.asList(num1, alpha))).isFalse();

        assertThatNullPointerException()
            .isThrownBy(() -> IbanPatternConverter.isAllNumeric(null))
            .withMessage("segments must not be null");
    }

    @DisplayName("IbanPatternConverter.allMatch should validate inputs and evaluate predicate")
    @Test
    void allMatch_shouldValidateInputsAndEvaluate() {
        Segment num1 = Segment.of(NUMERIC, 4);

        assertThatNullPointerException()
            .isThrownBy(() -> IbanPatternConverter.allMatch(null, Segment::isNumeric))
            .withMessage("segments must not be null");

        assertThatNullPointerException()
            .isThrownBy(() -> IbanPatternConverter.allMatch(singletonList(num1), null))
            .withMessage("predicate must not be null");

        assertThat(IbanPatternConverter.allMatch(emptyList(), Segment::isNumeric)).isTrue();
    }

    @DisplayName("IbanPatternConverter.calculateTotalLength should sum lengths or handle nulls")
    @Test
    void calculateTotalLength_shouldSumLengthsCorrectly() {
        assertThat(IbanPatternConverter.calculateTotalLength((Iterable<Segment>) null)).isEqualTo(0);
        assertThat(IbanPatternConverter.calculateTotalLength(emptyList())).isEqualTo(0);

        List<Segment> listWithNull = Arrays.asList(Segment.of(NUMERIC, 4), null, Segment.of(ALPHABETIC, 6));
        assertThat(IbanPatternConverter.calculateTotalLength(listWithNull)).isEqualTo(10);
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
