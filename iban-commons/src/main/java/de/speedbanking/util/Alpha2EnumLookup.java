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

import java.lang.reflect.Array;
import java.util.function.Function;

/**
 * A high-performance, allocation-free lookup registry for enums mapped by a two-letter uppercase code.
 *
 * @param <E> the enum type
 *
 * @since 1.8.8
 */
public final class Alpha2EnumLookup<E extends Enum<E>> {

    /**
     * The required exact length of the character sequence code.
     */
    private static final int CODE_LEN      = 2;

    /**
     * The number of letters in the English alphabet (A–Z), used as the radix
     * for flat array indexing.
     */
    private static final int ALPHABET_SIZE = 26;

    /**
     * Internal flat array containing the enum constants mapped by packed index.
     */
    private final E[]        lookupTable;

    /**
     * Constructs a new lookup registry for the specified enum class.
     * <p>
     * Initializes the internal flat lookup table using reflection to scan all available
     * enum constants at instantiation time.
     *
     * @param enumClass the class object of the enum type to map
     * @param codeExtractor a function to extract the two-letter uppercase code from an enum constant
     */
    @SuppressWarnings("unchecked")
    public Alpha2EnumLookup(Class<E> enumClass, Function<E, CharSequence> codeExtractor) {
        this.lookupTable = (E[]) Array.newInstance(enumClass, ALPHABET_SIZE * ALPHABET_SIZE);

        for (E constant : enumClass.getEnumConstants()) {
            CharSequence code = codeExtractor.apply(constant);
            if (code == null || code.length() != CODE_LEN) {
                throw new IllegalArgumentException(
                    "Code for enum constant " + enumClass.getSimpleName() + "." + constant.name()
                    + " must be exactly " + CODE_LEN + " characters long, but was: " + code);
            }

            char c1 = code.charAt(0);
            char c2 = code.charAt(1);
            if (isNotUpperCase(c1) || isNotUpperCase(c2)) {
                throw new IllegalArgumentException(
                    "Code for enum constant " + enumClass.getSimpleName() + "." + constant.name()
                    + " must contain only uppercase ASCII letters, but was: " + code);
            }

            lookupTable[packToIndex(c1, c2)] = constant;
        }
    }

    /**
     * Computes a unique, flat array index between 0 and 675 for two characters from A-Z.
     *
     * @param c1 the first uppercase character
     * @param c2 the second uppercase character
     * @return the computed flat array index
     */
    private static int packToIndex(char c1, char c2) {
        return (c1 - 'A') * ALPHABET_SIZE + (c2 - 'A');
    }

    /**
     * Looks up the enum constant for the given two-letter uppercase code.
     * <p>
     * This method is entirely allocation-free and operates in O(1) constant time.
     *
     * @param code the two-letter uppercase code to look up; may be any {@link CharSequence}
     * @return the matching enum constant, or {@code null} if the code is null,
     * does not match the required length, contains non-uppercase letters, or is unassigned
     */
    public E fromCode(CharSequence code) {
        if (code == null || code.length() != CODE_LEN) {
            return null;
        }
        char c1 = code.charAt(0);
        char c2 = code.charAt(1);
        if (isNotUpperCase(c1) || isNotUpperCase(c2)) {
            return null;
        }
        return lookupTable[packToIndex(c1, c2)];
    }

    /**
     * Checks if a character is NOT an uppercase ASCII letter ('A'-'Z').
     *
     * @param c the character to check
     * @return {@code true} if the character is not an uppercase letter, {@code false} otherwise
     */
    static boolean isNotUpperCase(final char c) {
        return c < 'A' || c > 'Z';
    }

    /**
     * High-performance check whether an enum constant is assigned to the given character combination.
     * <p>
     * This method is entirely allocation-free and avoids any string or wrapper object generation.
     *
     * @param c1 the first character to check
     * @param c2 the second character to check
     * @return {@code true} if both characters are uppercase and map to an existing constant;
     * {@code false} otherwise
     */
    public boolean isAssigned(char c1, char c2) {
        return CharUtil.isUpperCase(c1)
            && CharUtil.isUpperCase(c2)
            && null != lookupTable[packToIndex(c1, c2)];
    }

}

