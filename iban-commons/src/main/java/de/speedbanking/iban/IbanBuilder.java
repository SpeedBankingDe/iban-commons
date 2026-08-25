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
import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.util.IndexRange;
import de.speedbanking.util.Mod97;
import de.speedbanking.util.PatternCache;

import java.util.List;
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
     * Intended as a guard for builder subclasses whose custom BBAN component (pattern and {@link IndexRange})
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
     */
    public final Iban build() {
        String countryCode = getCountryData().getBaseCountry().getCountryCode();
        int expectedLength = getCountryData().getIbanLength();

        // Seed with country code, "00" check-digit placeholder (fixed up below) and a random,
        // but pattern-valid, BBAN. The bank code and account number ranges are overwritten right
        // away; appendSubclassComponents() overwrites any further country-specific sub-ranges.
        // Any BBAN positions this library does not expose as a settable component are left with
        // their random, pattern-valid content, so countries with a not fully modeled BBAN still
        // produce a structurally valid IBAN.
        StringBuilder ibanBuilder = new StringBuilder(expectedLength)
            .append(countryCode)
            .append("00");

        resolveComponent(ibanBuilder, getCountryData().getStructureData().getComponent(BBAN), null);
        resolveComponent(ibanBuilder, getCountryData().getBankCodeComponent(), bankCode);
        resolveComponent(ibanBuilder, getCountryData().getAccountNumberComponent(), accountNumber);

        appendSubclassComponents(ibanBuilder);

        // safeguard against structural alteration in subclass hook
        if (ibanBuilder.length() != expectedLength) {
            throw new IllegalStateException("Subclass component modification of '" + ibanBuilder
                + "' resulted in invalid IBAN length: " + ibanBuilder.length()
                + " (expected: " + expectedLength + ")");
        }

        // always calculdate NCD regardless of IbanConfig#isCalculateNcd
        fixNationalCheckDigit(getCountryData(), ibanBuilder);

        fixCheckDigits(ibanBuilder);

        return Iban.of(ibanBuilder.toString());
    }

    /**
     * Extension hook for subclasses to insert additional BBAN components.
     * <p>
     * The provided {@code ibanBuilder} argument is already pre-populated with a default BBAN pattern
     * as well as the resolved bank code and account number components.
     *
     * @param ibanBuilder the in-progress IBAN string builder
     * @return the string builder argument, potentially modified
     */
    protected StringBuilder appendSubclassComponents(StringBuilder ibanBuilder) {
        // default implementation returns the argument
        return ibanBuilder;
    }

    /**
     * Helper overload primarily for testing. Resolves a component into a new StringBuilder
     * pre-filled with spaces up to the country's IBAN length.
     *
     * @param ibanComponent the IBAN component specification, must not be null
     * @param input         the input character sequence, may be null
     * @return a new StringBuilder instance containing the resolved component at its target position
     */
    StringBuilder resolveComponent(IbanComponent ibanComponent, CharSequence input) {
        int length = countryData.getIbanLength();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(' ');
        }

        return resolveComponent(sb, ibanComponent, input);
    }

    /**
     * Resolves an IBAN component directly into the provided {@link StringBuilder} target,
     * avoiding intermediate String allocations when generating random values or padding inputs.
     *
     * @param target        the IBAN string builder to mutate, must not be null
     * @param ibanComponent the IBAN component specification, must not be null
     * @param input         the input character sequence, may be null
     * @return the mutated target StringBuilder instance
     */
    StringBuilder resolveComponent(StringBuilder target, IbanComponent ibanComponent, CharSequence input) {
        requireNonNull(target, "target must not be null");
        requireNonNull(ibanComponent, "component must not be null");

        List<Segment> segments = IbanPatternConverter.parseSegments(ibanComponent.getPattern());
        int requiredLength = IbanPatternConverter.calculateTotalLength(segments);
        int beginIndex = ibanComponent.getBeginIndex();

        if (input == null) {
            // write random characters directly into the existing StringBuilder buffer
            Random rng = getRandom();
            int currentPos = beginIndex;
            for (int i = 0; i < segments.size(); i++) {
                Segment segment = segments.get(i);

                SourceChars sourceChars;
                if (segment.isNumeric()) {
                    sourceChars = SourceChars.NUMERIC;
                } else if (segment.isAlphabetic()) {
                    sourceChars = SourceChars.ALPHABETIC;
                } else {
                    sourceChars = SourceChars.ALPHANUMERIC;
                }

                int segmentLen = segment.getLength();

                for (int j = 0; j < segmentLen; j++) {
                    char ch = sourceChars.nextChar(rng);
                    if (currentPos < target.length()) {
                        target.setCharAt(currentPos, ch);
                    } else {
                        target.append(ch);
                    }
                    currentPos++;
                }
            }
        } else {
            int inputLen = input.length();
            if (inputLen > requiredLength) {
                throw InvalidIbanException.of(errorFor(ibanComponent.getType()), input, getCountryData().getCountryCode());
            }

            int paddingLen = requiredLength - inputLen;
            boolean canPad = paddingLen > 0 && IbanPatternConverter.allMatch(segments, Segment::isNumericOrAlphanumeric);

            // validate pattern against the input (or padded representation if padding will be applied)
            String regex = IbanPatternConverter.buildRegex(segments);
            CharSequence checkTarget = canPad ? padLeft(input.toString(), requiredLength, '0') : input;
            if (!PatternCache.getDefault().getPattern(regex).matcher(checkTarget).matches()) {
                throw InvalidIbanException.of(errorFor(ibanComponent.getType()), input, getCountryData().getCountryCode());
            }

            // in-place mutation of target buffer to avoid intermediate String allocation for padded result
            int currentPos = beginIndex;

            // 1. Write leading zero-padding directly into buffer if required
            if (canPad) {
                for (int i = 0; i < paddingLen; i++) {
                    if (currentPos < target.length()) {
                        target.setCharAt(currentPos, '0');
                    } else {
                        target.append('0');
                    }
                    currentPos++;
                }
            }

            // 2. Write input characters directly into buffer
            for (int i = 0; i < inputLen; i++) {
                char ch = input.charAt(i);
                if (currentPos < target.length()) {
                    target.setCharAt(currentPos, ch);
                } else {
                    target.append(ch);
                }
                currentPos++;
            }
        }
        return target;
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
     * Pads the specified string on the left with a given character until it reaches the target length.
     * <p>
     * If the input string is already equal to or longer than the target length, or if it is
     * {@code null} or empty, the original string is returned unchanged.
     *
     * @param str          the string to pad, may be {@code null} or empty
     * @param targetLength the desired total length of the padded string
     * @param padChar      the character used for left-padding
     * @return the left-padded string, or the original string if no padding is required
     */
    static String padLeft(String str, int targetLength, char padChar) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        int paddingLen = targetLength - str.length();
        if (paddingLen <= 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(targetLength);
        for (int i = 0; i < paddingLen; i++) {
            sb.append(padChar);
        }
        return sb.append(str).toString();
    }

    /**
     * Calculates the correct ISO 7064 Mod 97-10 check digits for the given IBAN string
     * and overwrites the digits at the check digit positions (index 2 and 3).
     * <p>
     * This method temporarily sets the check digits to {@code "00"} to calculate the
     * required remainder {@code R}, then determines the final check digits {@code CD = 98 - R}.
     * <p>
     * Note: If the input argument is an instance of {@link StringBuilder}, it will be
     * modified in-place to achieve zero-allocation performance.
     *
     * @param iban the IBAN character sequence (must already be of the full IBAN length,
     *             with placeholders at the check digit position); if a {@code StringBuilder}
     *             is passed, it is mutated in place, otherwise a copy is created
     * @return a {@code StringBuilder} instance with the correct check digits applied
     * @throws InvalidIbanException if the structural validation fails for reasons other than the checksum
     */
    static StringBuilder fixCheckDigits(CharSequence iban) {
        IbanValidationResult result = IbanValidator.validate(iban);

        if (result.error != null && result.error != IbanValidationError.INVALID_CHECKSUM) {
            throw InvalidIbanException.of(result.error, iban);
        }

        StringBuilder sb = iban instanceof StringBuilder ? (StringBuilder) iban : new StringBuilder(iban);

        if (result.isValid()) {
            return sb;
        }

        // set placeholders to "00" (required for correct calculation context)
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT1, '0');
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT2, '0');

        // calculate the required check digits value (98 - modulo result)
        int checkDigitsValue = 98 - Mod97.calculate(sb);

        // manual zero-padding: faster than String.format
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT1, (char) ('0' + (checkDigitsValue / 10)));
        sb.setCharAt(IbanRegistry.INDEX_CHECK_DIGIT2, (char) ('0' + (checkDigitsValue % 10)));

        return sb;
    }

    /**
     * Overwrites the National Check Digit (NCD) field in {@code ibanBuilder} with the value
     * computed by the country's {@link NationalCheckDigitCalculator}, if one is available.
     * <p>
     * This method is a no-op when:
     * <ul>
     *   <li>the country has no NCD field
     *       ({@link IbanRegistry#getNationalCheckDigitComponent()} returns {@code null}), or</li>
     *   <li>the country's {@link CountryValidator} does not implement
     *       {@link NationalCheckDigitCalculator}.</li>
     * </ul>
     *
     * @param countryData the registry entry for the country, may not be {@code null}
     * @param ibanBuilder the mutable IBAN string with {@code "00"} ISO check-digit placeholders
     *                    and a randomly generated BBAN; modified in-place; may not be {@code null}
     * @return {@code ibanBuilder}, unmodified if no NCD field is defined for the country
     *         or if the {@link CountryValidator} does not implement
     *         {@link NationalCheckDigitCalculator}; otherwise the NCD field is
     *         overwritten in-place and the same instance is returned
     */
    static StringBuilder fixNationalCheckDigit(IbanRegistry countryData, StringBuilder ibanBuilder) {
        requireNonNull(countryData, "countryData must not be null");
        requireNonNull(ibanBuilder, "ibanBuilder must not be null");

        if (ibanBuilder.length() != countryData.getIbanLength()) {
            throw InvalidIbanException.of(IbanValidationError.INCORRECT_LENGTH_COUNTRY, ibanBuilder, countryData.getCountryCode());
        }

        // only countries with a registered NCD field are relevant
        IbanComponent ncdCompo = countryData.getNationalCheckDigitComponent();
        if (ncdCompo == null) {
            return ibanBuilder;
        }

        // verify validator interface safely before casting
        CountryValidator validator = IbanValidator.getCountryValidator(countryData);
        if (validator instanceof NationalCheckDigitCalculator) {
            char[] ncd = ((NationalCheckDigitCalculator) validator).calculateNationalCheckDigit(ibanBuilder);
            // write the computed NCD into the StringBuilder
            ibanBuilder.replace(ncdCompo.getBeginIndex(), ncdCompo.getEndIndex(), new String(ncd));
        }

        return ibanBuilder;
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
        protected StringBuilder appendSubclassComponents(StringBuilder ibanBuilder) {
            return resolveComponent(ibanBuilder, getCountryData().getStructureData().getComponent(BRANCH_CODE), branchCode);
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
        protected StringBuilder appendSubclassComponents(StringBuilder ibanBuilder) {
            super.appendSubclassComponents(ibanBuilder);
            return resolveComponent(ibanBuilder, ibanComponent, customValue);
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
        protected StringBuilder appendSubclassComponents(StringBuilder ibanBuilder) {
            resolveComponent(ibanBuilder, getCountryData().getStructureData().getComponent(BRANCH_CODE), branchCode);
            return super.appendSubclassComponents(ibanBuilder);
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
