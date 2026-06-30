#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  if JAVA_21_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
    export JAVA_HOME="${JAVA_21_HOME}"
  fi
fi

cd "${PROJECT_ROOT}"

echo "Running all tests from: ${PROJECT_ROOT}"
if [[ -n "${JAVA_HOME:-}" ]]; then
  echo "Using JAVA_HOME: ${JAVA_HOME}"
fi

TEST_EXIT=0
mvn clean test "$@" || TEST_EXIT=$?

echo ""
python3 "${SCRIPT_DIR}/generate-test-dashboard.py" || true

DASHBOARD="${PROJECT_ROOT}/target/test-dashboard.html"
if [[ -f "${DASHBOARD}" ]]; then
  if [[ "$(uname -s)" == "Darwin" ]] && command -v open >/dev/null 2>&1; then
    open "${DASHBOARD}" >/dev/null 2>&1 || true
  fi
fi

exit "${TEST_EXIT}"
