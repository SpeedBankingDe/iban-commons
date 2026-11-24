package de.speedbanking.bankdata.loader;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.spi.CountryBankDataLoader;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;

/**
 * Test fixture: an abstract loader that follows the naming convention, and the base of the other
 * test fixture loaders.
 */
abstract class BankDataLoaderXb implements CountryBankDataLoader {

    @Override
    public String getCountryCode() {
        return "XX";
    }

    @Override
    public URI getRemoteSourceUri() {
        return URI.create("https://example.invalid/");
    }

    @Override
    public Charset getRemoteSourceCharset() {
        return Charset.defaultCharset();
    }

    @Override
    public List<BankData> parse(InputStream rawSource, String sourceVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Duration getRecommendedRefreshThreshold() {
        return Duration.ofDays(1);
    }

}
