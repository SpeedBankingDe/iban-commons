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
 *     ({@link de.speedbanking.util.Mod97#calculateRange(CharSequence, int, int)}),
 *     and boolean convenience wrappers
 *     ({@link de.speedbanking.util.Mod97#isValid(CharSequence)}).
 *     Useful for any identifier based on ISO 7064, such as IBAN or the SEPA Creditor Identifier.
 *   </dd>
 *   <dt>{@link de.speedbanking.util.Country}</dt>
 *   <dd>Enum of all officially assigned ISO 3166-1 Alpha-2 country codes with their English names
 *     and primary currencies. Zero-allocation lookup via
 *     {@link de.speedbanking.util.Country#isAssigned(char, char)} and flag emoji generation via
 *     {@link de.speedbanking.util.Country#createFlagEmoji(String)}.</dd>
 *   <dt>{@link de.speedbanking.util.Currency}</dt>
 *   <dd>Enum of ISO 4217 currency codes linked to their issuing countries.</dd>
 *   <dt>{@link de.speedbanking.util.Continent}</dt>
 *   <dd>Enum of the seven continents of the world.</dd>
 *   <dt>{@link de.speedbanking.util.CharUtil}</dt>
 *   <dd>Low-level ASCII character checks (digit, uppercase, alphanumeric) optimized for
 *     banking identifier validation without regex or locale overhead.</dd>
 *   <dt>{@link de.speedbanking.util.CharArrayWrapper}</dt>
 *   <dd>Light-weight, immutable {@link CharSequence} wrapper for a {@code char} array,
 *     avoiding defensive copying in read-only validation or parsing scenarios.</dd>
 *   <dt>{@link de.speedbanking.util.IndexRange}</dt>
 *   <dd>Immutable half-open interval {@code [begin, end)} for defining BBAN component
 *     positions within an IBAN string.</dd>
 *   <dt>{@link de.speedbanking.util.Alpha2EnumLookup}</dt>
 *   <dd>Allocation-free lookup registry for enums mapped by a two-letter uppercase code,
 *     used internally by {@link de.speedbanking.util.Country} and {@link de.speedbanking.util.Continent}.</dd>
 *   <dt>{@link de.speedbanking.util.PatternCache}</dt>
 *   <dd>Thread-safe cache ensuring each unique regex/flag combination is compiled only once.</dd>
 *   <dt>{@link de.speedbanking.util.RegexSimplifier}</dt>
 *   <dd>Consolidates consecutive same-class regex blocks that use the {@code {n}} quantifier
 *     into a single block.</dd>
 *   <dt>{@link de.speedbanking.util.ValidationError}</dt>
 *   <dd>Common interface implemented by validation error enumerations such as
 *     {@link de.speedbanking.iban.IbanValidationError} and {@link de.speedbanking.bic.BicValidationError}.</dd>
 * </dl>
 */
package de.speedbanking.util;
