plugins {
    id("com.android.application")
}

android {
    namespace = "org.arabicltr.systemhook"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.arabicltr.systemhook"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.gradleProperty("KEYSTORE_PATH")
                .orElse(providers.environmentVariable("KEYSTORE_PATH"))
                .orNull
            val keystorePassword = providers.gradleProperty("KEYSTORE_PASSWORD")
                .orElse(providers.environmentVariable("KEYSTORE_PASSWORD"))
                .orNull
            val keyAliasProp = providers.gradleProperty("KEY_ALIAS")
                .orElse(providers.environmentVariable("KEY_ALIAS"))
                .orNull
            val keyPasswordProp = providers.gradleProperty("KEY_PASSWORD")
                .orElse(providers.environmentVariable("KEY_PASSWORD"))
                .orNull

            val supplied = listOf(keystorePath, keystorePassword, keyAliasProp, keyPasswordProp)
            require(supplied.all { it.isNullOrBlank() } || supplied.none { it.isNullOrBlank() }) {
                "Provide all four release signing properties together."
            }
            if (!keystorePath.isNullOrBlank()
                && !keystorePassword.isNullOrBlank()
                && !keyAliasProp.isNullOrBlank()
                && !keyPasswordProp.isNullOrBlank()
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    packaging {
        resources {
            // Ensure META-INF/xposed/* descriptors are never stripped.
            pickFirsts += setOf(
                "META-INF/xposed/module.prop",
                "META-INF/xposed/java_init.list",
                "META-INF/xposed/scope.list"
            )
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    compileOnly("io.github.libxposed:api:102.0.0")
}
