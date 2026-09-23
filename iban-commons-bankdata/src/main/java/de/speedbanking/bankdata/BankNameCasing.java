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
package de.speedbanking.bankdata;

import static java.util.Collections.unmodifiableSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Best-effort, opt-in conversion of an ALL-CAPS institution name (as several sources publish
 * their register verbatim) into a more readable display form, without touching {@link
 * BankData#getBankName()} itself or the bundled/cached source data.
 * <p>
 * A name that already contains a lowercase letter is returned unchanged - only a name that is
 * entirely upper-case is a candidate for conversion, since a mixed-case name is already the
 * source's own, presumably intentional, rendering (e.g. {@code MONETA Money Bank, a.s.}).
 * <p>
 * Within a fully upper-case name, each whitespace-separated token is title-cased individually,
 * except a token that is:
 * <ul>
 * <li>a known legal-form abbreviation ({@link #LEGAL_FORMS}, e.g. {@code AG}, {@code S.A.},
 * {@code N.V.}), matched with surrounding punctuation stripped, so both {@code SA} and
 * {@code S.A.} match the same {@code SA} entry, or</li>
 * <li>a run of single-letter initials (e.g. {@code J.P.}).</li>
 * </ul>
 * Both exceptions are left completely untouched, including their original punctuation.
 * <p>
 * This is deliberately a small, curated exception list, not a generic acronym detector: an
 * unrecognized short token (e.g. an internal product code like {@code GF} in {@code DZ BANK GF
 * AIS}) is still title-cased, and a genuine brand name that itself uses an internal capital with
 * no separating space (e.g. {@code BinckBank}) cannot be recovered from an all-caps source at
 * all - both are accepted limitations of a purely textual heuristic.
 *
 * @since 1.8.11
 */
public final class BankNameCasing {

    /**
     * Legal-form abbreviations found across the countries this module covers, upper-cased and
     * with punctuation stripped for matching (so this single set covers e.g. both {@code SA} and
     * {@code S.A.}). Left untouched wherever encountered as a whole token, rather than
     * title-cased.
     */
    private static final Set<String> LEGAL_FORMS = unmodifiableSet(new HashSet<>(Arrays.asList(
        // DE/AT/CH
        "AG", "GMBH", "KG", "KGAA", "EG", "EV", "SE",
        // FR
        "SA", "SAS", "SASU", "SARL", "SCA", "SNC", "EURL",
        // BE/NL
        "NV", "BV", "BVBA", "ASBL",
        // ES
        "SL", "SLU", "SC", "SCC",
        // PL
        "SPZOO",
        // CZ
        "SRO",
        // generic/international
        "PLC", "LTD", "LLC", "INC", "CORP", "SPA", "SRL", "AS", "ASA", "OY", "AB")));

    /** A whole token consisting of single-letter initials, e.g. {@code J.P.} or {@code A.}. */
    private static final Pattern INITIALS = Pattern.compile("(?:[A-ZÀ-ÖØ-Þ]\\.)+");

    private BankNameCasing() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Converts the given bank name to a more readable display form if it is entirely upper-case,
     * otherwise returns it unchanged.
     *
     * @param bankName the bank name to convert, may be {@code null}
     * @return the converted name, or the original name if {@code null}, empty, already mixed-case,
     *         or containing no letters at all
     */
    public static String toDisplayCase(String bankName) {
        if (bankName == null || bankName.isEmpty() || containsLowerCase(bankName)) {
            return bankName;
        }
        String[] tokens = bankName.split(" ", -1);
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = toDisplayToken(tokens[i]);
        }
        return String.join(" ", tokens);
    }

    private static boolean containsLowerCase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String toDisplayToken(String token) {
        if (INITIALS.matcher(token).matches()) {
            return token;
        }

        int start = 0;
        int end = token.length();
        while (start < end && !Character.isLetter(token.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isLetter(token.charAt(end - 1))) {
            end--;
        }
        if (start == end) {
            return token; // no letters in this token (punctuation-only, a number, ...)
        }

        String prefix = token.substring(0, start);
        String core = token.substring(start, end);
        String suffix = token.substring(end);

        String coreWithoutPunctuation = core.replaceAll("[^A-Za-zÀ-ÖØ-öø-ÿ]", "").toUpperCase(Locale.ROOT);
        if (LEGAL_FORMS.contains(coreWithoutPunctuation)) {
            return token;
        }

        return prefix + titleCase(core) + suffix;
    }

    /** Title-cases {@code core}, capitalizing after an internal hyphen as well (e.g. "Rhone-Alpes"). */
    private static String titleCase(String core) {
        StringBuilder sb = new StringBuilder(core.length());
        boolean startOfWord = true;
        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            if (!Character.isLetter(c)) {
                sb.append(c);
                startOfWord = true;
                continue;
            }
            sb.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            startOfWord = false;
        }
        return sb.toString();
    }

}
