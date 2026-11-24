#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
fail() { echo "ERROR: $*" >&2; exit 1; }

PROP="app/src/main/resources/META-INF/xposed/module.prop"
ENTRY="app/src/main/resources/META-INF/xposed/java_init.list"
SCOPE="app/src/main/resources/META-INF/xposed/scope.list"
MAIN="app/src/main/java/org/arabicltr/systemhook/MainHook.java"

for file in "$PROP" "$ENTRY" "$SCOPE" "$MAIN" "app/build.gradle.kts" "build.gradle.kts" "settings.gradle.kts"; do
  [[ -s "$file" ]] || fail "Required source file missing or empty: $file"
done

grep -Fxq 'minApiVersion=102' "$PROP" || fail "module.prop must require API 102"
grep -Fxq 'targetApiVersion=102' "$PROP" || fail "module.prop must target API 102"
grep -Fxq 'autoHotReload=true' "$PROP" || fail "module.prop hot reload declaration missing"
[[ "$(wc -l < "$ENTRY" | tr -d ' ')" == "1" ]] || fail "Hot reload requires exactly one Java entry class"
grep -Fxq 'org.arabicltr.systemhook.MainHook' "$ENTRY" || fail "Unexpected Java entry class"
grep -Fq 'extends XposedModule' "$MAIN" || fail "Entry class does not extend XposedModule"
grep -Fq 'setLocales' "$MAIN" || fail "LocaleList mutator hook missing"
grep -Fq 'setLocale' "$MAIN" || fail "Locale mutator hook missing"
grep -Fq 'getLayoutDirection' "$MAIN" || fail "Layout direction getter hook missing"
grep -Fq 'replaceHook' "$MAIN" || fail "Hot reload replacement logic missing"

if grep -Eq 'getLayoutDirectionFromLocale|TextUtilsCompat' "$MAIN"; then
  fail "Locale-based text direction must remain available to text rendering"
fi
bash -n scripts/build-linux.sh
bash -n scripts/validate-source.sh


printf 'Source validation passed. This does not compile the project or test runtime hooks.\n'
