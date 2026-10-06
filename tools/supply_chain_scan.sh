#!/usr/bin/env bash
#
# Supply-chain implant scanner for Kotlin Multiplatform projects.
#
# A KMP build executes more third-party-influenced code than most stacks, on more
# hosts. What actually runs during a build:
#
#   * settings.gradle.kts / build.gradle.kts -- arbitrary Kotlin at CONFIGURATION
#     time, before any task runs
#   * gradle/wrapper/gradle-wrapper.jar -- a binary that runs before Gradle exists
#   * gradlew / gradlew.bat -- the entry point shell scripts
#   * buildSrc/ and build-logic/ convention plugins -- compiled and applied
#   * Xcode "Run Script" build phases embedded in project.pbxproj
#   * webpack.config.d/*.js -- injected into the wasmJs/js webpack build
#   * npm dependencies for the wasm target (worker/package.json, kotlin-js-store)
#
# Runs entirely from the repo. It downloads nothing -- a scanner fetched at scan
# time is itself a supply-chain dependency.

set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

FAILED=0
fail() {
  echo "FAIL [$1] $2" >&2
  FAILED=1
}
warn() { echo "WARN [$1] $2" >&2; }

PRUNE='-name build -o -name .git -o -name .gradle -o -name .kotlin -o -name node_modules -o -name DerivedData'

echo "[1/7] Verifying the Gradle wrapper..."
WRAPPER_JAR=gradle/wrapper/gradle-wrapper.jar
WRAPPER_PROPS=gradle/wrapper/gradle-wrapper.properties

if [ -f "$WRAPPER_PROPS" ]; then
  dist_url=$(grep -E '^distributionUrl=' "$WRAPPER_PROPS" | cut -d= -f2- | sed 's|\\||g')
  case "$dist_url" in
    https://services.gradle.org/distributions/*) : ;;
    https://*) warn "wrapper-url" "distributionUrl is not the official Gradle host: $dist_url" ;;
    *) fail "wrapper-url" "distributionUrl is not HTTPS: $dist_url" ;;
  esac
  if ! grep -q '^distributionSha256Sum=' "$WRAPPER_PROPS"; then
    fail "wrapper-unpinned" "$WRAPPER_PROPS has no distributionSha256Sum -- the Gradle distribution is not pinned by hash"
  fi
fi

if [ -f "$WRAPPER_JAR" ]; then
  # The stock wrapper jar is ~45KB and contains only org/gradle classes. Anything
  # else in there executes before Gradle itself is even downloaded.
  size=$(wc -c < "$WRAPPER_JAR")
  if [ "$size" -gt 102400 ]; then
    fail "wrapper-jar-size" "$WRAPPER_JAR is ${size} bytes (stock is well under 100KB)"
  fi
  if command -v unzip >/dev/null 2>&1; then
    stray=$(unzip -l "$WRAPPER_JAR" 2>/dev/null | awk 'NR>3 && $4 != "" {print $4}' \
            | grep -vE '^(org/gradle/|org/|META-INF/|.*/$)' || true)
    if [ -n "$stray" ]; then
      fail "wrapper-jar-contents" "$WRAPPER_JAR contains non-Gradle entries:"
      printf '%s\n' "$stray" >&2
    fi
  fi
fi

echo "[2/7] Checking Gradle repositories resolve to trusted hosts..."
# A rogue repository entry can shadow a legitimate coordinate with a malicious jar.
mapfile -t GRADLE_FILES < <(
  find . \( $PRUNE \) -prune -o -type f \( -name '*.gradle.kts' -o -name '*.gradle' \) -print
)
for f in "${GRADLE_FILES[@]}"; do
  [ -e "$f" ] || continue
  urls=$(grep -oE 'maven[[:space:]]*\([[:space:]]*(url[[:space:]]*=[[:space:]]*)?"[^"]+"' "$f" \
         | grep -oE 'https?://[^"]+' || true)
  for u in $urls; do
    case "$u" in
      https://repo.maven.apache.org/*|https://repo1.maven.org/*|https://dl.google.com/*|\
      https://maven.google.com/*|https://plugins.gradle.org/*|https://central.sonatype.com/*|\
      https://s01.oss.sonatype.org/*|https://oss.sonatype.org/*) : ;;
      http://*) fail "insecure-repo" "$f uses a plaintext HTTP repository: $u" ;;
      *) warn "unknown-repo" "$f declares a non-standard Maven repository: $u" ;;
    esac
  done
done

echo "[3/7] Checking build files for oversized or padded content..."
# The padding trick: a legitimate head, a long run of whitespace, then the payload
# sitting off the right edge of the editor.
mapfile -t BUILD_FILES < <(
  find . \( $PRUNE \) -prune -o -type f \( \
    -name '*.gradle.kts' -o -name '*.gradle' -o -name '*.versions.toml' \
    -o -name 'gradlew' -o -name 'gradlew.bat' -o -path '*/webpack.config.d/*' \
  \) -print
)
for f in "${BUILD_FILES[@]}"; do
  [ -e "$f" ] || continue
  size=$(wc -c < "$f")
  if [ "$size" -gt 30720 ]; then
    fail "file-size" "$f is ${size} bytes -- unusually large for a build file"
  fi
  awk -v f="$f" '
    length > 500 {
      printf "FAIL [long-line] %s:%d (%d chars)\n", f, NR, length > "/dev/stderr"
      bad = 1
    }
    END { exit bad ? 1 : 0 }
  ' "$f" || FAILED=1
done

echo "[4/7] Checking for process execution and network access at build time..."
# Nothing in a starter's Gradle config needs to shell out or open a socket.
EXEC_PATTERNS=(
  'Runtime\.getRuntime\(\)\.exec'
  'ProcessBuilder'
  'URLClassLoader'
  '\bURL\("http'
  'HttpURLConnection|OkHttpClient'
  '\bcurl\b|\bwget\b'
  'Base64\.(getDecoder|decode)'
  'eval\s*\(|Function\s*\('
)
for f in "${BUILD_FILES[@]}"; do
  [ -e "$f" ] || continue
  case "$f" in */gradlew|*/gradlew.bat|./gradlew|./gradlew.bat) continue ;; esac
  for pattern in "${EXEC_PATTERNS[@]}"; do
    if grep -qE "$pattern" "$f" 2>/dev/null; then
      fail "build-capability" "$f matches: $pattern"
    fi
  done
done

echo "[5/7] Checking Xcode project for injected run-script phases..."
# A shellScript build phase in project.pbxproj runs on every Xcode build. The
# stock KMP template has exactly one, which invokes Gradle.
mapfile -t PBX < <(find . \( $PRUNE \) -prune -o -type f -name 'project.pbxproj' -print)
for f in "${PBX[@]}"; do
  [ -e "$f" ] || continue
  count=$(grep -c 'shellScript = ' "$f" || true)
  if [ "$count" -gt 2 ]; then
    fail "xcode-run-script" "$f declares $count shellScript phases (expected at most 2) -- review each"
  fi
  if grep -E 'shellScript = ' "$f" | grep -qE 'curl|wget|base64|eval|\| *sh|\| *bash'; then
    fail "xcode-run-script" "$f has a shellScript phase that downloads or evaluates code"
  fi
done

echo "[6/7] Checking JavaScript used by the wasm/js target..."
mapfile -t JS_FILES < <(
  find . \( $PRUNE \) -prune -o -type f -name '*.js' -print
)
JS_PATTERNS=(
  '_0x[0-9a-f]{4,6}'
  'child_process'
  '\beval\s*\('
  'new[[:space:]]+Function[[:space:]]*\('
  '(drpc\.org|blockscout|blastapi\.io|publicnode\.com|1rpc\.io)'
  'eth_(blockNumber|getBlockByNumber|call)'
)
for f in "${JS_FILES[@]}"; do
  [ -e "$f" ] || continue
  for pattern in "${JS_PATTERNS[@]}"; do
    if grep -qE "$pattern" "$f" 2>/dev/null; then
      fail "js-suspicious" "$f matches: $pattern"
    fi
  done
  awk -v f="$f" '
    length > 500 {
      printf "FAIL [long-line] %s:%d (%d chars)\n", f, NR, length > "/dev/stderr"
      bad = 1
    }
    END { exit bad ? 1 : 0 }
  ' "$f" || FAILED=1
done

echo "[7/7] Checking CI workflows do not execute remote code..."
if [ -d .github/workflows ]; then
  for f in .github/workflows/*.y*ml; do
    [ -e "$f" ] || continue
    if grep -qE '(curl|wget)[^|]*\|[[:space:]]*(ba)?sh' "$f"; then
      fail "ci-remote-exec" "$f pipes a downloaded script into a shell"
    fi
    if grep -qE '(curl|wget).*(-o|--output|-O)[[:space:]]' "$f"; then
      fail "ci-remote-fetch" "$f downloads a file at run time -- vendor it instead"
    fi
  done
fi

if [ "$FAILED" -ne 0 ]; then
  echo "" >&2
  echo "Scan FAILED. Do not build until resolved." >&2
  exit 1
fi

echo "OK: no supply-chain implant signatures found."
