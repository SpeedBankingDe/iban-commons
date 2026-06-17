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
package de.speedbanking.bic.junit.jupiter.api;

import static java.util.Objects.requireNonNull;

import de.speedbanking.bic.Bic;
import de.speedbanking.bic.InvalidBicException;
import de.speedbanking.util.Currency;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableTypeAssert;

import java.util.Objects;
import java.util.regex.Pattern;
/**
 * Entry point for AssertJ custom assertions for the {@link Bic} class,
 * including the concrete assertion implementation {@link BicAssert}.
 * <p>
 * To use, import statically:
 * <pre>{@code import static de.speedbanking.bic.BicAssertions.*;}</pre>
 * <p>
 * Example usage:
 * <pre>{@code
 * assertThat(Bic.of("MARKDEFF"))
 *     .isBic8()
 *     .hasCountryCode("DE")
 *     .hasBankCode("MARK")
 *     .hasLocationCode("FF")
 *     .hasBic11("MARKDEFFXXX");
 *
 * assertThatBicOf("MARKDEFF500")
 *     .isBic11()
 *     .hasCountryCode("DE");
 *
 * assertThatBicIsValid("MARKDEFF").isTrue();
 * }</pre>
 */
public class BicAssertions extends Assertions {

    /**
     * Creates a new {@link BicAssert} by parsing the given character sequence via
     * {@link Bic#of(CharSequence)}.
     * <p>
     * <ul>
     *   <li>A {@code null} argument is forwarded as-is; only {@code isNull()} will succeed on the
     *       returned assert — consistent with the AssertJ null-handling contract.
     *   </li>
     *   <li>An invalid BIC string causes an {@link AssertionError} whose cause is the
     *       {@link InvalidBicException} thrown by the parser — the error is surfaced before
     *       any assertion method is invoked, which is the standard behaviour for invalid inputs
     *       in custom AssertJ entry points.
     *   </li>
     * </ul>
     *
     * @param actual the BIC character sequence to assert on, or {@code null}
     * @return the custom assertion object
     *
     * @since 1.8.7
     */
    public static BicAssert assertThatBic(CharSequence actual) {
        if (actual == null) {
            return new BicAssert(null);
        }
        try {
            return new BicAssert(Bic.of(actual));
        } catch (InvalidBicException ex) {
            throw new AssertionError(ex.getMessage(), ex);
        }
    }

    /**
     * Creates a new instance of {@link BicAssert}.
     *
     * @param actual the BIC instance to assert on
     * @return the custom assertion object
     */
    public static BicAssert assertThat(Bic actual) {
        return new BicAssert(actual);
    }

    /**
     * Factory method required by AssertJ {@link SoftAssertions} to inject the SoftAssertions proxy.
     * <p>
     * Enables use of {@link BicAssert} within a {@code SoftAssertions} block:
     * <pre>{@code
     * SoftAssertions softly = new SoftAssertions();
     * BicAssertions.using(bic, softly).isBic8().hasCountryCode("DE");
     * softly.assertAll();
     * }</pre>
     *
     * @param actual the {@link Bic} instance to assert on
     * @param softly the {@link SoftAssertions} instance used for proxy injection
     * @return the custom assertion object
     */
    public static BicAssert using(Bic actual, SoftAssertions softly) {
        return softly.proxy(BicAssert.class, Bic.class, actual);
    }

    /**
     * Provides a boolean assertion for {@link Bic#isValid(CharSequence)}.
     *
     * @param bicValue the BIC character sequence to validate
     * @return a boolean assertion object
     */
    public static AbstractBooleanAssert<?> assertThatBicIsValid(CharSequence bicValue) {
        return assertThat(Bic.isValid(bicValue));
    }

    /**
     * Provides a typed throwable assertion for {@link InvalidBicException}.
     * <p>
     * Example:
     * <pre>{@code
     * assertThatInvalidBicException()
     *     .isThrownBy(() -> Bic.of("INVALID"))
     *     .withMessage("BIC has incorrect length");
     * }</pre>
     *
     * @return a throwable-type assertion scoped to {@link InvalidBicException}
     */
    public static ThrowableTypeAssert<InvalidBicException> assertThatInvalidBicException() {
        return assertThatExceptionOfType(InvalidBicException.class);
    }

    /**
     * Custom AssertJ assertions for {@link Bic}.
     */
    public static class BicAssert extends AbstractObjectAssert<BicAssert, Bic> {

        /**
         * Creates a new {@link BicAssert}.
         *
         * @param actual the BIC instance to assert on
         */
        public BicAssert(Bic actual) {
            super(actual, BicAssert.class);
        }

        /**
         * Verifies that the actual BIC is a BIC-8 (length 8, without branch code).
         *
         * @return this assertion object for method chaining
         */
        public BicAssert isBic8() {
            isNotNull();
            if (!actual.isBic8()) {
                failWithMessage("Expected BIC to be BIC-8 (length 8) but was BIC-11 (length 11)");
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC is NOT a BIC-8 (i.e., it is a BIC-11).
         *
         * @return this assertion object for method chaining
         */
        public BicAssert isNotBic8() {
            isNotNull();
            if (actual.isBic8()) {
                failWithMessage("Expected BIC to be BIC-11 (length 11) but was BIC-8 (length 8)");
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC is a BIC-11 (length 11, with branch code).
         *
         * @return this assertion object for method chaining
         */
        public BicAssert isBic11() {
            isNotNull();
            if (!actual.isBic11()) {
                failWithMessage("Expected BIC to be BIC-11 (length 11) but was BIC-8 (length 8)");
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC is NOT a BIC-11 (i.e., it is a BIC-8).
         *
         * @return this assertion object for method chaining
         */
        public BicAssert isNotBic11() {
            isNotNull();
            if (actual.isBic11()) {
                failWithMessage("Expected BIC to be BIC-8 (length 8) but was BIC-11 (length 11)");
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC length matches the expected length.
         *
         * @param expectedLength the expected length (8 or 11)
         * @return this assertion object for method chaining
         */
        public BicAssert hasLength(int expectedLength) {
            isNotNull();
            if (actual.length() != expectedLength) {
                failWithMessage("Expected BIC length to be %d but was %d for BIC '%s'",
                    expectedLength, actual.length(), actual);
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC's {@link Bic#toString()} representation matches
         * the expected string.
         * <p>
         * For BIC-8 instances this is equivalent to {@link #hasBic8(String)};
         * for BIC-11 instances it is equivalent to {@link #hasBic11(String)}.
         *
         * @param expectedToString the expected BIC string (8 or 11 chars, preserving original format)
         * @return this assertion object for method chaining
         */
        @Override
        public BicAssert hasToString(String expectedToString) {
            isNotNull();
            if (!Objects.equals(actual.toString(), expectedToString)) {
                failWithMessage("Expected BIC toString() to be '%s' but was '%s'",
                    expectedToString, actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the BIC-8 representation ({@link Bic#toBic8()}) equals the expected string.
         *
         * @param expectedBic8 the expected 8-character BIC string
         * @return this assertion object for method chaining
         */
        public BicAssert hasBic8(String expectedBic8) {
            isNotNull();
            if (!Objects.equals(actual.toBic8(), expectedBic8)) {
                failWithMessage("Expected BIC-8 to be '%s' but was '%s' for BIC '%s'",
                    expectedBic8, actual.toBic8(), actual);
            }
            return myself;
        }

        /**
         * Verifies that the BIC-11 representation ({@link Bic#toBic11()}) equals the expected string.
         * <p>
         * For a BIC-8 source the {@code "XXX"} head-office suffix is appended automatically;
         * the expected value must include this suffix.
         *
         * @param expectedBic11 the expected 11-character BIC string (incl. branch code)
         * @return this assertion object for method chaining
         */
        public BicAssert hasBic11(String expectedBic11) {
            isNotNull();
            if (!Objects.equals(actual.toBic11(), expectedBic11)) {
                failWithMessage("Expected BIC-11 to be '%s' but was '%s' for BIC '%s'",
                    expectedBic11, actual.toBic11(), actual);
            }
            return myself;
        }

        /**
         * Alias for {@link #hasBic8(String)} — kept for backward compatibility.
         *
         * @param expectedBic8 the expected 8-character BIC string
         * @return this assertion object for method chaining
         * @see #hasBic8(String)
         */
        public BicAssert isBic8EqualTo(String expectedBic8) {
            return hasBic8(expectedBic8);
        }

        /**
         * Alias for {@link #hasBic11(String)} — kept for backward compatibility.
         *
         * @param expectedBic11 the expected normalized 11-character BIC string
         * @return this assertion object for method chaining
         * @see #hasBic11(String)
         */
        public BicAssert isBic11NormalizedEqualTo(String expectedBic11) {
            return hasBic11(expectedBic11);
        }

        /**
         * Verifies that the actual BIC has the given Bank Code (first 4 characters).
         *
         * @param expectedBankCode the expected Bank Code (4 uppercase letters)
         * @return this assertion object for method chaining
         */
        public BicAssert hasBankCode(String expectedBankCode) {
            isNotNull();
            if (!Objects.equals(actual.getBankCode(), expectedBankCode)) {
                failWithMessage("Expected BIC bank code to be '%s' but was '%s' for BIC '%s'",
                    expectedBankCode, actual.getBankCode(), actual);
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC has the given Country Code (positions 5–6).
         *
         * @param expectedCountryCode the expected ISO 3166-1 Alpha-2 Country Code (e.g., {@code "DE"})
         * @return this assertion object for method chaining
         */
        public BicAssert hasCountryCode(String expectedCountryCode) {
            isNotNull();
            if (!Objects.equals(actual.getCountryCode(), expectedCountryCode)) {
                failWithMessage("Expected BIC country code to be '%s' but was '%s' for BIC '%s'",
                    expectedCountryCode, actual.getCountryCode(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the full English country name associated with this BIC equals the expected value.
         *
         * @param expectedCountryName the expected country name, e.g. {@code "Germany"}
         * @return {@code this} assertion object for method chaining
         */
        public BicAssert hasCountryName(String expectedCountryName) {
            isNotNull();
            if (!Objects.equals(actual.getCountryName(), expectedCountryName)) {
                failWithMessage("Expected country name to be '%s' but was '%s' for BIC '%s'",
                    expectedCountryName, actual.getCountryName(), actual);
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC has the given country flag emoji.
         *
         * @param expectedCountryFlag the expected country flag emoji string (e.g., {@code "🇩🇪"})
         * @return this assertion object for method chaining
         */
        public BicAssert hasCountryFlag(String expectedCountryFlag) {
            isNotNull();
            if (!Objects.equals(actual.getCountryFlag(), expectedCountryFlag)) {
                failWithMessage("Expected BIC country flag to be %s but was %s for BIC '%s'",
                    expectedCountryFlag, actual.getCountryFlag(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the primary {@link Currency} of this BIC's country equals the expected value.
         *
         * @param expectedCurrency the expected {@link Currency} constant, e.g. {@link Currency#EUR}
         * @return {@code this} assertion object for method chaining
         */
        public BicAssert hasCurrency(Currency expectedCurrency) {
            isNotNull();
            if (actual.getCurrency() != expectedCurrency) {
                failWithMessage("Expected currency to be '%s' but was '%s' for BIC '%s'",
                    expectedCurrency.getAlphaCode(), actual.getCurrencyCode(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the ISO 4217 three-letter currency code of this BIC's country equals
         * the expected value (e.g., {@code "EUR"}, {@code "GBP"}).
         * <p>
         * Convenience alternative to {@link #hasCurrency(Currency)} for cases where the
         * expected value is already available as a {@code String}.
         *
         * @param expectedCurrencyCode the expected ISO 4217 alpha code, e.g. {@code "EUR"}
         * @return {@code this} assertion object for method chaining
         */
        public BicAssert hasCurrencyCode(String expectedCurrencyCode) {
            isNotNull();
            if (!Objects.equals(actual.getCurrencyCode(), expectedCurrencyCode)) {
                failWithMessage("Expected currency code to be '%s' but was '%s' for BIC '%s'",
                    expectedCurrencyCode, actual.getCurrencyCode(), actual);
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC has the given Location Code (positions 7–8).
         *
         * @param expectedLocationCode the expected Location Code (2 alphanumeric characters)
         * @return this assertion object for method chaining
         */
        public BicAssert hasLocationCode(String expectedLocationCode) {
            isNotNull();
            if (!Objects.equals(actual.getLocationCode(), expectedLocationCode)) {
                failWithMessage("Expected BIC location code to be '%s' but was '%s' for BIC '%s'",
                    expectedLocationCode, actual.getLocationCode(), actual);
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC has the given Branch Code (positions 9–11).
         * <p>
         * Pass {@code null} to assert that the BIC is a BIC-8 (no branch code).
         *
         * @param expectedBranchCode the expected Branch Code (3 alphanumeric characters),
         *                           or {@code null} if a BIC-8 is expected
         * @return this assertion object for method chaining
         */
        public BicAssert hasBranchCode(String expectedBranchCode) {
            isNotNull();
            if (!Objects.equals(actual.getBranchCode(), expectedBranchCode)) {
                failWithMessage("Expected BIC branch code to be '%s' but was '%s' for BIC '%s'",
                    expectedBranchCode, actual.getBranchCode(), actual);
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC has no branch code, i.e. it is a BIC-8.
         * <p>
         * Convenience shorthand for {@code hasBranchCode(null)}.
         *
         * @return this assertion object for method chaining
         */
        public BicAssert hasNoBranchCode() {
            return hasBranchCode(null);
        }

        /**
         * Verifies that the BIC's {@link Bic#toString()} representation matches
         * the given regular-expression {@link Pattern}.
         * A {@code null} pattern skips the check.
         *
         * @param bicPattern the pattern to match against, or {@code null} to skip
         * @return this assertion object for method chaining
         */
        public BicAssert matches(Pattern bicPattern) {
            isNotNull();
            if (bicPattern != null && !bicPattern.matcher(actual.toString()).matches()) {
                failWithMessage("BIC '%s' does not match pattern '%s'", actual, bicPattern);
            }
            return myself;
        }

        /**
         * Verifies that this BIC compares as less than the given {@code other} BIC according to
         * {@link Bic#compareTo(Bic)} (lexicographic order of the normalized 11-character strings).
         *
         * @param other the BIC to compare against; must not be {@code null}
         * @return this assertion object for method chaining
         */
        public BicAssert isLessThan(Bic other) {
            isNotNull();
            requireNonNull(other, "The BIC to compare against must not be null");
            if (actual.compareTo(other) >= 0) {
                failWithMessage("Expected BIC '%s' to be less than '%s'", actual, other);
            }
            return myself;
        }

        /**
         * Verifies that this BIC compares as less than or equal to the given {@code other} BIC
         * according to {@link Bic#compareTo(Bic)}.
         *
         * @param other the BIC to compare against; must not be {@code null}
         * @return this assertion object for method chaining
         */
        public BicAssert isLessThanOrEqualTo(Bic other) {
            isNotNull();
            requireNonNull(other, "The BIC to compare against must not be null");
            if (actual.compareTo(other) > 0) {
                failWithMessage("Expected BIC '%s' to be less than or equal to '%s'", actual, other);
            }
            return myself;
        }

        /**
         * Verifies that this BIC compares as greater than the given {@code other} BIC according to
         * {@link Bic#compareTo(Bic)}.
         *
         * @param other the BIC to compare against; must not be {@code null}
         * @return this assertion object for method chaining
         */
        public BicAssert isGreaterThan(Bic other) {
            isNotNull();
            requireNonNull(other, "The BIC to compare against must not be null");
            if (actual.compareTo(other) <= 0) {
                failWithMessage("Expected BIC '%s' to be greater than '%s'", actual, other);
            }
            return myself;
        }

        /**
         * Verifies that this BIC compares as greater than or equal to the given {@code other} BIC
         * according to {@link Bic#compareTo(Bic)}.
         *
         * @param other the BIC to compare against; must not be {@code null}
         * @return this assertion object for method chaining
         */
        public BicAssert isGreaterThanOrEqualTo(Bic other) {
            isNotNull();
            requireNonNull(other, "The BIC to compare against must not be null");
            if (actual.compareTo(other) < 0) {
                failWithMessage("Expected BIC '%s' to be greater than or equal to '%s'", actual, other);
            }
            return myself;
        }

        /**
         * Verifies that this BIC compares as equal to the given {@code other} BIC via
         * {@link Bic#compareTo(Bic)}, i.e. {@code compareTo} returns {@code 0}.
         * <p>
         * Note: for value equality prefer the inherited {@link #isEqualTo(Object)} assertion,
         * which uses {@link Bic#equals(Object)}.
         *
         * @param other the BIC to compare against; must not be {@code null}
         * @return this assertion object for method chaining
         */
        public BicAssert isEqualByCompareTo(Bic other) {
            isNotNull();
            requireNonNull(other, "The BIC to compare against must not be null");
            if (actual.compareTo(other) != 0) {
                failWithMessage(
                    "Expected BIC '%s' to compare as equal to '%s' (compareTo == 0) but compareTo returned %d",
                    actual, other, actual.compareTo(other));
            }
            return myself;
        }

    }

}
