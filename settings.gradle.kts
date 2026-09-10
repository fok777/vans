@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        // maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven("https://jitpack.io")
        maven("https://api.xposed.info")
        google()
        mavenCentral()
    }
}

rootProject.name = "HyperCeiler"
include(":app", ":hidden-api", ":app:processor")
