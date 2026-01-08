package de.speedbanking.bic;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableTypeAssert;

import java.util.Objects;

/**
 * Entry point for AssertJ custom assertions for the {@link Bic} class,
 * including the concrete assertion implementation {@link BicAssert}.
 *
 * <p>To use, import statically: {@code import static de.speedbanking.bic.BicAssertions.assertThat;}
 */
public class BicAssertions extends Assertions {

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
     * Alias for {@link #assertThatExceptionOfType(Class)} for {@link InvalidBicException}.
     *
     * @return the created {@link ThrowableTypeAssert}
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
         * @param actual the BIC instance to assert on.
         */
        public BicAssert(Bic actual) {
            super(actual, BicAssert.class);
        }

        /**
         * Verifies that the actual BIC is a BIC-8 (length 8).
         *
         * @return This assertion object.
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
         * @return This assertion object.
         */
        public BicAssert isNotBic8() {
            isNotNull();
            if (actual.isBic8()) {
                failWithMessage("Expected BIC to be BIC-11 (length 11) but was BIC-8 (length 8)");
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC is a BIC-11 (length 11).
         *
         * @return This assertion object.
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
         * @return This assertion object.
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
         * @param expectedLength the expected length (8 or 11).
         * @return This assertion object.
         */
        public BicAssert hasLength(int expectedLength) {
            isNotNull();
            if (actual.length() != expectedLength) {
                failWithMessage("Expected BIC length to be <%d> but was <%d>",
                        expectedLength, actual.length());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC's toString() representation matches the expected string.
         *
         * @param expectedToString the expected BIC string (8 or 11 chars, matching original input).
         * @return This assertion object.
         */
        @Override
        public BicAssert hasToString(String expectedToString) {
            isNotNull();
            if (!Objects.equals(actual.toString(), expectedToString)) {
                failWithMessage("Expected BIC toString() to be <%s> but was <%s>",
                        expectedToString, actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC instance has the given Bank Code (first 4 characters).
         *
         * @param expectedBankCode the expected Bank Code.
         * @return This assertion object.
         */
        public BicAssert hasBankCode(String expectedBankCode) {
            isNotNull();
            if (!Objects.equals(actual.getBankCode(), expectedBankCode)) {
                failWithMessage("Expected BIC Bank Code to be <%s> but was <%s> for BIC <%s>",
                        expectedBankCode, actual.getBankCode(), actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC instance has the given Country Code (positions 5-6).
         *
         * @param expectedCountryCode the expected Country Code (ISO 3166-1 Alpha-2).
         * @return This assertion object.
         */
        public BicAssert hasCountryCode(String expectedCountryCode) {
            isNotNull();
            if (!Objects.equals(actual.getCountryCode(), expectedCountryCode)) {
                failWithMessage("Expected BIC Country Code to be <%s> but was <%s> for BIC <%s>",
                        expectedCountryCode, actual.getCountryCode(), actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC instance has the given country flag emoji.
         *
         * @param expectedCountryFlag the expected country flag emoji.
         * @return This assertion object.
         */
        public BicAssert hasCountryFlag(String expectedCountryFlag) {
            isNotNull();
            if (!Objects.equals(actual.getCountryFlag(), expectedCountryFlag)) {
                failWithMessage("Expected BIC Country Flag to be <%s> but was <%s> for BIC <%s>",
                        expectedCountryFlag, actual.getCountryFlag(), actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC instance has the given Location Code (positions 7-8).
         *
         * @param expectedLocationCode the expected Location Code.
         * @return This assertion object.
         */
        public BicAssert hasLocationCode(String expectedLocationCode) {
            isNotNull();
            if (!Objects.equals(actual.getLocationCode(), expectedLocationCode)) {
                failWithMessage("Expected BIC Location Code to be <%s> but was <%s> for BIC <%s>",
                        expectedLocationCode, actual.getLocationCode(), actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC instance has the given Branch Code (positions 9-11).
         * {@code null} is expected for a BIC-8.
         *
         * @param expectedBranchCode the expected Branch Code, or {@code null} if BIC-8.
         * @return This assertion object.
         */
        public BicAssert hasBranchCode(String expectedBranchCode) {
            isNotNull();
            if (!Objects.equals(actual.getBranchCode(), expectedBranchCode)) {
                failWithMessage("Expected BIC Branch Code to be <%s> but was <%s> for BIC <%s>",
                        expectedBranchCode, actual.getBranchCode(), actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC's BIC-11 normalized string matches the expected string.
         *
         * @param expectedBic11 the expected normalized BIC-11 string.
         * @return This assertion object.
         */
        public BicAssert isBic11NormalizedEqualTo(String expectedBic11) {
            isNotNull();
            if (!Objects.equals(actual.toBic11(), expectedBic11)) {
                failWithMessage("Expected BIC-11 normalized string to be <%s> but was <%s> for BIC <%s>",
                        expectedBic11, actual.toBic11(), actual.toString());
            }
            return myself;
        }

        /**
         * Verifies that the actual BIC's BIC-8 string matches the expected string.
         *
         * @param expectedBic8 the expected BIC-8 string.
         * @return This assertion object.
         */
        public BicAssert isBic8EqualTo(String expectedBic8) {
            isNotNull();
            if (!Objects.equals(actual.toBic8(), expectedBic8)) {
                failWithMessage("Expected BIC-8 string to be <%s> but was <%s> for BIC <%s>",
                        expectedBic8, actual.toBic8(), actual.toString());
            }
            return myself;
        }
    }

}
