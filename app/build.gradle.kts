import com.android.apksig.ApkVerifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.slate.studio"

// Minimum host build (roadmap 5.5, D23): 6641, the first local host build after the baseline 6640 (host master
// 340a0b511e, appendix F); roadmap P1.5 confirmed it as the local build that carries the media studio contract and the
// script modules (P1.1 - P1.5), P5.4 writes the final value. Keep it in sync with
// ThreeSlateStudioContract.REQUIRED_HOST_VERSION and .readme/common.json.
// zh-CN: 最低宿主构建号 (路线图 5.5, D23): 6641, 即基线 6640 (宿主 master 340a0b511e, 附录 F) 之后的第一个本地宿主构建;
// 路线图 P1.5 已确认它是含媒体工作室契约与脚本模块 (P1.1 - P1.5) 的本地构建号, P5.4 回填最终值.
// 与 ThreeSlateStudioContract.REQUIRED_HOST_VERSION 及 .readme/common.json 保持一致.
val requiredHostVersion = 6641

val buildTypeDebug = "debug"
val buildTypeRelease = "release"

fun File.sha256(): String = inputStream().buffered().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

// Lock files (new repository specification 5.5): `format=1` first, unique keys, comments and blank lines skipped.
// zh-CN: 锁文件 (新仓库规范 5.5): 首行 `format=1`, 键唯一, 跳过注释与空行.
fun readLock(path: String): Map<String, String> {
    val lockFile = rootProject.file(path)
    check(lockFile.isFile) { "Missing $path" }
    val entries = LinkedHashMap<String, String>()
    lockFile.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
        val key = line.substringBefore('=').trim()
        check(line.contains('=') && entries.put(key, line.substringAfter('=').trim()) == null) { "Malformed or duplicate entry in $path: $line" }
    }
    check(entries["format"] == "1") { "Unsupported lock format in $path" }
    return entries
}

fun verifiedFile(path: String, expected: String?, label: String): File {
    val file = rootProject.file(path)
    check(file.isFile) { "Missing $path ($label)" }
    check(expected != null && expected.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256 for $label" }
    val actual = file.sha256()
    check(actual == expected) { "SHA-256 mismatch for $path: expected $expected, actual $actual" }
    return file
}

// The host contract AARs (roadmap 5.5): media-studio-api (roadmap P1.1) and common-plugin-api. Release AARs only, staged
// under libs/ and pinned by locks/host-api-aars.lock; libs/README.md records where each one comes from.
// zh-CN: 宿主契约 AAR (路线图 5.5): media-studio-api (路线图 P1.1) 与 common-plugin-api. 只接受 release AAR, 暂存于 libs/ 并由
// locks/host-api-aars.lock 固定; 来源见 libs/README.md.
val hostApiAars: List<File> = run {
    val entries = readLock("locks/host-api-aars.lock")
    val names = entries.keys.filter { it.endsWith(".file") }.map { it.removeSuffix(".file") }.sorted()
    check(entries.keys == setOf("format") + names.flatMap { listOf("$it.file", "$it.sha256") }) { "Unexpected keys in locks/host-api-aars.lock" }
    names.map { name ->
        val fileName = entries.getValue("$name.file")
        check(fileName == File(fileName).name && fileName.endsWith(".aar") && !fileName.endsWith("-debug.aar")) { "Invalid $name.file" }
        verifiedFile("libs/$fileName", entries["$name.sha256"], name)
    }
}

android {
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Identity strings come from the build and are never translated (new repository specification 11).
        // zh-CN: 身份字符串来自构建, 不翻译 (新仓库规范第 11 节).
        resValue("string", "app_name", "3-Slate Studio")
        // Brand name answered as PluginInfo.name; never translated (roadmap 5.5). zh-CN: PluginInfo.name 使用的品牌名, 不翻译 (路线图 5.5).
        resValue("string", "plugin_name", "3-Slate Studio")
        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_id", "three-slate-studio")
        resValue("string", "plugin_engine", "media-studio")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_requires_host_version", "$requiredHostVersion")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    lint {
        abortOnError = true
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        resValues = true
    }

    // Single universal APK (roadmap D25): no `splits` block, PluginInfo.supportedAbis is an explicit empty array, and the
    // only native code is the Compose library listed in `allowedNativeLibraries` below.
    // zh-CN: 单个 universal APK (路线图 D25): 没有 `splits` 块, PluginInfo.supportedAbis 为显式空数组, 唯一的原生代码是下方
    // `allowedNativeLibraries` 列出的 Compose 库.
    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }

    bundle {
        language.enableSplit = false
        density.enableSplit = false
        abi.enableSplit = false
    }
}

// Native code inventory (roadmap D25, P0.1): the plugin has no native code of its own and Media3 brings none, but Compose
// ui-graphics depends on androidx.graphics:graphics-path, whose libandroidx.graphics.path.so is packaged for four ABIs
// (the 3-Maple Image finding). The APK stays one universal package; the shared gate enforces 16 KB alignment for the
// 64-bit ABIs and appendDigestToReleasedFiles asserts that exactly these libraries are present and stored uncompressed.
// zh-CN: 原生代码清单 (路线图 D25, P0.1): 插件自身没有原生代码, Media3 也不带, 但 Compose ui-graphics 依赖
// androidx.graphics:graphics-path, 其 libandroidx.graphics.path.so 打包了四个 ABI (3-Maple Image 的发现). APK 保持单个 universal 包;
// 共享门禁对 64 位 ABI 强制 16 KB 对齐, appendDigestToReleasedFiles 断言恰好包含这些库且以不压缩方式存储.
val allowedNativeLibraries = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86").map { "lib/$it/libandroidx.graphics.path.so" }
nativeAlignment { expectNoNativeLibraries.set(false) }

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    implementation(files(hostApiAars))

    implementation(libs.core.ktx)

    // Jetpack Compose (roadmap D25, P4): the BOM is the single version source; activity-compose follows it.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Media3 (roadmap D25, D29): processing with Transformer / Effect / Muxer, previews with ExoPlayer / UI.
    implementation(libs.media3.common)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.muxer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Material Color Utilities (MaterialKolor, MIT): the HCT tone rule of the theme accent of the Studio screens
    // (standalone settings specification section 6, roadmap P4.1), the same artifact as 3-Prism Camera.
    // zh-CN: Material Color Utilities (MaterialKolor, MIT): Studio 界面主题色的 HCT 色调规则 (独立设置规范第 6 节, 路线图 P4.1),
    // 与 3-Prism Camera 相同.
    implementation(libs.material.color.utilities)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    // Compose UI tests pull Espresso 3.5.0, whose idling sync reflects on InputManager.getInstance (gone on API 36).
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

// Runtime dependency lock (roadmap D39, P0.1): every Maven artifact the release APK packages, as `group:artifact=version`.
// writeRuntimeDependencyLock rewrites it after a dependency change; verifyRuntimeDependencyLock (run by `check` and by
// every release build) fails when the resolved graph differs, so THIRD_PARTY_NOTICES.md, which
// .python/generate_third_party_notices.py renders from this lock, cannot drift from what ships.
// zh-CN: 运行时依赖锁 (路线图 D39, P0.1): release APK 打包的全部 Maven 产物, 每行 `group:artifact=version`. 依赖变更后由
// writeRuntimeDependencyLock 重写; verifyRuntimeDependencyLock (`check` 与每次 release 构建都会运行) 在解析结果不一致时失败,
// 因此由 .python/generate_third_party_notices.py 依据此锁生成的 THIRD_PARTY_NOTICES.md 不会与实际打包内容漂移.
val runtimeDependencyLock: File = rootProject.file("locks/runtime-dependencies.lock")

fun runtimeDependencyLockText(modules: List<String>): String = buildString {
    append("# Maven artifacts packaged into the release APK (roadmap D39, P0.1), from the releaseRuntimeClasspath configuration.\n")
    append("# Generated by `gradlew :app:writeRuntimeDependencyLock`; `:app:verifyRuntimeDependencyLock` (run by `check` and by\n")
    append("# release builds) fails when the resolved graph differs. After a change, run `py .python/generate_third_party_notices.py`.\n")
    append("format=1\n")
    modules.forEach { append(it).append('\n') }
}

fun Task.releaseRuntimeModules(): Provider<List<String>> = project.configurations.named("releaseRuntimeClasspath").flatMap { configuration ->
    configuration.incoming.artifacts.resolvedArtifacts.map { artifacts ->
        // The staged host AARs are file dependencies (locks/host-api-aars.lock) and carry no module identifier.
        // zh-CN: 暂存的宿主 AAR 是文件依赖 (locks/host-api-aars.lock), 没有模块标识.
        artifacts.mapNotNull { artifact ->
            (artifact.id.componentIdentifier as? ModuleComponentIdentifier)?.let { "${it.group}:${it.module}=${it.version}" }
        }.distinct().sortedBy { it.substringBefore('=') }
    }
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register("writeRuntimeDependencyLock") {
        group = "build setup"
        description = "Rewrites locks/runtime-dependencies.lock from the release runtime classpath"
        val modules = releaseRuntimeModules()
        val lockFile = runtimeDependencyLock
        outputs.file(lockFile)
        outputs.upToDateWhen { false }
        doLast {
            lockFile.writeText(runtimeDependencyLockText(modules.get()), Charsets.UTF_8)
            println("Wrote ${modules.get().size} modules to $lockFile")
        }
    }

    register("verifyRuntimeDependencyLock") {
        group = "verification"
        description = "Fails when locks/runtime-dependencies.lock differs from the release runtime classpath"
        val modules = releaseRuntimeModules()
        val lockFile = runtimeDependencyLock
        inputs.file(lockFile)
        mustRunAfter("writeRuntimeDependencyLock")
        doLast {
            val locked = lockFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") && it != "format=1" }
            val resolved = modules.get()
            check(locked == resolved && lockFile.readText() == runtimeDependencyLockText(resolved)) {
                "locks/runtime-dependencies.lock is stale: added ${resolved - locked.toSet()}, removed ${locked - resolved.toSet()}. " +
                    "Run `gradlew :app:writeRuntimeDependencyLock`, then `py .python/generate_third_party_notices.py`."
            }
        }
    }

    named("check") { dependsOn("verifyRuntimeDependencyLock") }
    matching { it.name == "preReleaseBuild" }.configureEach { dependsOn("verifyRuntimeDependencyLock") }

    // Single signed release APK (roadmap D25, the 3-Chime Alarm form): exactly one APK, a valid signature, exactly the native
    // libraries of `allowedNativeLibraries` stored uncompressed, then `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk` in
    // releases/.
    // zh-CN: 单个已签名 release APK (路线图 D25, 3-Chime Alarm 形态): 恰好一个 APK, 签名有效, 原生库恰为 `allowedNativeLibraries`
    // 且不压缩存储, 然后以 `{ROOT_PROJECT_NAME}-v{VERSION_NAME}-{CRC32}.apk` 放入 releases/.
    register("appendDigestToReleasedFiles") {
        group = "distribution"
        description = "Builds and verifies the single signed release APK of 3-Slate Studio, then appends its CRC32 digest"
        dependsOn("assembleRelease")
        doLast {
            check(signs.isValid) { "Release signing is not configured (sign.properties); refusing to collect an unsigned APK" }
            val extension = utils.FILE_EXTENSION_APK
            val prefix = "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}".lowercase()
            val expectedName = "$prefix.$extension"
            val source = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease").get().asFile
            val apks = source.listFiles { candidate -> candidate.extension == extension }.orEmpty()
            check(apks.map { it.name } == listOf(expectedName)) {
                "Unexpected release APK set in $source: expected [$expectedName], actual ${apks.map { it.name }.sorted()}"
            }
            val apk = apks.single()
            val verification = ApkVerifier.Builder(apk).build().verify()
            check(verification.isVerified) { "Invalid or unsigned release APK ${apk.name}: ${verification.errors}" }
            ZipFile(apk).use { zip ->
                val natives = zip.entries().asSequence().filter { it.name.startsWith("lib/") || it.name.endsWith(".so") }.toList()
                check(natives.map { it.name }.sorted() == allowedNativeLibraries.sorted()) {
                    "Roadmap D25: unexpected native library set in ${apk.name}: ${natives.map { it.name }.sorted()}"
                }
                check(natives.all { it.method == ZipEntry.STORED }) { "Native libraries must be stored uncompressed" }
            }
            val collectedName = "$prefix-${utils.digestCRC32(apk)}.$extension"
            val destination = file("$rootDir/${buildTypeRelease}s")
            check(destination.isDirectory || destination.mkdirs()) { "Cannot create $destination" }
            apk.copyTo(destination.resolve(collectedName), overwrite = true)
            destination.listFiles { file -> file.name.startsWith("$prefix-") && file.extension == extension }
                .orEmpty()
                .filter { it.name != collectedName }
                .forEach { stale -> check(stale.delete()) { "Cannot remove superseded artifact: $stale" } }
            println("Destination: $destination")
            println(collectedName)
        }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}
