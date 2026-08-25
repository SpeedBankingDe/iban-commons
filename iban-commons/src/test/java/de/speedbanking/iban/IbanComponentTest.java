package de.speedbanking.iban;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.iban.IbanComponent.IbanComponentType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit test class for {@link IbanComponent}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IbanComponentTest {

    @DisplayName("Constructor sets all fields correctly")
    @Test
    void constructor_setsAllFields() {
        IbanComponent component = new IbanComponent(IbanComponentType.BANK_CODE, "[0-9]{5}", 4, 5);

        assertThat(component.getType()).isEqualTo(IbanComponentType.BANK_CODE);
        assertThat(component.getPattern()).isEqualTo("[0-9]{5}");
        assertThat(component.getBeginIndex()).isEqualTo(4);
        assertThat(component.getLength()).isEqualTo(5);
        assertThat(component.getEndIndex()).isEqualTo(9);
    }

    @DisplayName("Constructor accepts beginIndex == 0 (lower bound, valid)")
    @Test
    void constructor_acceptsZeroBeginIndex() {
        IbanComponent component = new IbanComponent(IbanComponentType.BBAN, "[A-Z0-9]+", 0, 3);

        assertThat(component.getBeginIndex()).isZero();
        assertThat(component.getEndIndex()).isEqualTo(3);
    }

    @DisplayName("Constructor accepts length == 1 (lower bound, valid)")
    @Test
    void constructor_acceptsLengthOfOne() {
        IbanComponent component = new IbanComponent(IbanComponentType.NATIONAL_CHECK_DIGIT, "[0-9]", 10, 1);

        assertThat(component.getLength()).isEqualTo(1);
        assertThat(component.getEndIndex()).isEqualTo(11);
    }

    @DisplayName("Constructor throws NullPointerException when type == null")
    @Test
    void constructor_rejectsNullType() {
        assertThatNullPointerException()
            .isThrownBy(() -> new IbanComponent(null, "[0-9]+", 0, 1))
            .withMessage("type required");
    }

    @DisplayName("Constructor throws NullPointerException when patternStr == null")
    @Test
    void constructor_rejectsNullPattern() {
        assertThatNullPointerException()
            .isThrownBy(() -> new IbanComponent(IbanComponentType.BANK_CODE, null, 0, 1))
            .withMessage("patternStr required");
    }

    @DisplayName("Constructor throws IllegalArgumentException for negative beginIndex")
    @Test
    void constructor_rejectsNegativeBeginIndex() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new IbanComponent(IbanComponentType.BANK_CODE, "[0-9]+", -1, 4))
            .withMessage("beginIndex must be >= 0, was -1");
    }

    @DisplayName("Constructor throws IllegalArgumentException for length <= 0")
    @ParameterizedTest(name = "length={0} leads to IllegalArgumentException")
    @ValueSource(ints = {0, -1, -5})
    void constructor_rejectsNonPositiveLength(int invalidLength) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new IbanComponent(IbanComponentType.ACCOUNT_NUMBER, "[0-9]+", 0, invalidLength))
            .withMessage("length must be > 0, was %d", invalidLength);
    }

    @DisplayName("accountType(...) creates a component of type ACCOUNT_TYPE")
    @Test
    void accountTypeFactory_createsAccountTypeComponent() {
        IbanComponent component = IbanComponent.accountType("[0-9]", 12, 1);

        assertThat(component.getType()).isEqualTo(IbanComponentType.ACCOUNT_TYPE);
        assertThat(component.getPattern()).isEqualTo("[0-9]");
        assertThat(component.getBeginIndex()).isEqualTo(12);
        assertThat(component.getLength()).isEqualTo(1);
    }

    @DisplayName("nationalCode(...) creates a component of type NATIONAL_CODE")
    @Test
    void nationalCodeFactory_createsNationalCodeComponent() {
        IbanComponent component = IbanComponent.nationalCode("[A-Z]{2}", 8, 2);

        assertThat(component.getType()).isEqualTo(IbanComponentType.NATIONAL_CODE);
        assertThat(component.getPattern()).isEqualTo("[A-Z]{2}");
        assertThat(component.getBeginIndex()).isEqualTo(8);
        assertThat(component.getLength()).isEqualTo(2);
    }

    @DisplayName("getEndIndex() equals beginIndex + length")
    @Test
    void getEndIndex_isBeginIndexPlusLength() {
        IbanComponent component = new IbanComponent(IbanComponentType.BRANCH_CODE, "[0-9]{3}", 9, 3);

        assertThat(component.getEndIndex()).isEqualTo(12);
    }

    @Nested
    @DisplayName("extractFrom(String)")
    final class ExtractFromString {

        @DisplayName("extracts the correct substring from an IBAN string")
        @Test
        void extractsSubstring() {
            IbanComponent bankCode = new IbanComponent(IbanComponentType.BANK_CODE, "[0-9]{8}", 4, 8);
            String iban = "DE89370400440532013000";

            assertThat(bankCode.extractFrom(iban)).isEqualTo("37040044");
        }

        @DisplayName("extracts from the start of the string (beginIndex 0)")
        @Test
        void extractsFromStart() {
            IbanComponent countryCode = new IbanComponent(IbanComponentType.BBAN, "[A-Z]{2}", 0, 2);

            assertThat(countryCode.extractFrom("DE89370400440532013000")).isEqualTo("DE");
        }

        @DisplayName("throws StringIndexOutOfBoundsException when the end index exceeds the string length")
        @Test
        void throwsWhenEndIndexExceedsStringLength() {
            IbanComponent component = new IbanComponent(IbanComponentType.ACCOUNT_NUMBER, "[0-9]+", 20, 10);

            assertThatThrownBy(() -> component.extractFrom("DE89370400440532013000"))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("extractFrom(char[])")
    final class ExtractFromCharArray {

        @DisplayName("extracts the correct subarray from a char[]")
        @Test
        void extractsSubarray() {
            IbanComponent bankCode = new IbanComponent(IbanComponentType.BANK_CODE, "[0-9]{8}", 4, 8);
            char[] iban = "DE89370400440532013000".toCharArray();

            assertThat(bankCode.extractFrom(iban)).isEqualTo("37040044".toCharArray());
        }

        @DisplayName("returns a new, independent array (not linked to the source)")
        @Test
        void returnsIndependentCopy() {
            IbanComponent component = new IbanComponent(IbanComponentType.NATIONAL_CODE, "[A-Z]{2}", 0, 2);
            char[] source = "AB123".toCharArray();

            char[] extracted = component.extractFrom(source);
            extracted[0] = 'Z';

            assertThat(source[0]).isEqualTo('A');
        }

        @DisplayName("throws ArrayIndexOutOfBoundsException when the end index exceeds the array length")
        @Test
        void throwsWhenEndIndexExceedsArrayLength() {
            IbanComponent component = new IbanComponent(IbanComponentType.ACCOUNT_NUMBER, "[0-9]+", 20, 10);
            char[] source = "DE89370400440532013000".toCharArray();

            assertThatThrownBy(() -> component.extractFrom(source))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }
    }

    @DisplayName("toString() contains the class name and all fields")
    @Test
    void toString_containsAllFields() {
        IbanComponent component = new IbanComponent(IbanComponentType.IDENTIFICATION_NUMBER, "[0-9]{4}", 6, 4);

        assertThat(component.toString())
            .isEqualTo("IbanComponent[type=IDENTIFICATION_NUMBER, pattern=[0-9]{4}, beginIndex=6, length=4]");
    }

}
