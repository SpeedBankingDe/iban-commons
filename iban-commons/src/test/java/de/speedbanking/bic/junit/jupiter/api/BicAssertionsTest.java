package de.speedbanking.bic.junit.jupiter.api;

import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThat;
import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThatBicIsValid;
import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThatBicOf;
import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThatInvalidBicException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.bic.Bic;
import de.speedbanking.bic.BicValidationError;
import de.speedbanking.bic.InvalidBicException;
import de.speedbanking.bic.junit.jupiter.api.BicAssertions.BicAssert;
import de.speedbanking.util.Currency;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the custom AssertJ assertions providing full coverage for BicAssertions.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BicAssertionsTest {

    private static final String VALID_BIC8 = "MARKDEFF";
    private static final String VALID_BIC11 = "MARKDEFF500";
    private static final String OTHER_BIC = "DEUTDEFFXXX";

    @DisplayName("Should cover all factory entry points")
    @Test
    void entryPoints_ShouldSucceed_WhenValidInputIsProvided() {
        assertThatBicOf(VALID_BIC8).isBic8().hasBankCode("MARK");
        assertThatBicOf(VALID_BIC11).isBic11().hasBranchCode("500");

        // Failure cases for entry points
        assertThatInvalidBicException()
            .isThrownBy(() -> assertThatBicOf("INVALID_LENGTH"));
    }

    @DisplayName("Should cover type and length assertions")
    @Test
    void typeAssertions_ShouldVerifyBicKind_WhenChecked() {
        Bic bic8 = Bic.of(VALID_BIC8);
        Bic bic11 = Bic.of(VALID_BIC11);

        assertThat(bic8).isBic8().isNotBic11().hasLength(8);
        assertThat(bic11).isBic11().isNotBic8().hasLength(11);
    }

    @DisplayName("Should cover metadata assertions")
    @Test
    void metadataAssertions_ShouldVerifyProperties_WhenBicIsParsed() {
        Bic bic = Bic.of(VALID_BIC8);
        assertThat(bic)
            .hasToString(VALID_BIC8)
            .hasBic8(VALID_BIC8)
            .hasBic11(VALID_BIC8 + "XXX")
            .hasBankCode("MARK")
            .hasCountryCode("DE")
            .hasCountryName("Germany")
            .hasCountryFlag("🇩🇪")
            .hasCurrency(Currency.EUR)
            .hasCurrencyCode("EUR")
            .hasLocationCode("FF")
            .hasNoBranchCode();

        assertThat(Bic.of(VALID_BIC11)).hasBranchCode("500");
    }

    @DisplayName("Should cover comparison assertions")
    @Test
    void comparisonAssertions_ShouldWork_WhenBicsAreCompared() {
        Bic bicA = Bic.of(VALID_BIC8); // MARK...
        Bic bicB = Bic.of(OTHER_BIC); // DEUT...

        assertThat(bicB)
            .isLessThan(bicA)
            .isLessThanOrEqualTo(bicA)
            .isLessThanOrEqualTo(bicB)
            .isGreaterThanOrEqualTo(bicB)
            .isEqualByCompareTo(Bic.of(OTHER_BIC));

        assertThat(bicA).isGreaterThan(bicB);
    }

    @DisplayName("Should cover failure messages for coverage")
    @Test
    void failureMessages_ShouldBeCorrect_WhenAssertionsFail() {
        Bic bic = Bic.of(VALID_BIC8);
        BicAssert bicAssert = assertThat(bic);

        assertThatThrownBy(() -> bicAssert.hasCountryCode("US"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC country code to be 'US' but was 'DE' for BIC '%s'", bic);

        assertThatThrownBy(() -> bicAssert.isBic11())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC to be BIC-11 (length 11) but was BIC-8 (length 8)");
    }

    @DisplayName("Should support SoftAssertions via 'using' proxy")
    @Test
    void softAssertions_ShouldSucceed_WhenUsingProxy() {
        SoftAssertions softly = new SoftAssertions();
        Bic bic = Bic.of(VALID_BIC11);

        BicAssertions.using(bic, softly)
            .hasCountryCode("DE")
            .isBic11();

        assertThatCode(softly::assertAll).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "BIC {0} validity should be {1}")
    @CsvSource(delimiter = '|', value = {
        "MARKDEFF    | true",
        "MARKDEFF500 | true",
        "TOO_SHORT   | false"
    })
    void validity_ShouldBeReflected_WhenInputIsChecked(String input, boolean expected) {
        assertThatBicIsValid(input).isEqualTo(expected);
    }

    @DisplayName("Should cover detailed BIC component assertions")
    @Test
    void componentAssertions_ShouldVerifyEachPart() {
        // Test BIC: MARK DE FF 500
        // Bank: MARK, Country: DE, Location: FF, Branch: 500
        Bic bic = Bic.of("MARKDEFF500");

        assertThat(bic)
            .hasBankCode("MARK")
            .hasCountryCode("DE")
            .hasLocationCode("FF")
            .hasBranchCode("500")
            .isBic11();

        // Negative tests for components to trigger failWithMessage
        assertThatThrownBy(() -> assertThat(bic).hasBankCode("XXXX"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC bank code to be 'XXXX' but was 'MARK' for BIC '%s'", bic);

        assertThatThrownBy(() -> assertThat(bic).hasLocationCode("ZZ"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC location code to be 'ZZ' but was 'FF' for BIC '%s'", bic);
    }

    @DisplayName("Should cover currency and country metadata assertions")
    @Test
    void metadataAssertions_ShouldHandleCurrencyAndFlags() {
        Bic bic = Bic.of("MARKDEFF"); // Germany, EUR

        assertThat(bic)
            .hasCurrency(Currency.EUR)
            .hasCurrencyCode("EUR")
            .hasCountryFlag("🇩🇪")
            .hasCountryName("Germany");

        assertThatThrownBy(() -> assertThat(bic).hasCurrencyCode("USD"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected currency code to be 'USD' but was 'EUR' for BIC '%s'", bic.toString());
    }

    @DisplayName("Should cover branch code edge cases")
    @Test
    void branchCodeAssertions_ShouldHandlePresenceAndAbsence() {
        Bic bic8 = Bic.of("MARKDEFF");
        Bic bic11 = Bic.of("MARKDEFFXXX");

        assertThat(bic8).hasNoBranchCode();

        assertThat(bic11).hasBranchCode("XXX");

        assertThatThrownBy(() -> assertThat(bic11).hasNoBranchCode())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC branch code to be 'null' but was 'XXX' for BIC '%s'", bic11);
    }

    @DisplayName("Should cover numeric length and string representations")
    @Test
    void lengthAssertions_ShouldVerifyExactLength() {
        Bic bic = Bic.of("MARKDEFF");

        assertThat(bic)
            .hasLength(8)
            .hasBic8("MARKDEFF")
            .hasBic11("MARKDEFFXXX");

        assertThatThrownBy(() -> assertThat(bic).hasLength(11))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC length to be 11 but was 8 for BIC '%s'", bic);
    }

    @DisplayName("Should cover null and exception factory points")
    @Test
    void factoryPoints_ShouldHandleEdgeCases() {
        assertThatBicIsValid(null).isFalse();
        assertThatBicIsValid("").isFalse();
        assertThatBicIsValid("MARKDEFF").isTrue();

        assertThatInvalidBicException().isThrownBy(() -> {
                throw InvalidBicException.of(BicValidationError.EMPTY);
            }).withMessage("BIC is null or empty (%s)", BicValidationError.EMPTY);
    }

}
