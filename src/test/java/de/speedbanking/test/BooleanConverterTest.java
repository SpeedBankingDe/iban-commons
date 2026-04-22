package de.speedbanking.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class BooleanConverterTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "ja   | true",
        "x    | true",
        "TRUE | true",
        "1    | true",
        "nein | false",
        "0    | false",
        "''   | false"
    })
    void convert_givenVariousStrings_shouldReturnExpectedBoolean(
            @ConvertWith(BooleanConverter.class) boolean input,
            boolean expected) {

        assertEquals(expected, input);
    }
}
