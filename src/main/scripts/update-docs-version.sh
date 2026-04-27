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
# Updates the project version in documentation files (README.md, index.html, ...).
#
# USAGE:
#   bash update-docs-version.sh <project-name> <version>
#
# EXAMPLE (Manual):
#   bash src/main/scripts/update-docs-version.sh "iban-commons" "1.8.6"
#
# EXAMPLE (Maven exec-plugin):
#   <arguments>
#       <argument>${maven.multiModuleProjectDirectory}/src/main/scripts/update-docs-version.sh</argument>
#       <argument>${project.dist.name}</argument>
#       <argument>${releaseVersion}</argument>
#   </arguments>
#
# Requirements: bash 3.2+, sed, git (optional)
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

# ── Locate project root ──────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]]; then
    echo "ERROR: Could not locate pom.xml under ${PROJECT_ROOT}" >&2
    exit 1
fi

# ── Resolve Arguments ────────────────────────────────────────────────────────
# $1: Project distribution name, $2: Target version
PROJ_NAME="${1:-iban-commons}"

_clean() {
    local v="${1//[[:space:]]/}"
    [[ "${v}" == *'${'* ]] && v=""
    echo "${v}"
}

NEW_VERSION="$(_clean "${2:-}")"

if [[ -z "${NEW_VERSION}" ]]; then
    NEW_VERSION="$(_clean "${releaseVersion:-}")"
fi

# Strip -SNAPSHOT suffix
NEW_VERSION="${NEW_VERSION%-SNAPSHOT}"

# Fallback: derive version from the most recent Git tag
if [[ -z "${NEW_VERSION}" ]]; then
    if git -C "${PROJECT_ROOT}" rev-parse --git-dir > /dev/null 2>&1; then
        RAW_TAG="$(git -C "${PROJECT_ROOT}" describe --tags --abbrev=0 2>/dev/null || true)"
        # Normalise common tag formats: v1.8.5, iban-commons-1.8.5, 1.8.5
        NEW_VERSION="${RAW_TAG#v}"
        NEW_VERSION="${NEW_VERSION#${PROJ_NAME}-}"
        NEW_VERSION="${NEW_VERSION%-SNAPSHOT}"
        echo "  (version derived from Git tag '${RAW_TAG}')"
    fi
fi

if [[ -z "${NEW_VERSION}" ]]; then
    echo "ERROR: Could not determine release version." >&2
    exit 1
fi

echo "Updating documentation for '${PROJ_NAME}' to version '${NEW_VERSION}' ..."

# ── Helper: portable in-place sed ────────────────────────────────────────────
sed_inplace() {
    local pattern="$1"
    local file="$2"
    if sed --version 2>/dev/null | grep -q GNU; then
        sed -i "${pattern}" "${file}"
    else
        # macOS/BSD sed requires an empty string for the -i flag
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

# ── Patch Documents ──────────────────────────────────────────────────────────
# Using \{1,\} instead of \+ for maximum portability across sed versions.

patch_file "README.md" "README.md" \
    "s|<version>[0-9]\{1,\}\.[0-9]\{1,\}\.[0-9]\{1,\}</version>|<version>${NEW_VERSION}</version>|g" \
    "s|${PROJ_NAME}:[0-9]\{1,\}\.[0-9]\{1,\}\.[0-9]\{1,\}|${PROJ_NAME}:${NEW_VERSION}|g"

patch_file "index.html" "index.html" \
    "s|<version>[0-9]\{1,\}\.[0-9]\{1,\}\.[0-9]\{1,\}</version>|<version>${NEW_VERSION}</version>|g" \
    "s|${PROJ_NAME}:[0-9]\{1,\}\.[0-9]\{1,\}\.[0-9]\{1,\}|${PROJ_NAME}:${NEW_VERSION}|g"

patch_file "SECURITY.md" "SECURITY.md" \
    "s/| [0-9]\{1,\}\.[0-9]\{1,\}\.[0-9]\{1,\} *| ✅ Supported |/| ${NEW_VERSION} | ✅ Supported |/"

echo "Done. Documentation updated to ${NEW_VERSION}."
