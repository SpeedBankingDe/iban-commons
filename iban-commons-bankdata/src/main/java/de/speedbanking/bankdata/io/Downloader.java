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
import java.net.URI;
import java.time.Duration;

/**
 * Downloads raw bytes from a remote source.
 * <p>
 * Extracted as an interface purely for testability: production code uses {@code HttpDownloader},
 * while tests can substitute a simple fake instead of hitting the network.
 * <p>
 * Implementations return the raw, undecoded bytes of the response body. Charset decoding is
 * deliberately <strong>not</strong> performed here, it is the responsibility of the
 * {@code CountryBankDataLoader} via {@code getRemoteSourceCharset()}, since upstream sources
 * (e.g. Bundesbank exports) are not reliably UTF-8 and their {@code Content-Type} header, if
 * present at all, cannot be trusted.
 *
 * @since 1.8.11
 */
public interface Downloader {

    /**
     * Creates the default, production {@link Downloader} implementation based on
     * {@link java.net.HttpURLConnection}.
     *
     * @return a new default {@link Downloader} instance
     */
    static Downloader createDefault() {
        return new HttpDownloader();
    }

    /**
     * Downloads the raw bytes at the given URI.
     *
     * @param uri            the source URI
     * @param connectTimeout the connect timeout
     * @param readTimeout    the read timeout
     * @return the raw, undecoded response body bytes
     * @throws IOException if the download fails (network error, non-2xx response, timeout, etc.)
     */
    byte[] download(URI uri, Duration connectTimeout, Duration readTimeout) throws IOException;

}
