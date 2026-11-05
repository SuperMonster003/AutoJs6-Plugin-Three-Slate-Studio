// @Hint: declared here, applied by the modules.
//  ! The platform-versions plugin publishes the AGP and Kotlin versions as system properties before this script runs, so
//  ! the plugin versions below come from one central decision and are never hardcoded. The Compose compiler plugin ships
//  ! with every Kotlin release since Kotlin 2.0, so it follows the Kotlin version the platform plugin selected.
//  ! zh-CN: platform-versions 插件在本脚本执行前以系统属性发布 AGP 与 Kotlin 版本, 因此下列插件版本来自同一个中央决策, 不做硬编码.
//  ! Compose 编译器插件自 Kotlin 2.0 起随 Kotlin 发布, 因此跟随平台插件选定的 Kotlin 版本.
plugins {
    id("com.android.application") version System.getProperty("gradle.agp.version") apply false
    id("org.jetbrains.kotlin.plugin.compose") version System.getProperty("gradle.kotlin.version") apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

tasks {
    register<Delete>("clean").configure {
        delete(rootProject.layout.buildDirectory)
    }
}
