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

import static java.util.Objects.requireNonNull;

import de.speedbanking.checkdigit.de.CheckDigitResult;
import de.speedbanking.checkdigit.de.GermanAccountCheckDigit;
import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanConfig;
import de.speedbanking.iban.IbanValidationError;
import de.speedbanking.iban.InvalidIbanException;
import de.speedbanking.util.Country;
import de.speedbanking.util.UtilityClasses;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Optional;

/**
 * An immutable, validated German IBAN.
 * <p>
 * Parsing works like that of {@link Iban}, restricted to IBANs with the country code {@code DE}.
 * {@link #toIban()} gives access to the usual {@link Iban} API, and {@link #getAccountCheckResult()}
 * checks the account number against the check digit method the Bundesbank publishes for the bank.
 * <pre>{@code
 * GermanIban iban = GermanIban.of("DE89370400440532013000");
 * GermanAccountCheckResult result = iban.getAccountCheckResult(); // VALID
 * }</pre>
 *
 * @since 1.8.12
 */
public final class GermanIban implements Serializable {

    private static final long    serialVersionUID               = 1L;

    /** Maven groupId and root package name alike. */
    private static final String  SPEEDBANKING                   = "de.speedbanking";
    private static final String  CHECK_DIGIT_MODULE_COORDINATES = SPEEDBANKING + ":iban-commons-de-checkdigit";
    private static final String  CHECK_DIGIT_MODULE_CLASS       = SPEEDBANKING + ".checkdigit.de.GermanAccountCheckDigit";

    private final Iban           iban;

    /**
     * Creates a German IBAN from an IBAN already known to be German.
     */
    private GermanIban(Iban iban) {
        this.iban = iban;
    }

    /**
     * Parses and validates the input character sequence as a German IBAN.
     *
     * @param iban the IBAN character sequence, may include spaces if {@link IbanConfig#isAllowSpace()} is {@code true}
     * @return a valid, immutable {@code GermanIban} instance
     * @throws InvalidIbanException if the input is not a valid IBAN, or not a German one
     */
    public static GermanIban of(CharSequence iban) throws InvalidIbanException {
        return of(Iban.of(iban));
    }

    /**
     * Returns the given IBAN as a German IBAN.
     *
     * @param iban a valid IBAN; must not be {@code null}
     * @return the German IBAN
     * @throws InvalidIbanException if the IBAN is not a German one
     */
    public static GermanIban of(Iban iban) throws InvalidIbanException {
        requireNonNull(iban, "iban must not be null");
        if (!isGerman(iban)) {
            throw InvalidIbanException.of(IbanValidationError.INVALID_COUNTRY, iban, iban.getCountryCode());
        }
        return new GermanIban(iban);
    }

    /**
     * Attempts to parse and validate the input character sequence as a German IBAN.
     *
     * @param iban the IBAN character sequence, may include spaces if {@link IbanConfig#isAllowSpace()} is {@code true}
     * @return an {@link Optional} containing the German IBAN, or empty if the input is not a valid
     *         German IBAN
     */
    public static Optional<GermanIban> tryParse(CharSequence iban) {
        return Iban.tryParse(iban).filter(GermanIban::isGerman).map(GermanIban::new);
    }

    /**
     * Checks whether the input character sequence is a valid German IBAN.
     *
     * @param iban the IBAN character sequence, may include spaces if {@link IbanConfig#isAllowSpace()} is {@code true}
     * @return {@code true} if the input is a valid German IBAN, {@code false} otherwise
     */
    public static boolean isValid(CharSequence iban) {
        return tryParse(iban).isPresent();
    }

    /**
     * Returns whether the given IBAN is a German one.
     */
    private static boolean isGerman(Iban iban) {
        return Country.DE.getCode().equals(iban.getCountryCode());
    }

    /**
     * Checks the account number against the check digit method the Bundesbank publishes for the
     * bank, taken from {@link BankDataLookup}.
     * <p>
     * Needs {@code iban-commons-de-checkdigit} on the classpath, which this module declares as an
     * optional dependency.
     *
     * @return the outcome of the check
     * @throws IllegalStateException if {@code iban-commons-de-checkdigit} is not on the classpath; the
     *         message names its Maven coordinates
     */
    public GermanAccountCheckResult getAccountCheckResult() {
        if (!CheckDigitModule.PRESENT) {
            throw new IllegalStateException(String.format(
                "Checking a German account number requires the optional module %s on the classpath (class %s not found)",
                CHECK_DIGIT_MODULE_COORDINATES, CHECK_DIGIT_MODULE_CLASS));
        }
        return checkAccount();
    }

    /**
     * Verifies the account number with the check digit method of the bank. The classes of the
     * optional module are only resolved when this method first runs, after the check in
     * {@link #getAccountCheckResult()}.
     */
    private GermanAccountCheckResult checkAccount() {
        Optional<BankData> bank = BankDataLookup.byIban(iban);
        if (!bank.isPresent()) {
            return GermanAccountCheckResult.BANK_CODE_UNKNOWN;
        }
        Optional<String> method = bank.get().checkDigitMethod();
        if (!method.isPresent()) {
            return GermanAccountCheckResult.METHOD_UNKNOWN;
        }

        CheckDigitResult result;
        try {
            result = GermanAccountCheckDigit.verify(method.get(), iban.getBankCode(), iban.getAccountNumber());
        } catch (IllegalArgumentException ignored) {
            return GermanAccountCheckResult.METHOD_NOT_IMPLEMENTED;
        }
        if (!result.isChecked()) {
            return GermanAccountCheckResult.NOT_CHECKED;
        }
        return result.isValid() ? GermanAccountCheckResult.VALID : GermanAccountCheckResult.INVALID;
    }

    /**
     * Returns this German IBAN as a plain {@link Iban}.
     *
     * @return the IBAN
     */
    public Iban toIban() {
        return iban;
    }

    /**
     * Returns the normalized IBAN string, without spaces.
     *
     * @return the normalized IBAN, e.g. {@code "DE89370400440532013000"}
     */
    @Override
    public String toString() {
        return iban.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return iban.equals(((GermanIban) o).iban);
    }

    @Override
    public int hashCode() {
        return iban.hashCode();
    }

    /**
     * Re-checks the country after deserialization, the {@link Iban} itself re-validates on its own.
     *
     * @return this instance
     * @throws ObjectStreamException if the deserialized IBAN is not a German one
     */
    private Object readResolve() throws ObjectStreamException {
        if (iban == null || !isGerman(iban)) {
            throw new InvalidObjectException("Not a German IBAN");
        }
        return this;
    }

    /**
     * Initialization-on-demand holder: the classpath is only probed for the optional module on the
     * first account check, once, without explicit synchronization.
     */
    private static final class CheckDigitModule {

        static final boolean PRESENT = isPresent();

        private CheckDigitModule() {
            throw UtilityClasses.cannotInstantiate(getClass());
        }

        /**
         * Returns whether the entry class of the optional module can be loaded, without initializing it.
         */
        private static boolean isPresent() {
            try {
                Class.forName(CHECK_DIGIT_MODULE_CLASS, false, GermanIban.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError ignored) {
                return false;
            }
        }

    }

}
