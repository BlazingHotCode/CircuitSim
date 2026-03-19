#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION="$(tr -d '\r\n' < "$ROOT_DIR/build/version.txt")"
MANIFEST_PATH="$ROOT_DIR/packaging/flatpak/io.github.BlazingHotCode.CircuitSim.flathub.yml"

usage() {
    cat <<'EOF'
Usage: scripts/update-flathub-manifest.sh

Updates the Flathub-ready Flatpak manifest to the current build/version.txt
using the GitHub source archive URL and checksum for the currently checked out commit.
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

if ! command -v git >/dev/null 2>&1; then
    printf 'Missing required command: git\n' >&2
    exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
    printf 'Missing required command: curl\n' >&2
    exit 1
fi

if ! command -v sha256sum >/dev/null 2>&1; then
    printf 'Missing required command: sha256sum\n' >&2
    exit 1
fi

COMMIT_HASH="$(git -C "$ROOT_DIR" rev-parse HEAD)"
SOURCE_URL="https://github.com/BlazingHotCode/CircuitSim/archive/$COMMIT_HASH.tar.gz"

SHA256="$(curl -L "$SOURCE_URL" | sha256sum | cut -d' ' -f1)"

TMP_FILE="$(mktemp)"
awk -v url="$SOURCE_URL" -v sha="$SHA256" '
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
