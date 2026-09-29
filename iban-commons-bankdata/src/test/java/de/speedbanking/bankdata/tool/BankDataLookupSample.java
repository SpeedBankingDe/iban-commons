package de.speedbanking.bankdata.tool;

import de.speedbanking.bankdata.BankData;
import de.speedbanking.bankdata.BankDataLookup;
import de.speedbanking.bankdata.BankDataRegistry;
import de.speedbanking.bankdata.log.NaiveLogFormatter;
import de.speedbanking.iban.Iban;
import de.speedbanking.iban.IbanBuilder;
import de.speedbanking.iban.IbanBuilder.HasBranchCode;
import de.speedbanking.iban.IbanRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sample program demonstrating {@link BankDataLookup}: generates a handful of random, valid
 * IBANs, each carrying a real, existing bank code drawn from the bundled bank data, and looks
 * up the resolved bank name and BIC for each.
 * <p>
 * Run with:
 * <pre>
 * cd iban-commons-bankdata
 * mvn test-compile exec:java@bank-data-lookup-sample
 * </pre>
 */
public final class BankDataLookupSample {

    private static final int SAMPLE_COUNT = 50;

    private BankDataLookupSample() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    public static void main(String[] args) {
        NaiveLogFormatter.installOnRootLogger();

        List<String> countryCodes = new ArrayList<>(BankDataLookup.getSupportedCountryCodes());
        if (countryCodes.isEmpty()) {
            System.err.printf("No countries%n");
            return;
        }

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            String countryCode = randomElement(countryCodes);
            Iban iban = randomIbanWithRealBankCode(countryCode);

            Optional<BankData> bankData = BankDataLookup.byIban(iban);
            if (bankData.isPresent()) {
                BankData data = bankData.get();
                System.out.printf("%-30s ==> BIC=%-14s Bank=%s, City=%s%n", iban, data.getBic(), data.getBankName(), data.getCity());
            } else {
                System.out.printf("%-30s ==> no bank data found%n", iban);
            }
        }
    }

    /**
     * Builds a random, valid IBAN for the given country whose bank code (and, where the country's
     * BBAN carries one, branch code) is an existing entry from the bundled bank data, so the
     * subsequent {@link BankDataLookup#byIban(Iban)} call actually resolves to a real institution
     * instead of "not found". Leaving the branch code to a random value (the builder's default)
     * would almost never hit a real bank+branch combination for a country whose bank data is keyed
     * on both, since a bank code typically has many branches in the bundled data but the builder
     * has no way to know which ones are real.
     * <p>
     * Only bank codes with a known BIC are considered: some sources (e.g. the Belgian NBB/BNB
     * directory) list currently unassigned codes as real rows with a placeholder institution
     * name ("VRIJ", Dutch for "free") and no BIC, which would otherwise look like a lookup bug
     * in this sample rather than the accurately reproduced source data it actually is.
     *
     * @param countryCode the ISO 3166-1 alpha-2 country code
     * @return a valid IBAN carrying a real, BIC-assigned bank code (and branch code, if applicable)
     */
    private static Iban randomIbanWithRealBankCode(String countryCode) {
        Map<String, BankData> bankDataByCode = BankDataRegistry.getCache(countryCode).getOrLoadSync();
        List<BankData> assignedBankData = new ArrayList<>();
        for (BankData bankData : bankDataByCode.values()) {
            if (bankData.getBic() != null) {
                assignedBankData.add(bankData);
            }
        }
        if (assignedBankData.isEmpty()) {
            throw new IllegalStateException("No BIC-assigned bank data available for country " + countryCode);
        }
        BankData bankData = randomElement(assignedBankData);

        IbanBuilder<?> builder = IbanRegistry.getByCode(countryCode).builder()
            .bankCode(bankData.getBankCode());
        if (builder instanceof HasBranchCode && bankData.getBranchCode() != null) {
            ((HasBranchCode<?>) builder).branchCode(bankData.getBranchCode());
        }
        return builder.build();
    }

    private static <T> T randomElement(List<T> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

}
