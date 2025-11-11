/*
 * Copyright © 2025 Markus Spann, SpeedBankingDe
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

import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.util.CountryUtil;
import de.speedbanking.util.IndexRange;

import java.lang.reflect.InvocationTargetException;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * The definitive, immutable registry for all **ISO 13616-compliant national IBAN formats**.
 * <p>
 * This enumeration holds the official structural rules (total length, BBAN pattern, component index ranges)
 * for each country, as published in the **SWIFT IBAN Registry (Release 100 - Oct 2025)**.
 * <p>
 * The **International Organization for Standardization (ISO)** designated **SWIFT** as the
 * Registration Authority for ISO 13616.
 *
 * <p>
 * **BBAN Pattern Notation** (defined in ISO 13616):
 * <ul>
 *   <li>{@code n}: Digits (numeric characters 0-9)</li>
 *   <li>{@code a}: Upper-case letters (alphabetic characters A-Z)</li>
 *   <li>{@code c}: Upper-case letters and digits (alphanumeric characters A-Z and 0-9)</li>
 *   <li>{@code !}: Indicates fixed length (e.g., 4!n)</li>
 * </ul>
 *
 * Indexing is zero-based and exclusive of the end index (Java's substring convention).
 * Example: A position "1-4" in the registry (BBAN starts at char 0) becomes index 4 to 8 in the full IBAN string.
 *
 * @since 1.8.0
 */
@SuppressWarnings({"checkstyle:NoWhitespaceBefore", "checkstyle:LineLength"})
public enum IbanRegistry {

    /*
       Name of country               , SEPA Y/N, Len, BBAN structure           , Bank code pattern / Index,           , Branch code pattern / Index,           , Account No. Index, IBAN electronic format example,
       Organisation, Department, Street Address, City / Postcode, Department (generic) Email, Department Tel, Last update date
    */
    /**
     * <strong>Andorra ({@code AD})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n4!n12!c}<br>
     * Example: {@code AD1200012030200359100100}
     */
    AD("Andorra"                     , true    , 24 , "4!n4!n12!c"             , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 24), "AD1200012030200359100100",
       "Associacio de Bancs Andorrans (ABA)", null, "C/ Ciutat de Consuegra, 16 Edifici l'Illa, esc. A, 2n pis", "AD500 Andorra la Vella Principat d'Andorra", "aba@aba.ad", "376 80 71 10", YearMonth.of(2021, 3)),

    /**
     * <strong>United Arab Emirates ({@code AE})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n16!n}<br>
     * Example: {@code AE070331234567890123456}
     */
    AE("United Arab Emirates"        , false   , 23 , "3!n16!n"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 23), "AE070331234567890123456",
       "Central Bank of the United Arab Emirates", null, "Bainuna Street, Al Bateen", "Abu Dhabi PO Box 854", null, null, YearMonth.of(2025, 2)),

    /**
     * <strong>Albania ({@code AL})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 8!n16!c}<br>
     * Example: {@code AL47212110090000000235698741}
     */
    AL("Albania"                     , false   , 28 , "8!n16!c"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), "5!n", IndexRange.of("Branch Code", 7, 11), IndexRange.of("Account No", 12, 28), "AL47212110090000000235698741",
       "Bank of Albania", "Payment systems", "Kompleksi Halili Rruga e Dibres", "1000 Tirana", null, null, YearMonth.of(2025, 6)),

    /**
     * <strong>Angola ({@code AO})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Example: {@code AO06000600000100037131174}
     */
    AO("Angola"                      , false   , 25 , "4!n4!n11!n2!n"          , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 23), null,
       null, null, null, null, null, null, null),

    /**
     * <strong>Austria ({@code AT})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n11!n}<br>
     * Example: {@code AT611904300234573201}
     */
    AT("Austria"                     , true    , 20 , "5!n11!n"                , "5!n"   , IndexRange.of("Bank Code", 4, 9), null ,       null, IndexRange.of("Account No", 9, 20), "AT611904300234573201",
       "PSA Payement Services Austria GMBH", "Account Systems", "Rivergate 2, Handelskai 92", "1200 Wien", "accountsystems@psa.at", "+ 43 15053280 / 0", YearMonth.of(2025, 10)),

    /**
     * <strong>Azerbaijan ({@code AZ})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!c}<br>
     * Example: {@code AZ21NABZ00000000137010001944}
     */
    AZ("Azerbaijan"                  , false   , 28 , "4!a20!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 28), "AZ21NABZ00000000137010001944",
       "Central Bank of the Republic of Azerbaijan", "Head Office", "32, R. Behbudov", "AZ 1014 Baku", "payment_systems@cbar.az", "+ 994 124931122", YearMonth.of(2016, 8)),

    /**
     * <strong>Bosnia and Herzegovina ({@code BA})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n3!n8!n2!n}<br>
     * Example: {@code BA391290079401028494}
     */
    BA("Bosnia and Herzegovina"      , false   , 20 , "3!n3!n8!n2!n"           , "3!n"   , IndexRange.of("Bank Code", 4, 7), "3!n", IndexRange.of("Branch Code", 7, 10), IndexRange.of("Account No", 10, 18), "BA391290079401028494",
       "Centralna banka Bosne i Hercegovine", "Payment Systems Division", "25 Maršala Tita Street", "71000 Sarajevo, Bosnia and Herzegovina", null, null, YearMonth.of(2016, 8)),

    /**
     * <strong>Belgium ({@code BE})</strong><p>
     * IBAN Length: 16<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n7!n2!n}<br>
     * Example: {@code BE68539007547034}
     */
    BE("Belgium"                     , true    , 16 , "3!n7!n2!n"              , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 14), "BE68539007547034",
       "Febelfin", "Payments & Operations", "Aarlenstraat 82", "1040 Brussels", "info@febelfin.be", "+ 32 25076811", YearMonth.of(2016, 9)),

    /**
     * <strong>Bulgaria ({@code BG})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a4!n2!n8!c}<br>
     * Example: {@code BG80BNBG96611020345678}
     */
    BG("Bulgaria"                    , true    , 22 , "4!a4!n2!n8!c"           , "4!a"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 14, 22), "BG80BNBG96611020345678",
       "Bulgarian National Bank", "Payment Systems and Minimum Reserves Directorate", "1, Knyaz Alexander I Sq.", "1000 Sofia, Bulgaria", "rtgs@bnbank.org", "+ 359 29145761", YearMonth.of(2016, 8)),

    /**
     * <strong>Bahrain ({@code BH})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a14!c}<br>
     * Example: {@code BH67BMAG00001299123456}
     */
    BH("Bahrain"                     , false   , 22 , "4!a14!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 22), "BH67BMAG00001299123456",
       "Central Bank of Bahrain", "Banking Services Directorate", "King Faisal Highway, Block 317, Road 1702, Building 96", "Manama", null, null, YearMonth.of(2012, 1)),

    /**
     * <strong>Burundi ({@code BI})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Example: {@code BI4210000100010000332045181}
     */
    BI("Burundi"                     , false   , 27 , "5!n5!n11!n2!n"          , "5!n"   , IndexRange.of("Bank Code", 4, 9), "5!n", IndexRange.of("Branch Code", 9, 14), IndexRange.of("Account No", 14, 27), "BI4210000100010000332045181",
       "Banque de la Republique du Burundi", null, "1, Avenue du Gouvernement PO BOX 705", "Bujumbura", "brb@brb.bi", null, YearMonth.of(2021, 10)),

    /**
     * <strong>Brazil ({@code BR})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 8!n5!n10!n1!a1!c}<br>
     * Example: {@code BR1800360305000010009795493C1}
     */
    BR("Brazil"                      , false   , 29 , "8!n5!n10!n1!a1!c"       , "8!n"   , IndexRange.of("Bank Code", 4, 12), "5!n", IndexRange.of("Branch Code", 12, 17), IndexRange.of("Account No", 17, 27), "BR1800360305000010009795493C1",
       "Banco Central do Brasil", "DEBAN - Departamento de Operações Bancárias e de Sistema de Pagamentos", "SBS Quadra 3 Bloco B", "71.070-900 Brasília", "iban@bcb.gov.br", "+ 55 (61)34142666 / + 55 (51)32157339", YearMonth.of(2016, 8)),

    /**
     * <strong>Belarus ({@code BY})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!c4!n16!c}<br>
     * Example: {@code BY13NBRB3600900000002Z00AB00}
     */
    BY("Belarus"                     , false   , 28 , "4!c4!n16!c"             , "4!c"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 28), "BY13NBRB3600900000002Z00AB00",
       "National Bank of the Republic of Belarus", "Payment system and digital technologies directorate", "Nezavisimosty Avenue, 20", "220008 Minsk", null, null, YearMonth.of(2024, 2)),

    /**
     * <strong>Switzerland ({@code CH})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n12!c}<br>
     * Example: {@code CH9300762011623852957}
     */
    CH("Switzerland"                 , true    , 21 , "5!n12!c"                , "5!n"   , IndexRange.of("Bank Code", 4, 9), null ,       null, IndexRange.of("Account No", 9, 21), "CH9300762011623852957",
       "SIX Interbank Clearing Ltd", "Zentrale Koordinationsstelle fuer IBAN/IPI - Technical Support", "Hardturmstrasse 201", "CH-8021 ZURICH", "iban@six-group.com", "+ 41 583994420", YearMonth.of(2016, 8)),

    /**
     * <strong>Costa Rica ({@code CR})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n14!n}<br>
     * Example: {@code CR05015202001026284066}
     */
    CR("Costa Rica"                  , false   , 22 , "4!n14!n"                , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 22), "CR05015202001026284066",
       "Banco Central de Costa Rica", "Sistema de Pagos", "Avenida Central y 1a. Calles 2 y 4", "10058-1000 San José", null, null, YearMonth.of(2019, 1)),

    /**
     * <strong>Cape Verde ({@code CV})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n13!c}<br>
     * Example: {@code CV05123412341234123412341}
     */
    CV("Cape Verde"                  , false   , 25 , "4!n4!n13!c"             , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 25), "CV05123412341234123412341",
       null, null, null, null, null, null, null),

    /**
     * <strong>Cyprus ({@code CY})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n5!n16!c}<br>
     * Example: {@code CY17002001280000001200527600}
     */
    CY("Cyprus"                      , true    , 28 , "3!n5!n16!c"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), "5!n", IndexRange.of("Branch Code", 7, 12), IndexRange.of("Account No", 12, 28), "CY17002001280000001200527600",
       "Central Bank of Cyprus", "Payment systems and Accounting Services", "80 Kennedy Avenue", "P.O. Box 25529 CY-1395 Nicosia", "PaymentSystems@centralbank.gov.cy", null, YearMonth.of(2009, 8)),

    /**
     * <strong>Czechia ({@code CZ})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n16!n}<br>
     * Example: {@code CZ6508000000192000145399}
     */
    CZ("Czechia"                     , true    , 24 , "4!n16!n"                , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 24), "CZ6508000000192000145399",
       "Czech National Bank", "Cash and Payments Department", "Na Příkopě 28", "Praha 1", "iban.info@cnb.cz", null, YearMonth.of(2025, 6)),

    /**
     * <strong>Germany ({@code DE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 8!n10!n}<br>
     * Example: {@code DE89370400440532013000}
     */
    DE("Germany"                     , true    , 22 , "8!n10!n"                , "8!n"   , IndexRange.of("Bank Code", 4, 12), null ,       null, IndexRange.of("Account No", 12, 22), "DE89370400440532013000",
       "Bundesverband deutscher Banken", null, "Burgstrasse 28", "10178 Berlin", "iban@bdb.de", "+ 49 3016632301", YearMonth.of(2011, 1)),

    /**
     * <strong>Djibouti ({@code DJ})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Example: {@code DJ2100010000000154000100186}
     */
    DJ("Djibouti"                    , false   , 27 , "5!n5!n11!n2!n"          , "5!n"   , IndexRange.of("Bank Code", 4, 9), "5!n", IndexRange.of("Branch Code", 9, 14),       IndexRange.of("Account No", 14, 25), "DJ2100010000000154000100186", // MSP-OK
       "Banque Centrale de Djibouti", null, "Avenue Cheick Osman P.O Box 705", "Djibouti", "bndj@intnet.dj", null, YearMonth.of(2022, 5)),

    /**
     * <strong>Denmark ({@code DK})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n9!n1!n}<br>
     * Example: {@code DK5000400440116243}
     */
    DK("Denmark"                     , true    , 18 , "4!n9!n1!n"              , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 18), "DK5000400440116243",
       "Finance Denmark", null, "7 Amaliegade", "DK 1256 Copenhagen K", "IBAN@FIDA.dk", null, YearMonth.of(2018, 11)),

    /**
     * <strong>Dominican Republic ({@code DO})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!c20!n}<br>
     * Example: {@code DO28BAGR00000001212453611324}
     */
    DO("Dominican Republic"          , false   , 28 , "4!c20!n"                , "4!c"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 28), "DO28BAGR00000001212453611324",
       "Central Bank of the Dominican Republic", "Payment Systems", "Av. Pedro Henríquez Urena esq. Leopoldo Navarro", "Santo Domingo", "sistema.pagos@bancentral.gov.do", "+ 1 8092219111 (ext. 3409)", YearMonth.of(2016, 9)),

    /**
     * <strong>Estonia ({@code EE})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 2!n14!n}<br>
     * Example: {@code EE382200221020145685}
     */
    EE("Estonia"                     , true    , 20 , "2!n14!n"                , "2!n"   , IndexRange.of("Bank Code", 4, 6), "2!n", IndexRange.of("Branch Code", 6, 8), IndexRange.of("Account No", 8, 19), "EE382200221020145685",
       "Estonian Banking Association", null, "Maakri 30", "10145 Tallinn", "pangaliit@pangaliit.ee", "+ 372 6116569", YearMonth.of(2024, 12)),

    /**
     * <strong>Egypt ({@code EG})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n17!n}<br>
     * Example: {@code EG380019000500000000263180002}
     */
    EG("Egypt"                       , false   , 29 , "4!n4!n17!n"             , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 29), "EG380019000500000000263180002",
       "Central Bank of Egypt", "Operations Sector", "54 El Gomherya street", "Cairo", "Operations.development@cbe.org.eg", "+ 20 16777 (ext. 3109)", YearMonth.of(2020, 1)),

    /**
     * <strong>Spain ({@code ES})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n4!n1!n1!n10!n}<br>
     * Example: {@code ES9121000418450200051332}
     */
    ES("Spain"                       , true    , 24 , "4!n4!n1!n1!n10!n"       , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 14, 24), "ES9121000418450200051332",
       "Asociación Española de Banca Privada (AEB)", null, "C/ Velázquez, 64 – 66", "28001 Madrid", "asesoria.pagos@aebanca.es", "+ 34 917891311", YearMonth.of(2016, 9)),

    /**
     * <strong>Finland ({@code FI})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n11!n}<br>
     * Example: {@code FI2112345600000785}
     */
    FI("Finland"                     , true    , 18 , "3!n11!n"                , "6!n"   , IndexRange.of("Bank Code", 4, 10), null ,       null, IndexRange.of("Account No", 10, 17), "FI2112345600000785", // MSP-OK
       "Federation of Finnish Financial Services", null, "PO Box 1009", "FIN-00101 Helsinki", "paymentsupport@finanssiala.fi", null, YearMonth.of(2016, 8)),

    /**
     * <strong>Aland Islands ({@code AX})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n11!n}<br>
     * Example: {@code FI2112345600000785}
     */
    AX("Aland Islands", FI),

    /**
     * <strong>Falkland Islands ({@code FK})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!a12!n}<br>
     * Example: {@code FK88SC123456789012}
     */
    FK("Falkland Islands"            , false   , 18 , "2!a12!n"                , "2!a"   , IndexRange.of("Bank Code", 4, 6), null ,       null,  IndexRange.of("Account No", 6, 18), "FK88SC123456789012", // MSP-OK
       "Falkland Islands Government", "The Treasury", "Thatcher Drive", "FIQQ 1ZZ Stanley", "treasury@sec.gov.fk", null, YearMonth.of(2023, 7)),

    /**
     * <strong>Faroe Islands ({@code FO})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n9!n1!n}<br>
     * Example: {@code FO6264600001631634}
     */
    FO("Faroe Islands"               , false   , 18 , "4!n9!n1!n"              , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 17), "FO6264600001631634",
       "Finance Denmark", null, "7 Amaliegade", "DK 1256 Copenhagen K", "IBAN@FIDA.dk", null, YearMonth.of(2017, 2)),

    /**
     * <strong>France ({@code FR})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    FR("France"                      , true    , 27 , "5!n5!n11!c2!n"          , "5!n"   , IndexRange.of("Bank Code", 4, 9), "5!n", IndexRange.of("Branch Code", 9, 14), IndexRange.of("Account No", 14, 25), "FR1420041010050500013M02606",
       "CFONB", null, "18 rue la Fayette", "75009 Paris", "cfonb@cfonb.fr", "+ 33 148005042", YearMonth.of(2016, 9)),

    /**
     * <strong>French Guiana ({@code GF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    GF("French Guiana", FR),

    /**
     * <strong>Guadeloupe ({@code GP})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    GP("Guadeloupe", FR),

    /**
     * <strong>Martinique ({@code MQ})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    MQ("Martinique", FR),

    /**
     * <strong>Réunion ({@code RE})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    RE("Réunion", FR),

    /**
     * <strong>French Polynesia ({@code PF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    PF("French Polynesia", FR),

    /**
     * <strong>French Southern Territories ({@code TF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    TF("French Southern Territories", FR),

    /**
     * <strong>Mayotte ({@code YT})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    YT("Mayotte", FR),

    /**
     * <strong>New Caledonia ({@code NC})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    NC("New Caledonia", FR),

    /**
     * <strong>Saint Barthélemy ({@code BL})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    BL("Saint Barthélemy", FR),

    /**
     * <strong>Saint Martin (French part) ({@code MF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    MF("Saint Martin (French part)", FR),

    /**
     * <strong>Saint Pierre and Miquelon ({@code PM})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    PM("Saint Pierre and Miquelon", FR),

    /**
     * <strong>Wallis and Futuna Islands ({@code WF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code FR1420041010050500013M02606}
     */
    WF("Wallis and Futuna Islands", FR),

    /**
     * <strong>Gabon ({@code GA})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n13!c}<br>
     * Example: {@code GA2140021010032001890020126}
     */
    GA("Gabon"                       , false   , 27 , "5!n5!n13!c"             , "5!n"   , IndexRange.of("Bank Code", 4, 9), "5!n", IndexRange.of("Branch Code", 9, 14), IndexRange.of("Account No", 14, 27), "GA2140021010032001890020126",
       null, null, null, null, null, null, null),

    /**
     * <strong>United Kingdom ({@code GB})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Example: {@code GB29NWBK60161331926819}
     */
    GB("United Kingdom"              , true    , 22 , "4!a6!n8!n"              , "4!a"   , IndexRange.of("Bank Code", 4, 8), "6!n", IndexRange.of("Branch Code", 8, 14), IndexRange.of("Account No", 14, 22), "GB29NWBK60161331926819",
       "Payments UK Management Ltd", "International Standards and Services", "14 Finsbury Square", "London EC2A 1LQ", null, null, YearMonth.of(2017, 5)),

    /**
     * <strong>Isle of Man ({@code IM})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Example: {@code GB29NWBK60161331926819}
     */
    IM("Isle of Man", GB),

    /**
     * <strong>Jersey ({@code JE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Example: {@code GB29NWBK60161331926819}
     */
    JE("Jersey", GB),

    /**
     * <strong>Guernsey ({@code GG})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Example: {@code GB29NWBK60161331926819}
     */
    GG("Guernsey", GB),

    /**
     * <strong>Georgia ({@code GE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!a16!n}<br>
     * Example: {@code GE29NB0000000101904917}
     */
    GE("Georgia"                     , false   , 22 , "2!a16!n"                , "2!a"   , IndexRange.of("Bank Code", 4, 6), null ,       null, IndexRange.of("Account No", 6, 22), "GE29NB0000000101904917",
       "National Bank of Georgia", "Payment Systems", "1, Zviad Gamsakhurdia Embankment", "0114 Tbilisi", "RTGS@nbg.gov.ge", "+ 995 322406555", YearMonth.of(2023, 4)),

    /**
     * <strong>Gibraltar ({@code GI})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a15!c}<br>
     * Example: {@code GI75NWBK000000007099453}
     */
    GI("Gibraltar"                   , true    , 23 , "4!a15!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 23), "GI75NWBK000000007099453",
       "Financial Services Commission", null, "PO Box 940", "Suite 3, Ground Floor, Atlantic Suites", "info@fsc.gi", "+350 20040283", YearMonth.of(2016, 9)),

    /**
     * <strong>Greenland ({@code GL})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n9!n1!n}<br>
     * Example: {@code GL8964710001000206}
     */
    GL("Greenland"                   , false   , 18 , "4!n9!n1!n"              , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 18), "GL8964710001000206",
       "Finance Denmark", null, "7 Amaliegade", "DK 1256 Copenhagen K", "IBAN@FIDA.dk", null, YearMonth.of(2017, 2)),

    /**
     * <strong>Greece ({@code GR})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n4!n16!c}<br>
     * Example: {@code GR1601101250000000012300695}
     */
    GR("Greece"                      , true    , 27 , "3!n4!n16!c"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), "4!n", IndexRange.of("Branch Code", 7, 11), IndexRange.of("Account No", 11, 27), "GR1601101250000000012300695",
       "Hellenic Bank Association", "Payment Systems", "Amerikis 21A", "10672 Athens", "hba@hba.gr", "+ 30 2103386500", YearMonth.of(2016, 8)),

    /**
     * <strong>Guatemala ({@code GT})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!c20!c}<br>
     * Example: {@code GT82TRAJ01020000001210029690}
     */
    GT("Guatemala"                   , false   , 28 , "4!c20!c"                , "4!c"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 28), "GT82TRAJ01020000001210029690",
       "Banco de Guatemala", "Accounting and Payment System", "7 Avenue 22-01 Zone 1", "01001 Guatemala", "contabilidad@banguat.gob.gt", "(502) 2429-6000 EXT. 4300 (502) 2253-5352", YearMonth.of(2016, 10)),

    /**
     * <strong>Honduras ({@code HN})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!n}<br>
     * Example: {@code HN88CABF00000000000250005469}
     */
    HN("Honduras"                    , false   , 28 , "4!a20!n"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 28), "HN88CABF00000000000250005469", // MSP-OK
       "Banco Central de Honduras", null, "Centro Cívico Gubernamental, Boulevard Fuerzas Armadas", "Tegucigalpa, MDC 3165", "carlos.avila@bch.hn", null, YearMonth.of(2024, 12)),

    /**
     * <strong>Croatia ({@code HR})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 7!n10!n}<br>
     * Example: {@code HR1210010051863000160}
     */
    HR("Croatia"                     , true    , 21 , "7!n10!n"                , "7!n"   , IndexRange.of("Bank Code", 4, 11), null ,       null, IndexRange.of("Account No", 11, 21), "HR1210010051863000160",
       "Croatian National Bank", "Payment Operations Area", "Trg hrvatskih velikana 3", "Zagreb / 10002", "zpp@hnb.hr", "+ 385 14564992", YearMonth.of(2016, 8)),

    /**
     * <strong>Hungary ({@code HU})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n4!n1!n15!n1!n}<br>
     * Example: {@code HU42117730161111101800000000}
     */
    HU("Hungary"                     , true    , 28 , "3!n4!n1!n15!n1!n"       , "3!n"   , IndexRange.of("Bank Code", 4, 7), "4!n", IndexRange.of("Branch Code", 7, 11), IndexRange.of("Account No", 11, 27), "HU42117730161111101800000000",
       "Hungarian Banking Association", null, "József nádor tér 5-6.", "H-1051 Budapest", "hba@hba.org.hu", "+ 36 13276030", YearMonth.of(2016, 9)),

    /**
     * <strong>Ireland ({@code IE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Example: {@code IE29AIBK93115212345678}
     */
    IE("Ireland"                     , true    , 22 , "4!a6!n8!n"              , "4!a"   , IndexRange.of("Bank Code", 4, 8), "6!n", IndexRange.of("Branch Code", 8, 14), IndexRange.of("Account No", 14, 22), "IE29AIBK93115212345678",
       "Banking & Payments Federation Ireland", null, "Floor 3 One Molesworth Street", "Dublin 2 D02 RF29", "info@bpfi.ie", null, YearMonth.of(2016, 8)),

    /**
     * <strong>Israel ({@code IL})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n3!n13!n}<br>
     * Example: {@code IL620108000000099999999}
     */
    IL("Israel"                      , false   , 23 , "3!n3!n13!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), "3!n", IndexRange.of("Branch Code", 7, 10), IndexRange.of("Account No", 10, 23), "IL620108000000099999999",
       "Bank of Israel", "Payment and Settlement Systems", "Kaplan Street, Kyriat Ben Gurion", "91007 Jerusalem", "zahav@boi.org.il", "+ 972 26552020", YearMonth.of(2016, 9)),

    /**
     * <strong>Iraq ({@code IQ})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a3!n12!n}<br>
     * Example: {@code IQ98NBIQ850123456789012}
     */
    IQ("Iraq"                        , false   , 23 , "4!a3!n12!n"             , "4!a"   , IndexRange.of("Bank Code", 4, 8), "3!n", IndexRange.of("Branch Code", 8, 11), IndexRange.of("Account No", 11, 23), "IQ98NBIQ850123456789012",
       "Central Bank of Iraq", "SWIFT Department", "Rasheed Street", "Baghdad", "cbi@cbi.iq", "+ 964 47903737479", YearMonth.of(2016, 11)),

    /**
     * <strong>Islamic Republic of Iran ({@code IR})</strong><p>
     * IBAN Length: 26<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n19!n}<br>
     * Example: {@code IR062960000000100324200001}
     */
    IR("Islamic Republic of Iran"    , false   , 26 , "3!n19!n"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 26), "IR062960000000100324200001",
       null, null, null, null, null, null, null),

    /**
     * <strong>Iceland ({@code IS})</strong><p>
     * IBAN Length: 26<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n2!n6!n10!n}<br>
     * Example: {@code IS140159260076545510730339}
     */
    IS("Iceland"                     , true    , 26 , "4!n2!n6!n10!n"          , "2!n"   , IndexRange.of("Bank Code", 4, 6), "2!n", IndexRange.of("Branch Code", 6, 8), IndexRange.of("Account No", 10, 16), "IS140159260076545510730339",
       "Icelandic Banks Data Centre", null, "Katrinartun 2", "105 Reykjavik", "hjalp@rb.is", "+ 354 5698877", YearMonth.of(2016, 8)),

    /**
     * <strong>Italy ({@code IT})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 1!a5!n5!n12!c}<br>
     * Example: {@code IT60X0542811101000000123456}
     */
    IT("Italy"                       , true    , 27 , "1!a5!n5!n12!c"          , "5!n"   , IndexRange.of("Bank Code", 5, 10), "5!n", IndexRange.of("Branch Code", 10, 15), IndexRange.of("Account No", 15, 27), "IT60X0542811101000000123456",
       "Associazione Bancaria Italiana", "Head of Payment Systems and Services", "Via delle Botteghe Oscure, 46", "00186 Rome – Italy", null, null, YearMonth.of(2013, 3)),

    /**
     * <strong>Jordan ({@code JO})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a4!n18!c}<br>
     * Example: {@code JO94CBJO0010000000000131000302}
     */
    JO("Jordan"                      , false   , 30 , "4!a4!n18!c"             , "4!a"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 30), "JO94CBJO0010000000000131000302",
       "Central Bank of Jordan", "Financial", "King Hussein Street", "11118 Amman – Capital", "finance@cbj.gov.jo", null, YearMonth.of(2025, 10)),

    /**
     * <strong>Kuwait ({@code KW})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a22!c}<br>
     * Example: {@code KW81CBKU0000000000001234560101}
     */
    KW("Kuwait"                      , false   , 30 , "4!a22!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 30), "KW81CBKU0000000000001234560101",
       "Central Bank of Kuwait", "Information Technology and Banking Operations Sector", "P.O. Box 526 Safat", "13006 Safat", "bito@cbk.gov.kw", null, YearMonth.of(2025, 10)),

    /**
     * <strong>Kazakhstan ({@code KZ})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n13!c}<br>
     * Example: {@code KZ86125KZT5004100100}
     */
    KZ("Kazakhstan"                  , false   , 20 , "3!n13!c"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 20), "KZ86125KZT5004100100",
       "National Bank of the Republic of Kazakhstan", "Payment Systems", "21, Koktem-3", "050040 Almaty", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Lebanon ({@code LB})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n20!c}<br>
     * Example: {@code LB62099900000001001901229114}
     */
    LB("Lebanon"                     , false   , 28 , "4!n20!c"                , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 28), "LB62099900000001001901229114",
       "Banque du Liban", "Payment Systems", "Masraf Lubnan street", "11-5544 Beirut", "IBAN@bdl.gov.lb", "+961-1-343317", YearMonth.of(2016, 9)),

    /**
     * <strong>Saint Lucia ({@code LC})</strong><p>
     * IBAN Length: 32<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a24!c}<br>
     * Example: {@code LC55HEMM000100010012001200023015}
     */
    LC("Saint Lucia"                 , false   , 32 , "4!a24!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 32), "LC55HEMM000100010012001200023015",
       "Saint Lucia Bureau of Standards", null, "Bisee Industrial Estate", "Castries, PO Box CP 5412", "slbs@candw.lc; info@slbs.org", null, YearMonth.of(2016, 9)),

    /**
     * <strong>Liechtenstein ({@code LI})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n12!c}<br>
     * Example: {@code LI21088100002324013AA}
     */
    LI("Liechtenstein"               , true    , 21 , "5!n12!c"                , "5!n"   , IndexRange.of("Bank Code", 4, 9), null ,       null, IndexRange.of("Account No", 9, 21), "LI21088100002324013AA",
       "Liechtenstein Bankers Association", null, "P.O. Box 254", "9490 Vaduz", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Lithuania ({@code LT})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n11!n}<br>
     * Example: {@code LT121000011101001000}
     */
    LT("Lithuania"                   , true    , 20 , "5!n11!n"                , "5!n"   , IndexRange.of("Bank Code", 4, 9), null ,       null, IndexRange.of("Account No", 9, 20), "LT121000011101001000",
       "Bank of Lithuania", "Operations and Payments Department", "Gedimino pr. 6", "Vilnius, LT-01103", "tarpbank@lb.lt", "+ 370 52680604", YearMonth.of(2025, 10)),

    /**
     * <strong>Luxembourg ({@code LU})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n13!c}<br>
     * Example: {@code LU280019400644750000}
     */
    LU("Luxembourg"                  , true    , 20 , "3!n13!c"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 20), "LU280019400644750000",
       "ABBL - Association des Banques et Banquiers Luxembourg", null, "Boîte Postale 13", "L-2010 Luxembourg", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Latvia ({@code LV})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a13!c}<br>
     * Example: {@code LV80BANK0000435195001}
     */
    LV("Latvia"                      , true    , 21 , "4!a13!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 21), "LV80BANK0000435195001",
       "Bank of Latvia", "Payment Systems", "K. Valdemāra 2A", "Riga, LV-1050", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Libya ({@code LY})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n3!n15!n}<br>
     * Example: {@code LY83002048000020100120361}
     */
    LY("Libya"                       , false   , 25 , "3!n3!n15!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), "3!n", IndexRange.of("Branch Code", 7, 10), IndexRange.of("Account No", 10, 25), "LY83002048000020100120361", // MSP-OK
       "Central Bank of Libya", "Payment and Settlement Department", "Alfatah Road", "1103 Tripoli", "info@cbl.gov.ly", "+218 912137654", YearMonth.of(2020, 9)),

    /**
     * <strong>Morocco ({@code MA})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n5!n16!n}<br>
     * Example: {@code MA64360815000001793222001617}
     */
    MA("Morocco"                     , false   , 28 , "3!n5!n16!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), "5!n", IndexRange.of("Branch Code", 7, 12), IndexRange.of("Account No", 12, 28), "MA64360815000001793222001617",
       null, null, null, null, null, null, null),

    /**
     * <strong>Monaco ({@code MC})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Example: {@code MC5811222000010123456789030}
     */
    MC("Monaco"                      , true    , 27 , "5!n5!n11!c2!n"          , "5!n"   , IndexRange.of("Bank Code", 4, 9), "5!n", IndexRange.of("Branch Code", 9, 14), IndexRange.of("Account No", 14, 25), "MC5811222000010123456789030",
       "Principauté de Monaco", null, "7, rue du Gabian", "MC98000", "amaf@amaf.mc", null, YearMonth.of(2025, 10)),

    /**
     * <strong>Moldova ({@code MD})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!c18!c}<br>
     * Example: {@code MD24AG000225100013104168}
     */
    MD("Moldova"                     , false   , 24 , "2!c18!c"                , "2!c"   , IndexRange.of("Bank Code", 4, 6), null ,       null, IndexRange.of("Account No", 6, 24), "MD24AG000225100013104168",
       "National Bank of Moldova", "Payments System", "1 Grigore Vieru Avenue", "MD-2005 Chisinau", null, null, YearMonth.of(2016, 9)),

    /**
     * <strong>Montenegro ({@code ME})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n13!n2!n}<br>
     * Example: {@code ME25505000012345678951}
     */
    ME("Montenegro"                  , false   , 22 , "3!n13!n2!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 20), "ME25505000012345678951",
       "Association of Montenegrin Banks", null, "Novaka Miloseva bb", "81000 Podgorica", "udruzenjebanaka@t-com.me", "+ 381 81232028", YearMonth.of(2010, 5)),

    /**
     * <strong>North Macedonia ({@code MK})</strong><p>
     * IBAN Length: 19<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n10!c2!n}<br>
     * Example: {@code MK07250120000058984}
     */
    MK("North Macedonia"             , false   , 19 , "3!n10!c2!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 17), "MK07250120000058984",
       "National Bank of the Republic of Macedonia", null, "K.J.Pitu 1", "1000 Skopje", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Mongolia ({@code MN})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n12!n}<br>
     * Example: {@code MN121234123456789123}
     */
    MN("Mongolia"                    , false   , 20 , "4!n12!n"                , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 20), "MN121234123456789123", // MSP-OK
       "Bank of Mongolia (The Central Bank)", null, "Baga toiruu-3", "15160 Ulaanbaatar", null, "976-11-327510", YearMonth.of(2023, 4)),

    /**
     * <strong>Mauritania ({@code MR})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Example: {@code MR1300020001010000123456753}
     */
    MR("Mauritania"                  , false   , 27 , "5!n5!n11!n2!n"          , "5!n"   , IndexRange.of("Bank Code", 4, 9), "5!n", IndexRange.of("Branch Code", 9, 14), IndexRange.of("Account No", 14, 25), "MR1300020001010000123456753",
       "Banque Centrale de Mauritanie", null, "Avenue de l'Indépendance", "BP 623 Nouakchott", "info@bcm.mr", "+ 222 45255206", YearMonth.of(2016, 9)),

    /**
     * <strong>Malta ({@code MT})</strong><p>
     * IBAN Length: 31<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a5!n18!c}<br>
     * Example: {@code MT84MALT011000012345MTLCAST001S}
     */
    MT("Malta"                       , true    , 31 , "4!a5!n18!c"             , "4!a"   , IndexRange.of("Bank Code", 4, 8), "5!n", IndexRange.of("Branch Code", 8, 13), IndexRange.of("Account No", 13, 31), "MT84MALT011000012345MTLCAST001S",
       "Malta Bankers' Association", "The Secretary General", "48/2 Birkirkara Road", "Attard ATD1210", "info@maltabankers.org", "+ 356 21412210 / + 356 21410572", YearMonth.of(2025, 10)),

    /**
     * <strong>Mauritius ({@code MU})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a2!n2!n12!n3!n3!a}<br>
     * Example: {@code MU17BOMM0101101030300200000MUR}
     */
    MU("Mauritius"                   , false   , 30 , "4!a2!n2!n12!n3!n3!a"    , "4!a2!n", IndexRange.of("Bank Code", 4, 10), "2!n", IndexRange.of("Branch Code", 10, 12), IndexRange.of("Account No", 12, 30), "MU17BOMM0101101030300200000MUR",
       "The Central Bank of Mauritius", null, "Sir William Newton Street", "Port Louis", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Mozambique ({@code MZ})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Example: {@code MZ59000800005138555713187}
     */
    MZ("Mozambique"                  , false   , 25 , "4!n4!n11!n2!n"          , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 23), "MZ59000800005138555713187",
       null, null, null, null, null, null, null),

    /**
     * <strong>Nicaragua ({@code NI})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!n}<br>
     * Example: {@code NI45BAPR00000013000003558124}
     */
    NI("Nicaragua"                   , false   , 28 , "4!a20!n"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 28), "NI45BAPR00000013000003558124", // MSP-OK
       "Banco Central de Nicaragua", null, "Paso a Desnivel Nejapa, 100 metros al este, Pista Juan Pablo II", "2252 - 2253 Managua", null, null, YearMonth.of(2024, 12)),

    /**
     * <strong>Netherlands ({@code NL})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a10!n}<br>
     * Example: {@code NL91ABNA0417164300}
     */
    NL("Netherlands"                 , true    , 18 , "4!a10!n"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 18), "NL91ABNA0417164300",
       "Betaalvereniging Nederland", null, "P.O Box 83073", "1080 AB Amsterdam", "sepa@betaalvereniging.nl", "+ 31 203051900", YearMonth.of(2020, 9)),

    /**
     * <strong>Norway ({@code NO})</strong><p>
     * IBAN Length: 15<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n6!n1!n}<br>
     * Example: {@code NO9386011117947}
     */
    NO("Norway"                      , true    , 15 , "4!n6!n1!n"              , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 14), "NO9386011117947",
       "DnB NOR Bank", null, "p.o.box 7100", "5020 Bergen", null, null, YearMonth.of(2009, 8)),

    /**
     * <strong>Oman ({@code OM})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n16!c}<br>
     * Example: {@code OM810180000001299123456}
     */
    OM("Oman"                        , false   , 23 , "3!n16!c"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 23), "OM810180000001299123456",
       "Central Bank of Oman", null, "Head Office, Al Markazi, Building Number:44 P.O. Box 1161", "112 Ruwi - Commercial Business District - Muscat", "cso@cbo.gov.om", null, YearMonth.of(2024, 3)),

    /**
     * <strong>Pakistan ({@code PK})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a16!c}<br>
     * Example: {@code PK36SCBL0000001123456702}
     */
    PK("Pakistan"                    , false   , 24 , "4!a16!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 24), "PK36SCBL0000001123456702",
       "State Bank of Pakistan", "Payment Systems", "4th Floor, Main Building, I.I. Chundrigar Road.", "74000 Karachi – Sindh – Pakistan", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Poland ({@code PL})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 8!n16!n}<br>
     * Example: {@code PL61109010140000071219812874}
     */
    PL("Poland"                      , true    , 28 , "8!n16!n"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), "8!n", IndexRange.of("Branch Code", 7, 11), IndexRange.of("Account No", 12, 28), "PL61109010140000071219812874",
       "Narodowy Bank Polski", "Payment Systems Deparment", "Świętokrzyska 11/21", "00 – 919 Warsaw", "sekretariat.dsp@nbp.pl", "48 22 185 27 25", YearMonth.of(2025, 10)),

    /**
     * <strong>Palestine ({@code PS})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a21!c}<br>
     * Example: {@code PS92PALS000000000400123456702}
     */
    PS("Palestine"                   , false   , 29 , "4!a21!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 29), "PS92PALS000000000400123456702",
       "Palestine Monetary Authority", "Payment Systems", "Al-Ramouni Nablus Street", "452 AL-BIREH", "psdsupport@pma.ps", null, YearMonth.of(2025, 10)),

    /**
     * <strong>Portugal ({@code PT})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Example: {@code PT50000201231234567890154}
     */
    PT("Portugal"                    , true    , 25 , "4!n4!n11!n2!n"          , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 23), "PT50000201231234567890154",
       "Banco de Portugal", "Payment Systems Department", "Rua do Comércio 148", "1100-150 Lisboa", "dpg@bportugal.pt", "+ 351 217813000", YearMonth.of(2025, 6)),

    /**
     * <strong>Qatar ({@code QA})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a21!c}<br>
     * Example: {@code QA58DOHB00001234567890ABCDEFG}
     */
    QA("Qatar"                       , false   , 29 , "4!a21!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 29), "QA58DOHB00001234567890ABCDEFG",
       "Qatar Central Bank", "Banking Payments and Settlement System", "Abdulla Bin Jassim Street", "1234 Doha", null, null, YearMonth.of(2014, 1)),

    /**
     * <strong>Romania ({@code RO})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a16!c}<br>
     * Example: {@code RO49AAAA1B31007593840000}
     */
    RO("Romania"                     , true    , 24 , "4!a16!c"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 24), "RO49AAAA1B31007593840000",
       "National Bank of Romania", null, "Lipscani St.,25th", "Sector 3, Bucharest 030031", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Serbia ({@code RS})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n13!n2!n}<br>
     * Example: {@code RS35260005601001611379}
     */
    RS("Serbia"                      , false   , 22 , "3!n13!n2!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 20), "RS35260005601001611379",
       "National bank of Serbia", null, "Nemanjina 17", "11000 Belgrade", "platni.sistem@nbs.rs", null, YearMonth.of(2017, 3)),

    /**
     * <strong>Russia ({@code RU})</strong><p>
     * IBAN Length: 33<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 9!n5!n15!c}<br>
     * Example: {@code RU0304452522540817810538091310419}
     */
    RU("Russia"                      , false   , 33 , "9!n5!n15!c"             , "9!n"   , IndexRange.of("Bank Code", 4, 13), "5!n", IndexRange.of("Branch Code", 13, 18), IndexRange.of("Account No", 18, 33), "RU0304452522540817810538091310419",
       "The Central Bank of the Russian Federation", null, "Neglinnaya Street, 12", "Moscow", "svc_dnps_ornps@cbr.ru", null, YearMonth.of(2025, 10)),

    /**
     * <strong>Saudi Arabia ({@code SA})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!n18!c}<br>
     * Example: {@code SA0380000000608010167519}
     */
    SA("Saudi Arabia"                , false   , 24 , "2!n18!c"                , "2!n"   , IndexRange.of("Bank Code", 4, 6), null ,       null, IndexRange.of("Account No", 6, 24), "SA0380000000608010167519",
       "SAMA, Head Office", "General Department of Payment Systems", "P.O. BOX 2992", "Riyadh 11169", "gdps@sama.gov.sa", "+ 966 114662015", YearMonth.of(2016, 9)),

    /**
     * <strong>Seychelles ({@code SC})</strong><p>
     * IBAN Length: 31<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a2!n2!n16!n3!a}<br>
     * Example: {@code SC18SSCB11010000000000001497USD}
     */
    SC("Seychelles"                  , false   , 31 , "4!a2!n2!n16!n3!a"       , "4!a2!n", IndexRange.of("Bank Code", 4, 10), "2!n", IndexRange.of("Branch Code", 10, 12), IndexRange.of("Account No", 12, 28), "SC18SSCB11010000000000001497USD",
       "Central Bank of Seychelles", null, "Independence Avenue", "Victoria - Mahe", "Psd@cbs.sc; BankingServices@cbs.sc", null, YearMonth.of(2019, 10)),

    /**
     * <strong>Sudan ({@code SD})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!n12!n}<br>
     * Example: {@code SD2129010501234001}
     */
    SD("Sudan"                       , false   , 18 , "2!n12!n"                , "2!n"   , IndexRange.of("Bank Code", 4, 6), null ,       null, IndexRange.of("Bank Code", 6, 18), "SD2129010501234001", // MSP-OK
       "Central Bank of Sudan (CBOS)", null, "HQ Building of Central Bank of SudanAljammah St.", "Khartoum 11111/313", null, null, YearMonth.of(2021, 10)),

    /**
     * <strong>Sweden ({@code SE})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n16!n1!n}<br>
     * Example: {@code SE4550000000058398257466}
     */
    SE("Sweden"                      , true    , 24 , "3!n16!n1!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 24), "SE4550000000058398257466",
       "Swedish Bankers' Association", null, null, "SE - 103 94 Stockholm", null, null, YearMonth.of(2009, 8)),

    /**
     * <strong>Slovenia ({@code SI})</strong><p>
     * IBAN Length: 19<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n8!n2!n}<br>
     * Example: {@code SI56263300012039086}
     */
    SI("Slovenia"                    , true    , 19 , "5!n8!n2!n"              , "5!n"   , IndexRange.of("Bank Code", 4, 6), "3!n", IndexRange.of("Branch Code", 6, 9), IndexRange.of("Account No", 9, 17), "SI56263300012039086", // MSP-OK
       "Bank of Slovenia", "Payment and Settlement Systems", "Slovenska 35", "SI-1505 Ljubljana", "Pomoc.PS@bsi.si", "+389 1 4719 568", YearMonth.of(2016, 10)),

    /**
     * <strong>Slovakia ({@code SK})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n6!n10!n}<br>
     * Example: {@code SK3112000000198742637541}
     */
    SK("Slovakia"                    , true    , 24 , "4!n6!n10!n"             , "4!n"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 24), "SK3112000000198742637541",
       "National Bank of Slovakia", null, "Imricha Karvaša 1", "813 25 Bratislava 1", "info@nbs.sk", null, YearMonth.of(2016, 8)),

    /**
     * <strong>San Marino ({@code SM})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 1!a5!n5!n12!c}<br>
     * Example: {@code SM86U0322509800000000270100}
     */
    SM("San Marino"                  , true    , 27 , "1!a5!n5!n12!c"          , "5!n"   , IndexRange.of("Bank Code", 5, 10), "5!n", IndexRange.of("Branch Code", 10, 15), IndexRange.of("Account No", 15, 27), "SM86U0322509800000000270100",
       "Banca Centrale della Repubblica di San Marino", "Payments System", "Via del Voltone, 120", "San Marino 47890", "sistemi.pagamento@bcsm.sm", "+ 378 882325", YearMonth.of(2016, 8)),

    /**
     * <strong>Somalia ({@code SO})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n3!n12!n}<br>
     * Example: {@code SO211000001001000100141}
     */
    SO("Somalia"                     , false   , 23 , "4!n3!n12!n"             , "4!n"   , IndexRange.of("Bank Code", 4, 8), "3!n", IndexRange.of("Branch Code", 8, 11), IndexRange.of("Account No", 11, 23), "SO211000001001000100141", // MSP-OK
       "Central Bank of Somalia", null, "Corso Somalia, 55 P.O Box 11", "Mogadishu", "info@centralbank.gov.so", null, YearMonth.of(2025, 2)),

    /**
     * <strong>Sao Tome and Principe ({@code ST})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Example: {@code ST23000100010051845310146}
     */
    ST("Sao Tome and Principe"       , false   , 25 , "4!n4!n11!n2!n"          , "4!n"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 25), "ST23000100010051845310146",
       "Banco Central de Sao Tome e Principe", "DSP", "Avenida Marginal 12 de Julho", "CP 13 Sao Tome", "dsp@bcstp.st", "+239 2243700", YearMonth.of(2020, 5)),

    /**
     * <strong>El Salvador ({@code SV})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!n}<br>
     * Example: {@code SV62CENR00000000000000700025}
     */
    SV("El Salvador"                 , false   , 28 , "4!a20!n"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 28), "SV62CENR00000000000000700025",
       "Banco Central de Reserva de El Salvador", "Departamento de Pagos y Valores", "1a Calle Poniente y 7Av. Nte. N 418", "San Salvador", "maria.delgado@bcr.gov.sv", "503 2281 8831", YearMonth.of(2021, 3)),

    /**
     * <strong>Timor-Leste ({@code TL})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n14!n2!n}<br>
     * Example: {@code TL380080012345678910157}
     */
    TL("Timor-Leste"                 , false   , 23 , "3!n14!n2!n"             , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 21), "TL380080012345678910157",
       "Banco Central de Timor-Leste", null, "Avenida Bispo Medeiros", "Dili", null, null, YearMonth.of(2014, 11)),

    /**
     * <strong>Tunisia ({@code TN})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!n3!n13!n2!n}<br>
     * Example: {@code TN5910006035183598478831}
     */
    TN("Tunisia"                     , false   , 24 , "2!n3!n13!n2!n"          , "2!n"   , IndexRange.of("Bank Code", 4, 6), "3!n", IndexRange.of("Branch Code", 6, 9), IndexRange.of("Account No", 9, 22), "TN5910006035183598478831",
       "Tunisia's Professional Association for Banks & Financial Institutions", null, null, null, "info@apbt.org.tn", "+ 216 71904423", YearMonth.of(2016, 5)),

    /**
     * <strong>Turkey ({@code TR})</strong><p>
     * IBAN Length: 26<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n1!n16!c}<br>
     * Example: {@code TR330006100519786457841326}
     */
    TR("Turkey"                      , false   , 26 , "5!n1!n16!c"             , "5!n"   , IndexRange.of("Bank Code", 4, 9), null ,       null, IndexRange.of("Account No", 10, 26), "TR330006100519786457841326",
       "Central Bank of the Republic of Turkey", "Payment Systems", "Anafartalar Mahallesi İstiklal Cad. No:10 Ulus Altındağ", "Ankara / 06050", "paymentsystems@tcmb.gov.tr", "+ 90 3125077901 / 02", YearMonth.of(2025, 10)),

    /**
     * <strong>Ukraine ({@code UA})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 6!n19!c}<br>
     * Example: {@code UA213223130000026007233566001}
     */
    UA("Ukraine"                     , false   , 29 , "6!n19!c"                , "6!n"   , IndexRange.of("Bank Code", 4, 10), null ,       null, IndexRange.of("Account No", 10, 29), "UA213223130000026007233566001",
       "Association UkrSWIFT", "Executive Board", "21A, Observatoma Str.", "04053 Kiev", "ukrswift@ukrswift.org", null, YearMonth.of(2025, 10)),

    /**
     * <strong>Holy See ({@code VA})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n15!n}<br>
     * Example: {@code VA59001123000012345678}
     */
    VA("HolyVatican City State"          , true    , 22 , "3!n15!n"                , "3!n"   , IndexRange.of("Bank Code", 4, 7), null ,       null, IndexRange.of("Account No", 7, 22), "VA59001123000012345678",
       "Financial Information Authority (Autorita di Informazione Finanziaria - AIF)", "Office for Supervision and Regulation", "Palazzo San Carlo", " 00120", "uvr@aif.va", "+39 06 69871522", YearMonth.of(2025, 10)),

    /**
     * <strong>Virgin Islands ({@code VG})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a16!n}<br>
     * Example: {@code VG96VPVG0000012345678901}
     */
    VG("Virgin Islands"              , false   , 24 , "4!a16!n"                , "4!a"   , IndexRange.of("Bank Code", 4, 8), null ,       null, IndexRange.of("Account No", 8, 24), "VG96VPVG0000012345678901",
       "VP Bank House", null, "156 Mainstreet", "VG1110 Road Town Tortola", null, null, YearMonth.of(2025, 10)),

    /**
     * <strong>Kosovo ({@code XK})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n10!n2!n}<br>
     * Example: {@code XK051212012345678906}
     */
    XK("Kosovo"                      , false   , 20 , "4!n10!n2!n"             , "2!n"   , IndexRange.of("Bank Code", 4, 6), "2!n", IndexRange.of("Branch Code", 6, 8), IndexRange.of("Account No", 8, 18), "XK051212012345678906",
       "Central Bank of the Republic of Kosovo", "Payment Systems", "Garibaldi 33", "Prishtina / 10000", "payment.systems@bqk-kos.org", "+ 381 (0)38222055 (ext. 209, 210 and 211)", YearMonth.of(2016, 9)),

    /**
     * <strong>Yemen ({@code YE})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a4!n18!c}<br>
     * Example: {@code YE15CBYE0001018861234567891234}
     */
    YE("Yemen"                       , false   , 30 , "4!a4!n18!c"             , "4!a"   , IndexRange.of("Bank Code", 4, 8), "4!n", IndexRange.of("Branch Code", 8, 12), IndexRange.of("Account No", 12, 30), "YE15CBYE0001018861234567891234", // MSP-OK
       "Central Bank of Yemen", "Payment Systems Depatment", "Crater Aledaroos Street 452", "Aden", null, null, YearMonth.of(2024, 7))
    ;

    private final String                           countryName;
    private final String                           countryFlag;
    private final boolean                          isSepa;
    private final int                              ibanLength;
    private final String                           bbanPatternStr;

    private final String                           bankCodePatternStr;
    private final IndexRange                       bankCodeIndexRange;

    private final String                           branchCodePatternStr;
    private final IndexRange                       branchCodeIndexRange;

    private final IndexRange                       accountNumberIndexRange;

    private final String                           ibanExample;

    private final String                           organisation;
    private final String                           department;
    private final String                           streetAddress;
    private final String                           cityPostcode;
    private final String                           departmentGenericEmail;
    private final String                           departmentTel;

    private final YearMonth                        lastUpdate;

    private final CountryValidator                 countryValidator;
    private       IbanRegistry                     primary;

    private final Pattern                          ibanRegex;

    /** The minimum required length: Country Code (2) + Check Digits (2). */
    static final int                               MIN_IBAN_BASE_LENGTH = 4;

    /** ISO 13616 standard minimum. */
    static final int                               MIN_IBAN_LENGTH      = Arrays.stream(values())
        .mapToInt(IbanRegistry::getIbanLength).min().orElse(MIN_IBAN_BASE_LENGTH);

    /** ISO 13616 standard maximum. */
    static final int                               MAX_IBAN_LENGTH      = Arrays.stream(values())
        .mapToInt(IbanRegistry::getIbanLength).max().orElse(34);

    /** Begin index of IBAN check digits within the full IBAN string (position 3, 0-based index 2). */
    static final int                               INDEX_CHECK_DIGITS   = 2;

    /** Begin index of BBAN within IBAN (position 5, 0-based index 4). */
    static final int                               INDEX_BBAN           = MIN_IBAN_BASE_LENGTH;

    /** Maximum length of BBAN. */
    static final int                               MAX_BBAN_LENGTH      = MAX_IBAN_LENGTH - INDEX_BBAN;

    /** The map for quick lookups by country code. */
    private static final Map<CharSequence, IbanRegistry> CODE_MAP             = buildCodeMap();

    /**
     * Main constructor for {@code IbanRegistry} enum constants.
     * <p>
     * Initializes the country's IBAN structure, format details, and regulatory contact information.
     *
     * @param countryName             The full English name of the country.
     * @param isSepa                  Whether the country is part of the SEPA zone.
     * @param ibanLength              The fixed length of the IBAN for this country.
     * @param bbanPatternStr          The BBAN structure pattern string (e.g., "4!n4!n12!c").
     * @param bankCodePatternStr      The character pattern for the Bank Identifier Code.
     * @param bankCodeIndexRange      The index range of the Bank Code within the IBAN.
     * @param branchCodePatternStr    The character pattern for the Branch Identifier Code.
     * @param branchCodeIndexRange    The index range of the Branch Code within the IBAN (may be {@code null}).
     * @param accountNumberIndexRange The index range of the Account Number within the IBAN.
     * @param ibanExample             An example of a valid IBAN for this country.
     * @param organisation            The name of the responsible financial organization/central bank.
     * @param department              The relevant department name.
     * @param streetAddress           The street address of the organization.
     * @param cityPostcode            The city and postal code of the organization.
     * @param departmentGenericEmail  A generic contact email address.
     * @param departmentTel           A contact telephone number.
     * @param lastUpdate              The month and year the data was last updated.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    IbanRegistry(
        String countryName,
        boolean isSepa,
        int ibanLength,

        String bbanPatternStr,
        String bankCodePatternStr,
        IndexRange bankCodeIndexRange,
        String branchCodePatternStr,
        IndexRange branchCodeIndexRange,
        IndexRange accountNumberIndexRange,

        String ibanExample,

        String organisation,
        String department,
        String streetAddress,
        String cityPostcode,
        String departmentGenericEmail,
        String departmentTel,

        YearMonth lastUpdate
        ) {

        this.countryName = Objects.requireNonNull(countryName, "countryName required");
        this.countryFlag = CountryUtil.createFlagEmoji(name());

        this.isSepa = isSepa;
        this.ibanLength = ibanLength;
        this.bbanPatternStr = Objects.requireNonNull(bbanPatternStr, "bbanPatternStr required");

        this.ibanRegex = Pattern.compile('^' + IbanPatternConverter.convertToRegex("2!a2!n" + bbanPatternStr) + '$');

        this.bankCodePatternStr = bankCodePatternStr;
        this.bankCodeIndexRange = bankCodeIndexRange;

        this.branchCodePatternStr = branchCodePatternStr;
        this.branchCodeIndexRange = branchCodeIndexRange;

        this.accountNumberIndexRange = accountNumberIndexRange;

        this.ibanExample = ibanExample;

        this.organisation = organisation;
        this.department = department;
        this.streetAddress = streetAddress;
        this.cityPostcode = cityPostcode;
        this.departmentGenericEmail = departmentGenericEmail;
        this.departmentTel = departmentTel;

        this.lastUpdate = lastUpdate;

        this.countryValidator = loadValidator(this.name());
    }

    /**
     * Secondary constructor for country codes that share their registry data with a primary country.
     * <p>
     * Copies all structural and contact data from the specified primary entry.
     *
     * @param countryName The full English name of the country.
     * @param primary     The {@code IbanRegistry} enum constant whose data is to be used.
     */
    IbanRegistry(String countryName, IbanRegistry primary) {
        this(
            countryName,
            primary.isSepa,
            primary.ibanLength,
            primary.bbanPatternStr,
            primary.bankCodePatternStr,
            primary.bankCodeIndexRange,
            primary.branchCodePatternStr,
            primary.branchCodeIndexRange,
            primary.accountNumberIndexRange,
            primary.ibanExample,
            primary.organisation,
            primary.department,
            primary.streetAddress,
            primary.cityPostcode,
            primary.departmentGenericEmail,
            primary.departmentTel,
            primary.lastUpdate
        );
        this.primary = primary;
    }

    /**
     * Returns the ISO 3166-1 Alpha-2 country code (e.g., "DE", "IT").
     *
     * @return The two-letter country code.
     */
    String getCountryCode() {
        return name();
    }

    /**
     * Returns the full English name of the country.
     *
     * @return The country name.
     */
    String getCountryName() {
        return countryName;
    }

    /**
     * Returns the two-character country flag emoji.
     *
     * @return The country flag emoji string.
     */
    String getCountryFlag() {
        return countryFlag;
    }

    /**
     * Returns whether the country is a part of the SEPA zone.
     *
     * @return {@code true} if the country is in SEPA, {@code false} otherwise.
     */
    boolean isSepa() {
        return isSepa;
    }

    /**
     * Returns the fixed length of the IBAN for this country.
     *
     * @return The total IBAN length.
     */
    int getIbanLength() {
        return ibanLength;
    }

    /**
     * Returns the regular expression {@code Pattern} object of the IBAN.
     *
     * @return The IBAN pattern object.
     */
    Pattern getIbanRegex() {
        return ibanRegex;
    }

    /**
     * Returns the length of the BBAN (Basic Bank Account Number) part of the IBAN.
     *
     * @return The BBAN length.
     */
    int getBbanLength() {
        return ibanLength - INDEX_BBAN;
    }

    /**
     * Returns the BBAN structure pattern string (e.g., "4!n4!n12!c").
     *
     * @return The BBAN pattern string.
     */
    String getBbanPatternStr() {
        return bbanPatternStr;
    }

    /**
     * Returns an example of a valid IBAN for this country.
     *
     * @return The IBAN example string.
     */
    String getIbanExample() {
        return ibanExample;
    }

    /**
     * Returns the character pattern string for the Bank Identifier Code.
     *
     * @return The bank code pattern string.
     */
    String getBankCodePatternStr() {
        return bankCodePatternStr;
    }

    /**
     * Returns the index range defining the position of the Bank Code within the IBAN.
     *
     * @return The {@code IndexRange} for the bank code.
     */
    IndexRange getBankCodeIndexRange() {
        return bankCodeIndexRange;
    }

    /**
     * Returns the character pattern string for the Branch Identifier Code.
     *
     * @return The branch code pattern string.
     */
    String getBranchCodePattern() {
        return branchCodePatternStr;
    }

    /**
     * Returns the index range defining the position of the Branch Code within the IBAN.
     *
     * @return The {@code IndexRange} for the branch code, or {@code null}.
     */
    IndexRange getBranchCodeIndexRange() {
        return branchCodeIndexRange;
    }

    /**
     * Returns whether the country's BBAN structure defines a separate Branch Code part.
     *
     * @return {@code true} if a branch code exists, {@code false} otherwise.
     */
    boolean hasBranchCode() {
        return branchCodeIndexRange != null;
    }

    /**
     * Returns the index range defining the position of the Account Number within the IBAN.
     *
     * @return The {@code IndexRange} for the account number.
     */
    IndexRange getAccountNumberIndexRange() {
        return accountNumberIndexRange;
    }

    /**
     * Returns the name of the regulatory or financial organization responsible for the IBAN registry.
     *
     * @return The organization's name.
     */
    String getOrganisation() {
        return organisation;
    }

    /**
     * Returns the relevant department name of the organization.
     *
     * @return The department name.
     */
    String getDepartment() {
        return department;
    }

    /**
     * Returns the street address of the organization.
     *
     * @return The street address.
     */
    String getStreetAddress() {
        return streetAddress;
    }

    /**
     * Returns the city and postal code of the organization.
     *
     * @return The city and postcode.
     */
    String getCityPostcode() {
        return cityPostcode;
    }

    /**
     * Returns the generic contact email address for the department.
     *
     * @return The email address.
     */
    String getDepartmentGenericEmail() {
        return departmentGenericEmail;
    }

    /**
     * Returns the contact telephone number for the department.
     *
     * @return The telephone number.
     */
    String getDepartmentTel() {
        return departmentTel;
    }

    /**
     * Returns the month and year the registry data was last updated by the source.
     *
     * @return The {@code YearMonth} of the last update.
     */
    YearMonth getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Returns the optional {@link CountryValidator} instance used for country-specific structure validation.
     *
     * @return The country validator implementation, or {@code null} if one is not found.
     */
    CountryValidator getCountryValidator() {
        return countryValidator;
    }

    /**
     * Returns the primary {@code IbanRegistry} entry if this entry is a secondary code
     * that shares data with another country (e.g., {@code AX} points to {@code FI}).
     *
     * @return The primary {@code IbanRegistry} entry, or {@code null} if this is a primary entry.
     */
    IbanRegistry getPrimary() {
        return primary;
    }

    /**
     * Provides a detailed representation of the registry entry's structure.
     *
     * @return A string representation of this registry entry.
     */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add(getCountryCode() + " (" + countryName + ")")
            .add("SEPA country: " + (isSepa ? "Yes" : "No"))
            .add("IBAN len: " + ibanLength)
            .add("BBAN pattern: " + bbanPatternStr)
            .add(Objects.toString(bankCodeIndexRange));
        if (branchCodeIndexRange != null) {
            joiner.add(Objects.toString(branchCodeIndexRange));
        }
        return joiner
            .add(Objects.toString(accountNumberIndexRange))
            .add("IBAN Example: " + ibanExample)
            .add("Organization: " + organisation)
            .add("Last Update: " + lastUpdate)
            .toString();
    }

    /**
     * Dynamically loads and instantiates the country-specific IBAN validator
     * using reflection based on the two-letter country code.
     * <p>
     * This method expects the validator implementation to be defined as a public
     * nested static class within the {@code ICountryValidator} interface,
     * named after the country code (e.g., {@code ICountryValidator.AD} for "AD").
     *
     * @param countryCode The two-letter country code (e.g., "DE", "AD").
     * @return            The instantiated {@link CountryValidator} for the given country,
     * or {@code null} if the validator class cannot be found or instantiated.
     */
    private static CountryValidator loadValidator(final String countryCode) {
        String className = CountryValidator.class.getName() + '$' + countryCode;

        try {
            Class<?> validatorClass = Class.forName(className);
            Object instance = validatorClass.getDeclaredConstructor().newInstance();
            return (CountryValidator) instance;

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Builds the static, immutable map for quick {@code IbanRegistry} lookups by country code.
     *
     * @return An unmodifiable {@code Map<String, IbanRegistry>} ready for static member assignment.
     * The map keys are interned country codes.
     */
    private static Map<CharSequence, IbanRegistry> buildCodeMap() {
        Map<String, IbanRegistry> map = new LinkedHashMap<>();
        for (IbanRegistry registry : values()) {
            map.put(registry.name().intern(), registry);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns the registry entry for a given country code instantly.
     *
     * @param code The two-letter country code (e.g., "DE").
     * @return     The {@code IbanRegistry} entry, or {@code null} if the country code is unsupported.
     */
    public static IbanRegistry getByCode(final CharSequence code) {
        return CODE_MAP.get(code);
    }

    /**
     * Returns the registry entry for a given country code instantly.
     *
     * @param c0 The first character of the country code.
     * @param c1 The second character of the country code.
     * @return   The {@code IbanRegistry} entry, or {@code null} if the country code is unsupported.
     */
    public static IbanRegistry getByCode(final char c0, final char c1) {
        return getByCode(new String(new char[] {c0, c1}).intern());
    }

}
