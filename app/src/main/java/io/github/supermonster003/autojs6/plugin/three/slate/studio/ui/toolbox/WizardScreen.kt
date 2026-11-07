package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.toolbox

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.FileKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.InputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OperationSchema
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OptionSchemas
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.OutputKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.codegen.ScriptCalls
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.MediaStudioException
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OperationOptions
import io.github.supermonster003.autojs6.plugin.three.slate.studio.core.OutputPaths
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StudioPreferences
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateDialog
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaEntry
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaKind
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.MediaLibrary
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.library.Thumbnail
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.tasks.StudioTasks
import java.io.File
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.plugin.mediastudio.api.MediaStudioOps

/**
 * One wizard page (roadmap D35, P4.2): inputs (system picker or the library), the schema-driven option form with its
 * advanced part, the output path and a live preview of the equivalent script call; submitting runs the request in the
 * shared executor and opens the result page.
 * zh-CN: 一个向导页 (路线图 D35, P4.2): 输入 (系统选择器或媒体库), 由模式驱动的选项表单及其高级部分, 输出路径与等价脚本调用的实时
 * 预览; 提交后请求在共用执行器中运行并打开结果页.
 */
@Composable
internal fun WizardScreen(op: String, initialInput: String?, onBack: () -> Unit, onSubmitted: (String) -> Unit) {
    val schema = OptionSchemas.of(op)
    if (schema == null) {
        SlateScreen(title = op, onBack = onBack) { EmptyState(title = stringResource(R.string.wizard_unknown), modifier = Modifier.padding(it)) }
        return
    }
    val context = LocalContext.current
    val prefs = remember { StudioPreferences.of(context) }
    val form = remember(op) { WizardForm(schema).apply { initialInput?.let(::addInput); applyDefaults(prefs.codec); chooseRequiredAlternative(this) } }
    val labels = Labels.operations.getValue(op)
    val snackbar = remember { SnackbarHostState() }
    val fixFields = stringResource(R.string.wizard_fix_fields)
    val scope = rememberCoroutineScope()
    var showIssues by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var rejection by remember { mutableStateOf<String?>(null) }
    var libraryPicker by remember { mutableStateOf(false) }

    /** Where the next system pick goes: an input index, -1 to add, or a field path. zh-CN: 下一次系统选择的去向. */
    var pickTarget by remember { mutableStateOf<String?>(null) }
    val noPath = stringResource(R.string.wizard_no_path)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val target = pickTarget ?: return@rememberLauncherForActivityResult
        pickTarget = null
        if (uri == null) return@rememberLauncherForActivityResult
        val path = MediaLibrary.pathOf(context, uri)
        if (path == null) {
            scope.launch { snackbar.showSnackbar(noPath) }
            return@rememberLauncherForActivityResult
        }
        when {
            target == TARGET_ADD -> form.addInput(path)
            target.startsWith(TARGET_INPUT) -> target.removePrefix(TARGET_INPUT).toIntOrNull()?.takeIf { it in form.inputs.indices }?.let { form.replaceInput(it, path) }
            else -> form.set(target, path)
        }
    }
    val pick: (String, Array<String>) -> Unit = { target, types ->
        pickTarget = target
        runCatching { picker.launch(types) }.onFailure { pickTarget = null }
    }

    val built = form.build()
    val issues = if (showIssues) built.issues else emptyMap()
    SlateScreen(
        title = stringResource(labels.title),
        onBack = onBack,
        snackbarHostState = snackbar,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    rejection?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr), color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp).testTag("slate.wizard.rejection"))
                    }
                    Button(
                        onClick = {
                            showIssues = true
                            rejection = null
                            if (!built.valid) {
                                scope.launch { snackbar.showSnackbar(fixFields) }
                                return@Button
                            }
                            val request = form.request(built).let { request ->
                                val output = request.output ?: customOutput(prefs, schema, request.inputs, request.options)
                                if (output == request.output) request else request.copy(output = output, code = ScriptCalls.code(schema, request.inputs, request.options, output))
                            }
                            rejection = precheck(schema, request.inputs.size, request.options)
                            if (rejection != null) return@Button
                            val key = StudioTasks.submit(context, request) { BitRatePlanner.apply(it, prefs.bitRatePolicy) }
                            onSubmitted(key)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("slate.wizard.submit"),
                    ) { Text(stringResource(R.string.wizard_submit)) }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("slate.wizard")) {
            Text(stringResource(labels.summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SectionHeader(stringResource(if (schema.inputs.max > 1) R.string.wizard_inputs else R.string.wizard_input))
            InputsEditor(
                form = form,
                schema = schema,
                issue = issues[WizardForm.INPUTS],
                onBrowse = { target -> pick(target, mimeTypes(schema.inputs.kind)) },
                onLibrary = { libraryPicker = true },
            )
            if (schema.fields.any { !it.advanced }) {
                SectionHeader(stringResource(R.string.wizard_options))
                FieldList(form, schema.fields, "", null, issues, advanced = false) { path, kind -> pick(path, fileTypes(kind)) }
            }
            if (schema.fields.any { it.advanced }) {
                Row(
                    Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced }.padding(horizontal = 24.dp, vertical = 12.dp).testTag("slate.wizard.advanced"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.wizard_advanced), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    Icon(painterResource(if (showAdvanced) R.drawable.ic_arrow_upward_24 else R.drawable.ic_arrow_downward_24), contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
                if (showAdvanced) FieldList(form, schema.fields, "", null, issues, advanced = true) { path, kind -> pick(path, fileTypes(kind)) }
            }
            SectionHeader(stringResource(R.string.wizard_output))
            OutlinedTextField(
                value = form.output,
                onValueChange = { form.output = it },
                singleLine = true,
                label = { Text(stringResource(if (schema.output == OutputKind.FOLDER) R.string.wizard_output_folder else R.string.wizard_output_file)) },
                placeholder = { Text(stringResource(R.string.wizard_output_default)) },
                supportingText = issues[WizardForm.OUTPUT]?.let { { Text(issueText(it), color = MaterialTheme.colorScheme.error) } }
                    ?: { Text(prefs.outputFolder?.let { stringResource(R.string.wizard_output_custom_folder, it) } ?: stringResource(R.string.wizard_output_hint)) },
                isError = issues[WizardForm.OUTPUT] != null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).testTag("slate.wizard.output"),
            )
            SectionHeader(stringResource(R.string.wizard_code))
            val code = remember(built, form.inputs.toList(), form.output) {
                runCatching { form.request(built).code }.getOrNull()
            }
            CodeCard(code ?: stringResource(R.string.wizard_code_pending), copyable = code != null, tag = "slate.wizard.code")
            Spacer(Modifier.height(24.dp))
        }
    }
    if (libraryPicker) {
        LibraryPickerDialog(
            kind = schema.inputs.kind,
            onDismiss = { libraryPicker = false },
            onPick = { path -> libraryPicker = false; form.addInput(path) },
        )
    }
}

@Composable
private fun InputsEditor(form: WizardForm, schema: OperationSchema, issue: FieldIssue?, onBrowse: (String) -> Unit, onLibrary: () -> Unit) {
    form.inputs.forEachIndexed { index, input ->
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 4.dp, bottom = 4.dp).testTag("slate.wizard.input.$index"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(File(input.path).name, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                    maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                Text(input.path, style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            }
            if (schema.inputs.max > 1) {
                IconButton(onClick = { form.moveInput(index, -1) }, enabled = index > 0) {
                    Icon(painterResource(R.drawable.ic_arrow_upward_24), contentDescription = stringResource(R.string.wizard_move_up))
                }
                IconButton(onClick = { form.moveInput(index, 1) }, enabled = index < form.inputs.lastIndex) {
                    Icon(painterResource(R.drawable.ic_arrow_downward_24), contentDescription = stringResource(R.string.wizard_move_down))
                }
            } else {
                IconButton(onClick = { onBrowse("$TARGET_INPUT$index") }, modifier = Modifier.testTag("slate.wizard.input.$index.replace")) {
                    Icon(painterResource(R.drawable.ic_folder_24), contentDescription = stringResource(R.string.wizard_pick_file))
                }
            }
            IconButton(onClick = { form.removeInput(index) }, modifier = Modifier.testTag("slate.wizard.input.$index.remove")) {
                Icon(painterResource(R.drawable.ic_close_24), contentDescription = stringResource(R.string.wizard_remove))
            }
        }
    }
    if (issue != null) {
        Text(issueText(issue), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp))
    }
    if (form.inputs.size < schema.inputs.max) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onBrowse(TARGET_ADD) }, modifier = Modifier.testTag("slate.wizard.browse")) {
                Icon(painterResource(R.drawable.ic_folder_24), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wizard_browse))
            }
            if (schema.inputs.kind != InputKind.SUBTITLE) {
                OutlinedButton(onClick = onLibrary, modifier = Modifier.testTag("slate.wizard.library")) {
                    Icon(painterResource(R.drawable.ic_movie_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wizard_from_library))
                }
            }
        }
    }
}

/** Monospace code with a copy button. zh-CN: 带复制按钮的等宽代码. */
@Composable
internal fun CodeCard(code: String, copyable: Boolean, tag: String) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp, end = 4.dp)) {
            Text(
                code,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp).testTag(tag),
            )
            if (copyable) {
                TextButton(
                    onClick = {
                        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("code", code))
                    },
                    modifier = Modifier.align(Alignment.End).testTag("$tag.copy"),
                ) {
                    Icon(painterResource(R.drawable.ic_content_copy_24), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wizard_copy_code))
                }
            }
        }
    }
}

/** Library media of the input kind. zh-CN: 符合输入类型的媒体库条目. */
@Composable
private fun LibraryPickerDialog(kind: InputKind, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<MediaEntry>?>(null) }
    LaunchedEffect(Unit) {
        entries = withContext(Dispatchers.IO) {
            MediaLibrary.query(context, MediaLibrary.folders(context)).filter { entry ->
                when (kind) {
                    InputKind.VIDEO -> entry.kind == MediaKind.VIDEO
                    InputKind.AUDIO -> entry.kind == MediaKind.AUDIO
                    InputKind.MEDIA -> entry.kind == MediaKind.VIDEO || entry.kind == MediaKind.AUDIO
                    InputKind.SUBTITLE -> false
                }
            }
        }
    }
    SlateDialog(
        title = stringResource(R.string.wizard_from_library),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.ui_dialog_close),
        onConfirm = onDismiss,
        dismissText = null,
        tag = "slate.dialog.library",
    ) {
        val list = entries
        when {
            list == null -> Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Text(stringResource(R.string.library_empty_title), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            else -> list.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(entry.path) }.padding(horizontal = 24.dp, vertical = 8.dp).testTag("slate.dialog.library.item"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Thumbnail(entry, Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(entry.name, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Chooses the first alternative of an exclusive group when the operation needs one (empty options are refused).
 * zh-CN: 操作需要某个互斥选择时 (空选项被拒绝) 预选互斥组的第一个选择.
 */
private fun chooseRequiredAlternative(form: WizardForm) {
    val schema = form.schema
    if (schema.op == OperationSchema.SUBTITLES_CONVERT) return
    val accepted = runCatching { OperationOptions.parse(schema.op, schema.inputs.min, emptyMap()) }.isSuccess
    if (accepted) return
    val field = schema.fields.firstOrNull { it.exclusiveGroup != null && it.alternative != null } ?: return
    form.alternatives[field.exclusiveGroup!!] = field.alternative!!
}

/**
 * The plugin's own validation before submitting, so a refused request stays on the form with the plugin's detail.
 * zh-CN: 提交前用插件自身的校验, 被拒绝的请求留在表单上并显示插件的详情.
 */
private fun precheck(schema: OperationSchema, inputCount: Int, options: Map<String, Any?>): String? {
    val op = if (schema.op == OperationSchema.SUBTITLES_CONVERT) return null else schema.op
    return try {
        OperationOptions.parse(op, inputCount, options)
        null
    } catch (e: MediaStudioException) {
        "${e.code}: ${e.detail}"
    } catch (e: IllegalArgumentException) {
        e.message
    }
}

/**
 * The output under the Studio output folder (D37) with the default file name of D14, or null to let the plugin choose.
 * zh-CN: Studio 输出目录 (D37) 下按 D14 默认文件名生成的输出; 为 null 时由插件选择.
 */
private fun customOutput(prefs: StudioPreferences, schema: OperationSchema, inputs: List<String>, options: Map<String, Any?>): String? {
    val folder = prefs.outputFolder ?: return null
    val source = inputs.singleOrNull()
    val op = if (schema.op == OperationSchema.SUBTITLES_CONVERT) MediaStudioOps.SUBTITLES_WRITE else schema.op
    val base = OutputPaths.defaultBaseName(source, op, System.currentTimeMillis(), TimeZone.getDefault())
    val extension = when (schema.output) {
        OutputKind.VIDEO -> "mp4"
        OutputKind.GIF -> "gif"
        OutputKind.AUDIO -> (options["format"] as? String) ?: "m4a"
        OutputKind.IMAGE -> ((options["format"] as? String) ?: schema.field("format")?.default as? String ?: "png").lowercase(Locale.ROOT)
        OutputKind.SUBTITLE -> (options["format"] as? String) ?: "srt"
        OutputKind.FOLDER -> null
    }
    val candidate = folder.trimEnd('/') + "/" + base + (extension?.let { ".$it" } ?: "")
    return OutputPaths.uniquePath(candidate, folder = extension == null) { File(it).exists() }
}

private fun mimeTypes(kind: InputKind): Array<String> = when (kind) {
    InputKind.VIDEO -> arrayOf("video/*")
    InputKind.AUDIO -> arrayOf("audio/*")
    InputKind.MEDIA -> arrayOf("video/*", "audio/*")
    InputKind.SUBTITLE -> arrayOf("*/*")
}

private fun fileTypes(kind: FileKind): Array<String> = when (kind) {
    FileKind.IMAGE -> arrayOf("image/*")
    FileKind.SUBTITLE -> arrayOf("*/*")
}

private const val TARGET_ADD = "#add"
private const val TARGET_INPUT = "#input:"
