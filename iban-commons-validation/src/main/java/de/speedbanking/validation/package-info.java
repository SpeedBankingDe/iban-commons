/**
 * Core package for Jakarta Bean Validation constraints and validators for IBAN and BIC.
 * <p>
 * Provides annotations such as {@link de.speedbanking.validation.ValidIban} and
 * {@link de.speedbanking.validation.ValidBic} to enforce proper financial data format and
 * checksums. {@code null} and empty values are considered valid by both constraints; combine
 * with {@code @NotNull} or {@code @NotBlank} to reject them. Constraint evaluation is delegated
 * to {@link de.speedbanking.validation.IbanConstraintValidator} and
 * {@link de.speedbanking.validation.BicConstraintValidator}, which reuse the validation engines
 * from {@code iban-commons}.
 *
 * @since 1.8.0
 */
package de.speedbanking.validation;
