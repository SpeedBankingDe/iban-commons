package de.speedbanking.bic;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz test for {@link BicValidator}.
 * <p>
 * Excluded from the default {@code mvn test} run (see the {@code run-fuzz-tests} profile in the
 * root pom); run explicitly as a fast regression test:
 * <pre>{@code
 * mvn -pl iban-commons test -Drun-fuzz-tests -Dtest=BicValidatorFuzzTest
 * }</pre>
 * To fuzz live instead, set {@code JAZZER_FUZZ=1} (an environment variable, not a system
 * property: Surefire does not reliably forward {@code -Djazzer.fuzz=true} to the forked test
 * JVM), e.g.:
 * <pre>{@code
 * JAZZER_FUZZ=1 mvn -pl iban-commons test -Drun-fuzz-tests -Dtest=BicValidatorFuzzTest
 * }</pre>
 */
@SuppressWarnings("checkstyle:MethodName")
final class BicValidatorFuzzTest {

    /**
     * {@link BicValidator#isValid(CharSequence)} and its {@link String}-optimized overload both
     * copy raw input into the same reused thread-local buffer ({@code VALIDATION_BUFFER}) via a
     * manual {@code charAt}/{@code getChars} copy before validating it - the same
     * shared-buffer, index-tracking shape that previously let
     * {@link de.speedbanking.util.Mod97#isValid(char[], int)} throw
     * {@link ArrayIndexOutOfBoundsException} instead of returning {@code false}
     * (see {@code Mod97FuzzTest}). Both overloads document the same never-throws,
     * false-for-anything-invalid contract, so any exception here is a regression.
     *
     * @param data fuzzer-supplied byte stream: an arbitrary string and a boolean deciding
     *             whether to exercise the {@code CharSequence} or the {@code String} overload
     */
    @FuzzTest
    void isValid_neverThrows(FuzzedDataProvider data) {
        String bic = data.consumeString(16);

        if (data.consumeBoolean()) {
            BicValidator.isValid((CharSequence) bic);
        } else {
            BicValidator.isValid(bic);
        }
    }

}
