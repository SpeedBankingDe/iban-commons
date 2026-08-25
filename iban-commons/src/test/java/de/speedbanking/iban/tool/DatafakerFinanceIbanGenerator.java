package de.speedbanking.iban.tool;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import de.speedbanking.iban.IbanRegistry;
import de.speedbanking.iban.util.IbanPatternConverter;

import java.util.List;
import java.util.stream.Stream;

/**
 * Utility for generating IBAN registry format mappings for Datafaker.
 * <p>
 * This tool iterates through the {@link IbanRegistry} and prints formatted
 * map-population statements, aligned for easy integration into Datafaker's
 * {@code net.datafaker.providers.base.Finance} class.
 */
public final class DatafakerFinanceIbanGenerator {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DatafakerFinanceIbanGenerator() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Generates the formatted map entries and prints them to system out.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println(String.join(System.lineSeparator(), formatEntries()));
    }

    static List<String> formatEntries() {
        return Stream.of(IbanRegistry.values())
            .filter(IbanRegistry::isBaseCountry)
            .sorted(comparing(IbanRegistry::getCountryCode))
            .map(DatafakerFinanceIbanGenerator::formatEntry)
            .collect(toList());
    }

    /**
     * Formats a single registry entry into a java map put statement.
     *
     * @param country the registry entry to format
     * @return a formatted string line
     */
    static String formatEntry(IbanRegistry country) {
        String bbanPattern = country.getBbanPattern();
        String bbanRegex = IbanPatternConverter.convertToRegex(bbanPattern);

        String mapPut = String.format("ibanFormats.put(\"%s\", \"%s\");",
            country.getCountryCode(),
            bbanRegex);

        return String.format("%-58s // %s %-25s : %s",
            mapPut,
            country.getCountryFlag(),
            country.getCountryName(),
            bbanPattern);
    }

}
