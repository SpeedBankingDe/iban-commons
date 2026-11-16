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
 * Loader for Swiss banks, sourced from the SIX Interbank Clearing "Bank Master" directory.
 * <p>
 * See {@link AbstractBankDataLoaderSix} for the source format and why no bundled snapshot is
 * shipped. Takes every row except those of Liechtenstein, which {@link BankDataLoaderLi} covers.
 * Rows of other countries are foreign institutions holding a Swiss IID and stay included.
 *
 * @since 1.8.12
 */
public final class BankDataLoaderCh extends AbstractBankDataLoaderSix {

    /**
     * Default remote source URI: the SIX Bank Master v3 CSV, a stable, versioned REST endpoint
     * (no login required).
     */
    public static final URI DEFAULT_SOURCE_URI = BankDataLoaderDefaults.sourceUri("CH");

    /**
     * Creates a loader for the default remote source {@link #DEFAULT_SOURCE_URI}.
     */
    public BankDataLoaderCh() {
        super(DEFAULT_SOURCE_URI);
    }

    @Override
    protected boolean includesCountry(String country) {
        return !BankDataLoaderLi.COUNTRY_CODE.equals(country);
    }

}
