/**
 * Core package for International Bank Account Number (IBAN) representation, validation, and
 * registry services according to <strong>ISO 13616</strong>.
 * <p>
 * Central entry point is the immutable {@link de.speedbanking.iban.Iban} model. Its structural
 * rules per country are held by {@link de.speedbanking.iban.IbanRegistry}, which also provides
 * access to {@link de.speedbanking.iban.IbanBuilder country builders} for constructing IBANs from
 * their components. Validation is performed by {@link de.speedbanking.iban.IbanValidator} through
 * a fail-fast pipeline of country-specific {@link de.speedbanking.iban.CountryValidator}
 * implementations (see {@link de.speedbanking.iban.CountryValidators}), optionally combined with a
 * {@link de.speedbanking.iban.NationalCheckDigitCalculator} (see
 * {@link de.speedbanking.iban.NationalCheckDigitCalculators}).
 * <p>
 * Further supporting classes:
 * <ul>
 *   <li>{@link de.speedbanking.iban.IbanConfig} – immutable, initialize-once global configuration.</li>
 *   <li>{@link de.speedbanking.iban.Formatter} – formats a normalized IBAN into the standard
 *       display form (space-separated groups).</li>
 *   <li>{@link de.speedbanking.iban.RandomIban} – generates syntactically valid IBANs for testing.</li>
 *   <li>{@link de.speedbanking.iban.IbanPlusKey} – derives lookup keys for the SWIFT IBAN Plus
 *       service, supporting BIC derivation from an IBAN.</li>
 *   <li>{@link de.speedbanking.iban.InvalidIbanException} – thrown by the strict
 *       {@link de.speedbanking.iban.Iban#of(CharSequence)} factory method on validation failure.</li>
 * </ul>
 *
 * @since 1.8.0
 */
package de.speedbanking.iban;
