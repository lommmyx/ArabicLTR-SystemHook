# Arabic LTR System Hook 0.5.0

تفرض الوحدة اتجاه التخطيط من اليسار إلى اليمين في مكونات Android التي تستخدم `Configuration`. يحتفظ الجهاز بلغته، ويظل اختيار اتجاه الفقرة متاحًا لنظام عرض النصوص. قد تعرض التطبيقات التي تضبط اتجاهها صراحةً تخطيطًا مختلفًا.

## دعم واجهة Shizuku+

في Vector افتح نطاق الوحدة واختر تطبيق Shizuku+ المثبّت لديك، ثم أغلق واجهة التطبيق بالكامل وأعد فتحها. تحمل النسخة العادية اسم الحزمة `af.shizuku.plus.api`، وتحمل نسخة Drop-In الاسم `moe.shizuku.privileged.api`.

يضبط الإصدار اتجاه نافذة النشاط والحوار قبل عرضهما ليرث محتوى Compose وواجهات Android اتجاه LTR. للتحقق بالعربية، افتح الشاشة الرئيسية ثم الإعدادات وأحد الحوارات. افحص موضع العنوان وأزرار شريط التطبيق، ثم دوّر الجهاز وأعد الفحص.

## البناء على Windows

استخدم JDK 17 وAndroid SDK Platform 35. يحدد Gradle Wrapper إصدار Gradle ويتحقق من بصمة التنزيل. في بيئة Windows المجهزة لهذا المشروع، شغّل من مجلد المشروع في PowerShell:

```powershell
$tools = Join-Path $env:LOCALAPPDATA 'ArabicLTR-Build'
$env:JAVA_HOME = Join-Path $tools 'jdk-17.0.21.1+2'
$env:GRADLE_USER_HOME = Join-Path $tools 'gradle-cache'
$env:ANDROID_HOME = Join-Path $tools 'sdk'
.\gradlew.bat :app:lintRelease :app:assembleRelease
```

توجد الحزمة في `app/build/outputs/apk/release/app-release.apk`. على هذا الجهاز مجلد مفتاح الإصدار هو `%LOCALAPPDATA%\ArabicLTR-Build\signing`، وإعداداته في ملف `gradle-cache/gradle.properties` ضمن بيئة البناء نفسها. احتفظ بنسخة احتياطية خاصة من المفتاح وإعداداته لاستمرار التحديثات.

## البناء على Linux وGitHub Actions

- على Linux شغّل `bash scripts/build-linux.sh` مع JDK 17 وأدوات Android SDK. يحفظ السكربت مفتاحه المحلي في `~/.config/arabicltr` عند أول تشغيل.
- لاستخدام هوية الإصدار نفسها على جهاز آخر، مرّر `KEYSTORE_PATH` و`KEYSTORE_PASSWORD` و`KEY_ALIAS` و`KEY_PASSWORD` الخاصة بالمفتاح نفسه.
- يشغّل GitHub Actions فحص Lint وينتج نسخة Debug عند غياب أسرار التوقيع. لإنتاج Release أضف أسرار `KEYSTORE_BASE64` و`KEYSTORE_PASSWORD` و`KEY_ALIAS` و`KEY_PASSWORD` للمفتاح الدائم نفسه.

## تجربة الوحدة على الجهاز

تحتاج الوحدة إلى إطار يدعم libxposed API 102. إذا كانت نسخة 0.3.0 التجريبية موقعة بمفتاح Android Debug، فإن الانتقال إلى توقيع الإصدار يتطلب إزالتها أولًا ثم إعادة اختيار النطاق في المدير.

1. اختر تطبيقًا واحدًا للتجربة من مدير Vector، مثل الإعدادات، ثم أعد تشغيل العملية حسب تعليمات المدير.
2. اضبط اللغة على العربية. افحص موضع عناصر الواجهة، واكتب في حقل البحث: «إصدار Android 17 (رقم ١٢٣)». افحص اتجاه الفقرة ومواضع الأقواس والمؤشر عند التحرير.
3. قارن السلوك مع تعطيل الوحدة وإعادة تشغيل العملية. بعد نجاح التجربة يمكنك توسيع النطاق تدريجيًا إلى واجهة النظام والمشغّل.
