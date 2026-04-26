package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class InvalidIbanExceptionTest {

    // -------------------------------------------------------------------------
    // Factory method: of(reason)
    // -------------------------------------------------------------------------

    @DisplayName("of(reason) should store reason and derive message from reason text")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanValidationError.class)
    void of_shouldInitializeCorrectly(IbanValidationError reason) {
        InvalidIbanException exception = InvalidIbanException.of(reason);

        assertThat(exception)
            .isNotNull()
            .isInstanceOf(RuntimeException.class)
            .as("Exception message must contain the reason's failure text")
            .hasMessage("%s (%s)", reason.getText(), reason)
            .hasToString("InvalidIbanException[reason=" + reason + ", input=null]");

        assertThat(exception.getReason())
            .as("getReason() should return the original ValidationError")
            .isEqualTo(reason);
    }

    @DisplayName("of(reason) should yield no input")
    @Test
    void of_shouldHaveNoInputWhenNotSupplied() {
        InvalidIbanException exception = InvalidIbanException.of(IbanValidationError.EMPTY);

        assertThat(exception.getInput())
            .as("getInput() should be null when no input was supplied")
            .isNull();
    }

    // -------------------------------------------------------------------------
    // Factory method: of(reason, input)
    // -------------------------------------------------------------------------

    @DisplayName("of(reason, input) should store both reason and input")
    @Test
    void of_shouldStoreReasonAndInput() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        InvalidIbanException exception = InvalidIbanException.of(reason, input);

        assertThat(exception.getReason())
            .isEqualTo(reason);

        assertThat(exception.getInput())
            .as("getInput() should return the supplied input")
            .isEqualTo(input);

        assertThat(exception.toString())
            .as("toString() should contain the input")
            .endsWith("input=" + input + ']');
    }

    @DisplayName("of(reason, empty string) should treat empty input as absent")
    @ParameterizedTest
    @NullAndEmptySource
    void of_shouldKeepInputEvenIfNullOrBlank(String input) {
        InvalidIbanException exception = InvalidIbanException.of(IbanValidationError.EMPTY, input);

        assertThat(exception.getInput()).isEqualTo(input);
    }

    // -------------------------------------------------------------------------
    // Null-safety
    // -------------------------------------------------------------------------

    @DisplayName("of(null) should throw NullPointerException")
    @Test
    void of_shouldThrowOnNullReason() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidIbanException.of(null))
            .withMessage("reason required");
    }

    @DisplayName("of(null, input) should throw NullPointerException")
    @Test
    void of_shouldThrowOnNullReasonWithInput() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidIbanException.of(null, "DE00123456789012345678"))
            .withMessage("reason required");
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @DisplayName("equals() should be true for same reason and same input")
    @Test
    void equals_shouldBeTrueForSameReasonAndInput() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        InvalidIbanException a = InvalidIbanException.of(reason, input);
        InvalidIbanException b = InvalidIbanException.of(reason, input);

        assertThat(a)
            .isEqualTo(b)
            .hasSameHashCodeAs(b);
    }

    @DisplayName("equals() should be true for same reason and both inputs null")
    @Test
    void equals_shouldBeTrueForSameReasonAndNullInput() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        InvalidIbanException a = InvalidIbanException.of(reason, null);
        InvalidIbanException b = InvalidIbanException.of(reason, null);

        assertThat(a)
            .isEqualTo(b)
            .hasSameHashCodeAs(b);
    }

    @DisplayName("equals() should be false for different reasons")
    @Test
    void equals_shouldBeFalseForDifferentReasons() {
        IbanValidationError[] errors = IbanValidationError.values();
        org.junit.jupiter.api.Assumptions.assumeTrue(errors.length >= 2,
            "Need at least two IbanValidationError values for this test");

        InvalidIbanException a = InvalidIbanException.of(errors[0]);
        InvalidIbanException b = InvalidIbanException.of(errors[1]);

        assertThat(a).isNotEqualTo(b);
    }

    @DisplayName("equals() should be false when one has input and the other does not")
    @Test
    void equals_shouldBeFalseWhenInputDiffers() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        InvalidIbanException withInput = InvalidIbanException.of(reason, "DE00123456789012345678");
        InvalidIbanException withoutInput = InvalidIbanException.of(reason);

        assertThat(withInput).isNotEqualTo(withoutInput);
    }

    @DisplayName("equals() should be reflexive")
    @Test
    @SuppressWarnings("SelfAssertion")
    void equals_shouldBeReflexive() {
        InvalidIbanException ex = InvalidIbanException.of(IbanValidationError.EMPTY);
        assertThat(ex).isEqualTo(ex);
    }

    @DisplayName("equals() should return false for null and other types")
    @Test
    void equals_shouldReturnFalseForNullAndOtherTypes() {
        InvalidIbanException ex = InvalidIbanException.of(IbanValidationError.EMPTY);
        assertThat(ex)
            .isNotNull()
            .isNotEqualTo("some string");
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @DisplayName("toString() should include class name, message, reason and input")
    @Test
    void toString_shouldContainAllParts() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        String result = InvalidIbanException.of(reason, input).toString();

        assertThat(result)
            .isEqualTo("InvalidIbanException[reason=%s, input=%s]", reason, input);
    }

    @DisplayName("toString() without input")
    @Test
    void toString_shouldHandleAbsentInput() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        String result = InvalidIbanException.of(reason).toString();

        assertThat(result)
            .isEqualTo("InvalidIbanException[reason=EMPTY, input=null]");
    }

}
