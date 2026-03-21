package de.speedbanking.bic;

import de.speedbanking.test.TestUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JUnit test class for {@link BicValidator}.
 */
class BicValidatorTest {

    @DisplayName("Private constructor should throw UnsupportedOperationException")
    @Test
    void privateConstructorShouldThrowException() {
        TestUtil.assertConstructorIsPrivate(BicValidator.class);
    }

}
