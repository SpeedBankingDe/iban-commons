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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import de.speedbanking.iban.Iban;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

/**
 * Tests {@link GermanAccountCheck} against the bundled DE.csv, which carries the Bundesbank check digit
 * method of every bank code.
 */
@SuppressWarnings("checkstyle:MethodName")
final class GermanAccountCheckTest {

    @BeforeAll
    static void disableNetworkRefresh() {
        BankDataRegistry.setDownloaderFactoryForTesting(() -> (uri, connectTimeout, readTimeout) -> {
            throw new IOException("network access disabled in unit tests");
        });
    }

    @AfterAll
    static void restoreDefaultDownloader() {
        BankDataRegistry.resetForTesting();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource({
        "DE89370400440532013000, VALID",              // 37040044 Commerzbank, method 13
        "DE74370400440542013000, INVALID",            // same bank, wrong account check digit
        "DE42100000000001234567, NOT_CHECKED",        // 10000000 Bundesbank, method 09
        "DE43055020051272427798, BANK_CODE_UNKNOWN",  // valid IBAN checksum, bank code does not exist
    })
    void check_germanIban_returnsOutcome(String iban, GermanAccountCheckResult expected) {
        assertThat(GermanAccountCheck.check(Iban.of(iban))).isEqualTo(expected);
    }

    @Test
    void check_nonGermanIban_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> GermanAccountCheck.check(Iban.of("AT611904300234573201")))
            .withMessageContaining("AT");
    }
}
