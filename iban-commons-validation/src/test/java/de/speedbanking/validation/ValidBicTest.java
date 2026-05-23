/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

/**
 * Tests for {@link ValidBic} and {@link BicConstraintValidator}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@DisplayName("@ValidBic constraint")
final class ValidBicTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    static final class WithBic {
        @ValidBic
        private final String bic;

        WithBic(String bic) {
            this.bic = bic;
        }

        String bic() {
            return bic;
        }
    }

    // -------------------------------------------------------------------------
    // Null / empty → always valid
    // -------------------------------------------------------------------------

    @DisplayName("null and empty pass without @NotNull/@NotBlank")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    void nullAndEmpty_areValid(String bic) {
        assertThat(validator.validate(new WithBic(bic))).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Valid BICs
    // -------------------------------------------------------------------------

    @DisplayName("Valid BICs pass validation")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "MARKDEFF",
        "MARKDEFFXXX",
        "BHLSDEM1",
        "NEDSZAJJ",
        "BNPAFRPP"
    })
    void validBic_passes(String bic) {
        assertThat(validator.validate(new WithBic(bic))).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Invalid BICs
    // -------------------------------------------------------------------------

    @DisplayName("Invalid BICs fail validation")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "MARK00FF",
        "SHORT12",
        "mARKDEFF",
        "NOTABIC"
    })
    void invalidBic_fails(String bic) {
        Set<ConstraintViolation<WithBic>> violations = validator.validate(new WithBic(bic));
        assertThat(violations).hasSize(1);
    }

    @DisplayName("Violation message contains specific BicValidationError text")
    @Test
    void invalidBic_violationMessage_containsErrorText() {
        Set<ConstraintViolation<WithBic>> violations =
            validator.validate(new WithBic("MARK00FF"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("BIC has invalid country code");
    }

}
