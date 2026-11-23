package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@SuppressWarnings("checkstyle:MethodName")
final class BoundedStreamsTest {

    private static final int LIMIT = 10_000;

    @ParameterizedTest(name = "[{index}] {0} bytes")
    @ValueSource(ints = {0, 1, 8192, LIMIT})
    void readAll_withinLimit_returnsAllBytes(int size) throws IOException {
        byte[] data = new byte[size];

        assertThat(BoundedStreams.readAll(new ByteArrayInputStream(data), LIMIT, "test data")).hasSize(size);
    }

    @ParameterizedTest(name = "[{index}] {0} bytes")
    @ValueSource(ints = {LIMIT + 1, 3 * LIMIT})
    void readAll_beyondLimit_throwsIOException(int size) {
        byte[] data = new byte[size];

        assertThatThrownBy(() -> BoundedStreams.readAll(new ByteArrayInputStream(data), LIMIT, "test data"))
            .isInstanceOf(IOException.class)
            .hasMessage("test data exceeds the limit of 10000 bytes");
    }

}
