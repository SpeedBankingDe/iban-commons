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
    void default_values_are_consistent() {
        assertThat(IbanConfig.isValidateNcd()).isFalse();
        assertThat(IbanConfig.isCalculateNcd()).isFalse();
        assertThat(IbanConfig.isAllowSpace()).isFalse();
        assertThat(IbanConfig.isAllowLowercase()).isFalse();
    }

    @DisplayName("get() without configure() should return DEFAULT instance")
    @Test
    void get_returns_default_instance() {
        assertThat(IbanConfig.get()).isSameAs(IbanConfig.DEFAULT);
    }

    // -------------------------------------------------------------------------
    // configure() + get()
    // -------------------------------------------------------------------------

    @DisplayName("configure() should install the provided instance")
    @Test
    void configure_updates_settings() {
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
    void configure_throws_on_null_input() {
        assertThatThrownBy(() -> IbanConfig.configure(null))
            .isInstanceOf(NullPointerException.class);
    }

    @DisplayName("configure() after get() should throw IllegalStateException")
    @Test
    void configure_fails_after_instance_freeze() {
        IbanConfig.get(); // freezes

        assertThatThrownBy(() -> IbanConfig.configure(IbanConfig.DEFAULT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already in use");
    }

    @DisplayName("configure() before get() should succeed")
    @Test
    void configure_succeeds_before_first_access() {
        IbanConfig custom = IbanConfig.builder().validateNcd(true).build();

        IbanConfig.configure(custom); // must not throw

        assertThat(IbanConfig.isValidateNcd()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    @DisplayName("Builder should set each property independently")
    @Test
    void builder_sets_all_properties_correctly() {
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
    void builder_defaults_align_with_global_defaults() {
        IbanConfig fromBuilder = IbanConfig.builder().build();

        assertThat(fromBuilder).hasToString(IbanConfig.DEFAULT.toString());
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @DisplayName("toString should contain class name and all property values")
    @Test
    void to_string_contains_relevant_fields() {
        String result = IbanConfig.DEFAULT.toString();

        assertThat(result)
            .contains("IbanConfig")
            .contains("validateNcd=false")
            .contains("calculateNcd=false")
            .contains("allowSpace=false")
            .contains("allowLowercase=false");
    }
}

