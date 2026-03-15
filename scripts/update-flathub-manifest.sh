#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION="$(tr -d '\r\n' < "$ROOT_DIR/build/version.txt")"
MANIFEST_PATH="$ROOT_DIR/packaging/flatpak/com.blazinghotcode.CircuitSim.flathub.yml"
JAR_PATH="$ROOT_DIR/dist/CircuitSim-$VERSION.jar"
RELEASE_URL="https://github.com/BlazingHotCode/CircuitSim/releases/download/v$VERSION/CircuitSim-$VERSION.jar"

usage() {
    cat <<'EOF'
Usage: scripts/update-flathub-manifest.sh

Updates the Flathub-ready Flatpak manifest to the current build/version.txt
using the locally built versioned jar checksum and GitHub release jar URL.
EOF
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" ]]; then
    usage
    exit 0
fi

if [[ ! -f "$MANIFEST_PATH" ]]; then
    printf 'Missing Flathub manifest: %s\n' "$MANIFEST_PATH" >&2
    exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
    printf 'Missing versioned jar: %s\n' "$JAR_PATH" >&2
    printf 'Run ./scripts/build-jar.sh first.\n' >&2
    exit 1
fi

SHA256="$(sha256sum "$JAR_PATH" | cut -d' ' -f1)"

TMP_FILE="$(mktemp)"
awk -v url="$RELEASE_URL" -v sha="$SHA256" '
    BEGIN {
        url_updated = 0
        sha_updated = 0
    }
    !url_updated && $1 == "url:" {
        print "        url: " url
        url_updated = 1
        next
    }
    !sha_updated && $1 == "sha256:" {
        print "        sha256: " sha
        sha_updated = 1
        next
    }
    {
        print
    }
' "$MANIFEST_PATH" > "$TMP_FILE"
mv "$TMP_FILE" "$MANIFEST_PATH"

printf 'Updated %s to v%s\n' "$MANIFEST_PATH" "$VERSION"
