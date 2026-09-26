package de.speedbanking.iban;

/**
 * Package-private access bridge for {@code iban-commons-validation} tests.
 * <p>
 * {@link IbanConfig#reset(IbanConfig)} is intentionally package-private in {@code iban-commons}
 * (test-only tear-down API) and unreachable from another module's tests. This class lives in the
 * same package name so it shares package-private access under classic (non-JPMS) classpath
 * loading, exposing just enough to reset the frozen global config between tests.
 */
public final class IbanConfigTestBridge {

    private IbanConfigTestBridge() {
    }

    /** Resets the global {@link IbanConfig} to the given instance, clearing the frozen flag. */
    public static void reset(IbanConfig config) {
        IbanConfig.reset(config);
    }

    /** Resets the global {@link IbanConfig} to {@link IbanConfig#DEFAULT}. */
    public static void resetToDefault() {
        IbanConfig.reset();
    }

}
