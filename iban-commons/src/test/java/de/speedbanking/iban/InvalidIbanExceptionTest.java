package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit test class for {@link InvalidIbanException}.
 * <p>
 * Ensures the exception correctly stores reason, input, and country code, and formats
 * exception messages and string representations properly.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class InvalidIbanExceptionTest {

    @DisplayName("of(reason) should store reason and derive message from reason text")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanValidationError.class)
    void of_reasonOnly_initializesFields(IbanValidationError reason) {
        InvalidIbanException exception = InvalidIbanException.of(reason);

        assertThat(exception)
            .isNotNull()
            .isInstanceOf(RuntimeException.class)
            .as("Exception message must contain the reason's failure text")
            .hasMessage("%s (%s)", reason.getText(), reason)
            .hasToString("InvalidIbanException[reason=" + reason + ", input='null']");

        assertThat(exception.getReason())
            .as("getReason() should return the original ValidationError")
            .isEqualTo(reason);

        assertThat(exception.getCountryCode())
            .as("getCountryCode() should be null when omitted")
            .isNull();
    }

    @DisplayName("of(reason) should yield no input")
    @Test
    void of_noInput_hasNoInput() {
        InvalidIbanException exception = InvalidIbanException.of(IbanValidationError.EMPTY);

        assertThat(exception.getInput())
            .as("getInput() should be null when no input was supplied")
            .isNull();
    }

    @DisplayName("of(reason, input) should store both reason and input")
    @Test
    void of_reasonAndInput_storesBoth() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        InvalidIbanException exception = InvalidIbanException.of(reason, input);

        assertThat(exception.getReason()).isEqualTo(reason);

        assertThat(exception.getInput())
            .as("getInput() should return the supplied input")
            .isEqualTo(input);

        assertThat(exception.getCountryCode()).isNull();

        assertThat(exception)
            .hasToString("InvalidIbanException[reason=%s, input='%s']", reason, input);
    }

    @DisplayName("of(reason, input, countryCode) should store reason, input, and upper-cased countryCode")
    @Test
    void of_reasonInputAndCountryCode_storesAll() {
        IbanValidationError reason = IbanValidationError.INVALID_BANK_CODE;
        String input = "DE00123456789012345678";
        String countryCode = "de";

        InvalidIbanException exception = InvalidIbanException.of(reason, input, countryCode);

        String expectedCountryCode = "DE";

        assertThat(exception.getReason()).isEqualTo(reason);
        assertThat(exception.getInput()).isEqualTo(input);
        assertThat(exception.getCountryCode())
            .as("country code should be normalized to upper case")
            .isEqualTo(expectedCountryCode);

        assertThat(exception)
            .hasMessage("%s (%s), country %s: '%s'", reason.getText(), reason, expectedCountryCode, input)
            .hasToString("%s[reason=%s, country=%s, input='%s']",
                InvalidIbanException.class.getSimpleName(), reason, expectedCountryCode, input);
    }

    @DisplayName("of(reason, input, countryCode) should normalize null or blank countryCode to null")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void of_nullOrBlankCountryCode_normalizesToNull(String countryCode) {
        InvalidIbanException exception = InvalidIbanException.of(IbanValidationError.EMPTY, "DE00123", countryCode);

        assertThat(exception.getCountryCode()).isNull();
    }

    @DisplayName("of(null) should throw NullPointerException")
    @Test
    void of_nullReason_throwsException() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidIbanException.of(null))
            .withMessage("reason required");
    }

    @DisplayName("of(null, input) should throw NullPointerException")
    @Test
    void of_nullReasonWithInput_throwsException() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> InvalidIbanException.of(null, "DE00123456789012345678"))
            .withMessage("reason required");
    }

    @DisplayName("equals() should be true for same reason, input, and countryCode")
    @Test
    void equals_sameReasonInputAndCountryCode_returnsTrue() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";
        String countryCode = "DE";

        InvalidIbanException a = InvalidIbanException.of(reason, input, countryCode);
        InvalidIbanException b = InvalidIbanException.of(reason, input, countryCode);

        assertThat(a)
            .isEqualTo(b)
            .hasSameHashCodeAs(b);
    }

    @DisplayName("equals() should be false when countryCode differs")
    @Test
    void equals_differentCountryCode_returnsFalse() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";

        InvalidIbanException withCountry = InvalidIbanException.of(reason, input, "DE");
        InvalidIbanException withoutCountry = InvalidIbanException.of(reason, input, null);

        assertThat(withCountry).isNotEqualTo(withoutCountry);
    }

    @DisplayName("equals() should be false when input differs")
    @Test
    void equals_differentInput_returnsFalse() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        InvalidIbanException withInput = InvalidIbanException.of(reason, "DE00123456789012345678");
        InvalidIbanException withoutInput = InvalidIbanException.of(reason);

        assertThat(withInput).isNotEqualTo(withoutInput);
    }

    @DisplayName("equals() should be false for different reasons")
    @Test
    void equals_differentReason_returnsFalse() {
        IbanValidationError[] errors = IbanValidationError.values();
        org.junit.jupiter.api.Assumptions.assumeTrue(errors.length >= 2,
            "Need at least two IbanValidationError values for this test");

        InvalidIbanException a = InvalidIbanException.of(errors[0]);
        InvalidIbanException b = InvalidIbanException.of(errors[1]);

        assertThat(a).isNotEqualTo(b);
    }

    @DisplayName("equals() should be reflexive")
    @Test
    @SuppressWarnings("SelfAssertion")
    void equals_sameInstance_returnsTrue() {
        InvalidIbanException ex = InvalidIbanException.of(IbanValidationError.EMPTY);
        assertThat(ex).isEqualTo(ex);
    }

    @DisplayName("equals() should return false for null and other types")
    @Test
    void equals_nullOrOtherType_returnsFalse() {
        InvalidIbanException ex = InvalidIbanException.of(IbanValidationError.EMPTY);
        assertThat(ex)
            .isNotNull()
            .isNotEqualTo("some string");
    }

    @DisplayName("toString() should include class name, reason, countryCode, and input")
    @Test
    void toString_allPartsPresent_containsAllParts() {
        IbanValidationError reason = IbanValidationError.EMPTY;
        String input = "DE00123456789012345678";
        String countryCode = "DE";

        String result = InvalidIbanException.of(reason, input, countryCode).toString();

        assertThat(result).isEqualTo("%s[reason=%s, country=%s, input='%s']",
            InvalidIbanException.class.getSimpleName(), reason, countryCode, input);
    }

    @DisplayName("toString() without countryCode and input")
    @Test
    void toString_reasonOnly_omitsCountryCode() {
        IbanValidationError reason = IbanValidationError.EMPTY;

        String result = InvalidIbanException.of(reason).toString();

        assertThat(result)
            .isEqualTo("InvalidIbanException[reason=EMPTY, input='null']");
    }

}
