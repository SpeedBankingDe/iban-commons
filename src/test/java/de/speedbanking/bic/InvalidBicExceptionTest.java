package de.speedbanking.bic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * JUnit test class for {@link InvalidBicException}.<br>
 * Ensures the exception correctly stores the reason and uses the reason's
 * failure text as the exception message.
 *
 * @since 1.8.5
 */
@SuppressWarnings("PMD.LinguisticNaming")
class InvalidBicExceptionTest extends org.assertj.core.api.Assertions {

    // -------------------------------------------------------------------------
    // Factory method: of(reason)
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Test Exception for reason: {0}")
    @EnumSource(BicValidationError.class)
    @DisplayName("of(reason) should store reason and derive message from reason text")
    void ofShouldInitializeCorrectly(BicValidationError reason) {
        InvalidBicException exception = InvalidBicException.of(reason);

        assertThat(exception)
            .isNotNull()
            .isInstanceOf(RuntimeException.class);

        assertThat(exception.getReason())
            .as("getReason() should return the original BicValidationError")
            .isEqualTo(reason);

        assertThat(exception.getMessage())
            .as("Exception message must match the reason's failure text")
            .isEqualTo(reason.getText());

        assertThat(exception.toString())
            .startsWith("InvalidBicException: ")
            .endsWith(" (" + reason + ")");
    }

    @Test
    @DisplayName("of(reason) should yield no input")
    void ofWithoutInputShouldHaveNoInput() {
        InvalidBicException exception = InvalidBicException.of(BicValidationError.values()[0]);

        assertThat(exception.getInput())
            .as("getInput() should be null when no input was supplied")
            .isNull();

        assertThat(exception.hasInput())
            .as("hasInput() should be false when no input was supplied")
            .isFalse();
    }

    // -------------------------------------------------------------------------
    // Factory method: of(reason, input)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("of(reason, input) should store both reason and input")
    void ofWithInputShouldStoreInput() {
        BicValidationError reason = BicValidationError.values()[0];
        String input = "DEUTDEDB";

        InvalidBicException exception = InvalidBicException.of(reason, input);

        assertThat(exception.getReason())
            .isEqualTo(reason);

        assertThat(exception.getInput())
            .as("getInput() should return the supplied input")
            .isEqualTo(input);

        assertThat(exception.hasInput())
            .as("hasInput() should be true when input is supplied")
            .isTrue();

        assertThat(exception.toString())
            .as("toString() should contain the input")
            .endsWith(": " + input);
    }

    @Test
    @DisplayName("of(reason, null) should treat null input as absent")
    void ofWithNullInputShouldHaveNoInput() {
        InvalidBicException exception = InvalidBicException.of(BicValidationError.values()[0], null);

        assertThat(exception.getInput()).isNull();
        assertThat(exception.hasInput()).isFalse();
    }

    @Test
    @DisplayName("of(reason, empty string) should treat empty input as absent")
    void ofWithEmptyInputShouldHaveNoInput() {
        InvalidBicException exception = InvalidBicException.of(BicValidationError.values()[0], "");

        assertThat(exception.getInput()).isNull();
        assertThat(exception.hasInput())
            .as("hasInput() should be false for an empty input string")
            .isFalse();
    }

    // -------------------------------------------------------------------------
    // Null-safety
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("of(null) should throw NullPointerException")
    void ofShouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidBicException.of(null))
            .withMessage("reason required");
    }

    @Test
    @DisplayName("of(null, input) should throw NullPointerException")
    void ofWithInputShouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidBicException.of(null, "DEUTDEDB"))
            .withMessage("reason required");
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("equals() should be true for same reason and same input")
    void equalsShouldBeTrueForSameReasonAndInput() {
        BicValidationError reason = BicValidationError.values()[0];
        String input = "DEUTDEDB";

        InvalidBicException a = InvalidBicException.of(reason, input);
        InvalidBicException b = InvalidBicException.of(reason, input);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals() should be true for same reason and both inputs null")
    void equalsShouldBeTrueForSameReasonAndNullInput() {
        BicValidationError reason = BicValidationError.values()[0];

        InvalidBicException a = InvalidBicException.of(reason, null);
        InvalidBicException b = InvalidBicException.of(reason, null);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("equals() should be false for different reasons")
    void equalsShouldBeFalseForDifferentReasons() {
        BicValidationError[] errors = BicValidationError.values();
        org.junit.jupiter.api.Assumptions.assumeTrue(errors.length >= 2,
            "Need at least two BicValidationError values for this test");

        InvalidBicException a = InvalidBicException.of(errors[0]);
        InvalidBicException b = InvalidBicException.of(errors[1]);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals() should be false when one has input and the other does not")
    void equalsShouldBeFalseWhenInputDiffers() {
        BicValidationError reason = BicValidationError.values()[0];

        InvalidBicException withInput    = InvalidBicException.of(reason, "DEUTDEDB");
        InvalidBicException withoutInput = InvalidBicException.of(reason);

        assertThat(withInput).isNotEqualTo(withoutInput);
    }

    @Test
    @DisplayName("equals() should be reflexive")
    void equalsShouldBeReflexive() {
        InvalidBicException ex = InvalidBicException.of(BicValidationError.values()[0]);
        assertThat(ex).isEqualTo(ex);
    }

    @Test
    @DisplayName("equals() should return false for null and other types")
    void equalsShouldReturnFalseForNullAndOtherTypes() {
        InvalidBicException ex = InvalidBicException.of(BicValidationError.values()[0]);
        assertThat(ex).isNotEqualTo(null);
        assertThat(ex).isNotEqualTo("some string");
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toString() should include class name, message, reason and input")
    void toStringShouldContainAllParts() {
        BicValidationError reason = BicValidationError.values()[0];
        String input = "DEUTDEDB";

        String result = InvalidBicException.of(reason, input).toString();

        assertThat(result)
            .startsWith("InvalidBicException: ")
            .contains("(" + reason + ")")
            .endsWith(": " + input);
    }

    @Test
    @DisplayName("toString() without input should not contain trailing colon segment")
    void toStringShouldNotContainInputSegmentWhenAbsent() {
        BicValidationError reason = BicValidationError.values()[0];

        String result = InvalidBicException.of(reason).toString();

        assertThat(result)
            .startsWith("InvalidBicException: ")
            .endsWith("(" + reason + ")")
            .doesNotContain(": " + reason + ":");
    }

}
