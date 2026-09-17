package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Unit tests for {@link HttpDownloadException}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class HttpDownloadExceptionTest {

    @Test
    void constructor_exposesMessageAndStatusCode() {
        HttpDownloadException ex = new HttpDownloadException("Unexpected HTTP status 407 downloading https://example.invalid", 407);

        assertThat(ex).isInstanceOf(IOException.class);
        assertThat(ex.getStatusCode()).isEqualTo(407);
        assertThat(ex.getMessage()).isEqualTo("Unexpected HTTP status 407 downloading https://example.invalid");
    }

}
