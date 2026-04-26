package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CharArrayWrapper}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class CharArrayWrapperTest {

    @DisplayName("Should wrap full array and provide correct characters")
    @Test
    void constructor_shouldWrapFullArray() {
        char[] data = "ABCDE".toCharArray();
        CharArrayWrapper wrapper = new CharArrayWrapper(data);

        assertThat(wrapper)
            .hasSize(5)
            .hasToString("ABCDE");

        assertThat(wrapper.charAt(0)).isEqualTo('A');
        assertThat(wrapper.charAt(4)).isEqualTo('E');
    }

    @DisplayName("Should wrap sub-ranges correctly")
    @ParameterizedTest(name = "[{index}] range [{0}, {1}] should represent {2}")
    @CsvSource(delimiter = '|', value = {
        "0 | 2 | AB",
        "1 | 3 | BCD",
        "4 | 1 | E",
        "5 | 0 | ''"
    })
    void constructor_shouldWrapSubRange(int offset, int length, String expected) {
        char[] data = "ABCDE".toCharArray();
        CharArrayWrapper wrapper = new CharArrayWrapper(data, offset, length);

        assertThat(wrapper)
            .hasSize(length)
            .hasToString(expected);
    }

    @DisplayName("Constructor should throw IndexOutOfBoundsException for invalid ranges")
    @ParameterizedTest(name = "[{index}] offset={0}, length={1} ({2})")
    @CsvSource(delimiter = '|', value = {
        "-1 |  0 | Offset negative",
        " 0 | -1 | Length negative",
        " 0 |  5 | Length exceeds array",
        " 3 |  2 | Offset + length exceeds array",
        " 5 |  0 | Offset outside array"
    })
    void constructor_shouldThrowOnInvalidBounds(int offset, int length, String reason) {
        char[] data = "ABCD".toCharArray(); // length 4

        assertThatThrownBy(() -> new CharArrayWrapper(data, offset, length))
            .as(reason)
            .isExactlyInstanceOf(IndexOutOfBoundsException.class)
            .hasMessage("Invalid range (offset: %d, length: %d) for array length %d",
                        offset, length, data.length);
    }

    @DisplayName("charAt should throw IndexOutOfBoundsException for invalid access")
    @ParameterizedTest(name = "[{index}] index {0} should throw exception")
    @ValueSource(ints = {-1, 3, 99})
    void charAt_shouldThrowOnInvalidIndex(int index) {
        CharArrayWrapper wrapper = new CharArrayWrapper("ABC".toCharArray());

        assertThatThrownBy(() -> wrapper.charAt(index))
            .isExactlyInstanceOf(IndexOutOfBoundsException.class);
    }

    @DisplayName("subSequence should throw IndexOutOfBoundsException for invalid ranges")
    @ParameterizedTest(name = "[{index}] start={0}, end={1}")
    @CsvSource(delimiter = '|', value = {
        "-1 |  1 | Start negative",
        " 1 |  0 | End before start",
        " 0 |  4 | End exceeds length",
        " 2 |  5 | End far out"
    })
    void subSequence_shouldThrowOnInvalidRange(int start, int end) {
        CharArrayWrapper wrapper = new CharArrayWrapper("ABC".toCharArray());

        assertThatThrownBy(() -> wrapper.subSequence(start, end))
            .isExactlyInstanceOf(IndexOutOfBoundsException.class)
            .hasMessageContaining("Invalid subSequence range");
    }

    @DisplayName("subSequence should return the same instance if range covers everything")
    @ParameterizedTest(name = "[{index}] start={0}, end={1} on wrapper with length {2}")
    @CsvSource(delimiter = '|', value = {
        "0 | 3 | 3 | Full range of 3 chars",
        "0 | 0 | 0 | Empty range of empty wrapper",
        "0 | 5 | 5 | Full range of 5 chars"
    })
    void subSequence_shouldReturnIdenticalInstanceOnFullRange(int start, int end, int length, String reason) {
        // given
        char[] data = "ABCDE".toCharArray();
        CharArrayWrapper wrapper = new CharArrayWrapper(data, 0, length);

        // when
        CharSequence result = wrapper.subSequence(start, end);

        // then
        assertThat(result)
            .as(reason)
            .isSameAs(wrapper);
    }

    @DisplayName("subSequence should return a new instance for actual sub-ranges")
    @ParameterizedTest(name = "[{index}] {2} (start={0}, end={1})")
    @CsvSource(delimiter = '|', value = {
        "1 | 4 | Middle sub-range",
        "0 | 4 | Start matches but end differs",
        "1 | 5 | End matches but start differs"
    })
    void subSequence_shouldReturnNewInstanceOnPartialRange(int start, int end, String reason) {
        String content = "ABCDE";
        CharArrayWrapper wrapper = new CharArrayWrapper(content.toCharArray());

        CharSequence sub = wrapper.subSequence(start, end);

        assertThat(sub)
            .as(reason)
            .isNotSameAs(wrapper)
            .isInstanceOf(CharArrayWrapper.class)
            .hasToString(content.substring(start, end));
    }

    @DisplayName("equals should handle various CharSequence types and edge cases")
    @Test
    @SuppressWarnings({"SelfAssertion", "UnnecessaryStringBuilder"})
    void equals_shouldBeRobustAgainstDifferentTypes() {
        String content = "ABC";
        CharArrayWrapper wrapper = new CharArrayWrapper(content.toCharArray());

        assertThat(wrapper)
            .isEqualTo(wrapper)                    // same instance
            .isEqualTo(content)                    // against String
            .isEqualTo(new StringBuilder(content)) // against StringBuilder
            .isNotEqualTo("ABD")                   // different content
            .isNotEqualTo("AB")                    // different length
            .isNotNull()                           // null check
            .isNotEqualTo(42);                     // different type
    }

    @DisplayName("hashCode should be consistent with equals and content")
    @Test
    void hashCode_shouldBeConsistentWithEquals() {
        String content = "ABC";
        char[] data = content.toCharArray();
        CharArrayWrapper wrapper = new CharArrayWrapper(data);
        CharArrayWrapper sameContent = new CharArrayWrapper(data.clone());
        CharArrayWrapper subWrapper = new CharArrayWrapper("XABCY".toCharArray(), 1, 3);

        // Consistency with equals: Equal objects must have equal hash codes
        assertThat(wrapper.hashCode())
            .isEqualTo(sameContent.hashCode())
            .isEqualTo(subWrapper.hashCode())
            .isEqualTo(content.hashCode());

        // Self-consistency
        int initialHash = wrapper.hashCode();
        assertThat(wrapper.hashCode()).isEqualTo(initialHash);
    }

    @DisplayName("hashCode should differ for different content")
    @Test
    void hashCode_shouldDifferOnDifferentContent() {
        CharArrayWrapper wrapper1 = new CharArrayWrapper("ABC".toCharArray());
        CharArrayWrapper wrapper2 = new CharArrayWrapper("ABD".toCharArray());

        assertThat(wrapper1.hashCode()).isNotEqualTo(wrapper2.hashCode());
    }

}
