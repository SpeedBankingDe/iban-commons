package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.iban.NationalCheckDigitCalculators.NoOpNcdCalculatorBase;
import de.speedbanking.test.TestUtil;
import de.speedbanking.util.IndexRange;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
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
    void testAllValidators(IbanRegistry countryData) {
        CountryValidator validator = IbanValidator.getCountryValidator(countryData);

        assertThat(validator)
            .as("Validator for %s should not be null", countryData)
            .isNotNull();

        // trigger validation to cover the internal class logic
        // use a dummy IBAN that matches the country code to reach the internal validation code
        CharSequence badIban = countryData.name() + String.format("%" + (countryData.getIbanLength() - countryData.name().length()) + "s", "0");

        assertThat(validator.validateIban(badIban))
            .as("Validation should fail for %s using bad iban '%s'", countryData.name(), badIban)
            .isFalse();

        CharSequence goodIban = countryData.getIbanExample();

        assertThat(validator.validateIban(goodIban))
            .as("Validation should succeed for %s using good iban '%s'", countryData.name(), goodIban)
            .isTrue();
    }

    @DisplayName("Should provide correct toString implementation")
    @ParameterizedTest
    @IbanRegistrySource
    void testToString(IbanRegistry countryData) {
        CountryValidator validator = IbanValidator.getCountryValidator(countryData);

        assertThat(validator.toString())
            .as("toString for %s should follow the pattern getClass().getSimpleName() + [...]", countryData.name())
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
    @ResourceLock(value = IbanConfigTest.RESOURCE_NAME)
    void testAllNcdValidators(IbanRegistry countryData) {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).calculateNcd(true).build());

        try {

            assertThat(IbanValidator.getCountryValidator(countryData))
                .as("Validator for %s should be an NCD calculator", countryData)
                .isInstanceOf(NationalCheckDigitCalculator.class)
                .isInstanceOf(AbstractNcdCountryValidator.class);

            AbstractNcdCountryValidator validator = (AbstractNcdCountryValidator) IbanValidator.getCountryValidator(countryData);

            String goodIban = countryData.getIbanExample();

            assertThat(validator.validateIban(goodIban))
                .as("Validation should succeed for %s using good iban '%s'", countryData.name(), goodIban)
                .isTrue();

            Assumptions.assumeFalse(validator.getNcdCalculator() instanceof NoOpNcdCalculatorBase);

            StringBuilder badNcdIban = new StringBuilder(countryData.getIbanExample());
            IndexRange ncdIndexRange = countryData.getNationalCheckDigitIndexRange();
            for (int idx = ncdIndexRange.getBegin(); idx < ncdIndexRange.getEnd(); idx++) {
                char c = badNcdIban.charAt(idx);
                badNcdIban.setCharAt(idx, c == '0' ? '1' : '0');
            }
            IbanValidator.fixCheckDigits(badNcdIban);

            assertThat(validator.validateIban(badNcdIban))
                .as("Validation should fail for %s using iban with invalid NCD '%s'", countryData.name(), badNcdIban)
                .isFalse();

        } finally {
            IbanConfig.reset();
        }

    }

}
