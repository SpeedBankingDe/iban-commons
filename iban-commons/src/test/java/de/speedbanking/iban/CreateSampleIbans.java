package de.speedbanking.iban;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Command-line utility to generate sample IBANs for specified country codes.
 * <p>
 * If no country codes are provided via command-line arguments, 10 distinct
 * random countries are selected from {@link IbanRegistry#ALL_COUNTRIES}.
 */
public final class CreateSampleIbans {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CreateSampleIbans() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Main entry point executing sample IBAN generation.
     *
     * @param args optional country codes to generate sample IBANs for
     */
    public static void main(String[] args) {
        Stream<IbanRegistry> registries = (args != null && args.length > 0)
            ? Arrays.stream(args).map(IbanRegistry::getByCode).filter(Objects::nonNull)
            : getRandomCountries(10);

        registries
            .map(entry -> entry.builder()
                .accountNumber("85354")
                //.bankCode("4711")
                .build())
            .forEach(System.out::println);
    }

    private static Stream<IbanRegistry> getRandomCountries(int count) {
        List<IbanRegistry> all = IbanRegistry.ALL_COUNTRIES;
        return ThreadLocalRandom.current()
            .ints(0, all.size())
            .limit(Math.min(count, all.size()))
            .mapToObj(all::get);
    }

}
