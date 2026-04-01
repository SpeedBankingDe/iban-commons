package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.iban.NationalCheckDigitCalculators.NoOpNcdCalculatorBase;
import de.speedbanking.test.TestUtil;
import de.speedbanking.util.IndexRange;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Unit tests for {@link CountryValidators}.
 */
class CountryValidatorsTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void testConstructorIsPrivate() {
        TestUtil.assertConstructorIsPrivate(CountryValidators.class);
    }

    @DisplayName("Should instantiate and invoke validator for each registry entry")
    @ParameterizedTest
    @IbanRegistrySource
    void testAllValidators(IbanRegistry country) {
        CountryValidator validator = country.getCountryValidator();

        assertThat(validator)
            .as("Validator for %s should not be null", country)
            .isNotNull();

        // trigger validation to cover the internal class logic
        // use a dummy IBAN that matches the country code to reach the internal validation code
        CharSequence badIban = country.name() + String.format("%" + (country.getIbanLength() - country.name().length()) + "s", "0");

        assertThat(validator.validateIban(badIban))
            .as("Validation should fail for %s using bad iban '%s'", country.name(), badIban)
            .isFalse();

        CharSequence goodIban = country.getIbanExample();

        assertThat(validator.validateIban(goodIban))
            .as("Validation should succeed for %s using good iban '%s'", country.name(), goodIban)
            .isTrue();
    }

    @DisplayName("Should provide correct toString implementation")
    @ParameterizedTest
    @IbanRegistrySource
    void testToString(IbanRegistry country) {
        CountryValidator validator = country.getCountryValidator();

        assertThat(validator.toString())
            .as("toString for %s should follow the pattern getClass().getSimpleName() + [...]", country.name())
            .contains(validator.getClass().getSimpleName())
            .contains("[")
            .contains("]");
    }

    static Stream<IbanRegistry> allNcdIbanRegistryEntries() {
        return Arrays.stream(IbanRegistry.values())
            .filter(IbanRegistry::hasNationalCheckDigit);
    }

    @DisplayName("Should instantiate and invoke all NCD validators")
    @ParameterizedTest
    @MethodSource("allNcdIbanRegistryEntries")
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES)
    void testAllNcdValidators(IbanRegistry country) {
        IbanConfig.NCD_VALIDATE.enable();
        IbanConfig.NCD_CALCULATE.enable();

        try {

            assertThat(country.getCountryValidator())
                .as("Validator for %s should be an NCD calculator", country)
                .isInstanceOf(NationalCheckDigitCalculator.class)
                .isInstanceOf(AbstractNcdCountryValidator.class);

            AbstractNcdCountryValidator validator = (AbstractNcdCountryValidator) country.getCountryValidator();

            String goodIban = country.getIbanExample();

            assertThat(validator.validateIban(goodIban))
                .as("Validation should succeed for %s using good iban '%s'", country.name(), goodIban)
                .isTrue();

            Assumptions.assumeFalse(validator.getNcdCalculator() instanceof NoOpNcdCalculatorBase);

            StringBuilder badNcdIban = new StringBuilder(country.getIbanExample());
            IndexRange ncdIndexRange = country.getNationalCheckDigitIndexRange();
            for (int idx = ncdIndexRange.getBegin(); idx < ncdIndexRange.getEnd(); idx++) {
                char c = badNcdIban.charAt(idx);
                badNcdIban.setCharAt(idx, c == '0' ? '1' : '0');
            }
            IbanValidator.fixCheckDigits(badNcdIban);

            assertThat(validator.validateIban(badNcdIban.toString()))
                .as("Validation should fail for %s using iban with invalid NCD '%s'", country.name(), badNcdIban)
                .isFalse();

        } finally {
            IbanConfig.resetAll();
        }

    }

}
