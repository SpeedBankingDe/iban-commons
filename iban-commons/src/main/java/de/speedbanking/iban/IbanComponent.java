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
package de.speedbanking.iban;

import static java.util.Objects.requireNonNull;

import de.speedbanking.iban.IbanRegistry.StructureData;
import de.speedbanking.iban.util.IbanCharType;
import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;

import java.util.List;

/**
 * Immutable pairing of a component's type, character pattern and position within the IBAN.
 * <p>
 * Package-private: used internally by {@link StructureData} / {@link StructureData.Builder} and by other
 * classes in this package (e.g. {@link IbanBuilder}) that need a component's pattern and position together.
 *
 * @since 1.8.9
 */
final class IbanComponent {
    private final IbanComponentType type;
    private final String            patternStr;
    private final int               beginIndex;
    private final int               length;

    /** Character types per position, derived lazily from the pattern, see {@link #getPositionTypes()}. */
    private PositionTypes positionTypes;

    IbanComponent(IbanComponentType type, String patternStr, int beginIndex, int length) {
        this.type = requireNonNull(type, "type required");
        this.patternStr = requireNonNull(patternStr, "patternStr required");
        if (beginIndex < 0) {
            throw new IllegalArgumentException("beginIndex must be >= 0, was " + beginIndex);
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0, was " + length);
        }
        this.beginIndex = beginIndex;
        this.length = length;
    }

    /**
     * Creates a component for the country-specific "Account Type" BBAN part
     * (e.g. Brazil's account type character).
     *
     * @param patternStr the character pattern string for this component
     * @param beginIndex the zero-based begin index within the full IBAN
     * @param length     the length of this component in characters
     * @return a new {@code IbanComponent} of type {@link IbanComponentType#ACCOUNT_TYPE}
     */
    static IbanComponent accountType(String patternStr, int beginIndex, int length) {
        return new IbanComponent(IbanComponentType.ACCOUNT_TYPE, patternStr, beginIndex, length);
    }

    /**
     * Creates a component for the country-specific "National Code" BBAN part.
     *
     * @param patternStr the character pattern string for this component
     * @param beginIndex the zero-based begin index within the full IBAN
     * @param length     the length of this component in characters
     * @return a new {@code IbanComponent} of type {@link IbanComponentType#NATIONAL_CODE}
     */
    static IbanComponent nationalCode(String patternStr, int beginIndex, int length) {
        return new IbanComponent(IbanComponentType.NATIONAL_CODE, patternStr, beginIndex, length);
    }

    IbanComponentType getType() {
        return type;
    }

    String getPattern() {
        return patternStr;
    }

    int getBeginIndex() {
        return beginIndex;
    }

    int getEndIndex() {
        return beginIndex + length;
    }

    int getLength() {
        return length;
    }

    /**
     * Returns the character type of each position of this component, parsed once from the
     * pattern notation and cached afterwards.
     * <p>
     * The cache is initialized racily: concurrent first calls may each parse the pattern, but
     * {@link PositionTypes} is immutable and safely published through its final fields.
     *
     * @return the position types, never {@code null}
     * @throws IllegalArgumentException if the pattern is not a valid IBAN structure notation
     */
    PositionTypes getPositionTypes() {
        PositionTypes result = positionTypes;
        if (result == null) {
            result = new PositionTypes(IbanPatternConverter.parseSegments(patternStr));
            positionTypes = result;
        }
        return result;
    }

    /**
     * Extracts this component's substring directly from the given full IBAN string.
     *
     * @param ibanStr the full, normalized IBAN string
     * @return the substring occupied by this component
     */
    String extractFrom(String ibanStr) {
        return ibanStr.substring(beginIndex, getEndIndex());
    }

    /**
     * Extracts this component's characters directly from the given full IBAN character sequence.
     *
     * @param sequence the full, normalized IBAN character sequence
     * @return a new array containing the characters occupied by this component
     */
    char[] extractFrom(final char[] sequence) {
        char[] result = new char[length];
        System.arraycopy(sequence, beginIndex, result, 0, length);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "[type=" + type
            + ", pattern=" + patternStr
            + ", beginIndex=" + beginIndex
            + ", length=" + length + "]";
    }

    /**
     * The character type of each position of a component, flattened from its pattern segments,
     * so that a value can be checked and generated position by position without a regex.
     *
     * @since 1.8.12
     */
    static final class PositionTypes {
        private final IbanCharType[] types;
        private final boolean        zeroPaddable;

        /**
         * Flattens the given segments into one character type per position.
         *
         * @param segments the parsed pattern segments, must not be null
         */
        PositionTypes(List<Segment> segments) {
            types = new IbanCharType[IbanPatternConverter.calculateTotalLength(segments)];
            int pos = 0;
            for (Segment segment : segments) {
                for (int i = 0; i < segment.getLength(); i++) {
                    types[pos++] = segment.getCharType();
                }
            }
            zeroPaddable = IbanPatternConverter.allMatch(segments, Segment::isNumericOrAlphanumeric);
        }

        /**
         * Returns the number of positions.
         *
         * @return the total length of the pattern
         */
        int length() {
            return types.length;
        }

        /**
         * Returns the character type at the given position.
         *
         * @param pos the zero-based position within the component
         * @return the character type, never {@code null}
         */
        IbanCharType get(int pos) {
            return types[pos];
        }

        /**
         * Returns whether a shorter value may be left-padded with {@code '0'}, which is the case
         * if every position accepts digits.
         *
         * @return {@code true} if all positions are numeric or alphanumeric
         */
        boolean isZeroPaddable() {
            return zeroPaddable;
        }
    }

    /**
     * Identifies a structural BBAN component that carries its own pattern and position within the IBAN.
     */
    enum IbanComponentType {
        BBAN("BBAN"),
        BANK_CODE("Bank Code"),
        BRANCH_CODE("Branch Code"),
        ACCOUNT_NUMBER("Account Number"),
        NATIONAL_CHECK_DIGIT("NCD"),
        ACCOUNT_TYPE("Account Type"),
        NATIONAL_CODE("National Code"),
        IDENTIFICATION_NUMBER("Identification Number"),
        ACCOUNT_TYPE_AND_CONTROL("Account Type/Control");

        private final String label;

        IbanComponentType(String label) {
            this.label = label;
        }

        /**
         * Returns the short, human-readable label used for this component type, e.g. in {@link IbanRegistry#toString()}.
         *
         * @return the display label
         */
        String getLabel() {
            return label;
        }
    }

}
