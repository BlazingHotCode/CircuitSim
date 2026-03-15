#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"
RESOURCES_DIR="$ROOT_DIR/src/main/resources"
OUT_DIR="$ROOT_DIR/out/dev"

mapfile -t SOURCES < <(find "$SRC_DIR" -name '*.java' | sort)
if [[ ${#SOURCES[@]} -eq 0 ]]; then
    printf 'No Java sources found under %s\n' "$SRC_DIR" >&2
    exit 1
fi

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

javac --release 21 -d "$OUT_DIR" "${SOURCES[@]}"
if [[ -d "$RESOURCES_DIR" ]]; then
    cp -R "$RESOURCES_DIR"/. "$OUT_DIR"/
fi
exec java -cp "$OUT_DIR" circuitsim.CircuitSim "$@"
