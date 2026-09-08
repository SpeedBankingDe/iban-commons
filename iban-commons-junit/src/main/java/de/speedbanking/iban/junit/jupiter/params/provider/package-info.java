/**
 * JUnit 5 argument providers for IBAN-related parameterized tests.
 * <p>
 * This package follows the naming conventions established by the JUnit 5 framework
 * (cf. {@code org.junit.jupiter.params.provider}) and provides three ready-to-use
 * {@link org.junit.jupiter.params.provider.ArgumentsSource} annotations:
 *
 * <ul>
 *   <li>{@link de.speedbanking.iban.junit.jupiter.params.provider.IbanCountrySource} –
 *       supplies {@link de.speedbanking.iban.IbanRegistry} entries filtered by an
 *       explicit include/exclude country list.</li>
 *   <li>{@link de.speedbanking.iban.junit.jupiter.params.provider.IbanRegistrySource} –
 *       supplies all (or a filtered subset of) {@link de.speedbanking.iban.IbanRegistry}
 *       enum constants.</li>
 *   <li>{@link de.speedbanking.iban.junit.jupiter.params.provider.RandomIbanSource} –
 *       generates a configurable number of random IBAN strings, optionally including a
 *       percentage of deliberately invalid ones.</li>
 * </ul>
 * <p>
 * All three annotations are meta-annotated with
 * {@link org.junit.jupiter.params.provider.ArgumentsSource} and implement
 * {@link org.junit.jupiter.params.provider.ArgumentsProvider} via inner classes,
 * so they are fully compatible with
 * {@link org.junit.jupiter.params.ParameterizedTest}.
 * <p>
 * Example:
 * <pre>{@code
 * @ParameterizedTest
 * @RandomIbanSource(value = {IbanRegistry.DE, IbanRegistry.AT}, count = 200)
 * void myTest(String iban) { ... }
 * }</pre>
 *
 * @since 1.8.6
 */
package de.speedbanking.iban.junit.jupiter.params.provider;
