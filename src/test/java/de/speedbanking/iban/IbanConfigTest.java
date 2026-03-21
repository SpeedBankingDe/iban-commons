package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link IbanConfig}.
 */
@ResourceLock(value = Resources.SYSTEM_PROPERTIES)
class IbanConfigTest {

    @BeforeEach
    void setup() {
        IbanConfig.resetAll();
    }

    @AfterEach
    void tearDown() {
        // ensure a clean state for the next test
        IbanConfig.NCD_VALIDATE.disable();
        IbanConfig.NCD_CALCULATE.disable();
        IbanConfig.resetAll();
        System.clearProperty("iban.ncd.validate");
        System.clearProperty("iban.ncd.calculate");
    }

    @DisplayName("Default values should be false")
    @Test
    void testDefaultValues() {
        assertThat((boolean) IbanConfig.NCD_VALIDATE.get())
            .as("Default national check digit (NCD) validation")
            .isFalse();

        assertThat((boolean) IbanConfig.NCD_CALCULATE.get())
            .as("Default national check digit (NCD) calculation")
            .isFalse();
    }

    @DisplayName("Should read from system properties")
    @Test
    void testReadSystemProperty() {
        // This covers the 'sysProp != null' branch in readSystemProperty()
        System.setProperty("iban.ncd.validate", "true");

        IbanConfig.NCD_VALIDATE.reset();

        assertThat(IbanConfig.NCD_VALIDATE.isEnabled())
            .as("Value after reading system property")
            .isTrue();
    }

    @DisplayName("Should set and get NCD validation flag")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSetValidateNationalCheckDigit(boolean value) {
        IbanConfig.NCD_VALIDATE.set(value);

        assertThat((boolean) IbanConfig.NCD_VALIDATE.get())
            .isEqualTo(value);
    }

    @DisplayName("Should set and get NCD calculation flag")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSetCalculateNationalCheckDigit(boolean value) {
        IbanConfig.NCD_CALCULATE.set(value);

        assertThat(IbanConfig.NCD_CALCULATE.isEnabled())
            .isEqualTo(value);
    }

    @DisplayName("ToString should return formatted string")
    @Test
    void testToString() {
        // Covers toString() method (currently at 0.0%)
        String result = IbanConfig.NCD_VALIDATE.toString();

        assertThat(result)
            .contains("IbanConfig")
            .contains("NCD_VALIDATE")
            .contains("false");
    }

    @DisplayName("Reset should restore default values")
    @Test
    void testReset() {
        IbanConfig.NCD_VALIDATE.enable();
        IbanConfig.NCD_CALCULATE.enable();

        IbanConfig.resetAll();

        assertThat(IbanConfig.NCD_VALIDATE.isEnabled()).isFalse();
        assertThat(IbanConfig.NCD_VALIDATE.isDisabled()).isTrue();

        assertThat(IbanConfig.NCD_CALCULATE.isEnabled()).isFalse();
        assertThat(IbanConfig.NCD_CALCULATE.isDisabled()).isTrue();
    }
}
