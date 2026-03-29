package de.speedbanking.bic;

import static de.speedbanking.bic.BicAssertions.assertThat;
import static de.speedbanking.bic.BicAssertions.assertThatInvalidBicException;

import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.catchThrowable;

import de.speedbanking.test.TestUtil;
import de.speedbanking.util.Currency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;

/**
 * JUnit tests for the new immutable {@link Bic} class.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
class BicTest {

    @DisplayName("of() should create Bic object for a valid BIC-8")
    @Test
    void of_ValidBic8_ShouldReturnBic() {
        assertThat(Bic.of("MARKDEFF"))
            .as("Check properties for BIC-8")
            .isBic8()
            .isNotBic11()
            .hasLength(8)
            .hasToString("MARKDEFF")
            .isBic8EqualTo("MARKDEFF")
            .isBic11NormalizedEqualTo("MARKDEFFXXX")
            .hasCountryCode("DE")
            .hasCountryName("Germany")
            .hasCountryFlag("🇩🇪")
            .hasCurrency(Currency.EUR)
            .hasCurrencyCode(Currency.EUR.name());
    }

    @DisplayName("of() should create Bic object for a valid BIC-11")
    @Test
    void of_ValidBic11_ShouldReturnBic() {
        assertThat(Bic.of("MARKDEFF500"))
            .as("Check properties for BIC-11")
            .isBic11()
            .isNotBic8()
            .hasLength(11)
            .hasToString("MARKDEFF500")
            .isBic11NormalizedEqualTo("MARKDEFF500")
            .hasCountryCode("DE")
            .hasCountryName("Germany")
            .hasCountryFlag("🇩🇪")
            .hasCurrency(Currency.EUR)
            .hasCurrencyCode(Currency.EUR.name());
    }

    @DisplayName("of() should throw exception for null or empty BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @NullAndEmptySource
    void of_NullOrEmpty_ShouldThrowException(String bic) {
        assertThatInvalidBicException()
            .isThrownBy(() -> Bic.of(bic))
            .withMessage("BIC is null or empty (" + BicValidationError.EMPTY + ")")
            .hasFieldOrPropertyWithValue("reason", BicValidationError.EMPTY);
    }

    @DisplayName("of() should throw exception for incorrect length")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {
        "SHORT12",      // 7 chars
        "LONG12345678", // 12 chars
        "INVALID99",    // 9 chars
        "MARKDEFFX",    // 9 chars
        "DEUTDEFF1234"  // 12 chars
    })
    void of_InvalidLength_ShouldThrowException(String bic) {
        assertThatInvalidBicException()
            .isThrownBy(() -> Bic.of(bic))
            .withMessage("BIC has incorrect length (INCORRECT_LENGTH): " + bic)
            .hasFieldOrPropertyWithValue("reason", BicValidationError.INCORRECT_LENGTH);
    }

    @DisplayName("of() should throw exception for invalid country code")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {
        "MARK99FF", // 99 is not a valid ISO country
        "MARKXXFF"  // XX is not a valid ISO country
    })
    void of_InvalidCountry_ShouldThrowException(String bic) {
        assertThatInvalidBicException()
            .isThrownBy(() -> Bic.of(bic))
            .withMessage("BIC has invalid country code (INVALID_COUNTRY): " + bic)
            .hasFieldOrPropertyWithValue("reason", BicValidationError.INVALID_COUNTRY);
    }

    @DisplayName("of() should throw exception for invalid bank code")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {
        "mARKDEFF",
        "MÄRKDEFF",
        "____DEFF"
    })
    void of_InvalidBankCode_ShouldThrowException(String bic) {
        assertThatInvalidBicException()
            .isThrownBy(() -> Bic.of(bic))
            .withMessage("Invalid bank code (INVALID_BANK_CODE): " + bic)
            .hasFieldOrPropertyWithValue("reason", BicValidationError.INVALID_BANK_CODE);
    }

    @DisplayName("of() should throw exception for illegal characters")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {
        "MARKDE 1", // space
        "MARKDE_1", // underscore
        "MARKDEff"  // lowercase
    })
    void of_IllegalCharacters_ShouldThrowException(String bic) {
        assertThatInvalidBicException()
            .isThrownBy(() -> Bic.of(bic))
            .withMessage("BIC contains illegal character(s) (ILLEGAL_CHARACTERS): " + bic)
            .hasFieldOrPropertyWithValue("reason", BicValidationError.ILLEGAL_CHARACTERS);
    }

    @DisplayName("tryParse() should return non-empty Optional for valid BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {"BHLSDEM1", "BHLSDEM1XXX"})
    void tryParse_ValidBic_ShouldReturnNonEmptyOptional(String bic) {
        assertThat(Bic.tryParse(bic))
            .isNotEmpty()
            .hasValue(Bic.of(bic));
    }

    @DisplayName("tryParseOrNull() should return valid BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {"BHLSDEM1", "BHLSDEM1XXX"})
    void tryParseOrNull_ValidBic_ShouldReturnNonEmptyOptional(String bic) {
        assertThat(Bic.tryParseOrNull(bic))
            .isEqualTo(Bic.of(bic));
    }

    @DisplayName("tryParse() should return Optional.empty for invalid BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {" ", "INVALID99", "MARK00FF"})
    @NullAndEmptySource
    void tryParse_InvalidBic_ShouldReturnEmptyOptional(String bic) {
        assertThat(Bic.tryParse(bic)).isEmpty();
    }

    @DisplayName("tryParseOrNull() should return null for invalid BIC")
    @ParameterizedTest(name = "BIC: ''{0}''")
    @ValueSource(strings = {" ", "INVALID99", "MARK00FF"})
    @NullAndEmptySource
    void tryParseOrNull_InvalidBic_ShouldReturnEmptyOptional(String bic) {
        assertThat(Bic.tryParseOrNull(bic)).isNull();
    }

    @DisplayName("isValid() should return true/false based on validation")
    @ParameterizedTest(name = "BIC: ''{0}'', valid: {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "MARKDEFF | true",
        "INVALID  | false",
        "(null)   | false",
        "''       | false",
        "' '      | false",
        "xyz      | false",
        "MARKxxFF | false",
        "MARK00FF | false"
    })
    void isValid_ShouldReflectValidationResult(String bic, boolean expectedResult) {
        assertThat(Bic.isValid(bic)).isEqualTo(expectedResult);
    }

    @DisplayName("Component getters should return correct parts")
    @ParameterizedTest(name = "BIC: {0}, BankCode: {1}, CountryCode: {2}, LocationCode: {3}, BranchCode: {4}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // BIC (Input) | Bank Code | Country Code | Location Code | Branch Code
        "MARKDEFF      | MARK      | DE           | FF            | (null)",
        "MARKDEFFXXX   | MARK      | DE           | FF            | XXX",
        "NEDSZAJJ      | NEDS      | ZA           | JJ            | (null)",
        "NEDSZAJJXXX   | NEDS      | ZA           | JJ            | XXX",
        "DBABDEFF500   | DBAB      | DE           | FF            | 500"
    })
    void getters_ShouldExtractCorrectComponents(
            String bicValue,
            String expectedBankCode,
            String expectedCountryCode,
            String expectedLocationCode,
            String expectedBranchCode
    ) {
        Bic bic = Bic.of(bicValue);

        assertThat(bic)
            .as("Component extraction check for BIC %s", bicValue)
            .hasBankCode(expectedBankCode)
            .hasCountryCode(expectedCountryCode)
            .hasLocationCode(expectedLocationCode)
            .hasBranchCode(expectedBranchCode);

        // the isSameAs checks are implementation details (caching) and must remain separate
        // to verify instance identity, as they do not return BicAssert.
        assertThat(bic.getBankCode()).isSameAs(bic.getBankCode());
        assertThat(bic.getCountryCode()).isSameAs(bic.getCountryCode());
        assertThat(bic.getLocationCode()).isSameAs(bic.getLocationCode());
    }

    @DisplayName("toBic11(), toBic8(), and toString() should return the correct format")
    @ParameterizedTest(name = "Input: {0}, toBic11: {1}, toBic8: {2}, toString: {3}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // BIC (Input) | Expected BIC-11 | Expected BIC-8 | Expected toString()
        "MARKDEFF      | MARKDEFFXXX     | MARKDEFF       | MARKDEFF   ",
        "MARKDEFFXXX   | MARKDEFFXXX     | MARKDEFF       | MARKDEFFXXX",
        "DBABDEFF500   | DBABDEFF500     | DBABDEFF       | DBABDEFF500"
    })
    void toBicFormats_ShouldReturnCorrectNormalizedStrings(String inputBic, String expectedBic11, String expectedBic8, String expectedToString) {
        assertThat(Bic.of(inputBic))
            .as("Format check for BIC %s", inputBic)
            .isBic11NormalizedEqualTo(expectedBic11)
            .isBic8EqualTo(expectedBic8)
            .hasToString(expectedToString);
    }

    @DisplayName("equality() and hashCode() should treat BIC-8 and BIC-11XXX as equal")
    @ParameterizedTest(name = "BIC-8: {0}, BIC-11: {1}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "MARKDEFF | MARKDEFFXXX",
        "NEDSZAJJ | NEDSZAJJXXX"
    })
    void equality_Bic8AndBic11XXX_ShouldBeEqual(String bic8Value, String bic11Value) {
        Bic bic8 = Bic.of(bic8Value);
        Bic bic11First = Bic.of(bic11Value);
        Bic bic11Second = Bic.of(bic11Value);

        // first chain: bic8 equals and hashCode checks
        assertThat(bic8)
            .as("bic8 equals and hashCode checks")
            .isEqualTo(bic8)
            .isEqualTo(bic11First)
            .isNotEqualTo(bic11First.toBic8()) // compare against String (should fail)
            .hasSameHashCodeAs(bic11First)
            .extracting(Object::hashCode)
            .isNotEqualTo(0);

        // second chain: bic11First equals and hashCode checks
        assertThat(bic11First)
            .as("bic11First equals and hashCode checks")
            .isEqualTo(bic11First)
            .isEqualTo(bic11Second)
            .isNotEqualTo(bic11First.toBic11()) // compare against String (should fail)
            .hasSameHashCodeAs(bic11Second)
            .extracting(Object::hashCode)
            .isNotEqualTo(0);
    }

    @DisplayName("equals() should not treat different branch codes as equal")
    @Test
    void equality_DifferentBICs_ShouldNotBeEqual() {
        Bic bic8 = Bic.of("MARKDEFF");
        Bic bic11Branch1 = Bic.of("MARKDEFF500");
        Bic bic11Branch2 = Bic.of("MARKDEFF100");

        assertThat(bic8)
            .isNotEqualTo(bic11Branch1);

        assertThat(bic11Branch1)
            .isNotEqualTo(bic11Branch2);
    }

    @DisplayName("compareTo() should compare based on BIC-11 string and handle null")
    @Test
    void compareTo_ShouldBeBasedOnBic11() {
        Bic bicA = Bic.of("MARKDEFF");    // MARKDEFFXXX
        Bic bicB = Bic.of("MARKDEFFXXX");
        Bic bicC = Bic.of("MARKDEFF500"); // MARKDEFF500
        Bic bicD = Bic.of("MARKUS33");    // MARKUS33XXX

        // first chain: equals (0) and null check
        assertThat(bicA.compareTo(bicB)).as("BICs should be equal by comparison").isZero();
        assertThatNullPointerException()
            .isThrownBy(() -> bicA.compareTo(null))
            .withMessage("Cannot compare Bic to null");

        // second chain: greater than (positive)
        assertThat(bicA.compareTo(bicC)).isGreaterThan(0);

        // third chain: less than (negative)
        assertThat(bicA.compareTo(bicD)).isLessThan(0);
    }

    @DisplayName("charAt() should return correct character and check bounds")
    @Test
    void charSequenceCharAt() {
        Bic bic = Bic.of("MARKDEFF500"); // 11 chars

        assertThat(bic)
            .returns('M', c -> c.charAt(0))
            .returns('0', c -> c.charAt(10));

        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> bic.charAt(-1))
            .withMessage("Index should be between 0 (inclusive) and 11 (exclusive), but was -1");
        assertThatIndexOutOfBoundsException()
            .isThrownBy(() -> bic.charAt(11))
            .withMessage("Index should be between 0 (inclusive) and 11 (exclusive), but was 11");
    }

    @DisplayName("subSequence() should return correct substring and check bounds")
    @Test
    void charSequenceSubSequence() {
        Bic bic = Bic.of("MARKDEFF"); // 8 chars

        assertThat(bic)
            .extracting(
                c -> c.subSequence(0, 8),
                c -> c.subSequence(4, 6),
                c -> c.subSequence(6, 8),
                c -> c.subSequence(4, 4)
            )
            .containsExactly(
                bic.toString(),
                "DE",
                "FF",
                ""
            );

        assertThatIndexOutOfBoundsException().isThrownBy(() -> bic.subSequence(-1, 2))
            .withMessage("Start index should be >= 0 (was: -1) and end index (exclusive) <= 8 (was: 2)");

        assertThatIndexOutOfBoundsException().isThrownBy(() -> bic.subSequence(0, bic.toString().length() + 1))
            .withMessage("Start index should be >= 0 (was: 0) and end index (exclusive) <= 8 (was: 9)");

        assertThatIndexOutOfBoundsException().isThrownBy(() -> bic.subSequence(5, 4))
            .withMessage("Start index should be >= 0 (was: 5) and end index (exclusive) <= 8 (was: 4)");
    }

    @DisplayName("subSequence() should return correct substring when range crosses into branch code (BIC-11)")
    @ParameterizedTest(name = "[{index}] subSequence({1},{2}) on ''{0}'' → ''{3}''")
    @CsvSource(delimiter = '|', value = {
        "MARKDEFF500 | 6  | 11 | FF500",
        "MARKDEFF500 | 0  | 11 | MARKDEFF500",
        "MARKDEFF500 | 8  | 11 | 500",
        "MARKDEFFXXX | 6  | 11 | FFXXX"
    })
    void charSequenceSubSequence_crossBoundaryBic11(String bicInput, int start, int end, String expected) {
        // exercises the toBic11() fallback path in subSequence() where end > BIC8_LENGTH
        assertThat(Bic.of(bicInput).subSequence(start, end))
            .as("subSequence(%d, %d) on %s", start, end, bicInput)
            .isEqualTo(expected);
    }

    @DisplayName("toBic11() should lazily compute and cache the BIC-11 string for BIC-8 objects")
    @Test
    void toBic11_lazyInit_bic8Object() {
        Bic bic = Bic.of("MARKDEFF");
        // first call: triggers lazy initialisation (bic11 == null branch)
        assertThat(bic.toBic11()).isEqualTo("MARKDEFFXXX");
        // second call: exercises the cached path (bic11 != null)
        assertThat(bic.toBic11()).isEqualTo("MARKDEFFXXX");
    }

    @DisplayName("toString() should return the original input string")
    @Test
    void toStringShouldReturnOriginalInput() {
        Bic bic8 = Bic.of("MARKDEFF");
        Bic bic11 = Bic.of("MARKDEFF500");

        assertThat(bic8).hasToString("MARKDEFF");
        assertThat(bic11).hasToString("MARKDEFF500");
    }

    // =========================================================================
    // Serialization tests (Memento Pattern)
    // =========================================================================

    /**
     * Verifies that a round-trip serialization/deserialization of a {@link Bic} instance
     * produces an equal object with the same string representation.
     * <p>
     * This covers the happy path of the Memento pattern: {@code writeReplace} emits the
     * {@link Bic.Memento} proxy, and {@code readResolve} reconstructs via {@link Bic#of}.
     *
     * @param bicInput a valid BIC string to round-trip
     * @throws IOException            if the byte stream cannot be written or read
     * @throws ClassNotFoundException never expected in this context
     */
    @DisplayName("Serialization round-trip should produce an equal Bic instance")
    @ParameterizedTest(name = "[{index}] round-trip: ''{0}''")
    @ValueSource(strings = {
        "MARKDEFF",
        "MARKDEFFXXX",
        "MARKDEFF500",
        "BHLSDEM1",
        "DEUTDEFF"
    })
    void serialization_ValidBic_ShouldPreserveState(String bicInput) throws IOException, ClassNotFoundException {
        Bic original = Bic.of(bicInput);
        final Bic bic = original;

        Bic restored = TestUtil.deserialize(TestUtil.serialize(bic));

        assertThat(restored)
            .as("Deserialized Bic must equal the original")
            .isNotNull()
            .isEqualTo(original)
            .hasToString(bicInput)
            .hasSameHashCodeAs(original);

        assertThat(restored.getBankCode())
            .as("Bank code must survive round-trip")
            .isEqualTo(original.getBankCode());

        assertThat(restored.getCountryCode())
            .as("Country code must survive round-trip")
            .isEqualTo(original.getCountryCode());
    }

    /**
     * Verifies that a BIC-8 remains a BIC-8 and a BIC-11 remains a BIC-11 after round-trip,
     * since {@code toString()} (and thus the Memento value) preserves the original form.
     *
     * @throws IOException            if the byte stream cannot be written or read
     * @throws ClassNotFoundException never expected in this context
     */
    @DisplayName("Serialization round-trip should preserve BIC-8 / BIC-11 distinction")
    @Test
    void testSerializationPreservesBicFormat() throws IOException, ClassNotFoundException {
        Bic originalBic8  = Bic.of("MARKDEFF");
        Bic originalBic11 = Bic.of("MARKDEFFXXX");
        final Bic bic = originalBic8;

        Bic restoredBic8  = TestUtil.deserialize(TestUtil.serialize(bic));
        final Bic bic1 = originalBic11;
        Bic restoredBic11 = TestUtil.deserialize(TestUtil.serialize(bic1));

        assertThat(restoredBic8.isBic8())
            .as("BIC-8 must remain BIC-8 after round-trip")
            .isTrue();
        assertThat(restoredBic11.isBic11())
            .as("BIC-11 must remain BIC-11 after round-trip")
            .isTrue();

        // BIC-8 and BIC-11XXX are equal by value but differ in format
        assertThat(restoredBic8).isEqualTo(restoredBic11);
        assertThat(restoredBic8.toString()).isNotEqualTo(restoredBic11.toString());
    }

    /**
     * Verifies that the serialized byte stream references the {@link Bic.Memento} proxy class
     * and not {@code Bic} directly, confirming that {@code writeReplace} is invoked.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Serialized stream must reference the Memento proxy, not Bic directly")
    @Test
    void testSerializedFormUsesMementoProxy() throws IOException {
        byte[] bytes = TestUtil.serialize(Bic.of("MARKDEFF"));
        String streamContent = new String(bytes, StandardCharsets.UTF_8);

        assertThat(streamContent)
            .as("Serialized stream must reference the Memento proxy class")
            .contains("Memento");
        assertThat(streamContent)
            .as("Serialized stream must not reference Bic directly as the top-level type")
            .doesNotContain("de.speedbanking.bic.Bic\n");
    }

    /**
     * Verifies that attempting to deserialize a raw {@code Bic} object directly
     * (bypassing the Memento proxy) is rejected.
     * <p>
     * The byte stream is manipulated by replacing the {@link Bic.Memento} class
     * descriptor with the {@link Bic} class descriptor, simulating a byte-stream
     * injection attack.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Direct deserialization of Bic bypassing Memento must be rejected")
    @Test
    void deserialization_DirectBic_ShouldThrowException() throws IOException {
        byte[] mementoBytes = TestUtil.serialize(Bic.of("MARKDEFF"));
        final byte[] stream = mementoBytes;

        byte[] tamperedBytes = TestUtil.replaceClassName(stream, Bic.Memento.class.getName(), Bic.class.getName());
        final byte[] bytes = tamperedBytes;

        assertThat(catchThrowable(() -> TestUtil.deserialize(bytes)))
            .as("Direct deserialization of Bic must be rejected")
            .isInstanceOfAny(InvalidClassException.class, StreamCorruptedException.class, IOException.class);
    }

    /**
     * Verifies that serialization followed by deserialization produces an object that
     * is equal to the original by value but is a distinct instance (no reference sharing).
     *
     * @throws IOException            if the byte stream cannot be written or read
     * @throws ClassNotFoundException never expected in this context
     */
    @DisplayName("Deserialized Bic instance must be distinct from the original")
    @Test
    void testDeserializedInstanceIsDistinct() throws ClassNotFoundException, IOException {
        Bic original = Bic.of("DEUTDEFF");
        final Bic bic = original;
        Bic restored = TestUtil.deserialize(TestUtil.serialize(bic));

        assertThat(restored)
            .isNotSameAs(original)
            .isEqualTo(original);
    }

    /**
     * Verifies that {@link Bic.Memento#readResolve()} throws {@link InvalidObjectException}
     * when the stored value is not a valid BIC string.
     * <p>
     * This covers the {@code catch (RuntimeException)} branch in {@code readResolve()}, which
     * wraps a validation failure into an {@link InvalidObjectException}.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Memento.readResolve() must reject an invalid BIC stored in the stream")
    @Test
    void testMementoReadResolveRejectsInvalidBic() throws IOException {
        byte[] corruptStream = TestUtil.buildMementoStream(Bic.of("MARKDEFF"), "NOT_A_BIC!!");

        assertThat(catchThrowable(() -> TestUtil.deserialize(corruptStream)))
            .as("readResolve() must reject an invalid BIC stored in the Memento")
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("Cannot restore Bic from serialized form");
    }

    /**
     * Verifies that {@link Bic.Memento#readObject(ObjectInputStream)} throws
     * {@link InvalidObjectException} when the stream contains an unsupported version number.
     * <p>
     * This covers the {@code version != STREAM_VERSION} branch. The stream is crafted with
     * version {@code 99L} instead of the expected {@code 1L}.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Memento.readObject() must reject a stream with an unsupported version")
    @Test
    void testMementoReadObjectRejectsUnknownStreamVersion() throws IOException {
        byte[] corruptStream = TestUtil.buildMementoStream(Bic.of("MARKDEFF"), 99L, "MARKDEFF");

        assertThat(catchThrowable(() -> TestUtil.deserialize(corruptStream)))
            .as("readObject() must reject a Memento with an unsupported stream version")
            .isInstanceOfAny(InvalidObjectException.class, EOFException.class)
            .hasMessageContaining("Unsupported Bic Memento stream version: 99");
    }

    /**
     * Verifies that {@link Bic#readObjectNoData()} throws {@link InvalidObjectException}.
     * <p>
     * {@code readObjectNoData()} is called by the JVM when a superclass added
     * {@code Serializable} after the subclass was already serialized, leaving no instance
     * data for the subclass in the stream. Since {@code Bic} is {@code final} and has no
     * subclasses, the only way to reach this path in a unit test is via reflection.
     *
     * @throws Exception if reflection access fails
     */
    @DisplayName("readObjectNoData() must throw InvalidObjectException")
    @Test
    void readObjectNoData_DirectInvocation_ShouldThrowException() throws Exception {
        Bic bic = Bic.of("MARKDEFF");

        Throwable cause = TestUtil.invokeSerializationGuard(bic, "readObjectNoData", new Class<?>[0]);

        assertThat(cause)
            .as("readObjectNoData() must throw InvalidObjectException")
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("must be deserialized via its Memento proxy");
    }

    /**
     * Verifies that {@link Bic#readObject(ObjectInputStream)} delegates to
     * {@link Bic#readObjectNoData()} and throws {@link InvalidObjectException}.
     * <p>
     * {@code readObject()} is unreachable in normal deserialisation because {@code writeReplace()}
     * always substitutes the Memento proxy. The only way to cover this branch is via reflection.
     *
     * @throws Exception if reflection access fails
     */
    @DisplayName("readObject() must throw InvalidObjectException when invoked directly")
    @Test
    void readObject_DirectInvocation_ShouldThrowException() throws Exception {
        Bic bic = Bic.of("MARKDEFF");

        Throwable cause = TestUtil.invokeSerializationGuard(bic, "readObject",
            new Class<?>[] {ObjectInputStream.class}, (ObjectInputStream) null);

        assertThat(cause)
            .as("readObject() must throw InvalidObjectException")
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("must be deserialized via its Memento proxy");
    }

}
