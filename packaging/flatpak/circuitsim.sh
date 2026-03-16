#!/usr/bin/env bash
set -euo pipefail

exec /app/jre/bin/java -Dfile.encoding=UTF-8 -jar /app/lib/CircuitSim.jar "$@"
