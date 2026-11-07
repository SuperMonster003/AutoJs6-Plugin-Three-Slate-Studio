package io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen

import java.util.Locale

/**
 * Renders [OptionSchemas] as TypeScript declarations of the option objects and the argument slots of every wizard
 * operation (roadmap D36: the code generator and the d.ts come from one source). The rendering is committed as
 * `docs/development/media-studio-options.d.ts`; `OptionSchemasTest` fails when it differs, and roadmap P5.2 builds the
 * Rhino `video.d.ts` / `audio.d.ts` from it. Pure Kotlin.
 * zh-CN: 把 [OptionSchemas] 渲染为各向导操作的选项对象与参数位置的 TypeScript 声明 (路线图 D36: 代码生成器与 d.ts 同源). 渲染
 * 结果入库为 `docs/development/media-studio-options.d.ts`; 不一致时 `OptionSchemasTest` 失败, 路线图 P5.2 据此编写 Rhino 的
 * `video.d.ts` / `audio.d.ts`. 纯 Kotlin.
 */
internal object MediaStudioDeclarations {

    const val PATH = "docs/development/media-studio-options.d.ts"

    fun render(): String = buildString {
        appendLine("// Generated from OptionSchemas of 3-Slate Studio (roadmap P4.2, D36). Do not edit by hand: change the schemas,")
        appendLine("// then copy app/build/declarations/media-studio-options.d.ts here (OptionSchemasTest writes it when they differ).")
        appendLine("// Times are milliseconds; null or a left-out key takes the default.")
        appendLine()
        appendLine("declare namespace MediaStudioOptions {")
        for (schema in OptionSchemas.ALL) {
            appendLine()
            appendLine("    // ${signature(schema)}")
            val base = typeName(schema)
            val shown = optionFields(schema)
            renderInterface(this, "${base}Options", shown, schema)
            for (field in schema.fields) renderNested(this, base, field, schema)
        }
        appendLine("}")
    }

    /** The script signature of a wizard operation, from its slots. zh-CN: 由参数位置得出的脚本签名. */
    fun signature(schema: OperationSchema): String {
        val base = typeName(schema)
        val parameters = schema.slots.map { slot ->
            when (slot) {
                Slot.INPUT -> "src: string"
                Slot.INPUTS -> "sources: string[]"
                Slot.MIX_INPUTS -> "inputs: (string | ${base}Input)[]"
                Slot.OVERLAYS -> "overlays: ${base}Overlay[]"
                Slot.AT -> "at: number"
                Slot.SUBTITLES -> "subtitles: string"
                Slot.STYLE -> "style?: ${base}Style"
                Slot.OPTIONS -> "options${if (schema.fields.any { it.required && it.key !in slotKeys(schema) }) "" else "?"}: ${base}Options"
                Slot.OUTPUT -> "output?: string | null"
            }
        }
        val method = if (schema.op == OperationSchema.SUBTITLES_CONVERT) "subtitles.parse(path) + video.subtitles.write" else "${schema.module}.${schema.method}"
        return "$method(${parameters.joinToString(", ")})"
    }

    private fun slotKeys(schema: OperationSchema): Set<String> = buildSet {
        if (Slot.OVERLAYS in schema.slots) add("overlays")
        if (Slot.AT in schema.slots) add("at")
        if (Slot.SUBTITLES in schema.slots) add("subtitles")
        if (Slot.STYLE in schema.slots) add("style")
        if (Slot.MIX_INPUTS in schema.slots) add("tracks")
    }

    private fun optionFields(schema: OperationSchema): List<OptionField> = schema.fields.filter { it.key !in slotKeys(schema) }

    private fun typeName(schema: OperationSchema): String {
        val method = schema.method.split('.').joinToString("") { part -> part.replaceFirstChar { it.titlecase(Locale.ROOT) } }
        return schema.module.replaceFirstChar { it.titlecase(Locale.ROOT) } + method
    }

    private fun renderNested(out: StringBuilder, base: String, field: OptionField, schema: OperationSchema) {
        when (val type = field.type) {
            is FieldType.Group -> {
                val name = if (field.key == "style") "${base}Style" else base + field.key.replaceFirstChar { it.titlecase(Locale.ROOT) }
                renderInterface(out, name, type.fields, schema)
            }
            is FieldType.Items -> {
                type.variants.forEach { (variant, fields) ->
                    val name = base + variant.replaceFirstChar { it.titlecase(Locale.ROOT) } + "Overlay"
                    out.appendLine("    interface $name {")
                    out.appendLine("        ${type.discriminator}: '$variant';")
                    fields.forEach { renderField(out, it, "        ") }
                    out.appendLine("    }")
                }
                out.appendLine("    type ${base}Overlay = ${type.variants.keys.joinToString(" | ") { base + it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } + "Overlay" }};")
            }
            is FieldType.PerInput -> {
                out.appendLine("    interface ${base}Input {")
                out.appendLine("        src: string;")
                type.fields.forEach { renderField(out, it, "        ") }
                out.appendLine("    }")
            }
            else -> Unit
        }
    }

    private fun renderInterface(out: StringBuilder, name: String, fields: List<OptionField>, schema: OperationSchema) {
        val merged = merge(fields)
        if (merged.isEmpty()) {
            out.appendLine("    type $name = Record<string, never>;")
            return
        }
        out.appendLine("    interface $name {")
        merged.forEach { renderField(out, it, "        ", schema) }
        out.appendLine("    }")
    }

    /** Fields that share a key (`sampleRate` per format) become one with the union of their choices. zh-CN: 同键字段合并. */
    private fun merge(fields: List<OptionField>): List<OptionField> {
        val result = LinkedHashMap<String, OptionField>()
        for (field in fields) {
            val previous = result[field.key]
            result[field.key] = if (previous == null) field else {
                val a = previous.type
                val b = field.type
                if (a is FieldType.IntChoice && b is FieldType.IntChoice) {
                    previous.copy(type = FieldType.IntChoice((a.values + b.values).distinct().sorted()), appliesWhen = null)
                } else previous
            }
        }
        return result.values.toList()
    }

    private fun renderField(out: StringBuilder, field: OptionField, indent: String, schema: OperationSchema? = null) {
        val doc = describe(field)
        if (doc.isNotEmpty()) out.appendLine("$indent/** $doc */")
        val nestedName = schema?.let { typeName(it) }
        val type = tsType(field, nestedName)
        out.appendLine("$indent${field.key}${if (field.required) "" else "?"}: $type;")
    }

    private fun tsType(field: OptionField, base: String?): String = when (val type = field.type) {
        is FieldType.Integer, is FieldType.Decimal -> "number"
        is FieldType.Choice -> type.values.joinToString(" | ") { "'$it'" }
        is FieldType.IntChoice -> type.values.joinToString(" | ")
        FieldType.Flag -> "boolean"
        is FieldType.FlagOrInteger, is FieldType.FlagOrDecimal -> "boolean | number"
        FieldType.Color -> "string | number"
        is FieldType.Length -> "number | string"
        is FieldType.Text, FieldType.Font, is FieldType.FilePath -> "string"
        is FieldType.Numbers -> "number[]"
        is FieldType.Group -> if (field.key == "style" && base != null) "${base}Style" else (base ?: "") + field.key.replaceFirstChar { it.titlecase(Locale.ROOT) }
        is FieldType.Items -> "${base}Overlay[]"
        is FieldType.PerInput -> "${base}Input[]"
    }

    /** Range, unit, default and conditions in one line. zh-CN: 范围, 单位, 默认值与条件写在一行. */
    fun describe(field: OptionField): String {
        val parts = ArrayList<String>()
        when (val type = field.type) {
            is FieldType.Integer -> parts += "Integer ${number(type.min.toDouble())} - ${number(type.max.toDouble())}${unit(field)}"
            is FieldType.Decimal -> parts += (if (type.minExclusive) "Greater than ${number(type.min)}, at most ${number(type.max)}" else "${number(type.min)} - ${number(type.max)}") + unit(field)
            is FieldType.FlagOrInteger -> parts += "true, false or an integer ${number(type.min.toDouble())} - ${number(type.max.toDouble())}"
            is FieldType.FlagOrDecimal -> parts += "true or a target ${number(type.min)} - ${number(type.max)}${unit(field)}"
            is FieldType.Length -> parts += when (type.lower) {
                LengthLower.SIGNED -> "Pixels -${number(type.maxPixels)} - ${number(type.maxPixels)} or a percentage \"-${number(type.maxPercent)}%\" - \"${number(type.maxPercent)}%\""
                LengthLower.NON_NEGATIVE -> "Pixels 0 - ${number(type.maxPixels)} or a percentage \"0%\" - \"${number(type.maxPercent)}%\""
                LengthLower.POSITIVE -> "Pixels or a percentage, greater than 0, at most ${number(type.maxPixels)} px or ${number(type.maxPercent)}%"
            }
            is FieldType.Text -> parts += "${type.minChars} - ${type.maxChars} characters"
            FieldType.Font -> parts += "sans-serif, serif, monospace or an absolute font file path"
            FieldType.Color -> parts += "#RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer"
            is FieldType.FilePath -> parts += "Absolute path of ${if (type.kind == FileKind.IMAGE) "an image" else "an SRT or ASS file"}"
            is FieldType.Numbers -> parts += "Up to ${type.maxCount} times${unit(field)}"
            is FieldType.PerInput -> parts += "One object per input, in input order"
            is FieldType.Items -> parts += "${type.min} - ${type.max} items"
            else -> Unit
        }
        if (field.uiChoices != null) parts += "multiples of 90, clockwise"
        field.default?.let { parts += "Default ${if (it is Double) number(it) else it}" }
        field.exclusiveGroup?.let { group -> parts += "Excludes the other ${group} keys" }
        field.appliesWhen?.let { condition -> parts += "Only when ${condition.key} is ${if (condition.negate) "not " else ""}${condition.values.sorted().joinToString(" / ")}" }
        return parts.joinToString(". ").let { if (it.isEmpty()) it else "$it." }
    }

    private fun unit(field: OptionField): String = if (field.unit == OptionUnit.NONE) "" else " " + field.unit.symbol

    private fun number(value: Double): String =
        if (value == Math.floor(value) && kotlin.math.abs(value) < 1e16) value.toLong().toString() else value.toString()

}
