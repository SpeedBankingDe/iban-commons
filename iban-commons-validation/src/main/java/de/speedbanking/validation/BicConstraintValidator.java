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

import de.speedbanking.bic.Bic;
import de.speedbanking.bic.InvalidBicException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Jakarta Bean Validation {@link ConstraintValidator} for the {@link ValidBic} constraint.
 * <p>
 * Delegates to {@link Bic#validate(CharSequence)}, which performs full ISO 9362
 * validation without allocating a {@link Bic} instance. The specific
 * {@link de.speedbanking.bic.BicValidationError} text is extracted from the thrown
 * {@link InvalidBicException} and used as the constraint violation message, replacing
 * the annotation's default message.
 * <p>
 * {@code null} and empty inputs pass validation; use {@code @NotNull} or {@code @NotBlank}
 * in combination to reject them.
 *
 * @see ValidBic
 * @see IbanConstraintValidator
 * @since 1.8.8
 */
public class BicConstraintValidator implements ConstraintValidator<ValidBic, CharSequence> {

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} for {@code null} and empty inputs (null-safe by convention).
     * Calls {@link Bic#validate(CharSequence)} for all other inputs; on failure, disables
     * the default constraint message and substitutes the error text from the thrown
     * {@link InvalidBicException}.
     */
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext ctx) {
        if (value == null || value.length() == 0) { // CharSequence has no isEmpty()
            return true;
        }

        try {

            Bic.validate(value);
            return true;

        } catch (InvalidBicException ex) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(ex.getReason().getText()).addConstraintViolation();
            return false;
        }
    }

}
