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

import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enumeration of the seven continents of the world.
 *
 * @since 1.8.8
 */
public enum Continent {

    /** Africa */
    AFRICA("AF", "Africa"),
    /** Antarctica */
    ANTARCTICA("AN", "Antarctica"),
    /** Asia */
    ASIA("AS", "Asia"),
    /** Europe */
    EUROPE("EU", "Europe"),
    /** North America */
    NORTH_AMERICA("NA", "North America"),
    /** Oceania */
    OCEANIA("OC", "Oceania"),
    /** South America */
    SOUTH_AMERICA("SA", "South America");

    private static final Map<String, Continent> LOOKUP = buildLookupMap();

    /**
     * The code of this continent.
     */
    private final String                        code;
    /**
     * The name of this continent.
     */
    private final String                        continentName;

    Continent(final String code, final String continentName) {
        this.code            = code;
        this.continentName   = continentName;
    }

    /**
     * Builds the lookup map at class-load time.<br>
     * The initial capacity is sized to avoid any rehashing.
     */
    private static Map<String, Continent> buildLookupMap() {
        Continent[] values = values();
        int capacity = (int) (values.length / 0.75f) + 1;
        Map<String, Continent> map = new LinkedHashMap<>(capacity);
        for (final Continent c : values) {
            map.put(c.getCode(), c);
        }
        return unmodifiableMap(map);
    }

    public String getCode() {
        return code;
    }

    public String getContinentName() {
        return continentName;
    }

    public static Continent fromCode(final String code) {
        return code == null ? null : LOOKUP.get(code);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name() + ']';
    }

}
