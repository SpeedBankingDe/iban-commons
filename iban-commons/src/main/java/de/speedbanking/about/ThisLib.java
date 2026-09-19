/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.about;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Provides metadata and build information about this library when executed directly via CLI.
 *
 * @since 1.8.11
 */
public final class ThisLib {

    private ThisLib() {
    }

    /**
     * Main entry point when the JAR file is executed directly.
     * <p>
     * Reads the {@code MANIFEST.MF} file from the JAR package and prints formatted metadata
     * to {@code System.out}.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        readManifest().ifPresent(m -> System.out.print(buildInfo(m)));
    }

    /**
     * Builds the formatted metadata text printed when the JAR file is executed directly.
     * <p>
     * If present, the title is printed on its own line, with the version appended and
     * prefixed with {@code "v"} when available; a version without a title is not printed.
     * The project description, if present, follows on its own line. A table of additional
     * metadata (vendor, build info, git info, and project links) is appended for every
     * attribute actually found in the manifest, followed by a closing notice.
     *
     * @param manifest the manifest to read metadata from, may be {@code null}
     * @return the formatted metadata text, or an empty string if the manifest carries
     *         none of the recognized attributes
     */
    static String buildInfo(Manifest manifest) {
        String title = getAttribute(manifest, "Implementation-Title", "Project-Name").orElse("");
        String version = getAttribute(manifest, "Implementation-Version", "Project-Version").orElse("");
        String description = getAttribute(manifest, "Project-Description").orElse("");

        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        if (!title.isEmpty()) {
            sb.append(nl).append(title);
            if (!version.isEmpty()) {
                sb.append(" v").append(version);
            }
            sb.append(nl);
        }

        if (!description.isEmpty()) {
            sb.append(description).append(nl);
        }
        if (sb.length() > 0) {
            sb.append(nl);
        }

        List<Map.Entry<String, Optional<String>>> metadata = Arrays.asList(
            entry("Vendor",      getAttribute(manifest, "Implementation-Vendor")),
            entry("Build JDK",   getAttribute(manifest, "Build-Jdk-Spec")),
            entry("Build Time",  getAttribute(manifest, "Build-Time")),
            entry("Git Commit",  getAttribute(manifest, "Git-Commit-Id", "X-BasePOM-Git-Commit-Id")),
            entry("Git Branch",  getAttribute(manifest, "Git-Branch")),
            entry("Homepage",    getAttribute(manifest, "Project-Url")),
            entry("Issues",      getAttribute(manifest, "Issue-Management-Url")),
            entry("Source Code", getAttribute(manifest, "Scm-Url")));

        for (Map.Entry<String, Optional<String>> entry : metadata) {
            if (entry.getValue().isPresent()) {
                sb.append(String.format("%-15s: %s", entry.getKey(), entry.getValue().get())).append(nl);
            }
        }

        if (sb.length() > 0) {
            sb.append(nl)
              .append("Please note: This JAR is a software library and not intended for direct CLI execution.").append(nl);
        }

        return sb.toString();
    }

    /**
     * Creates an immutable map entry representing a key-value metadata pair.
     *
     * @param <K>   the key type
     * @param <V>   the value type
     * @param key   metadata key
     * @param value metadata value
     * @return immutable map entry
     */
    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new SimpleImmutableEntry<>(key, value);
    }

    /**
     * Resolves and parses the {@code MANIFEST.MF} file from the location of this class's code source.
     *
     * @return an {@link Optional} containing the parsed {@link Manifest}, or empty if unavailable
     */
    private static Optional<Manifest> readManifest() {
        CodeSource codeSource = ThisLib.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return Optional.empty();
        }

        URL location = codeSource.getLocation();
        if (location == null) {
            return Optional.empty();
        }

        try {
            URL manifestUrl = new URL("jar:" + location.toExternalForm() + "!/META-INF/MANIFEST.MF");
            try (InputStream is = manifestUrl.openStream()) {
                return Optional.of(new Manifest(is));
            }
        } catch (IOException ignored) {
            // running outside JAR context (e.g., IDE) is expected
            return Optional.empty();
        }
    }

    /**
     * Searches for the given attribute names in the main attributes first.
     * <p>
     * If not found, searches across all named individual sections in the manifest.
     *
     * @param manifest the manifest to search in, may be {@code null}
     * @param attributeNames one or more attribute names to look for
     * @return an {@link Optional} containing the first non-blank attribute value, or empty
     */
    private static Optional<String> getAttribute(Manifest manifest, String... attributeNames) {
        for (String attrName : attributeNames) {
            Optional<String> value = findInAttributes(manifest.getMainAttributes(), attrName);
            if (value.isPresent()) {
                return value;
            }

            for (Attributes sectionAttributes : manifest.getEntries().values()) {
                value = findInAttributes(sectionAttributes, attrName);
                if (value.isPresent()) {
                    return value;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts and trims an attribute value from the given attributes object.
     *
     * @param attributes the attributes container, may be {@code null}
     * @param name the name of the attribute
     * @return an {@link Optional} containing the trimmed value, or empty if null or blank
     */
    private static Optional<String> findInAttributes(Attributes attributes, String name) {
        if (attributes == null || name == null) {
            return Optional.empty();
        }
        String val = attributes.getValue(name);
        if (val == null) {
            return Optional.empty();
        }
        String trimmed = val.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

}
