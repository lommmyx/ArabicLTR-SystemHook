# Technical references

These primary references were used while revising the source package:

- libxposed API 102 `XposedInterface` (including the meaning of `deoptimize`):
  https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.html
- libxposed API 102 lifecycle and hot reload callbacks:
  https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.html
- libxposed API 102 module metadata and entry-point registration:
  https://libxposed.github.io/api/io/github/libxposed/api/package-summary.html
- Android `Configuration` reference (`setLocale`, `setLocales`, and layout direction):
  https://developer.android.com/reference/android/content/res/Configuration
- Android AOSP `Configuration.java` implementation:
  https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/content/res/Configuration.java
- Gradle official release checksums (Gradle 8.11.1 binary distribution and Wrapper JAR):
  https://gradle.org/release-checksums/
- Gradle Wrapper user guide:
  https://docs.gradle.org/current/userguide/gradle_wrapper.html
- Android SDK setup action used in GitHub Actions:
  https://github.com/android-actions/setup-android

The Gradle Wrapper pins the binary distribution and its published SHA-256 in `gradle/wrapper/gradle-wrapper.properties`.

Shizuku+ manager UI and package identity were checked against both 13.7.0.r3395 release manifests and the following sources.

- [Manager product flavors](https://github.com/thejaustin/ShizukuPlus/blob/3d49a941390afe4dc00c2699b4e141d58311364f/manager/build.gradle)
- [HomeScreen layout](https://github.com/thejaustin/ShizukuPlus/blob/3d49a941390afe4dc00c2699b4e141d58311364f/manager/src/main/java/af/shizuku/manager/home/compose/HomeScreen.kt)
- [Compose UI 1.12.1 Android sources](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.12.1/ui-android-1.12.1-sources.jar)
