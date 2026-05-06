package de.speedbanking.iban.junit.jupiter.api;

import static java.util.Objects.requireNonNull;

import de.speedbanking.iban.Iban;
import de.speedbanking.iban.InvalidIbanException;
import de.speedbanking.util.Currency;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableTypeAssert;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Entry point for AssertJ custom assertions for the {@link Iban} class.
 * <p>
 * To use, import statically: {@code import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.*;}
 * <p>
 * The primary entry point is {@link #assertThatIban(CharSequence)}, which accepts any {@link CharSequence}
 * (e.g. a plain {@link String}), performs a null-check, and then parses the value via
 * {@link Iban#of(CharSequence)}. Passing {@code null} is permitted and yields an
 * {@link IbanAssert} on which only {@code isNull()} will succeed (consistent with the AssertJ contract).
 */
public class IbanAssertions extends Assertions {

    /**
     * Creates a new {@link IbanAssert} by parsing the given character sequence via
     * {@link Iban#of(CharSequence)}.
     * <ul>
     *   <li>A {@code null} argument is forwarded as-is; only {@code isNull()} will succeed on the
     *       returned assert — consistent with the AssertJ null-handling contract.
     *   </li>
     *   <li>An invalid IBAN string causes an {@link AssertionError} whose cause is the
     *       {@link InvalidIbanException} thrown by the parser — the error is surfaced before
     *       any assertion method is invoked, which is the standard behaviour for invalid inputs
     *       in custom AssertJ entry points.
     *   </li>
     * </ul>
     *
     * @param actual the IBAN character sequence to assert on, or {@code null}
     * @return the custom assertion object
     *
     * @since 1.8.7
     */
    public static IbanAssert assertThatIban(CharSequence actual) {
        if (actual == null) {
            return new IbanAssert(null);
        }
        try {
            return new IbanAssert(Iban.of(actual));
        } catch (InvalidIbanException ex) {
            throw new AssertionError(ex.getMessage(), ex);
        }
    }

    /**
     * @deprecated Use {@link #assertThatIban(CharSequence)} instead.
     */
    @Deprecated // (since = "1.8.7", forRemoval = true)
    @SuppressWarnings("InlineMeSuggester")
    public static IbanAssert assertThatIbanOf(CharSequence actual) {
        return assertThatIban(actual);
    }

    /**
     * Creates a new {@link IbanAssert} wrapping an already-parsed {@link Iban} instance.
     * <p>
     * This overload is provided for cases where an {@link Iban} object is already at hand
     * (e.g. returned directly by application code under test).
     *
     * @param actual the {@link Iban} instance to assert on, or {@code null}
     * @return the custom assertion object
     *
     * @since 1.8.7
     */
    public static IbanAssert assertThat(Iban actual) {
        return new IbanAssert(actual);
    }

    /**
     * Factory method required by AssertJ {@link SoftAssertions} to inject the SoftAssertions proxy.
     *
     * @param actual the {@link Iban} instance to assert on
     * @param softly the {@link SoftAssertions} instance used for proxy injection
     * @return the custom assertion object
     */
    public static IbanAssert using(Iban actual, SoftAssertions softly) {
        return softly.proxy(IbanAssert.class, Iban.class, actual);
    }

    /**
     * Provides a boolean assertion for {@link Iban#isValid(CharSequence)}.
     *
     * @param ibanValue the IBAN character sequence to validate
     * @return a boolean assertion object
     */
    public static AbstractBooleanAssert<?> assertThatIbanIsValid(CharSequence ibanValue) {
        return assertThat(Iban.isValid(ibanValue));
    }

    /**
     * Provides a typed throwable assertion for {@link InvalidIbanException}.
     *
     * @return a throwable-type assertion scoped to {@link InvalidIbanException}
     */
    public static ThrowableTypeAssert<InvalidIbanException> assertThatInvalidIbanException() {
        return assertThatExceptionOfType(InvalidIbanException.class);
    }

    /**
     * Custom AssertJ assertions for {@link Iban}.
     */
    public static class IbanAssert extends AbstractObjectAssert<IbanAssert, Iban> {

        public IbanAssert(Iban actual) {
            super(actual, IbanAssert.class);
        }

        /**
         * Shared implementation for all field-equality assertions.<br>
         * Calls {@link #isNotNull()}, compares {@code actualValue} to {@code expected} via
         * {@link Objects#equals}, and fails with a consistent message if they differ.
         *
         * @param fieldName   human-readable field label used in the failure message
         * @param expected    the expected value (may be {@code null})
         * @param actualValue the actual value retrieved from the IBAN (may be {@code null})
         * @param <T>         value type
         * @return {@code this} for method chaining
         */
        private <T> IbanAssert assertField(String fieldName, T expected, T actualValue) {
            isNotNull();
            if (!Objects.equals(actualValue, expected)) {
                failWithMessage("Expected %s to be '%s' but was '%s' for IBAN '%s'",
                    fieldName, expected, actualValue, actual);
            }
            return myself;
        }

        /**
         * Asserts that the normalized IBAN string (as returned by {@link Iban#toString()}) equals
         * the expected value.
         *
         * @param expectedNormalizedIban the expected normalized IBAN string, e.g. {@code "DE91100000000123456789"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasNormalizedValue(String expectedNormalizedIban) {
            isNotNull();
            if (!Objects.equals(actual.toString(), expectedNormalizedIban)) {
                failWithMessage("Expected normalized IBAN to be '%s' but was '%s'",
                    expectedNormalizedIban, actual);
            }
            return myself;
        }

        /**
         * Asserts that the formatted IBAN string (as returned by {@link Iban#toFormattedString()})
         * equals the expected value.
         * <p>
         * The formatted string groups the IBAN into blocks of four characters separated by spaces,
         * e.g. {@code "DE91 1000 0000 0123 4567 89"}.
         *
         * @param expectedFormattedString the expected formatted IBAN string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasFormattedString(String expectedFormattedString) {
            return assertField("formatted IBAN", expectedFormattedString, actual.toFormattedString());
        }

        /**
         * Asserts that the component string (as returned by {@link Iban#toComponentString()}) equals
         * the expected value.
         * <p>
         * The component string separates the IBAN into its structural parts (country code, check digits,
         * bank code, branch code, account number, national check digit) with spaces,
         * e.g. {@code "DE 91 10000000 0123456789"}.
         *
         * @param expectedComponentString the expected component string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasComponentString(String expectedComponentString) {
            return assertField("component string", expectedComponentString, actual.toComponentString());
        }

        /**
         * Asserts that the IBAN's total length (excluding spaces) equals the expected value.
         *
         * @param expectedLength the expected length
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasLength(int expectedLength) {
            isNotNull();
            if (actual.length() != expectedLength) {
                failWithMessage("Expected IBAN length to be %d but was %d for IBAN '%s'",
                    expectedLength, actual.length(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the IBAN's two-letter ISO 3166-1 Alpha-2 country code equals the expected value.
         *
         * @param expectedCountryCode the expected country code, e.g. {@code "DE"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCountryCode(String expectedCountryCode) {
            return assertField("country code", expectedCountryCode, actual.getCountryCode());
        }

        /**
         * Asserts that the full English country name associated with this IBAN equals the expected value.
         *
         * @param expectedCountryName the expected country name, e.g. {@code "Germany"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCountryName(String expectedCountryName) {
            return assertField("country name", expectedCountryName, actual.getCountryName());
        }

        /**
         * Asserts that the country flag emoji for this IBAN equals the expected value.
         *
         * @param expectedCountryFlag the expected flag emoji string, e.g. {@code "🇩🇪"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCountryFlag(String expectedCountryFlag) {
            return assertField("country flag", expectedCountryFlag, actual.getCountryFlag());
        }

        /**
         * Asserts that the primary {@link Currency} of this IBAN's country equals the expected value.
         *
         * @param expectedCurrency the expected {@link Currency} constant, e.g. {@link Currency#EUR}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCurrency(Currency expectedCurrency) {
            return assertField("currency", expectedCurrency.getAlphaCode(), actual.getCurrency().getAlphaCode());
        }

        /**
         * Asserts that the ISO 4217 three-letter currency code of this IBAN's country equals
         * the expected value (e.g., {@code "EUR"}, {@code "GBP"}).
         * <p>
         * Convenience alternative to {@link #hasCurrency(Currency)} for cases where the
         * expected value is already available as a {@code String}.
         *
         * @param expectedCurrencyCode the expected ISO 4217 alpha code, e.g. {@code "EUR"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCurrencyCode(String expectedCurrencyCode) {
            return assertField("currency code", expectedCurrencyCode, actual.getCurrencyCode());
        }

        /**
         * Asserts that the IBAN's SEPA participation status matches the expected value.
         *
         * @param expectedSepaParticipation {@code true} if SEPA participation is expected, {@code false} otherwise
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert isSepa(boolean expectedSepaParticipation) {
            isNotNull();
            if (actual.isSepa() != expectedSepaParticipation) {
                failWithMessage("Expected SEPA participation to be '%s' but was '%s' for IBAN '%s'",
                    expectedSepaParticipation, actual.isSepa(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the IBAN's two numeric check digits equal the expected string value.
         *
         * @param expectedCheckDigits the expected check digits as a zero-padded string, e.g. {@code "91"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCheckDigits(String expectedCheckDigits) {
            return assertField("check digits", expectedCheckDigits, actual.getCheckDigits());
        }

        /**
         * Asserts that the IBAN's two numeric check digits equal the expected integer value.
         * The integer is zero-padded to two digits before comparison.
         *
         * @param expectedCheckDigits the expected check digits as an integer, e.g. {@code 91}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCheckDigits(int expectedCheckDigits) {
            return hasCheckDigits(String.format("%02d", expectedCheckDigits));
        }

        /**
         * Asserts that the Basic Bank Account Number (BBAN) part of this IBAN equals the expected value.
         *
         * @param expectedBban the expected BBAN string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasBban(String expectedBban) {
            return assertField("BBAN", expectedBban, actual.getBban());
        }

        /**
         * Asserts that the bank code part of this IBAN equals the expected value.
         *
         * @param expectedBankCode the expected bank code string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasBankCode(String expectedBankCode) {
            return assertField("bank code", expectedBankCode, actual.getBankCode());
        }

        /**
         * Asserts that the branch code part of this IBAN equals the expected value.
         * Pass {@code null} to assert that the country does not define a branch code.
         *
         * @param expectedBranchCode the expected branch code string, or {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasBranchCode(String expectedBranchCode) {
            return assertField("branch code", expectedBranchCode, actual.getBranchCode());
        }

        /**
         * Asserts that the combined bank-and-branch code of this IBAN equals the expected value.
         * If the country defines no separate branch code, this is identical to the bank code.
         *
         * @param expectedBankAndBranchCode the expected combined code string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasBankAndBranchCode(String expectedBankAndBranchCode) {
            return assertField("bank and branch code", expectedBankAndBranchCode, actual.getBankAndBranchCode());
        }

        /**
         * Asserts that the national check digit (NCD) part of this IBAN equals the expected value.
         * Pass {@code null} to assert that the country does not define a national check digit.
         *
         * @param expectedNationalCheckDigit the expected national check digit string, or {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasNationalCheckDigit(String expectedNationalCheckDigit) {
            return assertField("national check digit", expectedNationalCheckDigit, actual.getNationalCheckDigit());
        }

        /**
         * Asserts that the account number part of this IBAN equals the expected value.
         *
         * @param expectedAccountNumber the expected account number string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasAccountNumber(String expectedAccountNumber) {
            return assertField("account number", expectedAccountNumber, actual.getAccountNumber());
        }

        /**
         * Asserts that the name of the IBAN-issuing organisation for this IBAN's country equals
         * the expected value. Pass {@code null} to assert that no organisation is defined.
         *
         * @param expectedOrganisation the expected organisation name, or {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasOrganisation(String expectedOrganisation) {
            return assertField("organisation", expectedOrganisation, actual.getOrganisation());
        }

        /**
         * Asserts that the normalized IBAN string matches the given regular-expression.
         * <p>
         * A {@code null} or empty regex skips the check.
         *
         * @param regex the regular expression to match against, or {@code null} to skip
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert matches(CharSequence regex) {
            isNotNull();
            if (regex != null && regex.length() > 0) {
                return matches(Pattern.compile(regex.toString()));
            }
            return myself;
        }

        /**
         * Asserts that the normalized IBAN string matches the given regular-expression {@link Pattern}.
         * A {@code null} pattern skips the check.
         *
         * @param ibanPattern the pattern to match against, or {@code null} to skip
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert matches(Pattern ibanPattern) {
            isNotNull();
            if (ibanPattern != null && !ibanPattern.matcher(actual.toString()).matches()) {
                failWithMessage("IBAN '%s' does not match pattern '%s'",
                    actual, ibanPattern);
            }
            return myself;
        }

        /**
         * Asserts that this IBAN is less than the given {@code other} IBAN according to
         * {@link Iban#compareTo(Iban)} (i.e. lexicographic order of normalized IBAN strings).
         *
         * @param other the IBAN to compare against; must not be {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert isLessThan(Iban other) {
            isNotNull();
            requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) >= 0) {
                failWithMessage("Expected IBAN '%s' to be less than '%s'",
                    actual, other);
            }
            return myself;
        }

        /**
         * Asserts that this IBAN is less than or equal to the given {@code other} IBAN according to
         * {@link Iban#compareTo(Iban)}.
         *
         * @param other the IBAN to compare against; must not be {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert isLessThanOrEqualTo(Iban other) {
            isNotNull();
            requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) > 0) {
                failWithMessage("Expected IBAN '%s' to be less than or equal to '%s'",
                    actual, other);
            }
            return myself;
        }

        /**
         * Asserts that this IBAN is greater than the given {@code other} IBAN according to
         * {@link Iban#compareTo(Iban)}.
         *
         * @param other the IBAN to compare against; must not be {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert isGreaterThan(Iban other) {
            isNotNull();
            requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) <= 0) {
                failWithMessage("Expected IBAN '%s' to be greater than '%s'",
                    actual, other);
            }
            return myself;
        }

        /**
         * Asserts that this IBAN is greater than or equal to the given {@code other} IBAN according to
         * {@link Iban#compareTo(Iban)}.
         *
         * @param other the IBAN to compare against; must not be {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert isGreaterThanOrEqualTo(Iban other) {
            isNotNull();
            requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) < 0) {
                failWithMessage("Expected IBAN '%s' to be greater than or equal to '%s'",
                    actual, other);
            }
            return myself;
        }

        /**
         * Asserts that this IBAN compares as equal to the given {@code other} IBAN via
         * {@link Iban#compareTo(Iban)}, i.e. {@code compareTo} returns {@code 0}.
         * <p>
         * Note: for value equality prefer the inherited {@link #isEqualTo(Object)} assertion, which
         * uses {@link Iban#equals(Object)}.
         *
         * @param other the IBAN to compare against; must not be {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert isEqualByCompareTo(Iban other) {
            isNotNull();
            requireNonNull(other, "The IBAN to compare against must not be null");
            int cmp = actual.compareTo(other);
            if (cmp != 0) {
                failWithMessage(
                    "Expected IBAN '%s' to compare as equal to '%s' (compareTo == 0) but compareTo returned %d",
                    actual, other, cmp);
            }
            return myself;
        }

    }

}
