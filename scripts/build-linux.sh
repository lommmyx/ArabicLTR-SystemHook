#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ANDROID_PLATFORM="android-35"
ANDROID_BUILD_TOOLS="35.0.0"

say() { printf '\n==> %s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

say "Checking JDK"
command -v java >/dev/null 2>&1 || fail "Install a JDK (17 recommended)."
command -v javac >/dev/null 2>&1 || fail "A full JDK is required; javac was not found. On Ubuntu: sudo apt install openjdk-17-jdk"
java -version
javac -version

say "Locating Android SDK"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"
SDKMANAGER="$(command -v sdkmanager || true)"
if [[ -z "$SDKMANAGER" && -d "$ANDROID_SDK_ROOT/cmdline-tools" ]]; then
  SDKMANAGER="$(find "$ANDROID_SDK_ROOT/cmdline-tools" -type f -name sdkmanager -perm -u+x 2>/dev/null | sort -V | tail -n 1 || true)"
fi
if [[ ! -x "$SDKMANAGER" ]]; then
  fail "Android SDK Command-line Tools were not found. Install Google's command-line tools, set ANDROID_SDK_ROOT, then rerun this script."
fi

yes | "$SDKMANAGER" --licenses >/dev/null || true
if [[ ! -d "$ANDROID_SDK_ROOT/platforms/$ANDROID_PLATFORM" || ! -d "$ANDROID_SDK_ROOT/build-tools/$ANDROID_BUILD_TOOLS" ]]; then
  say "Installing required Android SDK packages"
  "$SDKMANAGER" "platform-tools" "platforms;$ANDROID_PLATFORM" "build-tools;$ANDROID_BUILD_TOOLS"
fi

say "Preparing a persistent local signing key"
# A custom key may be supplied through environment variables. Otherwise create
# a private per-user key once and reuse it for future builds/updates.
if [[ -z "${KEYSTORE_PATH:-}" && -z "${KEYSTORE_PASSWORD:-}" && -z "${KEY_ALIAS:-}" && -z "${KEY_PASSWORD:-}" ]]; then
  CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/arabicltr"
  CONFIG_FILE="$CONFIG_DIR/signing.env"
  mkdir -p "$CONFIG_DIR"
  chmod 700 "$CONFIG_DIR"
  if [[ -f "$CONFIG_FILE" ]]; then
    # File contains only generated hex passwords and a local absolute keystore path.
    set -a
    # shellcheck disable=SC1090
    source "$CONFIG_FILE"
    set +a
  else
    command -v openssl >/dev/null 2>&1 || fail "openssl is required to generate a signing password."
    umask 077
    KEYSTORE_PATH="$CONFIG_DIR/arabicltr-release.p12"
    KEYSTORE_PASSWORD="$(openssl rand -hex 24)"
    KEY_ALIAS="arabicltr"
    KEY_PASSWORD="$KEYSTORE_PASSWORD"
    keytool -genkeypair -noprompt -v \
      -keystore "$KEYSTORE_PATH" -storetype PKCS12 \
      -alias "$KEY_ALIAS" -keyalg RSA -keysize 3072 -validity 10000 \
      -storepass "$KEYSTORE_PASSWORD" -keypass "$KEY_PASSWORD" \
      -dname "CN=ArabicLTR, OU=Module, O=ArabicLTR, C=SA"
    {
      printf 'KEYSTORE_PATH=%q\n' "$KEYSTORE_PATH"
      printf 'KEYSTORE_PASSWORD=%q\n' "$KEYSTORE_PASSWORD"
      printf 'KEY_ALIAS=%q\n' "$KEY_ALIAS"
      printf 'KEY_PASSWORD=%q\n' "$KEY_PASSWORD"
    } > "$CONFIG_FILE"
    chmod 600 "$CONFIG_FILE"
  fi
fi

[[ -n "${KEYSTORE_PATH:-}" && -n "${KEYSTORE_PASSWORD:-}" && -n "${KEY_ALIAS:-}" && -n "${KEY_PASSWORD:-}" ]] || fail "Signing configuration is incomplete. Set KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD together."
[[ -f "$KEYSTORE_PATH" ]] || fail "Keystore does not exist: $KEYSTORE_PATH"
export KEYSTORE_PATH KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD

say "Building release APK"
bash ./gradlew --no-daemon :app:lintRelease :app:assembleRelease

APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
[[ -f "$APK" ]] || fail "Gradle did not produce $APK"

say "Verifying APK archive and signature"
unzip -t "$APK" >/dev/null
APKSIGNER="$ANDROID_SDK_ROOT/build-tools/$ANDROID_BUILD_TOOLS/apksigner"
[[ -x "$APKSIGNER" ]] || fail "apksigner not found: $APKSIGNER"
"$APKSIGNER" verify --verbose --print-certs "$APK"

say "Verifying Vector metadata inside APK"
for entry in META-INF/xposed/module.prop META-INF/xposed/java_init.list META-INF/xposed/scope.list; do
  unzip -Z1 "$APK" | grep -Fxq "$entry" || fail "Missing APK entry: $entry"
done
unzip -p "$APK" META-INF/xposed/module.prop
unzip -p "$APK" META-INF/xposed/java_init.list
unzip -p "$APK" META-INF/xposed/scope.list

sha256sum "$APK"
printf '\nSUCCESS: APK built and signature-verified:\n%s\n' "$APK"
