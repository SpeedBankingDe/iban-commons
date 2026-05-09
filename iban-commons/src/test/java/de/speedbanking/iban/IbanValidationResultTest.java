package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JUnit test class for {@link IbanValidationResult}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IbanValidationResultTest {

    private static final String         VALID_IBAN     = "ES9121000418450200051332";
    private static final IbanRegistry   VALID_COUNTRY  = IbanRegistry.ES;

    @DisplayName("Should correctly create a valid result")
    @Test
    void valid_shouldStoreIbanAndCountryData_whenInputIsValid() {
        IbanValidationResult result = IbanValidationResult.valid(VALID_IBAN, VALID_COUNTRY);

        assertThat(result.isValid()).isTrue();
        assertThat(result.normIban).isEqualTo(VALID_IBAN);
        assertThat(result.countryData).isEqualTo(VALID_COUNTRY);
        assertThat(result.error).isNull();
    }

    @DisplayName("Should correctly create an invalid result")
    @Test
    void invalid_shouldStoreError_whenReasonIsProvided() {
        IbanValidationError testReason = IbanValidationError.EMPTY;

        IbanValidationResult result = IbanValidationResult.invalid(testReason);

        assertThat(result.isValid()).isFalse();
        assertThat(result.normIban).isNull();
        assertThat(result.countryData).isNull();
        assertThat(result.error).isEqualTo(testReason);
    }

    @DisplayName("toString() should be correctly formatted for a valid result")
    @Test
    void toString_shouldContainIban_whenResultIsValid() {
        IbanValidationResult result = IbanValidationResult.valid(VALID_IBAN, VALID_COUNTRY);
        String expected = IbanValidationResult.class.getSimpleName() + "[valid: " + VALID_IBAN + "]";

        assertThat(result).hasToString(expected);
    }

    @DisplayName("toString() should be correctly formatted for an invalid result")
    @Test
    void toString_shouldContainError_whenResultIsInvalid() {
        IbanValidationResult result = IbanValidationResult.invalid(IbanValidationError.INVALID_COUNTRY);
        String expected = IbanValidationResult.class.getSimpleName() + "[invalid: IBAN has invalid country code]";

        assertThat(result).hasToString(expected);
    }

}
