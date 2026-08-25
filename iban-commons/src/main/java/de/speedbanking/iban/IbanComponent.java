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
