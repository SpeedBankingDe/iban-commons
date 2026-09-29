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
package de.speedbanking.bankdata.spi;

/**
 * Checked exception thrown by a {@link CountryBankDataLoader} when a raw bank directory cannot
 * be parsed.
 * <p>
 * Deliberately a checked exception: a parse failure during a scheduled background refresh is an
 * expected, recoverable condition (e.g. the upstream source changed its format or served a
 * truncated download), not a programming error. Callers are expected to log the failure and keep
 * using the last known good in-memory/cached data set.
 *
 * @since 1.8.11
 */
public class BankDataParseException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message
     */
    public BankDataParseException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public BankDataParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
