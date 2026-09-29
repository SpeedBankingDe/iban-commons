package de.speedbanking.bankdata.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Unit tests for {@link NaiveLogger}, capturing published {@link LogRecord}s via a test-only
 * {@link Handler} attached directly to the underlying {@link Logger}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class NaiveLoggerTest {

    private final NaiveLogger logger = NaiveLogger.of(NaiveLoggerTest.class);
    private final Logger      delegate = Logger.getLogger(NaiveLoggerTest.class.getName());
    private final List<LogRecord> published = new ArrayList<>();
    private final Handler     capturingHandler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            published.add(record);
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }
    };

    private Level originalLevel;

    @BeforeEach
    void attachCapturingHandler() {
        originalLevel = delegate.getLevel();
        delegate.setLevel(Level.ALL);
        delegate.addHandler(capturingHandler);
    }

    @AfterEach
    void detachCapturingHandler() {
        delegate.removeHandler(capturingHandler);
        delegate.setLevel(originalLevel);
    }

    @Test
    void info_publishesInfoLevelRecordWithSubstitutedMessagePattern() {
        logger.info("Loaded {0} records for {1}", 42, "DE");

        assertThat(published).hasSize(1);
        LogRecord record = published.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.INFO);
        assertThat(record.getMessage()).isEqualTo("Loaded {0} records for {1}");
        assertThat(record.getParameters()).containsExactly(42, "DE");
        assertThat((Object) record.getThrown()).isNull();
    }

    @Test
    void warn_withoutCause_publishesWarningLevelRecordWithoutThrowable() {
        logger.warn("No data found for {0}", "XX");

        assertThat(published).hasSize(1);
        LogRecord record = published.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
        assertThat((Object) record.getThrown()).isNull();
    }

    @Test
    void warn_withCause_publishesWarningLevelRecordWithThrowable() {
        IllegalStateException cause = new IllegalStateException("boom");

        logger.warn("Refresh failed for {0}", cause, "DE");

        assertThat(published).hasSize(1);
        LogRecord record = published.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
        assertThat(record.getThrown()).isSameAs(cause);
    }

    @Test
    void error_publishesSevereLevelRecordWithThrowable() {
        IllegalStateException cause = new IllegalStateException("moved");

        logger.error("Source URL for {0} moved", cause, "DE");

        assertThat(published).hasSize(1);
        LogRecord record = published.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.SEVERE);
        assertThat(record.getThrown()).isSameAs(cause);
    }

    @Test
    void log_noArgs_recordHasNoParameters() {
        logger.info("Fixed message, no placeholders");

        assertThat(published).hasSize(1);
        assertThat((Object) published.get(0).getParameters()).isNull();
    }

    @Test
    void log_levelDisabled_publishesNothing() {
        delegate.setLevel(Level.OFF);

        logger.info("This should never be published");

        assertThat(published).isEmpty();
    }

    @Test
    void log_setsSourceClassAndMethodNameToCaller() {
        logger.info("Called from this test method");

        assertThat(published).hasSize(1);
        LogRecord record = published.get(0);
        assertThat(record.getSourceClassName()).isEqualTo(NaiveLoggerTest.class.getName());
        assertThat(record.getSourceMethodName()).isEqualTo("log_setsSourceClassAndMethodNameToCaller");
    }

    @Test
    void stringRepresentation_containsLoggerName() {
        String expected = "NaiveLogger[" + NaiveLoggerTest.class.getName() + "]";

        assertThat(logger.toString()).isEqualTo(expected);
    }

}
