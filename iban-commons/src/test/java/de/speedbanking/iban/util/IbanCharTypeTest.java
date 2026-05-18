package de.speedbanking.iban.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

/**
 * JUnit test class for {@link IbanCharType}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IbanCharTypeTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "ALPHABETIC   | a | [A-Z]",
        "NUMERIC      | n | [0-9]",
        "ALPHANUMERIC | c | [0-9A-Z]",
        "SPACE        | e | [ ]"
    })
    void getIbanCodeAndRegexPattern_givenEnumConstants_shouldMatchExpectedProperties(IbanCharType type, char expectedCode, String expectedRegex) {
        // fluent verification of enum properties
        assertThat(type.getIbanCode()).isEqualTo(expectedCode);
        assertThat(type.getRegexPattern()).isEqualTo(expectedRegex);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "a | ALPHABETIC",
        "n | NUMERIC",
        "c | ALPHANUMERIC",
        "e | SPACE"
    })
    void fromIbanCode_givenValidCode_shouldReturnMatchingEnumConstant(char code, IbanCharType expectedType) {
        IbanCharType actualType = IbanCharType.fromIbanCode(code);

        assertThat(actualType)
            .isNotNull()
            .isEqualTo(expectedType);
    }

    @Test
    void fromIbanCode_givenInvalidOrUppercaseCode_shouldReturnNull() {
        assertThat(IbanCharType.fromIbanCode('x')).isNull();
        assertThat(IbanCharType.fromIbanCode('A')).isNull();
    }

    @Test
    void getIbanCodes_givenExistingEnumConstants_shouldReturnAllDefinedCodes() {
        List<Character> codes = IbanCharType.getIbanCodes();

        assertThat(codes)
            .isNotNull()
            .hasSize(4)
            .containsExactlyInAnyOrder('a', 'n', 'c', 'e');
    }

}
