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
package de.speedbanking.bankdata.refresh;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bic.Bic;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces equal field values of the bank data records of one load by a single shared instance.
 * <p>
 * Bank names, BICs, cities, postal codes and check digit methods repeat a lot across the records
 * of a country, e.g. the Polish data has 2,040 records but only about 100 distinct bank names and
 * BICs. Sharing these values cuts the heap used by the loaded bank data by about a third.
 * <p>
 * The maps only live for one load, unlike {@link String#intern()}, so values of replaced data
 * become garbage after a refresh. Not thread-safe; use one instance per load.
 *
 * @since 1.8.13
 */
final class BankDataDeduplicator {

    private final Map<String, String> strings = new HashMap<>();
    private final Map<Bic, Bic>       bics    = new HashMap<>();

    /**
     * Returns a record equal to {@code data} whose field values are shared with all records
     * passed to this instance before.
     *
     * @param data the record to deduplicate
     * @return the deduplicated record, equal to {@code data}
     */
    BankData deduplicate(BankData data) {
        return BankData.builder(share(data.getCountryCode()), share(data.getBankCode()),
                share(data.getBankName()), share(data.getSourceVersion()))
            .branchCode(share(data.getBranchCode()))
            .bic(share(data.getBic()))
            .postalCode(share(data.getPostalCode()))
            .city(share(data.getCity()))
            .checkDigitMethod(share(data.getCheckDigitMethod()))
            .build();
    }

    /**
     * Returns the shared instance of the given string.
     *
     * @param value the value, may be {@code null}
     * @return the first equal value seen by this instance, or {@code null}
     */
    private String share(String value) {
        if (value == null) {
            return null;
        }
        String shared = strings.putIfAbsent(value, value);
        return shared == null ? value : shared;
    }

    /**
     * Returns the shared instance of the given BIC.
     *
     * @param value the value, may be {@code null}
     * @return the first equal BIC seen by this instance, or {@code null}
     */
    private Bic share(Bic value) {
        if (value == null) {
            return null;
        }
        Bic shared = bics.putIfAbsent(value, value);
        return shared == null ? value : shared;
    }
}
