package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Unit tests for {@link BankDataConfig}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataConfigTest {

    @TempDir
    private Path tempDir;

    // Surefire sets CONFIG_FILE_SYSTEM_PROPERTY globally (see pom.xml) to keep every OTHER test
    // class's cache directory hermetic; save/restore it here rather than clearing it outright, or
    // that hermetic setup would be gone for the rest of this JVM once this test class has run.
    private String originalConfigFileProperty;

    @BeforeEach
    void rememberOriginalConfigFileProperty() {
        originalConfigFileProperty = System.getProperty(BankDataConfig.CONFIG_FILE_SYSTEM_PROPERTY);
    }

    @AfterEach
    void resetGlobalConfig() {
        if (originalConfigFileProperty == null) {
            System.clearProperty(BankDataConfig.CONFIG_FILE_SYSTEM_PROPERTY);
        } else {
            System.setProperty(BankDataConfig.CONFIG_FILE_SYSTEM_PROPERTY, originalConfigFileProperty);
        }
        BankDataConfig.reset();
    }

    private Path writeConfigFile(String... lines) throws IOException {
        Path file = tempDir.resolve("bankdata-override.properties");
        Files.write(file, String.join(System.lineSeparator(), lines).getBytes(UTF_8));
        System.setProperty(BankDataConfig.CONFIG_FILE_SYSTEM_PROPERTY, file.toString());
        return file;
    }

    @Test
    void defaultConfig_hasExpectedDefaults() {
        BankDataConfig config = BankDataConfig.DEFAULT;

        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofDays(90));
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getCacheDirectory()).isNotNull();
        assertThat(config.isNetworkDisabled()).isFalse();
        assertThat(config.getSourceUriOverride("DE")).isEmpty();
    }

    @Test
    void builder_customValues_areApplied() {
        Path customDir = Paths.get("target", "test-bankdata-cache");
        BankDataConfig config = BankDataConfig.builder()
            .cacheDirectory(customDir)
            .staleThreshold(Duration.ofDays(1))
            .connectTimeout(Duration.ofSeconds(2))
            .readTimeout(Duration.ofSeconds(9))
            .disableNetwork(true)
            .build();

        assertThat(config.getCacheDirectory()).isEqualTo(customDir);
        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofDays(1));
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(9));
        assertThat(config.isNetworkDisabled()).isTrue();
    }

    @Test
    void networkDisabled_defaultsToFalseWhenNothingSet() {
        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.isNetworkDisabled()).isFalse();
    }

    @Test
    void configFile_overridesCacheDirDisableNetworkAndTimeouts() throws IOException {
        writeConfigFile(
            "cacheDir = " + tempDir.resolve("cache"),
            "disableNetwork = true",
            "staleThreshold = P30D",
            "connectTimeout = PT2S",
            "readTimeout = PT9S");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getCacheDirectory()).isEqualTo(tempDir.resolve("cache"));
        assertThat(config.isNetworkDisabled()).isTrue();
        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofDays(30));
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(9));
    }

    @Test
    void configFile_explicitBuilderValueOverridesFile() throws IOException {
        writeConfigFile("disableNetwork = true");

        BankDataConfig config = BankDataConfig.builder().disableNetwork(false).build();

        assertThat(config.isNetworkDisabled()).isFalse();
    }

    @Test
    void configFile_loaderUrlOverride_isExposedPerCountry() throws IOException {
        writeConfigFile("loader.DE.url = https://mirror.example.com/blz.csv");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getSourceUriOverride("DE")).contains(URI.create("https://mirror.example.com/blz.csv"));
        assertThat(config.getSourceUriOverride("AT")).isEmpty();
    }

    @Test
    void configFile_invalidLoaderUrl_isIgnored() throws IOException {
        writeConfigFile("loader.DE.url = not a valid uri");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getSourceUriOverride("DE")).isEmpty();
    }

    @Test
    void configFile_loaderUrlKeyWithoutCountryCode_isIgnoredInsteadOfCrashing() throws IOException {
        // "loader.url" matches both the "loader." prefix and the ".url" suffix via their shared
        // '.', but leaves no country code segment between them - must not throw
        // StringIndexOutOfBoundsException and take down the whole config with it
        writeConfigFile(
            "loader.url = https://mirror.example.com/blz.csv",
            "loader.DE.url = https://mirror.example.com/blz-de.csv");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getSourceUriOverride("DE")).contains(URI.create("https://mirror.example.com/blz-de.csv"));
    }

    @Test
    void configFile_invalidDuration_fallsBackToDefault() throws IOException {
        writeConfigFile("staleThreshold = not-a-duration");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofDays(90));
    }

    @Test
    void builder_staleThresholdBelowMinimum_throws() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> BankDataConfig.builder().staleThreshold(Duration.ofHours(5)))
            .withMessageContaining("staleThreshold");
    }

    @Test
    void builder_staleThresholdAtMinimum_isAccepted() {
        BankDataConfig config = BankDataConfig.builder().staleThreshold(Duration.ofHours(6)).build();

        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void configFile_staleThresholdBelowMinimum_fallsBackToMinimumInsteadOfCrashing() throws IOException {
        writeConfigFile("staleThreshold = PT1H");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void builder_connectTimeoutZero_throws() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> BankDataConfig.builder().connectTimeout(Duration.ZERO))
            .withMessageContaining("connectTimeout");
    }

    @Test
    void builder_readTimeoutNegative_throws() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> BankDataConfig.builder().readTimeout(Duration.ofSeconds(-1)))
            .withMessageContaining("readTimeout");
    }

    @Test
    void builder_connectTimeoutBeyondIntMillisRange_throws() {
        // Duration.ofMillis(Integer.MAX_VALUE) + 1ms would silently overflow to a negative int if
        // narrowed via (int) Duration.toMillis(), breaking every download's
        // HttpURLConnection.setConnectTimeout(int) call with an unchecked IllegalArgumentException
        assertThatIllegalArgumentException()
            .isThrownBy(() -> BankDataConfig.builder().connectTimeout(Duration.ofMillis(Integer.MAX_VALUE).plusMillis(1)))
            .withMessageContaining("connectTimeout");
    }

    @Test
    void configFile_readTimeoutZero_fallsBackToDefaultInsteadOfCrashing() throws IOException {
        writeConfigFile("readTimeout = PT0S");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void configFile_connectTimeoutBeyondIntMillisRange_fallsBackToDefaultInsteadOfCrashing() throws IOException {
        writeConfigFile("connectTimeout = PT" + (Integer.MAX_VALUE / 1000 + 1) + "S");

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void configFile_missingFile_fallsBackSilentlyToDefaults() {
        System.setProperty(BankDataConfig.CONFIG_FILE_SYSTEM_PROPERTY, tempDir.resolve("does-not-exist.properties").toString());

        BankDataConfig config = BankDataConfig.builder().build();

        assertThat(config.isNetworkDisabled()).isFalse();
        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofDays(90));
    }

    @Test
    void builderSourceUriOverride_takesPrecedenceOverConfigFile() throws IOException {
        writeConfigFile("loader.DE.url = https://mirror.example.com/blz.csv");

        BankDataConfig config = BankDataConfig.builder()
            .sourceUriOverride("DE", URI.create("https://builder.example.com/blz.csv"))
            .build();

        assertThat(config.getSourceUriOverride("DE")).contains(URI.create("https://builder.example.com/blz.csv"));
    }

    @Test
    void builderSourceUriOverride_lowercaseCountryCode_isMatchedCaseInsensitively() {
        // a loader's own countryCode field is always the uppercase canonical form, so a
        // lowercase-keyed override must still be found rather than silently never matching
        BankDataConfig config = BankDataConfig.builder()
            .sourceUriOverride("de", URI.create("https://builder.example.com/blz.csv"))
            .build();

        assertThat(config.getSourceUriOverride("DE")).contains(URI.create("https://builder.example.com/blz.csv"));
    }

    @Test
    void configure_afterGet_throws() {
        BankDataConfig.get();

        assertThatIllegalStateException().isThrownBy(() -> BankDataConfig.configure(BankDataConfig.DEFAULT));
    }

    @Test
    void configure_beforeGet_isHonored() {
        BankDataConfig custom = BankDataConfig.builder().staleThreshold(Duration.ofDays(3)).build();

        BankDataConfig.configure(custom);

        assertThat(BankDataConfig.get()).isSameAs(custom);
    }

    @Test
    void stringRepresentation_containsFieldValues() {
        assertThat(BankDataConfig.DEFAULT.toString()).contains("staleThreshold").contains("cacheDirectory").contains("sourceUriOverrides");
    }

}
