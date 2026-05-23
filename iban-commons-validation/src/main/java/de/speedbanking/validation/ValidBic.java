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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Validates that the annotated {@link CharSequence} is a syntactically valid BIC
 * (Business Identifier Code) according to ISO 9362.
 * <p>
 * {@code null} and empty values are considered valid — combine with {@code @NotNull} or
 * {@code @NotBlank} to reject them.
 * <p>
 * The constraint violation message is the English error text from
 * {@link de.speedbanking.bic.BicValidationError#getText()}, e.g.
 * {@code "BIC has invalid country code"}.
 *
 * @see ValidIban
 * @since 1.8.8
 */
@Documented
@Constraint(validatedBy = BicConstraintValidator.class)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
@Repeatable(ValidBic.List.class)
public @interface ValidBic {

    /**
     * The constraint violation message template.<br>
     * Overridden at runtime with the specific {@link de.speedbanking.bic.BicValidationError} text.
     */
    String message() default "Invalid BIC";

    /** Bean validation groups. */
    Class<?>[] groups() default {};

    /** Bean validation payload. */
    Class<? extends Payload>[] payload() default {};

    /**
     * Defines several {@link ValidBic} constraints on the same element.
     */
    @Documented
    @Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE })
    @Retention(RUNTIME)
    @interface List {
        ValidBic[] value();
    }

}
