package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory

/**
 * The generated scripts run in the host's Rhino (roadmap P4.4 acceptance: no syntax errors, and the options the script
 * passes equal the input). Each script runs against recording stubs of `video`, `audio`, `images`, `console` and
 * `sleep`; the recorded arguments, read back through `JSON.stringify`, must equal the request: the inputs, every option
 * (moved back out of the argument slots of [ScriptCalls]) and the output. Covers all 20 wizards of `OptionSchemas`
 * with every field filled (strings with quotes, backslashes, control characters, line separators and CJK text), with
 * the required fields only and without an output, plus `video.compose` (appendix B.4) and the `audio.record` script of
 * roadmap P4.5. The Rhino jar is the host's own slim build (`locks/rhino-jar.lock`, test classpath only).
 * zh-CN: 生成的脚本在宿主的 Rhino 中运行 (路线图 P4.4 验收: 无语法错误, 脚本传出的选项与输入相等). 每个脚本在记录调用的 `video`,
 * `audio`, `images`, `console` 与 `sleep` 桩上运行; 经 `JSON.stringify` 读回的参数必须与请求相等: 输入, 每个选项 (从 [ScriptCalls] 的
 * 参数位置移回) 与输出. 覆盖 `OptionSchemas` 的全部 20 个向导 (全部字段填写, 字符串含引号, 反斜杠, 控制字符, 行分隔符与中文; 只填必填
 * 字段; 无输出), 以及 `video.compose` (附录 B.4) 与路线图 P4.5 的 `audio.record` 脚本. Rhino jar 为宿主自己的 slim 构建
 * (`locks/rhino-jar.lock`, 只在测试类路径上).
 */
class GeneratedScriptRhinoTest {

    private data class Call(val name: String, val args: List<Any?>)

    private val backslash = Char(92).toString()
    private val tricky = "Say \"hi\" " + backslash + "n" + backslash + Char(10) + "tab" + Char(9) + "cr" + Char(13) + "ls" + Char(0x2028) +
        "ps" + Char(0x2029) + "nul" + Char(0) + " 视频字幕 " + String(Character.toChars(0x1F3AC))

    // region Running

    private fun stubs(): String {
        val methods = OptionSchemas.ALL.filter { it.op != OperationSchema.SUBTITLES_CONVERT }.map { "${it.module}.${it.method}" }.toSet() +
            setOf("video.compose", "video.subtitles.parse", "video.subtitles.write", "audio.record")
        val lines = mutableListOf(
            "var __calls = [];",
            "function __record(name, args) { __calls.push({ name: name, args: Array.prototype.slice.call(args) }); }",
            "var console = { log: function () { __record('console.log', arguments); } };",
            "var images = { save: function () { __record('images.save', arguments); } };",
            "function sleep() { __record('sleep', arguments); }",
            "var video = { subtitles: {} };",
            "var audio = {};",
        )
        for (method in methods) {
            val answer = when (method) {
                "video.frames" -> "['/out/1.png', '/out/2.png']"
                "video.frame" -> "{ width: 640, height: 360, recycle: function () { __record('image.recycle', arguments); } }"
                "video.subtitles.parse" -> "[{ start: 0, end: 1000, text: 'cue' }]"
                "audio.record" -> "{ stop: function () { __record('recording.stop', arguments); return { path: '/out/rec.m4a' }; } }"
                else -> "{ path: '/out/result' }"
            }
            lines += "$method = function () { __record('$method', arguments); return $answer; };"
        }
        return lines.joinToString("\n")
    }

    /** Compiles [code] (syntax errors throw) and runs it after the stubs; answers the recorded calls. zh-CN: 编译并运行, 返回记录的调用. */
    private fun run(code: String): List<Call> {
        val cx = ContextFactory().enterContext()
        try {
            cx.languageVersion = Context.VERSION_ES6
            cx.isInterpretedMode = true
            val scope = cx.initStandardObjects()
            cx.evaluateString(scope, stubs(), "stubs.js", 1, null)
            val script = cx.compileString(code, "generated.js", 1, null)
            script.exec(cx, scope, scope)
            val json = cx.evaluateString(scope, "JSON.stringify(__calls)", "read.js", 1, null) as String
            val array = JSONArray(json)
            return (0 until array.length()).map { i ->
                val call = array.getJSONObject(i)
                Call(call.getString("name"), plain(call.getJSONArray("args")) as List<Any?>)
            }
        } finally {
            cx.close()
        }
    }

    private fun plain(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONArray -> (0 until value.length()).map { plain(value.get(it)) }
        is JSONObject -> value.keys().asSequence().associateWith { plain(value.get(it)) }
        is Number -> value.toDouble()
        else -> value
    }

    /** Numbers as Double, null map values dropped, arrays as lists. zh-CN: 数字转为 Double, 去掉 Map 的 null 值, 数组转为列表. */
    private fun normalized(value: Any?): Any? = when (value) {
        is Number -> value.toDouble()
        is Map<*, *> -> value.entries.filter { it.value != null }.associate { it.key.toString() to normalized(it.value) }
        is Iterable<*> -> value.map { normalized(it) }
        is Array<*> -> value.map { normalized(it) }
        is LongArray -> value.map { it.toDouble() }
        is DoubleArray -> value.toList()
        else -> value
    }

    // endregion

    // region Samples

    private fun sample(field: OptionField, inputs: Int): Any? = when (val type = field.type) {
        is FieldType.Integer -> when {
            field.uiChoices != null -> field.uiChoices.last().toLong()
            1500L in type.min..type.max -> 1500L
            else -> type.min
        }
        is FieldType.Decimal -> when {
            1.5 > type.min && 1.5 <= type.max -> 1.5
            else -> (type.min + type.max) / 2
        }
        is FieldType.Choice -> type.values.last()
        is FieldType.IntChoice -> type.values.last().toLong()
        FieldType.Flag -> field.default != true
        is FieldType.FlagOrInteger -> 3L.coerceIn(type.min, type.max)
        is FieldType.FlagOrDecimal -> (type.min + type.max) / 2
        FieldType.Color -> "#80FF0000"
        is FieldType.Length -> "25%"
        is FieldType.Text -> tricky
        FieldType.Font -> "/sdcard/Fonts/My Font (1).ttf"
        is FieldType.FilePath -> if (type.kind == FileKind.IMAGE) "/sdcard/Pictures/logo \"copy\" ${backslash}1.png" else "/sdcard/Movies/字幕 (1).srt"
        is FieldType.Numbers -> listOf(0L, 1500.5, 3000L)
        is FieldType.Group -> fill(type.fields, inputs, full = true)
        is FieldType.Items -> type.variants.map { (name, fields) -> linkedMapOf<String, Any?>(type.discriminator to name) + fill(fields, inputs, full = true) }
        is FieldType.PerInput -> List(inputs) { index -> fill(type.fields, inputs, full = true) + ("at" to 1000L * index) }
    }

    /**
     * Every field ([full]) or the required ones; of an exclusive group only the first alternative, and fields whose
     * condition does not hold are left out.
     * zh-CN: 全部字段 ([full]) 或必填字段; 互斥组只取第一个选择, 条件不成立的字段省略.
     */
    private fun fill(fields: List<OptionField>, inputs: Int, full: Boolean): Map<String, Any?> {
        val chosen = linkedMapOf<String, Any?>()
        val firstAlternative = fields.filter { it.exclusiveGroup != null }.groupBy { it.exclusiveGroup }.mapValues { it.value.first().alternative }
        for (field in fields) {
            if (!full && !field.required) continue
            if (field.exclusiveGroup != null && field.alternative != firstAlternative[field.exclusiveGroup]) continue
            val condition = field.appliesWhen
            if (condition != null && !condition.holds(chosen[condition.key] as? String)) continue
            chosen[field.key] = sample(field, inputs)
        }
        return chosen
    }

    private fun inputs(schema: OperationSchema): List<String> {
        val count = maxOf(schema.inputs.min, if (schema.inputs.max > 1) 2 else 1)
        val extension = if (schema.inputs.kind == InputKind.SUBTITLE) "ass" else "mp4"
        return listOf("/sdcard/Movies/My \"clip\" 1.$extension", "/sdcard/Movies/视频 ${backslash}2.$extension", "/sdcard/Movies/c'3.$extension").take(count)
    }

    private fun output(schema: OperationSchema): String = when (schema.output) {
        OutputKind.VIDEO -> "/sdcard/Movies/out \"1\".mp4"
        OutputKind.AUDIO -> "/sdcard/Music/out 1.m4a"
        OutputKind.IMAGE -> if ((schema.field("format")?.type as? FieldType.Choice)?.values?.last() == "png") "/sdcard/Pictures/out 1.png" else "/sdcard/Pictures/out 1.jpg"
        OutputKind.GIF -> "/sdcard/Pictures/out 1.gif"
        OutputKind.FOLDER -> "/sdcard/Pictures/frames 1"
        OutputKind.SUBTITLE -> "/sdcard/Movies/字幕 out.srt"
    }

    // endregion

    // region Reading the calls back

    private data class Request(val inputs: List<String>, val options: Map<String, Any?>, val output: String?)

    /**
     * The request a recorded call carries: arguments matched to the schema slots by type (an empty optional slot is left
     * out, as the host maps them, appendix D Q27 (a)), then the slot values moved back into the options.
     * zh-CN: 记录的调用所携带的请求: 按类型把参数对应到模式的参数位置 (空的可选位置被省略, 与宿主的映射相同, 附录 D Q27 (a)), 再把位置上的
     * 值移回选项.
     */
    @Suppress("UNCHECKED_CAST")
    private fun requestOf(schema: OperationSchema, args: List<Any?>): Request {
        val rest = ArrayDeque(args)
        var inputs = emptyList<String>()
        val options = linkedMapOf<String, Any?>()
        var output: String? = null
        for (slot in schema.slots) {
            val next = rest.firstOrNull()
            when (slot) {
                Slot.INPUT -> inputs = listOf(rest.removeFirst() as String)
                Slot.INPUTS -> inputs = rest.removeFirst() as List<String>
                Slot.MIX_INPUTS -> {
                    val items = rest.removeFirst() as List<Any?>
                    inputs = items.map { (it as? Map<String, Any?>)?.get("src") as? String ?: it as String }
                    // Plain paths when no input has track options. zh-CN: 没有任何一路带音轨选项时是纯路径.
                    if (items.any { it is Map<*, *> }) options["tracks"] = items.map { (it as? Map<String, Any?>)?.minus("src") ?: emptyMap<String, Any?>() }
                }
                Slot.OVERLAYS -> options["overlays"] = rest.removeFirst()
                Slot.AT -> options["at"] = rest.removeFirst()
                Slot.SUBTITLES -> options["subtitles"] = rest.removeFirst()
                Slot.STYLE -> if (next is Map<*, *>) options["style"] = rest.removeFirst()
                Slot.OPTIONS -> if (next is Map<*, *>) options.putAll(rest.removeFirst() as Map<String, Any?>)
                Slot.OUTPUT -> if (next is String) output = rest.removeFirst() as String
            }
        }
        assertTrue("unread arguments $rest", rest.isEmpty())
        return Request(inputs, options, output)
    }

    @Suppress("UNCHECKED_CAST")
    private fun check(schema: OperationSchema, inputs: List<String>, options: Map<String, Any?>, output: String?) {
        val script = CodeGenerator.wizardScript(schema, inputs, options, output)
        val label = "${schema.op}: ${script.code}"
        assertTrue(label, script.code.startsWith("// Generated by 3-Slate Studio: "))
        val calls = run(script.code)
        val expected = Request(inputs, normalized(options) as Map<String, Any?>, output)
        val actual = when (schema.op) {
            OperationSchema.SUBTITLES_CONVERT -> {
                assertEquals(label, listOf("video.subtitles.parse", "video.subtitles.write", "console.log"), calls.map { it.name })
                val write = calls[1].args
                assertEquals(label, listOf(mapOf("start" to 0.0, "end" to 1000.0, "text" to "cue")), write[1])
                Request(calls[0].args.map { it as String }, if (write.size > 2) mapOf("format" to write[2]) else emptyMap(), write[0] as String?)
            }
            MediaStudioOps.FRAME -> {
                val names = listOf("video.frame") + (if (output != null) listOf("images.save") else listOf("console.log")) + "image.recycle"
                assertEquals(label, names, calls.map { it.name })
                val request = requestOf(schema, calls[0].args)
                if (output == null) request else {
                    val save = calls[1].args
                    assertEquals(label, null, save[0].takeIf { it !is Map<*, *> })
                    if (save.size > 2) assertEquals(label, listOf("jpg", (options["quality"] as Number).toDouble()), save.drop(2))
                    request.copy(output = save[1] as String)
                }
            }
            else -> {
                assertEquals(label, listOf("${schema.module}.${schema.method}", "console.log"), calls.map { it.name })
                requestOf(schema, calls[0].args)
            }
        }
        assertEquals(label, expected, actual.copy(options = normalized(actual.options) as Map<String, Any?>))
    }

    // endregion

    @Test
    fun everyWizardWithEveryField() {
        assertEquals(20, OptionSchemas.ALL.size)
        for (schema in OptionSchemas.ALL) {
            val inputs = inputs(schema)
            check(schema, inputs, fill(schema.fields, inputs.size, full = true), output(schema))
        }
    }

    @Test
    fun everyWizardWithTheRequiredFieldsAndNoOutput() {
        for (schema in OptionSchemas.ALL) {
            val inputs = inputs(schema)
            val options = fill(schema.fields, inputs.size, full = false)
            check(schema, inputs, options, output(schema))
            // subtitles.write needs a path; every other wizard has a default output (D14). zh-CN: subtitles.write 需要路径.
            if (schema.op != OperationSchema.SUBTITLES_CONVERT) check(schema, inputs, options, null)
        }
    }

    @Test
    fun framesTimesAndOtherAlternatives() {
        val frames = OptionSchemas.of(MediaStudioOps.FRAMES)!!
        val input = listOf("/sdcard/Movies/a.mp4")
        check(frames, input, mapOf("times" to listOf(500L, 1500L, 2500L)), "/sdcard/Pictures/frames")
        check(frames, input, mapOf("times" to listOf(0L, 1500.5), "format" to "jpg", "quality" to 80L), null)
        check(frames, input, mapOf("every" to 1000L, "start" to 500L, "end" to 9000L, "width" to 320L), null)
        check(OptionSchemas.of(MediaStudioOps.TRANSFORM)!!, input, mapOf("scale" to 0.5, "rotate" to -270L), null)
        check(OptionSchemas.of(MediaStudioOps.AUDIO_VOLUME)!!, input, mapOf("normalize" to true, "fadeIn" to 500L), null)
        check(OptionSchemas.of(MediaStudioOps.TO_GIF)!!, input, mapOf("loop" to 3L, "dither" to true), null)
    }

    @Test
    fun longCallsBreakAcrossLinesAndStillRun() {
        val schema = OptionSchemas.of(MediaStudioOps.OVERLAY)!!
        val overlays = List(12) { index -> mapOf("type" to "text", "text" to "line $index $tricky", "x" to "${index * 5}%", "from" to index * 1000L) }
        val script = CodeGenerator.wizardScript(schema, listOf("/sdcard/Movies/a.mp4"), mapOf("overlays" to overlays, "codec" to "h265"), "/sdcard/Movies/b.mp4")
        assertTrue(script.code, script.code.lines().all { it.length <= CodeGenerator.LINE_WIDTH + 200 })
        assertTrue(script.code, script.code.lines().size > 10)
        check(schema, listOf("/sdcard/Movies/a.mp4"), mapOf("overlays" to overlays, "codec" to "h265"), "/sdcard/Movies/b.mp4")
    }

    private fun timeline(clips: Int = 2): Map<String, Any?> = linkedMapOf(
        "width" to 1080L, "height" to 1920L, "frameRate" to 30L, "background" to "#000000",
        "video" to listOf(mapOf("clips" to List(clips) { index ->
            linkedMapOf<String, Any?>("src" to "/sdcard/Movies/clip \"$index\".mp4", "start" to 0L, "end" to 8000L, "speed" to 1.5, "volume" to 0.5,
                "transform" to mapOf("scale" to 1.0, "rotate" to 90L, "flip" to "horizontal"),
                "transition" to if (index == 0) null else mapOf("type" to "dissolve", "duration" to 500L))
        })),
        "audio" to listOf(mapOf("clips" to listOf(mapOf("src" to "/sdcard/Music/bgm.m4a", "at" to 0L, "end" to 15500L, "volume" to 0.4, "fadeIn" to 1000L, "fadeOut" to 2000L)))),
        "overlays" to listOf(
            mapOf("type" to "text", "text" to tricky, "from" to 1000L, "to" to 4000L, "x" to "50%", "y" to "10%", "fontSize" to 48L, "color" to "#FFFFFF", "stroke" to "#000000"),
            mapOf("type" to "image", "src" to "/sdcard/Pictures/logo.png", "from" to 0L, "to" to 15500L, "x" to "90%", "y" to "90%", "scale" to 0.3, "alpha" to 0.8),
        ),
        "subtitles" to mapOf("cues" to listOf(mapOf("start" to 0L, "end" to 2000L, "text" to tricky)), "style" to mapOf("fontSize" to 40L, "align" to "bottom")),
    )

    @Test
    fun composeScriptsCarryTheTimeline() {
        val cases = listOf(
            Triple(timeline(), "/sdcard/Movies/out 1.mp4", mapOf("codec" to "h265", "bitRate" to 8_000_000L)),
            Triple(timeline(), null, mapOf("codec" to "h264")),
            Triple(timeline(), "/sdcard/Movies/out.mp4", emptyMap()),
            Triple(timeline(256), null, emptyMap()),
        )
        for ((timeline, output, options) in cases) {
            val script = CodeGenerator.composeScript(timeline, output, options)
            assertEquals("video-compose.js", script.suggestedName)
            val calls = run(script.code)
            assertEquals(listOf("video.compose", "console.log"), calls.map { it.name })
            val args = calls[0].args
            assertEquals(normalized(timeline), args[0])
            assertEquals(output, args.getOrNull(1))
            assertEquals(normalized(options), args.getOrNull(2) ?: emptyMap<String, Any?>())
            assertEquals(if (options.isNotEmpty()) 3 else if (output != null) 2 else 1, args.size)
        }
        val call = CodeGenerator.composeCall(mapOf("width" to 640L, "video" to emptyList<Any>()), null, emptyMap())
        assertEquals("video.compose({ width: 640, video: [] });", call)
    }

    @Test
    fun recordScriptsStartWaitAndStop() {
        val path = "/sdcard/Music/rec \"1\" $backslash.m4a"
        val options = linkedMapOf<String, Any?>("source" to "mixed", "format" to "m4a", "sampleRate" to 48_000L, "channels" to 2L, "bitRate" to 192_000L, "maxDuration" to 60_000L, "unused" to null)
        val script = CodeGenerator.recordScript(path, options, 15_000)
        assertEquals("audio-record.js", script.suggestedName)
        val calls = run(script.code)
        assertEquals(listOf("audio.record", "sleep", "recording.stop", "console.log"), calls.map { it.name })
        assertEquals(listOf(path, normalized(options)), calls[0].args)
        assertEquals(listOf(15_000.0), calls[1].args)
        assertEquals(listOf("/out/rec.m4a"), calls[3].args)
        assertEquals(listOf(null, mapOf("source" to "mic")), run(CodeGenerator.recordScript(null, mapOf("source" to "mic")).code)[0].args)
        assertEquals(emptyList<Any?>(), run(CodeGenerator.recordScript(null).code)[0].args)
        assertEquals(listOf(path), run(CodeGenerator.recordScript(path).code)[0].args)
    }

}
