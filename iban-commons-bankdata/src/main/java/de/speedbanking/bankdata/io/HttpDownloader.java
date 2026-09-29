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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * Default {@link Downloader} implementation based on {@link HttpURLConnection}.
 * <p>
 * Deliberately avoids {@code java.net.http.HttpClient} (Java 11+) to keep this module compilable
 * with {@code releaseJavaVersion=8}, consistent with the rest of the {@code iban-commons} project.
 * <p>
 * {@link HttpURLConnection#setInstanceFollowRedirects(boolean)} does not follow redirects across
 * protocol changes (e.g. {@code http -> https}), so this implementation manually follows a single
 * additional redirect hop when it encounters a {@code 301}, {@code 302}, {@code 303}, {@code 307},
 * or {@code 308} response.
 * <p>
 * <strong>Why a browser {@code User-Agent}.</strong> Several of the national sources this module
 * downloads from (see the per-country {@code BankDataLoader*} classes) block or degrade requests
 * whose {@code User-Agent} identifies them as a bot or a bare Java HTTP client, even though the
 * exact same file is served without restriction to an ordinary browser visitor and is published
 * specifically for automated consumption (it is the machine-readable master data list institutions
 * are meant to integrate). {@link #applyBrowserHeaders(HttpURLConnection)} therefore sends the
 * same request headers Firefox would, purely to get past that filtering, not to misrepresent
 * <em>what</em> is requesting the file: this class only ever fetches the exact, publicly linked
 * CSV/XLSX/HTML resource named by a loader's {@code getRemoteSourceUri()}, one request at a time,
 * never crawls or enumerates URLs, and is rate-limited to at most one request per country every
 * {@code BankDataConfig.getStaleThreshold()} (default 90 days) by {@code CountryDataCache}'s
 * staleness check, plus a fixed 6-hour backoff after a failure - an
 * order of magnitude below anything that could look like scraping abuse. If a specific source's
 * terms of use turn out to prohibit even this, the fix is to stop bundling/downloading that
 * source's data (see the module README's "Data accuracy disclaimer"), not to keep spoofing a
 * browser against its wishes.
 *
 * @since 1.8.11
 */
final class HttpDownloader implements Downloader {

    private static final int    MAX_REDIRECT_HOPS = 1;

    private static final String USER_AGENT        =
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:155.0) Gecko/20100101 Firefox/155.0";

    private static final String ACCEPT            =
        "text/html,application/xhtml+xml,application/xml;q=0.9,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,*/*;q=0.8";

    private static final String ACCEPT_LANGUAGE   = "en-US,en;q=0.9,de;q=0.8";

    @Override
    public byte[] download(URI uri, Duration connectTimeout, Duration readTimeout) throws IOException {
        return download(uri, connectTimeout, readTimeout, MAX_REDIRECT_HOPS);
    }

    private byte[] download(URI uri, Duration connectTimeout, Duration readTimeout, int remainingHops) throws IOException {
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout((int) connectTimeout.toMillis());
        connection.setReadTimeout((int) readTimeout.toMillis());
        connection.setRequestMethod("GET");

        applyBrowserHeaders(connection);

        try {
            int status = connection.getResponseCode();
            if (isRedirect(status) && remainingHops > 0) {
                String location = connection.getHeaderField("Location");
                if (location == null || location.isEmpty()) {
                    throw new IOException("Received redirect status " + status + " from " + uri + " without a Location header");
                }
                URI target;
                try {
                    target = uri.resolve(location);
                } catch (IllegalArgumentException ex) {
                    throw new IOException("Received redirect status " + status + " from " + uri
                        + " with a malformed Location header '" + location + "'", ex);
                }
                return download(target, connectTimeout, readTimeout, remainingHops - 1);
            }
            if (status < 200 || status >= 300) {
                throw new HttpDownloadException("Unexpected HTTP status " + status + " downloading " + uri, status);
            }
            return readFully(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    private static void applyBrowserHeaders(HttpURLConnection connection) {
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", ACCEPT);
        connection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        connection.setRequestProperty("Sec-Fetch-Dest", "document");
        connection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        connection.setRequestProperty("Sec-Fetch-Site", "none");
        connection.setRequestProperty("Sec-Fetch-User", "?1");
        connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
    }

    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
            || status == HttpURLConnection.HTTP_MOVED_TEMP
            || status == HttpURLConnection.HTTP_SEE_OTHER
            || status == 307
            || status == 308;
    }

    private static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

}
