package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.spi.BankDataParseException;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Unit tests for {@link AbstractCountryBankDataLoader}, using minimal test-only subclasses since
 * every real loader's country code is folded into its own assertions already.
 */
@SuppressWarnings("checkstyle:MethodName")
final class AbstractCountryBankDataLoaderTest {

    private static final URI DEFAULT_URI = URI.create("https://example.invalid/data");

    /** Valid: simple name ends in "PL", a real ISO 3166-1 alpha-2 country code. */
    private static final class LoaderPl extends AbstractCountryBankDataLoader<BankDataLoaderPl.Column> {
        LoaderPl() {
            super(BankDataLoaderPl.Column.class, DEFAULT_URI);
        }

        @Override
        public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
            throw new BankDataParseException("not needed for this test");
        }
    }

    /** Invalid: simple name ends in "Zz", not a recognized ISO 3166-1 alpha-2 country code. */
    private static final class LoaderZz extends AbstractCountryBankDataLoader<BankDataLoaderPl.Column> {
        LoaderZz() {
            super(BankDataLoaderPl.Column.class, DEFAULT_URI);
        }

        @Override
        public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
            throw new BankDataParseException("not needed for this test");
        }
    }

    @Test
    void constructor_derivesCountryCodeFromClassNameSuffix() {
        assertThat(new LoaderPl().getCountryCode()).isEqualTo("PL");
    }

    @Test
    void constructor_classNameSuffixNotACountry_throws() {
        assertThatIllegalStateException().isThrownBy(LoaderZz::new)
            .withMessageContaining("Cannot derive country");
    }

    @Test
    @SuppressWarnings("PMD.UseDiamondOperator") // <> with an anonymous class needs -source 9+; this module targets 8
    void constructor_nullColumnClass_throws() {
        assertThatNullPointerException().isThrownBy(() -> new AbstractCountryBankDataLoader<BankDataLoaderPl.Column>(null, DEFAULT_URI) {
            @Override
            public List<BankData> parse(InputStream rawSource, String sourceVersion) {
                throw new UnsupportedOperationException();
            }
        }).withMessageContaining("columnClass");
    }

    @Test
    void constructor_nullDefaultSourceUri_throws() {
        assertThatNullPointerException().isThrownBy(() -> new SecondLoaderPl(null))
            .withMessageContaining("defaultSourceUri");
    }

    private static final class SecondLoaderPl extends AbstractCountryBankDataLoader<BankDataLoaderPl.Column> {
        SecondLoaderPl(URI uri) {
            super(BankDataLoaderPl.Column.class, uri);
        }

        @Override
        public List<BankData> parse(InputStream rawSource, String sourceVersion) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void remoteSourceUri_noOverrideConfigured_returnsCompiledInDefault() {
        assertThat(new LoaderPl().getRemoteSourceUri()).isEqualTo(DEFAULT_URI);
    }

    @Test
    void remoteSourceCharset_defaultsToUtf8() {
        assertThat(new LoaderPl().getRemoteSourceCharset()).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    void stringRepresentation_containsCountryCodeUriCharsetAndColumns() {
        String result = new LoaderPl().toString();

        assertThat(result)
            .contains("LoaderPl[")
            .contains("countryCode=PL")
            .contains("remoteSourceUri=" + DEFAULT_URI)
            .contains("remoteSourceCharset=")
            .contains("columns=");
    }

}
