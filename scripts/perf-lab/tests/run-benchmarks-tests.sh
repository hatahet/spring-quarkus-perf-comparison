#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../run-benchmarks.sh"

TESTS_RUN=0
TESTS_FAILED=0

assert_equals() {
  local description="$1"
  local expected="$2"
  local actual="$3"
  TESTS_RUN=$(( TESTS_RUN + 1 ))

  if [[ "$actual" == "$expected" ]]; then
    echo "PASS: ${description}"
  else
    echo "FAIL: ${description} (expected ${expected}, got ${actual})"
    TESTS_FAILED=$(( TESTS_FAILED + 1 ))
  fi
}

assert_contains() {
  local description="$1"
  local expected="$2"
  local actual="$3"
  TESTS_RUN=$(( TESTS_RUN + 1 ))
  if [[ "$actual" == *"$expected"* ]]; then
    echo "PASS: ${description}"
  else
    echo "FAIL: ${description} (missing ${expected})"
    TESTS_FAILED=$(( TESTS_FAILED + 1 ))
  fi
}

test_count_cpus() {
  assert_equals "single cpu (0)" \
    "1" "$(count_cpus "0")"

  assert_equals "single cpu (5)" \
    "1" "$(count_cpus "5")"

  assert_equals "simple range (0-3)" \
    "4" "$(count_cpus "0-3")"

  assert_equals "simple range (4-7)" \
    "4" "$(count_cpus "4-7")"

  assert_equals "step range (0-7:2)" \
    "4" "$(count_cpus "0-7:2")"

  assert_equals "step range (0-15:4)" \
    "4" "$(count_cpus "0-15:4")"

  assert_equals "comma-separated singles (0,1,2,3)" \
    "4" "$(count_cpus "0,1,2,3")"

  assert_equals "range + single (0-3,8)" \
    "5" "$(count_cpus "0-3,8")"

  assert_equals "range + singles (0-3,8,10-12)" \
    "8" "$(count_cpus "0-3,8,10-12")"

  assert_equals "range + step range (0-3,8-14:2)" \
    "8" "$(count_cpus "0-3,8-14:2")"

  assert_equals "multiple ranges (0-3,8-11)" \
    "8" "$(count_cpus "0-3,8-11")"
}

test_helidon_options() {
  local help_output script
  help_output=$("${SCRIPT_DIR}/../run-benchmarks.sh" --help)
  script=$(<"${SCRIPT_DIR}/../run-benchmarks.sh")

  assert_contains "help lists Helidon version" "--helidon-version <HELIDON_VERSION>" "$help_output"
  assert_contains "help lists SE runtime" "helidon4-se-jvm" "$help_output"
  assert_contains "help lists SE JPA runtime" "helidon4-se-jpa-jvm" "$help_output"
  assert_contains "help lists MP runtime" "helidon4-mp-jvm" "$help_output"
  assert_contains "help lists MP jOOQ runtime" "helidon4-mp-jooq-jvm" "$help_output"
  assert_contains "Helidon version defaults to 4.5.0" 'HELIDON_VERSION="4.5.0"' "$script"
  assert_contains "SE runtime is a default" 'DEFAULT_RUNTIMES=' "$script"
  assert_contains "default runtimes include MP" '"helidon4-mp-jvm"' "$script"
  assert_contains "default runtimes include MP jOOQ" '"helidon4-mp-jooq-jvm"' "$script"

  TESTS_RUN=$(( TESTS_RUN + 1 ))
  if "${SCRIPT_DIR}/../run-benchmarks.sh" --runtimes helidon4-unknown >/dev/null 2>&1; then
    echo "FAIL: rejects unknown Helidon runtime"
    TESTS_FAILED=$(( TESTS_FAILED + 1 ))
  else
    echo "PASS: rejects unknown Helidon runtime"
  fi
}

test_go_options() {
  local help_output script
  help_output=$("${SCRIPT_DIR}/../run-benchmarks.sh" --help)
  script=$(<"${SCRIPT_DIR}/../run-benchmarks.sh")

  assert_contains "help lists Go version" "--go-version <GO_VERSION>" "$help_output"
  assert_contains "help lists SQL runtime" "go-sql" "$help_output"
  assert_contains "help lists GORM runtime" "go-gorm" "$help_output"
  assert_contains "help lists Ent runtime" "go-ent" "$help_output"
  assert_contains "help lists Fiber runtime" "go-fiber" "$help_output"
  assert_contains "help lists Fiber prefork runtime" "go-fiber-prefork" "$help_output"
  assert_contains "Go version defaults to 1.26.5" 'GO_VERSION="1.26.5"' "$script"
  assert_contains "SQL runtime is allowed" '"go-sql"' "$script"
  assert_contains "GORM runtime is a default" '"go-gorm"' "$script"
  assert_contains "Ent runtime is a default" '"go-ent"' "$script"
  assert_contains "Fiber runtime is allowed" '"go-fiber"' "$script"
  assert_contains "Fiber prefork runtime is a default" '"go-fiber-prefork"' "$script"
  assert_equals "Xmx MiB converts to GOMEMLIMIT" "512MiB" "$(xmx_to_go_memlimit '-Xms512m -Xmx512m')"
  assert_equals "Xmx GiB converts to GOMEMLIMIT" "2GiB" "$(xmx_to_go_memlimit '-Xmx2g')"
  assert_equals "missing Xmx defaults GOMEMLIMIT" "512MiB" "$(xmx_to_go_memlimit '')"

  TESTS_RUN=$(( TESTS_RUN + 1 ))
  if "${SCRIPT_DIR}/../run-benchmarks.sh" --go-version 1.26 >/dev/null 2>&1; then
    echo "FAIL: rejects non-exact Go version"
    TESTS_FAILED=$(( TESTS_FAILED + 1 ))
  else
    echo "PASS: rejects non-exact Go version"
  fi

  TESTS_RUN=$(( TESTS_RUN + 1 ))
  if "${SCRIPT_DIR}/../run-benchmarks.sh" --runtimes go-unknown >/dev/null 2>&1; then
    echo "FAIL: rejects unknown Go runtime"
    TESTS_FAILED=$(( TESTS_FAILED + 1 ))
  else
    echo "PASS: rejects unknown Go runtime"
  fi
}

test_jooq_runtime() {
  local help_output script
  help_output=$("${SCRIPT_DIR}/../run-benchmarks.sh" --help)
  script=$(<"${SCRIPT_DIR}/../run-benchmarks.sh")
  assert_contains "help lists jOOQ runtime" "quarkus3-jooq-jvm" "$help_output"
  assert_contains "jOOQ runtime is allowed" '"quarkus3-jooq-jvm"' "$script"
}

test_dapper_runtime() {
  local help_output script
  help_output=$("${SCRIPT_DIR}/../run-benchmarks.sh" --help)
  script=$(<"${SCRIPT_DIR}/../run-benchmarks.sh")
  assert_contains "help lists Dapper runtime" "dotnet10-dapper" "$help_output"
  assert_contains "Dapper runtime is allowed" '"dotnet10-dapper"' "$script"
}

# --- Run tests ---

test_count_cpus
test_helidon_options
test_go_options
test_jooq_runtime
test_dapper_runtime

# --- Summary ---

echo ""
echo "${TESTS_RUN} tests run, ${TESTS_FAILED} failed."

if [[ "${TESTS_FAILED}" -gt 0 ]]; then
  exit 1
fi
