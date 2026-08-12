package de.speedbanking.iban.tool;

import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility tool for generating IBAN test data strings based on the {@link IbanRegistry}.
 * <p>
 * This generator iterates through all registered countries, creates a sample IBAN,
 * and formats the metadata into a pipe-separated string suitable for use in
 * parameterized tests (e.g., {@code @CsvSource}).
 */
final class IbanTestDataGenerator {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private IbanTestDataGenerator() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Generates a list of formatted test data strings for all countries in the registry.
     * <p>
     * Each line contains the full IBAN, length, country details, and decomposed BBAN components.
     * Missing bank codes or account numbers are logged to system error.
     *
     * @return a list of pipe-separated strings containing IBAN metadata
     */
    static List<String> createTestDatatAllCountries() {
        List<String> lines = new ArrayList<>();
        String formatIban = "%-" + IbanRegistry.MAX_IBAN_LENGTH + "s";
        String formatBban = "%-" + IbanRegistry.MAX_BBAN_LENGTH + "s";
        for (IbanRegistry countryData : IbanRegistry.values()) {
            Iban iban = Iban.of(countryData.getIbanExample());
            String str = iban.getBranchCode();
            String line = String.join(" | ",
                "\"" + String.format(formatIban, iban),
                String.valueOf(countryData.getIbanLength()),
                countryData.getCountryCode(),
                countryData.getCountryFlag(),
                iban.getCheckDigits(),
                String.format(formatBban, iban.getBban()),
                String.format("%-10s", iban.getBankCode()),
                String.format("%-6s", str == null ? "(null)" : str),
                String.format("%-24s", iban.getAccountNumber()) + "\""
            );
            lines.add(line);

            if (iban.getBankCode() == null) {
                System.err.printf("%s (%s): missing bank code%n", countryData.getCountryCode(), countryData.getCountryName());
            }
            if (iban.getAccountNumber() == null) {
                System.err.printf("%s (%s): missing account number%n", countryData.getCountryCode(), countryData.getCountryName());
            }
        }
        return lines;
    }

    public static void main(String[] args) {
        String nl = System.lineSeparator();
        System.out.print(String.join("," + nl, createTestDatatAllCountries()) + nl);
    }

}
