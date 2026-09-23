package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.bic.Bic;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BankData}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataTest {

    @Test
    void constructor_missingCountryCode_throws() {
        assertThatNullPointerException().isThrownBy(() -> new BankData(null, "10000000", null, "Bank", null, null, "v1"));
    }

    @Test
    void constructor_missingBankCode_throws() {
        assertThatNullPointerException().isThrownBy(() -> new BankData("DE", null, null, "Bank", null, null, "v1"));
    }

    @Test
    void constructor_missingBankName_throws() {
        assertThatNullPointerException().isThrownBy(() -> new BankData("DE", "10000000", null, null, null, null, "v1"));
    }

    @Test
    void constructor_missingSourceVersion_throws() {
        assertThatNullPointerException().isThrownBy(() -> new BankData("DE", "10000000", null, "Bank", null, null, null));
    }

    @Test
    void getters_returnConstructorValues() throws Exception {
        Bic bic = Bic.of("MARKDEF1100");
        BankData data = new BankData("DE", "10000000", bic, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(data.getCountryCode()).isEqualTo("DE");
        assertThat(data.getBankCode()).isEqualTo("10000000");
        assertThat((Object) data.getBic()).isEqualTo(bic);
        assertThat(data.getBankName()).isEqualTo("Bundesbank");
        assertThat(data.getPostalCode()).isEqualTo("10117");
        assertThat(data.getCity()).isEqualTo("Berlin");
        assertThat(data.getSourceVersion()).isEqualTo("v1");
    }

    @Test
    void optionalAccessors_present() throws Exception {
        Bic bic = Bic.of("MARKDEF1100");
        BankData data = new BankData("DE", "10000000", bic, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(data.bic()).contains(bic);
        assertThat(data.postalCode()).contains("10117");
        assertThat(data.city()).contains("Berlin");
    }

    @Test
    void optionalAccessors_absent() {
        BankData data = new BankData("DE", "10000000", null, "Bundesbank", null, null, "v1");

        assertThat(data.bic()).isEmpty();
        assertThat(data.postalCode()).isEmpty();
        assertThat(data.city()).isEmpty();
    }

    @Test
    void equalsAndHashCode_sameFields_areEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void equalsAndHashCode_differentBankCode_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("DE", "20000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void key_noBranchCode_returnsBankCode() {
        BankData data = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(data.getKey()).isEqualTo("10000000");
        assertThat((Object) data.getBranchCode()).isNull();
        assertThat(data.branchCode()).isEmpty();
    }

    @Test
    void key_withBranchCode_returnsBankCodePlusBranchCode() {
        BankData data = new BankData("PL", "101", "0000", null, "NBP", null, null, "v1");

        assertThat(data.getKey()).isEqualTo("1010000");
        assertThat(data.getBranchCode()).isEqualTo("0000");
        assertThat(data.branchCode()).contains("0000");
    }

    @Test
    void stringRepresentation_containsFieldValues() {
        BankData data = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(data.toString())
            .contains("DE")
            .contains("10000000")
            .contains("Bundesbank")
            .contains("10117")
            .contains("Berlin")
            .contains("v1");
    }

}
