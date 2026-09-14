package de.speedbanking.bankdata.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.BankDataConfig;
import de.speedbanking.bankdata.io.BankDataFormat;
import de.speedbanking.bankdata.io.Downloader;
import de.speedbanking.bankdata.loader.DeBundesbankLoader;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;
import de.speedbanking.iban.Iban;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
        CountryDataCache cache = new CountryDataCache(new DeBundesbankLoader(), downloader);

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
        String content = BankDataFormat.HEADER_LINE + "\n001,,Micro State Bank,,,\n";
        writeCacheFile(countryCode, content.getBytes(UTF_8));

        CountryDataCache cache = new CountryDataCache(new FakeLoader(countryCode), new FailingDownloader());

        Map<String, BankData> data = cache.getOrLoadSync();
        assertThat(data).containsKey("001");
        assertThat(data.get("001").getBankName()).isEqualTo("Micro State Bank");
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
    private static final class FakeLoader implements CountryBankDataLoader {

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

        @Override
        public String extractBankCode(Iban iban) {
            return iban.getBankCode();
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
