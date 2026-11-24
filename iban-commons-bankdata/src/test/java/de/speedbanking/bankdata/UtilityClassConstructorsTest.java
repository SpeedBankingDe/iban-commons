package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Checks that the utility classes of this module cannot be instantiated, not even via reflection.
 */
@SuppressWarnings("checkstyle:MethodName")
final class UtilityClassConstructorsTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        "de.speedbanking.bankdata.BankNameCasing",
        "de.speedbanking.bankdata.GermanIban$CheckDigitModule",
        "de.speedbanking.bankdata.io.BankDataFormat",
        "de.speedbanking.bankdata.io.BoundedStreams",
        "de.speedbanking.bankdata.io.MinimalXlsxReader",
    })
    void privateConstructor_invoked_throwsUnsupportedOperationException(String className) throws Exception {
        Constructor<?> constructor = Class.forName(className).getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
            .isInstanceOf(InvocationTargetException.class)
            .cause()
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Utility class %s cannot be instantiated", Class.forName(className).getSimpleName());
    }

}
