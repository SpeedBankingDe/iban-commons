package de.speedbanking.iban.junit.jupiter.api;

import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIban;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanIsValid;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatInvalidIbanException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanValidationError;
import de.speedbanking.iban.InvalidIbanException;
import de.speedbanking.util.Currency;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.regex.Pattern;

/**
 * Tests for the custom AssertJ assertions providing full coverage.
 */
@SuppressWarnings("checkstyle:MethodName")
final class IbanAssertionsTest {

    private static final String VALID_CY = "CY17002001280000001200527600";
    private static final String VALID_GL = "GL8964710001000206";
    // DE has: bankCode, no branchCode, no nationalCheckDigit
    private static final String VALID_DE = "DE89370400440532013000";
    // GB has: bankCode, branchCode (sort code), no nationalCheckDigit
    private static final String VALID_GB = "GB29NWBK60161331926819";
    // NO has: bankCode, no branchCode, nationalCheckDigit
    private static final String VALID_NO = "NO9386011117947";

    @DisplayName("assertThatIban(CharSequence) - valid string parses successfully")
    @Test
    @SuppressWarnings("UnnecessaryStringBuilder")
    void assertThat_charSequence_validString_succeeds() {
        assertThatIban(VALID_CY).hasCountryCode("CY");
        assertThatIban((CharSequence) new StringBuilder(VALID_CY)).hasCountryCode("CY");
    }

    @DisplayName("assertThatIban(CharSequence) - null is forwarded as-is; isNull() succeeds")
    @Test
    void assertThat_charSequence_null_isNullSucceeds() {
        assertThatIban((CharSequence) null).isNull();
    }

    @DisplayName("assertThatIban(CharSequence) - invalid string throws AssertionError wrapping InvalidIbanException")
    @Test
    void assertThat_charSequence_invalidString_throwsAssertionError() {
        String iban = "INVALID";
        assertThatThrownBy(() -> assertThatIban(iban))
            .isInstanceOf(AssertionError.class)
            .hasMessage("%s (%s): '%s'", IbanValidationError.INCORRECT_LENGTH.getText(),
                IbanValidationError.INCORRECT_LENGTH.name(), iban)
            .hasCauseInstanceOf(InvalidIbanException.class);
    }

    @DisplayName("assertThatIban(Iban) - pre-parsed instance is wrapped directly")
    @Test
    void assertThat_iban_wrapsDirectly() {
        Iban iban = Iban.of(VALID_CY);
        assertThatIban(iban).hasCountryCode("CY");
    }

    @DisplayName("assertThatIban(Iban) - null is forwarded as-is; isNull() succeeds")
    @Test
    void assertThat_iban_null_isNullSucceeds() {
        assertThatIban(null).isNull();
    }

    @DisplayName("assertThat(Iban) - static factory overload wraps a pre-parsed Iban directly")
    @Test
    void assertThat_iban_staticOverload_wrapsDirectly() {
        Iban iban = Iban.of(VALID_CY);
        IbanAssertions.assertThat(iban).hasCountryCode("CY");
    }

    @DisplayName("assertThat(Iban) - static factory overload: null is forwarded as-is; isNull() succeeds")
    @Test
    void assertThat_iban_staticOverload_null_isNullSucceeds() {
        IbanAssertions.assertThat((Iban) null).isNull();
    }

    @DisplayName("Metadata assertions - all structural properties of a Cyprus IBAN")
    @Test
    void metadataAssertions_cyprus_allProperties() {
        assertThatIban(VALID_CY)
            .hasNormalizedValue(VALID_CY)
            .hasFormattedString("CY17 0020 0128 0000 0012 0052 7600")
            .hasComponentString("CY 17 002 00128 0000001200527600")
            .hasLength(28)
            .hasCountryCode("CY")
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

    @DisplayName("hasBankCode - success path")
    @Test
    void hasBankCode_success() {
        assertThatIban(VALID_DE).hasBankCode("37040044");
    }

    @DisplayName("hasBranchCode - success path for IBAN with branch code (GB)")
    @Test
    void hasBranchCode_present_success() {
        assertThatIban(VALID_GB).hasBranchCode("601613");
    }

    @DisplayName("hasBranchCode - null asserts absence of branch code (DE)")
    @Test
    void hasBranchCode_absent_success() {
        assertThatIban(VALID_DE).hasBranchCode(null);
    }

    @DisplayName("hasBankAndBranchCode - success path (GB)")
    @Test
    void hasBankAndBranchCode_success() {
        assertThatIban(VALID_GB).hasBankAndBranchCode("NWBK601613");
    }

    @DisplayName("hasNationalCheckDigit - success path for IBAN with NCD (NO)")
    @Test
    void hasNationalCheckDigit_present_success() {
        assertThatIban(VALID_NO).hasNationalCheckDigit("7");
    }

    @DisplayName("hasNationalCheckDigit - null asserts absence of NCD (DE)")
    @Test
    void hasNationalCheckDigit_absent_success() {
        assertThatIban(VALID_DE).hasNationalCheckDigit(null);
    }

    @DisplayName("Failure messages - country code mismatch")
    @Test
    void failureMessage_countryCode_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        String expectedCountryCode = "GL";
        assertThatThrownBy(() -> assertThatIban(iban).hasCountryCode(expectedCountryCode))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected country code to be '%s' but was '%s' for IBAN '%s'", expectedCountryCode, iban.getCountryCode(), iban);
    }

    @DisplayName("Failure messages - length mismatch")
    @Test
    void failureMessage_length_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        int expectedLength = 10;
        assertThatThrownBy(() -> assertThatIban(iban).hasLength(expectedLength))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected IBAN length to be %d but was %d for IBAN '%s'",
                expectedLength, iban.length(), iban);
    }

    @DisplayName("Failure messages - SEPA mismatch")
    @Test
    void failureMessage_sepa_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        boolean expectedSepa = false;
        assertThatThrownBy(() -> {
            assertThatIban(iban).isSepa(expectedSepa);
        })
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected SEPA participation to be '%s' but was '%s' for IBAN '%s'",
                expectedSepa, !expectedSepa, iban);
    }

    @DisplayName("Failure messages - currency mismatch")
    @Test
    void failureMessage_currency_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        Currency expectedCcy = Currency.USD;
        assertThatThrownBy(() -> assertThatIban(iban).hasCurrency(expectedCcy))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected currency to be '%s' but was '%s' for IBAN '%s'",
                expectedCcy.getAlphaCode(), iban.getCurrencyCode(), iban);
    }

    @DisplayName("Failure messages - normalizedValue mismatch")
    @Test
    void failureMessage_normalizedValue_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasNormalizedValue("XX00000"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected normalized IBAN to be 'XX00000'");
    }

    @DisplayName("Failure messages - formattedString mismatch")
    @Test
    void failureMessage_formattedString_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasFormattedString("WRONG"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected formatted IBAN to be 'WRONG'");
    }

    @DisplayName("Failure messages - componentString mismatch")
    @Test
    void failureMessage_componentString_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasComponentString("WRONG"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected component string to be 'WRONG'");
    }

    @DisplayName("Failure messages - countryName mismatch")
    @Test
    void failureMessage_countryName_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasCountryName("Germany"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected country name to be 'Germany'");
    }

    @DisplayName("Failure messages - countryFlag mismatch")
    @Test
    void failureMessage_countryFlag_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasCountryFlag("🇩🇪"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected country flag to be '🇩🇪'");
    }

    @DisplayName("Failure messages - currencyCode mismatch")
    @Test
    void failureMessage_currencyCode_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasCurrencyCode("USD"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected currency code to be 'USD'");
    }

    @DisplayName("Failure messages - checkDigits (String) mismatch")
    @Test
    void failureMessage_checkDigits_string_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasCheckDigits("99"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected check digits to be '99'");
    }

    @DisplayName("Failure messages - bban mismatch")
    @Test
    void failureMessage_bban_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasBban("000000"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected BBAN to be '000000'");
    }

    @DisplayName("Failure messages - accountNumber mismatch")
    @Test
    void failureMessage_accountNumber_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasAccountNumber("0000000000"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected account number to be '0000000000'");
    }

    @DisplayName("Failure messages - organisation mismatch")
    @Test
    void failureMessage_organisation_mismatch() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).hasOrganisation("Deutsche Bundesbank"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected organisation to be 'Deutsche Bundesbank'");
    }

    @DisplayName("Failure messages - bankCode mismatch")
    @Test
    void failureMessage_bankCode_mismatch() {
        Iban iban = Iban.of(VALID_DE);
        assertThatThrownBy(() -> assertThatIban(iban).hasBankCode("00000000"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected bank code to be '00000000'");
    }

    @DisplayName("Failure messages - branchCode mismatch")
    @Test
    void failureMessage_branchCode_mismatch() {
        Iban iban = Iban.of(VALID_GB);
        assertThatThrownBy(() -> assertThatIban(iban).hasBranchCode("000000"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected branch code to be '000000'");
    }

    @DisplayName("Failure messages - bankAndBranchCode mismatch")
    @Test
    void failureMessage_bankAndBranchCode_mismatch() {
        Iban iban = Iban.of(VALID_GB);
        assertThatThrownBy(() -> assertThatIban(iban).hasBankAndBranchCode("WRONG"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected bank and branch code to be 'WRONG'");
    }

    @DisplayName("Failure messages - nationalCheckDigit mismatch")
    @Test
    void failureMessage_nationalCheckDigit_mismatch() {
        Iban iban = Iban.of(VALID_NO);
        assertThatThrownBy(() -> assertThatIban(iban).hasNationalCheckDigit("9"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected national check digit to be '9'");
    }

    @ParameterizedTest(name = "IBAN {0} validity should be {1}")
    @CsvSource(delimiter = '|', value = {
        "DE89370400440532013000 | true",
        "INVALID                | false"
    })
    void assertThatIbanIsValid_reflectsValidity(String input, boolean expected) {
        assertThatIbanIsValid(input).isEqualTo(expected);
    }

    @DisplayName("assertThatInvalidIbanException - scoped to InvalidIbanException")
    @Test
    void assertThatInvalidIbanException_catchesInvalidIban() {
        assertThatInvalidIbanException()
            .isThrownBy(() -> Iban.of("INVALID"))
            .withMessageContaining("INVALID");
    }

    @DisplayName("matches(CharSequence) - matching, null, and empty regex")
    @Test
    void matches_charSequence_successCases() {
        assertThatIban(VALID_CY).matches("^CY.+");
        assertThatIban(VALID_CY).matches((CharSequence) null);
        assertThatIban(VALID_CY).matches("");
    }

    @DisplayName("matches(CharSequence) - non-matching regex fails with descriptive message")
    @Test
    void matches_charSequence_mismatch_fails() {
        Iban iban = Iban.of(VALID_CY);
        String regex = "DE.*";
        assertThatThrownBy(() -> assertThatIban(iban).matches(regex))
            .isInstanceOf(AssertionError.class)
            .hasMessage("IBAN '%s' does not match pattern '%s'", iban, regex);
    }

    @DisplayName("matches(Pattern) - matching pattern succeeds")
    @Test
    void matches_pattern_success() {
        assertThatIban(VALID_CY).matches(Pattern.compile("^CY.+"));
    }

    @DisplayName("matches(Pattern) - null pattern skips the check")
    @Test
    void matches_pattern_null_skips() {
        assertThatIban(VALID_CY).matches((Pattern) null);
    }

    @DisplayName("matches(Pattern) - non-matching pattern fails with descriptive message")
    @Test
    void matches_pattern_mismatch_fails() {
        Iban iban = Iban.of(VALID_CY);
        Pattern pattern = Pattern.compile("DE.*");
        assertThatThrownBy(() -> assertThatIban(iban).matches(pattern))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("does not match pattern");
    }

    @DisplayName("Comparison assertions")
    @Test
    void comparisonAssertions_ordering() {
        Iban cyIban = Iban.of(VALID_CY);
        Iban glIban = Iban.of(VALID_GL);

        assertThatIban(cyIban)
            .isLessThan(glIban)
            .isLessThanOrEqualTo(glIban)
            .isLessThanOrEqualTo(cyIban)
            .isGreaterThanOrEqualTo(cyIban)
            .isEqualByCompareTo(Iban.of(VALID_CY));

        assertThatIban(glIban).isGreaterThan(cyIban);
    }

    @DisplayName("Comparison assertions - boundary cases with DK and GL")
    @Test
    void comparisonAssertions_boundaries() {
        Iban lower  = Iban.of("DK5000400440116243");
        Iban higher = Iban.of(VALID_GL);

        assertThatIban(lower).isLessThan(higher);
        assertThatIban(higher).isGreaterThan(lower);
        assertThatIban(lower).isLessThanOrEqualTo(lower);
        assertThatIban(higher).isGreaterThanOrEqualTo(higher);
    }

    @DisplayName("Comparison assertions - isLessThan fails with descriptive message")
    @Test
    void comparisonAssertions_isLessThan_fail() {
        Iban lower  = Iban.of("DK5000400440116243");
        Iban higher = Iban.of(VALID_GL);
        assertThatThrownBy(() -> assertThatIban(higher).isLessThan(lower))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected IBAN '%s' to be less than '%s'", higher, lower);
    }

    @DisplayName("Comparison assertions - isLessThanOrEqualTo fails when actual is greater")
    @Test
    void comparisonAssertions_isLessThanOrEqualTo_fail() {
        Iban lower  = Iban.of("DK5000400440116243");
        Iban higher = Iban.of(VALID_GL);
        assertThatThrownBy(() -> assertThatIban(higher).isLessThanOrEqualTo(lower))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected IBAN '%s' to be less than or equal to '%s'", higher, lower);
    }

    @DisplayName("Comparison assertions - isGreaterThan fails with descriptive message")
    @Test
    void comparisonAssertions_isGreaterThan_fail() {
        Iban lower  = Iban.of("DK5000400440116243");
        Iban higher = Iban.of(VALID_GL);
        assertThatThrownBy(() -> assertThatIban(lower).isGreaterThan(higher))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected IBAN '%s' to be greater than '%s'", lower, higher);
    }

    @DisplayName("Comparison assertions - isGreaterThanOrEqualTo fails when actual is less")
    @Test
    void comparisonAssertions_isGreaterThanOrEqualTo_fail() {
        Iban lower  = Iban.of("DK5000400440116243");
        Iban higher = Iban.of(VALID_GL);
        assertThatThrownBy(() -> assertThatIban(lower).isGreaterThanOrEqualTo(higher))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected IBAN '%s' to be greater than or equal to '%s'", lower, higher);
    }

    @DisplayName("Comparison assertions - isEqualByCompareTo fails when IBANs differ")
    @Test
    void comparisonAssertions_isEqualByCompareTo_fail() {
        Iban cyIban = Iban.of(VALID_CY);
        Iban glIban = Iban.of(VALID_GL);
        assertThatThrownBy(() -> assertThatIban(cyIban).isEqualByCompareTo(glIban))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expected IBAN")
            .hasMessageContaining("to compare as equal to");
    }

    @DisplayName("Comparison assertions - null argument throws NullPointerException")
    @Test
    void comparisonAssertions_nullArgument_throwsNpe() {
        Iban iban = Iban.of(VALID_CY);
        assertThatThrownBy(() -> assertThatIban(iban).isGreaterThan(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("The IBAN to compare against must not be null");
    }

    @DisplayName("SoftAssertions - using() proxy collects failures without throwing immediately")
    @Test
    void softAssertions_using_proxy() {
        SoftAssertions softly = new SoftAssertions();
        Iban iban = Iban.of(VALID_CY);

        IbanAssertions.using(iban, softly)
            .hasCountryCode("CY")
            .isSepa(true);

        assertThatCode(softly::assertAll).doesNotThrowAnyException();
    }

    @DisplayName("SoftAssertions - using() proxy collects failure without throwing immediately")
    @Test
    void softAssertions_using_proxy_collectsFailure() {
        SoftAssertions softly = new SoftAssertions();
        Iban iban = Iban.of(VALID_CY);

        IbanAssertions.using(iban, softly)
            .hasCountryCode("DE"); // wrong - should be collected, not thrown

        assertThatThrownBy(softly::assertAll)
            .isInstanceOf(AssertionError.class);
    }

}
