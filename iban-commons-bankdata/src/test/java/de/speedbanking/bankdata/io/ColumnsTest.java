package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;

import de.speedbanking.bankdata.io.BankDataFormat.Column;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link Columns}, using {@link BankDataFormat.Column} (indices 0 to 7, the last two
 * optional) as a real-world {@link ColumnDefinition} enum rather than a bespoke test fixture.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class ColumnsTest {

    private final Columns<Column> columns = Columns.of(Column.class);

    @Test
    void getMinColumnCount_optionalTrailingColumns_countsUpToLastRequiredColumn() {
        assertThat(columns.getMinColumnCount()).isEqualTo(6); // CITY(5) is the last required column
    }

    @Test
    void getOrEmpty_indexWithinBounds_returnsTrimmedValue() {
        List<String> fields = Arrays.asList("0", "1", "2", "  Bank Name  ", "4", "5");

        assertThat(columns.getOrEmpty(Column.BANK_NAME, fields)).isEqualTo("Bank Name");
    }

    @Test
    void getOrEmpty_nullField_returnsEmptyString() {
        List<String> fields = Arrays.asList("0", "1", "2", null, "4", "5");

        assertThat(columns.getOrEmpty(Column.BANK_NAME, fields)).isEmpty();
    }

    @Test
    void getOrEmpty_indexBeyondFieldCount_returnsEmptyString() {
        List<String> fields = Arrays.asList("0", "1", "2", "3", "4", "5");

        assertThat(columns.getOrEmpty(Column.CHECK_DIGIT_METHOD, fields)).isEmpty();
    }

    @Test
    void getOrNull_blankValue_returnsNull() {
        List<String> fields = Arrays.asList("0", "1", "2", "   ", "4", "5");

        assertThat((Object) columns.getOrNull(Column.BANK_NAME, fields)).isNull();
    }

    @Test
    void getOrNull_presentValue_returnsTrimmedValue() {
        List<String> fields = Arrays.asList("0", "1", "2", " Musterbank ", "4", "5");

        assertThat(columns.getOrNull(Column.BANK_NAME, fields)).isEqualTo("Musterbank");
    }

    @Test
    void stringRepresentation_listsEveryColumnNameAndIndex() {
        String result = columns.toString();

        assertThat(result)
            .startsWith("Column[")
            .contains("BANK_CODE(0)")
            .contains("BANK_NAME(3)")
            .contains("CITY(5)")
            .contains("CHECK_DIGIT_METHOD(7)");
    }

}
