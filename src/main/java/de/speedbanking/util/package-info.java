/**
 * Contains shared utility classes used across the library.
 *
 * <h2>Key classes</h2>
 * <dl>
 *   <dt>{@link de.speedbanking.util.Mod97}</dt>
 *   <dd>
 *     Public implementation of the <strong>ISO 7064 Mod 97-10</strong> checksum algorithm.
 *     Provides IBAN-style calculation with header rearrangement
 *     ({@link de.speedbanking.util.Mod97#calculate(CharSequence)}),
 *     a straight range variant for NCD sub-sequences
 *     ({@link de.speedbanking.util.Mod97#calculateRange(char[], int, int)}),
 *     and boolean convenience wrappers
 *     ({@link de.speedbanking.util.Mod97#isValid(CharSequence)}).
 *     Useful for any identifier based on ISO 7064, such as IBAN or the SEPA Creditor Identifier.
 *   </dd>
 *   <dt>{@link de.speedbanking.util.Iso3166Alpha2}</dt>
 *   <dd>Enum of all officially assigned ISO 3166-1 Alpha-2 country codes with their English names
 *     and primary currencies. Zero-allocation lookup via
 *     {@link de.speedbanking.util.Iso3166Alpha2#isAssigned(char, char)}.</dd>
 *   <dt>{@link de.speedbanking.util.Currency}</dt>
 *   <dd>Enum of ISO 4217 currency codes linked to their issuing countries.</dd>
 *   <dt>{@link de.speedbanking.util.CharUtil}</dt>
 *   <dd>Low-level ASCII character checks (digit, uppercase, alphanumeric) optimised for
 *     banking identifier validation without regex or locale overhead.</dd>
 *   <dt>{@link de.speedbanking.util.IndexRange}</dt>
 *   <dd>Immutable half-open interval {@code [begin, end)} for defining BBAN component
 *     positions within an IBAN string.</dd>
 *   <dt>{@link de.speedbanking.util.CountryUtil}</dt>
 *   <dd>Helpers for country code validation and flag emoji generation.</dd>
 * </dl>
 */
package de.speedbanking.util;
