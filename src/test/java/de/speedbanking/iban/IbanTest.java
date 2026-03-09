package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanIsValid;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanOf;
import static de.speedbanking.iban.IbanAssertions.assertThatIbanOfNormalized;
import static de.speedbanking.iban.IbanAssertions.assertThatInvalidIbanException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatComparable;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.assertj.core.api.Assertions.catchThrowable;

import de.speedbanking.util.TestUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Unit tests for the new immutable {@link Iban} class, covering IBAN validation and component extraction.
 */
class IbanTest {

    /**
     * Tests {@link Iban#of(CharSequence)} with various valid IBANs.
     *
     * @param ibanInput the valid IBAN string to test
     */
    @ParameterizedTest(name = "[{index}] Valid IBAN ''{0}''")
    @ValueSource(strings = {
        "DE 91 10 00 00 00 01 23 45 67 89",         // Germany
        "FR 14 20 04 10 10 05 05 00 01 3M 02 60 6", // France
        "GB33 BUKB 2020 1555 5555 55",              // United Kingdom
        "NL91 ABNA 0417 1643 00"                    // Netherlands
    })
    void testValidIban(String ibanInput) {
        String ibanInputNorm = ibanInput.replace(" ", "");

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
            () -> Iban.ofNormalized(ibanInputNorm))
            .as("Normalized IBAN '%s' is valid and should be instantiable", ibanInput)
            .doesNotThrowAnyException();

        Iban iban2 = assertThatIbanOfNormalized(ibanInputNorm)
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
    }

    /**
     * Tests {@link Iban#of(CharSequence)} with various invalid IBANs,
     * expecting an {@link InvalidIbanException} with a specific message pattern.
     *
     * @param ibanInput              the invalid IBAN string to test
     * @param expectedMessagePattern the regex pattern for the expected exception message
     */
    @ParameterizedTest(name = "[{index}] Invalid IBAN ''{0}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // IBAN (Input)                | ValidationError (Enum)   | Expected Message Pattern
        "(null)                        | EMPTY                    | IBAN is null or empty",
        "''                            | EMPTY                    | IBAN is null or empty",
        "'   '                         | EMPTY                    | IBAN is null or empty",
        "PS92pals000000000400123456702 | ILLEGAL_CHARACTERS       | IBAN contains illegal character(s)",
        "ps92pals000000000400123456702 | INVALID_COUNTRY          | IBAN has invalid country code",
        "Ps92pals000000000400123456702 | INVALID_COUNTRY          | IBAN has invalid country code",
        "XX12345678901234567890        | UNSUPPORTED_COUNTRY      | IBAN has unsupported country code",
        "DE123                         | INCORRECT_LENGTH         | IBAN has incorrect length",
        "DE91BHLSDEM1123456789         | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "GB33BUKB2020155555567         | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "GP464Q85KW3RR7JWATF3TGAZ6JNI  | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "DE91100000000123456780        | INVALID_CHECKSUM         | IBAN violates ISO 7064 Mod 97-10 checksum check"
    })
    void testInvalidIban(String ibanInput, IbanValidationError expectedValidationError, String expectedMessagePattern) {
        String ibanInputNorm = ibanInput == null ? null : ibanInput.replace(" ", "");

        IbanValidator.setLastReason(null);

        assertThatInvalidIbanException()
            .isThrownBy(() -> Iban.of(ibanInput))
            .withCause(null)
            .withMessage(expectedMessagePattern)
            .hasFieldOrPropertyWithValue("reason", expectedValidationError);

        assertThat(IbanValidator.getLastReason())
            .isNotNull()
            .extracting(IbanValidationError::getText)
            .isEqualTo(expectedMessagePattern);

        assertThatInvalidIbanException()
            .isThrownBy(() -> Iban.ofNormalized(ibanInputNorm))
            .withCause(null)
            .withMessage(expectedMessagePattern)
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
     * Tests {@link Iban#of(CharSequence)} with random IBANs.
     *
     * @param ibanInput the valid random IBAN string to test
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @RandomIbanSource(ibanCount = 500)
    void testRandomIban(String ibanInput) {
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

    @ParameterizedTest(name = "[{index}] {2}: {0}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // ibanInput                       | len| cc | fl | cd | bban                          | bankCode   | branch | account                 | ncd| ibanPlusLen
        // --------------------------------+----+----+----+----+-------------------------------+------------+--------+-------------------------+----+------------
        "AD1200012030200359100100          | 24 | AD | 🇦🇩 | 12 | 00012030200359100100          | 0001       | 2030   | 200359100100            |    |  8",
        "AE070331234567890123456           | 23 | AE | 🇦🇪 | 07 | 0331234567890123456           | 033        |        | 1234567890123456        |    |  3",
        "AL47212110090000000235698741      | 28 | AL | 🇦🇱 | 47 | 212110090000000235698741      | 212        | 1100   | 0000000235698741        | 9  |  8",
        "AO06000600000100037131174         | 25 | AO | 🇦🇴 | 06 | 000600000100037131174         | 0006       | 0000   | 01000371311             | 74 |  8",
        "AT611904300234573201              | 20 | AT | 🇦🇹 | 61 | 1904300234573201              | 19043      |        | 00234573201             |    |  5",
        "AZ21NABZ00000000137010001944      | 28 | AZ | 🇦🇿 | 21 | NABZ00000000137010001944      | NABZ       |        | 00000000137010001944    |    |  4",
        "BA391290079401028494              | 20 | BA | 🇧🇦 | 39 | 1290079401028494              | 129        | 007    | 94010284                | 94 |  6",
        "BE68539007547034                  | 16 | BE | 🇧🇪 | 68 | 539007547034                  | 539        |        | 0075470                 | 34 |  3",
        "BF21BF084010130046357400039       | 27 | BF | 🇧🇫 | 21 | BF084010130046357400039       | BF084      | 01013  | 00463574000             | 39 | 10",
        "BG80BNBG96611020345678            | 22 | BG | 🇧🇬 | 80 | BNBG96611020345678            | BNBG       | 9661   | 20345678                |    |  8",
        "BH67BMAG00001299123456            | 22 | BH | 🇧🇭 | 67 | BMAG00001299123456            | BMAG       |        | 00001299123456          |    |  4",
        "BI4210000100010000332045181       | 27 | BI | 🇧🇮 | 42 | 10000100010000332045181       | 10000      | 10001  | 0000332045181           |    | 10",
        "BJ66BJ0610100100144390000769      | 28 | BJ | 🇧🇯 | 66 | BJ0610100100144390000769      | BJ061      | 01001  | 001443900007            | 69 | 10",
        "BR1800360305000010009795493C1     | 29 | BR | 🇧🇷 | 18 | 00360305000010009795493C1     | 00360305   | 00001  | 0009795493              |    | 13",
        "BY13NBRB3600900000002Z00AB00      | 28 | BY | 🇧🇾 | 13 | NBRB3600900000002Z00AB00      | NBRB       | 3600   | 900000002Z00AB00        |    |  8",
        "CF4220001000010120069700160       | 27 | CF | 🇨🇫 | 42 | 20001000010120069700160       | 20001      | 00001  | 01200697001             | 60 | 10",
        "CH9300762011623852957             | 21 | CH | 🇨🇭 | 93 | 00762011623852957             | 00762      |        | 011623852957            |    |  5",
        "CM2110003024000224016952238       | 27 | CM | 🇨🇲 | 21 | 10003024000224016952238       | 10003      | 02400  | 02240169522             | 38 | 10",
        "CR05015202001026284066            | 22 | CR | 🇨🇷 | 05 | 015202001026284066            | 0152       |        | 02001026284066          |    |  4",
        "CV05123412341234123412341         | 25 | CV | 🇨🇻 | 05 | 123412341234123412341         | 1234       | 1234   | 1234123412341           |    |  8",
        "CY17002001280000001200527600      | 28 | CY | 🇨🇾 | 17 | 002001280000001200527600      | 002        | 00128  | 0000001200527600        |    |  8",
        "CZ6508000000192000145399          | 24 | CZ | 🇨🇿 | 65 | 08000000192000145399          | 0800       |        | 0000192000145399        |    |  4",
        "DE89370400440532013000            | 22 | DE | 🇩🇪 | 89 | 370400440532013000            | 37040044   |        | 0532013000              |    |  8",
        "DJ2100010000000154000100186       | 27 | DJ | 🇩🇯 | 21 | 00010000000154000100186       | 00010      | 00000  | 01540001001             |    | 10",
        "DK5000400440116243                | 18 | DK | 🇩🇰 | 50 | 00400440116243                | 0040       |        | 0440116243              |    |  4",
        "DO28BAGR00000001212453611324      | 28 | DO | 🇩🇴 | 28 | BAGR00000001212453611324      | BAGR       |        | 00000001212453611324    |    |  4",
        "DZ1700021000011130000005          | 24 | DZ | 🇩🇿 | 17 | 00021000011130000005          | 000        | 21000  | 0111300000              | 05 |  8",
        "EE382200221020145685              | 20 | EE | 🇪🇪 | 38 | 2200221020145685              | 22         | 00     | 22102014568             | 5  |  4",
        "EG380019000500000000263180002     | 29 | EG | 🇪🇬 | 38 | 0019000500000000263180002     | 0019       | 0005   | 00000000263180002       |    |  8",
        "ES9121000418450200051332          | 24 | ES | 🇪🇸 | 91 | 21000418450200051332          | 2100       | 0418   | 0200051332              | 45 | 10",
        "FI2112345600000785                | 18 | FI | 🇫🇮 | 21 | 12345600000785                | 123456     |        | 0000078                 | 5  |  6",
        "FK88SC123456789012                | 18 | FK | 🇫🇰 | 88 | SC123456789012                | SC         |        | 123456789012            |    |  2",
        "FO6264600001631634                | 18 | FO | 🇫🇴 | 62 | 64600001631634                | 6460       |        | 000163163               | 4  |  4",
        "FR1420041010050500013M02606       | 27 | FR | 🇫🇷 | 14 | 20041010050500013M02606       | 20041      | 01005  | 0500013M026             | 06 | 10",
        "GA2140021010032001890020126       | 27 | GA | 🇬🇦 | 21 | 40021010032001890020126       | 40021      | 01003  | 2001890020126           |    | 10",
        "GB29NWBK60161331926819            | 22 | GB | 🇬🇧 | 29 | NWBK60161331926819            | NWBK       | 601613 | 31926819                |    | 10",
        "GE29NB0000000101904917            | 22 | GE | 🇬🇪 | 29 | NB0000000101904917            | NB         |        | 0000000101904917        |    |  2",
        "GI75NWBK000000007099453           | 23 | GI | 🇬🇮 | 75 | NWBK000000007099453           | NWBK       |        | 000000007099453         |    |  4",
        "GL8964710001000206                | 18 | GL | 🇬🇱 | 89 | 64710001000206                | 6471       |        | 0001000206              |    |  4",
        "GQ7050002001003715228190196       | 27 | GQ | 🇬🇶 | 70 | 50002001003715228190196       | 50002      | 00100  | 37152281901             | 96 | 10",
        "GR1601101250000000012300695       | 27 | GR | 🇬🇷 | 16 | 01101250000000012300695       | 011        | 0125   | 0000000012300695        |    |  7",
        "GT82TRAJ01020000001210029690      | 28 | GT | 🇬🇹 | 82 | TRAJ01020000001210029690      | TRAJ       |        | 01020000001210029690    |    |  4",
        "HN88CABF00000000000250005469      | 28 | HN | 🇭🇳 | 88 | CABF00000000000250005469      | CABF       |        | 00000000000250005469    |    |  4",
        "HR1210010051863000160             | 21 | HR | 🇭🇷 | 12 | 10010051863000160             | 1001005    |        | 1863000160              |    |  7",
        "HU42117730161111101800000000      | 28 | HU | 🇭🇺 | 42 | 117730161111101800000000      | 117        | 7301   | 6111110180000000        | 0  |  7",
        "IE29AIBK93115212345678            | 22 | IE | 🇮🇪 | 29 | AIBK93115212345678            | AIBK       | 931152 | 12345678                |    | 10",
        "IL620108000000099999999           | 23 | IL | 🇮🇱 | 62 | 0108000000099999999           | 010        | 800    | 0000099999999           |    |  6",
        "IQ98NBIQ850123456789012           | 23 | IQ | 🇮🇶 | 98 | NBIQ850123456789012           | NBIQ       | 850    | 123456789012            |    |  7",
        "IR062960000000100324200001        | 26 | IR | 🇮🇷 | 06 | 2960000000100324200001        | 296        |        | 0000000100324200001     |    |  3",
        "IS140159260076545510730339        | 26 | IS | 🇮🇸 | 14 | 0159260076545510730339        | 01         | 59     | 007654                  |    |  4",
        "IT60X0542811101000000123456       | 27 | IT | 🇮🇹 | 60 | X0542811101000000123456       | 05428      | 11101  | 000000123456            | X  | 10",
        "JO94CBJO0010000000000131000302    | 30 | JO | 🇯🇴 | 94 | CBJO0010000000000131000302    | CBJO       | 0010   | 000000000131000302      |    |  8",
        "KM4600005000010010904400137       | 27 | KM | 🇰🇲 | 46 | 00005000010010904400137       | 00005      | 00001  | 00109044001             | 37 | 10",
        "KW81CBKU0000000000001234560101    | 30 | KW | 🇰🇼 | 81 | CBKU0000000000001234560101    | CBKU       |        | 0000000000001234560101  |    |  4",
        "KZ86125KZT5004100100              | 20 | KZ | 🇰🇿 | 86 | 125KZT5004100100              | 125        |        | KZT5004100100           |    |  3",
        "LB62099900000001001901229114      | 28 | LB | 🇱🇧 | 62 | 099900000001001901229114      | 0999       |        | 00000001001901229114    |    |  4",
        "LC55HEMM000100010012001200023015  | 32 | LC | 🇱🇨 | 55 | HEMM000100010012001200023015  | HEMM       |        | 000100010012001200023015|    |  4",
        "LI21088100002324013AA             | 21 | LI | 🇱🇮 | 21 | 088100002324013AA             | 08810      |        | 0002324013AA            |    |  5",
        "LT121000011101001000              | 20 | LT | 🇱🇹 | 12 | 1000011101001000              | 10000      |        | 11101001000             |    |  5",
        "LU280019400644750000              | 20 | LU | 🇱🇺 | 28 | 0019400644750000              | 001        |        | 9400644750000           |    |  3",
        "LV80BANK0000435195001             | 21 | LV | 🇱🇻 | 80 | BANK0000435195001             | BANK       |        | 0000435195001           |    |  4",
        "LY83002048000020100120361         | 25 | LY | 🇱🇾 | 83 | 002048000020100120361         | 002        | 048    | 000020100120361         |    |  6",
        "MA64360815000001793222001617      | 28 | MA | 🇲🇦 | 64 | 360815000001793222001617      | 360        | 81500  | 0001793222001617        |    |  8",
        "MC5811222000010123456789030       | 27 | MC | 🇲🇨 | 58 | 11222000010123456789030       | 11222      | 00001  | 01234567890             | 30 | 10",
        "MD24AG000225100013104168          | 24 | MD | 🇲🇩 | 24 | AG000225100013104168          | AG         |        | 000225100013104168      |    |  2",
        "ME25505000012345678951            | 22 | ME | 🇲🇪 | 25 | 505000012345678951            | 505        |        | 0000123456789           | 51 |  3",
        "MK07250120000058984               | 19 | MK | 🇲🇰 | 07 | 250120000058984               | 250        |        | 1200000589              | 84 |  3",
        "MN121234123456789123              | 20 | MN | 🇲🇳 | 12 | 1234123456789123              | 1234       |        | 123456789123            |    |  4",
        "MR1300020001010000123456753       | 27 | MR | 🇲🇷 | 13 | 00020001010000123456753       | 00020      | 00101  | 00001234567             | 53 | 10",
        "MT84MALT011000012345MTLCAST001S   | 31 | MT | 🇲🇹 | 84 | MALT011000012345MTLCAST001S   | MALT       | 01100  | 0012345MTLCAST001S      |    |  9",
        "MU17BOMM0101101030300200000MUR    | 30 | MU | 🇲🇺 | 17 | BOMM0101101030300200000MUR    | BOMM01     | 01     | 101030300200000MUR      |    |  8",
        "MZ59000800005138555713187         | 25 | MZ | 🇲🇿 | 59 | 000800005138555713187         | 0008       | 0000   | 51385557131             | 87 |  8",
        "NI45BAPR00000013000003558124      | 28 | NI | 🇳🇮 | 45 | BAPR00000013000003558124      | BAPR       |        | 00000013000003558124    |    |  4",
        "NL91ABNA0417164300                | 18 | NL | 🇳🇱 | 91 | ABNA0417164300                | ABNA       |        | 0417164300              |    |  4",
        "NO9386011117947                   | 15 | NO | 🇳🇴 | 93 | 86011117947                   | 8601       |        | 111794                  | 7  |  4",
        "OM810180000001299123456           | 23 | OM | 🇴🇲 | 81 | 0180000001299123456           | 018        |        | 0000001299123456        |    |  3",
        "PK36SCBL0000001123456702          | 24 | PK | 🇵🇰 | 36 | SCBL0000001123456702          | SCBL       |        | 0000001123456702        |    |  4",
        "PL61109010140000071219812874      | 28 | PL | 🇵🇱 | 61 | 109010140000071219812874      | 109        | 0101   | 0000071219812874        | 4  |  8",
        "PS92PALS000000000400123456702     | 29 | PS | 🇵🇸 | 92 | PALS000000000400123456702     | PALS       |        | 000000000400123456702   |    |  4",
        "PT50000201231234567890154         | 25 | PT | 🇵🇹 | 50 | 000201231234567890154         | 0002       | 0123   | 12345678901             | 54 |  8",
        "QA58DOHB00001234567890ABCDEFG     | 29 | QA | 🇶🇦 | 58 | DOHB00001234567890ABCDEFG     | DOHB       |        | 00001234567890ABCDEFG   |    |  4",
        "RO49AAAA1B31007593840000          | 24 | RO | 🇷🇴 | 49 | AAAA1B31007593840000          | AAAA       |        | 1B31007593840000        |    |  4",
        "RS35260005601001611379            | 22 | RS | 🇷🇸 | 35 | 260005601001611379            | 260        |        | 0056010016113           | 79 |  3",
        "RU0304452522540817810538091310419 | 33 | RU | 🇷🇺 | 03 | 04452522540817810538091310419 | 044525225  | 40817  | 810538091310419         |    | 14",
        "SA0380000000608010167519          | 24 | SA | 🇸🇦 | 03 | 80000000608010167519          | 80         |        | 000000608010167519      |    |  2",
        "SC18SSCB11010000000000001497USD   | 31 | SC | 🇸🇨 | 18 | SSCB11010000000000001497USD   | SSCB11     | 01     | 0000000000001497        |    |  8",
        "SD2129010501234001                | 18 | SD | 🇸🇩 | 21 | 29010501234001                | 29         |        | 010501234001            |    |  2",
        "SE4550000000058398257466          | 24 | SE | 🇸🇪 | 45 | 50000000058398257466          | 500        |        | 00000058398257466       |    |  3",
        "SI56263300012039086               | 19 | SI | 🇸🇮 | 56 | 263300012039086               | 26         | 330    | 00120390                | 86 |  5",
        "SK3112000000198742637541          | 24 | SK | 🇸🇰 | 31 | 12000000198742637541          | 1200       |        | 0000198742637541        |    |  4",
        "SM86U0322509800000000270100       | 27 | SM | 🇸🇲 | 86 | U0322509800000000270100       | 03225      | 09800  | 000000270100            | U  | 10",
        "SN08SN0100152000048500003035      | 28 | SN | 🇸🇳 | 08 | SN0100152000048500003035      | SN010      | 01520  | 000485000030            | 35 | 10",
        "SO211000001001000100141           | 23 | SO | 🇸🇴 | 21 | 1000001001000100141           | 1000       | 001    | 001000100141            |    |  7",
        "ST23000100010051845310146         | 25 | ST | 🇸🇹 | 23 | 000100010051845310146         | 0001       | 0001   | 0051845310146           |    |  8",
        "SV62CENR00000000000000700025      | 28 | SV | 🇸🇻 | 62 | CENR00000000000000700025      | CENR       |        | 00000000000000700025    |    |  4",
        "TG87TG0090100110232500400512      | 28 | TG | 🇹🇬 | 87 | TG0090100110232500400512      | TG009      | 01001  | 102325004005            | 12 | 10",
        "TL380080012345678910157           | 23 | TL | 🇹🇱 | 38 | 0080012345678910157           | 008        |        | 00123456789101          | 57 |  3",
        "TN5910006035183598478831          | 24 | TN | 🇹🇳 | 59 | 10006035183598478831          | 10         | 006    | 0351835984788           | 31 |  5",
        "TR330006100519786457841326        | 26 | TR | 🇹🇷 | 33 | 0006100519786457841326        | 00061      |        | 0519786457841326        | 0  |  6",
        "UA213223130000026007233566001     | 29 | UA | 🇺🇦 | 21 | 3223130000026007233566001     | 322313     |        | 0000026007233566001     |    |  6",
        "VA59001123000012345678            | 22 | VA | 🇻🇦 | 59 | 001123000012345678            | 001        |        | 123000012345678         |    |  3",
        "VG96VPVG0000012345678901          | 24 | VG | 🇻🇬 | 96 | VPVG0000012345678901          | VPVG       |        | 0000012345678901        |    |  4",
        "XK051212012345678906              | 20 | XK | 🇽🇰 | 05 | 1212012345678906              | 12         | 12     | 0123456789              | 06 |  4",
        "YE15CBYE0001018861234567891234    | 30 | YE | 🇾🇪 | 15 | CBYE0001018861234567891234    | CBYE       | 0001   | 018861234567891234      |    |  8",
    })
    @SuppressWarnings("checkstyle:ParameterNumber")
    void testValidIbanAllCountries(String ibanInput, int expectedIbanLength, String expectedCountryCode, String expectedCountryFlag,
            String expectedCheckDigits, String expectedBban, String expectedBankCode, String expectedBranchCode,
            String expectedAccountNumber, String expectedNcd, Integer expectedIbanPlusLen) {

        IbanRegistry countryData = IbanRegistry.getByCode(expectedCountryCode);

        assertThat(countryData)
            .isNotNull()
            .satisfies(reg -> {
                assertThat(reg.getPrimary()).isNull();
                assertThat(reg.getIbanRegex()).isNotNull();
                assertThat(reg.getStructureData()).isNotNull();
                assertThat(reg.getIbanExample()).isNotNull();
            });

        Iban iban = Iban.of(ibanInput);

        assertThat(iban)
            .hasLength(expectedIbanLength)
            .hasCountryCode(expectedCountryCode)
            .hasCountryFlag(expectedCountryFlag)
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

            .matches(countryData.getIbanRegex())

            .extracting(Iban::toFormattedString)
            .asString()
            .isNotBlank()
            .startsWith(countryData.getCountryCode());

        assertThat(IbanPlusKey.of(iban))
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
    @ParameterizedTest
    @EnumSource(IbanRegistry.class)
    void testInvalidIbanAllCountries(IbanRegistry entry) {
        String ibanStr1 = entry.getCountryCode() + "00" + "999999999999999999999999999999".substring(0, entry.getBbanLength());
        String ibanStr2 = entry.getCountryCode() + "00" + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".substring(0, entry.getBbanLength());

        for (String iban : Arrays.asList(ibanStr1, ibanStr2)) {
            assertThatInvalidIbanException()
                .isThrownBy(() -> Iban.of(iban))
                .withCause(null)
                .withMessageMatching(IbanValidationError.INVALID_STRUCTURE.getText() + '|' + IbanValidationError.INVALID_CHECKSUM.getText())
                .extracting("reason")
                .isIn(IbanValidationError.INVALID_STRUCTURE, IbanValidationError.INVALID_CHECKSUM);
        }

        Optional.ofNullable(entry.getCountryValidator())
            .ifPresent(v -> assertThat(v.validateIban(new char[entry.getIbanLength()])).isFalse());
    }

    /**
     * Tests {@link Iban#equals(Object)} and {@link Iban#hashCode()}.<br>
     * Ensures two IBAN instances created from the same normalized string are considered equal
     * and have the same hash code, following the contract for immutable value objects.
     */
    @Test
    void testEqualsAndHashCode() {
        String ibanStr1 = "DE89370400440532013000";
        String ibanStr2 = "DE89370400440532013000";
        String ibanStr3 = "DE62370400440532013001";

        Iban iban1 = Iban.of(ibanStr1);
        Iban iban2 = Iban.ofNormalized(ibanStr2);
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
    @ParameterizedTest(name = "[{index}] Compare ''{0}'' with ''{1}'': Expected sign {2}")
    @CsvSource(delimiter = '|', value = {
        "DE89370400440532013000      | DE89370400440532013000      | 0  ", // equal
        "DE62370400440532013001      | DE89370400440532013000      | -1 ", // iban1 < iban2
        "DE89370400440532013000      | DE62370400440532013001      | 1  ", // iban1 > iban2
        "FR1420041010050500013M02606 | DE89370400440532013000      | 1  ", // FR > DE (alphabetical)
        "DE89370400440532013000      | FR1420041010050500013M02606 | -1 "  // DE < FR
    })
    void testCompareTo(String iban1Input, String iban2Input, int expectedSign) {
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
    @ParameterizedTest(name = "[{index}] charAt({0}) must be ''{1}''")
    @CsvSource(delimiter = '|', value = {
        "0  | D",
        "1  | E",
        "2  | 8",
        "21 | 0"
    })
    void testCharAt(int index, char expectedChar) {
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
    @ParameterizedTest(name = "[{index}] subSequence({0}, {1}) must be ''{2}''")
    @CsvSource(delimiter = '|', value = {
        "0 | 2  | DE",                    // Country Code
        "2 | 4  | 89",                    // Check Digits
        "4 | 22 | 370400440532013000",    // BBAN
        "0 | 22 | DE89370400440532013000" // Full IBAN
    })
    void testSubSequence(int start, int end, String expectedString) {
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
    @ParameterizedTest(name = "[{index}] Serialization round-trip: ''{0}''")
    @ValueSource(strings = {
        "DE89370400440532013000",
        "GB29NWBK60161331926819",
        "FR1420041010050500013M02606",
        "NL91ABNA0417164300",
        "PL61109010140000071219812874"
    })
    void testSerializationRoundTrip(String ibanInput) throws IOException, ClassNotFoundException {
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
    @Test
    void testSerializedFormUsesMementoProxy() throws IOException {
        Iban iban = Iban.of("DE89370400440532013000");
        final Iban iban1 = iban;
        byte[] bytes = TestUtil.serialize(iban1);

        // The serialized stream must contain the Memento class name, not Iban itself
        String streamContent = new String(bytes);
        assertThat(streamContent)
            .as("Serialized stream must reference the Memento proxy class")
            .contains("Memento");
        assertThat(streamContent)
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
    @Test
    void testDirectDeserializationIsRejected() throws IOException {
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
    @Test
    void testDeserializedInstanceIsDistinct() throws IOException, ClassNotFoundException {
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
    @ParameterizedTest(name = "[{index}] isSepa flag preserved for ''{0}'' (expected: {1})")
    @CsvSource(delimiter = '|', value = {
        "SE4550000000058398257466      | true", // Sweden - SEPA
        "ES9121000418450200051332      | true", // Spain - SEPA
        "PS92PALS000000000400123456702 | false" // Palestine - non-SEPA
    })
    void testSerializationPreservesSepaFlag(String ibanInput, boolean expectedSepa)
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
    @Test
    void testMementoReadResolveRejectsInvalidIban() throws IOException {
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
    @Test
    void testMementoReadObjectRejectsUnknownStreamVersion() throws IOException {
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
    @Test
    void testReadObjectNoDataThrows() throws Exception {
        Iban iban = Iban.of("DE89370400440532013000");

        java.lang.reflect.Method m = Iban.class.getDeclaredMethod("readObjectNoData");
        m.setAccessible(true);

        assertThat(catchThrowable(() -> m.invoke(iban)))
            .as("readObjectNoData() must throw InvalidObjectException wrapped in InvocationTargetException")
            .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
            .cause()
            .isInstanceOf(InvalidObjectException.class)
            .hasMessageContaining("must be deserialized via its Memento proxy");
    }

}
