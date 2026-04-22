package de.speedbanking.bic;

import static de.speedbanking.bic.BicValidationError.EMPTY;
import static de.speedbanking.bic.BicValidationError.ILLEGAL_CHARACTERS;
import static de.speedbanking.bic.BicValidationError.INCORRECT_LENGTH;
import static de.speedbanking.bic.BicValidationError.INVALID_BANK_CODE;
import static de.speedbanking.bic.BicValidationError.INVALID_COUNTRY;
import static de.speedbanking.bic.BicValidationError.values;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * JUnit test class for the {@link BicValidationError} enum.<br>
 * Ensures all constants are defined and their failure texts are correct.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class BicValidationErrorTest {

    @DisplayName("Failure text should be defined for all reasons")
    @ParameterizedTest(name = "Constant {0} should have a non-empty failure text")
    @EnumSource(BicValidationError.class)
    void failure_text_is_present(BicValidationError reason) {
        String text = reason.getText();

        assertThat(text)
            .as("Failure text for %s must be defined and non-blank", reason.name())
            .isNotBlank();
    }

    @DisplayName("Verify InvalidBicException.toString() format")
    @ParameterizedTest(name = "toString for reason: {0}")
    @EnumSource(BicValidationError.class)
    void to_string_format_is_valid(BicValidationError reason) {
        InvalidBicException ex = InvalidBicException.of(reason);
        assertThat(ex.toString())
            .contains(ex.getClass().getSimpleName())
            .contains(reason.toString())
            .contains("[reason=" + ex.getReason() + ", input=null]");
    }

    @DisplayName("All failure texts and toString must match their expected descriptions")
    @ParameterizedTest(name = "Reason {0} text should be ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "EMPTY              | BIC is null or empty",
        "INCORRECT_LENGTH   | BIC has incorrect length",
        "ILLEGAL_CHARACTERS | BIC contains illegal character(s)",
        "INVALID_COUNTRY    | BIC has invalid country code"
    })
    void failure_text_matches_description(BicValidationError reason, String expectedText) {
        assertThat(reason.getText()).isEqualTo(expectedText);

        assertThat(reason.toLongString())
            .as("Check toLongString() format for %s", reason.name())
            .isEqualTo(String.format("%s[%s: %s]", BicValidationError.class.getSimpleName(), reason, expectedText));
    }

    @DisplayName("All expected enum constants should exist in the correct order")
    @Test
    void constants_are_correctly_ordered() {
        assertThat(values()).containsExactly(
            EMPTY,
            INCORRECT_LENGTH,
            INVALID_BANK_CODE,
            INVALID_COUNTRY,
            ILLEGAL_CHARACTERS);
    }

}

