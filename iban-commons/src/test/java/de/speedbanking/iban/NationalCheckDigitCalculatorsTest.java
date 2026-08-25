package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.iban.NationalCheckDigitCalculators.NcdCalculatorBase;
import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class NationalCheckDigitCalculatorsTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void constructor_shouldBePrivate() {
        TestUtil.assertConstructorIsPrivate(NationalCheckDigitCalculators.class);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNcdCalculatorClasses")
    void toString_shouldFollowCustomFormat(Class<? extends NcdCalculatorBase> clazz) throws Exception {
        Constructor<? extends NcdCalculatorBase> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        NcdCalculatorBase calculator = constructor.newInstance();

        String toString = calculator.toString();

        String expectedToString = String.format("%s$%s[NCD %s]",
            NationalCheckDigitCalculators.class.getSimpleName(),
            clazz.getSimpleName(),
            calculator.ncdComponent);

        assertThat(toString)
            .as("Check toString() format for class %s", clazz.getSimpleName())
            .isNotNull()
            .isEqualTo(expectedToString);
    }

    @SuppressWarnings("unchecked")
    private static Stream<Class<? extends NcdCalculatorBase>> provideNcdCalculatorClasses() {
        return Stream.of(NationalCheckDigitCalculators.class.getDeclaredClasses())
            .filter(NcdCalculatorBase.class::isAssignableFrom)
            .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
            .map(clazz -> (Class<? extends NcdCalculatorBase>) clazz);
    }

}
