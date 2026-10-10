package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for the default methods of {@link ColumnDefinition}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class ColumnDefinitionTest {

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "BIC                    | bic",
        "BANK_NAME              | bankName",
        "CHECK_DIGIT_METHOD     | checkDigitMethod",
        "NACHFOLGE_BANKLEITZAHL | nachfolgeBankleitzahl",
        "_LEADING               | leading",
        "TRAILING_              | trailing",
        "DOUBLE__UNDERSCORE     | doubleUnderscore",
    })
    void getCamelCaseName_constantName_returnsCamelCase(String name, String expected) {
        assertThat(column(name).getCamelCaseName()).isEqualTo(expected);
    }

    @Test
    void isOptional_notOverridden_returnsFalse() {
        assertThat(column("BIC").isOptional()).isFalse();
    }

    /**
     * Returns a column definition with the given name at index 0.
     */
    private static ColumnDefinition column(String name) {
        return new ColumnDefinition() {

            @Override
            public int getIndex() {
                return 0;
            }

            @Override
            public String name() {
                return name;
            }

        };
    }

}
