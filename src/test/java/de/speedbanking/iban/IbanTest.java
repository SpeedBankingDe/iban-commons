package de.speedbanking.iban;

import static de.speedbanking.iban.IbanAssertions.assertThat;
import static de.speedbanking.iban.IbanAssertions.assertThatInvalidIbanException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatComparable;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Optional;

/**
 * Unit tests for the new immutable {@link Iban} class, covering IBAN validation and component extraction.
 */
class IbanTest {

    /**
     * Tests {@link Iban#of(String)} with various valid IBANs.
     *
     * @param ibanInput The valid IBAN string to test.
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

        Iban iban1 = Iban.of(ibanInput);
        assertThat(iban1)
            .as("IBAN instance 1 must not be null and match input string")
            .isNotNull()
            .hasToString(ibanInputNorm);

        assertThatCode(
            () -> Iban.ofNormalized(ibanInputNorm))
            .as("Normalized IBAN '%s' is valid and should be instantiable", ibanInput)
            .doesNotThrowAnyException();

        Iban iban2 = Iban.ofNormalized(ibanInputNorm);
        assertThat(iban2)
            .as("IBAN instance 2 must not be null and match input string")
            .isNotNull()
            .hasToString(ibanInputNorm);

        assertThat(iban1).isEqualTo(iban2);
        assertThatComparable(iban1).isEqualTo(iban2);

        assertThat(Iban.tryParse(ibanInput))
            .isPresent()
            .contains(iban1);

        assertThat(Iban.isValid(ibanInputNorm)).isTrue();

        String invalidIban = ibanInput.substring(0, ibanInput.length() - 1) + "X";
        assertThat(Iban.isValid(invalidIban)).isFalse();
    }

    /**
     * Tests {@link Iban#of(String)} with various invalid IBANs,
     * expecting an {@link InvalidIbanException} with a specific message pattern.
     *
     * @param ibanInput The invalid IBAN string to test.
     * @param expectedMessagePattern The regex pattern for the expected exception message.
     */
    @ParameterizedTest(name = "[{index}] Invalid IBAN ''{0}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        // IBAN (Input)                | ValidationError (Enum)   | Expected Message Pattern
        "(null)                        | EMPTY                    | IBAN is null or empty",
        "''                            | EMPTY                    | IBAN is null or empty",
        "'   '                         | EMPTY                    | IBAN is null or empty",
        "PS92pals000000000400123456702 | ILLEGAL_CHARACTERS       | IBAN contains illegal character(s)",
        "ps92pals000000000400123456702 | INVALID_COUNTRY          | IBAN has invalid country code",
        "XX12345678901234567890        | UNSUPPORTED_COUNTRY      | IBAN has unsupported country code",
        "DE123                         | INCORRECT_LENGTH         | IBAN has incorrect length",
        "DE91BHLSDEM1123456789         | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
        "GB33BUKB2020155555555         | INCORRECT_LENGTH_COUNTRY | IBAN has incorrect length for specified country",
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

        assertThat(Iban.tryParse(ibanInput)).isEmpty();
        assertThat(IbanValidator.getLastReason()).isEqualTo(expectedValidationError);
        IbanValidator.setLastReason(null);
    }

    /**
     * Tests {@link Iban#of(String)} with random IBANs.
     *
     * @param ibanInput The valid random  IBAN string to test.
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
        assertThat(Iban.isValid(ibanInput)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {2}: {0}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "AD1200012030200359100100          | 24 | AD | 🇦🇩 | 12 | 00012030200359100100          | 0001       | 2030   | 200359100100            ",
        "AE070331234567890123456           | 23 | AE | 🇦🇪 | 07 | 0331234567890123456           | 033        | (null) | 1234567890123456        ",
        "AL47212110090000000235698741      | 28 | AL | 🇦🇱 | 47 | 212110090000000235698741      | 212        | 1100   | 0000000235698741        ",
        "AO06000600000100037131174         | 25 | AO | 🇦🇴 | 06 | 000600000100037131174         | 0006       | 0000   | 01000371311             ",
        "AT611904300234573201              | 20 | AT | 🇦🇹 | 61 | 1904300234573201              | 19043      | (null) | 00234573201             ",
        "AZ21NABZ00000000137010001944      | 28 | AZ | 🇦🇿 | 21 | NABZ00000000137010001944      | NABZ       | (null) | 00000000137010001944    ",
        "BA391290079401028494              | 20 | BA | 🇧🇦 | 39 | 1290079401028494              | 129        | 007    | 94010284                ",
        "BE68539007547034                  | 16 | BE | 🇧🇪 | 68 | 539007547034                  | 539        | (null) | 0075470                 ",
        "BG80BNBG96611020345678            | 22 | BG | 🇧🇬 | 80 | BNBG96611020345678            | BNBG       | 9661   | 20345678                ",
        "BH67BMAG00001299123456            | 22 | BH | 🇧🇭 | 67 | BMAG00001299123456            | BMAG       | (null) | 00001299123456          ",
        "BI4210000100010000332045181       | 27 | BI | 🇧🇮 | 42 | 10000100010000332045181       | 10000      | 10001  | 0000332045181           ",
        "BR1800360305000010009795493C1     | 29 | BR | 🇧🇷 | 18 | 00360305000010009795493C1     | 00360305   | 00001  | 0009795493              ",
        "BY13NBRB3600900000002Z00AB00      | 28 | BY | 🇧🇾 | 13 | NBRB3600900000002Z00AB00      | NBRB       | 3600   | 900000002Z00AB00        ",
        "CH9300762011623852957             | 21 | CH | 🇨🇭 | 93 | 00762011623852957             | 00762      | (null) | 011623852957            ",
        "CR05015202001026284066            | 22 | CR | 🇨🇷 | 05 | 015202001026284066            | 0152       | (null) | 02001026284066          ",
        "CV05123412341234123412341         | 25 | CV | 🇨🇻 | 05 | 123412341234123412341         | 1234       | 1234   | 1234123412341           ",
        "CY17002001280000001200527600      | 28 | CY | 🇨🇾 | 17 | 002001280000001200527600      | 002        | 00128  | 0000001200527600        ",
        "CZ6508000000192000145399          | 24 | CZ | 🇨🇿 | 65 | 08000000192000145399          | 0800       | (null) | 0000192000145399        ",
        "DE89370400440532013000            | 22 | DE | 🇩🇪 | 89 | 370400440532013000            | 37040044   | (null) | 0532013000              ",
        "DJ2100010000000154000100186       | 27 | DJ | 🇩🇯 | 21 | 00010000000154000100186       | 00010      | 00000  | 01540001001             ",
        "DK5000400440116243                | 18 | DK | 🇩🇰 | 50 | 00400440116243                | 0040       | (null) | 0440116243              ",
        "DO28BAGR00000001212453611324      | 28 | DO | 🇩🇴 | 28 | BAGR00000001212453611324      | BAGR       | (null) | 00000001212453611324    ",
        "EE382200221020145685              | 20 | EE | 🇪🇪 | 38 | 2200221020145685              | 22         | 00     | 22102014568             ",
        "EG380019000500000000263180002     | 29 | EG | 🇪🇬 | 38 | 0019000500000000263180002     | 0019       | 0005   | 00000000263180002       ",
        "ES9121000418450200051332          | 24 | ES | 🇪🇸 | 91 | 21000418450200051332          | 2100       | 0418   | 0200051332              ",
        "FI2112345600000785                | 18 | FI | 🇫🇮 | 21 | 12345600000785                | 123456     | (null) | 0000078                 ",
        "FK88SC123456789012                | 18 | FK | 🇫🇰 | 88 | SC123456789012                | SC         | (null) | 123456789012            ",
        "FO6264600001631634                | 18 | FO | 🇫🇴 | 62 | 64600001631634                | 6460       | (null) | 000163163               ",
        "FR1420041010050500013M02606       | 27 | FR | 🇫🇷 | 14 | 20041010050500013M02606       | 20041      | 01005  | 0500013M026             ",
        "GA2140021010032001890020126       | 27 | GA | 🇬🇦 | 21 | 40021010032001890020126       | 40021      | 01003  | 2001890020126           ",
        "GB29NWBK60161331926819            | 22 | GB | 🇬🇧 | 29 | NWBK60161331926819            | NWBK       | 601613 | 31926819                ",
        "GE29NB0000000101904917            | 22 | GE | 🇬🇪 | 29 | NB0000000101904917            | NB         | (null) | 0000000101904917        ",
        "GI75NWBK000000007099453           | 23 | GI | 🇬🇮 | 75 | NWBK000000007099453           | NWBK       | (null) | 000000007099453         ",
        "GL8964710001000206                | 18 | GL | 🇬🇱 | 89 | 64710001000206                | 6471       | (null) | 0001000206              ",
        "GR1601101250000000012300695       | 27 | GR | 🇬🇷 | 16 | 01101250000000012300695       | 011        | 0125   | 0000000012300695        ",
        "GT82TRAJ01020000001210029690      | 28 | GT | 🇬🇹 | 82 | TRAJ01020000001210029690      | TRAJ       | (null) | 01020000001210029690    ",
        "HN88CABF00000000000250005469      | 28 | HN | 🇭🇳 | 88 | CABF00000000000250005469      | CABF       | (null) | 00000000000250005469    ",
        "HR1210010051863000160             | 21 | HR | 🇭🇷 | 12 | 10010051863000160             | 1001005    | (null) | 1863000160              ",
        "HU42117730161111101800000000      | 28 | HU | 🇭🇺 | 42 | 117730161111101800000000      | 117        | 7301   | 6111110180000000        ",
        "IE29AIBK93115212345678            | 22 | IE | 🇮🇪 | 29 | AIBK93115212345678            | AIBK       | 931152 | 12345678                ",
        "IL620108000000099999999           | 23 | IL | 🇮🇱 | 62 | 0108000000099999999           | 010        | 800    | 0000099999999           ",
        "IQ98NBIQ850123456789012           | 23 | IQ | 🇮🇶 | 98 | NBIQ850123456789012           | NBIQ       | 850    | 123456789012            ",
        "IR062960000000100324200001        | 26 | IR | 🇮🇷 | 06 | 2960000000100324200001        | 296        | (null) | 0000000100324200001     ",
        "IS140159260076545510730339        | 26 | IS | 🇮🇸 | 14 | 0159260076545510730339        | 01         | 59     | 007654                  ",
        "IT60X0542811101000000123456       | 27 | IT | 🇮🇹 | 60 | X0542811101000000123456       | 05428      | 11101  | 000000123456            ",
        "JO94CBJO0010000000000131000302    | 30 | JO | 🇯🇴 | 94 | CBJO0010000000000131000302    | CBJO       | 0010   | 000000000131000302      ",
        "KW81CBKU0000000000001234560101    | 30 | KW | 🇰🇼 | 81 | CBKU0000000000001234560101    | CBKU       | (null) | 0000000000001234560101  ",
        "KZ86125KZT5004100100              | 20 | KZ | 🇰🇿 | 86 | 125KZT5004100100              | 125        | (null) | KZT5004100100           ",
        "LB62099900000001001901229114      | 28 | LB | 🇱🇧 | 62 | 099900000001001901229114      | 0999       | (null) | 00000001001901229114    ",
        "LC55HEMM000100010012001200023015  | 32 | LC | 🇱🇨 | 55 | HEMM000100010012001200023015  | HEMM       | (null) | 000100010012001200023015",
        "LI21088100002324013AA             | 21 | LI | 🇱🇮 | 21 | 088100002324013AA             | 08810      | (null) | 0002324013AA            ",
        "LT121000011101001000              | 20 | LT | 🇱🇹 | 12 | 1000011101001000              | 10000      | (null) | 11101001000             ",
        "LU280019400644750000              | 20 | LU | 🇱🇺 | 28 | 0019400644750000              | 001        | (null) | 9400644750000           ",
        "LV80BANK0000435195001             | 21 | LV | 🇱🇻 | 80 | BANK0000435195001             | BANK       | (null) | 0000435195001           ",
        "LY83002048000020100120361         | 25 | LY | 🇱🇾 | 83 | 002048000020100120361         | 002        | 048    | 000020100120361         ",
        "MA64360815000001793222001617      | 28 | MA | 🇲🇦 | 64 | 360815000001793222001617      | 360        | 81500  | 0001793222001617        ",
        "MC5811222000010123456789030       | 27 | MC | 🇲🇨 | 58 | 11222000010123456789030       | 11222      | 00001  | 01234567890             ",
        "MD24AG000225100013104168          | 24 | MD | 🇲🇩 | 24 | AG000225100013104168          | AG         | (null) | 000225100013104168      ",
        "ME25505000012345678951            | 22 | ME | 🇲🇪 | 25 | 505000012345678951            | 505        | (null) | 0000123456789           ",
        "MK07250120000058984               | 19 | MK | 🇲🇰 | 07 | 250120000058984               | 250        | (null) | 1200000589              ",
        "MN121234123456789123              | 20 | MN | 🇲🇳 | 12 | 1234123456789123              | 1234       | (null) | 123456789123            ",
        "MR1300020001010000123456753       | 27 | MR | 🇲🇷 | 13 | 00020001010000123456753       | 00020      | 00101  | 00001234567             ",
        "MT84MALT011000012345MTLCAST001S   | 31 | MT | 🇲🇹 | 84 | MALT011000012345MTLCAST001S   | MALT       | 01100  | 0012345MTLCAST001S      ",
        "MU17BOMM0101101030300200000MUR    | 30 | MU | 🇲🇺 | 17 | BOMM0101101030300200000MUR    | BOMM01     | 01     | 101030300200000MUR      ",
        "MZ59000800005138555713187         | 25 | MZ | 🇲🇿 | 59 | 000800005138555713187         | 0008       | 0000   | 51385557131             ",
        "NI45BAPR00000013000003558124      | 28 | NI | 🇳🇮 | 45 | BAPR00000013000003558124      | BAPR       | (null) | 00000013000003558124    ",
        "NL91ABNA0417164300                | 18 | NL | 🇳🇱 | 91 | ABNA0417164300                | ABNA       | (null) | 0417164300              ",
        "NO9386011117947                   | 15 | NO | 🇳🇴 | 93 | 86011117947                   | 8601       | (null) | 111794                  ",
        "OM810180000001299123456           | 23 | OM | 🇴🇲 | 81 | 0180000001299123456           | 018        | (null) | 0000001299123456        ",
        "PK36SCBL0000001123456702          | 24 | PK | 🇵🇰 | 36 | SCBL0000001123456702          | SCBL       | (null) | 0000001123456702        ",
        "PL61109010140000071219812874      | 28 | PL | 🇵🇱 | 61 | 109010140000071219812874      | 109        | 0101   | 0000071219812874        ",
        "PS92PALS000000000400123456702     | 29 | PS | 🇵🇸 | 92 | PALS000000000400123456702     | PALS       | (null) | 000000000400123456702   ",
        "PT50000201231234567890154         | 25 | PT | 🇵🇹 | 50 | 000201231234567890154         | 0002       | 0123   | 12345678901             ",
        "QA58DOHB00001234567890ABCDEFG     | 29 | QA | 🇶🇦 | 58 | DOHB00001234567890ABCDEFG     | DOHB       | (null) | 00001234567890ABCDEFG   ",
        "RO49AAAA1B31007593840000          | 24 | RO | 🇷🇴 | 49 | AAAA1B31007593840000          | AAAA       | (null) | 1B31007593840000        ",
        "RS35260005601001611379            | 22 | RS | 🇷🇸 | 35 | 260005601001611379            | 260        | (null) | 0056010016113           ",
        "RU0304452522540817810538091310419 | 33 | RU | 🇷🇺 | 03 | 04452522540817810538091310419 | 044525225  | 40817  | 810538091310419         ",
        "SA0380000000608010167519          | 24 | SA | 🇸🇦 | 03 | 80000000608010167519          | 80         | (null) | 000000608010167519      ",
        "SC18SSCB11010000000000001497USD   | 31 | SC | 🇸🇨 | 18 | SSCB11010000000000001497USD   | SSCB11     | 01     | 0000000000001497        ",
        "SD2129010501234001                | 18 | SD | 🇸🇩 | 21 | 29010501234001                | 29         | (null) | 010501234001            ",
        "SE4550000000058398257466          | 24 | SE | 🇸🇪 | 45 | 50000000058398257466          | 500        | (null) | 00000058398257466       ",
        "SI56263300012039086               | 19 | SI | 🇸🇮 | 56 | 263300012039086               | 26         | 330    | 00120390                ",
        "SK3112000000198742637541          | 24 | SK | 🇸🇰 | 31 | 12000000198742637541          | 1200       | (null) | 0000198742637541        ",
        "SM86U0322509800000000270100       | 27 | SM | 🇸🇲 | 86 | U0322509800000000270100       | 03225      | 09800  | 000000270100            ",
        "SO211000001001000100141           | 23 | SO | 🇸🇴 | 21 | 1000001001000100141           | 1000       | 001    | 001000100141            ",
        "ST23000100010051845310146         | 25 | ST | 🇸🇹 | 23 | 000100010051845310146         | 0001       | 0001   | 0051845310146           ",
        "SV62CENR00000000000000700025      | 28 | SV | 🇸🇻 | 62 | CENR00000000000000700025      | CENR       | (null) | 00000000000000700025    ",
        "TL380080012345678910157           | 23 | TL | 🇹🇱 | 38 | 0080012345678910157           | 008        | (null) | 00123456789101          ",
        "TN5910006035183598478831          | 24 | TN | 🇹🇳 | 59 | 10006035183598478831          | 10         | 006    | 0351835984788           ",
        "TR330006100519786457841326        | 26 | TR | 🇹🇷 | 33 | 0006100519786457841326        | 00061      | (null) | 0519786457841326        ",
        "UA213223130000026007233566001     | 29 | UA | 🇺🇦 | 21 | 3223130000026007233566001     | 322313     | (null) | 0000026007233566001     ",
        "VA59001123000012345678            | 22 | VA | 🇻🇦 | 59 | 001123000012345678            | 001        | (null) | 123000012345678         ",
        "VG96VPVG0000012345678901          | 24 | VG | 🇻🇬 | 96 | VPVG0000012345678901          | VPVG       | (null) | 0000012345678901        ",
        "XK051212012345678906              | 20 | XK | 🇽🇰 | 05 | 1212012345678906              | 12         | 12     | 0123456789              ",
        "YE15CBYE0001018861234567891234    | 30 | YE | 🇾🇪 | 15 | CBYE0001018861234567891234    | CBYE       | 0001   | 018861234567891234      ",
    })
    void testValidIbanAllCountries(String ibanInput, int expectedIbanLength, String expectedCountryCode, String expectedCountryFlag,
        String expectedCheckDigits, String expectedBban, String expectedBankCode, String expectedBranchCode, String expectedAccountNumber) {

        IbanRegistry registry = IbanRegistry.getByCode(expectedCountryCode);
        assertThat(registry).isNotNull();
        assertThat(registry.getPrimary()).isNull();

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
            .hasAccountNumber(expectedAccountNumber)

            .hasCountryCode(registry.getCountryCode())
            .hasCountryFlag(registry.getCountryFlag())
            .hasCountryName(registry.getCountryName())
            .hasOrganisation(registry.getOrganisation())

            .extracting(Iban::toFormattedString)
            .asString()
            .isNotBlank()
            .startsWith(registry.getCountryCode());

        assertThat(Iban.isValid(ibanInput)).isTrue();
    }

    /**
     * Tests various invalid IBANs for all supported countries, expecting structure or checksum errors.
     *
     * @param entry The {@link IbanRegistry} entry to test.
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

        // same IBAN content
        assertThat(iban1)
            .as("Iban instances with the same content must be equal")
            .isEqualTo(iban1)
            .isEqualTo(iban2)
            .hasSameHashCodeAs(iban2);

        // different IBAN content
        assertThat(iban1)
            .as("Iban instances with different content must not be equal")
            .isNotEqualTo(iban3)
            .doesNotHaveSameHashCodeAs(iban3);

        assertThat(iban1)
            .isNotEqualTo(null)
            .isNotEqualTo(ibanStr1)
            .isNotEqualTo(new Object());
    }

    /**
     * Tests {@link Iban#compareTo(Iban)} to ensure correct alphabetical ordering
     * based on the underlying normalized IBAN string.
     *
     * @param iban1Input First IBAN string.
     * @param iban2Input Second IBAN string.
     * @param expectedSign The expected sign of the comparison result (negative, zero, or positive).
     */
    @ParameterizedTest(name = "[{index}] Compare ''{0}'' with ''{1}'': Expected sign {2}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
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

        if (expectedSign == 0) {
            assertThat(result).as("Expected comparison result to be zero (equal)").isZero();
        } else if (expectedSign < 0) {
            assertThat(result).as("Expected comparison result to be negative (less than)").isNegative();
        } else {
            assertThat(result).as("Expected comparison result to be positive (greater than)").isPositive();
        }

        // Also check the inverse for consistency
        assertThat(iban2.compareTo(iban1)).isEqualTo(-result);
    }

    /**
     * Tests {@link Iban#charAt(int)} for boundary and content checks.
     * @param index The character index to test.
     * @param expectedChar The expected character at that index.
     */
    @ParameterizedTest(name = "[{index}] charAt({0}) must be ''{1}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "0  | D",
        "1  | E",
        "2  | 8",
        "21 | 0"
    })
    void testCharAt(int index, char expectedChar) {
        String ibanStr = "DE89370400440532013000"; // Length 22
        Iban iban = Iban.of(ibanStr);

        assertThat(iban.charAt(index)).isEqualTo(expectedChar);

        // Boundary checks
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
     * @param start The start index (inclusive).
     * @param end The end index (exclusive).
     * @param expectedString The expected sub-sequence string.
     */
    @ParameterizedTest(name = "[{index}] subSequence({0}, {1}) must be ''{2}''")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
        "0 | 2  | DE",                    // Country Code
        "2 | 4  | 89",                    // Check Digits
        "4 | 22 | 370400440532013000",    // BBAN
        "0 | 22 | DE89370400440532013000" // Full IBAN
    })
    void testSubSequence(int start, int end, String expectedString) {
        String ibanStr = "DE89370400440532013000"; // Length 22
        Iban iban = Iban.of(ibanStr);

        CharSequence result = iban.subSequence(start, end);

        assertThat(result)
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

}
