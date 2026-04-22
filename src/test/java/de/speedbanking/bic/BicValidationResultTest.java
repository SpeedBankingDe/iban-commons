package de.speedbanking.bic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * JUnit test class for {@link BicValidationResult}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class BicValidationResultTest {

    private final Bic validBic = Bic.of("PALSPS22XXX");

    private Constructor<BicValidationResult> getPrivateConstructor() throws NoSuchMethodException {
        Constructor<BicValidationResult> constructor =
            BicValidationResult.class.getDeclaredConstructor(CharSequence.class, BicValidationError.class);
        constructor.setAccessible(true);
        return constructor;
    }

    @DisplayName("Private constructor should throw exception if both arguments are NULL")
    @Test
    void privateConstructor_shouldThrowIfBothNull() throws Exception {
        Constructor<BicValidationResult> constructor = getPrivateConstructor();

        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(() -> constructor.newInstance(null, null))
            .withCauseInstanceOf(IllegalArgumentException.class)
            .extracting(Throwable::getCause)
            .extracting(Throwable::getMessage)
            .isEqualTo("BIC result must contain either a valid BIC or a validation error");
    }

    @DisplayName("Private constructor should throw exception if both arguments are NOT NULL")
    @Test
    void privateConstructor_shouldThrowIfBothNotNull() throws Exception {
        Constructor<BicValidationResult> constructor = getPrivateConstructor();

        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(() -> constructor.newInstance(validBic, BicValidationError.EMPTY))
            .withCauseInstanceOf(IllegalArgumentException.class)
            .extracting(Throwable::getCause)
            .extracting(Throwable::getMessage)
            .isEqualTo("BIC result cannot contain both a valid BIC and a validation error");
    }

    @DisplayName("Instantiation failures (factory methods)")
    @Test
    void instantiation_fails_on_invalid_input() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> BicValidationResult.valid(null))
            .withMessage("Valid result requires a BIC");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> BicValidationResult.invalid(null))
            .withMessage("Invalid result requires a validation error object");
    }

    @DisplayName("Should correctly create a valid result")
    @Test
    void valid_result_is_correctly_created() {
        BicValidationResult result = BicValidationResult.valid(validBic);

        assertThat(result.isValid()).isTrue();

        assertThat(result.getBic())
            .as("getBic() should return an Optional containing the Bic object")
            .isPresent()
            .contains(validBic);

        assertThat(result.getError()).isEmpty();
    }

    @DisplayName("Should correctly create an invalid result")
    @Test
    void invalid_result_stores_error_details() {
        BicValidationError testReason = BicValidationError.EMPTY;

        BicValidationResult result = BicValidationResult.invalid(testReason);

        assertThat(result.isValid()).isFalse();

        assertThat(result.getBic()).isEmpty();

        assertThat(result.getError())
            .as("getError() should return an Optional containing the ValidationError")
            .isPresent()
            .contains(testReason);
    }

    @DisplayName("toString() should be correctly formatted for a valid result")
    @Test
    void to_string_contains_bic_when_valid() {
        BicValidationResult result = BicValidationResult.valid(validBic);
        String expected = BicValidationResult.class.getSimpleName() + "[valid: " + validBic + "]";

        assertThat(result).hasToString(expected);
    }

    @DisplayName("toString() should be correctly formatted for an invalid result")
    @Test
    void to_string_contains_error_when_invalid() {
        BicValidationResult result = BicValidationResult.invalid(BicValidationError.INVALID_COUNTRY);
        String expected = BicValidationResult.class.getSimpleName() + "[invalid: BIC has invalid country code]";

        assertThat(result).hasToString(expected);
    }

}
