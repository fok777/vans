plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.internal"
    compileSdk = 37

    // HyperCeiler-Android13-Backport: 显式声明 minSdk 33，避免 AGP 取默认最低值
    defaultConfig {
        minSdk = 33
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        create("beta") {
            isMinifyEnabled = false
        }
        create("canary") {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin.jvmToolchain(25)

dependencies {
    implementation(libs.annotation)
}
