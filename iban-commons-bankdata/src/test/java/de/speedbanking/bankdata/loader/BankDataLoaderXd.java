package de.speedbanking.bankdata.loader;

/**
 * Test fixture: a loader whose static initializer fails.
 */
final class BankDataLoaderXd extends BankDataLoaderXb {

    private static final int VALUE = fail();

    @Override
    public String getCountryCode() {
        return "X" + VALUE;
    }

    /** Always throws, so that initializing this class fails. */
    private static int fail() {
        throw new IllegalStateException("simulated static initializer failure");
    }

}
