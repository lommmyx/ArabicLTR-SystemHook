# Build notes — v0.4.0

## Toolchain
- JDK 17 recommended (full JDK; `javac` required)
- Gradle 8.11.1 (provided by the Gradle Wrapper and checked against the published SHA-256)
- Android Gradle Plugin 8.7.3
- Android SDK Platform 35
- Android Build Tools 35.0.0
- `io.github.libxposed:api:102.0.0` (`compileOnly`, Maven Central)

## Build output
`app/build/outputs/apk/release/app-release.apk`

## Source validation
Run `scripts/validate-source.sh` to validate required metadata, the single Java entry class, key hook declarations, and shell script syntax. This is a static source check only.

## Verification performed by the Linux build script
1. `unzip -t` validates the APK archive.
2. `apksigner verify --verbose --print-certs` validates APK signing.
3. Required `META-INF/xposed/*` metadata entries must exist.
4. SHA-256 of the APK is printed.

## Device validation

Use the single-application test in README_AR.md to check layout, mixed-direction text, and editing on the target firmware.
