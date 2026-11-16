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
package de.speedbanking.bankdata.loader;

import java.net.URI;

/**
 * Loader for Liechtenstein banks, sourced from the SIX Interbank Clearing "Bank Master" directory.
 * <p>
 * Liechtenstein IBANs use IIDs from the Swiss number space, e.g. {@code 08810} for LGT Bank. See
 * {@link AbstractBankDataLoaderSix} for the source format and why no bundled snapshot is shipped.
 * Takes only the rows whose {@code Country} column is {@code LI}.
 *
 * @since 1.8.15
 */
public final class BankDataLoaderLi extends AbstractBankDataLoaderSix {

    /** Default remote source URI: the same SIX Bank Master v3 CSV as {@link BankDataLoaderCh}. */
    public static final URI DEFAULT_SOURCE_URI = BankDataLoaderDefaults.sourceUri("LI");

    static final String     COUNTRY_CODE       = "LI";

    /**
     * Creates a loader for the default remote source {@link #DEFAULT_SOURCE_URI}.
     */
    public BankDataLoaderLi() {
        super(DEFAULT_SOURCE_URI);
    }

    @Override
    protected boolean includesCountry(String country) {
        return COUNTRY_CODE.equals(country);
    }

}
