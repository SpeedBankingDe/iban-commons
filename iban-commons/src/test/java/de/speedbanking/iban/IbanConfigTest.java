package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Unit tests for {@link IbanConfig}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@ResourceLock(IbanConfigTest.RESOURCE_NAME)
final class IbanConfigTest {

    static final String RESOURCE_NAME = "IbanConfig";

    @BeforeEach
    void prepareIbanConfig() {
        IbanConfig.reset();
    }

    @AfterAll
    static void resetIbanConfig() {
        IbanConfig.reset();
    }

    // -------------------------------------------------------------------------
    // Default state
    // -------------------------------------------------------------------------

    @DisplayName("Default instance should have all options disabled")
    @Test
    void get_shouldHaveAllOptionsDisabled_whenDefaultIsUsed() {
        assertThat(IbanConfig.isValidateNcd()).isFalse();
        assertThat(IbanConfig.isCalculateNcd()).isFalse();
        assertThat(IbanConfig.isAllowSpace()).isFalse();
        assertThat(IbanConfig.isAllowLowercase()).isFalse();
    }

    @DisplayName("get() without configure() should return DEFAULT instance")
    @Test
    void get_shouldReturnDefaultInstance_whenNotConfigured() {
        assertThat(IbanConfig.get()).isSameAs(IbanConfig.DEFAULT);
    }

    // -------------------------------------------------------------------------
    // configure() + get()
    // -------------------------------------------------------------------------

    @DisplayName("configure() should install the provided instance")
    @Test
    void configure_shouldUpdateSettings_whenCustomInstanceIsProvided() {
        IbanConfig custom = IbanConfig.builder()
            .allowSpace(true)
            .allowLowercase(true)
            .build();

        IbanConfig.configure(custom);

        assertThat(IbanConfig.get()).isSameAs(custom);
        assertThat(IbanConfig.isAllowSpace()).isTrue();
        assertThat(IbanConfig.isAllowLowercase()).isTrue();
    }

    @DisplayName("configure() should reject null")
    @Test
    void configure_shouldThrowException_whenInputIsNull() {
        assertThatThrownBy(() -> IbanConfig.configure(null))
            .isInstanceOf(NullPointerException.class);
    }

    @DisplayName("configure() after get() should throw IllegalStateException")
    @Test
    void configure_shouldThrowException_whenInstanceIsAlreadyFrozen() {
        IbanConfig.get(); // freezes

        assertThatThrownBy(() -> IbanConfig.configure(IbanConfig.DEFAULT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already in use");
    }

    @DisplayName("configure() before get() should succeed")
    @Test
    void configure_shouldSucceed_whenCalledBeforeFirstAccess() {
        IbanConfig custom = IbanConfig.builder().validateNcd(true).build();

        IbanConfig.configure(custom); // must not throw

        assertThat(IbanConfig.isValidateNcd()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    @DisplayName("Builder should set each property independently")
    @Test
    void builder_shouldSetAllProperties_whenEachIsConfigured() {
        IbanConfig config = IbanConfig.builder()
            .validateNcd(true)
            .calculateNcd(true)
            .allowSpace(false)
            .allowLowercase(false)
            .build();

        // validate state using toString to avoid using the static global methods
        assertThat(config.toString())
            .contains("validateNcd=true")
            .contains("calculateNcd=true")
            .contains("allowSpace=false")
            .contains("allowLowercase=false");
    }

    @DisplayName("Builder defaults should match DEFAULT instance")
    @Test
    void builder_shouldMatchDefaultInstance_whenBuiltWithoutConfiguration() {
        IbanConfig fromBuilder = IbanConfig.builder().build();

        assertThat(fromBuilder).hasToString(IbanConfig.DEFAULT.toString());
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @DisplayName("toString should contain class name and all property values")
    @Test
    void toString_shouldContainAllFields_whenDefaultIsUsed() {
        String result = IbanConfig.DEFAULT.toString();

        assertThat(result)
            .contains("IbanConfig")
            .contains("validateNcd=false")
            .contains("calculateNcd=false")
            .contains("allowSpace=false")
            .contains("allowLowercase=false");
    }
}

