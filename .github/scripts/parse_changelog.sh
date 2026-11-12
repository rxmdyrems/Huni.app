#!/usr/bin/env bash
# Extract a changelog entry for one version while preserving its Markdown text.
# Supports canonical ## vX.Y.Z headings and the legacy ---vX.Y.Z separators.
# Usage: ./parse_changelog.sh <version> [changelog_file]
set -euo pipefail

VERSION="${1:-}"
CHANGELOG_FILE="${2:-CHANGELOG.md}"

if [ -z "$VERSION" ]; then
    echo "Error: version argument required" >&2
    echo "Usage: $0 <version> [changelog_file]" >&2
    exit 1
fi

if [ ! -f "$CHANGELOG_FILE" ]; then
    echo "Error: changelog file not found: '$CHANGELOG_FILE'" >&2
    exit 1
fi

VERSION="${VERSION#v}"

awk -v ver="$VERSION" '
    /^---v/ {
        if (found) exit
        if ($0 == "---v" ver) { found=1; next }
        next
    }
    /^##[[:space:]]+/ {
        heading=$0
        sub(/^##[[:space:]]+/, "", heading)
        if (heading ~ /^v?[0-9]+([.][0-9]+)*([[:space:]]|$)/) {
            if (found) exit
            sub(/^v/, "", heading)
            sub(/[[:space:]].*$/, "", heading)
            if (heading == ver) { found=1; next }
            next
        }
    }
    found && /^---[[:space:]]*$/ { exit }
    found { print }
    END { if (!found) exit 1 }
' "$CHANGELOG_FILE"
