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
package de.speedbanking.iban;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.util.CountryUtil;
import de.speedbanking.util.Currency;
import de.speedbanking.util.IndexRange;
import de.speedbanking.util.Iso3166Alpha2;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * The definitive, immutable registry for all **ISO 13616-compliant national IBAN formats**.
 * <p>
 * This enumeration holds the official structural rules (total length, BBAN pattern, component index ranges)
 * for each country, as published in the **SWIFT IBAN Registry (Release 100 - Oct 2025)**.<br>
 * Additional countries participating in the IBAN scheme are manually maintained in this class.
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
 * Indexing is zero-based and exclusive of the end index (Java's substring convention).<br>
 * Example: A position "1-4" in the registry (BBAN starts at char 0) becomes index 4 to 8 in the full IBAN string.
 *
 * @since 1.8.0
 */
@SuppressWarnings("ImmutableEnumChecker")
public enum IbanRegistry {

    // --- BGN: Enum Constants (generated from IBAN Registry) ---

    /**
     * <strong>Andorra ({@code AD})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n4!n12!c}<br>
     * Examples:<pre>
     *   unformatted: {@code AD1200012030200359100100}
     *   formatted:   {@code AD12 0001 2030 2003 5910 0100}
     *   components:  {@code AD 12 0001 2030 200359100100}
     * </pre>
     */
    AD(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("4!n4!n12!c")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 24))
         .build(),
       MetaData.of(
           "Andorra", true, "AD1200012030200359100100",
           YearMonth.of(2021, 3)),
       ContactData.of(
           "Associacio de Bancs Andorrans (ABA)", null, "C/ Ciutat de Consuegra, 16 Edifici l'Illa, esc. A, 2n pis",
           "AD500 Andorra la Vella Principat d'Andorra", "aba@aba.ad", "376 80 71 10")
    ),

    /**
     * <strong>United Arab Emirates ({@code AE})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n16!n}<br>
     * Examples:<pre>
     *   unformatted: {@code AE070331234567890123456}
     *   formatted:   {@code AE07 0331 2345 6789 0123 456}
     *   components:  {@code AE 07 033 1234567890123456}
     * </pre>
     */
    AE(StructureData.builder()
         .withIbanLength(23)
         .withBbanPattern("3!n16!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 23))
         .build(),
       MetaData.of(
           "United Arab Emirates", false, "AE070331234567890123456",
           YearMonth.of(2025, 2)),
       ContactData.of(
           "Central Bank of the United Arab Emirates", null, "Bainuna Street, Al Bateen",
           "Abu Dhabi PO Box 854", null, null)
    ),

    /**
     * <strong>Albania ({@code AL})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 8!n16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code AL47212110090000000235698741}
     *   formatted:   {@code AL47 2121 1009 0000 0002 3569 8741}
     *   components:  {@code AL 47 212 1100 9 0000000235698741}
     * </pre>
     */
    AL(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("8!n16!c")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("5!n", IndexRange.of(7, 11))
         .withNationalCheckDigit(IndexRange.of(11, 12)) // AL
         .withAccountNumber(IndexRange.of(12, 28))
         .build(),
       MetaData.of(
           "Albania", false, "AL47212110090000000235698741",
           YearMonth.of(2025, 6)),
       ContactData.of(
           "Bank of Albania", "Payment systems", "Kompleksi Halili Rruga e Dibres",
           "1000 Tirana", null, null)
    ),

    /**
     * <strong>Angola ({@code AO})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code AO06000600000100037131174}
     *   formatted:   {@code AO06 0006 0000 0100 0371 3117 4}
     *   components:  {@code AO 06 0006 0000 01000371311 74}
     * </pre>
     */
    AO(StructureData.builder()
         .withIbanLength(25)
         .withBbanPattern("4!n4!n11!n2!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 23))
         .withNationalCheckDigit(IndexRange.of(23, 25)) // AO
         .build(),
       MetaData.of(
           "Angola", false, "AO06000600000100037131174",
           null),
       null),

    /**
     * <strong>Austria ({@code AT})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n11!n}<br>
     * Examples:<pre>
     *   unformatted: {@code AT611904300234573201}
     *   formatted:   {@code AT61 1904 3002 3457 3201}
     *   components:  {@code AT 61 19043 00234573201}
     * </pre>
     */
    AT(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("5!n11!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withAccountNumber(IndexRange.of(9, 20))
         .build(),
       MetaData.of(
           "Austria", true, "AT611904300234573201",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "PSA Payement Services Austria GMBH", "Account Systems", "Rivergate 2, Handelskai 92",
           "1200 Wien", "accountsystems@psa.at", "+ 43 15053280 / 0")
    ),

    /**
     * <strong>Azerbaijan ({@code AZ})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!c}<br>
     * Examples:<pre>
     *   unformatted: {@code AZ21NABZ00000000137010001944}
     *   formatted:   {@code AZ21 NABZ 0000 0000 1370 1000 1944}
     *   components:  {@code AZ 21 NABZ 00000000137010001944}
     * </pre>
     */
    AZ(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!a20!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 28))
         .build(),
       MetaData.of(
           "Azerbaijan", false, "AZ21NABZ00000000137010001944",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Central Bank of the Republic of Azerbaijan", "Head Office", "32, R. Behbudov",
           "AZ 1014 Baku", "payment_systems@cbar.az", "+ 994 124931122")
    ),

    /**
     * <strong>Bosnia and Herzegovina ({@code BA})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n3!n8!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code BA391290079401028494}
     *   formatted:   {@code BA39 1290 0794 0102 8494}
     *   components:  {@code BA 39 129 007 94010284 94}
     * </pre>
     */
    BA(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("3!n3!n8!n2!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("3!n", IndexRange.of(7, 10))
         .withAccountNumber(IndexRange.of(10, 18))
         .withNationalCheckDigit(IndexRange.of(18, 20)) // BA
         .build(),
       MetaData.of(
           "Bosnia and Herzegovina", false, "BA391290079401028494",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Centralna banka Bosne i Hercegovine", "Payment Systems Division", "25 Maršala Tita Street",
           "71000 Sarajevo, Bosnia and Herzegovina", null, null)
    ),

    /**
     * <strong>Belgium ({@code BE})</strong><p>
     * IBAN Length: 16<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n7!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code BE68539007547034}
     *   formatted:   {@code BE68 5390 0754 7034}
     *   components:  {@code BE 68 539 0075470 34}
     * </pre>
     */
    BE(StructureData.builder()
         .withIbanLength(16)
         .withBbanPattern("3!n7!n2!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 14))
         .withNationalCheckDigit(IndexRange.of(14, 16)) // BE
         .build(),
       MetaData.of(
           "Belgium", true, "BE68539007547034",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Febelfin", "Payments & Operations", "Aarlenstraat 82",
           "1040 Brussels", "info@febelfin.be", "+ 32 25076811")
    ),

    /**
     * <strong>Bulgaria ({@code BG})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a4!n2!n8!c}<br>
     * Examples:<pre>
     *   unformatted: {@code BG80BNBG96611020345678}
     *   formatted:   {@code BG80 BNBG 9661 1020 3456 78}
     *   components:  {@code BG 80 BNBG 966110 20345678}
     * </pre>
     */
    BG(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("4!a4!n2!n8!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(14, 22))
         .build(),
       MetaData.of(
           "Bulgaria", true, "BG80BNBG96611020345678",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Bulgarian National Bank", "Payment Systems and Minimum Reserves Directorate", "1, Knyaz Alexander I Sq.",
           "1000 Sofia, Bulgaria", "rtgs@bnbank.org", "+ 359 29145761")
    ),

    /**
     * <strong>Bahrain ({@code BH})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a14!c}<br>
     * Examples:<pre>
     *   unformatted: {@code BH67BMAG00001299123456}
     *   formatted:   {@code BH67 BMAG 0000 1299 1234 56}
     *   components:  {@code BH 67 BMAG 00001299123456}
     * </pre>
     */
    BH(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("4!a14!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 22))
         .build(),
       MetaData.of(
           "Bahrain", false, "BH67BMAG00001299123456",
           YearMonth.of(2012, 1)),
       ContactData.of(
           "Central Bank of Bahrain", "Banking Services Directorate", "King Faisal Highway, Block 317, Road 1702, Building 96",
           "Manama", null, null)
    ),

    /**
     * <strong>Burundi ({@code BI})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code BI4210000100010000332045181}
     *   formatted:   {@code BI42 1000 0100 0100 0033 2045 181}
     *   components:  {@code BI 42 10000 10001 0000332045181}
     * </pre>
     */
    BI(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 27))
         .build(),
       MetaData.of(
           "Burundi", false, "BI4210000100010000332045181",
           YearMonth.of(2021, 10)),
       ContactData.of(
           "Banque de la Republique du Burundi", null, "1, Avenue du Gouvernement PO BOX 705",
           "Bujumbura", "brb@brb.bi", null)
    ),

    /**
     * <strong>Brazil ({@code BR})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 8!n5!n10!n1!a1!c}<br>
     * Examples:<pre>
     *   unformatted: {@code BR1800360305000010009795493C1}
     *   formatted:   {@code BR18 0036 0305 0000 1000 9795 493C 1}
     *   components:  {@code BR 18 00360305 00001 0009795493C1}
     * </pre>
     */
    BR(StructureData.builder()
         .withIbanLength(29)
         .withBbanPattern("8!n5!n10!n1!a1!c")
         .withBankCode("8!n", IndexRange.of(4, 12))
         .withBranchCode("5!n", IndexRange.of(12, 17))
         .withAccountNumber(IndexRange.of(17, 27))
         .build(),
       MetaData.of(
           "Brazil", false, "BR1800360305000010009795493C1",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Banco Central do Brasil", "DEBAN - Departamento de Operações Bancárias e de Sistema de Pagamentos", "SBS Quadra 3 Bloco B",
           "71.070-900 Brasília", "iban@bcb.gov.br", "+ 55 (61)34142666 / + 55 (51)32157339")
    ),

    /**
     * <strong>Belarus ({@code BY})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!c4!n16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code BY13NBRB3600900000002Z00AB00}
     *   formatted:   {@code BY13 NBRB 3600 9000 0000 2Z00 AB00}
     *   components:  {@code BY 13 NBRB 3600 900000002Z00AB00}
     * </pre>
     */
    BY(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!c4!n16!c")
         .withBankCode("4!c", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 28))
         .build(),
       MetaData.of(
           "Belarus", false, "BY13NBRB3600900000002Z00AB00",
           YearMonth.of(2024, 2)),
       ContactData.of(
           "National Bank of the Republic of Belarus", "Payment system and digital technologies directorate", "Nezavisimosty Avenue, 20",
           "220008 Minsk", null, null)
    ),

    /**
     * <strong>Switzerland ({@code CH})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n12!c}<br>
     * Examples:<pre>
     *   unformatted: {@code CH9300762011623852957}
     *   formatted:   {@code CH93 0076 2011 6238 5295 7}
     *   components:  {@code CH 93 00762 011623852957}
     * </pre>
     */
    CH(StructureData.builder()
         .withIbanLength(21)
         .withBbanPattern("5!n12!c")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withAccountNumber(IndexRange.of(9, 21))
         .build(),
       MetaData.of(
           "Switzerland", true, "CH9300762011623852957",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "SIX Interbank Clearing Ltd", "Zentrale Koordinationsstelle fuer IBAN/IPI - Technical Support", "Hardturmstrasse 201",
           "CH-8021 ZURICH", "iban@six-group.com", "+ 41 583994420")
    ),

    /**
     * <strong>Costa Rica ({@code CR})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n14!n}<br>
     * Examples:<pre>
     *   unformatted: {@code CR05015202001026284066}
     *   formatted:   {@code CR05 0152 0200 1026 2840 66}
     *   components:  {@code CR 05 0152 02001026284066}
     * </pre>
     */
    CR(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("4!n14!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 22))
         .build(),
       MetaData.of(
           "Costa Rica", false, "CR05015202001026284066",
           YearMonth.of(2019, 1)),
       ContactData.of(
           "Banco Central de Costa Rica", "Sistema de Pagos", "Avenida Central y 1a. Calles 2 y 4",
           "10058-1000 San José", null, null)
    ),

    /**
     * <strong>Cabo Verde ({@code CV})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n13!c}<br>
     * Examples:<pre>
     *   unformatted: {@code CV05123412341234123412341}
     *   formatted:   {@code CV05 1234 1234 1234 1234 1234 1}
     *   components:  {@code CV 05 1234 1234 1234123412341}
     * </pre>
     */
    CV(StructureData.builder()
         .withIbanLength(25)
         .withBbanPattern("4!n4!n13!c")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 25))
         .build(),
       MetaData.of(
           "Cape Verde", false, "CV05123412341234123412341",
           null),
       null),

    /**
     * <strong>Cyprus ({@code CY})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n5!n16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code CY17002001280000001200527600}
     *   formatted:   {@code CY17 0020 0128 0000 0012 0052 7600}
     *   components:  {@code CY 17 002 00128 0000001200527600}
     * </pre>
     */
    CY(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("3!n5!n16!c")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("5!n", IndexRange.of(7, 12))
         .withAccountNumber(IndexRange.of(12, 28))
         .build(),
       MetaData.of(
           "Cyprus", true, "CY17002001280000001200527600",
           YearMonth.of(2009, 8)),
       ContactData.of(
           "Central Bank of Cyprus", "Payment systems and Accounting Services", "80 Kennedy Avenue",
           "P.O. Box 25529 CY-1395 Nicosia", "PaymentSystems@centralbank.gov.cy", null)
    ),

    /**
     * <strong>Czechia ({@code CZ})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n16!n}<br>
     * Examples:<pre>
     *   unformatted: {@code CZ6508000000192000145399}
     *   formatted:   {@code CZ65 0800 0000 1920 0014 5399}
     *   components:  {@code CZ 65 0800 0000192000145399}
     * </pre>
     */
    CZ(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("4!n16!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 24))
         .build(),
       MetaData.of(
           "Czechia", true, "CZ6508000000192000145399",
           YearMonth.of(2025, 6)),
       ContactData.of(
           "Czech National Bank", "Cash and Payments Department", "Na Příkopě 28",
           "Praha 1", "iban.info@cnb.cz", null)
    ),

    /**
     * <strong>Germany ({@code DE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 8!n10!n}<br>
     * Examples:<pre>
     *   unformatted: {@code DE89370400440532013000}
     *   formatted:   {@code DE89 3704 0044 0532 0130 00}
     *   components:  {@code DE 89 37040044 0532013000}
     * </pre>
     */
    DE(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("8!n10!n")
         .withBankCode("8!n", IndexRange.of(4, 12))
         .withAccountNumber(IndexRange.of(12, 22))
         .build(),
       MetaData.of(
           "Germany", true, "DE89370400440532013000",
           YearMonth.of(2011, 1)),
       ContactData.of(
           "Bundesverband deutscher Banken", null, "Burgstraße 28",
           "10178 Berlin", "iban@bdb.de", "+ 49 3016632301")
    ),

    /**
     * <strong>Djibouti ({@code DJ})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code DJ2100010000000154000100186}
     *   formatted:   {@code DJ21 0001 0000 0001 5400 0100 186}
     *   components:  {@code DJ 21 00010 00000 0154000100186}
     * </pre>
     */
    DJ(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .build(),
       MetaData.of(
           "Djibouti", false, "DJ2100010000000154000100186",
           YearMonth.of(2022, 5)),
       ContactData.of(
           "Banque Centrale de Djibouti", null, "Avenue Cheick Osman P.O Box 705",
           "Djibouti", "bndj@intnet.dj", null)
    ),

    /**
     * <strong>Denmark ({@code DK})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n9!n1!n}<br>
     * Examples:<pre>
     *   unformatted: {@code DK5000400440116243}
     *   formatted:   {@code DK50 0040 0440 1162 43}
     *   components:  {@code DK 50 0040 0440116243}
     * </pre>
     */
    DK(StructureData.builder()
         .withIbanLength(18)
         .withBbanPattern("4!n9!n1!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 18))
         .build(),
       MetaData.of(
           "Denmark", true, "DK5000400440116243",
           YearMonth.of(2018, 11)),
       ContactData.of(
           "Finance Denmark", null, "7 Amaliegade",
           "DK 1256 Copenhagen K", "IBAN@FIDA.dk", null)
    ),

    /**
     * <strong>Dominican Republic ({@code DO})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!c20!n}<br>
     * Examples:<pre>
     *   unformatted: {@code DO28BAGR00000001212453611324}
     *   formatted:   {@code DO28 BAGR 0000 0001 2124 5361 1324}
     *   components:  {@code DO 28 BAGR 00000001212453611324}
     * </pre>
     */
    DO(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!c20!n")
         .withBankCode("4!c", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 28))
         .build(),
       MetaData.of(
           "Dominican Republic", false, "DO28BAGR00000001212453611324",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Central Bank of the Dominican Republic", "Payment Systems", "Av. Pedro Henríquez Urena esq. Leopoldo Navarro",
           "Santo Domingo", "sistema.pagos@bancentral.gov.do", "+ 1 8092219111 (ext. 3409)")
    ),

    /**
     * <strong>Estonia ({@code EE})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 2!n14!n}<br>
     * Examples:<pre>
     *   unformatted: {@code EE382200221020145685}
     *   formatted:   {@code EE38 2200 2210 2014 5685}
     *   components:  {@code EE 38 22 00 22102014568 5}
     * </pre>
     */
    EE(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("2!n14!n")
         .withBankCode("2!n", IndexRange.of(4, 6))
         .withBranchCode("2!n", IndexRange.of(6, 8))
         .withAccountNumber(IndexRange.of(8, 19))
         .withNationalCheckDigit(IndexRange.of(19, 20)) // EE
         .build(),
       MetaData.of(
           "Estonia", true, "EE382200221020145685",
           YearMonth.of(2024, 12)),
       ContactData.of(
           "Estonian Banking Association", null, "Maakri 30",
           "10145 Tallinn", "pangaliit@pangaliit.ee", "+ 372 6116569")
    ),

    /**
     * <strong>Egypt ({@code EG})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n17!n}<br>
     * Examples:<pre>
     *   unformatted: {@code EG380019000500000000263180002}
     *   formatted:   {@code EG38 0019 0005 0000 0000 2631 8000 2}
     *   components:  {@code EG 38 0019 0005 00000000263180002}
     * </pre>
     */
    EG(StructureData.builder()
         .withIbanLength(29)
         .withBbanPattern("4!n4!n17!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 29))
         .build(),
       MetaData.of(
           "Egypt", false, "EG380019000500000000263180002",
           YearMonth.of(2020, 1)),
       ContactData.of(
           "Central Bank of Egypt", "Operations Sector", "54 El Gomherya street",
           "Cairo", "Operations.development@cbe.org.eg", "+ 20 16777 (ext. 3109)")
    ),

    /**
     * <strong>Spain ({@code ES})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n4!n1!n1!n10!n}<br>
     * Examples:<pre>
     *   unformatted: {@code ES9121000418450200051332}
     *   formatted:   {@code ES91 2100 0418 4502 0005 1332}
     *   components:  {@code ES 91 2100 0418 45 0200051332}
     * </pre>
     */
    ES(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("4!n4!n1!n1!n10!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withNationalCheckDigit(IndexRange.of(12, 14)) // ES
         .withAccountNumber(IndexRange.of(14, 24))
         .build(),
       MetaData.of(
           "Spain", true, "ES9121000418450200051332",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Asociación Española de Banca Privada (AEB)", null, "C/ Velázquez, 64 – 66",
           "28001 Madrid", "asesoria.pagos@aebanca.es", "+ 34 917891311")
    ),

    /**
     * <strong>Finland ({@code FI})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n11!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FI2112345600000785}
     *   formatted:   {@code FI21 1234 5600 0007 85}
     *   components:  {@code FI 21 123456 0000078 5}
     * </pre>
     */
    FI(StructureData.builder()
         .withIbanLength(18)
         .withBbanPattern("3!n11!n")
         .withBankCode("6!n", IndexRange.of(4, 10))
         .withAccountNumber(IndexRange.of(10, 17))
         .withNationalCheckDigit(IndexRange.of(17, 18)) // FI
         .build(),
       MetaData.of(
           "Finland", true, "FI2112345600000785",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Federation of Finnish Financial Services", null, "PO Box 1009",
           "FIN-00101 Helsinki", "paymentsupport@finanssiala.fi", null)
    ),

    /**
     * <strong>Åland Islands ({@code AX})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n11!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FI2112345600000785}
     *   formatted:   {@code FI21 1234 5600 0007 85}
     *   components:  {@code FI 21 123456 0000078 5}
     * </pre>
     */
    AX("Åland Islands", FI),

    /**
     * <strong>Falkland Islands (Malvinas) ({@code FK})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!a12!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FK88SC123456789012}
     *   formatted:   {@code FK88 SC12 3456 7890 12}
     *   components:  {@code FK 88 SC 123456789012}
     * </pre>
     */
    FK(StructureData.builder()
         .withIbanLength(18)
         .withBbanPattern("2!a12!n")
         .withBankCode("2!a", IndexRange.of(4, 6))
         .withAccountNumber(IndexRange.of(6, 18))
         .build(),
       MetaData.of(
           "Falkland Islands", false, "FK88SC123456789012",
           YearMonth.of(2023, 7)),
       ContactData.of(
           "Falkland Islands Government", "The Treasury", "Thatcher Drive",
           "FIQQ 1ZZ Stanley", "treasury@sec.gov.fk", null)
    ),

    /**
     * <strong>Faroe Islands ({@code FO})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n9!n1!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FO6264600001631634}
     *   formatted:   {@code FO62 6460 0001 6316 34}
     *   components:  {@code FO 62 6460 000163163 4}
     * </pre>
     */
    FO(StructureData.builder()
         .withIbanLength(18)
         .withBbanPattern("4!n9!n1!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 17))
         .withNationalCheckDigit(IndexRange.of(17, 18)) // FO
         .build(),
       MetaData.of(
           "Faroe Islands", false, "FO6264600001631634",
           YearMonth.of(2017, 2)),
       DK.getContactData()),

    /**
     * <strong>France ({@code FR})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    FR(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!c2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27)) // FR
         .build(),
       MetaData.of(
           "France", true, "FR1420041010050500013M02606",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "CFONB", null, "18 rue la Fayette",
           "75009 Paris", "cfonb@cfonb.fr", "+ 33 148005042")
    ),

    /**
     * <strong>French Guiana ({@code GF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    GF("French Guiana", FR),

    /**
     * <strong>Guadeloupe ({@code GP})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    GP("Guadeloupe", FR),

    /**
     * <strong>Martinique ({@code MQ})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    MQ("Martinique", FR),

    /**
     * <strong>Réunion ({@code RE})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    RE("Réunion", FR),

    /**
     * <strong>French Polynesia ({@code PF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    PF("French Polynesia", FR),

    /**
     * <strong>French Southern Territories ({@code TF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    TF("French Southern Territories", FR),

    /**
     * <strong>Mayotte ({@code YT})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    YT("Mayotte", FR),

    /**
     * <strong>New Caledonia ({@code NC})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    NC("New Caledonia", FR),

    /**
     * <strong>Saint Barthélemy ({@code BL})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    BL("Saint Barthélemy", FR),

    /**
     * <strong>Saint Martin (French part) ({@code MF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    MF("Saint Martin (French part)", FR),

    /**
     * <strong>Saint Pierre and Miquelon ({@code PM})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    PM("Saint Pierre and Miquelon", FR),

    /**
     * <strong>Wallis and Futuna ({@code WF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code FR1420041010050500013M02606}
     *   formatted:   {@code FR14 2004 1010 0505 0001 3M02 606}
     *   components:  {@code FR 14 20041 01005 0500013M026 06}
     * </pre>
     */
    WF("Wallis and Futuna", FR),

    /**
     * <strong>Gabon ({@code GA})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n13!c}<br>
     * Examples:<pre>
     *   unformatted: {@code GA2140021010032001890020126}
     *   formatted:   {@code GA21 4002 1010 0320 0189 0020 126}
     *   components:  {@code GA 21 40021 01003 2001890020126}
     * </pre>
     */
    GA(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n13!c")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 27))
         .build(),
       MetaData.of(
           "Gabon", false, "GA2140021010032001890020126",
           null),
       null),

    /**
     * <strong>United Kingdom of Great Britain and Northern Ireland ({@code GB})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Examples:<pre>
     *   unformatted: {@code GB29NWBK60161331926819}
     *   formatted:   {@code GB29 NWBK 6016 1331 9268 19}
     *   components:  {@code GB 29 NWBK 601613 31926819}
     * </pre>
     */
    GB(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("4!a6!n8!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withBranchCode("6!n", IndexRange.of(8, 14))
         .withAccountNumber(IndexRange.of(14, 22))
         .build(),
       MetaData.of(
           "United Kingdom", true, "GB29NWBK60161331926819",
           YearMonth.of(2017, 5)),
       ContactData.of(
           "Payments UK Management Ltd", "International Standards and Services", "14 Finsbury Square",
           "London EC2A 1LQ", null, null)
    ),

    /**
     * <strong>Isle of Man ({@code IM})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Examples:<pre>
     *   unformatted: {@code GB29NWBK60161331926819}
     *   formatted:   {@code GB29 NWBK 6016 1331 9268 19}
     *   components:  {@code GB 29 NWBK 601613 31926819}
     * </pre>
     */
    IM("Isle of Man", GB),

    /**
     * <strong>Jersey ({@code JE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Examples:<pre>
     *   unformatted: {@code GB29NWBK60161331926819}
     *   formatted:   {@code GB29 NWBK 6016 1331 9268 19}
     *   components:  {@code GB 29 NWBK 601613 31926819}
     * </pre>
     */
    JE("Jersey", GB),

    /**
     * <strong>Guernsey ({@code GG})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Examples:<pre>
     *   unformatted: {@code GB29NWBK60161331926819}
     *   formatted:   {@code GB29 NWBK 6016 1331 9268 19}
     *   components:  {@code GB 29 NWBK 601613 31926819}
     * </pre>
     */
    GG("Guernsey", GB),

    /**
     * <strong>Georgia ({@code GE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!a16!n}<br>
     * Examples:<pre>
     *   unformatted: {@code GE29NB0000000101904917}
     *   formatted:   {@code GE29 NB00 0000 0101 9049 17}
     *   components:  {@code GE 29 NB 0000000101904917}
     * </pre>
     */
    GE(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("2!a16!n")
         .withBankCode("2!a", IndexRange.of(4, 6))
         .withAccountNumber(IndexRange.of(6, 22))
         .build(),
       MetaData.of(
           "Georgia", false, "GE29NB0000000101904917",
           YearMonth.of(2023, 4)),
       ContactData.of(
           "National Bank of Georgia", "Payment Systems", "1, Zviad Gamsakhurdia Embankment",
           "0114 Tbilisi", "RTGS@nbg.gov.ge", "+ 995 322406555")
    ),

    /**
     * <strong>Gibraltar ({@code GI})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a15!c}<br>
     * Examples:<pre>
     *   unformatted: {@code GI75NWBK000000007099453}
     *   formatted:   {@code GI75 NWBK 0000 0000 7099 453}
     *   components:  {@code GI 75 NWBK 000000007099453}
     * </pre>
     */
    GI(StructureData.builder()
         .withIbanLength(23)
         .withBbanPattern("4!a15!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 23))
         .build(),
       MetaData.of(
           "Gibraltar", true, "GI75NWBK000000007099453",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Financial Services Commission", null, "PO Box 940",
           "Suite 3, Ground Floor, Atlantic Suites", "info@fsc.gi", "+350 20040283")
    ),

    /**
     * <strong>Greenland ({@code GL})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n9!n1!n}<br>
     * Examples:<pre>
     *   unformatted: {@code GL8964710001000206}
     *   formatted:   {@code GL89 6471 0001 0002 06}
     *   components:  {@code GL 89 6471 0001000206}
     * </pre>
     */
    GL(StructureData.builder()
         .withIbanLength(18)
         .withBbanPattern("4!n9!n1!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 18))
         .build(),
       MetaData.of(
           "Greenland", false, "GL8964710001000206",
           YearMonth.of(2017, 2)),
       DK.getContactData()),

    /**
     * <strong>Greece ({@code GR})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n4!n16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code GR1601101250000000012300695}
     *   formatted:   {@code GR16 0110 1250 0000 0001 2300 695}
     *   components:  {@code GR 16 011 0125 0000000012300695}
     * </pre>
     */
    GR(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("3!n4!n16!c")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("4!n", IndexRange.of(7, 11))
         .withAccountNumber(IndexRange.of(11, 27))
         .build(),
       MetaData.of(
           "Greece", true, "GR1601101250000000012300695",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Hellenic Bank Association", "Payment Systems", "Amerikis 21A",
           "10672 Athens", "hba@hba.gr", "+ 30 2103386500")
    ),

    /**
     * <strong>Guatemala ({@code GT})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!c20!c}<br>
     * Examples:<pre>
     *   unformatted: {@code GT82TRAJ01020000001210029690}
     *   formatted:   {@code GT82 TRAJ 0102 0000 0012 1002 9690}
     *   components:  {@code GT 82 TRAJ 01020000001210029690}
     * </pre>
     */
    GT(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!c20!c")
         .withBankCode("4!c", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 28))
         .build(),
       MetaData.of(
           "Guatemala", false, "GT82TRAJ01020000001210029690",
           YearMonth.of(2016, 10)),
       ContactData.of(
           "Banco de Guatemala", "Accounting and Payment System", "7 Avenue 22-01 Zone 1",
           "01001 Guatemala", "contabilidad@banguat.gob.gt", "(502) 2429-6000 EXT. 4300 (502) 2253-5352")
    ),

    /**
     * <strong>Honduras ({@code HN})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!n}<br>
     * Examples:<pre>
     *   unformatted: {@code HN88CABF00000000000250005469}
     *   formatted:   {@code HN88 CABF 0000 0000 0002 5000 5469}
     *   components:  {@code HN 88 CABF 00000000000250005469}
     * </pre>
     */
    HN(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!a20!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 28))
         .build(),
       MetaData.of(
           "Honduras", false, "HN88CABF00000000000250005469",
           YearMonth.of(2024, 12)),
       ContactData.of(
           "Banco Central de Honduras", null, "Centro Cívico Gubernamental, Boulevard Fuerzas Armadas",
           "Tegucigalpa, MDC 3165", "carlos.avila@bch.hn", null)
    ),

    /**
     * <strong>Croatia ({@code HR})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 7!n10!n}<br>
     * Examples:<pre>
     *   unformatted: {@code HR1210010051863000160}
     *   formatted:   {@code HR12 1001 0051 8630 0016 0}
     *   components:  {@code HR 12 1001005 1863000160}
     * </pre>
     */
    HR(StructureData.builder()
         .withIbanLength(21)
         .withBbanPattern("7!n10!n")
         .withBankCode("7!n", IndexRange.of(4, 11))
         .withAccountNumber(IndexRange.of(11, 21))
         .build(),
       MetaData.of(
           "Croatia", true, "HR1210010051863000160",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Croatian National Bank", "Payment Operations Area", "Trg hrvatskih velikana 3",
           "Zagreb / 10002", "zpp@hnb.hr", "+ 385 14564992")
    ),

    /**
     * <strong>Hungary ({@code HU})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n4!n1!n15!n1!n}<br>
     * Examples:<pre>
     *   unformatted: {@code HU42117730161111101800000000}
     *   formatted:   {@code HU42 1177 3016 1111 1018 0000 0000}
     *   components:  {@code HU 42 117 7301 6111110180000000 0}
     * </pre>
     */
    HU(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("3!n4!n1!n15!n1!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("4!n", IndexRange.of(7, 11))
         .withAccountNumber(IndexRange.of(11, 27))
         .withNationalCheckDigit(IndexRange.of(27, 28)) // HU
         .build(),
       MetaData.of(
           "Hungary", true, "HU42117730161111101800000000",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Hungarian Banking Association", null, "József nádor tér 5-6",
           "H-1051 Budapest", "hba@hba.org.hu", "+ 36 13276030")
    ),

    /**
     * <strong>Ireland ({@code IE})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a6!n8!n}<br>
     * Examples:<pre>
     *   unformatted: {@code IE29AIBK93115212345678}
     *   formatted:   {@code IE29 AIBK 9311 5212 3456 78}
     *   components:  {@code IE 29 AIBK 931152 12345678}
     * </pre>
     */
    IE(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("4!a6!n8!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withBranchCode("6!n", IndexRange.of(8, 14))
         .withAccountNumber(IndexRange.of(14, 22))
         .build(),
       MetaData.of(
           "Ireland", true, "IE29AIBK93115212345678",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Banking & Payments Federation Ireland", null, "Floor 3 One Molesworth Street",
           "Dublin 2 D02 RF29", "info@bpfi.ie", null)
    ),

    /**
     * <strong>Israel ({@code IL})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n3!n13!n}<br>
     * Examples:<pre>
     *   unformatted: {@code IL620108000000099999999}
     *   formatted:   {@code IL62 0108 0000 0009 9999 999}
     *   components:  {@code IL 62 010 800 0000099999999}
     * </pre>
     */
    IL(StructureData.builder()
         .withIbanLength(23)
         .withBbanPattern("3!n3!n13!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("3!n", IndexRange.of(7, 10))
         .withAccountNumber(IndexRange.of(10, 23))
         .build(),
       MetaData.of(
           "Israel", false, "IL620108000000099999999",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Bank of Israel", "Payment and Settlement Systems", "Kaplan Street, Kyriat Ben Gurion",
           "91007 Jerusalem", "zahav@boi.org.il", "+ 972 26552020")
    ),

    /**
     * <strong>Iraq ({@code IQ})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a3!n12!n}<br>
     * Examples:<pre>
     *   unformatted: {@code IQ98NBIQ850123456789012}
     *   formatted:   {@code IQ98 NBIQ 8501 2345 6789 012}
     *   components:  {@code IQ 98 NBIQ 850 123456789012}
     * </pre>
     */
    IQ(StructureData.builder()
         .withIbanLength(23)
         .withBbanPattern("4!a3!n12!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withBranchCode("3!n", IndexRange.of(8, 11))
         .withAccountNumber(IndexRange.of(11, 23))
         .build(),
       MetaData.of(
           "Iraq", false, "IQ98NBIQ850123456789012",
           YearMonth.of(2016, 11)),
       ContactData.of(
           "Central Bank of Iraq", "SWIFT Department", "Rasheed Street",
           "Baghdad", "cbi@cbi.iq", "+ 964 47903737479")
    ),

    /**
     * <strong>Iran (Islamic Republic of) ({@code IR})</strong><p>
     * IBAN Length: 26<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n19!n}<br>
     * Examples:<pre>
     *   unformatted: {@code IR062960000000100324200001}
     *   formatted:   {@code IR06 2960 0000 0010 0324 2000 01}
     *   components:  {@code IR 06 296 0000000100324200001}
     * </pre>
     */
    IR(StructureData.builder()
         .withIbanLength(26)
         .withBbanPattern("3!n19!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 26))
         .build(),
       MetaData.of(
           "Islamic Republic of Iran", false, "IR062960000000100324200001",
           null),
       null),

    /**
     * <strong>Iceland ({@code IS})</strong><p>
     * IBAN Length: 26<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n2!n6!n10!n}<br>
     * Examples:<pre>
     *   unformatted: {@code IS140159260076545510730339}
     *   formatted:   {@code IS14 0159 2600 7654 5510 7303 39}
     *   components:  {@code IS 14 0159 26 0076545510730339}
     * </pre>
     */
    IS(StructureData.builder()
         .withIbanLength(26)
         .withBbanPattern("4!n2!n6!n10!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("2!n", IndexRange.of(8, 10))
         .withAccountNumber(IndexRange.of(10, 26))
         .build(),
       MetaData.of(
           "Iceland", true, "IS140159260076545510730339",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Icelandic Banks Data Centre", null, "Katrinartun 2",
           "105 Reykjavik", "hjalp@rb.is", "+ 354 5698877")
    ),

    /**
     * <strong>Italy ({@code IT})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 1!a5!n5!n12!c}<br>
     * Examples:<pre>
     *   unformatted: {@code IT60X0542811101000000123456}
     *   formatted:   {@code IT60 X054 2811 1010 0000 0123 456}
     *   components:  {@code IT 60 X 05428 11101 000000123456}
     * </pre>
     */
    IT(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("1!a5!n5!n12!c")
         .withNationalCheckDigit(IndexRange.of(4, 5)) // IT
         .withBankCode("5!n", IndexRange.of(5, 10))
         .withBranchCode("5!n", IndexRange.of(10, 15))
         .withAccountNumber(IndexRange.of(15, 27))
         .build(),
       MetaData.of(
           "Italy", true, "IT60X0542811101000000123456",
           YearMonth.of(2013, 3)),
       ContactData.of(
           "Associazione Bancaria Italiana", "Head of Payment Systems and Services", "Via delle Botteghe Oscure, 46",
           "00186 Rome – Italy", null, null)
    ),

    /**
     * <strong>Jordan ({@code JO})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a4!n18!c}<br>
     * Examples:<pre>
     *   unformatted: {@code JO94CBJO0010000000000131000302}
     *   formatted:   {@code JO94 CBJO 0010 0000 0000 0131 0003 02}
     *   components:  {@code JO 94 CBJO 0010 000000000131000302}
     * </pre>
     */
    JO(StructureData.builder()
         .withIbanLength(30)
         .withBbanPattern("4!a4!n18!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 30))
         .build(),
       MetaData.of(
           "Jordan", false, "JO94CBJO0010000000000131000302",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Central Bank of Jordan", "Financial", "King Hussein Street",
           "11118 Amman – Capital", "finance@cbj.gov.jo", null)
    ),

    /**
     * <strong>Kuwait ({@code KW})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a22!c}<br>
     * Examples:<pre>
     *   unformatted: {@code KW81CBKU0000000000001234560101}
     *   formatted:   {@code KW81 CBKU 0000 0000 0000 1234 5601 01}
     *   components:  {@code KW 81 CBKU 0000000000001234560101}
     * </pre>
     */
    KW(StructureData.builder()
         .withIbanLength(30)
         .withBbanPattern("4!a22!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 30))
         .build(),
       MetaData.of(
           "Kuwait", false, "KW81CBKU0000000000001234560101",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Central Bank of Kuwait", "Information Technology and Banking Operations Sector", "P.O. Box 526 Safat",
           "13006 Safat", "bito@cbk.gov.kw", null)
    ),

    /**
     * <strong>Kazakhstan ({@code KZ})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n13!c}<br>
     * Examples:<pre>
     *   unformatted: {@code KZ86125KZT5004100100}
     *   formatted:   {@code KZ86 125K ZT50 0410 0100}
     *   components:  {@code KZ 86 125 KZT5004100100}
     * </pre>
     */
    KZ(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("3!n13!c")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 20))
         .build(),
       MetaData.of(
           "Kazakhstan", false, "KZ86125KZT5004100100",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "National Bank of the Republic of Kazakhstan", "Payment Systems", "21, Koktem-3",
           "050040 Almaty", null, null)
    ),

    /**
     * <strong>Lebanon ({@code LB})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n20!c}<br>
     * Examples:<pre>
     *   unformatted: {@code LB62099900000001001901229114}
     *   formatted:   {@code LB62 0999 0000 0001 0019 0122 9114}
     *   components:  {@code LB 62 0999 00000001001901229114}
     * </pre>
     */
    LB(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!n20!c")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 28))
         .build(),
       MetaData.of(
           "Lebanon", false, "LB62099900000001001901229114",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Banque du Liban", "Payment Systems", "Masraf Lubnan street",
           "11-5544 Beirut", "IBAN@bdl.gov.lb", "+961-1-343317")
    ),

    /**
     * <strong>Saint Lucia ({@code LC})</strong><p>
     * IBAN Length: 32<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a24!c}<br>
     * Examples:<pre>
     *   unformatted: {@code LC55HEMM000100010012001200023015}
     *   formatted:   {@code LC55 HEMM 0001 0001 0012 0012 0002 3015}
     *   components:  {@code LC 55 HEMM 000100010012001200023015}
     * </pre>
     */
    LC(StructureData.builder()
         .withIbanLength(32)
         .withBbanPattern("4!a24!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 32))
         .build(),
       MetaData.of(
           "Saint Lucia", false, "LC55HEMM000100010012001200023015",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Saint Lucia Bureau of Standards", null, "Bisee Industrial Estate",
           "Castries, PO Box CP 5412", "slbs@candw.lc; info@slbs.org", null)
    ),

    /**
     * <strong>Liechtenstein ({@code LI})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n12!c}<br>
     * Examples:<pre>
     *   unformatted: {@code LI21088100002324013AA}
     *   formatted:   {@code LI21 0881 0000 2324 013A A}
     *   components:  {@code LI 21 08810 0002324013AA}
     * </pre>
     */
    LI(StructureData.builder()
         .withIbanLength(21)
         .withBbanPattern("5!n12!c")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withAccountNumber(IndexRange.of(9, 21))
         .build(),
       MetaData.of(
           "Liechtenstein", true, "LI21088100002324013AA",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Liechtenstein Bankers Association", null, "P.O. Box 254",
           "9490 Vaduz", null, null)
    ),

    /**
     * <strong>Lithuania ({@code LT})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n11!n}<br>
     * Examples:<pre>
     *   unformatted: {@code LT121000011101001000}
     *   formatted:   {@code LT12 1000 0111 0100 1000}
     *   components:  {@code LT 12 10000 11101001000}
     * </pre>
     */
    LT(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("5!n11!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withAccountNumber(IndexRange.of(9, 20))
         .build(),
       MetaData.of(
           "Lithuania", true, "LT121000011101001000",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Bank of Lithuania", "Operations and Payments Department", "Gedimino pr. 6",
           "Vilnius, LT-01103", "tarpbank@lb.lt", "+ 370 52680604")
    ),

    /**
     * <strong>Luxembourg ({@code LU})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n13!c}<br>
     * Examples:<pre>
     *   unformatted: {@code LU280019400644750000}
     *   formatted:   {@code LU28 0019 4006 4475 0000}
     *   components:  {@code LU 28 001 9400644750000}
     * </pre>
     */
    LU(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("3!n13!c")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 20))
         .build(),
       MetaData.of(
           "Luxembourg", true, "LU280019400644750000",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "ABBL - Association des Banques et Banquiers Luxembourg", null, "Boîte Postale 13",
           "L-2010 Luxembourg", null, null)
    ),

    /**
     * <strong>Latvia ({@code LV})</strong><p>
     * IBAN Length: 21<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a13!c}<br>
     * Examples:<pre>
     *   unformatted: {@code LV80BANK0000435195001}
     *   formatted:   {@code LV80 BANK 0000 4351 9500 1}
     *   components:  {@code LV 80 BANK 0000435195001}
     * </pre>
     */
    LV(StructureData.builder()
         .withIbanLength(21)
         .withBbanPattern("4!a13!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 21))
         .build(),
       MetaData.of(
           "Latvia", true, "LV80BANK0000435195001",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Bank of Latvia", "Payment Systems", "K. Valdemāra 2A",
           "Riga, LV-1050", null, null)
    ),

    /**
     * <strong>Libya ({@code LY})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n3!n15!n}<br>
     * Examples:<pre>
     *   unformatted: {@code LY83002048000020100120361}
     *   formatted:   {@code LY83 0020 4800 0020 1001 2036 1}
     *   components:  {@code LY 83 002 048 000020100120361}
     * </pre>
     */
    LY(StructureData.builder()
         .withIbanLength(25)
         .withBbanPattern("3!n3!n15!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("3!n", IndexRange.of(7, 10))
         .withAccountNumber(IndexRange.of(10, 25))
         .build(),
       MetaData.of(
           "Libya", false, "LY83002048000020100120361",
           YearMonth.of(2020, 9)),
       ContactData.of(
           "Central Bank of Libya", "Payment and Settlement Department", "Alfatah Road",
           "1103 Tripoli", "info@cbl.gov.ly", "+218 912137654")
    ),

    /**
     * <strong>Morocco ({@code MA})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n5!n16!n}<br>
     * Examples:<pre>
     *   unformatted: {@code MA64360815000001793222001617}
     *   formatted:   {@code MA64 3608 1500 0001 7932 2200 1617}
     *   components:  {@code MA 64 360 81500 0001793222001617}
     * </pre>
     */
    MA(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("3!n5!n16!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("5!n", IndexRange.of(7, 12))
         .withAccountNumber(IndexRange.of(12, 28))
         .build(),
       MetaData.of(
           "Morocco", false, "MA64360815000001793222001617",
           null),
       null),

    /**
     * <strong>Monaco ({@code MC})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 5!n5!n11!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code MC5811222000010123456789030}
     *   formatted:   {@code MC58 1122 2000 0101 2345 6789 030}
     *   components:  {@code MC 58 11222 00001 01234567890 30}
     * </pre>
     */
    MC(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!c2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27)) // MC
         .build(),
       MetaData.of(
           "Monaco", true, "MC5811222000010123456789030",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Principauté de Monaco", null, "7, rue du Gabian",
           "MC98000", "amaf@amaf.mc", null)
    ),

    /**
     * <strong>Moldova, Republic of ({@code MD})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!c18!c}<br>
     * Examples:<pre>
     *   unformatted: {@code MD24AG000225100013104168}
     *   formatted:   {@code MD24 AG00 0225 1000 1310 4168}
     *   components:  {@code MD 24 AG 000225100013104168}
     * </pre>
     */
    MD(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("2!c18!c")
         .withBankCode("2!c", IndexRange.of(4, 6))
         .withAccountNumber(IndexRange.of(6, 24))
         .build(),
       MetaData.of(
           "Moldova", false, "MD24AG000225100013104168",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "National Bank of Moldova", "Payments System", "1 Grigore Vieru Avenue",
           "MD-2005 Chisinau", null, null)
    ),

    /**
     * <strong>Montenegro ({@code ME})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n13!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code ME25505000012345678951}
     *   formatted:   {@code ME25 5050 0001 2345 6789 51}
     *   components:  {@code ME 25 505 0000123456789 51}
     * </pre>
     */
    ME(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("3!n13!n2!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 20))
         .withNationalCheckDigit(IndexRange.of(20, 22)) // ME
         .build(),
       MetaData.of(
           "Montenegro", false, "ME25505000012345678951",
           YearMonth.of(2010, 5)),
       ContactData.of(
           "Association of Montenegrin Banks", null, "Novaka Miloseva bb",
           "81000 Podgorica", "udruzenjebanaka@t-com.me", "+ 381 81232028")
    ),

    /**
     * <strong>North Macedonia ({@code MK})</strong><p>
     * IBAN Length: 19<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n10!c2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code MK07250120000058984}
     *   formatted:   {@code MK07 2501 2000 0058 984}
     *   components:  {@code MK 07 250 1200000589 84}
     * </pre>
     */
    MK(StructureData.builder()
         .withIbanLength(19)
         .withBbanPattern("3!n10!c2!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 17))
         .withNationalCheckDigit(IndexRange.of(17, 19)) // MK
         .build(),
       MetaData.of(
           "North Macedonia", false, "MK07250120000058984",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "National Bank of the Republic of Macedonia", null, "K.J.Pitu 1",
           "1000 Skopje", null, null)
    ),

    /**
     * <strong>Mongolia ({@code MN})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n12!n}<br>
     * Examples:<pre>
     *   unformatted: {@code MN121234123456789123}
     *   formatted:   {@code MN12 1234 1234 5678 9123}
     *   components:  {@code MN 12 1234 123456789123}
     * </pre>
     */
    MN(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("4!n12!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 20))
         .build(),
       MetaData.of(
           "Mongolia", false, "MN121234123456789123",
           YearMonth.of(2023, 4)),
       ContactData.of(
           "Bank of Mongolia (The Central Bank)", null, "Baga toiruu-3",
           "15160 Ulaanbaatar", null, "976-11-327510")
    ),

    /**
     * <strong>Mauritania ({@code MR})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code MR1300020001010000123456753}
     *   formatted:   {@code MR13 0002 0001 0100 0012 3456 753}
     *   components:  {@code MR 13 00020 00101 00001234567 53}
     * </pre>
     */
    MR(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27)) // MR
         .build(),
       MetaData.of(
           "Mauritania", false, "MR1300020001010000123456753",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Banque Centrale de Mauritanie", null, "Avenue de l'Indépendance",
           "BP 623 Nouakchott", "info@bcm.mr", "+ 222 45255206")
    ),

    /**
     * <strong>Malta ({@code MT})</strong><p>
     * IBAN Length: 31<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a5!n18!c}<br>
     * Examples:<pre>
     *   unformatted: {@code MT84MALT011000012345MTLCAST001S}
     *   formatted:   {@code MT84 MALT 0110 0001 2345 MTLC AST0 01S}
     *   components:  {@code MT 84 MALT 01100 0012345MTLCAST001S}
     * </pre>
     */
    MT(StructureData.builder()
         .withIbanLength(31)
         .withBbanPattern("4!a5!n18!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withBranchCode("5!n", IndexRange.of(8, 13))
         .withAccountNumber(IndexRange.of(13, 31))
         .build(),
       MetaData.of(
           "Malta", true, "MT84MALT011000012345MTLCAST001S",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Malta Bankers' Association", "The Secretary General", "48/2 Birkirkara Road",
           "Attard ATD1210", "info@maltabankers.org", "+ 356 21412210 / + 356 21410572")
    ),

    /**
     * <strong>Mauritius ({@code MU})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a2!n2!n12!n3!n3!a}<br>
     * Examples:<pre>
     *   unformatted: {@code MU17BOMM0101101030300200000MUR}
     *   formatted:   {@code MU17 BOMM 0101 1010 3030 0200 000M UR}
     *   components:  {@code MU 17 BOMM01 01 101030300200000MUR}
     * </pre>
     */
    MU(StructureData.builder()
         .withIbanLength(30)
         .withBbanPattern("4!a2!n2!n12!n3!n3!a")
         .withBankCode("4!a2!n", IndexRange.of(4, 10))
         .withBranchCode("2!n", IndexRange.of(10, 12))
         .withAccountNumber(IndexRange.of(12, 30))
         .build(),
       MetaData.of(
           "Mauritius", false, "MU17BOMM0101101030300200000MUR",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "The Central Bank of Mauritius", null, "Sir William Newton Street",
           "Port Louis", null, null)
    ),

    /**
     * <strong>Mozambique ({@code MZ})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code MZ59000800005138555713187}
     *   formatted:   {@code MZ59 0008 0000 5138 5557 1318 7}
     *   components:  {@code MZ 59 0008 0000 51385557131 87}
     * </pre>
     */
    MZ(StructureData.builder()
         .withIbanLength(25)
         .withBbanPattern("4!n4!n11!n2!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 23))
         .withNationalCheckDigit(IndexRange.of(23, 25)) // MZ
         .build(),
       MetaData.of(
           "Mozambique", false, "MZ59000800005138555713187",
           null),
       null),

    /**
     * <strong>Nicaragua ({@code NI})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!n}<br>
     * Examples:<pre>
     *   unformatted: {@code NI45BAPR00000013000003558124}
     *   formatted:   {@code NI45 BAPR 0000 0013 0000 0355 8124}
     *   components:  {@code NI 45 BAPR 00000013000003558124}
     * </pre>
     */
    NI(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!a20!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 28))
         .build(),
       MetaData.of(
           "Nicaragua", false, "NI45BAPR00000013000003558124",
           YearMonth.of(2024, 12)),
       ContactData.of(
           "Banco Central de Nicaragua", null, "Paso a Desnivel Nejapa, 100 metros al este, Pista Juan Pablo II",
           "2252 - 2253 Managua", null, null)
    ),

    /**
     * <strong>Netherlands ({@code NL})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a10!n}<br>
     * Examples:<pre>
     *   unformatted: {@code NL91ABNA0417164300}
     *   formatted:   {@code NL91 ABNA 0417 1643 00}
     *   components:  {@code NL 91 ABNA 0417164300}
     * </pre>
     */
    NL(StructureData.builder()
         .withIbanLength(18)
         .withBbanPattern("4!a10!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 18))
         .build(),
       MetaData.of(
           "Netherlands", true, "NL91ABNA0417164300",
           YearMonth.of(2020, 9)),
       ContactData.of(
           "Betaalvereniging Nederland", null, "P.O Box 83073",
           "1080 AB Amsterdam", "sepa@betaalvereniging.nl", "+ 31 203051900")
    ),

    /**
     * <strong>Norway ({@code NO})</strong><p>
     * IBAN Length: 15<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n6!n1!n}<br>
     * Examples:<pre>
     *   unformatted: {@code NO9386011117947}
     *   formatted:   {@code NO93 8601 1117 947}
     *   components:  {@code NO 93 8601 111794 7}
     * </pre>
     */
    NO(StructureData.builder()
         .withIbanLength(15)
         .withBbanPattern("4!n6!n1!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 14))
         .withNationalCheckDigit(IndexRange.of(14, 15)) // NO
         .build(),
       MetaData.of(
           "Norway", true, "NO9386011117947",
           YearMonth.of(2009, 8)),
       ContactData.of(
           "DnB NOR Bank", null, "p.o.box 7100",
           "5020 Bergen", null, null)
    ),

    /**
     * <strong>Oman ({@code OM})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code OM810180000001299123456}
     *   formatted:   {@code OM81 0180 0000 0129 9123 456}
     *   components:  {@code OM 81 018 0000001299123456}
     * </pre>
     */
    OM(StructureData.builder()
         .withIbanLength(23)
         .withBbanPattern("3!n16!c")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 23))
         .build(),
       MetaData.of(
           "Oman", false, "OM810180000001299123456",
           YearMonth.of(2024, 3)),
       ContactData.of(
           "Central Bank of Oman", null, "Head Office, Al Markazi, Building Number:44 P.O. Box 1161",
           "112 Ruwi - Commercial Business District - Muscat", "cso@cbo.gov.om", null)
    ),

    /**
     * <strong>Pakistan ({@code PK})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code PK36SCBL0000001123456702}
     *   formatted:   {@code PK36 SCBL 0000 0011 2345 6702}
     *   components:  {@code PK 36 SCBL 0000001123456702}
     * </pre>
     */
    PK(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("4!a16!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 24))
         .build(),
       MetaData.of(
           "Pakistan", false, "PK36SCBL0000001123456702",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "State Bank of Pakistan", "Payment Systems", "4th Floor, Main Building, I.I. Chundrigar Road.",
           "74000 Karachi – Sindh – Pakistan", null, null)
    ),

    /**
     * <strong>Poland ({@code PL})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 8!n16!n}<br>
     * Examples:<pre>
     *   unformatted: {@code PL61109010140000071219812874}
     *   formatted:   {@code PL61 1090 1014 0000 0712 1981 2874}
     *   components:  {@code PL 61 109 0101 4 0000071219812874}
     * </pre>
     */
    PL(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("8!n16!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("4!n", IndexRange.of(7, 11))
         .withNationalCheckDigit(IndexRange.of(11, 12)) // PL
         .withAccountNumber(IndexRange.of(12, 28))
         .build(),
       MetaData.of(
           "Poland", true, "PL61109010140000071219812874",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Narodowy Bank Polski", "Payment Systems Deparment", "Świętokrzyska 11/21",
           "00 – 919 Warsaw", "sekretariat.dsp@nbp.pl", "48 22 185 27 25")
    ),

    /**
     * <strong>Palestine, State of ({@code PS})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a21!c}<br>
     * Examples:<pre>
     *   unformatted: {@code PS92PALS000000000400123456702}
     *   formatted:   {@code PS92 PALS 0000 0000 0400 1234 5670 2}
     *   components:  {@code PS 92 PALS 000000000400123456702}
     * </pre>
     */
    PS(StructureData.builder()
         .withIbanLength(29)
         .withBbanPattern("4!a21!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 29))
         .build(),
       MetaData.of(
           "Palestine", false, "PS92PALS000000000400123456702",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Palestine Monetary Authority", "Payment Systems", "Al-Ramouni Nablus Street",
           "452 AL-BIREH", "psdsupport@pma.ps", null)
    ),

    /**
     * <strong>Portugal ({@code PT})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code PT50000201231234567890154}
     *   formatted:   {@code PT50 0002 0123 1234 5678 9015 4}
     *   components:  {@code PT 50 0002 0123 12345678901 54}
     * </pre>
     */
    PT(StructureData.builder()
         .withIbanLength(25)
         .withBbanPattern("4!n4!n11!n2!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 23))
         .withNationalCheckDigit(IndexRange.of(23, 25)) // PT
         .build(),
       MetaData.of(
           "Portugal", true, "PT50000201231234567890154",
           YearMonth.of(2025, 12)),
       ContactData.of(
           "Banco de Portugal", "Payment Systems Department", "Rua do Comércio 148",
           "1100-148 Lisboa", "dpg@bportugal.pt", "+ 351 217813000")
    ),

    /**
     * <strong>Qatar ({@code QA})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a21!c}<br>
     * Examples:<pre>
     *   unformatted: {@code QA58DOHB00001234567890ABCDEFG}
     *   formatted:   {@code QA58 DOHB 0000 1234 5678 90AB CDEF G}
     *   components:  {@code QA 58 DOHB 00001234567890ABCDEFG}
     * </pre>
     */
    QA(StructureData.builder()
         .withIbanLength(29)
         .withBbanPattern("4!a21!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 29))
         .build(),
       MetaData.of(
           "Qatar", false, "QA58DOHB00001234567890ABCDEFG",
           YearMonth.of(2014, 1)),
       ContactData.of(
           "Qatar Central Bank", "Banking Payments and Settlement System", "Abdulla Bin Jassim Street",
           "1234 Doha", null, null)
    ),

    /**
     * <strong>Romania ({@code RO})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!a16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code RO49AAAA1B31007593840000}
     *   formatted:   {@code RO49 AAAA 1B31 0075 9384 0000}
     *   components:  {@code RO 49 AAAA 1B31007593840000}
     * </pre>
     */
    RO(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("4!a16!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 24))
         .build(),
       MetaData.of(
           "Romania", true, "RO49AAAA1B31007593840000",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "National Bank of Romania", null, "Lipscani St.,25th",
           "Sector 3, Bucharest 030031", null, null)
    ),

    /**
     * <strong>Serbia ({@code RS})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n13!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code RS35260005601001611379}
     *   formatted:   {@code RS35 2600 0560 1001 6113 79}
     *   components:  {@code RS 35 260 0056010016113 79}
     * </pre>
     */
    RS(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("3!n13!n2!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 20))
         .withNationalCheckDigit(IndexRange.of(20, 22)) // RS
         .build(),
       MetaData.of(
           "Serbia", false, "RS35260005601001611379",
           YearMonth.of(2017, 3)),
       ContactData.of(
           "National bank of Serbia", null, "Nemanjina 17",
           "11000 Belgrade", "platni.sistem@nbs.rs", null)
    ),

    /**
     * <strong>Russian Federation ({@code RU})</strong><p>
     * IBAN Length: 33<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 9!n5!n15!c}<br>
     * Examples:<pre>
     *   unformatted: {@code RU0304452522540817810538091310419}
     *   formatted:   {@code RU03 0445 2522 5408 1781 0538 0913 1041 9}
     *   components:  {@code RU 03 044525225 40817 810538091310419}
     * </pre>
     */
    RU(StructureData.builder()
         .withIbanLength(33)
         .withBbanPattern("9!n5!n15!c")
         .withBankCode("9!n", IndexRange.of(4, 13))
         .withBranchCode("5!n", IndexRange.of(13, 18))
         .withAccountNumber(IndexRange.of(18, 33))
         .build(),
       MetaData.of(
           "Russia", false, "RU0304452522540817810538091310419",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "The Central Bank of the Russian Federation", null, "Neglinnaya Street, 12",
           "Moscow", "svc_dnps_ornps@cbr.ru", null)
    ),

    /**
     * <strong>Saudi Arabia ({@code SA})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!n18!c}<br>
     * Examples:<pre>
     *   unformatted: {@code SA0380000000608010167519}
     *   formatted:   {@code SA03 8000 0000 6080 1016 7519}
     *   components:  {@code SA 03 80 000000608010167519}
     * </pre>
     */
    SA(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("2!n18!c")
         .withBankCode("2!n", IndexRange.of(4, 6))
         .withAccountNumber(IndexRange.of(6, 24))
         .build(),
       MetaData.of(
           "Saudi Arabia", false, "SA0380000000608010167519",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "SAMA, Head Office", "General Department of Payment Systems", "P.O. BOX 2992",
           "Riyadh 11169", "gdps@sama.gov.sa", "+ 966 114662015")
    ),

    /**
     * <strong>Seychelles ({@code SC})</strong><p>
     * IBAN Length: 31<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a2!n2!n16!n3!a}<br>
     * Examples:<pre>
     *   unformatted: {@code SC18SSCB11010000000000001497USD}
     *   formatted:   {@code SC18 SSCB 1101 0000 0000 0000 1497 USD}
     *   components:  {@code SC 18 SSCB11 01 0000000000001497USD}
     * </pre>
     */
    SC(StructureData.builder()
         .withIbanLength(31)
         .withBbanPattern("4!a2!n2!n16!n3!a")
         .withBankCode("4!a2!n", IndexRange.of(4, 10))
         .withBranchCode("2!n", IndexRange.of(10, 12))
         .withAccountNumber(IndexRange.of(12, 28))
         .build(),
       MetaData.of(
           "Seychelles", false, "SC18SSCB11010000000000001497USD",
           YearMonth.of(2019, 10)),
       ContactData.of(
           "Central Bank of Seychelles", null, "Independence Avenue",
           "Victoria - Mahe", "Psd@cbs.sc; BankingServices@cbs.sc", null)
    ),

    /**
     * <strong>Sudan ({@code SD})</strong><p>
     * IBAN Length: 18<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!n12!n}<br>
     * Examples:<pre>
     *   unformatted: {@code SD2129010501234001}
     *   formatted:   {@code SD21 2901 0501 2340 01}
     *   components:  {@code SD 21 29 010501234001}
     * </pre>
     */
    SD(StructureData.builder()
         .withIbanLength(18)
         .withBbanPattern("2!n12!n")
         .withBankCode("2!n", IndexRange.of(4, 6))
         .withAccountNumber(IndexRange.of(6, 18))
         .build(),
       MetaData.of(
           "Sudan", false, "SD2129010501234001",
           YearMonth.of(2021, 10)),
       ContactData.of(
           "Central Bank of Sudan (CBOS)", null, "HQ Building of Central Bank of SudanAljammah St.",
           "Khartoum 11111/313", null, null)
    ),

    /**
     * <strong>Sweden ({@code SE})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n16!n1!n}<br>
     * Examples:<pre>
     *   unformatted: {@code SE4550000000058398257466}
     *   formatted:   {@code SE45 5000 0000 0583 9825 7466}
     *   components:  {@code SE 45 500 00000058398257466}
     * </pre>
     */
    SE(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("3!n16!n1!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 24))
         .build(),
       MetaData.of(
           "Sweden", true, "SE4550000000058398257466",
           YearMonth.of(2009, 8)),
       ContactData.of(
           "Swedish Bankers' Association", null, null,
           "SE - 103 94 Stockholm", null, null)
    ),

    /**
     * <strong>Slovenia ({@code SI})</strong><p>
     * IBAN Length: 19<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 2!n3!n8!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code SI56263300012039086}
     *   formatted:   {@code SI56 2633 0001 2039 086}
     *   components:  {@code SI 56 26 330 00120390 86}
     * </pre>
     */
    SI(StructureData.builder()
         .withIbanLength(19)
         .withBbanPattern("2!n3!n8!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 6))
         .withBranchCode("3!n", IndexRange.of(6, 9))
         .withAccountNumber(IndexRange.of(9, 17))
         .withNationalCheckDigit(IndexRange.of(17, 19)) // SI
         .build(),
       MetaData.of(
           "Slovenia", true, "SI56263300012039086",
           YearMonth.of(2016, 10)),
       ContactData.of(
           "Bank of Slovenia", "Payment and Settlement Systems", "Slovenska 35",
           "SI-1505 Ljubljana", "Pomoc.PS@bsi.si", "+389 1 4719 568")
    ),

    /**
     * <strong>Slovakia ({@code SK})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 4!n6!n10!n}<br>
     * Examples:<pre>
     *   unformatted: {@code SK3112000000198742637541}
     *   formatted:   {@code SK31 1200 0000 1987 4263 7541}
     *   components:  {@code SK 31 1200 0000198742637541}
     * </pre>
     */
    SK(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("4!n6!n10!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 24))
         .build(),
       MetaData.of(
           "Slovakia", true, "SK3112000000198742637541",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "National Bank of Slovakia", null, "Imricha Karvaša 1",
           "813 25 Bratislava 1", "info@nbs.sk", null)
    ),

    /**
     * <strong>San Marino ({@code SM})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 1!a5!n5!n12!c}<br>
     * Examples:<pre>
     *   unformatted: {@code SM86U0322509800000000270100}
     *   formatted:   {@code SM86 U032 2509 8000 0000 0270 100}
     *   components:  {@code SM 86 U 03225 09800 000000270100}
     * </pre>
     */
    SM(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("1!a5!n5!n12!c")
         .withNationalCheckDigit(IndexRange.of(4, 5)) // SM
         .withBankCode("5!n", IndexRange.of(5, 10))
         .withBranchCode("5!n", IndexRange.of(10, 15))
         .withAccountNumber(IndexRange.of(15, 27))
         .build(),
       MetaData.of(
           "San Marino", true, "SM86U0322509800000000270100",
           YearMonth.of(2016, 8)),
       ContactData.of(
           "Banca Centrale della Repubblica di San Marino", "Payments System", "Via del Voltone, 120",
           "San Marino 47890", "sistemi.pagamento@bcsm.sm", "+ 378 882325")
    ),

    /**
     * <strong>Somalia ({@code SO})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n3!n12!n}<br>
     * Examples:<pre>
     *   unformatted: {@code SO211000001001000100141}
     *   formatted:   {@code SO21 1000 0010 0100 0100 141}
     *   components:  {@code SO 21 1000 001 001000100141}
     * </pre>
     */
    SO(StructureData.builder()
         .withIbanLength(23)
         .withBbanPattern("4!n3!n12!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("3!n", IndexRange.of(8, 11))
         .withAccountNumber(IndexRange.of(11, 23))
         .build(),
       MetaData.of(
           "Somalia", false, "SO211000001001000100141",
           YearMonth.of(2025, 2)),
       ContactData.of(
           "Central Bank of Somalia", null, "Corso Somalia, 55 P.O Box 11",
           "Mogadishu", "info@centralbank.gov.so", null)
    ),

    /**
     * <strong>São Tomé and Príncipe ({@code ST})</strong><p>
     * IBAN Length: 25<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n4!n11!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code ST23000100010051845310146}
     *   formatted:   {@code ST23 0001 0001 0051 8453 1014 6}
     *   components:  {@code ST 23 0001 0001 0051845310146}
     * </pre>
     */
    ST(StructureData.builder()
         .withIbanLength(25)
         .withBbanPattern("4!n4!n11!n2!n")
         .withBankCode("4!n", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 25))
         .build(),
       MetaData.of(
           "Sao Tome and Principe", false, "ST23000100010051845310146",
           YearMonth.of(2020, 5)),
       ContactData.of(
           "Banco Central de Sao Tome e Principe", "DSP", "Avenida Marginal 12 de Julho",
           "CP 13 Sao Tome", "dsp@bcstp.st", "+239 2243700")
    ),

    /**
     * <strong>El Salvador ({@code SV})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a20!n}<br>
     * Examples:<pre>
     *   unformatted: {@code SV62CENR00000000000000700025}
     *   formatted:   {@code SV62 CENR 0000 0000 0000 0070 0025}
     *   components:  {@code SV 62 CENR 00000000000000700025}
     * </pre>
     */
    SV(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("4!a20!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 28))
         .build(),
       MetaData.of(
           "El Salvador", false, "SV62CENR00000000000000700025",
           YearMonth.of(2021, 3)),
       ContactData.of(
           "Banco Central de Reserva de El Salvador", "Departamento de Pagos y Valores", "1a Calle Poniente y 7Av. Nte. N 418",
           "San Salvador", "maria.delgado@bcr.gov.sv", "503 2281 8831")
    ),

    /**
     * <strong>Timor-Leste ({@code TL})</strong><p>
     * IBAN Length: 23<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n14!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code TL380080012345678910157}
     *   formatted:   {@code TL38 0080 0123 4567 8910 157}
     *   components:  {@code TL 38 008 00123456789101 57}
     * </pre>
     */
    TL(StructureData.builder()
         .withIbanLength(23)
         .withBbanPattern("3!n14!n2!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 21))
         .withNationalCheckDigit(IndexRange.of(21, 23)) // TL
         .build(),
       MetaData.of(
           "Timor-Leste", false, "TL380080012345678910157",
           YearMonth.of(2014, 11)),
       ContactData.of(
           "Banco Central de Timor-Leste", null, "Avenida Bispo Medeiros",
           "Dili", null, null)
    ),

    /**
     * <strong>Tunisia ({@code TN})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 2!n3!n13!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code TN5910006035183598478831}
     *   formatted:   {@code TN59 1000 6035 1835 9847 8831}
     *   components:  {@code TN 59 10 006 0351835984788 31}
     * </pre>
     */
    TN(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("2!n3!n13!n2!n")
         .withBankCode("2!n", IndexRange.of(4, 6))
         .withBranchCode("3!n", IndexRange.of(6, 9))
         .withAccountNumber(IndexRange.of(9, 22))
         .withNationalCheckDigit(IndexRange.of(22, 24)) // TN
         .build(),
       MetaData.of(
           "Tunisia", false, "TN5910006035183598478831",
           YearMonth.of(2016, 5)),
       ContactData.of(
           "Tunisia's Professional Association for Banks & Financial Institutions", null, null,
           null, "info@apbt.org.tn", "+ 216 71904423")
    ),

    /**
     * <strong>Türkiye ({@code TR})</strong><p>
     * IBAN Length: 26<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n1!n16!c}<br>
     * Examples:<pre>
     *   unformatted: {@code TR330006100519786457841326}
     *   formatted:   {@code TR33 0006 1005 1978 6457 8413 26}
     *   components:  {@code TR 33 00061 0 0519786457841326}
     * </pre>
     */
    TR(StructureData.builder()
         .withIbanLength(26)
         .withBbanPattern("5!n1!n16!c")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withNationalCheckDigit(IndexRange.of(9, 10)) // TR
         .withAccountNumber(IndexRange.of(10, 26))
         .build(),
       MetaData.of(
           "Turkey", false, "TR330006100519786457841326",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Central Bank of the Republic of Turkey", "Payment Systems", "Anafartalar Mahallesi İstiklal Cad. No:10 Ulus Altındağ",
           "Ankara / 06050", "paymentsystems@tcmb.gov.tr", "+ 90 3125077901 / 02")
    ),

    /**
     * <strong>Ukraine ({@code UA})</strong><p>
     * IBAN Length: 29<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 6!n19!c}<br>
     * Examples:<pre>
     *   unformatted: {@code UA213223130000026007233566001}
     *   formatted:   {@code UA21 3223 1300 0002 6007 2335 6600 1}
     *   components:  {@code UA 21 322313 0000026007233566001}
     * </pre>
     */
    UA(StructureData.builder()
         .withIbanLength(29)
         .withBbanPattern("6!n19!c")
         .withBankCode("6!n", IndexRange.of(4, 10))
         .withAccountNumber(IndexRange.of(10, 29))
         .build(),
       MetaData.of(
           "Ukraine", false, "UA213223130000026007233566001",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Association UkrSWIFT", "Executive Board", "21A, Observatoma Str.",
           "04053 Kiev", "ukrswift@ukrswift.org", null)
    ),

    /**
     * <strong>Holy See ({@code VA})</strong><p>
     * IBAN Length: 22<br>
     * SEPA: Yes<br>
     * BBAN Structure: {@code 3!n15!n}<br>
     * Examples:<pre>
     *   unformatted: {@code VA59001123000012345678}
     *   formatted:   {@code VA59 0011 2300 0012 3456 78}
     *   components:  {@code VA 59 001 123000012345678}
     * </pre>
     */
    VA(StructureData.builder()
         .withIbanLength(22)
         .withBbanPattern("3!n15!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withAccountNumber(IndexRange.of(7, 22))
         .build(),
       MetaData.of(
           "Vatican City State", true, "VA59001123000012345678",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "Financial Information Authority (Autorita di Informazione Finanziaria - AIF)", "Office for Supervision and Regulation", "Palazzo San Carlo",
           " 00120", "uvr@aif.va", "+39 06 69871522")
    ),

    /**
     * <strong>Virgin Islands (British) ({@code VG})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a16!n}<br>
     * Examples:<pre>
     *   unformatted: {@code VG96VPVG0000012345678901}
     *   formatted:   {@code VG96 VPVG 0000 0123 4567 8901}
     *   components:  {@code VG 96 VPVG 0000012345678901}
     * </pre>
     */
    VG(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("4!a16!n")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withAccountNumber(IndexRange.of(8, 24))
         .build(),
       MetaData.of(
           "Virgin Islands", false, "VG96VPVG0000012345678901",
           YearMonth.of(2025, 10)),
       ContactData.of(
           "VP Bank House", null, "156 Mainstreet",
           "VG1110 Road Town Tortola", null, null)
    ),

    /**
     * <strong>Kosovo ({@code XK})</strong><p>
     * IBAN Length: 20<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!n10!n2!n}<br>
     * Examples:<pre>
     *   unformatted: {@code XK051212012345678906}
     *   formatted:   {@code XK05 1212 0123 4567 8906}
     *   components:  {@code XK 05 12 12 0123456789 06}
     * </pre>
     */
    XK(StructureData.builder()
         .withIbanLength(20)
         .withBbanPattern("4!n10!n2!n")
         .withBankCode("2!n", IndexRange.of(4, 6))
         .withBranchCode("2!n", IndexRange.of(6, 8))
         .withAccountNumber(IndexRange.of(8, 18))
         .withNationalCheckDigit(IndexRange.of(18, 20)) // XK
         .build(),
       MetaData.of(
           "Kosovo", false, "XK051212012345678906",
           YearMonth.of(2016, 9)),
       ContactData.of(
           "Central Bank of the Republic of Kosovo", "Payment Systems", "Garibaldi 33",
           "Prishtina / 10000", "payment.systems@bqk-kos.org", "+ 381 (0)38222055 (ext. 209, 210 and 211)")
    ),

    /**
     * <strong>Yemen ({@code YE})</strong><p>
     * IBAN Length: 30<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 4!a4!n18!c}<br>
     * Examples:<pre>
     *   unformatted: {@code YE15CBYE0001018861234567891234}
     *   formatted:   {@code YE15 CBYE 0001 0188 6123 4567 8912 34}
     *   components:  {@code YE 15 CBYE 0001 018861234567891234}
     * </pre>
     */
    YE(StructureData.builder()
         .withIbanLength(30)
         .withBbanPattern("4!a4!n18!c")
         .withBankCode("4!a", IndexRange.of(4, 8))
         .withBranchCode("4!n", IndexRange.of(8, 12))
         .withAccountNumber(IndexRange.of(12, 30))
         .build(),
       MetaData.of(
           "Yemen", false, "YE15CBYE0001018861234567891234",
           YearMonth.of(2024, 7)),
       ContactData.of(
           "Central Bank of Yemen", "Payment Systems Depatment", "Crater Aledaroos Street 452",
           "Aden", null, null)
    ),

    // --- END: Enum Constants (generated from IBAN Registry) ---

    // --- BGN: Enum Constants (manually maintained) ---

    /**
     * <strong>Burkina Faso ({@code BF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!c5!n11!n2!n}<br>
     * Example: {@code BF21BF084010130046357400039}
     */
    BF(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!c5!n11!n2!n")
         .withBankCode("5!c", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27))
         .build(),
       MetaData.of(
           "Burkina Faso", false, "BF21BF084010130046357400039", null),
       ContactData.of(
           "Central Bank of West African States (BCEAO)", null, "Avenue Abdoulaye Fadiga",
           "BP 3108 Dakar", "courrier.bceao@bceao.int", "+221 33 839 05 00")
    ),

    /**
     * <strong>Benin ({@code BJ})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!a5!n12!n2!n}<br>
     * Example: {@code BJ66BJ0610100100144390000769}
     */
    BJ(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("5!c5!n12!n2!n")
         .withBankCode("5!c", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 26))
         .withNationalCheckDigit(IndexRange.of(26, 28))
         .build(),
       MetaData.of(
           "Benin", false, "BJ66BJ0610100100144390000769", null),
       ContactData.of(
           "Central Bank of West African States (BCEAO)", null, "Avenue Abdoulaye Fadiga",
           "BP 3108 Dakar", "courrier.bceao@bceao.int", "+221 33 839 05 00")
    ),

    /**
     * <strong>Central African Republic ({@code CF})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Example: {@code CF4220001000010120069700160}
     */
    CF(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27))
         .build(),
       MetaData.of(
           "Central African Republic", false, "CF4220001000010120069700160", null),
       ContactData.of(
           "BEAC", null, "B.P. 851 Bangui - RCA",
           "Bangui", "beacbgf@beac.int", "+236 21 61 24 00")
    ),

    /**
     * <strong>Cameroon ({@code CM})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Example: {@code CM2110003024000224016952238}
     */
    CM(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27))
         .build(),
       MetaData.of(
           "Cameroon", false, "CM2110003024000224016952238", null),
       ContactData.of(
           "BEAC", null, "B.P. 1917 Yaoundé - Cameroun",
           "Yaoundé", "beac@beac.int", "+237 222 23 40 30")
    ),

    /**
     * <strong>Algeria ({@code DZ})</strong><p>
     * IBAN Length: 24<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 3!n5!n10!n2!n}<br>
     * Example: {@code DZ1700021000011130000005}
     */
    DZ(StructureData.builder()
         .withIbanLength(24)
         .withBbanPattern("3!n5!n10!n2!n")
         .withBankCode("3!n", IndexRange.of(4, 7))
         .withBranchCode("5!n", IndexRange.of(7, 12))
         .withAccountNumber(IndexRange.of(12, 22))
         .withNationalCheckDigit(IndexRange.of(22, 24))
         .build(),
       MetaData.of(
           "Algeria", false, "DZ1700021000011130000005", null),
       ContactData.of(
           "Bank of Algeria", null, "38, Avenue Franklin Roosevelt, Sidi M'Hamed",
           "Algiers", "communication@bank-of-algeria.dz", "+213 23 487 131")
    ),

    /**
     * <strong>Equatorial Guinea ({@code GQ})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Example: {@code GQ7050002001003715228190196}
     */
    GQ(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27))
         .build(),
       MetaData.of(
           "Equatorial Guinea", false, "GQ7050002001003715228190196", null),
       ContactData.of(
           "BEAC", null, "B.P. 501 Malabo",
           "Malabo", "beacmal@beac.int", "+240 333 09 59 30")
    ),

    /**
     * <strong>Comoros ({@code KM})</strong><p>
     * IBAN Length: 27<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!n5!n11!n2!n}<br>
     * Example: {@code KM4600005000010010904400137}
     */
    KM(StructureData.builder()
         .withIbanLength(27)
         .withBbanPattern("5!n5!n11!n2!n")
         .withBankCode("5!n", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 25))
         .withNationalCheckDigit(IndexRange.of(25, 27))
         .build(),
       MetaData.of(
           "Comoros", false, "KM4600005000010010904400137", null),
       ContactData.of(
           "Banque Centrale des Comores", null, "Place de France",
           "405 Moroni", "secretariat@banque-comores.km", "+269 773-18-14")
    ),

    /**
     * <strong>Senegal ({@code SN})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!c5!n12!n2!n}<br>
     * Example: {@code SN08SN1910100101260047607163}
     */
    SN(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("5!c5!n12!n2!n")
         .withBankCode("5!c", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 26))
         .withNationalCheckDigit(IndexRange.of(26, 28))
         .build(),
       MetaData.of(
           "Senegal", false, "SN08SN1910100101260047607163",
           YearMonth.of(2015, 5)),
       ContactData.of(
           "Central Bank of West African States (BCEAO)", null, "Avenue Abdoulaye Fadiga",
           "BP 3108 Dakar", "courrier.bceao@bceao.int", "+221 33 839 05 00")
    ),

    /**
     * <strong>Togo ({@code TG})</strong><p>
     * IBAN Length: 28<br>
     * SEPA: No<br>
     * BBAN Structure: {@code 5!c5!n12!n2!n}<br>
     * Example: {@code TG87TG0090100110232500400512}
     */
    TG(StructureData.builder()
         .withIbanLength(28)
         .withBbanPattern("5!c5!n12!n2!n")
         .withBankCode("5!c", IndexRange.of(4, 9))
         .withBranchCode("5!n", IndexRange.of(9, 14))
         .withAccountNumber(IndexRange.of(14, 26))
         .withNationalCheckDigit(IndexRange.of(26, 28))
         .build(),
       MetaData.of(
           "Togo", false, "TG87TG0090100110232500400512", null),
       ContactData.of(
           "Central Bank of West African States (BCEAO)", null, "Avenue Abdoulaye Fadiga",
           "BP 3108 Dakar", "courrier.bceao@bceao.int", "+221 33 839 05 00")
    );

    // --- END: Enum Constants (manually maintained) ---

    private final StructureData         structureData;
    private final MetaData              metaData;
    private final ContactData           contactData;

    private final String                countryFlag;
    private final Pattern               ibanRegex;

    private final IbanRegistry          baseCountry;

    /** The minimum required length: Country Code (2) + Check Digits (2). */
    static final int                    MIN_IBAN_BASE_LENGTH = 4;

    /** ISO 13616 standard minimum. */
    static final int                    MIN_IBAN_LENGTH      = Arrays.stream(values())
        .mapToInt(IbanRegistry::getIbanLength).min().orElse(MIN_IBAN_BASE_LENGTH);

    /** ISO 13616 standard maximum. */
    static final int                    MAX_IBAN_LENGTH      = Arrays.stream(values())
        .mapToInt(IbanRegistry::getIbanLength).max().orElse(34);

    /** Index of first IBAN check digit within the full IBAN string (position 3, 0-based index 2). */
    static final int                    INDEX_CHECK_DIGIT1   = 2;

    /** Index of second IBAN check digit within the full IBAN string (position 4, 0-based index 3). */
    static final int                    INDEX_CHECK_DIGIT2   = 3;

    /** Begin index of the Basic Bank Account Number (BBAN) within IBAN (position 5, 0-based index 4). */
    static final int                    INDEX_BBAN           = MIN_IBAN_BASE_LENGTH;

    /** Maximum length of BBAN. */
    static final int                    MAX_BBAN_LENGTH      = MAX_IBAN_LENGTH - INDEX_BBAN;

    /**
     * Fixed-size lookup array covering all 676 possible two-letter combinations
     * ({@code AA}–{@code ZZ}) of ISO 3166-1 Alpha-2 country codes.
     * <p>
     * Each slot is addressed directly via {@link #calcLookupIndex(char, char)};
     * slots with no registered country remain {@code null}.
     * Most of the 676 slots are unused – this sparseness is intentional:
     * it enables a direct array access with no hashing, no autoboxing,
     * and no {@link java.util.HashMap} overhead on every lookup call.
     */
    private static final IbanRegistry[] ALL_ENTRIES          = buildLookupArray(false);

    /**
     * Lookup array for base country registry entries only, indexed by country code via {@link #calcLookupIndex(char, char)}.
     */
    private static final IbanRegistry[] BASE_ENTRIES         = buildLookupArray(true);

    /**
     * Main constructor for {@code IbanRegistry} enum constants.
     * <p>
     * Initializes the country's IBAN structure, format details, and regulatory contact information.
     *
     * @param structureData the IBAN structure details object
     * @param metaData      the metadata object
     * @param contactData   the contact details object
     * @param baseCountry   the base country entry
     */
    IbanRegistry(
        StructureData structureData,
        MetaData metaData,
        ContactData contactData,
        IbanRegistry baseCountry
        ) {

        this.structureData = requireNonNull(structureData, "structureData required");
        this.metaData = requireNonNull(metaData, "metaData required");
        this.contactData = Optional.ofNullable(contactData).orElse(ContactData.EMPTY);
        this.baseCountry = baseCountry;

        this.countryFlag = CountryUtil.createFlagEmoji(name());

        String ibanPatternNoCountry = "2!n" + structureData.bbanPatternStr();
        this.ibanRegex = Pattern.compile('^' + name() + IbanPatternConverter.convertToRegex(ibanPatternNoCountry) + '$');
    }

    /**
     * Constructor for {@code IbanRegistry} enum constants.
     *
     * @param structureData the IBAN structure details object
     * @param metaData      the metadata object
     * @param contactData   the contact details object
     */
    IbanRegistry(StructureData structureData, MetaData metaData, ContactData contactData) {
        this(structureData, metaData, contactData, null);
    }

    /**
     * Secondary constructor for country codes that share their registry data with a base country.
     * <p>
     * Copies all structural and contact data from the specified base country entry.
     *
     * @param countryName the full English name of the country
     * @param baseCountry the {@code IbanRegistry} enum constant whose data is to be used
     */
    IbanRegistry(String countryName, IbanRegistry baseCountry) {
        // delegate to the main constructor using the objects from the base country entry
        this(
            baseCountry.structureData,
            MetaData.of(
                countryName,
                baseCountry.isSepa(),
                baseCountry.getIbanExample(),
                baseCountry.getLastUpdate()
            ),
            baseCountry.contactData,
            baseCountry
        );
    }

    /**
     * Returns the ISO 3166-1 Alpha-2 country code (e.g., "DE", "IT").
     *
     * @return the two-letter country code
     */
    public String getCountryCode() {
        return name();
    }

    /**
     * Returns the full English name of the country.
     *
     * @return the country name
     */
    public String getCountryName() {
        return metaData.getCountryName();
    }

    /**
     * Returns the two-character country flag emoji.
     *
     * @return the country flag emoji string
     */
    public String getCountryFlag() {
        return countryFlag;
    }

    /**
     * Returns the primary {@link Currency} used in this country.
     * <p>
     * The currency is resolved via {@link Iso3166Alpha2#getCurrency()}, keyed by this
     * entry's ISO 3166-1 Alpha-2 country code.
     *
     * @return the {@link Currency} constant for this country
     *
     * @since 1.8.5
     *
     * @see Iso3166Alpha2#getCurrency()
     */
    public Currency getCurrency() {
        return Iso3166Alpha2.fromCode(getCountryCode()).getCurrency();
    }

    /**
     * Returns the ISO 4217 three-letter currency code for this country as a {@code String}
     * (e.g., {@code "EUR"}, {@code "GBP"}).
     * <p>
     * Convenience shorthand for {@code getCurrency().getAlphaCode()}.
     *
     * @return the currency code string
     *
     * @since 1.8.5
     */
    public String getCurrencyCode() {
        return getCurrency().getAlphaCode();
    }

    /**
     * Checks whether the country participates in the Single Euro Payments Area (SEPA).
     *
     * @return {@code true} if the country is a SEPA member, {@code false} otherwise
     * @see <a href="https://www.europeanpaymentscouncil.eu/document-library/other/map-sepa-scheme-countries-and-territories">Map of SEPA Scheme Countries and Territories</a>
     */
    public boolean isSepa() {
        return metaData.isSepa();
    }

    /**
     * Checks whether the country does not participate in the Single Euro Payments Area (SEPA).
     * <p>
     * This is the negation of {@link #isSepa()}.
     *
     * @return {@code true} if the country is not a SEPA member, {@code false} if it is
     */
    public boolean isNotSepa() {
        return !isSepa();
    }

    /**
     * Returns the fixed length of the IBAN for this country.
     *
     * @return the total IBAN length
     */
    public int getIbanLength() {
        return structureData.ibanLength();
    }

    /**
     * Returns the regular expression {@code Pattern} object of the IBAN.
     *
     * @return the IBAN pattern object
     */
    public Pattern getIbanRegex() {
        return ibanRegex;
    }

    /**
     * Returns the {@link StructureData} object.
     *
     * @return structure data
     */
    public StructureData getStructureData() {
        return structureData;
    }

    /**
     * Returns the length of the BBAN (Basic Bank Account Number) part of the IBAN.
     *
     * @return the BBAN length
     */
    public int getBbanLength() {
        return structureData.getBbanLength();
    }

    /**
     * Returns the BBAN structure pattern string (e.g., "4!n4!n12!c").
     *
     * @return the BBAN pattern string
     */
    public String getBbanPatternStr() {
        return structureData.bbanPatternStr();
    }

    /**
     * Returns an example of a valid IBAN for this country.
     *
     * @return the IBAN example string
     */
    public String getIbanExample() {
        return metaData.getIbanExample();
    }

    /**
     * Returns the character pattern string for the Bank Identifier Code.
     *
     * @return the bank code pattern string
     */
    public String getBankCodePatternStr() {
        return structureData.bankCodePatternStr();
    }

    /**
     * Returns the index range defining the position of the Bank Code within the IBAN.
     *
     * @return the {@code IndexRange} for the bank code
     */
    IndexRange getBankCodeIndexRange() {
        return structureData.bankCodeIndexRange();
    }

    /**
     * Returns the character pattern string for the Branch Identifier Code.
     *
     * @return the branch code pattern string
     */
    public String getBranchCodePattern() {
        return structureData.branchCodePatternStr();
    }

    /**
     * Returns the index range defining the position of the Branch Code within the IBAN.
     *
     * @return the {@code IndexRange} for the branch code, or {@code null}
     */
    IndexRange getBranchCodeIndexRange() {
        return structureData.branchCodeIndexRange();
    }

    /**
     * Returns whether the country's BBAN structure defines a separate Branch Code part.
     *
     * @return {@code true} if a branch code exists, {@code false} otherwise
     */
    public boolean hasBranchCode() {
        return structureData.hasBranchCode();
    }

    /**
     * Returns the index range defining the position of the Account Number within the IBAN.
     *
     * @return the {@code IndexRange} for the account number
     */
    IndexRange getAccountNumberIndexRange() {
        return structureData.accountNumberIndexRange();
    }

    /**
     * Returns the index range defining the position of the optional National Check Digit (NCD) within the IBAN.
     *
     * @return the {@code IndexRange} for the national check digit
     */
    IndexRange getNationalCheckDigitIndexRange() {
        return structureData.nationalCheckDigitIndexRange();
    }

    /**
     * Returns whether the country's BBAN structure defines a National Check Digit (NCD) part.
     *
     * @return {@code true} if a National Check Digit (NCD) exists, {@code false} otherwise
     */
    public boolean hasNationalCheckDigit() {
        return structureData.hasNationalCheckDigit();
    }

    /**
     * Returns the {@link ContactData} object.
     *
     * @return contact data
     */
    public ContactData getContactData() {
        return contactData;
    }

    /**
     * Returns the name of the regulatory or financial organization responsible for the IBAN registry.
     *
     * @return the organization's name
     */
    public String getOrganisation() {
        return contactData.getOrganisation();
    }

    /**
     * Returns the relevant department name of the organization.
     *
     * @return the department name
     */
    public String getDepartment() {
        return contactData.getDepartment();
    }

    /**
     * Returns the street address of the organization.
     *
     * @return the street address
     */
    public String getStreetAddress() {
        return contactData.getStreetAddress();
    }

    /**
     * Returns the city and postal code of the organization.
     *
     * @return the city and postcode (zip code)
     */
    public String getCityPostcode() {
        return contactData.getCityPostcode();
    }

    /**
     * Returns the generic contact email address for the department.
     *
     * @return the email address
     */
    public String getDepartmentGenericEmail() {
        return contactData.getDepartmentGenericEmail();
    }

    /**
     * Returns the contact telephone number for the department.
     *
     * @return the telephone number
     */
    public String getDepartmentTel() {
        return contactData.getDepartmentTel();
    }

    /**
     * Returns the month and year the registry data was last updated by the source.
     *
     * @return the {@code YearMonth} of the last update
     */
    public YearMonth getLastUpdate() {
        return metaData.getLastUpdate();
    }

    /**
     * Returns the year component of the last-update date.
     * <p>
     * Equivalent to {@code getLastUpdate().getYear()} but avoids a dependency on
     * {@link java.time.YearMonth}, which requires API level 26 on Android.
     *
     * @return the four-digit year of the last update (e.g., {@code 2025})
     *
     * @since 1.8.3
     */
    public int getLastUpdateYear() {
        return metaData.getLastUpdateYear();
    }

    /**
     * Returns the month component of the last-update date as a value from 1 (January)
     * to 12 (December).
     * <p>
     * Equivalent to {@code getLastUpdate().getMonthValue()} but avoids a dependency on
     * {@link java.time.YearMonth}, which requires API level 26 on Android.
     *
     * @return the month of the last update (1–12)
     *
     * @since 1.8.3
     */
    public int getLastUpdateMonth() {
        return metaData.getLastUpdateMonth();
    }

    /**
     * Returns the base country {@code IbanRegistry} entry if this entry is a derived code
     * that inherits data from another country (e.g., {@code AX} points to {@code FI}).
     *
     * @return the base country {@code IbanRegistry} entry, or {@code null} if this is a base country
     */
    public IbanRegistry getBaseCountry() {
        return baseCountry;
    }

    /**
     * Returns the {@code IbanRegistry} entries that derive their data from this base country
     * (e.g., {@code FI} would return a list containing {@code AX}).
     *
     * @return a list of derived {@code IbanRegistry} entries, or an empty list
     */
    List<IbanRegistry> getDerivedCountries() {
        return isBaseCountry()
            ? Arrays.stream(values())
                .filter(cd -> this == cd.getBaseCountry())
                .collect(toList())
            : emptyList();
    }

    /**
     * Returns {@code true} if this entry is a base country, i.e. it does not inherit data
     * from another country (e.g., {@code FI}).
     *
     * @return {@code true} if this is a base country
     */
    public boolean isBaseCountry() {
        return baseCountry == null;
    }

    /**
     * Returns {@code true} if this entry is a derived code that inherits its IBAN
     * structure from another country (e.g., {@code AX} inherits from {@code FI}).
     *
     * @return {@code true} if this is a derived country
     */
    public boolean isDerivedCountry() {
        return baseCountry != null;
    }

    /**
     * Provides a detailed representation of the registry entry's structure.
     *
     * @return a string representation of this registry entry
     */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add(getCountryCode() + " (" + getCountryName() + ")")
            .add("SEPA country: " + (isSepa() ? "Yes" : "No"))
            .add("IBAN len: " + getIbanLength())
            .add("BBAN pattern: " + getBbanPatternStr())
            .add("Bank Code: " + getBankCodeIndexRange());
        if (getBranchCodeIndexRange() != null) {
            joiner.add("Branch Code: " + getBranchCodeIndexRange());
        }
        return joiner
            .add("Account No: " + getAccountNumberIndexRange())
            .add("IBAN Example: " + getIbanExample())
            .add("Organization: " + getOrganisation())
            .add("Last Update: " + getLastUpdate())
            .toString();
    }

    /**
     * Computes a unique array index for a two-letter ISO 3166-1 Alpha-2 country code.
     * <p>
     * The computation assumes both characters are ASCII uppercase letters ('A' through 'Z').
     * The resulting index is mapped into a range of 0 to 675 (inclusive), representing
     * all possible 26 &times; 26 letter combinations.
     * <p>
     * If either character is outside the 'A'–'Z' range, the method returns -1
     * to indicate an invalid input.
     *
     * @param c1 the first character of the ISO country code (expected 'A'–'Z')
     * @param c2 the second character of the ISO country code (expected 'A'–'Z')
     * @return a zero-based index in the range [0, 675], or -1 if input is invalid
     */
    private static int calcLookupIndex(char c1, char c2) {
        int i1 = c1 - 'A';
        int i2 = c2 - 'A';
        if (i1 < 0 || i1 > 25 || i2 < 0 || i2 > 25) {
            return -1;
        }
        return i1 * 26 + i2;
    }

    /**
     * Builds the private, static lookup array for {@code IbanRegistry} entries.
     * <p>
     * The array has a fixed size of 676 (26 × 26), covering all possible two-letter
     * ISO 3166-1 Alpha-2 country codes. Each entry is indexed via
     * {@link #calcLookupIndex(char, char)}; positions with no registered country remain {@code null}.
     *
     * @param baseCountriesOnly if {@code true}, only entries where {@link #isBaseCountry()} holds are included
     * @return the populated lookup array
     */
    private static IbanRegistry[] buildLookupArray(boolean baseCountriesOnly) {
        IbanRegistry[] array = new IbanRegistry[26 * 26]; // 676 Einträge, alle null
        for (IbanRegistry entry : values()) {
            if (!baseCountriesOnly || entry.isBaseCountry()) {
                String name = entry.name();
                array[calcLookupIndex(name.charAt(0), name.charAt(1))] = entry;
            }
        }
        return array;
    }

    /**
     * Returns the registry entry for a given ISO 3166-1 Alpha-2 country code instantly.
     *
     * @param code the two-letter country code (e.g., "DE")
     * @return the matching {@link IbanRegistry} constant,
     *         or {@code null} if the code is {@code null}, not exactly two characters, or unknown in this registry
     */
    public static IbanRegistry getByCode(final CharSequence code) {
        return code == null || code.length() != 2 ? null : getByCode(code.charAt(0), code.charAt(1));
    }

    /**
     * Returns the registry entry for a given ISO 3166-1 Alpha-2 country code.
     * <p>
     * This lookup includes all known codes, including derived ones.
     *
     * @param c1 the first character of the country code
     * @param c2 the second character of the country code
     * @return the {@code IbanRegistry} entry, or {@code null} if unknown in this registry
     */
    public static IbanRegistry getByCode(final char c1, final char c2) {
        int idx = calcLookupIndex(c1, c2);
        return idx < 0 ? null : ALL_ENTRIES[idx];
    }

    /**
     * Returns the registry entry only if it is a base country.
     * <p>
     * Use this method for standard IBAN validation to ensure that derived
     * territories (like GF, GP) are not treated as independent IBAN nations.
     *
     * @param c1 the first character of the country code
     * @param c2 the second character of the country code
     * @return the base {@code IbanRegistry} entry, or {@code null} if derived or unknown in this registry
     */
    public static IbanRegistry getBaseEntryByCode(final char c1, final char c2) {
        int idx = calcLookupIndex(c1, c2);
        return idx < 0 ? null : BASE_ENTRIES[idx];
    }

    /**
     * Returns all IBAN registries for SEPA countries.
     *
     * @return an unmodifiable list of SEPA country registries
     */
    public static List<IbanRegistry> getSepaCountries() {
        return Arrays.stream(values())
            .filter(IbanRegistry::isSepa)
            .collect(collectingAndThen(
                toList(),
                Collections::unmodifiableList
            ));
    }

    /**
     * The immutable class defining the structure of the BBAN and its component index ranges.<br>
     * The builder pattern is used to handle optional index ranges and improve legibility.
     */
    static final class StructureData {
        private final int        ibanLength;
        private final String     bbanPatternStr;
        private final String     bankCodePatternStr;
        private final IndexRange bankCodeIndexRange;
        private final String     branchCodePatternStr;
        private final IndexRange branchCodeIndexRange;
        private final IndexRange accountNumberIndexRange;
        private final IndexRange nationalCheckDigitIndexRange;

        private StructureData(Builder builder) {
            this.ibanLength = builder.ibanLength;
            this.bbanPatternStr = requireNonNull(builder.bbanPatternStr, "bbanPatternStr required");
            this.bankCodePatternStr = builder.bankCodePatternStr;
            this.bankCodeIndexRange = builder.bankCodeIndexRange;
            this.branchCodePatternStr = builder.branchCodePatternStr;
            this.branchCodeIndexRange = builder.branchCodeIndexRange;
            this.accountNumberIndexRange = requireNonNull(builder.accountNumberIndexRange, "accountNumberIndexRange required");
            this.nationalCheckDigitIndexRange = builder.nationalCheckDigitIndexRange;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int        ibanLength;
            private String     bbanPatternStr;
            private IndexRange accountNumberIndexRange;

            // optional fields
            private String     bankCodePatternStr;
            private IndexRange bankCodeIndexRange;
            private String     branchCodePatternStr;
            private IndexRange branchCodeIndexRange;
            private IndexRange nationalCheckDigitIndexRange;

            private Builder() {
            }

            public Builder withIbanLength(int ibanLength) {
                this.ibanLength = ibanLength;
                return this;
            }

            public Builder withBbanPattern(String bbanPatternStr) {
                this.bbanPatternStr = bbanPatternStr;
                return this;
            }

            public Builder withAccountNumber(IndexRange accountNumberIndexRange) {
                this.accountNumberIndexRange = accountNumberIndexRange;
                return this;
            }

            public Builder withNationalCheckDigit(IndexRange nationalCheckDigitIndexRange) {
                this.nationalCheckDigitIndexRange = nationalCheckDigitIndexRange;
                return this;
            }

            public Builder withBankCode(String bankCodePatternStr, IndexRange bankCodeIndexRange) {
                this.bankCodePatternStr = bankCodePatternStr;
                this.bankCodeIndexRange = bankCodeIndexRange;
                return this;
            }

            public Builder withBranchCode(String branchCodePatternStr, IndexRange branchCodeIndexRange) {
                this.branchCodePatternStr = branchCodePatternStr;
                this.branchCodeIndexRange = branchCodeIndexRange;
                return this;
            }

            StructureData build() {
                if (ibanLength <= 0) {
                    throw new IllegalStateException("IBAN length must be set and positive");
                } else if (ibanLength < 15 || ibanLength > 34) { // ISO 13616 limits
                    throw new IllegalStateException("IBAN length must be between 15 and 34");
                }
                requireNonNull(bbanPatternStr, "BBAN pattern must be set");
                requireNonNull(accountNumberIndexRange, "Account number index range must be set");

                return new StructureData(this);
            }
        }

        public int ibanLength() {
            return ibanLength;
        }

        public String bbanPatternStr() {
            return bbanPatternStr;
        }

        public String bankCodePatternStr() {
            return bankCodePatternStr;
        }

        IndexRange bankCodeIndexRange() {
            return bankCodeIndexRange;
        }

        public String branchCodePatternStr() {
            return branchCodePatternStr;
        }

        IndexRange branchCodeIndexRange() {
            return branchCodeIndexRange;
        }

        public boolean hasBranchCode() {
            return branchCodeIndexRange != null;
        }

        IndexRange accountNumberIndexRange() {
            return accountNumberIndexRange;
        }

        IndexRange nationalCheckDigitIndexRange() {
            return nationalCheckDigitIndexRange;
        }

        public boolean hasNationalCheckDigit() {
            return nationalCheckDigitIndexRange != null;
        }

        public int getBbanLength() {
            return ibanLength - INDEX_BBAN;
        }
    }

    /**
     * The immutable class defining the contact and regulatory data.
     */
    static final class ContactData {
        private static final ContactData EMPTY = of(null, null, null, null, null, null);

        private final String organisation;
        private final String department;
        private final String streetAddress;
        private final String cityPostcode;
        private final String departmentGenericEmail;
        private final String departmentTel;

        private ContactData(
            String organisation,
            String department,
            String streetAddress,
            String cityPostcode,
            String departmentGenericEmail,
            String departmentTel) {
            this.organisation = organisation;
            this.department = department;
            this.streetAddress = streetAddress;
            this.cityPostcode = cityPostcode;
            this.departmentGenericEmail = departmentGenericEmail;
            this.departmentTel = departmentTel;
        }

        public static ContactData of(
            String organisation,
            String department,
            String streetAddress,
            String cityPostcode,
            String departmentGenericEmail,
            String departmentTel) {
            return new ContactData(
                organisation, department, streetAddress,
                cityPostcode, departmentGenericEmail, departmentTel);
        }

        public String getOrganisation() {
            return organisation;
        }

        public String getDepartment() {
            return department;
        }

        public String getStreetAddress() {
            return streetAddress;
        }

        public String getCityPostcode() {
            return cityPostcode;
        }

        public String getDepartmentGenericEmail() {
            return departmentGenericEmail;
        }

        public String getDepartmentTel() {
            return departmentTel;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ContactData other = (ContactData) obj;
            return Objects.equals(organisation, other.organisation)
                && Objects.equals(department, other.department)
                && Objects.equals(streetAddress, other.streetAddress)
                && Objects.equals(cityPostcode, other.cityPostcode)
                && Objects.equals(departmentGenericEmail, other.departmentGenericEmail)
                && Objects.equals(departmentTel, other.departmentTel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(organisation, department, streetAddress,
                                cityPostcode, departmentGenericEmail, departmentTel);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                + '['
                + "organisation=" + organisation
                + ", department=" + department
                + ", streetAddress=" + streetAddress
                + ", cityPostcode=" + cityPostcode
                + ", departmentGenericEmail=" + departmentGenericEmail
                + ", departmentTel=" + departmentTel
                + ']';
        }
    }

    /**
     * The immutable class defining the country name, SEPA status, example, and last update date.
     */
    static final class MetaData {
        private final String    countryName;
        private final boolean   isSepa;
        private final String    ibanExample;
        private final YearMonth lastUpdate;

        private MetaData(
            String countryName,
            boolean isSepa,
            String ibanExample,
            YearMonth lastUpdate) {
            this.countryName = requireNonNull(countryName, "countryName required");
            this.isSepa = isSepa;
            this.ibanExample = requireNonNull(ibanExample, "ibanExample required");
            this.lastUpdate = lastUpdate;
        }

        public static MetaData of(
            String countryName,
            boolean isSepa,
            String ibanExample,
            YearMonth lastUpdate) {
            return new MetaData(
                countryName, isSepa, ibanExample, lastUpdate);
        }

        public String getCountryName() {
            return countryName;
        }

        public boolean isSepa() {
            return isSepa;
        }

        public String getIbanExample() {
            return ibanExample;
        }

        public YearMonth getLastUpdate() {
            return lastUpdate;
        }

        /**
         * Returns the year component of the last-update date.
         * <p>
         * Equivalent to {@code getLastUpdate().getYear()} but avoids a dependency on
         * {@link java.time.YearMonth}, which requires API level 26 on Android.
         *
         * @return the four-digit year of the last update (e.g., {@code 2025})
         *
         * @since 1.8.3
         */
        public int getLastUpdateYear() {
            return lastUpdate.getYear();
        }

        /**
         * Returns the month component of the last-update date as a value from 1 (January)
         * to 12 (December).
         * <p>
         * Equivalent to {@code getLastUpdate().getMonthValue()} but avoids a dependency on
         * {@link java.time.YearMonth}, which requires API level 26 on Android.
         *
         * @return the month of the last update (1–12)
         *
         * @since 1.8.3
         */
        public int getLastUpdateMonth() {
            return lastUpdate.getMonthValue();
        }
    }

}

