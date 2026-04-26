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

import de.speedbanking.util.IndexRange;
import de.speedbanking.util.Mod97;

import java.util.stream.IntStream;

/**
 * Container class for all country-specific {@link NationalCheckDigitCalculator}
 * implementations.
 *
 * <p>Contains all National Check Digit (NCD) algorithms used in IBAN structures.
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li><strong>Base countries</strong> extend {@link NcdCalculatorBase} directly and
 *       implement the full algorithm</li>
 *   <li><strong>Derived countries</strong> extend a base country implementation
 *       (e.g., all French territories extend {@link FR})</li>
 *   <li><strong>MOD 97-10 countries</strong> extend {@link Mod97RemainderOneNcdCalculatorBase}.</li>
 *   <li><strong>No validated NCD</strong> (algorithm not specified or unknown)
 *       extend {@link NoOpNcdCalculatorBase}</li>
 *   <li>No duplicated logic — algorithms are shared through inheritance.</li>
 * </ul>
 *
 * <h3>Base class hierarchy</h3>
 * <pre>
 * NationalCheckDigitCalculator               (interface)
 * └── NcdCalculatorBase                      (abstract — holds countryData, default validate, mod97)
 *     ├── NoOpNcdCalculatorBase              (abstract — no NCD validation, passthrough)
 *     ├── Mod97RemainderOneNcdCalculatorBase (abstract — ISO 7064 MOD 97-10, remainder == 1)
 *     └── (concrete country classes)
 * </pre>
 *
 * @since 1.8.5
 *
 * @see NationalCheckDigitCalculator
 * @see NcdCalculatorBase
 * @see Mod97RemainderOneNcdCalculatorBase
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
final class NationalCheckDigitCalculators {

    private NationalCheckDigitCalculators() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * National Check Digit calculator for <strong>Albania (AL)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 8n bank/branch + 1n NCD + 16n account = 28 digits (IBAN length 28).
     *
     * <p><strong>Algorithm:</strong>
     * Weighted MOD 10 applied to the 8-digit bank/branch prefix.
     * Weights: {@code {9, 7, 3, 1, 9, 7, 3}} (cyclic). Result: {@code (10 − sum % 10) % 10}.
     */
    static final class AL extends NcdCalculatorBase {
        private static final int[] WEIGHTS = {9, 7, 3, 1, 9, 7, 3};

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            int s = 0;
            for (int i = 0; i < WEIGHTS.length; i++) {
                s += (iban[BBAN_START + i] - '0') * WEIGHTS[i];
            }
            return oneDigit((10 - s % 10) % 10);
        }
    }

    /**
     * National Check Digit calculator for <strong>Angola (AO)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 21n prefix + 2n NCD = 23 digits (IBAN length 25).
     *
     * <p><strong>Algorithm:</strong>
     * ISO 7064 MOD 97-10 variant. The NCD field is masked with {@code "00"}, then
     * {@code mod97} is computed over the BBAN prefix. NCD = {@code 98 − remainder},
     * with {@code 0} treated as {@code 98} to avoid a zero check digit.
     *
     * <p>To avoid unnecessary heap allocation, the masked BBAN is presented to
     * {@link Mod97#calculateRange(CharSequence, int, int)} as a virtual
     * {@link CharSequence} view over the original IBAN — no intermediate {@code char[]}
     * is allocated.
     */
    static final class AO extends NcdCalculatorBase {

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            final int prefixLen = ncdIndexRange.getBegin() - BBAN_START;
            final int maskedLen = countryData.getIbanLength() - BBAN_START;

            // virtual CharSequence view: forwards the BBAN prefix as-is, and substitutes
            // '0' for the two NCD positions — without copying any characters
            int remainder = Mod97.calculateRange(new CharSequence() {
                @Override
                public int length() {
                    return maskedLen;
                }

                @Override
                public char charAt(int index) {
                    // delegate to the original IBAN for the prefix, mask NCD positions with '0'
                    return index < prefixLen ? iban[BBAN_START + index] : '0';
                }

                @Override
                public CharSequence subSequence(int s, int e) {
                    throw new UnsupportedOperationException();
                }
            }, 0, maskedLen);

            // remainder calculation (using the char[] overload of Mod97)
            int ncd = remainder == 0 ? 0 : CHECK_DIGIT_MAGIC_NUMBER - remainder;

            return twoDigits(ncd);
        }
    }

    /**
     * National Check Digit calculator for <strong>Bosnia and Herzegovina (BA)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class BA extends Mod97RemainderOneNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Belgium (BE)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 3n bank + 7n account + 2n NCD = 12 digits (IBAN length 16).
     *
     * <p><strong>Algorithm:</strong>
     * MOD 97 applied to the 10-digit prefix (bank code + account number).
     * NCD = remainder, except remainder {@code 0} yields NCD = {@code 97}.
     * Result range: {@code 01–97}.
     */
    static final class BE extends NcdCalculatorBase {

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            int lengthToProcess = ncdIndexRange.getBegin() - BBAN_START; // 10 digits for BE

            int remainder = mod97(iban, BBAN_START, lengthToProcess);

            // per Belgian standard: remainder 0 maps to 97, not 00
            int ncd = (remainder == 0) ? 97 : remainder;

            return twoDigits(ncd);
        }
    }

    /**
     * National Check Digit calculator for <strong>Estonia (EE)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 2n bank + 2n branch + 11n account + 1n NCD + 3n reserved
     *  = 16 digits (IBAN length 20). Only the 15 digits before the NCD are used.
     *
     * <p><strong>Algorithm:</strong>
     * Weighted MOD 10 with weights {@code {7, 3, 1}} (cyclic, left-to-right) over the BBAN prefix.
     * Result: {@code (10 − sum % 10) % 10}.
     */
    static final class EE extends NcdCalculatorBase {
        private static final int[] WEIGHTS = {7, 3, 1};

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            int s = 0;
            for (int i = BBAN_START; i < ncdIndexRange.getBegin(); i++) {
                s += (iban[i] - '0') * WEIGHTS[(i - BBAN_START) % WEIGHTS.length];
            }
            return oneDigit((10 - s % 10) % 10);
        }
    }

    /**
     * National Check Digit calculator for <strong>Spain (ES)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 4n bank + 4n branch + 2n DC + 10n account = 20 digits
     * (IBAN length 24).
     *
     * <p><strong>Dígito de Control (DC):</strong>
     * Two separate check digits at positions {@code DC[0]} and {@code DC[1]}:
     * <ul>
     *   <li>{@code DC[0]}: computed from bank (4 digits) + branch (4 digits) using
     *       weights {@code {4, 8, 5, 10, 9, 7, 3, 6}} (weights[2..9], simulating a
     *       virtual "00" prefix with weights[0..1]).</li>
     *   <li>{@code DC[1]}: computed from account number (10 digits) using
     *       weights {@code {1, 2, 4, 8, 5, 10, 9, 7, 3, 6}}.</li>
     * </ul>
     *
     * <p><strong>Algorithm:</strong>
     * Weighted MOD 11. Result = {@code 11 − (sum % 11)}, mapping 11 → 0 and 10 → 1.
     */
    static final class ES extends NcdCalculatorBase {

        private static final int[] WEIGHTS = {1, 2, 4, 8, 5, 10, 9, 7, 3, 6};

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            // DC[0]: bank (4) + branch (4), weight offset 2 simulates "00" prefix
            int firstDigit   = calculateMod11Spanish(iban, BBAN_START, 8, 2);

            // DC[1]: account number (10 digits) after the NCD field
            int accountStart = ncdIndexRange.getEnd();
            int secondDigit  = calculateMod11Spanish(iban, accountStart, 10, 0);

            // the combination of both digits (0-9) results in a value from 00 to 99
            final int ncdValue = firstDigit * 10 + secondDigit;

            // return the pre-allocated CharSequence from cache
            // (Assuming the interface or internal logic handles CharSequence)
            return twoDigits(ncdValue);
        }

        /**
         * Computes a single Spanish DC digit using weighted MOD 11.
         *
         * <p>Formula: {@code result = 11 − (sum % 11)}, with special cases
         * {@code 11 → 0} and {@code 10 → 1}.
         *
         * @param iban         the IBAN character array
         * @param offset       start index in the array
         * @param length       number of digits to process
         * @param weightOffset starting index into {@link #WEIGHTS}
         * @return a single digit in range [0, 9]
         */
        private static int calculateMod11Spanish(final char[] iban, final int offset,
                                                 final int length, final int weightOffset) {
            int sum = 0;
            for (int i = 0; i < length; i++) {
                sum += (iban[offset + i] - '0') * WEIGHTS[weightOffset + i];
            }

            int remainder = 11 - (sum % 11);
            if (remainder == 11) {
                return 0;
            } else if (remainder == 10) {
                return 1;
            } else {
                return remainder;
            }
        }

    }

    /**
     * National Check Digit calculator for <strong>Finland (FI)</strong> and
     * <strong>Åland Islands (AX)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 6n bank + 7n account + 1n NCD = 14 digits
     * (IBAN length 18).
     *
     * <p><strong>Algorithm:</strong>
     * Luhn (MOD 10) applied right-to-left over the 13-digit prefix.
     * Weights {@code {2, 1}} alternate from the rightmost digit leftward.
     * Products ≥ 10 are reduced by summing their digits.
     * NCD = {@code (10 − sum % 10) % 10}.
     *
     * <p>Not declared {@code final} to allow {@link AX} to inherit this implementation.
     */
    static class FI extends NcdCalculatorBase {

        private static final int[] WEIGHTS = {2, 1};

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            int lengthToProcess = ncdIndexRange.getBegin() - BBAN_START;

            int sum = 0;
            for (int i = 0; i < lengthToProcess; i++) {
                int digit   = iban[ncdIndexRange.getBegin() - 1 - i] - '0';
                int product = digit * WEIGHTS[i % WEIGHTS.length];
                if (product >= 10) {
                    product = (product / 10) + (product % 10);
                }
                sum += product;
            }

            return oneDigit((10 - (sum % 10)) % 10);
        }
    }

    /** Åland Islands (AX) — uses Finland algorithm. */
    static final class AX extends FI {
    }

    /**
     * National Check Digit calculator for the <strong>Faroe Islands (FO)</strong>.
     *
     * <p>The SWIFT IBAN Registry does not specify a standardised NCD algorithm for FO.
     * Validation accepts any well-formed IBAN of the correct length.
     */
    static final class FO extends NoOpNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>France (FR)</strong> and all associated
     * territories.
     *
     * <p>Territories sharing this algorithm:
     * Guadeloupe (GP), Martinique (MQ), French Guiana (GF), Réunion (RE),
     * French Polynesia (PF), French Southern Territories (TF), Mayotte (YT),
     * New Caledonia (NC), Saint Barthélemy (BL), Saint Martin (MF),
     * Saint Pierre and Miquelon (PM), Wallis and Futuna (WF), Monaco (MC),
     * and several West-African countries (BF, BJ, CF, CM, GQ, KM, SN).
     * Tunisia (TN) uses the same formula.
     *
     * <p><strong>RIB — Clé RIB (Relevé d'Identité Bancaire):</strong>
     * Two-digit checksum {@code 01–97} at the end of the BBAN.
     *
     * <p><strong>Algorithm:</strong>
     * MOD 97 with French-specific letter substitution (see {@link #getFrenchRIBValue}).
     * NCD = {@code 97 − (prefix_remainder × 100 mod 97)}, with 0 mapped to 97.
     *
     * <p><strong>Letter mapping</strong> (differs from ISO 7064 A=10, B=11…):
     * <table>
     *   <caption>French RIB letter values</caption>
     *   <tr><th>Value</th><th>Letters</th></tr>
     *   <tr><td>1</td><td>A, J</td></tr>
     *   <tr><td>2</td><td>B, K, S</td></tr>
     *   <tr><td>3</td><td>C, L, T</td></tr>
     *   <tr><td>4</td><td>D, M, U</td></tr>
     *   <tr><td>5</td><td>E, N, V</td></tr>
     *   <tr><td>6</td><td>F, O, W</td></tr>
     *   <tr><td>7</td><td>G, P, X</td></tr>
     *   <tr><td>8</td><td>H, Q, Y</td></tr>
     *   <tr><td>9</td><td>I, R, Z</td></tr>
     * </table>
     *
     * <p>Not declared {@code final} to allow territory subclasses to inherit this implementation.
     */
    static class FR extends NcdCalculatorBase {

        @Override
        public final char[] calculateNationalCheckDigit(final char[] iban) {
            int lengthToProcess = ncdIndexRange.getBegin() - BBAN_START;

            int remainder = mod97French(iban, BBAN_START, lengthToProcess);

            // Clé RIB formula: 97 − (remainder × 100 mod 97); 0 maps to 97
            remainder = (remainder * 100) % MODULO_BASE;
            int ncd = MODULO_BASE - remainder;

            return twoDigits(ncd == 0 ? MODULO_BASE : ncd);
        }

        /**
         * Computes MOD 97 using the French RIB letter-to-digit mapping.
         *
         * <p>Unlike {@link NcdCalculatorBase#mod97(CharSequence, int, int)} (ISO 7064, A=10…Z=35),
         * each letter is mapped to a <em>single</em> digit 1–9 (see {@link #getFrenchRIBValue})
         * and fed as one digit into the streaming accumulator.
         *
         * @param data   the character array
         * @param offset start index (inclusive)
         * @param length number of characters to process
         * @return MOD 97 remainder in range [0, 96]
         */
        static int mod97French(final char[] data, final int offset, final int length) {
            int remainder = 0;
            for (int i = 0; i < length; i++) {
                remainder = (remainder * 10 + getFrenchRIBValue(data[offset + i])) % MODULO_BASE;
            }
            return remainder;
        }

        /**
         * Maps a character to its French RIB single-digit value (0–9).
         *
         * <p>Digits {@code '0'–'9'} map to themselves.
         * Letters follow a cyclic 1–9 pattern that repeats across A–I, J–R, and S–Z
         * (with S=2 rather than S=1, skipping 1 in the third group).
         *
         * @param ch the input character (digit or uppercase ASCII letter)
         * @return the RIB value in range [0, 9]; 0 for unrecognised characters
         */
        static int getFrenchRIBValue(final char ch) {
            if (ch >= '0' && ch <= '9') {
                return ch - '0';
            }
            if (ch >= 'A' && ch <= 'Z') {
                if (ch <= 'I') {
                    return ch - 'A' + 1; // A=1 … I=9
                }
                if (ch <= 'R') {
                    return ch - 'J' + 1; // J=1 … R=9
                }
                return ch - 'S' + 2;    // S=2 … Z=9 (skips 1)
            }
            return 0;
        }
    }

    /** Guadeloupe (GP) — uses France (FR) algorithm. */
    static final class GP extends FR {
    }

    /** Martinique (MQ) — uses France (FR) algorithm. */
    static final class MQ extends FR {
    }

    /** French Guiana (GF) — uses France (FR) algorithm. */
    static final class GF extends FR {
    }

    /** Réunion (RE) — uses France (FR) algorithm. */
    static final class RE extends FR {
    }

    /** French Polynesia (PF) — uses France (FR) algorithm. */
    static final class PF extends FR {
    }

    /** French Southern Territories (TF) — uses France (FR) algorithm. */
    static final class TF extends FR {
    }

    /** Mayotte (YT) — uses France (FR) algorithm. */
    static final class YT extends FR {
    }

    /** New Caledonia (NC) — uses France (FR) algorithm. */
    static final class NC extends FR {
    }

    /** Saint Barthélemy (BL) — uses France (FR) algorithm. */
    static final class BL extends FR {
    }

    /** Saint Martin (MF) — uses France (FR) algorithm. */
    static final class MF extends FR {
    }

    /** Saint Pierre and Miquelon (PM) — uses France (FR) algorithm. */
    static final class PM extends FR {
    }

    /** Wallis and Futuna (WF) — uses France (FR) algorithm. */
    static final class WF extends FR {
    }

    /**
     * National Check Digit calculator for <strong>Hungary (HU)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 3n bank + 4n branch + 1n NCD1 + 15n account + 1n NCD2
     * = 24 digits (IBAN length 28). This implementation computes the <em>second</em> check digit
     * (NCD2), which covers the 15-digit account number starting at IBAN index 12.
     *
     * <p><strong>Algorithm:</strong>
     * Weighted MOD 10 with weights {@code {9, 7, 3, 1}} (cyclic). The NCD equals the ones digit
     * of the weighted sum, i.e., {@code sum % 10} (the NCD is chosen so that including it makes
     * the total sum a multiple of 10 using weight 1 for the NCD position).
     */
    static final class HU extends NcdCalculatorBase {
        private static final int[] WEIGHTS = {9, 7, 3, 1};

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            // NCD2 covers the 15-digit account number: IBAN indices 12–26
            int accountStart = BBAN_START + 8; // after 3n bank + 4n branch + 1n NCD1
            int s = 0;
            for (int i = 0; i < 15; i++) {
                s += (iban[accountStart + i] - '0') * WEIGHTS[i % WEIGHTS.length];
            }
            return oneDigit(s % 10);
        }
    }

    /**
     * National Check Digit calculator for <strong>Italy (IT)</strong> and
     * <strong>San Marino (SM)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 1a CIN + 5n bank + 5n branch + 12c account = 23 chars
     * (IBAN length 27). The CIN is at {@code BBAN_START} (IBAN index 4); the algorithm
     * processes the remaining 22 characters from index 5 onward.
     *
     * <p><strong>CIN (Codice di Controllo / Codice Identificativo Numero):</strong>
     * A single letter {@code 'A'–'Z'} derived from the 22-character BBAN suffix (bank + branch
     * + account).
     *
     * <p><strong>Algorithm:</strong>
     * <ol>
     *   <li>Assign a numeric value to each character:
     *     <ul>
     *       <li><em>Odd positions</em> (1st, 3rd, …): use {@link #ODD_MAP}.</li>
     *       <li><em>Even positions</em> (2nd, 4th, …): digits map to 0–9, letters to 10–35.</li>
     *     </ul>
     *   </li>
     *   <li>Sum all values and take {@code sum % 26}.</li>
     *   <li>Convert result (0–25) to letter {@code 'A'–'Z'}.</li>
     * </ol>
     *
     * <p>Not declared {@code final} to allow {@link SM} to inherit this implementation.
     */
    static class IT extends NcdCalculatorBase {

        /**
         * Lookup table for characters at <em>odd</em> positions (1-based).
         * Index 0–9 covers digits {@code '0'–'9'}; index 10–35 covers letters {@code 'A'–'Z'}.
         */
        private static final int[] ODD_MAP = {
            1, 0, 5, 7, 9, 13, 15, 17, 19, 21, // '0' – '9'
            1, 0, 5, 7, 9, 13, 15, 17, 19, 21, // 'A' – 'J'
            2, 4, 18, 20, 11, 3, 6, 8, 12, 14, // 'K' – 'T'
            16, 10, 22, 25, 24, 23             // 'U' – 'Z'
        };

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            // CIN is at BBAN_START (index 4); algorithm covers indices 5 to end of BBAN
            int cinIndex = ncdIndexRange.getBegin();
            int bbanEnd  = countryData.getIbanLength();

            int sum = 0;
            for (int i = cinIndex + 1; i < bbanEnd; i++) {
                char ch  = iban[i];
                int val  = (ch <= '9') ? ch - '0' : ch - 'A' + 10;
                int pos  = i - (cinIndex + 1); // 0-based position after CIN

                sum += (pos % 2 == 0) ? ODD_MAP[val] : val;
            }

            return oneLetter(sum % 26);
        }
    }

    /** Monaco (MC) — uses France (FR) algorithm. */
    static final class MC extends FR {
    }

    /**
     * National Check Digit calculator for <strong>Montenegro (ME)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class ME extends Mod97RemainderOneNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>North Macedonia (MK)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class MK extends Mod97RemainderOneNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Mauritania (MR)</strong>.
     *
     * <p>The SWIFT IBAN Registry does not specify a standardised NCD algorithm for MR.
     * Validation accepts any well-formed IBAN of the correct length.
     */
    static final class MR extends NoOpNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Mozambique (MZ)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class MZ extends Mod97RemainderOneNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Norway (NO)</strong>.
     *
     * <p><strong>BBAN structure:</strong> 4n bank + 6n account + 1n NCD = 11 digits
     * (IBAN length 15).
     *
     * <p><strong>Algorithm:</strong>
     * Weighted MOD 11 with weights {@code {5, 4, 3, 2, 7, 6, 5, 4, 3, 2}} applied to the
     * 10-digit prefix (bank + account). NCD = {@code remainder == 0 ? 0 : 11 − remainder}.
     *
     * <p><strong>Special case — result {@code 10}:</strong>
     * A computed result of {@code 10} means the account number is <em>not issuable</em> by
     * Norwegian banks (Bankenes Standardiseringskontor). In this case
     * {@link #calculateNationalCheckDigit} returns the NCD value already present in the
     * input IBAN unchanged (passthrough), so that {@link #validateNationalCheckDigit} will
     * always pass for such account numbers.
     */
    static final class NO extends NcdCalculatorBase {

        private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            int lengthToProcess = ncdIndexRange.getBegin() - BBAN_START;

            int sum = 0;
            for (int i = 0; i < lengthToProcess; i++) {
                sum += (iban[BBAN_START + i] - '0') * WEIGHTS[i];
            }

            int remainder = sum % 11;
            int result    = (remainder == 0) ? 0 : (11 - remainder);

            // result == 10: account number not issuable — return existing NCD as-is
            return result == 10
                ? ncdIndexRange.applyTo(iban)
                : oneDigit(result);
        }
    }

    /**
     * National Check Digit calculator for <strong>Poland (PL)</strong>.
     *
     * <p>The SWIFT IBAN Registry does not specify a standardised NCD algorithm for PL.
     * Validation accepts any well-formed IBAN of the correct length.
     */
    static final class PL extends NoOpNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Portugal (PT)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     * Algorithm identical to Bosnia and Herzegovina (BA) and Montenegro (ME).
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class PT extends Mod97RemainderOneNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Serbia (RS)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class RS extends Mod97RemainderOneNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Slovenia (SI)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     * Algorithm identical to Bosnia and Herzegovina (BA) and Portugal (PT).
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class SI extends Mod97RemainderOneNcdCalculatorBase {
    }

    /** San Marino (SM) — uses Italy (IT) algorithm. */
    static final class SM extends IT {
    }

    /**
     * National Check Digit calculator for <strong>Timor-Leste (TL)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class TL extends Mod97RemainderOneNcdCalculatorBase {
    }

    /** Tunisia (TN) — uses France (FR) algorithm. */
    static final class TN extends FR {
    }

    /**
     * National Check Digit calculator for <strong>Turkey (TR)</strong>.
     *
     * <p>The SWIFT IBAN Registry does not specify a standardised NCD algorithm for TR.
     * Validation accepts any well-formed IBAN of the correct length.
     */
    static final class TR extends NoOpNcdCalculatorBase {
    }

    /**
     * National Check Digit calculator for <strong>Kosovo (XK)</strong>.
     *
     * <p>Uses ISO 7064 MOD 97-10. The full BBAN must produce remainder {@code 1}.
     *
     * @see Mod97RemainderOneNcdCalculatorBase
     */
    static final class XK extends Mod97RemainderOneNcdCalculatorBase {
    }

    // -------------------------------------------------------------------------
    // West-African CFA-franc zone — all use the France (FR) / Clé RIB algorithm
    // -------------------------------------------------------------------------

    /** Burkina Faso (BF) — uses France (FR) algorithm. */
    static final class BF extends FR {
    }

    /** Benin (BJ) — uses France (FR) algorithm. */
    static final class BJ extends FR {
    }

    /** Central African Republic (CF) — uses France (FR) algorithm. */
    static final class CF extends FR {
    }

    /** Cameroon (CM) — uses France (FR) algorithm. */
    static final class CM extends FR {
    }

    /**
     * National Check Digit calculator for <strong>Algeria (DZ)</strong>.
     *
     * <p>The SWIFT IBAN Registry does not specify a standardised NCD algorithm for DZ.
     * Validation accepts any well-formed IBAN of the correct length.
     */
    static final class DZ extends NoOpNcdCalculatorBase {
    }

    /** Equatorial Guinea (GQ) — uses France (FR) algorithm. */
    static final class GQ extends FR {
    }

    /** Comoros (KM) — uses France (FR) algorithm. */
    static final class KM extends FR {
    }

    /** Senegal (SN) — uses France (FR) algorithm. */
    static final class SN extends FR {
    }

    /**
     * National Check Digit calculator for <strong>Togo (TG)</strong>.
     *
     * <p>The SWIFT IBAN Registry does not specify a standardised NCD algorithm for TG.
     * Validation accepts any well-formed IBAN of the correct length.
     */
    static final class TG extends NoOpNcdCalculatorBase {
    }

    // =========================================================================
    // Base classes
    // =========================================================================

    /**
     * Abstract base class for all NCD calculators.
     *
     * <p>Provides:
     * <ul>
     *   <li>Auto-wiring of {@link #countryData} from the class's simple name via
     *       {@link IbanRegistry#getByCode(String)}.</li>
     *   <li>A default {@link #validateNationalCheckDigit} implementation that extracts
     *       the NCD from the IBAN and compares it to {@link #calculateNationalCheckDigit}.</li>
     *   <li>Shared constants ({@link #BBAN_START}, {@link #MODULO_BASE},
     *       {@link #CHECK_DIGIT_MAGIC_NUMBER}).</li>
     *   <li>Shared factory methods ({@link #oneDigit(int)}, {@link #twoDigits(int)}).</li>
     *   <li>Shared utility method {@link #mod97(char[], int, int)}.</li>
     *   <li>A {@link #toString()} that includes the country code.</li>
     * </ul>
     *
     * <p>Subclasses must implement {@link #calculateNationalCheckDigit(CharSequence)} and may
     * override {@link #validateNationalCheckDigit(CharSequence)} when a more efficient or
     * structurally different validation is required (e.g., {@link FR}).
     */
    abstract static class NcdCalculatorBase implements NationalCheckDigitCalculator {

        /** IBAN index at which the BBAN starts (always 4: country code + 2 check digits). */
        static final int   BBAN_START               = IbanRegistry.INDEX_BBAN;

        /** Modulus used by ISO 7064 MOD 97-10 algorithms. */
        static final int   MODULO_BASE              = 97;

        /**
         * Magic constant for the MOD 97-10 NCD formula: {@code NCD = 98 − remainder}.
         * Equals {@code MODULO_BASE + 1}.
         */
        static final int   CHECK_DIGIT_MAGIC_NUMBER = 98;

        /** Cache for single digits '0' through '9' as arrays. */
        private static final char[][]       DIGIT_CACHE =
            IntStream.range(0, 10)
                .mapToObj(i -> new char[] {(char) ('0' + i)})
                .toArray(char[][]::new);

        /** Cache for two-digit character arrays "00" through "99". */
        private static final char[][]       TWO_DIGIT_CACHE =
            IntStream.range(0, 100)
                .mapToObj(i -> new char[] {
                    (char) ('0' + (i / 10)),
                    (char) ('0' + (i % 10))
                })
                .toArray(char[][]::new);

        /** Cache for single uppercase letters 'A' through 'Z' as arrays. */
        private static final char[][]       LETTER_CACHE =
            IntStream.range(0, 26)
                .mapToObj(i -> new char[] {(char) ('A' + i)})
                .toArray(char[][]::new);

        /** Country-specific registry data, i.e. the enum entry in {@link IbanRegistry}, resolved at construction time. */
        final IbanRegistry countryData;

        /** Index range defining the position of the optional National Check Digit (NCD) within the IBAN. */
        final IndexRange   ncdIndexRange;

        /**
         * Resolves {@link #countryData} from the concrete subclass's simple name.
         *
         * @throws NullPointerException if the class name does not match any registered country code
         */
        NcdCalculatorBase() {
            final String clazzName = getClass().getSimpleName();
            countryData = requireNonNull(IbanRegistry.getByCode(clazzName),
                clazzName + " is not a supported IBAN country code");
            ncdIndexRange = countryData.getNationalCheckDigitIndexRange();
        }

        /**
         * Performs a high-performance validation of the National Check Digit (NCD).
         * <p>
         * Instead of extracting sub-arrays, this implementation operates directly on the
         * shared {@code iban} array using offsets. This eliminates heap allocations
         * during the validation of country-specific checksums (e.g., Italy's CIN).
         *
         * @param iban the normalized IBAN character array
         * @return {@code true} if the stored NCD matches the computed NCD
         */
        @Override
        public boolean validateNationalCheckDigit(final char[] iban) {
            // use coordinates instead of allocating a new array via existingNcd.applyTo(iban)
            final int start = ncdIndexRange.getBegin();
            final int length = ncdIndexRange.length();

            // the computed NCD should ideally return a primitive or use a small cached buffer
            final char[] computedNcd = calculateNationalCheckDigit(iban);

            // perform an in-place comparison to stay within L1 cache and avoid allocation
            if (length != computedNcd.length) {
                return false;
            }

            for (int i = 0; i < length; i++) {
                if (iban[start + i] != computedNcd[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns a character array containing the ASCII digit for {@code value}.
         * <p>
         * This implementation uses a static cache to avoid allocations.
         *
         * @param value a non-negative integer in range [0, 9]
         * @return a cached character array of length 1
         * @throws ArrayIndexOutOfBoundsException if value is not in range [0, 9]
         */
        static char[] oneDigit(final int value) {
            return DIGIT_CACHE[value];
        }

        /**
         * Returns a character array containing the two ASCII digit characters
         * of {@code value}, zero-padded (e.g. {@code 7} → {@code "07"}).
         * <p>
         * This implementation uses a static cache to avoid allocations.
         *
         * @param value a non-negative integer in range [0, 99]
         * @return a cached character array of length 2
         * @throws ArrayIndexOutOfBoundsException if value is not in range [0, 99]
         */
        static char[] twoDigits(final int value) {
            return TWO_DIGIT_CACHE[value];
        }

        /**
         * Returns a character array containing the uppercase ASCII letter
         * corresponding to the given alphabetic index.
         *
         * @param index a value in range [0, 25] where 0 is 'A' and 25 is 'Z'
         * @return a cached character array of length 1
         * @throws ArrayIndexOutOfBoundsException if index is not in range [0, 25]
         */
        static char[] oneLetter(final int index) {
            return LETTER_CACHE[index];
        }

        /**
         * Computes the ISO 7064 MOD 97-10 remainder for a sub-sequence of a character sequence.
         * <p>
         * Delegates to {@link Mod97#calculateRange(char[], int, int)}.
         *
         * @param data   the source character array
         * @param offset start index (inclusive)
         * @param length number of characters to process
         * @return the MOD 97 remainder in range [0, 96]
         */
        static int mod97(final char[] data, final int offset, final int length) {
            return Mod97.calculateRange(data, offset, length);
        }

        @Override
        public final String toString() {
            final String className = getClass().getName();
            return className.substring(className.lastIndexOf('.') + 1) + "[NCD " + ncdIndexRange + ']';
        }
    }

    /**
     * Base class for countries where no standardised NCD algorithm is specified by the
     * SWIFT IBAN Registry.
     *
     * <p>{@link #calculateNationalCheckDigit} returns the NCD already present in the
     * IBAN unchanged (passthrough). {@link #validateNationalCheckDigit} (inherited from
     * {@link NcdCalculatorBase}) will therefore always return {@code true} for a structurally
     * valid IBAN of the correct length.
     */
    abstract static class NoOpNcdCalculatorBase extends NcdCalculatorBase {

        /**
         * Returns the NCD value already present in {@code iban} without recomputing it.
         *
         * @param iban the normalized IBAN
         * @return the current NCD field extracted verbatim from {@code iban}
         */
        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            return ncdIndexRange.applyTo(iban);
        }
    }

    /**
     * Base class for countries that use the ISO 7064 MOD 97-10 algorithm where the
     * <em>full</em> BBAN (including the NCD) must produce a remainder of exactly {@code 1}
     * when divided by {@code 97}.
     *
     * <p>This is the standard algorithm used by BA, ME, MK, MZ, PT, RS, SI, TL, XK and others.
     *
     * <p><strong>Calculation:</strong>
     * {@code NCD = 98 − ((prefix_remainder × 100) mod 97)}, where
     * {@code prefix_remainder} is {@code mod97} applied to all BBAN digits before the
     * NCD field.
     *
     * <p><strong>Validation:</strong>
     * {@code mod97(full_BBAN) == 1}.
     */
    abstract static class Mod97RemainderOneNcdCalculatorBase extends NcdCalculatorBase {

        /**
         * Computes the two-digit NCD using the MOD 97-10 formula:
         * {@code 98 − ((prefix_remainder × 100) mod 97)}.
         *
         * @param iban the normalized IBAN
         * @return two-element {@link CharSequence} with the zero-padded NCD (e.g. {@code {'0', '7'}})
         */
        @Override
        public final char[] calculateNationalCheckDigit(final char[] iban) {
            int remainder = mod97(iban, BBAN_START, ncdIndexRange.getBegin() - BBAN_START);
            remainder = (remainder * 100) % MODULO_BASE;

            return twoDigits(CHECK_DIGIT_MAGIC_NUMBER - remainder);
        }

        /**
         * Validates by verifying that {@code mod97(full_BBAN) == 1}.
         *
         * @param iban the normalized IBAN
         * @return {@code true} if the full BBAN produces remainder {@code 1}
         */
        @Override
        public final boolean validateNationalCheckDigit(final char[] iban) {
            int bbanLength = countryData.getIbanLength() - BBAN_START;
            return mod97(iban, BBAN_START, bbanLength) == 1;
        }
    }

}
