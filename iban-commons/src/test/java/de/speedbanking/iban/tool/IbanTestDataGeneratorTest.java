package de.speedbanking.iban.tool;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.iban.IbanRegistry;

import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:MethodName")
final class IbanTestDataGeneratorTest {

    @Test
    void createTestDatatAllCountries_shouldNotThrow() {
        assertThat(IbanTestDataGenerator.createTestDatatAllCountries()).hasSize(IbanRegistry.values().length);
    }

}
