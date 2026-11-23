package de.speedbanking.bic.junit.jupiter.api;

import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThat;
import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThatBic;
import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThatBicIsNotValid;
import static de.speedbanking.bic.junit.jupiter.api.BicAssertions.assertThatBicIsValid;
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

import java.util.regex.Pattern;

/**
 * Tests for the custom AssertJ assertions providing full coverage for BicAssertions.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BicAssertionsTest {

    private static final String VALID_BIC8_MARKDEFF     = "MARKDEFF";
    private static final String VALID_BIC11_MARKDEFF500 = "MARKDEFF500";
    private static final String OTHER_BIC_DEUTDEFFXXX   = "DEUTDEFFXXX";

    @DisplayName("Should cover all factory entry points - happy paths")
    @Test
    void entryPoints_validInput_succeed() {
        assertThatBic(VALID_BIC8_MARKDEFF)
            .isBic8()
            .hasBankCode("MARK");
        assertThatBic(VALID_BIC11_MARKDEFF500)
            .isBic11()
            .hasBranchCode("500");

        assertThatInvalidBicException()
            .isThrownBy(() -> Bic.of("INVALID_LENGTH"));
    }

    @DisplayName("assertThatBic(null) should return an assert whose isNull() succeeds")
    @Test
    void assertThatBic_null_handlesNull() {
        assertThatBic(null).isNull();
    }

    @DisplayName("assertThatBic should wrap InvalidBicException in AssertionError for an invalid BIC")
    @Test
    void assertThatBic_invalidBic_throwsAssertionError() {
        assertThatThrownBy(() -> assertThatBic("TOOLONG_BIC_VALUE"))
            .isInstanceOf(AssertionError.class)
            .hasCauseInstanceOf(InvalidBicException.class);
    }

    @DisplayName("BicAssertions can be instantiated via its inherited public constructor")
    @Test
    void bicAssertions_constructor_isReachable() {
        BicAssertions instance = new BicAssertions();
        assertThat(instance).isNotNull();
    }

    @DisplayName("Should verify BIC-8 / BIC-11 type and length")
    @Test
    void typeAssertions_bic8AndBic11_verifyKind() {
        Bic bic8  = Bic.of(VALID_BIC8_MARKDEFF);
        Bic bic11 = Bic.of(VALID_BIC11_MARKDEFF500);

        assertThat(bic8)
            .isBic8()
            .isNotBic11()
            .hasLength(8);
        assertThat(bic11)
            .isBic11()
            .isNotBic8()
            .hasLength(11);
    }

    @DisplayName("isBic8 should fail with message when BIC is BIC-11")
    @Test
    void isBic8_bic11_fails() {
        assertThatThrownBy(() -> assertThat(Bic.of(VALID_BIC11_MARKDEFF500)).isBic8())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC to be BIC-8 (length %d) but was BIC-11 (length %d)", Bic.BIC8_LENGTH, Bic.BIC11_LENGTH);
    }

    @DisplayName("isNotBic8 should fail with message when BIC is BIC-8")
    @Test
    void isNotBic8_bic8_fails() {
        assertThatThrownBy(() -> assertThat(Bic.of(VALID_BIC8_MARKDEFF)).isNotBic8())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC to be BIC-11 (length %d) but was BIC-8 (length %d)", Bic.BIC11_LENGTH, Bic.BIC8_LENGTH);
    }

    @DisplayName("isNotBic11 should fail with message when BIC is BIC-11")
    @Test
    void isNotBic11_bic11_fails() {
        assertThatThrownBy(() -> assertThat(Bic.of(VALID_BIC11_MARKDEFF500)).isNotBic11())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC to be BIC-8 (length %d) but was BIC-11 (length %d)", Bic.BIC8_LENGTH, Bic.BIC11_LENGTH);
    }

    @DisplayName("Should verify all metadata properties for a BIC-8")
    @Test
    void metadataAssertions_parsedBic_verifyProperties() {
        assertThatBic(VALID_BIC8_MARKDEFF)
            .hasToString(VALID_BIC8_MARKDEFF)
            .hasBic8(VALID_BIC8_MARKDEFF)
            .hasBic11(VALID_BIC8_MARKDEFF + "XXX")
            .hasBankCode("MARK")
            .hasCountryCode("DE")
            .hasCountryName("Germany")
            .hasCountryFlag("\uD83C\uDDE9\uD83C\uDDEA")
            .hasCurrency(Currency.EUR)
            .hasCurrencyCode("EUR")
            .hasLocationCode("FF")
            .hasNoBranchCode();

        assertThatBic(VALID_BIC11_MARKDEFF500)
            .hasBranchCode("500");
    }

    @DisplayName("hasToString should fail with message on mismatch")
    @Test
    void hasToString_mismatch_fails() {
        assertThatThrownBy(() -> assertThatBic(VALID_BIC8_MARKDEFF).hasToString("WRONGBIC"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC toString() to be 'WRONGBIC' but was '%s'", VALID_BIC8_MARKDEFF);
    }

    @DisplayName("hasBic8 should fail with message on mismatch")
    @Test
    void hasBic8_mismatch_fails() {
        String bic = VALID_BIC8_MARKDEFF;
        assertThatThrownBy(() -> assertThatBic(bic).hasBic8("WRONGBIC"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC-8 to be 'WRONGBIC' but was '%s' for BIC '%s'", bic, bic);
    }

    @DisplayName("hasBic11 should fail with message on mismatch")
    @Test
    void hasBic11_mismatch_fails() {
        String bic = VALID_BIC8_MARKDEFF;
        assertThatThrownBy(() -> assertThatBic(bic).hasBic11("WRONGBICXXX"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC-11 to be 'WRONGBICXXX' but was '%sXXX' for BIC '%s'", bic, bic);
    }

    @DisplayName("hasCountryName should fail with message on mismatch")
    @Test
    void hasCountryName_mismatch_fails() {
        String bic = VALID_BIC8_MARKDEFF;
        assertThatThrownBy(() -> assertThatBic(bic).hasCountryName("France"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected country name to be 'France' but was 'Germany' for BIC '%s'", bic);
    }

    @DisplayName("hasCountryFlag should fail with message on mismatch")
    @Test
    void hasCountryFlag_mismatch_fails() {
        String bic = VALID_BIC8_MARKDEFF;
        assertThatThrownBy(() -> assertThatBic(bic).hasCountryFlag("🇫🇷"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC country flag to be 🇫🇷 but was 🇩🇪 for BIC 'MARKDEFF'");
    }

    @DisplayName("hasCurrency should fail with message on mismatch")
    @Test
    void hasCurrency_mismatch_fails() {
        Bic bic = Bic.of(VALID_BIC8_MARKDEFF); // Germany -> EUR
        assertThatThrownBy(() -> assertThat(bic).hasCurrency(Currency.GBP))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected currency to be 'GBP' but was 'EUR' for BIC '%s'", bic);
    }

    @DisplayName("Should verify all comparison assertions for ordered BICs")
    @Test
    void comparisonAssertions_bics_work() {
        Bic bicA = Bic.of(VALID_BIC8_MARKDEFF);   // MARK... - lexicographically greater
        Bic bicB = Bic.of(OTHER_BIC_DEUTDEFFXXX); // DEUT...

        assertThat(bicB)
            .isLessThan(bicA)
            .isLessThanOrEqualTo(bicA)
            .isLessThanOrEqualTo(bicB)
            .isGreaterThanOrEqualTo(bicB)
            .isEqualByCompareTo(Bic.of(OTHER_BIC_DEUTDEFFXXX));

        assertThat(bicA)
            .isGreaterThan(bicB)
            .isGreaterThanOrEqualTo(bicA);
    }

    @DisplayName("isLessThan should fail with message when actual is not strictly less")
    @Test
    void isLessThan_actualGreaterOrEqual_fails() {
        Bic bicA = Bic.of(VALID_BIC8_MARKDEFF);
        Bic bicB = Bic.of(OTHER_BIC_DEUTDEFFXXX);
        assertThatThrownBy(() -> assertThat(bicA).isLessThan(bicB))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC '%s' to be less than '%s'", bicA, bicB);
    }

    @DisplayName("isLessThan should throw NullPointerException when 'other' is null")
    @Test
    void isLessThan_null_throwsNpe() {
        assertThatThrownBy(() -> assertThat(Bic.of(VALID_BIC8_MARKDEFF)).isLessThan(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("The BIC to compare against must not be null");
    }

    @DisplayName("isLessThanOrEqualTo should fail with message when actual is strictly greater")
    @Test
    void isLessThanOrEqualTo_actualGreater_fails() {
        Bic bicA = Bic.of(VALID_BIC8_MARKDEFF);
        Bic bicB = Bic.of(OTHER_BIC_DEUTDEFFXXX);
        assertThatThrownBy(() -> assertThat(bicA).isLessThanOrEqualTo(bicB))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC '%s' to be less than or equal to '%s'", bicA, bicB);
    }

    @DisplayName("isLessThanOrEqualTo should throw NullPointerException when 'other' is null")
    @Test
    void isLessThanOrEqualTo_null_throwsNpe() {
        assertThatThrownBy(() -> assertThat(Bic.of(VALID_BIC8_MARKDEFF)).isLessThanOrEqualTo(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("The BIC to compare against must not be null");
    }

    @DisplayName("isGreaterThan should fail with message when actual is not strictly greater")
    @Test
    void isGreaterThan_actualLessOrEqual_fails() {
        Bic bicA = Bic.of(VALID_BIC8_MARKDEFF);
        Bic bicB = Bic.of(OTHER_BIC_DEUTDEFFXXX);
        assertThatThrownBy(() -> assertThat(bicB).isGreaterThan(bicA))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC '%s' to be greater than '%s'", bicB, bicA);
    }

    @DisplayName("isGreaterThan should throw NullPointerException when 'other' is null")
    @Test
    void isGreaterThan_null_throwsNpe() {
        assertThatThrownBy(() -> assertThatBic(VALID_BIC8_MARKDEFF).isGreaterThan(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("The BIC to compare against must not be null");
    }

    @DisplayName("isGreaterThanOrEqualTo should fail with message when actual is strictly less")
    @Test
    void isGreaterThanOrEqualTo_actualLess_fails() {
        Bic bicA = Bic.of(VALID_BIC8_MARKDEFF);
        Bic bicB = Bic.of(OTHER_BIC_DEUTDEFFXXX);
        assertThatThrownBy(() -> assertThat(bicB).isGreaterThanOrEqualTo(bicA))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC '%s' to be greater than or equal to '%s'", bicB, bicA);
    }

    @DisplayName("isGreaterThanOrEqualTo should throw NullPointerException when 'other' is null")
    @Test
    void isGreaterThanOrEqualTo_null_throwsNpe() {
        assertThatThrownBy(() -> assertThatBic(VALID_BIC8_MARKDEFF).isGreaterThanOrEqualTo(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("The BIC to compare against must not be null");
    }

    /**
     * The failure path of {@code isEqualByCompareTo} also triggers the second
     * {@code actual.compareTo(other)} call on line 532 (format argument for the error message),
     * covering the double-evaluation that would ideally be refactored into a local variable.
     */
    @DisplayName("isEqualByCompareTo should fail with message including compareTo result")
    @Test
    void isEqualByCompareTo_differentBics_fails() {
        Bic bicA = Bic.of(VALID_BIC8_MARKDEFF);
        Bic bicB = Bic.of(OTHER_BIC_DEUTDEFFXXX);
        assertThatThrownBy(() -> assertThat(bicA).isEqualByCompareTo(bicB))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC 'MARKDEFF' to compare as equal to 'DEUTDEFFXXX' (compareTo == 0) but compareTo returned 9");
    }

    @DisplayName("isEqualByCompareTo should throw NullPointerException when 'other' is null")
    @Test
    void isEqualByCompareTo_null_throwsNpe() {
        assertThatThrownBy(() -> assertThatBic(VALID_BIC8_MARKDEFF).isEqualByCompareTo(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("The BIC to compare against must not be null");
    }

    @DisplayName("matches(Pattern) should handle null skip, passing match, and failing match")
    @Test
    void matches_predicates_coversAllBranches() {
        Bic bic = Bic.of(VALID_BIC8_MARKDEFF);

        // null guard: bicPattern != null evaluates to false -> no failWithMessage
        assertThatCode(() -> assertThat(bic).matches((Pattern) null))
            .doesNotThrowAnyException();

        // matching pattern -> passes
        assertThatCode(() -> assertThat(bic).matches(Pattern.compile("MARK.{4}")))
            .doesNotThrowAnyException();

        // non-matching pattern -> failWithMessage
        assertThatThrownBy(() -> assertThat(bic).matches(Pattern.compile("DEUT.{4}")))
            .isInstanceOf(AssertionError.class)
            .hasMessage("BIC 'MARKDEFF' does not match pattern 'DEUT.{4}'");
    }

    @DisplayName("isBic8EqualTo and isBic11NormalizedEqualTo should delegate to their canonical counterparts")
    @Test
    void backwardCompatAliases_invoked_delegateToNewMethods() {
        assertThat(Bic.of(VALID_BIC8_MARKDEFF)).isBic8EqualTo(VALID_BIC8_MARKDEFF);
        assertThat(Bic.of(VALID_BIC11_MARKDEFF500)).isBic11NormalizedEqualTo(VALID_BIC11_MARKDEFF500);
    }

    @DisplayName("Should support SoftAssertions via 'using' proxy without collecting errors")
    @Test
    void softAssertions_proxy_succeed() {
        SoftAssertions softly = new SoftAssertions();
        Bic bic = Bic.of(VALID_BIC11_MARKDEFF500);

        BicAssertions.using(bic, softly)
            .hasCountryCode("DE")
            .isBic11();

        assertThatCode(softly::assertAll).doesNotThrowAnyException();
    }

    @DisplayName("Failure messages should contain expected and actual values")
    @Test
    void failureMessages_failingAssertions_areCorrect() {
        Bic bic = Bic.of(VALID_BIC8_MARKDEFF);
        BicAssert bicAssert = assertThat(bic);

        assertThatThrownBy(() -> bicAssert.hasCountryCode("US"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC country code to be 'US' but was 'DE' for BIC '%s'", bic);

        assertThatThrownBy(bicAssert::isBic11)
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC to be BIC-11 (length 11) but was BIC-8 (length 8)");
    }

    @ParameterizedTest(name = "BIC '{0}' validity should be {1}")
    @CsvSource(delimiter = '|', value = {
        "MARKDEFF    | true",
        "MARKDEFF500 | true",
        "TOO_SHORT   | false"
    })
    void validity_variousInputs_isReflected(String input, boolean expected) {
        if (expected) {
            assertThatBicIsValid(input);
        } else {
            assertThatBicIsNotValid(input);
        }
    }

    @DisplayName("Should verify each BIC component and their failure messages")
    @Test
    void componentAssertions_parsedBic_verifyEachPart() {
        String bic = VALID_BIC11_MARKDEFF500;

        assertThatBic(bic)
            .hasBankCode("MARK")
            .hasCountryCode("DE")
            .hasLocationCode("FF")
            .hasBranchCode("500")
            .isBic11();

        assertThatThrownBy(() -> assertThatBic(bic).hasBankCode("XXXX"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC bank code to be 'XXXX' but was 'MARK' for BIC '%s'", bic);

        assertThatThrownBy(() -> assertThatBic(bic).hasLocationCode("ZZ"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC location code to be 'ZZ' but was 'FF' for BIC '%s'", bic);
    }

    @DisplayName("Should verify currency and country metadata with failure cases")
    @Test
    void metadataAssertions_currencyAndFlags_areHandled() {
        String bic = VALID_BIC8_MARKDEFF;

        assertThatBic(bic)
            .hasCurrency(Currency.EUR)
            .hasCurrencyCode("EUR")
            .hasCountryName("Germany");

        assertThatThrownBy(() -> assertThatBic(bic).hasCurrencyCode("USD"))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected currency code to be 'USD' but was 'EUR' for BIC '%s'", bic);
    }

    @DisplayName("Should handle branch code presence and absence with failure message")
    @Test
    void branchCodeAssertions_presenceAndAbsence_areHandled() {
        Bic bic8  = Bic.of(VALID_BIC8_MARKDEFF);
        Bic bic11 = Bic.of("MARKDEFFXXX");

        assertThat(bic8).hasNoBranchCode();
        assertThat(bic11).hasBranchCode("XXX");

        assertThatThrownBy(() -> assertThat(bic11).hasNoBranchCode())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC branch code to be 'null' but was 'XXX' for BIC '%s'", bic11);
    }

    @DisplayName("Should verify BIC length with failure message on mismatch")
    @Test
    void lengthAssertions_bic8AndBic11_verifyExactLength() {
        String bic = VALID_BIC8_MARKDEFF;

        assertThatBic(bic)
            .hasLength(8)
            .hasBic8("MARKDEFF")
            .hasBic11("MARKDEFFXXX");

        assertThatThrownBy(() -> assertThatBic(bic).hasLength(11))
            .isInstanceOf(AssertionError.class)
            .hasMessage("Expected BIC length to be 11 but was 8 for BIC '%s'", bic);
    }

    @DisplayName("Should handle null, empty, and valid inputs in assertThatBicIsValid")
    @Test
    void factoryPoints_edgeCases_areHandled() {
        assertThatBicIsNotValid(null);
        assertThatBicIsNotValid("");
        assertThatBicIsValid("MARKDEFF");

        assertThatInvalidBicException()
            .isThrownBy(() -> {
                throw InvalidBicException.of(BicValidationError.EMPTY);
            })
            .withMessage("BIC is null or empty (%s)", BicValidationError.EMPTY);
    }

}

