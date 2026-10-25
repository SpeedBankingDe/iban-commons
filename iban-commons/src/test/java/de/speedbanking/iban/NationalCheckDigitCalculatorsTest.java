package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.iban.NationalCheckDigitCalculators.NcdCalculatorBase;
import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class NationalCheckDigitCalculatorsTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void constructor_shouldBePrivate() {
        TestUtil.assertConstructorIsPrivate(NationalCheckDigitCalculators.class);
    }

    static Stream<IbanRegistry> ncdCountries() {
        return IbanRegistry.ALL_COUNTRIES.stream()
            .filter(IbanRegistry::hasNationalCheckDigit);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("ncdCountries")
    void forCountry_ncdCountry_returnsCalculatorOfBaseCountry(IbanRegistry countryData) {
        NcdCalculatorBase calculator = (NcdCalculatorBase) NationalCheckDigitCalculators.forCountry(countryData);

        assertThat(calculator.countryData).isSameAs(countryData.getBaseCountry());
        assertThat(calculator.ncdComponent).isEqualTo(countryData.getBaseCountry().getNationalCheckDigitComponent());
        assertThat(calculator).hasToString("%s[%s, NCD %s]",
            calculator.name(), countryData.getBaseCountry().getCountryCode(), calculator.ncdComponent);
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "FR | FR",
        "GP | FR",
        "MC | FR",
        "SN | FR",
        "IT | IT",
        "SM | IT",
        "PT | Mod97RemainderOneNcdCalculator",
        "SE | NoOpNcdCalculator",
    })
    void forCountry_sharedAlgorithm_returnsAlgorithmClass(IbanRegistry countryData, String algorithm) {
        assertThat(((NcdCalculatorBase) NationalCheckDigitCalculators.forCountry(countryData)).name())
            .isEqualTo(algorithm);
    }

    @Test
    void constructor_countryWithoutNcd_throwsNullPointerException() {
        assertThatThrownBy(() -> new NationalCheckDigitCalculators.NoOpNcdCalculator(IbanRegistry.DE))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessage("DE has no national check digit");
    }

    @Test
    void constructor_nullCountry_throwsNullPointerException() {
        assertThatThrownBy(() -> new NationalCheckDigitCalculators.NoOpNcdCalculator(null))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessage("countryData must not be null");
    }

    @Test
    void forCountry_countryWithoutNcd_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> NationalCheckDigitCalculators.forCountry(IbanRegistry.DE))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("No national check digit algorithm for DE");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' -> {1}")
    @CsvSource(delimiter = '|', value = {
        "0 | 0",
        "9 | 9",
        "A | 1",
        "I | 9",
        "J | 1",
        "R | 9",
        "S | 2",
        "Z | 9",
        "a | 0",
        "- | 0",
    })
    void getFrenchRIBValue_character_returnsRibValue(char ch, int expected) {
        assertThat(NationalCheckDigitCalculators.getFrenchRIBValue(ch)).isEqualTo(expected);
    }
}
