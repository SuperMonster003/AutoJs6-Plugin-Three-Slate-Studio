import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl` /* kotlin("jvm") */
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        register("utils") {
            id = "org.autojs.build.utils"
            implementationClass = "org.autojs.build.UtilsPlugin"
            displayName = "AutoJs6 Build Utils Plugin"
            description = "Provides digest, date and version helpers."
        }
        register("versions") {
            id = "org.autojs.build.versions"
            implementationClass = "org.autojs.build.VersionsPlugin"
            displayName = "AutoJs6 Versions Plugin"
            description = "Provides version helpers."
        }
        register("signs") {
            id = "org.autojs.build.signs"
            implementationClass = "org.autojs.build.SignsPlugin"
            displayName = "AutoJs6 Signs Plugin"
            description = "Provides signing helpers."
        }
        register("properties") {
            id = "org.autojs.build.properties"
            implementationClass = "org.autojs.build.PropertiesPlugin"
            displayName = "AutoJs6 Properties Plugin"
            description = "Provides properties helpers."
        }
        register("jvmConvention") {
            id = "org.autojs.build.jvm-convention"
            implementationClass = "org.autojs.build.JvmConventionPlugin"
            displayName = "AutoJs6 JVM Convention Plugin"
            description = "Configures Java/Kotlin targets for Android modules using central Versions."
        }
    }
}

System.getProperty("gradle.java.version.select").toInt().let { jdk ->
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(jdk))
        }
    }
    kotlin {
        jvmToolchain(jdk)
    }
}

System.getProperty("gradle.jvm.target.effective").let { jvm ->
    java {
        sourceCompatibility = JavaVersion.toVersion(jvm)
        targetCompatibility = JavaVersion.toVersion(jvm)
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvm))
        }
    }
}
