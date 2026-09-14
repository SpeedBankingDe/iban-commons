package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.speedbanking.iban.Iban;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * End-to-end unit tests for {@link BankDataLookup} against the bundled classpath fallback data
 * (no network access involved).
 * <p>
 * Staleness of the bundled fallback data is based on the bundled classpath resource's actual
 * last-modified timestamp, so a {@code find(...)} call below may or may not also fire a
 * non-blocking background refresh attempt depending on the build's resource timestamps; either
 * way, the downloader factory is swapped for a network-refusing fake for the duration of this test
 * class so that this test never reaches out to the real network, only logs a harmless warning (if
 * a refresh is attempted at all) and keeps the bundled data active.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankDataLookupTest {

    @BeforeAll
    static void disableNetworkRefresh() {
        BankDataRegistry.setDownloaderFactoryForTesting(() -> (uri, connectTimeout, readTimeout) -> {
            throw new IOException("network access disabled in unit tests");
        });
    }

    @AfterAll
    static void restoreDefaultDownloader() {
        BankDataRegistry.resetForTesting();
    }

    @Test
    void find_ibanString_bundledGermanBankCode_resolvesBicAndBankName() throws Exception {
        // DE89370400440532013000 is the SWIFT IBAN Registry's own DE example IBAN; its bank code
        // 37040044 (Commerzbank, Koeln) is a real entry in the live Bundesbank BLZ directory the
        // bundled DE.csv fixture is a downloaded snapshot of.
        Optional<BankData> result = BankDataLookup.find("DE89370400440532013000");

        assertThat(result).isPresent();
        assertThat(result.get().getBankCode()).isEqualTo("37040044");
        assertThat((Object) result.get().getBic()).isNotNull();
        assertThat(result.get().getBic().toString()).isEqualTo("COBADEFFXXX");
        assertThat(result.get().getBankName()).isEqualTo("Commerzbank");
    }

    @Test
    void find_iban_bundledAustrianBankCode_resolves() throws Exception {
        // AT27 2060 1002 3457 3201: bank code 20601 (Sparkasse Bregenz Bank AG) is a real entry in
        // the live OeNB SEPA-Zahlungsverkehrs-Verzeichnis the bundled AT.csv fixture is a
        // downloaded snapshot of; check digits computed for this bank code and an arbitrary account.
        Iban iban = Iban.of("AT272060100234573201");

        Optional<BankData> result = BankDataLookup.find(iban);

        assertThat(result).isPresent();
        assertThat(result.get().getBankCode()).isEqualTo("20601");
        assertThat(result.get().getCity()).isEqualTo("Bregenz");
    }

    @Test
    void findByBankCode_bundledSwissBcNumber_resolves() {
        // BC number 00100 (Schweizerische Nationalbank) is a real entry in the live source the
        // bundled CH.csv fixture is a downloaded snapshot of.
        Optional<BankData> result = BankDataLookup.findByBankCode("CH", "00100");

        assertThat(result).isPresent();
        assertThat(result.get().getBankName()).isEqualTo("Schweizerische Nationalbank");
    }

    @Test
    void find_unsupportedCountry_returnsEmpty() throws Exception {
        Optional<BankData> result = BankDataLookup.find("FR1420041010050500013M02606");

        assertThat(result).isEmpty();
    }

    @Test
    void find_unknownBankCode_returnsEmpty() {
        Optional<BankData> result = BankDataLookup.findByBankCode("DE", "99999999");

        assertThat(result).isEmpty();
    }

    @Test
    void find_invalidIbanString_returnsEmpty() {
        assertThat(BankDataLookup.find("not-an-iban")).isEmpty();
    }

    @Test
    void find_nullIban_returnsEmpty() {
        assertThat(BankDataLookup.find((Iban) null)).isEmpty();
    }

    @Test
    void isCountrySupported_delegatesToRegistry() {
        assertThat(BankDataLookup.isCountrySupported("DE")).isTrue();
        assertThat(BankDataLookup.isCountrySupported("XX")).isFalse();
    }

    @Test
    void supportedCountryCodes_delegatesToRegistry() {
        assertThat(BankDataLookup.getSupportedCountryCodes()).containsExactlyInAnyOrder("DE", "AT", "CH", "CZ", "BE");
    }

    @Test
    void constructor_private_throwsUnsupportedOperationException() throws NoSuchMethodException {
        Constructor<BankDataLookup> constructor = BankDataLookup.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(exception.getCause()).isInstanceOf(UnsupportedOperationException.class);
    }

}
