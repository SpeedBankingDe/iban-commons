/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.bankdata.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Compact, single-line {@link Formatter} for {@link java.util.logging} output mimicking
 * SLF4J's {@code SimpleLogger} formatting.
 * <p>
 * Output layout:
 * <pre>
 * 2026-09-15 20:03:35.123 [main] INFO CountryDataCache - Loaded ...
 * </pre>
 * Any {@linkplain LogRecord#getThrown() thrown} stack trace is appended after the line.
 * <p>
 * Not installed automatically, a library must never impose logging configuration on the host
 * application. Wire it up explicitly where desired, either declaratively via a
 * {@code logging.properties} file or programmatically via {@link #installOnRootLogger()}.
 *
 * @since 1.8.11
 */
public final class NaiveLogFormatter extends Formatter {

    private static final Map<Level, String> LEVEL_MAP = new LinkedHashMap<>();

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    /**
     * Installs a new instance of this formatter on every {@link Handler} currently attached to
     * the root logger (adding a {@link ConsoleHandler} first if none exist yet).
     * <p>
     * Unlike a {@code logging.properties} file passed via the {@code java.util.logging.config.file}
     * system property, this works no matter when it is called, in particular it is the only
     * reliable option for a {@code main()} method run in-process by a build tool (e.g. the
     * {@code exec-maven-plugin} {@code java} goal), where some earlier component in the same JVM
     * has typically already triggered {@link java.util.logging.LogManager}'s lazy initialization
     * with the default configuration before the system property could take effect.
     */
    public static void installOnRootLogger() {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length == 0) {
            rootLogger.addHandler(new ConsoleHandler());
            handlers = rootLogger.getHandlers();
        }
        NaiveLogFormatter formatter = new NaiveLogFormatter();
        for (Handler handler : handlers) {
            handler.setFormatter(formatter);
        }
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder line = new StringBuilder(128)
            .append(DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(record.getMillis())))
            .append(" [")
            .append(getThreadName(record))
            .append("] ")
            .append(LEVEL_MAP.computeIfAbsent(record.getLevel(), l -> String.format("%-8s", l.getName())))
            .append(' ')
            .append(getShortLoggerName(record.getLoggerName()))
            .append(" - ")
            .append(formatMessage(record))
            .append(System.lineSeparator());

        if (record.getThrown() != null) {
            StringWriter stackTrace = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(stackTrace));
            line.append(stackTrace);
        }

        return line.toString();
    }

    private String getThreadName(LogRecord record) {
        long currentThreadId = Thread.currentThread().getId();
        if (record.getThreadID() == (int) currentThreadId) {
            return Thread.currentThread().getName();
        }
        return "Thread-" + record.getThreadID();
    }

    private String getShortLoggerName(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return "";
        }
        int lastDot = loggerName.lastIndexOf('.');
        return lastDot >= 0 ? loggerName.substring(lastDot + 1) : loggerName;
    }

}
