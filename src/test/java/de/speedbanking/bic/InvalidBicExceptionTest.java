package de.speedbanking.bic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

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

    @DisplayName("of(reason) should store reason and derive message from reason text")
    @ParameterizedTest(name = "Test Exception for reason: {0}")
    @EnumSource(BicValidationError.class)
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
            .isEqualTo(reason.getText() + " (" + reason + ")");

        assertThat(exception)
            .hasToString("InvalidBicException[reason=" + reason + ", input=null]");
    }

    @DisplayName("of(reason) should yield no input")
    @Test
    void ofWithoutInputShouldHaveNoInput() {
        InvalidBicException exception = InvalidBicException.of(BicValidationError.EMPTY);

        assertThat(exception.getInput())
            .as("getInput() should be null when no input was supplied")
            .isNull();
    }

    // -------------------------------------------------------------------------
    // Factory method: of(reason, input)
    // -------------------------------------------------------------------------

    @DisplayName("of(reason, input) should store both reason and input")
    @Test
    void ofWithInputShouldStoreInput() {
        BicValidationError reason = BicValidationError.EMPTY;
        String input = "DEUTDEDB";

        InvalidBicException exception = InvalidBicException.of(reason, input);

        assertThat(exception.getReason())
            .isEqualTo(reason);

        assertThat(exception.getInput())
            .as("getInput() should return the supplied input")
            .isEqualTo(input);

        assertThat(exception.toString())
            .as("toString() should contain the input")
            .endsWith("input=" + input + "]");
    }

    @DisplayName("of(reason, <empty>) should keep input")
    @ParameterizedTest(name = "input ''{0}''")
    @NullAndEmptySource
    void ofWithNullInputShouldKeepInput(String input) {
        InvalidBicException exception = InvalidBicException.of(BicValidationError.EMPTY, input);

        assertThat(exception.getInput()).isEqualTo(input);
    }

    // -------------------------------------------------------------------------
    // Null-safety
    // -------------------------------------------------------------------------

    @DisplayName("of(null) should throw NullPointerException")
    @Test
    void ofShouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidBicException.of(null))
            .withMessage("reason required");
    }

    @DisplayName("of(null, input) should throw NullPointerException")
    @Test
    void ofWithInputShouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidBicException.of(null, "DEUTDEDB"))
            .withMessage("reason required");
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @DisplayName("equals() should be true for same reason and same input")
    @Test
    void equalsShouldBeTrueForSameReasonAndInput() {
        BicValidationError reason = BicValidationError.EMPTY;
        String input = "DEUTDEDB";

        InvalidBicException a = InvalidBicException.of(reason, input);
        InvalidBicException b = InvalidBicException.of(reason, input);

        assertThat(a)
            .isEqualTo(b)
            .hasSameHashCodeAs(b);
    }

    @DisplayName("equals() should be true for same reason and both inputs null")
    @Test
    void equalsShouldBeTrueForSameReasonAndNullInput() {
        BicValidationError reason = BicValidationError.EMPTY;

        InvalidBicException a = InvalidBicException.of(reason, null);
        InvalidBicException b = InvalidBicException.of(reason, null);

        assertThat(a)
            .isEqualTo(b)
            .hasSameHashCodeAs(b);
    }

    @DisplayName("equals() should be false for different reasons")
    @Test
    void equalsShouldBeFalseForDifferentReasons() {
        BicValidationError[] errors = BicValidationError.values();
        org.junit.jupiter.api.Assumptions.assumeTrue(errors.length >= 2,
            "Need at least two BicValidationError values for this test");

        InvalidBicException a = InvalidBicException.of(errors[0]);
        InvalidBicException b = InvalidBicException.of(errors[1]);

        assertThat(a).isNotEqualTo(b);
    }

    @DisplayName("equals() should be false when one has input and the other does not")
    @Test
    void equalsShouldBeFalseWhenInputDiffers() {
        BicValidationError reason = BicValidationError.EMPTY;

        InvalidBicException withInput    = InvalidBicException.of(reason, "DEUTDEDB");
        InvalidBicException withoutInput = InvalidBicException.of(reason);

        assertThat(withInput).isNotEqualTo(withoutInput);
    }

    @DisplayName("equals() should be reflexive")
    @Test
    @SuppressWarnings("SelfAssertion")
    void equalsShouldBeReflexive() {
        InvalidBicException ex = InvalidBicException.of(BicValidationError.EMPTY);
        assertThat(ex).isEqualTo(ex);
    }

    @DisplayName("equals() should return false for null and other types")
    @Test
    void equalsShouldReturnFalseForNullAndOtherTypes() {
        InvalidBicException ex = InvalidBicException.of(BicValidationError.EMPTY);
        assertThat(ex)
            .isNotEqualTo(null)
            .isNotEqualTo("some string");
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @DisplayName("toString() should include class name, message, reason and input")
    @Test
    void toStringShouldContainAllParts() {
        BicValidationError reason = BicValidationError.EMPTY;
        String input = "DEUTDEDB";

        String result = InvalidBicException.of(reason, input).toString();

        assertThat(result)
            .startsWith("InvalidBicException[")
            .contains("reason=" + reason)
            .contains("input=" + input);
    }

    @DisplayName("toString() without input should not contain trailing colon segment")
    @Test
    void toStringShouldNotContainInputSegmentWhenAbsent() {
        BicValidationError reason = BicValidationError.EMPTY;

        String result = InvalidBicException.of(reason).toString();

        assertThat(result)
            .isEqualTo("InvalidBicException[reason=EMPTY, input=null]");
    }

}
