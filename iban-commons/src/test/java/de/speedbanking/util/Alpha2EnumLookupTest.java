package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

final class Alpha2EnumLookupTest {

    private enum ValidTestEnum {
        AA("AA"),
        ZZ("ZZ");

        private final String code;

        ValidTestEnum(String code) {
            this.code = code;
        }

        String getCode() {
            return code;
        }
    }

    private enum BadLengthEnum {
        AAA("AAA");

        private final String code;

        BadLengthEnum(String code) {
            this.code = code;
        }

        String getCode() {
            return code;
        }
    }

    private enum BadCaseEnum {
        AA("aa");

        private final String code;

        BadCaseEnum(String code) {
            this.code = code;
        }

        String getCode() {
            return code;
        }
    }

    private final Alpha2EnumLookup<ValidTestEnum> lookup = new Alpha2EnumLookup<>(ValidTestEnum.class, ValidTestEnum::getCode);

    @DisplayName("should resolve valid boundaries successfully")
    @Test
    void shouldResolveValidBoundaries() {
        assertThat(lookup.fromCode("AA")).isEqualTo(ValidTestEnum.AA);
        assertThat(lookup.fromCode("ZZ")).isEqualTo(ValidTestEnum.ZZ);
    }

    @DisplayName("should return null for invalid, non-uppercase or out-of-bounds inputs")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A", "AAA", "aa", "zz", "A{", "88", "A1"})
    void shouldReturnNullForInvalidInputs(String code) {
        assertThat(lookup.fromCode(code)).isNull();
    }

    @DisplayName("should return true for assigned uppercase character pairs")
    @Test
    void shouldReturnTrueForAssignedPairs() {
        assertThat(lookup.isAssigned('A', 'A')).isTrue();
        assertThat(lookup.isAssigned('Z', 'Z')).isTrue();
    }

    @DisplayName("should return false for unassigned but syntactically valid pairs")
    @Test
    void shouldReturnFalseForUnassignedPairs() {
        assertThat(lookup.isAssigned('A', 'B')).isFalse();
    }

    @DisplayName("should return false when characters violate uppercase constraints")
    @Test
    void shouldReturnFalseForInvalidCharacters() {
        assertThat(lookup.isAssigned('a', 'a')).isFalse();
        assertThat(lookup.isAssigned('A', '{')).isFalse();
        assertThat(lookup.isAssigned('1', '2')).isFalse();
    }

    @DisplayName("should throw IllegalArgumentException when code length is invalid during initialization")
    @Test
    void shouldThrowExceptionForInvalidLength() {
        assertThrows(IllegalArgumentException.class, () ->
            new Alpha2EnumLookup<>(BadLengthEnum.class, BadLengthEnum::getCode)
        );
    }

    @DisplayName("should throw IllegalArgumentException when code contains non-uppercase letters during initialization")
    @Test
    void shouldThrowExceptionForInvalidCasing() {
        assertThrows(IllegalArgumentException.class, () ->
            new Alpha2EnumLookup<>(BadCaseEnum.class, BadCaseEnum::getCode)
        );
    }

}
