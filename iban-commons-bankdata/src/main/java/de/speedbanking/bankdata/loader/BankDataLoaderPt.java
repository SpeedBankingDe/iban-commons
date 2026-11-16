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
import java.util.regex.Pattern;

/**
 * Loader for Portuguese banks, sourced from the ECB's monthly list of financial institutions.
 * <p>
 * See {@link AbstractBankDataLoaderEcb} for the shared source format. For Portugal the
 * {@code RIAD_CODE} carries the Banco de Portugal institution code without leading zeros, checked
 * against known codes ({@code PT35} Caixa Geral de Depositos, {@code PT33} Millennium BCP,
 * {@code PT7} Novo Banco, {@code PT10} Banco BPI). It is padded to the 4 digits used in the IBAN.
 * A few branches of foreign institutions are listed with their 9-digit tax number instead, these
 * are skipped.
 *
 * @since 1.8.15
 */
public final class BankDataLoaderPt extends AbstractBankDataLoaderEcb {

    /** Default remote source URI: the ECB's list-of-financial-institutions landing page. */
    public static final URI      DEFAULT_SOURCE_URI = BankDataLoaderDefaults.sourceUri("PT");

    private static final Pattern RIAD_SUFFIX        = Pattern.compile("\\d{1,4}");

    /**
     * Creates a loader for the default remote source {@link #DEFAULT_SOURCE_URI}.
     */
    public BankDataLoaderPt() {
        super(DEFAULT_SOURCE_URI);
    }

    /**
     * Pads a code of up to 4 digits with leading zeros, skips anything else.
     */
    @Override
    protected String toBankCode(String riadSuffix) {
        return RIAD_SUFFIX.matcher(riadSuffix).matches() ? String.format("%4s", riadSuffix).replace(' ', '0') : null;
    }

}
