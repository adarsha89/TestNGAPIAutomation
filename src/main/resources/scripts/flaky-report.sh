#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
REPORT_FILE="$PROJECT_ROOT/target/cli-reports/flaky-report.html"

cd "$PROJECT_ROOT"
./mvnw -q exec:java -Dexec.args="flaky-report $*"

if [[ -f "$REPORT_FILE" ]]; then
    if command -v open >/dev/null 2>&1; then
        open "$REPORT_FILE"
    elif command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$REPORT_FILE"
    else
        echo "Report generated at $REPORT_FILE"
    fi
fi
