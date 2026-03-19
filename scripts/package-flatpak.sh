#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_ID="io.github.BlazingHotCode.CircuitSim"
VERSION="$(tr -d '\r\n' < "$ROOT_DIR/build/version.txt")"
MANIFEST="$ROOT_DIR/packaging/flatpak/$APP_ID.yml"
STAGING_DIR="$ROOT_DIR/build/package-input/flatpak"
BUILD_DIR="$ROOT_DIR/build/flatpak/build-dir"
REPO_DIR="$ROOT_DIR/build/flatpak/repo"
OUTPUT_DIR="$ROOT_DIR/build/package/flatpak"
FLATPAK_BRANCH="stable"
PREPARE_ONLY=0

resolve_release_arch() {
    case "$(uname -m)" in
        x86_64|amd64)
            printf 'x86_64\n'
            ;;
        aarch64|arm64)
            printf 'arm64\n'
            ;;
        *)
            uname -m
            ;;
    esac
}

RELEASE_ARCH="$(resolve_release_arch)"
BUNDLE_PATH="$OUTPUT_DIR/CircuitSim-linux-$VERSION-$RELEASE_ARCH.flatpak"

usage() {
    cat <<'EOF'
Usage: scripts/package-flatpak.sh [--prepare-only]

Builds a Flatpak bundle for CircuitSim.
EOF
}

require_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        printf 'Required command not found: %s\n' "$1" >&2
        exit 1
    fi
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --prepare-only)
            PREPARE_ONLY=1
            shift
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

require_cmd tar

"$ROOT_DIR/scripts/build-jar.sh"

if [[ ! -f "$MANIFEST" ]]; then
    printf 'Missing Flatpak manifest: %s\n' "$MANIFEST" >&2
    exit 1
fi

rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR" "$OUTPUT_DIR"
cp "$ROOT_DIR/dist/CircuitSim.jar" "$STAGING_DIR/CircuitSim.jar"

if [[ $PREPARE_ONLY -eq 1 ]]; then
    printf 'Prepared Flatpak inputs in %s\n' "$STAGING_DIR"
    exit 0
fi

require_cmd flatpak
require_cmd flatpak-builder

rm -rf "$BUILD_DIR" "$REPO_DIR"
rm -f "$BUNDLE_PATH"

flatpak-builder --force-clean --repo="$REPO_DIR" --default-branch="$FLATPAK_BRANCH" "$BUILD_DIR" "$MANIFEST"
flatpak build-bundle "$REPO_DIR" "$BUNDLE_PATH" "$APP_ID" "$FLATPAK_BRANCH"

printf 'Flatpak bundle created at %s\n' "$BUNDLE_PATH"
