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

import static java.util.Objects.requireNonNull;

import de.speedbanking.checkdigit.de.CheckDigitResult;
import de.speedbanking.checkdigit.de.GermanAccountCheckDigit;
import de.speedbanking.iban.Iban;
import de.speedbanking.util.UtilityClasses;

import java.util.Optional;

/**
 * Checks the account number of a German IBAN against the check digit method of its bank, taken
 * from {@link BankDataLookup}.
 * <p>
 * Needs {@code iban-commons-de-checkdigit} on the classpath; this module declares it as an
 * optional dependency.
 *
 * @since 1.8.12
 */
public final class GermanAccountCheck {

    private static final String GERMANY = "DE";

    private GermanAccountCheck() {
        throw UtilityClasses.cannotInstantiate(getClass());
    }

    /**
     * Checks the account number of a German IBAN.
     *
     * @param iban a German IBAN; must not be {@code null}
     * @return the outcome of the check
     * @throws IllegalArgumentException if the IBAN is not German
     */
    public static GermanAccountCheckResult check(Iban iban) {
        requireNonNull(iban, "iban must not be null");
        if (!GERMANY.equals(iban.getCountryCode())) {
            throw new IllegalArgumentException(String.format("Not a German IBAN: %s", iban.getCountryCode()));
        }

        Optional<BankData> bank = BankDataLookup.byIban(iban);
        if (!bank.isPresent()) {
            return GermanAccountCheckResult.BANK_CODE_UNKNOWN;
        }
        Optional<String> method = bank.get().checkDigitMethod();
        if (!method.isPresent()) {
            return GermanAccountCheckResult.METHOD_UNKNOWN;
        }

        CheckDigitResult result;
        try {
            result = GermanAccountCheckDigit.verify(method.get(), iban.getBankCode(), iban.getAccountNumber());
        } catch (IllegalArgumentException ignored) {
            return GermanAccountCheckResult.METHOD_NOT_IMPLEMENTED;
        }
        if (!result.isChecked()) {
            return GermanAccountCheckResult.NOT_CHECKED;
        }
        return result.isValid() ? GermanAccountCheckResult.VALID : GermanAccountCheckResult.INVALID;
    }
}
