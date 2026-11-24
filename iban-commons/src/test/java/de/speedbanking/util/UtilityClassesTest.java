package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class UtilityClassesTest {

    @Test
    void privateConstructor_invoked_throwsException() {
        TestUtil.assertConstructorIsPrivate(UtilityClasses.class);
    }

    @Test
    void cannotInstantiate_anyClass_namesClassInMessage() {
        assertThat(UtilityClasses.cannotInstantiate(CharUtil.class))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Utility class CharUtil cannot be instantiated");
    }

}
