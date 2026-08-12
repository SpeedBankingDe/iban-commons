/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.util;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A thread-safe cache for compiled regular expression patterns.
 * <p>
 * This class ensures that each unique regex string and flag combination is compiled only once,
 * and subsequent requests for the same parameters return the cached {@link Pattern} instance.
 * <p>
 * This improves performance when the same patterns are used repeatedly.
 * <p>
 * Example usage:
 * <pre>
 *   PatternCache store = new PatternCache();
 *   Pattern pattern1 = store.getPattern("[A-Z]{4}");
 *   Pattern pattern2 = store.getPattern("[A-Z]{4}"); // returns same instance as pattern1
 *   assert pattern1 == pattern2; // true
 * </pre>
 */
public final class PatternCache {

    private static final int          NO_FLAG          = 0x0;

    private static final PatternCache DEFAULT_INSTANCE = new PatternCache();

    private final ConcurrentMap<CacheKey, Pattern> patternCache;

    /**
     * Internal immutable composite key to prevent string concatenation overhead and key collisions.
     */
    static final class CacheKey {

        private final String regex;
        private final int    flags;

        CacheKey(String regex, int flags) {
            this.regex = requireNonNull(regex, "regex required");
            this.flags = flags;
        }

        String getRegex() {
            return regex;
        }

        int getFlags() {
            return flags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return flags == cacheKey.flags && regex.equals(cacheKey.regex);
        }

        @Override
        public int hashCode() {
            return 31 * regex.hashCode() + flags;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[regex=" + regex + ", flags=" + flags + "]";
        }

    }

    /**
     * Creates a new PatternCache with an empty cache.
     */
    PatternCache() {
        this.patternCache = new ConcurrentHashMap<>();
    }

    /**
     * Returns the default global PatternCache instance.
     *
     * @return the default PatternCache instance
     */
    public static PatternCache getDefault() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Returns a compiled {@link Pattern} for the given regex string without flags.
     *
     * @param regex the regular expression string, must not be {@code null}
     * @return a compiled Pattern object
     * @throws NullPointerException if regex is {@code null}
     * @throws PatternSyntaxException if the regex syntax is invalid
     */
    public Pattern getPattern(String regex) {
        return getPattern(regex, NO_FLAG);
    }

    /**
     * Returns a compiled {@link Pattern} for the given regex string with flags.
     *
     * @param regex the regular expression string, must not be {@code null}
     * @param flags match flags bit mask
     * @return a compiled Pattern object
     * @throws NullPointerException if regex is {@code null}
     * @throws PatternSyntaxException if the regex syntax is invalid
     */
    public Pattern getPattern(String regex, int flags) {
        return patternCache.computeIfAbsent(new CacheKey(regex, flags), key -> Pattern.compile(key.getRegex(), key.getFlags()));
    }

    /**
     * Returns the number of cached patterns.
     *
     * @return the size of the pattern cache
     */
    public int size() {
        return patternCache.size();
    }

    /**
     * Checks if a pattern for the given regex without flags is cached.
     *
     * @param regex the regular expression string to check
     * @return true if cached, false otherwise
     */
    public boolean contains(String regex) {
        return contains(regex, NO_FLAG);
    }

    /**
     * Checks if a pattern for the given regex and flags combination is cached.
     *
     * @param regex the regular expression string to check
     * @param flags match flags bit mask
     * @return true if cached, false otherwise
     */
    public boolean contains(String regex, int flags) {
        return regex != null && patternCache.containsKey(new CacheKey(regex, flags));
    }

    /**
     * Checks if any pattern for the given regex string is cached, regardless of flags.
     *
     * @param regex the regular expression string to check
     * @return true if cached with any flag combination, false otherwise
     */
    public boolean containsAnyFlags(String regex) {
        return regex != null
            && patternCache.keySet().stream().anyMatch(key -> key.getRegex().equals(regex));
    }

}
