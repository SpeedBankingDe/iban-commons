package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:MethodName")
final class IbanTestDataGeneratorTest {

    @Test
    void createTestDatatAllCountries_shouldNotThrow() {
        assertThat(IbanTestDataGenerator.createTestDatatAllCountries()).hasSize(IbanRegistry.values().length);
    }

}
