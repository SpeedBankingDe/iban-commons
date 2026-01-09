package de.speedbanking.iban;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * JUnit test class for {@link InvalidIbanException}.<br>
 * Ensures the exception correctly stores the reason and uses the reason's
 * failure text as the exception message.
 */
class InvalidIbanExceptionTest extends org.assertj.core.api.Assertions {

    @ParameterizedTest(name = "Test Exception for reason: {0}")
    @EnumSource(IbanValidationError.class)
    @DisplayName("Exception should store the reason and use its failure text as message")
    void ofShouldInitializeCorrectly(IbanValidationError reason) {
        InvalidIbanException exception = InvalidIbanException.of(reason);

        assertThat(exception)
            .isNotNull()
            .isInstanceOf(RuntimeException.class);

        assertThat(exception.getReason())
            .as("getReason() should return the original ValidationError")
            .isEqualTo(reason);

        assertThat(exception.getMessage())
            .as("Exception message must match the reason's failure text")
            .isEqualTo(reason.getText());

        assertThat(exception.toString())
            .startsWith("InvalidIbanException: ")
            .endsWith(" (" + reason + ")");
    }

    /**
     * Tests the behavior when a {@code null} reason is passed.
     */
    @Test
    @DisplayName("Constructor/of() should throw NullPointerException when reason is null")
    void ofShouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidIbanException.of(null))
            .withMessage("reason required");
    }

}

