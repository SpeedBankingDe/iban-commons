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
 * Loader for Spanish banks, sourced from the ECB's monthly list of financial institutions.
 * <p>
 * See {@link AbstractBankDataLoaderEcb} for the shared source format and the verification that,
 * for Spain specifically, the file's {@code RIAD_CODE} equals the real {@code código de entidad}.
 *
 * @since 1.8.11
 */
public final class BankDataLoaderEs extends AbstractBankDataLoaderEcb {

    /** Default remote source URI: the ECB's list-of-financial-institutions landing page. */
    public static final URI DEFAULT_SOURCE_URI = BankDataLoaderDefaults.sourceUri("ES");

    public BankDataLoaderEs() {
        super(DEFAULT_SOURCE_URI);
    }

}
