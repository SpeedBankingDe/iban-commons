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
package de.speedbanking.bankdata.io;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.StringJoiner;

/**
 * Utility wrapper providing field extraction, bounds checking, and string formatting
 * for a {@link ColumnDefinition} enum type.
 *
 * @param <E> enum type implementing {@link ColumnDefinition}
 * @since 1.8.12
 */
public final class Columns<E extends Enum<E> & ColumnDefinition> {

    private final Class<E> enumClass;
    private final int      minColumnCount;

    /**
     * Creates a column accessor for the given enum type, see {@link #of(Class)}.
     */
    private Columns(Class<E> enumClass) {
        this.enumClass = requireNonNull(enumClass, "enumClass must not be null");
        this.minColumnCount = calculateMinColumnCount(enumClass);
    }

    /**
     * Creates a column accessor for the given {@link ColumnDefinition} enum type.
     *
     * @param <E>       enum type implementing {@link ColumnDefinition}
     * @param enumClass the enum class; must not be {@code null}
     * @return a new column accessor
     */
    public static <E extends Enum<E> & ColumnDefinition> Columns<E> of(Class<E> enumClass) {
        return new Columns<>(enumClass);
    }

    /**
     * Returns the minimum number of fields a row must have: one more than the highest index of a
     * non-optional column (see {@link ColumnDefinition#isOptional()}).
     *
     * @return the minimum column count
     */
    public int getMinColumnCount() {
        return minColumnCount;
    }

    /**
     * Returns the trimmed field of the given column, or an empty string if the row ends before
     * that column or the field is {@code null}.
     *
     * @param column the column
     * @param fields the fields of one row
     * @return the trimmed field, never {@code null}
     */
    public String getOrEmpty(E column, List<String> fields) {
        int idx = column.getIndex();
        return idx < fields.size() ? trimToEmpty(fields.get(idx)) : "";
    }

    /**
     * Returns the trimmed field of the given column, or {@code null} if it is empty or missing.
     *
     * @param column the column
     * @param fields the fields of one row
     * @return the trimmed field, or {@code null}
     */
    public String getOrNull(E column, List<String> fields) {
        String val = getOrEmpty(column, fields);
        return val.isEmpty() ? null : val;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", enumClass.getSimpleName() + "[", "]");
        for (E col : enumClass.getEnumConstants()) {
            joiner.add(col.name() + "(" + col.getIndex() + ")");
        }
        return joiner.toString();
    }

    /**
     * Returns one more than the highest index of a non-optional column of the given enum type.
     */
    private static <E extends Enum<E> & ColumnDefinition> int calculateMinColumnCount(Class<E> enumClass) {
        int max = -1;
        for (E col : enumClass.getEnumConstants()) {
            if (!col.isOptional() && col.getIndex() > max) {
                max = col.getIndex();
            }
        }
        return max + 1;
    }

    /**
     * Returns the trimmed input, or an empty string for {@code null}.
     */
    private static String trimToEmpty(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }

}
