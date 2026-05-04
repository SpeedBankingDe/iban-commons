package de.speedbanking.iban.util;

import static de.speedbanking.iban.util.IbanCharType.ALPHABETIC;
import static de.speedbanking.iban.util.IbanCharType.ALPHANUMERIC;
import static de.speedbanking.iban.util.IbanCharType.NUMERIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

/**
 * JUnit test class for {@link IbanPatternConverter}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IbanPatternConverterTest {

    /**
     * Helper method to create Segment instances via reflection for testing
     */
    private Segment createSegment(IbanCharType type, int length) throws Exception {
        Constructor<Segment> constructor = Segment.class.getDeclaredConstructor(IbanCharType.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(type, length);
    }

    /**
     * Tests various valid IBAN pattern notations against their expected regex counterparts.
     */
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

    /**
     * Tests that an {@link IllegalArgumentException} is thrown when an unsupported
     * character type (not a, n, or c) is found in the pattern notation.
     */
    @DisplayName("Unsupported character type should throw IllegalArgumentException")
    @ParameterizedTest(name = "Type: {0}")
    @ValueSource(strings = {"x", "A", "!", "1", "@"})
    void convertToRegex_givenUnsupportedCharacterType_shouldThrowException(String invalidType) {
        String input = "4!" + invalidType;
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IbanPatternConverter.convertToRegex(input))
            .withMessage("Unknown character type '%s' at index 0, valid types are: [a, n, c, e]", invalidType);
    }

    /**
     * Tests that an {@link IllegalArgumentException} is thrown when the input string
     * contains characters that cannot be parsed as a valid IBAN segment.
     */
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

    /**
     * Tests the public utility method {@link IbanPatternConverter#isValid(String)}.
     */
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
        "4!x      | false"  // invalid type (covered by the fallback catch)
    })
    void isValid_givenVariousInputs_shouldReturnExpectedBoolean(String input, boolean expected) {
        assertThat(IbanPatternConverter.isValid(input))
            .as("isValid failed for input: '%s'", input)
            .isEqualTo(expected);
    }

    /**
     * Tests the aggregation logic with various segment sequences,
     * ensuring consecutive segments of the same type are merged.
     */
    @DisplayName("Should merge consecutive segments of the same type")
    @Test
    void aggregateSegments_givenConsecutiveSegmentsSameType_shouldMergeThem() throws Exception {
        // 1. Case: Null or Empty input
        assertThat(IbanPatternConverter.aggregateSegments(null)).isEmpty();
        assertThat(IbanPatternConverter.aggregateSegments(emptyList())).isEmpty();

        // 2. Case: No aggregation needed (alternating types)
        List<Segment> listA = Arrays.asList(
            createSegment(ALPHABETIC, 2),
            createSegment(NUMERIC, 3),
            createSegment(ALPHABETIC, 4)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listA)).isEqualTo(listA);

        // 3. Case: Full aggregation
        List<Segment> listB = Arrays.asList(
            createSegment(ALPHABETIC, 2),
            createSegment(ALPHABETIC, 3),
            createSegment(ALPHABETIC, 4)
        );
        List<Segment> expectedB = singletonList(
            createSegment(ALPHABETIC, 9)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listB)).isEqualTo(expectedB);

        // 4. Case: Partial aggregation (start, middle, end)
        List<Segment> listC = Arrays.asList(
            createSegment(ALPHABETIC, 1),
            createSegment(ALPHABETIC, 1),
            createSegment(NUMERIC, 1),
            createSegment(NUMERIC, 2),
            createSegment(ALPHABETIC, 3),
            createSegment(ALPHABETIC, 4)
        );
        List<Segment> expectedC = Arrays.asList(
            createSegment(ALPHABETIC, 2),
            createSegment(NUMERIC, 3),
            createSegment(ALPHABETIC, 7)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listC)).isEqualTo(expectedC);
    }

    /**
     * Tests the final step of Regex construction, independent of parsing/aggregation.
     */
    @DisplayName("buildRegex should correctly concatenate segment patterns")
    @Test
    void buildRegex_givenSegmentList_shouldConcatenatePatterns() throws Exception {
        assertThat(IbanPatternConverter.buildRegex(null)).isNull();
        assertThat(IbanPatternConverter.buildRegex(emptyList())).isNull();

        List<Segment> segments = Arrays.asList(
            createSegment(ALPHABETIC, 4),
            createSegment(ALPHANUMERIC, 16),
            createSegment(ALPHABETIC, 1)
        );

        String expected = "[A-Z]{4}[0-9A-Z]{16}[A-Z]";

        assertThat(IbanPatternConverter.buildRegex(segments)).isEqualTo(expected);

        assertThat(IbanPatternConverter.buildRegex(emptyList())).isNull();
    }

    /**
     * Tests the {@link Segment#toString()} method.
     */
    @DisplayName("Segment toString() should return expected format")
    @Test
    void toString_givenSegment_shouldReturnExpectedFormat() throws Exception {
        Segment segment = createSegment(ALPHABETIC, 10);
        assertThat(segment).hasToString("Segment[charType=ALPHABETIC, length=10]");
    }

    @DisplayName("Segment addLength should handle non-positive addition")
    @Test
    void addLength_givenNonPositiveValue_shouldReturnSameInstance() throws Exception {
        Segment segment5 = createSegment(ALPHABETIC, 5);

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
    void equalsAndHashCode_givenVariousSegments_shouldBeConsistent() throws Exception {
        Segment segmentA = createSegment(ALPHABETIC, 10);
        Segment segmentB = createSegment(ALPHABETIC, 10);
        Segment segmentC = createSegment(ALPHABETIC, 5);
        Segment segmentD = createSegment(NUMERIC, 10);

        assertThat(segmentA)
            // check with null and different class
            .isNotNull()
            .isNotEqualTo((Object) "Not a Segment")

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
