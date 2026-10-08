package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.bic.Bic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link BankData}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataTest {

    @AfterEach
    void resetGlobalConfig() {
        BankDataConfig.reset();
    }

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
    void checkDigitMethod_builderWithMethod_returnsMethod() throws Exception {
        BankData data = BankData.builder("DE", "37040044", "Commerzbank", "v1")
            .bic(Bic.of("COBADEFFXXX")).postalCode("50447").city("Koeln").checkDigitMethod("13").build();

        assertThat(data.getCheckDigitMethod()).isEqualTo("13");
        assertThat(data.checkDigitMethod()).contains("13");
        assertThat(data.getCity()).isEqualTo("Koeln");
        assertThat(data.getSourceVersion()).isEqualTo("v1");
        assertThat(data.toString()).contains("checkDigitMethod=13");
    }

    @Test
    void checkDigitMethod_constructorWithoutMethod_returnsEmpty() {
        BankData data = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat((Object) data.getCheckDigitMethod()).isNull();
        assertThat(data.checkDigitMethod()).isEmpty();
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
    void equals_sameInstance_isEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(a.equals(a)).isTrue();
    }

    @Test
    void equals_null_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(a.equals(null)).isFalse();
    }

    @Test
    void equals_differentType_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(a.equals((Object) "DE10000000")).isFalse();
    }

    @Test
    void equalsAndHashCode_differentCountryCode_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("AT", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAndHashCode_differentBranchCode_notEqual() {
        BankData a = new BankData("PL", "101", "0000", null, "NBP", null, null, "v1");
        BankData b = new BankData("PL", "101", "0001", null, "NBP", null, null, "v1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAndHashCode_differentBic_notEqual() throws Exception {
        BankData a = new BankData("DE", "10000000", Bic.of("MARKDEF1100"), "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("DE", "10000000", Bic.of("MARKDEF1XXX"), "Bundesbank", "10117", "Berlin", "v1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAndHashCode_differentBankName_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("DE", "10000000", null, "Other Bank", "10117", "Berlin", "v1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAndHashCode_differentPostalCode_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("DE", "10000000", null, "Bundesbank", "20095", "Berlin", "v1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAndHashCode_differentCity_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Frankfurt", "v1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAndHashCode_sameCheckDigitMethod_areEqual() {
        BankData a = BankData.builder("DE", "10000000", "Bundesbank", "v1").checkDigitMethod("09").build();
        BankData b = BankData.builder("DE", "10000000", "Bundesbank", "v1").checkDigitMethod("09").build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @ParameterizedTest(name = "[{index}] {0} vs {1}")
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
        "09   | A4",
        "09   | NULL",
        "NULL | 09",
    })
    void equalsAndHashCode_differentCheckDigitMethod_notEqual(String method, String otherMethod) {
        BankData a = BankData.builder("DE", "10000000", "Bundesbank", "v1").checkDigitMethod(method).build();
        BankData b = BankData.builder("DE", "10000000", "Bundesbank", "v1").checkDigitMethod(otherMethod).build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsAndHashCode_differentSourceVersion_notEqual() {
        BankData a = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v1");
        BankData b = new BankData("DE", "10000000", null, "Bundesbank", "10117", "Berlin", "v2");

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
    void displayBankName_casingEnabled_appliesBankNameCasing() {
        BankData data = new BankData("DE", "10000000", null, "DEUTSCHE BANK AG", "10117", "Berlin", "v1");

        assertThat(data.getDisplayBankName()).isEqualTo("Deutsche Bank AG");
    }

    @Test
    void displayBankName_casingDisabledForCountry_returnsBankNameUnchanged() {
        BankDataConfig.configure(BankDataConfig.builder().displayCasingOverride("DE", false).build());
        BankData data = new BankData("DE", "10000000", null, "DEUTSCHE BANK AG", "10117", "Berlin", "v1");

        assertThat(data.getDisplayBankName()).isEqualTo("DEUTSCHE BANK AG");
    }

    @Test
    void displayBankName_casingDisabledForOtherCountry_stillAppliesForThisOne() {
        BankDataConfig.configure(BankDataConfig.builder().displayCasingOverride("CZ", false).build());
        BankData data = new BankData("DE", "10000000", null, "DEUTSCHE BANK AG", "10117", "Berlin", "v1");

        assertThat(data.getDisplayBankName()).isEqualTo("Deutsche Bank AG");
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
