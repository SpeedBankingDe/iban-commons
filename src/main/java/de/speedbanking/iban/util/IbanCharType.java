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
package de.speedbanking.iban.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines the character types used in the **ISO 13616 IBAN structure notation**
 * for defining the Basic Bank Account Number (BBAN) pattern.
 * <p>
 * Each constant maps a single-character ISO code (e.g., 'n', 'a', 'c') to its corresponding
 * Java regular expression character set.
 */
public enum IbanCharType {

    /**
     * Alphabetic characters (A-Z).
     * <p>
     * Corresponds to the ISO 13616 code {@code 'a'}.
     */
    ALPHABETIC('a', "[A-Z]"),

    /**
     * Numeric characters (0-9).
     * <p>
     * Corresponds to the ISO 13616 code {@code 'n'}.
     */
    NUMERIC('n', "\\d"),

    /**
     * Alphanumeric characters (A-Z, 0-9).
     * <p>
     * Corresponds to the ISO 13616 code {@code 'c'}.
     */
    ALPHANUMERIC('c', "[A-Z0-9]"),

    /**
     * Blank space (used only in the display format notation).
     * <p>
     * Corresponds to the ISO 13616 code {@code 'e'}.
     */
    SPACE('e', "[ ]");

    private final char   ibanCode;
    private final String regexPattern;

    IbanCharType(char ibanCode, String regexPattern) {
        this.ibanCode = ibanCode;
        this.regexPattern = regexPattern;
    }

    /**
     * Returns the single-character code used in the ISO 13616 IBAN structure notation.
     *
     * @return The character code (e.g., {@code 'a'}, {@code 'n'}, or {@code 'c'}).
     */
    public char getIbanCode() {
        return ibanCode;
    }

    /**
     * Returns the Java Regular Expression character set matching this character type.
     *
     * @return The regular expression pattern string (e.g., {@code "[A-Z]"}).
     */
    public String getRegexPattern() {
        return regexPattern;
    }

    /**
     * Finds the {@link IbanCharType} by its single-character IBAN code.
     *
     * @param ibanCode The character code ({@code 'a'}, {@code 'n'}, {@code 'c'}, or {@code 'e'}).
     * @return The matching {@code IbanCharType}, or {@code null} if not found.
     */
    public static IbanCharType fromIbanCode(char ibanCode) {
        for (IbanCharType type : values()) {
            if (type.ibanCode == ibanCode) {
                return type;
            }
        }
        return null;
    }

    /**
     * Returns a list of all defined single-character IBAN codes.
     *
     * @return A list of {@code Character} objects representing the codes.
     */
    public static List<Character> getIbanCodes() {
        return Arrays.stream(values())
            .map(IbanCharType::getIbanCode)
            .map(Character::valueOf)
            .collect(Collectors.toList());
    }

}
