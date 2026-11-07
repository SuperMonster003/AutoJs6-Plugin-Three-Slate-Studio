package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.FieldType
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.LengthLower
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionField
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.ScriptCalls
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Colors
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OverlayOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.VideoCodec
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioRequest
import java.util.Locale

/** Why a field value is not accepted; the screen localizes it. zh-CN: 字段值不被接受的原因; 由界面本地化. */
internal sealed interface FieldIssue {
    data object Required : FieldIssue
    data object NotNumber : FieldIssue
    data object NotInteger : FieldIssue
    data class OutOfRange(val min: String, val max: String) : FieldIssue
    data object NotColor : FieldIssue
    data object NotLength : FieldIssue
    data class TextLength(val min: Int, val max: Int) : FieldIssue
    data object NotAbsolute : FieldIssue
    data class TooMany(val max: Int) : FieldIssue
    data class Inputs(val min: Int, val max: Int) : FieldIssue
}

/** One input file of the form, with a stable id for per-input rows. zh-CN: 表单的一个输入文件, 带有供逐输入行使用的稳定 id. */
internal data class InputItem(val id: Int, val path: String)

/** One item of an [FieldType.Items] list (an overlay). zh-CN: [FieldType.Items] 列表的一项 (一个叠加物). */
internal data class ItemRef(val id: Int, val variant: String)

/**
 * The state of one wizard (roadmap D35, P4.2), driven by its [OperationSchema]: raw texts by field path, the chosen
 * alternative of each exclusive group, the items of item lists and the inputs. [build] turns it into contract options
 * (the same shape scripts send through the host) with one issue per bad field; nothing is hand-written per operation.
 * Paths: `key`, `group/key`, `list#id/key`, `list@inputId/key`.
 * zh-CN: 一个向导的状态 (路线图 D35, P4.2), 由其 [OperationSchema] 驱动: 按字段路径保存的原始文本, 各互斥组所选的选择, 项目列表
 * 中的项与输入. [build] 把它转成契约选项 (与脚本经宿主发送的形态相同), 每个错误字段一个问题; 不为单个操作手写任何东西.
 * 路径: `key`, `group/key`, `list#id/key`, `list@inputId/key`.
 */
internal class WizardForm(val schema: OperationSchema) {

    val inputs = mutableStateListOf<InputItem>()
    val values = mutableStateMapOf<String, String>()

    /** Group path to alternative; absent means none. zh-CN: 组路径到所选选择; 不存在表示未选. */
    val alternatives = mutableStateMapOf<String, String>()
    val items = mutableStateMapOf<String, List<ItemRef>>()
    var output by mutableStateOf("")
    private var nextId = 0

    fun addInput(path: String) {
        if (inputs.size < schema.inputs.max) inputs += InputItem(nextId++, path)
    }

    fun replaceInput(index: Int, path: String) {
        inputs[index] = inputs[index].copy(path = path)
    }

    fun removeInput(index: Int) {
        val removed = inputs.removeAt(index)
        values.keys.filter { it.contains("@${removed.id}/") }.forEach(values::remove)
    }

    fun moveInput(index: Int, delta: Int) {
        val target = index + delta
        if (target !in inputs.indices) return
        val item = inputs.removeAt(index)
        inputs.add(target, item)
    }

    fun addItem(path: String, variant: String, max: Int) {
        val current = items[path].orEmpty()
        if (current.size < max) items[path] = current + ItemRef(nextId++, variant)
    }

    fun removeItem(path: String, id: Int) {
        items[path] = items[path].orEmpty().filter { it.id != id }
        values.keys.filter { it.startsWith("$path#$id/") }.forEach(values::remove)
    }

    fun set(path: String, text: String?) {
        if (text.isNullOrEmpty()) values.remove(path) else values[path] = text
    }

    /** Fills the wizard defaults the Studio settings choose (D37: the default codec). zh-CN: 填入 Studio 设置选择的向导默认值. */
    fun applyDefaults(codec: VideoCodec) {
        if (codec == VideoCodec.H264) return
        forEachField(schema.fields, "") { path, field ->
            val type = field.type
            if (field.key == "codec" && type is FieldType.Choice && codec.id in type.values) values[path] = codec.id
        }
    }

    private fun forEachField(fields: List<OptionField>, prefix: String, action: (String, OptionField) -> Unit) {
        fields.forEach { field ->
            val path = prefix + field.key
            when (val type = field.type) {
                is FieldType.Group -> forEachField(type.fields, "$path/", action)
                else -> action(path, field)
            }
        }
    }

    /** The result of [build]. zh-CN: [build] 的结果. */
    data class Built(val options: Map<String, Any?>, val issues: Map<String, FieldIssue>) {
        val valid: Boolean get() = issues.isEmpty()
    }

    fun build(): Built {
        val issues = LinkedHashMap<String, FieldIssue>()
        if (inputs.size !in schema.inputs.min..schema.inputs.max) issues[INPUTS] = FieldIssue.Inputs(schema.inputs.min, schema.inputs.max)
        val options = walk(schema.fields, "", issues, values)
        if (output.isNotBlank() && !output.trim().startsWith("/")) issues[OUTPUT] = FieldIssue.NotAbsolute
        return Built(options, issues)
    }

    /** The request and its equivalent script call. zh-CN: 请求及其等价的脚本调用. */
    fun request(built: Built): StudioRequest {
        val paths = inputs.map { it.path }
        val target = output.trim().ifEmpty { null }
        return StudioRequest(schema, paths, built.options, target, ScriptCalls.code(schema, paths, built.options, target))
    }

    /**
     * True when [field] of the list [siblings] (under [prefix]) shows: its condition on a sibling holds (the sibling's
     * default counts when it is empty) and its alternative is chosen.
     * zh-CN: 列表 [siblings] (位于 [prefix] 下) 中的 [field] 是否显示: 对同级字段的条件成立 (同级为空时按其默认值) 且其所属选择已选中.
     */
    fun visible(field: OptionField, prefix: String, siblings: List<OptionField>): Boolean {
        val condition = field.appliesWhen
        if (condition != null) {
            val sibling = values[prefix + condition.key] ?: siblings.firstOrNull { it.key == condition.key }?.default?.toString()
            if (!condition.holds(sibling)) return false
        }
        val group = field.exclusiveGroup ?: return true
        return alternatives[prefix + group] == field.alternative
    }

    private fun walk(fields: List<OptionField>, prefix: String, issues: MutableMap<String, FieldIssue>, raw: Map<String, String>): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for (field in fields) {
            if (!visible(field, prefix, fields)) continue
            val path = prefix + field.key
            when (val type = field.type) {
                is FieldType.Group -> {
                    // An empty optional group is left out, so its required keys only count once one key is set.
                    // zh-CN: 空的可选组被省略, 因此只有设置了其中一个键后, 组内的必填键才计入.
                    val nestedIssues = LinkedHashMap<String, FieldIssue>()
                    val nested = walk(type.fields, "$path/", nestedIssues, raw)
                    if (nested.isNotEmpty() || nestedIssues.values.any { it != FieldIssue.Required }) {
                        issues.putAll(nestedIssues)
                        out[field.key] = nested
                    } else if (field.required) {
                        issues[path] = FieldIssue.Required
                    }
                }
                is FieldType.Items -> {
                    val list = items[path].orEmpty().map { ref ->
                        linkedMapOf<String, Any?>(type.discriminator to ref.variant) + walk(type.variants.getValue(ref.variant), "$path#${ref.id}/", issues, raw)
                    }
                    when {
                        list.isEmpty() && field.required -> issues[path] = FieldIssue.Required
                        list.size > type.max -> issues[path] = FieldIssue.TooMany(type.max)
                        list.isNotEmpty() -> out[field.key] = list
                    }
                }
                is FieldType.PerInput -> {
                    val list = inputs.map { input -> walk(type.fields, "$path@${input.id}/", issues, raw) }
                    if (list.any { it.isNotEmpty() }) out[field.key] = list
                }
                else -> {
                    val text = raw[path]?.trim()
                    if (text.isNullOrEmpty()) {
                        if (field.required) issues[path] = FieldIssue.Required
                        continue
                    }
                    convert(type, text).fold({ out[field.key] = it }, { issues[path] = (it as IssueException).issue })
                }
            }
        }
        return out
    }

    private class IssueException(val issue: FieldIssue) : Exception()

    private fun fail(issue: FieldIssue): Result<Any?> = Result.failure(IssueException(issue))

    /** Converts one raw text to its contract value. zh-CN: 把一个原始文本转换为契约值. */
    private fun convert(type: FieldType, text: String): Result<Any?> = when (type) {
        is FieldType.Integer -> integer(text, type.min, type.max)
        is FieldType.Decimal -> decimal(text, type.min, type.max, type.minExclusive)
        is FieldType.Choice -> Result.success(text)
        is FieldType.IntChoice -> text.toIntOrNull()?.let { Result.success(it.toLong()) } ?: fail(FieldIssue.NotInteger)
        FieldType.Flag -> Result.success(text == TRUE)
        is FieldType.FlagOrInteger -> when (text) {
            TRUE -> Result.success(true)
            FALSE -> Result.success(false)
            else -> integer(text, type.min, type.max)
        }
        is FieldType.FlagOrDecimal -> when (text) {
            TRUE -> Result.success(true)
            FALSE -> Result.success(false)
            else -> decimal(text, type.min, type.max, false)
        }
        FieldType.Color -> if (Colors.parse(text) != null) Result.success(text) else fail(FieldIssue.NotColor)
        is FieldType.Length -> length(text, type)
        is FieldType.Text -> if (text.length in type.minChars..type.maxChars) Result.success(text) else fail(FieldIssue.TextLength(type.minChars, type.maxChars))
        FieldType.Font -> if (text in OverlayOptions.FONT_FAMILIES || text.startsWith("/")) Result.success(text) else fail(FieldIssue.NotAbsolute)
        is FieldType.FilePath -> if (text.startsWith("/")) Result.success(text) else fail(FieldIssue.NotAbsolute)
        is FieldType.Numbers -> numbers(text, type)
        is FieldType.Group, is FieldType.Items, is FieldType.PerInput -> error("nested types are walked")
    }

    private fun integer(text: String, min: Long, max: Long): Result<Any?> {
        val value = text.toLongOrNull() ?: return if (text.toDoubleOrNull() != null) fail(FieldIssue.NotInteger) else fail(FieldIssue.NotNumber)
        return if (value in min..max) Result.success(value) else fail(FieldIssue.OutOfRange(min.toString(), max.toString()))
    }

    private fun decimal(text: String, min: Double, max: Double, minExclusive: Boolean): Result<Any?> {
        val value = text.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return fail(FieldIssue.NotNumber)
        val above = if (minExclusive) value > min else value >= min
        return if (above && value <= max) Result.success(value) else fail(FieldIssue.OutOfRange(number(min), number(max)))
    }

    private fun length(text: String, type: FieldType.Length): Result<Any?> {
        if (text.endsWith("%")) {
            val percent = text.dropLast(1).trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: return fail(FieldIssue.NotLength)
            val ok = when (type.lower) {
                LengthLower.SIGNED -> kotlin.math.abs(percent) <= type.maxPercent
                LengthLower.NON_NEGATIVE -> percent in 0.0..type.maxPercent
                LengthLower.POSITIVE -> percent > 0 && percent <= type.maxPercent
            }
            return if (ok) Result.success("${number(percent)}%") else fail(FieldIssue.OutOfRange(lowerOf(type, type.maxPercent) + "%", number(type.maxPercent) + "%"))
        }
        val pixels = text.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return fail(FieldIssue.NotLength)
        val ok = when (type.lower) {
            LengthLower.SIGNED -> kotlin.math.abs(pixels) <= type.maxPixels
            LengthLower.NON_NEGATIVE -> pixels in 0.0..type.maxPixels
            LengthLower.POSITIVE -> pixels > 0 && pixels <= type.maxPixels
        }
        return if (ok) Result.success(pixels) else fail(FieldIssue.OutOfRange(lowerOf(type, type.maxPixels), number(type.maxPixels)))
    }

    private fun lowerOf(type: FieldType.Length, max: Double) = when (type.lower) {
        LengthLower.SIGNED -> "-" + number(max)
        else -> "0"
    }

    private fun numbers(text: String, type: FieldType.Numbers): Result<Any?> {
        val parts = text.split(',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return fail(FieldIssue.Required)
        if (parts.size > type.maxCount) return fail(FieldIssue.TooMany(type.maxCount))
        val values = parts.map { part -> part.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return fail(FieldIssue.NotNumber) }
        if (values.any { it < type.min || it > type.max }) return fail(FieldIssue.OutOfRange(number(type.min), number(type.max)))
        return Result.success(values)
    }

    companion object {
        const val INPUTS = "#inputs"
        const val OUTPUT = "#output"
        const val TRUE = "true"
        const val FALSE = "false"

        fun number(value: Double): String =
            if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) value.toLong().toString() else String.format(Locale.ROOT, "%s", value)
    }
}
