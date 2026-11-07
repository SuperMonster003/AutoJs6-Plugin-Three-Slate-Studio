package io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.release

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.slate.studio.R
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.slate.studio.ui.components.SlateScreen
import java.util.Locale

/**
 * The bundled changelog for a locale (`assets/doc/CHANGELOG-<lang>.md`, roadmap P3.3), English otherwise. Pure Kotlin.
 * zh-CN: 某区域对应的内置 changelog (`assets/doc/CHANGELOG-<lang>.md`, 路线图 P3.3), 其他区域用英文. 纯 Kotlin.
 */
internal object ReleaseHistoryAssets {
    fun assetFor(locale: Locale): String = "doc/" + when (locale.language.lowercase(Locale.ROOT)) {
        "zh" -> when {
            locale.script.equals("Hans", ignoreCase = true) -> "CHANGELOG-zh-Hans.md"
            locale.country.equals("HK", ignoreCase = true) || locale.country.equals("MO", ignoreCase = true) -> "CHANGELOG-zh-Hant-HK.md"
            locale.script.equals("Hant", ignoreCase = true) || locale.country.equals("TW", ignoreCase = true) -> "CHANGELOG-zh-Hant-TW.md"
            else -> "CHANGELOG-zh-Hans.md"
        }
        "ar", "es", "fr", "ja", "ko", "ru" -> "CHANGELOG-${locale.language.lowercase(Locale.ROOT)}.md"
        else -> "CHANGELOG-en.md"
    }
}

internal data class ReleaseItem(val tag: String?, val text: String)

internal data class ReleaseEntry(val version: String, val date: String?, val items: List<ReleaseItem>)

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

/** The bundled release history (roadmap P3.3); the update dialog's neutral button opens it. zh-CN: 内置发行历史 (路线图 P3.3). */
class ReleaseHistoryActivity : StandaloneActivity() {

    private val entries: List<ReleaseEntry> by lazy {
        runCatching {
            assets.open(ReleaseHistoryAssets.assetFor(resources.configuration.locales[0])).use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()?.let(ReleaseHistoryParser::parse).orEmpty()
    }

    @Composable
    override fun Content() = ReleaseHistoryScreen(entries, onBack = ::finish)
}

@Composable
internal fun ReleaseHistoryScreen(entries: List<ReleaseEntry>, onBack: () -> Unit) {
    SlateScreen(title = stringResource(R.string.release_history_title), onBack = onBack) { padding ->
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState(title = stringResource(R.string.release_history_unavailable), icon = R.drawable.ic_warning_24)
            }
            return@SlateScreen
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("slate.releaseHistory.list")) {
            itemsIndexed(entries, key = { _, entry -> entry.version }) { index, entry ->
                if (index > 0) Hairline(insetStart = 24.dp)
                ReleaseEntryItem(entry)
            }
        }
    }
}

@Composable
private fun ReleaseEntryItem(entry: ReleaseEntry) {
    // One focus stop per release: keys walk the history and a screen reader reads a release in one go.
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .indication(interaction, ripple())
            .focusable(interactionSource = interaction)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("slate.releaseHistory.${entry.version}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.version, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            entry.date?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        entry.items.forEach { item ->
            Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                if (item.tag != null) {
                    Text(
                        item.tag,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(item.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
