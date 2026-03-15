#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_NAME="CircuitSim"
PACKAGE_NAME="circuitsim"
VERSION="$(tr -d '\r\n' < "$ROOT_DIR/build/version.txt")"
TYPE="both"
SKIP_JPACKAGE=0
DEST_DIR="$ROOT_DIR/build/package/linux"
INPUT_DIR="$ROOT_DIR/build/package-input/linux"
DIST_DIR="$ROOT_DIR/dist"
MAIN_JAR="$DIST_DIR/$APP_NAME.jar"

usage() {
    cat <<'EOF'
Usage: scripts/package-linux.sh [--type app-image|deb|both] [--dest DIR] [--skip-jpackage]

Builds a Linux package with a bundled Java runtime so end users do not need a JDK/JRE installed.
EOF
}

require_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        printf 'Required command not found: %s\n' "$1" >&2
        exit 1
    fi
}

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

    if command -v javac >/dev/null 2>&1; then
        candidate="$(cd "$(dirname "$(command -v javac)")/.." && pwd)/bin/$tool_name"
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
    printf 'javac resolved to: %s\n' "$(command -v javac 2>/dev/null || printf 'not found')" >&2
    printf 'Install a full JDK 21 that includes jpackage and set JAVA_HOME or PATH to it.\n' >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --type)
            TYPE="$2"
            shift 2
            ;;
        --dest)
            DEST_DIR="$2"
            shift 2
            ;;
        --skip-jpackage)
            SKIP_JPACKAGE=1
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

case "$TYPE" in
    app-image|deb|both)
        ;;
    *)
        printf 'Unsupported package type: %s\n' "$TYPE" >&2
        usage >&2
        exit 1
        ;;
esac

require_cmd java
require_cmd javac
require_cmd jar
require_cmd jdeps

JAVAC_BIN="$(require_java_tool javac)"
JAR_BIN="$(require_java_tool jar)"
JDEPS_BIN="$(require_java_tool jdeps)"

"$ROOT_DIR/scripts/build-jar.sh"

if [[ ! -f "$MAIN_JAR" ]]; then
    printf 'Expected jar not found: %s\n' "$MAIN_JAR" >&2
    exit 1
fi

MODULES="$("$JDEPS_BIN" --multi-release 21 --ignore-missing-deps --print-module-deps "$MAIN_JAR")"
if [[ -z "$MODULES" ]]; then
    MODULES="java.base,java.desktop"
fi

rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR" "$DEST_DIR"
cp "$MAIN_JAR" "$INPUT_DIR/$APP_NAME.jar"

if [[ $SKIP_JPACKAGE -eq 1 ]]; then
    printf 'Built jar only. Skipped jpackage. Required modules: %s\n' "$MODULES"
    exit 0
fi

JPACKAGE_BIN="$(require_java_tool jpackage)"

COMMON_ARGS=(
    --name "$APP_NAME"
    --app-version "$VERSION"
    --input "$INPUT_DIR"
    --main-jar "$APP_NAME.jar"
    --main-class circuitsim.CircuitSim
    --dest "$DEST_DIR"
    --vendor "CircuitSim"
    --description "Interactive, real-time circuit simulator"
    --add-modules "$MODULES"
    --java-options=-Dfile.encoding=UTF-8
)

"$JPACKAGE_BIN" --type app-image "${COMMON_ARGS[@]}"

if [[ "$TYPE" == "deb" || "$TYPE" == "both" ]]; then
    "$JPACKAGE_BIN" --type deb \
        --linux-package-name "$PACKAGE_NAME" \
        --linux-shortcut \
        --linux-menu-group Education \
        "${COMMON_ARGS[@]}"
fi

printf 'Linux packages created in %s\n' "$DEST_DIR"
