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

import static de.speedbanking.util.CharUtil.isAllDigits;
import static de.speedbanking.util.CharUtil.isAllUpperCase;

import de.speedbanking.util.CharUtil;

/**
 * Functional Interface for country-specific IBAN and BBAN (Basic Bank Account Number) validations.
 * <p>
 * Each supported country must provide a concrete implementation to validate
 * the BBAN portion of the IBAN according to its national format rules.
 */
@FunctionalInterface
interface CountryValidator {

    /**
     * Country-specific validation of the BBAN structure inside the IBAN.
     * <p>
     * Assumes that the input array has already undergone elementary validations (length, characters etc.).<br>
     * As such, the input is expected to contain only digits and uppercase characters.
     * Therefore, there is no need to check for {@code null}, array length, and {@link CharUtil#isDigitOrUpperCase(char)}.
     *
     * @param iban the IBAN character sequence to validate
     * @return {@code true} if the IBAN conforms to the country's structure rules, {@code false} otherwise
     */
    boolean validateIban(char[] iban);

    /** Validator for Andorra (AD), IBAN length: 24, BBAN pattern: {@code 4!n4!n12!c} */
    final class AD implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 12);
        }
    }

    /** Validator for United Arab Emirates (AE), IBAN length: 23, BBAN pattern: {@code 3!n16!n} */
    final class AE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 23);
        }
    }

    /** Validator for Albania (AL), IBAN length: 28, BBAN pattern: {@code 8!n16!c} */
    final class AL implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 12);
        }
    }

    /** Validator for Angola (AO), IBAN length: 25, BBAN pattern: {@code 4!n4!n11!n2!n} */
    final class AO implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 25);
        }
    }

    /** Validator for Austria (AT), IBAN length: 20, BBAN pattern: {@code 5!n11!n} */
    final class AT implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 20);
        }
    }

    /** Validator for Azerbaijan (AZ), IBAN length: 28, BBAN pattern: {@code 4!a20!c} */
    final class AZ implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Bosnia and Herzegovina (BA), IBAN length: 20, BBAN pattern: {@code 3!n3!n8!n2!n} */
    final class BA implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 20);
        }
    }

    /** Validator for Belgium (BE), IBAN length: 16, BBAN pattern: {@code 3!n7!n2!n} */
    final class BE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 16);
        }
    }

    /** Validator for Bulgaria (BG), IBAN length: 22, BBAN pattern: {@code 4!a4!n2!n8!c} */
    final class BG implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 14);
        }
    }

    /** Validator for Bahrain (BH), IBAN length: 22, BBAN pattern: {@code 4!a14!c} */
    final class BH implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Burundi (BI), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!n2!n} */
    final class BI implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 27);
        }
    }

    /** Validator for Brazil (BR), IBAN length: 29, BBAN pattern: {@code 8!n5!n10!n1!a1!c} */
    final class BR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 27)
                   && isAllUpperCase(iban, 27, 28);
        }
    }

    /** Validator for Belarus (BY), IBAN length: 28, BBAN pattern: {@code 4!c4!n16!c} */
    final class BY implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 8, 12);
        }
    }

    /** Validator for Switzerland (CH), IBAN length: 21, BBAN pattern: {@code 5!n12!c} */
    final class CH implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 9);
        }
    }

    /** Validator for Costa Rica (CR), IBAN length: 22, BBAN pattern: {@code 4!n14!n} */
    final class CR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 22);
        }
    }

    /** Validator for Cape Verde (CV), IBAN length: 25, BBAN pattern: {@code 4!n4!n13!c} */
    final class CV implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 12);
        }
    }

    /** Validator for Cyprus (CY), IBAN length: 28, BBAN pattern: {@code 3!n5!n16!c} */
    final class CY implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 12);
        }
    }

    /** Validator for Czechia (CZ), IBAN length: 24, BBAN pattern: {@code 4!n16!n} */
    final class CZ implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 24);
        }
    }

    /** Validator for Germany (DE), IBAN length: 22, BBAN pattern: {@code 8!n10!n} */
    final class DE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 22);
        }
    }

    /** Validator for Djibouti (DJ), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!n2!n} */
    final class DJ implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 27);
        }
    }

    /** Validator for Denmark (DK), IBAN length: 18, BBAN pattern: {@code 4!n9!n1!n} */
    final class DK implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 18);
        }
    }

    /** Validator for Dominican Republic (DO), IBAN length: 28, BBAN pattern: {@code 4!c20!n} */
    final class DO implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 8, 28);
        }
    }

    /** Validator for Estonia (EE), IBAN length: 20, BBAN pattern: {@code 2!n14!n} */
    final class EE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 20);
        }
    }

    /** Validator for Egypt (EG), IBAN length: 29, BBAN pattern: {@code 4!n4!n17!n} */
    final class EG implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 29);
        }
    }

    /** Validator for Spain (ES), IBAN length: 24, BBAN pattern: {@code 4!n4!n1!n1!n10!n} */
    final class ES implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 24);
        }
    }

    /** Validator for Finland (FI), IBAN length: 18, BBAN pattern: {@code 3!n11!n} */
    final class FI implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 18);
        }
    }

    /** Validator for Åland Islands (AX), IBAN length: 18, BBAN pattern: {@code 3!n11!n} */
    final class AX implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 18);
        }
    }

    /** Validator for Falkland Islands (FK), IBAN length: 18, BBAN pattern: {@code 2!a12!n} */
    final class FK implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 6)
                   && isAllDigits(iban, 6, 18);
        }
    }

    /** Validator for Faroe Islands (FO), IBAN length: 18, BBAN pattern: {@code 4!n9!n1!n} */
    final class FO implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 18);
        }
    }

    /** Validator for France (FR), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class FR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for French Guiana (GF), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class GF implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Guadeloupe (GP), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class GP implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Martinique (MQ), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class MQ implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Réunion (RE), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class RE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for French Polynesia (PF), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class PF implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for French Southern Territories (TF), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class TF implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Mayotte (YT), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class YT implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for New Caledonia (NC), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class NC implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Saint Barthélemy (BL), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class BL implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Saint Martin (French part) (MF), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class MF implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Saint Pierre and Miquelon (PM), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class PM implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Wallis and Futuna Islands (WF), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class WF implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Gabon (GA), IBAN length: 27, BBAN pattern: {@code 5!n5!n13!c} */
    final class GA implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14);
        }
    }

    /** Validator for United Kingdom (GB), IBAN length: 22, BBAN pattern: {@code 4!a6!n8!n} */
    final class GB implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 22);
        }
    }

    /** Validator for Isle of Man (IM), IBAN length: 22, BBAN pattern: {@code 4!a6!n8!n} */
    final class IM implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 22);
        }
    }

    /** Validator for Jersey (JE), IBAN length: 22, BBAN pattern: {@code 4!a6!n8!n} */
    final class JE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 22);
        }
    }

    /** Validator for Guernsey (GG), IBAN length: 22, BBAN pattern: {@code 4!a6!n8!n} */
    final class GG implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 22);
        }
    }

    /** Validator for Georgia (GE), IBAN length: 22, BBAN pattern: {@code 2!a16!n} */
    final class GE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 6)
                   && isAllDigits(iban, 6, 22);
        }
    }

    /** Validator for Gibraltar (GI), IBAN length: 23, BBAN pattern: {@code 4!a15!c} */
    final class GI implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Greenland (GL), IBAN length: 18, BBAN pattern: {@code 4!n9!n1!n} */
    final class GL implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 18);
        }
    }

    /** Validator for Greece (GR), IBAN length: 27, BBAN pattern: {@code 3!n4!n16!c} */
    final class GR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 11);
        }
    }

    /** Validator for Honduras (HN), IBAN length: 28, BBAN pattern: {@code 4!a20!n} */
    final class HN implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 28);
        }
    }

    /** Validator for Croatia (HR), IBAN length: 21, BBAN pattern: {@code 7!n10!n} */
    final class HR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 21);
        }
    }

    /** Validator for Hungary (HU), IBAN length: 28, BBAN pattern: {@code 3!n4!n1!n15!n1!n} */
    final class HU implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 28);
        }
    }

    /** Validator for Ireland (IE), IBAN length: 22, BBAN pattern: {@code 4!a6!n8!n} */
    final class IE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 22);
        }
    }

    /** Validator for Israel (IL), IBAN length: 23, BBAN pattern: {@code 3!n3!n13!n} */
    final class IL implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 23);
        }
    }

    /** Validator for Iraq (IQ), IBAN length: 23, BBAN pattern: {@code 4!a3!n12!n} */
    final class IQ implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 23);
        }
    }

    /** Validator for Islamic Republic of Iran (IR), IBAN length: 26, BBAN pattern: {@code 3!n19!n} */
    final class IR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 26);
        }
    }

    /** Validator for Iceland (IS), IBAN length: 26, BBAN pattern: {@code 4!n2!n6!n10!n} */
    final class IS implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 26);
        }
    }

    /** Validator for Italy (IT), IBAN length: 27, BBAN pattern: {@code 1!a5!n5!n12!c} */
    final class IT implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 5)
                   && isAllDigits(iban, 5, 15);
        }
    }

    /** Validator for Jordan (JO), IBAN length: 30, BBAN pattern: {@code 4!a4!n18!c} */
    final class JO implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 12);
        }
    }

    /** Validator for Kuwait (KW), IBAN length: 30, BBAN pattern: {@code 4!a22!c} */
    final class KW implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Kazakhstan (KZ), IBAN length: 20, BBAN pattern: {@code 3!n13!c} */
    final class KZ implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 7);
        }
    }

    /** Validator for Lebanon (LB), IBAN length: 28, BBAN pattern: {@code 4!n20!c} */
    final class LB implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 8);
        }
    }

    /** Validator for Saint Lucia (LC), IBAN length: 32, BBAN pattern: {@code 4!a24!c} */
    final class LC implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Liechtenstein (LI), IBAN length: 21, BBAN pattern: {@code 5!n12!c} */
    final class LI implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 9);
        }
    }

    /** Validator for Lithuania (LT), IBAN length: 20, BBAN pattern: {@code 5!n11!n} */
    final class LT implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 20);
        }
    }

    /** Validator for Luxembourg (LU), IBAN length: 20, BBAN pattern: {@code 3!n13!c} */
    final class LU implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 7);
        }
    }

    /** Validator for Latvia (LV), IBAN length: 21, BBAN pattern: {@code 4!a13!c} */
    final class LV implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Libya (LY), IBAN length: 25, BBAN pattern: {@code 3!n3!n15!n} */
    final class LY implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 25);
        }
    }

    /** Validator for Morocco (MA), IBAN length: 28, BBAN pattern: {@code 3!n5!n16!n} */
    final class MA implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 28);
        }
    }

    /** Validator for Monaco (MC), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!c2!n} */
    final class MC implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 14)
                   && isAllDigits(iban, 25, 27);
        }
    }

    /** Validator for Montenegro (ME), IBAN length: 22, BBAN pattern: {@code 3!n13!n2!n} */
    final class ME implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 22);
        }
    }

    /** Validator for North Macedonia (MK), IBAN length: 19, BBAN pattern: {@code 3!n10!c2!n} */
    final class MK implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 7)
                   && isAllDigits(iban, 17, 19);
        }
    }

    /** Validator for Mongolia (MN), IBAN length: 20, BBAN pattern: {@code 4!n12!n} */
    final class MN implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 20);
        }
    }

    /** Validator for Mauritania (MR), IBAN length: 27, BBAN pattern: {@code 5!n5!n11!n2!n} */
    final class MR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 27);
        }
    }

    /** Validator for Malta (MT), IBAN length: 31, BBAN pattern: {@code 4!a5!n18!c} */
    final class MT implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 13);
        }
    }

    /** Validator for Mauritius (MU), IBAN length: 30, BBAN pattern: {@code 4!a2!n2!n12!n3!n3!a} */
    final class MU implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 8, 27)
                   && isAllUpperCase(iban, 4, 8)
                   && isAllUpperCase(iban, 27, 30);
        }
    }

    /** Validator for Mozambique (MZ), IBAN length: 25, BBAN pattern: {@code 4!n4!n11!n2!n} */
    final class MZ implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 25);
        }
    }

    /** Validator for Nicaragua (NI), IBAN length: 28, BBAN pattern: {@code 4!a20!n} */
    final class NI implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 28);
        }
    }

    /** Validator for Netherlands (NL), IBAN length: 18, BBAN pattern: {@code 4!a10!n} */
    final class NL implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 18);
        }
    }

    /** Validator for Norway (NO), IBAN length: 15, BBAN pattern: {@code 4!n6!n1!n} */
    final class NO implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 15);
        }
    }

    /** Validator for Oman (OM), IBAN length: 23, BBAN pattern: {@code 3!n16!c} */
    final class OM implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 7);
        }
    }

    /** Validator for Pakistan (PK), IBAN length: 24, BBAN pattern: {@code 4!a16!c} */
    final class PK implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Poland (PL), IBAN length: 28, BBAN pattern: {@code 8!n16!n} */
    final class PL implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 28);
        }
    }

    /** Validator for Palestine (PS), IBAN length: 29, BBAN pattern: {@code 4!a21!c} */
    final class PS implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Portugal (PT), IBAN length: 25, BBAN pattern: {@code 4!n4!n11!n2!n} */
    final class PT implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 25);
        }
    }

    /** Validator for Qatar (QA), IBAN length: 29, BBAN pattern: {@code 4!a21!c} */
    final class QA implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Romania (RO), IBAN length: 24, BBAN pattern: {@code 4!a16!c} */
    final class RO implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8);
        }
    }

    /** Validator for Serbia (RS), IBAN length: 22, BBAN pattern: {@code 3!n13!n2!n} */
    final class RS implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 22);
        }
    }

    /** Validator for Russia (RU), IBAN length: 33, BBAN pattern: {@code 9!n5!n15!c} */
    final class RU implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 18);
        }
    }

    /** Validator for Saudi Arabia (SA), IBAN length: 24, BBAN pattern: {@code 2!n18!c} */
    final class SA implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 6);
        }
    }

    /** Validator for Seychelles (SC), IBAN length: 31, BBAN pattern: {@code 4!a2!n2!n16!n3!a} */
    final class SC implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 8, 28)
                   && isAllUpperCase(iban, 4, 8)
                   && isAllUpperCase(iban, 28, 31);
        }
    }

    /** Validator for Sudan (SD), IBAN length: 18, BBAN pattern: {@code 2!n12!n} */
    final class SD implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 18);
        }
    }

    /** Validator for Sweden (SE), IBAN length: 24, BBAN pattern: {@code 3!n16!n1!n} */
    final class SE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 24);
        }
    }

    /** Validator for Slovenia (SI), IBAN length: 19, BBAN pattern: {@code 5!n8!n2!n} */
    final class SI implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 19);
        }
    }

    /** Validator for Slovakia (SK), IBAN length: 24, BBAN pattern: {@code 4!n6!n10!n} */
    final class SK implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 24);
        }
    }

    /** Validator for San Marino (SM), IBAN length: 27, BBAN pattern: {@code 1!a5!n5!n12!c} */
    final class SM implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 5)
                   && isAllDigits(iban, 5, 15);
        }
    }

    /** Validator for Somalia (SO), IBAN length: 23, BBAN pattern: {@code 4!n3!n12!n} */
    final class SO implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 23);
        }
    }

    /** Validator for São Tomé und Príncipe (ST), IBAN length: 25, BBAN pattern: {@code 4!n4!n11!n2!n} */
    final class ST implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 25);
        }
    }

    /** Validator for El Salvador (SV), IBAN length: 28, BBAN pattern: {@code 4!a20!n} */
    final class SV implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 28);
        }
    }

    /** Validator for Timor-Leste (TL), IBAN length: 23, BBAN pattern: {@code 3!n14!n2!n} */
    final class TL implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 23);
        }
    }

    /** Validator for Tunisia (TN), IBAN length: 24, BBAN pattern: {@code 2!n3!n13!n2!n} */
    final class TN implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 24);
        }
    }

    /** Validator for Turkey (TR), IBAN length: 26, BBAN pattern: {@code 5!n1!n16!c} */
    final class TR implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 10);
        }
    }

    /** Validator for Ukraine (UA), IBAN length: 29, BBAN pattern: {@code 6!n19!c} */
    final class UA implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 10);
        }
    }

    /** Validator for HolyVatican City State (VA), IBAN length: 22, BBAN pattern: {@code 3!n15!n} */
    final class VA implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 22);
        }
    }

    /** Validator for Virgin Islands (VG), IBAN length: 24, BBAN pattern: {@code 4!a16!n} */
    final class VG implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 24);
        }
    }

    /** Validator for Kosovo (XK), IBAN length: 20, BBAN pattern: {@code 4!n10!n2!n} */
    final class XK implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllDigits(iban, 4, 20);
        }
    }

    /** Validator for Yemen (YE), IBAN length: 30, BBAN pattern: {@code 4!a4!n18!c} */
    final class YE implements CountryValidator {
        @Override
        public boolean validateIban(final char[] iban) {
            return isAllUpperCase(iban, 4, 8)
                   && isAllDigits(iban, 8, 12);
        }
    }

}
