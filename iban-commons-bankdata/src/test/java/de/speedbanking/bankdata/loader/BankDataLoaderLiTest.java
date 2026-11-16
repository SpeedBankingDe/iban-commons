package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.speedbanking.bankdata.BankData;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

/**
 * Unit tests for {@link BankDataLoaderLi}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderLiTest {

    private final BankDataLoaderLi loader = new BankDataLoaderLi();

    @Test
    void countryCode_returnsLI() {
        assertThat(loader.getCountryCode()).isEqualTo("LI");
    }

    @Test
    void remoteSourceUri_sameAsSwissLoader() {
        assertThat(loader.getRemoteSourceUri()).isEqualTo(BankDataLoaderCh.DEFAULT_SOURCE_URI);
    }

    @Test
    void remoteSourceCharset_returnsUtf8() {
        assertThat(loader.getRemoteSourceCharset()).isEqualTo(UTF_8);
    }

    @Test
    void allowsBundledSnapshot_returnsFalse() {
        assertThat(loader.allowsBundledSnapshot()).isFalse();
    }

    @Test
    void parse_sampleFile_keepsOnlyLiechtensteinRows() throws Exception {
        List<BankData> records;
        try (InputStream in = getClass().getResourceAsStream("/bankdata/raw/CH-sample.txt")) {
            records = loader.parse(in, "test-version");
        }

        assertThat(records).singleElement().satisfies(r -> {
            assertThat(r.getCountryCode()).isEqualTo("LI");
            assertThat(r.getBankCode()).isEqualTo("08810");
            assertThat(r.getBankName()).isEqualTo("Musterbank Vaduz AG");
            assertThat((Object) r.getBic()).hasToString("BLFLLI2XXXX");
            assertThat(r.getPostalCode()).isEqualTo("9490");
            assertThat(r.getCity()).isEqualTo("Vaduz");
        });
    }

}
