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

import java.util.Objects;

/**
 * Represents an immutable range defined by a begin index (inclusive)
 * and an ending index (exclusive).
 * <p>
 * This class models a **half-open interval** {@code [begin, end)}, which is consistent with
 * indexing conventions used in Java (e.g., {@link String#substring(int, int)}) and
 * is ideally suited for defining segments within a string (such as BBAN parts in an IBAN).<br>
 * It implements {@link Comparable} to allow sorting by start position, then end position.
 *
 * @since 1.8.0
 */
public final class IndexRange implements java.io.Serializable, Comparable<IndexRange> {

    /** Serial version UID. */
    private static final long serialVersionUID = 42L;

    /** The begin index of the range (inclusive). */
    private final int         begin;

    /** The ending index of the range (exclusive). */
    private final int         end;

    /**
     * Private constructor.
     * @param begin the begin index (inclusive)
     * @param end   the ending index (exclusive)
     *
     * @throws IllegalArgumentException if end is less than begin or indices are negative
     */
    private IndexRange(final int begin, final int end) {
        if (begin < 0) {
            throw new IllegalArgumentException("Index 'begin' (" + begin + ") must be non-negative");
        } else if (end < begin) {
            throw new IllegalArgumentException(
                "Index 'end' (" + end + ") must be greater than or equal to 'begin' (" + begin + ")");
        }

        this.begin = begin;
        this.end = end;
    }

    /**
     * Static factory method to create an {@code IndexRange} instance.
     *
     * @param begin the begin index (inclusive)
     * @param end   the ending index (exclusive)
     * @return a new {@code IndexRange} instance
     *
     * @throws IllegalArgumentException if end is less than begin or indices are negative
     */
    public static IndexRange of(int begin, int end) {
        return new IndexRange(begin, end);
    }

    /**
     * Returns the begin index of the range (inclusive).
     * <p>
     * This is the index of the first character included in the range.
     *
     * @return the begin index
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
     * @return the ending index
     */
    public int getEnd() {
        return end;
    }

    /**
     * Returns the size (length) of the range.
     *
     * @return the number of characters in the range ({@code end - start()})
     */
    public int length() {
        return end - begin;
    }

    /**
     * Extracts the substring defined by this range from the given character sequence.
     *
     * @param sequence the character sequence (e.g., a normalized IBAN string)
     * @return the substring covered by this range
     * @throws NullPointerException if the sequence is null
     * @throws IndexOutOfBoundsException if the sequence is shorter than {@code end}
     */
    public String applyTo(final CharSequence sequence) {
        return sequence.subSequence(begin, end).toString();
    }

    /**
     * Extracts the portion of the given character array that corresponds to this index range.
     *
     * @param sequence the character array to extract from
     * @return a new character array containing the extracted range
     * @throws NullPointerException if the sequence is null
     * @throws IndexOutOfBoundsException if the range is outside the sequence bounds
     */
    public char[] applyTo(final char[] sequence) {
        int len = length();
        char[] result = new char[len];
        System.arraycopy(sequence, begin, result, 0, len);
        return result;
    }

    /**
     * Compares this range with another based on the start index, then the end index.
     * <p>
     * This sorting order is useful for processing segments of a string in sequential order.
     *
     * @param other the range to be compared
     * @return a negative integer, zero, or a positive integer as this range
     *         is less than, equal to, or greater than the specified range.
     * @throws NullPointerException if the specified object is null
     * @since 1.8.1
     */
    @Override
    public int compareTo(IndexRange other) {
        requireNonNull(other, "Comparison object must not be null");
        int result = Integer.compare(this.begin, other.begin);
        if (result == 0) {
            result = Integer.compare(this.end, other.end);
        }
        return result;
    }

    /**
     * Compares this range to the specified object. The result is {@code true}
     * if and only if the argument is not {@code null} and is an {@code IndexRange}
     * object that has the same start, and end indices.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IndexRange other = (IndexRange) o;
        return begin == other.begin && end == other.end;
    }

    /**
     * Returns a hash code based on the begin and end fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
    }

    /**
     * Returns a string representation of this range,
     * inclusive start index, inclusive last index (calculated as {@code end - 1}), and total length.<br>
     * Example: {@code IndexRange[Bank Code: 4-7 (4)]}
     *
     * @return a string representation of the range
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
            + '['
            + begin + '-' + (end - 1)
            + " (" + length() + ")]";
    }

}
