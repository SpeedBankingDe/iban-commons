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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Minimal internal logging facade for this module.
 * <p>
 * Wraps {@link java.util.logging.Logger} (available since Java 1.4, no new runtime dependency)
 * rather than {@code java.lang.System.Logger} (Java 9+), since this module targets
 * {@code releaseJavaVersion=8} for binary compatibility. Kept as a thin facade so the logging
 * backend can be swapped centrally later without touching call sites.
 * <p>
 * All methods take a {@code {0}}, {@code {1}}, ... {@link java.text.MessageFormat} style pattern
 * plus the substitution arguments, rather than a pre-concatenated message: substitution is
 * deferred to the handler and skipped entirely when the level is disabled, and the log statement
 * itself stays a plain, readable template instead of a string-concatenation expression.
 *
 * @since 1.8.11
 */
public final class NaiveLogger {

    private final Logger delegate;

    private NaiveLogger(Class<?> owner) {
        this.delegate = Logger.getLogger(owner.getName());
    }

    /**
     * Returns a facade bound to the given owning class.
     *
     * @param owner the class to log on behalf of
     * @return a new {@link NaiveLogger} instance
     */
    public static NaiveLogger of(Class<?> owner) {
        return new NaiveLogger(owner);
    }

    /**
     * Logs an informational message.
     *
     * @param messagePattern a {@link java.text.MessageFormat} style pattern, e.g. {@code "Loaded {0} records for {1}"}
     * @param args           the values substituted into {@code messagePattern}, in order
     */
    public void info(String messagePattern, Object... args) {
        log(Level.INFO, messagePattern, null, args);
    }

    /**
     * Logs a warning message.
     *
     * @param messagePattern a {@link java.text.MessageFormat} style pattern, e.g. {@code "No data found for {0}"}
     * @param args           the values substituted into {@code messagePattern}, in order
     */
    public void warn(String messagePattern, Object... args) {
        log(Level.WARNING, messagePattern, null, args);
    }

    /**
     * Logs a warning message together with the causing throwable.
     *
     * @param messagePattern a {@link java.text.MessageFormat} style pattern, e.g. {@code "Refresh failed for {0}"}
     * @param cause          the throwable that caused the warning
     * @param args           the values substituted into {@code messagePattern}, in order
     */
    public void warn(String messagePattern, Throwable cause, Object... args) {
        log(Level.WARNING, messagePattern, cause, args);
    }

    /**
     * Logs an error message together with the causing throwable, for a condition that will not
     * resolve itself and needs a human to intervene (as opposed to {@link #warn}, used for
     * transient, self-recovering conditions).
     *
     * @param messagePattern a {@link java.text.MessageFormat} style pattern, e.g. {@code "Source URL for {0} moved"}
     * @param cause          the throwable that caused the error
     * @param args           the values substituted into {@code messagePattern}, in order
     */
    public void error(String messagePattern, Throwable cause, Object... args) {
        log(Level.SEVERE, messagePattern, cause, args);
    }

    private void log(Level level, String messagePattern, Throwable cause, Object[] args) {
        if (!delegate.isLoggable(level)) {
            return;
        }
        LogRecord record = new LogRecord(level, messagePattern);
        record.setLoggerName(delegate.getName());
        if (args != null && args.length > 0) {
            record.setParameters(args);
        }
        record.setThrown(cause);
        inferCaller(record);
        delegate.log(record);
    }

    private void inferCaller(LogRecord record) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String thisClassName = NaiveLogger.class.getName();
        int i = 0;
        while (i < stack.length) {
            if (thisClassName.equals(stack[i].getClassName())) {
                break;
            }
            i++;
        }
        while (i < stack.length) {
            if (!thisClassName.equals(stack[i].getClassName())) {
                StackTraceElement frame = stack[i];
                record.setSourceClassName(frame.getClassName());
                record.setSourceMethodName(frame.getMethodName());
                return;
            }
            i++;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + delegate.getName() + "]";
    }

}
