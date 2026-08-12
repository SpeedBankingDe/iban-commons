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

import static de.speedbanking.iban.IbanComponent.ACCOUNT_NUMBER;
import static de.speedbanking.iban.IbanComponent.BANK_CODE;
import static de.speedbanking.iban.IbanComponent.BBAN;
import static de.speedbanking.iban.IbanComponent.BRANCH_CODE;

import static java.util.Objects.requireNonNull;

import de.speedbanking.iban.util.IbanPatternConverter;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;
import de.speedbanking.util.IndexRange;
import de.speedbanking.util.Mod97;
import de.speedbanking.util.PatternCache;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

    private final IbanRegistry countryData;

    private Random             random;

    private String             bankCode;
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

    protected final IbanRegistry getCountryData() {
        return countryData;
    }

    /**
     * Validates that {@code actual} is either the {@code expectedBase} country entry or a derived country based on it.
     * <p>
     * Used by country-specific builder subclasses whose custom BBAN component (pattern and {@link IndexRange})
     * is hardcoded for a single base country. Since such a subclass cannot honor an arbitrary {@link IbanRegistry}
     * argument, this guard turns an accidental mis-wiring into an immediate, clear failure instead of silently
     * producing an invalid IBAN.
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
     * Returns the configured {@link Random} instance, creating a default one if not set.
     *
     * @return the random instance, never {@code null}
     */
    protected final Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;
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
        IndexRange bankCodeIndexRange = getCountryData().getBankCodeIndexRange();
        IndexRange accountNumberIndexRange = getCountryData().getAccountNumberIndexRange();

        // Seed with country code, "00" check-digit placeholder (fixed up below) and a random,
        // but pattern-valid, BBAN. The bank code and account number ranges are overwritten right
        // away; appendSubclassComponents() overwrites any further country-specific sub-ranges.
        // Any BBAN positions this library does not expose as a settable component are left with
        // their random, pattern-valid content, so countries with a not fully modeled BBAN still
        // produce a structurally valid IBAN.
        StringBuilder ibanBuilder = new StringBuilder(expectedLength)
            .append(countryCode)
            .append("00")
            .append(resolveComponent(BBAN, null))
            .replace(bankCodeIndexRange.getBegin(),
                     bankCodeIndexRange.getEnd(),
                     resolveComponent(BANK_CODE, bankCode))
            .replace(accountNumberIndexRange.getBegin(),
                     accountNumberIndexRange.getEnd(),
                     resolveComponent(ACCOUNT_NUMBER, accountNumber));

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
     * Resolves an IBAN component by validating and formatting input or generating random values.
     * <p>
     * If the input is null, random values matching the component pattern are generated.
     *
     * @param component the IBAN component specification, must not be null
     * @param input     the input string, may be null
     * @return the resolved or generated component string
     * @throws InvalidIbanException if input length or pattern is invalid
     */
    String resolveComponent(IbanComponent component, String input) {
        requireNonNull(component, "component must not be null");
        return resolveComponent(component.getPattern(getCountryData()), component.getValidationError(), input);
    }

    /**
     * Resolves an IBAN component pattern by validating and formatting input or generating random values.
     * <p>
     * Useful for custom attributes that do not map to standard IbanComponent enum values.
     *
     * @param pattern         the IBAN component pattern string, must not be null
     * @param validationError the error to throw on validation failure, must not be null
     * @param input           the input string, may be null
     * @return the resolved or generated component string
     * @throws InvalidIbanException if input length or pattern is invalid
     */
    String resolveComponent(String pattern, IbanValidationError validationError, String input) {
        List<Segment> segments = IbanPatternConverter.parseSegments(pattern);
        int requiredLength = Segment.calculateTotalLength(segments);
        if (input == null) {
            StringBuilder sb = new StringBuilder(requiredLength);
            for (int i = 0; i < segments.size(); i++) {
                sb.append(generateRandom(segments.get(i)));
            }
            return sb.toString();
        } else {
            if (input.length() > requiredLength) {
                throw InvalidIbanException.of(validationError, input, getCountryData().getCountryCode());
            }
            String resolvedValue;
            if (input.length() < requiredLength && Segment.allMatch(segments, Segment::isNumericOrAlphanumeric)) {
                // numeric padding with leading zeros if shorter than expected length
                resolvedValue = padLeft(input, requiredLength, '0');
            } else {
                resolvedValue = input;
            }
            String regex = IbanPatternConverter.buildRegex(segments);
            if (!PatternCache.getDefault().getPattern(regex).matcher(resolvedValue).matches()) {
                throw InvalidIbanException.of(validationError, input, getCountryData().getCountryCode());
            }
            return resolvedValue;
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
        char[] padding = new char[paddingLen];
        Arrays.fill(padding, padChar);
        return new String(padding) + str;
    }

    /**
     * Generates a random string for a single IBAN notation {@link Segment}.
     * <p>
     * The character set is chosen by the segment's {@code CharType}:
     * <ul>
     *   <li>{@code NUMERIC}      → digits {@code 0–9}</li>
     *   <li>{@code ALPHABETIC}   → upper-case letters {@code A–Z}</li>
     *   <li>{@code ALPHANUMERIC} → the combination of the above</li>
     * </ul>
     *
     * @param segment the pattern segment specifying the character type and length
     * @return a randomly generated string of {@link Segment#getLength()} characters
     * @throws IllegalStateException if an unrecognised {@code CharType} is encountered
     */
    String generateRandom(Segment segment) {
        requireNonNull(segment, "segment");

        SourceChars sourceChars;
        if (segment.isNumeric()) {
            sourceChars = SourceChars.NUMERIC;
        } else if (segment.isAlphabetic()) {
            sourceChars = SourceChars.ALPHABETIC;
        } else {
            sourceChars = SourceChars.ALPHANUMERIC;
        }

        int segmentLen = segment.getLength();
        StringBuilder sb = new StringBuilder(segmentLen);
        for (int i = 0; i < segmentLen; i++) {
            sb.append(sourceChars.nextChar(getRandom()));
        }
        return sb.toString();
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
     *       ({@link IbanRegistry#getNationalCheckDigitIndexRange()} returns {@code null}), or</li>
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
        IndexRange ncdRange = countryData.getNationalCheckDigitIndexRange();
        if (ncdRange == null) {
            return ibanBuilder;
        }

        // verify validator interface safely before casting
        CountryValidator validator = IbanValidator.getCountryValidator(countryData);
        if (validator instanceof NationalCheckDigitCalculator) {
            char[] ncd = ((NationalCheckDigitCalculator) validator).calculateNationalCheckDigit(ibanBuilder);

            // write the computed NCD into the StringBuilder
            for (int idxIban = ncdRange.getBegin(), idxNcd = 0; idxIban < ncdRange.getEnd(); idxIban++, idxNcd++) {
                ibanBuilder.setCharAt(idxIban, ncd[idxNcd]);
            }
        }

        return ibanBuilder;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName())
            .append('[')
            .append("country=").append(getCountryData().getCountryCode())
            .append(", bankCode=").append(bankCode)
            .append(", accountNumber=").append(accountNumber);
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
        NUMERIC("0123456789"),
        ALPHABETIC("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
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
     *
     * @since 1.9.0
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
            return ibanBuilder.replace(getCountryData().getBranchCodeIndexRange().getBegin(),
                                       getCountryData().getBranchCodeIndexRange().getEnd(),
                                       resolveComponent(BRANCH_CODE, branchCode));
        }

        @Override
        protected StringBuilder appendToString(StringBuilder sb) {
            return super.appendToString(sb)
                        .append(", branchCode=").append(branchCode);
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

        private final String     componentName;
        private final String     pattern;
        private final IndexRange indexRange;

        private String           customValue;

        protected AbstractCustomIbanBuilder(IbanRegistry countryData, String componentName, String pattern, IndexRange indexRange) {
            super(countryData);
            this.componentName = requireNonNull(componentName, "componentName");
            this.pattern = requireNonNull(pattern, "pattern");
            this.indexRange = requireNonNull(indexRange, "indexRange");
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
            return super.appendSubclassComponents(ibanBuilder)
                        .replace(indexRange.getBegin(), indexRange.getEnd(),
                            resolveComponent(pattern, IbanValidationError.INVALID_STRUCTURE, customValue));
        }

        @Override
        protected StringBuilder appendToString(StringBuilder sb) {
            return super.appendToString(sb)
                        .append(", ").append(componentName).append('=').append(customValue);
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

        protected AbstractCustomIbanBuilderWithBranchCode(IbanRegistry countryData, String componentName, String pattern, IndexRange indexRange) {
            super(countryData, componentName, pattern, indexRange);
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
            ibanBuilder.replace(getCountryData().getBranchCodeIndexRange().getBegin(),
                                getCountryData().getBranchCodeIndexRange().getEnd(),
                                resolveComponent(BRANCH_CODE, branchCode));
            return super.appendSubclassComponents(ibanBuilder);
        }

        @Override
        protected StringBuilder appendToString(StringBuilder sb) {
            sb.append(", branchCode=").append(branchCode);
            return super.appendToString(sb);
        }
    }

    /**
     * Specialized builder for Bulgarian (BG) IBAN structures.
     */
    public static class BgIbanBuilder extends AbstractCustomIbanBuilderWithBranchCode<BgIbanBuilder> {
        BgIbanBuilder() {
            this(IbanRegistry.BG);
        }

        BgIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.BG), "accountType", "2!n", IndexRange.of(12, 14));
        }

        public BgIbanBuilder accountType(String accountType) {
            return customValue(accountType);
        }
    }

    /**
     * Specialized builder for Algerian (DZ) IBAN structures.
     */
    public static class DzIbanBuilder extends AbstractCustomIbanBuilderWithBranchCode<DzIbanBuilder> {
        DzIbanBuilder() {
            this(IbanRegistry.DZ);
        }

        DzIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.DZ), "nationalCode", "2!n", IndexRange.of(22, 24));
        }

        public DzIbanBuilder nationalCode(String nationalCode) {
            return customValue(nationalCode);
        }
    }

    /**
     * Specialized builder for Icelandic (IS) IBAN structures.
     */
    public static class IsIbanBuilder extends AbstractCustomIbanBuilder<IsIbanBuilder> {
        IsIbanBuilder() {
            this(IbanRegistry.IS);
        }

        IsIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.IS), "identificationNumber", "10!n", IndexRange.of(16, 26));
        }

        public IsIbanBuilder identificationNumber(String identificationNumber) {
            return customValue(identificationNumber);
        }
    }

    /**
     * Specialized builder for Mauritian (MU) IBAN structures.
     */
    public static class MuIbanBuilder extends AbstractCustomIbanBuilderWithBranchCode<MuIbanBuilder> {
        MuIbanBuilder() {
            this(IbanRegistry.MU);
        }

        MuIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.MU), "nationalCode", "3!a", IndexRange.of(27, 30));
        }

        public MuIbanBuilder nationalCode(String nationalCode) {
            return customValue(nationalCode);
        }
    }

    /**
     * Specialized builder for Polish (PL) IBAN structures.
     */
    public static class PlIbanBuilder extends AbstractCustomIbanBuilderWithBranchCode<PlIbanBuilder> {
        PlIbanBuilder() {
            this(IbanRegistry.PL);
        }

        PlIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.PL), "nationalCode", "1!n", IndexRange.of(11, 12));
        }

        public PlIbanBuilder nationalCode(String nationalCode) {
            return customValue(nationalCode);
        }
    }

    /**
     * Specialized builder for Seychellois (SC) IBAN structures.
     */
    public static class ScIbanBuilder extends AbstractCustomIbanBuilderWithBranchCode<ScIbanBuilder> {
        ScIbanBuilder() {
            this(IbanRegistry.SC);
        }

        ScIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.SC), "nationalCode", "3!a", IndexRange.of(28, 31));
        }

        public ScIbanBuilder nationalCode(String nationalCode) {
            return customValue(nationalCode);
        }
    }

    /**
     * Specialized builder for Togolese (TG) IBAN structures.
     */
    public static class TgIbanBuilder extends AbstractCustomIbanBuilderWithBranchCode<TgIbanBuilder> {
        TgIbanBuilder() {
            this(IbanRegistry.TG);
        }

        TgIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.TG), "nationalCode", "2!n", IndexRange.of(26, 28));
        }

        public TgIbanBuilder nationalCode(String nationalCode) {
            return customValue(nationalCode);
        }
    }

    /**
     * Specialized builder for Brazilian (BR) IBAN structures.
     */
    public static class BrIbanBuilder extends AbstractCustomIbanBuilderWithBranchCode<BrIbanBuilder> {
        BrIbanBuilder() {
            this(IbanRegistry.BR);
        }

        BrIbanBuilder(IbanRegistry countryData) {
            super(requireCountry(countryData, IbanRegistry.BR), "accountTypeAndControl", "1!a1!c", IndexRange.of(27, 29));
        }

        /**
         * Sets the combined account type and control characters for the Brazilian IBAN.
         *
         * @param accountTypeAndControl the 2-character account type and control value (e.g. "C1", "P1")
         * @return this builder instance
         */
        public BrIbanBuilder accountTypeAndControl(String accountTypeAndControl) {
            return customValue(accountTypeAndControl);
        }
    }

}
