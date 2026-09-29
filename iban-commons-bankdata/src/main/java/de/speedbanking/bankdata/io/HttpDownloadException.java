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
package de.speedbanking.bankdata.io;

import java.io.IOException;

/**
 * Thrown by a {@link Downloader} when a remote source answers with an HTTP status code outside the
 * {@code 2xx} range.
 * <p>
 * Carrying the status code separately from the message lets callers (e.g. {@code CountryDataCache}'s
 * failure diagnosis) distinguish specific, actionable cases, such as {@code 407 Proxy Authentication
 * Required}, from a generic download failure.
 *
 * @since 1.8.11
 */
public class HttpDownloadException extends IOException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    /**
     * Creates a new exception for the given failed HTTP status code.
     *
     * @param message    the detail message
     * @param statusCode the HTTP status code received from the remote source
     */
    public HttpDownloadException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code that caused this exception.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

}
