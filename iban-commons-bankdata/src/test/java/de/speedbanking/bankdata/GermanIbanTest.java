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

import static java.nio.charset.StandardCharsets.US_ASCII;

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
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

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

    @ParameterizedTest(name = "[{index}] method {1} -> {2}")
    @CsvSource(nullValues = "null", value = {
        "DE89370400440532013000, 13,   VALID",
        "DE74370400440542013000, 13,   INVALID",
        "DE89370400440532013000, 09,   NOT_CHECKED",
        "DE89370400440532013000, null, METHOD_UNKNOWN",
        "DE89370400440532013000, Z9,   METHOD_NOT_IMPLEMENTED",
    })
    void getAccountCheckResult_ownBankData_returnsOutcome(String iban, String method, GermanAccountCheckResult expected) {
        BankData bank = BankData.builder("DE", "37040044", "Commerzbank", "test").checkDigitMethod(method).build();

        assertThat(GermanIban.of(iban).getAccountCheckResult(bank)).isEqualTo(expected);
    }

    @Test
    void getAccountCheckResult_noBankData_returnsBankCodeUnknown() {
        assertThat(GermanIban.of("DE89370400440532013000").getAccountCheckResult(null))
            .isEqualTo(GermanAccountCheckResult.BANK_CODE_UNKNOWN);
    }

    @ParameterizedTest(name = "[{index}] {0}:{1}")
    @CsvSource({
        "DE, 10000000",  // another German bank
        "AT, 37040044",  // same bank code, other country
    })
    void getAccountCheckResult_bankDataOfOtherBank_throwsIllegalArgumentException(String countryCode, String bankCode) {
        BankData bank = BankData.builder(countryCode, bankCode, "Some bank", "test").checkDigitMethod("13").build();
        GermanIban iban = GermanIban.of("DE89370400440532013000");

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> iban.getAccountCheckResult(bank))
            .withMessageContaining(bankCode);
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

    @Test
    void deserialization_streamWithNonGermanIban_throwsInvalidObjectException() throws Exception {
        // a tampered stream: the German IBAN replaced by a valid British one of the same length,
        // which the Iban itself accepts, so only GermanIban's own country check can reject it
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(GermanIban.of("DE89370400440532013000"));
        }
        byte[] tampered = replace(bytes.toByteArray(), "DE89370400440532013000", "GB82WEST12345698765432");

        assertThatExceptionOfType(InvalidObjectException.class)
            .isThrownBy(() -> {
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(tampered))) {
                    in.readObject();
                }
            })
            .withMessage("Not a German IBAN");
    }

    @Test
    void equals_sameInstanceNullOtherTypeOrOtherIban_comparesByIban() {
        GermanIban iban = GermanIban.of("DE89370400440532013000");

        assertThat(iban.equals(iban)).isTrue();
        assertThat(iban.equals(null)).isFalse();
        assertThat(iban.equals((Object) iban.toIban())).isFalse();
        assertThat(iban).isNotEqualTo(GermanIban.of("DE02120300000000202051"));
    }

    /**
     * Replaces the single occurrence of an ASCII string in a byte array with another of the same length.
     */
    private static byte[] replace(byte[] data, String search, String replacement) {
        byte[] searchBytes = search.getBytes(US_ASCII);
        byte[] result = data.clone();
        for (int i = 0; i <= result.length - searchBytes.length; i++) {
            if (Arrays.equals(Arrays.copyOfRange(result, i, i + searchBytes.length), searchBytes)) {
                System.arraycopy(replacement.getBytes(US_ASCII), 0, result, i, searchBytes.length);
                return result;
            }
        }
        throw new AssertionError(String.format("%s not found in serialized data", search));
    }

}
