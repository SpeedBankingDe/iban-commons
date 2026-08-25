package de.speedbanking.iban;

import static de.speedbanking.iban.IbanComponent.IbanComponentType.ACCOUNT_NUMBER;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.ACCOUNT_TYPE;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.ACCOUNT_TYPE_AND_CONTROL;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.BANK_CODE;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.BRANCH_CODE;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.IDENTIFICATION_NUMBER;
import static de.speedbanking.iban.IbanComponent.IbanComponentType.NATIONAL_CODE;
import static de.speedbanking.iban.IbanValidationError.EMPTY;
import static de.speedbanking.iban.IbanValidationError.INCORRECT_LENGTH;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIban;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanString;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatInvalidIbanException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.speedbanking.iban.IbanBuilder.AccountTypeAndControlIbanBuilderWithBranchCode;
import de.speedbanking.iban.IbanBuilder.AccountTypeIbanBuilderWithBranchCode;
import de.speedbanking.iban.IbanBuilder.IbanBuilderWithBranchCode;
import de.speedbanking.iban.IbanBuilder.IdentificationNumberIbanBuilderWithBranchCode;
import de.speedbanking.iban.IbanBuilder.NationalCodeIbanBuilderWithBranchCode;
import de.speedbanking.iban.IbanBuilder.StandardIbanBuilder;
import de.speedbanking.iban.IbanComponent.IbanComponentType;
import de.speedbanking.iban.util.IbanPatternConverter.Segment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.stream.Stream;

/**
 * JUnit test class for {@link IbanBuilder}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
@ResourceLock(IbanConfigTest.RESOURCE_NAME)
final class IbanBuilderTest {

    @BeforeAll
    static void prepareConfig() {
        IbanConfig.reset(IbanConfig.builder().validateNcd(true).calculateNcd(true).build());
    }

    @AfterAll
    static void resetConfig() {
        IbanConfig.reset();
    }

    static Stream<IbanRegistry> ncdCountries() {
        return IbanRegistry.ALL_COUNTRIES.stream()
            .filter(IbanRegistry::hasNationalCheckDigit);
    }

    @DisplayName("of: should return correct IbanBuilder subclass based on country structure")
    @Test
    void of_factory_returnsCorrectBuilderType() {
        IbanBuilder<?> foBuilder = IbanRegistry.FO.builder();
        assertThat(foBuilder)
            .isExactlyInstanceOf(StandardIbanBuilder.class);

        IbanBuilder<?> giBuilder = IbanRegistry.GR.builder();
        assertThat(giBuilder)
            .isInstanceOf(IbanBuilderWithBranchCode.class);
    }

    @DisplayName("requireCountry: returns actual when it exactly matches the expected base country")
    @Test
    void requireCountry_exactMatch_returnsActual() {
        assertThat(IbanBuilder.requireCountry(IbanRegistry.BG, IbanRegistry.BG)).isSameAs(IbanRegistry.BG);
    }

    @DisplayName("requireCountry: returns actual when it is a country derived from the expected base country")
    @Test
    void requireCountry_derivedCountry_returnsActual() {
        // AX (Åland Islands) ist in der Registry von FI (Finnland) abgeleitet; keines der acht
        // Custom-Builder-Länder hat aktuell eine abgeleitete Variante — daher hier bewusst
        // sachfremde, aber reale Registry-Daten, um genau diesen Zweig zu erreichen.
        assertThat(IbanBuilder.requireCountry(IbanRegistry.AX, IbanRegistry.FI)).isSameAs(IbanRegistry.AX);
    }

    @DisplayName("requireCountry: throws IllegalArgumentException for an unrelated country")
    @Test
    void requireCountry_unrelatedCountry_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> IbanBuilder.requireCountry(IbanRegistry.DE, IbanRegistry.BG))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("This builder only supports BG or derived countries but was constructed with country data for DE");
    }

    @DisplayName("requireCountry: throws NullPointerException when actual is null")
    @Test
    void requireCountry_nullActual_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> IbanBuilder.requireCountry(null, IbanRegistry.BG))
            .withMessage("countryData must not be null");
    }

    @DisplayName("AbstractCustomIbanBuilder: throws NPE with descriptive message when component undefined for country")
    @Test
    void abstractCustomIbanBuilder_missingComponent_throwsNpeWithMessage() {
        assertThatNullPointerException()
            .isThrownBy(() -> new NationalCodeIbanBuilderWithBranchCode(IbanRegistry.DE))
            .withMessageContaining("NATIONAL_CODE")
            .withMessageContaining("DE");
    }

    @DisplayName("build: random generation produces valid IBAN")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(value = IbanRegistry.class)
    void build_randomGeneration_producesValidIban(IbanRegistry countryData) {
        Iban iban = countryData.builder()
            .withRandom(new Random(42L))
            .build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
    }

    @DisplayName("build: custom inputs produce expected IBAN string for Greenland (GL)")
    @Test
    void build_customInputs_producesValidIban() {
        Iban iban = IbanRegistry.GL.builder()
            .bankCode("6471")
            .accountNumber("1234567890")
            .build();

        assertThatIban(iban).hasToString("GL2164711234567890");
    }

    @DisplayName("build: short numeric input is padded with leading zeros for Finland (FI)")
    @Test
    void build_shortNumericInput_padsWithLeadingZeros() {
        String bankCode = "9181";
        String accountNumber = "12345";
        Iban iban = IbanRegistry.FI.builder()
            .bankCode(bankCode)
            .accountNumber(accountNumber)
            .build();

        assertThatIban(iban)
            .hasBankCode("00" + bankCode)
            .hasAccountNumber("00" + accountNumber)
            .hasToString("FI3100918100123457")
            .hasComponentString("FI 31 009181 0012345 7");
    }

    @DisplayName("build: builder with branch code correctly formats IBAN")
    @Test
    void build_withBranchCode_producesValidIban() {
        IbanBuilderWithBranchCode builder = IbanRegistry.MC.builder();
        Iban iban = builder
            .bankCode("3000")
            .branchCode("00001")
            .accountNumber("12345678901")
            .build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
    }

    @DisplayName("build: input exceeding maximum length throws InvalidIbanException")
    @Test
    void build_inputExceedsLength_throwsInvalidIbanException() {
        IbanBuilder<?> builder = IbanRegistry.GL.builder();

        assertThatInvalidIbanException()
            .isThrownBy(() -> builder.bankCode("123456").build());
    }

    /**
     * Verifies that for every country defining a National Check Digit (NCD), random generation
     * produces a valid IBAN with correct national check digits when calculation is enabled.
     */
    @DisplayName("build: random generation produces valid NCD for all NCD-enabled countries")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("ncdCountries")
    void generateWithNcdEnabled_producesValidIban(IbanRegistry countryData) {
        Random rnd = new Random(42L);
        Iban iban = countryData.builder().withRandom(rnd).build();

        assertThat(IbanValidator.isValid(iban.toString()))
            .as("NCD-enabled generation produced invalid IBAN for %s: %s", countryData, iban)
            .isTrue();
    }

    @DisplayName("createSegment: passing null charType throws NullPointerException")
    @Test
    void createSegment_unknownCharType_throwsNullPointerException() {
        assertThatNullPointerException().isThrownBy(() -> Segment.of(null, 5));
    }

    @DisplayName("padLeft: pads string with leading characters when shorter than target length")
    @Test
    void padLeft_shorterInput_padsWithGivenChar() {
        assertThat(IbanBuilder.padLeft("123", 6, '0')).isEqualTo("000123");
    }

    @DisplayName("padLeft: returns input unchanged when already at or above target length")
    @Test
    void padLeft_inputAtOrAboveTargetLength_returnsUnchanged() {
        assertThat(IbanBuilder.padLeft("123456", 6, '0')).isEqualTo("123456");
        assertThat(IbanBuilder.padLeft("1234567", 6, '0')).isEqualTo("1234567");
    }

    @DisplayName("padLeft: returns null or empty input unchanged")
    @Test
    void padLeft_nullOrEmptyInput_returnsUnchanged() {
        assertThat(IbanBuilder.padLeft(null, 6, '0')).isNull();
        assertThat(IbanBuilder.padLeft("", 6, '0')).isEmpty();
    }

    @DisplayName("resolveComponent: numeric input shorter than required length is left-padded with zeros")
    @Test
    void resolveComponent_shortNumericInput_isPadded() {
        IbanBuilder<?> builder = IbanRegistry.FO.builder();
        IbanComponent component = builder.getCountryData().getStructureData().getComponent(ACCOUNT_NUMBER);
        StringBuilder result = builder.resolveComponent(component, "12345");

        assertThat(result).hasSize(18).hasToString("        000012345 ");
    }

    @DisplayName("resolveComponent: invalid bank code input throws InvalidIbanException with INVALID_BANK_CODE")
    @Test
    void resolveComponent_invalidBankCode_throwsSpecificException() {
        IbanBuilder<?> builder = IbanRegistry.GL.builder();

        assertThatInvalidIbanException()
            .isThrownBy(() -> builder.bankCode("123456").build())
            .hasFieldOrPropertyWithValue("reason", IbanValidationError.INVALID_BANK_CODE);
    }

    @DisplayName("resolveComponent: invalid branch code input throws InvalidIbanException with INVALID_BRANCH_CODE")
    @Test
    void resolveComponent_invalidBranchCode_throwsSpecificException() {
        IbanBuilderWithBranchCode builder = IbanRegistry.MC.builder();

        assertThatInvalidIbanException()
            .isThrownBy(() -> builder.branchCode("TOO_LONG_BRANCH_CODE").build())
            .hasFieldOrPropertyWithValue("reason", IbanValidationError.INVALID_BRANCH_CODE);
    }

    @DisplayName("resolveComponent: passing null component throws NullPointerException")
    @Test
    void resolveComponent_nullComponent_throwsNPE() {
        IbanBuilder<?> builder = IbanRegistry.GL.builder();

        assertThatNullPointerException()
            .isThrownBy(() -> builder.resolveComponent(null, null, "1234"));
        assertThatNullPointerException()
            .isThrownBy(() -> builder.resolveComponent(new StringBuilder(), null, "1234"));
    }

    @DisplayName("resolveComponent: input with invalid characters for pattern throws InvalidIbanException")
    @Test
    void resolveComponent_invalidCharacters_throwsInvalidIbanException() {
        IbanBuilder<?> builder = IbanRegistry.GL.builder();

        // GL bankCode pattern is '4!n'; string length is valid (4) but contains non-digits
        assertThatInvalidIbanException()
            .isThrownBy(() -> builder.bankCode("12AB").build())
            .hasFieldOrPropertyWithValue("reason", IbanValidationError.INVALID_BANK_CODE);
    }

    @DisplayName("resolveComponent: short input for non-numeric segment is not padded and fails regex validation")
    @Test
    void resolveComponent_shortAlphabeticInput_failsValidation() {
        // SC nationalCode pattern is '3!a' (ALPHABETIC); input length (2) < requiredLength (3)
        NationalCodeIbanBuilderWithBranchCode builder = IbanRegistry.SC.builder();
        builder.bankCode("ABCD01")
            .branchCode("23")
            .accountNumber("1234567890123456")
            .nationalCode("AB");

        assertThatInvalidIbanException()
            .isThrownBy(builder::build)
            .hasFieldOrPropertyWithValue("reason", IbanValidationError.INVALID_STRUCTURE);
    }

    @DisplayName("resolveComponent: pattern mismatch with exact length throws InvalidIbanException")
    @Test
    void resolveComponent_directPatternMismatch_throwsInvalidIbanException() {
        IbanBuilder<?> builder = IbanRegistry.GL.builder();

        assertThatInvalidIbanException()
            .isThrownBy(() -> builder.resolveComponent(new IbanComponent(IbanComponentType.BANK_CODE, "4!n", 4, 4), "999X"))
            .hasFieldOrPropertyWithValue("reason", IbanValidationError.INVALID_BANK_CODE);

        assertThatInvalidIbanException()
            .isThrownBy(() -> builder.resolveComponent(IbanRegistry.GL.getStructureData().getComponent(IbanComponentType.BBAN), "###"))
            .hasFieldOrPropertyWithValue("reason", IbanValidationError.INVALID_STRUCTURE);
    }

    /**
     * Ensures that when {@code fixCheckDigits} is invoked on an IBAN whose check digits are
     * already correct, the validation short-circuits and the buffer is returned unchanged.
     */
    @DisplayName("fixCheckDigits: returns unchanged buffer when check digits are already valid")
    @Test
    @SuppressWarnings("UnnecessaryStringBuilder")
    void fixCheckDigits_alreadyValid_returnsUnchangedEarly() {
        StringBuilder sb = new StringBuilder("GL2164711234567890");

        StringBuilder result = IbanBuilder.fixCheckDigits(sb);

        assertThat(result).isSameAs(sb)
            .hasToString(sb.toString());
    }

    /**
     * {@link IbanBuilder.StandardIbanBuilder} is normally only reachable through
     * {@link IbanRegistry#builder()}; this directly exercises its package-private constructor
     * to ensure the type itself is covered.
     */
    @DisplayName("StandardIbanBuilder: direct construction and build produces a valid IBAN")
    @Test
    void standardIbanBuilder_directConstruction_producesValidIban() {
        StandardIbanBuilder builder = new StandardIbanBuilder(IbanRegistry.GL);

        Iban iban = builder.bankCode("6471")
            .accountNumber("1234567890")
            .build();

        assertThatIban(iban)
            .isNotNull()
            .hasToString(iban.toString());
    }

    @DisplayName("toString: IbanBuilder includes country data, bank code and account number")
    @Test
    void toString_standardBuilder_containsFields() {
        IbanBuilder<?> builder = IbanRegistry.GL.builder()
            .bankCode("6471")
            .accountNumber("1234567890");

        assertThat(builder.toString())
            .contains(BANK_CODE.getLabel() + '=' + "6471")
            .contains(ACCOUNT_NUMBER.getLabel() + '=' + "1234567890");
    }

    @DisplayName("toString: IbanBuilderWithBranchCode additionally includes the branch code")
    @Test
    void toString_branchCodeBuilder_containsBranchCode() {
        IbanBuilderWithBranchCode builder = IbanRegistry.MC.builder();
        builder
            .bankCode("3000")
            .branchCode("00001")
            .accountNumber("12345678901");

        assertThat(builder.toString())
            .contains(BANK_CODE.getLabel() + '=' + "3000")
            .contains(BRANCH_CODE.getLabel() + '=' + "00001")
            .contains(ACCOUNT_NUMBER.getLabel() + '=' + "12345678901");
    }

    /**
     * Test-only implementation of {@link IbanBuilder} designed to simulate a corrupting subclass
     * that appends unexpected characters beyond the country's defined IBAN length.
     */
    private static final class LengthCorruptingIbanBuilder extends IbanBuilder<LengthCorruptingIbanBuilder> {

        LengthCorruptingIbanBuilder(IbanRegistry countryData) {
            super(countryData);
        }

        @Override
        protected StringBuilder appendSubclassComponents(StringBuilder ibanBuilder) {
            return ibanBuilder.append("EXTRA");
        }
    }

    /**
     * Ensures internal consistency guards trigger an {@link IllegalStateException} if custom or corrupt
     * subclasses append components that violate the exact target length for the specified country.
     */
    @DisplayName("build: subclass corrupting IBAN length throws IllegalStateException")
    @Test
    void build_subclassCorruptsLength_throwsIllegalStateException() {
        IbanBuilder<?> corruptedBuilder = new LengthCorruptingIbanBuilder(IbanRegistry.FO);

        assertThatThrownBy(corruptedBuilder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("invalid IBAN length");
    }

    @DisplayName("fixCheckDigits: throws InvalidIbanException when input is null")
    @Test
    void fixCheckDigits_shouldThrowException_whenInputIsNull() {
        assertThatInvalidIbanException()
            .isThrownBy(() -> IbanBuilder.fixCheckDigits(null))
            .withNoCause()
            .withMessage("%s (%s)", EMPTY.getText(), EMPTY)
            .hasFieldOrPropertyWithValue("reason", EMPTY);
    }

    @DisplayName("fixCheckDigits: throws IllegalArgumentException when length is out of bounds")
    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        "GL00", // too short
        "GL006471123456789012345678901234567890" // too long
    })
    void fixCheckDigits_shouldThrowException_whenLengthIsOutOfBounds(String invalidIban) {
        assertThatInvalidIbanException()
            .isThrownBy(() -> IbanBuilder.fixCheckDigits(invalidIban))
            .withNoCause()
            .withMessage("%s (%s): '%s'", INCORRECT_LENGTH.getText(), INCORRECT_LENGTH, invalidIban)
            .hasFieldOrPropertyWithValue("reason", INCORRECT_LENGTH);
    }

    /**
     * Verifies in-place mutation performance behavior. Passing a {@link StringBuilder} instance
     * to {@code fixCheckDigits} must modify and return the exact same buffer instance rather than
     * allocating new objects.
     */
    @DisplayName("fixCheckDigits: reuses existing StringBuilder instance to prevent re-allocation")
    @Test
    @SuppressWarnings("UnnecessaryStringBuilder")
    void fixCheckDigits_shouldReuseInstance_whenInputIsStringBuilder() {
        StringBuilder inputBuffer = new StringBuilder("GL0064711234567890");
        StringBuilder resultBuffer = IbanBuilder.fixCheckDigits(inputBuffer);

        assertThat(resultBuffer)
            .as("The returned StringBuilder must be identical to the input instance")
            .isSameAs(inputBuffer);

        assertThat(resultBuffer)
            .as("The check digits must be correctly computed and mutated inside the buffer")
            .hasToString(inputBuffer.toString());
    }

    @DisplayName("fixCheckDigits: creates new StringBuilder instance when input is non-StringBuilder CharSequence")
    @Test
    void fixCheckDigits_shouldCreateNewStringBuilder_whenInputIsString() {
        String input = "PS55RLKTIVUU04P3AF3VJKXZE9RAZ";

        StringBuilder resultBuffer = IbanBuilder.fixCheckDigits(input);

        assertThat(resultBuffer)
            .as("A new StringBuilder instance must be allocated with computed check digits")
            .isNotSameAs(input)
            .hasToString(input);
    }

    @DisplayName("fixNationalCheckDigit: returns unchanged buffer for country without NCD requirement")
    @Test
    void fixNationalCheckDigit_noNcdField_returnsUnchanged() {
        assertThat(IbanRegistry.GL.getNationalCheckDigitComponent()).isNull();

        StringBuilder sb = new StringBuilder("GL5864711234567890");
        StringBuilder result  = IbanBuilder.fixNationalCheckDigit(IbanRegistry.GL, sb);

        assertThat(result).isSameAs(sb)
                          .hasToString("GL5864711234567890");
    }

    /**
     * Ensures NCD recalculation guards fail early when presented with a string buffer whose
     * length does not match the target country's official IBAN format definition.
     */
    @DisplayName("fixNationalCheckDigit: throws InvalidIbanException when buffer length is invalid")
    @Test
    void fixNationalCheckDigit_wrongLength_throws() {
        assertThatInvalidIbanException()
            .isThrownBy(() -> IbanBuilder.fixNationalCheckDigit(IbanRegistry.GL, new StringBuilder("GL00")))
            .withMessageStartingWith("IBAN has incorrect length for specified country");
    }

    @DisplayName("fixNationalCheckDigit: throws NullPointerException when countryData is null")
    @Test
    void fixNationalCheckDigit_nullCountryData_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> IbanBuilder.fixNationalCheckDigit(null, new StringBuilder()));
    }

    @DisplayName("fixNationalCheckDigit: throws NullPointerException when ibanBuilder is null")
    @Test
    void fixNationalCheckDigit_nullIbanBuilder_throwsNPE() {
        assertThatNullPointerException()
            .isThrownBy(() -> IbanBuilder.fixNationalCheckDigit(IbanRegistry.GL, null));
    }

    @DisplayName("AccountTypeIbanBuilderWithBranchCode: builds valid Bulgarian IBAN and includes accountType in toString")
    @Test
    void accountTypeBuilder_bulgaria_buildsValidIbanAndFormatsToString() {
        AccountTypeIbanBuilderWithBranchCode builder = IbanRegistry.BG.builder();
        builder.bankCode("BNBG")
            .branchCode("9661")
            .accountNumber("12345678")
            .accountType("10");

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
        assertThat(builder.toString())
            .contains("AccountTypeIbanBuilderWithBranchCode")
            .contains("country=BG")
            .contains(BANK_CODE.getLabel() + "=BNBG")
            .contains(BRANCH_CODE.getLabel() + "=9661")
            .contains(ACCOUNT_NUMBER.getLabel() + "=12345678")
            .contains(ACCOUNT_TYPE.getLabel() + "=10");
    }

    @DisplayName("NationalCodeIbanBuilderWithBranchCode: builds valid Algerian IBAN and includes nationalCode in toString")
    @Test
    void nationalCodeBuilder_algeria_buildsValidIbanAndFormatsToString() {
        String nationalCode = "12";
        NationalCodeIbanBuilderWithBranchCode builder = IbanRegistry.DZ.builder();
        builder.bankCode("001")
            .branchCode("001")
            .accountNumber("1234567")
            .nationalCode(nationalCode);

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
        assertThat(builder.toString())
            .startsWith("NationalCodeIbanBuilderWithBranchCode")
            .contains(NATIONAL_CODE.getLabel()  + '=' + nationalCode);
    }

    @DisplayName("IdentificationNumberIbanBuilder: builds valid Icelandic IBAN")
    @Test
    void identificationNumberBuilder_iceland_buildsValidIban() {
        String identificationNumber = "0101901239";
        IdentificationNumberIbanBuilderWithBranchCode builder = IbanRegistry.IS.builder();
        builder.bankCode("0101")
            .accountNumber("123456")
            .identificationNumber(identificationNumber);

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
        assertThat(builder.toString())
            .startsWith(IdentificationNumberIbanBuilderWithBranchCode.class.getSimpleName())
            .contains("country=IS")
            .contains(IDENTIFICATION_NUMBER.getLabel() + '=' + identificationNumber);
    }

    @DisplayName("NationalCodeIbanBuilderWithBranchCode: builds valid Mauritian IBAN and includes nationalCode in toString")
    @Test
    void nationalCodeBuilder_mauritius_buildsValidIbanAndFormatsToString() {
        String nationalCode = "MUR";
        NationalCodeIbanBuilderWithBranchCode builder = IbanRegistry.MU.builder();
        builder.bankCode("BOMM01")
            .branchCode("01")
            .accountNumber("123456789101000")
            .nationalCode(nationalCode);

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
        assertThat(builder.toString())
            .startsWith(NationalCodeIbanBuilderWithBranchCode.class.getSimpleName())
            .contains(NATIONAL_CODE.getLabel() + '=' + nationalCode);
    }

    @DisplayName("NationalCodeIbanBuilderWithBranchCode: builds valid Polish IBAN and includes nationalCode in toString")
    @Test
    void nationalCodeBuilder_poland_buildsValidIbanAndFormatsToString() {
        NationalCodeIbanBuilderWithBranchCode builder = IbanRegistry.PL.builder();
        builder.bankCode("105")
            .branchCode("0009")
            .nationalCode("9")
            .accountNumber("7603123456789123");

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid().isEqualTo("PL10105000997603123456789123");
        assertThat(builder.toString())
            .startsWith(NationalCodeIbanBuilderWithBranchCode.class.getSimpleName())
            .contains(BANK_CODE.getLabel() + "=105");
    }

    @DisplayName("NationalCodeIbanBuilderWithBranchCode: builds valid Seychellois IBAN and includes nationalCode in toString")
    @Test
    void nationalCodeBuilder_seychelles_buildsValidIbanAndFormatsToString() {
        String nationalCode = "SCR";
        NationalCodeIbanBuilderWithBranchCode builder = IbanRegistry.SC.builder();
        builder.bankCode("ABCD01")
            .branchCode("23")
            .accountNumber("1234567890123456")
            .nationalCode(nationalCode);

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
        assertThat(builder.toString())
            .startsWith(NationalCodeIbanBuilderWithBranchCode.class.getSimpleName())
            .contains(NATIONAL_CODE.getLabel() + '=' + nationalCode);
    }

    @DisplayName("NationalCodeIbanBuilderWithBranchCode: builds valid Togolese IBAN and includes nationalCode in toString")
    @Test
    void nationalCodeBuilder_togo_buildsValidIbanAndFormatsToString() {
        String nationalCode = "12";
        NationalCodeIbanBuilderWithBranchCode builder = IbanRegistry.TG.builder();
        builder.bankCode("TG009")
            .branchCode("01001")
            .accountNumber("102325004005")
            .nationalCode(nationalCode);

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
        assertThat(builder.toString())
            .startsWith(NationalCodeIbanBuilderWithBranchCode.class.getSimpleName())
            .contains("country=TG")
            .contains(NATIONAL_CODE.getLabel() + '=' + nationalCode);
    }

    @DisplayName("AccountTypeAndControlIbanBuilderWithBranchCode: builds valid Brazilian IBAN with combined accountTypeAndControl component")
    @Test
    void accountTypeAndControlBuilder_brazil_buildsValidIbanAndFormatsToString() {
        AccountTypeAndControlIbanBuilderWithBranchCode builder = IbanRegistry.BR.builder();
        builder.bankCode("00360305")
            .branchCode("00001")
            .accountNumber("0009795493")
            .accountTypeAndControl("P1");

        Iban iban = builder.build();

        assertThatIban(iban).isNotNull();
        assertThatIbanString(iban.toString()).isValid();
        assertThat(builder.toString())
            .contains(AccountTypeAndControlIbanBuilderWithBranchCode.class.getSimpleName())
            .contains(ACCOUNT_TYPE_AND_CONTROL.getLabel() + "=P1");
    }

}
