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
import static org.assertj.core.api.Assertions.assertThatComparable;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanValidationError;
import de.speedbanking.iban.InvalidIbanException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Unit tests for {@link GermanIban}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class GermanIbanTest {

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
    void getAccountCheckResult_germanIban_returnsOutcome(String iban, GermanAccountCheckResult expected) {
        assertThat(GermanIban.of(iban).getAccountCheckResult()).isEqualTo(expected);
    }

    @Test
    void of_germanIban_wrapsIban() {
        GermanIban iban = GermanIban.of("DE89370400440532013000");

        assertThat(iban)
            .hasToString("DE89370400440532013000")
            .isEqualTo(GermanIban.of(Iban.of("DE89370400440532013000")))
            .hasSameHashCodeAs(GermanIban.of("DE89370400440532013000"));
        assertThatComparable(iban.toIban()).isEqualTo(Iban.of("DE89370400440532013000"));
    }

    @Test
    void of_nonGermanIban_throwsInvalidIbanException() {
        assertThatExceptionOfType(InvalidIbanException.class)
            .isThrownBy(() -> GermanIban.of("AT611904300234573201"))
            .matches(e -> e.getReason() == IbanValidationError.INVALID_COUNTRY, "reason INVALID_COUNTRY");
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource({
        "DE89370400440532013000, true",   // valid German IBAN
        "AT611904300234573201,   false",  // valid, but not German
        "DE89370400440532013001, false",  // wrong IBAN check digits
    })
    void isValid_input_returnsWhetherValidGermanIban(String input, boolean expected) {
        assertThat(GermanIban.isValid(input)).isEqualTo(expected);
        assertThat(GermanIban.tryParse(input).isPresent()).isEqualTo(expected);
    }

    @Test
    void serialization_roundTrip_returnsEqualGermanIban() throws Exception {
        GermanIban iban = GermanIban.of("DE89370400440532013000");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(iban);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertThat(in.readObject()).isEqualTo(iban);
        }
    }

}
