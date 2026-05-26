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
 * Enumeration of all officially assigned ISO 3166-1 Alpha-2 country codes
 * as published by the ISO 3166 Maintenance Agency.
 * <p>
 * Each constant carries the two-letter code (the enum name itself), the
 * corresponding English short name as specified in ISO 3166-1, the
 * primary ISO 4217 currency code used in that country or territory, and the
 * associated continent.
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
 * @since 1.8.8 (perviously named Iso3166Alpha2 - since 1.8.4)
 */
public enum Country {

    // -------------------------------------------------------------------------
    // A
    // -------------------------------------------------------------------------
    /** Andorra */
    AD("Andorra",                                              Currency.EUR, Continent.EUROPE),
    /** United Arab Emirates */
    AE("United Arab Emirates",                                 Currency.AED, Continent.ASIA),
    /** Afghanistan */
    AF("Afghanistan",                                          Currency.AFN, Continent.ASIA),
    /** Antigua and Barbuda */
    AG("Antigua and Barbuda",                                  Currency.XCD, Continent.NORTH_AMERICA),
    /** Anguilla */
    AI("Anguilla",                                             Currency.XCD, Continent.NORTH_AMERICA),
    /** Albania */
    AL("Albania",                                              Currency.ALL, Continent.EUROPE),
    /** Armenia */
    AM("Armenia",                                              Currency.AMD, Continent.ASIA),
    /** Angola */
    AO("Angola",                                               Currency.AOA, Continent.AFRICA),
    /** Antarctica — no currency in use */
    AQ("Antarctica",                                           null,         Continent.ANTARCTICA),
    /** Argentina */
    AR("Argentina",                                            Currency.ARS, Continent.SOUTH_AMERICA),
    /** American Samoa */
    AS("American Samoa",                                       Currency.USD, Continent.OCEANIA),
    /** Austria */
    AT("Austria",                                              Currency.EUR, Continent.EUROPE),
    /** Australia */
    AU("Australia",                                            Currency.AUD, Continent.OCEANIA),
    /** Aruba */
    AW("Aruba",                                                Currency.AWG, Continent.NORTH_AMERICA),
    /** Åland Islands */
    AX("Åland Islands",                                        Currency.EUR, Continent.EUROPE),
    /** Azerbaijan */
    AZ("Azerbaijan",                                           Currency.AZN, Continent.ASIA),

    // -------------------------------------------------------------------------
    // B
    // -------------------------------------------------------------------------
    /** Bosnia and Herzegovina */
    BA("Bosnia and Herzegovina",                               Currency.BAM, Continent.EUROPE),
    /** Barbados */
    BB("Barbados",                                             Currency.BBD, Continent.NORTH_AMERICA),
    /** Bangladesh */
    BD("Bangladesh",                                           Currency.BDT, Continent.ASIA),
    /** Belgium */
    BE("Belgium",                                              Currency.EUR, Continent.EUROPE),
    /** Burkina Faso */
    BF("Burkina Faso",                                         Currency.XOF, Continent.AFRICA),
    /** Bulgaria */
    BG("Bulgaria",                                             Currency.BGN, Continent.EUROPE),
    /** Bahrain */
    BH("Bahrain",                                              Currency.BHD, Continent.ASIA),
    /** Burundi */
    BI("Burundi",                                              Currency.BIF, Continent.AFRICA),
    /** Benin */
    BJ("Benin",                                                Currency.XOF, Continent.AFRICA),
    /** Saint Barthélemy */
    BL("Saint Barthélemy",                                     Currency.EUR, Continent.NORTH_AMERICA),
    /** Bermuda */
    BM("Bermuda",                                              Currency.BMD, Continent.NORTH_AMERICA),
    /** Brunei Darussalam */
    BN("Brunei Darussalam",                                    Currency.BND, Continent.ASIA),
    /** Bolivia (Plurinational State of) */
    BO("Bolivia",                                              Currency.BOB, Continent.SOUTH_AMERICA),
    /** Bonaire, Sint Eustatius and Saba */
    BQ("Bonaire, Sint Eustatius and Saba",                     Currency.USD, Continent.NORTH_AMERICA),
    /** Brazil */
    BR("Brazil",                                               Currency.BRL, Continent.SOUTH_AMERICA),
    /** Bahamas */
    BS("Bahamas",                                              Currency.BSD, Continent.NORTH_AMERICA),
    /** Bhutan */
    BT("Bhutan",                                               Currency.BTN, Continent.ASIA),
    /** Bouvet Island */
    BV("Bouvet Island",                                        Currency.NOK, Continent.ANTARCTICA),
    /** Botswana */
    BW("Botswana",                                             Currency.BWP, Continent.AFRICA),
    /** Belarus */
    BY("Belarus",                                              Currency.BYN, Continent.EUROPE),
    /** Belize */
    BZ("Belize",                                               Currency.BZD, Continent.NORTH_AMERICA),

    // -------------------------------------------------------------------------
    // C
    // -------------------------------------------------------------------------
    /** Canada */
    CA("Canada",                                               Currency.CAD, Continent.NORTH_AMERICA),
    /** Cocos (Keeling) Islands */
    CC("Cocos (Keeling) Islands",                              Currency.AUD, Continent.OCEANIA),
    /** Congo, Democratic Republic of the */
    CD("Congo, Democratic Republic of the",                    Currency.CDF, Continent.AFRICA),
    /** Central African Republic */
    CF("Central African Republic",                             Currency.XAF, Continent.AFRICA),
    /** Congo */
    CG("Congo",                                                Currency.XAF, Continent.AFRICA),
    /** Switzerland */
    CH("Switzerland",                                          Currency.CHF, Continent.EUROPE),
    /** Côte d'Ivoire */
    CI("Côte d'Ivoire",                                        Currency.XOF, Continent.AFRICA),
    /** Cook Islands */
    CK("Cook Islands",                                         Currency.NZD, Continent.OCEANIA),
    /** Chile */
    CL("Chile",                                                Currency.CLP, Continent.SOUTH_AMERICA),
    /** Cameroon */
    CM("Cameroon",                                             Currency.XAF, Continent.AFRICA),
    /** China */
    CN("China",                                                Currency.CNY, Continent.ASIA),
    /** Colombia */
    CO("Colombia",                                             Currency.COP, Continent.SOUTH_AMERICA),
    /** Costa Rica */
    CR("Costa Rica",                                           Currency.CRC, Continent.NORTH_AMERICA),
    /** Cuba */
    CU("Cuba",                                                 Currency.CUP, Continent.NORTH_AMERICA),
    /** Cabo Verde */
    CV("Cabo Verde",                                           Currency.CVE, Continent.AFRICA),
    /** Curaçao */
    CW("Curaçao",                                              Currency.ANG, Continent.NORTH_AMERICA),
    /** Christmas Island */
    CX("Christmas Island",                                     Currency.AUD, Continent.OCEANIA),
    /** Cyprus */
    CY("Cyprus",                                               Currency.EUR, Continent.EUROPE),
    /** Czechia */
    CZ("Czechia",                                              Currency.CZK, Continent.EUROPE),

    // -------------------------------------------------------------------------
    // D
    // -------------------------------------------------------------------------
    /** Germany */
    DE("Germany",                                              Currency.EUR, Continent.EUROPE),
    /** Djibouti */
    DJ("Djibouti",                                             Currency.DJF, Continent.AFRICA),
    /** Denmark */
    DK("Denmark",                                              Currency.DKK, Continent.EUROPE),
    /** Dominica */
    DM("Dominica",                                             Currency.XCD, Continent.NORTH_AMERICA),
    /** Dominican Republic */
    DO("Dominican Republic",                                   Currency.DOP, Continent.NORTH_AMERICA),
    /** Algeria */
    DZ("Algeria",                                              Currency.DZD, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // E
    // -------------------------------------------------------------------------
    /** Ecuador */
    EC("Ecuador",                                              Currency.USD, Continent.SOUTH_AMERICA),
    /** Estonia */
    EE("Estonia",                                              Currency.EUR, Continent.EUROPE),
    /** Egypt */
    EG("Egypt",                                                Currency.EGP, Continent.AFRICA),
    /** Western Sahara */
    EH("Western Sahara",                                       Currency.MAD, Continent.AFRICA),
    /** Eritrea */
    ER("Eritrea",                                              Currency.ERN, Continent.AFRICA),
    /** Spain */
    ES("Spain",                                                Currency.EUR, Continent.EUROPE),
    /** Ethiopia */
    ET("Ethiopia",                                             Currency.ETB, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // F
    // -------------------------------------------------------------------------
    /** Finland */
    FI("Finland",                                              Currency.EUR, Continent.EUROPE),
    /** Fiji */
    FJ("Fiji",                                                 Currency.FJD, Continent.OCEANIA),
    /** Falkland Islands (Malvinas) */
    FK("Falkland Islands (Malvinas)",                          Currency.FKP, Continent.SOUTH_AMERICA),
    /** Micronesia (Federated States of) */
    FM("Micronesia (Federated States of)",                     Currency.USD, Continent.OCEANIA),
    /** Faroe Islands */
    FO("Faroe Islands",                                        Currency.DKK, Continent.EUROPE),
    /** France */
    FR("France",                                               Currency.EUR, Continent.EUROPE),

    // -------------------------------------------------------------------------
    // G
    // -------------------------------------------------------------------------
    /** Gabon */
    GA("Gabon",                                                Currency.XAF, Continent.AFRICA),
    /** United Kingdom of Great Britain and Northern Ireland */
    GB("United Kingdom of Great Britain and Northern Ireland", Currency.GBP, Continent.EUROPE),
    /** Grenada */
    GD("Grenada",                                              Currency.XCD, Continent.NORTH_AMERICA),
    /** Georgia */
    GE("Georgia",                                              Currency.GEL, Continent.ASIA),
    /** French Guiana */
    GF("French Guiana",                                        Currency.EUR, Continent.SOUTH_AMERICA),
    /** Guernsey */
    GG("Guernsey",                                             Currency.GBP, Continent.EUROPE),
    /** Ghana */
    GH("Ghana",                                                Currency.GHS, Continent.AFRICA),
    /** Gibraltar */
    GI("Gibraltar",                                            Currency.GIP, Continent.EUROPE),
    /** Greenland */
    GL("Greenland",                                            Currency.DKK, Continent.NORTH_AMERICA),
    /** Gambia */
    GM("Gambia",                                               Currency.GMD, Continent.AFRICA),
    /** Guinea */
    GN("Guinea",                                               Currency.GNF, Continent.AFRICA),
    /** Guadeloupe */
    GP("Guadeloupe",                                           Currency.EUR, Continent.NORTH_AMERICA),
    /** Equatorial Guinea */
    GQ("Equatorial Guinea",                                    Currency.XAF, Continent.AFRICA),
    /** Greece */
    GR("Greece",                                               Currency.EUR, Continent.EUROPE),
    /** Guatemala */
    GT("Guatemala",                                            Currency.GTQ, Continent.NORTH_AMERICA),
    /** Guam */
    GU("Guam",                                                 Currency.USD, Continent.OCEANIA),
    /** Guinea-Bissau */
    GW("Guinea-Bissau",                                        Currency.XOF, Continent.AFRICA),
    /** Guyana */
    GY("Guyana",                                               Currency.GYD, Continent.SOUTH_AMERICA),

    // -------------------------------------------------------------------------
    // H
    // -------------------------------------------------------------------------
    /** Hong Kong */
    HK("Hong Kong",                                            Currency.HKD, Continent.ASIA),
    /** Heard Island and McDonald Islands */
    HM("Heard Island and McDonald Islands",                    Currency.AUD, Continent.ANTARCTICA),
    /** Honduras */
    HN("Honduras",                                             Currency.HNL, Continent.NORTH_AMERICA),
    /** Croatia */
    HR("Croatia",                                              Currency.EUR, Continent.EUROPE),
    /** Haiti */
    HT("Haiti",                                                Currency.HTG, Continent.NORTH_AMERICA),
    /** Hungary */
    HU("Hungary",                                              Currency.HUF, Continent.EUROPE),

    // -------------------------------------------------------------------------
    // I
    // -------------------------------------------------------------------------
    /** Indonesia */
    ID("Indonesia",                                            Currency.IDR, Continent.ASIA),
    /** Ireland */
    IE("Ireland",                                              Currency.EUR, Continent.EUROPE),
    /** Israel */
    IL("Israel",                                               Currency.ILS, Continent.ASIA),
    /** Isle of Man */
    IM("Isle of Man",                                          Currency.GBP, Continent.EUROPE),
    /** India */
    IN("India",                                                Currency.INR, Continent.ASIA),
    /** British Indian Ocean Territory */
    IO("British Indian Ocean Territory",                       Currency.USD, Continent.ASIA),
    /** Iraq */
    IQ("Iraq",                                                 Currency.IQD, Continent.ASIA),
    /** Iran (Islamic Republic of) */
    IR("Iran (Islamic Republic of)",                           Currency.IRR, Continent.ASIA),
    /** Iceland */
    IS("Iceland",                                              Currency.ISK, Continent.EUROPE),
    /** Italy */
    IT("Italy",                                                Currency.EUR, Continent.EUROPE),

    // -------------------------------------------------------------------------
    // J
    // -------------------------------------------------------------------------
    /** Jersey */
    JE("Jersey",                                               Currency.GBP, Continent.EUROPE),
    /** Jamaica */
    JM("Jamaica",                                              Currency.JMD, Continent.NORTH_AMERICA),
    /** Jordan */
    JO("Jordan",                                               Currency.JOD,  Continent.ASIA),
    /** Japan */
    JP("Japan",                                                Currency.JPY, Continent.ASIA),

    // -------------------------------------------------------------------------
    // K
    // -------------------------------------------------------------------------
    /** Kenya */
    KE("Kenya",                                                Currency.KES, Continent.AFRICA),
    /** Kyrgyzstan */
    KG("Kyrgyzstan",                                           Currency.KGS, Continent.ASIA),
    /** Cambodia */
    KH("Cambodia",                                             Currency.KHR, Continent.ASIA),
    /** Kiribati */
    KI("Kiribati",                                             Currency.AUD, Continent.OCEANIA),
    /** Comoros */
    KM("Comoros",                                              Currency.KMF, Continent.AFRICA),
    /** Saint Kitts and Nevis */
    KN("Saint Kitts and Nevis",                                Currency.XCD, Continent.NORTH_AMERICA),
    /** Korea (Democratic People's Republic of) */
    KP("Korea (Democratic People's Republic of)",              Currency.KPW, Continent.ASIA),
    /** Korea, Republic of */
    KR("Korea, Republic of",                                   Currency.KRW, Continent.ASIA),
    /** Kuwait */
    KW("Kuwait",                                               Currency.KWD, Continent.ASIA),
    /** Cayman Islands */
    KY("Cayman Islands",                                       Currency.KYD, Continent.NORTH_AMERICA),
    /** Kazakhstan */
    KZ("Kazakhstan",                                           Currency.KZT, Continent.ASIA),

    // -------------------------------------------------------------------------
    // L
    // -------------------------------------------------------------------------
    /** Lao People's Democratic Republic */
    LA("Lao People's Democratic Republic",                     Currency.LAK, Continent.ASIA),
    /** Lebanon */
    LB("Lebanon",                                              Currency.LBP, Continent.ASIA),
    /** Saint Lucia */
    LC("Saint Lucia",                                          Currency.XCD, Continent.NORTH_AMERICA),
    /** Liechtenstein */
    LI("Liechtenstein",                                        Currency.CHF, Continent.EUROPE),
    /** Sri Lanka */
    LK("Sri Lanka",                                            Currency.LKR, Continent.ASIA),
    /** Liberia */
    LR("Liberia",                                              Currency.LRD, Continent.AFRICA),
    /** Lesotho */
    LS("Lesotho",                                              Currency.LSL, Continent.AFRICA),
    /** Lithuania */
    LT("Lithuania",                                            Currency.EUR, Continent.EUROPE),
    /** Luxembourg */
    LU("Luxembourg",                                           Currency.EUR, Continent.EUROPE),
    /** Latvia */
    LV("Latvia",                                               Currency.EUR, Continent.EUROPE),
    /** Libya */
    LY("Libya",                                                Currency.LYD, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // M
    // -------------------------------------------------------------------------
    /** Morocco */
    MA("Morocco",                                              Currency.MAD, Continent.AFRICA),
    /** Monaco */
    MC("Monaco",                                               Currency.EUR, Continent.EUROPE),
    /** Moldova, Republic of */
    MD("Moldova, Republic of",                                 Currency.MDL, Continent.EUROPE),
    /** Montenegro */
    ME("Montenegro",                                           Currency.EUR, Continent.EUROPE),
    /** Saint Martin (French part) */
    MF("Saint Martin (French part)",                           Currency.EUR, Continent.NORTH_AMERICA),
    /** Madagascar */
    MG("Madagascar",                                           Currency.MGA, Continent.AFRICA),
    /** Marshall Islands */
    MH("Marshall Islands",                                     Currency.USD, Continent.OCEANIA),
    /** North Macedonia */
    MK("North Macedonia",                                      Currency.MKD, Continent.EUROPE),
    /** Mali */
    ML("Mali",                                                 Currency.XOF, Continent.AFRICA),
    /** Myanmar */
    MM("Myanmar",                                              Currency.MMK, Continent.ASIA),
    /** Mongolia */
    MN("Mongolia",                                             Currency.MNT, Continent.ASIA),
    /** Macao */
    MO("Macao",                                                Currency.MOP, Continent.ASIA),
    /** Northern Mariana Islands */
    MP("Northern Mariana Islands",                             Currency.USD, Continent.OCEANIA),
    /** Martinique */
    MQ("Martinique",                                           Currency.EUR, Continent.NORTH_AMERICA),
    /** Mauritania */
    MR("Mauritania",                                           Currency.MRU, Continent.AFRICA),
    /** Montserrat */
    MS("Montserrat",                                           Currency.XCD, Continent.NORTH_AMERICA),
    /** Malta */
    MT("Malta",                                                Currency.EUR, Continent.EUROPE),
    /** Mauritius */
    MU("Mauritius",                                            Currency.MUR, Continent.AFRICA),
    /** Maldives */
    MV("Maldives",                                             Currency.MVR, Continent.ASIA),
    /** Malawi */
    MW("Malawi",                                               Currency.MWK, Continent.AFRICA),
    /** Mexico */
    MX("Mexico",                                               Currency.MXN, Continent.NORTH_AMERICA),
    /** Malaysia */
    MY("Malaysia",                                             Currency.MYR, Continent.ASIA),
    /** Mozambique */
    MZ("Mozambique",                                           Currency.MZN, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // N
    // -------------------------------------------------------------------------
    /** Namibia */
    NA("Namibia",                                              Currency.NAD, Continent.AFRICA),
    /** New Caledonia */
    NC("New Caledonia",                                        Currency.XPF, Continent.OCEANIA),
    /** Niger */
    NE("Niger",                                                Currency.XOF, Continent.AFRICA),
    /** Norfolk Island */
    NF("Norfolk Island",                                       Currency.AUD, Continent.OCEANIA),
    /** Nigeria */
    NG("Nigeria",                                              Currency.NGN, Continent.AFRICA),
    /** Nicaragua */
    NI("Nicaragua",                                            Currency.NIO, Continent.NORTH_AMERICA),
    /** Netherlands */
    NL("Netherlands",                                          Currency.EUR, Continent.EUROPE),
    /** Norway */
    NO("Norway",                                               Currency.NOK, Continent.EUROPE),
    /** Nepal */
    NP("Nepal",                                                Currency.NPR, Continent.ASIA),
    /** Nauru */
    NR("Nauru",                                                Currency.AUD, Continent.OCEANIA),
    /** Niue */
    NU("Niue",                                                 Currency.NZD, Continent.OCEANIA),
    /** New Zealand */
    NZ("New Zealand",                                          Currency.NZD, Continent.OCEANIA),

    // -------------------------------------------------------------------------
    // O
    // -------------------------------------------------------------------------
    /** Oman */
    OM("Oman",                                                 Currency.OMR, Continent.ASIA),

    // -------------------------------------------------------------------------
    // P
    // -------------------------------------------------------------------------
    /** Panama */
    PA("Panama",                                               Currency.USD, Continent.NORTH_AMERICA),
    /** Peru */
    PE("Peru",                                                 Currency.PEN, Continent.SOUTH_AMERICA),
    /** French Polynesia */
    PF("French Polynesia",                                     Currency.XPF, Continent.OCEANIA),
    /** Papua New Guinea */
    PG("Papua New Guinea",                                     Currency.PGK, Continent.OCEANIA),
    /** Philippines */
    PH("Philippines",                                          Currency.PHP, Continent.ASIA),
    /** Pakistan */
    PK("Pakistan",                                             Currency.PKR, Continent.ASIA),
    /** Poland */
    PL("Poland",                                               Currency.PLN, Continent.EUROPE),
    /** Saint Pierre and Miquelon */
    PM("Saint Pierre and Miquelon",                            Currency.EUR, Continent.NORTH_AMERICA),
    /** Pitcairn */
    PN("Pitcairn",                                             Currency.NZD, Continent.OCEANIA),
    /** Puerto Rico */
    PR("Puerto Rico",                                          Currency.USD, Continent.NORTH_AMERICA),
    /** Palestine, State of */
    PS("Palestine, State of",                                  Currency.ILS, Continent.ASIA),
    /** Portugal */
    PT("Portugal",                                             Currency.EUR, Continent.EUROPE),
    /** Palau */
    PW("Palau",                                                Currency.USD, Continent.OCEANIA),
    /** Paraguay */
    PY("Paraguay",                                             Currency.PYG, Continent.SOUTH_AMERICA),

    // -------------------------------------------------------------------------
    // Q
    // -------------------------------------------------------------------------
    /** Qatar */
    QA("Qatar",                                                Currency.QAR, Continent.ASIA),

    // -------------------------------------------------------------------------
    // R
    // -------------------------------------------------------------------------
    /** Réunion */
    RE("Réunion",                                              Currency.EUR, Continent.AFRICA),
    /** Romania */
    RO("Romania",                                              Currency.RON, Continent.EUROPE),
    /** Serbia */
    RS("Serbia",                                               Currency.RSD, Continent.EUROPE),
    /** Russian Federation */
    RU("Russian Federation",                                   Currency.RUB, Continent.EUROPE),
    /** Rwanda */
    RW("Rwanda",                                               Currency.RWF, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // S
    // -------------------------------------------------------------------------
    /** Saudi Arabia */
    SA("Saudi Arabia",                                         Currency.SAR, Continent.ASIA),
    /** Solomon Islands */
    SB("Solomon Islands",                                      Currency.SBD, Continent.OCEANIA),
    /** Seychelles */
    SC("Seychelles",                                           Currency.SCR, Continent.AFRICA),
    /** Sudan */
    SD("Sudan",                                                Currency.SDG, Continent.AFRICA),
    /** Sweden */
    SE("Sweden",                                               Currency.SEK, Continent.EUROPE),
    /** Singapore */
    SG("Singapore",                                            Currency.SGD, Continent.ASIA),
    /** Saint Helena, Ascension and Tristan da Cunha */
    SH("Saint Helena, Ascension and Tristan da Cunha",         Currency.SHP, Continent.AFRICA),
    /** Slovenia */
    SI("Slovenia",                                             Currency.EUR, Continent.EUROPE),
    /** Svalbard and Jan Mayen */
    SJ("Svalbard and Jan Mayen",                               Currency.NOK, Continent.EUROPE),
    /** Slovakia */
    SK("Slovakia",                                             Currency.EUR, Continent.EUROPE),
    /** Sierra Leone */
    SL("Sierra Leone",                                         Currency.SLE, Continent.AFRICA),
    /** San Marino */
    SM("San Marino",                                           Currency.EUR, Continent.EUROPE),
    /** Senegal */
    SN("Senegal",                                              Currency.XOF, Continent.AFRICA),
    /** Somalia */
    SO("Somalia",                                              Currency.SOS, Continent.AFRICA),
    /** Suriname */
    SR("Suriname",                                             Currency.SRD, Continent.SOUTH_AMERICA),
    /** South Sudan */
    SS("South Sudan",                                          Currency.SSP, Continent.AFRICA),
    /** São Tomé and Príncipe */
    ST("São Tomé and Príncipe",                                Currency.STN, Continent.AFRICA),
    /** El Salvador */
    SV("El Salvador",                                          Currency.USD, Continent.NORTH_AMERICA),
    /** Sint Maarten (Dutch part) */
    SX("Sint Maarten (Dutch part)",                            Currency.ANG, Continent.NORTH_AMERICA),
    /** Syrian Arab Republic */
    SY("Syrian Arab Republic",                                 Currency.SYP, Continent.ASIA),
    /** Eswatini */
    SZ("Eswatini",                                             Currency.SZL, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // T
    // -------------------------------------------------------------------------
    /** Turks and Caicos Islands */
    TC("Turks and Caicos Islands",                             Currency.USD, Continent.NORTH_AMERICA),
    /** Chad */
    TD("Chad",                                                 Currency.XAF, Continent.AFRICA),
    /** French Southern Territories */
    TF("French Southern Territories",                          Currency.EUR, Continent.ANTARCTICA),
    /** Togo */
    TG("Togo",                                                 Currency.XOF, Continent.AFRICA),
    /** Thailand */
    TH("Thailand",                                             Currency.THB, Continent.ASIA),
    /** Tajikistan */
    TJ("Tajikistan",                                           Currency.TJS, Continent.ASIA),
    /** Tokelau */
    TK("Tokelau",                                              Currency.NZD, Continent.OCEANIA),
    /** Timor-Leste */
    TL("Timor-Leste",                                          Currency.USD, Continent.ASIA),
    /** Turkmenistan */
    TM("Turkmenistan",                                         Currency.TMT, Continent.ASIA),
    /** Tunisia */
    TN("Tunisia",                                              Currency.TND, Continent.AFRICA),
    /** Tonga */
    TO("Tonga",                                                Currency.TOP, Continent.ASIA),
    /** Türkiye */
    TR("Türkiye",                                              Currency.TRY, Continent.ASIA),
    /** Trinidad and Tobago */
    TT("Trinidad and Tobago",                                  Currency.TTD, Continent.NORTH_AMERICA),
    /** Tuvalu */
    TV("Tuvalu",                                               Currency.AUD, Continent.OCEANIA),
    /** Taiwan, Province of China */
    TW("Taiwan, Province of China",                            Currency.TWD, Continent.ASIA),
    /** Tanzania, United Republic of */
    TZ("Tanzania, United Republic of",                         Currency.TZS, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // U
    // -------------------------------------------------------------------------
    /** Ukraine */
    UA("Ukraine",                                              Currency.UAH, Continent.EUROPE),
    /** Uganda */
    UG("Uganda",                                               Currency.UGX, Continent.AFRICA),
    /** United States Minor Outlying Islands */
    UM("United States Minor Outlying Islands",                 Currency.USD, Continent.OCEANIA),
    /** United States of America */
    US("United States of America",                             Currency.USD, Continent.NORTH_AMERICA),
    /** Uruguay */
    UY("Uruguay",                                              Currency.UYU, Continent.SOUTH_AMERICA),
    /** Uzbekistan */
    UZ("Uzbekistan",                                           Currency.UZS, Continent.ASIA),

    // -------------------------------------------------------------------------
    // V
    // -------------------------------------------------------------------------
    /** Holy See */
    VA("Holy See",                                             Currency.EUR, Continent.EUROPE),
    /** Saint Vincent and the Grenadines */
    VC("Saint Vincent and the Grenadines",                     Currency.XCD, Continent.NORTH_AMERICA),
    /** Venezuela (Bolivarian Republic of) */
    VE("Venezuela (Bolivarian Republic of)",                   Currency.VES, Continent.SOUTH_AMERICA),
    /** Virgin Islands (British) */
    VG("Virgin Islands (British)",                             Currency.USD, Continent.NORTH_AMERICA),
    /** Virgin Islands (U.S.) */
    VI("Virgin Islands (U.S.)",                                Currency.USD, Continent.NORTH_AMERICA),
    /** Viet Nam */
    VN("Viet Nam",                                             Currency.VND, Continent.ASIA),
    /** Vanuatu */
    VU("Vanuatu",                                              Currency.VUV, Continent.OCEANIA),

    // -------------------------------------------------------------------------
    // W
    // -------------------------------------------------------------------------
    /** Wallis and Futuna */
    WF("Wallis and Futuna",                                    Currency.XPF, Continent.OCEANIA),
    /** Samoa */
    WS("Samoa",                                                Currency.WST, Continent.OCEANIA),

    // -------------------------------------------------------------------------
    // X  (user-assigned, but widely adopted in banking)
    // -------------------------------------------------------------------------
    /** Kosovo (user-assigned; widely adopted in banking and payment systems) */
    XK("Kosovo",                                               Currency.EUR, Continent.EUROPE),

    // -------------------------------------------------------------------------
    // Y
    // -------------------------------------------------------------------------
    /** Yemen */
    YE("Yemen",                                                Currency.YER, Continent.ASIA),
    /** Mayotte */
    YT("Mayotte",                                              Currency.EUR, Continent.AFRICA),

    // -------------------------------------------------------------------------
    // Z
    // -------------------------------------------------------------------------
    /** South Africa */
    ZA("South Africa",                                         Currency.ZAR, Continent.AFRICA),
    /** Zambia */
    ZM("Zambia",                                               Currency.ZMW, Continent.AFRICA),
    /** Zimbabwe */
    ZW("Zimbabwe",                                             Currency.ZWL, Continent.AFRICA);

    /**
     * Constant for length of two-letter code.
     */
    private static final int                   CODE_LEN = 2;

    /**
     * Internal unmodifiable primitive-friendly lookup map from packed two-letter code to enum constant.
     */
    private static final Map<Integer, Country> LOOKUP   = buildLookupMap();

    /** The English short country name as defined in ISO 3166-1. */
    private final String                       countryName;

    /**
     * The primary ISO 4217 currency used in this country or territory.
     * <p>
     * {@code null} only for {@link #AQ} (Antarctica), which has no currency in use.
     */
    private final Currency                     currency;

    /**
     * The continent of this country or territory.
     */
    private final Continent                    continent;

    Country(final String countryName, final Currency currency, final Continent continent) {
        this.countryName = countryName;
        this.currency    = currency;
        this.continent   = continent;
    }

    /**
     * Packs two characters into a single {@code int} using bit-shifting: {@code (char1 << 16) | char2}.
     */
    private static int pack(char c1, char c2) {
        return (c1 << 16) | c2;
    }

    /**
     * Builds the lookup map at class-load time.<br>
     * The initial capacity is sized to avoid any rehashing.
     */
    private static Map<Integer, Country> buildLookupMap() {
        Country[] values = values();
        int capacity = (int) (values.length / 0.75f) + 1;
        Map<Integer, Country> map = new LinkedHashMap<>(capacity);
        for (final Country c : values) {
            String name = c.name();
            map.put(pack(name.charAt(0), name.charAt(1)), c);
        }
        return unmodifiableMap(map);
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
     * Returns the English short country name as defined in ISO 3166-1.
     *
     * @return the English country name (e.g., {@code "Bolivia"})
     */
    public String getCountryName() {
        return countryName;
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
     * Returns the continent of this country or territory.
     *
     * @since 1.8.8
     */
    public Continent getContinent() {
        return continent;
    }

    /**
     * Looks up the enum constant for the given two-letter country code.
     * <p>
     * Accepts any {@link CharSequence} implementation ({@link String}, {@link StringBuilder},
     * {@link StringBuffer}, etc.) so callers are not forced to materialize a {@code String}
     * solely for this lookup.
     * <p>
     * The lookup is case-sensitive; only uppercase codes are recognized
     * (e.g., {@code "LT"} matches, {@code "lt"} does not).<br>
     * Leading or trailing whitespace is <em>not</em> stripped — {@code " DE"} is not
     * a valid two-letter code and returns {@code null}.
     *
     * @param code the two-letter ISO 3166-1 Alpha-2 code (case-sensitive, e.g., {@code "LV"});
     *             may be any {@link CharSequence}
     * @return the matching {@link Country} constant,
     *         or {@code null} if the code is {@code null}, not exactly two characters, or unknown
     */
    public static Country fromCode(final CharSequence code) {
        return code == null || code.length() != CODE_LEN ? null : LOOKUP.get(pack(code.charAt(0), code.charAt(1)));
    }

    /**
     * High-performance check using primitive chars.
     * <p>
     * Zero-allocation, no String creation.
     */
    public static boolean isAssigned(char c1, char c2) {
        return LOOKUP.containsKey(pack(c1, c2));
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
        return code != null && code.length() == 2 && isAssigned(code.charAt(0), code.charAt(1));
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
        return name() + " (" + countryName + ')';
    }

}
