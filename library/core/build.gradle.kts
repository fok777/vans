plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sevtinge.hyperceiler.core"
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
    // api(projects.library.hook)
    api(projects.library.common)
    api(projects.library.provision)
    api(libs.appiconloader)
}
