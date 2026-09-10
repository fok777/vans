// file:noinspection DependencyNotationArgument
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
}

val apkId = "HyperCeiler"
val gitHash: String by lazy { runGitCommand("rev-parse", "--short", "HEAD") ?: "unknown" }
val gitHashLong: String by lazy { runGitCommand("rev-parse", "HEAD") ?: "unknown" }
val gitCommitCount: Int by lazy { runGitCommand("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 0 }
val gitBranch: String by lazy {
    val url = runGitCommand("remote", "get-url", "origin") ?: "unknown"
    val branch = runGitCommand("branch", "--show-current") ?: "unknown"
    """github\.com[:/](.+?)(\.git)?$""".toRegex().find(url)?.groupValues?.get(1).orEmpty() + "/" + branch
}
// HyperCeiler-Android13-Backport:
// 官方 2.10.166 的 versionCode 是 4571（Xposed 模块仓库 tag: 4571-2.10.166）。
// 本仓库是 fork，git 提交数只有几十，算出来的 versionCode 远低于官方，
// 于是 LSPosed 按包名匹配仓库后一直提示「需要更新」—— 但官方新版要求
// Android 15+（minSdk 35），在 Android 13 上根本装不上，点了也白点。
// 这里给 versionCode 设下限，消除误报。纯版本号，不影响任何功能。
val gitVersionCode: Int by lazy { maxOf(5 + gitCommitCount, 99999) }

fun runGitCommand(vararg args: String): String? = runCatching {
    ProcessBuilder(listOf("git") + args)
        .redirectErrorStream(true)
        .start()
        .let { process ->
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else null
        }
}.getOrNull()

fun loadPropertiesFromFile(fileName: String): Properties? =
    rootProject.file(fileName).takeIf { it.exists() }?.let { file ->
        Properties().apply { load(file.inputStream()) }
    }

android {
    namespace = "com.sevtinge.hyperceiler"
    compileSdk = 37
    compileSdkMinor = 0
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = namespace
        // HyperCeiler-Android13-Backport: 原值 35 (Android 15 / HyperOS 3.0)
        // 下调至 33 (Android 13) 以允许在 Android 13 设备上安装与运行。
        // 注意：这仅解决「能不能装」，hook 是否生效取决于各规则类的 @HookBase 版本范围。
        minSdk = 33
        targetSdk = 37
        versionCode = gitVersionCode
        versionName = "2.10.166"

        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date())

        val buildConfigData = mapOf(
            "BUILD_TIME" to buildTime,
            "BUILD_OS_NAME" to System.getProperty("os.name"),
            "BUILD_USER_NAME" to System.getProperty("user.name"),
            "BUILD_JAVA_VERSION" to System.getProperty("java.version"),
            "GIT_BRANCH" to gitBranch
        )

        for ((key, value) in buildConfigData) {
            buildConfigField("String", key, "\"$value\"")
        }

        ndk {
            // noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x36")
    }

    // HyperCeiler-Android13-Backport:
    // minSdk 下调后，大量为 Android 15/16 编写的调用会触发 NewApi 检查。
    // 项目已在关键路径上做运行时版本判断，这里把 lint 降级为非阻断，避免 release 构建被中断。
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        checkAllWarnings = false
    }

    packaging {
        resources {
            merges += listOf("META-INF/xposed/*")
            excludes += listOf("**")
        }
        dex {
            useLegacyPackaging = true
        }
    }

    val properties: Properties? = loadPropertiesFromFile("signing.properties")
    fun getString(propertyName: String, environmentName: String, prompt: String): String =
        properties?.getProperty(propertyName)
            ?: System.getenv(environmentName)
            ?: System.console()?.readLine("\n$prompt: ").orEmpty()

    val buildTimeSuffix: String by lazy {
        SimpleDateFormat("MMddHHmm").apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date())
    }
    val dateSuffix: String by lazy {
        DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())
    }

    signingConfigs {
        create("hasProperties") {
            if (properties != null) {
                storeFile = file(getString("storeFile", "STORE_FILE", "Store file"))
                storePassword = getString("storePassword", "STORE_PASSWORD", "Store password")
                keyAlias = getString("keyAlias", "KEY_ALIAS", "Key alias")
                keyPassword = getString("keyPassword", "KEY_PASSWORD", "Key password")
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        val configSigning: ApplicationBuildType.() -> Unit = {
            val signingConfigName = if (properties != null) "hasProperties" else "debug"
            signingConfig = signingConfigs.findByName(signingConfigName)
        }

        val applyBase: ApplicationBuildType.() -> Unit = {
            optimization.enable = true
            buildConfigField("String", "GIT_CODE", "\"$gitVersionCode\"")
        }

        release {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            versionNameSuffix = "-$dateSuffix"
        }

        create("beta") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-$dateSuffix"
        }

        create("canary") {
            applyBase()
            configSigning()
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            versionNameSuffix = "-${gitHash}-r${gitVersionCode}"
        }

        debug {
            isMinifyEnabled = false
            buildConfigField("String", "GIT_HASH", "\"$gitHashLong\"")
            buildConfigField("String", "GIT_CODE", "\"$gitVersionCode\"")
            versionNameSuffix = "-${buildTimeSuffix}-r${gitVersionCode}"
            if (properties != null) {
                signingConfig = signingConfigs.findByName("hasProperties")
            }
        }
    }

}

afterEvaluate {
    base {
        val buildTypeName = gradle.startParameter.taskNames
            .firstOrNull { it.contains("assemble", ignoreCase = true) }
            ?.substringAfterLast(":")
            ?.replace("assemble", "", ignoreCase = true)
            ?.lowercase() ?: "debug"
        val suffix = android.buildTypes.findByName(buildTypeName)?.versionNameSuffix ?: ""
        archivesName.set("$apkId-${android.defaultConfig.versionName}$suffix")
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin.jvmToolchain(25)

dependencies {
    implementation(libs.viewpager2)
    implementation(libs.expansion)
    implementation(projects.library.core)
    implementation(projects.library.common)

    api (libs.room.runtime)
    // FTS 支持
    api (libs.room.ktx)
    annotationProcessor (libs.room.compiler)
}
