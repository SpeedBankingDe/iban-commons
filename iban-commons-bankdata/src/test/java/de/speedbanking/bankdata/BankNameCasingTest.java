package de.speedbanking.bankdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BankNameCasing}, exercised both with synthetic cases and with real
 * all-caps names taken from the bundled ES/FR/NL/BE/DE/PL CSV snapshots.
 */
@SuppressWarnings("checkstyle:MethodName")
final class BankNameCasingTest {

    @Test
    void displayCase_null_returnsNull() {
        assertThat(BankNameCasing.toDisplayCase(null)).isNull();
    }

    @Test
    void displayCase_empty_returnsEmpty() {
        assertThat(BankNameCasing.toDisplayCase("")).isEmpty();
    }

    @Test
    void displayCase_alreadyMixedCase_returnsUnchanged() {
        assertThat(BankNameCasing.toDisplayCase("MONETA Money Bank, a.s.")).isEqualTo("MONETA Money Bank, a.s.");
    }

    @Test
    void displayCase_simpleAllCapsName_titleCased() {
        assertThat(BankNameCasing.toDisplayCase("BANQUE STELLANTIS FRANCE")).isEqualTo("Banque Stellantis France");
    }

    @Test
    void displayCase_legalFormAbbreviation_leftUntouched() {
        assertThat(BankNameCasing.toDisplayCase("ABN AMRO BANK N.V.")).isEqualTo("Abn Amro Bank N.V.");
        assertThat(BankNameCasing.toDisplayCase("ARAB BANK EUROPE SA")).isEqualTo("Arab Bank Europe SA");
        assertThat(BankNameCasing.toDisplayCase("BELFIUS BANK NV")).isEqualTo("Belfius Bank NV");
    }

    @Test
    void displayCase_initialsToken_leftUntouched() {
        assertThat(BankNameCasing.toDisplayCase("J.P. MORGAN SE, SUCURSAL EN ESPAÑA"))
            .isEqualTo("J.P. Morgan SE, Sucursal En España");
    }

    @Test
    void displayCase_internalHyphen_bothPartsCapitalized() {
        assertThat(BankNameCasing.toDisplayCase("BANQUE POPULAIRE AUVERGNE RHONE-ALPES"))
            .isEqualTo("Banque Populaire Auvergne Rhone-Alpes");
    }

    @Test
    void displayCase_unicodeLetters_handledCorrectly() {
        assertThat(BankNameCasing.toDisplayCase("BANCO FINANTIA, S.A., SUCURSAL EN ESPAÑA"))
            .isEqualTo("Banco Finantia, S.A., Sucursal En España");
        assertThat(BankNameCasing.toDisplayCase("KOMERČNÍ BANKA, A.S.")).isEqualTo("Komerční Banka, A.S.");
    }

    @Test
    void displayCase_unrecognizedShortToken_stillTitleCased() {
        // "GF" isn't a known legal form (it's Deutsche Bank's internal "Geschäftsfeld" code), so
        // it is title-cased like any other unrecognized token - an accepted limitation.
        assertThat(BankNameCasing.toDisplayCase("DZ BANK GF AIS")).isEqualTo("Dz Bank Gf Ais");
    }

}
