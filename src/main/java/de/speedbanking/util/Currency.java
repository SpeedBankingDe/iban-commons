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
 * ISO 4217 currency codes referenced by the countries and territories in {@link Iso3166Alpha2}.
 * <p>
 * Each constant represents a circulating, officially assigned ISO 4217 currency.
 * The enum name is the three-letter alphabetic code (e.g., {@code EUR}, {@code GBP});
 * the numeric code and English currency name are carried as enum fields.
 * <p>
 * Supranational / regional currencies are included as first-class members:
 * <ul>
 *   <li>{@link #EUR} — Euro (Eurozone + territories)</li>
 *   <li>{@link #XAF} — CFA Franc BEAC (Central Africa)</li>
 *   <li>{@link #XOF} — CFA Franc BCEAO (West Africa)</li>
 *   <li>{@link #XCD} — East Caribbean Dollar</li>
 *   <li>{@link #XPF} — CFP Franc (Pacific territories)</li>
 * </ul>
 * <p>
 * The enum covers exactly the currency codes assigned in {@link Iso3166Alpha2}; it does
 * <em>not</em> claim to be a complete ISO 4217 registry. In particular, precious-metal
 * codes ({@code XAU}, {@code XAG}, …) and the testing code {@code XTS} are absent.
 * <p>
 * <strong>Note on the name {@code Currency}:</strong> {@link java.util.Currency} is a
 * class in the standard library. Within this package there is no conflict; code that
 * imports both must use the fully-qualified name for {@code java.util.Currency}.
 *
 * @since 1.8.5
 *
 * @see Iso3166Alpha2
 * @see <a href="https://www.iso.org/iso-4217-currency-codes.html">ISO 4217</a>
 */
public enum Currency {

    // -------------------------------------------------------------------------
    // A
    // -------------------------------------------------------------------------
    /** UAE Dirham — United Arab Emirates */
    AED("UAE Dirham",                           784),
    /** Afghan Afghani — Afghanistan */
    AFN("Afghan Afghani",                       971),
    /** Albanian Lek — Albania */
    ALL("Albanian Lek",                           8),
    /** Armenian Dram — Armenia */
    AMD("Armenian Dram",                         51),
    /** Netherlands Antillean Guilder — Curaçao, Sint Maarten */
    ANG("Netherlands Antillean Guilder",        532),
    /** Angolan Kwanza — Angola */
    AOA("Angolan Kwanza",                       973),
    /** Argentine Peso — Argentina */
    ARS("Argentine Peso",                        32),
    /** Australian Dollar — Australia and territories */
    AUD("Australian Dollar",                     36),
    /** Aruban Florin — Aruba */
    AWG("Aruban Florin",                        533),
    /** Azerbaijani Manat — Azerbaijan */
    AZN("Azerbaijani Manat",                    944),

    // -------------------------------------------------------------------------
    // B
    // -------------------------------------------------------------------------
    /** Bosnia-Herzegovina Convertible Mark — Bosnia and Herzegovina */
    BAM("Bosnia-Herzegovina Convertible Mark",  977),
    /** Barbadian Dollar — Barbados */
    BBD("Barbadian Dollar",                      52),
    /** Bangladeshi Taka — Bangladesh */
    BDT("Bangladeshi Taka",                      50),
    /** Bulgarian Lev — Bulgaria */
    BGN("Bulgarian Lev",                        975),
    /** Bahraini Dinar — Bahrain */
    BHD("Bahraini Dinar",                        48),
    /** Burundian Franc — Burundi */
    BIF("Burundian Franc",                      108),
    /** Bermudian Dollar — Bermuda */
    BMD("Bermudian Dollar",                      60),
    /** Brunei Dollar — Brunei Darussalam */
    BND("Brunei Dollar",                         96),
    /** Bolivian Boliviano — Bolivia */
    BOB("Bolivian Boliviano",                    68),
    /** Brazilian Real — Brazil */
    BRL("Brazilian Real",                       986),
    /** Bahamian Dollar — Bahamas */
    BSD("Bahamian Dollar",                       44),
    /** Bhutanese Ngultrum — Bhutan */
    BTN("Bhutanese Ngultrum",                    64),
    /** Botswana Pula — Botswana */
    BWP("Botswana Pula",                         72),
    /** Belarusian Ruble — Belarus (redenominated 2016) */
    BYN("Belarusian Ruble",                     933),
    /** Belize Dollar — Belize */
    BZD("Belize Dollar",                         84),

    // -------------------------------------------------------------------------
    // C
    // -------------------------------------------------------------------------
    /** Canadian Dollar — Canada */
    CAD("Canadian Dollar",                      124),
    /** Congolese Franc — Congo, Democratic Republic of the */
    CDF("Congolese Franc",                      976),
    /** Swiss Franc — Switzerland, Liechtenstein */
    CHF("Swiss Franc",                          756),
    /** Chilean Peso — Chile */
    CLP("Chilean Peso",                         152),
    /** Chinese Yuan Renminbi — China */
    CNY("Chinese Yuan Renminbi",                156),
    /** Colombian Peso — Colombia */
    COP("Colombian Peso",                       170),
    /** Costa Rican Colón — Costa Rica */
    CRC("Costa Rican Colón",                    188),
    /** Cuban Peso — Cuba */
    CUP("Cuban Peso",                           192),
    /** Cape Verdean Escudo — Cabo Verde */
    CVE("Cape Verdean Escudo",                  132),
    /** Czech Koruna — Czechia */
    CZK("Czech Koruna",                         203),

    // -------------------------------------------------------------------------
    // D
    // -------------------------------------------------------------------------
    /** Djiboutian Franc — Djibouti */
    DJF("Djiboutian Franc",                     262),
    /** Danish Krone — Denmark, Faroe Islands, Greenland */
    DKK("Danish Krone",                         208),
    /** Dominican Peso — Dominican Republic */
    DOP("Dominican Peso",                       214),
    /** Algerian Dinar — Algeria */
    DZD("Algerian Dinar",                        12),

    // -------------------------------------------------------------------------
    // E
    // -------------------------------------------------------------------------
    /** Egyptian Pound — Egypt */
    EGP("Egyptian Pound",                       818),
    /** Eritrean Nakfa — Eritrea */
    ERN("Eritrean Nakfa",                       232),
    /** Ethiopian Birr — Ethiopia */
    ETB("Ethiopian Birr",                       230),
    /**
     * Euro — Eurozone member states and territories.
     * <p>
     * Countries: AD, AT, AX, BE, BL, CY, DE, EE, ES, FI, FR, GF, GP, GR, HR,
     * IE, IT, LT, LU, LV, MC, ME, MF, MQ, MT, NL, PM, PT, RE, SI, SK, SM,
     * TF, VA, XK, YT.
     */
    EUR("Euro",                                 978),

    // -------------------------------------------------------------------------
    // F
    // -------------------------------------------------------------------------
    /** Fijian Dollar — Fiji */
    FJD("Fijian Dollar",                        242),
    /** Falkland Islands Pound — Falkland Islands */
    FKP("Falkland Islands Pound",               238),

    // -------------------------------------------------------------------------
    // G
    // -------------------------------------------------------------------------
    /** British Pound Sterling — United Kingdom and Crown Dependencies */
    GBP("British Pound Sterling",               826),
    /** Georgian Lari — Georgia */
    GEL("Georgian Lari",                        981),
    /** Ghanaian Cedi — Ghana */
    GHS("Ghanaian Cedi",                        936),
    /** Gibraltar Pound — Gibraltar */
    GIP("Gibraltar Pound",                      292),
    /** Gambian Dalasi — Gambia */
    GMD("Gambian Dalasi",                       270),
    /** Guinean Franc — Guinea */
    GNF("Guinean Franc",                        324),
    /** Guatemalan Quetzal — Guatemala */
    GTQ("Guatemalan Quetzal",                   320),
    /** Guyanese Dollar — Guyana */
    GYD("Guyanese Dollar",                      328),

    // -------------------------------------------------------------------------
    // H
    // -------------------------------------------------------------------------
    /** Hong Kong Dollar — Hong Kong */
    HKD("Hong Kong Dollar",                     344),
    /** Honduran Lempira — Honduras */
    HNL("Honduran Lempira",                     340),
    /** Haitian Gourde — Haiti */
    HTG("Haitian Gourde",                       332),
    /** Hungarian Forint — Hungary */
    HUF("Hungarian Forint",                     348),

    // -------------------------------------------------------------------------
    // I
    // -------------------------------------------------------------------------
    /** Indonesian Rupiah — Indonesia */
    IDR("Indonesian Rupiah",                    360),
    /** Israeli New Shekel — Israel, Palestine */
    ILS("Israeli New Shekel",                   376),
    /** Indian Rupee — India */
    INR("Indian Rupee",                         356),
    /** Iraqi Dinar — Iraq */
    IQD("Iraqi Dinar",                          368),
    /** Iranian Rial — Iran */
    IRR("Iranian Rial",                         364),
    /** Icelandic Króna — Iceland */
    ISK("Icelandic Króna",                      352),

    // -------------------------------------------------------------------------
    // J
    // -------------------------------------------------------------------------
    /** Jamaican Dollar — Jamaica */
    JMD("Jamaican Dollar",                      388),
    /** Jordanian Dinar — Jordan */
    JOD("Jordanian Dinar",                      400),
    /** Japanese Yen — Japan */
    JPY("Japanese Yen",                         392),

    // -------------------------------------------------------------------------
    // K
    // -------------------------------------------------------------------------
    /** Kenyan Shilling — Kenya */
    KES("Kenyan Shilling",                      404),
    /** Kyrgystani Som — Kyrgyzstan */
    KGS("Kyrgystani Som",                       417),
    /** Cambodian Riel — Cambodia */
    KHR("Cambodian Riel",                       116),
    /** Comorian Franc — Comoros */
    KMF("Comorian Franc",                       174),
    /** North Korean Won — Korea, Democratic People's Republic of */
    KPW("North Korean Won",                     408),
    /** South Korean Won — Korea, Republic of */
    KRW("South Korean Won",                     410),
    /** Kuwaiti Dinar — Kuwait */
    KWD("Kuwaiti Dinar",                        414),
    /** Cayman Islands Dollar — Cayman Islands */
    KYD("Cayman Islands Dollar",                136),
    /** Kazakhstani Tenge — Kazakhstan */
    KZT("Kazakhstani Tenge",                    398),

    // -------------------------------------------------------------------------
    // L
    // -------------------------------------------------------------------------
    /** Laotian Kip — Lao PDR */
    LAK("Laotian Kip",                          418),
    /** Lebanese Pound — Lebanon */
    LBP("Lebanese Pound",                       422),
    /** Sri Lankan Rupee — Sri Lanka */
    LKR("Sri Lankan Rupee",                     144),
    /** Liberian Dollar — Liberia */
    LRD("Liberian Dollar",                      430),
    /** Lesotho Loti — Lesotho */
    LSL("Lesotho Loti",                         426),
    /** Libyan Dinar — Libya */
    LYD("Libyan Dinar",                         434),

    // -------------------------------------------------------------------------
    // M
    // -------------------------------------------------------------------------
    /** Moroccan Dirham — Morocco, Western Sahara */
    MAD("Moroccan Dirham",                      504),
    /** Moldovan Leu — Moldova */
    MDL("Moldovan Leu",                         498),
    /** Malagasy Ariary — Madagascar */
    MGA("Malagasy Ariary",                      969),
    /** Macedonian Denar — North Macedonia */
    MKD("Macedonian Denar",                     807),
    /** Myanmar Kyat — Myanmar */
    MMK("Myanmar Kyat",                         104),
    /** Mongolian Tögrög — Mongolia */
    MNT("Mongolian Tögrög",                     496),
    /** Macanese Pataca — Macao */
    MOP("Macanese Pataca",                      446),
    /** Mauritanian Ouguiya — Mauritania (new, since 2018) */
    MRU("Mauritanian Ouguiya",                  929),
    /** Mauritian Rupee — Mauritius */
    MUR("Mauritian Rupee",                      480),
    /** Maldivian Rufiyaa — Maldives */
    MVR("Maldivian Rufiyaa",                    462),
    /** Malawian Kwacha — Malawi */
    MWK("Malawian Kwacha",                      454),
    /** Mexican Peso — Mexico */
    MXN("Mexican Peso",                         484),
    /** Malaysian Ringgit — Malaysia */
    MYR("Malaysian Ringgit",                    458),
    /** Mozambican Metical — Mozambique */
    MZN("Mozambican Metical",                   943),

    // -------------------------------------------------------------------------
    // N
    // -------------------------------------------------------------------------
    /** Namibian Dollar — Namibia */
    NAD("Namibian Dollar",                      516),
    /** Nigerian Naira — Nigeria */
    NGN("Nigerian Naira",                       566),
    /** Nicaraguan Córdoba — Nicaragua */
    NIO("Nicaraguan Córdoba",                   558),
    /** Norwegian Krone — Norway, Bouvet Island, Svalbard and Jan Mayen */
    NOK("Norwegian Krone",                      578),
    /** Nepalese Rupee — Nepal */
    NPR("Nepalese Rupee",                       524),
    /** New Zealand Dollar — New Zealand and territories */
    NZD("New Zealand Dollar",                   554),

    // -------------------------------------------------------------------------
    // O
    // -------------------------------------------------------------------------
    /** Omani Rial — Oman */
    OMR("Omani Rial",                           512),

    // -------------------------------------------------------------------------
    // P
    // -------------------------------------------------------------------------
    /** Peruvian Sol — Peru */
    PEN("Peruvian Sol",                         604),
    /** Papua New Guinean Kina — Papua New Guinea */
    PGK("Papua New Guinean Kina",               598),
    /** Philippine Peso — Philippines */
    PHP("Philippine Peso",                      608),
    /** Pakistani Rupee — Pakistan */
    PKR("Pakistani Rupee",                      586),
    /** Polish Złoty — Poland */
    PLN("Polish Złoty",                         985),
    /** Paraguayan Guaraní — Paraguay */
    PYG("Paraguayan Guaraní",                   600),

    // -------------------------------------------------------------------------
    // Q
    // -------------------------------------------------------------------------
    /** Qatari Riyal — Qatar */
    QAR("Qatari Riyal",                         634),

    // -------------------------------------------------------------------------
    // R
    // -------------------------------------------------------------------------
    /** Romanian Leu — Romania */
    RON("Romanian Leu",                         946),
    /** Serbian Dinar — Serbia */
    RSD("Serbian Dinar",                        941),
    /** Russian Ruble — Russian Federation */
    RUB("Russian Ruble",                        643),
    /** Rwandan Franc — Rwanda */
    RWF("Rwandan Franc",                        646),

    // -------------------------------------------------------------------------
    // S
    // -------------------------------------------------------------------------
    /** Saudi Riyal — Saudi Arabia */
    SAR("Saudi Riyal",                          682),
    /** Solomon Islands Dollar — Solomon Islands */
    SBD("Solomon Islands Dollar",                90),
    /** Seychellois Rupee — Seychelles */
    SCR("Seychellois Rupee",                    690),
    /** Sudanese Pound — Sudan */
    SDG("Sudanese Pound",                       938),
    /** Swedish Krona — Sweden */
    SEK("Swedish Krona",                        752),
    /** Singapore Dollar — Singapore */
    SGD("Singapore Dollar",                     702),
    /** Saint Helenian Pound — Saint Helena, Ascension and Tristan da Cunha */
    SHP("Saint Helenian Pound",                 654),
    /** Sierra Leonean Leone — Sierra Leone (redenominated 2022) */
    SLE("Sierra Leonean Leone",                 925),
    /** Somali Shilling — Somalia */
    SOS("Somali Shilling",                      706),
    /** Surinamese Dollar — Suriname */
    SRD("Surinamese Dollar",                    968),
    /** South Sudanese Pound — South Sudan */
    SSP("South Sudanese Pound",                 728),
    /** São Tomé and Príncipe Dobra — São Tomé and Príncipe */
    STN("São Tomé and Príncipe Dobra",          930),
    /** Syrian Pound — Syrian Arab Republic */
    SYP("Syrian Pound",                         760),
    /** Swazi Lilangeni — Eswatini */
    SZL("Swazi Lilangeni",                      748),

    // -------------------------------------------------------------------------
    // T
    // -------------------------------------------------------------------------
    /** Thai Baht — Thailand */
    THB("Thai Baht",                            764),
    /** Tajikistani Somoni — Tajikistan */
    TJS("Tajikistani Somoni",                   972),
    /** Turkmenistani Manat — Turkmenistan */
    TMT("Turkmenistani Manat",                  934),
    /** Tunisian Dinar — Tunisia */
    TND("Tunisian Dinar",                       788),
    /** Tongan Paʻanga — Tonga */
    TOP("Tongan Paʻanga",                       776),
    /** Turkish Lira — Türkiye */
    TRY("Turkish Lira",                         949),
    /** Trinidad and Tobago Dollar — Trinidad and Tobago */
    TTD("Trinidad and Tobago Dollar",           780),
    /** New Taiwan Dollar — Taiwan */
    TWD("New Taiwan Dollar",                    901),
    /** Tanzanian Shilling — Tanzania */
    TZS("Tanzanian Shilling",                   834),

    // -------------------------------------------------------------------------
    // U
    // -------------------------------------------------------------------------
    /** Ukrainian Hryvnia — Ukraine */
    UAH("Ukrainian Hryvnia",                    980),
    /** Ugandan Shilling — Uganda */
    UGX("Ugandan Shilling",                     800),
    /**
     * US Dollar — United States and territories.
     * <p>
     * Also used as legal tender in: AS, BQ, EC, FM, GU, IO, MH, MP, PA, PR, SV,
     * TC, TL, UM, VG, VI.
     */
    USD("US Dollar",                            840),
    /** Uruguayan Peso — Uruguay */
    UYU("Uruguayan Peso",                       858),
    /** Uzbekistani Som — Uzbekistan */
    UZS("Uzbekistani Som",                      860),

    // -------------------------------------------------------------------------
    // V
    // -------------------------------------------------------------------------
    /** Venezuelan Bolívar Soberano — Venezuela (since 2018) */
    VES("Venezuelan Bolívar Soberano",          928),
    /** Vietnamese Đồng — Viet Nam */
    VND("Vietnamese Đồng",                      704),
    /** Vanuatu Vatu — Vanuatu */
    VUV("Vanuatu Vatu",                         548),

    // -------------------------------------------------------------------------
    // W
    // -------------------------------------------------------------------------
    /** Samoan Tālā — Samoa */
    WST("Samoan Tālā",                          882),

    // -------------------------------------------------------------------------
    // X — supranational / regional currencies
    // -------------------------------------------------------------------------
    /**
     * CFA Franc BEAC — Central African Economic and Monetary Community.
     * <p>
     * Countries: CF, CG, CM, GA, GQ, TD.
     */
    XAF("CFA Franc BEAC",                       950),
    /**
     * East Caribbean Dollar — Organisation of Eastern Caribbean States.
     * <p>
     * Countries: AG, AI, DM, GD, KN, LC, MS, VC.
     */
    XCD("East Caribbean Dollar",                951),
    /**
     * CFA Franc BCEAO — West African Economic and Monetary Union.
     * <p>
     * Countries: BF, BJ, CI, GW, ML, NE, SN, TG.
     */
    XOF("CFA Franc BCEAO",                      952),
    /**
     * CFP Franc — French Pacific Territories.
     * <p>
     * Countries: NC, PF, WF.
     */
    XPF("CFP Franc",                            953),

    // -------------------------------------------------------------------------
    // Y
    // -------------------------------------------------------------------------
    /** Yemeni Rial — Yemen */
    YER("Yemeni Rial",                          886),

    // -------------------------------------------------------------------------
    // Z
    // -------------------------------------------------------------------------
    /** South African Rand — South Africa */
    ZAR("South African Rand",                   710),
    /** Zambian Kwacha — Zambia */
    ZMW("Zambian Kwacha",                       967),
    /** Zimbabwean Dollar — Zimbabwe */
    ZWL("Zimbabwean Dollar",                    932);

    // -------------------------------------------------------------------------

    /** The English currency name as defined in ISO 4217. */
    private final String currencyName;

    /** The ISO 4217 three-digit numeric currency code. */
    private final int    numericCode;

    Currency(final String currencyName, final int numericCode) {
        this.currencyName = currencyName;
        this.numericCode  = numericCode;
    }

    /**
     * Returns the English currency name as defined in ISO 4217
     * (e.g., {@code "Euro"}, {@code "Swiss Franc"}).
     *
     * @return the currency name
     */
    public String getCurrencyName() {
        return currencyName;
    }

    /**
     * Returns the three-letter ISO 4217 alphabetic currency code.
     * <p>
     * Identical to {@link #name()} — provided for symmetry with {@link #getNumericCode()}.
     *
     * @return the alphabetic code (e.g., {@code "EUR"}, {@code "GBP"})
     */
    public String getAlphaCode() {
        return name();
    }

    /**
     * Returns the ISO 4217 three-digit numeric currency code
     * (e.g., {@code 978} for EUR, {@code 826} for GBP).
     *
     * @return the numeric code
     */
    public int getNumericCode() {
        return numericCode;
    }

    /**
     * Returns a human-readable representation combining the alphabetic code and currency name.
     * <p>
     * Example: {@code "EUR (Euro)"}
     *
     * @return the formatted string
     */
    @Override
    public String toString() {
        return name() + " (" + currencyName + ")";
    }

}
