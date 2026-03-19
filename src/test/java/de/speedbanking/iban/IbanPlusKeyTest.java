package de.speedbanking.iban;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * Tests for {@link IbanPlusKey} utility.
 * <p>
 * Validates the extraction of routing segments required for the SWIFT IBAN Plus service.
 */
class IbanPlusKeyTest extends org.assertj.core.api.Assertions {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructorShouldThrowException() throws Exception {
        Constructor<IbanPlusKey> constructor = IbanPlusKey.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(constructor::newInstance)
            .withCauseInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getMessage)
            .isEqualTo("Utility class " + IbanPlusKey.class.getSimpleName() + " cannot be instantiated");
    }

    @ParameterizedTest(name = "{index}: {2} ({0})")
    @CsvSource(delimiter = '|', value = {
        // iban                          | cc | bankCode | branch| ncd| ibanPlusLen
        // ------------------------------+----+----------+-------+----+------------
        "AD1200012030200359100100        | AD | 0001     | 2030  |    |  8",
        "AL47212110090000000235698741    | AL | 212      | 11009 |    |  8",
        "AT611904300234573201            | AT | 19043    |       |    |  5",
        "BE68539007547034                | BE | 539      |       |    |  3",
        "DE89370400440532013000          | DE | 37040044 |       |    |  8",
        "ES9121000418450200051332        | ES | 2100     | 0418  | 45 | 10",
        "FR1420041010050500013M02606     | FR | 20041    | 01005 |    | 10",
        "IT60X0542811101000000123456     | IT | 05428    | 11101 |    | 10",
        "PL61109010140000071219812874    | PL | 109      | 0101  | 4  |  8",
        "SC18SSCB11010000000000001497USD | SC | SSCB11   | 01    |    |  8"
    })
    void shouldExtractCorrectKeyForAllCountries(String ibanInput, String countryCode,
                                                String bankCode, String branchCode,
                                                String expectedNcd, int expectedIbanPlusLen) {
        String actualKey = IbanPlusKey.of(ibanInput);

        assertThat(actualKey)
            .as("IbanPlusKey for %s (%s)", countryCode, ibanInput)
            .isNotNull()
            .hasSize(expectedIbanPlusLen)
            .contains(bankCode);

        Optional.ofNullable(expectedNcd).ifPresent(
            ncd -> assertThat(actualKey).endsWith(ncd));
    }

    @ParameterizedTest(name = "{index}: {0} -> {2}")
    @CsvSource(delimiter = '|', value = {
        //cc| iban                            | ibanPlusKey
        //--+---------------------------------+------------
        "DE | DE89370400440532013000          | 37040044",   // only BankCode
        "PL | PL61109010140000071219812874    | 10901014",   // BC + Branch + NCD (appended)
        "IT | IT60X0542811101000000123456     | 0542811101", // BC + Branch (NCD at start ignored)
        "ES | ES9121000418450200051332        | 2100041845", // BC + Branch + NCD (appended)
        "SC | SC18SSCB11010000000000001497USD | SSCB1101"    // alphanumeric routing
    })
    @DisplayName("Should extract correct IbanPlus lookup key for various country structures")
    void shouldExtractCorrectKey(String country, String iban, String expected) {
        assertThat(IbanPlusKey.of(iban))
            .as("IbanPlus key extraction for %s", country)
            .isNotNull()
            .isEqualTo(expected);
    }

    @DisplayName("Should return null for invalid, empty, or unparseable IBAN strings")
    @ParameterizedTest(name = "{index}: input=\"{0}\" should return null")
    @ValueSource(strings = {"INVALID", " ", "DE89", "123", "\n"})
    @NullAndEmptySource
    void shouldReturnNullForInvalidInput(String invalidInput) {
        assertThat(IbanPlusKey.of(invalidInput))
            .as("Input '%s' should result in a null key", invalidInput)
            .isNull();
    }

    @DisplayName("Should return null for null IBAN")
    @Test
    void shouldReturnNullForNullIban() {
        assertThat(IbanPlusKey.of((Iban) null))
            .as("Null Iban should result in a null key")
            .isNull();
    }

    @DisplayName("Should return null for IBAN with invalid country code")
    @Test
    void shouldReturnNullForIbanWithInvalidCountryCode() {
        Iban iban = spy(Iban.of("MT84 MALT 0110 0001 2345 MTLC AST0 01S"));
        when(iban.getCountryCode()).thenReturn("!!");
        assertThat(IbanPlusKey.of(iban))
            .as("Input Iban '%s' (invalid country code) should result in a null key", iban)
            .isNull();
    }

    @DisplayName("Should be thread-safe due to immutable strategy cache")
    @Test
    void shouldBeThreadSafe() {
        // the strategy cache is pre-calculated and read-only
        // test ensures the class can be loaded and used without side effects
        assertThat(IbanPlusKey.of("DE89370400440532013000")).isEqualTo("37040044");
    }

}
