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

import de.speedbanking.util.Mod97;
import de.speedbanking.util.UtilityClasses;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Container class for all {@link NationalCheckDigitCalculator} implementations.
 * <p>
 * Contains all National Check Digit (NCD) algorithms used in IBAN structures. Each algorithm
 * is implemented once and shared by all countries using it, e.g. the French Cle RIB by France,
 * Monaco, Tunisia and several West-African countries. The calculator for a country is
 * obtained via {@link #forCountry(IbanRegistry)}, which passes the country to the calculator's
 * constructor.
 * <p>
 * The country-specific algorithms are static methods named after the country defining them,
 * e.g. {@link #fr(NcdCalculatorBase, char[])}, and run inside an {@link AlgorithmNcdCalculator}.
 * Static methods instead of one class per algorithm keep the jar small.
 *
 * <h3>Class hierarchy</h3>
 * <pre>
 * NationalCheckDigitCalculator            (interface)
 * └── NcdCalculatorBase                   (abstract, holds countryData, default validate)
 *     ├── AlgorithmNcdCalculator          (delegates to al, ao, be, ee, es, fi, fr, ...)
 *     ├── NoOpNcdCalculator               (no NCD validation, passthrough)
 *     └── Mod97RemainderOneNcdCalculator  (ISO 7064 MOD 97-10, remainder == 1)
 * </pre>
 *
 * @since 1.8.5
 *
 * @see NationalCheckDigitCalculator
 * @see NcdCalculatorBase
 * @see Mod97RemainderOneNcdCalculator
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
final class NationalCheckDigitCalculators {

    /** IBAN index at which the BBAN starts (always 4: country code + 2 check digits). */
    private static final int BBAN_START               = IbanRegistry.INDEX_BBAN;

    /** Modulus used by ISO 7064 MOD 97-10 algorithms. */
    private static final int MODULO_BASE              = 97;

    /**
     * Magic constant for the MOD 97-10 NCD formula: {@code NCD = 98 − remainder}.
     * Equals {@code MODULO_BASE + 1}.
     */
    private static final int CHECK_DIGIT_MAGIC_NUMBER = 98;

    /** Cache for single digits '0' through '9' as arrays. */
    private static final char[][] DIGIT_CACHE =
        IntStream.range(0, 10)
            .mapToObj(i -> new char[] {(char) ('0' + i)})
            .toArray(char[][]::new);

    /** Cache for two-digit character arrays "00" through "99". */
    private static final char[][] TWO_DIGIT_CACHE =
        IntStream.range(0, 100)
            .mapToObj(i -> new char[] {
                (char) ('0' + (i / 10)),
                (char) ('0' + (i % 10))
            })
            .toArray(char[][]::new);

    /** Cache for single uppercase letters 'A' through 'Z' as arrays. */
    private static final char[][] LETTER_CACHE =
        IntStream.range(0, 26)
            .mapToObj(i -> new char[] {(char) ('A' + i)})
            .toArray(char[][]::new);

    /** Weights of the Albanian algorithm, see {@link #al(NcdCalculatorBase, char[])}. */
    private static final int[] AL_WEIGHTS = {9, 7, 3, 1, 9, 7, 3};

    /** Weights of the Estonian algorithm, see {@link #ee(NcdCalculatorBase, char[])}. */
    private static final int[] EE_WEIGHTS = {7, 3, 1};

    /** Weights of the Spanish algorithm, see {@link #es(NcdCalculatorBase, char[])}. */
    private static final int[] ES_WEIGHTS = {1, 2, 4, 8, 5, 10, 9, 7, 3, 6};

    /** Weights of the Finnish algorithm, see {@link #fi(NcdCalculatorBase, char[])}. */
    private static final int[] FI_WEIGHTS = {2, 1};

    /** Weights of the Hungarian algorithm, see {@link #hu(NcdCalculatorBase, char[])}. */
    private static final int[] HU_WEIGHTS = {9, 7, 3, 1};

    /** Weights of the Norwegian algorithm, see {@link #no(NcdCalculatorBase, char[])}. */
    private static final int[] NO_WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    /**
     * Lookup table of the Italian algorithm for characters at <em>odd</em> positions (1-based).
     * Index 0-9 covers digits {@code '0'-'9'}; index 10-35 covers letters {@code 'A'-'Z'}.
     */
    private static final int[] IT_ODD_MAP = {
        1, 0, 5, 7, 9, 13, 15, 17, 19, 21, // '0' - '9'
        1, 0, 5, 7, 9, 13, 15, 17, 19, 21, // 'A' - 'J'
        2, 4, 18, 20, 11, 3, 6, 8, 12, 14, // 'K' - 'T'
        16, 10, 22, 25, 24, 23             // 'U' - 'Z'
    };

    /** Calculator factories by base country code. */
    private static final Map<String, Function<IbanRegistry, NationalCheckDigitCalculator>> FACTORIES = createFactories();

    /** Not instantiable. */
    private NationalCheckDigitCalculators() {
        throw UtilityClasses.cannotInstantiate(getClass());
    }

    /**
     * Creates the NCD calculator for the given country.
     *
     * @param countryData the registry entry of a country with an NCD; must not be {@code null}
     * @return a new calculator for the base country of {@code countryData}, never {@code null}
     * @throws IllegalArgumentException if no NCD algorithm is known for the country
     *
     * @since 1.8.13
     */
    static NationalCheckDigitCalculator forCountry(IbanRegistry countryData) {
        IbanRegistry baseCountry = countryData.getBaseCountry();
        Function<IbanRegistry, NationalCheckDigitCalculator> factory = FACTORIES.get(baseCountry.getCountryCode());
        if (factory == null) {
            throw new IllegalArgumentException(String.format("No national check digit algorithm for %s", baseCountry.getCountryCode()));
        }
        return factory.apply(baseCountry);
    }

    /**
     * Maps each base country with an NCD to the factory of its algorithm.
     *
     * @return the factories by base country code
     */
    private static Map<String, Function<IbanRegistry, NationalCheckDigitCalculator>> createFactories() {
        Map<String, Function<IbanRegistry, NationalCheckDigitCalculator>> factories = new HashMap<>();
        register(factories, "AL", NationalCheckDigitCalculators::al, "AL");
        register(factories, "AO", NationalCheckDigitCalculators::ao, "AO");
        register(factories, "BE", NationalCheckDigitCalculators::be, "BE");
        register(factories, "EE", NationalCheckDigitCalculators::ee, "EE");
        register(factories, "ES", NationalCheckDigitCalculators::es, "ES");
        register(factories, "FI", NationalCheckDigitCalculators::fi, "FI");
        register(factories, "HU", NationalCheckDigitCalculators::hu, "HU");
        register(factories, "NO", NationalCheckDigitCalculators::no, "NO");
        register(factories, "IT", NationalCheckDigitCalculators::it, "IT", "SM");
        register(factories, "FR", NationalCheckDigitCalculators::fr, "FR", "DJ", "MC", "TN",
            "BF", "BJ", "CF", "CM", "GQ", "KM", "SN"); // West-African CFA franc zone
        register(factories, Mod97RemainderOneNcdCalculator::new,
            "BA", "ME", "MK", "MZ", "PT", "RS", "SI", "TL", "XK");
        register(factories, NoOpNcdCalculator::new,
            "CG", "CI", "FO", "GW", "MG", "ML", "MR", "NE", "SE", "TD", "TR");
        return factories;
    }

    /**
     * Registers the given factory for all given country codes.
     *
     * @param factories    the map to add to
     * @param factory      the calculator factory
     * @param countryCodes the base country codes using the algorithm
     */
    private static void register(Map<String, Function<IbanRegistry, NationalCheckDigitCalculator>> factories,
        Function<IbanRegistry, NationalCheckDigitCalculator> factory, String... countryCodes) {
        for (String countryCode : countryCodes) {
            factories.put(countryCode, factory);
        }
    }

    /**
     * Registers the given algorithm for all given country codes.
     *
     * @param factories    the map to add to
     * @param name         the algorithm name, i.e. the country defining it
     * @param algorithm    the algorithm
     * @param countryCodes the base country codes using the algorithm
     */
    private static void register(Map<String, Function<IbanRegistry, NationalCheckDigitCalculator>> factories,
        String name, BiFunction<NcdCalculatorBase, char[], char[]> algorithm, String... countryCodes) {
        register(factories, countryData -> new AlgorithmNcdCalculator(countryData, name, algorithm), countryCodes);
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
    static char[] oneDigit(int value) {
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
    static char[] twoDigits(int value) {
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
    static char[] oneLetter(int index) {
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
    static int mod97(char[] data, int offset, int length) {
        return Mod97.calculateRange(data, offset, length);
    }

    /**
     * National Check Digit algorithm for <strong>Albania (AL)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 7n bank/branch + 1n NCD + 16n account = 24 digits (IBAN length 28).
     * <p>
     * <strong>Algorithm:</strong>
     * Weighted MOD 10 applied to the 7-digit bank/branch prefix.
     * Weights: {@code {9, 7, 3, 1, 9, 7, 3}} (cyclic). Result: {@code (10 − sum % 10) % 10}.
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digit
     */
    static char[] al(NcdCalculatorBase c, char[] iban) {
        int s = 0;
        for (int i = 0; i < AL_WEIGHTS.length; i++) {
            s += (iban[BBAN_START + i] - '0') * AL_WEIGHTS[i];
        }
        return oneDigit((10 - s % 10) % 10);
    }

    /**
     * National Check Digit algorithm for <strong>Angola (AO)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 21n prefix + 2n NCD = 23 digits (IBAN length 25).
     * <p>
     * <strong>Algorithm:</strong>
     * ISO 7064 MOD 97-10 variant. The NCD field is masked with {@code "00"}, then
     * {@code mod97} is computed over the BBAN prefix. NCD = {@code 98 − remainder},
     * with {@code 0} treated as {@code 98} to avoid a zero check digit.
     * <p>
     * To avoid unnecessary heap allocation, the masked BBAN is presented to
     * {@link Mod97#calculateRange(CharSequence, int, int)} as a virtual
     * {@link CharSequence} view over the original IBAN - no intermediate {@code char[]}
     * is allocated.
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digits
     */
    static char[] ao(NcdCalculatorBase c, char[] iban) {
        int prefixLen = c.ncdComponent.getBeginIndex() - BBAN_START;
        int maskedLen = c.countryData.getIbanLength() - BBAN_START;

        // virtual CharSequence view: forwards the BBAN prefix as-is, and substitutes
        // '0' for the two NCD positions - without copying any characters
        int remainder = Mod97.calculateRange(new CharSequence() {
            @Override
            public int length() {
                return maskedLen;
            }

            @Override
            public char charAt(int index) {
                return index < prefixLen ? iban[BBAN_START + index] : '0';
            }

            @Override
            public CharSequence subSequence(int s, int e) {
                throw new UnsupportedOperationException();
            }
        }, 0, maskedLen);

        return twoDigits(remainder == 0 ? 0 : CHECK_DIGIT_MAGIC_NUMBER - remainder);
    }

    /**
     * National Check Digit algorithm for <strong>Belgium (BE)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 3n bank + 7n account + 2n NCD = 12 digits (IBAN length 16).
     * <p>
     * <strong>Algorithm:</strong>
     * MOD 97 applied to the 10-digit prefix (bank code + account number).
     * NCD = remainder, except remainder {@code 0} yields NCD = {@code 97}.
     * Result range: {@code 01–97}.
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digits
     */
    static char[] be(NcdCalculatorBase c, char[] iban) {
        int remainder = mod97(iban, BBAN_START, c.ncdComponent.getBeginIndex() - BBAN_START);
        return twoDigits(remainder == 0 ? 97 : remainder);
    }

    /**
     * National Check Digit algorithm for <strong>Estonia (EE)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 2n bank + 2n branch + 11n account + 1n NCD
     *  = 16 digits (IBAN length 20). Only the 15 digits before the NCD are used.
     * <p>
     * <strong>Algorithm:</strong>
     * Weighted MOD 10 with weights {@code {7, 3, 1}} (cyclic, left-to-right) over the BBAN prefix.
     * Result: {@code (10 − sum % 10) % 10}.
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digit
     */
    static char[] ee(NcdCalculatorBase c, char[] iban) {
        int s = 0;
        for (int i = BBAN_START; i < c.ncdComponent.getBeginIndex(); i++) {
            s += (iban[i] - '0') * EE_WEIGHTS[(i - BBAN_START) % EE_WEIGHTS.length];
        }
        return oneDigit((10 - s % 10) % 10);
    }

    /**
     * National Check Digit algorithm for <strong>Spain (ES)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 4n bank + 4n branch + 2n DC + 10n account = 20 digits
     * (IBAN length 24).
     * <p>
     * <strong>Dígito de Control (DC):</strong>
     * Two separate check digits at positions {@code DC[0]} and {@code DC[1]}:
     * <ul>
     *   <li>{@code DC[0]}: computed from bank (4 digits) + branch (4 digits) using
     *       weights {@code {4, 8, 5, 10, 9, 7, 3, 6}} (weights[2..9], simulating a
     *       virtual "00" prefix with weights[0..1]).</li>
     *   <li>{@code DC[1]}: computed from account number (10 digits) using
     *       weights {@code {1, 2, 4, 8, 5, 10, 9, 7, 3, 6}}.</li>
     * </ul>
     * <p>
     * <strong>Algorithm:</strong>
     * Weighted MOD 11. Result = {@code 11 − (sum % 11)}, mapping 11 → 0 and 10 → 1.
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digits
     */
    static char[] es(NcdCalculatorBase c, char[] iban) {
        // DC[0]: bank (4) + branch (4), weight offset 2 simulates "00" prefix
        int firstDigit  = calculateMod11Spanish(iban, BBAN_START, 8, 2);
        // DC[1]: account number (10 digits) after the NCD field
        int secondDigit = calculateMod11Spanish(iban, c.ncdComponent.getEndIndex(), 10, 0);
        return twoDigits(firstDigit * 10 + secondDigit);
    }

    /**
     * Computes a single Spanish DC digit using weighted MOD 11.
     * <p>
     * Formula: {@code result = 11 − (sum % 11)}, with special cases
     * {@code 11 → 0} and {@code 10 → 1}.
     *
     * @param iban         the IBAN character array
     * @param offset       start index in the array
     * @param length       number of digits to process
     * @param weightOffset starting index into {@link #ES_WEIGHTS}
     * @return a single digit in range [0, 9]
     */
    private static int calculateMod11Spanish(char[] iban, int offset, int length, int weightOffset) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (iban[offset + i] - '0') * ES_WEIGHTS[weightOffset + i];
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

    /**
     * National Check Digit algorithm for <strong>Finland (FI)</strong> and
     * <strong>Åland Islands (AX)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 6n bank + 7n account + 1n NCD = 14 digits
     * (IBAN length 18).
     * <p>
     * <strong>Algorithm:</strong>
     * Luhn (MOD 10) applied right-to-left over the 13-digit prefix.
     * Weights {@code {2, 1}} alternate from the rightmost digit leftward.
     * Products ≥ 10 are reduced by summing their digits.
     * NCD = {@code (10 − sum % 10) % 10}.
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digit
     */
    static char[] fi(NcdCalculatorBase c, char[] iban) {
        int ncdBegin = c.ncdComponent.getBeginIndex();
        int sum = 0;
        for (int i = 0; i < ncdBegin - BBAN_START; i++) {
            int product = (iban[ncdBegin - 1 - i] - '0') * FI_WEIGHTS[i % FI_WEIGHTS.length];
            if (product >= 10) {
                product = (product / 10) + (product % 10);
            }
            sum += product;
        }
        return oneDigit((10 - (sum % 10)) % 10);
    }

    /**
     * National Check Digit algorithm for <strong>France (FR)</strong> and all associated
     * territories.
     * <p>
     * Territories sharing this algorithm:
     * Guadeloupe (GP), Martinique (MQ), French Guiana (GF), Réunion (RE),
     * French Polynesia (PF), French Southern Territories (TF), Mayotte (YT),
     * New Caledonia (NC), Saint Barthélemy (BL), Saint Martin (MF),
     * Saint Pierre and Miquelon (PM), Wallis and Futuna (WF), Monaco (MC),
     * and several West-African countries (BF, BJ, CF, CM, GQ, KM, SN).
     * Tunisia (TN) and Djibouti (DJ) use the same formula.
     * <p>
     * <strong>RIB - Clé RIB (Relevé d'Identité Bancaire):</strong>
     * Two-digit checksum {@code 01–97} at the end of the BBAN.
     * <p>
     * <strong>Algorithm:</strong>
     * MOD 97 with French-specific letter substitution (see {@link #getFrenchRIBValue(char)}).
     * NCD = {@code 97 − (prefix_remainder × 100 mod 97)}, always in range 01-97.
     * <p>
     * <strong>Letter mapping</strong> (differs from ISO 7064 A=10, B=11…):
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
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digits
     */
    static char[] fr(NcdCalculatorBase c, char[] iban) {
        int remainder = mod97French(iban, BBAN_START, c.ncdComponent.getBeginIndex() - BBAN_START);
        return twoDigits(MODULO_BASE - (remainder * 100) % MODULO_BASE);
    }

    /**
     * Computes MOD 97 using the French RIB letter-to-digit mapping.
     * <p>
     * Unlike {@link Mod97#calculateRange(CharSequence, int, int)} (ISO 7064, A=10…Z=35),
     * each letter is mapped to a <em>single</em> digit 1–9 (see {@link #getFrenchRIBValue(char)})
     * and fed as one digit into the streaming accumulator.
     *
     * @param data   the character array
     * @param offset start index (inclusive)
     * @param length number of characters to process
     * @return MOD 97 remainder in range [0, 96]
     */
    static int mod97French(char[] data, int offset, int length) {
        int remainder = 0;
        for (int i = 0; i < length; i++) {
            remainder = (remainder * 10 + getFrenchRIBValue(data[offset + i])) % MODULO_BASE;
        }
        return remainder;
    }

    /**
     * Maps a character to its French RIB single-digit value (0–9).
     * <p>
     * Digits {@code '0'–'9'} map to themselves.
     * Letters follow a cyclic 1–9 pattern that repeats across A–I, J–R, and S–Z
     * (with S=2 rather than S=1, skipping 1 in the third group).
     *
     * @param ch the input character (digit or uppercase ASCII letter)
     * @return the RIB value in range [0, 9]; 0 for unrecognised characters
     */
    static int getFrenchRIBValue(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'A' && ch <= 'Z') {
            if (ch <= 'I') {
                return ch - 'A' + 1; // A=1 ... I=9
            }
            if (ch <= 'R') {
                return ch - 'J' + 1; // J=1 ... R=9
            }
            return ch - 'S' + 2;    // S=2 ... Z=9 (skips 1)
        }
        return 0;
    }

    /**
     * National Check Digit algorithm for <strong>Hungary (HU)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 3n bank + 4n branch + 1n NCD1 + 15n account + 1n NCD2
     * = 24 digits (IBAN length 28). This implementation computes the <em>second</em> check digit
     * (NCD2), which covers the 15-digit account number starting at IBAN index 12.
     * <p>
     * <strong>Algorithm:</strong>
     * Weighted MOD 10 with weights {@code {9, 7, 3, 1}} (cyclic). The NCD equals the ones digit
     * of the weighted sum, i.e., {@code sum % 10} (the NCD is chosen so that including it makes
     * the total sum a multiple of 10 using weight 1 for the NCD position).
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digit
     */
    static char[] hu(NcdCalculatorBase c, char[] iban) {
        int accountStart = BBAN_START + 8; // after 3n bank + 4n branch + 1n NCD1
        int s = 0;
        for (int i = 0; i < 15; i++) {
            s += (iban[accountStart + i] - '0') * HU_WEIGHTS[i % HU_WEIGHTS.length];
        }
        return oneDigit(s % 10);
    }

    /**
     * National Check Digit algorithm for <strong>Italy (IT)</strong> and
     * <strong>San Marino (SM)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 1a CIN + 5n bank + 5n branch + 12c account = 23 chars
     * (IBAN length 27). The CIN is at {@code BBAN_START} (IBAN index 4); the algorithm
     * processes the remaining 22 characters from index 5 onward.
     * <p>
     * <strong>CIN (Codice di Controllo / Codice Identificativo Numero):</strong>
     * A single letter {@code 'A'–'Z'} derived from the 22-character BBAN suffix (bank + branch
     * + account).
     * <p>
     * <strong>Algorithm:</strong>
     * <ol>
     *   <li>Assign a numeric value to each character:
     *     <ul>
     *       <li><em>Odd positions</em> (1st, 3rd, …): use {@link #IT_ODD_MAP}.</li>
     *       <li><em>Even positions</em> (2nd, 4th, …): digits map to 0–9, letters to 10–35.</li>
     *     </ul>
     *   </li>
     *   <li>Sum all values and take {@code sum % 26}.</li>
     *   <li>Convert result (0–25) to letter {@code 'A'–'Z'}.</li>
     * </ol>
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check character
     */
    static char[] it(NcdCalculatorBase c, char[] iban) {
        int cinIndex = c.ncdComponent.getBeginIndex();
        int bbanEnd  = c.countryData.getIbanLength();
        int sum = 0;
        for (int i = cinIndex + 1; i < bbanEnd; i++) {
            char ch = iban[i];
            int val = (ch <= '9') ? ch - '0' : ch - 'A' + 10;
            sum += ((i - cinIndex - 1) % 2 == 0) ? IT_ODD_MAP[val] : val;
        }
        return oneLetter(sum % 26);
    }

    /**
     * National Check Digit algorithm for <strong>Norway (NO)</strong>.
     * <p>
     * <strong>BBAN structure:</strong> 4n bank + 6n account + 1n NCD = 11 digits
     * (IBAN length 15).
     * <p>
     * <strong>Algorithm:</strong>
     * Weighted MOD 11 with weights {@code {5, 4, 3, 2, 7, 6, 5, 4, 3, 2}} applied to the
     * 10-digit prefix (bank + account). NCD = {@code remainder == 0 ? 0 : 11 − remainder}.
     * <p>
     * <strong>Special case - result {@code 10}:</strong>
     * A computed result of {@code 10} means the account number is <em>not issuable</em> by
     * Norwegian banks (Bankenes Standardiseringskontor). In this case
     * {@link NationalCheckDigitCalculator#calculateNationalCheckDigit(char[])} returns the NCD value already present in the
     * input IBAN unchanged (passthrough), so that {@link NationalCheckDigitCalculator#validateNationalCheckDigit(char[])} will
     * always pass for such account numbers.
     *
     * @param c    the calculator of the country
     * @param iban the normalized IBAN
     * @return the check digit
     */
    static char[] no(NcdCalculatorBase c, char[] iban) {
        int sum = 0;
        for (int i = 0; i < c.ncdComponent.getBeginIndex() - BBAN_START; i++) {
            sum += (iban[BBAN_START + i] - '0') * NO_WEIGHTS[i];
        }
        int remainder = sum % 11;
        int result    = (remainder == 0) ? 0 : (11 - remainder);
        return result == 10 ? c.ncdComponent.extractFrom(iban) : oneDigit(result);
    }

    /**
     * Abstract base class for all NCD calculators.
     * <p>
     * Provides:
     * <ul>
     *   <li>The {@link #countryData} and {@link #ncdComponent} of the country passed
     *       to the constructor.</li>
     *   <li>A default {@link #validateNationalCheckDigit} implementation that extracts
     *       the NCD from the IBAN and compares it to {@link #calculateNationalCheckDigit}.</li>
     *   <li>A {@link #toString()} that includes the algorithm name and the country code.</li>
     * </ul>
     * <p>
     * Subclasses must implement {@link #calculateNationalCheckDigit(char[])} and may
     * override {@link #validateNationalCheckDigit(char[])} when a more efficient or
     * structurally different validation is required (e.g., {@link Mod97RemainderOneNcdCalculator}).
     */
    abstract static class NcdCalculatorBase implements NationalCheckDigitCalculator {

        /** Country-specific registry data, i.e. the enum entry in {@link IbanRegistry} passed at construction time. */
        final IbanRegistry            countryData;

        /** The National Check Digit (NCD) component within the IBAN. */
        final IbanComponent           ncdComponent;

        /**
         * Creates the calculator for the given country.
         *
         * @param countryData the registry entry of a country with an NCD; must not be {@code null}
         * @throws NullPointerException if {@code countryData} is {@code null} or has no NCD component
         */
        NcdCalculatorBase(IbanRegistry countryData) {
            this.countryData = requireNonNull(countryData, "countryData must not be null");
            ncdComponent = requireNonNull(countryData.getNationalCheckDigitComponent(),
                () -> String.format("%s has no national check digit", countryData.getCountryCode()));
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
            final int start = ncdComponent.getBeginIndex();
            final int length = ncdComponent.getLength();

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
         * Returns the name of the algorithm, by default the simple class name.
         *
         * @return the algorithm name
         */
        String name() {
            return getClass().getSimpleName();
        }

        /**
         * Returns a human-readable representation in the format
         * {@code "Algorithm[XX, NCD component]"}, e.g. {@code "FR[MC, NCD ...]"}.
         *
         * @return a non-{@code null} string identifying algorithm and country
         */
        @Override
        public final String toString() {
            return String.format("%s[%s, NCD %s]", name(), countryData.getCountryCode(), ncdComponent);
        }
    }

    /**
     * Calculator delegating to one of the algorithms of the enclosing class.
     */
    static final class AlgorithmNcdCalculator extends NcdCalculatorBase {

        private final String                                        name;
        private final BiFunction<NcdCalculatorBase, char[], char[]> algorithm;

        /**
         * Creates the calculator for the given country.
         *
         * @param countryData the registry entry of a country using the algorithm
         * @param name        the algorithm name
         * @param algorithm   the algorithm
         */
        AlgorithmNcdCalculator(IbanRegistry countryData, String name, BiFunction<NcdCalculatorBase, char[], char[]> algorithm) {
            super(countryData);
            this.name = name;
            this.algorithm = algorithm;
        }

        @Override
        public char[] calculateNationalCheckDigit(char[] iban) {
            return algorithm.apply(this, iban);
        }

        @Override
        String name() {
            return name;
        }
    }

    /**
     * Calculator for countries where no standardised NCD algorithm is specified by the
     * SWIFT IBAN Registry.
     * <p>
     * {@link #calculateNationalCheckDigit} returns the NCD already present in the
     * IBAN unchanged (passthrough). {@link #validateNationalCheckDigit} (inherited from
     * {@link NcdCalculatorBase}) will therefore always return {@code true} for a structurally
     * valid IBAN of the correct length.
     *
     * @since 1.8.13
     */
    static final class NoOpNcdCalculator extends NcdCalculatorBase {

        /**
         * Creates the calculator for the given country.
         *
         * @param countryData the registry entry of a country without a specified NCD algorithm
         */
        NoOpNcdCalculator(IbanRegistry countryData) {
            super(countryData);
        }

        /**
         * Returns the NCD value already present in {@code iban} without recomputing it.
         *
         * @param iban the normalized IBAN
         * @return the current NCD field extracted verbatim from {@code iban}
         */
        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            return ncdComponent.extractFrom(iban);
        }
    }

    /**
     * Calculator for countries that use the ISO 7064 MOD 97-10 algorithm where the
     * <em>full</em> BBAN (including the NCD) must produce a remainder of exactly {@code 1}
     * when divided by {@code 97}.
     * <p>
     * This is the standard algorithm used by BA, ME, MK, MZ, PT, RS, SI, TL and XK.
     * <p>
     * <strong>Calculation:</strong>
     * {@code NCD = 98 − ((prefix_remainder × 100) mod 97)}, where
     * {@code prefix_remainder} is {@code mod97} applied to all BBAN digits before the
     * NCD field.
     * <p>
     * <strong>Validation:</strong>
     * {@code mod97(full_BBAN) == 1}.
     *
     * @since 1.8.13
     */
    static final class Mod97RemainderOneNcdCalculator extends NcdCalculatorBase {

        /**
         * Creates the calculator for the given country.
         *
         * @param countryData the registry entry of a country using this algorithm
         */
        Mod97RemainderOneNcdCalculator(IbanRegistry countryData) {
            super(countryData);
        }

        /**
         * Computes the two-digit NCD using the MOD 97-10 formula:
         * {@code 98 − ((prefix_remainder × 100) mod 97)}.
         *
         * @param iban the normalized IBAN
         * @return two-element {@code char[]} with the zero-padded NCD (e.g. {@code {'0', '7'}})
         */
        @Override
        public char[] calculateNationalCheckDigit(final char[] iban) {
            int remainder = mod97(iban, BBAN_START, ncdComponent.getBeginIndex() - BBAN_START);
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
        public boolean validateNationalCheckDigit(final char[] iban) {
            int bbanLength = countryData.getIbanLength() - BBAN_START;
            return mod97(iban, BBAN_START, bbanLength) == 1;
        }
    }

}
