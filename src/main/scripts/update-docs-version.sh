#!/usr/bin/env bash
#
# Copyright © 2025-2026 Markus Spann, SpeedBankingDe
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# src/main/scripts/update-docs-version.sh
#
# Updates the project version in documentation files (README.md, index.html, …).
# Called automatically by maven-release-plugin via the completionGoals hook,
# but can also be run manually from any directory:
#
#   bash src/main/scripts/update-docs-version.sh 1.8.4
#
# Requirements: bash 3.2+, sed, git (optional – for auto-commit)
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

# ── Locate project root (directory containing pom.xml) ───────────────────────
# Works regardless of where the script is called from.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]]; then
    echo "ERROR: Could not locate pom.xml under ${PROJECT_ROOT}" >&2
    exit 1
fi

# ── Resolve target version ────────────────────────────────────────────────────
# Resolution order (first non-empty, non-placeholder value wins):
#
#   1. Explicit CLI argument:      bash update-docs-version.sh 1.8.4
#   2. Environment variable:       releaseVersion=1.8.4 bash update-docs-version.sh
#      (maven-release-plugin does NOT reliably expand ${releaseVersion} as a
#      Maven property when delegating to exec-maven-plugin — the literal string
#      "${releaseVersion}" may arrive here as $1 when the property is unset.)
#   3. Latest Git tag set by maven-release-plugin (most recent vX.Y.Z or
#      <artifactId>-X.Y.Z tag). The tag is created *before* completionGoals run,
#      so it always reflects the release version, not the next SNAPSHOT.
#
# NOTE: Auto-detection from pom.xml is intentionally NOT used: at completionGoals
# time the POM already contains the next development version (e.g. 1.8.5-SNAPSHOT).

# Helper: strip whitespace; return empty string for unresolved Maven placeholders
_clean() {
    local v="${1//[[:space:]]/}"
    # Reject unexpanded Maven property placeholders like "${releaseVersion}"
    [[ "${v}" == *'${'* ]] && v=""
    echo "${v}"
}

NEW_VERSION="$(_clean "${1:-}")"

if [[ -z "${NEW_VERSION}" ]]; then
    NEW_VERSION="$(_clean "${releaseVersion:-}")"
fi

# Strip an accidental -SNAPSHOT suffix (e.g. passed during dry-run / simulation)
NEW_VERSION="${NEW_VERSION%-SNAPSHOT}"

# Fall back to the most recent Git tag when called via completionGoals
if [[ -z "${NEW_VERSION}" ]]; then
    if git -C "${PROJECT_ROOT}" rev-parse --git-dir > /dev/null 2>&1; then
        RAW_TAG="$(git -C "${PROJECT_ROOT}" describe --tags --abbrev=0 2>/dev/null || true)"
        # Normalise common tag formats: v1.8.4, iban-commons-1.8.4, 1.8.4
        NEW_VERSION="${RAW_TAG#v}"
        NEW_VERSION="${NEW_VERSION#iban-commons-}"
        NEW_VERSION="${NEW_VERSION%-SNAPSHOT}"
        echo "  (version derived from Git tag '${RAW_TAG}')"
    fi
fi

if [[ -z "${NEW_VERSION}" ]]; then
    echo "ERROR: Could not determine release version." >&2
    echo "  Options:" >&2
    echo "    a) Pass it as \$1:                bash update-docs-version.sh 1.8.4" >&2
    echo "    b) Export an env variable:        releaseVersion=1.8.4 bash ..." >&2
    echo "    c) Ensure a Git tag exists in ${PROJECT_ROOT}" >&2
    exit 1
fi

# Safety net: abort if the resolved version still looks like a pre-release string
if [[ "${NEW_VERSION}" == *-SNAPSHOT || "${NEW_VERSION}" == *'${'* ]]; then
    echo "ERROR: Resolved version '${NEW_VERSION}' is not a valid release version." >&2
    echo "  Aborting to avoid patching docs with a pre-release version string." >&2
    exit 1
fi

echo "Updating documentation to version ${NEW_VERSION} ..."
echo "  Project root: ${PROJECT_ROOT}"

# ── Helper: portable in-place sed (GNU/Linux + macOS/BSD) ────────────────────
sed_inplace() {
    local pattern="$1"
    local file="$2"
    if sed --version 2>/dev/null | grep -q GNU; then
        sed -i "${pattern}" "${file}"
    else
        sed -i '' "${pattern}" "${file}"
    fi
}

patch_file() {
    local file="${PROJECT_ROOT}/$1"
    local desc="$2"
    shift 2
    if [[ ! -f "${file}" ]]; then
        echo "  – ${desc} not found, skipping"
        return
    fi
    for pattern in "$@"; do
        sed_inplace "${pattern}" "${file}"
    done
    echo "  ✔ ${desc}"
}

# ── README.md ─────────────────────────────────────────────────────────────────
patch_file "README.md" "README.md" \
    "s|<version>[0-9]\+\.[0-9]\+\.[0-9]\+</version>|<version>${NEW_VERSION}</version>|g" \
    "s|iban-commons:[0-9]\+\.[0-9]\+\.[0-9]\+|iban-commons:${NEW_VERSION}|g"

# ── index.html ────────────────────────────────────────────────────────────────
patch_file "index.html" "index.html" \
    "s|<version>[0-9]\+\.[0-9]\+\.[0-9]\+</version>|<version>${NEW_VERSION}</version>|g" \
    "s|iban-commons:[0-9]\+\.[0-9]\+\.[0-9]\+|iban-commons:${NEW_VERSION}|g"

# ── SECURITY.md ───────────────────────────────────────────────────────────────
patch_file "SECURITY.md" "SECURITY.md" \
    "s/| [0-9]\+\.[0-9]\+\.[0-9]\+ *| ✅ Supported |/| ${NEW_VERSION} | ✅ Supported |/"

# ── Optional: stage + amend release commit ────────────────────────────────────
# Uncomment if you want the docs changes included in the release tag commit.
#
# if git -C "${PROJECT_ROOT}" rev-parse --git-dir > /dev/null 2>&1; then
#     git -C "${PROJECT_ROOT}" add README.md index.html SECURITY.md 2>/dev/null || true
#     git -C "${PROJECT_ROOT}" commit --amend --no-edit --allow-empty
#     echo "  ✔ docs changes staged and commit amended"
# fi

echo "Done. Documentation updated to ${NEW_VERSION}."
