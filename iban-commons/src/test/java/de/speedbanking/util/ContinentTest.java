package de.speedbanking.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JUnit tests for {@link Continent}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class ContinentTest {

    @DisplayName("getCode() returns the correct two-letter continent code")
    @ParameterizedTest(name = "[{index}] {0} -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "AFRICA        | AF",
        "ANTARCTICA    | AN",
        "ASIA          | AS",
        "EUROPE        | EU",
        "NORTH_AMERICA | NA",
        "OCEANIA       | OC",
        "SOUTH_AMERICA | SA"
    })
    void getCode_knownConstants_returnsCorrectCode(Continent continent, String expectedCode) {
        assertThat(continent.getCode()).isEqualTo(expectedCode);
    }

    @DisplayName("getContinentName() returns the correct English continent name")
    @ParameterizedTest(name = "[{index}] {0} -> ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "AFRICA        | Africa",
        "ANTARCTICA    | Antarctica",
        "ASIA          | Asia",
        "EUROPE        | Europe",
        "NORTH_AMERICA | North America",
        "OCEANIA       | Oceania",
        "SOUTH_AMERICA | South America"
    })
    void getContinentName_knownConstants_returnsCorrectName(Continent continent, String expectedName) {
        assertThat(continent.getContinentName()).isEqualTo(expectedName);
    }

    @DisplayName("fromCode() returns the correct constant for every assigned code")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Continent.class)
    void fromCode_assignedCode_returnsMatchingConstant(Continent expectedContinent) {
        assertThat(Continent.fromCode(expectedContinent.getCode()))
            .isSameAs(expectedContinent);
    }

    @DisplayName("fromCode() returns null for null or empty input")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @NullAndEmptySource
    void fromCode_nullOrEmpty_returnsNull(String code) {
        assertThat(Continent.fromCode(code)).isNull();
    }

    @DisplayName("fromCode() returns null for unassigned, wrong-case or invalid codes")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @ValueSource(strings = {"eu", "EU ", "XX", "A", "EUR"})
    void fromCode_invalidOrUnassigned_returnsNull(String code) {
        assertThat(Continent.fromCode(code)).isNull();
    }

    @DisplayName("toString() returns format matching getClass().getSimpleName()[NAME]")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Continent.class)
    void toString_allConstants_matchesExpectedPattern(Continent continent) {
        assertThat(continent).hasToString("%s[%s, code=%s, continentName=%s]",
            continent.getDeclaringClass().getSimpleName(), continent.name(), continent.getCode(), continent.getContinentName());
    }

    @DisplayName("values() contains exactly 7 constants")
    @Test
    void enum_values_containsExactlySevenConstants() {
        assertThat(Continent.values()).hasSize(7);
    }

    @DisplayName("valueOf() resolves every constant from its exact name")
    @ParameterizedTest(name = "[{index}] ''{0}''")
    @EnumSource(Continent.class)
    void enum_valueOf_resolvesEveryConstant(Continent continent) {
        assertThat(Continent.valueOf(continent.name())).isSameAs(continent);
    }

    @DisplayName("valueOf() throws IllegalArgumentException for unknown names")
    @Test
    void enum_valueOf_unknownName_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Continent.valueOf("INVALID_CONTINENT_NAME"));
    }

    @DisplayName("Every continent code is exactly 2 uppercase ASCII letters")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Continent.class)
    void invariant_allCodes_areTwoUppercaseAsciiLetters(Continent continent) {
        assertThat(continent.getCode())
            .hasSize(2)
            .matches("^[A-Z]{2}$");
    }

    @DisplayName("No duplicate codes exist across all constants")
    @Test
    void invariant_allCodes_areUnique() {
        Set<String> seenCodes = new LinkedHashSet<>();
        for (Continent continent : Continent.values()) {
            boolean isUnique = seenCodes.add(continent.getCode());
            assertThat(isUnique)
                .as("Duplicate code detected: %s", continent.getCode())
                .isTrue();
        }
    }

    @DisplayName("No duplicate continent names exist across all constants")
    @Test
    void invariant_allContinentNames_areUnique() {
        Set<String> seenNames = new LinkedHashSet<>();
        for (Continent continent : Continent.values()) {
            boolean isUnique = seenNames.add(continent.getContinentName());
            assertThat(isUnique)
                .as("Duplicate name detected: %s", continent.getContinentName())
                .isTrue();
        }
    }

}
