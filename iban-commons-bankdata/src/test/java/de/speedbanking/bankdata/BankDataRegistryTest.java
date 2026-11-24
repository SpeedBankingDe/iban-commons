package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for {@link BankDataRegistry}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataRegistryTest {

    @Test
    void supportedCountryCodes_containsShippedCountries() {
        assertThat(BankDataRegistry.getSupportedCountryCodes()).containsExactlyInAnyOrder("DE", "AT", "CH", "LI", "CZ", "BE", "PL", "NL", "ES", "FR", "GR", "PT");
    }

    @Test
    void isCountrySupported_knownCountry_returnsTrue() {
        assertThat(BankDataRegistry.isCountrySupported("DE")).isTrue();
    }

    @Test
    void isCountrySupported_unknownCountry_returnsFalse() {
        assertThat(BankDataRegistry.isCountrySupported("XX")).isFalse();
    }

    @Test
    void loaderLookup_knownCountry_present() {
        assertThat(BankDataRegistry.getLoader("DE")).isPresent();
    }

    @Test
    void loaderLookup_unknownCountry_empty() {
        assertThat(BankDataRegistry.getLoader("XX")).isEmpty();
    }

    @Test
    void cacheLookup_unknownCountry_throws() {
        assertThatIllegalArgumentException().isThrownBy(() -> BankDataRegistry.getCache("XX"));
    }

    @Test
    void cacheLookup_calledTwice_returnsSameInstance() {
        assertThat(BankDataRegistry.getCache("DE")).isSameAs(BankDataRegistry.getCache("DE"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        "XA", // class is no loader
        "XB", // loader class is abstract
        "XC", // loader class has no no-argument constructor
        "XD", // static initializer of the loader class fails
        "XE", // no loader class at all
    })
    void tryLoadForCountry_unusableLoaderClass_returnsNull(String countryCode) {
        assertThat(BankDataRegistry.tryLoadForCountry(countryCode)).isNull();
    }

    @Test
    void constructor_private_throwsUnsupportedOperationException() throws NoSuchMethodException {
        Constructor<BankDataRegistry> constructor = BankDataRegistry.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(exception.getCause()).isInstanceOf(UnsupportedOperationException.class);
    }

}
