/**
 * German bank account number ("Kontonummer") check digit methods.
 * <p>
 * Implements the catalogue of check digit computation methods maintained by the
 * <strong>Deutsche Bundesbank</strong> under the title
 * <em>"Prüfzifferberechnungsmethoden in der deutschen Kreditwirtschaft"</em>
 * (methods {@code 00}–{@code 99}, {@code A0}–{@code D9}). Each method verifies
 * whether a check digit embedded in a 10-digit domestic account number is
 * internally consistent — <em>not</em> whether the account actually exists.
 * <p>
 * 122 of the 127 assignable method codes from the reference source are implemented —
 * see the "Coverage" section on
 * {@link de.speedbanking.checkdigit.de.GermanCheckDigitMethod} for exactly which five
 * are left out and why.
 * <p>
 * This module is deliberately standalone and does not depend on {@code iban-commons}:
 * a caller supplies the Bankleitzahl (BLZ), the account number, and the method code
 * to use. The method code itself is bank-specific and must be obtained from elsewhere
 * (e.g. the Bundesbank BLZ master-data file) — this module only executes a given
 * method, it does not select one.
 *
 * <h2>Key classes</h2>
 * <dl>
 *   <dt>{@link de.speedbanking.checkdigit.de.GermanAccountCheckDigit}</dt>
 *   <dd>Public entry point. Parses a BLZ/account-number pair and delegates to the
 *     {@link de.speedbanking.checkdigit.de.GermanCheckDigitMethod} identified by its code.</dd>
 *   <dt>{@link de.speedbanking.checkdigit.de.GermanCheckDigitMethod}</dt>
 *   <dd>Enum of individual check digit algorithms, one constant per Bundesbank method code.</dd>
 *   <dt>{@link de.speedbanking.checkdigit.de.CheckDigitResult}</dt>
 *   <dd>Outcome of a check digit computation, distinguishing a verified pass/fail
 *     from methods that perform no verification at all.</dd>
 * </dl>
 *
 * @see <a href="https://www.bundesbank.de/de/startseite/pruefzifferberechnungsmethoden-603320">
 *      Deutsche Bundesbank: Prüfzifferberechnungsmethoden</a>
 */
package de.speedbanking.checkdigit.de;
