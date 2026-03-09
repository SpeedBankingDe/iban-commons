package de.speedbanking.iban;

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
     * Factory method required by AssertJ SoftAssertions to inject the SoftAssertions proxy.
     *
     * @param actual the {@link Iban} instance to assert on
     * @param softly the {@link SoftAssertions} instance used for proxy injection
     * @return the custom assertion object
     */
    public static IbanAssert using(Iban actual, SoftAssertions softly) {
        return new IbanAssert(actual);
    }

    // --- Static Utility Assertions for Iban Factory Methods ---

    public static IbanAssert assertThatIbanOf(CharSequence ibanValue) {
        return assertThat(Iban.of(ibanValue));
    }

    public static IbanAssert assertThatIbanOfNormalized(CharSequence ibanValue) {
        return assertThat(Iban.ofNormalized(ibanValue));
    }

    /**
     * Provides a boolean assertion for {@link Iban#isValid(CharSequence)}.
     */
    public static AbstractBooleanAssert<?> assertThatIbanIsValid(CharSequence ibanValue) {
        return assertThat(Iban.isValid(ibanValue));
    }

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

        public IbanAssert hasLength(int expectedLength) {
            isNotNull();
            if (actual.length() != expectedLength) {
                failWithMessage("Expected IBAN length to be %d but was %d for IBAN '%s'",
                    expectedLength, actual.length(), actual);
            }
            return myself;
        }

        public IbanAssert hasCountryCode(String expectedCountryCode) {
            isNotNull();
            if (!Objects.equals(actual.getCountryCode(), expectedCountryCode)) {
                failWithMessage("Expected country code to be '%s' but was '%s' for IBAN '%s'",
                    expectedCountryCode, actual.getCountryCode(), actual);
            }
            return myself;
        }

        public IbanAssert hasCountryName(String expectedCountryName) {
            isNotNull();
            if (!Objects.equals(actual.getCountryName(), expectedCountryName)) {
                failWithMessage("Expected country name to be '%s' but was '%s' for IBAN '%s'",
                    expectedCountryName, actual.getCountryName(), actual);
            }
            return myself;
        }

        public IbanAssert hasCountryFlag(String expectedCountryFlag) {
            isNotNull();
            if (!Objects.equals(actual.getCountryFlag(), expectedCountryFlag)) {
                failWithMessage("Expected country flag to be '%s' but was '%s' for IBAN '%s'",
                    expectedCountryFlag, actual.getCountryFlag(), actual);
            }
            return myself;
        }

        public IbanAssert isSepa(boolean expectedSepaParticipation) {
            isNotNull();
            if (!Objects.equals(actual.isSepa(), expectedSepaParticipation)) {
                failWithMessage("Expected SEPA participation to be '%s' but was '%s' for IBAN '%s'",
                    expectedSepaParticipation, actual.isSepa(), actual);
            }
            return myself;
        }

        public IbanAssert hasCheckDigits(String expectedCheckDigits) {
            isNotNull();
            if (!Objects.equals(actual.getCheckDigits(), expectedCheckDigits)) {
                failWithMessage("Expected check digits to be '%s' but was '%s' for IBAN '%s'",
                    expectedCheckDigits, actual.getCheckDigits(), actual);
            }
            return myself;
        }

        public IbanAssert hasCheckDigits(int expectedCheckDigits) {
            return hasCheckDigits(String.format("%02d", expectedCheckDigits));
        }

        public IbanAssert hasBban(String expectedBban) {
            isNotNull();
            if (!Objects.equals(actual.getBban(), expectedBban)) {
                failWithMessage("Expected BBAN to be '%s' but was '%s' for IBAN '%s'",
                    expectedBban, actual.getBban(), actual);
            }
            return myself;
        }

        public IbanAssert hasBankCode(String expectedBankCode) {
            isNotNull();
            if (!Objects.equals(actual.getBankCode(), expectedBankCode)) {
                failWithMessage("Expected bank code to be '%s' but was '%s' for IBAN '%s'",
                    expectedBankCode, actual.getBankCode(), actual);
            }
            return myself;
        }

        public IbanAssert hasBranchCode(String expectedBranchCode) {
            isNotNull();
            if (!Objects.equals(actual.getBranchCode(), expectedBranchCode)) {
                failWithMessage("Expected branch code to be '%s' but was '%s' for IBAN '%s'",
                    expectedBranchCode, actual.getBranchCode(), actual);
            }
            return myself;
        }

        public IbanAssert hasBankAndBranchCode(String expectedBankAndBranchCode) {
            isNotNull();
            if (!Objects.equals(actual.getBankAndBranchCode(), expectedBankAndBranchCode)) {
                failWithMessage("Expected bank and branch code to be '%s' but was '%s' for IBAN '%s'",
                    expectedBankAndBranchCode, actual.getBankAndBranchCode(), actual);
            }
            return myself;
        }

        public IbanAssert hasNationalCheckDigit(String expectedNationalCheckDigit) {
            isNotNull();
            if (!Objects.equals(actual.getNationalCheckDigit(), expectedNationalCheckDigit)) {
                failWithMessage("Expected national check digit to be '%s' but was '%s' for IBAN '%s'",
                    expectedNationalCheckDigit, actual.getNationalCheckDigit(), actual);
            }
            return myself;
        }

        public IbanAssert hasAccountNumber(String expectedAccountNumber) {
            isNotNull();
            if (!Objects.equals(actual.getAccountNumber(), expectedAccountNumber)) {
                failWithMessage("Expected account number to be '%s' but was '%s' for IBAN '%s'",
                    expectedAccountNumber, actual.getAccountNumber(), actual);
            }
            return myself;
        }

        public IbanAssert hasOrganisation(String expectedOrganisation) {
            isNotNull();
            if (!Objects.equals(actual.getOrganisation(), expectedOrganisation)) {
                failWithMessage("Expected organisation to be '%s' but was '%s' for IBAN '%s'",
                    expectedOrganisation, actual.getOrganisation(), actual);
            }
            return myself;
        }

        public IbanAssert matches(Pattern ibanPattern) {
            isNotNull();
            if (ibanPattern != null && !ibanPattern.matcher(actual.toString()).matches()) {
                failWithMessage("IBAN '%s' does not match pattern '%s'",
                    actual, ibanPattern);
            }
            return myself;
        }

    }

}
