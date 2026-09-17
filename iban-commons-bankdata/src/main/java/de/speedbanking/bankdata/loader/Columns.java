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
package de.speedbanking.bankdata.loader;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.StringJoiner;

/**
 * Utility wrapper providing field extraction, bounds checking, and string formatting
 * for a {@link ColumnDefinition} enum type.
 *
 * @param <E> enum type implementing {@link ColumnDefinition}
 * @since 1.8.11
 */
public final class Columns<E extends Enum<E> & ColumnDefinition> {

    private final Class<E> enumClass;
    private final int      minColumnCount;

    private Columns(Class<E> enumClass) {
        this.enumClass = requireNonNull(enumClass, "enumClass must not be null");
        this.minColumnCount = calculateMinColumnCount(enumClass);
    }

    public static <E extends Enum<E> & ColumnDefinition> Columns<E> of(Class<E> enumClass) {
        return new Columns<>(enumClass);
    }

    public int getMinColumnCount() {
        return minColumnCount;
    }

    public String getOrEmpty(E column, List<String> fields) {
        int idx = column.getIndex();
        return idx < fields.size() ? trimToEmpty(fields.get(idx)) : "";
    }

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

    private static <E extends Enum<E> & ColumnDefinition> int calculateMinColumnCount(Class<E> enumClass) {
        int max = 0;
        for (E col : enumClass.getEnumConstants()) {
            if (col.getIndex() > max) {
                max = col.getIndex();
            }
        }
        return max + 1;
    }

    private static String trimToEmpty(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }

}
