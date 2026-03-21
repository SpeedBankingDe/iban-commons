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
 * Each constant carries the two-letter code (the enum name itself), the
 * corresponding English short name as specified in ISO 3166-1, and the
 * primary ISO 4217 currency code used in that country or territory.
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
 * Notes on currency assignments:
 * <ul>
 *   <li>{@code AQ} (Antarctica) carries a {@code null} {@link Currency} — no currency is in use.</li>
 *   <li>Overseas territories use the currency of their administering state
 *       (e.g., {@code AX} → {@link Currency#EUR}, {@code GG} → {@link Currency#GBP}).</li>
 *   <li>Countries using another nation's currency without formal adoption are listed
 *       with the actually circulating currency (e.g., {@code ME}, {@code XK} → {@link Currency#EUR}).</li>
 *   <li>Currency constants reflect ISO 4217 as of 2025 (e.g., {@code SL} → {@link Currency#SLE},
 *       {@code BY} → {@link Currency#BYN}, {@code VE} → {@link Currency#VES}).</li>
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
    AW("Aruba",                                                Currency.AWG),
    /** Afghanistan */
    AF("Afghanistan",                                          Currency.AFN),
    /** Angola */
    AO("Angola",                                               Currency.AOA),
    /** Anguilla */
    AI("Anguilla",                                             Currency.XCD),
    /** Åland Islands */
    AX("Åland Islands",                                        Currency.EUR),
    /** Albania */
    AL("Albania",                                              Currency.ALL),
    /** Andorra */
    AD("Andorra",                                              Currency.EUR),
    /** United Arab Emirates */
    AE("United Arab Emirates",                                 Currency.AED),
    /** Argentina */
    AR("Argentina",                                            Currency.ARS),
    /** Armenia */
    AM("Armenia",                                              Currency.AMD),
    /** American Samoa */
    AS("American Samoa",                                       Currency.USD),
    /** Antarctica — no currency in use */
    AQ("Antarctica",                                           null),
    /** French Southern Territories */
    TF("French Southern Territories",                          Currency.EUR),
    /** Antigua and Barbuda */
    AG("Antigua and Barbuda",                                  Currency.XCD),
    /** Australia */
    AU("Australia",                                            Currency.AUD),
    /** Austria */
    AT("Austria",                                              Currency.EUR),
    /** Azerbaijan */
    AZ("Azerbaijan",                                           Currency.AZN),

    // -------------------------------------------------------------------------
    // B
    // -------------------------------------------------------------------------
    /** Burundi */
    BI("Burundi",                                              Currency.BIF),
    /** Belgium */
    BE("Belgium",                                              Currency.EUR),
    /** Benin */
    BJ("Benin",                                                Currency.XOF),
    /** Bonaire, Sint Eustatius and Saba */
    BQ("Bonaire, Sint Eustatius and Saba",                     Currency.USD),
    /** Burkina Faso */
    BF("Burkina Faso",                                         Currency.XOF),
    /** Bangladesh */
    BD("Bangladesh",                                           Currency.BDT),
    /** Bulgaria */
    BG("Bulgaria",                                             Currency.BGN),
    /** Bahrain */
    BH("Bahrain",                                              Currency.BHD),
    /** Bahamas */
    BS("Bahamas",                                              Currency.BSD),
    /** Bosnia and Herzegovina */
    BA("Bosnia and Herzegovina",                               Currency.BAM),
    /** Saint Barthélemy */
    BL("Saint Barthélemy",                                     Currency.EUR),
    /** Belarus */
    BY("Belarus",                                              Currency.BYN),
    /** Belize */
    BZ("Belize",                                               Currency.BZD),
    /** Bermuda */
    BM("Bermuda",                                              Currency.BMD),
    /** Bolivia (Plurinational State of) */
    BO("Bolivia",                                              Currency.BOB),
    /** Brazil */
    BR("Brazil",                                               Currency.BRL),
    /** Barbados */
    BB("Barbados",                                             Currency.BBD),
    /** Brunei Darussalam */
    BN("Brunei Darussalam",                                    Currency.BND),
    /** Bhutan */
    BT("Bhutan",                                               Currency.BTN),
    /** Bouvet Island */
    BV("Bouvet Island",                                        Currency.NOK),
    /** Botswana */
    BW("Botswana",                                             Currency.BWP),
    /** Central African Republic */
    CF("Central African Republic",                             Currency.XAF),

    // -------------------------------------------------------------------------
    // C
    // -------------------------------------------------------------------------
    /** Canada */
    CA("Canada",                                               Currency.CAD),
    /** Cocos (Keeling) Islands */
    CC("Cocos (Keeling) Islands",                              Currency.AUD),
    /** Switzerland */
    CH("Switzerland",                                          Currency.CHF),
    /** Chile */
    CL("Chile",                                                Currency.CLP),
    /** China */
    CN("China",                                                Currency.CNY),
    /** Côte d'Ivoire */
    CI("Côte d'Ivoire",                                        Currency.XOF),
    /** Cameroon */
    CM("Cameroon",                                             Currency.XAF),
    /** Congo, Democratic Republic of the */
    CD("Congo, Democratic Republic of the",                    Currency.CDF),
    /** Congo */
    CG("Congo",                                                Currency.XAF),
    /** Cook Islands */
    CK("Cook Islands",                                         Currency.NZD),
    /** Colombia */
    CO("Colombia",                                             Currency.COP),
    /** Comoros */
    KM("Comoros",                                              Currency.KMF),
    /** Cabo Verde */
    CV("Cabo Verde",                                           Currency.CVE),
    /** Costa Rica */
    CR("Costa Rica",                                           Currency.CRC),
    /** Cuba */
    CU("Cuba",                                                 Currency.CUP),
    /** Curaçao */
    CW("Curaçao",                                              Currency.ANG),
    /** Christmas Island */
    CX("Christmas Island",                                     Currency.AUD),
    /** Cayman Islands */
    KY("Cayman Islands",                                       Currency.KYD),
    /** Cyprus */
    CY("Cyprus",                                               Currency.EUR),
    /** Czechia */
    CZ("Czechia",                                              Currency.CZK),

    // -------------------------------------------------------------------------
    // D
    // -------------------------------------------------------------------------
    /** Germany */
    DE("Germany",                                              Currency.EUR),
    /** Djibouti */
    DJ("Djibouti",                                             Currency.DJF),
    /** Dominica */
    DM("Dominica",                                             Currency.XCD),
    /** Denmark */
    DK("Denmark",                                              Currency.DKK),
    /** Dominican Republic */
    DO("Dominican Republic",                                   Currency.DOP),
    /** Algeria */
    DZ("Algeria",                                              Currency.DZD),

    // -------------------------------------------------------------------------
    // E
    // -------------------------------------------------------------------------
    /** Ecuador */
    EC("Ecuador",                                              Currency.USD),
    /** Egypt */
    EG("Egypt",                                                Currency.EGP),
    /** Eritrea */
    ER("Eritrea",                                              Currency.ERN),
    /** Western Sahara */
    EH("Western Sahara",                                       Currency.MAD),
    /** Spain */
    ES("Spain",                                                Currency.EUR),
    /** Estonia */
    EE("Estonia",                                              Currency.EUR),
    /** Ethiopia */
    ET("Ethiopia",                                             Currency.ETB),

    // -------------------------------------------------------------------------
    // F
    // -------------------------------------------------------------------------
    /** Finland */
    FI("Finland",                                              Currency.EUR),
    /** Fiji */
    FJ("Fiji",                                                 Currency.FJD),
    /** Falkland Islands (Malvinas) */
    FK("Falkland Islands (Malvinas)",                          Currency.FKP),
    /** France */
    FR("France",                                               Currency.EUR),
    /** Faroe Islands */
    FO("Faroe Islands",                                        Currency.DKK),
    /** Micronesia (Federated States of) */
    FM("Micronesia (Federated States of)",                     Currency.USD),

    // -------------------------------------------------------------------------
    // G
    // -------------------------------------------------------------------------
    /** Gabon */
    GA("Gabon",                                                Currency.XAF),
    /** United Kingdom of Great Britain and Northern Ireland */
    GB("United Kingdom of Great Britain and Northern Ireland", Currency.GBP),
    /** Georgia */
    GE("Georgia",                                              Currency.GEL),
    /** Guernsey */
    GG("Guernsey",                                             Currency.GBP),
    /** Ghana */
    GH("Ghana",                                                Currency.GHS),
    /** Gibraltar */
    GI("Gibraltar",                                            Currency.GIP),
    /** Guinea */
    GN("Guinea",                                               Currency.GNF),
    /** Guadeloupe */
    GP("Guadeloupe",                                           Currency.EUR),
    /** Gambia */
    GM("Gambia",                                               Currency.GMD),
    /** Guinea-Bissau */
    GW("Guinea-Bissau",                                        Currency.XOF),
    /** Equatorial Guinea */
    GQ("Equatorial Guinea",                                    Currency.XAF),
    /** Greece */
    GR("Greece",                                               Currency.EUR),
    /** Grenada */
    GD("Grenada",                                              Currency.XCD),
    /** Greenland */
    GL("Greenland",                                            Currency.DKK),
    /** Guatemala */
    GT("Guatemala",                                            Currency.GTQ),
    /** French Guiana */
    GF("French Guiana",                                        Currency.EUR),
    /** Guam */
    GU("Guam",                                                 Currency.USD),
    /** Guyana */
    GY("Guyana",                                               Currency.GYD),
    /** Hong Kong */
    HK("Hong Kong",                                            Currency.HKD),

    // -------------------------------------------------------------------------
    // H
    // -------------------------------------------------------------------------
    /** Heard Island and McDonald Islands */
    HM("Heard Island and McDonald Islands",                    Currency.AUD),
    /** Honduras */
    HN("Honduras",                                             Currency.HNL),
    /** Croatia */
    HR("Croatia",                                              Currency.EUR),
    /** Haiti */
    HT("Haiti",                                                Currency.HTG),
    /** Hungary */
    HU("Hungary",                                              Currency.HUF),

    // -------------------------------------------------------------------------
    // I
    // -------------------------------------------------------------------------
    /** Indonesia */
    ID("Indonesia",                                            Currency.IDR),
    /** Isle of Man */
    IM("Isle of Man",                                          Currency.GBP),
    /** India */
    IN("India",                                                Currency.INR),
    /** British Indian Ocean Territory */
    IO("British Indian Ocean Territory",                       Currency.USD),
    /** Ireland */
    IE("Ireland",                                              Currency.EUR),
    /** Iran (Islamic Republic of) */
    IR("Iran (Islamic Republic of)",                           Currency.IRR),
    /** Iraq */
    IQ("Iraq",                                                 Currency.IQD),
    /** Iceland */
    IS("Iceland",                                              Currency.ISK),
    /** Israel */
    IL("Israel",                                               Currency.ILS),
    /** Italy */
    IT("Italy",                                                Currency.EUR),

    // -------------------------------------------------------------------------
    // J
    // -------------------------------------------------------------------------
    /** Jamaica */
    JM("Jamaica",                                              Currency.JMD),
    /** Jersey */
    JE("Jersey",                                               Currency.GBP),
    /** Jordan */
    JO("Jordan",                                               Currency.JOD),
    /** Japan */
    JP("Japan",                                                Currency.JPY),

    // -------------------------------------------------------------------------
    // K
    // -------------------------------------------------------------------------
    /** Kenya */
    KE("Kenya",                                                Currency.KES),
    /** Kyrgyzstan */
    KG("Kyrgyzstan",                                           Currency.KGS),
    /** Cambodia */
    KH("Cambodia",                                             Currency.KHR),
    /** Kiribati */
    KI("Kiribati",                                             Currency.AUD),
    /** Saint Kitts and Nevis */
    KN("Saint Kitts and Nevis",                                Currency.XCD),
    /** Korea, Republic of */
    KR("Korea, Republic of",                                   Currency.KRW),
    /** Kuwait */
    KW("Kuwait",                                               Currency.KWD),
    /** Kazakhstan */
    KZ("Kazakhstan",                                           Currency.KZT),
    /** Korea (Democratic People's Republic of) */
    KP("Korea (Democratic People's Republic of)",              Currency.KPW),

    // -------------------------------------------------------------------------
    // L
    // -------------------------------------------------------------------------
    /** Lao People's Democratic Republic */
    LA("Lao People's Democratic Republic",                     Currency.LAK),
    /** Lebanon */
    LB("Lebanon",                                              Currency.LBP),
    /** Liberia */
    LR("Liberia",                                              Currency.LRD),
    /** Libya */
    LY("Libya",                                                Currency.LYD),
    /** Saint Lucia */
    LC("Saint Lucia",                                          Currency.XCD),
    /** Liechtenstein */
    LI("Liechtenstein",                                        Currency.CHF),
    /** Sri Lanka */
    LK("Sri Lanka",                                            Currency.LKR),
    /** Lesotho */
    LS("Lesotho",                                              Currency.LSL),
    /** Lithuania */
    LT("Lithuania",                                            Currency.EUR),
    /** Luxembourg */
    LU("Luxembourg",                                           Currency.EUR),
    /** Latvia */
    LV("Latvia",                                               Currency.EUR),

    // -------------------------------------------------------------------------
    // M
    // -------------------------------------------------------------------------
    /** Macao */
    MO("Macao",                                                Currency.MOP),
    /** Saint Martin (French part) */
    MF("Saint Martin (French part)",                           Currency.EUR),
    /** Morocco */
    MA("Morocco",                                              Currency.MAD),
    /** Monaco */
    MC("Monaco",                                               Currency.EUR),
    /** Moldova, Republic of */
    MD("Moldova, Republic of",                                 Currency.MDL),
    /** Madagascar */
    MG("Madagascar",                                           Currency.MGA),
    /** Maldives */
    MV("Maldives",                                             Currency.MVR),
    /** Mexico */
    MX("Mexico",                                               Currency.MXN),
    /** Marshall Islands */
    MH("Marshall Islands",                                     Currency.USD),
    /** North Macedonia */
    MK("North Macedonia",                                      Currency.MKD),
    /** Mali */
    ML("Mali",                                                 Currency.XOF),
    /** Malta */
    MT("Malta",                                                Currency.EUR),
    /** Myanmar */
    MM("Myanmar",                                              Currency.MMK),
    /** Mongolia */
    MN("Mongolia",                                             Currency.MNT),
    /** Northern Mariana Islands */
    MP("Northern Mariana Islands",                             Currency.USD),
    /** Mozambique */
    MZ("Mozambique",                                           Currency.MZN),
    /** Mauritania */
    MR("Mauritania",                                           Currency.MRU),
    /** Montenegro */
    ME("Montenegro",                                           Currency.EUR),
    /** Montserrat */
    MS("Montserrat",                                           Currency.XCD),
    /** Martinique */
    MQ("Martinique",                                           Currency.EUR),
    /** Mauritius */
    MU("Mauritius",                                            Currency.MUR),
    /** Malawi */
    MW("Malawi",                                               Currency.MWK),
    /** Malaysia */
    MY("Malaysia",                                             Currency.MYR),
    /** Mayotte */
    YT("Mayotte",                                              Currency.EUR),

    // -------------------------------------------------------------------------
    // N
    // -------------------------------------------------------------------------
    /** Namibia */
    NA("Namibia",                                              Currency.NAD),
    /** New Caledonia */
    NC("New Caledonia",                                        Currency.XPF),
    /** Niger */
    NE("Niger",                                                Currency.XOF),
    /** Norfolk Island */
    NF("Norfolk Island",                                       Currency.AUD),
    /** Nigeria */
    NG("Nigeria",                                              Currency.NGN),
    /** Nicaragua */
    NI("Nicaragua",                                            Currency.NIO),
    /** Niue */
    NU("Niue",                                                 Currency.NZD),
    /** Netherlands */
    NL("Netherlands",                                          Currency.EUR),
    /** Norway */
    NO("Norway",                                               Currency.NOK),
    /** Nepal */
    NP("Nepal",                                                Currency.NPR),
    /** Nauru */
    NR("Nauru",                                                Currency.AUD),
    /** New Zealand */
    NZ("New Zealand",                                          Currency.NZD),

    // -------------------------------------------------------------------------
    // O
    // -------------------------------------------------------------------------
    /** Oman */
    OM("Oman",                                                 Currency.OMR),

    // -------------------------------------------------------------------------
    // P
    // -------------------------------------------------------------------------
    /** Panama */
    PA("Panama",                                               Currency.USD),
    /** Peru */
    PE("Peru",                                                 Currency.PEN),
    /** French Polynesia */
    PF("French Polynesia",                                     Currency.XPF),
    /** Papua New Guinea */
    PG("Papua New Guinea",                                     Currency.PGK),
    /** Philippines */
    PH("Philippines",                                          Currency.PHP),
    /** Pakistan */
    PK("Pakistan",                                             Currency.PKR),
    /** Poland */
    PL("Poland",                                               Currency.PLN),
    /** Saint Pierre and Miquelon */
    PM("Saint Pierre and Miquelon",                            Currency.EUR),
    /** Pitcairn */
    PN("Pitcairn",                                             Currency.NZD),
    /** Puerto Rico */
    PR("Puerto Rico",                                          Currency.USD),
    /** Palestine, State of */
    PS("Palestine, State of",                                  Currency.ILS),
    /** Portugal */
    PT("Portugal",                                             Currency.EUR),
    /** Palau */
    PW("Palau",                                                Currency.USD),
    /** Paraguay */
    PY("Paraguay",                                             Currency.PYG),

    // -------------------------------------------------------------------------
    // Q
    // -------------------------------------------------------------------------
    /** Qatar */
    QA("Qatar",                                                Currency.QAR),

    // -------------------------------------------------------------------------
    // R
    // -------------------------------------------------------------------------
    /** Réunion */
    RE("Réunion",                                              Currency.EUR),
    /** Romania */
    RO("Romania",                                              Currency.RON),
    /** Serbia */
    RS("Serbia",                                               Currency.RSD),
    /** Russian Federation */
    RU("Russian Federation",                                   Currency.RUB),
    /** Rwanda */
    RW("Rwanda",                                               Currency.RWF),

    // -------------------------------------------------------------------------
    // S
    // -------------------------------------------------------------------------
    /** Saudi Arabia */
    SA("Saudi Arabia",                                         Currency.SAR),
    /** Solomon Islands */
    SB("Solomon Islands",                                      Currency.SBD),
    /** Sudan */
    SD("Sudan",                                                Currency.SDG),
    /** Sweden */
    SE("Sweden",                                               Currency.SEK),
    /** Singapore */
    SG("Singapore",                                            Currency.SGD),
    /** Saint Helena, Ascension and Tristan da Cunha */
    SH("Saint Helena, Ascension and Tristan da Cunha",         Currency.SHP),
    /** Slovenia */
    SI("Slovenia",                                             Currency.EUR),
    /** Svalbard and Jan Mayen */
    SJ("Svalbard and Jan Mayen",                               Currency.NOK),
    /** Slovakia */
    SK("Slovakia",                                             Currency.EUR),
    /** Sierra Leone */
    SL("Sierra Leone",                                         Currency.SLE),
    /** San Marino */
    SM("San Marino",                                           Currency.EUR),
    /** Senegal */
    SN("Senegal",                                              Currency.XOF),
    /** Somalia */
    SO("Somalia",                                              Currency.SOS),
    /** Suriname */
    SR("Suriname",                                             Currency.SRD),
    /** South Sudan */
    SS("South Sudan",                                          Currency.SSP),
    /** São Tomé and Príncipe */
    ST("São Tomé and Príncipe",                                Currency.STN),
    /** El Salvador */
    SV("El Salvador",                                          Currency.USD),
    /** Sint Maarten (Dutch part) */
    SX("Sint Maarten (Dutch part)",                            Currency.ANG),
    /** Syrian Arab Republic */
    SY("Syrian Arab Republic",                                 Currency.SYP),
    /** Eswatini */
    SZ("Eswatini",                                             Currency.SZL),
    /** Seychelles */
    SC("Seychelles",                                           Currency.SCR),

    // -------------------------------------------------------------------------
    // T
    // -------------------------------------------------------------------------
    /** Turks and Caicos Islands */
    TC("Turks and Caicos Islands",                             Currency.USD),
    /** Chad */
    TD("Chad",                                                 Currency.XAF),
    /** Togo */
    TG("Togo",                                                 Currency.XOF),
    /** Thailand */
    TH("Thailand",                                             Currency.THB),
    /** Tajikistan */
    TJ("Tajikistan",                                           Currency.TJS),
    /** Tokelau */
    TK("Tokelau",                                              Currency.NZD),
    /** Turkmenistan */
    TM("Turkmenistan",                                         Currency.TMT),
    /** Timor-Leste */
    TL("Timor-Leste",                                          Currency.USD),
    /** Tonga */
    TO("Tonga",                                                Currency.TOP),
    /** Trinidad and Tobago */
    TT("Trinidad and Tobago",                                  Currency.TTD),
    /** Tunisia */
    TN("Tunisia",                                              Currency.TND),
    /** Türkiye */
    TR("Türkiye",                                              Currency.TRY),
    /** Tuvalu */
    TV("Tuvalu",                                               Currency.AUD),
    /** Taiwan, Province of China */
    TW("Taiwan, Province of China",                            Currency.TWD),
    /** Tanzania, United Republic of */
    TZ("Tanzania, United Republic of",                         Currency.TZS),

    // -------------------------------------------------------------------------
    // U
    // -------------------------------------------------------------------------
    /** Ukraine */
    UA("Ukraine",                                              Currency.UAH),
    /** Uganda */
    UG("Uganda",                                               Currency.UGX),
    /** United States Minor Outlying Islands */
    UM("United States Minor Outlying Islands",                 Currency.USD),
    /** Uruguay */
    UY("Uruguay",                                              Currency.UYU),
    /** Uzbekistan */
    UZ("Uzbekistan",                                           Currency.UZS),
    /** United States of America */
    US("United States of America",                             Currency.USD),

    // -------------------------------------------------------------------------
    // V
    // -------------------------------------------------------------------------
    /** Holy See */
    VA("Holy See",                                             Currency.EUR),
    /** Saint Vincent and the Grenadines */
    VC("Saint Vincent and the Grenadines",                     Currency.XCD),
    /** Venezuela (Bolivarian Republic of) */
    VE("Venezuela (Bolivarian Republic of)",                   Currency.VES),
    /** Virgin Islands (British) */
    VG("Virgin Islands (British)",                             Currency.USD),
    /** Virgin Islands (U.S.) */
    VI("Virgin Islands (U.S.)",                                Currency.USD),
    /** Viet Nam */
    VN("Viet Nam",                                             Currency.VND),
    /** Vanuatu */
    VU("Vanuatu",                                              Currency.VUV),

    // -------------------------------------------------------------------------
    // W
    // -------------------------------------------------------------------------
    /** Wallis and Futuna */
    WF("Wallis and Futuna",                                    Currency.XPF),
    /** Samoa */
    WS("Samoa",                                                Currency.WST),

    // -------------------------------------------------------------------------
    // X  (user-assigned, but widely adopted in banking)
    // -------------------------------------------------------------------------
    /** Kosovo (user-assigned; widely adopted in banking and payment systems) */
    XK("Kosovo",                                               Currency.EUR),

    // -------------------------------------------------------------------------
    // Y
    // -------------------------------------------------------------------------
    /** Yemen */
    YE("Yemen",                                                Currency.YER),

    // -------------------------------------------------------------------------
    // Z
    // -------------------------------------------------------------------------
    /** South Africa */
    ZA("South Africa",                                         Currency.ZAR),
    /** Zambia */
    ZM("Zambia",                                               Currency.ZMW),
    /** Zimbabwe */
    ZW("Zimbabwe",                                             Currency.ZWL);

    /**
     * Unmodifiable lookup map from two-letter code string to enum constant.<br>
     * Preserves declaration order via {@link LinkedHashMap}.
     */
    private static final Map<String, Iso3166Alpha2> LOOKUP = buildLookup();

    /** The English short country name as defined in ISO 3166-1. */
    private final String                            countryName;

    /**
     * The primary ISO 4217 currency used in this country or territory.
     * <p>
     * {@code null} only for {@link #AQ} (Antarctica), which has no currency in use.
     */
    private final Currency                          currency;

    Iso3166Alpha2(final String countryName, final Currency currency) {
        this.countryName = countryName;
        this.currency    = currency;
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
     * Returns the primary ISO 4217 currency used in this country or territory.
     * <p>
     * The only constant that returns {@code null} is {@link #AQ} (Antarctica),
     * which has no internationally recognized currency in use.
     *
     * @return the {@link Currency} constant, or {@code null} for {@link #AQ}
     *
     * @since 1.8.5
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Looks up the enum constant for the given two-letter country code.
     * <p>
     * Accepts any {@link CharSequence} implementation ({@link String}, {@link StringBuilder},
     * {@link StringBuffer}, etc.) so callers are not forced to materialize a {@code String}
     * solely for this lookup.
     * <p>
     * The lookup is case-sensitive; only uppercase codes are recognized
     * (e.g., {@code "LT"} matches, {@code "lt"} does not).
     * Leading or trailing whitespace is <em>not</em> stripped — {@code " DE"} is not
     * a valid two-letter code and returns {@code null}.
     *
     * @param code the two-letter ISO 3166-1 Alpha-2 code (case-sensitive, e.g., {@code "LV"});
     *             may be any {@link CharSequence}
     * @return the matching {@link Iso3166Alpha2} constant,
     *         or {@code null} if the code is {@code null}, not exactly two characters, or unknown
     */
    public static Iso3166Alpha2 fromCode(final CharSequence code) {
        if (code == null || code.length() != 2) {
            return null;
        }
        return LOOKUP.get(code.toString());
    }

    /**
     * Checks whether the given value is a valid, officially assigned ISO 3166-1 Alpha-2 code.
     * <p>
     * Accepts any {@link CharSequence} implementation ({@link String}, {@link StringBuilder},
     * {@link StringBuffer}, etc.).
     * <p>
     * This method performs an exact lookup against the full list of assigned codes.
     * Pure format checks (two uppercase letters A–Z) are <em>not</em> sufficient —
     * for example, {@code "AA"} or {@code "ZZ"} pass a format check but are not assigned.
     *
     * @param code the two-letter code to check (case-sensitive); may be any {@link CharSequence}
     * @return {@code true} if the code is an officially assigned ISO 3166-1 Alpha-2 code;
     *         {@code false} otherwise
     */
    public static boolean isAssigned(final CharSequence code) {
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

