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

    @DisplayName("Should respect relaxed validation settings")
    @Test
    void testRelaxedSettings() {
        IbanConfig.ALLOW_SPACE.enable();
        IbanConfig.ALLOW_LOWERCASE.enable();

        assertThat(IbanConfig.ALLOW_SPACE.isEnabled()).isTrue();
        assertThat(IbanConfig.ALLOW_LOWERCASE.isEnabled()).isTrue();
    }

    @DisplayName("Should read from system properties")
    @Test
    void testReadSystemProperty() {
        System.setProperty("iban.ncd.validate", "true");

        IbanConfig.NCD_VALIDATE.reset();

        assertThat(IbanConfig.NCD_VALIDATE.isEnabled())
            .as("Value after reading system property")
            .isTrue();
    }

    @DisplayName("Should apply parser when reading system property")
    @Test
    void testSystemPropertyParsing() {
        // Testet den Pfad: sysProp != null -> parser.apply(sysProp)
        System.setProperty("iban.allow.space", "true");
        IbanConfig.ALLOW_SPACE.reset();
        assertThat(IbanConfig.ALLOW_SPACE.isEnabled()).isTrue();

        System.setProperty("iban.allow.space", "false");
        IbanConfig.ALLOW_SPACE.reset();
        assertThat(IbanConfig.ALLOW_SPACE.isEnabled()).isFalse();

        // Testet ungültigen Input für den Boolean-Parser (ergibt false)
        System.setProperty("iban.allow.space", "not-a-boolean");
        IbanConfig.ALLOW_SPACE.reset();
        assertThat(IbanConfig.ALLOW_SPACE.isEnabled()).isFalse();
    }

    @DisplayName("Should set and get NCD validation flag")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSetValidateNationalCheckDigit(boolean value) {
        IbanConfig.NCD_VALIDATE.set(value);

        assertThat((boolean) IbanConfig.NCD_VALIDATE.get()).isEqualTo(value);
    }

    @DisplayName("Should set and get NCD calculation flag")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSetCalculateNationalCheckDigit(boolean value) {
        IbanConfig.NCD_CALCULATE.set(value);

        assertThat(IbanConfig.NCD_CALCULATE.isEnabled()).isEqualTo(value);
    }

    @DisplayName("IsDisabled should return inverse of isEnabled")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIsDisabled(boolean value) {
        IbanConfig.NCD_VALIDATE.set(value);
        assertThat(IbanConfig.NCD_VALIDATE.isDisabled()).isNotEqualTo(value);
    }

    @DisplayName("ToString should return formatted string")
    @Test
    void testToString() {
        String result = IbanConfig.NCD_VALIDATE.toString();

        assertThat(result)
            .contains("IbanConfig")
            .contains("NCD_VALIDATE")
            .contains("false");
    }

    @DisplayName("Reset should restore default values")
    @Test
    void testResetAll() {
        IbanConfig.NCD_VALIDATE.enable();
        IbanConfig.NCD_CALCULATE.enable();
        IbanConfig.ALLOW_SPACE.enable();
        IbanConfig.ALLOW_LOWERCASE.enable();

        IbanConfig.resetAll();

        assertThat(IbanConfig.NCD_VALIDATE.isEnabled()).isFalse();
        assertThat(IbanConfig.NCD_VALIDATE.isDisabled()).isTrue();

        assertThat(IbanConfig.NCD_CALCULATE.isEnabled()).isFalse();
        assertThat(IbanConfig.NCD_CALCULATE.isDisabled()).isTrue();

        assertThat(IbanConfig.ALLOW_SPACE.isEnabled()).isFalse();
        assertThat(IbanConfig.ALLOW_SPACE.isDisabled()).isTrue();

        assertThat(IbanConfig.ALLOW_LOWERCASE.isEnabled()).isFalse();
        assertThat(IbanConfig.ALLOW_LOWERCASE.isDisabled()).isTrue();
    }
}
