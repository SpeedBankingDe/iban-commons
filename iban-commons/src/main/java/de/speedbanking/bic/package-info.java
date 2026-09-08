/**
 * Core package for Business Identifier Code (BIC / SWIFT Code) representation and validation
 * according to <strong>ISO 9362</strong>.
 * <p>
 * Central entry point is the immutable {@link de.speedbanking.bic.Bic} model, created via its
 * static factory methods after successful validation by {@link de.speedbanking.bic.BicValidator}.
 * Validation failures are reported either as a {@link de.speedbanking.bic.BicValidationResult}
 * (non-throwing) carrying a {@link de.speedbanking.bic.BicValidationError}, or as an
 * {@link de.speedbanking.bic.InvalidBicException} for the strict factory methods.
 * {@link de.speedbanking.bic.RandomBic} generates syntactically valid BICs for testing purposes.
 *
 * @since 1.8.0
 */
package de.speedbanking.bic;
