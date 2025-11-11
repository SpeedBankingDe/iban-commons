package de.speedbanking.bic;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * JUnit test class for {@link BicValidator}.
 */
class BicValidatorTest extends Assertions {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructorShouldThrowException() throws Exception {
        Constructor<BicValidator> constructor = BicValidator.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(constructor::newInstance)
            .withCauseInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getCause)
            .isInstanceOf(UnsupportedOperationException.class)
            .extracting(Throwable::getMessage)
            .isEqualTo("Utility class " + BicValidator.class.getSimpleName() + " cannot be instantiated");
    }

}
