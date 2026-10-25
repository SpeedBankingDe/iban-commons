package de.speedbanking.bankdata.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bic.Bic;

import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:MethodName")
final class BankDataDeduplicatorTest {

    @Test
    void deduplicate_record_returnsEqualRecord() {
        BankData data = record("10010010", "PBNKDEFFXXX", "10559", "24");

        assertThat(new BankDataDeduplicator().deduplicate(data)).isEqualTo(data);
    }

    @Test
    void deduplicate_equalValues_sharesInstances() {
        BankDataDeduplicator deduplicator = new BankDataDeduplicator();
        BankData first = deduplicator.deduplicate(record("10010010", "PBNKDEFFXXX", "10559", "24"));
        BankData second = deduplicator.deduplicate(record("10010020", "PBNKDEFFXXX", "10559", "24"));

        assertThat(second.getBankCode()).isNotSameAs(first.getBankCode());
        assertThat(second.getBankName()).isSameAs(first.getBankName());
        assertThat((Object) second.getBic()).isSameAs(first.getBic()); // Bic is both Comparable and CharSequence
        assertThat(second.getPostalCode()).isSameAs(first.getPostalCode());
        assertThat(second.getCity()).isSameAs(first.getCity());
        assertThat(second.getCheckDigitMethod()).isSameAs(first.getCheckDigitMethod());
    }

    @Test
    void deduplicate_nullFields_keepsNull() {
        BankData data = BankData.builder("DE", "10010010", "Postbank", "v1").build();

        BankData result = new BankDataDeduplicator().deduplicate(data);

        assertThat(result).isEqualTo(data);
        assertThat((Object) result.getBic()).isNull();
        assertThat(result.getCity()).isNull();
    }

    /**
     * Creates a record whose string values are fresh instances, as a parser would produce them.
     */
    private static BankData record(String bankCode, String bic, String postalCode, String checkDigitMethod) {
        return BankData.builder(copy("DE"), copy(bankCode), copy("Postbank Ndl der Deutsche Bank"), copy("v1"))
            .bic(Bic.of(copy(bic)))
            .postalCode(copy(postalCode))
            .city(copy("Berlin"))
            .checkDigitMethod(copy(checkDigitMethod))
            .build();
    }

    /**
     * Returns a new string instance equal to {@code value}.
     */
    private static String copy(String value) {
        return String.valueOf(value.toCharArray());
    }
}
