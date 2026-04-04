# Contributing to IBAN Commons

First off, thank you for considering contributing to IBAN Commons! 🎉

We welcome contributions from everyone, whether you're fixing a typo, reporting a bug, or implementing a new feature.

## 🚀 Quick Start

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/iban-commons.git`
3. Create a branch: `git checkout -b feature/your-feature-name`
4. Make your changes and commit them
5. Push to your fork: `git push origin feature/your-feature-name`
6. Open a Pull Request and describe your changes

## 💡 How Can I Contribute?

### Reporting Bugs 🐛

Found a bug? Please open an [issue](https://github.com/SpeedBankingDe/iban-commons/issues) with:
- A clear, descriptive title
- Steps to reproduce the issue
- Expected vs. actual behavior
- Your Java version and OS
- Sample code if possible

### Suggesting Enhancements 💡

Have an idea? We'd love to hear it! Please:
- Check [existing discussions](https://github.com/SpeedBankingDe/iban-commons/discussions) first
- Open a new discussion or issue
- Explain your use case and why this enhancement would be useful

### Submitting Code Changes ✨

We appreciate all contributions! Here's what we look for:

#### Code Quality
- **Follow existing style**: Use the same code style as the rest of the project (import order, 4 spaces-indenting, bracket placements)
- **Write tests**: All new features and bug fixes must have tests
- **Keep it simple**: Prefer clarity over cleverness
- **Document**: Add JavaDoc for public APIs

#### Testing
```bash
# Run all tests
mvn clean verify

# Run specific test
mvn test -Dtest=IbanTest
```

#### Commit Messages
Write clear, concise commit messages.\
Refer to the issue if issue-related.
```
Add support for new country XY

- Implement IBAN structure for country XY
- Add test cases for XY IBANs
- Update IbanRegistry with official data
```

## 🏗️ Development Setup

### Prerequisites
- **JDK 17** or higher required to build the project
- Maven 3.9+
- Git
- Spare Time

### Building
```bash
git clone https://github.com/SpeedBankingDe/iban-commons.git
cd iban-commons
mvn clean install

# Running all tests
mvn test
```

### Code Quality Checks
We use several tools to maintain code quality:
- [**Checkstyle**](https://checkstyle.sourceforge.io/): Code style enforcement
- [**PMD**](https://pmd.github.io/): Static code analysis
- [**PITest**](https://pitest.org/): Mutation testing
- [**Maven Enforcer Plugin**](https://maven.apache.org/enforcer/maven-enforcer-plugin/): Control certain environmental constraints

Run all checks:
```bash
mvn clean verify
```

## 📝 Coding Guidelines

### General Principles
- **Zero dependencies**: Do not add external dependencies without discussion
- **Java 8 compatible**: Code must work with Java 8
- **Immutability**: Prefer immutable objects
- **Thread-safety**: Ensure thread-safe code
- **Performance**: Consider performance impact

### Naming Conventions
- **Classes**: `PascalCase` (e.g., `IbanValidator`)
- **Methods**: `camelCase` (e.g., `validateChecksum()`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_IBAN_LENGTH`)
- **Packages**: lowercase (e.g., `de.speedbanking.iban`)

### Documentation
- **JavaDoc**: Required for all public APIs
- **Inline comments**: Use sparingly, prefer self-documenting code
- **README**: Update if adding user-facing features

## 🧪 Testing Guidelines

### Test Coverage
- Aim for 100% coverage
- Test both _happy paths_ and _edge cases_
- Include tests for _invalid input_

### Test Naming
Use descriptive names that explain what's being tested:
```java
@Test
void shouldThrowExceptionWhenIbanIsTooShort() {
    assertThrows(InvalidIbanException.class, 
        () -> Iban.of("DE123"));
}

@Test
void shouldReturnEmptyOptionalForInvalidIban() {
    Optional<Iban> result = Iban.tryParse("INVALID");
    assertThat(result).isEmpty();
}
```

### AssertJ
We use [AssertJ](https://assertj.github.io/doc/) for fluent assertions:
```java
assertThat(iban.getCountryCode()).isEqualTo("DE");
assertThat(iban.getBankCode()).isNotNull();
assertThat(validIbans).hasSize(5).allMatch(Iban::isValid);
```

## 🔍 Pull Request Process

1. **Update documentation** if needed (README, JavaDoc)
2. **Add/update tests** for your changes
3. **Run all tests**: `mvn clean verify`
4. **Check that builds pass** on GitHub Actions
5. **Request review** from maintainers
6. **Address feedback** promptly and kindly

### PR Checklist
- [ ] Code follows project style
- [ ] Tests added/updated
- [ ] All tests pass
- [ ] Documentation updated
- [ ] No breaking changes (or discussed first)
- [ ] Commit messages are clear

## 🌟 Adding New Countries

To add support for a new IBAN country:

1. **Get official data** from [SWIFT IBAN Registry](https://www.swift.com/standards/data-standards/iban)
2. **Add enum constant** in `IbanRegistry.java`
3. **Add tests** in `IbanRegistryTest.java` and `IbanTest.java`
4. **Update documentation** if needed
5. **Run benchmarks** to ensure no performance regression

## 💬 Communication

- **Be respectful**: We're all here to help
- **Be patient**: Maintainers are volunteers
- **Be constructive**: Focus on solutions, not problems
- **Ask questions**: If something's unclear, just ask!

For questions or discussions:
- 💬 [GitHub Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)
- 🐛 [GitHub Issues](https://github.com/SpeedBankingDe/iban-commons/issues)

## 📜 Code of Conduct

This project follows a simple code of conduct:
- **Be kind** and respectful
- **Welcome** newcomers
- **Focus** on constructive feedback
- **Respect** different viewpoints and experiences

## 🙏 Recognition

All contributors will be recognized in:
- GitHub contributors list
- Release notes (for significant contributions)

Thank you for helping make IBAN Commons better! 🚀

---

**Questions?** Don't hesitate to ask in [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions)!
