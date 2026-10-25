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
package de.speedbanking.iban;

import static de.speedbanking.iban.IbanComponent.IbanComponentType.ACCOUNT_NUMBER;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.ACCOUNT_TYPE;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.ACCOUNT_TYPE_AND_CONTROL;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.BANK_CODE;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.BBAN;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.BRANCH_CODE;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.IDENTIFICATION_NUMBER;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.NATIONAL_CODE;

import static java.util.Objects.requireNonNull;

import de.speedbanking.iban.IbanComponent.IbanComponentType;
import de.speedbanking.iban.IbanComponent.PositionTypes;
import de.speedbanking.iban.util.IbanCharType;
import de.speedbanking.util.CharUtil;
import de.speedbanking.util.Mod97;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Standard IBAN builder for account structures consisting of bank code and account number (e.g., DE, AT).
 * <p>
 * Serves as the base class for more specialized country builders. Direct instantiation is restricted;
 * instances must be obtained via {@link IbanRegistry#builder()}.
 *
 * @param <B> self-type of the concrete builder subclass
 *
 * @since 1.8.9
 */
public class IbanBuilder<B extends IbanBuilder<B>> {

    /** Marks a BBAN position that no component has written yet, see {@link #build()}. */
    private static final char UNSET = '\0';

    /**
     * Per-thread working buffers of {@link #build()}, one per country, indexed by the ordinal of
     * the {@link IbanRegistry} entry and each exactly as long as the country's IBAN. The array
     * and each buffer are created lazily on first use, a buffer is prepared for reuse at the
     * start of every call (see {@link #buildBuffer(IbanComponent)}). A buffer never leaves
     * {@code build()}: the result is copied into a {@link String} before returning.
     * <p>
     * The thread-local value holds JDK types only, so a pooled thread outliving the application
     * does not keep this library's class loader alive; this is why the buffers are not kept in
     * a map keyed by {@link IbanRegistry}. Being bounded per thread, they are never removed.
     */
    @SuppressWarnings("java:S5164") // small, bounded per-thread buffers of JDK types only, see above
    private static final ThreadLocal<char[][]> BUILD_BUFFERS = ThreadLocal
        .withInitial(() -> new char[IbanRegistry.values().length][]);

    /** The registry entry defining this builder's country structure, never {@code null}. */
    private final IbanRegistry countryData;

    /** Lazily initialized or explicitly injected random instance. */
    private Random             random;

    /** The bank code set via {@link #bankCode(String)}, or {@code null} for random generation. */
    private String             bankCode;
    /** The account number set via {@link #accountNumber(String)}, or {@code null} for random generation. */
    private String             accountNumber;

    /**
     * Package-private constructor to enforce instantiation via {@link IbanRegistry#builder()}.
     *
     * @param countryData the registry entry defining the country structure, must not be null
     */
    IbanBuilder(IbanRegistry countryData) {
        this.countryData = requireNonNull(countryData, "country");
    }

    /**
     * Helper method to cast 'this' to the concrete builder subclass type {@code B}.
     *
     * @return this builder instance cast to type B
     */
    @SuppressWarnings("unchecked")
    protected final B self() {
        return (B) this;
    }

    /**
     * Returns the registry entry defining this builder's country structure.
     *
     * @return the country data, never {@code null}
     */
    protected final IbanRegistry getCountryData() {
        return countryData;
    }

    /**
     * Validates that {@code actual} is either the {@code expectedBase} country entry or a derived country based on it.
     * <p>
     * Intended as a guard for builder subclasses whose custom BBAN component (pattern and position)
     * is hardcoded for a single base country and therefore cannot honor an arbitrary {@link IbanRegistry}
     * argument; such a guard turns an accidental mis-wiring into an immediate, clear failure instead of
     * silently producing an invalid IBAN.
     * <p>
     * Currently unused within this class: none of the builders defined here are hardcoded to a single
     * country anymore (see {@link NationalCodeIbanBuilderWithBranchCode} and its siblings, which accept
     * any {@link IbanRegistry} whose structure defines the relevant component). Kept as package-private
     * infrastructure for a future builder that does need this guard; remove if no other caller exists.
     *
     * @param actual   the country data passed to the builder constructor, must not be null
     * @param expected the single base country this builder subclass supports, must not be null
     * @return {@code actual}, unchanged
     * @throws IllegalArgumentException if {@code actual} is neither {@code expectedBase} nor a country derived from it
     */
    static IbanRegistry requireCountry(IbanRegistry actual, IbanRegistry expected) {
        requireNonNull(actual, "countryData must not be null");
        if (actual == expected || (actual.isDerivedCountry() && actual.getBaseCountry() == expected)) {
            return actual;
        }
        throw new IllegalArgumentException("This builder only supports " + expected.getCountryCode()
        + " or derived countries but was constructed with country data for " + actual.getCountryCode());
    }

    /**
     * Returns the configured {@link Random} instance, falling back to {@link ThreadLocalRandom#current()}
     * to avoid unnecessary synchronization overhead and allocation costs.
     *
     * @return the random instance, never {@code null}
     */
    protected final Random getRandom() {
        return random != null ? random : ThreadLocalRandom.current();
    }

    /**
     * Sets a custom random instance for value generation.
     *
     * @param random the random instance, must not be null
     * @return this builder instance
     */
    public final B withRandom(Random random) {
        this.random = requireNonNull(random, "random");
        return self();
    }

    /**
     * Sets the bank code for the IBAN.
     *
     * @param bankCode the bank code, may be null
     * @return this builder instance
     */
    public final B bankCode(String bankCode) {
        this.bankCode = bankCode;
        return self();
    }

    /**
     * Sets the account number for the IBAN.
     *
     * @param accountNumber the account number, may be null
     * @return this builder instance
     */
    public final B accountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return self();
    }

    /**
     * Builds and returns a fully formatted and valid IBAN instance.
     *
     * @return the constructed IBAN
     * @throws InvalidIbanException if a component value does not match its pattern, or if the
     *                              assembled IBAN unexpectedly fails structural validation
     */
    public final Iban build() {
        // Start from country code, "00" check-digit placeholder (fixed up below) and an unset
        // BBAN. The bank code and account number ranges are written right away;
        // appendSubclassComponents() writes any further country-specific sub-ranges. BBAN
        // positions this library does not expose as a settable component are filled with random,
        // pattern-valid content afterwards, so countries with a not fully modeled BBAN still
        // produce a structurally valid IBAN, without generating characters that a component
        // overwrites anyway.
        IbanComponent bban = getCountryData().getStructureData().getComponent(BBAN);
        char[] iban = buildBuffer(bban);

        resolveComponent(iban, getCountryData().getBankCodeComponent(), bankCode);
        resolveComponent(iban, getCountryData().getAccountNumberComponent(), accountNumber);

        appendSubclassComponents(iban);

        fillUnsetPositions(iban, bban);

        // always calculate NCD regardless of IbanConfig#isCalculateNcd
        fixNationalCheckDigit(getCountryData(), iban);

        fixCheckDigits(iban);

        // fixCheckDigits() already ran a full structural validation and just installed the
        // correct checksum, so the result is provably valid at this point. Re-parsing it via
        // Iban.of() would repeat that same structural + checksum validation for no benefit;
        // constructing directly (this class shares Iban's package) skips that redundant pass.
        return new Iban(new String(iban), getCountryData());
    }

    /**
     * Returns this thread's working buffer for the builder's country, holding the country code,
     * the {@code "00"} check-digit placeholder and an {@link #UNSET} BBAN.
     * <p>
     * The country code is written once when the buffer is created. The check digits are reset so
     * that a national check digit calculator never sees those of the previous IBAN, and the BBAN
     * is reset since unset positions are filled with fresh random characters later and must not
     * keep the content of the previous IBAN.
     */
    @SuppressWarnings("EnumOrdinal")
    private char[] buildBuffer(IbanComponent bban) {
        int index = countryData.ordinal();
        char[][] buffers = BUILD_BUFFERS.get();
        char[] buffer = buffers[index];
        if (buffer == null) {
            buffer = new char[countryData.getIbanLength()];
            String countryCode = countryData.getBaseCountry().getCountryCode();
            buffer[0] = countryCode.charAt(0);
            buffer[1] = countryCode.charAt(1);
            buffers[index] = buffer;
        }
        buffer[IbanRegistry.INDEX_CHECK_DIGIT1] = '0';
        buffer[IbanRegistry.INDEX_CHECK_DIGIT2] = '0';
        Arrays.fill(buffer, bban.getBeginIndex(), bban.getEndIndex(), UNSET);
        return buffer;
    }

    /**
     * Extension hook for subclasses to insert additional BBAN components.
     * <p>
     * The provided {@code iban} array has the country's IBAN length and already contains the
     * resolved bank code and account number components. BBAN positions that no component writes
     * are filled with random, pattern-valid characters after this hook returns.
     *
     * @param iban the in-progress IBAN, modified in place
     * @return the {@code iban} argument, potentially modified
     */
    protected char[] appendSubclassComponents(char[] iban) {
        // default implementation returns the argument
        return iban;
    }

    /**
     * Helper overload primarily for testing. Resolves a component into a new array of the
     * country's IBAN length, pre-filled with spaces.
     *
     * @param ibanComponent the IBAN component specification, must not be null
     * @param input         the input character sequence, may be null
     * @return a new array containing the resolved component at its target position
     */
    char[] resolveComponent(IbanComponent ibanComponent, CharSequence input) {
        char[] iban = new char[countryData.getIbanLength()];
        Arrays.fill(iban, ' ');
        return resolveComponent(iban, ibanComponent, input);
    }

    /**
     * Resolves an IBAN component directly into the provided target array, either from the given
     * input or, if there is none, with random characters matching the component's pattern.
     *
     * @param target        the IBAN to modify in place, must not be null
     * @param ibanComponent the IBAN component specification, must not be null
     * @param input         the input character sequence, may be null
     * @return the {@code target} argument
     */
    char[] resolveComponent(char[] target, IbanComponent ibanComponent, CharSequence input) {
        requireNonNull(target, "target must not be null");
        requireNonNull(ibanComponent, "component must not be null");

        PositionTypes types = ibanComponent.getPositionTypes();
        int beginIndex = ibanComponent.getBeginIndex();

        if (input == null) {
            Random rng = getRandom();
            for (int i = 0; i < types.length(); i++) {
                target[beginIndex + i] = randomChar(types.get(i), rng);
            }
        } else {
            writeInputComponent(target, ibanComponent, types, input);
        }
        return target;
    }

    /**
     * Fills every BBAN position that is still {@link #UNSET} with a random character of the
     * position's character type.
     */
    private void fillUnsetPositions(char[] target, IbanComponent bban) {
        PositionTypes types = bban.getPositionTypes();
        int beginIndex = bban.getBeginIndex();
        Random rng = null;
        for (int i = 0; i < types.length(); i++) {
            if (target[beginIndex + i] == UNSET) {
                if (rng == null) {
                    rng = getRandom();
                }
                target[beginIndex + i] = randomChar(types.get(i), rng);
            }
        }
    }

    /**
     * Returns a random character of the given character type.
     */
    private static char randomChar(IbanCharType type, Random rng) {
        switch (type) {
            case NUMERIC:
                return SourceChars.NUMERIC.nextChar(rng);
            case ALPHABETIC:
                return SourceChars.ALPHABETIC.nextChar(rng);
            default:
                return SourceChars.ALPHANUMERIC.nextChar(rng);
        }
    }

    /**
     * Returns whether {@code ch} is allowed at a position of the given character type.
     */
    private static boolean matches(IbanCharType type, char ch) {
        switch (type) {
            case NUMERIC:
                return CharUtil.isDigit(ch);
            case ALPHABETIC:
                return CharUtil.isUpperCase(ch);
            case ALPHANUMERIC:
                return CharUtil.isDigitOrUpperCase(ch);
            default:
                return ch == ' ';
        }
    }

    /**
     * Validates the given input position by position against the component's character types,
     * as if it were left-padded with {@code '0'} where the component allows that, then writes
     * the padding and the input characters directly into the target buffer.
     */
    private void writeInputComponent(char[] target, IbanComponent ibanComponent, PositionTypes types, CharSequence input) {
        int inputLen = input.length();
        int paddingLen = types.length() - inputLen;
        boolean valid = inputLen > 0 && paddingLen >= 0 && (paddingLen == 0 || types.isZeroPaddable());
        for (int i = 0; valid && i < inputLen; i++) {
            valid = matches(types.get(paddingLen + i), input.charAt(i));
        }
        if (!valid) {
            throw InvalidIbanException.of(errorFor(ibanComponent.getType()), input, getCountryData().getCountryCode());
        }

        int beginIndex = ibanComponent.getBeginIndex();
        Arrays.fill(target, beginIndex, beginIndex + paddingLen, '0');
        for (int i = 0; i < inputLen; i++) {
            target[beginIndex + paddingLen + i] = input.charAt(i);
        }
    }

    /**
     * Maps a structural {@link IbanComponentType} to the {@link IbanValidationError} to report
     * when its resolved value fails pattern validation.
     * <p>
     * Only {@code BBAN}, {@code BANK_CODE}, {@code BRANCH_CODE} and {@code ACCOUNT_NUMBER} have a
     * dedicated error; every other component type (national check digit, and the various
     * country-specific custom components such as account type or identification number) shares
     * the generic {@link IbanValidationError#INVALID_STRUCTURE}, since none of them warrants its
     * own error code.
     *
     * @param type the component type to look up, must not be null
     * @return the validation error to use for this component type
     */
    static IbanValidationError errorFor(IbanComponentType type) {
        switch (type) {
            case BANK_CODE:
                return IbanValidationError.INVALID_BANK_CODE;
            case BRANCH_CODE:
                return IbanValidationError.INVALID_BRANCH_CODE;
            case ACCOUNT_NUMBER:
                return IbanValidationError.INVALID_ACCOUNT_NUMBER;
            default:
                return IbanValidationError.INVALID_STRUCTURE;
        }
    }

    /**
     * Validates the structure of the given IBAN and overwrites its ISO 7064 Mod 97-10 check
     * digits (index 2 and 3) with the correct value, in place.
     * <p>
     * The check digits are first set to {@code "00"} to calculate the required remainder
     * {@code R}, then the final check digits {@code CD = 98 - R} are written. Their previous
     * content is irrelevant and is not validated.
     *
     * @param iban the normalized IBAN, exactly as long as its country's IBAN; modified in place
     * @return the {@code iban} argument with the correct check digits applied
     * @throws InvalidIbanException if {@code iban} is {@code null} or fails structural validation
     */
    static char[] fixCheckDigits(char[] iban) {
        if (iban == null) {
            throw InvalidIbanException.of(IbanValidationError.EMPTY);
        }

        if (iban.length > IbanRegistry.INDEX_CHECK_DIGIT2) {
            iban[IbanRegistry.INDEX_CHECK_DIGIT1] = '0';
            iban[IbanRegistry.INDEX_CHECK_DIGIT2] = '0';
        }

        IbanValidationError error = IbanValidator.validateStructure(iban);
        if (error != null) {
            throw InvalidIbanException.of(error, new String(iban));
        }

        // calculate the required check digits value (98 - modulo result)
        int checkDigitsValue = Mod97.MAX_CHECK_DIGIT_VALUE - Mod97.calculate(iban);

        // manual zero-padding: faster than String.format
        iban[IbanRegistry.INDEX_CHECK_DIGIT1] = (char) ('0' + (checkDigitsValue / 10));
        iban[IbanRegistry.INDEX_CHECK_DIGIT2] = (char) ('0' + (checkDigitsValue % 10));

        return iban;
    }

    /**
     * Overwrites the National Check Digit (NCD) field in {@code ibanBuilder} with the value
     * computed by the country's {@link NationalCheckDigitCalculator}.
     * <p>
     * This method is a no-op when the country has no NCD field
     * ({@link IbanRegistry#getNationalCheckDigitComponent()} returns {@code null}).
     *
     * @param countryData the registry entry for the country, may not be {@code null}
     * @param iban        the IBAN with a completely written BBAN; modified in place; may not
     *                    be {@code null}
     * @return {@code iban}, unmodified if no NCD field is defined for the country,
     *         otherwise with the NCD field overwritten in place
     */
    static char[] fixNationalCheckDigit(IbanRegistry countryData, char[] iban) {
        requireNonNull(countryData, "countryData must not be null");
        requireNonNull(iban, "iban must not be null");

        if (iban.length != countryData.getIbanLength()) {
            throw InvalidIbanException.of(IbanValidationError.INCORRECT_LENGTH_COUNTRY, new String(iban), countryData.getCountryCode());
        }

        // only countries with a registered NCD field are relevant
        IbanComponent ncdCompo = countryData.getNationalCheckDigitComponent();
        if (ncdCompo == null) {
            return iban;
        }

        char[] ncd = IbanValidator.getCountryValidator(countryData).calculateNationalCheckDigit(iban);
        System.arraycopy(ncd, 0, iban, ncdCompo.getBeginIndex(), ncd.length);

        return iban;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder(128)
            .append(getClass().getSimpleName())
            .append('[')
            .append("country=").append(getCountryData().getCountryCode())
            .append(", ").append(BANK_CODE.getLabel()).append('=').append(bankCode)
            .append(", ").append(ACCOUNT_NUMBER.getLabel()).append('=').append(accountNumber);
        appendToString(sb);
        return sb.append(']').toString();
    }
    /**
     * Appends state attributes of this builder to the provided {@link StringBuilder}.
     * <p>
     * Subclasses must override this method to append their own fields, ensuring
     * to call {@code super.appendToString(sb)} first.
     *
     * @param builder the builder to append field representations to, must not be null
     */
    protected StringBuilder appendToString(StringBuilder builder) {
        return builder;
    }

    /**
     * Internal character pools used for generating random IBAN segment values.
     */
    private enum SourceChars {
        /** Digits {@code 0-9}. */
        NUMERIC("0123456789"),
        /** Upper-case letters {@code A-Z}. */
        ALPHABETIC("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        /** Digits and upper-case letters combined. */
        ALPHANUMERIC("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        private final String chars;
        private final int    length;

        /**
         * Creates a new character pool instance.
         *
         * @param chars the string containing all allowed characters for this pool
         */
        SourceChars(String chars) {
            this.chars = chars;
            this.length = chars.length();
        }

        /**
         * Returns a random character from this character set.
         *
         * @param random the random instance to use, must not be null
         * @return a randomly selected character
         */
        char nextChar(Random random) {
            return chars.charAt(random.nextInt(length));
        }
    }

    /**
     * Capability interface for IBAN builders that support setting a branch code component.
     *
     * @param <B> self-type of the concrete builder
     */
    @FunctionalInterface
    public interface HasBranchCode<B extends HasBranchCode<B>> {

        /**
         * Sets the branch code for the IBAN.
         *
         * @param branchCode the branch code, may be null
         * @return this builder instance
         */
        B branchCode(String branchCode);

    }

    /**
     * Capability interface for IBAN builders that support setting an account type component
     * (e.g. Bulgaria's BG single-character account type).
     *
     * @param <B> self-type of the concrete builder
     */
    @FunctionalInterface
    public interface HasAccountType<B extends HasAccountType<B>> {

        /**
         * Sets the account type for the IBAN.
         *
         * @param accountType the account type, may be null
         * @return this builder instance
         */
        B accountType(String accountType);

    }

    /**
     * Capability interface for IBAN builders that support setting a combined account type and
     * control component (e.g. Brazil's BR two-character account type/control field).
     *
     * @param <B> self-type of the concrete builder
     */
    @FunctionalInterface
    public interface HasAccountTypeAndControl<B extends HasAccountTypeAndControl<B>> {

        /**
         * Sets the combined account type and control characters for the IBAN.
         *
         * @param accountTypeAndControl the account type and control value, may be null
         * @return this builder instance
         */
        B accountTypeAndControl(String accountTypeAndControl);

    }

    /**
     * Capability interface for IBAN builders that support setting a national code component
     * (e.g. Algeria's DZ, Mauritius' MU, Poland's PL, Seychelles' SC or Togo's TG national code).
     *
     * @param <B> self-type of the concrete builder
     */
    @FunctionalInterface
    public interface HasNationalCode<B extends HasNationalCode<B>> {

        /**
         * Sets the national code for the IBAN.
         *
         * @param nationalCode the national code, may be null
         * @return this builder instance
         */
        B nationalCode(String nationalCode);

    }

    /**
     * Capability interface for IBAN builders that support setting an identification number
     * component (e.g. Iceland's IS identification number).
     *
     * @param <B> self-type of the concrete builder
     */
    @FunctionalInterface
    public interface HasIdentificationNumber<B extends HasIdentificationNumber<B>> {

        /**
         * Sets the identification number for the IBAN.
         *
         * @param identificationNumber the identification number, may be null
         * @return this builder instance
         */
        B identificationNumber(String identificationNumber);

    }

    /**
     * Standard concrete builder implementation for standard IBAN structures consisting only
     * of bank code and account number (e.g., DE, AT).
     * <p>
     * Serves as the default non-abstract implementation of {@link IbanBuilder} bound to itself
     * via the generic self-type pattern. Direct instantiation is package-private; instances
     * should be obtained via {@link IbanRegistry#builder()}.
     */
    public static class StandardIbanBuilder extends IbanBuilder<StandardIbanBuilder> {
        /**
         * Package-private constructor for standard country builder instances.
         *
         * @param countryData the registry entry defining the country structure, must not be null
         */
        StandardIbanBuilder(IbanRegistry countryData) {
            super(countryData);
        }

    }

    /**
     * Abstract base builder for IBAN structures containing a branch code.
     *
     * @param <B> the concrete builder subtype
     */
    protected abstract static class AbstractIbanBuilderWithBranchCode<B extends AbstractIbanBuilderWithBranchCode<B>>
            extends IbanBuilder<B>
            implements HasBranchCode<B> {

        private String branchCode;

        /**
         * @param countryData the registry entry defining the country structure, must not be null
         */
        AbstractIbanBuilderWithBranchCode(IbanRegistry countryData) {
            super(countryData);
        }

        /**
         * Sets the branch code for the IBAN.
         *
         * @param branchCode the branch code, may be null
         * @return this builder instance
         */
        @Override
        public B branchCode(String branchCode) {
            this.branchCode = branchCode;
            return self();
        }

        @Override
        protected char[] appendSubclassComponents(char[] iban) {
            return resolveComponent(iban, getCountryData().getStructureData().getComponent(BRANCH_CODE), branchCode);
        }

        @Override
        protected StringBuilder appendToString(StringBuilder sb) {
            return super.appendToString(sb)
                        .append(", ")
                        .append(BRANCH_CODE.getLabel())
                        .append('=')
                        .append(branchCode);
        }
    }

    /**
     * Concrete builder for IBAN structures consisting of bank code, branch code, and account number.
     */
    public static class IbanBuilderWithBranchCode extends AbstractIbanBuilderWithBranchCode<IbanBuilderWithBranchCode> {
        /**
         * Package-private constructor for standard branch-code country builder instances.
         *
         * @param countryData the registry entry defining the country structure, must not be null
         */
        IbanBuilderWithBranchCode(IbanRegistry countryData) {
            super(countryData);
        }
    }

    /**
     * Abstract base builder for IBAN structures containing a single custom BBAN component (without branch code).
     *
     * @param <B> the concrete builder subtype
     */
    protected abstract static class AbstractCustomIbanBuilder<B extends AbstractCustomIbanBuilder<B>>
            extends IbanBuilder<B> {

        private final IbanComponent ibanComponent;
        private String              customValue;

        /**
         * @param countryData   the registry entry defining the country structure, must not be null
         * @param componentType the custom component type this builder subclass supports; must be defined
         *                      in {@code countryData}'s structure (see {@link IbanRegistry#getComponent(IbanComponentType)})
         */
        protected AbstractCustomIbanBuilder(IbanRegistry countryData, IbanComponentType componentType) {
            super(countryData);
            requireNonNull(componentType, "componentType");
            this.ibanComponent = requireNonNull(countryData.getComponent(componentType),
                () -> "No " + componentType + " component defined for " + countryData.getCountryCode());
        }

        /**
         * Sets the custom component value.
         *
         * @param value the custom component value, may be null for random generation
         * @return this builder instance
         */
        protected final B customValue(String value) {
            this.customValue = value;
            return self();
        }

        @Override
        protected char[] appendSubclassComponents(char[] iban) {
            super.appendSubclassComponents(iban);
            return resolveComponent(iban, ibanComponent, customValue);
        }

        @Override
        protected StringBuilder appendToString(StringBuilder sb) {
            return super.appendToString(sb)
                        .append(", ").append(ibanComponent.getType().getLabel()).append('=').append(customValue);
        }
    }

    /**
     * Abstract base builder for IBAN structures containing both a branch code and a custom BBAN component.
     *
     * @param <B> the concrete builder subtype
     */
    protected abstract static class AbstractCustomIbanBuilderWithBranchCode<B extends AbstractCustomIbanBuilderWithBranchCode<B>>
            extends AbstractCustomIbanBuilder<B>
            implements HasBranchCode<B> {

        private String branchCode;

        /**
         * @param countryData   the registry entry defining the country structure, must not be null
         * @param componentType the custom component type this builder subclass supports; must be defined
         *                      in {@code countryData}'s structure (see {@link IbanRegistry#getComponent(IbanComponentType)})
         */
        protected AbstractCustomIbanBuilderWithBranchCode(IbanRegistry countryData, IbanComponentType componentType) {
            super(countryData, componentType);
        }

        /**
         * Sets the branch code for the IBAN.
         *
         * @param branchCode the branch code, may be null
         * @return this builder instance
         */
        @Override
        public B branchCode(String branchCode) {
            this.branchCode = branchCode;
            return self();
        }

        @Override
        protected char[] appendSubclassComponents(char[] iban) {
            resolveComponent(iban, getCountryData().getStructureData().getComponent(BRANCH_CODE), branchCode);
            return super.appendSubclassComponents(iban);
        }

        @Override
        protected StringBuilder appendToString(StringBuilder sb) {
            super.appendToString(sb);
            return sb.append(", ")
                     .append(BRANCH_CODE.getLabel())
                     .append('=')
                     .append(branchCode);
        }
    }

    /**
     * Concrete builder for IBAN structures consisting of bank code, account number, branch code
     * and an account type as the only country-specific custom BBAN component (e.g. BG).
     */
    public static class AccountTypeIbanBuilderWithBranchCode
            extends AbstractCustomIbanBuilderWithBranchCode<AccountTypeIbanBuilderWithBranchCode>
            implements HasAccountType<AccountTypeIbanBuilderWithBranchCode> {

        /**
         * Package-private constructor for account-type-with-branch-code country builder instances.
         *
         * @param countryData the registry entry defining the country structure, must not be null
         */
        AccountTypeIbanBuilderWithBranchCode(IbanRegistry countryData) {
            super(countryData, ACCOUNT_TYPE);
        }

        @Override
        public AccountTypeIbanBuilderWithBranchCode accountType(String accountType) {
            return customValue(accountType);
        }
    }

    /**
     * Concrete builder for IBAN structures consisting of bank code, account number, branch code
     * and a combined account type/control field as the only country-specific custom BBAN
     * component (e.g. BR).
     */
    public static class AccountTypeAndControlIbanBuilderWithBranchCode
            extends AbstractCustomIbanBuilderWithBranchCode<AccountTypeAndControlIbanBuilderWithBranchCode>
            implements HasAccountTypeAndControl<AccountTypeAndControlIbanBuilderWithBranchCode> {

        /**
         * Package-private constructor for account-type-and-control-with-branch-code country builder instances.
         *
         * @param countryData the registry entry defining the country structure, must not be null
         */
        AccountTypeAndControlIbanBuilderWithBranchCode(IbanRegistry countryData) {
            super(countryData, ACCOUNT_TYPE_AND_CONTROL);
        }

        @Override
        public AccountTypeAndControlIbanBuilderWithBranchCode accountTypeAndControl(String accountTypeAndControl) {
            return customValue(accountTypeAndControl);
        }
    }

    /**
     * Concrete builder for IBAN structures consisting of bank code, account number, branch code
     * and a national code as the only country-specific custom BBAN component
     * (e.g. DZ, MU, PL, SC, TG).
     */
    public static class NationalCodeIbanBuilderWithBranchCode
            extends AbstractCustomIbanBuilderWithBranchCode<NationalCodeIbanBuilderWithBranchCode>
            implements HasNationalCode<NationalCodeIbanBuilderWithBranchCode> {

        /**
         * Package-private constructor for national-code-with-branch-code country builder instances.
         *
         * @param countryData the registry entry defining the country structure, must not be null
         */
        NationalCodeIbanBuilderWithBranchCode(IbanRegistry countryData) {
            super(countryData, NATIONAL_CODE);
        }

        @Override
        public NationalCodeIbanBuilderWithBranchCode nationalCode(String nationalCode) {
            return customValue(nationalCode);
        }
    }

    /**
     * Concrete builder for IBAN structures consisting of bank code, branch code, account number and an
     * identification number (e.g. IS).
     */
    public static class IdentificationNumberIbanBuilderWithBranchCode
            extends AbstractCustomIbanBuilderWithBranchCode<IdentificationNumberIbanBuilderWithBranchCode>
            implements HasIdentificationNumber<IdentificationNumberIbanBuilderWithBranchCode> {

        /**
         * Package-private constructor for identification-number country builder instances.
         *
         * @param countryData the registry entry defining the country structure, must not be null
         */
        IdentificationNumberIbanBuilderWithBranchCode(IbanRegistry countryData) {
            super(countryData, IDENTIFICATION_NUMBER);
        }

        @Override
        public IdentificationNumberIbanBuilderWithBranchCode identificationNumber(String identificationNumber) {
            return customValue(identificationNumber);
        }
    }

}
