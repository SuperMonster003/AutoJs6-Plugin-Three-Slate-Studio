enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "autojs6-plugin-three-slate-studio"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        id("io.github.supermonster003.autojs6-platform-versions") version "1.9.1"
        // The shared 16 KB page alignment gate (roadmap D25): Compose ui-graphics brings libandroidx.graphics.path.so.
        // zh-CN: 共享的 16 KB 页对齐门禁 (路线图 D25): Compose ui-graphics 带入 libandroidx.graphics.path.so.
        id("io.github.supermonster003.autojs6-native-alignment") version "1.9.1"
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
}

plugins {
    id("io.github.supermonster003.autojs6-platform-versions")
    // @Hint by SuperMonster003 on Sep 14, 2025.
    //  ! Enable JDK auto-resolution/download capability for build modules.
    //  ! zh-CN: 让构建模块具备 JDK 自动解析/下载能力.
    id("org.gradle.toolchains.foojay-resolver-convention")
}

includeBuild("build-logic")

include(":app")
