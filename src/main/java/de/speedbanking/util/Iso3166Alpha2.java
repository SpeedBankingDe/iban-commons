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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enumeration of all officially assigned ISO 3166-1 Alpha-2 country codes
 * as published by the ISO 3166 Maintenance Agency.
 * <p>
 * Each constant carries the two-letter code (the enum name itself) and the
 * corresponding English short name as specified in ISO 3166-1.
 * <p>
 * The list contains all <strong>249 currently assigned</strong> Alpha-2 codes
 * (status: 2025). It intentionally excludes:
 * <ul>
 *   <li>
 *       Deleted/transitionally reserved codes (e.g., {@code CS}, {@code AN})
 *   </li>
 *   <li>
 *       Exceptionally reserved codes used only for specific purposes (e.g., {@code EU}, {@code UN})
 *   </li>
 *   <li>
 *       User-assigned codes ({@code AA}, {@code QM}–{@code QZ}, {@code XA}–{@code XZ},
 *       {@code ZZ}) — <em>except</em> {@code XK} (Kosovo), which is widely adopted
 *       in banking and payment systems
 *   </li>
 * </ul>
 * <p>
 * Lookup is O(1) via the internal {@link #LOOKUP} map; enum-name resolution via
 * {@link #valueOf(String)} is equally efficient.
 * <p>
 * For more information, see:
 * <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166-1 Alpha-2 Specification</a>
 *
 * @since 1.8.4
 */
public enum Iso3166Alpha2 {

    // -------------------------------------------------------------------------
    // A
    // -------------------------------------------------------------------------
    /** Aruba */
    AW("Aruba"),
    /** Afghanistan */
    AF("Afghanistan"),
    /** Angola */
    AO("Angola"),
    /** Anguilla */
    AI("Anguilla"),
    /** Åland Islands */
    AX("Åland Islands"),
    /** Albania */
    AL("Albania"),
    /** Andorra */
    AD("Andorra"),
    /** United Arab Emirates */
    AE("United Arab Emirates"),
    /** Argentina */
    AR("Argentina"),
    /** Armenia */
    AM("Armenia"),
    /** American Samoa */
    AS("American Samoa"),
    /** Antarctica */
    AQ("Antarctica"),
    /** French Southern Territories */
    TF("French Southern Territories"),
    /** Antigua and Barbuda */
    AG("Antigua and Barbuda"),
    /** Australia */
    AU("Australia"),
    /** Austria */
    AT("Austria"),
    /** Azerbaijan */
    AZ("Azerbaijan"),

    // -------------------------------------------------------------------------
    // B
    // -------------------------------------------------------------------------
    /** Burundi */
    BI("Burundi"),
    /** Belgium */
    BE("Belgium"),
    /** Benin */
    BJ("Benin"),
    /** Bonaire, Sint Eustatius and Saba */
    BQ("Bonaire, Sint Eustatius and Saba"),
    /** Burkina Faso */
    BF("Burkina Faso"),
    /** Bangladesh */
    BD("Bangladesh"),
    /** Bulgaria */
    BG("Bulgaria"),
    /** Bahrain */
    BH("Bahrain"),
    /** Bahamas */
    BS("Bahamas"),
    /** Bosnia and Herzegovina */
    BA("Bosnia and Herzegovina"),
    /** Saint Barthélemy */
    BL("Saint Barthélemy"),
    /** Belarus */
    BY("Belarus"),
    /** Belize */
    BZ("Belize"),
    /** Bermuda */
    BM("Bermuda"),
    /** Bolivia (Plurinational State of) */
    BO("Bolivia"),
    /** Brazil */
    BR("Brazil"),
    /** Barbados */
    BB("Barbados"),
    /** Brunei Darussalam */
    BN("Brunei Darussalam"),
    /** Bhutan */
    BT("Bhutan"),
    /** Bouvet Island */
    BV("Bouvet Island"),
    /** Botswana */
    BW("Botswana"),
    /** Central African Republic */
    CF("Central African Republic"),

    // -------------------------------------------------------------------------
    // C
    // -------------------------------------------------------------------------
    /** Canada */
    CA("Canada"),
    /** Cocos (Keeling) Islands */
    CC("Cocos (Keeling) Islands"),
    /** Switzerland */
    CH("Switzerland"),
    /** Chile */
    CL("Chile"),
    /** China */
    CN("China"),
    /** Côte d'Ivoire */
    CI("Côte d'Ivoire"),
    /** Cameroon */
    CM("Cameroon"),
    /** Congo, Democratic Republic of the */
    CD("Congo, Democratic Republic of the"),
    /** Congo */
    CG("Congo"),
    /** Cook Islands */
    CK("Cook Islands"),
    /** Colombia */
    CO("Colombia"),
    /** Comoros */
    KM("Comoros"),
    /** Cabo Verde */
    CV("Cabo Verde"),
    /** Costa Rica */
    CR("Costa Rica"),
    /** Cuba */
    CU("Cuba"),
    /** Curaçao */
    CW("Curaçao"),
    /** Christmas Island */
    CX("Christmas Island"),
    /** Cayman Islands */
    KY("Cayman Islands"),
    /** Cyprus */
    CY("Cyprus"),
    /** Czechia */
    CZ("Czechia"),

    // -------------------------------------------------------------------------
    // D
    // -------------------------------------------------------------------------
    /** Germany */
    DE("Germany"),
    /** Djibouti */
    DJ("Djibouti"),
    /** Dominica */
    DM("Dominica"),
    /** Denmark */
    DK("Denmark"),
    /** Dominican Republic */
    DO("Dominican Republic"),
    /** Algeria */
    DZ("Algeria"),

    // -------------------------------------------------------------------------
    // E
    // -------------------------------------------------------------------------
    /** Ecuador */
    EC("Ecuador"),
    /** Egypt */
    EG("Egypt"),
    /** Eritrea */
    ER("Eritrea"),
    /** Western Sahara */
    EH("Western Sahara"),
    /** Spain */
    ES("Spain"),
    /** Estonia */
    EE("Estonia"),
    /** Ethiopia */
    ET("Ethiopia"),

    // -------------------------------------------------------------------------
    // F
    // -------------------------------------------------------------------------
    /** Finland */
    FI("Finland"),
    /** Fiji */
    FJ("Fiji"),
    /** Falkland Islands (Malvinas) */
    FK("Falkland Islands (Malvinas)"),
    /** France */
    FR("France"),
    /** Faroe Islands */
    FO("Faroe Islands"),
    /** Micronesia (Federated States of) */
    FM("Micronesia (Federated States of)"),

    // -------------------------------------------------------------------------
    // G
    // -------------------------------------------------------------------------
    /** Gabon */
    GA("Gabon"),
    /** United Kingdom of Great Britain and Northern Ireland */
    GB("United Kingdom of Great Britain and Northern Ireland"),
    /** Georgia */
    GE("Georgia"),
    /** Guernsey */
    GG("Guernsey"),
    /** Ghana */
    GH("Ghana"),
    /** Gibraltar */
    GI("Gibraltar"),
    /** Guinea */
    GN("Guinea"),
    /** Guadeloupe */
    GP("Guadeloupe"),
    /** Gambia */
    GM("Gambia"),
    /** Guinea-Bissau */
    GW("Guinea-Bissau"),
    /** Equatorial Guinea */
    GQ("Equatorial Guinea"),
    /** Greece */
    GR("Greece"),
    /** Grenada */
    GD("Grenada"),
    /** Greenland */
    GL("Greenland"),
    /** Guatemala */
    GT("Guatemala"),
    /** French Guiana */
    GF("French Guiana"),
    /** Guam */
    GU("Guam"),
    /** Guyana */
    GY("Guyana"),
    /** Hong Kong */
    HK("Hong Kong"),

    // -------------------------------------------------------------------------
    // H
    // -------------------------------------------------------------------------
    /** Heard Island and McDonald Islands */
    HM("Heard Island and McDonald Islands"),
    /** Honduras */
    HN("Honduras"),
    /** Croatia */
    HR("Croatia"),
    /** Haiti */
    HT("Haiti"),
    /** Hungary */
    HU("Hungary"),

    // -------------------------------------------------------------------------
    // I
    // -------------------------------------------------------------------------
    /** Indonesia */
    ID("Indonesia"),
    /** Isle of Man */
    IM("Isle of Man"),
    /** India */
    IN("India"),
    /** British Indian Ocean Territory */
    IO("British Indian Ocean Territory"),
    /** Ireland */
    IE("Ireland"),
    /** Iran (Islamic Republic of) */
    IR("Iran (Islamic Republic of)"),
    /** Iraq */
    IQ("Iraq"),
    /** Iceland */
    IS("Iceland"),
    /** Israel */
    IL("Israel"),
    /** Italy */
    IT("Italy"),

    // -------------------------------------------------------------------------
    // J
    // -------------------------------------------------------------------------
    /** Jamaica */
    JM("Jamaica"),
    /** Jersey */
    JE("Jersey"),
    /** Jordan */
    JO("Jordan"),
    /** Japan */
    JP("Japan"),

    // -------------------------------------------------------------------------
    // K
    // -------------------------------------------------------------------------
    /** Kenya */
    KE("Kenya"),
    /** Kyrgyzstan */
    KG("Kyrgyzstan"),
    /** Cambodia */
    KH("Cambodia"),
    /** Kiribati */
    KI("Kiribati"),
    /** Saint Kitts and Nevis */
    KN("Saint Kitts and Nevis"),
    /** Korea, Republic of */
    KR("Korea, Republic of"),
    /** Kuwait */
    KW("Kuwait"),
    /** Kazakhstan */
    KZ("Kazakhstan"),
    /** Korea (Democratic People's Republic of) */
    KP("Korea (Democratic People's Republic of)"),

    // -------------------------------------------------------------------------
    // L
    // -------------------------------------------------------------------------
    /** Lao People's Democratic Republic */
    LA("Lao People's Democratic Republic"),
    /** Lebanon */
    LB("Lebanon"),
    /** Liberia */
    LR("Liberia"),
    /** Libya */
    LY("Libya"),
    /** Saint Lucia */
    LC("Saint Lucia"),
    /** Liechtenstein */
    LI("Liechtenstein"),
    /** Sri Lanka */
    LK("Sri Lanka"),
    /** Lesotho */
    LS("Lesotho"),
    /** Lithuania */
    LT("Lithuania"),
    /** Luxembourg */
    LU("Luxembourg"),
    /** Latvia */
    LV("Latvia"),

    // -------------------------------------------------------------------------
    // M
    // -------------------------------------------------------------------------
    /** Macao */
    MO("Macao"),
    /** Saint Martin (French part) */
    MF("Saint Martin (French part)"),
    /** Morocco */
    MA("Morocco"),
    /** Monaco */
    MC("Monaco"),
    /** Moldova, Republic of */
    MD("Moldova, Republic of"),
    /** Madagascar */
    MG("Madagascar"),
    /** Maldives */
    MV("Maldives"),
    /** Mexico */
    MX("Mexico"),
    /** Marshall Islands */
    MH("Marshall Islands"),
    /** North Macedonia */
    MK("North Macedonia"),
    /** Mali */
    ML("Mali"),
    /** Malta */
    MT("Malta"),
    /** Myanmar */
    MM("Myanmar"),
    /** Mongolia */
    MN("Mongolia"),
    /** Northern Mariana Islands */
    MP("Northern Mariana Islands"),
    /** Mozambique */
    MZ("Mozambique"),
    /** Mauritania */
    MR("Mauritania"),
    /** Montenegro */
    ME("Montenegro"),
    /** Montserrat */
    MS("Montserrat"),
    /** Martinique */
    MQ("Martinique"),
    /** Mauritius */
    MU("Mauritius"),
    /** Malawi */
    MW("Malawi"),
    /** Malaysia */
    MY("Malaysia"),
    /** Mayotte */
    YT("Mayotte"),

    // -------------------------------------------------------------------------
    // N
    // -------------------------------------------------------------------------
    /** Namibia */
    NA("Namibia"),
    /** New Caledonia */
    NC("New Caledonia"),
    /** Niger */
    NE("Niger"),
    /** Norfolk Island */
    NF("Norfolk Island"),
    /** Nigeria */
    NG("Nigeria"),
    /** Nicaragua */
    NI("Nicaragua"),
    /** Niue */
    NU("Niue"),
    /** Netherlands */
    NL("Netherlands"),
    /** Norway */
    NO("Norway"),
    /** Nepal */
    NP("Nepal"),
    /** Nauru */
    NR("Nauru"),
    /** New Zealand */
    NZ("New Zealand"),

    // -------------------------------------------------------------------------
    // O
    // -------------------------------------------------------------------------
    /** Oman */
    OM("Oman"),

    // -------------------------------------------------------------------------
    // P
    // -------------------------------------------------------------------------
    /** Panama */
    PA("Panama"),
    /** Peru */
    PE("Peru"),
    /** French Polynesia */
    PF("French Polynesia"),
    /** Papua New Guinea */
    PG("Papua New Guinea"),
    /** Philippines */
    PH("Philippines"),
    /** Pakistan */
    PK("Pakistan"),
    /** Poland */
    PL("Poland"),
    /** Saint Pierre and Miquelon */
    PM("Saint Pierre and Miquelon"),
    /** Pitcairn */
    PN("Pitcairn"),
    /** Puerto Rico */
    PR("Puerto Rico"),
    /** Palestine, State of */
    PS("Palestine, State of"),
    /** Portugal */
    PT("Portugal"),
    /** Palau */
    PW("Palau"),
    /** Paraguay */
    PY("Paraguay"),

    // -------------------------------------------------------------------------
    // Q
    // -------------------------------------------------------------------------
    /** Qatar */
    QA("Qatar"),

    // -------------------------------------------------------------------------
    // R
    // -------------------------------------------------------------------------
    /** Réunion */
    RE("Réunion"),
    /** Romania */
    RO("Romania"),
    /** Serbia */
    RS("Serbia"),
    /** Russian Federation */
    RU("Russian Federation"),
    /** Rwanda */
    RW("Rwanda"),

    // -------------------------------------------------------------------------
    // S
    // -------------------------------------------------------------------------
    /** Saudi Arabia */
    SA("Saudi Arabia"),
    /** Solomon Islands */
    SB("Solomon Islands"),
    /** Sudan */
    SD("Sudan"),
    /** Sweden */
    SE("Sweden"),
    /** Singapore */
    SG("Singapore"),
    /** Saint Helena, Ascension and Tristan da Cunha */
    SH("Saint Helena, Ascension and Tristan da Cunha"),
    /** Slovenia */
    SI("Slovenia"),
    /** Svalbard and Jan Mayen */
    SJ("Svalbard and Jan Mayen"),
    /** Slovakia */
    SK("Slovakia"),
    /** Sierra Leone */
    SL("Sierra Leone"),
    /** San Marino */
    SM("San Marino"),
    /** Senegal */
    SN("Senegal"),
    /** Somalia */
    SO("Somalia"),
    /** Suriname */
    SR("Suriname"),
    /** South Sudan */
    SS("South Sudan"),
    /** São Tomé and Príncipe */
    ST("São Tomé and Príncipe"),
    /** El Salvador */
    SV("El Salvador"),
    /** Sint Maarten (Dutch part) */
    SX("Sint Maarten (Dutch part)"),
    /** Syrian Arab Republic */
    SY("Syrian Arab Republic"),
    /** Eswatini */
    SZ("Eswatini"),
    /** Seychelles */
    SC("Seychelles"),

    // -------------------------------------------------------------------------
    // T
    // -------------------------------------------------------------------------
    /** Turks and Caicos Islands */
    TC("Turks and Caicos Islands"),
    /** Chad */
    TD("Chad"),
    /** Togo */
    TG("Togo"),
    /** Thailand */
    TH("Thailand"),
    /** Tajikistan */
    TJ("Tajikistan"),
    /** Tokelau */
    TK("Tokelau"),
    /** Turkmenistan */
    TM("Turkmenistan"),
    /** Timor-Leste */
    TL("Timor-Leste"),
    /** Tonga */
    TO("Tonga"),
    /** Trinidad and Tobago */
    TT("Trinidad and Tobago"),
    /** Tunisia */
    TN("Tunisia"),
    /** Türkiye */
    TR("Türkiye"),
    /** Tuvalu */
    TV("Tuvalu"),
    /** Taiwan, Province of China */
    TW("Taiwan, Province of China"),
    /** Tanzania, United Republic of */
    TZ("Tanzania, United Republic of"),

    // -------------------------------------------------------------------------
    // U
    // -------------------------------------------------------------------------
    /** Ukraine */
    UA("Ukraine"),
    /** Uganda */
    UG("Uganda"),
    /** United States Minor Outlying Islands */
    UM("United States Minor Outlying Islands"),
    /** Uruguay */
    UY("Uruguay"),
    /** Uzbekistan */
    UZ("Uzbekistan"),
    /** United States of America */
    US("United States of America"),

    // -------------------------------------------------------------------------
    // V
    // -------------------------------------------------------------------------
    /** Holy See */
    VA("Holy See"),
    /** Saint Vincent and the Grenadines */
    VC("Saint Vincent and the Grenadines"),
    /** Venezuela (Bolivarian Republic of) */
    VE("Venezuela (Bolivarian Republic of)"),
    /** Virgin Islands (British) */
    VG("Virgin Islands (British)"),
    /** Virgin Islands (U.S.) */
    VI("Virgin Islands (U.S.)"),
    /** Viet Nam */
    VN("Viet Nam"),
    /** Vanuatu */
    VU("Vanuatu"),

    // -------------------------------------------------------------------------
    // W
    // -------------------------------------------------------------------------
    /** Wallis and Futuna */
    WF("Wallis and Futuna"),
    /** Samoa */
    WS("Samoa"),

    // -------------------------------------------------------------------------
    // X  (user-assigned, but widely adopted in banking)
    // -------------------------------------------------------------------------
    /** Kosovo (user-assigned; widely adopted in banking and payment systems) */
    XK("Kosovo"),

    // -------------------------------------------------------------------------
    // Y
    // -------------------------------------------------------------------------
    /** Yemen */
    YE("Yemen"),

    // -------------------------------------------------------------------------
    // Z
    // -------------------------------------------------------------------------
    /** South Africa */
    ZA("South Africa"),
    /** Zambia */
    ZM("Zambia"),
    /** Zimbabwe */
    ZW("Zimbabwe");

    /**
     * Unmodifiable lookup map from two-letter code string to enum constant.<br>
     * Preserves declaration order via {@link LinkedHashMap}.
     */
    private static final Map<String, Iso3166Alpha2> LOOKUP = buildLookup();

    /** The English short country name as defined in ISO 3166-1. */
    private final String                            countryName;

    Iso3166Alpha2(final String countryName) {
        this.countryName = countryName;
    }

    /**
     * Builds the lookup map at class-load time.<br>
     * The initial capacity is sized to avoid any rehashing.
     */
    private static Map<String, Iso3166Alpha2> buildLookup() {
        Iso3166Alpha2[] values = values();
        int capacity = (int) (values.length / 0.75f) + 1;
        Map<String, Iso3166Alpha2> map = new LinkedHashMap<>(capacity);
        for (final Iso3166Alpha2 c : values) {
            map.put(c.name(), c);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns the English short country name as defined in ISO 3166-1.
     *
     * @return the English country name (e.g., {@code "Bolivia"})
     */
    public String getCountryName() {
        return countryName;
    }

    /**
     * Returns the two-letter ISO 3166-1 Alpha-2 code (identical to {@link #name()}).
     * <p>
     * Provided for convenience and symmetry with {@link #getCountryName()}.
     *
     * @return the Alpha-2 code (e.g., {@code "CN"}, {@code "JP"}, etc.)
     */
    public String getCode() {
        return name();
    }

    /**
     * Looks up the enum constant for the given two-letter country code.
     * <p>
     * The lookup is case-sensitive; only uppercase codes are recognized
     * (e.g., {@code "LT"} matches, {@code "lt"} does not).
     *
     * @param code the two-letter ISO 3166-1 Alpha-2 code (case-sensitive, e.g., {@code "LV"})
     * @return the matching {@link Iso3166Alpha2} constant,
     *         or {@code null} if the code is unknown, {@code null}, or not exactly two characters
     */
    public static Iso3166Alpha2 fromCode(final String code) {
        if (code == null || code.length() != 2) {
            return null;
        }
        return LOOKUP.get(code);
    }

    /**
     * Checks whether the given string is a valid, officially assigned ISO 3166-1 Alpha-2 code.
     * <p>
     * This method performs an exact lookup against the full list of assigned codes.
     * Pure format checks (two uppercase letters A–Z) are <em>not</em> sufficient —
     * for example, {@code "AA"} or {@code "ZZ"} pass a format check but are not assigned.
     *
     * @param code the two-letter code to check (case-sensitive)
     * @return {@code true} if the code is an officially assigned ISO 3166-1 Alpha-2 code;
     *         {@code false} otherwise
     */
    public static boolean isAssigned(final String code) {
        return fromCode(code) != null;
    }

    /**
     * Returns a human-readable representation combining the code and country name.
     * <p>
     * Example: {@code "IQ (Iraq)"}
     *
     * @return the formatted string
     */
    @Override
    public String toString() {
        return name() + " (" + countryName + ")";
    }

}
