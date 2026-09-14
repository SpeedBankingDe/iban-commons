package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Unit tests for {@link BankDataConfig}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataConfigTest {

    @AfterEach
    void resetGlobalConfig() {
        BankDataConfig.reset();
    }

    @Test
    void defaultConfig_hasExpectedDefaults() {
        BankDataConfig config = BankDataConfig.DEFAULT;

        assertThat(config.getStaleThreshold()).isEqualTo(Duration.ofDays(90));
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getCacheDirectory()).isNotNull();
        assertThat(config.isNetworkDisabled()).isFalse();
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
    void networkDisabled_resolvedFromSystemProperty() {
        System.setProperty(BankDataConfig.DISABLE_NETWORK_SYSTEM_PROPERTY, "true");
        try {
            BankDataConfig config = BankDataConfig.builder().build();

            assertThat(config.isNetworkDisabled()).isTrue();
        } finally {
            System.clearProperty(BankDataConfig.DISABLE_NETWORK_SYSTEM_PROPERTY);
        }
    }

    @Test
    void networkDisabled_explicitBuilderValueOverridesSystemProperty() {
        System.setProperty(BankDataConfig.DISABLE_NETWORK_SYSTEM_PROPERTY, "true");
        try {
            BankDataConfig config = BankDataConfig.builder().disableNetwork(false).build();

            assertThat(config.isNetworkDisabled()).isFalse();
        } finally {
            System.clearProperty(BankDataConfig.DISABLE_NETWORK_SYSTEM_PROPERTY);
        }
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
        assertThat(BankDataConfig.DEFAULT.toString()).contains("staleThreshold").contains("cacheDirectory");
    }

}
