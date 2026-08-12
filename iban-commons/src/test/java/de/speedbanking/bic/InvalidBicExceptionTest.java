package de.speedbanking.bic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit test class for {@link InvalidBicException}.
 * <p>
 * Ensures the exception correctly stores reason, input, and country code, and formats
 * exception messages and string representations properly.
 *
 * @since 1.8.5
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class InvalidBicExceptionTest {

    @DisplayName("of(reason) should store reason and derive message from reason text")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(BicValidationError.class)
    void of_shouldInitializeCorrectly_whenReasonIsGiven(BicValidationError reason) {
        InvalidBicException exception = InvalidBicException.of(reason);

        assertThat(exception)
            .isNotNull()
            .isInstanceOf(RuntimeException.class);

        assertThat(exception.getReason())
            .as("getReason() should return the original BicValidationError")
            .isEqualTo(reason);

        assertThat(exception.getCountryCode())
            .as("getCountryCode() should be null when omitted")
            .isNull();

        assertThat(exception)
            .as("Exception message must match the reason's failure text")
            .hasMessage("%s (%s)", reason.getText(), reason);

        assertThat(exception)
            .hasToString("InvalidBicException[reason=" + reason + ", input='null']");
    }

    @DisplayName("of(reason) should yield no input")
    @Test
    void of_shouldHaveNoInput_whenCalledWithoutInput() {
        InvalidBicException exception = InvalidBicException.of(BicValidationError.EMPTY);

        assertThat(exception.getInput())
            .as("getInput() should be null when no input was supplied")
            .isNull();
    }

    @DisplayName("of(reason, input) should store both reason and input")
    @Test
    void of_shouldStoreInput_whenInputIsGiven() {
        BicValidationError reason = BicValidationError.EMPTY;
        String input = "DEUTDEDB";

        InvalidBicException exception = InvalidBicException.of(reason, input);

        assertThat(exception.getReason()).isEqualTo(reason);

        assertThat(exception.getInput())
            .as("getInput() should return the supplied input")
            .isEqualTo(input);

        assertThat(exception.getCountryCode()).isNull();

        assertThat(exception)
            .hasToString("InvalidBicException[reason=%s, input='%s']", reason, input);
    }

    @DisplayName("of(reason, input, countryCode) should store reason, input, and upper-cased countryCode")
    @Test
    void of_shouldStoreReasonInputAndCountryCode() {
        BicValidationError reason = BicValidationError.INVALID_COUNTRY;
        String input = "DEUTXXDB";
        String countryCode = "de";

        InvalidBicException exception = InvalidBicException.of(reason, input, countryCode);

        assertThat(exception.getReason()).isEqualTo(reason);
        assertThat(exception.getInput()).isEqualTo(input);
        assertThat(exception.getCountryCode())
            .as("country code should be normalized to upper case")
            .isEqualTo("DE");

        assertThat(exception)
            .hasMessage("%s (%s), country DE: '%s'", reason.getText(), reason, input);

        assertThat(exception)
            .hasToString("%s[reason=%s, country=DE, input='%s']", InvalidBicException.class.getSimpleName(), reason, input);
    }

    @DisplayName("of(reason, input, countryCode) should normalize null or blank countryCode to null")
    @ParameterizedTest(name = "[{index}] {0}")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void of_shouldNormalizeNullOrBlankCountryCodeToNull(String countryCode) {
        InvalidBicException exception = InvalidBicException.of(BicValidationError.EMPTY, "DEUTDEDB", countryCode);

        assertThat(exception.getCountryCode()).isNull();
    }

    @DisplayName("of(null) should throw NullPointerException")
    @Test
    void of_shouldThrowException_whenReasonIsNull() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidBicException.of(null))
            .withMessage("reason required");
    }

    @DisplayName("of(null, input) should throw NullPointerException")
    @Test
    void of_shouldThrowException_whenReasonIsNullAndInputIsGiven() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidBicException.of(null, "DEUTDEDB"))
            .withMessage("reason required");
    }

    @DisplayName("equals() should be true for same reason, input, and countryCode")
    @Test
    void equals_shouldBeTrue_whenReasonInputAndCountryCodeAreEqual() {
        BicValidationError reason = BicValidationError.EMPTY;
        String input = "DEUTDEDB";
        String countryCode = "DE";

        InvalidBicException a = InvalidBicException.of(reason, input, countryCode);
        InvalidBicException b = InvalidBicException.of(reason, input, countryCode);

        assertThat(a)
            .isEqualTo(b)
            .hasSameHashCodeAs(b);
    }

    @DisplayName("equals() should be false when countryCode differs")
    @Test
    void equals_shouldBeFalse_whenCountryCodeDiffers() {
        BicValidationError reason = BicValidationError.EMPTY;
        String input = "DEUTDEDB";

        InvalidBicException withCountry = InvalidBicException.of(reason, input, "DE");
        InvalidBicException withoutCountry = InvalidBicException.of(reason, input, null);

        assertThat(withCountry).isNotEqualTo(withoutCountry);
    }

    @DisplayName("equals() should be false when input differs")
    @Test
    void equals_shouldBeFalse_whenInputsDiffer() {
        BicValidationError reason = BicValidationError.EMPTY;

        InvalidBicException withInput    = InvalidBicException.of(reason, "DEUTDEDB");
        InvalidBicException withoutInput = InvalidBicException.of(reason);

        assertThat(withInput).isNotEqualTo(withoutInput);
    }

    @DisplayName("equals() should be false for different reasons")
    @Test
    void equals_shouldBeFalse_whenReasonsAreDifferent() {
        BicValidationError[] errors = BicValidationError.values();
        org.junit.jupiter.api.Assumptions.assumeTrue(errors.length >= 2,
            "Need at least two BicValidationError values for this test");

        InvalidBicException a = InvalidBicException.of(errors[0]);
        InvalidBicException b = InvalidBicException.of(errors[1]);

        assertThat(a).isNotEqualTo(b);
    }

    @DisplayName("equals() should be reflexive")
    @Test
    @SuppressWarnings("SelfAssertion")
    void equals_shouldBeReflexive_whenComparedToItself() {
        InvalidBicException ex = InvalidBicException.of(BicValidationError.EMPTY);
        assertThat(ex).isEqualTo(ex);
    }

    @DisplayName("equals() should return false for null and other types")
    @Test
    void equals_shouldBeFalse_whenComparedToNullOrOtherType() {
        InvalidBicException ex = InvalidBicException.of(BicValidationError.EMPTY);
        assertThat(ex)
            .isNotNull()
            .isNotEqualTo("some string");
    }

    @DisplayName("toString() should include class name, reason, countryCode, and input")
    @Test
    void toString_shouldContainAllParts_whenInputAndCountryCodeAreGiven() {
        BicValidationError reason = BicValidationError.EMPTY;
        String input = "DEUTDEDB";
        String countryCode = "DE";

        String result = InvalidBicException.of(reason, input, countryCode).toString();

        assertThat(result)
            .isEqualTo("%s[reason=%s, country=%s, input='%s']",
                InvalidBicException.class.getSimpleName(), reason, countryCode, input);
    }

    @DisplayName("toString() without countryCode and input")
    @Test
    void toString_shouldHandleAbsentCountryCodeAndInput() {
        BicValidationError reason = BicValidationError.EMPTY;

        String result = InvalidBicException.of(reason).toString();

        assertThat(result)
            .isEqualTo("InvalidBicException[reason=EMPTY, input='null']");
    }

}
