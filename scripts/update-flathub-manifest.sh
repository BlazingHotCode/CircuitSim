#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION="$(tr -d '\r\n' < "$ROOT_DIR/build/version.txt")"
MANIFEST_PATH="$ROOT_DIR/packaging/flatpak/io.github.BlazingHotCode.CircuitSim.flathub.yml"
SOURCE_MODE="head"
TAG_NAME=""
SOURCE_URL=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tag)
            if [[ $# -lt 2 ]]; then
                printf 'Missing value for %s\n' "$1" >&2
                exit 1
            fi
            SOURCE_MODE="tag"
            TAG_NAME="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            printf 'Unknown argument: %s\n' "$1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

usage() {
    cat <<'EOF'
Usage: scripts/update-flathub-manifest.sh [--tag vX.Y.Z]

Updates the Flathub-ready Flatpak manifest to the current build/version.txt
using either the currently checked out commit or a tagged release source archive.
EOF
}

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

if [[ "$SOURCE_MODE" == "tag" ]]; then
    EXPECTED_TAG="v$VERSION"
    if [[ "$TAG_NAME" != "$EXPECTED_TAG" ]]; then
        printf 'Tag %s does not match build/version.txt (%s)\n' "$TAG_NAME" "$VERSION" >&2
        exit 1
    fi
    SOURCE_URL="https://github.com/BlazingHotCode/CircuitSim/archive/refs/tags/$TAG_NAME.tar.gz"
else
    SOURCE_URL="https://github.com/BlazingHotCode/CircuitSim/archive/$COMMIT_HASH.tar.gz"
fi

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
