package de.speedbanking.checkdigit.de;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CheckDigitResult}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class CheckDigitResultTest {

    @Test
    void of_true_isValidAndChecked() {
        CheckDigitResult result = CheckDigitResult.of(true);

        assertThat(result.isValid()).isTrue();
        assertThat(result.isChecked()).isTrue();
    }

    @Test
    void of_false_isInvalidAndChecked() {
        CheckDigitResult result = CheckDigitResult.of(false);

        assertThat(result.isValid()).isFalse();
        assertThat(result.isChecked()).isTrue();
    }

    @Test
    void notChecked_isValidButNotChecked() {
        assertThat(CheckDigitResult.NOT_CHECKED.isValid()).isTrue();
        assertThat(CheckDigitResult.NOT_CHECKED.isChecked()).isFalse();
    }

    @Test
    void equalsAndHashCode_sameState_areEqual() {
        assertThat(CheckDigitResult.of(true)).isEqualTo(CheckDigitResult.of(true));
        assertThat(CheckDigitResult.of(true)).hasSameHashCodeAs(CheckDigitResult.of(true));
        assertThat(CheckDigitResult.of(true)).isNotEqualTo(CheckDigitResult.of(false));
        assertThat(CheckDigitResult.of(true)).isNotEqualTo(CheckDigitResult.NOT_CHECKED);
        assertThat(CheckDigitResult.of(false)).isNotEqualTo(CheckDigitResult.NOT_CHECKED);
        assertThat(CheckDigitResult.of(false)).isEqualTo(CheckDigitResult.of(false));
        assertThat(CheckDigitResult.of(true)).isNotEqualTo("not a CheckDigitResult");
        assertThat(CheckDigitResult.of(true)).isEqualTo(CheckDigitResult.of(true));
    }

    @Test
    void stringRepresentation_containsFieldValues() {
        assertThat(CheckDigitResult.of(true).toString())
            .contains("valid=true")
            .contains("checked=true");
    }

}
