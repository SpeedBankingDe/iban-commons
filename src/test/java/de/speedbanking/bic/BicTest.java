package de.speedbanking.bic;

import static de.speedbanking.bic.BicAssertions.assertThat;
import static de.speedbanking.bic.BicAssertions.assertThatInvalidBicException;

import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit tests for the new immutable {@link Bic} class, covering BIC validation and component extraction.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
class BicTest {

    @DisplayName("of() should create Bic object for a valid BIC-8")
    @Test
    void of_ValidBic8_ShouldReturnBic() {
        assertThat(Bic.of("MARKDEFF"))
            .as("Check properties for BIC-8")
            .isBic8()
            .isNotBic11()
            .hasLength(8)
            .hasToString("MARKDEFF")
            .isBic8EqualTo("MARKDEFF")
            .isBic11NormalizedEqualTo("MARKDEFFXXX");
    }

    @DisplayName("of() should create Bic object for a valid BIC-11")
    @Test
    void of_ValidBic11_ShouldReturnBic() {
        assertThat(Bic.of("MARKDEFF500"))
            .as("Check properties for BIC-11")
            .isBic11()
            .isNotBic8()
            .hasLength(11)
            .hasToString("MARKDEFF500")
            .isBic11NormalizedEqualTo("MARKDEFF500")
            .hasCountryCode("DE")
            .hasCountryFlag("🇩🇪");
    }

    @DisplayName("of() should throw InvalidBicException for invalid BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {" ", "INVALID99", "MARKDE"})
    void of_InvalidBic_ShouldThrowException(String bic) {
        assertThatInvalidBicException()
            .isThrownBy(() -> Bic.of(bic))
            .withCause(null)
            .withMessage("BIC has incorrect length")
            .hasFieldOrPropertyWithValue("reason", BicValidationError.INCORRECT_LENGTH);
    }

    @DisplayName("tryParse() should return non-empty Optional for valid BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {"BHLSDEM1", "BHLSDEM1XXX"})
    void tryParse_ValidBic_ShouldReturnNonEmptyOptional(String bic) {
        assertThat(Bic.tryParse(bic))
            .isNotEmpty()
            .hasValue(Bic.of(bic));
    }

    @DisplayName("tryParse() should return Optional.empty for invalid BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {" ", "INVALID99", "MARK00FF"})
    @NullAndEmptySource
    void tryParse_InvalidBic_ShouldReturnEmptyOptional(String bic) {
        assertThat(Bic.tryParse(bic)).isEmpty();
    }

    @DisplayName("isValid() should return true/false based on validation")
    @ParameterizedTest(name = "BIC: ''{0}'', valid: {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "MARKDEFF | true",
        "INVALID  | false",
        "(null)   | false",
        "''       | false",
        "' '      | false",
        "xyz      | false",
        "MARKxxFF | false",
        "MARK00FF | false"
    })
    void isValid_ShouldReflectValidationResult(String bic, boolean expectedResult) {
        assertThat(Bic.isValid(bic)).isEqualTo(expectedResult);
    }

    @DisplayName("Component getters should return correct parts")
    @ParameterizedTest(name = "BIC: {0}, BankCode: {1}, CountryCode: {2}, LocationCode: {3}, BranchCode: {4}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // BIC (Input) | Bank Code | Country Code | Location Code | Branch Code
        "MARKDEFF      | MARK      | DE           | FF            | (null)",
        "MARKDEFFXXX   | MARK      | DE           | FF            | XXX",
        "NEDSZAJJ      | NEDS      | ZA           | JJ            | (null)",
        "NEDSZAJJXXX   | NEDS      | ZA           | JJ            | XXX",
        "DBABDEFF500   | DBAB      | DE           | FF            | 500"
    })
    void getters_ShouldExtractCorrectComponents(
            String bicValue,
            String expectedBankCode,
            String expectedCountryCode,
            String expectedLocationCode,
            String expectedBranchCode
    ) {
        Bic bic = Bic.of(bicValue);

        assertThat(bic)
            .as("Component extraction check for BIC %s", bicValue)
            .hasBankCode(expectedBankCode)
            .hasCountryCode(expectedCountryCode)
            .hasLocationCode(expectedLocationCode)
            .hasBranchCode(expectedBranchCode);

        // the isSameAs checks are implementation details (caching) and must remain separate
        // to verify instance identity, as they do not return BicAssert.
        assertThat(bic.getBankCode()).isSameAs(bic.getBankCode());
        assertThat(bic.getCountryCode()).isSameAs(bic.getCountryCode());
        assertThat(bic.getLocationCode()).isSameAs(bic.getLocationCode());
    }

    @DisplayName("toBic11(), toBic8(), and toString() should return the correct format")
    @ParameterizedTest(name = "Input: {0}, toBic11: {1}, toBic8: {2}, toString: {3}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // BIC (Input) | Expected BIC-11 | Expected BIC-8 | Expected toString()
        "MARKDEFF      | MARKDEFFXXX     | MARKDEFF       | MARKDEFF   ",
        "MARKDEFFXXX   | MARKDEFFXXX     | MARKDEFF       | MARKDEFFXXX",
        "DBABDEFF500   | DBABDEFF500     | DBABDEFF       | DBABDEFF500"
    })
    void toBicFormats_ShouldReturnCorrectNormalizedStrings(String inputBic, String expectedBic11, String expectedBic8, String expectedToString) {
        assertThat(Bic.of(inputBic))
            .as("Format check for BIC %s", inputBic)
            .isBic11NormalizedEqualTo(expectedBic11)
            .isBic8EqualTo(expectedBic8)
            .hasToString(expectedToString);
    }

    @DisplayName("equality() and hashCode() should treat BIC-8 and BIC-11XXX as equal")
    @ParameterizedTest(name = "BIC-8: {0}, BIC-11: {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "MARKDEFF | MARKDEFFXXX",
        "NEDSZAJJ | NEDSZAJJXXX"
    })
    void equality_Bic8AndBic11XXX_ShouldBeEqual(String bic8Value, String bic11Value) {
        Bic bic8 = Bic.of(bic8Value);
        Bic bic11First = Bic.of(bic11Value);
        Bic bic11Second = Bic.of(bic11Value);

        // first chain: bic8 equals and hashCode checks
        assertThat(bic8)
            .as("bic8 equals and hashCode checks")
            .isEqualTo(bic8)
            .isEqualTo(bic11First)
            .isNotEqualTo(bic11First.toBic8()) // compare against String (should fail)
            .hasSameHashCodeAs(bic11First)
            .extracting(Object::hashCode)
            .isNotEqualTo(0);

        // second chain: bic11First equals and hashCode checks
        assertThat(bic11First)
            .as("bic11First equals and hashCode checks")
            .isEqualTo(bic11First)
            .isEqualTo(bic11Second)
            .isNotEqualTo(bic11First.toBic11()) // compare against String (should fail)
            .hasSameHashCodeAs(bic11Second)
            .extracting(Object::hashCode)
            .isNotEqualTo(0);
    }

    @DisplayName("equals() should not treat different branch codes as equal")
    @Test
    void equality_DifferentBICs_ShouldNotBeEqual() {
        Bic bic8 = Bic.of("MARKDEFF");
        Bic bic11Branch1 = Bic.of("MARKDEFF500");
        Bic bic11Branch2 = Bic.of("MARKDEFF100");

        assertThat(bic8)
            .isNotEqualTo(bic11Branch1);

        assertThat(bic11Branch1)
            .isNotEqualTo(bic11Branch2);
    }

    @DisplayName("compareTo() should compare based on BIC-11 string and handle null")
    @Test
    void compareTo_ShouldBeBasedOnBic11() {
        Bic bicA = Bic.of("MARKDEFF");    // MARKDEFFXXX
        Bic bicB = Bic.of("MARKDEFFXXX");
        Bic bicC = Bic.of("MARKDEFF500"); // MARKDEFF500
        Bic bicD = Bic.of("MARKUS33");    // MARKUS33XXX

        // first chain: equals (0) and null check
        assertThat(bicA.compareTo(bicB)).isEqualTo(0);
        assertThatNullPointerException()
            .isThrownBy(() -> bicA.compareTo(null))
            .withMessage("Cannot compare Bic to null");

        // second chain: greater than (positive)
        assertThat(bicA.compareTo(bicC)).isGreaterThan(0);

        // third chain: less than (negative)
        assertThat(bicA.compareTo(bicD)).isLessThan(0);
    }

    @DisplayName("charAt() should return correct character and check bounds")
    @Test
    void charSequenceCharAt() {
        Bic bic = Bic.of("MARKDEFF500"); // 11 chars

        assertThat(bic)
            .returns('M', c -> c.charAt(0))
            .returns('0', c -> c.charAt(10));

        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> bic.charAt(-1))
            .withMessage("Index should be between 0 (inclusive) and 11 (exclusive), but was -1");
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> bic.charAt(11))
            .withMessage("Index should be between 0 (inclusive) and 11 (exclusive), but was 11");
    }

    @DisplayName("subSequence() should return correct substring and check bounds")
    @Test
    void charSequenceSubSequence() {
        Bic bic = Bic.of("MARKDEFF"); // 8 chars

        assertThat(bic)
            .extracting(
                c -> c.subSequence(0, 8),
                c -> c.subSequence(4, 6),
                c -> c.subSequence(6, 8),
                c -> c.subSequence(4, 4)
            )
            .containsExactly(
                bic.toString(),
                "DE",
                "FF",
                ""
            );

        assertThatIndexOutOfBoundsException().isThrownBy(() -> bic.subSequence(-1, 2))
            .withMessage("Start index should be >= 0 (was: -1) and end index (exclusive) <= 8 (was: 2)");

        assertThatIndexOutOfBoundsException().isThrownBy(() -> bic.subSequence(0, bic.toString().length() + 1))
            .withMessage("Start index should be >= 0 (was: 0) and end index (exclusive) <= 8 (was: 9)");

        assertThatIndexOutOfBoundsException().isThrownBy(() -> bic.subSequence(5, 4))
            .withMessage("Start index should be >= 0 (was: 5) and end index (exclusive) <= 8 (was: 4)");
    }

    @DisplayName("toString() should return the original input string")
    @Test
    void toStringShouldReturnOriginalInput() {
        Bic bic8 = Bic.of("MARKDEFF");
        Bic bic11 = Bic.of("MARKDEFF500");

        assertThat(bic8).hasToString("MARKDEFF");
        assertThat(bic11).hasToString("MARKDEFF500");
    }

}
