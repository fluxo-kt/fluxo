#!/bin/sh
# Proves that every public-API check still FAILS when public API changes, by planting new public functions and
# running the checks.
#
# A check that compares nothing stays green forever, so green alone proves nothing; a harness, plugin or Kotlin update
# can silently stop any lane from comparing (AGENTS.md gotcha #5). Each lane is asserted on its own: one lane failing
# must not hide another that went quiet.
#
# Lanes and the evidence each must show for the planted symbol:
#   checkKotlinAbi  diffs of api/fluxo-data.klib.api, api/jvm/fluxo-data.api and api/android/fluxo-data.api
#   tsApiCheck      `+export declare function apiGateCanary` (JS, from a @FluxoJsExport common function)
#   wasmTsApiCheck  `+export declare function apiGateCanaryWasm` (Wasm-JS exports only @JsExport in wasmJs code)
#
# Run after a build so only fluxo-data recompiles. Extra Gradle arguments (e.g. -Pfluxo.dogfood=false) pass through.
set -eu
cd "$(dirname "$0")/../.."

module=fluxo-data
common_file=$module/src/commonMain/kotlin/kt/fluxo/data/ApiGateCanary.kt
wasm_dir=$module/src/wasmJsMain
wasm_file=$wasm_dir/kotlin/kt/fluxo/data/ApiGateCanaryWasm.kt
if [ -e "$common_file" ] || [ -e "$wasm_dir" ]; then
  echo "::error::$common_file or $wasm_dir already exists; the canary would overwrite or delete real code." >&2
  exit 2
fi

log=$(mktemp)
cleanup() {
  rm -f "$common_file" "$log"
  rm -rf "$wasm_dir"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

cat > "$common_file" <<'EOF'
package kt.fluxo.data

import kt.fluxo.common.annotation.FluxoJsExport

@FluxoJsExport
public fun apiGateCanary(): Int = 1
EOF
mkdir -p "$(dirname "$wasm_file")"
cat > "$wasm_file" <<'EOF'
package kt.fluxo.data

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
public fun apiGateCanaryWasm(): Int = 2
EOF

if ./gradlew ":$module:checkKotlinAbi" ":$module:tsApiCheck" ":$module:wasmTsApiCheck" --continue --console=plain "$@" \
  > "$log" 2>&1; then
  cat "$log"
  echo "::error::The API checks passed with new public API planted in $module: at least one check compares nothing." >&2
  exit 1
fi

missing=""
expect() { # $1 = lane label, $2 = fixed string that only that lane's diff prints
  grep -qF -- "$2" "$log" || missing="$missing\n  $1: no line containing '$2'"
}
# A dump's diff header appears only when that dump differs, and the job's apiCheck passed before the plant, so each
# header is that lane reporting the planted API.
expect "checkKotlinAbi klib" "$module/build/kotlin/abi/$module.klib.api"
expect "checkKotlinAbi jvm" "$module/build/kotlin/abi/jvm/$module.api"
expect "checkKotlinAbi android" "$module/build/kotlin/abi/android/$module.api"
expect "tsApiCheck" "+export declare function apiGateCanary(): number;"
expect "wasmTsApiCheck" "+export declare function apiGateCanaryWasm(): number;"

if [ -n "$missing" ]; then
  cat "$log"
  # A failure other than the planted diff (e.g. a compile error) also lands here, with the log above to read.
  printf '::error::API check lanes did not report the planted API:%b\n' "$missing" >&2
  exit 1
fi
echo "All API check lanes failed on the planted API, as they must."
