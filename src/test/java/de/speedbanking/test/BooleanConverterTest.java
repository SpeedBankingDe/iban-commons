package de.speedbanking.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

class BooleanConverterTest {

    @ParameterizedTest
    @CsvSource({
        "ja, true",
        "x, true",
        "TRUE, true",
        "1, true",
        "nein, false",
        "0, false",
        ", false"
    })
    void shouldConvertToBoolean(
            @ConvertWith(BooleanConverter.class) boolean input,
            boolean expected) {

        assertEquals(expected, input);
    }
}
