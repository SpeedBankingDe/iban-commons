package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanIsValid;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanOf;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanParse;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanTryParse;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanTryParseOrNull;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanTryParseValue;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.iban.IbanAssertions.IbanAssert;
import de.speedbanking.util.Currency;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the custom AssertJ assertions providing full coverage.
 */
@SuppressWarnings("checkstyle:MethodName")
final class IbanAssertionsTest {

    private static final String VALID_CY = "CY17002001280000001200527600";
    private static final String VALID_GL = "GL8964710001000206";

    @DisplayName("Should cover all factory entry points")
    @Test
    void entryPoints_ShouldSucceed_WhenValidInputIsProvided() {
        assertThatIbanOf(VALID_CY).hasCountryCode("CY");
        assertThatIbanParse(VALID_CY).hasCountryCode("CY");

        // Test Optional and Nullable variants
        assertThatIbanTryParse(VALID_CY).isPresent();
        assertThatIbanTryParseValue(VALID_CY).hasNormalizedValue(VALID_CY);
        assertThatIbanTryParseOrNull(VALID_CY).hasNormalizedValue(VALID_CY);

        // Failure cases for entry points to cover the Error-Throwing branches
        assertThatThrownBy(() -> assertThatIbanTryParseValue("INVALID"))
            .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertThatIbanTryParseOrNull("INVALID"))
            .isInstanceOf(AssertionError.class);
    }

    @DisplayName("Should cover metadata assertions")
    @Test
    void metadataAssertions_ShouldVerifyProperties_WhenIbanIsParsed() {
        Iban iban = Iban.of(VALID_CY);
        assertThat(iban)
            .hasFormattedString("CY17 0020 0128 0000 0012 0052 7600")
            .hasComponentString("CY 17 002 00128 0000001200527600")
            .hasCountryName("Cyprus")
            .hasCountryFlag("🇨🇾")
            .hasCurrency(Currency.EUR)
            .hasCurrencyCode(Currency.EUR.getAlphaCode())
            .isSepa(true)
            .hasCheckDigits("17")
            .hasCheckDigits(17)
            .hasBban("002001280000001200527600")
            .hasAccountNumber("0000001200527600")
            .hasOrganisation("Central Bank of Cyprus");
    }

    @DisplayName("Should cover comparison assertions")
    @Test
    void comparisonAssertions_ShouldWork_WhenIbansAreCompared() {
        Iban cyIban = Iban.of(VALID_CY);
        Iban glIban = Iban.of(VALID_GL);

        assertThat(cyIban)
            .isLessThan(glIban)
            .isLessThanOrEqualTo(glIban)
            .isLessThanOrEqualTo(cyIban)
            .isGreaterThanOrEqualTo(cyIban)
            .isEqualByCompareTo(Iban.of(VALID_CY));

        assertThat(glIban).isGreaterThan(cyIban);
    }

    @DisplayName("Should cover failure messages for coverage")
    @Test
    void failureMessages_ShouldBeCorrect_WhenAssertionsFail() {
        Iban iban = Iban.of(VALID_CY);

        IbanAssert ibanAssertions = assertThat(iban);

        assertThatThrownBy(() -> ibanAssertions.hasCountryCode("GL"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected country code to be 'GL' but was 'CY'");

        assertThatThrownBy(() -> ibanAssertions.hasLength(10))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected IBAN length to be 10");
    }

    @DisplayName("Should support SoftAssertions via 'using' proxy")
    @Test
    void softAssertions_ShouldSucceed_WhenUsingProxy() {
        SoftAssertions softly = new SoftAssertions();
        Iban iban = Iban.of(VALID_CY);

        IbanAssertions.using(iban, softly)
            .hasCountryCode("CY")
            .isSepa(true);

        assertThatCode(softly::assertAll).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "IBAN {0} validity should be {1}")
    @CsvSource(delimiter = '|', value = {
        "DE89370400440532013000 | true",
        "INVALID                | false"
    })
    void validity_ShouldBeReflected_WhenInputIsChecked(String input, boolean expected) {
        assertThatIbanIsValid(input).isEqualTo(expected);
    }
}

