package de.speedbanking.iban.util;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.iban.util.IbanPatternConverter.Segment;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Fuzz test for {@link IbanPatternConverter}.
 * <p>
 * Input length is capped at 32 characters: {@link IbanPatternConverter#parseSegments} caches
 * every distinct pattern notation string it is given, forever, by design (it is meant only for
 * the library's own small, fixed set of country patterns). An unbounded live-fuzzing run
 * generates millions of distinct strings; capping the length keeps the resulting cache growth
 * within a reasonable memory bound for a single fuzzing session.
 * <p>
 * Excluded from the default {@code mvn test} run (see the {@code run-fuzz-tests} profile in the
 * root pom); run explicitly as a fast regression test (against
 * {@code src/test/resources/.../inputs}):
 * <pre>{@code
 * mvn -pl iban-commons test -Drun-fuzz-tests -Dtest=IbanPatternConverterFuzzTest
 * }</pre>
 * To fuzz live instead, set {@code JAZZER_FUZZ=1} (an environment variable, not a system
 * property: Surefire does not reliably forward {@code -Djazzer.fuzz=true} to the forked test
 * JVM), e.g.:
 * <pre>{@code
 * JAZZER_FUZZ=1 mvn -pl iban-commons test -Drun-fuzz-tests -Dtest=IbanPatternConverterFuzzTest
 * }</pre>
 */
@SuppressWarnings("checkstyle:MethodName")
final class IbanPatternConverterFuzzTest {

    private static final int MAX_PATTERN_LENGTH = 32;

    /**
     * {@link IbanPatternConverter#convertToRegex} documents exactly one exception for malformed
     * input: {@link IllegalArgumentException}. No pattern notation string should ever produce
     * any other exception, and a successfully produced regex string must always be a compilable
     * Java regular expression.
     * <p>
     * Separately, {@link IbanPatternConverter#aggregateSegments} merges consecutive same-type
     * segments by summing their {@code int} lengths ({@code Segment#addLength}), with no
     * explicit overflow check; it relies on {@code Segment}'s constructor rejecting a length
     * {@code < 1} immediately after each individual addition. Two's-complement arithmetic makes
     * that sufficient for exactly two terms (the largest possible sum of two positive ints,
     * {@code 2 * (Integer.MAX_VALUE)}, still wraps negative rather than back around to a
     * smaller-but-valid positive value) - but the segment-length total before and after
     * aggregation is still asserted to always agree, as a general safety net against this
     * invariant ever breaking under a future change (e.g. summing more than two terms without
     * validating each intermediate step).
     *
     * @param data fuzzer-supplied byte stream, consumed as the pattern notation string
     */
    @FuzzTest
    void convertToRegex_neverThrowsUndocumentedException(FuzzedDataProvider data) {
        String pattern = data.consumeString(MAX_PATTERN_LENGTH);

        try {
            List<Segment> segments = IbanPatternConverter.parseSegments(pattern);
            String regex = IbanPatternConverter.convertToRegex(pattern);

            assertThat(regex).isNotNull();
            Pattern.compile(regex);

            List<Segment> aggregated = IbanPatternConverter.aggregateSegments(segments);
            assertThat(IbanPatternConverter.calculateTotalLength(aggregated))
                .isEqualTo(IbanPatternConverter.calculateTotalLength(segments));
        } catch (IllegalArgumentException expected) {
            // documented outcome for malformed pattern notation; anything else fails the fuzz test
        }
    }

}
