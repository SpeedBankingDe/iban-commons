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
package de.speedbanking.bankdata.refresh;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.BankDataConfig;
import de.speedbanking.bankdata.io.BankDataFormat;
import de.speedbanking.bankdata.io.Downloader;
import de.speedbanking.bankdata.io.HttpDownloadException;
import de.speedbanking.bankdata.log.NaiveLogger;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-country lazily loaded, asynchronously refreshed bank data cache.
 * <p>
 * One instance per supported country, created lazily on first lookup. {@link #getOrLoadSync()}
 * synchronously builds the initial in-memory index, trying in order: a previously persisted local
 * cache file, the bundled classpath fallback ("Urladung"), or an empty result if neither exists.
 * <p>
 * Once that's returned, {@link #triggerAsyncRefreshIfStale()} may kick off a non-blocking
 * background refresh if the loaded data is older than the staleness threshold - at most one at a
 * time per country. Staleness is based on the actual age of the data (local cache file's
 * last-modified time, or the bundled resource's), so a freshly rebuilt bundled resource right
 * after a release isn't immediately flagged stale.
 * <p>
 * A refresh that comes back with fewer than half the bundled fallback's records is treated as
 * suspect - a truncated response, or an upstream page that changed shape but still returned
 * {@code 200 OK} - and gets discarded: log a warning, keep the last known good data.
 * <p>
 * A failed refresh (network error, parse error, discarded suspect response) shouldn't get hammered
 * on every subsequent lookup, so {@link #backOffAfterFailure(boolean)} back-dates the staleness
 * clock instead of adding a separate cooldown field. A 4xx (source URL is wrong) resets the clock
 * as if freshly loaded, so the next attempt waits the full threshold; anything else (5xx, timeout,
 * connection error, suspect response) only waits a short fixed backoff since it might just recover
 * on its own. An unanticipated {@link RuntimeException} (most likely a bug in a loader's
 * {@code parse()} rather than a network or data problem) is caught separately from the expected
 * {@link java.io.IOException}/{@link de.speedbanking.bankdata.spi.BankDataParseException} failure
 * modes and logged at {@code SEVERE} with distinct wording, so it doesn't blend in with the
 * routine warnings above, even though it still backs off the same way.
 *
 * @since 1.8.11
 */
public final class CountryDataCache {

    private static final NaiveLogger LOGGER = NaiveLogger.of(CountryDataCache.class);

    private static final String      CLASSPATH_RESOURCE_PREFIX = "/bankdata/";
    private static final String      CSV_SUFFIX = ".csv";
    private static final String      BUNDLED_SOURCE_VERSION = "bundled";

    /**
     * Fixed backoff applied after a transient refresh failure (5xx, timeout, connection error, or a
     * refresh discarded as suspect), by back-dating {@link State#fetchedAt} so the existing
     * staleness check in {@link #triggerAsyncRefreshIfStale()} naturally suppresses another attempt
     * until this much time has passed, without a separate cooldown field.
     */
    private static final Duration       TRANSIENT_FAILURE_BACKOFF = Duration.ofHours(6);

    private final CountryBankDataLoader loader;
    private final Downloader            downloader;
    private final Object                loadLock                  = new Object();
    private volatile State              state;
    private final AtomicBoolean         refreshInFlight           = new AtomicBoolean(false);
    private volatile long               bundledRecordCount        = -1;

    /**
     * Creates a new cache for the given country loader.
     *
     * @param loader     the loader responsible for this country
     * @param downloader the downloader used for background refreshes
     */
    public CountryDataCache(CountryBankDataLoader loader, Downloader downloader) {
        this.loader = loader;
        this.downloader = downloader;
    }

    /**
     * Returns the in-memory lookup index, loading it synchronously on first call.
     *
     * @return an immutable map of {@link BankData#getKey()} to {@link BankData}; never {@code null}, may be empty
     */
    public Map<String, BankData> getOrLoadSync() {
        State current = state;
        if (current != null) {
            return current.data;
        }
        synchronized (loadLock) {
            current = state;
            if (current != null) {
                return current.data;
            }
            loadInitial();
            return state.data;
        }
    }

    private void loadInitial() {
        String countryCode = loader.getCountryCode();
        Path cacheFile = cacheFilePath(countryCode);

        if (Files.isReadable(cacheFile)) {
            try {
                if (!looksLikeValidBankDataFile(cacheFile)) {
                    LOGGER.warn("Local bank data cache file for country {0} does not look like a valid bank data file "
                        + "(empty, or missing/unexpected CSV header), falling back to bundled data", countryCode);
                } else {
                    Instant fetchedAt = Files.getLastModifiedTime(cacheFile).toInstant();
                    String sourceVersion = fetchedAt.toString();
                    Map<String, BankData> data = readDataFile(cacheFile, countryCode, sourceVersion);
                    state = new State(data, sourceVersion, fetchedAt, false);
                    LOGGER.info("Loaded local bank data cache for country {0}: {1} records (source version {2})",
                        countryCode, data.size(), sourceVersion);
                    return;
                }
            } catch (IOException ex) {
                LOGGER.warn("Failed to read local bank data cache for country {0}, falling back to bundled data", ex, countryCode);
            }
        }

        loadBundledFallback(countryCode);
    }

    /**
     * Cheap sanity check performed before parsing a local cache file: the file must be non-empty
     * and start with the exact expected CSV header line.
     * <p>
     * Deliberately not a minimum byte-size threshold, a legitimately tiny country (e.g. a
     * micro-state with a single bank) would otherwise be rejected as "suspiciously small" even
     * though its file is complete and valid. Since {@link #persistToCache(List)} already writes
     * via a temp-file-then-atomic-rename, a partially written file can never land at the final
     * path in the first place; the remaining corruption cases (external tampering, filesystem
     * errors) are already caught by {@link BankDataFormat#read} throwing an {@link IOException},
     * which the caller falls back on regardless.
     *
     * @param cacheFile the local cache file to check
     * @return {@code true} if the file is non-empty and starts with {@link BankDataFormat#HEADER_LINE}
     * @throws IOException if the file cannot be read
     */
    private static boolean looksLikeValidBankDataFile(Path cacheFile) throws IOException {
        if (Files.size(cacheFile) == 0) {
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(cacheFile, UTF_8)) {
            String firstLine = reader.readLine();
            return BankDataFormat.HEADER_LINE.equals(firstLine);
        }
    }

    private void loadBundledFallback(String countryCode) {
        String resourceName = CLASSPATH_RESOURCE_PREFIX + countryCode + CSV_SUFFIX;
        URL resourceUrl = CountryDataCache.class.getResource(resourceName);
        if (resourceUrl == null) {
            LOGGER.warn("No bundled bank data resource found for country {0}", countryCode);
            state = new State(emptyMap(), BUNDLED_SOURCE_VERSION, Instant.EPOCH, true);
            return;
        }

        try {
            URLConnection connection = resourceUrl.openConnection();
            Map<String, BankData> data;
            try (InputStream in = connection.getInputStream()) {
                data = toMap(BankDataFormat.read(in, UTF_8, countryCode, BUNDLED_SOURCE_VERSION));
            }
            // getLastModified() returns 0 when it cannot be determined; treat that as "unknown",
            // maximally stale, so a refresh is still attempted soon (the only remaining
            // always-stale case, not the normal path anymore)
            long lastModifiedMillis = connection.getLastModified();
            Instant fetchedAt = lastModifiedMillis > 0 ? Instant.ofEpochMilli(lastModifiedMillis) : Instant.EPOCH;

            state = new State(data, BUNDLED_SOURCE_VERSION, fetchedAt, true);

            LOGGER.info("Loaded bundled fallback bank data for country {0}: {1} records (resource last modified {2})",
                countryCode, data.size(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault()).format(fetchedAt));
        } catch (IOException ex) {
            LOGGER.warn("Failed to read bundled bank data resource for country {0}", ex, countryCode);
            state = new State(emptyMap(), BUNDLED_SOURCE_VERSION, Instant.EPOCH, true);
        }
    }

    /**
     * Checks whether the currently loaded data is stale and, if so, triggers a non-blocking
     * background refresh. Never throws, never blocks. A no-op if network access is disabled (see
     * {@link BankDataConfig#isNetworkDisabled()}) or a refresh for this country is already in
     * flight.
     */
    @SuppressWarnings("TimeZoneUsage") // Instant.now() here is a plain elapsed-time/staleness comparison, not a display timestamp
    public void triggerAsyncRefreshIfStale() {
        if (BankDataConfig.get().isNetworkDisabled()) {
            return;
        }
        State current = state;
        Duration threshold = loader.getRecommendedRefreshThreshold();
        boolean stale = current == null || Duration.between(current.fetchedAt, Instant.now()).compareTo(threshold) > 0;
        if (!stale) {
            return;
        }
        if (refreshInFlight.compareAndSet(false, true)) {
            if (current == null) {
                LOGGER.info("Bank data for country {0} has not been loaded yet, scheduling a background refresh", loader.getCountryCode());
            } else {
                LOGGER.info("Bank data for country {0} is stale (currently loaded from {1}, source version {2}), scheduling a background refresh",
                    loader.getCountryCode(), current.fromBundledFallback ? "bundled fallback" : "local cache", current.sourceVersion);
            }
            RefreshExecutors.get().execute(this::refreshNow);
        }
    }

    @SuppressWarnings("TimeZoneUsage") // sourceVersion is a UTC-based, machine-readable vintage stamp, not a display timestamp
    private void refreshNow() {
        try {
            BankDataConfig config = BankDataConfig.get();
            URI sourceUri = loader.getRemoteSourceUri();
            byte[] rawBytes = downloader.download(sourceUri, config.getConnectTimeout(), config.getReadTimeout());
            Optional<URI> actualSourceUri = loader.resolveActualSourceUri(sourceUri, rawBytes);
            if (actualSourceUri.isPresent()) {
                LOGGER.info("Resolved actual bank data source for country {0}: {1} -> {2}",
                    loader.getCountryCode(), sourceUri, actualSourceUri.get());
                rawBytes = downloader.download(actualSourceUri.get(), config.getConnectTimeout(), config.getReadTimeout());
            }
            Instant fetchedAt = Instant.now();
            String sourceVersion = fetchedAt.toString();
            List<BankData> records = loader.parse(new ByteArrayInputStream(rawBytes), sourceVersion);

            long bundledCount = getBundledRecordCount();
            // an empty refresh is always rejected, even with no bundled baseline to compare against
            // (bundledCount == 0, e.g. a future country shipped without bundled fallback data, or a
            // bundled resource that failed to load): a genuinely empty upstream response is not a
            // plausible outcome for a national bank directory, so this is never a false positive
            if (records.isEmpty() || (bundledCount > 0 && records.size() * 2L < bundledCount)) {
                LOGGER.warn("Refreshed bank data for country {0} has only {1} record(s), less than half of the {2} record(s) "
                    + "in the bundled fallback; discarding this refresh as likely truncated/corrupted upstream data and "
                    + "keeping last known good data", loader.getCountryCode(), records.size(), bundledCount);
                // truncated/corrupted-response, not a wrong URL - the class Javadoc classifies this
                // under "any other failure" (short backoff), not the 4xx/permanent case
                backOffAfterFailure(false);
                return;
            }

            Map<String, BankData> newData = toMap(records);

            state = new State(newData, sourceVersion, fetchedAt, false);

            persistToCache(records);
            LOGGER.info("Refreshed bank data for country {0}: {1} records (source version {2})",
                loader.getCountryCode(), newData.size(), sourceVersion);
        } catch (IOException | BankDataParseException ex) {
            // anticipated failure modes: a network/download problem, or upstream data this loader's
            // parse() correctly recognized and rejected as malformed
            boolean permanent = isPermanentFailure(ex);
            if (permanent) {
                LOGGER.error("Background bank data refresh for country {0} failed with {1} at {2}: the source URL likely "
                    + "moved or was removed, a retry cannot fix this. Please check for a newer release of this library "
                    + "with an updated source URI for this country and upgrade. Keeping last known good data and not "
                    + "attempting another automatic refresh for this country for up to {3}",
                    ex, loader.getCountryCode(), describeFailure(ex), loader.getRemoteSourceUri(), loader.getRecommendedRefreshThreshold());
            } else {
                LOGGER.warn("Background bank data refresh failed for country {0}, keeping last known good data ({1})",
                    ex, loader.getCountryCode(), describeFailure(ex));
            }
            backOffAfterFailure(permanent);
        } catch (RuntimeException ex) {
            // an unanticipated failure (NPE, IndexOutOfBoundsException, ...) most likely means a bug
            // in this loader's parse() or elsewhere in this method, not a network/data problem, so it
            // is logged at SEVERE and worded distinctly rather than blending in with the routine,
            // expected-to-happen-occasionally warnings above; still backed off like a transient
            // failure (not escalated to the permanent/full-threshold backoff, since nothing here
            // establishes this is specific to the current source URI the way a 4xx status does)
            LOGGER.error("Background bank data refresh for country {0} failed unexpectedly ({1}), which looks like a bug "
                + "in this loader rather than a network or upstream data problem. Keeping last known good data and "
                + "retrying in {2}; please report this if it keeps happening.",
                ex, loader.getCountryCode(), ex.getClass().getSimpleName(), TRANSIENT_FAILURE_BACKOFF);
            backOffAfterFailure(false);
        } finally {
            refreshInFlight.set(false);
        }
    }

    /**
     * Back-dates the currently loaded {@link State#fetchedAt} so the normal staleness check
     * suppresses another refresh attempt for a while, without retrying (and without a separate
     * cooldown field or config option). Leaves the loaded data itself untouched.
     *
     * @param permanent {@code true} for a failure that a retry cannot fix (a 4xx HTTP status: the
     *                  source URL itself is wrong), which resets the staleness clock as if freshly
     *                  loaded so the next attempt waits the full {@link
     *                  CountryBankDataLoader#getRecommendedRefreshThreshold()}; {@code false} for a
     *                  transient failure (5xx, timeout, connection error, suspect response), which
     *                  only waits {@link #TRANSIENT_FAILURE_BACKOFF}
     */
    @SuppressWarnings("TimeZoneUsage") // Instant.now() here is a plain elapsed-time/staleness comparison, not a display timestamp
    private void backOffAfterFailure(boolean permanent) {
        State current = state;
        if (current == null) {
            return;
        }
        Instant newFetchedAt = permanent
            ? Instant.now()
            : Instant.now().minus(loader.getRecommendedRefreshThreshold()).plus(TRANSIENT_FAILURE_BACKOFF);
        state = new State(current.data, current.sourceVersion, newFetchedAt, current.fromBundledFallback);
    }

    /**
     * Checks whether a refresh failure is one a retry cannot fix: a 4xx HTTP status means the
     * remote source URL itself is wrong (moved, removed), so hitting it again on the usual cadence
     * would just keep failing (and keep bothering whatever now lives at that URL). The source URI is
     * hardcoded per loader (no runtime override), so the only real fix is a newer release of this
     * library with an updated URI.
     *
     * @param ex the failure from a refresh attempt
     * @return {@code true} if the failure is a 4xx HTTP status
     */
    private static boolean isPermanentFailure(Throwable ex) {
        if (!(ex instanceof HttpDownloadException)) {
            return false;
        }
        int statusCode = ((HttpDownloadException) ex).getStatusCode();
        return statusCode >= HttpURLConnection.HTTP_BAD_REQUEST && statusCode < HttpURLConnection.HTTP_INTERNAL_ERROR;
    }

    /**
     * Returns a short, actionable diagnosis for a failed download, to make common network
     * misconfigurations (proxy, DNS, firewall, timeout) easier to spot in the log than a bare
     * exception class name.
     *
     * @param ex the throwable to describe
     * @return a short diagnostic phrase
     */
    private static String describeFailure(Throwable ex) {
        if (ex instanceof HttpDownloadException) {
            int statusCode = ((HttpDownloadException) ex).getStatusCode();
            if (statusCode == HttpURLConnection.HTTP_PROXY_AUTH) {
                return "HTTP 407: proxy authentication required, check proxy credentials/configuration";
            }
            return "HTTP status " + statusCode;
        } else if (ex instanceof UnknownHostException) {
            return "unknown host, check DNS resolution and network connectivity";
        } else if (ex instanceof ConnectException) {
            return "connection refused, a firewall or proxy may be blocking the connection";
        } else if (ex instanceof SocketTimeoutException) {
            return "timed out, possibly a slow network or an unresponsive proxy";
        }
        return ex.getClass().getSimpleName();
    }

    /**
     * Returns the number of data records in the bundled classpath fallback resource for this
     * cache's country, used by {@link #refreshNow()} as a sanity baseline for freshly downloaded
     * data. Counted once per {@link CountryDataCache} instance and cached, since the bundled
     * resource on the classpath never changes at runtime.
     *
     * @return the record count of the bundled resource, or {@code 0} if it does not exist or
     *         cannot be read (in which case the sanity check in {@link #refreshNow()} is skipped)
     */
    private long getBundledRecordCount() {
        long count = bundledRecordCount;
        if (count >= 0) {
            return count;
        }
        count = countBundledRecords(loader.getCountryCode());
        bundledRecordCount = count;
        return count;
    }

    private static long countBundledRecords(String countryCode) {
        String resourceName = CLASSPATH_RESOURCE_PREFIX + countryCode + CSV_SUFFIX;
        URL resourceUrl = CountryDataCache.class.getResource(resourceName);
        if (resourceUrl == null) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            return reader.lines().skip(1).count(); // skip the CSV header line
        } catch (IOException ex) {
            LOGGER.warn("Failed to count records in bundled bank data resource for country {0} for refresh sanity check", ex, countryCode);
            return 0;
        }
    }

    private void persistToCache(List<BankData> records) {
        String countryCode = loader.getCountryCode();
        Path cacheFile = cacheFilePath(countryCode);
        Path tempFile = null;
        try {
            Files.createDirectories(BankDataConfig.get().getCacheDirectory());

            tempFile = Files.createTempFile(BankDataConfig.get().getCacheDirectory(), countryCode, CSV_SUFFIX + ".tmp");
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                BankDataFormat.write(out, records);
            }

            // a single atomic rename: with one file per country (instead of the former .dat + .meta
            // sidecar pair) there is no longer a second rename that could leave the pair inconsistent
            Files.move(tempFile, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            tempFile = null;
        } catch (IOException ex) {
            LOGGER.warn("Failed to persist refreshed bank data cache for country {0}", ex, countryCode);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            LOGGER.warn("Failed to clean up temporary bank data cache file {0}", ex, path);
        }
    }

    private Map<String, BankData> readDataFile(Path cacheFile, String countryCode, String sourceVersion) throws IOException {
        try (InputStream in = Files.newInputStream(cacheFile)) {
            return toMap(BankDataFormat.read(in, UTF_8, countryCode, sourceVersion));
        }
    }

    /**
     * Consolidates parsed records into the final lookup map, keyed by {@link BankData#getKey()}.
     * <p>
     * A {@code CountryBankDataLoader} is expected to already dedup the raw rows it parses (see its
     * class Javadoc), so a collision here is not expected in the normal path; it can still happen
     * for data read back from a local cache file (external tampering, filesystem corruption, or a
     * future loader without its own dedup), so it is handled defensively: the first occurrence
     * wins, matching every loader's own dedup convention, and the collision is logged rather than
     * silently dropping a record with zero diagnostic trail.
     *
     * @param records the parsed records, in encounter order
     * @return an immutable map of {@link BankData#getKey()} to {@link BankData}
     */
    private static Map<String, BankData> toMap(List<BankData> records) {
        Map<String, BankData> map = new LinkedHashMap<>(Math.max(16, records.size() * 2));
        for (BankData record : records) {
            BankData previous = map.putIfAbsent(record.getKey(), record);
            if (previous != null) {
                LOGGER.warn("Duplicate bank data key {0} for country {1}: keeping {2}, discarding {3}",
                    record.getKey(), record.getCountryCode(), previous, record);
            }
        }
        return unmodifiableMap(map);
    }

    private static Path cacheFilePath(String countryCode) {
        return BankDataConfig.get().getCacheDirectory().resolve(countryCode + CSV_SUFFIX);
    }

    @Override
    public String toString() {
        Optional<State> current = Optional.ofNullable(state);
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add("countryCode=" + loader.getCountryCode())
            .add("loaded=" + current.isPresent())
            .add("records=" + current.map(s -> s.data.size()).orElse(0))
            .add("sourceVersion=" + current.map(s -> s.sourceVersion).orElse(null))
            .add("fetchedAt=" + current.map(s -> s.fetchedAt).orElse(null))
            .add("fromBundledFallback=" + current.map(s -> s.fromBundledFallback).orElse(false))
            .toString();
    }

    /**
     * Immutable snapshot of the currently loaded index and its provenance, always replaced
     * together so a concurrent reader never observes data and metadata from two different loads.
     */
    private static final class State {
        private final Map<String, BankData> data;
        private final String                sourceVersion;
        private final Instant               fetchedAt;
        private final boolean               fromBundledFallback;

        private State(Map<String, BankData> data, String sourceVersion, Instant fetchedAt, boolean fromBundledFallback) {
            this.data = data;
            this.sourceVersion = sourceVersion;
            this.fetchedAt = fetchedAt;
            this.fromBundledFallback = fromBundledFallback;
        }
    }

}
