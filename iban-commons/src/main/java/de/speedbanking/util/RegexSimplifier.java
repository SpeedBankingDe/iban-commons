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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplifies a regular expression by consolidating consecutive blocks
 * of the same character class that use the {n} quantifier.
 * <p>
 * Example:
 * <pre>
 *   ^BI[0-9]{2}[0-9]{5}[0-9]{5}[0-9]{11}[0-9]{2}$  ->  ^BI[0-9]{25}$
 * </pre>
 */
public final class RegexSimplifier {

    /**
     * Regex to identify a single, consolidatable segment.
     * Uses negative lookbehind to avoid matching escaped character classes or dots.
     * <p>
     * Captures:
     * 1: The character class or dot (e.g., [0-9], [A-Z], or '.')
     * 2: The repetition count inside the curly braces
     */
    private static final Pattern CONSOLIDABLE_PATTERN = Pattern.compile("(?<!\\\\)(\\[[^\\]]+\\]|\\.)\\{(\\d+)\\}");

    /**
     * Simplifies the regex by combining consecutive identical character class/literal blocks
     * that use the '{n}' quantifier.
     *
     * @param regex The input regular expression string.
     * @return The simplified regular expression string.
     */
    public static String simplify(String regex) {
        if (regex == null || regex.isEmpty()) {
            return regex;
        }

        StringBuilder simplified = new StringBuilder(regex.length());
        Matcher matcher = CONSOLIDABLE_PATTERN.matcher(regex);

        int lastEnd = 0;
        String currentBlockType = null;
        int currentLength = 0;

        while (matcher.find()) {
            String blockType = matcher.group(1);
            int length = Integer.parseInt(matcher.group(2));

            // check if current match directly continues the previous block of same type
            if (currentBlockType != null) {
                if (matcher.start() == lastEnd && blockType.equals(currentBlockType)) {
                    // accumulate length for consecutive matching block
                    currentLength += length;
                    lastEnd = matcher.end();
                    continue;
                } else {
                    // flush previously accumulated block due to gap or type change
                    simplified.append(currentBlockType).append('{').append(currentLength).append('}');
                    currentBlockType = null;
                }
            }

            // append non-consolidatable prefix between matches
            simplified.append(regex, lastEnd, matcher.start());

            currentBlockType = blockType;
            currentLength = length;
            lastEnd = matcher.end();
        }

        // flush final pending block if present
        if (currentBlockType != null) {
            simplified.append(currentBlockType).append('{').append(currentLength).append('}');
        }

        // append remaining trailing characters
        simplified.append(regex, lastEnd, regex.length());

        return simplified.toString();
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RegexSimplifier() {
        throw new UnsupportedOperationException(
            "Utility class " + RegexSimplifier.class.getSimpleName() + " cannot be instantiated");
    }

}
