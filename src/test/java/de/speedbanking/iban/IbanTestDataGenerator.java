package de.speedbanking.iban;

import java.util.ArrayList;
import java.util.List;

final class IbanTestDataGenerator {

    List<String> createTestDatatAllCountries() {
        List<String> lines = new ArrayList<>();
        for (IbanRegistry ir : IbanRegistry.values()) {
            Iban iban = Iban.of(ir.getIbanExample());
            String str = iban.getBranchCode();
            String line = String.join(" | ",
                String.format("\"%-" + IbanRegistry.MAX_IBAN_LENGTH + "s", iban),
                String.valueOf(ir.getIbanLength()),
                ir.getCountryCode(),
                ir.getCountryFlag(),
                iban.getCheckDigits(),
                String.format("%-" + IbanRegistry.MAX_BBAN_LENGTH + "s", iban.getBban()),
                String.format("%-10s", iban.getBankCode()),
                String.format("%-6s", str == null ? "(null)" : str),
                String.format("%-24s\"", iban.getAccountNumber())
            );
            lines.add(line);

            if (iban.getBankCode() == null) {
                System.err.println(ir.getCountryCode() + " (" + ir.getCountryName() + "): missing bank code");
            }
            if (iban.getAccountNumber() == null) {
                System.err.println(ir.getCountryCode() + " (" + ir.getCountryName() + "): missing account number");
            }
        }
        return lines;
    }

    public static void main(String[] args) {
        System.out.print(String.join("," + System.lineSeparator(),
            new IbanTestDataGenerator().createTestDatatAllCountries()) + System.lineSeparator());
    }

}
