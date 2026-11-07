package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.FieldType
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionField
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionSchemas
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.ScriptCalls
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wizard form of roadmap P4.2: what the form builds from its texts is what the plugin's validator accepts and what
 * the equivalent script call shows; every schema key, alternative and worded choice has a label.
 * zh-CN: 路线图 P4.2 的向导表单: 表单由文本构建的选项能被插件校验器接受, 并与等价脚本调用一致; 每个模式键, 互斥选择与需要文字的
 * 选项值都有名称.
 */
class WizardFormTest {

    private fun form(op: String, inputs: Int = 0) = WizardForm(OptionSchemas.of(op)!!).apply {
        repeat(inputs) { addInput("/sdcard/Movies/in$it.mp4") }
    }

    /** Minimal texts per wizard, as a user would type them. zh-CN: 每个向导的最少输入, 如同用户输入. */
    private fun fillMinimal(form: WizardForm) {
        when (form.schema.op) {
            MediaStudioOps.TRIM, MediaStudioOps.AUDIO_TRIM -> form.set("start", "0")
            MediaStudioOps.TRANSFORM -> form.set("flip", "horizontal")
            MediaStudioOps.SPEED, MediaStudioOps.AUDIO_SPEED -> form.set("factor", "2")
            MediaStudioOps.OVERLAY -> form.addItem("overlays", "text", 64).also { form.set("overlays#${form.items["overlays"]!![0].id}/text", "Hello") }
            MediaStudioOps.FRAME -> form.set("at", "1000")
            MediaStudioOps.FRAMES -> { form.alternatives["sampling"] = "at"; form.set("at", "1000") }
            MediaStudioOps.AUDIO_VOLUME -> { form.alternatives["level"] = "gain"; form.set("gain", "0.5") }
            MediaStudioOps.AUDIO_CONVERT -> form.set("format", "wav")
            MediaStudioOps.BURN_SUBTITLES -> form.set("subtitles", "/sdcard/a.srt")
            OperationSchema.SUBTITLES_CONVERT -> form.set("format", "ass")
        }
    }

    @Test
    fun minimalFormsAreAcceptedByThePlugin() {
        for (schema in OptionSchemas.ALL) {
            val form = form(schema.op, schema.inputs.min.coerceAtLeast(1))
            fillMinimal(form)
            val built = form.build()
            assertTrue("${schema.op}: ${built.issues}", built.valid)
            if (schema.op != OperationSchema.SUBTITLES_CONVERT) {
                OperationOptions.parse(schema.op, form.inputs.size, built.options)
            }
            val request = form.request(built)
            assertEquals(ScriptCalls.code(schema, form.inputs.map { it.path }, built.options, null), request.code)
        }
    }

    @Test
    fun textsBecomeContractValues() {
        val trim = form(MediaStudioOps.TRIM, 1)
        trim.set("start", "1500")
        trim.alternatives["length"] = "duration"
        trim.set("duration", "3000")
        trim.set("end", "9999")
        val built = trim.build()
        assertTrue(built.issues.toString(), built.valid)
        assertEquals(mapOf("start" to 1500L, "duration" to 3000L), built.options)
        assertEquals("video.trim(\"/sdcard/Movies/in0.mp4\", { start: 1500, duration: 3000 });", trim.request(built).code)
    }

    @Test
    fun badTextsAreReportedPerField() {
        val transform = form(MediaStudioOps.TRANSFORM, 1)
        transform.alternatives["size"] = "size"
        transform.set("width", "8")
        transform.set("height", "abc")
        transform.set("crop/x", "4")
        val issues = transform.build().issues
        assertTrue(issues["width"] is FieldIssue.OutOfRange)
        assertEquals(FieldIssue.NotNumber, issues["height"])
        assertEquals(FieldIssue.Required, issues["crop/width"])
        assertEquals(FieldIssue.Required, issues["crop/height"])

        val overlay = form(MediaStudioOps.OVERLAY, 1)
        assertEquals(FieldIssue.Required, overlay.build().issues["overlays"])
        overlay.addItem("overlays", "text", 64)
        val id = overlay.items["overlays"]!![0].id
        overlay.set("overlays#$id/text", "Hi")
        overlay.set("overlays#$id/color", "#12")
        overlay.set("overlays#$id/x", "120%")
        overlay.set("overlays#$id/y", "-40")
        val overlayIssues = overlay.build().issues
        assertEquals(FieldIssue.NotColor, overlayIssues["overlays#$id/color"])
        assertEquals(null, overlayIssues["overlays#$id/x"])
        overlay.set("overlays#$id/color", "white")
        val built = overlay.build()
        assertTrue(built.issues.toString(), built.valid)
        assertEquals(listOf(mapOf("type" to "text", "text" to "Hi", "color" to "white", "x" to "120%", "y" to -40.0)), built.options["overlays"])

        val concat = form(MediaStudioOps.CONCAT, 1)
        assertEquals(FieldIssue.Inputs(2, concat.schema.inputs.max), concat.build().issues[WizardForm.INPUTS])
        concat.output = "relative.mp4"
        assertEquals(FieldIssue.NotAbsolute, concat.build().issues[WizardForm.OUTPUT])
    }

    @Test
    fun conditionalFieldsFollowTheirSibling() {
        val schema = OptionSchemas.of(MediaStudioOps.AUDIO_CONVERT)!!
        val form = WizardForm(schema)
        form.set("format", "wav")
        val bitRate = schema.fields.first { it.key == "bitRate" }
        assertTrue(!form.visible(bitRate, "", schema.fields))
        form.set("format", "m4a")
        assertTrue(form.visible(bitRate, "", schema.fields))
        val oggRates = schema.fields.filter { it.key == "sampleRate" }
        form.set("format", "ogg")
        assertEquals(1, oggRates.count { form.visible(it, "", schema.fields) })
    }

    @Test
    fun perInputTracksBecomeMixInputs() {
        val form = form(MediaStudioOps.AUDIO_MIX, 2)
        form.set("tracks@${form.inputs[1].id}/volume", "0.5")
        form.set("tracks@${form.inputs[1].id}/at", "1000")
        form.set("format", "wav")
        val built = form.build()
        assertTrue(built.issues.toString(), built.valid)
        OperationOptions.parse(MediaStudioOps.AUDIO_MIX, 2, built.options)
        assertEquals(
            "audio.mix([\"/sdcard/Movies/in0.mp4\", { src: \"/sdcard/Movies/in1.mp4\", volume: 0.5, at: 1000 }], { format: \"wav\" });",
            form.request(built).code,
        )
        form.removeInput(1)
        assertTrue(form.values.keys.none { it.startsWith("tracks@") })
    }

    @Test
    fun studioCodecDefaultIsPrefilled() {
        val form = form(MediaStudioOps.BURN_SUBTITLES, 1)
        form.applyDefaults(VideoCodec.H265)
        assertEquals("h265", form.values["style/codec"])
        val transcode = form(MediaStudioOps.TRANSCODE, 1).apply { applyDefaults(VideoCodec.H264) }
        assertTrue(transcode.values.isEmpty())
    }

    @Test
    fun everyKeyAlternativeAndWordedChoiceHasALabel() {
        val worded = setOf("fit", "flip", "align", "anchor", "type")
        var checked = 0
        fun walk(fields: List<OptionField>, parent: String?) {
            for (field in fields) {
                assertNotEquals("label of $parent.${field.key}", 0, Labels.field(parent, field.key))
                checked++
                if (field.exclusiveGroup != null) {
                    assertTrue("alternative ${field.exclusiveGroup}.${field.alternative}", "${field.exclusiveGroup}.${field.alternative}" in Labels.alternatives)
                }
                when (val type = field.type) {
                    is FieldType.Group -> walk(type.fields, field.key)
                    is FieldType.PerInput -> walk(type.fields, field.key)
                    is FieldType.Items -> type.variants.forEach { (variant, variantFields) ->
                        assertTrue("variant $variant", Labels.choiceKey(type.discriminator, variant) in Labels.choices)
                        walk(variantFields, field.key)
                    }
                    is FieldType.Choice -> if (field.key in worded) type.values.forEach { value ->
                        assertTrue("choice ${field.key}.$value", Labels.choiceKey(field.key, value) in Labels.choices)
                    }
                    else -> Unit
                }
            }
        }
        for (schema in OptionSchemas.ALL) {
            assertTrue("operation ${schema.op}", schema.op in Labels.operations)
            walk(schema.fields, null)
        }
        assertEquals(OptionSchemas.ALL.size, Labels.operations.size)
        assertTrue("checked $checked fields", checked > 150)
    }
}
