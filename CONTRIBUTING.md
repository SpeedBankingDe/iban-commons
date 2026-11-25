# Contributing to IBAN Commons

Thank you for considering a contribution to IBAN Commons. Fixing a typo, reporting a bug, sharing an idea or sending a pull request all help, and every one of them is welcome.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/rocket-dark.svg"><img src=".github/icons/rocket.svg" height="20" alt=""></picture> Quick Start

1. Fork the repository and clone your fork: `git clone https://github.com/YOUR_USERNAME/iban-commons.git`
2. Create a branch: `git checkout -b feature/your-feature-name`
3. Make your changes and run `mvn`
4. Push the branch to your fork and open a pull request

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/bug-dark.svg"><img src=".github/icons/bug.svg" height="20" alt=""></picture> Reporting Bugs

Please open an [issue](https://github.com/SpeedBankingDe/iban-commons/issues/new/choose) using the bug report template. Helpful are the library version, your Java version and OS, the input that misbehaves (an IBAN or BIC, anonymized if it is real), and what you expected instead. A failing test or a short code snippet makes a fix much faster.

Security issues should not go into a public issue, see [SECURITY.md](SECURITY.md).

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/bulb-dark.svg"><img src=".github/icons/bulb.svg" height="20" alt=""></picture> Suggesting Enhancements

- Ensure you are on the latest version
- Check existing [issues](https://github.com/SpeedBankingDe/iban-commons/issues) and [discussions](https://github.com/SpeedBankingDe/iban-commons/discussions) first
- Open a new issue or discussion
- Explain your use case and why this fix or enhancement is needed

For larger changes, please discuss the idea before writing the code, so your work does not end up going in a different direction than the project.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/tool-dark.svg"><img src=".github/icons/tool.svg" height="20" alt=""></picture> Building

You need JDK 21 or newer to build. The library itself targets Java 8, so the compiler rejects APIs that are not available there. Maven 3.9+ is required, or use the included wrapper (`./mvnw`).

```bash
git clone https://github.com/SpeedBankingDe/iban-commons.git
cd iban-commons
mvn

# a single module and the modules it depends on
mvn -pl iban-commons-bankdata -am

# run the tests on Java 8, as CI does (needs a JDK 8 in ~/.m2/toolchains.xml)
mvn -Dtest.jdk=8
```

`mvn` runs the default goal `clean verify`. It compiles with [Error Prone](https://errorprone.info/), runs [Checkstyle](https://checkstyle.sourceforge.io/), [PMD](https://pmd.github.io/), the tests and the [Maven Enforcer](https://maven.apache.org/enforcer/maven-enforcer-plugin/) rules, and fails on any violation. There is no separate lint step. New files need the Apache license header, which `mvn license:format` adds. Mutation tests with [PITest](https://pitest.org/) are optional: `mvn test-compile org.pitest:pitest-maven:mutationCoverage`.

CI runs the tests on Java 8, 21 and 25, on Ubuntu and Windows.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/code-dark.svg"><img src=".github/icons/code.svg" height="20" alt=""></picture> Coding Guidelines

The best guide is the surrounding code, and the `.editorconfig` takes care of formatting in most IDEs. Beyond what Checkstyle and PMD enforce, a few things are worth knowing:

- The core module has no runtime dependencies, and it should stay that way. Please discuss any new dependency first, also in the optional modules.
- Code must run on Java 8. The core module must also run on Android (API 24+), which the build checks.
- Validation is on the hot path, so think about allocations and performance. Model classes are immutable and thread-safe.
- Every public class and method needs Javadoc. New public classes get an `@since` tag with the upcoming version.
- No `final` on local variables or method parameters. Member declarations are aligned in columns, local ones are not.
- Exception messages use `String.format` with placeholders instead of string concatenation.
- Javadoc, Markdown and commit messages use plain ASCII punctuation (no typographic dashes or quotes).

[AGENTS.md](AGENTS.md) describes these and further conventions in more detail.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/flask-dark.svg"><img src=".github/icons/flask.svg" height="20" alt=""></picture> Testing

New features and bug fixes need tests. Test coverage is around 99%, please help keep it there, including invalid input and edge cases.

Tests use JUnit 5 and [AssertJ](https://assertj.github.io/doc/), not the JUnit assertions. Test methods are named `methodUnderTest_condition_expectedOutcome`, without "should" or "when". Cases that differ only in input and output go into one `@ParameterizedTest`. Please keep test classes flat, without `@Nested` classes.

```java
@Test
void of_tooShort_throwsInvalidIbanException() {
    assertThatThrownBy(() -> Iban.of("DE123"))
        .isInstanceOf(InvalidIbanException.class);
}

@ParameterizedTest
@ValueSource(strings = {"", "INVALID", "DE00123456789"})
void tryParse_invalidInput_returnsEmpty(String input) {
    assertThat(Iban.tryParse(input)).isEmpty();
}
```

To iterate over countries or random IBANs, use the source annotations from `iban-commons-junit` (`@IbanCountrySource`, `@RandomIbanSource`, `@IbanRegistrySource`) rather than a hand-written `@MethodSource`.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/git-commit-dark.svg"><img src=".github/icons/git-commit.svg" height="20" alt=""></picture> Commit Messages

Commit subjects follow the `type(scope): subject` pattern you can see in `git log`, for example `fix: deserialize Iban regardless of NCD config` or `feat(bankdata): add LI, GR and PT`. Keep the subject short and in present tense, and use the body to explain why, if that is not obvious. Mention the issue number if there is one.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/git-pull-request-dark.svg"><img src=".github/icons/git-pull-request.svg" height="20" alt=""></picture> Pull Requests

Before opening a pull request, run `mvn` and update the Javadoc and the README if the change is visible to users. Small, focused pull requests are easier to review than large ones. Breaking changes to the public API need to be discussed first.

After you open the pull request, CI builds it on all supported platforms. A maintainer will review it, and some back and forth is normal. If you can, add follow-up commits to the pull request rather than force-pushing, so the review stays easy to follow.

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/world-dark.svg"><img src=".github/icons/world.svg" height="20" alt=""></picture> Country Data

The IBAN formats in `IbanRegistry` are generated from the [SWIFT IBAN Registry](https://www.swift.com/standards/data-standards/iban) and should not be edited by hand. If a country is missing or a format is outdated, please open an issue with a reference to the registry release.

Bank data for further countries is a good place to contribute. `iban-commons-bankdata` finds a loader by its class name `BankDataLoader<CC>` in the `loader` package. A new country needs such a loader (the existing ones show how to read the usual source formats), its source URL in `_bankdata.properties`, a bundled CSV snapshot and a test. `BundledDataRefreshTool` creates the snapshot:

```bash
cd iban-commons-bankdata
mvn test-compile exec:java@refresh-bundled-bankdata -Dexec.args="CC"
```

## <picture><source media="(prefers-color-scheme: dark)" srcset=".github/icons/messages-dark.svg"><img src=".github/icons/messages.svg" height="20" alt=""></picture> Questions

If something is unclear, just ask in [Discussions](https://github.com/SpeedBankingDe/iban-commons/discussions). Please be kind and patient with each other, the project is maintained in spare time.

By contributing, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE.txt), like the rest of the project.

Thank you for helping make IBAN Commons better.
