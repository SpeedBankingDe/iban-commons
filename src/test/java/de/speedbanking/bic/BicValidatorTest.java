package de.speedbanking.bic;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JUnit test class for {@link BicValidator}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BicValidatorTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructor_shouldThrowException_whenInstantiated() {
        TestUtil.assertConstructorIsPrivate(BicValidator.class);
    }

}
