package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

/**
 * Unit tests for {@link AtBankDataLoader}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class AtBankDataLoaderTest {

    private final AtBankDataLoader loader = new AtBankDataLoader();

    @Test
    void countryCode_returnsAT() {
        assertThat(loader.getCountryCode()).isEqualTo("AT");
    }

    @Test
    void parse_sampleFile_returnsAllRecords_andHandlesMissingBic() throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/raw/AT-sample.txt")) {
            records = loader.parse(in, "test-version");
        }

        assertThat(records).hasSize(3);
        assertThat(records).extracting(BankData::getBankCode).containsExactly("12000", "18170", "34923");

        BankData withoutBic = records.stream().filter(r -> r.getBankCode().equals("18170")).findFirst().orElseThrow(AssertionError::new);
        assertThat((Object) withoutBic.getBic()).isNull();

        BankData headOffice = records.stream().filter(r -> r.getBankCode().equals("12000")).findFirst().orElseThrow(AssertionError::new);
        assertThat(headOffice.getBankName()).isEqualTo("UniCredit Bank Austria AG");
        assertThat((Object) headOffice.getBic()).isNotNull();
        assertThat(headOffice.getBic().toString()).isEqualTo("BKAUATWWXXX");
        assertThat(headOffice.getPostalCode()).isEqualTo("1020");
        assertThat(headOffice.getCity()).isEqualTo("Wien");
    }

}
