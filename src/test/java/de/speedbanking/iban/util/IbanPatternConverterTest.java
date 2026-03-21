package de.speedbanking.iban.util;

import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.test.TestUtil;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JUnit test class for {@link IbanPatternConverter}.
 */
class IbanPatternConverterTest extends Assertions {

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
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "4!a16!c       |  [A-Z]{4}[A-Z0-9]{16}",
        "30!n          |  \\d{30}",
        "2!a3!n4!c     |  [A-Z]{2}\\d{3}[A-Z0-9]{4}",
        "1!a1!n1!c     |  [A-Z]\\d[A-Z0-9]",
        "4!a2!a3!n     |  [A-Z]{6}\\d{3}",           // aggregation (4a + 2a = 6a) then segment change (3n)
        "4!a3!n2!n     |  [A-Z]{4}\\d{5}",           // segment change, then aggregation (3n + 2n = 5n). Ensures line 160 is covered.
        "4!a10!a       |  [A-Z]{14}",                // only two segments of the same type
        "2!c1!a2!a3!n  |  [A-Z0-9]{2}[A-Z]{3}\\d{3}" // all three types and aggregation
    })
    void testConvertToRegexShouldSucceed(String input, String expected) {
        String patternInput = (input == null) ? "" : input;
        String actual = IbanPatternConverter.convertToRegex(patternInput);

        assertThat(actual)
            .as("Conversion failed for input: %s", patternInput)
            .isEqualTo(expected);
    }

    /**
     * Tests that an {@link IllegalArgumentException} is thrown when an unsupported
     * character type (not a, n, or c) is found in the pattern notation.
     * The original test logic is kept but the assertion is switched to AssertJ.
     */
    @DisplayName("Unsupported character type should throw IllegalArgumentException")
    @ParameterizedTest(name = "Type: {0}")
    @ValueSource(strings = {"x", "A", "!", "1", "@"})
    void testUnknownCharacterTypeThrowsException(String invalidType) {
        String input = "4!" + invalidType;
        assertThatIllegalArgumentException()
            .isThrownBy(() -> IbanPatternConverter.convertToRegex(input))
            .withMessage("Unknown character type '"
                         + invalidType
                         + "' at index 0, valid types are: [a, n, c, e]");
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
    void testInvalidFormatThrowsException(String input, String expectedErrorSegment) {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> IbanPatternConverter.convertToRegex(input))
            .withMessage(expectedErrorSegment);
    }

    /**
     * Tests the public utility method {@link IbanPatternConverter#isValid(String)}.
     * This increases coverage for the public API entry point.
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
    void testIsValid(String input, boolean expected) {
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
    void testAggregateSegments() throws Exception {
        // 1. Case: Null or Empty input
        assertThat(IbanPatternConverter.aggregateSegments(null)).isEmpty();
        assertThat(IbanPatternConverter.aggregateSegments(Collections.emptyList())).isEmpty();

        // 2. Case: No aggregation needed (alternating types)
        List<Segment> listA = Arrays.asList(
            createSegment(IbanCharType.ALPHABETIC, 2),
            createSegment(IbanCharType.NUMERIC, 3),
            createSegment(IbanCharType.ALPHABETIC, 4)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listA)).isEqualTo(listA);

        // 3. Case: Full aggregation
        List<Segment> listB = Arrays.asList(
            createSegment(IbanCharType.ALPHABETIC, 2),
            createSegment(IbanCharType.ALPHABETIC, 3),
            createSegment(IbanCharType.ALPHABETIC, 4)
        );
        List<Segment> expectedB = Collections.singletonList(
            createSegment(IbanCharType.ALPHABETIC, 9)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listB)).isEqualTo(expectedB);

        // 4. Case: Partial aggregation (start, middle, end)
        List<Segment> listC = Arrays.asList(
            createSegment(IbanCharType.ALPHABETIC, 1),
            createSegment(IbanCharType.ALPHABETIC, 1),
            createSegment(IbanCharType.NUMERIC, 1),
            createSegment(IbanCharType.NUMERIC, 2),
            createSegment(IbanCharType.ALPHABETIC, 3),
            createSegment(IbanCharType.ALPHABETIC, 4)
        );
        List<Segment> expectedC = Arrays.asList(
            createSegment(IbanCharType.ALPHABETIC, 2),
            createSegment(IbanCharType.NUMERIC, 3),
            createSegment(IbanCharType.ALPHABETIC, 7)
        );
        assertThat(IbanPatternConverter.aggregateSegments(listC)).isEqualTo(expectedC);
    }

    /**
     * Tests the final step of Regex construction, independent of parsing/aggregation.
     */
    @DisplayName("buildRegex should correctly concatenate segment patterns")
    @Test
    void testBuildRegex() throws Exception {
        // 1. Case: Null or Empty input
        assertThat(IbanPatternConverter.buildRegex(null)).isNull();
        assertThat(IbanPatternConverter.buildRegex(Collections.emptyList())).isNull();

        // 2. Case: Mixed segments (with length 1 and > 1)
        List<Segment> segments = Arrays.asList(
            createSegment(IbanCharType.ALPHABETIC, 4),
            createSegment(IbanCharType.ALPHANUMERIC, 16),
            createSegment(IbanCharType.ALPHABETIC, 1)
        );

        String expected = "[A-Z]{4}[A-Z0-9]{16}[A-Z]";

        assertThat(IbanPatternConverter.buildRegex(segments)).isEqualTo(expected);

        assertThat(IbanPatternConverter.buildRegex(Collections.emptyList())).isNull();
    }

    /**
     * Tests the {@link Segment#toString()} method, increasing coverage.
     */
    @DisplayName("Segment toString() should return expected format")
    @Test
    void testSegmentToString() throws Exception {
        Segment segment = createSegment(IbanCharType.ALPHABETIC, 10);
        assertThat(segment).hasToString("Segment[charType=ALPHABETIC, length=10]");
    }

    @DisplayName("Segment addLength should handle non-positive addition")
    @Test
    void testSegmentAddLengthShouldHandleNonPositive() throws Exception {
        Segment segment5 = createSegment(IbanCharType.ALPHABETIC, 5);

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
    void testSegmentEqualsAndHashCodeConsistency() throws Exception {
        Segment segmentA = createSegment(IbanCharType.ALPHABETIC, 10);
        Segment segmentB = createSegment(IbanCharType.ALPHABETIC, 10);
        Segment segmentC = createSegment(IbanCharType.ALPHABETIC, 5);
        Segment segmentD = createSegment(IbanCharType.NUMERIC, 10);

        assertThat(segmentA)
            // check with null and different class
            .isNotEqualTo(null)
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
    void testPrivateConstructorPreventsInstantiation() {
        TestUtil.assertConstructorIsPrivate(IbanPatternConverter.class);
    }

}
