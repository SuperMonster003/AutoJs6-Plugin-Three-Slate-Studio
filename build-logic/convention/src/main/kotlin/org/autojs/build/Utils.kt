package org.autojs.build

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32

object Utils {

    const val FILE_EXTENSION_APK = "apk"

    private object LOGGER {
        const val LEVEL_INFO = 1
        const val LEVEL_WARN = 2
        const val LEVEL_ERROR = 3
    }

    private var CURRENT_LOGGER_LEVEL = LOGGER.LEVEL_ERROR

    @JvmOverloads
    fun newFormatted(title: String, contents: Collection<String> = emptyList(), subtitle: String? = null) = Formatted(title, contents, subtitle)

    fun newVersions(project: Project) = Versions(project)

    fun newSigns(project: Project) = Signs(project)

    fun newProperties(project: Project) = BuildProperties.loadFrom(project)

    fun hours2Millis(hour: Double) = hour * 3.6e6

    fun getDateString(format: String, zone: String): String {
        // e.g. May 23, 2011
        return SimpleDateFormat(format, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(zone)
        }.format(Date())
    }

    fun getAssembleTaskName(flavorName: String, buildType: String) = "assemble${capitalize(flavorName)}${capitalize(buildType)}"

    fun getAssembleFullTaskName(projectName: String, flavorName: String, buildType: String) = ":$projectName:${getAssembleTaskName(flavorName, buildType)}"

    fun digestCRC32(file: File): String {
        val buffer = ByteArray(4096)
        return FileInputStream(file).use { input ->
            CRC32().let { digest ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
                String.format("%08x", digest.value)
            }
        }
    }

    inline fun <reified T> org.gradle.api.plugins.ExtraPropertiesExtension.getOrNull(key: String): T? {
        if (!has(key)) return null
        val result = get(key)
        require(result is T?) {
            "The type of $key is ${result?.javaClass?.name}, but ${T::class.java.name} is required"
        }
        return result
    }

    inline fun <reified T> org.gradle.api.plugins.ExtraPropertiesExtension.require(key: String): T {
        require(has(key)) {
            "The key $key is not found in extra properties"
        }
        val result = get(key)
        require(result is T) {
            "The type of $key is ${result?.javaClass?.name}, but ${T::class.java.name} is required"
        }
        return result
    }

    private fun capitalize(s: String) = s.replaceFirstChar { it.uppercase(Locale.getDefault()) }

    private fun parseBooleanOrNull(value: String?): Boolean? = when (value?.trim()?.lowercase(Locale.getDefault())) {
        "1", "true", "yes", "on" -> true
        "0", "false", "no", "off" -> false
        else -> null
    }

    private fun parseMajorVersionOrNull(version: String?): Int? {
        if (version.isNullOrBlank()) return null
        return version.substringBefore('.').substringBefore('-').toIntOrNull()
    }

    private fun detectAgpVersionOrNull(): String? = runCatching {
        Class.forName("com.android.Version")
            .getField("ANDROID_GRADLE_PLUGIN_VERSION")
            .get(null) as? String
    }.getOrNull()

    private fun shouldUseBuiltInKotlin(project: Project): Boolean {
        parseBooleanOrNull(project.providers.gradleProperty("useBuiltInKotlin").orNull)?.let { overridden ->
            project.logInfo("[JvmConv] useBuiltInKotlin overridden by gradle property to '$overridden' for '${project.path}'")
            return overridden
        }

        val agpVersion = detectAgpVersionOrNull()
        parseMajorVersionOrNull(agpVersion)?.let { agpMajor ->
            val useBuiltIn = agpMajor >= 9
            project.logInfo(
                "[JvmConv] AGP version='$agpVersion' (major=$agpMajor), " +
                        "useBuiltInKotlin='$useBuiltIn' for '${project.path}'"
            )
            return useBuiltIn
        }

        val gradleVersion = project.gradle.gradleVersion
        val gradleMajor = parseMajorVersionOrNull(gradleVersion) ?: 0
        val useBuiltIn = gradleMajor >= 9
        project.logWarn(
            "[JvmConv] Failed to determine AGP version; fallback to Gradle version='$gradleVersion' " +
                    "(major=$gradleMajor), useBuiltInKotlin='$useBuiltIn' for '${project.path}'"
        )
        return useBuiltIn
    }

    private fun applyKotlinAndroidPluginIfNeeded(project: Project) {
        val kotlinAndroidPluginId = "org.jetbrains.kotlin.android"
        if (project.plugins.hasPlugin(kotlinAndroidPluginId)) {
            project.logInfo("[JvmConv] '$kotlinAndroidPluginId' already applied for '${project.path}'")
            return
        }
        if (shouldUseBuiltInKotlin(project)) {
            project.logInfo("[JvmConv] Skip applying '$kotlinAndroidPluginId' (AGP built-in Kotlin) for '${project.path}'")
            return
        }

        runCatching {
            project.pluginManager.apply(kotlinAndroidPluginId)
        }.onSuccess {
            project.logInfo("[JvmConv] Applied '$kotlinAndroidPluginId' for '${project.path}'")
        }.onFailure {
            project.logError("[JvmConv] Failed to apply '$kotlinAndroidPluginId' for '${project.path}'", it)
            throw it
        }
    }

    // Configure Java/Kotlin target version uniformly for Android modules:
    // - Android.compileOptions.sourceCompatibility/targetCompatibility = versions.javaVersion
    // - Kotlin compile tasks jvmTarget = versions.javaVersion level
    // Note:
    // - Android extension is stably available after projectsEvaluated
    // - Kotlin tasks use tasks.configureEach for dynamic configuration to catch whenever tasks are created
    //
    // zh-CN:
    //
    // 统一为 Android 模块配置 Java/Kotlin 的目标版本:
    // - Android.compileOptions.sourceCompatibility/targetCompatibility = versions.javaVersion
    // - Kotlin 编译任务的 jvmTarget = versions.javaVersion 对应级别
    // 注意:
    // - Android 扩展在 projectsEvaluated 后稳定可得
    // - Kotlin 任务用 tasks.configureEach 动态配置, 任务何时创建都能命中
    @JvmStatic
    fun configureJvmForAndroidModule(project: Project) {
        val versions = newVersions(project)
        project.logInfo("[JvmConv] Enter configureJvmForAndroidModule for module='${project.path}', javaVersion='${versions.javaVersion}', javaVersionString='${versions.javaVersionString}'")

        val installer = {
            project.logInfo("[JvmConv] Detected Android plugin in module='${project.path}', installing configuration")
            applyKotlinAndroidPluginIfNeeded(project)

            System.getProperty("gradle.java.version.select").let { ver ->
                configureJavaToolchainLanguageLevel(project, ver)
                configureJavaToolchainForAllJavaCompile(project, ver)
                configureKotlinJvmToolchain(project, ver)
            }

            System.getProperty("gradle.jvm.target.effective").let { ver ->
                configureAndroidCompileOptions(project, ver)
                configureKotlinJvmTargetLazily(project, ver)
            }

            project.logInfo("[JvmConv] Installed Java toolchain (module+tasks) and Kotlin jvmTarget for '${project.path}'")
        }

        project.plugins.withId("com.android.application") { installer() }
        project.plugins.withId("com.android.library") { installer() }
    }

    // Module-level toolchain: Let AGP/Gradle know which JDK language level should be used for this module.
    // zh-CN: 模块级 Toolchain: 让 AGP/Gradle 知道本模块应使用的 JDK 语言级别.
    private fun configureJavaToolchainLanguageLevel(project: Project, target: String) {
        val javaExt = project.extensions.findByType(JavaPluginExtension::class.java)
        if (javaExt == null) {
            project.logWarn("[JvmConv] JavaPluginExtension not found in '${project.path}', skip module-level toolchain")
            return
        }
        runCatching {
            javaExt.toolchain.languageVersion.set(JavaLanguageVersion.of(target))
        }.onSuccess {
            project.logInfo("[JvmConv] Module-level toolchain languageVersion set to $target for '${project.path}'")
        }.onFailure {
            project.logError("[JvmConv] Set module-level toolchain languageVersion failed on '${project.path}': ${it.message}", it)
        }
    }

    private fun configureKotlinJvmToolchain(project: Project, target: String) {
        val kotlinExt = project.extensions.findByName("kotlin")
        if (kotlinExt == null) {
            project.logWarn("[JvmConv] Kotlin extension not found in '${project.path}', skip Kotlin jvmToolchain")
            return
        }

        runCatching {
            kotlinExt.javaClass.getMethod("jvmToolchain", Int::class.java)
                .invoke(kotlinExt, target.toInt())
        }.onSuccess {
            project.logInfo("[JvmConv] Kotlin jvmToolchain set to $target for '${project.path}'")
        }.onFailure {
            project.logError("[JvmConv] Set Kotlin jvmToolchain failed on '${project.path}': ${it.message}", it)
        }
    }

    // Task-level Toolchain: Specify javaCompiler for all JavaCompile tasks, without setting --release (prohibited by AGP).
    // zh-CN: 任务级 Toolchain: 对所有 JavaCompile 指定 javaCompiler, 且不要设置 --release (AGP 禁止).
    private fun configureJavaToolchainForAllJavaCompile(project: Project, target: String) {
        val toolchains = runCatching {
            project.extensions.getByType(JavaToolchainService::class.java)
        }.onFailure {
            project.logError("[JvmConv] JavaToolchainService not available on '${project.path}': ${it.message}", it)
        }.getOrNull() ?: return

        val langVersion = JavaLanguageVersion.of(target)
        project.tasks.withType(JavaCompile::class.java).configureEach(object : Action<JavaCompile> {
            override fun execute(t: JavaCompile) {
                runCatching {
                    val compilerProvider = toolchains.compilerFor { languageVersion.set(langVersion) }
                    t.javaCompiler.set(compilerProvider)
                    project.logInfo("[JvmConv] JavaCompile '${t.path}' uses toolchain JDK $target (no --release)")
                }.onFailure {
                    project.logError("[JvmConv] '${t.path}' set javaCompiler(toolchain) failed: ${it.message}", it)
                }
            }
        })
    }

    // 将 Android 的 compileOptions.sourceCompatibility/targetCompatibility 设置为给定目标 (如 "24"), 确保与 Kotlin 一致
    // zh-CN: 使用反射避免对 AGP 具体类型的直接依赖; 兼容 application/library.
    private fun configureAndroidCompileOptions(project: Project, target: String) {
        val androidExt = project.extensions.findByName("android") ?: run {
            project.logWarn("[JvmConv] Android extension not found in '${project.path}', skip compileOptions alignment")
            return
        }
        runCatching {
            val compileOptions = androidExt.javaClass.methods
                .firstOrNull { it.name == "getCompileOptions" && it.parameterTypes.isEmpty() }
                ?.invoke(androidExt) ?: return@runCatching project.logWarn("[JvmConv] compileOptions not found on android extension for '${project.path}'")

            val javaVersionClass = Class.forName("org.gradle.api.JavaVersion")
            val javaVersion = JavaVersion.toVersion(target)

            val setSource = compileOptions.javaClass.methods.firstOrNull {
                it.name == "setSourceCompatibility" && it.parameterTypes.size == 1 && it.parameterTypes[0] == javaVersionClass
            } ?: throw IllegalStateException("setSourceCompatibility(JavaVersion) not found")
            val setTarget = compileOptions.javaClass.methods.firstOrNull {
                it.name == "setTargetCompatibility" && it.parameterTypes.size == 1 && it.parameterTypes[0] == javaVersionClass
            } ?: throw IllegalStateException("setTargetCompatibility(JavaVersion) not found")

            setSource.invoke(compileOptions, javaVersion)
            setTarget.invoke(compileOptions, javaVersion)

            project.logInfo("[JvmConv] Android compileOptions source/target set to Java $target for '${project.path}'")
        }.onFailure {
            project.logError("[JvmConv] Configure Android compileOptions failed on '${project.path}': ${it.message}", it)
        }
    }

    // Configure jvmTarget for KotlinCompile tasks:
    // Use configureEach for lazy configuration on task creation;
    // Compatible with old and new APIs.
    // zh-CN:
    // 为 KotlinCompile 任务设置 jvmTarget:
    // 使用 configureEach, 任务实现时自动应用;
    // 兼容新旧 API.
    private fun configureKotlinJvmTargetLazily(project: Project, target: String) {
        val desiredStr = target // e.g. "22"
        val desiredEnumName = "JVM_${desiredStr}" // e.g. "JVM_22"
        project.logInfo("[JvmConv] Will configure Kotlin jvmTarget lazily to '$desiredEnumName' in '${project.path}'")

        project.tasks.configureEach(object : Action<Task> {
            override fun execute(task: Task) {
                if (!isKotlinCompileTask(task, project)) return

                project.logInfo("[JvmConv] <KotlinTask> '${task.path}' class='${task.javaClass.name}'")

                // Try Kotlin 2.x first: compilerOptions.jvmTarget(Property<JvmTarget>).
                // zh-CN: 优先尝试 Kotlin 2.x: compilerOptions.jvmTarget(Property<JvmTarget>).
                val compilerOptions = runCatching {
                    task.javaClass.methods.firstOrNull { it.name == "getCompilerOptions" && it.parameterTypes.isEmpty() }
                        ?.also { project.logInfo("[JvmConv] '${task.path}' found method: ${it.toGenericString()}") }
                        ?.invoke(task)
                }.onFailure {
                    project.logError("[JvmConv] '${task.path}' getCompilerOptions() failed", it)
                }.getOrNull()

                if (compilerOptions != null) {
                    project.logInfo("[JvmConv] '${task.path}' compilerOptions class='${compilerOptions.javaClass.name}'")
                    val jvmTargetProp = runCatching {
                        compilerOptions.javaClass.methods.firstOrNull { it.name == "getJvmTarget" && it.parameterTypes.isEmpty() }
                            ?.also { project.logInfo("[JvmConv] '${task.path}' found method: ${it.toGenericString()}") }
                            ?.invoke(compilerOptions)
                    }.onFailure {
                        project.logError("[JvmConv] '${task.path}' compilerOptions.getJvmTarget() failed", it)
                    }.getOrNull()

                    if (jvmTargetProp != null) {
                        project.logInfo("[JvmConv] '${task.path}' jvmTarget property class='${jvmTargetProp.javaClass.name}'")
                        val propSet = jvmTargetProp.javaClass.methods.firstOrNull {
                            it.name == "set" && it.parameterTypes.size == 1
                        }
                        project.logInfo("[JvmConv] '${task.path}' Property.set method: ${propSet?.toGenericString() ?: "NOT_FOUND"}")

                        val jvmTargetEnum = runCatching {
                            val enumClass = Class.forName("org.jetbrains.kotlin.gradle.dsl.JvmTarget")
                            enumClass.enumConstants?.firstOrNull { it.toString().equals(desiredEnumName, ignoreCase = true) }
                                ?.also { project.logInfo("[JvmConv] '${task.path}' resolved enum '$desiredEnumName' = $it") }
                        }.onFailure {
                            project.logError("[JvmConv] '${task.path}' resolve enum '$desiredEnumName' failed", it)
                        }.getOrNull()

                        if (propSet != null && jvmTargetEnum != null) {
                            runCatching { propSet.invoke(jvmTargetProp, jvmTargetEnum) }
                                .onSuccess {
                                    project.logInfo("[JvmConv] '${task.path}' jvmTarget set via compilerOptions to '$desiredEnumName'")
                                    return
                                }
                                .onFailure {
                                    project.logWarn("[JvmConv] '${task.path}' jvmTarget set via compilerOptions failed, will try legacy API.\ne: $it")
                                }
                        } else {
                            project.logWarn("[JvmConv] '${task.path}' compilerOptions path unavailable (propSet=$propSet, enum=$jvmTargetEnum), trying legacy kotlinOptions")
                        }
                    } else {
                        project.logWarn("[JvmConv] '${task.path}' compilerOptions.getJvmTarget() returned null, trying legacy kotlinOptions")
                    }
                } else {
                    project.logWarn("[JvmConv] '${task.path}' compilerOptions not found, trying legacy kotlinOptions")
                }

                // Fallback to legacy API: kotlinOptions.setJvmTarget(String).
                // zh-CN: 兼容旧 API: kotlinOptions.setJvmTarget(String).
                val kotlinOptions = runCatching {
                    task.javaClass.methods.firstOrNull { it.name == "getKotlinOptions" && it.parameterTypes.isEmpty() }
                        ?.also { project.logInfo("[JvmConv] '${task.path}' found method: ${it.toGenericString()}") }
                        ?.invoke(task)
                }.onFailure {
                    project.logError("[JvmConv] '${task.path}' getKotlinOptions() failed", it)
                }.getOrNull()

                if (kotlinOptions != null) {
                    project.logInfo("[JvmConv] '${task.path}' kotlinOptions class='${kotlinOptions.javaClass.name}'")
                    val setJvmTarget = kotlinOptions.javaClass.methods.firstOrNull {
                        it.name == "setJvmTarget" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java
                    }
                    project.logInfo("[JvmConv] '${task.path}' kotlinOptions.setJvmTarget method: ${setJvmTarget?.toGenericString() ?: "NOT_FOUND"}")

                    runCatching { setJvmTarget?.invoke(kotlinOptions, desiredStr) }
                        .onSuccess { project.logInfo("[JvmConv] '${task.path}' jvmTarget set via kotlinOptions to '$desiredStr'") }
                        .onFailure { project.logError("[JvmConv] '${task.path}' jvmTarget set via kotlinOptions failed", it) }
                } else {
                    project.logWarn("[JvmConv] '${task.path}' kotlinOptions not found; jvmTarget not configured")
                }
            }
        })
    }

    private fun isKotlinCompileTask(task: Task, project: Project): Boolean {
        val name = task.name.lowercase(Locale.getDefault())
        val clsName = task.javaClass.name
        val hasCompilerOptions = task.javaClass.methods.any { it.name == "getCompilerOptions" && it.parameterTypes.isEmpty() }
        val hasKotlinOptions = task.javaClass.methods.any { it.name == "getKotlinOptions" && it.parameterTypes.isEmpty() }
        val matched = when {
            name.contains("kotlin") && name.contains("compile") -> true
            clsName.contains("Kotlin", ignoreCase = true) && clsName.contains("Compile", ignoreCase = true) -> true
            hasCompilerOptions || hasKotlinOptions -> true
            else -> false
        }
        if (matched) {
            project.logInfo("[JvmConv] Task matched as KotlinCompile: path='${task.path}', class='$clsName'")
        }
        return matched
    }

    private fun Project.logInfo(msg: String) {
        if (CURRENT_LOGGER_LEVEL <= LOGGER.LEVEL_INFO) {
            logger.lifecycle(msg)
        }
    }

    private fun Project.logWarn(msg: String) {
        if (CURRENT_LOGGER_LEVEL <= LOGGER.LEVEL_WARN) {
            logger.warn(msg)
        }
    }

    private fun Project.logError(msg: String, t: Throwable? = null) {
        if (CURRENT_LOGGER_LEVEL <= LOGGER.LEVEL_ERROR) {
            if (t != null) logger.error(msg, t) else logger.error(msg)
        }
    }

}
