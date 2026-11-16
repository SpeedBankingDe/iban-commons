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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loader for Greek banks, sourced from the ECB's monthly list of financial institutions.
 * <p>
 * See {@link AbstractBankDataLoaderEcb} for the shared source format. For Greece the
 * {@code RIAD_CODE} carries the 3-digit Hellenic bank code, checked against known codes
 * ({@code GR011} National Bank of Greece, {@code GR014N} Alpha Bank, {@code GR017N} Piraeus Bank,
 * {@code GR026N} Eurobank). Some codes have a trailing {@code N}, which is dropped.
 *
 * @since 1.8.15
 */
public final class BankDataLoaderGr extends AbstractBankDataLoaderEcb {

    /** Default remote source URI: the ECB's list-of-financial-institutions landing page. */
    public static final URI      DEFAULT_SOURCE_URI = BankDataLoaderDefaults.sourceUri("GR");

    private static final Pattern RIAD_SUFFIX        = Pattern.compile("(\\d{3})N?");

    /**
     * Creates a loader for the default remote source {@link #DEFAULT_SOURCE_URI}.
     */
    public BankDataLoaderGr() {
        super(DEFAULT_SOURCE_URI);
    }

    /**
     * Drops the optional trailing {@code N}, skips anything that is not a 3-digit code.
     */
    @Override
    protected String toBankCode(String riadSuffix) {
        Matcher matcher = RIAD_SUFFIX.matcher(riadSuffix);
        return matcher.matches() ? matcher.group(1) : null;
    }

}
