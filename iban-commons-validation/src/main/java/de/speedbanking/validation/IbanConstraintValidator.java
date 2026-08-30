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

import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanConfig;
import de.speedbanking.iban.IbanValidationError;
import de.speedbanking.iban.InvalidIbanException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation {@link ConstraintValidator} for the {@link ValidIban} constraint.
 * <p>
 * Delegates to {@link Iban#validate(CharSequence)}, which performs the full ISO 13616
 * validation without allocating an {@link Iban} instance. The specific
 * {@link de.speedbanking.iban.IbanValidationError} text is extracted from the thrown
 * {@link InvalidIbanException} and used as the constraint violation message, replacing
 * the annotation's default message.
 * <p>
 * {@code null} and empty inputs pass validation; use {@code @NotNull} or {@code @NotBlank}
 * in combination to reject them.
 *
 * @see ValidIban
 * @see BicConstraintValidator
 * @since 1.8.8
 */
public class IbanConstraintValidator implements ConstraintValidator<ValidIban, CharSequence> {

    private boolean allowSpace;

    @Override
    public void initialize(ValidIban annotation) {
        this.allowSpace = annotation.allowSpace();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} for {@code null} and empty inputs (null-safe by convention).
     * Calls {@link Iban#validate(CharSequence)} for all other inputs; on failure, disables
     * the default constraint message and substitutes the error text from the thrown
     * {@link InvalidIbanException}.
     */
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext ctx) {
        if (value == null || value.length() == 0) { // CharSequence has no isEmpty()
            return true;
        }

        // Iban.validate() only ever honors the global IbanConfig.isAllowSpace() flag, so the
        // per-field allowSpace() attribute has to be enforced here on both sides:
        if (!allowSpace && IbanConfig.isAllowSpace() && containsSpace(value)) {
            // annotation forbids spaces even though the global config would otherwise accept them
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(IbanValidationError.ILLEGAL_CHARACTERS.getText())
               .addConstraintViolation();
            return false;
        }

        // annotation permits spaces but the global IbanConfig does not: strip them before
        // delegating so that Iban.validate() does not reject them.
        CharSequence input = allowSpace && !IbanConfig.isAllowSpace() ? stripSpaces(value) : value;

        try {

            Iban.validate(input);
            return true;

        } catch (InvalidIbanException ex) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(ex.getReason().getText())
               .addConstraintViolation();
            return false;
        }
    }

    private static boolean containsSpace(CharSequence value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == ' ') {
                return true;
            }
        }
        return false;
    }

    private static String stripSpaces(CharSequence value) {
        // avoids regex overhead for the common case of no spaces
        String s = value.toString();
        return s.indexOf(' ') < 0 ? s : s.replace(" ", "");
    }

}
