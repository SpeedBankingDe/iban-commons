package de.speedbanking.bankdata.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Unit tests for {@link NaiveLogFormatter}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class NaiveLogFormatterTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private final NaiveLogFormatter formatter = new NaiveLogFormatter();

    @Test
    void format_simpleMessage_producesSimpleLoggerLayout() {
        long millis = System.currentTimeMillis();
        LogRecord record = new LogRecord(Level.INFO, "The answer is {0}");
        record.setLoggerName("deep.thought.question.life");
        record.setParameters(new Object[] {42});
        record.setMillis(millis);

        String formatted = formatter.format(record);

        String expectedTimestamp = DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
        String currentThreadName = Thread.currentThread().getName();

        String expected = String.format("%s [%s] INFO     life - The answer is 42%s",
            expectedTimestamp, currentThreadName, System.lineSeparator());

        assertThat(formatted).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "de.speedbanking.bankdata.refresh.CountryDataCache, CountryDataCache",
        "CountryDataCache, CountryDataCache",
        "'', ''"
    })
    void format_shortLoggerName_extractsSimpleClassName(String loggerName, String expectedShortName) {
        LogRecord record = new LogRecord(Level.INFO, "test");
        record.setLoggerName(loggerName);
        record.setMillis(0L);

        String formatted = formatter.format(record);

        assertThat(formatted).contains(" " + expectedShortName + " - test");
    }

    @Test
    void format_differentThread_usesFallbackThreadId() {
        LogRecord record = new LogRecord(Level.INFO, "async task");
        record.setLoggerName("AsyncLogger");
        record.setMillis(0L);
        record.setThreadID(999);

        String formatted = formatter.format(record);

        assertThat(formatted).contains("[Thread-999]");
    }

    @Test
    void format_withThrown_appendsStackTrace() {
        LogRecord record = new LogRecord(Level.WARNING, "failed");
        record.setLoggerName("de.speedbanking.BoomTest");
        record.setMillis(0L);
        record.setThrown(new IllegalStateException("boom"));

        String formatted = formatter.format(record);

        assertThat(formatted)
            .contains("BoomTest")
            .contains("failed")
            .contains("IllegalStateException: boom");
    }

    @Test
    void installOnRootLogger_noExistingHandlers_addsConsoleHandlerWithThisFormatter() {
        Logger rootLogger = Logger.getLogger("");
        Handler[] original = rootLogger.getHandlers();
        try {
            for (Handler handler : original) {
                rootLogger.removeHandler(handler);
            }

            NaiveLogFormatter.installOnRootLogger();

            assertThat(rootLogger.getHandlers()).hasSize(1);
            assertThat(rootLogger.getHandlers()[0]).isInstanceOf(ConsoleHandler.class);
            assertThat(rootLogger.getHandlers()[0].getFormatter()).isInstanceOf(NaiveLogFormatter.class);
        } finally {
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }
            for (Handler handler : original) {
                rootLogger.addHandler(handler);
            }
        }
    }

    @Test
    void installOnRootLogger_existingHandlers_replacesTheirFormatter() {
        Logger rootLogger = Logger.getLogger("");
        Handler[] original = rootLogger.getHandlers();
        Handler probe = new ConsoleHandler();
        try {
            for (Handler handler : original) {
                rootLogger.removeHandler(handler);
            }
            rootLogger.addHandler(probe);

            NaiveLogFormatter.installOnRootLogger();

            assertThat(probe.getFormatter()).isInstanceOf(NaiveLogFormatter.class);
        } finally {
            rootLogger.removeHandler(probe);
            for (Handler handler : original) {
                rootLogger.addHandler(handler);
            }
        }
    }

}
