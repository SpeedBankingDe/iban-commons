package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.util.Iso3166Alpha2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Tests for {@link AbstractNcdCountryValidator}.
 * <p>
 * ensures correct delegation to the NCD calculator and reflection-based loading.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@ResourceLock(IbanConfigTest.RESOURCE_NAME)
final class AbstractNcdCountryValidatorTest {

    /**
     * Concrete implementation for testing.
     * <p>
     * name matches {@link IbanRegistry#PT}.
     */
    static final class PT extends AbstractNcdCountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return true;
        }
    }

    /**
     * Implementation with no matching class in {@link NationalCheckDigitCalculators}.
     */
    static final class XYZ extends AbstractNcdCountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return false;
        }
    }

    @BeforeEach
    void prepareConfig() {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).calculateNcd(true).build());
    }

    @AfterEach
    void resetConfig() {
        IbanConfig.reset();
    }

    /**
     * Verifies that an {@link ExceptionInInitializerError} is thrown when the NCD calculator
     * class cannot be loaded/instantiated, provided that NCD validation is enabled.
     */
    @DisplayName("loadNcdCalculator() should throw ExceptionInInitializerError on invalid country code")
    @Test
    void loadNcdCalculator_shouldThrowException_whenClassDoesNotExist() {
        assertThatThrownBy(() -> AbstractNcdCountryValidator.loadNcdCalculator(XYZ.class))
            .isExactlyInstanceOf(ExceptionInInitializerError.class)
            .hasMessageContaining("Could not load ncd calculator class");
    }

    @DisplayName("validateNationalCheckDigit should delegate to calculator when enabled")
    @Test
    void validateNationalCheckDigit_shouldDelegate_whenEnabled() {
        AbstractNcdCountryValidator validator = new PT();
        String iban = "PT50000201231234567890154";

        assertThat(validator.validateNationalCheckDigit(iban))
            .isEqualTo(validator.validateNationalCheckDigit(iban.toCharArray()))
            .isTrue();
    }

    @DisplayName("validateNationalCheckDigit should return true when validation is disabled")
    @Test
    void validateNationalCheckDigit_shouldReturnTrue_whenDisabled() {
        IbanConfig.reset(IbanConfig.builder().validateNcd(false).build());

        AbstractNcdCountryValidator validator = new PT();
        assertThat(validator.validateNationalCheckDigit(new char[0])).isTrue();
    }

    @DisplayName("calculateNationalCheckDigit should return range when calculation is disabled")
    @Test
    void calculateNationalCheckDigit_shouldReturnRange_whenDisabled() {
        IbanConfig.reset(IbanConfig.builder().calculateNcd(false).build());

        AbstractNcdCountryValidator validator = new PT();
        String iban = "PT50002700000001234567833";
        assertThat(validator.calculateNationalCheckDigit(iban)).contains('3', '3');
    }

    @DisplayName("calculateNationalCheckDigit should return calculated value when enabled")
    @Test
    void calculateNationalCheckDigit_shouldDelegateToCalculator_whenEnabled() {
        AbstractNcdCountryValidator validator = new PT();
        CharSequence iban = "PT50002700000001234567833";

        char[] result = validator.calculateNationalCheckDigit(iban);

        assertThat(result).isNotNull();
    }

    @DisplayName("loadNcdCalculator should throw IllegalStateException on failure if config requires it")
    @Test
    void loadNcdCalculator_shouldThrowException_onMissingClass() {
        assertThatThrownBy(XYZ::new)
            .isExactlyInstanceOf(ExceptionInInitializerError.class)
            .hasMessage("'XYZ' is not a supported IBAN country code");
    }

    @DisplayName("getNcdCalculator should return the instance loaded via reflection")
    @Test
    void getNcdCalculator_shouldReturnLoadedInstance() {
        AbstractNcdCountryValidator validator = new PT();

        assertThat(validator.getNcdCalculator())
            .isNotNull()
            .isInstanceOf(NationalCheckDigitCalculator.class);
    }

    @DisplayName("toString should still function correctly via inheritance")
    @Test
    void toString_shouldIncludeCountryName() {
        AbstractNcdCountryValidator validator = new PT();

        assertThat(validator.toString())
            .contains("PT[Portugal]")
            .contains(IbanRegistry.PT.getCountryName())
            .contains(Iso3166Alpha2.PT.getCountryName());
    }
}

