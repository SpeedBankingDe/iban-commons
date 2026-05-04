package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AbstractCountryValidator}.
 * <p>
 * Uses a concrete mock implementation to verify the base class behavior.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class AbstractCountryValidatorTest {

    /**
     * Concrete implementation for testing purposes.
     * <p>
     * name must match a valid entry in {@link IbanRegistry} (e.g., DE).
     */
    static final class DE extends AbstractCountryValidator {
        @Override
        public boolean validateIban(char[] iban) {
            return true;
        }
    }

    /**
     * Implementation with an invalid name to test constructor failure.
     */
    static final class INVALID extends AbstractCountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return true;
        }
    }

    @DisplayName("Constructor should resolve country data when class name is valid")
    @Test
    void constructor_shouldResolveCountryData_whenClassNameIsValid() {
        AbstractCountryValidator validator = new DE();

        assertThat(validator.getCountryData())
            .isNotNull()
            .isEqualTo(IbanRegistry.DE);
    }

    @DisplayName("Constructor should throw exception when class name is not in registry")
    @Test
    void constructor_shouldThrowException_whenClassNameIsInvalid() {
        assertThatThrownBy(INVALID::new)
            .isExactlyInstanceOf(ExceptionInInitializerError.class)
            .hasMessage("'INVALID' is not a supported IBAN country code");
    }

    @DisplayName("toString should return formatted string matching the contract")
    @Test
    void toString_shouldReturnFormattedString() {
        AbstractCountryValidator validator = new DE();

        // expected format: DE[Germany]
        String expected = "DE[" + IbanRegistry.DE.getCountryName() + "]";

        assertThat(validator).hasToString(expected);
    }

    @DisplayName("getCountryData should return the same instance as resolved at construction")
    @Test
    void getCountryData_shouldReturnRegistryEntry() {
        AbstractCountryValidator validator = new DE();
        IbanRegistry data = validator.getCountryData();

        assertThat(data).isSameAs(IbanRegistry.DE);
    }

    @DisplayName("toString should match the required pattern and contain country name")
    @Test
    void toString_shouldFollowPattern() {
        AbstractCountryValidator validator = new DE();
        String result = validator.toString();

        // Testing the specific format requirements
        assertThat(result)
            .startsWith("DE[")
            .endsWith("]")
            .contains(IbanRegistry.DE.getCountryName());
    }

}
