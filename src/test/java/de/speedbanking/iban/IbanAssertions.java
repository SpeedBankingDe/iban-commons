package de.speedbanking.iban;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableTypeAssert;

import java.util.Objects;

/**
 * Entry point for AssertJ custom assertions for the {@link Iban} class,
 * including the concrete assertion implementation {@link IbanAssert}.
 * <p>
 * To use, import statically: {@code import static de.speedbanking.iban.IbanAssertions.assertThat;}
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
     * Factory method required by AssertJ SoftAssertions to inject the SoftAssertions proxy.
     * This method allows custom assertions like {@code IbanAssert} to be used within
     * a {@code SoftAssertions.assertSoftly(...)} block.
     *
     * @param actual the {@link Iban} instance to assert on
     * @param softly the {@link SoftAssertions} instance used for proxy injection
     * @return the custom assertion object, injected with soft assertion behavior
     */
    public static IbanAssert using(Iban actual, SoftAssertions softly) {
        // simply delegate to the IbanAssert constructor.
        // AssertJ handles the necessary proxying internally during the call chain
        return new IbanAssert(actual);
    }

    /**
     * Alias for {@link #assertThatExceptionOfType(Class)} for {@link InvalidIbanException}.
     *
     * @return the created {@link ThrowableTypeAssert}
     */
    public static ThrowableTypeAssert<InvalidIbanException> assertThatInvalidIbanException() {
        return assertThatExceptionOfType(InvalidIbanException.class);
    }

    /**
     * Custom AssertJ assertions for {@link Iban}.
     */
    public static class IbanAssert extends AbstractObjectAssert<IbanAssert, Iban> {

        /**
         * Creates a new {@link IbanAssert}.
         *
         * @param actual the IBAN instance to assert on.
         */
        public IbanAssert(Iban actual) {
            super(actual, IbanAssert.class);
        }

        /**
         * Verifies that the actual IBAN instance has the expected total length.
         *
         * @param expectedLength the expected length of the IBAN string.
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given country code.
         *
         * @param expectedCountryCode the expected country code (e.g., "DE", "FR").
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given country flag.
         *
         * @param expectedCountryFlag the expected country flag string.
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given country name.
         *
         * @param expectedCountryName the expected country name (e.g., "Germany", "France").
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given check digits.
         *
         * @param expectedCheckDigits the expected check digits (e.g., "91").
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given check digits.
         *
         * @param expectedCheckDigits the expected check digits as an integer (e.g., 91, 5).
         * @return This assertion object.
         */
        public IbanAssert hasCheckDigits(int expectedCheckDigits) {
            // The check digits component of an IBAN is always 2 characters long,
            // requiring zero-padding for single-digit inputs (e.g., 5 -> "05").
            String expectedString = String.format("%02d", expectedCheckDigits);

            return hasCheckDigits(expectedString);
        }

        /**
         * Verifies that the actual IBAN instance has the given BBAN (Basic Bank Account Number).
         *
         * @param expectedBban the expected BBAN part.
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given bank code.
         *
         * @param expectedBankCode the expected bank code (e.g., "37040044").
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given branch code.
         *
         * @param expectedBranchCode the expected branch code (may be null).
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given bank and branch code.
         *
         * @param expectedBankAndBranchCode the expected bank and branch code (e.g., "37040044").
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given national check digit (NCD).
         *
         * @param expectedNationalCheckDigit the expected national check digit.
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given account number.
         *
         * @param expectedAccountNumber the expected account number.
         * @return This assertion object.
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
         * Verifies that the actual IBAN instance has the given organisation.
         *
         * @param expectedOrganisation the expected organisation (e.g., "Associazione Bancaria Italiana", "Bundesverband deutscher Banken").
         * @return This assertion object.
         */
        public IbanAssert hasOrganisation(String expectedOrganisation) {
            isNotNull();

            if (!Objects.equals(actual.getOrganisation(), expectedOrganisation)) {
                failWithMessage("Expected organisation to be '%s' but was '%s' for IBAN '%s'",
                    expectedOrganisation, actual.getOrganisation(), actual);
            }

            return myself;
        }

    }

}
