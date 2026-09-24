package de.speedbanking.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz test for {@link Mod97}.
 * <p>
 * Excluded from the default {@code mvn test} run (see the {@code run-fuzz-tests} profile in the
 * root pom); run explicitly as a fast regression test (against
 * {@code src/test/resources/.../inputs}):
 * <pre>{@code
 * mvn -pl iban-commons test -Drun-fuzz-tests -Dtest=Mod97FuzzTest
 * }</pre>
 * To fuzz live instead, set {@code JAZZER_FUZZ=1} (an environment variable, not a system
 * property: Surefire does not reliably forward {@code -Djazzer.fuzz=true} to the forked test
 * JVM), e.g.:
 * <pre>{@code
 * JAZZER_FUZZ=1 mvn -pl iban-commons test -Drun-fuzz-tests -Dtest=Mod97FuzzTest
 * }</pre>
 */
@SuppressWarnings("checkstyle:MethodName")
final class Mod97FuzzTest {

    /**
     * {@link Mod97#isValid(char[], int)} documents no exception at all for any combination of
     * arguments (other than a {@code null} array, which is explicitly handled and returns
     * {@code false}) - it is meant to be usable on shared/pooled buffers, where {@code length}
     * legitimately does not equal {@code iban.length}. In particular, a {@code length} larger
     * than the array's actual size must be rejected as invalid input, not read out of bounds
     * (see the regression test {@code isValid_charArrayWithLen_lengthExceedsArraySize_returnsFalse}
     * in {@code Mod97Test} for the concrete case this once threw {@link ArrayIndexOutOfBoundsException}).
     *
     * @param data fuzzer-supplied byte stream: an array, an independently chosen (and
     *             potentially mismatched) length, and a boolean deciding whether to test the
     *             {@code char[]} overload or the null-array short-circuit
     */
    @FuzzTest
    void isValid_charArrayWithLen_neverThrows(FuzzedDataProvider data) {
        char[] iban = data.consumeBoolean() ? null : data.consumeString(64).toCharArray();
        int length = data.consumeInt();

        Mod97.isValid(iban, length);
    }

}
