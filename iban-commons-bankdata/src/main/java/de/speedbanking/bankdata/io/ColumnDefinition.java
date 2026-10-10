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

/**
 * Interface to be implemented by the column enums of a delimited row format, such as a loader's
 * raw source or this module's own bank data format.
 *
 * @since 1.8.12
 */
public interface ColumnDefinition {

    /**
     * Gets the zero-based index of this column.
     *
     * @return column index
     */
    int getIndex();

    /**
     * Gets the enum constant name.
     *
     * @return column name
     */
    String name();

    /**
     * Returns whether this column may be missing at the end of a row. A row must contain every
     * column up to the last non-optional one, see {@link Columns#getMinColumnCount()}.
     *
     * @return {@code true} if the column is optional, {@code false} by default
     */
    default boolean isOptional() {
        return false;
    }

    /**
     * Returns the enum constant name in camel case, e.g. {@code bankName} for {@code BANK_NAME}.
     *
     * @return the camel case name
     */
    default String getCamelCaseName() {
        String name = name();
        StringBuilder sb = new StringBuilder(name.length());
        boolean upperNext = false;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '_') {
                upperNext = sb.length() > 0;
            } else {
                sb.append(upperNext ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                upperNext = false;
            }
        }
        return sb.toString();
    }

}
