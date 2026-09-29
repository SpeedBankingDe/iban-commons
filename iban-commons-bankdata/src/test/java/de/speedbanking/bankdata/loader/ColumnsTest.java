package de.speedbanking.bankdata.loader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link Columns}, using {@link BankDataLoaderPl.Column} (indices 1, 4, 9, 10, 19)
 * as a real-world {@link ColumnDefinition} enum rather than a bespoke test fixture.
 */
@SuppressWarnings("checkstyle:MethodName")
final class ColumnsTest {

    private final Columns<BankDataLoaderPl.Column> columns = Columns.of(BankDataLoaderPl.Column.class);

    @Test
    void minColumnCount_isOneMoreThanHighestColumnIndex() {
        assertThat(columns.getMinColumnCount()).isEqualTo(20); // highest index is BIC(19)
    }

    @Test
    void orEmpty_indexWithinBounds_returnsTrimmedValue() {
        List<String> fields = Arrays.asList("0", "  Bank Name  ", "2", "3", "4");

        assertThat(columns.getOrEmpty(BankDataLoaderPl.Column.BANK_NAME, fields)).isEqualTo("Bank Name");
    }

    @Test
    void orEmpty_nullField_returnsEmptyString() {
        List<String> fields = Arrays.asList("0", null, "2", "3", "4");

        assertThat(columns.getOrEmpty(BankDataLoaderPl.Column.BANK_NAME, fields)).isEmpty();
    }

    @Test
    void orEmpty_indexBeyondFieldCount_returnsEmptyString() {
        List<String> fields = Arrays.asList("0", "1");

        assertThat(columns.getOrEmpty(BankDataLoaderPl.Column.BIC, fields)).isEmpty();
    }

    @Test
    void orNull_blankValue_returnsNull() {
        List<String> fields = Arrays.asList("0", "   ", "2", "3", "4");

        assertThat((Object) columns.getOrNull(BankDataLoaderPl.Column.BANK_NAME, fields)).isNull();
    }

    @Test
    void orNull_presentValue_returnsTrimmedValue() {
        List<String> fields = Arrays.asList("0", " Musterbank ", "2", "3", "4");

        assertThat(columns.getOrNull(BankDataLoaderPl.Column.BANK_NAME, fields)).isEqualTo("Musterbank");
    }

    @Test
    void stringRepresentation_listsEveryColumnNameAndIndex() {
        String result = columns.toString();

        assertThat(result)
            .startsWith("Column[")
            .contains("SETTLEMENT_CODE(4)")
            .contains("BANK_NAME(1)")
            .contains("POSTAL_CODE(9)")
            .contains("CITY(10)")
            .contains("BIC(19)");
    }

}
