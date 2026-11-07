package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import io.github.supermonster003.autojs6.plugin.three.slate.studio.RepositoryFiles
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Fit
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.ImageFormat
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationSpec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OverlayItem
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import org.autojs.plugin.mediastudio.api.MediaStudioErrorCodes
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.file.Files

/**
 * The wizard schemas against the plugin's validators (roadmap P4.2: the form fields match the script options): every
 * field's bounds, choices, conditions, alternatives and required flag go through `OperationOptions.parse`, the validator
 * sources name no key the schemas lack, the documented defaults are the parsed ones, and the committed declarations equal
 * the rendering of the schemas.
 * zh-CN: 向导模式对照插件校验器 (路线图 P4.2: 表单字段与脚本选项一一对应): 每个字段的边界, 选项, 条件, 互斥与必填都经
 * `OperationOptions.parse` 核对, 校验器源码中的键模式里都有, 文档默认值即解析后的值, 入库的声明等于模式的渲染结果.
 */
class OptionSchemasTest {

    private sealed interface Outcome {
        data object Accepted : Outcome
        data class Rejected(val code: String, val detail: String) : Outcome
    }

    private fun contractOp(schema: OperationSchema) =
        if (schema.op == OperationSchema.SUBTITLES_CONVERT) MediaStudioOps.SUBTITLES_WRITE else schema.op

    private fun inputs(schema: OperationSchema) = when (schema.op) {
        OperationSchema.SUBTITLES_CONVERT -> 0
        else -> schema.inputs.min
    }

    /** Options that satisfy every requirement of the operation except the field under test. zh-CN: 除被测字段外满足全部要求的选项. */
    private fun minimal(schema: OperationSchema): Map<String, Any?> = when (schema.op) {
        MediaStudioOps.TRIM, MediaStudioOps.AUDIO_TRIM -> mapOf("start" to 0)
        MediaStudioOps.SPEED, MediaStudioOps.AUDIO_SPEED -> mapOf("factor" to 1.0)
        MediaStudioOps.TRANSFORM -> mapOf("flip" to "horizontal", "rotate" to 90)
        MediaStudioOps.OVERLAY -> mapOf("overlays" to listOf(mapOf("type" to "text", "text" to "a")))
        MediaStudioOps.FRAMES, MediaStudioOps.FRAME -> mapOf("at" to 0.0)
        MediaStudioOps.AUDIO_VOLUME -> mapOf("gain" to 1.0)
        MediaStudioOps.AUDIO_CONVERT -> mapOf("format" to "m4a")
        MediaStudioOps.BURN_SUBTITLES -> mapOf("subtitles" to "/sdcard/a.srt")
        OperationSchema.SUBTITLES_CONVERT -> mapOf("format" to "srt", "cues" to listOf(mapOf("start" to 0, "end" to 1000, "text" to "a")))
        else -> emptyMap()
    }

    /** What the alternatives of an exclusive group need besides the field under test. zh-CN: 互斥组各选择除被测字段外所需的键. */
    private fun alternativeBase(schema: OperationSchema, field: OptionField): Map<String, Any?> = when {
        schema.op == MediaStudioOps.FRAMES && field.alternative == "range" && field.key != "count" && field.key != "every" -> mapOf("count" to 1)
        else -> emptyMap()
    }

    private fun parse(schema: OperationSchema, options: Map<String, Any?>): Outcome = try {
        OperationOptions.parse(contractOp(schema), inputs(schema), options)
        Outcome.Accepted
    } catch (e: MediaStudioException) {
        Outcome.Rejected(e.code, e.detail)
    }

    /** The base options with [value] at the path of the field. zh-CN: 在字段路径上放入 [value] 后的基础选项. */
    private fun interface Placer {
        fun place(value: Any?): Map<String, Any?>
    }

    private fun expectAccepted(label: String, outcome: Outcome, tolerateLimit: Boolean = false) {
        if (outcome is Outcome.Rejected && !(tolerateLimit && outcome.code == MediaStudioErrorCodes.LIMIT_EXCEEDED)) {
            fail("$label: expected accepted, got ${outcome.code}: ${outcome.detail}")
        }
    }

    private fun expectRejected(label: String, outcome: Outcome) {
        if (outcome == Outcome.Accepted) fail("$label: expected rejected, was accepted")
    }

    private var checks = 0

    /** Bounds, choices and kinds of one field. zh-CN: 一个字段的边界, 选项与类型. */
    private fun checkField(label: String, field: OptionField, placer: Placer, schema: OperationSchema, tolerateLimitAtMax: Boolean = false) {
        fun ok(value: Any?, tolerate: Boolean = false) { checks++; expectAccepted("$label=$value", parse(schema, placer.place(value)), tolerate) }
        fun no(value: Any?) { checks++; expectRejected("$label=$value", parse(schema, placer.place(value))) }
        when (val type = field.type) {
            is FieldType.Integer -> if (field.uiChoices != null) {
                field.uiChoices.forEach { ok(it) }
                no(45)
            } else {
                ok(type.min)
                ok(type.max, tolerateLimitAtMax)
                if (type.min > -OperationOptions.MAX_TIME_MILLIS) no(type.min - 1)
                if (type.max < OperationOptions.MAX_TIME_MILLIS) no(type.max + 1)
                no(type.min + 0.5)
                no("1")
            }
            is FieldType.Decimal -> {
                if (type.minExclusive) {
                    no(type.min)
                    ok(type.min + minOf(1.0, (type.max - type.min) / 2))
                } else {
                    ok(type.min)
                    no(type.min - 0.5)
                }
                ok(type.max, tolerateLimitAtMax)
                if (type.max < OperationOptions.MAX_TIME_MILLIS.toDouble()) no(type.max + 0.5)
                no("x")
            }
            is FieldType.Choice -> {
                type.values.forEach { ok(it) }
                no("bogus")
            }
            is FieldType.IntChoice -> {
                type.values.forEach { ok(it) }
                no(type.values.max() + 1)
            }
            FieldType.Flag -> {
                ok(true)
                ok(false)
                no("yes")
            }
            is FieldType.FlagOrInteger -> {
                ok(true)
                ok(false)
                ok(type.min)
                ok(type.max)
                no(type.max + 1)
                no(type.min - 1)
            }
            is FieldType.FlagOrDecimal -> {
                ok(true)
                ok(type.min)
                ok(type.max)
                no(type.min - 0.5)
                no(type.max + 0.5)
            }
            FieldType.Color -> {
                ok("#FF0000")
                ok("#80FF0000")
                ok("red")
                no("not-a-color")
            }
            is FieldType.Length -> {
                ok(type.maxPixels)
                ok("${type.maxPercent.toInt()}%")
                no(type.maxPixels + 1)
                no("${type.maxPercent.toInt() + 1}%")
                when (type.lower) {
                    LengthLower.SIGNED -> { ok(-type.maxPixels); no(-type.maxPixels - 1) }
                    LengthLower.NON_NEGATIVE -> { ok(0); no(-1) }
                    LengthLower.POSITIVE -> { no(0); ok(1) }
                }
                no("12px")
            }
            is FieldType.Text -> {
                ok("a".repeat(type.minChars))
                ok("a".repeat(type.maxChars))
                no("a".repeat(type.maxChars + 1))
            }
            FieldType.Font -> {
                ok("serif")
                ok("/system/fonts/Roboto-Regular.ttf")
                no("Comic Sans")
            }
            is FieldType.FilePath -> {
                ok(if (type.kind == FileKind.IMAGE) "/sdcard/logo.png" else "/sdcard/a.srt")
                no("relative.png")
            }
            is FieldType.Numbers -> {
                ok(listOf(type.min, type.max))
                no(listOf(type.min - 1))
                no(List(type.maxCount + 1) { it.toDouble() })
            }
            is FieldType.Group, is FieldType.Items, is FieldType.PerInput -> Unit
        }
        if (field.required) {
            checks++
            expectRejected("$label missing", parse(schema, placer.place(MISSING)))
        }
    }

    /** Walks a field list under [prefix], recursing into groups, overlay variants and per-input objects. zh-CN: 逐层遍历字段. */
    private fun walk(schema: OperationSchema, fields: List<OptionField>, label: String, base: Map<String, Any?>, place: (Map<String, Any?>, String, Any?) -> Map<String, Any?>) {
        for (field in fields) {
            val name = "$label.${field.key}"
            when (val type = field.type) {
                is FieldType.Group -> {
                    val inner = minimalGroup(field)
                    walk(schema, type.fields, name, inner) { groupBase, key, value ->
                        place(base, field.key, put(groupBase, key, value))
                    }
                }
                is FieldType.Items -> type.variants.forEach { (variant, variantFields) ->
                    val itemBase = minimalItem(variant)
                    walk(schema, variantFields, "$name[$variant]", itemBase) { item, key, value ->
                        place(base, field.key, listOf(put(item, key, value)))
                    }
                }
                is FieldType.PerInput -> walk(schema, type.fields, "$name[0]", emptyMap()) { track, key, value ->
                    place(base, field.key, List(schema.inputs.min) { index -> if (index == 0) put(track, key, value) else emptyMap() })
                }
                else -> {
                    val conditionBase = field.appliesWhen?.let { condition ->
                        val sibling = fields.first { it.key == condition.key }
                        val choices = (sibling.type as FieldType.Choice).values
                        mapOf(condition.key to choices.first { condition.holds(it) })
                    }.orEmpty()
                    val groupFree = if (field.exclusiveGroup == null) base else base.filterKeys { key ->
                        fields.none { it.key == key && it.exclusiveGroup == field.exclusiveGroup && it.alternative != field.alternative }
                    }
                    val start = groupFree + conditionBase + alternativeBase(schema, field)
                    val tolerate = schema.op == MediaStudioOps.TO_GIF && field.key == "end"
                    checkField(name, field, { value -> place(start, field.key, value) }, schema, tolerate)
                }
            }
        }
    }

    private fun minimalGroup(field: OptionField): Map<String, Any?> = when (field.key) {
        "crop" -> mapOf("width" to 16, "height" to 16)
        else -> emptyMap()
    }

    private fun minimalItem(variant: String): Map<String, Any?> = when (variant) {
        "text" -> mapOf("type" to "text", "text" to "a")
        else -> mapOf("type" to "image", "src" to "/sdcard/logo.png")
    }

    private fun put(map: Map<String, Any?>, key: String, value: Any?): Map<String, Any?> =
        if (value === MISSING) map - key else map + (key to value)

    @Test
    fun `every field of every wizard matches the validator`() {
        for (schema in OptionSchemas.ALL) {
            val base = minimal(schema)
            walk(schema, schema.fields, schema.op, base) { b, key, value -> put(b, key, value) }
            // Unknown keys are refused. zh-CN: 未知键被拒绝.
            checks++
            expectRejected("${schema.op}.bogusKey", parse(schema, base + ("bogusKey" to 1)))
            // The minimal options are valid. zh-CN: 最小选项本身合法.
            checks++
            expectAccepted("${schema.op} minimal", parse(schema, base))
        }
        assertTrue("ran $checks checks", checks > 700)
    }

    @Test
    fun `alternatives of one exclusive group are refused together`() {
        for (schema in OptionSchemas.ALL) {
            val groups = schema.fields.filter { it.exclusiveGroup != null }.groupBy { it.exclusiveGroup }
            for ((group, fields) in groups) {
                val alternatives = fields.groupBy { it.alternative }.values.map { it.first() }
                if (alternatives.size < 2) continue
                val values = alternatives.associate { it.key to sample(it) }
                val outcome = parse(schema, minimal(schema).filterKeys { key -> fields.none { it.key == key } } + values + alternativeBase(schema, alternatives.last()))
                expectRejected("${schema.op} $group ${values.keys}", outcome)
            }
        }
    }

    private fun sample(field: OptionField): Any = when (val type = field.type) {
        is FieldType.Integer -> maxOf(type.min, 16L)
        is FieldType.Decimal -> if (type.minExclusive) type.min + 1 else type.min
        is FieldType.Numbers -> listOf(0.0)
        is FieldType.FlagOrDecimal -> true
        is FieldType.Length -> 16
        else -> error("no sample for ${field.key}")
    }

    @Test
    fun `conditional fields are refused outside their condition`() {
        val extract = OptionSchemas.of(MediaStudioOps.EXTRACT_AUDIO)!!
        expectRejected("bitRate with wav", parse(extract, mapOf("format" to "wav", "bitRate" to 128_000)))
        expectAccepted("bitRate with m4a", parse(extract, mapOf("format" to "m4a", "bitRate" to 128_000)))
        expectRejected("44100 with ogg", parse(extract, mapOf("format" to "ogg", "sampleRate" to 44_100)))
        val thumbnail = OptionSchemas.of(MediaStudioOps.THUMBNAIL)!!
        expectRejected("quality with png", parse(thumbnail, mapOf("format" to "png", "quality" to 50)))
    }

    @Test
    fun `documented defaults are the parsed ones`() {
        fun default(op: String, key: String) = OptionSchemas.of(op)!!.field(key)!!.default
        val concat = OperationOptions.parse(MediaStudioOps.CONCAT, 2, emptyMap()) as OperationSpec.Concat
        assertEquals(default(MediaStudioOps.CONCAT, "fit"), concat.fit.id)
        assertEquals(Fit.CONTAIN, concat.fit)
        assertEquals(default(MediaStudioOps.CONCAT, "codec"), concat.video.codec.id)
        assertEquals(VideoCodec.H264, concat.video.codec)
        val speed = OperationOptions.parse(MediaStudioOps.SPEED, 1, mapOf("factor" to 2.0)) as OperationSpec.Speed
        assertEquals(default(MediaStudioOps.SPEED, "keepPitch"), speed.keepPitch)
        val extract = OperationOptions.parse(MediaStudioOps.EXTRACT_AUDIO, 1, emptyMap()) as OperationSpec.ExtractAudio
        assertEquals(default(MediaStudioOps.EXTRACT_AUDIO, "format"), extract.container.id)
        assertEquals(AudioContainer.M4A, extract.container)
        val gif = OperationOptions.parse(MediaStudioOps.TO_GIF, 1, emptyMap()) as OperationSpec.ToGif
        assertEquals(default(MediaStudioOps.TO_GIF, "fps"), gif.fps.toLong())
        assertEquals(default(MediaStudioOps.TO_GIF, "start"), gif.startMillis)
        assertEquals(0, gif.loop)
        assertEquals(default(MediaStudioOps.TO_GIF, "dither"), gif.dither)
        val thumbnail = OperationOptions.parse(MediaStudioOps.THUMBNAIL, 1, emptyMap()) as OperationSpec.Thumbnail
        assertEquals(default(MediaStudioOps.THUMBNAIL, "format"), thumbnail.image.format.id)
        assertEquals(ImageFormat.JPG, thumbnail.image.format)
        val jpeg = OperationOptions.parse(MediaStudioOps.THUMBNAIL, 1, mapOf("format" to "jpg")) as OperationSpec.Thumbnail
        assertEquals(default(MediaStudioOps.THUMBNAIL, "quality"), jpeg.image.quality.toLong())
        val frames = OperationOptions.parse(MediaStudioOps.FRAMES, 1, mapOf("at" to 0, "format" to "jpg")) as OperationSpec.Frames
        assertEquals(OptionSchemas.of(MediaStudioOps.FRAMES)!!.fields.first { it.key == "quality" }.default, frames.image.quality.toLong())
        val overlay = OperationOptions.parse(MediaStudioOps.OVERLAY, 1, mapOf("overlays" to listOf(mapOf("type" to "text", "text" to "a")))) as OperationSpec.Overlay
        val text = overlay.items.single() as OverlayItem.Text
        val textFields = (OptionSchemas.of(MediaStudioOps.OVERLAY)!!.field("overlays")!!.type as FieldType.Items).variants.getValue("text")
        assertEquals(textFields.first { it.key == "fontSize" }.default, text.fontSize)
        val trim = OperationOptions.parse(MediaStudioOps.TRIM, 1, mapOf("end" to 1000)) as OperationSpec.Trim
        assertEquals(default(MediaStudioOps.TRIM, "start"), trim.startMillis)
        val volume = OperationOptions.parse(MediaStudioOps.AUDIO_VOLUME, 1, mapOf("normalize" to true)) as OperationSpec.AudioVolume
        assertEquals(OperationOptions.DEFAULT_NORMALIZE_DBFS, volume.normalizeDbfs!!, 0.0)
    }

    @Test
    fun `the validators read no key the schemas lack`() {
        val sources = listOf("OperationOptions.kt", "OverlayOptions.kt", "SubtitleOptions.kt")
            .joinToString("\n") { RepositoryFiles.text("app/src/main/java/io/github/supermonster003/autojs6/plugin/three/slate/studio/core/$it") }
        val read = Regex("""\.(?:double|int|long|bool|choice|string|color|length|time|dimension|margin|numbers|objects|child|numberOrString|boolOrInt|boolOrDouble)\("(\w+)"""")
            .findAll(sources).map { it.groupValues[1] }.toSet()
        val schemaKeys = HashSet<String>()
        fun collect(fields: List<OptionField>) {
            for (field in fields) {
                schemaKeys += field.key
                when (val type = field.type) {
                    is FieldType.Group -> collect(type.fields)
                    is FieldType.Items -> { schemaKeys += type.discriminator; type.variants.values.forEach(::collect) }
                    is FieldType.PerInput -> collect(type.fields)
                    else -> Unit
                }
            }
        }
        OptionSchemas.ALL.forEach { collect(it.fields) }
        // `cues` (cue arrays of burnSubtitles / subtitlesWrite, edited in the timeline of roadmap P4.3) and the cue keys.
        // zh-CN: `cues` (burnSubtitles / subtitlesWrite 的 cue 数组, 由路线图 P4.3 的时间线编辑) 及 cue 的键.
        val notInWizards = setOf("cues", "text", "src")
        val missing = read - schemaKeys - notInWizards
        assertTrue("validator keys without a schema field: $missing", missing.isEmpty())
        assertTrue(read.size > 40)
    }

    @Test
    fun `every wizard has a schema and the schemas cover the processing operations`() {
        val wizardOps = OptionSchemas.ALL.map { it.op }.toSet()
        val expected = OperationOptions.SUBMIT_OPS.toSet() - setOf(MediaStudioOps.COMPOSE, MediaStudioOps.SUBTITLES_PARSE, MediaStudioOps.SUBTITLES_WRITE) +
            OperationSchema.SUBTITLES_CONVERT
        assertEquals(expected, wizardOps)
        assertEquals(20, OptionSchemas.ALL.size)
    }

    @Test
    fun `the committed declarations equal the rendering of the schemas`() {
        val rendered = MediaStudioDeclarations.render()
        val committed = RepositoryFiles.path(MediaStudioDeclarations.PATH)
        val current = if (Files.isRegularFile(committed)) Files.readString(committed).replace("\r\n", "\n") else ""
        if (current != rendered) {
            val target = RepositoryFiles.path("app/build/declarations/media-studio-options.d.ts")
            Files.createDirectories(target.parent)
            Files.writeString(target, rendered)
            fail("${MediaStudioDeclarations.PATH} is stale: copy $target over it")
        }
    }

    @Test
    fun `script calls put the options into the argument slots`() {
        val trim = OptionSchemas.of(MediaStudioOps.TRIM)!!
        assertEquals("""video.trim("/a.mp4", { start: 0, end: 5000 });""", ScriptCalls.code(trim, listOf("/a.mp4"), mapOf("start" to 0, "end" to 5000), null))
        assertEquals("""video.trim("/a.mp4", { start: 1 }, "/b.mp4");""", ScriptCalls.code(trim, listOf("/a.mp4"), mapOf("start" to 1), "/b.mp4"))
        val remove = OptionSchemas.of(MediaStudioOps.REMOVE_AUDIO)!!
        assertEquals("""video.removeAudio("/a.mp4");""", ScriptCalls.code(remove, listOf("/a.mp4"), emptyMap(), null))
        val overlay = OptionSchemas.of(MediaStudioOps.OVERLAY)!!
        assertEquals(
            """video.overlay("/a.mp4", [{ type: "text", text: "Hi" }], { codec: "h265" });""",
            ScriptCalls.code(overlay, listOf("/a.mp4"), mapOf("overlays" to listOf(mapOf("type" to "text", "text" to "Hi")), "codec" to "h265"), null),
        )
        val mix = OptionSchemas.of(MediaStudioOps.AUDIO_MIX)!!
        assertEquals(
            """audio.mix(["/a.m4a", { src: "/b.m4a", volume: 0.5, at: 1000 }], { format: "wav" });""",
            ScriptCalls.code(mix, listOf("/a.m4a", "/b.m4a"), mapOf("tracks" to listOf(emptyMap<String, Any>(), mapOf("volume" to 0.5, "at" to 1000)), "format" to "wav"), null),
        )
        val burn = OptionSchemas.of(MediaStudioOps.BURN_SUBTITLES)!!
        assertEquals(
            """video.burnSubtitles("/a.mp4", "/a.srt", { fontSize: 40 }, "/o.mp4");""",
            ScriptCalls.code(burn, listOf("/a.mp4"), mapOf("subtitles" to "/a.srt", "style" to mapOf("fontSize" to 40)), "/o.mp4"),
        )
        val frame = OptionSchemas.of(MediaStudioOps.FRAME)!!
        assertEquals(
            "let image = video.frame(\"/a.mp4\", 1500, { format: \"jpg\", quality: 80 });\nimages.save(image, \"/f.jpg\", \"jpg\", 80);\nimage.recycle();",
            ScriptCalls.code(frame, listOf("/a.mp4"), mapOf("at" to 1500, "format" to "jpg", "quality" to 80), "/f.jpg"),
        )
        val convert = OptionSchemas.of(OperationSchema.SUBTITLES_CONVERT)!!
        assertEquals(
            "let cues = video.subtitles.parse(\"/a.srt\");\nvideo.subtitles.write(\"/a.ass\", cues, \"ass\");",
            ScriptCalls.code(convert, listOf("/a.srt"), mapOf("format" to "ass"), "/a.ass"),
        )
        val concat = OptionSchemas.of(MediaStudioOps.AUDIO_CONCAT)!!
        assertEquals("""audio.concat(["/a.wav", "/b.wav"], "/c.flac", { format: "flac" });""",
            ScriptCalls.code(concat, listOf("/a.wav", "/b.wav"), mapOf("format" to "flac"), "/c.flac"))
    }

    private companion object {
        val MISSING = Any()
    }

}
