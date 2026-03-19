package de.speedbanking.iban;

import de.speedbanking.util.IndexRange;
import de.speedbanking.util.Iso3166Alpha2;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;

/**
 * JUnit test class for {@link IbanRegistry}.
 */
@SuppressWarnings("PMD.LinguisticNaming")
class IbanRegistryTest extends org.assertj.core.api.Assertions {

    @DisplayName("Should return the correct registry entry for a valid code")
    @Test
    void getByCodeShouldReturnCorrectEntry() {
        IbanRegistry de = IbanRegistry.getByCode("DE");

        assertThat(de)
            .as("DE should be found")
            .isNotNull()
            .extracting(IbanRegistry::getCountryName, IbanRegistry::isSepa, IbanRegistry::getPrimary)
            .containsExactly("Germany", true, null);
    }

    @DisplayName("Should return null for an unsupported country code")
    @ParameterizedTest
    @ValueSource(strings = {"XX", "xx"})
    @NullAndEmptySource
    void getByCodeShouldReturnNullForInvalidCode(String code) {
        assertThat(IbanRegistry.getByCode(code))
            .as("'%s' should not exist", code)
            .isNull();
    }

    @DisplayName("Should handle case-sensitive lookup correctly (enum names are upper-case)")
    @Test
    void getByCodeShouldBeCaseSensitive() {
        assertThat(IbanRegistry.getByCode("de"))
            .as("Lookup should be case-sensitive and fail for 'de'")
            .isNull();

        assertThat(IbanRegistry.getByCode("DE"))
            .as("Lookup should succeed for 'DE'")
            .isNotNull();
    }

    @DisplayName("Should return a non-empty list of SEPA countries")
    @Test
    void getSepaCountriesShouldReturnList() {
        java.util.List<IbanRegistry> sepaCountries = IbanRegistry.getSepaCountries();

        assertThat(sepaCountries)
            .as("SEPA country list should not be null or empty")
            .isNotEmpty()
            .allMatch(IbanRegistry::isSepa)
            .contains(IbanRegistry.DE, IbanRegistry.FR, IbanRegistry.IT)
            .hasSizeGreaterThan(35);
    }

    @DisplayName("Should have correct length and derived IBAN pattern string")
    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
            "DE | 22 | 8!n10!n       | (null)", // Deutschland: 18 Zeichen
            "FR | 27 | 5!n5!n11!c2!n | (null)", // Frankreich: 23 Zeichen
            "NO | 15 | 4!n6!n1!n     | (null)", // Norwegen: kürzeste IBAN
            "MT | 31 | 4!a5!n18!c    | (null)", // Malta: längste IBAN
            "AX | 18 | 3!n11!n       | FI",     // Åland Islands: primary Finland
            "GP | 27 | 5!n5!n11!c2!n | FR"      // Guadeloupe: primary France"
    })
    void checkIbanProperties(String code, int expectedLength, String expectedPattern, IbanRegistry expectedPrimary) {
        IbanRegistry entry = IbanRegistry.getByCode(code);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(entry)
            .isNotNull()
            .extracting(IbanRegistry::getIbanLength)
            .as("IBAN length mismatch for %s", code)
            .isEqualTo(expectedLength);

        softly.assertThat(entry.getBbanPatternStr())
            .as("BBAN pattern string mismatch for %s", code)
            .isEqualTo(expectedPattern);

        softly.assertThat(entry.getPrimary()).isEqualTo(expectedPrimary);

        softly.assertAll();
    }

    @DisplayName("Germany (DE) should not have a separate branch code")
    @Test
    void checkIbanDe() {
        SoftAssertions softly = new SoftAssertions();
        IbanRegistry registryDe = IbanRegistry.getByCode("DE");

        softly.assertThat(registryDe)
            .isNotNull()
            .isSameAs(IbanRegistry.DE)
            .extracting(IbanRegistry::getCountryCode, IbanRegistry::getCountryName, IbanRegistry::getCountryFlag, IbanRegistry::isSepa, IbanRegistry::getPrimary)
            .containsExactly("DE", "Germany", "🇩🇪", true, null);

        softly.assertThat(registryDe.getIbanLength()).isEqualTo(22);
        softly.assertThat(registryDe.getBbanLength()).isEqualTo(18);
        softly.assertThat(registryDe.getBbanPatternStr()).isEqualTo("8!n10!n");
        softly.assertThat(registryDe.getIbanExample()).isEqualTo("DE89370400440532013000");

        softly.assertThat(registryDe.getBankCodePatternStr()).isEqualTo("8!n");
        softly.assertThat(registryDe.getBankCodeIndexRange())
            .isNotNull()
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(4, 12);

        softly.assertThat(registryDe.getBranchCodePattern()).isNull();
        softly.assertThat(registryDe.getBranchCodeIndexRange()).isNull();
        softly.assertThat(registryDe.hasBranchCode()).isFalse();

        softly.assertThat(registryDe.getAccountNumberIndexRange())
            .isNotNull()
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(12, 22);

        softly.assertThat(registryDe.getOrganisation()).isEqualTo("Bundesverband deutscher Banken");
        softly.assertThat(registryDe.getDepartment()).isNull();
        softly.assertThat(registryDe.getStreetAddress()).isEqualTo("Burgstraße 28");
        softly.assertThat(registryDe.getCityPostcode()).isEqualTo("10178 Berlin");
        softly.assertThat(registryDe.getDepartmentGenericEmail()).isEqualTo("iban@bdb.de");
        softly.assertThat(registryDe.getDepartmentTel()).isEqualTo("+ 49 3016632301");

        softly.assertThat(registryDe.getCountryValidator())
            .isInstanceOf(CountryValidator.DE.class)
            .isNotNull();

        softly.assertThat(registryDe.getLastUpdate()).isEqualTo(YearMonth.of(2011, 1));
        softly.assertThat(registryDe.getLastUpdateYear()).isEqualTo(2011);
        softly.assertThat(registryDe.getLastUpdateMonth()).isEqualTo(1);

        softly.assertAll();
    }

    @DisplayName("France (FR) should have a branch code")
    @Test
    void checkIbanFr() {
        SoftAssertions softly = new SoftAssertions();
        IbanRegistry registryFr = IbanRegistry.FR;

        softly.assertThat(registryFr)
            .isNotNull()
            .isSameAs(IbanRegistry.getByCode("FR"))
            .extracting(IbanRegistry::getCountryCode, IbanRegistry::getCountryName, IbanRegistry::getCountryFlag, IbanRegistry::isSepa, IbanRegistry::getPrimary)
            .containsExactly("FR", "France", "🇫🇷", true, null);

        softly.assertThat(registryFr.getIbanLength()).isEqualTo(27);
        softly.assertThat(registryFr.getBbanLength()).isEqualTo(23);
        softly.assertThat(registryFr.getBbanPatternStr()).isEqualTo("5!n5!n11!c2!n");
        softly.assertThat(registryFr.getIbanExample()).isEqualTo("FR1420041010050500013M02606");

        softly.assertThat(registryFr.getBankCodePatternStr()).isEqualTo("5!n");
        softly.assertThat(registryFr.getBankCodeIndexRange())
            .isNotNull()
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(4, 9);

        softly.assertThat(registryFr.getBranchCodePattern()).isEqualTo("5!n");
        softly.assertThat(registryFr.getBranchCodeIndexRange())
            .isNotNull()
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(9, 14);
        softly.assertThat(registryFr.hasBranchCode()).isTrue();

        softly.assertThat(registryFr.getAccountNumberIndexRange())
            .isNotNull()
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(14, 25);

        softly.assertThat(registryFr.getOrganisation()).isEqualTo("CFONB");
        softly.assertThat(registryFr.getDepartment()).isNull();
        softly.assertThat(registryFr.getStreetAddress()).isEqualTo("18 rue la Fayette");
        softly.assertThat(registryFr.getCityPostcode()).isEqualTo("75009 Paris");
        softly.assertThat(registryFr.getDepartmentGenericEmail()).isEqualTo("cfonb@cfonb.fr");
        softly.assertThat(registryFr.getDepartmentTel()).isEqualTo("+ 33 148005042");

        softly.assertThat(registryFr.getCountryValidator())
            .isInstanceOf(CountryValidator.FR.class)
            .isNotNull();

        softly.assertThat(registryFr.getLastUpdate()).isEqualTo(YearMonth.of(2016, 9));

        softly.assertAll();
    }

    @DisplayName("Italy (IT) should have an offset Bank ID and Branch ID")
    @Test
    void checkIbanIt() {
        SoftAssertions softly = new SoftAssertions();
        IbanRegistry registryIt = IbanRegistry.IT;

        softly.assertThat(registryIt)
            .as("IT entry should exist and be the static constant")
            .isNotNull()
            .isSameAs(IbanRegistry.getByCode("IT"))
            .extracting(IbanRegistry::getCountryCode, IbanRegistry::getCountryName, IbanRegistry::getCountryFlag, IbanRegistry::isSepa, IbanRegistry::getPrimary)
            .containsExactly("IT", "Italy", "🇮🇹", true, null);

        softly.assertThat(registryIt.getBankCodePatternStr()).isEqualTo("5!n");
        softly.assertThat(registryIt.getBankCodeIndexRange())
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(5, 10);

        softly.assertThat(registryIt.hasBranchCode()).isTrue();
        softly.assertThat(registryIt.getBranchCodePattern()).isEqualTo("5!n");
        softly.assertThat(registryIt.getBranchCodeIndexRange())
            .extracting(IndexRange::getBegin, IndexRange::getEnd)
            .containsExactly(10, 15);

        softly.assertAll();
    }

    @DisplayName("Non-SEPA country should return false for isSepa")
    @Test
    void checkIbanNonSepaCountry() {
        assertThat(IbanRegistry.getByCode("PS"))
            .as("PS (Palestine) entry should exist")
            .isNotNull()
            .extracting(IbanRegistry::getCountryName, IbanRegistry::isSepa, IbanRegistry::getPrimary)
            .containsExactly("Palestine", false, null);
    }

    @DisplayName("toString() should contain essential data for DE")
    @Test
    void toStringShouldBeDetailedDE() {
        String expected = "IbanRegistry[DE (Germany), "
            + "SEPA country: Yes, "
            + "IBAN len: 22, "
            + "BBAN pattern: 8!n10!n, "
            + "Bank Code: IndexRange[4-11 (8)], "
            + "Account No: IndexRange[12-21 (10)], "
            + "IBAN Example: DE89370400440532013000, "
            + "Organization: Bundesverband deutscher Banken, "
            + "Last Update: 2011-01]";
        assertThat(IbanRegistry.DE).hasToString(expected);
    }

    @DisplayName("All entries must exist in Iso3166Alpha2")
    @ParameterizedTest
    @EnumSource(IbanRegistry.class)
    void allEntriesMustExistInIso3166Alpha2(IbanRegistry entry) {
        assertThat(Iso3166Alpha2.fromCode(entry.getCountryCode())).isNotNull();
    }

    @DisplayName("All entries must have null or valid lastUpdate date")
    @ParameterizedTest
    @EnumSource(IbanRegistry.class)
    void allEntriesMustHaveLastUpdate(IbanRegistry entry) {
        YearMonth firstYearMonth = YearMonth.of(2000, 1);
        assertThat(entry.getLastUpdate())
            .as("lastUpdate must be null or a valid date after %s for %s", firstYearMonth, entry.getCountryCode())
            .satisfiesAnyOf(
                lu -> assertThat(lu).isNull(),
                lu -> assertThat(lu)
                          .isAfter(firstYearMonth)
                          .isBeforeOrEqualTo(YearMonth.now())
            );
    }

    @DisplayName("All entries must have a valid IBAN length (4 to 34)")
    @ParameterizedTest
    @EnumSource(IbanRegistry.class)
    void allEntriesMustHaveValidIbanLength(IbanRegistry entry) {
        assertThat(entry.getIbanLength())
            .as("IBAN length for %s", entry.getCountryCode())
            .isBetween(IbanRegistry.MIN_IBAN_LENGTH, IbanRegistry.MAX_IBAN_LENGTH);
    }

    @DisplayName("All entries must have a valid example IBAN")
    @ParameterizedTest
    @EnumSource(IbanRegistry.class)
    void allEntriesMustHaveValidIbanExample(IbanRegistry entry) {

        assertThat(entry.getIbanExample())
            .as("Example IBAN missing for %s", entry.getCountryCode())
            .isNotNull();

        assertThatCode(() -> Iban.of(entry.getIbanExample())).doesNotThrowAnyException();
    }

    @DisplayName("Should verify ContactData properties and immutability")
    @Test
    void shouldVerifyContactData() {
        IbanRegistry.ContactData contact = IbanRegistry.ContactData.of("Swift", "Standards", "Street", "City", "Email", "Tel");

        assertThat(contact)
            .isNotNull()
            .returns("Swift",     IbanRegistry.ContactData::getOrganisation)
            .returns("Standards", IbanRegistry.ContactData::getDepartment)
            .returns("Street",    IbanRegistry.ContactData::getStreetAddress)
            .returns("City",      IbanRegistry.ContactData::getCityPostcode)
            .returns("Email",     IbanRegistry.ContactData::getDepartmentGenericEmail)
            .returns("Tel",       IbanRegistry.ContactData::getDepartmentTel);
    }

    @DisplayName("Should satisfy equals and hashCode for ContactData")
    @Test
    void shouldSatisfyEqualsHashCodeForContactData() {
        IbanRegistry.ContactData contact1 = IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Tel");
        IbanRegistry.ContactData contact2 = IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Tel");
        IbanRegistry.ContactData contactDiff = IbanRegistry.ContactData.of("Other", "Dept", "Street", "City", "Mail", "Tel");

        assertThat(contact1)
            .isEqualTo(contact1) // same instance
            .isEqualTo(contact2) // same values
            .hasSameHashCodeAs(contact2)
            .isNotEqualTo(contactDiff)
            .isNotEqualTo(null)
            .isNotEqualTo(new Object());
    }

    @DisplayName("Should cover all equality branches for ContactData")
    @Test
    void contactDataEqualsBranchCoverage() {
        IbanRegistry.ContactData base = IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Tel");

        // test for each field being different to trigger 'false' result for each && branch
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(base).isNotEqualTo(IbanRegistry.ContactData.of("Diff", "Dept", "Street", "City", "Mail", "Tel"));
        softly.assertThat(base).isNotEqualTo(IbanRegistry.ContactData.of("Org", "Diff", "Street", "City", "Mail", "Tel"));
        softly.assertThat(base).isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Diff", "City", "Mail", "Tel"));
        softly.assertThat(base).isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Street", "Diff", "Mail", "Tel"));
        softly.assertThat(base).isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Diff", "Tel"));
        softly.assertThat(base).isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Diff"));

        // test with null values to cover Objects.equals null-handling
        IbanRegistry.ContactData withNulls = IbanRegistry.ContactData.of(null, null, null, null, null, null);
        softly.assertThat(withNulls).isNotEqualTo(base);
        softly.assertThat(base).isNotEqualTo(withNulls);

        softly.assertAll();
    }

    @DisplayName("Should return correct toString for ContactData")
    @Test
    void shouldReturnCorrectToStringForContactData() {
        IbanRegistry.ContactData contact = IbanRegistry.ContactData.of("Org", "Dept", null, null, null, null);

        // checking the pattern: getClass().getSimpleName() + [fields]
        assertThat(contact).hasToString(
            "ContactData[organisation=Org, department=Dept, streetAddress=null, "
            + "cityPostcode=null, departmentGenericEmail=null, departmentTel=null]");
    }

    @DisplayName("Validate IBAN length is positive")
    @ParameterizedTest(name = "Should throw exception for invalid IBAN length: {0}")
    @ValueSource(ints = {-1, 0})
    void shouldThrowExceptionWhenIbanLengthIsInvalid(int invalidLength) {
        IbanRegistry.StructureData.Builder builder = IbanRegistry.StructureData.builder()
            .withIbanLength(invalidLength)
            .withBbanPattern("4!n")
            .withAccountNumber(IndexRange.of(4, 8));

        // testing the validation logic inside the constructor (called by build())
        assertThatIllegalStateException()
            .isThrownBy(builder::build)
            .withMessage("IBAN length must be set and positive");
    }

    @DisplayName("Should throw exception when required BBAN pattern is missing")
    @Test
    void shouldThrowExceptionWhenBbanPatternIsMissing() {
        IbanRegistry.StructureData.Builder builder = IbanRegistry.StructureData.builder()
            .withIbanLength(20)
            .withAccountNumber(IndexRange.of(4, 20));

        assertThatNullPointerException()
            .isThrownBy(builder::build);
    }

    @DisplayName("Validate IBAN length limits (15-34)")
    @ParameterizedTest(name = "Should throw exception for ISO 13616 violation: {0}")
    @ValueSource(ints = {14, 35})
    void shouldThrowExceptionWhenIbanLengthViolatesIsoLimits(int invalidLength) {
        IbanRegistry.StructureData.Builder builder = IbanRegistry.StructureData.builder()
            .withIbanLength(invalidLength)
            .withBbanPattern("4!n")
            .withAccountNumber(IndexRange.of(4, invalidLength));

        assertThatIllegalStateException()
            .isThrownBy(builder::build)
            .withMessage("IBAN length must be between 15 and 34");
    }

}
