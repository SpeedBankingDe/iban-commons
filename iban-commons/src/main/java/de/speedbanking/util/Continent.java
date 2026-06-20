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

/**
 * Enumeration of the seven continents of the world.
 *
 * @since 1.8.8
 */
public enum Continent {

    AFRICA("AF", "Africa"),
    ANTARCTICA("AN", "Antarctica"),
    ASIA("AS", "Asia"),
    EUROPE("EU", "Europe"),
    NORTH_AMERICA("NA", "North America"),
    OCEANIA("OC", "Oceania"),
    SOUTH_AMERICA("SA", "South America");

    /**
     * Dedicated allocation-free registry lookup for {@code Continent} constants mapped by their two-letter code.
     */
    private static final Alpha2EnumLookup<Continent> LOOKUP = new Alpha2EnumLookup<>(Continent.class, Continent::getCode);

    /**
     * The code of this continent.
     */
    private final String                             code;

    /**
     * The name of this continent.
     */
    private final String                             continentName;

    Continent(String code, String continentName) {
        this.code = code;
        this.continentName = continentName;
    }

    public String getCode() {
        return code;
    }

    public String getContinentName() {
        return continentName;
    }

    /**
     * Looks up the enum constant for the given two-letter continent code.
     * <p>
     * Zero-allocation lookup supporting any {@link CharSequence}.
     *
     * @param code the two-letter continent code (case-sensitive, e.g., {@code "EU"});
     * may be any {@link CharSequence}
     * @return the matching {@link Continent} constant, or {@code null} if the code is null,
     * does not match the required format or length, or is unassigned
     */
    public static Continent fromCode(CharSequence code) {
        return LOOKUP.fromCode(code);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name() + ", code=" + code + ", continentName=" + continentName + ']';
    }

}
