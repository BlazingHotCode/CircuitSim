#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION_FILE="$ROOT_DIR/build/version.txt"
MANIFEST_BASE_FILE="$ROOT_DIR/build/manifest-base.txt"
SRC_DIR="$ROOT_DIR/src/main/java"
OUT_DIR="$ROOT_DIR/out"
DIST_DIR="$ROOT_DIR/dist"
APP_NAME="CircuitSim"
MAIN_JAR="$DIST_DIR/$APP_NAME.jar"

resolve_java_tool() {
    local tool_name="$1"
    local candidate

    if command -v "$tool_name" >/dev/null 2>&1; then
        command -v "$tool_name"
        return 0
    fi

    if [[ -n "${JAVA_HOME:-}" ]]; then
        candidate="$JAVA_HOME/bin/$tool_name"
        if [[ -x "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    fi

    return 1
}

require_java_tool() {
    local tool_name="$1"
    local tool_path

    if tool_path="$(resolve_java_tool "$tool_name")"; then
        printf '%s\n' "$tool_path"
        return 0
    fi

    printf 'Required tool not found: %s\n' "$tool_name" >&2
    printf 'JAVA_HOME: %s\n' "${JAVA_HOME:-not set}" >&2
    exit 1
}

if [[ ! -f "$VERSION_FILE" ]]; then
    printf 'Missing version file: %s\n' "$VERSION_FILE" >&2
    exit 1
fi

if [[ ! -f "$MANIFEST_BASE_FILE" ]]; then
    printf 'Missing manifest template: %s\n' "$MANIFEST_BASE_FILE" >&2
    exit 1
fi

VERSION="$(tr -d '\r\n' < "$VERSION_FILE")"
MANIFEST_FILE="$DIST_DIR/manifest.generated.txt"
VERSIONED_JAR="$DIST_DIR/$APP_NAME-$VERSION.jar"
JAVAC_BIN="$(require_java_tool javac)"
JAR_BIN="$(require_java_tool jar)"

mapfile -t SOURCES < <(find "$SRC_DIR" -name '*.java' | sort)
if [[ ${#SOURCES[@]} -eq 0 ]]; then
    printf 'No Java sources found under %s\n' "$SRC_DIR" >&2
    exit 1
fi

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR" "$DIST_DIR"

"$JAVAC_BIN" --release 21 -d "$OUT_DIR" "${SOURCES[@]}"

cp "$MANIFEST_BASE_FILE" "$MANIFEST_FILE"
printf 'Implementation-Version: %s\n' "$VERSION" >> "$MANIFEST_FILE"

"$JAR_BIN" cfm "$VERSIONED_JAR" "$MANIFEST_FILE" -C "$OUT_DIR" .
cp "$VERSIONED_JAR" "$MAIN_JAR"

printf 'Built %s and %s\n' "$VERSIONED_JAR" "$MAIN_JAR"
