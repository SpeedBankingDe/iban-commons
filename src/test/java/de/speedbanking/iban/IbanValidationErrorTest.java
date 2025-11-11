package de.speedbanking.iban;

import static de.speedbanking.iban.IbanValidationError.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * JUnit test class for the {@link IbanValidationError} enum.<br>
 * Ensures all constants are defined and their failure texts are correct.
 */
class IbanValidationErrorTest extends Assertions {

    @DisplayName("Failure text should be defined for all reasons")
    @ParameterizedTest(name = "Constant {0} should have a non-empty failure text")
    @EnumSource(IbanValidationError.class)
    void failureTextShouldNotBeNullOrEmpty(IbanValidationError reason) {
        String text = reason.getText();

        assertThat(text)
            .as("Failure text for %s must be defined and non-blank", reason.name())
            .isNotBlank();
    }

    @DisplayName("All failure texts must match their fixed expected descriptions")
    @ParameterizedTest(name = "Reason {0} text should be ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "EMPTY                    | IBAN is null or empty",
        "INCORRECT_LENGTH         | IBAN has incorrect length",
        "ILLEGAL_CHARACTERS       | IBAN contains illegal character(s)",
        "INVALID_COUNTRY          | IBAN has invalid country code",
        "UNSUPPORTED_COUNTRY      | IBAN has unsupported country code",
        "INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "INVALID_STRUCTURE        | IBAN violates country-specific structure rules",
        "INVALID_CHECKSUM         | IBAN violates ISO 7064 Mod 97-10 checksum check"
    })
    void failureTextsShouldMatchFixedExpectedDescriptions(IbanValidationError reason, String expectedText) {
        assertThat(reason.getText()).isEqualTo(expectedText);

        assertThat(reason.toLongString())
            .as("Check toLongString() format for %s", reason.name())
            .isEqualTo(String.format("%s[%s: %s]", IbanValidationError.class.getSimpleName(), reason, expectedText));
    }

    @DisplayName("All expected enum constants should exist")
    @Test
    void allConstantsShouldExist() {
        assertThat(values())
            .containsExactly(EMPTY,
                             INCORRECT_LENGTH,
                             ILLEGAL_CHARACTERS,
                             INVALID_COUNTRY,
                             UNSUPPORTED_COUNTRY,
                             INCORRECT_LENGTH_COUNTRY,
                             INVALID_CHECK_DIGITS,
                             INVALID_STRUCTURE,
                             INVALID_CHECKSUM);
    }

}

