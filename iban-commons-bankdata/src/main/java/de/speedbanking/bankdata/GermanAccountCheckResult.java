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
package de.speedbanking.bankdata;

/**
 * Outcome of {@link GermanAccountCheck#check(de.speedbanking.iban.Iban)}.
 *
 * @since 1.8.12
 */
public enum GermanAccountCheckResult {

    /** The check digit of the account number matches the method of the bank. */
    VALID,

    /** The account number fails the check digit method of the bank. */
    INVALID,

    /** The method of the bank defines no check digit for this account number (for example method 09). */
    NOT_CHECKED,

    /** The bank code is not in the bank data. */
    BANK_CODE_UNKNOWN,

    /** The bank data carries no check digit method for the bank, for example an older cache file. */
    METHOD_UNKNOWN,

    /** The method of the bank is not implemented by {@code iban-commons-de-checkdigit}. */
    METHOD_NOT_IMPLEMENTED
}
