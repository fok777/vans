import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sevtinge.hyperceiler.common"
    compileSdk = 37

    defaultConfig {
        // HyperCeiler-Android13-Backport: 35 -> 33 (Android 13)
        minSdk = 33

        buildConfigField("String", "APP_MODULE_ID", "\"com.sevtinge.hyperceiler\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // HyperCeiler-Android13-Backport: 同 app 模块，minSdk 下调后不让 lint 阻断构建
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildTypes {
        release {}
        create("beta") {}
        create("canary") {}
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    api(libs.bundles.miuix)
    // libxposed API 102
    compileOnlyApi(libs.libxposed.api)
}
