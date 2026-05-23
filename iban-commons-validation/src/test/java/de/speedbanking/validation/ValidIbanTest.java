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
 * Tests for {@link ValidIban} and {@link IbanConstraintValidator}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@DisplayName("@ValidIban constraint")
final class ValidIbanTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

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

    static final class WithIbanAllowSpace {
        @ValidIban(allowSpace = true)
        private final String iban;

        WithIbanAllowSpace(String iban) {
            this.iban = iban;
        }

        String iban() {
            return iban;
        }
    }

    // -------------------------------------------------------------------------
    // Null / empty -> always valid
    // -------------------------------------------------------------------------

    @DisplayName("null and empty pass without @NotNull/@NotBlank")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    void nullAndEmpty_areValid(String iban) {
        assertThat(validator.validate(new WithIban(iban))).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Valid IBANs
    // -------------------------------------------------------------------------

    @DisplayName("Valid IBANs pass validation")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "DE89370400440532013000",
        "GB29NWBK60161331926819",
        "FR1420041010050500013M02606",
        "NL91ABNA0417164300",
        "AT611904300234573201"
    })
    void validIban_passes(String iban) {
        assertThat(validator.validate(new WithIban(iban))).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Invalid IBANs
    // -------------------------------------------------------------------------

    @DisplayName("Invalid IBANs fail validation")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {
        "XX12345678901234567890",
        "DE123",
        "DE91100000000123456780",
        "NOTANIBAN"
    })
    void invalidIban_fails(String iban) {
        Set<ConstraintViolation<WithIban>> violations = validator.validate(new WithIban(iban));
        assertThat(violations).hasSize(1);
    }

    @DisplayName("Violation message contains specific IbanValidationError text")
    @Test
    void invalidIban_violationMessage_containsErrorText() {
        Set<ConstraintViolation<WithIban>> violations =
            validator.validate(new WithIban("DE91100000000123456780"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("IBAN violates ISO 7064 Mod 97-10 checksum check");
    }

    @DisplayName("Country code error produces correct violation message")
    @Test
    void invalidCountry_violationMessage() {
        Set<ConstraintViolation<WithIban>> violations =
            validator.validate(new WithIban("XX12345678901234567890"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("IBAN has invalid country code");
    }

    // -------------------------------------------------------------------------
    // allowSpace
    // -------------------------------------------------------------------------

    @DisplayName("Grouped IBAN passes when allowSpace = true")
    @Test
    void groupedIban_passesWithAllowSpace() {
        assertThat(validator.validate(new WithIbanAllowSpace("DE89 3704 0044 0532 0130 00"))).isEmpty();
    }

    @DisplayName("Grouped IBAN with spaces stripped produces no violation (covers stripSpaces replace-branch)")
    @Test
    void groupedIban_spacesAreStripped() {
        // Explicitly drives the indexOf(' ') >= 0 branch in stripSpaces():
        // the grouped and compact forms must both pass.
        assertThat(validator.validate(new WithIbanAllowSpace("DE89 3704 0044 0532 0130 00"))).isEmpty();
        assertThat(validator.validate(new WithIbanAllowSpace("DE89370400440532013000"))).isEmpty();
    }

    @DisplayName("Grouped IBAN fails when allowSpace = false (default)")
    @Test
    void groupedIban_failsWithoutAllowSpace() {
        assertThat(validator.validate(new WithIban("DE89 3704 0044 0532 0130 00"))).hasSize(1);
    }

}
