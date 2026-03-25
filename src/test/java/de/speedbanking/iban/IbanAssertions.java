package de.speedbanking.iban;

import de.speedbanking.util.Currency;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.OptionalAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableTypeAssert;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Entry point for AssertJ custom assertions for the {@link Iban} class.
 * <p>
 * To use, import statically: {@code import static de.speedbanking.iban.IbanAssertions.*;}
 */
public class IbanAssertions extends Assertions {

    /**
     * Creates a new instance of {@link IbanAssert}.
     *
     * @param actual the IBAN instance to assert on
     * @return the custom assertion object
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

    // --- Static Utility Assertions for Iban Factory Methods ---

    /**
     * Creates an {@link IbanAssert} by parsing the given IBAN string via {@link Iban#of(CharSequence)}.
     * Spaces are permitted; normalization is applied automatically.
     *
     * @param ibanValue the IBAN character sequence to parse
     * @return the custom assertion object
     * @throws InvalidIbanException if the IBAN is invalid
     */
    public static IbanAssert assertThatIbanOf(CharSequence ibanValue) {
        return assertThat(Iban.of(ibanValue));
    }

    /**
     * Creates an {@link IbanAssert} by parsing the given pre-normalized IBAN string
     * via {@link Iban#ofNormalized(CharSequence)}.
     * The input is expected to be uppercase and free of spaces.
     *
     * @param ibanValue the normalized IBAN character sequence to parse
     * @return the custom assertion object
     * @throws InvalidIbanException if the IBAN is invalid
     */
    public static IbanAssert assertThatIbanOfNormalized(CharSequence ibanValue) {
        return assertThat(Iban.ofNormalized(ibanValue));
    }

    /**
     * Creates an {@link IbanAssert} by parsing the given IBAN string via {@link Iban#parse(CharSequence)}.
     * Functionally equivalent to {@link #assertThatIbanOf(CharSequence)}; provided for symmetry
     * with the {@code Iban} API.
     *
     * @param ibanValue the IBAN character sequence to parse
     * @return the custom assertion object
     * @throws InvalidIbanException if the IBAN is invalid
     */
    public static IbanAssert assertThatIbanParse(CharSequence ibanValue) {
        return assertThat(Iban.parse(ibanValue));
    }

    /**
     * Creates an {@link AbstractObjectAssert} for the {@link Optional} returned by
     * {@link Iban#tryParse(CharSequence)}, allowing assertions on the Optional itself
     * (e.g. {@code isPresent()}, {@code isEmpty()}, {@code hasValueSatisfying(…)}).
     *
     * @param ibanValue the IBAN character sequence to parse
     * @return an Optional assertion object
     */
    public static OptionalAssert<Iban> assertThatIbanTryParse(CharSequence ibanValue) {
        return assertThat(Iban.tryParse(ibanValue));
    }

    /**
     * Creates an {@link IbanAssert} for a valid IBAN parsed via {@link Iban#tryParse(CharSequence)}.
     * The Optional must be present; if it is empty the assertion fails immediately.
     *
     * @param ibanValue the IBAN character sequence to parse
     * @return the custom assertion object
     */
    public static IbanAssert assertThatIbanTryParseValue(CharSequence ibanValue) {
        Optional<Iban> result = Iban.tryParse(ibanValue);
        if (!result.isPresent()) {
            throw new AssertionError("Expected Iban.tryParse(\"" + ibanValue + "\") to return a non-empty Optional, but it was empty.");
        }
        return assertThat(result.get());
    }

    /**
     * Creates an {@link IbanAssert} for a valid IBAN parsed via {@link Iban#tryParseOrNull(CharSequence)}.
     * The result must be non-null; if it is null the assertion fails immediately.
     *
     * @param ibanValue the IBAN character sequence to parse
     * @return the custom assertion object
     */
    public static IbanAssert assertThatIbanTryParseOrNull(CharSequence ibanValue) {
        Iban result = Iban.tryParseOrNull(ibanValue);
        if (result == null) {
            throw new AssertionError("Expected Iban.tryParseOrNull(\"" + ibanValue + "\") to return a non-null Iban, but it returned null.");
        }
        return assertThat(result);
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

    // -------------------------------------------------------------------------

    /**
     * Custom AssertJ assertions for {@link Iban}.
     */
    public static class IbanAssert extends AbstractObjectAssert<IbanAssert, Iban> {

        public IbanAssert(Iban actual) {
            super(actual, IbanAssert.class);
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
            isNotNull();
            if (!Objects.equals(actual.toFormattedString(), expectedFormattedString)) {
                failWithMessage("Expected formatted IBAN to be '%s' but was '%s' for IBAN '%s'",
                    expectedFormattedString, actual.toFormattedString(), actual);
            }
            return myself;
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
            isNotNull();
            if (!Objects.equals(actual.toComponentString(), expectedComponentString)) {
                failWithMessage("Expected component string to be '%s' but was '%s' for IBAN '%s'",
                    expectedComponentString, actual.toComponentString(), actual);
            }
            return myself;
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
            isNotNull();
            if (!Objects.equals(actual.getCountryCode(), expectedCountryCode)) {
                failWithMessage("Expected country code to be '%s' but was '%s' for IBAN '%s'",
                    expectedCountryCode, actual.getCountryCode(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the full English country name associated with this IBAN equals the expected value.
         *
         * @param expectedCountryName the expected country name, e.g. {@code "Germany"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCountryName(String expectedCountryName) {
            isNotNull();
            if (!Objects.equals(actual.getCountryName(), expectedCountryName)) {
                failWithMessage("Expected country name to be '%s' but was '%s' for IBAN '%s'",
                    expectedCountryName, actual.getCountryName(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the country flag emoji for this IBAN equals the expected value.
         *
         * @param expectedCountryFlag the expected flag emoji string, e.g. {@code "🇩🇪"}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCountryFlag(String expectedCountryFlag) {
            isNotNull();
            if (!Objects.equals(actual.getCountryFlag(), expectedCountryFlag)) {
                failWithMessage("Expected country flag to be '%s' but was '%s' for IBAN '%s'",
                    expectedCountryFlag, actual.getCountryFlag(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the primary {@link Currency} of this IBAN's country equals the expected value.
         *
         * @param expectedCurrency the expected {@link Currency} constant, e.g. {@link Currency#EUR}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasCurrency(Currency expectedCurrency) {
            isNotNull();
            if (!Objects.equals(actual.getCurrency(), expectedCurrency)) {
                failWithMessage("Expected currency to be '%s' but was '%s' for IBAN '%s'",
                    expectedCurrency, actual.getCurrency(), actual);
            }
            return myself;
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
            isNotNull();
            if (!Objects.equals(actual.getCurrencyCode(), expectedCurrencyCode)) {
                failWithMessage("Expected currency code to be '%s' but was '%s' for IBAN '%s'",
                    expectedCurrencyCode, actual.getCurrencyCode(), actual);
            }
            return myself;
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
            isNotNull();
            if (!Objects.equals(actual.getCheckDigits(), expectedCheckDigits)) {
                failWithMessage("Expected check digits to be '%s' but was '%s' for IBAN '%s'",
                    expectedCheckDigits, actual.getCheckDigits(), actual);
            }
            return myself;
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
            isNotNull();
            if (!Objects.equals(actual.getBban(), expectedBban)) {
                failWithMessage("Expected BBAN to be '%s' but was '%s' for IBAN '%s'",
                    expectedBban, actual.getBban(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the bank code part of this IBAN equals the expected value.
         *
         * @param expectedBankCode the expected bank code string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasBankCode(String expectedBankCode) {
            isNotNull();
            if (!Objects.equals(actual.getBankCode(), expectedBankCode)) {
                failWithMessage("Expected bank code to be '%s' but was '%s' for IBAN '%s'",
                    expectedBankCode, actual.getBankCode(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the branch code part of this IBAN equals the expected value.
         * Pass {@code null} to assert that the country does not define a branch code.
         *
         * @param expectedBranchCode the expected branch code string, or {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasBranchCode(String expectedBranchCode) {
            isNotNull();
            if (!Objects.equals(actual.getBranchCode(), expectedBranchCode)) {
                failWithMessage("Expected branch code to be '%s' but was '%s' for IBAN '%s'",
                    expectedBranchCode, actual.getBranchCode(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the combined bank-and-branch code of this IBAN equals the expected value.
         * If the country defines no separate branch code, this is identical to the bank code.
         *
         * @param expectedBankAndBranchCode the expected combined code string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasBankAndBranchCode(String expectedBankAndBranchCode) {
            isNotNull();
            if (!Objects.equals(actual.getBankAndBranchCode(), expectedBankAndBranchCode)) {
                failWithMessage("Expected bank and branch code to be '%s' but was '%s' for IBAN '%s'",
                    expectedBankAndBranchCode, actual.getBankAndBranchCode(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the national check digit (NCD) part of this IBAN equals the expected value.
         * Pass {@code null} to assert that the country does not define a national check digit.
         *
         * @param expectedNationalCheckDigit the expected national check digit string, or {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasNationalCheckDigit(String expectedNationalCheckDigit) {
            isNotNull();
            if (!Objects.equals(actual.getNationalCheckDigit(), expectedNationalCheckDigit)) {
                failWithMessage("Expected national check digit to be '%s' but was '%s' for IBAN '%s'",
                    expectedNationalCheckDigit, actual.getNationalCheckDigit(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the account number part of this IBAN equals the expected value.
         *
         * @param expectedAccountNumber the expected account number string
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasAccountNumber(String expectedAccountNumber) {
            isNotNull();
            if (!Objects.equals(actual.getAccountNumber(), expectedAccountNumber)) {
                failWithMessage("Expected account number to be '%s' but was '%s' for IBAN '%s'",
                    expectedAccountNumber, actual.getAccountNumber(), actual);
            }
            return myself;
        }

        /**
         * Asserts that the name of the IBAN-issuing organisation for this IBAN's country equals
         * the expected value. Pass {@code null} to assert that no organisation is defined.
         *
         * @param expectedOrganisation the expected organisation name, or {@code null}
         * @return {@code this} assertion object for method chaining
         */
        public IbanAssert hasOrganisation(String expectedOrganisation) {
            isNotNull();
            if (!Objects.equals(actual.getOrganisation(), expectedOrganisation)) {
                failWithMessage("Expected organisation to be '%s' but was '%s' for IBAN '%s'",
                    expectedOrganisation, actual.getOrganisation(), actual);
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
            Objects.requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) >= 0) {
                failWithMessage("Expected IBAN '%s' to be less than '%s' but it was not",
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
            Objects.requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) > 0) {
                failWithMessage("Expected IBAN '%s' to be less than or equal to '%s' but it was not",
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
            Objects.requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) <= 0) {
                failWithMessage("Expected IBAN '%s' to be greater than '%s' but it was not",
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
            Objects.requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) < 0) {
                failWithMessage("Expected IBAN '%s' to be greater than or equal to '%s' but it was not",
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
            Objects.requireNonNull(other, "The IBAN to compare against must not be null");
            if (actual.compareTo(other) != 0) {
                failWithMessage("Expected IBAN '%s' to compare as equal to '%s' (compareTo == 0) but compareTo returned %d",
                    actual, other, actual.compareTo(other));
            }
            return myself;
        }

    }

}
