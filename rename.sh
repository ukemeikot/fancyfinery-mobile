#!/usr/bin/env bash
# Rename the project package.
# Usage: ./rename.sh com.yourcompany.appname
set -euo pipefail

OLD_PACKAGE="com.fancyfinery.mobile"
NEW_PACKAGE="${1:-}"

if [ -z "$NEW_PACKAGE" ]; then
    echo "Usage: ./rename.sh <new.package.name>"
    echo "Example: ./rename.sh com.acme.myapp"
    exit 1
fi

if ! printf '%s' "$NEW_PACKAGE" | grep -qE '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$'; then
    echo "Error: '$NEW_PACKAGE' is not a valid package name." >&2
    echo "Use lowercase segments separated by dots, e.g. com.acme.myapp" >&2
    exit 1
fi

OLD_PATH=$(printf '%s' "$OLD_PACKAGE" | tr '.' '/')
NEW_PATH=$(printf '%s' "$NEW_PACKAGE" | tr '.' '/')

echo "Renaming $OLD_PACKAGE -> $NEW_PACKAGE"

# GNU sed wants `sed -i`, BSD/macOS sed wants `sed -i ''`. The original script
# hardcoded the BSD form, which fails on Linux. Writing to a temp file and moving
# it back behaves identically on both.
sed_inplace() {
    expr="$1"; shift
    for f in "$@"; do
        [ -f "$f" ] || continue
        sed "$expr" "$f" > "$f.rename.tmp" && mv "$f.rename.tmp" "$f"
    done
}

# 1. Rewrite references inside text files. .pbxproj and .xcconfig matter for the
#    iOS bundle identifier; the original script missed both.
files=$(find . -type f \
    \( -name '*.kt' -o -name '*.kts' -o -name '*.xml' -o -name '*.swift' \
       -o -name '*.pro' -o -name '*.pbxproj' -o -name '*.xcconfig' -o -name '*.md' \
       -o -name '*.json' -o -name '*.yml' -o -name '*.yaml' \) \
    -not -path '*/.git/*' -not -path '*/build/*' -not -path '*/.gradle/*' \
    -not -path '*/.kotlin/*')

# shellcheck disable=SC2086
sed_inplace "s|${OLD_PACKAGE}|${NEW_PACKAGE}|g; s|${OLD_PATH}|${NEW_PATH}|g" $files

# 2. Move the source directories. Kotlin source layout mirrors the package by
#    convention; the original script rewrote text only, leaving every file in a
#    stale directory tree.
find . -type d -path "*/kotlin/${OLD_PATH}" -not -path '*/build/*' | while read -r dir; do
    base="${dir%/${OLD_PATH}}"
    mkdir -p "$base/$(dirname "$NEW_PATH")"
    mv "$dir" "$base/$NEW_PATH"
    old_root="$base/$(printf '%s' "$OLD_PATH" | cut -d/ -f1)"
    if [ -d "$old_root" ]; then
        find "$old_root" -type d -empty -delete 2>/dev/null || true
    fi
done

# 3. Room exports schemas into a directory named after the database's FQN.
for s in composeApp/schemas/"${OLD_PACKAGE}".*; do
    [ -e "$s" ] || continue
    mv "$s" "composeApp/schemas/$(basename "$s" | sed "s|${OLD_PACKAGE}|${NEW_PACKAGE}|")"
done

# 4. Update this script's own default so it stays re-runnable.
sed_inplace "s|^OLD_PACKAGE=\"${OLD_PACKAGE}\"|OLD_PACKAGE=\"${NEW_PACKAGE}\"|" "$0"

echo "Done."
echo ""
echo "Next steps:"
echo "  1. Set BASE_URL in composeApp/src/commonMain/kotlin/${NEW_PATH}/core/network/NetworkConfig.kt"
echo "  2. Update app_name in androidApp/src/main/res/values/strings.xml"
echo "  3. Check PRODUCT_BUNDLE_IDENTIFIER in iosApp/Configuration/Config.xcconfig"
echo "  4. ./gradlew :androidApp:assembleDebug"
