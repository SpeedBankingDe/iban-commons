package de.speedbanking.bic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JUnit test class for {@link BicValidationResult}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class BicValidationResultTest {

    private final Bic validBic = Bic.of("PALSPS22XXX");

    @DisplayName("Should correctly create a valid result")
    @Test
    void valid_shouldStoreBic_whenBicIsValid() {
        BicValidationResult result = BicValidationResult.valid(validBic);

        assertThat(result.isValid()).isTrue();
        assertThat(result.bic).isEqualTo(validBic);
        assertThat(result.error).isNull();
    }

    @DisplayName("Should correctly create an invalid result")
    @Test
    void invalid_shouldStoreErrorDetails_whenReasonIsProvided() {
        BicValidationError testReason = BicValidationError.EMPTY;

        BicValidationResult result = BicValidationResult.invalid(testReason);

        assertThat(result.isValid()).isFalse();
        assertThat(result.bic).isNull();
        assertThat(result.error).isEqualTo(testReason);
    }

    @DisplayName("toString() should be correctly formatted for a valid result")
    @Test
    void toString_shouldContainBic_whenResultIsValid() {
        BicValidationResult result = BicValidationResult.valid(validBic);
        String expected = BicValidationResult.class.getSimpleName() + "[valid: " + validBic + "]";

        assertThat(result).hasToString(expected);
    }

    @DisplayName("toString() should be correctly formatted for an invalid result")
    @Test
    void toString_shouldContainError_whenResultIsInvalid() {
        BicValidationResult result = BicValidationResult.invalid(BicValidationError.INVALID_COUNTRY);
        String expected = BicValidationResult.class.getSimpleName() + "[invalid: BIC has invalid country code]";

        assertThat(result).hasToString(expected);
    }

}
