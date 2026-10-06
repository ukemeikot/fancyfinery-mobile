#!/usr/bin/env bash
# Rebuild and reinstall on the connected phone, then relaunch.
#
# Compose Hot Reload does NOT work on an Android device — it is JVM-only. This
# is the device loop; `./gradlew :composeApp:runHot --auto` is the fast
# hot-reloading preview, in a phone-sized desktop window.
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
export PATH="$PATH:/c/Users/$USERNAME/AppData/Local/Android/Sdk/platform-tools"

./gradlew :androidApp:installDebug --console=plain "$@"

adb shell am force-stop com.fancyfinery.mobile || true
adb shell am start -n com.fancyfinery.mobile/com.fancyfinery.mobile.android.MainActivity >/dev/null
echo "Installed and launched on $(adb shell getprop ro.product.model | tr -d '\r')."
