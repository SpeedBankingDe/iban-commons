package de.speedbanking.util;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;

import de.speedbanking.util.PatternCache.CacheKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class PatternCacheTest extends Assertions {

    private PatternCache patternCache;

    @BeforeEach
    void setUp() {
        patternCache = new PatternCache();
    }

    @Test
    void getDefault_returnsNonNullAndSameInstance() {
        PatternCache instance1 = PatternCache.getDefault();
        PatternCache instance2 = PatternCache.getDefault();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "getDefault should always return the same singleton instance");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "[A-Z]{4} | [A-Z]{4}",
        "\\d+     | \\d+",
        "[a-z0-9]+| [a-z0-9]+",
        "^test$   | ^test$"
    })
    void getPattern_sameRegex_returnsSameInstance(String regex1, String regex2) {
        // when
        Pattern pattern1 = patternCache.getPattern(regex1);
        Pattern pattern2 = patternCache.getPattern(regex2);

        // then
        assertSame(pattern1, pattern2, "Same regex should return same Pattern instance");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "[A-Z]+ | [a-z]+",
        "\\d+   | \\w+",
        "test   | test123"
    })
    void getPattern_differentRegex_returnsDifferentInstances(String regex1, String regex2) {
        // when
        Pattern pattern1 = patternCache.getPattern(regex1);
        Pattern pattern2 = patternCache.getPattern(regex2);

        // then
        assertNotSame(pattern1, pattern2, "Different regex should return different Pattern instances");
    }

    @Test
    void getPattern_withoutFlags_compilesCorrectly() {
        // given
        String regex = "[A-Z]{4}";

        // when
        Pattern pattern = patternCache.getPattern(regex);

        // then
        assertNotNull(pattern);
        assertTrue(pattern.matcher("ABCD").matches());
        assertFalse(pattern.matcher("abcd").matches());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "[a-z]+ | " + CASE_INSENSITIVE + " | abcd | true",
        "[a-z]+ | " + CASE_INSENSITIVE + " | ABCD | true",
        "[a-z]+ | 0 | ABCD | false"
    })
    void getPattern_withFlags_compilesCorrectly(String regex, int flags, String input, boolean shouldMatch) {
        // when
        Pattern pattern = patternCache.getPattern(regex, flags);

        // then
        assertEquals(shouldMatch, pattern.matcher(input).matches());
    }

    @Test
    void getPattern_sameRegexWithSameFlags_returnsSameInstance() {
        // given
        String regex = "[a-z]+";
        int flags = CASE_INSENSITIVE;

        // when
        Pattern pattern1 = patternCache.getPattern(regex, flags);
        Pattern pattern2 = patternCache.getPattern(regex, flags);

        // then
        assertSame(pattern1, pattern2);
    }

    @Test
    void getPattern_sameRegexWithDifferentFlags_returnsDifferentInstances() {
        // given
        String regex = "[a-z]+";

        // when
        Pattern pattern1 = patternCache.getPattern(regex, 0);
        Pattern pattern2 = patternCache.getPattern(regex, CASE_INSENSITIVE);

        // then
        assertNotSame(pattern1, pattern2);
    }

    @Test
    void getPattern_withoutFlagsAndWithZeroFlags_returnsSameInstance() {
        // given
        String regex = "[A-Z]+";

        // when
        Pattern pattern1 = patternCache.getPattern(regex);
        Pattern pattern2 = patternCache.getPattern(regex, 0);

        // then
        assertSame(pattern1, pattern2, "getPattern(regex) and getPattern(regex, 0) should return same instance");
    }

    @Test
    void getPattern_multipleFlagsCombination_cachesCorrectly() {
        // given
        String regex = "^test$";
        int flags = CASE_INSENSITIVE | MULTILINE;

        // when
        Pattern pattern1 = patternCache.getPattern(regex, flags);
        Pattern pattern2 = patternCache.getPattern(regex, flags);

        // then
        assertSame(pattern1, pattern2);
        assertEquals(flags, pattern1.flags());
    }

    @ParameterizedTest
    @NullSource
    void getPattern_nullRegex_throwsNullPointerException(String regex) {
        // when/then
        assertThrows(NullPointerException.class, () -> patternCache.getPattern(regex));
    }

    @ParameterizedTest
    @NullSource
    void getPattern_nullRegexWithFlags_throwsNullPointerException(String regex) {
        // when/then
        assertThrows(NullPointerException.class, () -> patternCache.getPattern(regex, CASE_INSENSITIVE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[", "(", "*", "??"})
    void getPattern_invalidRegex_throwsPatternSyntaxException(String invalidRegex) {
        // when/then
        assertThrows(PatternSyntaxException.class, () -> patternCache.getPattern(invalidRegex));
    }

    @Test
    void size_emptyCache_returnsZero() {
        // when/then
        assertEquals(0, patternCache.size());
    }

    @Test
    void size_afterAddingPatterns_returnsCorrectCount() {
        // when
        patternCache.getPattern("[A-Z]+");
        patternCache.getPattern("\\d+");
        patternCache.getPattern("[a-z]+");

        // then
        assertEquals(3, patternCache.size());
    }

    @Test
    void size_samePatternMultipleTimes_countsOnlyOnce() {
        // when
        patternCache.getPattern("[A-Z]+");
        patternCache.getPattern("[A-Z]+");
        patternCache.getPattern("[A-Z]+");

        // then
        assertEquals(1, patternCache.size());
    }

    @Test
    void size_sameRegexDifferentFlags_countsMultiple() {
        // when
        patternCache.getPattern("[a-z]+");
        patternCache.getPattern("[a-z]+", CASE_INSENSITIVE);
        patternCache.getPattern("[a-z]+", MULTILINE);

        // then
        assertEquals(3, patternCache.size(), "Same regex with different flags should count as separate entries");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "[A-Z]+  | true",
        "\\d+    | true",
        "[a-z]{4}| true"
    })
    void contains_existingPattern_returnsTrue(String regex, boolean expected) {
        // given
        patternCache.getPattern(regex);

        // when/then
        assertEquals(expected, patternCache.contains(regex));
    }

    @Test
    void contains_patternCachedWithFlags_withoutFlagsReturnsFalse() {
        // given
        patternCache.getPattern("[a-z]+", CASE_INSENSITIVE);

        // when/then
        assertFalse(patternCache.contains("[a-z]+"), "contains(regex) should return false if pattern was cached with non-zero flags");
    }

    @Test
    void contains_withFlags_returnsTrueOnlyWhenFlagsMatch() {
        // given
        patternCache.getPattern("test", CASE_INSENSITIVE);

        // when/then
        assertTrue(patternCache.contains("test", CASE_INSENSITIVE));
        assertFalse(patternCache.contains("test", MULTILINE));
        assertFalse(patternCache.contains("test", 0));
    }

    @Test
    void contains_nullRegex_returnsFalse() {
        // when/then
        assertFalse(patternCache.contains(null));
        assertFalse(patternCache.contains(null, CASE_INSENSITIVE));
    }

    @Test
    void contains_emptyCache_returnsFalse() {
        // when/then
        assertFalse(patternCache.contains("[A-Z]+"));
        assertFalse(patternCache.contains("[A-Z]+", CASE_INSENSITIVE));
    }

    @Test
    void containsAnyFlags_matchingRegex_returnsTrueRegardlessOfFlags() {
        // given
        patternCache.getPattern("sample", CASE_INSENSITIVE);

        // when/then
        assertTrue(patternCache.containsAnyFlags("sample"));
        assertFalse(patternCache.containsAnyFlags("other"));
    }

    @Test
    void containsAnyFlags_nullRegex_returnsFalse() {
        // when/then
        assertFalse(patternCache.containsAnyFlags(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo|flags:2", "bar|flags:0"})
    void getPattern_potentialKeyCollisionStrings_handledSafely(String regex) {
        // given
        Pattern pattern1 = patternCache.getPattern(regex, 0);
        Pattern pattern2 = patternCache.getPattern("foo", 2);

        // when/then
        assertNotSame(pattern1, pattern2, "Potential collision strings must produce distinct cache entries");
        assertTrue(patternCache.contains(regex, 0));
    }

    @Test
    void threadSafety_concurrentAccess_handlesCorrectly() throws InterruptedException {
        // given
        String regex = "[A-Z]{4}";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        Pattern[] patterns = new Pattern[threadCount];

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> patterns[index] = patternCache.getPattern(regex));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // then
        assertEquals(1, patternCache.size(), "Only one pattern should be cached despite concurrent access");
        for (int i = 1; i < threadCount; i++) {
            assertSame(patterns[0], patterns[i], "All threads should get the same Pattern instance");
        }
    }

    @Test
    void cacheKey_equalsAndHashCodeAndToString() {
        // given
        Object key1 = new CacheKey("abc", CASE_INSENSITIVE);
        Object key1Duplicate = new CacheKey("abc", CASE_INSENSITIVE);
        Object keyDiffRegex = new CacheKey("xyz", CASE_INSENSITIVE);
        Object keyDiffFlags = new CacheKey("abc", 0);

        // then: equals reflexive and null / class checks
        assertEquals(key1, key1);
        assertNotEquals(key1, null);
        assertNotEquals(key1, "some string");

        // then: equals equality / inequality checks
        assertEquals(key1, key1Duplicate);
        assertNotEquals(key1, keyDiffRegex);
        assertNotEquals(key1, keyDiffFlags);

        // then: hashCode check
        assertEquals(key1.hashCode(), key1Duplicate.hashCode());

        // then: toString check
        assertEquals("CacheKey[regex=abc, flags=" + CASE_INSENSITIVE + "]", key1.toString());
    }

    @ParameterizedTest
    @NullSource
    void cacheKey_nullRegex_throwsNullPointerException(String regex) {
        // when/then
        assertThrows(NullPointerException.class, () -> new CacheKey(regex, 0));
    }

}
