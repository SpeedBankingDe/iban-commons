package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Checks that every loader reports a failing raw source as a {@link BankDataParseException}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class CountryBankDataLoaderReadFailureTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(classes = {
        BankDataLoaderAt.class,
        BankDataLoaderBe.class,
        BankDataLoaderCh.class,
        BankDataLoaderCz.class,
        BankDataLoaderDe.class,
        BankDataLoaderFr.class,
        BankDataLoaderNl.class,
        BankDataLoaderPl.class,
    })
    void parse_unreadableSource_throwsBankDataParseException(Class<? extends CountryBankDataLoader> loaderClass) throws Exception {
        CountryBankDataLoader loader = loaderClass.getDeclaredConstructor().newInstance();

        assertThatThrownBy(() -> loader.parse(new FailingInputStream(), "test-version"))
            .isInstanceOf(BankDataParseException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    /** An input stream whose every read fails. */
    private static final class FailingInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            throw new IOException("simulated read failure");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException("simulated read failure");
        }

    }

}
