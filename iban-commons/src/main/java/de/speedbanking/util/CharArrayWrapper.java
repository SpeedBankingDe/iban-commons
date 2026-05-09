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

/**
 * A light-weight, immutable {@link CharSequence} wrapper for a {@code char} array.
 * <p>
 * This class avoids defensive copying to provide maximum performance in read-only scenarios.
 * It is particularly useful for avoiding {@code String} or {@code char[]} allocations
 * during validation or parsing logic.
 *
 * @since 1.8.5
 */
public final class CharArrayWrapper implements CharSequence {

    private final char[] data;
    private final int    offset;
    private final int    length;

    /**
     * Constructs a wrapper for the entire provided array.
     *
     * @param data the array to wrap; must not be {@code null}
     * @throws NullPointerException if {@code data} is {@code null}
     */
    public CharArrayWrapper(final char[] data) {
        this(data, 0, requireNonNull(data, "Data array must not be null").length);
    }

    /**
     * Constructs a wrapper for a specific range of the provided array.
     *
     * @param data   the array to wrap; must not be {@code null}
     * @param offset the start index (inclusive)
     * @param length the number of characters to include
     * @throws NullPointerException      if {@code data} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset} or {@code length} are invalid
     */
    public CharArrayWrapper(final char[] data, final int offset, final int length) {
        requireNonNull(data, "Data array must not be null");
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new IndexOutOfBoundsException(String.format(
                "Invalid range (offset: %d, length: %d) for array length %d", offset, length, data.length));
        }
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(final int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        return data[offset + index];
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        if (start < 0 || end < start || end > length) {
            throw new IndexOutOfBoundsException("Invalid subSequence range: [" + start + ", " + end + ']');
        } else if (start == 0 && end == length) {
            return this;
        }
        return new CharArrayWrapper(data, offset + start, end - start);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof CharSequence)) {
            return false;
        }
        CharSequence other = (CharSequence) o;
        if (this.length() != other.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (this.charAt(i) != other.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0; // compatible with String.hashCode
        for (int i = 0; i < length; i++) {
            result = 31 * result + data[offset + i];
        }
        return result;
    }

    /**
     * Returns a string representation of this sequence.
     * <p>
     * This method creates a new {@link String} object.
     *
     * @return the string containing the characters in this sequence
     */
    @Override
    public String toString() {
        return new String(data, offset, length);
    }

}
