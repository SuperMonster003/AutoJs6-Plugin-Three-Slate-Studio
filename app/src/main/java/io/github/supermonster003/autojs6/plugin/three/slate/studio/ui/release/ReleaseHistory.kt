package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.TopBarAction
import java.io.IOException
import java.util.Locale

/**
 * The bundled changelog for a locale (`assets/doc/CHANGELOG-<lang>.md`, roadmap P3.3), English otherwise. Pure Kotlin.
 * zh-CN: 某区域对应的内置 changelog (`assets/doc/CHANGELOG-<lang>.md`, 路线图 P3.3), 其他区域用英文. 纯 Kotlin.
 */
internal object ReleaseHistoryAssets {
    fun assetFor(locale: Locale): String = when (locale.language.lowercase(Locale.ROOT)) {
        "zh" -> when {
            locale.script.equals("Hans", ignoreCase = true) -> "doc/CHANGELOG-zh-Hans.md"
            locale.country.equals("HK", ignoreCase = true) || locale.country.equals("MO", ignoreCase = true) -> "doc/CHANGELOG-zh-Hant-HK.md"
            locale.script.equals("Hant", ignoreCase = true) || locale.country.equals("TW", ignoreCase = true) -> "doc/CHANGELOG-zh-Hant-TW.md"
            else -> "doc/CHANGELOG-zh-Hans.md"
        }
        "ar", "es", "fr", "ja", "ko", "ru" -> "doc/CHANGELOG-${locale.language.lowercase(Locale.ROOT)}.md"
        else -> FALLBACK
    }

    /**
     * The assets to try in order: the one for [locale], then English (release history specification section 3).
     * zh-CN: 依次尝试的文件: [locale] 对应的文件, 然后是英文 (发行历史规范第 3 节).
     */
    fun candidatesFor(locale: Locale): List<String> = listOf(assetFor(locale), FALLBACK).distinct()

    private const val FALLBACK = "doc/CHANGELOG-en.md"
}

internal data class ReleaseItem(val tag: String?, val text: String)

internal data class ReleaseGroup(val tag: String?, val items: List<String>)

internal data class ReleaseEntry(val version: String, val date: String?, val items: List<ReleaseItem>) {
    /**
     * The items by category, each category once, in the order the categories first appear (untagged: tag null).
     * zh-CN: 按类别分组的条目, 每个类别一次, 按类别首次出现的顺序 (无标签: tag 为 null).
     */
    val groups: List<ReleaseGroup>
        get() = items.groupBy { it.tag }.map { (tag, grouped) -> ReleaseGroup(tag, grouped.map { it.text }) }

    /** How many items each category has, in the same order; untagged items are not counted. zh-CN: 各类别的条目数, 顺序相同; 无标签的条目不计. */
    val categoryCounts: List<Pair<String, Int>>
        get() = groups.mapNotNull { group -> group.tag?.let { it to group.items.size } }
}

/**
 * Parses the generated changelog dialect: `# vX.Y.Z` headings, `###### date` lines and `` * `tag` text `` bullets;
 * anything else (title, separators) is ignored. Pure Kotlin, the same rules as 3-Maple Image.
 * zh-CN: 解析生成的 changelog 方言: `# vX.Y.Z` 标题, `###### 日期` 行与 `` * `标签` 文字 `` 条目; 其余内容 (标题, 分隔线) 忽略. 纯 Kotlin, 规则与
 * 3-Maple Image 相同.
 */
internal object ReleaseHistoryParser {
    private val BULLET = Regex("^\\*\\s+(?:`([^`]+)`\\s*)?(.*)$")
    private val VERSION = Regex("^#{1,2}\\s+(v\\S+)\\s*$")

    fun parse(markdown: String): List<ReleaseEntry> {
        val entries = ArrayList<ReleaseEntry>()
        var version: String? = null
        var date: String? = null
        var items = ArrayList<ReleaseItem>()
        fun flush() {
            version?.let { entries += ReleaseEntry(it, date, items.toList()) }
        }
        for (raw in markdown.lineSequence()) {
            val line = raw.trim()
            val heading = VERSION.matchEntire(line)
            when {
                heading != null -> {
                    flush()
                    version = heading.groupValues[1]
                    date = null
                    items = ArrayList()
                }
                line.startsWith("###### ") -> date = line.removePrefix("###### ").trim()
                line.startsWith("* ") && version != null -> {
                    val match = BULLET.matchEntire(line) ?: continue
                    val tag = match.groupValues[1].takeIf { it.isNotEmpty() }
                    val text = match.groupValues[2].trim()
                    if (tag != null || text.isNotEmpty()) items += ReleaseItem(tag, text)
                }
            }
        }
        flush()
        return entries
    }
}

/**
 * The releases of the first candidate asset that holds at least one, so a missing, blank or unparsable localized
 * changelog falls back to the English one; empty when none of them does (the page shows its empty state).
 * zh-CN: 第一个至少含一个版本的候选文件中的版本, 因此缺失, 空白或解析不出版本的本地化 changelog 回退到英文; 都没有时为空 (页面显示空状态).
 */
internal fun loadReleaseHistory(candidates: List<String>, loader: (String) -> String): List<ReleaseEntry> {
    for (path in candidates) {
        val markdown = try { loader(path) } catch (_: IOException) { continue }
        val entries = ReleaseHistoryParser.parse(markdown)
        if (entries.isNotEmpty()) return entries
    }
    return emptyList()
}

/** A link of a release note, as the range of its text in [ReleaseHistoryInline.text] (Q160 A). zh-CN: 发行说明里的一个链接, 以其文字在 [ReleaseHistoryInline.text] 中的区间表示. */
internal data class ReleaseHistoryLink(val start: Int, val end: Int, val url: String)

/** The text of a release note without the link markup, with its links. zh-CN: 去掉链接标记后的发行说明文字及其链接. */
internal data class ReleaseHistoryInline(val text: String, val links: List<ReleaseHistoryLink>)

/**
 * Turns the markdown links of a release note (`[text](https://...)`) into their text (plugin-center Q160 A). A pair of
 * `_` around a link (the italic spelling of the issue links) and a pair of backticks around its text go too, so
 * `` _[`issue #575`](url)_ `` reads "issue #575". Only http and https links count; a note without one stays as it is.
 * zh-CN: 把发行说明里的 Markdown 链接 (`[文字](https://...)`) 换成其文字 (plugin-center Q160 A); 链接外成对的 `_` (issue 链接的斜体写法) 与文字外成对的反引号
 * 一并去掉. 只认 http 与 https 链接; 没有链接的说明原样保留.
 */
internal object ReleaseHistoryInlineParser {
    private val LINK_PATTERN = Regex("""([_`]?)\[(`?)([^\]]+?)\2\]\((https?://[^\s)]+)\)\1""")

    fun parse(text: String): ReleaseHistoryInline {
        val matches = LINK_PATTERN.findAll(text).toList()
        if (matches.isEmpty()) return ReleaseHistoryInline(text, emptyList())
        val plain = StringBuilder()
        val links = mutableListOf<ReleaseHistoryLink>()
        var last = 0
        for (match in matches) {
            plain.append(text, last, match.range.first)
            val start = plain.length
            plain.append(match.groupValues[3])
            links.add(ReleaseHistoryLink(start, plain.length, match.groupValues[4]))
            last = match.range.last + 1
        }
        plain.append(text, last, text.length)
        return ReleaseHistoryInline(plain.toString(), links)
    }
}

/**
 * Opens the release history page; the settings row and the "Release history" button of the update dialog both lead here.
 * zh-CN: 打开发行历史页; 设置页的那一行与更新对话框的 "发行历史" 按钮都打开这里.
 */
internal fun Context.openReleaseHistory() {
    startActivity(Intent(this, ReleaseHistoryActivity::class.java))
}

/**
 * The bundled release history (roadmap P3.3; plugin-center P21.1, release history specification): the title "Release
 * history" with the plugin name as the subtitle, one expandable group per released version.
 * zh-CN: 内置发行历史 (路线图 P3.3; plugin-center P21.1, 发行历史规范): 标题 "发行历史", 子标题为插件名称, 每个发行版本一个可展开的分组.
 */
class ReleaseHistoryActivity : StandaloneActivity() {

    private val entries: List<ReleaseEntry> by lazy {
        loadReleaseHistory(ReleaseHistoryAssets.candidatesFor(resources.configuration.locales[0])) { path ->
            assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
        }
    }

    @Composable
    override fun Content() = ReleaseHistoryScreen(entries, onBack = ::finish)
}

/** The expanded versions survive rotation and process recreation. zh-CN: 展开的版本在旋转与进程重建后保留. */
private val ExpandedVersionsSaver = Saver<Set<String>, ArrayList<String>>(save = { ArrayList(it) }, restore = { it.toSet() })

/**
 * Geometry of the release history page (release history specification section 4). zh-CN: 发行历史页的几何尺寸 (发行历史规范第 4 节).
 */
internal object ReleaseHistoryDefaults {
    val horizontalPadding = 24.dp
    val headerEndPadding = 16.dp
    val verticalPadding = 16.dp
    val bulletSlot = 20.dp
    val bulletSize = 5.dp
    val bulletStart = 6.dp
    val firstGroupSpacing = 10.dp
    val groupSpacing = 14.dp
    val itemSpacing = 6.dp

    /** The empty state sits 48 dp below the top bar; [EmptyState] brings 32 of them. zh-CN: 空状态距顶栏 48 dp, 其中 32 来自 [EmptyState]. */
    val emptyStateSpacer = 16.dp
}

@Composable
internal fun ReleaseHistoryScreen(entries: List<ReleaseEntry>, onBack: () -> Unit) {
    var expanded by rememberSaveable(stateSaver = ExpandedVersionsSaver) {
        mutableStateOf(entries.firstOrNull()?.let { setOf(it.version) }.orEmpty())
    }
    val allExpanded = entries.isNotEmpty() && entries.all { it.version in expanded }
    SlateScreen(
        title = stringResource(R.string.release_history),
        subtitle = stringResource(R.string.app_name),
        onBack = onBack,
        actions = {
            if (entries.isNotEmpty()) {
                TopBarAction(
                    icon = if (allExpanded) R.drawable.ic_unfold_less_24 else R.drawable.ic_unfold_more_24,
                    description = stringResource(if (allExpanded) R.string.release_history_collapse_all else R.string.release_history_expand_all),
                    tag = "slate.releaseHistory.toggleAll",
                    onClick = { expanded = if (allExpanded) emptySet() else entries.mapTo(mutableSetOf()) { it.version } },
                )
            }
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Spacer(Modifier.height(ReleaseHistoryDefaults.emptyStateSpacer))
                EmptyState(title = stringResource(R.string.release_history_unavailable), icon = R.drawable.ic_warning_24)
            }
            return@SlateScreen
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("slate.releaseHistory.list")) {
            itemsIndexed(entries, key = { _, entry -> entry.version }) { index, entry ->
                if (index > 0) Hairline(insetStart = ReleaseHistoryDefaults.horizontalPadding)
                val isExpanded = entry.version in expanded
                ReleaseEntryItem(entry, expanded = isExpanded, onToggle = {
                    expanded = if (isExpanded) expanded - entry.version else expanded + entry.version
                })
            }
        }
    }
}

/**
 * One release: the header (version, date, chevron; the category counts while collapsed) is one focus stop that toggles
 * the release, and the expanded items are read in one go after it.
 * zh-CN: 一个版本: 版本头 (版本号, 日期, 箭头; 收起时还有各类别计数) 是一个切换展开的焦点, 展开的各条在它之后一次读完.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseEntryItem(entry: ReleaseEntry, expanded: Boolean, onToggle: () -> Unit) {
    val state = stringResource(if (expanded) R.string.release_history_expanded else R.string.release_history_collapsed)
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .testTag("slate.releaseHistory.entry.${entry.version}")
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = state }
                .padding(
                    start = ReleaseHistoryDefaults.horizontalPadding,
                    end = ReleaseHistoryDefaults.headerEndPadding,
                    top = ReleaseHistoryDefaults.verticalPadding,
                    bottom = if (expanded) 0.dp else ReleaseHistoryDefaults.verticalPadding,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.version, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                entry.date?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.width(8.dp))
                Icon(
                    painterResource(R.drawable.ic_expand_more_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).rotate(chevronRotation),
                )
            }
            val counts = entry.categoryCounts
            if (!expanded && counts.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp).testTag("slate.releaseHistory.counts.${entry.version}"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    counts.forEach { (tag, count) -> CategoryChip(tag, count) }
                }
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slate.releaseHistory.body.${entry.version}")
                    .semantics(mergeDescendants = true) {}
                    .padding(
                        start = ReleaseHistoryDefaults.horizontalPadding,
                        end = ReleaseHistoryDefaults.horizontalPadding,
                        bottom = ReleaseHistoryDefaults.verticalPadding,
                    ),
            ) {
                entry.groups.forEachIndexed { index, group ->
                    val top = if (index == 0) ReleaseHistoryDefaults.firstGroupSpacing else ReleaseHistoryDefaults.groupSpacing
                    if (group.tag != null) CategoryChip(group.tag, count = null, modifier = Modifier.padding(top = top))
                    group.items.forEachIndexed { itemIndex, text ->
                        val itemTop = if (group.tag == null && itemIndex == 0) top else ReleaseHistoryDefaults.itemSpacing
                        BulletItem(text, Modifier.padding(top = itemTop))
                    }
                }
            }
        }
    }
}

/** A category chip, with its item count while the release is collapsed. zh-CN: 类别胶囊, 版本收起时带条目数. */
@Composable
private fun CategoryChip(tag: String, count: Int?, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tag, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = color)
        if (count != null) {
            Spacer(Modifier.width(4.dp))
            Text(count.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

/**
 * An item with a drawn bullet dot centered on its first line; wrapped lines align with the text, not the dot.
 * Markdown links in the text show as link text in the primary color, underlined, and open in the browser (Q160 A).
 * zh-CN: 带绘制圆点的条目, 圆点垂直居中于第一行; 折行与正文对齐, 不回到圆点下方. 正文里的 Markdown 链接显示为主色带下划线的
 * 链接文字, 点按在浏览器中打开 (Q160 A).
 */
@Composable
private fun BulletItem(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyMedium
    val firstLine: Dp = with(LocalDensity.current) { if (style.lineHeight.isSpecified) style.lineHeight.toDp() else 20.dp }
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
    val inline = remember(text) { ReleaseHistoryInlineParser.parse(text) }
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val body = remember(inline, linkColor, uriHandler) {
        buildAnnotatedString {
            append(inline.text)
            val styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            // Our own listener: with the default one, a device without a browser crashes the page (IllegalArgumentException).
            // zh-CN: 自己的监听器: 用默认的那个, 没有浏览器的设备会让页面崩溃 (IllegalArgumentException).
            inline.links.forEach { link ->
                addLink(LinkAnnotation.Url(link.url, styles) { runCatching { uriHandler.openUri(link.url) } }, link.start, link.end)
            }
        }
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.width(ReleaseHistoryDefaults.bulletSlot).height(firstLine), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .padding(start = ReleaseHistoryDefaults.bulletStart)
                    .size(ReleaseHistoryDefaults.bulletSize)
                    .background(dotColor, CircleShape),
            )
        }
        Text(body, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}
