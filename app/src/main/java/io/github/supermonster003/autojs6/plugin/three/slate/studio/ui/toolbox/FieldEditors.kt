package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.FieldType
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.FileKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionField
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionUnit
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.Colors
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OverlayOptions
import java.io.File

/**
 * Editors of [WizardForm] fields: one per [FieldType], all labels and limits from the schema (roadmap P4.2). [advanced]
 * selects which fields of a list show (null: all); [onPick] opens the system picker for a file field path.
 * zh-CN: [WizardForm] 字段的编辑器: 每种 [FieldType] 一个, 名称与限制都来自模式 (路线图 P4.2). [advanced] 选择列表中显示的字段
 * (null: 全部); [onPick] 为文件字段路径打开系统选择器.
 */
@Composable
internal fun FieldList(
    form: WizardForm,
    fields: List<OptionField>,
    prefix: String,
    parent: String?,
    issues: Map<String, FieldIssue>,
    advanced: Boolean?,
    onPick: (path: String, kind: FileKind) -> Unit,
) {
    val shownGroups = HashSet<String>()
    for (field in fields) {
        if (advanced != null && field.advanced != advanced) continue
        val group = field.exclusiveGroup
        if (group != null && shownGroups.add(group)) {
            AlternativeChips(form, fields, prefix, group)
        }
        if (!form.visible(field, prefix, fields)) continue
        FieldEditor(form, field, prefix, parent, issues, onPick)
    }
}

/** Chips choosing one alternative of an exclusive group, or none. zh-CN: 选择互斥组中一个选择 (或不选) 的标签. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlternativeChips(form: WizardForm, fields: List<OptionField>, prefix: String, group: String) {
    val alternatives = fields.filter { it.exclusiveGroup == group }.mapNotNull { it.alternative }.distinct()
    val path = prefix + group
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.wizard_choose_one), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.alternatives[path] == null,
                onClick = { form.alternatives.remove(path) },
                label = { Text(stringResource(R.string.wizard_not_set)) },
                modifier = Modifier.testTag("slate.wizard.alt.$path.none"),
            )
            alternatives.forEach { alternative ->
                FilterChip(
                    selected = form.alternatives[path] == alternative,
                    onClick = { form.alternatives[path] = alternative },
                    label = { Text(Labels.alternatives["$group.$alternative"]?.let { stringResource(it) } ?: alternative) },
                    modifier = Modifier.testTag("slate.wizard.alt.$path.$alternative"),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldEditor(
    form: WizardForm,
    field: OptionField,
    prefix: String,
    parent: String?,
    issues: Map<String, FieldIssue>,
    onPick: (path: String, kind: FileKind) -> Unit,
) {
    val path = prefix + field.key
    val label = fieldLabel(parent, field)
    val issue = issues[path]
    val tag = "slate.wizard.field.$path"
    when (val type = field.type) {
        is FieldType.Group -> {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp))
            Column(Modifier.padding(start = 12.dp)) {
                FieldList(form, type.fields, "$path/", field.key, issues, null, onPick)
            }
        }
        is FieldType.Items -> ItemsEditor(form, field, type, path, label, issues, issue, onPick)
        is FieldType.PerInput -> {
            form.inputs.forEachIndexed { index, input ->
                Text(
                    stringResource(R.string.wizard_track, index + 1, File(input.path).name),
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
                )
                Column(Modifier.padding(start = 12.dp)) {
                    FieldList(form, type.fields, "$path@${input.id}/", field.key, issues, null, onPick)
                }
            }
        }
        FieldType.Flag -> {
            val checked = (form.values[path] ?: field.default?.toString()) == WizardForm.TRUE
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = { next -> setOrDefault(form, field, path, next.toString()) })
            }
        }
        is FieldType.Choice -> ChoiceEditor(form, field, path, label, type.values, issue, tag)
        is FieldType.IntChoice -> ChoiceEditor(form, field, path, label, type.values.map { it.toString() }, issue, tag)
        is FieldType.Integer -> {
            val choices = field.uiChoices
            if (choices != null) ChoiceEditor(form, field, path, label, choices.map { it.toString() }, issue, tag)
            else TextEditor(form, field, path, label, issue, tag, KeyboardType.Number, rangeHint(type.min.toString(), type.max.toString(), type.max.toDouble()))
        }
        is FieldType.Decimal -> TextEditor(form, field, path, label, issue, tag, KeyboardType.Decimal,
            rangeHint(WizardForm.number(type.min), WizardForm.number(type.max), type.max))
        is FieldType.FlagOrInteger, is FieldType.FlagOrDecimal -> FlagOrNumberEditor(form, field, path, label, issue, tag)
        FieldType.Color -> {
            val color = form.values[path]?.let { Colors.parse(it) }
            TextEditor(form, field, path, label, issue, tag, KeyboardType.Ascii, stringResource(R.string.wizard_color_hint)) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (color != null) Color(color) else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
            }
        }
        is FieldType.Length -> TextEditor(form, field, path, label, issue, tag, KeyboardType.Ascii, stringResource(R.string.wizard_length_hint, "50%"))
        is FieldType.Text -> TextEditor(form, field, path, label, issue, tag, KeyboardType.Text, null, singleLine = false)
        FieldType.Font -> {
            TextEditor(form, field, path, label, issue, tag, KeyboardType.Uri, stringResource(R.string.wizard_font_hint))
            FlowRow(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayOptions.FONT_FAMILIES.forEach { family ->
                    FilterChip(selected = form.values[path] == family, onClick = { form.set(path, family) }, label = { Text(family) })
                }
            }
        }
        is FieldType.FilePath -> {
            TextEditor(form, field, path, label, issue, tag, KeyboardType.Uri, null) {
                IconButton(onClick = { onPick(path, type.kind) }, modifier = Modifier.testTag("$tag.pick")) {
                    Icon(painterResource(R.drawable.ic_folder_24), contentDescription = stringResource(R.string.wizard_pick_file))
                }
            }
        }
        is FieldType.Numbers -> TextEditor(form, field, path, label, issue, tag, KeyboardType.Ascii, stringResource(R.string.wizard_numbers_hint, type.maxCount))
    }
}

/** Sets [value], or clears it when it equals the default (the call stays minimal). zh-CN: 设置值, 与默认值相同时清除. */
private fun setOrDefault(form: WizardForm, field: OptionField, path: String, value: String) {
    if (!field.required && value == field.default?.toString()) form.values.remove(path) else form.values[path] = value
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceEditor(form: WizardForm, field: OptionField, path: String, label: String, values: List<String>, issue: FieldIssue?, tag: String) {
    val selected = form.values[path] ?: field.default?.toString()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).testTag(tag)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { setOrDefault(form, field, path, value) },
                    label = { Text(choiceLabel(field.key, value, field.unit)) },
                    modifier = Modifier.testTag("$tag.$value"),
                )
            }
            if (!field.required && field.default == null && form.values[path] != null) {
                FilterChip(selected = false, onClick = { form.values.remove(path) }, label = { Text(stringResource(R.string.wizard_not_set)) })
            }
        }
        IssueText(issue)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagOrNumberEditor(form: WizardForm, field: OptionField, path: String, label: String, issue: FieldIssue?, tag: String) {
    val raw = form.values[path]
    val mode = when (raw ?: field.default?.toString()) {
        null -> null
        WizardForm.TRUE -> WizardForm.TRUE
        WizardForm.FALSE -> WizardForm.FALSE
        else -> NUMBER
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag(tag)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 24.dp))
        Labels.hints[field.key]?.let { hint ->
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp))
        }
        FlowRow(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (field.default == null) {
                FilterChip(selected = mode == null, onClick = { form.values.remove(path) }, label = { Text(stringResource(R.string.wizard_not_set)) })
            }
            FilterChip(selected = mode == WizardForm.TRUE, onClick = { setOrDefault(form, field, path, WizardForm.TRUE) },
                label = { Text(stringResource(R.string.wizard_flag_on)) }, modifier = Modifier.testTag("$tag.true"))
            FilterChip(selected = mode == WizardForm.FALSE, onClick = { setOrDefault(form, field, path, WizardForm.FALSE) },
                label = { Text(stringResource(R.string.wizard_flag_off)) }, modifier = Modifier.testTag("$tag.false"))
            FilterChip(selected = mode == NUMBER, onClick = { if (mode != NUMBER) form.values[path] = NUMBER_PLACEHOLDER },
                label = { Text(stringResource(R.string.wizard_flag_value)) }, modifier = Modifier.testTag("$tag.number"))
        }
        if (mode == NUMBER) {
            val type = field.type
            val hint = when (type) {
                is FieldType.FlagOrInteger -> rangeHint(type.min.toString(), type.max.toString(), type.max.toDouble())
                is FieldType.FlagOrDecimal -> rangeHint(WizardForm.number(type.min), WizardForm.number(type.max), type.max)
                else -> null
            }
            OutlinedTextField(
                value = raw?.takeIf { it != NUMBER_PLACEHOLDER }.orEmpty(),
                onValueChange = { form.values[path] = it.ifEmpty { NUMBER_PLACEHOLDER } },
                singleLine = true,
                label = { Text(unitLabel(label, field.unit)) },
                supportingText = (issue?.let { { Text(issueText(it), color = MaterialTheme.colorScheme.error) } } ?: hint?.let { { Text(it) } }),
                isError = issue != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("$tag.value"),
            )
        } else {
            IssueText(issue, Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
private fun TextEditor(
    form: WizardForm,
    field: OptionField,
    path: String,
    label: String,
    issue: FieldIssue?,
    tag: String,
    keyboard: KeyboardType,
    hint: String?,
    singleLine: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val default = field.default?.let { defaultText(field, it) }
    val support = listOfNotNull(default?.let { stringResource(R.string.wizard_default_value, it) }, hint).joinToString("  ")
    OutlinedTextField(
        value = form.values[path].orEmpty(),
        onValueChange = { form.set(path, it) },
        singleLine = singleLine,
        label = { Text(unitLabel(label, field.unit) + if (field.required) " *" else "") },
        placeholder = default?.let { { Text(it) } },
        supportingText = if (issue != null) {
            { Text(issueText(issue), color = MaterialTheme.colorScheme.error) }
        } else if (support.isNotEmpty()) {
            { Text(support) }
        } else null,
        isError = issue != null,
        trailingIcon = trailing,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).testTag(tag),
    )
}

@Composable
private fun ItemsEditor(
    form: WizardForm,
    field: OptionField,
    type: FieldType.Items,
    path: String,
    label: String,
    issues: Map<String, FieldIssue>,
    issue: FieldIssue?,
    onPick: (path: String, kind: FileKind) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp))
    IssueText(issue, Modifier.padding(horizontal = 24.dp))
    form.items[path].orEmpty().forEachIndexed { index, item ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("slate.wizard.item.$index"),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.wizard_item_title, index + 1, choiceLabel(type.discriminator, item.variant, field.unit)),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { form.removeItem(path, item.id) }, modifier = Modifier.testTag("slate.wizard.item.$index.remove")) {
                        Icon(painterResource(R.drawable.ic_delete_24), contentDescription = stringResource(R.string.wizard_remove))
                    }
                }
                FieldList(form, type.variants.getValue(item.variant), "$path#${item.id}/", field.key, issues, null, onPick)
            }
        }
    }
    Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        type.variants.keys.forEach { variant ->
            OutlinedButton(
                onClick = { form.addItem(path, variant, type.max) },
                enabled = form.items[path].orEmpty().size < type.max,
                modifier = Modifier.testTag("slate.wizard.add.$variant"),
            ) {
                Icon(painterResource(R.drawable.ic_add_24), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wizard_add_item, choiceLabel(type.discriminator, variant, field.unit)))
            }
        }
    }
}

@Composable
private fun IssueText(issue: FieldIssue?, modifier: Modifier = Modifier) {
    if (issue != null) Text(issueText(issue), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = modifier)
}

@Composable
internal fun issueText(issue: FieldIssue): String = when (issue) {
    FieldIssue.Required -> stringResource(R.string.wizard_issue_required)
    FieldIssue.NotNumber -> stringResource(R.string.wizard_issue_number)
    FieldIssue.NotInteger -> stringResource(R.string.wizard_issue_integer)
    is FieldIssue.OutOfRange -> stringResource(R.string.wizard_issue_range, issue.min, issue.max)
    FieldIssue.NotColor -> stringResource(R.string.wizard_issue_color)
    FieldIssue.NotLength -> stringResource(R.string.wizard_issue_length)
    is FieldIssue.TextLength -> stringResource(R.string.wizard_issue_text_length, issue.min, issue.max)
    FieldIssue.NotAbsolute -> stringResource(R.string.wizard_issue_absolute)
    is FieldIssue.TooMany -> stringResource(R.string.wizard_issue_too_many, issue.max)
    is FieldIssue.Inputs -> if (issue.min == issue.max) stringResource(R.string.wizard_issue_inputs_exact, issue.min)
    else stringResource(R.string.wizard_issue_inputs_range, issue.min, issue.max)
}

@Composable
private fun fieldLabel(parent: String?, field: OptionField): String {
    val id = Labels.field(parent, field.key)
    return if (id != 0) stringResource(id) else field.key
}

@Composable
private fun unitLabel(label: String, unit: OptionUnit): String = if (unit == OptionUnit.NONE) label else "$label ($UNIT_START${unit.symbol}$UNIT_END)"

/** A maximum from [UNBOUNDED] up is a technical limit (time and pixel fields), so only the minimum is shown. zh-CN: 不小于 [UNBOUNDED] 的上限只是技术上限 (时间与像素字段), 只显示下限. */
@Composable
private fun rangeHint(min: String, max: String, maxValue: Double): String =
    if (maxValue >= UNBOUNDED) stringResource(R.string.wizard_min_hint, min) else stringResource(R.string.wizard_range_hint, min, max)

private const val UNBOUNDED = Int.MAX_VALUE.toDouble()

@Composable
internal fun choiceLabel(key: String, value: String, unit: OptionUnit): String {
    val id = Labels.choices[Labels.choiceKey(key, value)]
    return when {
        id != null -> stringResource(id)
        unit != OptionUnit.NONE -> "$value ${unit.symbol}"
        else -> Labels.plainChoice(value)
    }
}

private fun defaultText(field: OptionField, value: Any): String = when (value) {
    is Double -> WizardForm.number(value)
    is Boolean -> value.toString()
    else -> value.toString()
}

private const val NUMBER = "#number"

/** Kept while the number box of a flag-or-number field is empty. zh-CN: 布尔或数值字段的数值框为空时保留的占位值. */
internal const val NUMBER_PLACEHOLDER = " "

/** Units are Latin symbols: isolate them inside right-to-left labels. zh-CN: 单位是拉丁符号, 在从右到左的名称中隔离. */
private const val UNIT_START = "\u2068"
private const val UNIT_END = "\u2069"
