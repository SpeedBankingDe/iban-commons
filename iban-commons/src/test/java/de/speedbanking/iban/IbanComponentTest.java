package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * JUnit test class for {@link IbanComponent}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IbanComponentTest {

    @DisplayName("getValidationError: should return associated non-null validation error for every component")
    @ParameterizedTest
    @EnumSource(IbanComponent.class)
    void getValidationError_shouldReturnNonNullValidationError(IbanComponent component) {
        IbanValidationError error = component.getValidationError();

        assertThat(error).isNotNull();
    }

    @DisplayName("getValidationError: should map specific components to expected IbanValidationError enum constants")
    @Test
    void getValidationError_shouldMapToExpectedErrors() {
        assertThat(IbanComponent.BBAN.getValidationError())
            .isEqualTo(IbanValidationError.INVALID_BBAN);

        assertThat(IbanComponent.BANK_CODE.getValidationError())
            .isEqualTo(IbanValidationError.INVALID_BANK_CODE);

        assertThat(IbanComponent.BRANCH_CODE.getValidationError())
            .isEqualTo(IbanValidationError.INVALID_BRANCH_CODE);

        assertThat(IbanComponent.ACCOUNT_NUMBER.getValidationError())
            .isEqualTo(IbanValidationError.INVALID_ACCOUNT_NUMBER);
    }

    @DisplayName("getPattern: should extract corresponding pattern string from registry")
    @Test
    void getPattern_shouldExtractPatternFromRegistry() {
        IbanRegistry registryDe = IbanRegistry.DE;

        assertThat(IbanComponent.BBAN.getPattern(registryDe))
            .isEqualTo(registryDe.getBbanPattern());

        assertThat(IbanComponent.BANK_CODE.getPattern(registryDe))
            .isEqualTo(registryDe.getBankCodePattern());

        assertThat(IbanComponent.ACCOUNT_NUMBER.getPattern(registryDe))
            .isEqualTo(registryDe.getAccountNumberPattern());

        // Germany does not define a separate branch code pattern
        assertThat(IbanComponent.BRANCH_CODE.getPattern(registryDe))
            .isNull();

        IbanRegistry registryMc = IbanRegistry.MC;
        assertThat(IbanComponent.BRANCH_CODE.getPattern(registryMc))
            .isEqualTo(registryMc.getBranchCodePattern());
    }

    @DisplayName("getPattern: passing null registry should throw NullPointerException")
    @Test
    void getPattern_nullRegistry_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> IbanComponent.BBAN.getPattern(null))
            .withMessage("registry must not be null");
    }

    @DisplayName("valueOf: should correctly resolve component by enum constant name")
    @Test
    void valueOf_shouldReturnCorrectEnumConstant() {
        assertThat(IbanComponent.valueOf("BBAN")).isEqualTo(IbanComponent.BBAN);
        assertThat(IbanComponent.valueOf("BANK_CODE")).isEqualTo(IbanComponent.BANK_CODE);
        assertThat(IbanComponent.valueOf("BRANCH_CODE")).isEqualTo(IbanComponent.BRANCH_CODE);
        assertThat(IbanComponent.valueOf("ACCOUNT_NUMBER")).isEqualTo(IbanComponent.ACCOUNT_NUMBER);
    }

}
