/*
 * Copyright © 2025 Markus Spann, SpeedBankingDe
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

import java.util.Objects;

/**
 * Represents an immutable, named range defined by a begin index (inclusive) and an
 * ending index (exclusive).
 * <p>
 * This class models a **half-open interval** {@code [begin, end)}, which is consistent with
 * indexing conventions used in Java (e.g., {@link String#substring(int, int)}) and
 * is ideally suited for defining segments within a string (such as BBAN parts in an IBAN).
 *
 * @since 1.8.0
 */
public final class IndexRange implements java.io.Serializable {

    /** Serial version UID. */
    private static final long serialVersionUID = 42L;

    /** The descriptive name for the range (e.g., "Bank Code"). Can be null. */
    private final String      name;

    /** The begin index of the range (inclusive). */
    private final int         begin;

    /** The ending index of the range (exclusive). */
    private final int         end;

    /**
     * Private constructor.
     *
     * @param name  The descriptive name for the range (e.g., "Bank Code"). May be {@code null}.
     * @param begin The begin index (inclusive).
     * @param end   The ending index (exclusive).
     *
     * @throws IllegalArgumentException if end is less than begin or indices are negative.
     */
    private IndexRange(final String name, final int begin, final int end) {
        if (begin < 0) {
            throw new IllegalArgumentException("Index 'begin' (" + begin + ") must be non-negative");
        } else if (end < begin) {
            throw new IllegalArgumentException(
                "Index 'end' (" + end + ") must be greater than or equal to 'begin' (" + begin + ")");
        }

        this.name = name;
        this.begin = begin;
        this.end = end;
    }

    /**
     * Static factory method to create an {@code IndexRange} instance with a descriptive name.
     *
     * @param name  The descriptive name for the range (e.g., "Bank Code").
     * @param begin The begin index (inclusive).
     * @param end   The ending index (exclusive).
     * @return A new named {@code IndexRange} instance.
     *
     * @throws IllegalArgumentException if end is less than begin or indices are negative.
     */
    public static IndexRange of(String name, int begin, int end) {
        return new IndexRange(name, begin, end);
    }

    /**
     * Static factory method to create an unnamed {@code IndexRange} instance.
     *
     * @param begin The begin index (inclusive).
     * @param end   The ending index (exclusive).
     * @return A new unnamed {@code IndexRange} instance.
     *
     * @throws IllegalArgumentException if end is less than begin or indices are negative.
     */
    public static IndexRange of(int begin, int end) {
        return new IndexRange(null, begin, end);
    }

    /**
     * Returns the descriptive name of the range.
     *
     * @return The name, or {@code null} if no name was provided.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the begin index of the range (inclusive).
     * <p>
     * This is the index of the first character included in the range.
     *
     * @return The begin index.
     */
    public int getBegin() {
        return begin;
    }

    /**
     * Returns the ending index of the range (exclusive).
     * <p>
     * This is the index *after* the last character included in the range, following
     * the convention of {@link String#substring(int, int)}.
     *
     * @return The ending index.
     */
    public int getEnd() {
        return end;
    }

    /**
     * Returns the size (length) of the range.
     *
     * @return The number of characters in the range ({@code end - start()}).
     */
    public int length() {
        return end - begin;
    }

    /**
     * Extracts the substring defined by this range from the given character sequence.
     *
     * @param sequence The character sequence (e.g., a normalized IBAN string).
     * @return The substring covered by this range.
     * @throws IndexOutOfBoundsException if the sequence is shorter than {@code end}.
     */
    public CharSequence applyTo(final CharSequence sequence) {
        return sequence.subSequence(begin, end);
    }

    /**
     * Extracts the portion of the given character array that corresponds to this index range.
     * <p>
     * This is equivalent to {@code new String(sequence, start(), length())}.
     *
     * @param sequence The character array to extract from.
     * @return The extracted substring.
     * @throws NullPointerException      if the sequence is {@code null}.
     * @throws IndexOutOfBoundsException if the range is outside the bounds of the sequence.
     */
    public String applyTo(final char[] sequence) {
        int count = end - begin;
        return new String(sequence, begin, count);
    }

    /**
     * Compares this range to the specified object. The result is {@code true}
     * if and only if the argument is not {@code null} and is an {@code IndexRange}
     * object that has the same name, start, and end indices.
     *
     * @param o The object to compare with.
     * @return {@code true} if the objects are the same; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IndexRange other = (IndexRange) o;
        return begin == other.begin
            && end == other.end
            && Objects.equals(name, other.name);
    }

    /**
     * Returns a hash code based on the begin, end, and name fields.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(begin, end, name);
    }

    /**
     * Returns a string representation of this range, including its optional name,
     * inclusive start index, inclusive last index (calculated as {@code end - 1}), and total length.<br>
     * Example: {@code IndexRange[Bank Code: 4-7 (4)]} or {@code IndexRange[4-7 (4)]}
     *
     * @return A string representation of the range.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
            + '['
            + (name == null ? "" : name + ": ")
            + begin + '-' + (end - 1)
            + " (" + length() + ')'
            + ']';
    }

}
