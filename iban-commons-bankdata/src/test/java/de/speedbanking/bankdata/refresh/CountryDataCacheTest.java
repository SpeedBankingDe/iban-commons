package de.speedbanking.bankdata.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.BankDataConfig;
import de.speedbanking.bankdata.io.BankDataFormat;
import de.speedbanking.bankdata.io.Downloader;
import de.speedbanking.bankdata.io.HttpDownloadException;
import de.speedbanking.bankdata.loader.BankDataLoaderDe;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Unit tests for {@link CountryDataCache}, exercising the refresh orchestration (lazy sync load,
 * staleness-triggered async refresh, atomic in-memory swap, error path keeps last good data) with
 * a simple hand-written {@link Downloader} fake instead of a network connection.
 */
@SuppressWarnings("checkstyle:MethodName")
final class CountryDataCacheTest {

    private static final String TEST_COUNTRY_CODE = "ZZ"; // deliberately unregistered, no bundled resource

    @BeforeEach
    void deleteAnyLeftoverCacheFile() throws IOException {
        // several tests below assume TEST_COUNTRY_CODE/CZ start out with no persisted local cache
        // file; without this cleanup, a cache file written by one test method (the cache directory
        // is shared for the whole test run) could leak into and destabilize another
        Files.deleteIfExists(BankDataConfig.get().getCacheDirectory().resolve(TEST_COUNTRY_CODE + ".csv"));
        Files.deleteIfExists(BankDataConfig.get().getCacheDirectory().resolve("CZ.csv"));
    }

    @Test
    void loadSync_noCacheFileNoBundledResource_returnsEmptyMap() {
        CountryDataCache cache = new CountryDataCache(new FakeLoader(), new FailingDownloader());

        assertThat(cache.getOrLoadSync()).isEmpty();
    }

    @Test
    void triggerAsyncRefreshIfStale_successfulDownload_updatesInMemoryDataAndPersistsFiles() throws Exception {
        BankData record = new BankData(TEST_COUNTRY_CODE, "12345", null, "Fake Bank", null, null, "irrelevant");
        FakeLoader loader = new FakeLoader(singletonList(record));
        SucceedingDownloader downloader = new SucceedingDownloader("raw-bytes".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        assertThat(cache.getOrLoadSync()).isEmpty(); // nothing loaded yet

        cache.triggerAsyncRefreshIfStale();
        Map<String, BankData> refreshed = awaitNonEmpty(cache);

        assertThat(refreshed).containsKey("12345");
        assertThat(refreshed.get("12345").getBankName()).isEqualTo("Fake Bank");
        assertThat(downloader.callCount.get()).isEqualTo(1);

        // dataRef is swapped in-memory slightly before the cache file is written to disk
        // (see CountryDataCache#refreshNow), so poll for file persistence rather than asserting immediately.
        awaitFileExists(BankDataConfig.get().getCacheDirectory().resolve(TEST_COUNTRY_CODE + ".csv"));
    }

    @Test
    void triggerAsyncRefreshIfStale_duplicateKeyInParsedRecords_keepsFirstOccurrence() throws Exception {
        // toMap() defensively dedups (first occurrence wins, matching every loader's own dedup
        // convention) in case two records ever share a lookup key, even though a well-behaved
        // loader is expected to have already deduped its own raw rows before returning them
        BankData first = new BankData(TEST_COUNTRY_CODE, "12345", null, "First Bank", null, null, "irrelevant");
        BankData duplicate = new BankData(TEST_COUNTRY_CODE, "12345", null, "Duplicate Bank", null, null, "irrelevant");
        FakeLoader loader = new FakeLoader(Arrays.asList(first, duplicate));
        SucceedingDownloader downloader = new SucceedingDownloader("raw-bytes".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.triggerAsyncRefreshIfStale();
        Map<String, BankData> refreshed = awaitNonEmpty(cache);

        assertThat(refreshed).hasSize(1);
        assertThat(refreshed.get("12345").getBankName()).isEqualTo("First Bank");
    }

    @Test
    void triggerAsyncRefreshIfStale_downloadFails_keepsPreviousData() throws Exception {
        FakeLoader loader = new FakeLoader(emptyList());
        FailingDownloader downloader = new FailingDownloader();
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.getOrLoadSync(); // establishes initial (empty) state
        cache.triggerAsyncRefreshIfStale();

        Thread.sleep(200); // give the background executor a chance to run and fail

        assertThat(cache.getOrLoadSync()).isEmpty();
        assertThat(downloader.callCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void triggerAsyncRefreshIfStale_concurrentCalls_onlyOneRefreshInFlight() throws Exception {
        SlowDownloader downloader = new SlowDownloader();
        FakeLoader loader = new FakeLoader(emptyList());
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.triggerAsyncRefreshIfStale();
        cache.triggerAsyncRefreshIfStale();
        cache.triggerAsyncRefreshIfStale();

        downloader.awaitAtLeastOneCall();
        Thread.sleep(100);

        assertThat(downloader.callCount.get()).isEqualTo(1);
        downloader.release();
    }

    @Test
    void triggerAsyncRefreshIfStale_freshBundledResource_doesNotTriggerRefresh() throws Exception {
        // the real /bankdata/DE.csv bundled resource on the test classpath was just copied there
        // by the build (maven-resources-plugin), so its last-modified timestamp is recent, well
        // within the default 90-day staleness threshold: a freshly rebuilt bundled resource (e.g.
        // right after a release) must not immediately be flagged stale
        FailingDownloader downloader = new FailingDownloader();
        CountryDataCache cache = new CountryDataCache(new BankDataLoaderDe(), downloader);

        cache.getOrLoadSync();
        cache.triggerAsyncRefreshIfStale();
        Thread.sleep(200); // give a wrongly-scheduled background refresh a chance to run

        assertThat(downloader.callCount.get()).isZero();
    }

    @Test
    void triggerAsyncRefreshIfStale_bundledResourceWithUndeterminableTimestamp_triggersRefresh() throws Exception {
        // a country with no bundled classpath resource at all, so loadBundledFallback falls back
        // to the Instant.EPOCH sentinel, the one remaining "always stale" case; uses a country code
        // distinct from TEST_COUNTRY_CODE ("ZZ") so its persisted cache file cannot leak into the
        // other tests above/below that assert on ZZ's cache state
        SucceedingDownloader downloader = new SucceedingDownloader("raw-bytes".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(new FakeLoader("Y4", emptyList()), downloader);

        cache.getOrLoadSync();
        cache.triggerAsyncRefreshIfStale();

        long deadline = System.currentTimeMillis() + 5000;
        while (downloader.callCount.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        assertThat(downloader.callCount.get()).isEqualTo(1);
    }

    @Test
    void triggerAsyncRefreshIfStale_downloadedDataFarBelowHalfOfBundledFallback_discardsRefreshAndKeepsPreviousData() throws Exception {
        // CZ.csv on the test classpath has 46 records; a "refresh" that only comes back with a
        // single record looks like truncated/corrupted upstream data, not a legitimate shrink
        FakeLoader loader = new FakeLoader("CZ", singletonList(
            new BankData("CZ", "0001", null, "Suspiciously Tiny Result", null, null, "irrelevant")));
        SucceedingDownloader downloader = new SucceedingDownloader("raw-bytes".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        Map<String, BankData> initial = cache.getOrLoadSync(); // loads the real bundled CZ.csv fallback
        assertThat(initial).hasSizeGreaterThan(2);

        cache.triggerAsyncRefreshIfStale();
        Thread.sleep(200); // give the background executor a chance to run

        assertThat(downloader.callCount.get()).isEqualTo(1);
        assertThat(cache.getOrLoadSync()).isEqualTo(initial); // discarded refresh must not replace the good data
        assertThat(Files.exists(BankDataConfig.get().getCacheDirectory().resolve("CZ.csv"))).isFalse();
    }

    @Test
    void triggerAsyncRefreshIfStale_emptyRefreshResult_isAlwaysDiscardedEvenWithoutBundledBaseline() throws Exception {
        // TEST_COUNTRY_CODE ("ZZ") has no bundled classpath resource, so bundledCount is 0 and the
        // "less than half the bundled fallback" check alone would never fire for an empty result
        AtomicInteger parseCallCount = new AtomicInteger();
        BankData record = new BankData(TEST_COUNTRY_CODE, "12345", null, "Real Bank", null, null, "irrelevant");
        CountryBankDataLoader loader = new FakeLoader(singletonList(record)) {
            @Override
            public List<BankData> parse(InputStream rawSource, String sourceVersion) {
                return parseCallCount.getAndIncrement() == 0 ? singletonList(record) : emptyList();
            }
        };
        SucceedingDownloader downloader = new SucceedingDownloader("raw-bytes".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.getOrLoadSync();
        cache.triggerAsyncRefreshIfStale();
        Map<String, BankData> established = awaitNonEmpty(cache);
        assertThat(established).containsKey("12345");

        cache.triggerAsyncRefreshIfStale(); // second refresh returns an empty result this time
        Thread.sleep(200); // give the background executor a chance to run

        assertThat(cache.getOrLoadSync()).isEqualTo(established); // the empty result must not replace good data
    }

    @Test
    void loadSync_emptyCacheFile_fallsBackToBundled() throws Exception {
        String countryCode = "Y1";
        writeCacheFile(countryCode, new byte[0]);

        CountryDataCache cache = new CountryDataCache(new FakeLoader(countryCode), new FailingDownloader());

        assertThat(cache.getOrLoadSync()).isEmpty();
    }

    @Test
    void loadSync_cacheFileWithWrongHeader_fallsBackToBundled() throws Exception {
        String countryCode = "Y2";
        String content = "not,the,right,header\n12345,,Fake Bank,,,\n";
        writeCacheFile(countryCode, content.getBytes(UTF_8));

        CountryDataCache cache = new CountryDataCache(new FakeLoader(countryCode), new FailingDownloader());

        assertThat(cache.getOrLoadSync()).isEmpty();
    }

    @Test
    void loadSync_tinyValidCacheFile_loadsSingleRecordInsteadOfBeingRejectedAsTooSmall() throws Exception {
        // guards against a byte-size sanity check that would wrongly reject a legitimately tiny
        // country (e.g. a micro-state with a single bank) as "suspiciously small"
        String countryCode = "Y3";
        String content = BankDataFormat.HEADER_LINE + "\n001;;;Micro State Bank;;;\n";
        writeCacheFile(countryCode, content.getBytes(UTF_8));

        CountryDataCache cache = new CountryDataCache(new FakeLoader(countryCode), new FailingDownloader());

        Map<String, BankData> data = cache.getOrLoadSync();
        assertThat(data).containsKey("001");
        assertThat(data.get("001").getBankName()).isEqualTo("Micro State Bank");
    }

    @Test
    void triggerAsyncRefreshIfStale_loaderResolvesActualSourceUri_downloadsSecondHopAndParsesIt() throws Exception {
        URI landingPageUri = URI.create("https://example.invalid/zz-landing-page");
        URI actualUri = URI.create("https://example.invalid/zz-actual-data");
        BankData record = new BankData(TEST_COUNTRY_CODE, "99999", null, "Redirected Bank", null, null, "irrelevant");

        CountryBankDataLoader loader = new FakeLoader(singletonList(record)) {
            @Override
            public URI getRemoteSourceUri() {
                return landingPageUri;
            }

            @Override
            public Optional<URI> resolveActualSourceUri(URI downloadedFrom, byte[] downloadedBytes) {
                // must not run assertions here: this executes on the background refresh thread,
                // where a failed assertion is silently swallowed rather than failing the test
                return Optional.of(actualUri);
            }
        };
        RecordingDownloader downloader = new RecordingDownloader();
        downloader.respond(landingPageUri, "landing-page-html".getBytes(UTF_8));
        downloader.respond(actualUri, "actual-data".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.getOrLoadSync(); // establishes initial state before the async refresh runs, avoiding a
                                // race with loadInitial() clobbering an already-completed refresh
        cache.triggerAsyncRefreshIfStale();
        Map<String, BankData> refreshed = awaitNonEmpty(cache);

        assertThat(refreshed).containsKey("99999");
        assertThat(downloader.requestedUris).containsExactly(landingPageUri, actualUri);
    }

    @Test
    void triggerAsyncRefreshIfStale_loaderDoesNotResolveActualSourceUri_downloadsOnlyOnce() throws Exception {
        BankData record = new BankData(TEST_COUNTRY_CODE, "12345", null, "Fake Bank", null, null, "irrelevant");
        FakeLoader loader = new FakeLoader(singletonList(record));
        RecordingDownloader downloader = new RecordingDownloader();
        downloader.respond(loader.getRemoteSourceUri(), "raw-bytes".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.getOrLoadSync();
        cache.triggerAsyncRefreshIfStale();
        awaitNonEmpty(cache);

        assertThat(downloader.requestedUris).containsExactly(loader.getRemoteSourceUri());
    }

    @Test
    void loadSync_cacheFileWithMalformedDataRow_fallsBackToBundled() throws Exception {
        // valid header, but a data row with too few fields makes BankDataFormat#read throw
        // IOException, exercising loadInitial()'s readDataFile() catch block
        String countryCode = "Y6";
        String content = BankDataFormat.HEADER_LINE + "\n001;002\n";
        writeCacheFile(countryCode, content.getBytes(UTF_8));

        CountryDataCache cache = new CountryDataCache(new FakeLoader(countryCode), new FailingDownloader());

        assertThat(cache.getOrLoadSync()).isEmpty();
    }

    @Test
    void triggerAsyncRefreshIfStale_persistToCacheFails_stillUpdatesInMemoryData() throws Exception {
        // pre-create a non-empty directory at the target cache file path so the atomic rename in
        // persistToCache() fails with IOException, which persistToCache() must swallow (log and
        // continue) rather than let it corrupt the in-memory refresh that already succeeded
        String countryCode = "Y5";
        Path collidingDir = BankDataConfig.get().getCacheDirectory().resolve(countryCode + ".csv");
        Files.createDirectories(collidingDir.resolve("blocking-child"));
        try {
            BankData record = new BankData(countryCode, "12345", null, "Fake Bank", null, null, "irrelevant");
            FakeLoader loader = new FakeLoader(countryCode, singletonList(record));
            SucceedingDownloader downloader = new SucceedingDownloader("raw-bytes".getBytes(UTF_8));
            CountryDataCache cache = new CountryDataCache(loader, downloader);

            cache.getOrLoadSync();
            cache.triggerAsyncRefreshIfStale();
            Map<String, BankData> refreshed = awaitNonEmpty(cache);

            assertThat(refreshed).containsKey("12345");
        } finally {
            Files.deleteIfExists(collidingDir.resolve("blocking-child"));
            Files.deleteIfExists(collidingDir);
        }
    }

    @Test
    void triggerAsyncRefreshIfStale_permanentHttpFailure_backsOffUntilFullThreshold() throws Exception {
        // a 4xx status is classified as permanent (the source URL itself is wrong); backOffAfterFailure()
        // must then reset fetchedAt to "now" instead of the short transient backoff, so an immediate
        // second trigger (with a non-zero threshold, unlike the other tests' Duration.ZERO) must not
        // fire another download
        FakeLoader loader = new FakeLoader("Y8", emptyList()) {
            @Override
            public Duration getRecommendedRefreshThreshold() {
                return Duration.ofHours(1);
            }
        };
        ThrowingDownloader downloader = new ThrowingDownloader(() -> new HttpDownloadException("not found", HttpURLConnection.HTTP_NOT_FOUND));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.getOrLoadSync(); // establishes initial (empty) state, required for backOffAfterFailure() to act
        cache.triggerAsyncRefreshIfStale();
        downloader.awaitCallCount(1);
        Thread.sleep(100);

        cache.triggerAsyncRefreshIfStale(); // immediately stale-checked again; must not trigger another attempt
        Thread.sleep(100);

        assertThat(downloader.callCount.get()).isEqualTo(1);
        assertThat(cache.getOrLoadSync()).isEmpty();
    }

    @Test
    void triggerAsyncRefreshIfStale_loaderThrowsRuntimeException_backsOffTransientlyWithoutCrashing() throws Exception {
        // a bug in a loader's parse() (e.g. an NPE on unexpected upstream shape) is not a
        // network/download problem, nor a case the loader itself recognized and rejected via
        // BankDataParseException, so it must not be classified as the permanent 4xx case; it still
        // gets the short transient backoff, and must not corrupt refreshInFlight or crash the
        // background executor
        CountryBankDataLoader loader = new FakeLoader("Y7", emptyList()) {
            @Override
            public Duration getRecommendedRefreshThreshold() {
                return Duration.ofHours(1);
            }

            @Override
            public List<BankData> parse(InputStream rawSource, String sourceVersion) {
                throw new NullPointerException("simulated bug in loader.parse()");
            }
        };
        SucceedingDownloader downloader = new SucceedingDownloader("raw-bytes".getBytes(UTF_8));
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.getOrLoadSync(); // establishes initial (empty) state, required for backOffAfterFailure() to act
        cache.triggerAsyncRefreshIfStale();
        Thread.sleep(200);

        assertThat(downloader.callCount.get()).isEqualTo(1);
        assertThat(cache.getOrLoadSync()).isEmpty();

        cache.triggerAsyncRefreshIfStale(); // immediately stale-checked again; must not trigger another attempt
        Thread.sleep(100);

        assertThat(downloader.callCount.get()).isEqualTo(1);
    }

    @Test
    void triggerAsyncRefreshIfStale_proxyAuthRequired_describesFailureAndKeepsPreviousData() throws Exception {
        assertDescribeFailureBranchIsExercised("Y9", () -> new HttpDownloadException("proxy auth", HttpURLConnection.HTTP_PROXY_AUTH));
    }

    @Test
    void triggerAsyncRefreshIfStale_unknownHost_describesFailureAndKeepsPreviousData() throws Exception {
        assertDescribeFailureBranchIsExercised("YA", () -> new UnknownHostException("example.invalid"));
    }

    @Test
    void triggerAsyncRefreshIfStale_connectionRefused_describesFailureAndKeepsPreviousData() throws Exception {
        assertDescribeFailureBranchIsExercised("YB", () -> new ConnectException("connection refused"));
    }

    @Test
    void triggerAsyncRefreshIfStale_socketTimeout_describesFailureAndKeepsPreviousData() throws Exception {
        assertDescribeFailureBranchIsExercised("YC", () -> new SocketTimeoutException("timed out"));
    }

    @Test
    void triggerAsyncRefreshIfStale_serverErrorHttpStatus_isNotClassifiedAsPermanent() throws Exception {
        // a 5xx status is transient (unlike a 4xx), exercising isPermanentFailure()'s false branch
        // for an actual HttpDownloadException, distinct from the "not even an HttpDownloadException" case
        assertDescribeFailureBranchIsExercised("YF", () -> new HttpDownloadException("server error", HttpURLConnection.HTTP_INTERNAL_ERROR));
    }

    private static void assertDescribeFailureBranchIsExercised(String countryCode, Supplier<IOException> exceptionSupplier) throws Exception {
        FakeLoader loader = new FakeLoader(countryCode, emptyList());
        ThrowingDownloader downloader = new ThrowingDownloader(exceptionSupplier);
        CountryDataCache cache = new CountryDataCache(loader, downloader);

        cache.getOrLoadSync();
        cache.triggerAsyncRefreshIfStale();
        downloader.awaitCallCount(1);

        assertThat(downloader.callCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(cache.getOrLoadSync()).isEmpty();
    }

    @Test
    void loadSync_concurrentCallsDuringSlowLoad_secondCallerReusesResultFromFirst() throws Exception {
        // forces the second synchronized re-check inside getOrLoadSync() (the "state was set by
        // the other thread while we were waiting for the lock" branch) instead of only the
        // outer, unsynchronized fast-path check
        SlowFirstCallLoader loader = new SlowFirstCallLoader("YD");
        CountryDataCache cache = new CountryDataCache(loader, new FailingDownloader());

        Thread first = new Thread(cache::getOrLoadSync);
        first.start();
        Thread.sleep(50); // let the first thread enter loadInitial() and start sleeping while holding loadLock
        Thread second = new Thread(cache::getOrLoadSync);
        second.start();

        first.join(2000);
        second.join(2000);

        assertThat(cache.getOrLoadSync()).isEmpty();
    }

    @Test
    void stringSummary_reflectsUnloadedAndLoadedState() {
        FakeLoader loader = new FakeLoader("YE", emptyList());
        CountryDataCache cache = new CountryDataCache(loader, new FailingDownloader());

        assertThat(cache.toString())
            .contains("countryCode=YE")
            .contains("loaded=false")
            .contains("records=0")
            .contains("sourceVersion=null")
            .contains("fetchedAt=null")
            .contains("fromBundledFallback=false");

        cache.getOrLoadSync();

        assertThat(cache.toString())
            .contains("loaded=true")
            .contains("records=0")
            .contains("fromBundledFallback=true");
    }

    private static void writeCacheFile(String countryCode, byte[] content) throws IOException {
        Path cacheFile = BankDataConfig.get().getCacheDirectory().resolve(countryCode + ".csv");
        Files.createDirectories(cacheFile.getParent());
        Files.write(cacheFile, content);
    }

    private static void awaitFileExists(Path path) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!Files.exists(path) && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertThat(Files.exists(path)).as("expected %s to exist", path).isTrue();
    }

    private static Map<String, BankData> awaitNonEmpty(CountryDataCache cache) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        Map<String, BankData> result = cache.getOrLoadSync();
        while (result.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
            result = cache.getOrLoadSync();
        }
        return result;
    }

    /**
     * Minimal {@link CountryBankDataLoader} test double.
     */
    private static class FakeLoader implements CountryBankDataLoader {

        private final String         countryCode;
        private final List<BankData> parseResult;

        private FakeLoader() {
            this(TEST_COUNTRY_CODE, emptyList());
        }

        private FakeLoader(List<BankData> parseResult) {
            this(TEST_COUNTRY_CODE, parseResult);
        }

        private FakeLoader(String countryCode) {
            this(countryCode, emptyList());
        }

        private FakeLoader(String countryCode, List<BankData> parseResult) {
            this.countryCode = countryCode;
            this.parseResult = parseResult;
        }

        @Override
        public String getCountryCode() {
            return countryCode;
        }

        @Override
        public URI getRemoteSourceUri() {
            return URI.create("https://example.invalid/zz-bank-data");
        }

        @Override
        public Charset getRemoteSourceCharset() {
            return UTF_8;
        }

        @Override
        public List<BankData> parse(InputStream rawSource, String sourceVersion) throws BankDataParseException {
            return parseResult;
        }

        @Override
        public Duration getRecommendedRefreshThreshold() {
            return Duration.ZERO; // always stale, to make tests deterministic
        }
    }

    /**
     * {@link Downloader} fake that always succeeds with fixed bytes.
     */
    private static final class SucceedingDownloader implements Downloader {

        private final byte[]        bytes;
        private final AtomicInteger callCount = new AtomicInteger();

        private SucceedingDownloader(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public byte[] download(URI uri, Duration connectTimeout, Duration readTimeout) {
            callCount.incrementAndGet();
            return bytes;
        }
    }

    /**
     * {@link Downloader} fake returning per-URI canned responses and recording every requested URI,
     * for verifying a two-hop {@link CountryBankDataLoader#resolveActualSourceUri(URI, byte[])} flow.
     */
    private static final class RecordingDownloader implements Downloader {

        private final Map<URI, byte[]> responses     = new ConcurrentHashMap<>();
        private final List<URI>        requestedUris = new CopyOnWriteArrayList<>();

        void respond(URI uri, byte[] bytes) {
            responses.put(uri, bytes);
        }

        @Override
        public byte[] download(URI uri, Duration connectTimeout, Duration readTimeout) throws IOException {
            requestedUris.add(uri);
            byte[] bytes = responses.get(uri);
            if (bytes == null) {
                throw new IOException("No canned response configured for " + uri);
            }
            return bytes;
        }
    }

    /**
     * {@link Downloader} fake that always fails with a fresh exception from the given supplier,
     * for exercising {@code CountryDataCache}'s per-exception-type failure diagnosis.
     */
    private static final class ThrowingDownloader implements Downloader {

        private final Supplier<IOException> exceptionSupplier;
        private final AtomicInteger         callCount = new AtomicInteger();

        private ThrowingDownloader(Supplier<IOException> exceptionSupplier) {
            this.exceptionSupplier = exceptionSupplier;
        }

        @Override
        public byte[] download(URI uri, Duration connectTimeout, Duration readTimeout) throws IOException {
            callCount.incrementAndGet();
            throw exceptionSupplier.get();
        }

        private void awaitCallCount(int expected) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5000;
            while (callCount.get() < expected && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
        }
    }

    /**
     * {@link CountryBankDataLoader} test double whose {@link #getCountryCode()} blocks on its very
     * first invocation, to widen the window in which a concurrent {@code getOrLoadSync()} caller
     * can observe {@code state == null} and start waiting on {@code loadLock} before the first
     * caller finishes {@code loadInitial()} and releases it.
     */
    private static final class SlowFirstCallLoader extends FakeLoader {

        private final AtomicBoolean firstCall = new AtomicBoolean(true);

        private SlowFirstCallLoader(String countryCode) {
            super(countryCode);
        }

        @Override
        public String getCountryCode() {
            if (firstCall.compareAndSet(true, false)) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            return super.getCountryCode();
        }
    }

    /**
     * {@link Downloader} fake that always fails.
     */
    private static final class FailingDownloader implements Downloader {

        private final AtomicInteger callCount = new AtomicInteger();

        @Override
        public byte[] download(URI uri, Duration connectTimeout, Duration readTimeout) throws IOException {
            callCount.incrementAndGet();
            throw new IOException("simulated download failure");
        }
    }

    /**
     * {@link Downloader} fake that blocks until deliberately released, to test that only one
     * refresh runs at a time per country.
     */
    private static final class SlowDownloader implements Downloader {

        private final AtomicInteger callCount = new AtomicInteger();
        private final Object        lock      = new Object();
        private volatile boolean    released;

        @Override
        public byte[] download(URI uri, Duration connectTimeout, Duration readTimeout) throws IOException {
            callCount.incrementAndGet();
            synchronized (lock) {
                while (!released) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IOException("interrupted", ex);
                    }
                }
            }
            return new byte[0];
        }

        private void awaitAtLeastOneCall() throws InterruptedException {
            long deadline = System.currentTimeMillis() + 2000;
            while (callCount.get() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
        }

        private void release() {
            synchronized (lock) {
                released = true;
                lock.notifyAll();
            }
        }
    }

}
