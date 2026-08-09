package de.speedbanking.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class RegexSimplifierTest extends Assertions {

    /**
     * Tests the simplify method using various inputs for consolidation,
     * provided via the @CsvSource annotation.
     *
     * The CSV format is: "inputRegex", "expectedRegex", "description"
     * Note: Curly braces {} must be escaped or carefully handled within the CSV strings.
     */
    @ParameterizedTest(name = "[{index}] {2}: {0} -> {1}")
    @CsvSource({
        // 1. Original example: Consolidation of [0-9] blocks
        "'^BI[0-9]{2}[0-9]{5}[0-9]{5}[0-9]{11}[0-9]{2}$', '^BI[0-9]{25}$', 'IBAN example consolidation'",

        // 2. Mixed classes: Should consolidate [A-Z] and [0-9] separately
        "'[A-Z]{3}[A-Z]{1}[0-9]{4}[0-9]{2}[A-Z]{2}$', '[A-Z]{4}[0-9]{6}[A-Z]{2}$', 'Mixed character classes'",

        // 3. No consolidation needed: Single blocks
        "'^[A-Z]{4}[0-9]{2}$', '^[A-Z]{4}[0-9]{2}$', 'No consolidation needed'",

        // 4. Consolidation with a mix of char classes (alphanumeric [A-Za-z0-9])
        "'[A-Za-z0-9]{5}[A-Za-z0-9]{5}abc[0-9]{1}', '[A-Za-z0-9]{10}abc[0-9]{1}', 'Alphanumeric class and literal'",

        // 5. Literal repetition consolidation
        "'.{3}.{7}[0-9]{2}', '.{10}[0-9]{2}', 'Wildcard dot consolidation'",

        // 6. Test with literal characters that are NOT part of a class block
        "'[0-9]{1}X[0-9]{2}[0-9]{3}', '[0-9]{1}X[0-9]{5}', 'Literal character separating blocks'",

        // 7. Edge case: Empty string input
        "'', '', 'Empty string input'",

        // 8. Escaped characters: Escaped dot should not be treated as wildcard dot
        "'\\.[0-9]{2}[0-9]{3}', '\\.[0-9]{5}', 'Escaped dot followed by consolidatable block'"
    })
    @DisplayName("Tests regex simplification using @CsvSource")
    void simplify_validInput_returnsExpectedResult(String inputRegex, String expectedRegex, String description) {
        String actualRegex = RegexSimplifier.simplify(inputRegex);
        assertEquals(expectedRegex, actualRegex, "The simplified regex did not match the expected result for: " + description);
    }

    @DisplayName("Tests null input handling")
    @ParameterizedTest
    @NullSource
    void simplify_nullInput_returnsNull(String inputRegex) {
        assertNull(RegexSimplifier.simplify(inputRegex));
    }

    @DisplayName("Tests private constructor reflection call for coverage")
    @Test
    void constructor_private_throwsUnsupportedOperationException() throws NoSuchMethodException {
        Constructor<RegexSimplifier> constructor = RegexSimplifier.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

}
