package de.speedbanking.iban;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * JUnit test class for {@link InvalidIbanException}.<br>
 * Ensures the exception correctly stores the reason and uses the reason's
 * failure text as the exception message.
 */
@SuppressWarnings("PMD.LinguisticNaming")
class InvalidIbanExceptionTest extends org.assertj.core.api.Assertions {

    // -------------------------------------------------------------------------
    // Factory method: of(reason)
    // -------------------------------------------------------------------------

    @DisplayName("of(reason) should store reason and derive message from reason text")
    @ParameterizedTest(name = "Test Exception for reason: {0}")
    @EnumSource(IbanValidationError.class)
    void ofShouldInitializeCorrectly(IbanValidationError reason) {
        InvalidIbanException exception = InvalidIbanException.of(reason);

        assertThat(exception)
            .isNotNull()
            .isInstanceOf(RuntimeException.class);

        assertThat(exception.getReason())
            .as("getReason() should return the original ValidationError")
            .isEqualTo(reason);

        assertThat(exception.getMessage())
            .as("Exception message must contain the reason's failure text")
            .isEqualTo(reason.getText() + " (" + reason + ")");

        assertThat(exception.toString())
            .isEqualTo("InvalidIbanException[reason=" + reason + ", input=null]");
    }

    @DisplayName("of(reason) should yield no input")
    @Test
    void ofWithoutInputShouldHaveNoInput() {
        InvalidIbanException exception = InvalidIbanException.of(IbanValidationError.EMPTY);

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

    @DisplayName("of(reason, input) should store both reason and input")
    @Test
    void ofWithInputShouldStoreInput() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        InvalidIbanException exception = InvalidIbanException.of(reason, input);

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
            .endsWith("input=" + input + ']');
    }

    @DisplayName("of(reason, empty string) should treat empty input as absent")
    @ParameterizedTest
    @NullAndEmptySource
    void ofWithNullOrBlankInputShouldHaveNoInput(String input) {
        InvalidIbanException exception = InvalidIbanException.of(IbanValidationError.EMPTY, input);

        assertThat(exception.getInput()).isNull();
        assertThat(exception.hasInput())
            .as("hasInput() should be false for an empty input string")
            .isFalse();
    }

    // -------------------------------------------------------------------------
    // Null-safety
    // -------------------------------------------------------------------------

    @DisplayName("of(null) should throw NullPointerException")
    @Test
    void ofShouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidIbanException.of(null))
            .withMessage("reason required");
    }

    @DisplayName("of(null, input) should throw NullPointerException")
    @Test
    void ofWithInputShouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidIbanException.of(null, "DE00123456789012345678"))
            .withMessage("reason required");
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @DisplayName("equals() should be true for same reason and same input")
    @Test
    void equalsShouldBeTrueForSameReasonAndInput() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        InvalidIbanException a = InvalidIbanException.of(reason, input);
        InvalidIbanException b = InvalidIbanException.of(reason, input);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @DisplayName("equals() should be true for same reason and both inputs null")
    @Test
    void equalsShouldBeTrueForSameReasonAndNullInput() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        InvalidIbanException a = InvalidIbanException.of(reason, null);
        InvalidIbanException b = InvalidIbanException.of(reason, null);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @DisplayName("equals() should be false for different reasons")
    @Test
    void equalsShouldBeFalseForDifferentReasons() {
        IbanValidationError[] errors = IbanValidationError.values();
        org.junit.jupiter.api.Assumptions.assumeTrue(errors.length >= 2,
            "Need at least two IbanValidationError values for this test");

        InvalidIbanException a = InvalidIbanException.of(errors[0]);
        InvalidIbanException b = InvalidIbanException.of(errors[1]);

        assertThat(a).isNotEqualTo(b);
    }

    @DisplayName("equals() should be false when one has input and the other does not")
    @Test
    void equalsShouldBeFalseWhenInputDiffers() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        InvalidIbanException withInput    = InvalidIbanException.of(reason, "DE00123456789012345678");
        InvalidIbanException withoutInput = InvalidIbanException.of(reason);

        assertThat(withInput).isNotEqualTo(withoutInput);
    }

    @DisplayName("equals() should be reflexive")
    @Test
    @SuppressWarnings("SelfAssertion")
    void equalsShouldBeReflexive() {
        InvalidIbanException ex = InvalidIbanException.of(IbanValidationError.EMPTY);
        assertThat(ex).isEqualTo(ex);
    }

    @DisplayName("equals() should return false for null and other types")
    @Test
    void equalsShouldReturnFalseForNullAndOtherTypes() {
        InvalidIbanException ex = InvalidIbanException.of(IbanValidationError.EMPTY);
        assertThat(ex).isNotEqualTo(null);
        assertThat(ex).isNotEqualTo("some string");
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @DisplayName("toString() should include class name, message, reason and input")
    @Test
    void toStringShouldContainAllParts() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        String result = InvalidIbanException.of(reason, input).toString();

        assertThat(result)
            .isEqualTo("InvalidIbanException[reason=" + reason + ", input=" + input + "]");
    }

    @DisplayName("toString() without input")
    @Test
    void toStringShouldNotContainInputSegmentWhenAbsent() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        String result = InvalidIbanException.of(reason).toString();

        assertThat(result)
            .isEqualTo("InvalidIbanException[reason=EMPTY, input=null]");
    }

}
