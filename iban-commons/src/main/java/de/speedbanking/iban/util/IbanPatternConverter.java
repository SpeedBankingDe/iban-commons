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
package de.speedbanking.iban.util;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import de.speedbanking.util.UtilityClasses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to convert the IBAN-style structure notation (as defined in the
 * SWIFT/ISO 13616 standard, e.g., "4!a16!n") into a standard Java regular expression
 * pattern for structural validation.
 */
public final class IbanPatternConverter {

    /**
     * The compiled regular expression used to find segments in the IBAN-style
     * pattern notation, such as "4!a", "12!n", or "5!c".
     * <p>
     * Group 2 is a single wildcard character {@code (.)} to ensure the matcher
     * always finds segments like "4!x" and allows the Java code to validate the type.
     * <ul>
     *   <li>Group 1: Length ({@code \d+})</li>
     *   <li>Group 2: Character Type ({@code .})</li>
     * </ul>
     */
    static final Pattern SEGMENT_PATTERN = Pattern.compile("([1-9][0-9]*)!(.)");

    /**
     * Cache of already-parsed segment lists, keyed by the exact pattern notation string.
     * <p>
     * {@link #parseSegments} is called repeatedly with the same, small, fixed set of pattern
     * strings (the structural patterns defined once per country in {@code IbanRegistry}), for
     * example while building the country regexes and the per-position character types of the
     * IBAN components. Since a given pattern notation always parses to the same result, caching
     * avoids repeating the regex matching and {@code Segments} construction on every call.
     * <p>
     * Mirrors the caching approach of {@link de.speedbanking.util.PatternCache}: an unbounded,
     * thread-safe map. This is safe for the library's own, closed set of country patterns; a
     * caller that repeatedly parses a high-cardinality stream of distinct, externally supplied
     * pattern strings would grow this cache without bound, so this method should not be used as
     * a target for arbitrary, untrusted pattern input in a hot path.
     */
    private static final ConcurrentMap<String, Segments> SEGMENT_CACHE = new ConcurrentHashMap<>();

    /**
     * Cache of already-built regex strings, keyed by the exact pattern notation string.
     * <p>
     * Mirrors {@link #SEGMENT_CACHE}: {@link #convertToRegex(String)} is typically called with
     * the same small, fixed set of pattern strings. Caching avoids rebuilding the same regex
     * string (and the {@code StringBuilder} it takes to build it) on every call.
     */
    private static final ConcurrentMap<String, String> REGEX_CACHE = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private IbanPatternConverter() {
        throw UtilityClasses.cannotInstantiate(getClass());
    }

    /**
     * Parses the IBAN-style structure notation, validates the syntax, and converts
     * the components into {@link Segments}.
     * <p>
     * Results are cached by exact input string (see {@link #SEGMENT_CACHE}); the returned
     * segments are immutable and, for a given input, always the same shared instance.
     *
     * @param patternNotation the IBAN structure pattern string (e.g., "4!a16!c")
     * @return the sequential, validated segments (intermediate format)
     * @throws IllegalArgumentException if the pattern is null, empty, contains illegal
     *         characters, or an unknown character type.
     */
    public static Segments parseSegments(String patternNotation) {
        if (patternNotation == null) {
            throw new IllegalArgumentException("Pattern notation must not be null");
        } else if (patternNotation.isEmpty()) {
            throw new IllegalArgumentException("Pattern notation must not be empty");
        } else if (patternNotation.trim().length() != patternNotation.length()) {
            throw new IllegalArgumentException("Pattern contains illegal leading/trailing whitespace");
        }
        // computeIfAbsent does not cache a mapping if doParseSegments throws, so invalid
        // patterns are simply re-validated (and re-rejected) on every call, never cached.
        return SEGMENT_CACHE.computeIfAbsent(patternNotation, IbanPatternConverter::doParseSegments);
    }

    /**
     * Performs the actual, uncached parsing of an already null/empty/whitespace-checked pattern
     * notation string. Extracted from {@link #parseSegments} so that only this pure computation
     * — not the cheap upfront validation — is memoized in {@link #SEGMENT_CACHE}.
     *
     * @param patternNotation the pre-validated IBAN structure pattern string
     * @return the sequential, validated segments
     * @throws IllegalArgumentException if the pattern contains illegal characters, an unknown
     *         character type, or a length value that is too large
     */
    private static Segments doParseSegments(String patternNotation) {
        // use the pre-compiled pattern to create a matcher for the input
        Matcher matcher = SEGMENT_PATTERN.matcher(patternNotation);
        List<Segment> segments = new ArrayList<>();
        int lastIndex = 0; // track the end index of the last match

        while (matcher.find()) {
            // check for unparsed characters between matches
            if (matcher.start() != lastIndex) {
                String invalidPart = patternNotation.substring(lastIndex, matcher.start());
                throw new IllegalArgumentException(
                    String.format("Pattern contains invalid characters at index %d: '%s'",
                        lastIndex, invalidPart));
            }

            // group 1: length string (e.g., "4")
            // group 2: type character (e.g., "a", "n", "c", "e")
            String lenStr = matcher.group(1);
            char typeCode = matcher.group(2).charAt(0);
            int len;

            try {
                len = Integer.parseInt(lenStr);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                    String.format("Length value too large at index %d: %s", matcher.start(), lenStr), ex);
            }

            IbanCharType charType = IbanCharType.fromIbanCode(typeCode);

            if (charType == null) {
                throw new IllegalArgumentException(
                    String.format("Unknown character type '%s' at index %d, valid types are: %s",
                        typeCode, matcher.start(), IbanCharType.getIbanCodes()));
            }

            // store the validated segment in the intermediate format
            segments.add(Segment.of(charType, len));

            lastIndex = matcher.end(); // update index for next check
        }

        // final check: ensure the entire input string was consumed
        if (lastIndex < patternNotation.length()) {
            String trailing = patternNotation.substring(lastIndex);
            throw new IllegalArgumentException(
                String.format("Pattern contains invalid trailing characters starting at index %d: '%s'",
                    lastIndex, trailing));
        }

        return new Segments(segments);
    }

    /**
     * Converts the IBAN-style pattern notation (e.g., {@code 4!a16!c}) into a standard
     * Java regular expression string.
     *
     * @param patternNotation the IBAN structure pattern string (e.g., "4!a16!c")
     * @return a valid Java regular expression string (e.g., "[A-Z]{4}[A-Z0-9]{16}")
     * @throws IllegalArgumentException if the pattern is null, empty, or contains invalid syntax
     */
    public static String convertToRegex(String patternNotation) {
        // parsing and validation
        Segments segments = parseSegments(patternNotation);

        return REGEX_CACHE.computeIfAbsent(patternNotation, p -> segments.aggregate().toRegex());
    }

    /**
     * Checks whether the provided pattern is valid.
     *
     * @param patternNotation the IBAN structure pattern string (e.g., "4!a16!c")
     * @return {@code true} if the pattern is valid, {@code false} otherwise
     */
    public static boolean isValid(String patternNotation) {
        try {
            // check if the conversion succeeds without throwing an exception
            convertToRegex(patternNotation);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Computes the total length (in characters) by summing the lengths of all segments in the given
     * pattern string (e.g. {@code "4!n4!n12!c"} yields {@code 4 + 4 + 12 = 20}).
     *
     * @param pattern the pattern string, must not be null
     * @return the total length
     */
    public static int calculateTotalLength(String pattern) {
        return parseSegments(pattern).totalLength();
    }

    /**
     * Internal, immutable data class representing a single structural segment
     * of the Basic Bank Account Number (BBAN) pattern.
     * <p>
     * A segment is defined by its required {@link IbanCharType} (e.g., numeric)
     * and its fixed {@code length}. This class is an intermediate structure
     * used during the parsing of the ISO 13616 pattern notation.
     */
    public static final class Segment {

        private final IbanCharType charType;
        private final int          length;

        /**
         * Constructs an immutable segment.
         *
         * @param charType the character type
         * @param length   the fixed length
         */
        public Segment(IbanCharType charType, int length) {
            this.charType = requireNonNull(charType, "charType must not be null");
            if (length < 1) {
                throw new IllegalArgumentException("length must be > 0");
            }
            this.length = length;
        }

        /**
         * Static factory method.
         *
         * @param charType the character type of the segment
         * @param length   the fixed length of the segment
         * @return a new {@code Segment} instance
         */
        public static Segment of(IbanCharType charType, int length) {
            return new Segment(charType, length);
        }

        /**
         * Returns the character type (e.g., {@link IbanCharType#NUMERIC}).
         *
         * @return the character type
         */
        public IbanCharType getCharType() {
            return charType;
        }

        /**
         * Returns whether this segment's character type is {@link IbanCharType#NUMERIC}.
         *
         * @return {@code true} if the segment is numeric
         */
        public boolean isNumeric() {
            return IbanCharType.NUMERIC == charType;
        }

        /**
         * Returns whether this segment's character type is {@link IbanCharType#ALPHABETIC}.
         *
         * @return {@code true} if the segment is alphabetic
         */
        public boolean isAlphabetic() {
            return IbanCharType.ALPHABETIC == charType;
        }

        /**
         * Returns whether this segment's character type is {@link IbanCharType#ALPHANUMERIC}.
         *
         * @return {@code true} if the segment is alphanumeric
         */
        public boolean isAlphanumeric() {
            return IbanCharType.ALPHANUMERIC == charType;
        }

        /**
         * Returns whether this segment's character type is {@link IbanCharType#NUMERIC}
         * or {@link IbanCharType#ALPHANUMERIC}.
         *
         * @return {@code true} if the segment is numeric or alphanumeric
         */
        public boolean isNumericOrAlphanumeric() {
            return IbanCharType.NUMERIC == charType || IbanCharType.ALPHANUMERIC == charType;
        }

        /**
         * Returns the fixed length of the segment.
         *
         * @return the length
         */
        public int getLength() {
            return length;
        }

        /**
         * Returns a new instance with the same character type but with the length
         * increased by the given amount.
         *
         * @param toAdd the length to add to the current segment length
         * @return a new {@code Segment} object with the combined length
         */
        public Segment addLength(int toAdd) {
            if (toAdd <= 0) {
                return this;
            }
            return new Segment(charType, length + toAdd);
        }

        /**
         * Converts this segment back to its IBAN structure notation string.
         *
         * @return the string representation in IBAN notation (e.g., "4!a")
         */
        public String toPatternNotation() {
            return length + "!" + charType.getIbanCode();
        }

        /**
         * Compares this segment to the specified object.
         *
         * @param o the object to compare with
         * @return {@code true} if the objects are the same; {@code false} otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Segment)) {
                return false;
            }
            Segment segment = (Segment) o;
            return length == segment.length
                && charType == segment.charType;
        }

        /**
         * Computes the hash code for this segment.
         *
         * @return the hash code value for this segment
         */
        @Override
        public int hashCode() {
            return Objects.hash(charType, length);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                + '['
                + "charType=" + charType
                + ", length=" + length
                + ']';
        }
    }

    /**
     * Immutable, sequential segments of an IBAN structure pattern, as returned by
     * {@link #parseSegments(String)}.
     *
     * @since 1.8.13
     */
    public static final class Segments implements Iterable<Segment> {

        /** The segments in pattern order, never exposed in modifiable form. */
        private final List<Segment> list;

        /** The combined length of all segments, computed once on construction. */
        private final int           totalLength;

        /**
         * Wraps the given list, which must not be modified afterwards.
         *
         * @param list the list of segments, must not contain null
         */
        Segments(List<Segment> list) {
            this.list = list;
            int length = 0;
            for (Segment segment : list) {
                length += segment.getLength();
            }
            totalLength = length;
        }

        /**
         * Static factory method.
         *
         * @param segments the segments in pattern order, must not contain null
         * @return a new {@code Segments} instance
         * @throws NullPointerException if any segment is null
         */
        public static Segments of(Segment... segments) {
            for (Segment segment : segments) {
                requireNonNull(segment, "segment must not be null");
            }
            return new Segments(new ArrayList<>(Arrays.asList(segments)));
        }

        /**
         * Returns the number of segments.
         *
         * @return the number of segments
         */
        public int size() {
            return list.size();
        }

        /**
         * Returns whether there are no segments.
         *
         * @return {@code true} if there are no segments
         */
        public boolean isEmpty() {
            return list.isEmpty();
        }

        /**
         * Returns the segment at the given index.
         *
         * @param index the index of the segment
         * @return the segment
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        public Segment get(int index) {
            return list.get(index);
        }

        /**
         * Returns the segments as an unmodifiable list.
         *
         * @return the unmodifiable list of segments
         */
        public List<Segment> asList() {
            return unmodifiableList(list);
        }

        /**
         * Returns the combined length of all segments, e.g. 20 for {@code "4!n4!n12!c"}.
         *
         * @return the total length, 0 if there are no segments
         */
        public int totalLength() {
            return totalLength;
        }

        /**
         * Checks whether at least one segment has the given character type.
         *
         * @param charType the character type to look for
         * @return {@code true} if any segment has the given character type
         */
        public boolean containsType(IbanCharType charType) {
            for (Segment segment : list) {
                if (segment.getCharType() == charType) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks whether all segments satisfy the given predicate.
         *
         * @param predicate the condition to evaluate for each segment, must not be null
         * @return {@code true} if all segments match the predicate, also if there are no segments
         * @throws NullPointerException if {@code predicate} is null
         */
        public boolean allMatch(Predicate<Segment> predicate) {
            requireNonNull(predicate, "predicate must not be null");
            for (Segment segment : list) {
                if (!predicate.test(segment)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Merges all consecutive segments of the same character type by adding up their lengths.
         * This is a form of Run-Length Encoding (RLE).
         *
         * @return the merged segments, this instance if there was nothing to merge
         */
        public Segments aggregate() {
            if (list.size() < 2) {
                return this;
            }

            List<Segment> aggregated = new ArrayList<>();
            Segment current = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                Segment next = list.get(i);
                if (next.getCharType() == current.getCharType()) {
                    current = current.addLength(next.getLength());
                } else {
                    aggregated.add(current);
                    current = next;
                }
            }
            aggregated.add(current);

            return aggregated.size() == list.size() ? this : new Segments(aggregated);
        }

        /**
         * Returns the index ranges of all segments of the given character type.
         * <p>
         * Each range is a pair of begin (inclusive) and end (exclusive) index. The indices
         * count from {@code offset}, which is the position of the first segment.
         *
         * @param charType the character type of the segments to return ranges for
         * @param offset   the index of the first segment
         * @return the begin and end index pairs, empty if no segment has the given type
         */
        public int[] ranges(IbanCharType charType, int offset) {
            int[] ranges = new int[list.size() * 2];
            int count = 0;
            int begin = offset;
            for (Segment segment : list) {
                int end = begin + segment.getLength();
                if (segment.getCharType() == charType) {
                    ranges[count++] = begin;
                    ranges[count++] = end;
                }
                begin = end;
            }
            return Arrays.copyOf(ranges, count);
        }

        /**
         * Converts the segments into a Java regular expression, one character class with a
         * fixed-length quantifier per segment.
         *
         * @return the regular expression (e.g. "[A-Z]{4}[A-Z0-9]{16}"), empty if there are no segments
         */
        public String toRegex() {
            StringBuilder regex = new StringBuilder();
            for (Segment segment : list) {
                regex.append(segment.getCharType().getRegexPattern());
                if (segment.getLength() > 1) {
                    regex.append('{').append(segment.getLength()).append('}');
                }
            }
            return regex.toString();
        }

        /**
         * Returns an iterator over the segments in pattern order.
         *
         * @return an iterator that does not support removal
         */
        @Override
        public Iterator<Segment> iterator() {
            return asList().iterator();
        }

        /**
         * Compares these segments to the specified object.
         *
         * @param o the object to compare with
         * @return {@code true} if the object contains equal segments in the same order
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Segments)) {
                return false;
            }
            return list.equals(((Segments) o).list);
        }

        /**
         * Computes the hash code of these segments.
         *
         * @return the hash code value
         */
        @Override
        public int hashCode() {
            return list.hashCode();
        }

        /**
         * Returns a string representation of these segments, e.g.
         * {@code "Segments[Segment[charType=ALPHABETIC, length=4]]"}.
         *
         * @return the class name followed by the list of segments
         */
        @Override
        public String toString() {
            return getClass().getSimpleName() + list;
        }
    }
}
