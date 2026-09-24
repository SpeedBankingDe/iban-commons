package de.speedbanking.checkdigit.de;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz test for {@link GermanAccountCheckDigit}.
 * <p>
 * Excluded from the default {@code mvn test} run (see the {@code run-fuzz-tests} profile in the
 * root pom); run explicitly as a fast regression test (against
 * {@code src/test/resources/.../inputs}):
 * <pre>{@code
 * mvn -pl iban-commons-de-checkdigit test -Drun-fuzz-tests \
 *     -Dtest=GermanAccountCheckDigitFuzzTest#verify_knownMethod_neverThrowsUndocumentedException
 * }</pre>
 * To fuzz live instead, set {@code JAZZER_FUZZ=1} (an environment variable, not a system
 * property: Surefire does not reliably forward {@code -Djazzer.fuzz=true} to the forked test
 * JVM), e.g.:
 * <pre>{@code
 * JAZZER_FUZZ=1 mvn -pl iban-commons-de-checkdigit test -Drun-fuzz-tests \
 *     -Dtest=GermanAccountCheckDigitFuzzTest#verify_knownMethod_neverThrowsUndocumentedException
 * }</pre>
 */
@SuppressWarnings("checkstyle:MethodName")
final class GermanAccountCheckDigitFuzzTest {

    /**
     * {@link GermanAccountCheckDigit#verify} documents exactly two exceptions for malformed
     * input: {@link NullPointerException} for a {@code null} argument, {@link IllegalArgumentException}
     * for anything else it rejects (unknown method code, non-numeric or too-long BLZ/account
     * number). No arbitrary combination of arguments should ever produce any other exception.
     *
     * @param data fuzzer-supplied byte stream, sliced into the three input strings
     */
    @FuzzTest
    void verify_neverThrowsUndocumentedException(FuzzedDataProvider data) {
        String methodCode = data.consumeBoolean() ? null : data.consumeString(4);
        String blz = data.consumeBoolean() ? null : data.consumeString(16);
        String accountNumber = data.consumeBoolean() ? null : data.consumeRemainingAsString();

        try {
            GermanAccountCheckDigit.verify(methodCode, blz, accountNumber);
        } catch (NullPointerException | IllegalArgumentException expected) {
            // documented outcomes for null/malformed input; anything else fails the fuzz test
        }
    }

    /**
     * Variant of {@link #verify_neverThrowsUndocumentedException} that always picks a
     * <em>known, implemented</em> method code and purely numeric BLZ/account digits, so the
     * fuzzer actually reaches and exercises the ~100 hand-written, per-method {@code calculate()}
     * algorithms in {@link GermanCheckDigitMethod} (each with its own manual digit-array
     * indexing) on essentially every run, instead of almost always bailing out earlier - either
     * at {@link GermanCheckDigitMethod#fromCode} on an unrecognized code, or at the digit-only
     * check in {@code normalize()} on a non-digit character. For every combination of digits,
     * {@code calculate()} must never throw anything at all (it is only ever invoked with
     * already-normalized, fixed-length digit arrays).
     *
     * @param data fuzzer-supplied byte stream: one int selects the method, the rest is consumed
     *             digit-by-digit into the BLZ and account number
     */
    @FuzzTest
    void verify_knownMethod_neverThrowsUndocumentedException(FuzzedDataProvider data) {
        GermanCheckDigitMethod[] methods = GermanCheckDigitMethod.values();
        String methodCode = methods[data.consumeInt(0, methods.length - 1)].getCode();
        String blz = digitString(data, 8);
        String accountNumber = digitString(data, 10);

        GermanAccountCheckDigit.verify(methodCode, blz, accountNumber);
    }

    private static String digitString(FuzzedDataProvider data, int length) {
        StringBuilder digits = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            digits.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return digits.toString();
    }

}
