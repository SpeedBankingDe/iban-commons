package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.iban.NationalCheckDigitCalculators.NoOpNcdCalculator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Unit tests for {@link CountryValidator}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@ResourceLock(IbanConfigTest.RESOURCE_NAME)
final class CountryValidatorTest {

    @AfterEach
    void resetConfig() {
        IbanConfig.reset();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void validateIban_registryExample_returnsTrue(IbanRegistry countryData) {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).build());

        assertThat(IbanValidator.getCountryValidator(countryData).validateIban(countryData.getIbanExample()))
            .as("Validation should succeed for %s using example '%s'", countryData, countryData.getIbanExample())
            .isTrue();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void validateIban_bbanOfSpaces_returnsFalse(IbanRegistry countryData) {
        String format = "%s%-" + (countryData.getIbanLength() - 2) + "s";
        String badIban = String.format(format, countryData.name(), "0");

        assertThat(IbanValidator.getCountryValidator(countryData).validateIban(badIban))
            .as("Validation should fail for %s using '%s'", countryData, badIban)
            .isFalse();
    }

    @ParameterizedTest(name = "[{index}] {0}[{1}] = ''{2}'': {4}")
    @CsvSource(delimiter = '|', value = {
        "BG |  4 | 1 | false | digit in alphabetic segment",
        "BG |  8 | A | false | letter in numeric segment",
        "BG | 14 | A | true  | letter in alphanumeric segment",
        "BG | 14 | _ | true  | alphanumeric segment among others is not checked",
        "GT | 10 | _ | false | single alphanumeric segment is checked as a whole",
        "DE | 21 | A | false | letter in last position of numeric segment",
    })
    void validateIban_modifiedCharacter_returnsExpected(IbanRegistry countryData, int index, char ch, boolean expected, String reason) {
        char[] iban = countryData.getIbanExample().toCharArray();
        iban[index] = ch;

        assertThat(IbanValidator.getCountryValidator(countryData).validateIban(iban))
            .as(reason)
            .isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] {0}: {1} -> {2}")
    @CsvSource(delimiter = '|', value = {
        "4!a4!n     | XX00ABCD1234 | true",
        "4!a4!n     | XX00ABC11234 | false",
        "4!a4!n     | XX00ABCD123A | false",
        "2!n2!n     | XX001234     | true",
        "2!n2!n     | XX00123A     | false",
        "4!c        | XX00AB12     | true",
        "4!c        | XX00ab12     | false",
        "2!c2!n     | XX00ab12     | true",
        "2!a2!c2!a  | XX00AB12CD   | true",
        "2!a2!c2!a  | XX00AB12C1   | false",
    })
    void validateIban_customPattern_returnsExpected(String bbanPattern, String iban, boolean expected) {
        // DE has no NCD, so only the structure derived from the pattern is checked
        CountryValidator validator = new CountryValidator(IbanRegistry.DE, bbanPattern);

        assertThat(validator.validateIban(iban)).isEqualTo(expected);
    }

    @Test
    void constructor_spaceSegment_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new CountryValidator(IbanRegistry.DE, "8!n2!e8!n"))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported space segment in BBAN pattern 8!n2!e8!n of DE");
    }

    static Stream<IbanRegistry> ncdCountriesWithAlgorithm() {
        return IbanRegistry.ALL_COUNTRIES.stream()
            .filter(IbanRegistry::hasNationalCheckDigit)
            .filter(countryData -> !hasNoOpNcdCalculator(countryData));
    }

    static Stream<IbanRegistry> ncdCountriesWithoutAlgorithm() {
        return IbanRegistry.ALL_COUNTRIES.stream()
            .filter(IbanRegistry::hasNationalCheckDigit)
            .filter(CountryValidatorTest::hasNoOpNcdCalculator);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("ncdCountriesWithAlgorithm")
    void validateIban_invalidNcd_returnsFalse(IbanRegistry countryData) {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).build());
        char[] badNcdIban = withAlteredNcd(countryData);

        assertThat(IbanValidator.getCountryValidator(countryData).validateIban(badNcdIban))
            .as("Validation should fail for %s using IBAN with invalid NCD '%s'", countryData, new String(badNcdIban))
            .isFalse();
    }

    // the SWIFT IBAN Registry defines an NCD position for these countries, but no algorithm
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("ncdCountriesWithoutAlgorithm")
    void validateIban_alteredNcdWithoutAlgorithm_returnsTrue(IbanRegistry countryData) {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).build());
        char[] alteredNcdIban = withAlteredNcd(countryData);

        assertThat(IbanValidator.getCountryValidator(countryData).validateIban(alteredNcdIban))
            .as("Validation should succeed for %s using IBAN with altered NCD '%s'", countryData, new String(alteredNcdIban))
            .isTrue();
    }

    /**
     * Returns whether the country has an NCD position but no algorithm to check it.
     */
    private static boolean hasNoOpNcdCalculator(IbanRegistry countryData) {
        return IbanValidator.getCountryValidator(countryData).getNcdCalculator() instanceof NoOpNcdCalculator;
    }

    /**
     * Returns the country's example IBAN with every NCD character replaced by another one of the same kind.
     */
    private static char[] withAlteredNcd(IbanRegistry countryData) {
        char[] iban = countryData.getIbanExample().toCharArray();
        IbanComponent ncdComponent = countryData.getNationalCheckDigitComponent();
        for (int idx = ncdComponent.getBeginIndex(); idx < ncdComponent.getEndIndex(); idx++) {
            char c = iban[idx];
            if (Character.isDigit(c)) {
                iban[idx] = c == '0' ? '1' : '0';
            } else {
                iban[idx] = c == 'Z' ? 'A' : (char) (c + 1);
            }
        }
        return iban;
    }

    @Test
    void validateNationalCheckDigit_validationDisabled_returnsTrue() {
        IbanConfig.reset(IbanConfig.builder().validateNcd(false).build());

        // NCD 33 replaced by 00
        assertThat(IbanValidator.getCountryValidator(IbanRegistry.PT).validateNationalCheckDigit("PT50002700000001234567800".toCharArray()))
            .isTrue();
    }

    @Test
    void validateNationalCheckDigit_countryWithoutNcd_returnsTrue() {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).build());

        assertThat(IbanValidator.getCountryValidator(IbanRegistry.DE).validateNationalCheckDigit(new char[0]))
            .isTrue();
    }

    @ParameterizedTest(name = "[{index}] calculateNcd={0}")
    @CsvSource(delimiter = '|', value = {
        "true  | 33",
        "false | 00",
    })
    void calculateNationalCheckDigit_ncdPlaceholder_returnsExpected(boolean calculateNcd, String expected) {
        IbanConfig.reset(IbanConfig.builder().calculateNcd(calculateNcd).build());

        assertThat(IbanValidator.getCountryValidator(IbanRegistry.PT).calculateNationalCheckDigit("PT50002700000001234567800".toCharArray()))
            .containsExactly(expected.toCharArray());
    }

    @Test
    void calculateNationalCheckDigit_countryWithoutNcd_throwsIllegalStateException() {
        CountryValidator validator = IbanValidator.getCountryValidator(IbanRegistry.DE);

        assertThat(validator.getNcdCalculator()).isNull();
        assertThatThrownBy(() -> validator.calculateNationalCheckDigit(IbanRegistry.DE.getIbanExample().toCharArray()))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("DE has no national check digit");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void toString_anyCountry_returnsBaseCountryCodeAndName(IbanRegistry countryData) {
        IbanRegistry baseCountry = countryData.getBaseCountry();

        assertThat(IbanValidator.getCountryValidator(countryData))
            .hasToString("%s[%s]", baseCountry.getCountryCode(), baseCountry.getCountryName());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void getCountryData_anyCountry_returnsBaseCountry(IbanRegistry countryData) {
        assertThat(IbanValidator.getCountryValidator(countryData).getCountryData())
            .isSameAs(countryData.getBaseCountry());
    }

}
