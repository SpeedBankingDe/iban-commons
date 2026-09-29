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

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
 * {@code S.A.} match the same {@code SA} entry, rendered in its curated canonical casing (e.g.
 * {@code GMBH} becomes {@code GmbH}, {@code EG} becomes {@code eG}) rather than left as-is,</li>
 * <li>a run of single-letter initials (e.g. {@code J.P.}), left completely untouched, or</li>
 * <li>three characters or fewer once punctuation is stripped (e.g. {@code DZ}, {@code BNP}),
 * left completely untouched as a likely abbreviation or brand token, unless it is a known
 * connector word ({@link #CONNECTORS}, e.g. {@code VAN}, {@code DE}), which is still title-cased
 * normally.</li>
 * </ul>
 * <p>
 * This is deliberately a small, curated exception list, not a generic acronym detector: an
 * unrecognized token longer than three characters (e.g. a product code) is still title-cased,
 * and a genuine brand name that itself uses an internal capital with no separating space (e.g.
 * {@code BinckBank}) cannot be recovered from an all-caps source at all - both are accepted
 * limitations of a purely textual heuristic.
 *
 * @since 1.8.12
 */
public final class BankNameCasing {

    /** A token this short (once punctuation is stripped) is left untouched, see the class Javadoc. */
    private static final int SHORT_TOKEN_MAX_LENGTH = 3;

    /**
     * Legal-form abbreviations found across the countries this module covers, keyed upper-cased
     * and with punctuation stripped (so this single map covers e.g. both {@code SA} and {@code
     * S.A.}), mapped to the letter-casing to render them in. Most entries map to themselves
     * (rendered exactly as encountered, original punctuation preserved); a handful of curated
     * exceptions (e.g. {@code GMBH}/{@code GmbH}, {@code EG}/{@code eG}) have a conventional
     * mixed-case rendering instead. A value must have the same number of letters as its key -
     * {@link #applyLegalFormCasing(String, String)} maps them onto the original token position by
     * position, leaving any punctuation within the token (e.g. the dots in {@code S.A.}) untouched.
     */
    private static final Map<String, String> LEGAL_FORMS = buildLegalForms();

    /**
     * Connector words (prepositions/conjunctions/articles) that must still be title-cased
     * normally despite being {@link #SHORT_TOKEN_MAX_LENGTH} characters or fewer, since - unlike
     * an abbreviation or brand token such as {@code DZ} or {@code BNP} - their all-caps rendering
     * (e.g. {@code VAN} in {@code VAN LANSCHOT}) is not itself meaningful and would otherwise be
     * left looking like an unintended shout.
     */
    private static final Set<String> CONNECTORS = unmodifiableSet(new HashSet<>(Arrays.asList(
        "VAN", "VON", "DER", "DEN", "DEL", "DES", "DA", "DI", "DU", "LA", "LE", "DE", "EN", "Y", "ET", "UND", "AND", "OF")));

    /** A whole token consisting of single-letter initials, e.g. {@code J.P.} or {@code A.}. */
    private static final Pattern INITIALS = Pattern.compile("(?:[A-ZÀ-ÖØ-Þ]\\.)+");

    private static Map<String, String> buildLegalForms() {
        Map<String, String> forms = new HashMap<>();
        for (String form : new String[] {
            // DE/AT/CH
            "AG", "KG", "EV", "SE",
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
            "PLC", "LTD", "LLC", "INC", "CORP", "SPA", "SRL", "AS", "ASA", "OY", "AB"}) {
            forms.put(form, form);
        }
        // curated exceptions with a conventional mixed-case rendering instead of the identity above
        forms.put("GMBH", "GmbH");
        forms.put("KGAA", "KGaA");
        forms.put("EG", "eG");
        return unmodifiableMap(forms);
    }

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

        String canonical = LEGAL_FORMS.get(coreWithoutPunctuation);
        if (canonical != null) {
            return prefix + applyLegalFormCasing(core, canonical) + suffix;
        }

        if (INITIALS.matcher(token).matches()) {
            return token;
        }

        if (coreWithoutPunctuation.length() <= SHORT_TOKEN_MAX_LENGTH && !CONNECTORS.contains(coreWithoutPunctuation)) {
            return token;
        }

        return prefix + titleCase(core) + suffix;
    }

    /**
     * Renders {@code core} in the letter-casing given by {@code canonical}, applied position by
     * position over its letters only - any punctuation within {@code core} (e.g. the dots in
     * {@code S.A.}) is left untouched at its original position.
     *
     * @param core      the token's letter-containing portion, all upper-case
     * @param canonical the desired letter-casing, with exactly as many letters as {@code core}
     * @return {@code core} with {@code canonical}'s letter-casing applied
     */
    private static String applyLegalFormCasing(String core, String canonical) {
        StringBuilder sb = new StringBuilder(core.length());
        int canonicalIndex = 0;
        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            sb.append(Character.isLetter(c) ? canonical.charAt(canonicalIndex++) : c);
        }
        return sb.toString();
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
