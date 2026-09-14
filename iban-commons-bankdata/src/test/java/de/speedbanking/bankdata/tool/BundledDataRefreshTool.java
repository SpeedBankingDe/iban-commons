package de.speedbanking.bankdata.tool;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.BankDataRegistry;
import de.speedbanking.bankdata.io.BankDataFormat;
import de.speedbanking.bankdata.io.Downloader;
import de.speedbanking.bankdata.spi.BankDataParseException;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Maintainer-only tool that regenerates the bundled "Urladung" CSV fallback files under
 * {@code src/main/resources/bankdata} from each country's live upstream source.
 * <p>
 * For every country registered in {@link BankDataRegistry}, this tool downloads the raw source
 * (with generous timeouts, since this is a one-off manual run, not a latency-sensitive path),
 * parses it with the country's loader, and overwrites {@code {CC}.csv} on success. A failure for
 * one country (network error, parse error, placeholder/invalid source URL, ...) is reported to
 * {@code System.err} and leaves that country's existing bundled file untouched; the tool then
 * continues with the next country. This is a best-effort maintainer convenience, typically run
 * once before cutting a release, not a CI gate: the JVM exit code stays {@code 0} even when some
 * countries fail, the final summary makes any failures visible instead.
 * <p>
 * Must be run with the module directory ({@code iban-commons-bankdata/}) as the actual process
 * working directory, since it writes to {@code src/main/resources/bankdata/{CC}.csv} using a path
 * relative to the current working directory. Invoking Maven with {@code -pl iban-commons-bankdata}
 * from the repository root does <strong>not</strong> change the working directory (the
 * {@code exec-maven-plugin} {@code java} goal runs in-process and inherits whatever directory
 * {@code mvn} itself was started from), so {@code cd} into the module directory first. If the
 * expected {@code src/main/resources/bankdata} directory is not found relative to the working
 * directory, this tool aborts immediately without writing anything rather than silently creating a
 * stray directory tree in the wrong place. Invocation:
 * <pre>
 * cd iban-commons-bankdata
 * mvn test-compile exec:java@refresh-bundled-bankdata
 * </pre>
 * With no arguments, every supported country is refreshed. Optionally, one or more ISO 3166-1
 * alpha-2 country codes can be passed via {@code -Dexec.args} to refresh only those countries, e.g.
 * to re-verify a single country without waiting on the others:
 * <pre>
 * mvn test-compile exec:java@refresh-bundled-bankdata -Dexec.args="CZ BE"
 * </pre>
 * Passing {@code -h} or {@code --help} prints usage instead of refreshing anything.
 *
 * @since 1.8.11
 */
public final class BundledDataRefreshTool {

    private static final Path     BUNDLED_RESOURCE_DIR = Paths.get("src", "main", "resources", "bankdata");
    private static final Duration CONNECT_TIMEOUT      = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT         = Duration.ofSeconds(60);

    private BundledDataRefreshTool() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    public static void main(String[] args) {
        if (isHelpRequested(args)) {
            printUsage(System.out);
            return;
        }

        if (!Files.isDirectory(BUNDLED_RESOURCE_DIR)) {
            System.err.printf("Expected directory %s does not exist.%n"
                + "This tool must be run with the iban-commons-bankdata module directory as the working "
                + "directory (mvn -pl iban-commons-bankdata test-compile exec:java@refresh-bundled-bankdata), %n"
                + "not the repository root or any other directory. %n"
                + "Aborting without writing anything.%n",
                BUNDLED_RESOURCE_DIR.toAbsolutePath());
            return;
        }

        List<String> countryCodes = resolveCountryCodes(args);
        if (countryCodes == null) {
            return;
        }

        Downloader downloader = Downloader.createDefault();
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String countryCode : countryCodes) {
            try {
                RefreshResult result = refreshCountry(countryCode, downloader);
                succeeded.add(countryCode);
                String bicSuffix = result.recordsWithoutBic > 0 ? ", " + result.recordsWithoutBic + " without a BIC" : "";
                System.out.printf("%s: OK - %d records, %d raw bytes downloaded in %d ms%s%n",
                    countryCode, result.recordCount, result.rawByteCount, result.elapsed.toMillis(), bicSuffix);
            } catch (IOException | BankDataParseException | RuntimeException ex) {
                failed.add(countryCode);
                System.err.printf("%s: FAILED - %s: %s%n", countryCode, ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        System.out.printf("%n");
        System.out.printf("Summary: %d succeeded %s, %d failed %s%n", succeeded.size(), succeeded, failed.size(), failed);
    }

    static boolean isHelpRequested(String[] args) {
        for (String arg : args) {
            if ("-h".equals(arg) || "--help".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    static void printUsage(PrintStream out) {
        out.printf("Usage: BundledDataRefreshTool [COUNTRY_CODE...]%n");
        out.printf("%n");
        out.printf("Regenerates the bundled bank data CSV fallback files under%n");
        out.printf("src/main/resources/bankdata from each country's live upstream source.%n");
        out.printf("%n");
        out.printf("With no arguments, refreshes all supported countries: %s%n", new TreeSet<>(BankDataRegistry.getSupportedCountryCodes()));
        out.printf("With one or more ISO 3166-1 alpha-2 country codes, refreshes only those countries.%n");
        out.printf("%n");
        out.printf("Must be run with iban-commons-bankdata/ as the working directory:%n");
        out.printf("  cd iban-commons-bankdata%n");
        out.printf("  mvn test-compile exec:java@refresh-bundled-bankdata%n");
        out.printf("  mvn test-compile exec:java@refresh-bundled-bankdata -Dexec.args=\"CZ BE\"%n");
    }

    /**
     * Resolves the country codes to refresh from the given command-line arguments.
     *
     * @param args the raw command-line arguments (no options besides {@code -h}/{@code --help},
     *             which is handled separately)
     * @return the (uppercased) country codes to refresh, all supported countries if {@code args} is
     *         empty, or {@code null} if an unsupported country code was passed (an error has
     *         already been printed to {@code System.err} in that case)
     */
    static List<String> resolveCountryCodes(String[] args) {
        if (args.length == 0) {
            return new ArrayList<>(new TreeSet<>(BankDataRegistry.getSupportedCountryCodes()));
        }
        List<String> countryCodes = new ArrayList<>();
        for (String arg : args) {
            String countryCode = arg.toUpperCase(Locale.ROOT);
            if (!BankDataRegistry.getSupportedCountryCodes().contains(countryCode)) {
                System.err.printf("Unknown country code: %s. Supported: %s%n", arg, new TreeSet<>(BankDataRegistry.getSupportedCountryCodes()));
                return null;
            }
            countryCodes.add(countryCode);
        }
        return countryCodes;
    }

    @SuppressWarnings("TimeZoneUsage") // sourceVersion is a UTC-based, machine-readable vintage stamp, not a display timestamp
    private static RefreshResult refreshCountry(String countryCode, Downloader downloader) throws IOException, BankDataParseException {
        Optional<CountryBankDataLoader> maybeLoader = BankDataRegistry.getLoader(countryCode);
        if (!maybeLoader.isPresent()) {
            throw new IllegalStateException("No loader registered for country " + countryCode);
        }
        CountryBankDataLoader loader = maybeLoader.get();

        Instant start = Instant.now();
        byte[] rawBytes = downloader.download(loader.getRemoteSourceUri(), CONNECT_TIMEOUT, READ_TIMEOUT);
        String sourceVersion = Instant.now().toString();
        List<BankData> records = loader.parse(new ByteArrayInputStream(rawBytes), sourceVersion);
        Duration elapsed = Duration.between(start, Instant.now());

        Path target = BUNDLED_RESOURCE_DIR.resolve(countryCode + ".csv");
        Path tempFile = Files.createTempFile(target.getParent(), countryCode, ".csv.tmp");
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            BankDataFormat.write(out, records);
        }
        Files.move(tempFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        long recordsWithoutBic = records.stream().filter(r -> r.getBic() == null).count();
        return new RefreshResult(records.size(), rawBytes.length, elapsed, recordsWithoutBic);
    }

    private static final class RefreshResult {

        private final int      recordCount;
        private final int      rawByteCount;
        private final Duration elapsed;
        private final long     recordsWithoutBic;

        RefreshResult(int recordCount, int rawByteCount, Duration elapsed, long recordsWithoutBic) {
            this.recordCount       = recordCount;
            this.rawByteCount      = rawByteCount;
            this.elapsed           = elapsed;
            this.recordsWithoutBic = recordsWithoutBic;
        }

    }

}
