/**
 * Internal utilities specifically designed for IBAN pattern processing and validation.
 * <p>
 * {@link de.speedbanking.iban.util.IbanCharType} classifies BBAN pattern placeholder characters
 * (e.g. {@code n}, {@code a}, {@code c}), while
 * {@link de.speedbanking.iban.util.IbanPatternConverter} translates a country's BBAN pattern into
 * an equivalent regular expression. {@link de.speedbanking.iban.util.InvalidBaseException} is
 * thrown when such conversions encounter an unsupported or malformed pattern.
 *
 * @since 1.8.0
 */
package de.speedbanking.iban.util;
