package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThatIbanIsValid;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanOfNormalized;
import static de.speedbanking.iban.IbanAssertions.assertThatInvalidIbanException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the custom AssertJ assertions.
 */
class IbanAssertionsTest {

    @DisplayName("Should assert on normalized IBAN creation")
    @Test
    void shouldAssertOnNormalizedIban() {
        // test lowercase and spaces
        assertThatIbanOfNormalized("DE89370400440532013000")
            .hasCountryCode("DE")
            .hasBankCode("37040044")
            .hasLength(22);
    }

    @ParameterizedTest(name = "IBAN {0} validity should be {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "DE89370400440532013000 | true",
        "INVALID                | false",
        "' '                    | false"
    })
    @DisplayName("Should assert IBAN validity")
    void shouldAssertIbanValidity(String input, boolean expected) {
        assertThatIbanIsValid(input).isEqualTo(expected);
    }

    @DisplayName("Should catch InvalidIbanException fluently")
    @Test
    void shouldCatchInvalidIbanException() {
        assertThatInvalidIbanException().isThrownBy(() -> Iban.of("SHORT"))
            .withMessage("IBAN has unsupported country code (UNSUPPORTED_COUNTRY): SHORT")
            .withNoCause();
    }

}
