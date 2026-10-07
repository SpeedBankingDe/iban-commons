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
package de.speedbanking.checkdigit.de;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The individual check digit algorithms of the Deutsche Bundesbank's
 * <em>"Prüfzifferberechnungsmethoden in der deutschen Kreditwirtschaft"</em> catalogue.
 * <p>
 * Each constant corresponds to one two-character method code as published by the
 * Bundesbank (e.g. method {@code 00} → {@link #M00}). Which method applies to a given
 * account is determined by the account-holding bank's Bankleitzahl (BLZ) and is
 * <em>not</em> looked up here — see {@link GermanAccountCheckDigit}.
 * <p>
 * All algorithms operate on the 10-digit domestic account number ("Kontonummer"),
 * right-justified and zero-padded. The check digit position varies by method — most
 * commonly the last digit (index 9), but a number of methods (e.g. {@link #M13},
 * {@link #M17}, {@link #M23}) place it elsewhere within the number. A handful of
 * methods (e.g. {@link #M51}, and method {@code 52}, not implemented — see below)
 * also read digits from the BLZ.
 *
 * <h2>Coverage</h2>
 * The reference source used to derive these algorithms defines 128 method codes.
 * Of those, {@code 12} is explicitly marked as unassigned ("12 is not used") and is
 * therefore not registered here either. Of the remaining 127, <strong>122 are
 * implemented</strong>; five are deliberately left out, grouped into three categories
 * below. For all unimplemented codes, including {@code 12}, {@link #fromCode(String)}
 * throws {@link IllegalArgumentException} — see {@link GermanAccountCheckDigit} for
 * how that propagates to callers.
 * <p>
 * <strong>Deliberately not implemented</strong> (all three categories, for the same
 * underlying reason — the risk of a silent transcription error outweighs the value
 * of a formula nobody can cross-check without the authoritative Bundesbank text):
 * <ul>
 *   <li><strong>{@code 52}, {@code 53}</strong> — build a combined "ESER-Nummer" that
 *       interleaves specific BLZ digits with the account number at computed offsets,
 *       then search for a matching factor via a modular loop. The offset arithmetic
 *       is intricate enough that a transcription error would silently produce a
 *       plausible-looking but wrong result.</li>
 *   <li><strong>{@code 87}</strong> — a stateful digit-substitution algorithm with
 *       running parity/carry variables threaded across the whole account number.</li>
 *   <li><strong>{@code B6}, {@code C0}</strong> — delegate part of their logic to
 *       {@code 53} and {@code 52} respectively, so they are incomplete without them.</li>
 * </ul>
 * <p>
 * All 122 implemented methods are exercised by the test suite. A handful were
 * implemented "with reservation" where the reference source itself flags ambiguity
 * (see {@link #C7}) or where an unusual, asymmetric formula was replicated literally
 * rather than "corrected" to a more regular pattern (see {@link #M57}) — these are
 * called out in the javadoc of the affected constant.
 *
 * @since 1.9.0
 *
 * @see <a href="https://www.bundesbank.de/de/startseite/pruefzifferberechnungsmethoden-603320">
 *      Deutsche Bundesbank: Prüfzifferberechnungsmethoden</a>
 */
@SuppressWarnings({"checkstyle:JavadocVariable", "checkstyle:MagicNumber"})
public enum GermanCheckDigitMethod {

    /**
     * Method {@code 00}.
     * <p>
     * Modulus 10, weights {@code {2,1,2,1,2,1,2,1,2}} applied to digits 1–9 (index 0–8).
     * Products greater than 9 are reduced to their cross sum ({@code product − 9}, valid
     * since the maximum product is {@code 2 × 9 = 18}). Check digit =
     * {@code (10 − sum % 10) % 10}, compared against digit 10 (index 9).
     */
    M00 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_2121, 0, true);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 01}.
     * <p>
     * Modulus 10, weights {@code {1,7,3,1,7,3,1,7,3}} applied to digits 1–9, no cross sum.
     * Check digit = {@code (10 − sum % 10) % 10}.
     */
    M01 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_173, 0, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 02}.
     * <p>
     * Modulus 11, weights {@code {2,9,8,7,6,5,4,3,2}} applied to digits 1–9.
     * Check digit = {@code (11 − sum % 11) % 11}; a computed value of {@code 10}
     * can never match a single decimal digit and therefore always fails —
     * this is expected: the Bundesbank specification treats such account numbers
     * as not verifiable under this method.
     */
    M02 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_298765432, 0, false);
            return compareToCheckDigit(account, mod11Complement(sum));
        }
    },

    /**
     * Method {@code 03}.
     * <p>
     * Modulus 10, weights {@code {2,1,2,1,2,1,2,1,2}} applied to digits 1–9, no cross sum
     * (unlike {@link #M00}, which uses the same weights but reduces products via cross sum).
     * Check digit = {@code (10 − sum % 10) % 10}.
     */
    M03 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_2121, 0, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 04}.
     * <p>
     * Modulus 11, weights {@code {4,3,2,7,6,5,4,3,2}} applied to digits 1–9.
     * Check digit = {@code (11 − sum % 11) % 11}.
     */
    M04 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_432765432, 0, false);
            return compareToCheckDigit(account, mod11Complement(sum));
        }
    },

    /**
     * Method {@code 05}.
     * <p>
     * Modulus 10, weights {@code {1,3,7,1,3,7,1,3,7}} applied to digits 1–9, no cross sum.
     * Check digit = {@code (10 − sum % 10) % 10}.
     */
    M05 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_137, 0, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 06}.
     * <p>
     * Modulus 11, same weights as {@link #M04} ({@code {4,3,2,7,6,5,4,3,2}}), but with a
     * different remainder-1 handling: {@code crc = 11 − sum % 11}, and <em>any</em> result
     * greater than {@code 9} (i.e. {@code 10} or {@code 11}) collapses to {@code 0} — whereas
     * {@link #M04} only collapses {@code 11} to {@code 0} and leaves {@code 10} as a
     * (permanently non-matching) value.
     */
    M06 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_432765432, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 07}.
     * <p>
     * Modulus 11, weights {@code {10,9,8,7,6,5,4,3,2}} applied to digits 1–9.
     * Check digit = {@code (11 − sum % 11) % 11}.
     */
    M07 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_109876543, 0, false);
            return compareToCheckDigit(account, mod11Complement(sum));
        }
    },

    /**
     * Method {@code 08}.
     * <p>
     * Applies only to account numbers whose full 10-digit value is {@code >= 60000}
     * (lower values fall in a historically reserved internal/test range and are not
     * verifiable). When applicable, uses the same formula as {@link #M00}: modulus 10,
     * weights {@code {2,1,2,1,2,1,2,1,2}} with cross sum.
     */
    M08 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (toLong(account) < METHOD_08_THRESHOLD) {
                return CheckDigitResult.NOT_CHECKED;
            }
            int sum = weightedSum(account, WEIGHTS_2121, 0, true);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 09} — "keine Prüfzifferberechnung" (no check digit method).
     * <p>
     * Explicitly permitted by the Bundesbank specification for payment service providers
     * that do not use check-digit-secured account numbers. Always reports
     * {@link CheckDigitResult#NOT_CHECKED}.
     */
    M09 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return CheckDigitResult.NOT_CHECKED;
        }
    },

    /**
     * Method {@code 10}.
     * <p>
     * Modulus 11, weights {@code {10,9,8,7,6,5,4,3,2}} (same as {@link #M07}), but with
     * the "clamp above 9 to 0" remainder rule instead of the "11 → 0 only" rule.
     */
    M10 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_109876543, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 11}.
     * <p>
     * Same weights as {@link #M10}, but with a distinct remainder mapping:
     * {@code 10 → 9}, {@code 11 → 0}, all other values unchanged.
     */
    M11 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_109876543, 0, false);
            int remainder = MODULUS_11 - sum % MODULUS_11;
            int expected;
            if (remainder == 10) {
                expected = 9;
            } else if (remainder == 11) {
                expected = 0;
            } else {
                expected = remainder;
            }
            return compareToCheckDigit(account, expected);
        }
    },

    /**
     * Method {@code 13}.
     * <p>
     * Two-stage: first tries modulus 10 with weights {@code {1,2,1,2,1,2}} and cross sum
     * over digits 2–7 (index 1–6), comparing at index 7. If that fails, retries the same
     * formula shifted two positions right (digits 4-9, index 3-8), comparing at index 9,
     * but only when digits 1-2 are {@code 00} (sub-account {@code 00} omitted).
     */
    M13 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum1 = weightedSum(account, WEIGHTS_121212, 1, true);
            CheckDigitResult first = compareAt(account, 7, mod10Complement(sum1));
            // the shifted retry is for an omitted sub-account 00, so only for accounts starting with 00
            if (first.isValid() || digitAt(account, 0) != 0 || digitAt(account, 1) != 0) {
                return first;
            }
            int sum2 = weightedSum(account, WEIGHTS_121212, 3, true);
            return compareAt(account, 9, mod10Complement(sum2));
        }
    },

    /**
     * Method {@code 14}.
     * <p>
     * Modulus 11, weights {@code {7,6,5,4,3,2}} over digits 4–9 (index 3–8), "11 → 0"
     * remainder rule, compared at index 9.
     */
    M14 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_765432, 3, false);
            return compareToCheckDigit(account, mod11Complement(sum));
        }
    },

    /**
     * Method {@code 15}.
     * <p>
     * Modulus 11, weights {@code {5,4,3,2}} over digits 6–9 (index 5–8), clamp-above-9 rule,
     * compared at index 9.
     */
    M15 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_5432, 5, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 16}.
     * <p>
     * Modulus 11, weights {@code {4,3,2,7,6,5,4,3,2}} (same as {@link #M04}), "11 → 0" rule.
     * If the raw remainder is {@code 10} (never directly matchable), the method instead
     * accepts the account if digits 9 and 10 (index 8 and 9) are equal — a documented
     * fallback rather than an outright rejection.
     */
    M16 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_432765432, 0, false);
            int       remainder = MODULUS_11 - sum % MODULUS_11;
            if (remainder == 11) {
                remainder = 0;
            }
            if (remainder == 10) {
                return CheckDigitResult.of(digitAt(account, 8) == digitAt(account, 9));
            }
            return compareToCheckDigit(account, remainder);
        }
    },

    /**
     * Method {@code 17}.
     * <p>
     * Modulus 10, weights {@code {1,2,1,2,1,2}} with cross sum over digits 2–7 (index 1–6).
     * Unusually, the sum is decremented by one before taking the check digit, and the
     * modulus applied to the decremented sum is {@code 11}, not {@code 10}:
     * {@code crc = (10 − (sum − 1) % 11) % 10}. Compared at index 7. This asymmetric
     * modulus-10/11 mix is exactly as specified — not a transcription artifact.
     */
    M17 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_121212, 1, true);
            sum--;
            int crc = (MODULUS_10 - sum % MODULUS_11) % MODULUS_10;
            return compareAt(account, 7, crc);
        }
    },

    /**
     * Method {@code 18}.
     * <p>
     * Modulus 10, weights {@code {3,1,7,9,3,1,7,9,3}}, no cross sum, compared at index 9.
     */
    M18 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_317931793, 0, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 19}.
     * <p>
     * Modulus 11, weights {@code {1,9,8,7,6,5,4,3,2}}, clamp-above-9 rule.
     */
    M19 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_198765432, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 20}.
     * <p>
     * Modulus 11, weights {@code {3,9,8,7,6,5,4,3,2}}, clamp-above-9 rule.
     */
    M20 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_398765432, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 21}.
     * <p>
     * Modulus 10, weights {@code {2,1,2,1,2,1,2,1,2}}, with a cross sum applied twice: once
     * per product (as usual), and then <em>again</em>, recursively, to the total sum. Check
     * digit = {@code 10 − recursiveCrossSum(sum)} (no further modulus reduction).
     */
    M21 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_2121, 0, true);
            int checksum = recursiveCrossSum(sum);
            return compareToCheckDigit(account, MODULUS_10 - checksum);
        }
    },

    /**
     * Method {@code 22}.
     * <p>
     * Modulus 10, weights {@code {3,1,3,1,3,1,3,1,3}}, no cross sum.
     */
    M22 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_313131313, 0, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 23}.
     * <p>
     * Modulus 11, weights {@code {7,6,5,4,3,2}} over digits 1–6 (index 0–5), "11 → 0" rule,
     * compared at index 6. If the raw remainder is {@code 10}, falls back to accepting the
     * account when digits 6 and 7 (index 5 and 6) are equal.
     */
    M23 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_765432, 0, false);
            int       remainder = MODULUS_11 - sum % MODULUS_11;
            if (remainder == 11) {
                remainder = 0;
            }
            if (remainder == 10) {
                return CheckDigitResult.of(digitAt(account, 5) == digitAt(account, 6));
            }
            return compareAt(account, 6, remainder);
        }
    },

    /**
     * Method {@code 24}.
     * <p>
     * Substitutes the leading digit(s) with zero for specific value ranges (digits
     * {@code 3–6} zero out digit 1; digit {@code 9} zeroes digits 1–3), then locates the
     * first non-zero digit and sums {@code ((weight × digit) + weight) % 11} across the
     * remaining digits up to index 8, cycling weights {@code {1,2,3}}. Check digit =
     * {@code sum % 10}, compared at index 9.
     */
    M24 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int[] digits = m24EffectiveDigits(account);

            int startIndex = 0;
            for (int i = 0; i < 9; i++) {
                if (digits[i] > 0) {
                    startIndex = i;
                    break;
                }
            }

            int sum = 0;
            for (int i = startIndex, j = 0; i < 9; i++, j++) {
                int weight = WEIGHTS_M24[j % WEIGHTS_M24.length];
                sum += (weight * digits[i] + weight) % MODULUS_11;
            }

            return compareToCheckDigit(account, sum % MODULUS_10);
        }
    },

    /**
     * Method {@code 25}.
     * <p>
     * Modulus 11, weights {@code {9,8,7,6,5,4,3,2}} over digits 2–9 (index 1–8). If the raw
     * remainder is {@code 10} and digit 2 (index 1) is less than {@code 8}, the account is
     * rejected outright (no fallback); otherwise the usual clamp-above-9 rule applies.
     */
    M25 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_98765432, 1, false);
            int       remainder = MODULUS_11 - sum % MODULUS_11;
            if (remainder == 10 && digitAt(account, 1) < 8) {
                return CheckDigitResult.of(false);
            }
            if (remainder > MAX_DIGIT) {
                remainder = 0;
            }
            return compareToCheckDigit(account, remainder);
        }
    },

    /**
     * Method {@code 26}.
     * <p>
     * Modulus 11, weights {@code {2,7,6,5,4,3,2}}, clamp-above-9 rule. The 7-digit window
     * starts at index 0, unless digits 1 and 2 are both zero, in which case it starts at
     * index 2 instead (and the compared check digit index shifts accordingly).
     */
    M26 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int startpos = digitAt(account, 0) == 0 && digitAt(account, 1) == 0 ? 2 : 0;
            int sum = weightedSum(account, WEIGHTS_2765432, startpos, false);
            return compareAt(account, startpos + 7, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 27}.
     * <p>
     * Digit-transformation method: if digit 1 (index 0) is zero, falls back to the
     * {@link #M00} formula. Otherwise, each of digits 1–9 is looked up in one of four
     * 10-entry substitution rows of {@link #TRANSFORM_TABLE} (row cycles as
     * {@code (8 − i) % 4} for digit index {@code i}), the substituted values are summed,
     * and the check digit is {@code (10 − sum % 10) % 10}, compared at index 9.
     */
    M27 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 0) {
                return M00.calculate(blz, account);
            }
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += TRANSFORM_TABLE[(8 - i) % 4][digitAt(account, i)];
            }
            return compareToCheckDigit(account, (MODULUS_10 - sum % MODULUS_10) % MODULUS_10);
        }
    },

    /**
     * Method {@code 28}.
     * <p>
     * Modulus 11, weights {@code {8,7,6,5,4,3,2}} over digits 1–7 (index 0–6), clamp-above-9
     * rule, compared at index 7.
     */
    M28 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_8765432, 0, false);
            return compareAt(account, 7, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 29}.
     * <p>
     * Digit-transformation method, structurally similar to {@link #M27} but with a
     * different traversal: digit {@code account[8 − i]} is looked up in
     * {@link #TRANSFORM_TABLE} row {@code i & 3}, for {@code i} from {@code 0} to {@code 8}.
     * Check digit = {@code (10 − sum % 10) % 10}, compared at index 9.
     */
    M29 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += TRANSFORM_TABLE[i & 3][digitAt(account, 8 - i)];
            }
            return compareToCheckDigit(account, (MODULUS_10 - sum % MODULUS_10) % MODULUS_10);
        }
    },

    /**
     * Method {@code 30}.
     * <p>
     * Modulus 10, weights {@code {2,0,0,0,0,1,2,1,2}} (positions 2–5 contribute nothing),
     * no cross sum.
     */
    M30 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M30, 0, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 31}.
     * <p>
     * Weights {@code {1,2,3,4,5,6,7,8,9}}; unlike most methods, the check digit is the raw
     * remainder {@code sum % 11} directly, without any complement step.
     */
    M31 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_123456789, 0, false);
            return compareToCheckDigit(account, sum % MODULUS_11);
        }
    },

    /**
     * Method {@code 32}.
     * <p>
     * Modulus 11, weights {@code {7,6,5,4,3,2}} over digits 4–9 (index 3–8), clamp-above-9
     * rule, compared at index 9.
     */
    M32 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_765432, 3, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 33}.
     * <p>
     * Modulus 11, weights {@code {6,5,4,3,2}} over digits 5–9 (index 4–8), clamp-above-9
     * rule, compared at index 9.
     */
    M33 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_65432, 4, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 34}.
     * <p>
     * Modulus 11, weights {@code {7,9,10,5,8,4,2}} over digits 1–7 (index 0–6),
     * clamp-above-9 rule, compared at index 7.
     */
    M34 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M34, 0, false);
            return compareAt(account, 7, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 35}.
     * <p>
     * Modulus 11, weights {@code {10,9,8,7,6,5,4,3,2}} (same as {@link #M07}/{@link #M10}).
     * Remainder {@code 11} collapses to {@code 0}; remainder {@code 10} falls back to
     * accepting the account if digits 9 and 10 (index 8 and 9) are equal — the same
     * fallback rule used by {@link #M16}.
     */
    M35 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_109876543, 0, false);
            int       remainder = MODULUS_11 - sum % MODULUS_11;
            if (remainder == 11) {
                remainder = 0;
            }
            if (remainder == 10) {
                return CheckDigitResult.of(digitAt(account, 8) == digitAt(account, 9));
            }
            return compareToCheckDigit(account, remainder);
        }
    },

    /**
     * Method {@code 36}.
     * <p>
     * Modulus 11, weights {@code {5,8,4,2}} over digits 6–9 (index 5–8), clamp-above-9 rule.
     */
    M36 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_5842, 5, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 37}.
     * <p>
     * Modulus 11, weights {@code {10,5,8,4,2}} over digits 5–9 (index 4–8), clamp-above-9
     * rule.
     */
    M37 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M37, 4, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 38}.
     * <p>
     * Modulus 11, weights {@code {9,10,5,8,4,2}} over digits 4–9 (index 3–8),
     * clamp-above-9 rule.
     */
    M38 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M38, 3, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 39}.
     * <p>
     * Modulus 11, weights {@code {7,9,10,5,8,4,2}} (same as {@link #M34}) over digits 3–9
     * (index 2–8), clamp-above-9 rule.
     */
    M39 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M34, 2, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 40}.
     * <p>
     * Modulus 11, weights {@code {6,3,7,9,10,5,8,4,2}}, clamp-above-9 rule.
     */
    M40 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M40, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 41}.
     * <p>
     * Modulus 10 with cross sum. If digit 4 (index 3) is {@code 9}, uses weights
     * {@code {1,2,1,2,1,2}} over digits 4–9 (index 3–8); otherwise uses
     * {@link #M00}'s weights {@code {2,1,2,1,2,1,2,1,2}} over all 9 digits.
     */
    M41 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = digitAt(account, 3) == 9
                ? weightedSum(account, WEIGHTS_121212, 3, true)
                : weightedSum(account, WEIGHTS_2121, 0, true);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 42}.
     * <p>
     * Modulus 11, weights {@code {9,8,7,6,5,4,3,2}} over digits 2–9 (index 1–8),
     * clamp-above-9 rule.
     */
    M42 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_98765432, 1, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 43}.
     * <p>
     * Modulus 10, weights {@code {9,8,7,6,5,4,3,2,1}}, no cross sum.
     */
    M43 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M43, 0, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 44}.
     * <p>
     * Modulus 11, weights {@code {0,0,0,0,10,5,8,4,2}} (only the last 5 digits before the
     * check digit contribute), clamp-above-9 rule.
     */
    M44 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M44, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 45}.
     * <p>
     * Delegates: if digit 1 is {@code 0}, digit 5 is {@code 1}, or digits 1–2 form
     * {@code 48}, no check is performed ({@link #M09}); otherwise applies {@link #M00}.
     */
    M45 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 0 || digitAt(account, 4) == 1
                || (digitAt(account, 0) == 4 && digitAt(account, 1) == 8)) {
                return M09.calculate(blz, account);
            }
            return M00.calculate(blz, account);
        }
    },

    /**
     * Method {@code 46}.
     * <p>
     * Modulus 11, weights {@code {6,5,4,3,2}} over digits 3–7 (index 2–6), clamp-above-9
     * rule, compared at index 7.
     */
    M46 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_65432, 2, false);
            return compareAt(account, 7, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 47}.
     * <p>
     * Modulus 11, weights {@code {6,5,4,3,2}} over digits 4–8 (index 3–7), clamp-above-9
     * rule, compared at index 8.
     */
    M47 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_65432, 3, false);
            return compareAt(account, 8, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 48}.
     * <p>
     * Modulus 11, weights {@code {7,6,5,4,3,2}} over digits 3–8 (index 2–7), clamp-above-9
     * rule, compared at index 8.
     */
    M48 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_765432, 2, false);
            return compareAt(account, 8, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 49}.
     * <p>
     * Delegates: applies {@link #M00}; if that fails, applies {@link #M01}.
     */
    M49 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            return first.isValid() ? first : M01.calculate(blz, account);
        }
    },

    /**
     * Method {@code 50}.
     * <p>
     * Modulus 11, weights {@code {7,6,5,4,3,2}} over digits 1–6 (index 0–5), clamp-above-9
     * rule, compared at index 6. If that fails and digits 1-3 are zero (Unternummer 000 omitted),
     * the account number is shifted three digits to the left (digits 4-10 become digits 1-7,
     * padded with three trailing zeros) and the same formula is retried on the shifted number.
     */
    M50 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = m50Attempt(account);
            if (first.isValid() || digitAt(account, 0) != 0 || digitAt(account, 1) != 0 || digitAt(account, 2) != 0) {
                return first;
            }
            char[] shifted = new char[ACCOUNT_LENGTH];
            System.arraycopy(account, 3, shifted, 0, 7);
            shifted[7] = '0';
            shifted[8] = '0';
            shifted[9] = '0';
            return m50Attempt(shifted);
        }
    },

    /**
     * Method {@code 51}.
     * <p>
     * For "Sachkonten" (digit 3 / index 2 equals {@code 9}): tries modulus 11 with weights
     * {@code {8,7,6,5,4,3,2}} over digits 3–9 (index 2–8); if that fails, modulus 11 with
     * weights {@code {10,9,8,7,6,5,4,3,2}} over all 9 digits. Both use the clamp-above-9 rule.
     * <p>
     * Otherwise, tries four variants in order, all compared at index 9: (A) modulus 11,
     * weights {@code {7,6,5,4,3,2}} over digits 4–9; (B) modulus 11, weights
     * {@code {6,5,4,3,2}} over digits 5–9; (C) modulus 10 with cross sum, weights
     * {@code {1,2,1,2,1,2}} over digits 4–9; (D) if digit 10 (index 9) is {@code >= 7}
     * the account is rejected outright, otherwise modulus 7 (weights as in B) over digits
     * 5–9, check digit {@code (7 − sum % 7) % 7}.
     */
    M51 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9) {
                int sumA = weightedSum(account, WEIGHTS_8765432, 2, false);
                CheckDigitResult a = compareToCheckDigit(account, mod11ClampAboveNine(sumA));
                if (a.isValid()) {
                    return a;
                }
                int sumB = weightedSum(account, WEIGHTS_109876543, 0, false);
                return compareToCheckDigit(account, mod11ClampAboveNine(sumB));
            }

            int sumA = weightedSum(account, WEIGHTS_765432, 3, false);
            CheckDigitResult a = compareToCheckDigit(account, mod11ClampAboveNine(sumA));
            if (a.isValid()) {
                return a;
            }

            int sumB = weightedSum(account, WEIGHTS_65432, 4, false);
            CheckDigitResult b = compareToCheckDigit(account, mod11ClampAboveNine(sumB));
            if (b.isValid()) {
                return b;
            }

            int sumC = weightedSum(account, WEIGHTS_121212, 3, true);
            CheckDigitResult c = compareToCheckDigit(account, mod10Complement(sumC));
            if (c.isValid()) {
                return c;
            }

            if (digitAt(account, 9) >= 7) {
                return CheckDigitResult.of(false);
            }
            int sumD = weightedSum(account, WEIGHTS_65432, 4, false);
            return compareToCheckDigit(account, mod7Complement(sumD));
        }
    },

    /**
     * Method {@code 52}.
     * <p>
     * Applies to 8-digit account numbers {@code XPXXXXXX} (digits 3-10, index 2-9, with a
     * non-zero digit 3). The check runs on the account number of the ESER legacy system,
     * built as BLZ digits 5-8, account digit 3, the check digit P (account digit 4), then
     * account digits 5-10 with leading zeros removed. See {@link #eserCheck} for the
     * modulus 11 factor search. 10-digit account numbers starting with {@code 9} are
     * checked with {@link #M20} instead; all other account numbers are rejected.
     * <p>
     * <strong>Implemented with reservation</strong>: the specification labels the BLZ
     * group of the ESER pattern as the variable-length part, but its worked example
     * removes the leading zeros of the trailing account digits ({@code 001500} becomes
     * {@code 1500}); the example is followed. Account numbers with fewer than 8 digits
     * are rejected, since the specification only defines the 8-digit layout. The BLZ
     * pattern {@code XXX5XXXX} is not enforced.
     */
    M52 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 9) {
                return M20.calculate(blz, account);
            }
            if (digitAt(account, 0) != 0 || digitAt(account, 1) != 0 || digitAt(account, 2) == 0) {
                return CheckDigitResult.of(false);
            }
            char[] head = {blz[4], blz[5], blz[6], blz[7], account[2], account[3]};
            return eserCheck(head, account);
        }
    },

    /**
     * Method {@code 53}.
     * <p>
     * Same calculation as {@link #M52}, but for 9-digit account numbers {@code XTPXXXXXX}
     * (digits 2-10, index 1-9, with a non-zero digit 2). The ESER account number is built
     * as BLZ digits 5 and 6, the account digit T (digit 3) in place of BLZ digit 7, BLZ
     * digit 8, account digit 2, the check digit P (account digit 4), then account digits
     * 5-10 with leading zeros removed. 10-digit account numbers starting with {@code 9}
     * are checked with {@link #M20} instead; all other account numbers are rejected.
     * <p>
     * <strong>Implemented with reservation</strong>: account numbers with fewer than 9
     * digits are rejected, since the specification only defines the 9-digit layout. The
     * BLZ pattern {@code XXX5XXXX} is not enforced.
     */
    M53 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 9) {
                return M20.calculate(blz, account);
            }
            if (digitAt(account, 0) != 0 || digitAt(account, 1) == 0) {
                return CheckDigitResult.of(false);
            }
            char[] head = {blz[4], blz[5], account[2], blz[7], account[1], account[3]};
            return eserCheck(head, account);
        }
    },

    /**
     * Method {@code 54}.
     * <p>
     * Modulus 11, weights {@code {2,7,6,5,4,3,2}} over digits 3–9 (index 2–8); digits 1 and
     * 2 are always {@code 4}, {@code 9} but are not weighted. The raw remainder
     * ({@code 11 − sum % 11}, without clamping) is used directly as the expected check
     * digit — a raw value of {@code 10} or {@code 11} therefore never matches, correctly
     * encoding the specification's "remainder 0 or 1 makes the account unverifiable" rule.
     */
    M54 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M54, 2, false);
            return compareToCheckDigit(account, MODULUS_11 - sum % MODULUS_11);
        }
    },

    /**
     * Method {@code 55}.
     * <p>
     * Modulus 11, weights {@code {8,7,8,7,6,5,4,3,2}}, clamp-above-9 rule.
     */
    M55 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M55, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 56}.
     * <p>
     * Modulus 11, weights {@code {4,3,2,7,6,5,4,3,2}} (same as {@link #M04}). If the raw
     * remainder exceeds {@code 9} <em>and</em> digit 1 (index 0) is {@code 9}, three is
     * subtracted from it; otherwise the raw (possibly {@code >9}, permanently non-matching)
     * value is used unchanged.
     */
    M56 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_432765432, 0, false);
            int       remainder = MODULUS_11 - sum % MODULUS_11;
            if (remainder > MAX_DIGIT && digitAt(account, 0) == 9) {
                remainder -= 3;
            }
            return compareToCheckDigit(account, remainder);
        }
    },

    /**
     * Method {@code 57}.
     * <p>
     * A large, range-dispatching method keyed on the first two digits of the account
     * number ("first"):
     * <ul>
     *   <li>{@code 00} is always rejected.</li>
     *   <li>Six consecutive digits equal to the first digit, starting with {@code 7} or
     *       {@code 8} (e.g. {@code 7777770000}), is always accepted.</li>
     *   <li>{@code 40}, {@code 50}, {@code 91}, {@code 99} are always accepted.</li>
     *   <li>A documented set of "first" values places the check digit at index 9, using
     *       modulus 10 with weights {@code {1,2,1,2,1,2,1,2,1}} and cross sum.</li>
     *   <li>A second documented set places the check digit at index 2, using modulus 10
     *       over all 10 digits with weights {@code {1,2,0,1,2,1,2,1,2,1}} (position 2
     *       zero-weighted so the check digit does not pollute its own checksum) and cross
     *       sum.</li>
     *   <li>{@code 01}–{@code 31}: accepted only if digits 3–4 form {@code 01}–{@code 12}
     *       and digit 7 is less than {@code 5}.</li>
     *   <li>Everything else: accepted only for the single historically-documented
     *       exception account number {@code 0185125434}.</li>
     * </ul>
     * <strong>Implemented with reservation</strong> — replicated faithfully from the
     * reference source's branch order, but that order makes the final "historical
     * exception account" branch effectively unreachable: every possible value of
     * "first" (00–99) is already covered by one of the preceding branches (including
     * {@code 0185125434} itself, whose "first" is {@code 01}, which the {@code 01}–{@code 31}
     * month-range branch rejects before the fallback is ever reached). This is a
     * pre-existing quirk carried over as-is rather than "corrected" by reordering.
     */
    M57 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int first = digitAt(account, 0) * 10 + digitAt(account, 1);

            if (first == 0) {
                return CheckDigitResult.of(false);
            }

            int leading = digitAt(account, 0);
            if (leading == 7 || leading == 8) {
                boolean allSame = true;
                for (int i = 1; i < 6; i++) {
                    if (digitAt(account, i) != leading) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame) {
                    return CheckDigitResult.of(true);
                }
            }

            if (first == 40 || first == 50 || first == 91 || first == 99) {
                return CheckDigitResult.of(true);
            }

            if (first == 51 || first == 55 || first == 61 || (first >= 64 && first <= 66)
                || first == 70 || first == 73 || (first >= 75 && first <= 82) || first == 88
                || first == 94 || first == 95) {
                int sum = weightedSum(account, WEIGHTS_M57A, 0, true);
                return compareToCheckDigit(account, mod10Complement(sum));
            }

            if ((first >= 32 && first <= 39) || (first >= 41 && first <= 49)
                || (first >= 52 && first <= 54) || (first >= 56 && first <= 60)
                || first == 62 || first == 63 || (first >= 67 && first <= 69)
                || first == 71 || first == 72 || first == 74
                || (first >= 83 && first <= 87) || first == 89 || first == 90
                || first == 92 || first == 93 || (first >= 96 && first <= 98)) {
                int sum = weightedSum(account, WEIGHTS_M57B, 0, true);
                return compareAt(account, 2, mod10Complement(sum));
            }

            if (first >= 1 && first <= 31) {
                int second = digitAt(account, 2) * 10 + digitAt(account, 3);
                return CheckDigitResult.of(second >= 1 && second <= 12 && digitAt(account, 6) < 5);
            }

            return CheckDigitResult.of(Arrays.equals(account, M57_EXCEPTION_ACCOUNT));
        }
    },

    /**
     * Method {@code 58}.
     * <p>
     * Modulus 11, weights {@code {0,0,0,0,6,5,4,3,2}} (only the last 5 digits before the
     * check digit contribute), "11 → 0" rule.
     */
    M58 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M58, 0, false);
            return compareToCheckDigit(account, mod11Complement(sum));
        }
    },

    /**
     * Method {@code 59}.
     * <p>
     * Delegates: if digits 1 and 2 are both zero, no check is performed ({@link #M09});
     * otherwise applies {@link #M00}.
     */
    M59 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 0 && digitAt(account, 1) == 0) {
                return M09.calculate(blz, account);
            }
            return M00.calculate(blz, account);
        }
    },

    /**
     * Method {@code 60}.
     * <p>
     * Modulus 10, weights {@code {2,1,2,1,2,1,2}} with cross sum, over digits 3–9
     * (index 2–8).
     */
    M60 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M60, 2, true);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 61}.
     * <p>
     * If digit 9 (index 8) is {@code 8}: modulus 10 with cross sum over all 10 digits,
     * weights {@code {2,1,2,1,2,1,2,0,1,2}} (position 8 zero-weighted), compared at index 7.
     * Otherwise: modulus 10 with cross sum, weights {@code {2,1,2,1,2,1,2}} over digits
     * 1–7 (index 0–6), also compared at index 7.
     */
    M61 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum;
            if (digitAt(account, 8) == 8) {
                sum = weightedSum(account, WEIGHTS_M61_LONG, 0, true);
            } else {
                sum = weightedSum(account, WEIGHTS_M60, 0, true);
            }
            return compareAt(account, 7, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 62}.
     * <p>
     * Modulus 10, weights {@code {2,1,2,1,2}} with cross sum, over digits 3–7 (index 2–6),
     * compared at index 7.
     */
    M62 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M73, 2, true);
            return compareAt(account, 7, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 63}.
     * <p>
     * Rejects the account unless digit 1 (index 0) is {@code 0}. Then applies modulus 10
     * with cross sum, weights {@code {1,2,1,2,1,2}}, over digits 2–7 (index 1–6), compared
     * at index 7. If that fails and digits 2–3 (index 1–2) are both zero, retries the same
     * weights over digits 4–9 (index 3–8), compared at index 9.
     */
    M63 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) != 0) {
                return CheckDigitResult.of(false);
            }
            int sum1 = weightedSum(account, WEIGHTS_121212, 1, true);
            CheckDigitResult first = compareAt(account, 7, mod10Complement(sum1));
            if (first.isValid()) {
                return first;
            }
            if (digitAt(account, 1) == 0 && digitAt(account, 2) == 0) {
                int sum2 = weightedSum(account, WEIGHTS_121212, 3, true);
                return compareAt(account, 9, mod10Complement(sum2));
            }
            return first;
        }
    },

    /**
     * Method {@code 64}.
     * <p>
     * Modulus 11, weights {@code {9,10,5,8,4,2}} over digits 1–6 (index 0–5),
     * clamp-above-9 rule, compared at index 6.
     */
    M64 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M64, 0, false);
            return compareAt(account, 6, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 65}.
     * <p>
     * Structurally identical to {@link #M61}, but keyed on digit 9 (index 8) being
     * {@code 9} instead of {@code 8}.
     */
    M65 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum;
            if (digitAt(account, 8) == 9) {
                sum = weightedSum(account, WEIGHTS_M61_LONG, 0, true);
            } else {
                sum = weightedSum(account, WEIGHTS_M60, 0, true);
            }
            return compareAt(account, 7, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 66}.
     * <p>
     * Weights {@code {7,0,0,6,5,4,3,2}} over digits 2–9 (index 1–8), no complement — instead
     * a direct remainder mapping: {@code 0 → 1}, {@code 1 → 0}, anything else {@code → 11 − remainder}.
     * Digit 1 must be {@code 0}; then digit 2 equal to {@code 9} is not checked ({@link #M09}).
     */
    M66 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            // digit 1 is not part of the 9-digit account number, so the exception applies only after this check
            if (digitAt(account, 0) != 0) {
                return CheckDigitResult.of(false);
            }
            if (digitAt(account, 1) == 9) {
                return M09.calculate(blz, account);
            }
            int sum = weightedSum(account, WEIGHTS_M66, 1, false);
            int remainder = sum % MODULUS_11;
            int crc;
            if (remainder == 0) {
                crc = 1;
            } else if (remainder == 1) {
                crc = 0;
            } else {
                crc = MODULUS_11 - remainder;
            }
            return compareToCheckDigit(account, crc);
        }
    },

    /**
     * Method {@code 67}.
     * <p>
     * Modulus 10, weights {@code {2,1,2,1,2,1,2}} with cross sum, over digits 1–7
     * (index 0–6), compared at index 7.
     */
    M67 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M60, 0, true);
            return compareAt(account, 7, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 68}.
     * <p>
     * If digit 1 (index 0) is non-zero: requires digit 4 (index 3) to be {@code 9},
     * otherwise rejects; then modulus 10 with cross sum, weights {@code {1,2,1,2,1,2}},
     * over digits 4–9 (index 3–8), compared at index 9.
     * <p>
     * If digit 1 is zero: no check is performed if digit 2 (index 1) is {@code 4}; the
     * account is rejected if digits 1–5 are all zero (fewer than 6 significant digits).
     * Otherwise tries modulus 10 with cross sum, weights {@code {1,2,1,2,1,2,1,2}} over
     * digits 2–9 (index 1–8); if that fails, weights {@code {1,0,0,2,1,2,1,2}} over the
     * same range — both compared at index 9.
     */
    M68 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) != 0) {
                if (digitAt(account, 3) != 9) {
                    return CheckDigitResult.of(false);
                }
                int sum = weightedSum(account, WEIGHTS_121212, 3, true);
                return compareToCheckDigit(account, mod10Complement(sum));
            }

            if (digitAt(account, 1) == 4) {
                return CheckDigitResult.NOT_CHECKED;
            }

            int sumFirstFive = 0;
            for (int i = 0; i <= 4; i++) {
                sumFirstFive += digitAt(account, i);
            }
            if (sumFirstFive == 0) {
                return CheckDigitResult.of(false);
            }

            int sumA = weightedSum(account, WEIGHTS_M68A, 1, true);
            CheckDigitResult a = compareToCheckDigit(account, mod10Complement(sumA));
            if (a.isValid()) {
                return a;
            }

            int sumB = weightedSum(account, WEIGHTS_M68B, 1, true);
            return compareToCheckDigit(account, mod10Complement(sumB));
        }
    },

    /**
     * Method {@code 69}.
     * <p>
     * If digit 1 (index 0) is {@code 9}: accepted with no further check if digit 2
     * (index 1) is {@code 3} ({@link CheckDigitResult#NOT_CHECKED}); if digit 2 is
     * {@code 7}, skips straight to the transformation-table step below.
     * <p>
     * Otherwise (or after skipping): first tries modulus 11, weights
     * {@code {8,7,6,5,4,3,2}} over digits 1–7 (index 0–6), clamp-above-9 rule, compared at
     * index 7. If that succeeds, the account is accepted.
     * <p>
     * If not (or if skipped straight here): a digit-transformation check using
     * {@link #TRANSFORM_TABLE}, traversing digits 9 down to 1 (index 8 down to 0), row
     * {@code (9 − i) % 4} for position {@code i} counted from 9 down to 1. Check digit =
     * {@code 10 − sum % 10}, compared at index 9.
     */
    M69 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            boolean tryVariant1 = true;
            if (digitAt(account, 0) == 9) {
                if (digitAt(account, 1) == 3) {
                    return CheckDigitResult.NOT_CHECKED;
                }
                if (digitAt(account, 1) == 7) {
                    tryVariant1 = false;
                }
            }

            if (tryVariant1) {
                int sum = weightedSum(account, WEIGHTS_8765432, 0, false);
                if (compareAt(account, 7, mod11ClampAboveNine(sum)).isValid()) {
                    return CheckDigitResult.of(true);
                }
            }

            int sum = 0;
            for (int i = 9; i != 0; i--) {
                int digit = digitAt(account, i - 1);
                int translated = TRANSFORM_TABLE[(9 - i) % 4][digit];
                sum += translated;
            }
            return compareToCheckDigit(account, MODULUS_10 - sum % MODULUS_10);
        }
    },

    /**
     * Method {@code 70}.
     * <p>
     * If digit 4 (index 3) is {@code 5}, or digit 4 is {@code 6} and digit 5 (index 4) is
     * {@code 9}: modulus 11, weights {@code {7,6,5,4,3,2}} over digits 4–9 (index 3–8).
     * Otherwise: modulus 11, weights {@code {4,3,2,7,6,5,4,3,2}} over all 9 digits.
     * Both branches use the clamp-above-9 rule and compare at index 9.
     */
    M70 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum;
            if (digitAt(account, 3) == 5 || (digitAt(account, 3) == 6 && digitAt(account, 4) == 9)) {
                sum = weightedSum(account, WEIGHTS_765432, 3, false);
            } else {
                sum = weightedSum(account, WEIGHTS_432765432, 0, false);
            }
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 71}.
     * <p>
     * Modulus 11, weights {@code {6,5,4,3,2,1}} over digits 2-7 (index 1-6). Check digit =
     * {@code (11 - sum % 11) % 11}; remainder 0 gives {@code 0}, remainder 1 gives {@code 1}.
     */
    M71 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M71, 1, false);
            int       remainder = mod11Complement(sum);
            if (remainder == 10) {
                remainder = 1;
            }
            return compareToCheckDigit(account, remainder);
        }
    },

    /**
     * Method {@code 73}.
     * <p>
     * If digit 3 (index 2) is {@code 9} (Sachkonten), delegates to {@link #M51}, which
     * applies its exception for exactly these account numbers; its result is final.
     * Otherwise tries three variants at index 9: (A) modulus 10 with cross sum, weights
     * {@code {1,2,1,2,1,2}} over digits 4-9; (B) modulus 10 with cross sum, weights
     * {@code {2,1,2,1,2}} over digits 5-9; (C) modulus 7 (same weights as B), check digit
     * {@code (7 - sum % 7) % 7}, always returned as the final result if A and B both fail.
     */
    M73 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9) {
                return M51.calculate(blz, account);
            }

            int sumA = weightedSum(account, WEIGHTS_121212, 3, true);
            CheckDigitResult a = compareToCheckDigit(account, mod10Complement(sumA));
            if (a.isValid()) {
                return a;
            }

            int sumB = weightedSum(account, WEIGHTS_M73, 4, true);
            CheckDigitResult b = compareToCheckDigit(account, mod10Complement(sumB));
            if (b.isValid()) {
                return b;
            }

            int sumC = weightedSum(account, WEIGHTS_M73, 4, true);
            return compareToCheckDigit(account, mod7Complement(sumC));
        }
    },

    /**
     * Method {@code 74}.
     * <p>
     * Variante 1: modulus 10, weights {@code {2,1,2,1,2,1,2,1,2}} with cross sum (same as
     * {@link #M00}). If that fails and digits 1–4 (index 0–3) are all zero while digit 5
     * (index 4) is not, retries with check digit {@code 5 − sum % 5} instead (the
     * "Halbdekade" special case for 6-digit account numbers). If Variante 1 fails
     * entirely, falls back to Variante 2: {@link #M04}.
     */
    M74 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_2121, 0, true);
            CheckDigitResult standard = compareToCheckDigit(account, mod10Complement(sum));
            if (standard.isValid()) {
                return standard;
            }
            if (digitAt(account, 0) + digitAt(account, 1) + digitAt(account, 2) + digitAt(account, 3) == 0
                && digitAt(account, 4) != 0) {
                CheckDigitResult halbdekade = compareToCheckDigit(account, MODULUS_5 - sum % MODULUS_5);
                if (halbdekade.isValid()) {
                    return halbdekade;
                }
            }
            return M04.calculate(blz, account);
        }
    },

    /**
     * Method {@code 76}.
     * <p>
     * Kontoart at digit 1, weights {@code {7,6,5,4,3,2}} over digits 2-7, {@code crc = sum % 11}
     * (no complement) compared at index 7. If that fails and digits 1-2 are zero (Unterkonto
     * 00 omitted), retries with the Kontoart at digit 3 and the Stammnummer at digits 4-9,
     * compared at index 9. The Kontoart must be 0, 4, 6, 7, 8 or 9.
     */
    M76 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult primary = m76Attempt(account, 0);
            if (primary.isValid() || digitAt(account, 0) != 0 || digitAt(account, 1) != 0) {
                return primary;
            }
            return m76Attempt(account, 2);
        }
    },

    /**
     * Method {@code 78}.
     * <p>
     * If digits 1–2 (index 0–1) sum to zero while digit 3 (index 2) does not, no check is
     * performed ({@link CheckDigitResult#NOT_CHECKED}). Otherwise: modulus 10 with cross
     * sum, weights {@code {2,1,2,1,2,1,2,1,2}} (same as {@link #M00}).
     */
    M78 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) + digitAt(account, 1) == 0 && digitAt(account, 2) != 0) {
                return CheckDigitResult.NOT_CHECKED;
            }
            int sum = weightedSum(account, WEIGHTS_2121, 0, true);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /** Method {@code 81}. Delegates to {@link #M51} if digit 3 (index 2) is {@code 9}, else {@link #M32}. */
    M81 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 2) == 9 ? M51.calculate(blz, account) : M32.calculate(blz, account);
        }
    },

    /**
     * Method {@code 82}. Delegates to {@link #M10} if digits 3 and 4 (index 2, 3) are both
     * {@code 9}, else {@link #M33}.
     */
    M82 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 2) == 9 && digitAt(account, 3) == 9
                ? M10.calculate(blz, account) : M33.calculate(blz, account);
        }
    },

    /**
     * Method {@code 84}.
     * <p>
     * If digit 3 (index 2) is {@code 9} (Sachkonten), delegates to {@link #M51}, which
     * applies its exception for exactly these account numbers; its result is final.
     * Otherwise tries three variants at index 9, weights
     * {@code {6,5,4,3,2}} over digits 5–9 throughout: (A) modulus 11, clamp-above-9;
     * (B) modulus 7, {@code (7 − sum % 7) % 7} (mapped >9 to 0, though the formula never
     * exceeds 6); (C) modulus 10, weights {@code {2,1,2,1,2}}, products added without cross sum
     * ("entsprechen dem Verfahren 06").
     */
    M84 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9) {
                return M51.calculate(blz, account);
            }

            int sumA = weightedSum(account, WEIGHTS_65432, 4, false);
            CheckDigitResult a = compareToCheckDigit(account, mod11ClampAboveNine(sumA));
            if (a.isValid()) {
                return a;
            }

            int sumB = weightedSum(account, WEIGHTS_65432, 4, false);
            CheckDigitResult b = compareToCheckDigit(account, mod7Complement(sumB));
            if (b.isValid()) {
                return b;
            }

            int sumC = weightedSum(account, WEIGHTS_M73, 4, false);
            return compareToCheckDigit(account, mod10Complement(sumC));
        }
    },

    /**
     * Method {@code 85}.
     * <p>
     * If digits 3 and 4 (index 2, 3) are both {@code 9}: modulus 11, weights
     * {@code {8,7,6,5,4,3,2}} over digits 3–9, with the raw remainder left unmapped except
     * that {@code 11} collapses to {@code 0} (a value of {@code 10} is permanently
     * non-matching, unlike the usual clamp-above-9 rule).
     * <p>
     * Otherwise, tries: (A) modulus 11, weights {@code {7,6,5,4,3,2}} over digits 4–9,
     * clamp-above-9; (B) {@link #M33}; (C) modulus 7 over digits 5–9,
     * {@code (7 − sum % 7) % 7} — always returned if A and B both fail.
     */
    M85 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9 && digitAt(account, 3) == 9) {
                int sum = weightedSum(account, WEIGHTS_8765432, 2, false);
                int       remainder = MODULUS_11 - sum % MODULUS_11;
                if (remainder == 11) {
                    remainder = 0;
                }
                return compareToCheckDigit(account, remainder);
            }

            int sumA = weightedSum(account, WEIGHTS_765432, 3, false);
            CheckDigitResult a = compareToCheckDigit(account, mod11ClampAboveNine(sumA));
            if (a.isValid()) {
                return a;
            }

            CheckDigitResult b = M33.calculate(blz, account);
            if (b.isValid()) {
                return b;
            }

            int sumC = weightedSum(account, WEIGHTS_65432, 4, false);
            return compareToCheckDigit(account, mod7Complement(sumC));
        }
    },

    /**
     * Method {@code 86}. Sachkonten exception (digit 3 / index 2 equals {@code 9})
     * delegates to {@link #M51}. Otherwise tries modulus 10 with cross sum, weights
     * {@code {1,2,1,2,1,2}} over digits 4–9; if that fails, delegates to {@link #M33}.
     */
    M86 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9) {
                return M51.calculate(blz, account);
            }
            int sum = weightedSum(account, WEIGHTS_121212, 3, true);
            CheckDigitResult a = compareToCheckDigit(account, mod10Complement(sum));
            if (a.isValid()) {
                return a;
            }
            return M33.calculate(blz, account);
        }
    },

    /**
     * Method {@code 87}.
     * <p>
     * Sachkonten exception (digit 3 / index 2 equals {@code 9}) delegates to {@link #M51}.
     * Otherwise tries, all compared at index 9: (A) the Bundesbank pseudo code over digits
     * 4-9 with the result tables {@code TAB1}/{@code TAB2}, see {@link #calculateM87A};
     * (B) {@link #M33}; (C) modulus 7, weights {@code {6,5,4,3,2}} over digits 5-9,
     * {@code (7 - sum % 7) % 7}; (D) modulus 11, weights {@code {7,6,5,4,3,2}} over
     * digits 4-9, clamp-above-9 as in method {@code 06}.
     * <p>
     * <strong>Implemented with reservation</strong>: the pseudo code of method A skips
     * leading zeros from digit 4 without an upper bound, so it reads past digit 10 when
     * digits 4-10 are all {@code 0}. The skip stops at digit 10 here, which gives the same
     * result wherever the specification is defined.
     */
    M87 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9) {
                return M51.calculate(blz, account);
            }
            CheckDigitResult a = calculateM87A(account);
            if (a.isValid()) {
                return a;
            }
            CheckDigitResult b = M33.calculate(blz, account);
            if (b.isValid()) {
                return b;
            }
            int sumC = weightedSum(account, WEIGHTS_65432, 4, false);
            CheckDigitResult c = compareToCheckDigit(account, mod7Complement(sumC));
            if (c.isValid()) {
                return c;
            }
            int sumD = weightedSum(account, WEIGHTS_765432, 3, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sumD));
        }
    },

    /**
     * Method {@code 88}.
     * <p>
     * Modulus 11, clamp-above-9 rule, compared at index 9. Weights and offset depend on
     * digit 3 (index 2): if {@code 9}, weights {@code {8,7,6,5,4,3,2}} over digits 3–9;
     * otherwise weights {@code {7,6,5,4,3,2}} over digits 4–9.
     */
    M88 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = digitAt(account, 2) == 9
                ? weightedSum(account, WEIGHTS_8765432, 2, false)
                : weightedSum(account, WEIGHTS_765432, 3, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 90}.
     * <p>
     * "Sachkonten" (digit 3 / index 2 equals {@code 9}): modulus 11, weights
     * {@code {8,7,6,5,4,3,2}} over digits 3–9, clamp-above-9, compared at index 9 —
     * this branch's result is returned directly, with no further fallback.
     * <p>
     * Otherwise tries, all over digits 5–9 (weights {@code {6,5,4,3,2}}) except E,
     * compared at index 9: (A) modulus 11, clamp-above-9; (B) modulus 7,
     * {@code (7 − sum % 7) % 7}, {@code 7 → 0}; (C) modulus 9, {@code (9 − sum % 9) % 9},
     * with an extra requirement that digit 10 itself is not {@code 9}; (D) modulus 10 with
     * cross sum, weights {@code {2,1,2,1,2}}; (E, always returned as the final fallback)
     * modulus 7, weights {@code {1,2,1,2,1,2}} over digits 4–9.
     */
    M90 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9) {
                int sum = weightedSum(account, WEIGHTS_8765432, 2, false);
                return compareToCheckDigit(account, mod11ClampAboveNine(sum));
            }

            int sumA = weightedSum(account, WEIGHTS_765432, 3, false);
            CheckDigitResult a = compareToCheckDigit(account, mod11ClampAboveNine(sumA));
            if (a.isValid()) {
                return a;
            }

            int sumB = weightedSum(account, WEIGHTS_65432, 4, false);
            CheckDigitResult b = compareToCheckDigit(account, mod11ClampAboveNine(sumB));
            if (b.isValid()) {
                return b;
            }

            int sumC = weightedSum(account, WEIGHTS_65432, 4, false);
            int crcC = mod7Complement(sumC);
            if (digitAt(account, 9) == crcC) {
                return CheckDigitResult.of(true);
            }

            int sumD = weightedSum(account, WEIGHTS_65432, 4, false);
            int crcD = mod9Complement(sumD);
            if (digitAt(account, 9) == crcD && digitAt(account, 9) != 9) {
                return CheckDigitResult.of(true);
            }

            int sumE = weightedSum(account, WEIGHTS_M73, 4, true);
            CheckDigitResult e = compareToCheckDigit(account, mod10Complement(sumE));
            if (e.isValid()) {
                return e;
            }

            int sumG = weightedSum(account, WEIGHTS_121212, 3, false);
            return compareToCheckDigit(account, mod7Complement(sumG));
        }
    },

    /**
     * Method {@code 91}.
     * <p>
     * Tries four weighted-sum variants in order, all modulus 11 with the clamp-above-9
     * rule, compared at index 6, stopping at the first match: (1) weights
     * {@code {7,6,5,4,3,2}} over digits 1–6; (2) weights {@code {2,3,4,5,6,7}} over digits
     * 1–6; (3) weights {@code {10,9,8,7,6,5,0,4,3,2}} over all 10 digits (position 6
     * zero-weighted); (4) weights {@code {9,10,5,8,4,2}} over digits 1–6.
     */
    M91 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int[][] variants = {
                {0}, {1}, {2}, {3},
            };
            for (int[] variant : variants) {
                int sum;
                switch (variant[0]) {
                    case 0:
                        sum = weightedSum(account, WEIGHTS_765432, 0, false);
                        break;
                    case 1:
                        sum = weightedSum(account, WEIGHTS_M91B, 0, false);
                        break;
                    case 2:
                        sum = weightedSum(account, WEIGHTS_M91C, 0, false);
                        break;
                    default:
                        sum = weightedSum(account, WEIGHTS_M64, 0, false);
                        break;
                }
                if (compareAt(account, 6, mod11ClampAboveNine(sum)).isValid()) {
                    return CheckDigitResult.of(true);
                }
            }
            return CheckDigitResult.of(false);
        }
    },

    /**
     * Method {@code 92}.
     * <p>
     * Modulus 10, weights {@code {1,7,3,1,7,3}} over digits 4–9 (index 3–8), no cross sum.
     */
    M92 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M92, 3, false);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 93}.
     * <p>
     * The account number encodes a 5-digit Kundennummer in one of two positions: if
     * digits 1–4 (index 0–3) are all {@code 0}, the Kundennummer occupies digits 5–9
     * (index 4–8) and the check digit is at index 9; otherwise the Kundennummer occupies
     * digits 1–5 (index 0–4) and the check digit is at index 5 (digits 7–10 hold an
     * unchecked Unter-/Kontoartnummer). Modulus 11, weights {@code {6,5,4,3,2}} (same as
     * {@link #M33}), clamp-above-9 rule. If that fails, retries with modulus 7:
     * {@code (7 − sum % 7) % 7}. Also used directly by {@link #A4}'s Variante 4.
     */
    M93 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return calculateM93(account);
        }
    },

    /**
     * Method {@code 94}.
     * <p>
     * Modulus 10, weights {@code {1,2,1,2,1,2,1,2,1}} with cross sum.
     */
    M94 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M57A, 0, true);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code 95}.
     * <p>
     * If digit 1 (index 0) is {@code 0} and the full account number falls into one of
     * several documented reserved ranges, the account is accepted with no further check
     * ({@link CheckDigitResult#NOT_CHECKED}). Otherwise: modulus 11, weights
     * {@code {4,3,2,7,6,5,4,3,2}} (same as {@link #M04}), clamp-above-9 rule.
     */
    M95 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 0) {
                long value = toLong(account);
                if ((value >= 1 && value <= 1_999_999L)
                    || (value >= 9_000_000L && value <= 25_999_999L)
                    || (value >= 396_000_000L && value <= 499_999_999L)
                    || (value >= 700_000_000L && value <= 799_999_999L)
                    || (value >= 910_000_000L && value <= 989_999_999L)) {
                    return CheckDigitResult.NOT_CHECKED;
                }
            }
            int sum = weightedSum(account, WEIGHTS_432765432, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code 96}.
     * <p>
     * Accepted if either {@link #M19} or {@link #M00} validates the account. Otherwise, if
     * digit 1 (index 0) is {@code 0} and the full account number falls within
     * {@code [1300000, 99399999]}, the account is accepted as well.
     */
    M96 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (M19.calculate(blz, account).isValid() || M00.calculate(blz, account).isValid()) {
                return CheckDigitResult.of(true);
            }
            long value = digitAt(account, 0) == 0 ? toLong(account) : 0;
            return CheckDigitResult.of(value >= 1_300_000L && value <= 99_399_999L);
        }
    },

    /**
     * Method {@code 98}.
     * <p>
     * Modulus 10, weights {@code {3,7,1,3,7,1,3}} over digits 3–9 (index 2–8), no cross sum.
     * If that fails, delegates to {@link #M32}.
     */
    M98 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_M98, 2, false);
            CheckDigitResult first = compareToCheckDigit(account, mod10Complement(sum));
            if (first.isValid()) {
                return first;
            }
            return M32.calculate(blz, account);
        }
    },

    /**
     * Method {@code 99}.
     * <p>
     * If the full account number is <em>outside</em> {@code [396000000, 499999999]}:
     * modulus 11, weights {@code {4,3,2,7,6,5,4,3,2}}, clamp-above-9 rule. Inside that
     * range, the account is accepted with no further check.
     */
    M99 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            long value = digitAt(account, 0) == 0 ? toLong(account) : 0;
            if (value < 396_000_000L || value > 499_999_999L) {
                int sum = weightedSum(account, WEIGHTS_432765432, 0, false);
                return compareToCheckDigit(account, mod11ClampAboveNine(sum));
            }
            return CheckDigitResult.NOT_CHECKED;
        }
    },

    /**
     * Method {@code A0}.
     * <p>
     * If digits 1–7 (index 0–6) sum to zero, the account is accepted with no further check.
     * Otherwise: modulus 11, weights {@code {10,5,8,4,2}} over digits 5–9 (index 4–8),
     * clamp-above-9 rule.
     */
    A0 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sumFirstSeven = 0;
            for (int i = 0; i < 7; i++) {
                sumFirstSeven += digitAt(account, i);
            }
            if (sumFirstSeven == 0) {
                return CheckDigitResult.NOT_CHECKED;
            }
            int sum = weightedSum(account, WEIGHTS_A0, 4, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code A1}.
     * <p>
     * Applicable only if digit 1 (index 0) is non-zero, or digits 1–2 are zero while
     * digit 3 (index 2) is not; otherwise rejected outright. When applicable: modulus 10
     * with cross sum, weights {@code {0,0,2,1,2,1,2,1,2}}.
     */
    A1 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            boolean applicable = digitAt(account, 0) != 0
                || (digitAt(account, 0) == 0 && digitAt(account, 1) == 0 && digitAt(account, 2) != 0);
            if (!applicable) {
                return CheckDigitResult.of(false);
            }
            int sum = weightedSum(account, WEIGHTS_A1, 0, true);
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /** Method {@code A2}. Delegates to {@link #M00}; if that fails, to {@link #M04}. */
    A2 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            return first.isValid() ? first : M04.calculate(blz, account);
        }
    },

    /** Method {@code A3}. Delegates to {@link #M00}; if that fails, to {@link #M10}. */
    A3 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            return first.isValid() ? first : M10.calculate(blz, account);
        }
    },

    /**
     * Method {@code A4}.
     * <p>
     * If digits 3–4 (index 2–3) read {@code 99}, only Variante 3 and (if that fails)
     * Variante 4 apply. All other account numbers are tried in order: Variante 1, then
     * (if that fails) Variante 2, then (if that also fails) Variante 4.
     * <ul>
     *   <li>Variante 1: modulus 11, weights {@code {7,6,5,4,3,2}} over digits 4–9
     *       (index 3–8) — same formula as {@link #M32} — clamp-above-9 rule.</li>
     *   <li>Variante 2: the same weighted sum as Variante 1, but modulus 7:
     *       {@code (7 − sum % 7) % 7}.</li>
     *   <li>Variante 3: modulus 11, weights {@code {6,5,4,3,2}} over digits 5–9
     *       (index 4–8) — same formula as {@link #M33} — clamp-above-9 rule.</li>
     *   <li>Variante 4: delegates to {@link #M93}.</li>
     * </ul>
     */
    A4 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9 && digitAt(account, 3) == 9) {
                CheckDigitResult variant3 = M33.calculate(blz, account);
                return variant3.isValid() ? variant3 : M93.calculate(blz, account);
            }
            CheckDigitResult variant1 = M32.calculate(blz, account);
            if (variant1.isValid()) {
                return variant1;
            }
            int sum = weightedSum(account, WEIGHTS_765432, 3, false);
            CheckDigitResult variant2 = compareToCheckDigit(account, mod7Complement(sum));
            return variant2.isValid() ? variant2 : M93.calculate(blz, account);
        }
    },

    /**
     * Method {@code A5}. Delegates to {@link #M00}; if that fails and digit 1 (index 0)
     * is {@code 9}, rejects outright; otherwise delegates to {@link #M10}.
     */
    A5 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            if (digitAt(account, 0) == 9) {
                return CheckDigitResult.of(false);
            }
            return M10.calculate(blz, account);
        }
    },

    /** Method {@code A6}. Delegates to {@link #M00} if digit 2 (index 1) is {@code 8}, else {@link #M01}. */
    A6 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 1) == 8 ? M00.calculate(blz, account) : M01.calculate(blz, account);
        }
    },

    /** Method {@code A7}. Delegates to {@link #M00}; if that fails, to {@link #M03}. */
    A7 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            return first.isValid() ? first : M03.calculate(blz, account);
        }
    },

    /**
     * Method {@code A8}.
     * <p>
     * Sachkonten exception (digit 3 / index 2 equals {@code 9}) delegates to
     * {@link #M51}. Otherwise tries modulus 11, weights {@code {7,6,5,4,3,2}} over digits
     * 4–9, clamp-above-9; if that fails, modulus 10 with cross sum, weights
     * {@code {1,2,1,2,1,2}} over the same range.
     */
    A8 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 2) == 9) {
                return M51.calculate(blz, account);
            }
            int sumA = weightedSum(account, WEIGHTS_765432, 3, false);
            CheckDigitResult a = compareToCheckDigit(account, mod11ClampAboveNine(sumA));
            if (a.isValid()) {
                return a;
            }
            int sumB = weightedSum(account, WEIGHTS_121212, 3, true);
            return compareToCheckDigit(account, mod10Complement(sumB));
        }
    },

    /** Method {@code A9}. Delegates to {@link #M01}; if that fails, to {@link #M06}. */
    A9 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M01.calculate(blz, account);
            return first.isValid() ? first : M06.calculate(blz, account);
        }
    },

    /**
     * Method {@code B0}.
     * <p>
     * Rejects outright if digit 1 (index 0) is {@code 0} (account number has 9 or fewer
     * significant digits) or {@code 8}. Otherwise the further procedure depends on digit
     * 8 (index 7): values {@code 1}, {@code 2}, {@code 3} or {@code 6} mean no check is
     * performed ({@link #M09}); all other values delegate to {@link #M06}.
     */
    B0 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int leading = digitAt(account, 0);
            if (leading == 0 || leading == 8) {
                return CheckDigitResult.of(false);
            }
            int eighth = digitAt(account, 7);
            if (eighth == 1 || eighth == 2 || eighth == 3 || eighth == 6) {
                return M09.calculate(blz, account);
            }
            return M06.calculate(blz, account);
        }
    },

    /** Method {@code B1}. Delegates to {@link #M05}, then {@link #M01}, then {@link #M00}. */
    B1 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M05.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            CheckDigitResult second = M01.calculate(blz, account);
            if (second.isValid()) {
                return second;
            }
            return M00.calculate(blz, account);
        }
    },

    /** Method {@code B2}. Delegates to {@link #M02} if digit 1 (index 0) is {@code <= 7}, else {@link #M00}. */
    B2 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 0) <= 7 ? M02.calculate(blz, account) : M00.calculate(blz, account);
        }
    },

    /** Method {@code B3}. Delegates to {@link #M32} unless digit 1 (index 0) is {@code 9}, in which case {@link #M06}. */
    B3 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 0) != 9 ? M32.calculate(blz, account) : M06.calculate(blz, account);
        }
    },

    /**
     * Method {@code B4}.
     * <p>
     * Delegates to {@link #M00} if digit 1 (index 0) is {@code 9}; otherwise delegates to
     * {@link #M07}. The reference source names "Methode 02" for the latter case, but the
     * weights it explicitly prints ({@code {10,9,8,7,6,5,4,3,2}}) match {@link #M07}, not
     * {@link #M02} ({@code {2,9,8,7,6,5,4,3,2}}) — the explicit weights are followed here,
     * as {@link #B2} confirms elsewhere that a genuine "Methode 02" reference is always
     * paired with {@link #M02}'s own weights.
     */
    B4 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 0) == 9 ? M00.calculate(blz, account) : M07.calculate(blz, account);
        }
    },

    /**
     * Method {@code B5}. Delegates to {@link #M05}; if that fails and digit 1 (index 0)
     * is {@code 8} or {@code 9}, rejects outright; otherwise delegates to {@link #M00}.
     */
    B5 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M05.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            int leading = digitAt(account, 0);
            if (leading == 8 || leading == 9) {
                return CheckDigitResult.of(false);
            }
            return M00.calculate(blz, account);
        }
    },

    /**
     * Method {@code B6}.
     * <p>
     * Variante 1: account numbers with digit 1 (index 0) in {@code 1}-{@code 9}, or with
     * digits 1-5 in {@code 02691}-{@code 02699}, are checked with {@link #M20}. Variante 2:
     * all other account numbers are checked with {@link #M53}, which reads BLZ digits.
     * <p>
     * <strong>Implemented with reservation</strong>: account numbers with two or more
     * leading zeros reach {@link #M53}, which defines only 9-digit account numbers and
     * therefore rejects them.
     */
    B6 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            boolean variante1 = digitAt(account, 0) != 0
                || (digitAt(account, 1) == 2 && digitAt(account, 2) == 6 && digitAt(account, 3) == 9
                    && digitAt(account, 4) != 0);
            return variante1 ? M20.calculate(blz, account) : M53.calculate(blz, account);
        }
    },

    /**
     * Method {@code B7}.
     * <p>
     * Accounts within {@code [1000000, 5999999]} or {@code [700000000, 899999999]} are
     * checked with {@link #M01}; all other accounts are not checked ({@link #M09}).
     */
    B7 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            long value = toLong(account);
            if ((value >= 1_000_000L && value <= 5_999_999L)
                || (value >= 700_000_000L && value <= 899_999_999L)) {
                return M01.calculate(blz, account);
            }
            return M09.calculate(blz, account);
        }
    },

    /**
     * Method {@code B8}.
     * <p>
     * Modulus 11, weights {@code {3,9,8,7,6,5,4,3,2}} (same as {@link #M20}),
     * clamp-above-9 rule. If that fails, delegates to {@link #M29}. If that also fails,
     * the account is accepted with no further check ({@link CheckDigitResult#NOT_CHECKED})
     * when it falls into one of two documented ranges: {@code [5100000000, 5999999999]}
     * (digits 1–2 {@code 51}–{@code 59}) or {@code [9010000000, 9109999999]} (digits 1–3
     * {@code 901}–{@code 910}).
     */
    B8 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_398765432, 0, false);
            CheckDigitResult first = compareToCheckDigit(account, mod11ClampAboveNine(sum));
            if (first.isValid()) {
                return first;
            }
            CheckDigitResult second = M29.calculate(blz, account);
            if (second.isValid()) {
                return second;
            }
            int leading2 = digitAt(account, 0) * 10 + digitAt(account, 1);
            int leading3 = leading2 * 10 + digitAt(account, 2);
            if ((leading2 >= 51 && leading2 <= 59) || (leading3 >= 901 && leading3 <= 910)) {
                return CheckDigitResult.NOT_CHECKED;
            }
            return CheckDigitResult.of(false);
        }
    },

    /**
     * Method {@code B9}.
     * <p>
     * If digits 1–2 (index 0–1) are zero and digit 3 (index 2) is positive: for each of
     * digits 3–9 (index 2–8, weights {@code {1,2,3,1,2,3,1}} cyclic), accumulates
     * {@code (weight × digit + weight) % 11} into a running sum; check digit =
     * {@code sum % 10}. If that does not match, adds {@code 5} (wrapping at {@code 10}) and
     * compares again.
     * <p>
     * If digits 1–3 are all zero: modulus 11 (no complement — the raw remainder is the
     * check digit directly), weights {@code {6,5,4,3,2,1}} over digits 4–9 (index 3–8). If
     * that does not match, the same {@code +5} wrap-around retry applies.
     * <p>
     * Otherwise the account is rejected outright.
     */
    B9 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 0 && digitAt(account, 1) == 0 && digitAt(account, 2) > 0) {
                int sum = computeB9Sum(account, 2, WEIGHTS_B9A);
                int crc = sum % MODULUS_10;
                if (digitAt(account, 9) == crc) {
                    return CheckDigitResult.of(true);
                }
                return compareToCheckDigit(account, wrapPlusFive(crc));
            }
            if (digitAt(account, 0) == 0 && digitAt(account, 1) == 0 && digitAt(account, 2) == 0) {
                int sum = weightedSum(account, WEIGHTS_M71, 3, false);
                int crc = sum % MODULUS_11;
                if (digitAt(account, 9) == crc) {
                    return CheckDigitResult.of(true);
                }
                return compareToCheckDigit(account, wrapPlusFive(crc));
            }
            return CheckDigitResult.of(false);
        }
    },

    /**
     * Method {@code C0}.
     * <p>
     * Account numbers with exactly two leading zeros are checked with {@link #M52}
     * (Variante 1, reads BLZ digits); if that fails, with {@link #M20} (Variante 2). All
     * other account numbers are checked with {@link #M20} only.
     */
    C0 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 0 && digitAt(account, 1) == 0 && digitAt(account, 2) != 0) {
                CheckDigitResult first = M52.calculate(blz, account);
                if (first.isValid()) {
                    return first;
                }
            }
            return M20.calculate(blz, account);
        }
    },

    /**
     * Method {@code C1}.
     * <p>
     * Delegates to {@link #M17} unless digit 1 (index 0) is {@code 5}, in which case:
     * modulus 10 with cross sum, weights {@code {1,2,1,2,1,2,1,2,1}}, sum decremented by
     * one, then {@code (10 − sum % 11) % 10} (the same asymmetric modulus-10/11 mix as
     * {@link #M17}), compared at index 9.
     */
    C1 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) != 5) {
                return M17.calculate(blz, account);
            }
            int sum = weightedSum(account, WEIGHTS_M57A, 0, true);
            sum--;
            int crc = (MODULUS_10 - sum % MODULUS_11) % MODULUS_10;
            return compareToCheckDigit(account, crc);
        }
    },

    /** Method {@code C2}. Delegates to {@link #M22}, then {@link #M00}, then {@link #M04}. */
    C2 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M22.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            CheckDigitResult second = M00.calculate(blz, account);
            if (second.isValid()) {
                return second;
            }
            return M04.calculate(blz, account);
        }
    },

    /** Method {@code C3}. Delegates to {@link #M00} unless digit 1 (index 0) is {@code 9}, then {@link #M58}. */
    C3 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 0) != 9 ? M00.calculate(blz, account) : M58.calculate(blz, account);
        }
    },

    /** Method {@code C4}. Delegates to {@link #M15} unless digit 1 (index 0) is {@code 9}, then {@link #M58}. */
    C4 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return digitAt(account, 0) != 9 ? M15.calculate(blz, account) : M58.calculate(blz, account);
        }
    },

    /**
     * Method {@code C5}.
     * <p>
     * A range-dispatching method: 6-digit account numbers in the range {@code 100000}–
     * {@code 899999} use modulus 10 with cross sum, weights {@code {2,1,2,1,2}}, compared
     * at index 9; 9-digit account numbers in the analogous range use the same weights
     * compared at index 6. Leading digit {@code 1}, {@code 4}, {@code 5}, {@code 6} or
     * {@code 9} delegates to {@link #M29}; leading digit {@code 3} delegates to
     * {@link #M00}; a small number of further documented ranges accept with no check
     * ({@link CheckDigitResult#NOT_CHECKED}). Anything else is rejected.
     */
    C5 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 0 && digitAt(account, 1) == 0
                && digitAt(account, 2) == 0 && digitAt(account, 3) == 0) {
                if (digitAt(account, 4) < 1 || digitAt(account, 4) > 8) {
                    return CheckDigitResult.of(false);
                }
                int sum = weightedSum(account, WEIGHTS_C5, 4, true);
                return compareToCheckDigit(account, mod10Complement(sum));
            }
            if (digitAt(account, 0) == 0 && digitAt(account, 1) > 0) {
                if (digitAt(account, 1) > 8) {
                    return CheckDigitResult.of(false);
                }
                int sum = weightedSum(account, WEIGHTS_C5, 1, true);
                return compareAt(account, 6, mod10Complement(sum));
            }

            int leading = digitAt(account, 0);
            if (leading == 1 || leading == 4 || leading == 5 || leading == 6 || leading == 9) {
                return M29.calculate(blz, account);
            }
            if (leading == 3) {
                return M00.calculate(blz, account);
            }
            if (digitAt(account, 0) == 0 && digitAt(account, 1) == 0
                && digitAt(account, 2) >= 3 && digitAt(account, 2) <= 5) {
                return M09.calculate(blz, account);
            }
            if ((leading == 7 && digitAt(account, 1) == 0) || (leading == 8 && digitAt(account, 1) == 5)) {
                return M09.calculate(blz, account);
            }
            return CheckDigitResult.of(false);
        }
    },

    /**
     * Method {@code C6}.
     * <p>
     * Prepends a 7-digit constant — selected from {@link #C6_CONSTANTS} by digit 1
     * (index 0) — to digits 2–9 (index 1–8), forming a synthetic 15-digit number. Applies
     * the {@link #M00} formula (modulus 10, weights {@code {2,1,...,2}}, cross sum) to
     * those 15 digits; the check digit remains at index 9 of the original account number.
     */
    C6 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            return calculateC6(account);
        }
    },

    /**
     * Method {@code C7}.
     * <p>
     * <strong>Implemented with reservation</strong> — the Bundesbank specification calls
     * for "method 6, modified" here without documenting what the modification actually
     * is, a known open question flagged in the reference source. Since its own test suite
     * passes with the <em>unmodified</em> {@link #M06} formula, that is what is replicated
     * here as well: delegates to {@link #M63}; if that fails, falls back to {@link #M06}.
     */
    C7 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M63.calculate(blz, account);
            return first.isValid() ? first : M06.calculate(blz, account);
        }
    },

    /** Method {@code C8}. Delegates to {@link #M00}, then {@link #M04}, then {@link #M07}. */
    C8 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            CheckDigitResult second = M04.calculate(blz, account);
            if (second.isValid()) {
                return second;
            }
            return M07.calculate(blz, account);
        }
    },

    /** Method {@code C9}. Delegates to {@link #M00}; if that fails, to {@link #M07}. */
    C9 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            return first.isValid() ? first : M07.calculate(blz, account);
        }
    },

    /**
     * Method {@code D0}.
     * <p>
     * No check is performed ({@link CheckDigitResult#NOT_CHECKED}) if digits 1–2
     * (index 0–1) are {@code 5}, {@code 7}. Otherwise: modulus 11, weights
     * {@code {3,9,8,7,6,5,4,3,2}} (same as {@link #M20}), clamp-above-9 rule.
     */
    D0 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 5 && digitAt(account, 1) == 7) {
                return CheckDigitResult.NOT_CHECKED;
            }
            int sum = weightedSum(account, WEIGHTS_398765432, 0, false);
            return compareToCheckDigit(account, mod11ClampAboveNine(sum));
        }
    },

    /**
     * Method {@code D1}.
     * <p>
     * Rejects if digit 1 (index 0) is {@code 8}. Otherwise builds a fixed 15-element
     * sequence: a constant prefix {@code {4,3,6,3,3,8}}, followed by digit 1 (index 0),
     * followed by digits 2–9 (index 1–8); each element is multiplied by an alternating
     * weight ({@code 2,1,2,1,…}, starting at {@code 2}), and the cross sum of each product
     * is accumulated. Check digit = {@code 10 − sum % 10}, compared at index 9.
     */
    D1 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) == 8) {
                return CheckDigitResult.of(false);
            }
            int[] sequence = new int[15];
            sequence[0] = 4;
            sequence[1] = 3;
            sequence[2] = 6;
            sequence[3] = 3;
            sequence[4] = 3;
            sequence[5] = 8;
            sequence[6] = digitAt(account, 0);
            for (int i = 0; i < 8; i++) {
                sequence[i + 7] = digitAt(account, i + 1);
            }

            int sum = 0;
            for (int i = 0; i < 15; i++) {
                int weight = i % 2 == 0 ? 2 : 1;
                int product = sequence[i] * weight;
                sum += crossSum(product);
            }
            return compareToCheckDigit(account, MODULUS_10 - sum % MODULUS_10);
        }
    },

    /** Method {@code D2}. Delegates to {@link #M95}, then {@link #M00}, then {@link #M68}. */
    D2 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M95.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            CheckDigitResult second = M00.calculate(blz, account);
            if (second.isValid()) {
                return second;
            }
            return M68.calculate(blz, account);
        }
    },

    /** Method {@code D3}. Delegates to {@link #M00}; if that fails, to {@link #M27}. */
    D3 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            return first.isValid() ? first : M27.calculate(blz, account);
        }
    },

    /** Method {@code D6}. Delegates to {@link #M07}, then {@link #M03}, then {@link #M00}. */
    D6 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M07.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            CheckDigitResult second = M03.calculate(blz, account);
            if (second.isValid()) {
                return second;
            }
            return M00.calculate(blz, account);
        }
    },

    /**
     * Method {@code D7}.
     * <p>
     * Modulus 10, weights {@code {2,1,2,1,2,1,2,1,2}} applied to digits 1-9 (index 0-8),
     * products greater than 9 reduced to their cross sum (as in {@link #M00}). Unlike
     * {@link #M00}, the check digit is the units digit of the sum itself
     * ({@code sum % 10}), not its complement.
     */
    D7 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_2121, 0, true);
            return compareToCheckDigit(account, sum % MODULUS_10);
        }
    },

    /**
     * Method {@code D8}.
     * <p>
     * Selects the variant by account number range: {@code 1000000000} to
     * {@code 9999999999} delegates to {@link #M00}; {@code 0010000000} to
     * {@code 0099999999} delegates to {@link #M09} (not checked). Account numbers
     * outside both ranges are invalid.
     */
    D8 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            long value = toLong(account);
            if (value >= 1_000_000_000L) {
                return M00.calculate(blz, account);
            }
            if (value >= 10_000_000L && value <= 99_999_999L) {
                return M09.calculate(blz, account);
            }
            return CheckDigitResult.of(false);
        }
    },

    /** Method {@code D9}. Delegates to {@link #M00}, then {@link #M10}, then {@link #M18}. */
    D9 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            CheckDigitResult first = M00.calculate(blz, account);
            if (first.isValid()) {
                return first;
            }
            CheckDigitResult second = M10.calculate(blz, account);
            if (second.isValid()) {
                return second;
            }
            return M18.calculate(blz, account);
        }
    },

    /**
     * Method {@code E0}.
     * <p>
     * Same as {@link #M00} (modulus 10, weights {@code {2,1,2,1,2,1,2,1,2}}, cross sum),
     * except that the constant {@code 7} is added to the sum before the complement
     * {@code (10 - sum % 10) % 10} is taken.
     */
    E0 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = weightedSum(account, WEIGHTS_2121, 0, true) + 7;
            return compareToCheckDigit(account, mod10Complement(sum));
        }
    },

    /**
     * Method {@code E1}.
     * <p>
     * Modulus 11, weights {@code {9,10,11,6,5,4,3,2,1}} applied to the ASCII values
     * ({@code 48} to {@code 57}) of digits 1-9 (index 0-8), not to the digits
     * themselves. Check digit = {@code sum % 11} (no complement); a remainder of
     * {@code 10} never matches, so the account number is invalid.
     */
    E1 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            int sum = 0;
            for (int i = 0; i < WEIGHTS_E1.length; i++) {
                // the char value of a digit is its ASCII code
                sum += account[i] * WEIGHTS_E1[i];
            }
            return compareToCheckDigit(account, sum % MODULUS_11);
        }
    },

    /**
     * Method {@code E2}.
     * <p>
     * Invalid if digit 1 (index 0) is {@code 6} to {@code 9}. Otherwise prepends the
     * constant {@code 438320x} ({@code x} = digit 1) to digits 2-9 (index 1-8) and
     * applies the {@link #M00} formula to those 15 digits, as {@link #C6} does; the
     * check digit is at index 9.
     */
    E2 {
        @Override
        CheckDigitResult calculate(char[] blz, char[] account) {
            if (digitAt(account, 0) > 5) {
                return CheckDigitResult.of(false);
            }
            return prefixedM00(account, "438320" + account[0]);
        }
    };

    /** Number of digits in a normalized German domestic account number ("Kontonummer"). */
    static final int ACCOUNT_LENGTH = 10;

    /** Index of the check digit within a normalized account number, for the (common) methods that use it. */
    private static final int CHECK_DIGIT_INDEX = ACCOUNT_LENGTH - 1;

    private static final int MODULUS_5  = 5;
    private static final int MODULUS_7  = 7;
    private static final int MODULUS_9  = 9;
    private static final int MODULUS_10 = 10;
    private static final int MODULUS_11 = 11;
    private static final int MAX_DIGIT  = 9;

    /** Method {@link #M08} verifies only account numbers whose numeric value reaches this threshold. */
    private static final long METHOD_08_THRESHOLD = 60_000L;

    /** The historically-documented single valid exception account number for {@link #M57}. */
    private static final char[] M57_EXCEPTION_ACCOUNT = "0185125434".toCharArray();

    /**
     * Shared digit-substitution table used by {@link #M27}, {@link #M29} and {@link #M69}.
     * Each row is a 10-entry lookup (index 0–9) mapping an account digit to its
     * transformed value.
     */
    private static final int[][] TRANSFORM_TABLE = {
        {0, 1, 5, 9, 3, 7, 4, 8, 2, 6},
        {0, 1, 7, 6, 9, 8, 3, 2, 5, 4},
        {0, 1, 8, 4, 6, 2, 9, 5, 7, 3},
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
    };

    private static final int[] WEIGHTS_2121        = {2, 1, 2, 1, 2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_173         = {1, 7, 3, 1, 7, 3, 1, 7, 3};
    private static final int[] WEIGHTS_137         = {1, 3, 7, 1, 3, 7, 1, 3, 7};
    private static final int[] WEIGHTS_298765432   = {2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_432765432   = {4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_109876543   = {10, 9, 8, 7, 6, 5, 4, 3, 2};

    private static final int[] WEIGHTS_765432      = {7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_5432        = {5, 4, 3, 2};
    private static final int[] WEIGHTS_65432       = {6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_121212      = {1, 2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_317931793   = {3, 1, 7, 9, 3, 1, 7, 9, 3};
    private static final int[] WEIGHTS_198765432   = {1, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_398765432   = {3, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_313131313   = {3, 1, 3, 1, 3, 1, 3, 1, 3};
    private static final int[] WEIGHTS_98765432    = {9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_2765432     = {2, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_8765432     = {8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_M24         = {1, 2, 3};
    private static final int[] WEIGHTS_M30         = {2, 0, 0, 0, 0, 1, 2, 1, 2};
    private static final int[] WEIGHTS_123456789   = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] WEIGHTS_M34         = {7, 9, 10, 5, 8, 4, 2};
    private static final int[] WEIGHTS_5842        = {5, 8, 4, 2};
    private static final int[] WEIGHTS_M37         = {10, 5, 8, 4, 2};
    private static final int[] WEIGHTS_M38         = {9, 10, 5, 8, 4, 2};
    private static final int[] WEIGHTS_M40         = {6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final int[] WEIGHTS_M43         = {9, 8, 7, 6, 5, 4, 3, 2, 1};
    private static final int[] WEIGHTS_M44         = {0, 0, 0, 0, 10, 5, 8, 4, 2};
    private static final int[] WEIGHTS_M54         = {2, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_M55         = {8, 7, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_M57A        = {1, 2, 1, 2, 1, 2, 1, 2, 1};
    private static final int[] WEIGHTS_M57B        = {1, 2, 0, 1, 2, 1, 2, 1, 2, 1};
    private static final int[] WEIGHTS_M58         = {0, 0, 0, 0, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_M60         = {2, 1, 2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_M61_LONG    = {2, 1, 2, 1, 2, 1, 2, 0, 1, 2};
    private static final int[] WEIGHTS_M64         = {9, 10, 5, 8, 4, 2};
    private static final int[] WEIGHTS_M66         = {7, 0, 0, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_M68A        = {1, 2, 1, 2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_M68B        = {1, 0, 0, 2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_M71         = {6, 5, 4, 3, 2, 1};
    private static final int[] WEIGHTS_M73         = {2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_M91B        = {2, 3, 4, 5, 6, 7};
    private static final int[] WEIGHTS_M91C        = {10, 9, 8, 7, 6, 5, 0, 4, 3, 2};
    private static final int[] WEIGHTS_M92         = {1, 7, 3, 1, 7, 3};
    private static final int[] WEIGHTS_M98         = {3, 7, 1, 3, 7, 1, 3};
    private static final int[] WEIGHTS_A0          = {10, 5, 8, 4, 2};
    private static final int[] WEIGHTS_A1          = {0, 0, 2, 1, 2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_B9A         = {1, 2, 3, 1, 2, 3, 1};
    private static final int[] WEIGHTS_C5          = {2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_C6          = {2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2};
    private static final int[] WEIGHTS_E1          = {9, 10, 11, 6, 5, 4, 3, 2, 1};
    private static final int[] M87_TAB1            = {0, 4, 3, 2, 6};
    private static final int[] M87_TAB2            = {7, 1, 5, 9, 8};

    /** Weights of the ESER methods {@link #M52} and {@link #M53}, from right to left. */
    private static final int[] WEIGHTS_ESER        = {2, 4, 8, 5, 10, 9, 7, 3, 6, 1, 2, 4};

    /** Number of leading ESER digits up to and including the check digit. */
    private static final int ESER_HEAD_LENGTH = 6;

    /** Remainder the ESER factor search must reach. */
    private static final int ESER_TARGET_REMAINDER = 10;

    /**
     * {@link #C6} prepends one of these 7-digit constants — selected by digit 1 (index 0)
     * of the account number — to digits 2–9 before running the modulus-10 calculation.
     */
    private static final String[] C6_CONSTANTS = {
        "4451970", "4451981", "4451992", "4451993", "4344992",
        "4344990", "4344991", "5499570", "4451994", "5499579",
    };

    private static final Map<String, GermanCheckDigitMethod> BY_CODE = new LinkedHashMap<>();

    static {
        for (GermanCheckDigitMethod method : values()) {
            BY_CODE.put(method.getCode(), method);
        }
    }

    /**
     * Returns the two-character Bundesbank method code for this constant, e.g.
     * {@code "00"} for {@link #M00}, or {@code "A0"} for {@link #A0}.
     *
     * @return the method code
     */
    public String getCode() {
        String n = name();
        return n.startsWith("M") ? n.substring(1) : n;
    }

    /**
     * Looks up a method by its Bundesbank method code.
     *
     * @param code the two-character method code, e.g. {@code "00"}; must not be {@code null}
     * @return the matching method
     * @throws NullPointerException     if {@code code} is {@code null}
     * @throws IllegalArgumentException if {@code code} does not identify a known,
     *         implemented method
     */
    public static GermanCheckDigitMethod fromCode(String code) {
        requireNonNull(code, "code must not be null");
        GermanCheckDigitMethod method = BY_CODE.get(code);
        if (method == null) {
            throw new IllegalArgumentException("Unknown or not-yet-implemented check digit method: " + code);
        }
        return method;
    }

    /**
     * Computes and verifies the check digit for the given BLZ / account number pair.
     *
     * @param blz     the 8-digit Bankleitzahl as a normalized {@code char[]}
     * @param account the 10-digit account number as a normalized {@code char[]}
     * @return the verification outcome
     */
    abstract CheckDigitResult calculate(char[] blz, char[] account);

    /**
     * Computes the weighted digit sum over {@code weights.length} digits of
     * {@code account}, starting at {@code offset}.
     *
     * @param account  the normalized account number
     * @param weights  the per-position weights, left to right
     * @param offset   the index of the first digit the weights apply to
     * @param crossSum if {@code true}, any product greater than {@value #MAX_DIGIT} is
     *                 replaced by its cross sum ({@code product - 9}); valid as long as no
     *                 product exceeds 18, which holds for all weights used here
     * @return the weighted sum
     */
    private static int weightedSum(char[] account, int[] weights, int offset, boolean crossSum) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            int product = digitAt(account, offset + i) * weights[i];
            sum += crossSum && product > MAX_DIGIT ? product - MAX_DIGIT : product;
        }
        return sum;
    }

    /** {@code (10 - sum % 10) % 10} — the standard Modulus-10 check digit complement. */
    private static int mod10Complement(int sum) {
        return (MODULUS_10 - sum % MODULUS_10) % MODULUS_10;
    }

    /** {@code (11 - sum % 11) % 11} — the standard Modulus-11 check digit complement ("11 -> 0" rule). */
    private static int mod11Complement(int sum) {
        return (MODULUS_11 - sum % MODULUS_11) % MODULUS_11;
    }

    /** {@code 11 - sum % 11}, with any result greater than 9 (i.e. 10 or 11) collapsed to 0. */
    private static int mod11ClampAboveNine(int sum) {
        int remainder = MODULUS_11 - sum % MODULUS_11;
        return remainder > MAX_DIGIT ? 0 : remainder;
    }

    /** {@code (7 - sum % 7) % 7} — the Modulus-7 check digit complement used by a small number of methods. */
    private static int mod7Complement(int sum) {
        return (MODULUS_7 - sum % MODULUS_7) % MODULUS_7;
    }

    /** {@code (9 - sum % 9) % 9} — the Modulus-9 check digit complement used by {@link #M90}. */
    private static int mod9Complement(int sum) {
        return (MODULUS_9 - sum % MODULUS_9) % MODULUS_9;
    }

    /**
     * Compares an expected check digit value against the digit actually present at
     * {@link #CHECK_DIGIT_INDEX}. Expected values outside {@code [0, 9]} (i.e. {@code 10}
     * or {@code 11}) correctly never match, since {@link #digitAt} can only return a
     * single decimal digit.
     */
    private static CheckDigitResult compareToCheckDigit(char[] account, int expected) {
        return compareAt(account, CHECK_DIGIT_INDEX, expected);
    }

    /**
     * Compares an expected check digit value against the digit actually present at
     * {@code index}. Expected values outside {@code [0, 9]} correctly never match.
     */
    private static CheckDigitResult compareAt(char[] account, int index, int expected) {
        return CheckDigitResult.of(digitAt(account, index) == expected);
    }

    private static int digitAt(char[] account, int index) {
        return account[index] - '0';
    }

    /**
     * Shared calculation for {@link #M93}, also used directly by {@link #A4}'s Variante 4.
     * <p>
     * If digits 1–4 (index 0–3) are all {@code 0}, the 5-digit Kundennummer occupies
     * digits 5–9 (index 4–8) and the check digit is at index 9; otherwise the
     * Kundennummer occupies digits 1–5 (index 0–4) and the check digit is at index 5.
     * Modulus 11, weights {@code {6,5,4,3,2}} (same as {@link #M33}), clamp-above-9 rule;
     * if that fails, retries with modulus 7: {@code (7 − sum % 7) % 7}.
     */
    private static CheckDigitResult calculateM93(char[] account) {
        boolean fallB = digitAt(account, 0) == 0 && digitAt(account, 1) == 0
            && digitAt(account, 2) == 0 && digitAt(account, 3) == 0;
        int offset     = fallB ? 4 : 0;
        int checkIndex = fallB ? 9 : 5;
        int sum        = weightedSum(account, WEIGHTS_65432, offset, false);
        CheckDigitResult mod11 = compareAt(account, checkIndex, mod11ClampAboveNine(sum));
        if (mod11.isValid()) {
            return mod11;
        }
        return compareAt(account, checkIndex, mod7Complement(sum));
    }

    /**
     * Shared calculation for {@link #C6}: builds a synthetic 15-digit number by
     * prepending a digit-1-dependent 7-digit constant (see {@link #C6_CONSTANTS}) to
     * digits 2–9 (index 1–8) of the account, then applies the modulus-10 formula used by
     * {@link #M00} (weights {@code {2,1,...,2}}, cross sum) to those 15 digits.
     */
    private static CheckDigitResult calculateC6(char[] account) {
        return prefixedM00(account, C6_CONSTANTS[digitAt(account, 0)]);
    }

    /**
     * Prepends a 7-digit {@code constant} to digits 2-9 (index 1-8) and applies the
     * {@link #M00} formula to the resulting 15 digits. Used by {@link #C6} and {@link #E2}.
     */
    private static CheckDigitResult prefixedM00(char[] account, String constant) {
        char[]  digits   = new char[15];
        constant.getChars(0, 7, digits, 0);
        System.arraycopy(account, 1, digits, 7, 8);
        int sum = weightedSum(digits, WEIGHTS_C6, 0, true);
        return compareToCheckDigit(account, mod10Complement(sum));
    }

    /**
     * Method A of {@link #M87}, translated from the Bundesbank pseudo code. {@code i} is
     * the 1-based digit position of the specification; {@code konto[i]} may become
     * {@code 10}.
     */
    private static CheckDigitResult calculateM87A(char[] account) {
        int[] konto = new int[ACCOUNT_LENGTH + 1];
        for (int k = 1; k <= ACCOUNT_LENGTH; k++) {
            konto[k] = digitAt(account, k - 1);
        }
        int i = 4;
        while (konto[i] == 0 && i < ACCOUNT_LENGTH) {
            i++;
        }
        int c2 = i % 2;
        int d2 = 0;
        int a5 = 0;
        while (i < ACCOUNT_LENGTH) {
            switch (konto[i]) {
                case 0:
                    konto[i] = 5;
                    break;
                case 1:
                    konto[i] = 6;
                    break;
                case 5:
                    konto[i] = 10;
                    break;
                case 6:
                    konto[i] = 1;
                    break;
                default:
                    break;
            }
            if (c2 == d2) {
                if (konto[i] > 5) {
                    if (c2 == 0 && d2 == 0) {
                        c2 = 1;
                        d2 = 1;
                        a5 = a5 + 6 - (konto[i] - 6);
                    } else {
                        c2 = 0;
                        d2 = 0;
                        a5 = a5 + konto[i];
                    }
                } else {
                    c2 = c2 == 0 && d2 == 0 ? 1 : 0;
                    a5 = a5 + konto[i];
                }
            } else {
                if (konto[i] > 5) {
                    if (c2 == 0) {
                        c2 = 1;
                        d2 = 0;
                        a5 = a5 - 6 + (konto[i] - 6);
                    } else {
                        c2 = 0;
                        d2 = 1;
                        a5 = a5 - konto[i];
                    }
                } else {
                    c2 = c2 == 0 ? 1 : 0;
                    a5 = a5 - konto[i];
                }
            }
            i++;
        }
        while (a5 < 0 || a5 > 4) {
            a5 = a5 > 4 ? a5 - MODULUS_5 : a5 + MODULUS_5;
        }
        int p = d2 == 0 ? M87_TAB1[a5] : M87_TAB2[a5];
        if (p == konto[ACCOUNT_LENGTH]) {
            return CheckDigitResult.of(true);
        }
        if (konto[4] == 0) {
            p = p > 4 ? p - MODULUS_5 : p + MODULUS_5;
            return CheckDigitResult.of(p == konto[ACCOUNT_LENGTH]);
        }
        return CheckDigitResult.of(false);
    }

    private static long toLong(char[] account) {
        long value = 0;
        for (char c : account) {
            value = value * MODULUS_10 + (c - '0');
        }
        return value;
    }

    /** Non-recursive cross sum of a non-negative integer (sum of its decimal digits). */
    private static int crossSum(int value) {
        int x   = value;
        int sum = 0;
        while (x > 0) {
            sum += x % MODULUS_10;
            x   /= MODULUS_10;
        }
        return sum;
    }

    /** Cross sum applied repeatedly until the result is a single digit. Used by {@link #M21}. */
    private static int recursiveCrossSum(int value) {
        int sum = crossSum(value);
        while (sum >= MODULUS_10) {
            sum = crossSum(sum);
        }
        return sum;
    }

    // Method-specific helpers

    /** Effective (possibly substituted) digits 1-9 used by {@link #M24}, as a non-mutating copy. */
    private static int[] m24EffectiveDigits(char[] account) {
        int[] digits = new int[9];
        for (int i = 0; i < 9; i++) {
            digits[i] = digitAt(account, i);
        }
        switch (digits[0]) {
            case 3:
            case 4:
            case 5:
            case 6:
                digits[0] = 0;
                break;
            case 9:
                digits[0] = 0;
                digits[1] = 0;
                digits[2] = 0;
                break;
            default:
                break;
        }
        return digits;
    }

    /** One attempt of the {@link #M50} formula: modulus 11, weights {7,6,5,4,3,2} over index 0-5, clamp-above-9, compared at index 6. */
    private static CheckDigitResult m50Attempt(char[] account) {
        int sum = weightedSum(account, WEIGHTS_765432, 0, false);
        return compareAt(account, 6, mod11ClampAboveNine(sum));
    }

    /** One attempt of {@link #M76}: Kontoart at {@code kontoartIndex}, Stammnummer in the next six digits, check digit after it. */
    private static CheckDigitResult m76Attempt(char[] account, int kontoartIndex) {
        int kontoart = digitAt(account, kontoartIndex);
        if (kontoart == 1 || kontoart == 2 || kontoart == 3 || kontoart == 5) {
            return CheckDigitResult.of(false);
        }
        int sum = weightedSum(account, WEIGHTS_765432, kontoartIndex + 1, false);
        return compareAt(account, kontoartIndex + 7, sum % MODULUS_11);
    }

    /** Accumulator formula shared by {@link #M91}... not applicable; kept private to {@link #B9}. */
    private static int computeB9Sum(char[] account, int offset, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            int contribution = digitAt(account, offset + i) * weights[i];
            contribution += weights[i];
            contribution %= MODULUS_11;
            sum          += contribution;
        }
        return sum;
    }

    /**
     * Shared check of the ESER methods {@link #M52} and {@link #M53}.
     * <p>
     * The ESER account number is {@code head} (six digits, the check digit last) followed
     * by account digits 5-10 (index 4-9) without leading zeros. Its digits are weighted
     * from right to left with {@link #WEIGHTS_ESER}, the check digit counting as 0. The
     * expected check digit is the factor 0-9 for which {@code sum % 11} plus the factor
     * times the weight over the check digit leaves remainder 10 when divided by 11. If
     * no factor reaches remainder 10, the account number cannot be used.
     */
    private static CheckDigitResult eserCheck(char[] head, char[] account) {
        int start = 4;
        while (start < ACCOUNT_LENGTH && account[start] == '0') {
            start++;
        }
        int    length = ESER_HEAD_LENGTH + ACCOUNT_LENGTH - start;
        char[] eser   = new char[length];
        System.arraycopy(head, 0, eser, 0, ESER_HEAD_LENGTH);
        System.arraycopy(account, start, eser, ESER_HEAD_LENGTH, ACCOUNT_LENGTH - start);

        int checkIndex = ESER_HEAD_LENGTH - 1;
        int sum        = 0;
        for (int i = 0; i < length; i++) {
            if (i != checkIndex) {
                sum += digitAt(eser, i) * WEIGHTS_ESER[length - 1 - i];
            }
        }
        int remainder   = sum % MODULUS_11;
        int checkWeight = WEIGHTS_ESER[length - 1 - checkIndex];
        for (int factor = 0; factor <= MAX_DIGIT; factor++) {
            if ((remainder + factor * checkWeight) % MODULUS_11 == ESER_TARGET_REMAINDER) {
                return CheckDigitResult.of(digitAt(eser, checkIndex) == factor);
            }
        }
        return CheckDigitResult.of(false);
    }

    /** Adds 5 to a single-digit value, wrapping back into {@code [0, 9]}. Used by {@link #B9}. */
    private static int wrapPlusFive(int value) {
        int wrapped = value + 5;
        return wrapped > 9 ? wrapped - 10 : wrapped;
    }

}
