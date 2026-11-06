package io.github.supermonster003.autojs6.plugin.three.slate.studio

import org.autojs.plugin.mediastudio.api.MediaStudioContract
import org.autojs.plugin.mediastudio.api.MediaStudioOps
import org.autojs.plugin.mediastudio.api.MediaStudioPluginActions

/**
 * Identity and contract constants of the plugin (roadmap 5.5). JVM tests keep them equal to the build resources, the
 * Manifest and the README sources.
 * zh-CN: 插件的身份与契约常量 (路线图 5.5 节). JVM 测试保证其与构建资源, Manifest 及 README 源一致.
 */
object ThreeSlateStudioContract {

    const val PLUGIN_ID = "three-slate-studio"
    const val ENGINE = "media-studio"
    const val VARIANT = "default"

    const val INFO_ACTION = MediaStudioPluginActions.INFO
    const val INFO_CATEGORY = PLUGIN_ID

    const val SERVICE_ACTION = MediaStudioPluginActions.MEDIA_STUDIO
    const val SERVICE_CATEGORY = MediaStudioPluginActions.SERVICE_CATEGORY

    /**
     * The host build the plugin requires: 6641, the first local build after the baseline 6640, which roadmap P1.5
     * confirmed as the local build with the contract and the script modules (P1.1 - P1.5); P5.4 moves it to the build
     * with every host item (roadmap D23).
     * zh-CN: 插件要求的宿主构建号: 6641 (基线 6640 之后的第一个本地构建号), 路线图 P1.5 已确认它含契约与脚本模块
     * (P1.1 - P1.5); P5.4 改为含全部宿主条目的构建号 (路线图 D23).
     */
    const val REQUIRED_HOST_VERSION = 6641L

    /** Media studio contract version (roadmap D28). zh-CN: 媒体工坊契约版本 (路线图 D28). */
    const val CONTRACT_VERSION = MediaStudioContract.CONTRACT_VERSION

    /**
     * Operations the plugin implements (roadmap P2.1 - P2.5): `probe` and `audioProbe` through `probe()`, the others
     * through `submit`; every other contract operation answers `INVALID_OPTIONS: NOT_IMPLEMENTED`.
     * zh-CN: 插件已实现的操作 (路线图 P2.1 - P2.5): `probe` 与 `audioProbe` 经 `probe()`, 其余经 `submit`; 其他契约操作回答
     * `INVALID_OPTIONS: NOT_IMPLEMENTED`.
     */
    val IMPLEMENTED_OPS: List<String> = listOf(
        MediaStudioOps.PROBE,
        MediaStudioOps.TRIM,
        MediaStudioOps.CONCAT,
        MediaStudioOps.SPEED,
        MediaStudioOps.TRANSFORM,
        MediaStudioOps.EXTRACT_AUDIO,
        MediaStudioOps.REMOVE_AUDIO,
        MediaStudioOps.TRANSCODE,
        MediaStudioOps.OVERLAY,
        MediaStudioOps.FRAMES,
        MediaStudioOps.FRAME,
        MediaStudioOps.THUMBNAIL,
        MediaStudioOps.TO_GIF,
        MediaStudioOps.AUDIO_PROBE,
        MediaStudioOps.AUDIO_TRIM,
        MediaStudioOps.AUDIO_CONCAT,
        MediaStudioOps.AUDIO_MIX,
        MediaStudioOps.AUDIO_VOLUME,
        MediaStudioOps.AUDIO_CONVERT,
        MediaStudioOps.AUDIO_SPEED,
    )

    /** Detail prefix of operations not implemented yet. zh-CN: 尚未实现的操作的详情前缀. */
    const val NOT_IMPLEMENTED = "NOT_IMPLEMENTED"

    /** The plugin instruction resource of the Plugin Center. zh-CN: 插件中心使用的插件说明资源. */
    const val INSTRUCTION_REFERENCE = "@raw/plugin_instruction"

}
