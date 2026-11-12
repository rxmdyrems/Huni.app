#!/usr/bin/env bash
# Extract a version's release notes from a Markdown changelog or legacy ---v file.
# Usage: parse_changelog.sh <version> [CHANGELOG.md]
set -euo pipefail

VERSION="${1:-}"
CHANGELOG_FILE="${2:-CHANGELOG.md}"

if [ -z "$VERSION" ]; then
    echo "Error: version argument required" >&2
    echo "Usage: $0 <version> [CHANGELOG.md]" >&2
    exit 1
fi
if [ ! -f "$CHANGELOG_FILE" ]; then
    echo "Error: changelog file not found: '$CHANGELOG_FILE'" >&2
    exit 1
fi

VERSION="${VERSION#v}"

if grep -Eq "^## v?${VERSION}([[:space:](]|$)" "$CHANGELOG_FILE"; then
    awk -v ver="$VERSION" '
        function trim(value) {
            sub(/^[[:space:]]+/, "", value)
            sub(/[[:space:]]+$/, "", value)
            return value
        }
        /^## / {
            heading = $0
            sub(/^## /, "", heading)
            sub(/^v/, "", heading)
            split(heading, parts, /[[:space:](]/)
            if (parts[1] == ver) {
                found = 1
                lines[++count] = $0
                next
            }
            if (found) exit
        }
        found { lines[++count] = $0 }
        END {
            if (!found) exit 1
            while (count > 0) {
                tail = tolower(trim(lines[count]))
                if (tail == "" || tail ~ /^---+$/ || tail ~ /<\/?sub([[:space:]>]|\/)/) {
                    count--
                } else {
                    break
                }
            }
            for (i = 1; i <= count; i++) print lines[i]
        }
    ' "$CHANGELOG_FILE"
elif grep -q "^---v${VERSION}$" "$CHANGELOG_FILE"; then
    awk -v ver="$VERSION" '
        function trim(value) {
            sub(/^[[:space:]]+/, "", value)
            sub(/[[:space:]]+$/, "", value)
            return value
        }
        /^---v/ {
            if (found) exit
            if ($0 == "---v" ver) { found = 1; next }
            next
        }
        found { lines[++count] = $0 }
        END {
            if (!found) exit 1
            while (count > 0) {
                tail = tolower(trim(lines[count]))
                if (tail == "" || tail ~ /^---+$/ || tail ~ /<\/?sub([[:space:]>]|\/)/) {
                    count--
                } else {
                    break
                }
            }
            for (i = 1; i <= count; i++) print lines[i]
        }
    ' "$CHANGELOG_FILE"
else
    echo "Error: version '$VERSION' not found in '$CHANGELOG_FILE'" >&2
    exit 1
fi
