package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanIsValid;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanOf;
import static de.speedbanking.iban.IbanAssertions.assertThatInvalidIbanException;
import static de.speedbanking.iban.IbanRegistry.FI;
import static de.speedbanking.iban.IbanRegistry.FR;
import static de.speedbanking.iban.IbanRegistry.GB;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatComparable;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assumptions.assumeThat;

import de.speedbanking.iban.IbanRegistrySource.CountryType;
import de.speedbanking.test.BooleanConverter;
import de.speedbanking.test.TestUtil;
import de.speedbanking.util.Iso3166Alpha2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

/**
 * Unit tests for the new immutable {@link Iban} class, covering IBAN validation and component extraction.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
class IbanTest {

    /**
     * Tests {@link Iban#of(CharSequence)} with various valid IBANs.
     *
     * @param ibanInput the valid IBAN string to test
     */
    @DisplayName("Valid IBAN parsing and equality")
    @ParameterizedTest(name = "[{index}] Valid IBAN ''{0}''")
    @ValueSource(strings = {
        "DE 91 10 00 00 00 01 23 45 67 89",         // Germany
        "FR 14 20 04 10 10 05 05 00 01 3M 02 60 6", // France
        "GB33 BUKB 2020 1555 5555 55",              // United Kingdom
        "NL91 ABNA 0417 1643 00"                    // Netherlands
    })
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES)
    void of_ValidIban_ShouldReturnIban(String ibanInput) {
        String ibanInputNorm = ibanInput.replace(" ", "");

        try {
            IbanConfig.ALLOW_SPACE.enable();

            assertThatCode(
                () -> Iban.of(ibanInput))
                .as("IBAN '%s' is valid and should be instantiable", ibanInput)
                .doesNotThrowAnyException();

            Iban iban1 = assertThatIbanOf(ibanInput)
                .as("IBAN instance 1 must not be null and match input string")
                .isNotNull()
                .hasToString(ibanInputNorm)
                .actual();

            assertThatCode(
                () -> Iban.of(ibanInputNorm))
                .as("Normalized IBAN '%s' is valid and should be instantiable", ibanInput)
                .doesNotThrowAnyException();

            Iban iban2 = assertThatIbanOf(ibanInputNorm)
                .as("IBAN instance 2 must not be null and match input string")
                .isNotNull()
                .hasToString(ibanInputNorm)
                .isEqualTo(iban1)
                .actual();

            assertThatComparable(iban1).isEqualTo(iban2);

            assertThat(Iban.tryParse(ibanInput))
                .isPresent()
                .contains(iban1);

            assertThat(Iban.tryParseOrNull(ibanInput))
                .isEqualTo(iban1);

            assertThatIbanIsValid(ibanInputNorm);

            String invalidIban = ibanInput.substring(0, ibanInput.length() - 1) + "X";
            assertThat(Iban.isValid(invalidIban)).isFalse();

        } finally {
            IbanConfig.ALLOW_SPACE.reset();
        }

    }

    /**
     * Tests {@link Iban#of(CharSequence)} with various invalid IBANs,
     * expecting an {@link InvalidIbanException} with a specific message pattern.
     *
     * @param ibanInput              the invalid IBAN string to test
     * @param expectedMessagePattern the regex pattern for the expected exception message
     */
    @DisplayName("Invalid IBAN throws InvalidIbanException")
    @ParameterizedTest(name = "[{index}] Invalid IBAN ''{0}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // IBAN (Input)                | ValidationError (Enum)   | Expected Message Pattern
        "(null)                        | EMPTY                    | IBAN is null or empty",
        "''                            | EMPTY                    | IBAN is null or empty",
        "PS92pals000000000400123456702 | ILLEGAL_CHARACTERS       | IBAN contains illegal character(s)",
        "ps92pals000000000400123456702 | ILLEGAL_CHARACTERS       | IBAN contains illegal character(s)",
        "Ps92pals000000000400123456702 | ILLEGAL_CHARACTERS       | IBAN contains illegal character(s)",
        "XX12345678901234567890        | INVALID_COUNTRY          | IBAN has invalid country code",
        "DE123                         | INCORRECT_LENGTH         | IBAN has incorrect length",
        "DE91BHLSDEM1123456789         | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "GB33BUKB2020155555567         | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "GP464Q85KW3RR7JWATF3TGAZ6JNI  | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "DE91100000000123456780        | INVALID_CHECKSUM         | IBAN violates ISO 7064 Mod 97-10 checksum check"
    })
    void of_InvalidIban_ShouldThrowException(String ibanInput, IbanValidationError expectedValidationError, String expectedMessagePattern) {
        String ibanInputNorm = ibanInput == null ? "" : ibanInput.replace(" ", "");

        IbanValidator.setLastReason(null);

        assertThatInvalidIbanException()
            .isThrownBy(() -> Iban.ofNormalized(ibanInput))
            .withCause(null)
            .withMessage(expectedMessagePattern + " (" + expectedValidationError + ")" + (ibanInputNorm.isEmpty() ? "" : ": " + ibanInputNorm))
            .hasFieldOrPropertyWithValue("reason", expectedValidationError);

        assertThat(IbanValidator.getLastReason())
            .isNotNull()
            .extracting(IbanValidationError::getText)
            .isEqualTo(expectedMessagePattern);

        assertThatInvalidIbanException()
            .isThrownBy(() -> Iban.of(ibanInputNorm))
            .withCause(null)
            .withMessage(expectedMessagePattern + " (" + expectedValidationError + ")" + (ibanInputNorm.isEmpty() ? "" : ": " + ibanInputNorm))
            .hasFieldOrPropertyWithValue("reason", expectedValidationError);
        IbanValidator.setLastReason(null);

        assertThat(Iban.isValid(ibanInput)).isFalse();

        Optional.ofNullable(ibanInput).ifPresent(input ->
            assertThat(IbanValidator.isMod97Valid(input)).isFalse());

        assertThat(Iban.tryParse(ibanInput)).isEmpty();
        assertThat(Iban.tryParseOrNull(ibanInput)).isNull();

        assertThat(IbanValidator.getLastReason()).isEqualTo(expectedValidationError);
        IbanValidator.setLastReason(null);
    }

    /**
     * Verifies the relationship between a base country and its associated derived country enum entries.
     * <p>
     * Ensures that a base {@link IbanRegistry} entry:
     * <ul>
     * <li>Does not have a reference to another base country (it is a root)</li>
     * <li>Is correctly flagged as a base country</li>
     * <li>Has at least one derived record</li>
     * <li>Maintains bidirectional consistency (derived countries point back to this base)</li>
     * </ul>
     *
     * @param country the {@link IbanRegistry} entry to test as a base
     */
    @DisplayName("Base/derived registry relationship")
    @ParameterizedTest(name = "[{index}] {0} — base/derived relationship")
    @IbanRegistrySource({FI, FR, GB})
    void registry_BaseAndDerived_ShouldBeConsistent(IbanRegistry country) {
        assertThat(country.getBaseCountry()).isNull();
        assertThat(country.isBaseCountry()).isTrue();

        assertThat(country.getDerivedCountries())
            .as("Derived countries for %s", country)
            .isNotEmpty()
            .allSatisfy(derived -> {
                assertThat(derived.getBaseCountry()).isEqualTo(country);
                assertThat(derived.isBaseCountry()).isFalse();
                assertThat(derived.isDerivedCountry()).isTrue();
            });
    }

    @DisplayName("Derived/base registry relationship")
    @ParameterizedTest(name = "[{index}] {0} — derived/base relationship")
    @IbanRegistrySource(countryType = CountryType.DERIVED)
    void registry_DerivedAndBase_ShouldBeConsistent(IbanRegistry country) {
        assertThat(country.getBaseCountry()).isNotNull();
        assertThat(country.getBaseCountry().isBaseCountry()).isTrue();
        assertThat(country.isBaseCountry()).isFalse();
        assertThat(country.isDerivedCountry()).isTrue();

        assertThat(country.getDerivedCountries()).isEmpty();
    }

    /**
     * Tests {@link Iban#of(CharSequence)} with random IBANs.
     *
     * @param ibanInput the valid random IBAN string to test
     */
    @DisplayName("Random IBAN generation and validation")
    @ParameterizedTest(name = "[{index}] {0}")
    @RandomIbanSource(ibanCount = 1111)
    void of_RandomIban_ShouldBeValid(String ibanInput) {
        assertThat(ibanInput).isNotNull();

        assertThatCode(
            () -> Iban.of(ibanInput))
            .as("IBAN '%s' is valid", ibanInput)
            .doesNotThrowAnyException();

        Iban iban = Iban.of(ibanInput);
        assertThat(iban)
            .hasToString(ibanInput)
            .hasCountryName(IbanRegistry.valueOf(iban.getCountryCode()).getCountryName());
        assertThatIbanIsValid(ibanInput);
    }

    @DisplayName("Valid IBAN structure and components — all countries")
    @ParameterizedTest(name = "[{index}] {3}: {0}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // ibanInput                       | len| b | cc | fl | cd | bban                          | bankCode   | branch | account                 | ncd| ibanPlusLen
        // --------------------------------+----+---+----+----+----+-------------------------------+------------+--------+-------------------------+----+------------
        "AD1200012030200359100100          | 24 | x | AD | 🇦🇩 | 12 | 00012030200359100100          | 0001       | 2030   | 200359100100            |    |  8",
        "AE070331234567890123456           | 23 | x | AE | 🇦🇪 | 07 | 0331234567890123456           | 033        |        | 1234567890123456        |    |  3",
        "AL47212110090000000235698741      | 28 | x | AL | 🇦🇱 | 47 | 212110090000000235698741      | 212        | 1100   | 0000000235698741        | 9  |  8",
        "AO06000600000100037131174         | 25 | x | AO | 🇦🇴 | 06 | 000600000100037131174         | 0006       | 0000   | 01000371311             | 74 |  8",
        "AT611904300234573201              | 20 | x | AT | 🇦🇹 | 61 | 1904300234573201              | 19043      |        | 00234573201             |    |  5",
        "AZ21NABZ00000000137010001944      | 28 | x | AZ | 🇦🇿 | 21 | NABZ00000000137010001944      | NABZ       |        | 00000000137010001944    |    |  4",
        "BA391290079401028494              | 20 | x | BA | 🇧🇦 | 39 | 1290079401028494              | 129        | 007    | 94010284                | 94 |  6",
        "BE68539007547034                  | 16 | x | BE | 🇧🇪 | 68 | 539007547034                  | 539        |        | 0075470                 | 34 |  3",
        "BF21BF084010130046357400039       | 27 | x | BF | 🇧🇫 | 21 | BF084010130046357400039       | BF084      | 01013  | 00463574000             | 39 | 10",
        "BG80BNBG96611020345678            | 22 | x | BG | 🇧🇬 | 80 | BNBG96611020345678            | BNBG       | 9661   | 20345678                |    |  8",
        "BH67BMAG00001299123456            | 22 | x | BH | 🇧🇭 | 67 | BMAG00001299123456            | BMAG       |        | 00001299123456          |    |  4",
        "BI4210000100010000332045181       | 27 | x | BI | 🇧🇮 | 42 | 10000100010000332045181       | 10000      | 10001  | 0000332045181           |    | 10",
        "BJ66BJ0610100100144390000769      | 28 | x | BJ | 🇧🇯 | 66 | BJ0610100100144390000769      | BJ061      | 01001  | 001443900007            | 69 | 10",
        "BR1800360305000010009795493C1     | 29 | x | BR | 🇧🇷 | 18 | 00360305000010009795493C1     | 00360305   | 00001  | 0009795493              |    | 13",
        "BY13NBRB3600900000002Z00AB00      | 28 | x | BY | 🇧🇾 | 13 | NBRB3600900000002Z00AB00      | NBRB       | 3600   | 900000002Z00AB00        |    |  8",
        "CF4220001000010120069700160       | 27 | x | CF | 🇨🇫 | 42 | 20001000010120069700160       | 20001      | 00001  | 01200697001             | 60 | 10",
        "CH9300762011623852957             | 21 | x | CH | 🇨🇭 | 93 | 00762011623852957             | 00762      |        | 011623852957            |    |  5",
        "CM2110003024000224016952238       | 27 | x | CM | 🇨🇲 | 21 | 10003024000224016952238       | 10003      | 02400  | 02240169522             | 38 | 10",
        "CR05015202001026284066            | 22 | x | CR | 🇨🇷 | 05 | 015202001026284066            | 0152       |        | 02001026284066          |    |  4",
        "CV05123412341234123412341         | 25 | x | CV | 🇨🇻 | 05 | 123412341234123412341         | 1234       | 1234   | 1234123412341           |    |  8",
        "CY17002001280000001200527600      | 28 | x | CY | 🇨🇾 | 17 | 002001280000001200527600      | 002        | 00128  | 0000001200527600        |    |  8",
        "CZ6508000000192000145399          | 24 | x | CZ | 🇨🇿 | 65 | 08000000192000145399          | 0800       |        | 0000192000145399        |    |  4",
        "DE89370400440532013000            | 22 | x | DE | 🇩🇪 | 89 | 370400440532013000            | 37040044   |        | 0532013000              |    |  8",
        "DJ2100010000000154000100186       | 27 | x | DJ | 🇩🇯 | 21 | 00010000000154000100186       | 00010      | 00000  | 01540001001             |    | 10",
        "DK5000400440116243                | 18 | x | DK | 🇩🇰 | 50 | 00400440116243                | 0040       |        | 0440116243              |    |  4",
        "DO28BAGR00000001212453611324      | 28 | x | DO | 🇩🇴 | 28 | BAGR00000001212453611324      | BAGR       |        | 00000001212453611324    |    |  4",
        "DZ1700021000011130000005          | 24 | x | DZ | 🇩🇿 | 17 | 00021000011130000005          | 000        | 21000  | 0111300000              | 05 |  8",
        "EE382200221020145685              | 20 | x | EE | 🇪🇪 | 38 | 2200221020145685              | 22         | 00     | 22102014568             | 5  |  4",
        "EG380019000500000000263180002     | 29 | x | EG | 🇪🇬 | 38 | 0019000500000000263180002     | 0019       | 0005   | 00000000263180002       |    |  8",
        "ES9121000418450200051332          | 24 | x | ES | 🇪🇸 | 91 | 21000418450200051332          | 2100       | 0418   | 0200051332              | 45 | 10",
        "FI2112345600000785                | 18 | x | FI | 🇫🇮 | 21 | 12345600000785                | 123456     |        | 0000078                 | 5  |  6",
        "FK88SC123456789012                | 18 | x | FK | 🇫🇰 | 88 | SC123456789012                | SC         |        | 123456789012            |    |  2",
        "FO6264600001631634                | 18 | x | FO | 🇫🇴 | 62 | 64600001631634                | 6460       |        | 000163163               | 4  |  4",
        "FR1420041010050500013M02606       | 27 | x | FR | 🇫🇷 | 14 | 20041010050500013M02606       | 20041      | 01005  | 0500013M026             | 06 | 10",
        "GA2140021010032001890020126       | 27 | x | GA | 🇬🇦 | 21 | 40021010032001890020126       | 40021      | 01003  | 2001890020126           |    | 10",
        "GB29NWBK60161331926819            | 22 | x | GB | 🇬🇧 | 29 | NWBK60161331926819            | NWBK       | 601613 | 31926819                |    | 10",
        "GE29NB0000000101904917            | 22 | x | GE | 🇬🇪 | 29 | NB0000000101904917            | NB         |        | 0000000101904917        |    |  2",
        "GI75NWBK000000007099453           | 23 | x | GI | 🇬🇮 | 75 | NWBK000000007099453           | NWBK       |        | 000000007099453         |    |  4",
        "GL8964710001000206                | 18 | x | GL | 🇬🇱 | 89 | 64710001000206                | 6471       |        | 0001000206              |    |  4",
        "GQ7050002001003715228190196       | 27 | x | GQ | 🇬🇶 | 70 | 50002001003715228190196       | 50002      | 00100  | 37152281901             | 96 | 10",
        "GR1601101250000000012300695       | 27 | x | GR | 🇬🇷 | 16 | 01101250000000012300695       | 011        | 0125   | 0000000012300695        |    |  7",
        "GT82TRAJ01020000001210029690      | 28 | x | GT | 🇬🇹 | 82 | TRAJ01020000001210029690      | TRAJ       |        | 01020000001210029690    |    |  4",
        "HN88CABF00000000000250005469      | 28 | x | HN | 🇭🇳 | 88 | CABF00000000000250005469      | CABF       |        | 00000000000250005469    |    |  4",
        "HR1210010051863000160             | 21 | x | HR | 🇭🇷 | 12 | 10010051863000160             | 1001005    |        | 1863000160              |    |  7",
        "HU42117730161111101800000000      | 28 | x | HU | 🇭🇺 | 42 | 117730161111101800000000      | 117        | 7301   | 6111110180000000        | 0  |  7",
        "IE29AIBK93115212345678            | 22 | x | IE | 🇮🇪 | 29 | AIBK93115212345678            | AIBK       | 931152 | 12345678                |    | 10",
        "IL620108000000099999999           | 23 | x | IL | 🇮🇱 | 62 | 0108000000099999999           | 010        | 800    | 0000099999999           |    |  6",
        "IQ98NBIQ850123456789012           | 23 | x | IQ | 🇮🇶 | 98 | NBIQ850123456789012           | NBIQ       | 850    | 123456789012            |    |  7",
        "IR062960000000100324200001        | 26 | x | IR | 🇮🇷 | 06 | 2960000000100324200001        | 296        |        | 0000000100324200001     |    |  3",
        "IS140159260076545510730339        | 26 | x | IS | 🇮🇸 | 14 | 0159260076545510730339        | 0159       | 26     | 0076545510730339        |    |  6",
        "IT60X0542811101000000123456       | 27 | x | IT | 🇮🇹 | 60 | X0542811101000000123456       | 05428      | 11101  | 000000123456            | X  | 10",
        "JO94CBJO0010000000000131000302    | 30 | x | JO | 🇯🇴 | 94 | CBJO0010000000000131000302    | CBJO       | 0010   | 000000000131000302      |    |  8",
        "KM4600005000010010904400137       | 27 | x | KM | 🇰🇲 | 46 | 00005000010010904400137       | 00005      | 00001  | 00109044001             | 37 | 10",
        "KW81CBKU0000000000001234560101    | 30 | x | KW | 🇰🇼 | 81 | CBKU0000000000001234560101    | CBKU       |        | 0000000000001234560101  |    |  4",
        "KZ86125KZT5004100100              | 20 | x | KZ | 🇰🇿 | 86 | 125KZT5004100100              | 125        |        | KZT5004100100           |    |  3",
        "LB62099900000001001901229114      | 28 | x | LB | 🇱🇧 | 62 | 099900000001001901229114      | 0999       |        | 00000001001901229114    |    |  4",
        "LC55HEMM000100010012001200023015  | 32 | x | LC | 🇱🇨 | 55 | HEMM000100010012001200023015  | HEMM       |        | 000100010012001200023015|    |  4",
        "LI21088100002324013AA             | 21 | x | LI | 🇱🇮 | 21 | 088100002324013AA             | 08810      |        | 0002324013AA            |    |  5",
        "LT121000011101001000              | 20 | x | LT | 🇱🇹 | 12 | 1000011101001000              | 10000      |        | 11101001000             |    |  5",
        "LU280019400644750000              | 20 | x | LU | 🇱🇺 | 28 | 0019400644750000              | 001        |        | 9400644750000           |    |  3",
        "LV80BANK0000435195001             | 21 | x | LV | 🇱🇻 | 80 | BANK0000435195001             | BANK       |        | 0000435195001           |    |  4",
        "LY83002048000020100120361         | 25 | x | LY | 🇱🇾 | 83 | 002048000020100120361         | 002        | 048    | 000020100120361         |    |  6",
        "MA64360815000001793222001617      | 28 | x | MA | 🇲🇦 | 64 | 360815000001793222001617      | 360        | 81500  | 0001793222001617        |    |  8",
        "MC5811222000010123456789030       | 27 | x | MC | 🇲🇨 | 58 | 11222000010123456789030       | 11222      | 00001  | 01234567890             | 30 | 10",
        "MD24AG000225100013104168          | 24 | x | MD | 🇲🇩 | 24 | AG000225100013104168          | AG         |        | 000225100013104168      |    |  2",
        "ME25505000012345678951            | 22 | x | ME | 🇲🇪 | 25 | 505000012345678951            | 505        |        | 0000123456789           | 51 |  3",
        "MK07250120000058984               | 19 | x | MK | 🇲🇰 | 07 | 250120000058984               | 250        |        | 1200000589              | 84 |  3",
        "MN121234123456789123              | 20 | x | MN | 🇲🇳 | 12 | 1234123456789123              | 1234       |        | 123456789123            |    |  4",
        "MR1300020001010000123456753       | 27 | x | MR | 🇲🇷 | 13 | 00020001010000123456753       | 00020      | 00101  | 00001234567             | 53 | 10",
        "MT84MALT011000012345MTLCAST001S   | 31 | x | MT | 🇲🇹 | 84 | MALT011000012345MTLCAST001S   | MALT       | 01100  | 0012345MTLCAST001S      |    |  9",
        "MU17BOMM0101101030300200000MUR    | 30 | x | MU | 🇲🇺 | 17 | BOMM0101101030300200000MUR    | BOMM01     | 01     | 101030300200000MUR      |    |  8",
        "MZ59000800005138555713187         | 25 | x | MZ | 🇲🇿 | 59 | 000800005138555713187         | 0008       | 0000   | 51385557131             | 87 |  8",
        "NI45BAPR00000013000003558124      | 28 | x | NI | 🇳🇮 | 45 | BAPR00000013000003558124      | BAPR       |        | 00000013000003558124    |    |  4",
        "NL91ABNA0417164300                | 18 | x | NL | 🇳🇱 | 91 | ABNA0417164300                | ABNA       |        | 0417164300              |    |  4",
        "NO9386011117947                   | 15 | x | NO | 🇳🇴 | 93 | 86011117947                   | 8601       |        | 111794                  | 7  |  4",
        "OM810180000001299123456           | 23 | x | OM | 🇴🇲 | 81 | 0180000001299123456           | 018        |        | 0000001299123456        |    |  3",
        "PK36SCBL0000001123456702          | 24 | x | PK | 🇵🇰 | 36 | SCBL0000001123456702          | SCBL       |        | 0000001123456702        |    |  4",
        "PL61109010140000071219812874      | 28 | x | PL | 🇵🇱 | 61 | 109010140000071219812874      | 109        | 0101   | 0000071219812874        | 4  |  8",
        "PS92PALS000000000400123456702     | 29 | x | PS | 🇵🇸 | 92 | PALS000000000400123456702     | PALS       |        | 000000000400123456702   |    |  4",
        "PT50000201231234567890154         | 25 | x | PT | 🇵🇹 | 50 | 000201231234567890154         | 0002       | 0123   | 12345678901             | 54 |  8",
        "QA58DOHB00001234567890ABCDEFG     | 29 | x | QA | 🇶🇦 | 58 | DOHB00001234567890ABCDEFG     | DOHB       |        | 00001234567890ABCDEFG   |    |  4",
        "RO49AAAA1B31007593840000          | 24 | x | RO | 🇷🇴 | 49 | AAAA1B31007593840000          | AAAA       |        | 1B31007593840000        |    |  4",
        "RS35260005601001611379            | 22 | x | RS | 🇷🇸 | 35 | 260005601001611379            | 260        |        | 0056010016113           | 79 |  3",
        "RU0304452522540817810538091310419 | 33 | x | RU | 🇷🇺 | 03 | 04452522540817810538091310419 | 044525225  | 40817  | 810538091310419         |    | 14",
        "SA0380000000608010167519          | 24 | x | SA | 🇸🇦 | 03 | 80000000608010167519          | 80         |        | 000000608010167519      |    |  2",
        "SC18SSCB11010000000000001497USD   | 31 | x | SC | 🇸🇨 | 18 | SSCB11010000000000001497USD   | SSCB11     | 01     | 0000000000001497        |    |  8",
        "SD2129010501234001                | 18 | x | SD | 🇸🇩 | 21 | 29010501234001                | 29         |        | 010501234001            |    |  2",
        "SE4550000000058398257466          | 24 | x | SE | 🇸🇪 | 45 | 50000000058398257466          | 500        |        | 00000058398257466       |    |  3",
        "SI56263300012039086               | 19 | x | SI | 🇸🇮 | 56 | 263300012039086               | 26         | 330    | 00120390                | 86 |  5",
        "SK3112000000198742637541          | 24 | x | SK | 🇸🇰 | 31 | 12000000198742637541          | 1200       |        | 0000198742637541        |    |  4",
        "SM86U0322509800000000270100       | 27 | x | SM | 🇸🇲 | 86 | U0322509800000000270100       | 03225      | 09800  | 000000270100            | U  | 10",
        "SN08SN0100152000048500003035      | 28 | x | SN | 🇸🇳 | 08 | SN0100152000048500003035      | SN010      | 01520  | 000485000030            | 35 | 10",
        "SO211000001001000100141           | 23 | x | SO | 🇸🇴 | 21 | 1000001001000100141           | 1000       | 001    | 001000100141            |    |  7",
        "ST23000100010051845310146         | 25 | x | ST | 🇸🇹 | 23 | 000100010051845310146         | 0001       | 0001   | 0051845310146           |    |  8",
        "SV62CENR00000000000000700025      | 28 | x | SV | 🇸🇻 | 62 | CENR00000000000000700025      | CENR       |        | 00000000000000700025    |    |  4",
        "TG87TG0090100110232500400512      | 28 | x | TG | 🇹🇬 | 87 | TG0090100110232500400512      | TG009      | 01001  | 102325004005            | 12 | 10",
        "TL380080012345678910157           | 23 | x | TL | 🇹🇱 | 38 | 0080012345678910157           | 008        |        | 00123456789101          | 57 |  3",
        "TN5910006035183598478831          | 24 | x | TN | 🇹🇳 | 59 | 10006035183598478831          | 10         | 006    | 0351835984788           | 31 |  5",
        "TR330006100519786457841326        | 26 | x | TR | 🇹🇷 | 33 | 0006100519786457841326        | 00061      |        | 0519786457841326        | 0  |  6",
        "UA213223130000026007233566001     | 29 | x | UA | 🇺🇦 | 21 | 3223130000026007233566001     | 322313     |        | 0000026007233566001     |    |  6",
        "VA59001123000012345678            | 22 | x | VA | 🇻🇦 | 59 | 001123000012345678            | 001        |        | 123000012345678         |    |  3",
        "VG96VPVG0000012345678901          | 24 | x | VG | 🇻🇬 | 96 | VPVG0000012345678901          | VPVG       |        | 0000012345678901        |    |  4",
        "XK051212012345678906              | 20 | x | XK | 🇽🇰 | 05 | 1212012345678906              | 12         | 12     | 0123456789              | 06 |  4",
        "YE15CBYE0001018861234567891234    | 30 | x | YE | 🇾🇪 | 15 | CBYE0001018861234567891234    | CBYE       | 0001   | 018861234567891234      |    |  8",
    })

    @SuppressWarnings("checkstyle:ParameterNumber")
    void of_ValidIbanAllCountries_ShouldReturnIban(String ibanInput, int expectedIbanLength, @ConvertWith(BooleanConverter.class) boolean expectedBaseCountry,
            String expectedCountryCode, String expectedCountryFlag,
            String expectedCheckDigits, String expectedBban, String expectedBankCode, String expectedBranchCode,
            String expectedAccountNumber, String expectedNcd, Integer expectedIbanPlusLen) {

        IbanRegistry countryData = IbanRegistry.getByCode(expectedCountryCode);

        assertThat(countryData)
            .isNotNull()
            .satisfies(reg -> {
                assertThat(reg.isBaseCountry()).isEqualTo(expectedBaseCountry);
                assertThat(reg.isDerivedCountry()).isEqualTo(!expectedBaseCountry);
                if (expectedBaseCountry) {
                    assertThat(reg.getBaseCountry()).isNull();
                } else {
                    assertThat(reg.getBaseCountry()).isNotNull();
                }
                assertThat(reg.getIbanRegex()).isNotNull();
                assertThat(reg.getStructureData()).isNotNull();
                assertThat(reg.getIbanExample()).isNotNull();
            });

        assertThatNoException()
            .as("IBAN '%s' should be valid", ibanInput)
            .isThrownBy(() -> Iban.of(ibanInput));

        Iban iban = Iban.of(ibanInput);

        assertThat(iban)
            .hasLength(expectedIbanLength)
            .hasCountryCode(expectedCountryCode)
            .hasCountryFlag(expectedCountryFlag)
            .hasCurrency(Iso3166Alpha2.fromCode(expectedCountryCode).getCurrency())
            .hasCheckDigits(expectedCheckDigits)
            .hasBban(expectedBban)
            .hasBankCode(expectedBankCode)
            .hasBranchCode(expectedBranchCode)
            .hasBankAndBranchCode(expectedBankCode + Optional.ofNullable(expectedBranchCode).orElse(""))
            .hasNationalCheckDigit(expectedNcd)
            .hasAccountNumber(expectedAccountNumber)

            .hasCountryCode(countryData.getCountryCode())
            .hasCountryName(countryData.getCountryName())
            .hasCountryFlag(countryData.getCountryFlag())
            .hasOrganisation(countryData.getOrganisation())
            .isSepa(countryData.isSepa())

            .matches(countryData.getIbanRegex());

        String ibanLastChar = iban.subSequence(iban.length() - 1, iban.length());

        assertThat(iban.toFormattedString())
            .as("Expected valid formatted string for IBAN '%s' but got: '%s'", iban, iban.toFormattedString())
            .isNotBlank()
            .hasSizeGreaterThan(iban.length())
            .startsWith(iban.getCountryCode() + iban.getCheckDigits() + ' ')
            .endsWith(ibanLastChar);

        assertThat(iban.toComponentString())
            .as("Expected valid component string for IBAN '%s' but got: '%s'", iban, iban.toComponentString())
            .isNotBlank()
            .hasSizeGreaterThan(iban.length())
            .startsWith(iban.getCountryCode() + ' ' + iban.getCheckDigits())
            .contains(iban.getBankCode(), iban.getAccountNumber())
            .doesNotContain("  ")
            .endsWith(ibanLastChar);

        assertThat(IbanPlusKey.of(iban))
            .as("Expected valid Iban Plus key for IBAN '%s' but got: '%s'", iban, IbanPlusKey.of(iban))
            .isNotBlank()
            .as("Expected length of IBAN Plus code to be %d for IBAN '%s'", expectedIbanPlusLen, iban)
            .hasSize(expectedIbanPlusLen);

        assertThat(Iban.isValid(ibanInput))
            .as("Expected IBAN '%s' to be valid", iban)
            .isTrue();
    }

    /**
     * Tests various invalid IBANs for all supported countries, expecting structure or checksum errors.
     *
     * @param entry the {@link IbanRegistry} entry to test
     */
    @DisplayName("Invalid IBAN rejected — all countries")
    @ParameterizedTest
    @EnumSource(IbanRegistry.class)
    void of_InvalidIbanAllCountries_ShouldThrowException(IbanRegistry entry) {
        String ibanStr1 = entry.getCountryCode() + "00" + "999999999999999999999999999999".substring(0, entry.getBbanLength());
        String ibanStr2 = entry.getCountryCode() + "00" + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".substring(0, entry.getBbanLength());

        for (String iban : Arrays.asList(ibanStr1, ibanStr2)) {
            assertThatInvalidIbanException()
                .isThrownBy(() -> Iban.of(iban))
                .withCause(null)
                .withMessageMatching(String.format("^(?:%s|%s) \\([A-Z].+", IbanValidationError.INVALID_STRUCTURE.getText(), IbanValidationError.INVALID_CHECKSUM.getText()))
                .extracting("reason")
                .isIn(IbanValidationError.INVALID_STRUCTURE, IbanValidationError.INVALID_CHECKSUM);
        }

        assumeThat(entry.getCountryValidator())
            .as("Assuming that country validator for %s is present", entry.getCountryCode())
            .isNotNull();

        char[] randomAscii = new Random().ints(entry.getIbanLength(), 32, 127)
            .mapToObj(i -> (char) i)
            .map(Character::toLowerCase)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString()
            .toCharArray();
        randomAscii[IbanRegistry.INDEX_BBAN] = '_';
        randomAscii[IbanRegistry.INDEX_BBAN + 1] = 'x';

        assertThat(entry.getCountryValidator().validateIban(new String(randomAscii)))
            .as("Expected validation of random ascii array to fail for '%s' with data: '%s'", entry.getCountryCode(), new String(randomAscii))
            .isFalse();
    }

    /**
     * Tests {@link Iban#equals(Object)} and {@link Iban#hashCode()}.<br>
     * Ensures two IBAN instances created from the same normalized string are considered equal
     * and have the same hash code, following the contract for immutable value objects.
     */
    @DisplayName("equals() and hashCode() contract")
    @Test
    void equalsAndHashCode_SimilarIbans_ShouldBeConsistent() {
        String ibanStr1 = "DE89370400440532013000";
        String ibanStr2 = "DE89370400440532013000";
        String ibanStr3 = "DE62370400440532013001";

        Iban iban1 = Iban.of(ibanStr1);
        Iban iban2 = Iban.of(ibanStr2);
        Iban iban3 = Iban.of(ibanStr3);

        assertThat(iban1)
            .isNotEqualTo(null)
            .isNotEqualTo(ibanStr1)
            .isNotEqualTo(new Object())

            // same IBAN content
            .as("Iban instances with the same content must be equal")
            .isEqualTo(iban1)
            .isEqualTo(iban2)
            .hasSameHashCodeAs(iban2)

            // different IBAN content
            .as("Iban instances with different content must not be equal")
            .isNotEqualTo(iban3)
            .doesNotHaveSameHashCodeAs(iban3);
    }

    /**
     * Tests {@link Iban#compareTo(Iban)} to ensure correct alphabetical ordering
     * based on the underlying normalized IBAN string.
     *
     * @param iban1Input   first IBAN string
     * @param iban2Input   second IBAN string
     * @param expectedSign the expected sign of the comparison result (negative, zero, or positive)
     */
    @DisplayName("compareTo() ordering")
    @ParameterizedTest(name = "[{index}] Compare ''{0}'' with ''{1}'': Expected sign {2}")
    @CsvSource(delimiter = '|', value = {
        "DE89370400440532013000      | DE89370400440532013000      | 0  ", // equal
        "DE62370400440532013001      | DE89370400440532013000      | -1 ", // iban1 < iban2
        "DE89370400440532013000      | DE62370400440532013001      | 1  ", // iban1 > iban2
        "FR1420041010050500013M02606 | DE89370400440532013000      | 1  ", // FR > DE (alphabetical)
        "DE89370400440532013000      | FR1420041010050500013M02606 | -1 "  // DE < FR
    })
    void compareTo_VariousIbans_ShouldFollowLexicographicalOrder(String iban1Input, String iban2Input, int expectedSign) {
        Iban iban1 = Iban.of(iban1Input);
        Iban iban2 = Iban.of(iban2Input);

        int result = iban1.compareTo(iban2);

        assertThat(Integer.signum(result))
            .as("Comparison of %s and %s", iban1Input, iban2Input)
            .isEqualTo(expectedSign);

        // also check the inverse for consistency
        assertThat(iban2.compareTo(iban1)).isEqualTo(-result);
    }

    /**
     * Tests {@link Iban#charAt(int)} for boundary and content checks.
     * @param index        the character index to test
     * @param expectedChar the expected character at that index
     */
    @DisplayName("charAt() content and boundary checks")
    @ParameterizedTest(name = "[{index}] charAt({0}) must be ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "0  | D",
        "1  | E",
        "2  | 8",
        "21 | 0"
    })
    void charAt_ValidIndex_ShouldReturnChar(int index, char expectedChar) {
        String ibanStr = "DE89370400440532013000"; // Length 22
        Iban iban = Iban.of(ibanStr);

        assertThat(iban.charAt(index)).isEqualTo(expectedChar);

        // boundary checks
        assertThatIndexOutOfBoundsException().isThrownBy(() -> iban.charAt(-1));

        // index equals length (inclusive/exclusive boundary)
        assertThatIndexOutOfBoundsException().isThrownBy(() -> iban.charAt(ibanStr.length()));

        // index greater than length
        assertThatIndexOutOfBoundsException()
            .as("Index > length must throw IndexOutOfBoundsException")
            .isThrownBy(() -> iban.charAt(ibanStr.length() + 1));
    }

    /**
     * Tests {@link Iban#subSequence(int, int)} for correct substring extraction.
     * @param start the start index (inclusive)
     * @param end   the end index (exclusive)
     * @param expectedString the expected sub-sequence string
     */
    @DisplayName("subSequence() extraction and boundary checks")
    @ParameterizedTest(name = "[{index}] subSequence({0}, {1}) must be ''{2}''")
    @CsvSource(delimiter = '|', value = {
        "0 | 2  | DE",                    // Country Code
        "2 | 4  | 89",                    // Check Digits
        "4 | 22 | 370400440532013000",    // BBAN
        "0 | 22 | DE89370400440532013000" // Full IBAN
    })
    void subSequence_ValidRange_ShouldReturnSubstring(int start, int end, String expectedString) {
        String ibanStr = "DE89370400440532013000"; // length 22
        Iban iban = Iban.of(ibanStr);

        assertThat(iban.subSequence(start, end))
            .as("Sub-sequence must match the expected string")
            .isInstanceOf(CharSequence.class)
            .hasToString(expectedString)
            .hasSameClassAs(iban.subSequence(start, end));

        assertThatIndexOutOfBoundsException().isThrownBy(() -> iban.subSequence(-1, 2));

        // end index is greater than length (boundary condition 1)
        assertThatIndexOutOfBoundsException().isThrownBy(() -> iban.subSequence(0, ibanStr.length() + 1));

        // start index is equal to the end index (returns empty string - still valid)
        assertThat(iban.subSequence(10, 10)).hasToString("");

        // end index equals length
        assertThat(iban.subSequence(0, ibanStr.length()))
            .as("SubSequence with end=length must return the full IBAN")
            .hasToString(ibanStr);

        assertThatIndexOutOfBoundsException().isThrownBy(() -> iban.subSequence(5, 4)); // start > end
    }

    // =========================================================================
    // Serialization tests (Memento Pattern)
    // =========================================================================

    /**
     * Verifies that a round-trip serialization/deserialization of an {@link Iban} instance
     * produces an equal object with the same normalized string and country data.
     * <p>
     * This covers the happy path of the Memento pattern: {@code writeReplace} emits the
     * {@link Iban.Memento} proxy, and {@code readResolve} reconstructs via {@link Iban#parse}.
     *
     * @param ibanInput a valid IBAN string to round-trip
     * @throws IOException            if the byte stream cannot be written or read
     * @throws ClassNotFoundException never expected in this context
     */
    @DisplayName("Serialization round-trip preserves IBAN value")
    @ParameterizedTest(name = "[{index}] Serialization round-trip: ''{0}''")
    @ValueSource(strings = {
        "DE89370400440532013000",
        "GB29NWBK60161331926819",
        "FR1420041010050500013M02606",
        "NL91ABNA0417164300",
        "PL61109010140000071219812874"
    })
    void serialization_ValidIban_ShouldPreserveState(String ibanInput) throws IOException, ClassNotFoundException {
        Iban original = Iban.of(ibanInput);
        final Iban iban = original;

        byte[] bytes = TestUtil.serialize(iban);
        final byte[] bytes1 = bytes;
        Iban restored = TestUtil.deserialize(bytes1);

        assertThat(restored)
            .as("Deserialized Iban must equal the original")
            .isNotNull()
            .isEqualTo(original)
            .hasToString(ibanInput)
            .hasSameHashCodeAs(original);

        assertThat(restored.getCountryCode())
            .as("Country code must survive round-trip")
            .isEqualTo(original.getCountryCode());

        assertThat(restored.getCheckDigits())
            .as("Check digits must survive round-trip")
            .isEqualTo(original.getCheckDigits());
    }

    /**
     * Verifies that the serialized byte stream references the {@link Iban.Memento} proxy class
     * and NOT {@code Iban} directly, confirming that {@code writeReplace} is invoked.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Serialized form uses Memento proxy class")
    @Test
    void serialization_Iban_ShouldUseMementoProxy() throws IOException {
        Iban iban = Iban.of("DE89370400440532013000");
        final Iban iban1 = iban;
        byte[] bytes = TestUtil.serialize(iban1);

        // the serialized stream must contain the Memento class name, not Iban itself
        String streamContent = new String(bytes, StandardCharsets.UTF_8);
        assertThat(streamContent)
            .as("Serialized stream must reference the Memento proxy class")
            .contains("Memento")
            .as("Serialized stream must not reference Iban directly as the top-level type")
            .doesNotContain("de.speedbanking.iban.Iban\n");
    }

    /**
     * Verifies that attempting to deserialize a raw {@code Iban} object directly
     * (bypassing the Memento proxy) throws {@link InvalidClassException}.
     * <p>
     * This protects against crafted byte-stream attacks that try to inject an
     * {@code Iban} instance without going through validation.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Direct deserialization bypassing Memento is rejected")
    @Test
    void deserialization_DirectIbanInStream_ShouldThrowException() throws IOException {
        // Obtain a valid Iban serialized form (which uses Memento)
        Iban iban = Iban.of("DE89370400440532013000");
        final Iban iban1 = iban;
        byte[] mementoBytes = TestUtil.serialize(iban1);

        // Replace the Memento class descriptor with the Iban class descriptor in the stream
        // to simulate a crafted stream that bypasses writeReplace.
        String mementoClassName = Iban.Memento.class.getName(); // "de.speedbanking.iban.Iban$Memento"
        String ibanClassName    = Iban.class.getName();
        final byte[] stream = mementoBytes;
        final String oldName = mementoClassName;
        final String newName = ibanClassName;         // "de.speedbanking.iban.Iban"

        byte[] tamperedBytes = TestUtil.replaceClassName(stream, oldName, newName);
        final byte[] bytes = tamperedBytes;

        assertThat(catchThrowable(() -> TestUtil.deserialize(bytes)))
            .as("Direct deserialization of Iban must be rejected")
            .isInstanceOfAny(InvalidClassException.class, StreamCorruptedException.class, IOException.class);
    }

    /**
     * Verifies that serialization followed by deserialization produces an object that
     * is equal to the original by value but is a distinct instance (no reference sharing).
     *
     * @throws IOException            if the byte stream cannot be written or read
     * @throws ClassNotFoundException never expected in this context
     */
    @DisplayName("Deserialized instance is distinct but equal")
    @Test
    void deserialization_ValidIban_ShouldCreateNewInstance() throws IOException, ClassNotFoundException {
        Iban original = Iban.of("NL91ABNA0417164300");
        final Iban iban = original;
        Iban restored = TestUtil.deserialize(TestUtil.serialize(iban));

        assertThat(restored)
            .isNotSameAs(original)
            .isEqualTo(original);
    }

    /**
     * Verifies that serialization preserves the {@code isSepa()} flag across the round-trip,
     * covering both a SEPA country (DE) and a non-SEPA country (AE).
     *
     * @throws IOException            if the byte stream cannot be written or read
     * @throws ClassNotFoundException never expected in this context
     */
    @DisplayName("Serialization preserves isSepa() flag")
    @ParameterizedTest(name = "[{index}] isSepa flag preserved for ''{0}'' (expected: {1})")
    @CsvSource(delimiter = '|', value = {
        "SE4550000000058398257466      | true", // Sweden - SEPA
        "ES9121000418450200051332      | true", // Spain - SEPA
        "PS92PALS000000000400123456702 | false" // Palestine - non-SEPA
    })
    void serialization_SepaIban_ShouldPreserveSepaFlag(String ibanInput, boolean expectedSepa)
            throws IOException, ClassNotFoundException {
        Iban original = Iban.of(ibanInput.trim());
        final Iban iban = original;
        Iban restored = TestUtil.deserialize(TestUtil.serialize(iban));

        assertThat(restored.isSepa())
            .as("isSepa() flag must be preserved through serialization for '%s'", ibanInput)
            .isEqualTo(expectedSepa);
    }

    /**
     * Verifies that {@link Iban.Memento#readResolve()} throws {@link InvalidObjectException}
     * when the stored value is not a valid IBAN string.
     * <p>
     * This covers the {@code catch (RuntimeException)} branch in {@code readResolve()}, which
     * wraps a validation failure into an {@link InvalidObjectException}. The stream is crafted
     * manually: standard Memento class descriptor + STREAM_VERSION (1L) + an invalid payload,
     * so that {@code readResolve()} receives a non-validating string.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Memento.readResolve() rejects invalid IBAN payload")
    @Test
    void memento_InvalidIban_ShouldRejectReadResolve() throws IOException {
        byte[] corruptStream = TestUtil.buildMementoStream(Iban.of("DE89370400440532013000"), "NOT_AN_IBAN");

        assertThat(catchThrowable(() -> TestUtil.deserialize(corruptStream)))
            .as("readResolve() must reject an invalid IBAN stored in the Memento")
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("Cannot restore Iban from serialized form");
    }

    /**
     * Verifies that {@link Iban.Memento#readObject(ObjectInputStream)} throws
     * {@link InvalidObjectException} when the stream contains an unsupported version number.
     * <p>
     * This covers the {@code version != STREAM_VERSION} branch. The stream is crafted with
     * version {@code 99L} instead of the expected {@code 1L}.
     *
     * @throws IOException if the byte stream cannot be written
     */
    @DisplayName("Memento.readObject() rejects unknown stream version")
    @Test
    void memento_InvalidVersion_ShouldRejectReadObject() throws IOException {
        byte[] corruptStream = TestUtil.buildMementoStream(Iban.of("DE89370400440532013000"), 99L, "DE89370400440532013000");

        assertThat(catchThrowable(() -> TestUtil.deserialize(corruptStream)))
            .as("readObject() must reject a Memento with an unsupported stream version")
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("Unsupported Iban Memento stream version: 99");
    }

    /**
     * Verifies that {@link Iban#readObjectNoData()} throws {@link InvalidObjectException}.
     * <p>
     * {@code readObjectNoData()} is called by the JVM when a superclass added
     * {@code Serializable} after the subclass was already serialized, leaving no instance
     * data for the subclass in the stream. Since {@code Iban} is {@code final} and has no
     * subclasses, the only way to reach this path in a unit test is via reflection.
     *
     * @throws Exception if reflection access fails
     */
    @DisplayName("readObjectNoData() must throw InvalidObjectException")
    @Test
    void readObjectNoData_DirectInvocation_ShouldThrowException() throws Exception {
        Iban iban = Iban.of("DE89370400440532013000");

        Throwable cause = TestUtil.invokeSerializationGuard(iban, "readObjectNoData", new Class<?>[0]);

        assertThat(cause)
            .as("readObjectNoData() must throw InvalidObjectException")
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("must be deserialized via its Memento proxy");
    }

    /**
     * Verifies that {@link Iban#readObject(ObjectInputStream)} throws {@link InvalidObjectException}.
     * <p>
     * {@code readObject()} is unreachable in normal deserialisation because {@code writeReplace()}
     * always substitutes the Memento proxy. The only way to cover this branch is via reflection.
     *
     * @throws Exception if reflection access fails
     */
    @DisplayName("readObject() must throw InvalidObjectException when invoked directly")
    @Test
    void readObject_DirectInvocation_ShouldThrowException() throws Exception {
        Iban iban = Iban.of("DE89370400440532013000");

        Throwable cause = TestUtil.invokeSerializationGuard(iban, "readObject",
            new Class<?>[] {ObjectInputStream.class}, (ObjectInputStream) null);

        assertThat(cause)
            .as("readObject() must throw InvalidObjectException")
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("must be deserialized via its Memento proxy");
    }

    // =========================================================================
    // getCurrency() / getCurrencyCode()
    // =========================================================================

    /**
     * Verifies that {@link Iban#getCurrency()} returns the correct {@link de.speedbanking.util.Currency}
     * constant and that {@link Iban#getCurrencyCode()} returns the matching ISO 4217 alpha code.
     * <p>
     * Test cases deliberately cover:
     * <ul>
     *   <li>Eurozone SEPA countries (DE, AT, FR, IE, NL, FI, BE)</li>
     *   <li>Non-Euro SEPA countries (GB/GBP, CH/CHF, SE/SEK, NO/NOK, DK/DKK, PL/PLN)</li>
     *   <li>Non-SEPA countries with various currencies (AE/AED, PS/ILS, QA/QAR)</li>
     *   <li>Countries using CHF without being Switzerland (LI)</li>
     * </ul>
     */
    @DisplayName("getCurrency() and getCurrencyCode() return the correct ISO 4217 currency")
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', value = {
        // ibanInput                         | expectedCurrency
        // Eurozone SEPA
        "DE89370400440532013000              | EUR",  // Germany
        "AT611904300234573201                | EUR",  // Austria
        "FR1420041010050500013M02606         | EUR",  // France
        "IE29AIBK93115212345678              | EUR",  // Ireland
        "NL91ABNA0417164300                  | EUR",  // Netherlands
        "FI2112345600000785                  | EUR",  // Finland
        "BE68539007547034                    | EUR",  // Belgium
        "LU280019400644750000                | EUR",  // Luxembourg
        "GR1601101250000000012300695         | EUR",  // Greece
        "IT60X0542811101000000123456         | EUR",  // Italy
        "ES9121000418450200051332            | EUR",  // Spain
        "PT50000201231234567890154           | EUR",  // Portugal
        "ME25505000012345678951              | EUR",  // Montenegro (EUR without EU)
        "XK051212012345678906               | EUR",  // Kosovo (EUR without EU)
        // Non-Euro SEPA — own currencies
        "GB29NWBK60161331926819             | GBP",  // United Kingdom
        "CH9300762011623852957              | CHF",  // Switzerland
        "LI21088100002324013AA              | CHF",  // Liechtenstein (uses CHF)
        "SE4550000000058398257466           | SEK",  // Sweden
        "NO9386011117947                    | NOK",  // Norway
        "DK5000400440116243                 | DKK",  // Denmark
        "PL61109010140000071219812874       | PLN",  // Poland
        "CZ6508000000192000145399           | CZK",  // Czechia
        "HU42117730161111101800000000       | HUF",  // Hungary
        "RO49AAAA1B31007593840000           | RON",  // Romania
        "BG80BNBG96611020345678             | BGN",  // Bulgaria
        "IS140159260076545510730339          | ISK",  // Iceland
        // Non-SEPA
        "AE070331234567890123456            | AED",  // UAE
        "PS92PALS000000000400123456702      | ILS",  // Palestine
        "QA58DOHB00001234567890ABCDEFG      | QAR",  // Qatar
        "SA0380000000608010167519           | SAR",  // Saudi Arabia
        "TR330006100519786457841326         | TRY",  // Türkiye
        "PK36SCBL0000001123456702          | PKR",  // Pakistan
    })
    void getCurrency_knownIbans_returnsCorrectCurrency(String ibanInput, String expectedCurrency) {
        Iban iban = Iban.of(ibanInput.trim());

        assertThat(iban.getCurrency())
            .as("getCurrency() for IBAN '%s'", ibanInput)
            .isNotNull()
            .isEqualTo(de.speedbanking.util.Currency.valueOf(expectedCurrency));

        assertThat(iban.getCurrencyCode())
            .as("getCurrencyCode() for IBAN '%s'", ibanInput)
            .isEqualTo(expectedCurrency);
    }

    /**
     * Verifies that {@link Iban#getCurrencyCode()} is always consistent with
     * {@link Iban#getCurrency()}{@code .getAlphaCode()} for every IBAN example
     * in the registry.
     * <p>
     * This sweeps all {@link IbanRegistry} entries to ensure no constant is
     * accidentally misconfigured in {@link de.speedbanking.util.Iso3166Alpha2}.
     */
    @DisplayName("getCurrencyCode() is consistent with getCurrency().getAlphaCode() — all countries")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void getCurrencyCode_allCountries_consistentWithGetCurrency(IbanRegistry entry) {
        Iban iban = Iban.of(entry.getIbanExample());

        de.speedbanking.util.Currency currency = iban.getCurrency();
        String currencyCode = iban.getCurrencyCode();

        if (currency != null) {
            assertThat(currencyCode)
                .as("getCurrencyCode() must equal getCurrency().getAlphaCode() for '%s'", entry)
                .isEqualTo(currency.getAlphaCode());
        } else {
            assertThat(currencyCode)
                .as("getCurrencyCode() must be null when getCurrency() is null for '%s'", entry)
                .isNull();
        }
    }

    /**
     * Verifies that {@link Iban#getCurrency()} returns a non-null value for all
     * countries currently in the registry — none of them correspond to the only
     * currency-less {@link de.speedbanking.util.Iso3166Alpha2} entry ({@code AQ}).
     */
    @DisplayName("getCurrency() is non-null for all IbanRegistry entries")
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(IbanRegistry.class)
    void getCurrency_allCountries_neverNull(IbanRegistry entry) {
        Iban iban = Iban.of(entry.getIbanExample());

        assertThat(iban.getCurrency())
            .as("getCurrency() must not be null for entry '%s'", entry)
            .isNotNull();

        assertThat(iban.getCurrencyCode())
            .as("getCurrencyCode() must not be null or blank for entry '%s'", entry)
            .isNotNull()
            .isNotBlank();
    }

}

