package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

import java.net.URI;

/**
 * Unit tests for {@link BankDataLoaderDefaults}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLoaderDefaultsTest {

    @Test
    void sourceUri_knownCountry_returnsConfiguredUri() {
        assertThat(BankDataLoaderDefaults.sourceUri("PL")).isEqualTo(URI.create(BankDataLoaderPl.DEFAULT_SOURCE_URI.toString()));
    }

    @Test
    void sourceUri_unknownCountry_throws() {
        assertThatIllegalStateException().isThrownBy(() -> BankDataLoaderDefaults.sourceUri("XX"))
            .withMessageContaining("loader.XX.url");
    }

}
