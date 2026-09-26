package de.speedbanking.validation;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.iban.IbanConfig;
import de.speedbanking.iban.IbanConfigTestBridge;
import de.speedbanking.iban.IbanValidationError;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

/**
 * Covers {@link IbanConstraintValidator}'s {@code containsSpace()} branch, which only triggers
 * when the global {@link IbanConfig#isAllowSpace()} is {@code true} while the per-field
 * {@code @ValidIban(allowSpace = false)} still forbids spaces. {@link IbanConfig} is a
 * configure-once, freeze-for-the-JVM singleton, so this needs the test-only reset bridge and its
 * own resource lock, isolated from {@link ValidIbanTest} (which relies on the default config).
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@ResourceLock(IbanConstraintValidatorGlobalAllowSpaceTest.RESOURCE_NAME)
final class IbanConstraintValidatorGlobalAllowSpaceTest {

    static final String RESOURCE_NAME = "IbanConfig";

    private static Validator validator;

    static final class WithIban {
        @ValidIban
        private final String iban;

        WithIban(String iban) {
            this.iban = iban;
        }

        String iban() {
            return iban;
        }
    }

    @BeforeEach
    void configureGlobalAllowSpace() {
        IbanConfigTestBridge.reset(IbanConfig.builder().allowSpace(true).build());
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @AfterAll
    static void resetGlobalConfig() {
        IbanConfigTestBridge.resetToDefault();
    }

    @DisplayName("annotation forbids spaces even though the global config allows them")
    @Test
    void groupedIban_globalAllowSpaceTrue_annotationAllowSpaceFalse_isRejected() {
        Set<ConstraintViolation<WithIban>> violations =
            validator.validate(new WithIban("DE89 3704 0044 0532 0130 00"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo(IbanValidationError.ILLEGAL_CHARACTERS.getText());
    }

    @DisplayName("compact IBAN still passes under the same global config")
    @Test
    void compactIban_globalAllowSpaceTrue_annotationAllowSpaceFalse_isValid() {
        assertThat(validator.validate(new WithIban("DE89370400440532013000"))).isEmpty();
    }

}
