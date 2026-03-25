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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
     * Private constructor to prevent instantiation of this utility class.
     */
    private IbanPatternConverter() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", IbanPatternConverter.class.getSimpleName()));
    }

    /**
     * Parses the IBAN-style structure notation, validates the syntax, and converts
     * the components into a list of {@code Segment} records.
     *
     * @param patternNotation the IBAN structure pattern string (e.g., "4!a16!c")
     * @return a sequential list of validated segments (intermediate format)
     * @throws IllegalArgumentException if the pattern is null, empty, contains illegal
     *         characters, or an unknown character type.
     */
    public static List<Segment> parseSegments(String patternNotation) {
        if (patternNotation == null) {
            throw new IllegalArgumentException("Pattern notation must not be null");
        } else if (patternNotation.isEmpty()) {
            throw new IllegalArgumentException("Pattern notation must not be empty");
        } else if (patternNotation.trim().length() != patternNotation.length()) {
            throw new IllegalArgumentException("Pattern contains illegal leading/trailing whitespace");
        }

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
                // Diese Prüfung ist bei korrekter SEGMENT_PATTERN Regex unnötig,
                // aber zur Sicherheit beibehalten
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

        return segments;
    }

    /**
     * Creates a new unmodifiable list by aggregating all subsequent segments of the same character type,
     * adding up their lengths. This is a form of Run-Length Encoding (RLE).
     *
     * @param segments the input list of segments
     * @return a new list with consecutive segments of the same type merged
     */
    @SuppressWarnings("MixedMutabilityReturnType")
    public static List<Segment> aggregateSegments(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Segment> aggregated = new ArrayList<>();

        // start with the first segment as the current segment to be combined
        Segment current = segments.get(0);

        // iterate starting from the second element
        for (int i = 1; i < segments.size(); i++) {
            Segment next = segments.get(i);

            // check if the current segment's type matches the next one
            if (next.getCharType() == current.getCharType()) {
                // If yes: combine lengths
                current = current.addLength(next.getLength());
            } else {
                // If no: the run is broken. Finalize the current combined segment and add it to the result
                aggregated.add(current);
                // start a new run with the 'next' segment
                current = next;
            }
        }

        // add the last accumulated segment after the loop finishes
        aggregated.add(current);

        return Collections.unmodifiableList(aggregated);
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
        List<Segment> segments = aggregateSegments(parseSegments(patternNotation));

        return buildRegex(segments);
    }

    /**
     * Constructs the final Java Regex string from a sequential list of validated segments.
     * This method handles the concatenation of character sets and the application of quantifiers.
     *
     * @param segments the list of validated segments (intermediate format)
     * @return the resulting Java regular expression string (e.g., "[A-Z]{4}[A-Z0-9]{16}")
     */
    public static String buildRegex(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return null;
        }

        StringBuilder regexBuilder = new StringBuilder();

        for (Segment segment : segments) {
            // append the character set (e.g., "[A-Z0-9]")
            regexBuilder.append(segment.getCharType().getRegexPattern());

            // append the fixed-length quantifier (e.g., "{16}")
            if (segment.getLength() > 1) {
                regexBuilder.append('{').append(segment.getLength()).append('}');
            }
        }

        return regexBuilder.toString();
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
         * Private constructor for the immutable segment.
         *
         * @param charType the character type.
         * @param length   the fixed length.
         */
        public Segment(IbanCharType charType, int length) {
            this.charType = charType;
            this.length = length;
        }

        /**
         * Static factory method.
         *
         * @param charType the character type of the segment.
         * @param length   the fixed length of the segment.
         * @return a new {@code Segment} instance.
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
         * @param toAdd the length to add to the current segment length.
         * @return a new {@code Segment} object with the combined length.
         */
        public Segment addLength(int toAdd) {
            if (toAdd <= 0) {
                return this;
            }
            return new Segment(charType, length + toAdd);
        }

        /**
         * Compares this segment to the specified object.
         *
         * @param o the object to compare with.
         * @return {@code true} if the objects are the same; {@code false} otherwise.
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
         * Returns a hash code for this segment.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(charType, length);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '[' + "charType=" + charType + ", length=" + length + ']';
        }
    }

}
