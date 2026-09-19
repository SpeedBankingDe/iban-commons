package de.speedbanking.about;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.Manifest;

@SuppressWarnings("checkstyle:MethodName")
final class ThisLibTest {

    @Test
    void buildInfo_fullManifest_containsAllFields() throws IOException {
        Manifest manifest = manifestOf(
            "Implementation-Title: iban-commons",
            "Implementation-Version: 1.8.11",
            "Implementation-Vendor: SpeedBankingDe",
            "Project-Description: IBAN and BIC validation library",
            "Build-Jdk-Spec: 21",
            "Build-Time: 2026-09-19T10:00:00Z",
            "Git-Commit-Id: abcdef1",
            "Git-Branch: master",
            "Project-Url: https://www.speedbanking.de",
            "Issue-Management-Url: https://github.com/SpeedBankingDe/iban-commons/issues",
            "Scm-Url: https://github.com/SpeedBankingDe/iban-commons");

        String info = ThisLib.buildInfo(manifest);

        assertThat(info)
            .contains("iban-commons v1.8.11")
            .contains("IBAN and BIC validation library")
            .contains("Vendor         : SpeedBankingDe")
            .contains("Build JDK      : 21")
            .contains("Build Time     : 2026-09-19T10:00:00Z")
            .contains("Git Commit     : abcdef1")
            .contains("Git Branch     : master")
            .contains("Homepage       : https://www.speedbanking.de")
            .contains("Issues         : https://github.com/SpeedBankingDe/iban-commons/issues")
            .contains("Source Code    : https://github.com/SpeedBankingDe/iban-commons")
            .contains("not intended for direct CLI execution");
    }

    @Test
    void buildInfo_emptyManifest_returnsEmptyString() throws IOException {
        Manifest manifest = manifestOf();

        String info = ThisLib.buildInfo(manifest);

        assertThat(info).isEmpty();
    }

    @Test
    void buildInfo_titleWithoutVersion_omitsVersionSuffix() throws IOException {
        Manifest manifest = manifestOf("Implementation-Title: iban-commons");

        String info = ThisLib.buildInfo(manifest);

        assertThat(info)
            .contains("iban-commons")
            .doesNotContain(" v")
            .contains("not intended for direct CLI execution");
    }

    @Test
    void buildInfo_versionWithoutTitle_dropsVersionSilently() throws IOException {
        Manifest manifest = manifestOf("Implementation-Version: 1.8.11");

        String info = ThisLib.buildInfo(manifest);

        assertThat(info).doesNotContain("1.8.11");
    }

    @Test
    void buildInfo_blankAttributeValue_treatedAsMissing() throws IOException {
        Manifest manifest = manifestOf(
            "Implementation-Title: iban-commons",
            "Implementation-Vendor:  ");

        String info = ThisLib.buildInfo(manifest);

        assertThat(info).doesNotContain("Vendor");
    }

    @Test
    void buildInfo_attributeOnlyInNamedSection_isFound() throws IOException {
        String raw = "Manifest-Version: 1.0" + System.lineSeparator()
            + "Implementation-Title: iban-commons" + System.lineSeparator()
            + System.lineSeparator()
            + "Name: de/speedbanking/about/" + System.lineSeparator()
            + "Implementation-Vendor: SectionVendor" + System.lineSeparator();
        Manifest manifest = new Manifest(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));

        String info = ThisLib.buildInfo(manifest);

        assertThat(info).contains("Vendor         : SectionVendor");
    }

    @Test
    void buildInfo_fallbackAttributeNames_areUsedWhenPrimaryMissing() throws IOException {
        Manifest manifest = manifestOf(
            "Project-Name: iban-commons",
            "Project-Version: 1.8.11",
            "X-BasePOM-Git-Commit-Id: abcdef1");

        String info = ThisLib.buildInfo(manifest);

        assertThat(info)
            .contains("iban-commons v1.8.11")
            .contains("Git Commit     : abcdef1");
    }

    @Test
    void main_outsideJarContext_doesNotThrow() {
        assertThatCode(() -> ThisLib.main(new String[0])).doesNotThrowAnyException();
    }

    private static Manifest manifestOf(String... lines) throws IOException {
        StringBuilder sb = new StringBuilder("Manifest-Version: 1.0").append(System.lineSeparator());
        for (String line : lines) {
            sb.append(line).append(System.lineSeparator());
        }
        return new Manifest(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

}
