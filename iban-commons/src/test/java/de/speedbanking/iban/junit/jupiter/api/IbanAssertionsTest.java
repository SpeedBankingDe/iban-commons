package de.speedbanking.iban.junit.jupiter.api;

import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThat;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanIsValid;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanOf;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanParse;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanTryParse;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanTryParseValue;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.iban.Iban;
import de.speedbanking.iban.junit.jupiter.api.IbanAssertions.IbanAssert;
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

        // Failure cases for entry points to cover the Error-Throwing branches
        assertThatThrownBy(() -> assertThatIbanTryParseValue("INVALID"))
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
            .hasMessage("Expected country code to be 'GL' but was 'CY' for IBAN '%s'", iban);

        assertThatThrownBy(() -> ibanAssertions.hasLength(10))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected IBAN length to be 10 but was 28 for IBAN '%s'", iban);
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

    @DisplayName("Should cover regex matches assertion")
    @Test
    void matches_ShouldSucceed_WhenRegexIsValid() {
        Iban iban = Iban.of(VALID_CY);

        // success cases
        assertThat(iban).matches("CY.*");
        assertThat(iban).matches((CharSequence) null);
        assertThat(iban).matches("");

        // failure case
        String regex = "DE.*";
        assertThatThrownBy(() -> {
            assertThat(iban).matches(regex);
        })
            .isInstanceOf(AssertionError.class)
            .hasMessage("IBAN '%s' does not match pattern '%s'", iban, regex);
    }

    @DisplayName("Should cover all parsing factory points")
    @Test
    void parsingFactories_ShouldHandleVariousInputs() {
        String validRaw = "DE89370400440532013000";

        assertThatIbanParse(validRaw).hasCountryCode("DE");

        assertThatIbanTryParse(validRaw).isPresent();
        assertThatIbanTryParse("INVALID").isEmpty();

        assertThatIbanTryParseValue(validRaw).hasToString(validRaw);
        assertThatThrownBy(() -> assertThatIbanTryParseValue("INVALID"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected Iban.tryParse(\"INVALID\") to return a non-empty Optional, but it was empty");
    }

    @DisplayName("Should cover currency and SEPA metadata")
    @Test
    void metadataAssertions_ShouldVerifyIbanProperties() {
        Iban iban = Iban.of(VALID_CY);

        assertThat(iban)
            .hasCurrency(Currency.EUR)
            .isSepa(true);

        assertThatThrownBy(() -> assertThat(iban).isSepa(false))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected SEPA participation to be 'false' for IBAN '%s'", iban);

        assertThatThrownBy(() -> assertThat(iban).hasCurrency(Currency.USD))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected currency to be 'USD' but was 'EUR' for IBAN '%s'", iban);
    }

    @DisplayName("Should cover comparison edge cases")
    @Test
    void comparisonAssertions_ShouldHandleAllBounds() {
        Iban lower = Iban.of("DK5000400440116243");
        Iban higher = Iban.of("GL8964710001000206");

        assertThat(lower).isLessThan(higher);
        assertThat(higher).isGreaterThan(lower);
        assertThat(lower).isLessThanOrEqualTo(lower);
        assertThat(higher).isGreaterThanOrEqualTo(higher);

        assertThatThrownBy(() -> assertThat(higher).isLessThan(lower))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected IBAN '%s' to be less than '%s'", higher, lower);
    }

    @DisplayName("Should verify null handling in assertions")
    @Test
    void nullHandling_ShouldThrowAssertionError() {
        Iban iban = Iban.of(VALID_CY);

        assertThatThrownBy(() -> assertThat((Iban) null).hasCountryCode("DE"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expecting actual not to be null");

        assertThatThrownBy(() -> assertThat(iban).isGreaterThan(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("The IBAN to compare against must not be null");
    }

}

