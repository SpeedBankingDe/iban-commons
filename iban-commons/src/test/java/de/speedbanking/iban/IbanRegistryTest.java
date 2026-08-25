package de.speedbanking.iban;

import static de.speedbanking.iban.IbanRegistry.MAX_IBAN_LENGTH;
import static de.speedbanking.iban.IbanRegistry.MIN_IBAN_BASE_LENGTH;
import static de.speedbanking.iban.IbanRegistry.MIN_IBAN_LENGTH;
import static de.speedbanking.iban.junit.jupiter.api.IbanAssertions.assertThatIbanString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import de.speedbanking.iban.IbanRegistry.MetaData;
import de.speedbanking.util.Country;
import de.speedbanking.util.Currency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.time.ZoneId;

/**
 * JUnit test class for {@link IbanRegistry}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class IbanRegistryTest {

    @DisplayName("Should return the correct registry entry for a valid code")
    @Test
    void getByCode_shouldReturnCorrectEntry() {
        IbanRegistry de = IbanRegistry.getByCode("DE");

        assertThat(de)
            .as("DE should be found")
            .isNotNull()
            .extracting(IbanRegistry::getCountryName, IbanRegistry::isSepa, IbanRegistry::isNotSepa, IbanRegistry::getBaseCountry)
            .containsExactly("Germany", true, false, IbanRegistry.DE);
    }

    @DisplayName("Should return null for an unsupported country code")
    @ParameterizedTest(name = "[{index}]: {0}")
    @ValueSource(strings = {"XX", "xx"})
    @NullAndEmptySource
    void getByCode_shouldReturnNullForInvalidCode(String code) {
        assertThat(IbanRegistry.getByCode(code))
            .as("'%s' should not exist", code)
            .isNull();
    }

    @DisplayName("Should handle case-sensitive lookup correctly (enum names are upper-case)")
    @Test
    void getByCode_shouldBeCaseSensitive() {
        assertThat(IbanRegistry.getByCode("de"))
            .as("Lookup should be case-sensitive and fail for 'de'")
            .isNull();

        assertThat(IbanRegistry.getByCode("DE"))
            .as("Lookup should succeed for 'DE'")
            .isNotNull();
    }

    @DisplayName("Should return a non-empty list of SEPA countries")
    @Test
    void getSepaCountries_shouldReturnList() {
        java.util.List<IbanRegistry> sepaCountries = IbanRegistry.getSepaCountries();

        assertThat(sepaCountries)
            .as("SEPA country list should not be null or empty")
            .isNotEmpty()
            .allMatch(IbanRegistry::isSepa)
            .noneMatch(IbanRegistry::isNotSepa)
            .contains(IbanRegistry.DE, IbanRegistry.FR, IbanRegistry.IT)
            .hasSizeGreaterThan(35);
    }

    @DisplayName("Should have correct length and derived IBAN pattern string")
    @ParameterizedTest(name = "[{index}]: {0}")
    @CsvSource(delimiter = '|', nullValues = "(null)", value = {
            "DE | 22 | 8!n10!n       | DE", // Germany: 18 chars
            "FR | 27 | 5!n5!n11!c2!n | FR", // France: 23 chars
            "NO | 15 | 4!n6!n1!n     | NO", // Norway: shortest IBAN
            "MT | 31 | 4!a5!n18!c    | MT", // Malta: longest IBAN
            "AX | 18 | 3!n11!n       | FI", // Åland Islands: base country Finland
            "GP | 27 | 5!n5!n11!c2!n | FR"  // Guadeloupe: base country France"
    })
    void checkIbanProperties_shouldMatchExpectedValues(String code, int expectedLength, String expectedPattern, IbanRegistry expectedBaseCountry) {
        IbanRegistry entry = IbanRegistry.getByCode(code);

        assertThat(entry)
            .isNotNull()
            .extracting(IbanRegistry::getIbanLength)
            .as("IBAN length mismatch for %s", code)
            .isEqualTo(expectedLength);

        assertThat(entry.getBbanPattern())
            .as("BBAN pattern string mismatch for %s", code)
            .isEqualTo(expectedPattern);

        assertThat(entry.getBaseCountry()).isEqualTo(expectedBaseCountry);
    }

    @DisplayName("Germany (DE) should not have a separate branch code")
    @Test
    void checkIbanDe_shouldHaveCorrectStructure() {
        IbanRegistry registryDe = IbanRegistry.getByCode("DE");

        assertThat(registryDe)
            .isNotNull()
            .isSameAs(IbanRegistry.DE)
            .isSameAs(IbanRegistry.getByCode('D', 'E'))
            .extracting(IbanRegistry::getCountryCode, IbanRegistry::getCountryName, IbanRegistry::getCountryFlag,
                IbanRegistry::isSepa, IbanRegistry::getBaseCountry,
                IbanRegistry::getCurrency, IbanRegistry::getCurrencyCode)
            .containsExactly("DE", "Germany", "🇩🇪", true, IbanRegistry.DE, Currency.EUR, "EUR");

        assertThat(registryDe.getIbanLength()).isEqualTo(22);
        assertThat(registryDe.getBbanLength()).isEqualTo(18);
        assertThat(registryDe.getBbanPattern()).isEqualTo("8!n10!n");
        assertThat(registryDe.getIbanExample()).isEqualTo("DE89370400440532013000");

        assertThat(registryDe.getBankCodePattern()).isEqualTo("8!n");
        assertThat(registryDe.getBankCodeComponent())
            .isNotNull()
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(4, 12);

        assertThat(registryDe.getBranchCodePattern()).isNull();
        assertThat(registryDe.getBranchCodeComponent()).isNull();
        assertThat(registryDe.hasBranchCode()).isFalse();

        assertThat(registryDe.getAccountNumberLength())
            .isEqualTo(10)
            .isEqualTo(registryDe.getAccountNumberComponent().getLength());

        assertThat(registryDe.getAccountNumberPattern()).isEqualTo("10!n");
        assertThat(registryDe.getAccountNumberComponent())
            .isNotNull()
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(12, 22);

        assertThat(registryDe.getOrganisation()).isEqualTo("Bundesverband deutscher Banken");
        assertThat(registryDe.getDepartment()).isNull();
        assertThat(registryDe.getStreetAddress()).isEqualTo("Burgstraße 28");
        assertThat(registryDe.getCityPostcode()).isEqualTo("10178 Berlin");
        assertThat(registryDe.getDepartmentGenericEmail()).isEqualTo("iban@bdb.de");
        assertThat(registryDe.getDepartmentTel()).isEqualTo("+ 49 3016632301");

        assertThat(registryDe.getLastUpdate()).isEqualTo(YearMonth.of(2011, 1));
        assertThat(registryDe.getLastUpdateYear()).isEqualTo(2011);
        assertThat(registryDe.getLastUpdateMonth()).isEqualTo(1);
    }

    @DisplayName("France (FR) should have a branch code")
    @Test
    void checkIbanFr_shouldHaveCorrectStructure() {
        IbanRegistry registryFr = IbanRegistry.FR;

        assertThat(registryFr)
            .isNotNull()
            .isSameAs(IbanRegistry.getByCode("FR"))
            .isSameAs(IbanRegistry.getByCode('F', 'R'))
            .extracting(IbanRegistry::getCountryCode, IbanRegistry::getCountryName, IbanRegistry::getCountryFlag,
                IbanRegistry::isSepa, IbanRegistry::getBaseCountry,
                IbanRegistry::getCurrency, IbanRegistry::getCurrencyCode)
            .containsExactly("FR", "France", "🇫🇷", true, IbanRegistry.FR, Currency.EUR, "EUR");

        assertThat(registryFr.getIbanLength()).isEqualTo(27);
        assertThat(registryFr.getBbanLength()).isEqualTo(23);
        assertThat(registryFr.getBbanPattern()).isEqualTo("5!n5!n11!c2!n");
        assertThat(registryFr.getIbanExample()).isEqualTo("FR1420041010050500013M02606");

        assertThat(registryFr.getBankCodePattern()).isEqualTo("5!n");
        assertThat(registryFr.getBankCodeComponent())
            .isNotNull()
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(4, 9);

        assertThat(registryFr.getBranchCodePattern()).isEqualTo("5!n");
        assertThat(registryFr.getBranchCodeComponent())
            .isNotNull()
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(9, 14);
        assertThat(registryFr.hasBranchCode()).isTrue();

        assertThat(registryFr.getAccountNumberLength())
            .isEqualTo(11)
            .isEqualTo(registryFr.getAccountNumberComponent().getLength());

        assertThat(registryFr.getAccountNumberPattern()).isEqualTo("11!c");
        assertThat(registryFr.getAccountNumberComponent())
            .isNotNull()
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(14, 25);

        assertThat(registryFr.getOrganisation()).isEqualTo("CFONB");
        assertThat(registryFr.getDepartment()).isNull();
        assertThat(registryFr.getStreetAddress()).isEqualTo("18 rue la Fayette");
        assertThat(registryFr.getCityPostcode()).isEqualTo("75009 Paris");
        assertThat(registryFr.getDepartmentGenericEmail()).isEqualTo("cfonb@cfonb.fr");
        assertThat(registryFr.getDepartmentTel()).isEqualTo("+ 33 148005042");

        assertThat(registryFr.getLastUpdate()).isEqualTo(YearMonth.of(2016, 9));
    }

    @DisplayName("Italy (IT) should have correct structure")
    @Test
    void checkIbanIt_shouldHaveCorrectStructure() {
        IbanRegistry registryIt = IbanRegistry.IT;

        assertThat(registryIt)
            .as("IT entry should exist and be the static constant")
            .isNotNull()
            .isSameAs(IbanRegistry.getByCode("IT"))
            .isSameAs(IbanRegistry.getByCode('I', 'T'))
            .extracting(IbanRegistry::getCountryCode, IbanRegistry::getCountryName, IbanRegistry::getCountryFlag,
                IbanRegistry::isSepa, IbanRegistry::getBaseCountry,
                IbanRegistry::getCurrency, IbanRegistry::getCurrencyCode)
            .containsExactly("IT", "Italy", "🇮🇹", true, IbanRegistry.IT, Currency.EUR, "EUR");

        assertThat(registryIt.getBankCodePattern()).isEqualTo("5!n");
        assertThat(registryIt.getBankCodeComponent())
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(5, 10);

        assertThat(registryIt.hasBranchCode()).isTrue();
        assertThat(registryIt.getBranchCodePattern()).isEqualTo("5!n");
        assertThat(registryIt.getBranchCodeComponent())
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(10, 15);
    }

    @DisplayName("Non-SEPA country should return false for isSepa")
    @Test
    void checkIbanNonSepaCountry_shouldReturnFalse() {
        assertThat(IbanRegistry.getByCode("PS"))
            .as("PS (Palestine) entry should exist")
            .isNotNull()
            .extracting(IbanRegistry::getCountryName, IbanRegistry::isSepa, IbanRegistry::isNotSepa, IbanRegistry::getBaseCountry)
            .containsExactly("Palestine, State of", false, true, IbanRegistry.PS);
    }

    @DisplayName("toString() should contain essential data for DE")
    @Test
    void toString_shouldBeDetailedForDE() {
        String expected = "IbanRegistry[DE (Germany), "
            + "SEPA country: Yes, "
            + "IBAN len: 22, "
            + "BBAN pattern: 8!n10!n, "
            + "Bank Code: 4-11 (8), "
            + "Account Number: 12-21 (10), "
            + "IBAN Example: DE89370400440532013000, "
            + "Organization: Bundesverband deutscher Banken, "
            + "Last Update: 2011-01]";
        assertThat(IbanRegistry.DE).hasToString(expected);
    }

    @DisplayName("getBaseEntryByCode should return the entry only for base countries, null for derived or unknown codes")
    @Test
    void getBaseEntryByCode_shouldReturnEntryOnlyForBaseCountries() {
        assertThat(IbanRegistry.getBaseEntryByCode('F', 'R'))
            .as("FR is a base country")
            .isSameAs(IbanRegistry.FR);

        assertThat(IbanRegistry.getBaseEntryByCode('G', 'F'))
            .as("GF is derived from FR, not a base country itself")
            .isNull();

        assertThat(IbanRegistry.getBaseEntryByCode('X', 'X'))
            .as("XX is not a known country code at all")
            .isNull();
    }

    @DisplayName("isBaseCountry/isDerivedCountry should reflect the base/derived hierarchy")
    @Test
    void isBaseCountry_and_isDerivedCountry_shouldReflectHierarchy() {
        assertThat(IbanRegistry.FR)
            .returns(true, IbanRegistry::isBaseCountry)
            .returns(false, IbanRegistry::isDerivedCountry);

        assertThat(IbanRegistry.GF)
            .as("Guadeloupe derives its data from France")
            .returns(false, IbanRegistry::isBaseCountry)
            .returns(true, IbanRegistry::isDerivedCountry);
    }

    @DisplayName("getDerivedCountries should list derived entries for a base country, and be empty otherwise")
    @Test
    void getDerivedCountries_shouldListDerivedEntriesForBaseCountry() {
        assertThat(IbanRegistry.FR.getDerivedCountries())
            .as("France has several overseas territories sharing its IBAN structure")
            .isNotEmpty()
            .allMatch(IbanRegistry::isDerivedCountry)
            .contains(IbanRegistry.GF, IbanRegistry.GP, IbanRegistry.MQ, IbanRegistry.RE);

        assertThat(IbanRegistry.GF.getDerivedCountries())
            .as("a derived country is not itself a base country, so it has no derived countries of its own")
            .isEmpty();
    }

    @DisplayName("getIbanRegex should match a valid IBAN and reject an invalid one")
    @Test
    void getIbanRegex_shouldMatchValidIbanAndRejectInvalid() {
        assertThat(IbanRegistry.DE.getIbanRegex().matcher("DE89370400440532013000").matches())
            .as("regex should match DE's own example IBAN")
            .isTrue();

        assertThat(IbanRegistry.DE.getIbanRegex().matcher("FR1420041010050500013M02606").matches())
            .as("regex should not match an IBAN of a different country")
            .isFalse();
    }

    @DisplayName("getBranchCodeLength should return 0 when absent, and the actual length when present")
    @Test
    void getBranchCodeLength_shouldReturnZeroWhenAbsent() {
        assertThat(IbanRegistry.DE.getBranchCodeLength()).isZero();
        assertThat(IbanRegistry.FR.getBranchCodeLength()).isEqualTo(5);
    }

    @DisplayName("National Check Digit should be present for AL and absent for DE")
    @Test
    void nationalCheckDigit_shouldBePresentForAlAndAbsentForDe() {
        assertThat(IbanRegistry.AL.hasNationalCheckDigit()).isTrue();
        assertThat(IbanRegistry.AL.getNationalCheckDigitPattern()).isEqualTo("1!n");
        assertThat(IbanRegistry.AL.getNationalCheckDigitComponent())
            .isNotNull()
            .extracting(IbanComponent::getBeginIndex, IbanComponent::getEndIndex)
            .containsExactly(11, 12);

        assertThat(IbanRegistry.DE.hasNationalCheckDigit()).isFalse();
        assertThat(IbanRegistry.DE.getNationalCheckDigitPattern()).isNull();
        assertThat(IbanRegistry.DE.getNationalCheckDigitComponent()).isNull();
    }

    @DisplayName("Package-private internals (StructureData, MetaData, ContactData, builder factory) should be accessible and consistent")
    @Test
    void internalAccessors_shouldReturnConsistentValues() {
        IbanRegistry de = IbanRegistry.DE;

        assertThat(de.getStructureData()).isNotNull();

        assertThat(de.getMetaData())
            .isNotNull()
            .returns(de.getIbanExample(), MetaData::getIbanExample)
            .returns(de.getLastUpdate(), MetaData::getLastUpdate)
            .returns(de.isSepa(), MetaData::isSepa);

        assertThat(de.getContactData())
            .isNotNull()
            .returns(de.getOrganisation(), IbanRegistry.ContactData::getOrganisation);

        assertThat(de.getBuilderFactory())
            .as("builder factory should produce a builder for this same registry entry")
            .isNotNull();
        assertThat(de.getBuilderFactory().apply(de)).isNotNull();
    }

    @DisplayName("All entries must exist in Country Enum")
    @ParameterizedTest(name = "[{index}]: {0}")
    @EnumSource(IbanRegistry.class)
    void allEntries_mustExistInCountryEnum(IbanRegistry entry) {
        assertThat(Country.fromCode(entry.getCountryCode())).isNotNull();
    }

    @DisplayName("All entries must have null or valid lastUpdate date")
    @ParameterizedTest(name = "[{index}]: {0}")
    @EnumSource(IbanRegistry.class)
    void allEntries_mustHaveValidLastUpdate(IbanRegistry entry) {
        if (entry.getLastUpdate() == null) {
            assertThat(entry.getLastUpdateMonth()).isZero();
            assertThat(entry.getLastUpdateYear()).isZero();
        } else {
            assertThat(entry.getLastUpdate())
                .as("lastUpdate date for country %s", entry.getCountryCode())
                .isAfter(YearMonth.of(1997, 1))
                .isBeforeOrEqualTo(YearMonth.now(ZoneId.systemDefault()));
        }
    }

    @DisplayName("All entries must have a valid IBAN length (4 to 34)")
    @ParameterizedTest(name = "[{index}]: {0}")
    @EnumSource(IbanRegistry.class)
    void allEntries_mustHaveValidIbanLength(IbanRegistry entry) {
        assertThat(entry.getIbanLength())
            .as("IBAN length for %s", entry.getCountryCode())
            .isBetween(MIN_IBAN_LENGTH, MAX_IBAN_LENGTH);
    }

    @DisplayName("All entries must have a valid example IBAN")
    @ParameterizedTest(name = "[{index}]: {0}")
    @EnumSource(IbanRegistry.class)
    void allEntries_mustHaveValidIbanExample(IbanRegistry entry) {
        String ibanExample = entry.getIbanExample();
        assertThatIbanString(ibanExample)
            .as("Example IBAN missing for %s", entry.getCountryCode())
            .isValid();

        assertThatCode(() -> Iban.of(ibanExample)).doesNotThrowAnyException();
    }

    @DisplayName("Should verify ContactData properties and immutability")
    @Test
    void contactData_shouldBeInitializedCorrectly() {
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
    @SuppressWarnings("SelfAssertion")
    void contactData_shouldSatisfyEqualsHashCode() {
        IbanRegistry.ContactData contact1 = IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Tel");
        IbanRegistry.ContactData contact2 = IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Tel");
        IbanRegistry.ContactData contactDiff = IbanRegistry.ContactData.of("Other", "Dept", "Street", "City", "Mail", "Tel");

        assertThat(contact1)
            .isEqualTo(contact1) // same instance
            .isEqualTo(contact2) // same values
            .hasSameHashCodeAs(contact2)
            .isNotEqualTo(contactDiff)
            .isNotNull()
            .isNotEqualTo(new Object());
    }

    @DisplayName("Should cover all equality branches for ContactData")
    @Test
    void contactData_shouldCoverEqualsBranchCoverage() {
        IbanRegistry.ContactData base = IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Tel");

        // test for each field being different to trigger 'false' result for each && branch
        assertThat(base).isNotEqualTo(IbanRegistry.ContactData.of("Diff", "Dept", "Street", "City", "Mail", "Tel"))
                        .isNotEqualTo(IbanRegistry.ContactData.of("Org", "Diff", "Street", "City", "Mail", "Tel"))
                        .isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Diff", "City", "Mail", "Tel"))
                        .isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Street", "Diff", "Mail", "Tel"))
                        .isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Diff", "Tel"))
                        .isNotEqualTo(IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Diff"));

        // test with null values to cover Objects.equals null-handling
        IbanRegistry.ContactData withNulls = IbanRegistry.ContactData.of(null, null, null, null, null, null);
        assertThat(withNulls).isNotEqualTo(base);
        assertThat(base).isNotEqualTo(withNulls);
    }

    @DisplayName("Should return correct toString for ContactData")
    @Test
    void contactData_shouldReturnCorrectToString() {
        IbanRegistry.ContactData contact = IbanRegistry.ContactData.of("Org", "Dept", null, null, null, null);

        // checking the pattern: getClass().getSimpleName() + [fields]
        assertThat(contact).hasToString(
            "ContactData[organisation=Org, department=Dept, streetAddress=null, "
            + "cityPostcode=null, departmentGenericEmail=null, departmentTel=null]");
    }

    @DisplayName("Should correctly evaluate isEmpty for ContactData")
    @Test
    void contactData_isEmpty_shouldReturnExpectedResult() {
        // null reference
        assertThat(IbanRegistry.ContactData.isEmpty(null)).isTrue();

        // completely empty or whitespace-only instances
        IbanRegistry.ContactData allNull = IbanRegistry.ContactData.of(null, null, null, null, null, null);

        assertThat(IbanRegistry.ContactData.isEmpty(allNull)).isTrue();

        // fully populated instance
        IbanRegistry.ContactData fullyPopulated = IbanRegistry.ContactData.of("Org", "Dept", "Street", "City", "Mail", "Tel");
        assertThat(IbanRegistry.ContactData.isEmpty(fullyPopulated)).isFalse();

        // partially populated instances (boundary testing each field)
        assertThat(IbanRegistry.ContactData.isEmpty(IbanRegistry.ContactData.of("Org", null, null, null, null, null))).isFalse();
        assertThat(IbanRegistry.ContactData.isEmpty(IbanRegistry.ContactData.of(null, "Dept", null, null, null, null))).isFalse();
        assertThat(IbanRegistry.ContactData.isEmpty(IbanRegistry.ContactData.of(null, null, "Street", null, null, null))).isFalse();
        assertThat(IbanRegistry.ContactData.isEmpty(IbanRegistry.ContactData.of(null, null, null, "City", null, null))).isFalse();
        assertThat(IbanRegistry.ContactData.isEmpty(IbanRegistry.ContactData.of(null, null, null, null, "Mail", null))).isFalse();
        assertThat(IbanRegistry.ContactData.isEmpty(IbanRegistry.ContactData.of(null, null, null, null, null, "Tel"))).isFalse();
    }

    @DisplayName("Should cover all getters of MetaData")
    @Test
    void metaData_getters_shouldReturnConfiguredValues() {
        YearMonth now = YearMonth.now(ZoneId.systemDefault());
        MetaData metaData = MetaData.of(true, "DE89370400440532013000", now);

        assertThat(metaData.isSepa()).isTrue();
        assertThat(metaData.getIbanExample()).isEqualTo("DE89370400440532013000");
        assertThat(metaData.getLastUpdate()).isEqualTo(now);
    }

    @DisplayName("Should throw exception when required BBAN pattern is missing")
    @Test
    void build_shouldThrowExceptionWhenBbanPatternIsMissing() {
        IbanRegistry.StructureData.Builder builder = IbanRegistry.StructureData.builder()
            .withAccountNumber("16!n", 4);

        assertThatIllegalStateException()
            .isThrownBy(builder::build)
            .withMessage("BBAN must be set");
    }

    @DisplayName("Should throw exception when required bank code is missing")
    @Test
    void build_shouldThrowExceptionWhenBankCodeIsMissing() {
        IbanRegistry.StructureData.Builder builder = IbanRegistry.StructureData.builder()
            .withBbanPattern("16!n")
            .withAccountNumber("16!n", 4);

        assertThatIllegalStateException()
            .isThrownBy(builder::build)
            .withMessage("Bank Code must be set");
    }

    @DisplayName("Should throw exception when required account number is missing")
    @Test
    void build_shouldThrowExceptionWhenAccountNumberIsMissing() {
        IbanRegistry.StructureData.Builder builder = IbanRegistry.StructureData.builder()
            .withBbanPattern("4!n")
            .withBankCode("4!n", 4);

        assertThatIllegalStateException()
            .isThrownBy(builder::build)
            .withMessage("Account Number must be set");
    }

    @DisplayName("Validate IBAN length limits (15-34)")
    @ParameterizedTest(name = "[{index}]: {0}")
    @ValueSource(ints = {14, 34})
    void build_shouldThrowExceptionWhenBbanpatternLengthViolatesIsoLimits(int invalidIbanLength) {
        int invalidBbanLength = invalidIbanLength - MIN_IBAN_BASE_LENGTH;
        IbanRegistry.StructureData.Builder builder = IbanRegistry.StructureData.builder()
            .withBbanPattern(invalidBbanLength + "!n")
            .withBankCode("4!n", 4)
            .withAccountNumber("16!n", 8);

        assertThatIllegalStateException()
            .isThrownBy(builder::build)
            .withMessage("IBAN length must be between %d and %d, but was %d",
                MIN_IBAN_LENGTH, MAX_IBAN_LENGTH, invalidIbanLength);
    }

}
