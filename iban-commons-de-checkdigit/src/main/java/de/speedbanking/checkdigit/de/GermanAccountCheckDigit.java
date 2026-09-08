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
package de.speedbanking.checkdigit.de;

import static java.util.Objects.requireNonNull;

/**
 * Public entry point for verifying German domestic account number ("Kontonummer")
 * check digits.
 * <p>
 * This class only <em>executes</em> a check digit method — it does not determine which
 * method applies to a given Bankleitzahl (BLZ). Callers must supply the method code
 * themselves, typically obtained by looking up the BLZ in the Bundesbank's
 * "Bankleitzahlendatei" (a separate concern, deliberately not part of this module).
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * CheckDigitResult result = GermanAccountCheckDigit.verify("00", "10000000", "0532013000");
 * if (result.isValid()) {
 *     // structurally consistent (or the method performs no verification)
 * }
 * }</pre>
 *
 * @since 1.9.0
 *
 * @see GermanCheckDigitMethod
 */
public final class GermanAccountCheckDigit {

    private static final int BLZ_LENGTH = 8;

    private GermanAccountCheckDigit() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Verifies the check digit of {@code accountNumber} using the method identified by
     * {@code methodCode}.
     *
     * @param methodCode    the two-character Bundesbank method code, e.g. {@code "00"};
     *                      must not be {@code null}
     * @param blz           the 8-digit Bankleitzahl, digits only (whitespace is stripped);
     *                      must not be {@code null}
     * @param accountNumber the account number, 1 to 10 digits, digits only (whitespace is
     *                      stripped); shorter numbers are treated as right-justified and
     *                      zero-padded, per the Bundesbank specification;
     *                      must not be {@code null}
     * @return the verification outcome
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code methodCode} is not a known, implemented
     *                                  method, or if {@code blz} / {@code accountNumber}
     *                                  are not purely numeric or exceed their maximum length
     */
    public static CheckDigitResult verify(String methodCode, String blz, String accountNumber) {
        GermanCheckDigitMethod method = GermanCheckDigitMethod.fromCode(requireNonNull(methodCode, "methodCode must not be null"));

        char[] blzDigits = normalize(blz, "blz", BLZ_LENGTH);
        char[] accountDigits = normalize(accountNumber, "accountNumber", GermanCheckDigitMethod.ACCOUNT_LENGTH);

        return method.calculate(blzDigits, accountDigits);
    }

    /**
     * Strips whitespace, validates that only digits remain, and zero-pads (left) to
     * exactly {@code length} characters.
     *
     * @param value     the raw input; must not be {@code null}
     * @param fieldName used only for the exception message
     * @param length    the required, fixed output length
     * @return the normalized digits as a {@code char[]} of exactly {@code length} characters
     */
    private static char[] normalize(String value, String fieldName, int length) {
        requireNonNull(value, fieldName + " must not be null");

        StringBuilder digitsOnly = new StringBuilder(length);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isWhitespace(c)) {
                digitsOnly.append(c);
            }
        }

        if (digitsOnly.length() == 0 || digitsOnly.length() > length) {
            throw new IllegalArgumentException(
                fieldName + " must contain between 1 and " + length + " digits, was: '" + value + "'");
        }

        char[] result = new char[length];
        int offset = length - digitsOnly.length();
        for (int i = 0; i < offset; i++) {
            result[i] = '0';
        }
        for (int i = 0; i < digitsOnly.length(); i++) {
            char c = digitsOnly.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException(fieldName + " must contain only digits, was: '" + value + "'");
            }
            result[offset + i] = c;
        }
        return result;
    }

}
