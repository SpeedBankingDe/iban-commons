package de.speedbanking.bankdata.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BankDataParseException}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataParseExceptionTest {

    @Test
    void constructor_messageOnly_setsMessageAndNoCause() {
        BankDataParseException exception = new BankDataParseException("parse failed");

        assertThat(exception.getMessage()).isEqualTo("parse failed");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_messageAndCause_setsBoth() {
        Exception cause = new IllegalStateException("root cause");

        BankDataParseException exception = new BankDataParseException("parse failed", cause);

        assertThat(exception.getMessage()).isEqualTo("parse failed");
        assertThat(exception.getCause()).isSameAs(cause);
    }

}
