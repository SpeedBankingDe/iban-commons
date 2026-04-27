package de.speedbanking.iban.tool;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.iban.IbanRegistry;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

@SuppressWarnings("checkstyle:MethodName")
final class DatafakerFinanceIbanGeneratorTest {

    @Test
    void createTestDatatAllCountries_shouldNotThrow() {
        int count = (int) Arrays.stream(IbanRegistry.values()).filter(IbanRegistry::isBaseCountry).count();
        assertThat(DatafakerFinanceIbanGenerator.formatEntries()).hasSize(count);
    }

}
