# 3-Slate Studio 插件路线图

更新: 2026-10-22 (立项). 本文件是 `AutoJs6-Plugin-Three-Slate-Studio` (显示名 `3-Slate Studio`, 录屏 + 媒体加工方向, 插件选题分析 2026-10 的第 3 优先级候选) 的唯一进度与决策记录. 立项基线: 宿主 AutoJs6 `master` `63d1cb259` (2026-10-22, `VERSION_NAME` 6.8.0 未发布, `VERSION_BUILD` 4183, versionCode 5835), 宿主 Node 桥接录屏内核 `bd522f8a7` / `e8f8edb7a` (2026-10-17), 3-Macaw Speech `ROADMAP.md` 作为版式与流程模板.

使用方式:

- 每个条目完成时在条目前打勾 (`[x]`), 在条目末尾写提交 hash (插件仓库) 或 "宿主本地提交 `<hash>`" (宿主仓库只落本地提交, 推送由维护者决定). 阶段总览的状态列同步更新.
- 第 1 节是维护者经七轮选择题 (2026-10-22) 选定的固定决策, 不再重问. 第 2 节是据此派生的技术决策, 进入对应阶段前可推翻 (推翻点见各条), 推翻时在条目里写明理由并改写该行.
- 实现中遇到需要维护者拍板的事, 不阻塞的按推荐项先行, 登记到附录 D (编号, 问题, 选项, 推荐项, 是否已先行, 影响的条目, 改选返工量) 与看板 `D:/idea-projects/.claude/boards/3-slate.md`; 阻塞的停下来用选择题问维护者.
- 每完成一个子阶段就把进度写进本文件, 不靠会话上下文记住.
- 条目前缀: (插件) 本仓库; (宿主) AutoJs6 仓库; (资产) AutoJs6-Shared-Assets; (索引) AutoJs6-Official-Plugins-Index; (文档) AutoJs6-Documentation / TypeScript-Declarations / Ace / Offline-Docs; (测试) 设备或 AVD 验证; (发布) 发布门禁.

---

## 1. 固定决策

维护者 2026-10-22 经七轮选择题选定, 不再重问.

| 编号 | 事项 | 决策 |
| --- | --- | --- |
| D1 | 名称 | `3-Slate Studio`: Word `Slate` (场记板), 后缀 `Studio`. 仓库 `AutoJs6-Plugin-Three-Slate-Studio`, 机器标识 `three-slate-studio`. |
| D2 | 录屏归属 | 两条都做: 1.0.0 由宿主把 Node 桥接已有的录屏内核泛化并暴露给 Rhino 脚本 (`video.record`, 复用 `images.requestScreenCapture` 的授权, 录屏期间截图照常); 1.2.0 插件再做自持 MediaProjection 的增强录屏 (悬浮控制条, 无宿主截图会话时可用). |
| D3 | 1.0.0 加工范围 | 剪辑全集 + 音频加工: 按时间裁剪, 拼接, 变速, 缩放 / 旋转 / 裁切, 文字 / 图片水印, 抽帧, 转 GIF, 抽音频, 去音轨, 转码; 纯音频的裁剪 / 拼接 / 混音 / 音量淡入淡出 / 格式转换. |
| D4 | 加工引擎 | Media3 Transformer 1.10.1 + 平台 MediaCodec 为主, 1.0.0 APK 无原生库; GIF 用自带纯 Kotlin 编码器, 抽帧用 MediaMetadataRetriever / MediaCodec. 1.1.0 加自建 LGPL FFmpeg 作为按需下载的大库 (Shared-Assets 新类别, 按 ABI 下载). |
| D5 | FFmpeg 命令透传 | 1.1.0 随大库提供 `video.ffmpeg(args)`: 只允许读写脚本可访问的文件, 禁用网络协议, 带超时与进度; 未装大库时报 `LIBRARY_MISSING` 并引导安装. |
| D6 | 脚本 API 形态 | 新全局模块 `video` 与 `audio` (`$video` / `$audio` 同对象). `video.record` / `audio.record` 由宿主实现, 其余由插件经契约实现; 不扩展宿主 `media` 模块. |
| D7 | 独立界面 | 工具箱式界面 + 多轨完整时间线剪辑器 (见 D10). |
| D8 | 任务模型 | 与 `speech` 模块同一约定: 同步方法阻塞 (不可在 UI 线程调用), `xxxAsync` 返回 Promise; `options.onProgress(progress)` 回调; 停止脚本即取消; `video.tasks()` / `audio.tasks()` 列出并可取消本脚本的进行中任务, 与 Studio 任务队列同一套. |
| D9 | 文件访问 | 插件申请 `MANAGE_EXTERNAL_STORAGE` (与 3-Maple Image 一致), 按脚本传入的路径直接读写; 未授权时报 `STORAGE_PERMISSION_DENIED` 并由插件界面引导授权. |
| D10 | 时间线剪辑器 | 1.0.0 做多轨完整剪辑器: 视频 / 音频 / 叠加多轨, 转场, 字幕; 导出走与脚本相同的 `video.compose` 管线. |
| D11 | 代码生成 | 界面中的每个向导与时间线导出都能生成等价的 `video.*` / `audio.*` 代码, 可复制, 并可一键在宿主新建脚本 (D19). |
| D12 | 转场与字幕 | 转场 6 种以上 (见 D30), 字幕轨内置编辑, SRT 导入 / 导出, ASS 导入 (1.0.0 基础子集, 1.1.0 libass 完整渲染, 见 D22 / D24), 导出时烧录进画面. |
| D13 | 后续版本 | 1.1.0 = FFmpeg 大库 + 命令透传 + MP3 / WebM / MKV 输出 + libass; 1.2.0 = 插件自持投影的增强录屏 + 悬浮控制条 + Shizuku `screenrecord` 免确认录屏 (已装 Shizuku 且授权时走 shell, 否则回落系统确认). |
| D14 | 默认输出 | 输出路径参数可省略: 视频默认 `/sdcard/Movies/3-Slate Studio/<源名>-<操作>-<时间戳>.<ext>`, 音频默认 `/sdcard/Music/3-Slate Studio/`, 宿主录屏默认 `/sdcard/Movies/AutoJs6/`, 录音默认 `/sdcard/Music/AutoJs6/`; 完成后登记 MediaStore 使相册可见; 返回值带实际路径与媒体信息. Studio 界面默认同一目录, 设置可改. |
| D15 | 1.0.0 音频输出格式 | M4A (AAC), WAV (PCM16), FLAC (平台编码器, API 24+), OGG / Opus (API 29+, `MediaMuxer` OGG 封装); MP3 / 其他容器随 1.1.0 FFmpeg. 输入按系统解码器 (MP3 / AAC / FLAC / OGG / WAV / AMR 等). |
| D16 | 图标 | 候选 B "张开的场记板 + 两条横线": 源图 `.python/icons/three-slate-ic-launcher-light.png` / `-dark.png` (1024 px 透明 RGBA, alpha 相同, 不透明像素 `#272727` / `#D8D8D8`, 本次立项会话生成并入库), 正式资源由 Icon Studio 在 P0.2 生成. |
| D17 | 调度分配方式 | 全部卡片制: 调度会话每批 (含首批) 发任务卡片, 由维护者点击启动. |
| D18 | 插件联动 | 松耦合, 不依赖其他插件: 探测用自带 `MediaMetadataRetriever` / `MediaExtractor` (`video.probe`), 预览用自带 ExoPlayer; 抽帧返回路径与 `Image` 对象以便交给 `images.*` / 3-Maple Image; 已装 MediaInfo 时 Studio 详情页多一个 "用 MediaInfo 查看" 入口 (Intent, 不绑定). |
| D19 | 新建脚本入口 | 宿主新增受 `org.autojs.permission.PLUGIN` 保护的 Activity 入口 (action `org.autojs.autojs.action.CREATE_SCRIPT`, extras: 代码, 建议文件名, 来源插件), 打开编辑器新建未保存文件; 通用, 其他插件以后也能用. |
| D20 | 契约命名 | `plugin-api/media-studio-api`, 包 `org.autojs.plugin.mediastudio.api`, action `org.autojs.plugin.MEDIA_STUDIO`, engine / category `media-studio`, 宿主桥接包 `org.autojs.autojs.core.plugin.mediastudio`; 一个契约同时承载 `video` 与 `audio` 两个模块. |
| D21 | 麦克风录音 | 纳入: 宿主泛化 Node 的纯音频录制内核, 暴露 `audio.record` / stop / status (麦克风; API 29+ 可选内录 / 混合), 与 `video.record` 共用选项与文档版式. |
| D22 | ASS 字幕 | 目标是 libass 完整渲染; 分发方式见 D24. |
| D23 | 1.0.0 发布门 | 不等宿主 6.8.0 公开: 所有条目通过即发 1.0.0 并登记官方索引, `REQUIRES_HOST_VERSION` 写宿主本地构建号 (Macaw 先例), 插件中心对旧宿主显示不兼容. |
| D24 | libass 分发 | 1.1.0 随 FFmpeg 大库 (FFmpeg 编入 libass, 含 FreeType / HarfBuzz / fribidi, 单 ABI 约 +3 MB); 1.0.0 的 ASS 按基础子集渲染 (D31), 1.0.0 APK 无原生库. |

---

## 2. 派生技术决策

D25-D40 据第 1 节派生, 进入对应阶段前可推翻 (推翻点见各条), 之后视同固定.

| 编号 | 决策 | 含义 | 推翻点 |
| --- | --- | --- | --- |
| D25 | 工具链基线 | 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 的占位符表与 3-Maple Image 的当前依赖线: compileSdk / targetSdk 37, minSdk 24, Kotlin 2.x, Compose BOM 2026.09.00, navigation 2.10.2, media3 1.10.1 (`media3-transformer`, `media3-effect`, `media3-muxer`, `media3-exoplayer`, `media3-ui`), `PLATFORM_VERSIONS_PLUGIN_VERSION` 1.9.1; 版本号起 1.0.0, `VERSION_BUILD` 在骨架阶段为提交数 + 1. 1.0.0 APK 为 universal 单包 (无原生库, 不拆 ABI); 16 KB 页校验在 1.0.0 只需确认依赖 AAR 内无 `.so`. | P0.1 |
| D26 | 宿主录屏 / 录音内核泛化 | 把 `engine/NodeScreenRecording.kt`, `ScreenRecordingAudio.kt`, `ScreenRecordingFrames.kt`, `ScreenRecordingMuxer` 与 Node 的纯音频录制从 `engine/` 抽到 `core/record/` (类名 `ScreenRecorder`, `AudioRecorder`), Node 桥接与 Rhino `video.record` / `audio.record` 共用; 行为与 Node 版一致 (H.264 MP4, 宽高须为偶数且同时给出, `frameRate` 1-120 默认 30, `bitRate` 默认 8 Mbps, `audio` none / mic / playback / mixed, AAC 48 kHz 128 kbps, playback / mixed 需 API 29+), Rhino 侧增加 `maxDuration` 与 `onStop`. 同一宿主进程同时只有一个录屏; 录屏期间 `images.captureScreen` 由帧分发回读 (宿主已实现). 内录声道数是否改立体声见附录 D Q1. | P1.2 |
| D27 | 宿主绑定与能力声明 | `MediaStudioPluginHost` 基于 `core/plugin/AidlPluginHost.kt`, 绑定 flags 同 `SpeechPluginHost` (含 API 34 `BIND_ALLOW_ACTIVITY_STARTS`, 以便插件在宿主后台时弹出 "所有文件访问" 授权页与 MediaStore 写入确认); 脚本能力声明: `video.record` 需 `screen_capture`, 带音频的录屏与 `audio.record` 需 `media.recording` + `RECORD_AUDIO`, 加工类方法需 `media`; 裸脚本能力声明与 MCP / Node 的声明保持一致. | P1.3 |
| D28 | 契约请求模型 | `IMediaStudioPlugin.submit(request, callback)` 返回任务 ID, 请求与结果用 `Bundle` (不走 JSON 字符串; 选项像 `JSON.stringify` 一样序列化, 函数被忽略), `IMediaStudioCallback.onProgress / onComplete / onError`; `cancel(taskId)`, `probe(path)`, `listTasks(hostToken)`; 宿主回调 Binder 死亡即取消其全部任务并删除未完成的输出文件; 同一宿主 Binder token 最多 4 个并发任务 (超出 `TASK_LIMIT`), 全插件最多 2 个同时编码 (其余排队). 草案见附录 C. | P1.1 |
| D29 | 加工管线 | 单输入单输出的操作 (裁剪, 变速, 变换, 叠加, 转码, 去音轨, 抽音频) 走 `Transformer` + `EditedMediaItem` + `Effects`; 拼接与多轨走 `Composition` (多 `EditedMediaItemSequence`); 裁剪无需重编码时启用 `experimentalSetTrimOptimizationEnabled`; 默认视频编码 H.264 (`h265` 可选, 设备不支持时报 `UNSUPPORTED_FORMAT`), 默认码率按分辨率估算 (1080p 8 Mbps); 音频处理用 `media3-effect` 的 `AudioProcessor` (音量 / 变速 / 声道) 与 `Composition` 多序列混音; 超过 2 GB 或 60 分钟的单输入返回 `warnings[]` 但不拒绝. | P2.1 |
| D30 | 转场 | 7 种: `cut`, `fade` (经黑场), `dissolve` (叠化), `slide` (推移, 方向参数), `wipe` (擦除, 方向参数), `zoom` (缩放), `blur` (模糊叠化). 除 `cut` / `fade` 外均为重叠区间双序列合成 (`VideoCompositorSettings` + 自写 GLSL 效果, 两段各延长重叠时长); 转场时长 100-3000 ms 默认 500 ms; 时间线 JSON 与界面共用同一转场表. | P3.2 |
| D31 | 字幕 | SRT 完整解析 / 导出 (UTF-8, 含 BOM 与 CRLF 容错); ASS 1.0.0 基础子集: `[V4+ Styles]` 的字体 / 字号 / 主色 / 描边色 / 粗斜 / 描边 / 阴影 / 对齐 / 边距, Dialogue 的时间 / 图层 / 换行, 内联标签只认 b, i, c, fs, an, pos (ASS 写法为反斜杠加标签名), 其余标签 (卡拉 OK, 动画, 绘图) 忽略并返回 `warnings[]`; 烧录用 `media3-effect` `TextOverlay` 逐帧渲染 (Android `StaticLayout`), 字体用系统字体或脚本指定的 TTF 路径. 1.1.0 大库装好后 ASS 自动改走 libass. | P3.3 |
| D32 | GIF | 自带纯 Kotlin 编码器: 全局或逐帧调色板 (中位切分, 最多 256 色), 可选 Floyd-Steinberg 抖动, LZW 编码, 循环与帧延时; 默认 10 fps, 宽度 480, 单次上限 600 帧 (超出报 `LIMIT_EXCEEDED`, 建议先裁剪或降帧率). 不引入第三方 GIF 库 (附录 D Q2 可改). | P2.4 |
| D33 | 时间线 JSON | `video.compose(timeline)` 的 `timeline` 为对象: `width` / `height` / `frameRate` / `background`, `video[]` (轨道数组, 每轨 `clips[]`: `src`, `start`, `end`, `at`, `speed`, `volume`, `transform`, `transition`), `audio[]` (轨道数组, `clips[]`: `src`, `start`, `end`, `at`, `volume`, `fadeIn`, `fadeOut`), `overlays[]` (文字 / 图片, 含时间段与位置), `subtitles` (`src` 或 `cues[]` 与 `style`); 轨道顺序即 z 顺序; 界面工程文件 `.slate.json` 就是同一 JSON 加界面状态 (缩放, 选中). 草案见附录 B.4. | P3.1 |
| D34 | 预览引擎 | 时间线预览优先用 media3 `CompositionPlayer` (实验 API) 直接播放 `Composition`; P4.3 开工先 spike: 在 API 28 / 33 / 35 真机上转场与叠加能实时预览则采用, 否则退到自写预览 (ExoPlayer 逐片段播放 + Compose 叠层绘制转场近似), 结论回填附录 D Q3. | P4.3 |
| D35 | 工具箱向导 | 每个脚本 API 一个表单页 (选文件, 填参数, 预览结果, 执行入队), 表单字段即 API 选项, 执行后结果页显示输出路径, 媒体信息, 生成的代码 (D11); 向导与脚本共用插件内同一执行器, 任务队列页显示两者的任务. | P4.2 |
| D36 | 代码生成与宿主入口 | 代码生成器以 API 选项对象为输入输出 JS 源码 (字符串转义, 路径用原值), 与 d.ts 同源维护; "在 AutoJs6 中打开" 调用 D19 的 `CREATE_SCRIPT`, 宿主未装或版本过低时只提供复制. | P4.4 |
| D37 | Studio 首页与存储 | 首页列出 `Movies/3-Slate Studio`, `Movies/AutoJs6`, `Music/3-Slate Studio`, `Music/AutoJs6` 与用户添加的目录 (MediaStore 查询, 不扫全盘); 文件操作 (重命名, 删除, 分享) 限于这些目录; 设置项: 默认输出目录, 默认编码 (H.264 / H.265), 默认码率策略, 硬件编码器偏好, 任务并发数, 启动器图标模式 (黑白图标规范第 1 节的四个选项). | P4.1 |
| D38 | 插件中心与向导目录 | `ThreeSlateStudioInfoService` (action `org.autojs.plugin.INFO`, category `three-slate-studio`), 能力服务受 `org.autojs.permission.PLUGIN` 保护; 向导目录 `PluginInstallWizardCatalog` 放 `TOOLS` 分类 `official("three.slate.studio")`, 回退标题 `Three Slate Studio` (附录 D Q4 可改为新分类). | P1.5 |
| D39 | 许可证与声明 | 源码 MPL-2.0 (`LICENSE` 完整文本); 1.0.0 依赖均为 Apache-2.0 (media3, Compose, Kotlin), `THIRD_PARTY_NOTICES.md` 由脚本生成; 1.1.0 FFmpeg 大库按 LGPL-2.1-or-later 独立分发 (不打进 APK, 动态加载), 大库自身的 `NOTICES` 随大库包分发, 专利风险说明写进 README "FFmpeg 大库" 节. | P0.1 |
| D40 | 文档与声明同步 | 文档站新增 `video.md` 与 `audio.md` (10 种语言, 半角标点), 宿主录屏 / 录音也写在这两页 (不写进 `images.md`); d.ts 新增 `video.d.ts` / `audio.d.ts`; Ace 补全与 Offline Docs 同步; 四仓库均只落本地提交, 推送由维护者决定. | P5.1 |

---

## 3. 范围与非目标

范围 (1.0.0):

- 脚本 API `video` / `audio` 全集 (附录 B): 宿主实现的录屏与录音, 插件实现的全部加工, 同步 + Async 两形态, 进度与取消, 任务列表.
- 宿主契约 `media-studio-api`, 绑定与能力声明, 插件中心注册, 向导条目, `CREATE_SCRIPT` 入口, 官方索引登记.
- 插件加工核心: 裁剪, 拼接, 变速, 变换, 叠加, 抽帧, GIF, 抽音频, 去音轨, 转码, 音频加工, 探测.
- 多轨合成: 时间线 JSON, 7 种转场, SRT / ASS (基础子集) 字幕烧录.
- 独立界面: 首页与任务队列, 工具箱向导, 多轨时间线剪辑器 (预览, 转场, 字幕, 叠加), 代码生成与宿主入口, 设置, 发行历史, 更新检查, Icon Studio 图标与四种启动器模式.
- 文档, d.ts, Ace, Offline Docs, README, changelog, 宿主样例脚本, 兼容矩阵, 1.0.0 发布.

非目标 (不做或排到 1.1.0 之后, 评估记录见附录 G):

- 1.0.0 不含任何原生库: FFmpeg, libass, MP3 编码, WebM / MKV 输出均为 1.1.0 (D4, D13, D24).
- 插件自持投影的录屏, 悬浮控制条, Shizuku 免确认均为 1.2.0 (D2, D13).
- 在线服务 (云转码, 云字幕), AI 字幕生成 (可由 3-Macaw Speech 的识别结果经脚本生成 SRT, 本插件不内置).
- 相机拍摄, 直播推流, 画中画播放器, 多设备同步.
- 完整 ASS 特效 (卡拉 OK, 动画, 绘图) 在 1.0.0 忽略 (D31).

---

## 4. 现状诊断

### 4.1 可直接复用的宿主能力

- 屏幕投影授权与截图管线: `core/image/capture/ScreenCapturer.java` (MediaProjection + ImageReader, `setRecordingSurface` / `restoreCaptureSurface` / `replaceRecordingSurface` / `getAudioProjection`), `ScreenCaptureRequester.java` (API 34 `MediaProjectionConfig`, 先起 `ScreenCapturerForegroundService` 再取投影), `permission/MediaProjectionPermission.kt`; Manifest 已声明 `FOREGROUND_SERVICE_MEDIA_PROJECTION`.
- Node 桥接录屏内核 (仅 Node / Bun 可用, Rhino 未暴露): `engine/NodeScreenRecording.kt` (MediaCodec `COLOR_FormatSurface` H.264 + 流式 `ScreenRecordingMuxer` 写 MP4, start / status / capture / finish), `engine/ScreenRecordingAudio.kt` (`AudioPlaybackCaptureConfiguration` 内录 / 麦克风 / 混合, AAC), `engine/ScreenRecordingFrames.kt` (投影输入经 GL 分发给编码器并按需回读截图), `engine/NodeScreenCaptureSession.kt` (startRecording / stopRecording / cancelRecording / recordingStatus); 协议文档 `docs/dev/runtime-node-recording-2026-10-17.md`. Node 的纯音频录制 start / getStatus / stop 同样存在.
- 插件基础设施: `core/plugin/AidlPluginHost.kt` (绑定, `allowActivityStarts`), `plugin-api/common-plugin-api`, `plugin-api/speech-api` (Bundle 请求, 回调 Binder, `ParcelFileDescriptor` 用法, `getInfo` 居首) 作契约模板, 插件中心与 `PluginInstallWizardCatalog`, 官方索引工作流.
- Shizuku 依赖已在宿主 (`dev.rikka.shizuku:api` 13.1.5), 1.2.0 的免确认录屏可参考宿主现有用法.
- 相关插件: 3-Maple Image (Compose 工具箱界面, `MANAGE_EXTERNAL_STORAGE`, Icon Studio 接入, 发布流程), 3-Terra / 3-Amber Player (media3 1.10.1 ExoPlayer 用法), MediaInfo 2.1.4 (契约与注册模板), 3-Block Compose (设计器导出代码的交互).

### 4.2 缺口 (需要新建或修改)

- 宿主: `core/record/` 录屏 / 录音内核 (从 `engine/` 泛化), Rhino `video` / `audio` 模块 (录制部分自实现, 加工部分转调插件), `plugin-api/media-studio-api` 契约, `MediaStudioPluginHost` 绑定, 能力声明, `CREATE_SCRIPT` Activity 入口, 插件中心 / 向导 / 协议文档 / changelog.
- 插件: 全部 (仓库刚建立, 只有 ROADMAP 与图标源图).
- 文档四仓库: `video.md`, `audio.md`, d.ts, Ace, Offline Docs.
- 资产 (1.1.0): Shared-Assets `media-libs` 类别 (FFmpeg + libass 按 ABI 包, 签名目录, 发布工作流).

### 4.3 外部事实

- FFmpegKit 2025-01-06 退役, Maven Central 二进制 2025-04-01 删除, `arthenica/ffmpeg-kit` 2026-07-02 归档只读; 社区分支 (`dev.ffmpegkit-maintained`, `TheByteArray`, `howseen/ffmpeg-kit-min-16KB`) 存在但维护状态与许可审查不一. 自建 FFmpeg (LGPL 配置, `--enable-mediacodec --enable-jni`, `h264_mediacodec` / `hevc_mediacodec` 硬编, 不启用 x264 / GPL 组件) 可行, 需 NDK r26c 补丁与 16 KB 对齐. 1.1.0 的 P6.1 决定自建还是采用社区分支 (附录 D Q5).
- Media3 Transformer 1.10.1 (Apache-2.0): 裁剪 (含免重编码优化), 缩放 / 旋转 / 裁切, `OverlayEffect` 文字 / 图片叠加, `setSpeed` + `setFrameRate`, `EditedMediaItemSequence` / `Composition` 拼接与多序列合成 (`VideoCompositorSettings`), transmux, `media3-muxer` 只出 MP4; 无内置转场; `CompositionPlayer` 为实验 API. 平台 MediaCodec 可编 AAC / FLAC (API 24+) / Opus (API 29+), 不可编 MP3; `MediaMuxer` 支持 MP4, WebM (仅 VP8 / VP9 / Vorbis / Opus), OGG (API 29+), 3GP.
- Android 14+: 每次录屏需用户同意, 一个 MediaProjection 只能建一个 VirtualDisplay, 授权 Intent 绑定请求方 UID 不能跨应用; 这是 D2 两条路线各自保留授权的根本原因.
- Garage 第三方 `org.autojs.plugin.ffmpeg` (hyb1996, 1.1, 2022-09-04, 仅 arm64-v8a, `plugins.load` 后 `ffmpeg.inProcess.exec`) 是 FFmpeg 透传的用户需求先例, 1.1.0 的 `video.ffmpeg` 在文档里给出迁移说明.

---

## 5. 目标架构

### 5.1 数据流: 录屏与录音 (宿主)

```text
脚本 video.record(path, options)
  -> 宿主 VideoModule.record: 检查 screen_capture 能力, 无授权时走 ScreenCaptureRequester 请求
  -> core/record/ScreenRecorder (原 NodeScreenRecording): ScreenCapturer.setRecordingSurface -> MediaCodec AVC
     + AudioRecorder (原 ScreenRecordingAudio, mic / playback / mixed -> AAC) -> ScreenRecordingMuxer 流式写 MP4
  -> 返回 ScreenRecording 对象: stop() 收尾并返回结果, cancel() 丢弃, status(), capture() 取当前帧
脚本 audio.record(path, options) -> AudioRecorder 单独运行 -> M4A / WAV
Node 桥接 screen_recorder / recorder 改为调用同一 core/record 内核 (行为不变)
```

### 5.2 数据流: 加工 (插件)

```text
脚本 video.trim(src, opts, out) / audio.mix(...) / video.compose(timeline, out)
  -> 宿主 VideoModule / AudioModule: 校验选项 (未知键 INVALID_OPTIONS, 上限 LIMIT_EXCEEDED), 填默认输出路径 (D14)
  -> MediaStudioPluginHost (AidlPluginHost 子类) 绑定 ThreeSlateStudioService
  -> IMediaStudioPlugin.submit(Bundle request, IMediaStudioCallback) -> taskId
  -> 插件 TaskRunner (并发 2 编码 / 每宿主 token 4 任务): 按 op 选管线
       Transformer 单输入 | Composition 多序列 | MediaMetadataRetriever 抽帧 | GifEncoder | AudioPipeline
     进度经 onProgress(taskId, {percent, stage, etaMillis}) 回宿主 -> options.onProgress
  -> 完成: 登记 MediaStore, onComplete(taskId, result Bundle) -> 脚本返回 {path, durationMillis, width, height, sizeBytes, mime, warnings[]}
  -> 失败 / 取消: 删除未完成输出, onError(taskId, {code, message})
Studio 界面的向导与时间线调用同一 TaskRunner, 任务队列页显示脚本与界面两类任务
```

### 5.3 数据流: 代码生成与宿主入口

```text
Studio 向导 / 时间线 -> 选项对象 -> CodeGenerator -> JS 源码 -> 复制到剪贴板
                                                   -> Intent org.autojs.autojs.action.CREATE_SCRIPT (extras code, suggestedName, source)
                                                   -> 宿主 CreateScriptActivity (PLUGIN 权限) -> 编辑器新建未保存文件
```

### 5.4 包结构 (插件)

```text
io.github.supermonster003.autojs6.plugin.three.slate.studio
  ThreeSlateStudioApplication, ThreeSlateStudioService (IMediaStudioPlugin), ThreeSlateStudioInfoService, ThreeSlateStudioContract
  core/        TaskRunner, TaskRegistry, OutputPaths (D14), MediaStoreRegistrar, Probe
  pipeline/    TransformerPipeline, CompositionBuilder, Transitions (GLSL), Overlays, Subtitles (Srt, AssSubset, Burner), Frames, GifEncoder, AudioPipeline
  codegen/     CodeGenerator, OptionSchemas (与 d.ts 同源)
  ui/          home, tasks, toolbox/<op>, timeline (editor, preview, tracks), settings, releases, update, launcher icon
  bigrun/      (1.1.0) LibraryManager, FfmpegRunner, AssRenderer
  recording/   (1.2.0) ProjectionRecorder, FloatingControls, ShizukuRecorder
```

### 5.5 身份派生表

| 项 | 值 |
| --- | --- |
| `{PROJECT_NAME}` | `AutoJs6-Plugin-Three-Slate-Studio` |
| `{ROOT_PROJECT_NAME}` | `autojs6-plugin-three-slate-studio` |
| `{APP_NAME}` (不可翻译) | `3-Slate Studio` (阿拉伯语按 Three 系列双向隔离规则用 FSI / PDI 包裹) |
| `{APPLICATION_ID}` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.slate.studio` |
| `{PLUGIN_ID}` / `{PLUGIN_ENGINE}` / `{PLUGIN_VARIANT}` | `three-slate-studio` / `media-studio` / `default` |
| 身份类 | `ThreeSlateStudioContract`, `ThreeSlateStudioService`, `ThreeSlateStudioInfoService`, `ThreeSlateStudioApplication`, `Theme.ThreeSlateStudio`, 测试类同名后缀 |
| 插件中心身份服务 | `ThreeSlateStudioInfoService`, action `org.autojs.plugin.INFO`, category `three-slate-studio` |
| 能力服务 | `ThreeSlateStudioService`, action `org.autojs.plugin.MEDIA_STUDIO`, category `media-studio`, 受 `org.autojs.permission.PLUGIN` 保护 |
| `{CAPABILITY_API}` | `media-studio-api` (宿主 `plugin-api/media-studio-api`, AAR 复制到 `libs/media-studio-api.aar`; 另需 `libs/common-plugin-api.aar`; 登记在 `locks/host-api-aars.lock`) |
| 宿主契约包 | `org.autojs.plugin.mediastudio.api` |
| 宿主桥接包 | `org.autojs.autojs.core.plugin.mediastudio` |
| 脚本 API | `video` 与 `audio` (`$video` / `$audio` 同对象) |
| `{REQUIRES_HOST_VERSION}` | 基线 5835 (宿主 `63d1cb259`), P1.5 写临时值, P5.4 回填为含全部宿主条目的本地构建号 (D23) |
| 向导目录 | `PluginInstallWizardCatalog` `TOOLS` 分类, `official("three.slate.studio")`, 回退标题 `Three Slate Studio` (附录 D Q4) |
| `plugin_description` (zh-Hans) | `视频与音频加工, 多轨时间线剪辑, 转场与字幕, 界面操作可生成脚本代码` (句尾无点号, 不含 "AutoJs6"; P0.1 定稿, 10 语言见 `values*/strings.xml`) |
| GitHub 仓库描述 | `Video and audio processing, multi-track timeline editing and screen recording companion for AutoJs6 scripts` |
| 发布文件名 | `autojs6-plugin-three-slate-studio-v{VERSION_NAME}-{CRC32}.apk` (1.0.0 universal 单包, D25) |
| 索引清单 | `release-manifests/io.github.supermonster003.autojs6.plugin.three.slate.studio/<versionCode>.json` |
| 图标 | `.python/icons/three-slate-ic-launcher-light.png` / `-dark.png` (D16), Icon Studio 合成 (P0.2) |
| 权限 | `org.autojs.permission.PLUGIN`, `MANAGE_EXTERNAL_STORAGE` (+ API 32 及以下的 `READ/WRITE_EXTERNAL_STORAGE`), `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (API 33+, 首页列表), `POST_NOTIFICATIONS` (任务进度通知), `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` (长任务前台服务), `INTERNET` + `ACCESS_NETWORK_STATE` (更新检查, 1.1.0 大库下载); 1.2.0 再加投影与悬浮窗相关权限 |
| 许可证 | 源码 MPL-2.0; 依赖 Apache-2.0; 1.1.0 FFmpeg 大库 LGPL-2.1-or-later 独立分发 (D39) |

---

## 6. 阶段总览

| 阶段 | 版本 | 主题 | 状态 |
| --- | --- | --- | --- |
| P0 | 1.0.0 | 仓库骨架: 构建链与身份, Icon Studio 图标, 测试与 CI | 未开始 |
| P1 | 宿主 6.8.0 | 契约 `media-studio-api`, 录屏 / 录音内核泛化, 脚本模块 `video` / `audio`, `CREATE_SCRIPT` 入口, 注册与协议文档 | 未开始 |
| P2 | 1.0.0 | 插件加工核心: 执行器与任务模型, 视频基础加工, 叠加, 抽帧与 GIF, 音频加工, 设备矩阵 | 未开始 |
| P3 | 1.0.0 | 多轨合成: 时间线 JSON 与 `video.compose`, 转场, 字幕 | 未开始 |
| P4 | 1.0.0 | Studio 界面: 应用骨架, 工具箱向导, 时间线剪辑器, 代码生成与宿主入口 | 未开始 |
| P5 | 1.0.0 | 文档, 声明, 四仓库同步, 样例, 兼容矩阵, 发布与索引登记 | 未开始 |
| P6 | 1.1.0 | FFmpeg + libass 大库: 自建或选型, Shared-Assets 类别, 下载 / 校验 / 加载, 命令透传, 新格式, 完整 ASS, 发布 | 未开始 |
| P7 | 1.2.0 | 录屏增强: 插件自持投影, 悬浮控制条与 Studio 录屏入口, Shizuku 免确认, 发布 | 未开始 |

依赖顺序:

```text
P0 (骨架)                                  P1 (宿主) 与 P0 并行: 不同仓库, P1.1 契约 AAR 产出后 P0.1 把它放进 libs/
 -> P2 (加工核心)                          依赖 P0.1, P0.3 与 P1.1 的契约常量; 真机脚本测试依赖 P1.3
     -> P3 (多轨合成)                      依赖 P2.1-P2.3 (管线, 叠加) 与 P2.5 (混音)
         -> P4 (界面)                      P4.1 / P4.2 依赖 P2; P4.3 依赖 P3; P4.4 依赖 P1.4 与 P4.2 / P4.3
             -> P5 (文档, 发布)            依赖 P1-P4 全部通过
                 -> P6 (1.1.0) -> P7 (1.2.0)   P6 与 P7 互不依赖, 维护者按 D13 顺序安排
```

可并行的批次建议 (调度会话按 D17 发卡片):

- 批次 1: P0.1 + P0.3 (插件骨架与 CI, 一个子会话); P1.1 + P1.2 (宿主契约与录制内核泛化, 一个子会话); P0.2 (图标, 可与 P0.1 合并).
- 批次 2: P1.3 + P1.4 + P1.5 (宿主模块与入口); P2.1 + P2.2 (插件执行器与视频基础加工).
- 批次 3: P2.3 + P2.4 (叠加, 抽帧与 GIF); P2.5 (音频加工); 两者可并行.
- 批次 4: P3.1 + P3.2 (时间线与转场); P3.3 (字幕) 可并行; P2.6 设备矩阵在 P2 全部完成后.
- 批次 5: P4.1 + P4.2 (应用骨架与向导); 之后 P4.3 (时间线剪辑器) 与 P4.4 (代码生成) 并行.
- 批次 6: P5.1 + P5.2 (文档与声明) 与 P5.3 (README, 样例, 兼容矩阵) 并行; P5.4 发布.

发布门禁 (每个阶段的 "(发布)" 条目都指向本清单):

1. 插件 `./gradlew test connectedDebugAndroidTest` 通过 (API 35 AVD), 宿主涉及模块的单元测试通过.
2. 真机脚本回归: 附录 F 设备矩阵中的 Sony API 28 / 31 / 33, Xiaomi Pad API 35 四台通过, OnePlus API 35 (ColorOS) 至少跑一次; Samsung 16 KB 设备在引入原生库的版本 (1.1.0 起) 必跑.
3. 10 种语言资源与文档只用半角标点 (`COMMON_AGENTS.md` 规则), `prepush_check.py` 通过, `THIRD_PARTY_NOTICES.md` 与依赖一致.
4. 发布 APK 为 release 签名, 用宿主插件中心安装并跑 `samples/` 冒烟脚本; versionCode, 文件名 CRC32 与索引清单一致.
5. changelog 10 种语言同步 (zh-Hans 由子会话写, 其他语言由维护者同步或子会话按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 规则翻译后由维护者复核).

---

## P0: 仓库骨架

状态: 未开始. 设备: 无 (构建与单元测试); P0.3 的仪器测试用 API 35 AVD.

### P0.1 构建链与身份 (D25, D39)

- [ ] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 建立 Gradle 工程 (`settings.gradle.kts`, `build-logic`, `gradle/libs.versions.toml`, `version.properties`, `app/`), 填入 5.5 节身份; `libs/common-plugin-api.aar` 与 `libs/media-studio-api.aar` (P1.1 产出, 未产出前用 P1.1 的常量草案本地构建 AAR 顶替并在 `locks/host-api-aars.lock` 标注临时) 入库.
- [ ] (插件) 依赖线: Compose BOM 2026.09.00, navigation 2.10.2, media3 1.10.1 全套 (`transformer`, `effect`, `muxer`, `exoplayer`, `ui`, `common`), 无原生库; `./gradlew assembleDebug` 产出 universal APK; 检查依赖 AAR 内无 `.so` (有则记录并评估 16 KB).
- [ ] (插件) `ThreeSlateStudioApplication`, `ThreeSlateStudioInfoService`, `ThreeSlateStudioService` 空实现 (返回 `getInfo` 与 `NOT_IMPLEMENTED`), `ThreeSlateStudioContract` (ID, engine, 契约版本), Manifest (权限, 服务, `MANAGE_EXTERNAL_STORAGE` 授权页), 10 种语言 `strings.xml` (`app_name` 不可翻译, `plugin_description` 定稿).
- [ ] (插件) `LICENSE` (MPL-2.0), `README.md` 骨架 (含 "FFmpeg 大库" 节的占位与专利说明), `THIRD_PARTY_NOTICES.md` 生成脚本, `.gitattributes` (已入库), `.editorconfig`, `prepush_check.py` 接入.
- 验收: `./gradlew assembleDebug test` 通过; APK 安装到 API 35 AVD 后插件中心能识别 `three-slate-studio` (身份服务) 并显示未启用; 提交前 `VERSION_BUILD` = 提交数 + 1.

### P0.2 图标与启动器模式 (D16)

- [ ] (插件) 用 Icon Studio (`D:/webstorm-projects/AutoJs6-Icon-Studio`, 内核 1.4.0) 从 `.python/icons/three-slate-ic-launcher-light.png` / `-dark.png` 生成全套资源 (`ic_plugin_center`, `ic_launcher`, `ic_launcher_system*`, monochrome, 四个 launcher alias, `.icons/recipe.json`, `.python/icon_studio_runtime.py` / `generate_icon_studio.py`), 视觉尺寸 V = 0.52, 安全圆断言通过.
- [ ] (插件) 设置页的启动器图标四选项 (自适应自动 / 固定亮 / 固定暗 / 透明) 与规范提示文案 (可先在 P4.1 的设置页落地, 本条只要求资源与组件 alias 就绪).
- 验收: 工程测试中的图标尺寸 / 透明度 / 安全圆断言通过; API 35 AVD 与一台真机的启动器与插件中心亮暗主题下图标可辨.

### P0.3 测试与 CI

- [ ] (插件) 单元测试骨架 (选项校验, 输出路径, 代码生成器), 仪器测试骨架 (服务绑定, `getInfo`), 测试夹具生成脚本 `.python/generate_media_fixtures.py` (用 MediaCodec 在测试运行时生成 3 秒彩条 MP4 与正弦波 M4A / WAV, 不入库二进制).
- [ ] (插件) GitHub Actions: `build.yml` (assemble + test), `android-test.yml` (API 35 AVD 仪器测试, 与 CI 同款 AVD 配方), `release.yml` (tag 触发, 产出 `autojs6-plugin-three-slate-studio-v{VERSION_NAME}-{CRC32}.apk` 与索引清单草稿).
- 验收: 本地 `./gradlew test connectedDebugAndroidTest` 通过; 工作流文件经 `prepush_check.py` 检查 (远端运行待首次推送).

---

## P1: 宿主契约, 录制内核与脚本模块

状态: 未开始. 全部为宿主本地提交 (不推送). 设备: Sony XQ-AT72 API 31, XQ-DQ72 API 33 (录屏与内录), G8441 API 28 (无内录), Xiaomi Pad API 35.

### P1.1 契约模块 `plugin-api/media-studio-api` (D20, D28)

- [ ] (宿主) 新建 `plugin-api/media-studio-api` (照 `speech-api`): `MediaStudioPluginActions.MEDIA_STUDIO`, `MediaStudioPluginIds` (官方 ID / 包名 / `ENGINE = "media-studio"` / `CATEGORY`), `MediaStudioContract.CONTRACT_VERSION = 1`, `MediaStudioCapabilityKeys`, `MediaStudioLimits` (并发, 文件大小警告阈值, GIF 帧数), `MediaStudioRequestKeys` / `ResultKeys` / `ProgressKeys` / `ErrorCodes`, `MediaStudioOps` (操作名常量表, 与附录 B 一一对应); AIDL `IMediaStudioPlugin` (`getInfo` 居首), `IMediaStudioCallback`; 草案见附录 C.
- [ ] (宿主) `maven-central.gradle.kts` 登记, AAR 产出到插件 `libs/`, `locks/host-api-aars.lock` 记录版本与 SHA-256.
- 验收: 宿主 `:plugin-api:media-studio-api:assemble` 通过; AIDL 编译进宿主 app; 常量表与附录 B 的方法名逐项对得上.

### P1.2 录屏 / 录音内核泛化 (D26)

- [ ] (宿主) 把 `engine/NodeScreenRecording.kt`, `ScreenRecordingAudio.kt`, `ScreenRecordingFrames.kt`, `ScreenRecordingMuxer` 与 Node 纯音频录制抽到 `core/record/` (`ScreenRecorder`, `AudioRecorder`, `RecordingMuxer`, `RecordingOptions`), Node 桥接改为调用新内核, 行为不变 (对照 `docs/dev/runtime-node-recording-2026-10-17.md` 的用例复跑).
- [ ] (宿主) 新增 Rhino 侧的 `maxDuration` (到时自动 stop 并触发 `onStop`), `onStop` 回调, `capture()` 取当前帧; 录音内核支持 WAV (PCM16) 输出与 M4A; 内录声道数按附录 D Q1.
- 验收: Node 桥接的录屏 / 录音用例在 API 31 / 33 真机复跑通过; 新内核单元测试 (选项校验, 偶数宽高, 输出已存在) 通过.

### P1.3 脚本模块 `video` / `audio` (D6, D8, D27)

- [ ] (宿主) `runtime/api/VideoModule`, `AudioModule` 与 JS 包装 (`modules/__video__.js` / `__audio__.js` 或同等位置): 录制方法自实现; 加工方法校验选项后经 `MediaStudioPluginHost` 转调插件; 同步方法禁止 UI 线程 (`INVALID_OPTIONS`), `xxxAsync` 返回 Promise; `onProgress` 回调经回调 Binder 投递到脚本线程; 停止脚本取消全部任务; `tasks()` / `cancelAll()`; 默认输出路径 (D14) 由宿主填充; 插件缺失 / 禁用 / 不兼容按 `PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` / `PLUGIN_INCOMPATIBLE` 抛 `MediaStudioError` (`code`, `message`, `taskId`).
- [ ] (宿主) 能力声明: `screen_capture`, `media.recording` + `RECORD_AUDIO`, `media`; 裸脚本 / MCP / Node 的能力声明一致; 绑定 flags 与 `allowActivityStarts` (D27).
- [ ] (宿主) 样例脚本 `samples/` 草稿: 录屏 10 秒并转 GIF, 录音并转 FLAC, 裁剪 + 水印 + 拼接, 时间线合成 (P5.3 定稿).
- 验收: 宿主 debug 构建装到 API 31 / 33 真机, 用插件 P2.1 的 `probe` 与 `trim` 走通脚本 -> 宿主 -> 插件 -> 回调全链路; 插件未安装时错误码正确; UI 线程调用同步方法被拒.

### P1.4 `CREATE_SCRIPT` 入口 (D19)

- [ ] (宿主) `CreateScriptActivity` (action `org.autojs.autojs.action.CREATE_SCRIPT`, 受 `org.autojs.permission.PLUGIN` 保护, extras `code`, `suggestedName`, `source`), 打开编辑器并新建未保存文件 (文件名建议 = `suggestedName`, 冲突时加序号); 代码超过 512 KB 时拒绝并提示.
- 验收: 用 `am start` 与插件 P4.4 各验一次; 无 PLUGIN 权限的应用调用被拒.

### P1.5 注册, 协议文档, changelog 与版本回填 (D38)

- [ ] (宿主) 插件中心登记 `three-slate-studio` (官方 ID, 包名, 最低插件版本), `PluginInstallWizardCatalog` `TOOLS` 条目, 协议文档 `docs/dev/media-studio-plugin-protocol.md` (契约, 能力声明, 错误码, 录制内核泛化说明), `.changelog/lang_zh-Hans.json` 条目 (通俗文案).
- [ ] (宿主) 插件 `REQUIRES_HOST_VERSION` 临时值 = 含 P1.1-P1.4 的本地构建号, P5.4 回填.
- 验收: 插件中心能发现, 安装, 启用插件并显示描述与图标; 协议文档与 AIDL 一致.

---

## P2: 插件加工核心

状态: 未开始. 设备: API 35 AVD (仪器测试), Sony XQ-DQ72 API 33 与 Xiaomi Pad API 35 (硬件编码器), G8441 API 28 (低端机性能基线).

### P2.1 执行器, 任务模型与探测 (D28, D29, D14)

- [ ] (插件) `ThreeSlateStudioService` 实现 `submit` / `cancel` / `probe` / `listTasks`; `TaskRunner` (队列, 并发 2 编码, 每宿主 token 4 任务, 宿主 Binder 死亡即取消并删除未完成输出); 长任务 (> 10 秒) 启动 `dataSync` 前台服务与进度通知; `OutputPaths` (默认目录, 命名, 冲突加序号), `MediaStoreRegistrar`; `MANAGE_EXTERNAL_STORAGE` 检查与引导 Activity; `probe` 返回时长 / 宽高 / 旋转 / 帧率 / 码率 / 编码 / 音轨 / 大小.
- [ ] (插件) 选项校验层 (与宿主同一规则二次校验), 错误码表 (附录 C.3), `warnings[]` 机制.
- 验收: 单元测试覆盖队列 / 并发 / 取消 / 输出命名; 仪器测试经 Binder 跑 `probe` 与一个 `trim`; 宿主 P1.3 真机联调通过.

### P2.2 视频基础加工 (D29)

- [ ] (插件) `trim` (免重编码优化优先, 失败回退重编码), `concat` (分辨率不一致时按首段或 options 缩放), `speed` (`keepPitch` 用音频变速处理器), `transform` (缩放 / 旋转 / 裁切 / 翻转), `transcode` (H.264 / H.265, 分辨率, 码率, 帧率, 音频参数), `removeAudio`, `extractAudio` (M4A / WAV / FLAC / OGG, 复用 P2.5 编码器).
- 验收: 每个操作在 3 秒彩条夹具上有仪器测试 (输出可探测, 时长 / 尺寸 / 轨道符合预期); 真机上 1080p 60 秒素材的转码耗时与输出码率记录到附录 F.

### P2.3 叠加 (D29)

- [ ] (插件) `overlay`: 文字 (字体 / 字号 / 颜色 / 描边 / 阴影 / 对齐 / 位置 / 时间段 / 透明度, 系统字体或 TTF 路径) 与图片 (路径, 缩放, 位置, 时间段, 透明度), 多个叠加一次完成; 坐标支持像素与百分比.
- 验收: 仪器测试对输出抽帧并比对叠加区域的像素差; 真机目视一次.

### P2.4 抽帧与 GIF (D32)

- [ ] (插件) `frames` (按时间点 / 间隔 / 数量, PNG / JPG, 宽度, 输出目录) 与 `frame` (单帧返回 `Image`), `thumbnail`; `toGif` (起止, fps, 宽度, 循环, 抖动, 上限 600 帧), 自带 `GifEncoder` (中位切分调色板 + LZW).
- 验收: GIF 输出能被 `MediaMetadataRetriever` / 浏览器解码且帧数与时长正确; 单元测试覆盖调色板与 LZW 边界 (256 色, 单色, 超长帧).

### P2.5 音频加工 (D15)

- [ ] (插件) `audio.trim`, `concat`, `mix` (多路, 各自音量与偏移), `volume` (增益 / 归一化 / 淡入淡出), `convert` (M4A / WAV / FLAC / OGG-Opus, 采样率, 声道, 码率), `speed`; `AudioPipeline` 用 media3 解码 + `AudioProcessor` + 平台编码器, OGG 用 `MediaMuxer` OGG (API 29+, 低版本报 `UNSUPPORTED_FORMAT`).
- 验收: 每种输出格式有仪器测试 (可解码, 时长与声道正确); 混音结果在真机听一次.

### P2.6 设备矩阵与性能基线

- [ ] (测试) 附录 F 设备矩阵跑一遍 P2 的回归脚本 (插件仓库 `scripts/device-regression.js`, 只输出每项通过 / 失败与耗时); 记录各机硬件编码器名称, 1080p 转码倍速, H.265 可用性.
- 验收: 结果表回填附录 F; 失败项登记为条目或待决.

---

## P3: 多轨合成

状态: 未开始. 设备: API 35 AVD, Sony XQ-DQ72 API 33, Xiaomi Pad API 35, G8441 API 28 (GL 效果在低端 GPU 的可用性).

### P3.1 时间线 JSON 与 `video.compose` (D33)

- [ ] (插件) 时间线模型与校验 (轨道, 片段, 时间重叠规则, z 顺序), `CompositionBuilder` 把时间线编成 media3 `Composition` (视频轨 -> 序列, 音频轨 -> 音频序列, 叠加 -> `OverlayEffect`, 片段变换 -> `Effects`); `compose` 操作接入 `TaskRunner`; 工程文件 `.slate.json` 读写.
- 验收: 仪器测试: 两段视频 + 一段音频 + 一个文字叠加的时间线导出成功, 时长 / 轨道 / 叠加位置正确; JSON 校验的错误信息指出字段路径.

### P3.2 转场 (D30)

- [ ] (插件) 7 种转场的 GLSL 效果与重叠区间双序列合成; 转场时长校验; 时间线 `transition` 字段与界面转场表同源.
- 验收: 每种转场在仪器测试里导出 2 秒样本并抽取中间帧比对 (两源各占一定比例); 真机 (含 API 28) 目视一次, 记录 GPU 不支持时的回退 (退为 `dissolve` 或 `cut` 并返回 warning).

### P3.3 字幕 (D31)

- [ ] (插件) SRT 解析 / 导出, ASS 基础子集解析 (样式表 + 允许的内联标签, 其余忽略并 warning), 字幕渲染器 (`StaticLayout` 到 `TextOverlay`), `burnSubtitles` 操作与时间线 `subtitles` 字段; `video.subtitles.parse` / `write` 暴露给脚本.
- 验收: 单元测试覆盖 SRT (BOM, CRLF, 空行, 重叠时间) 与 ASS 子集 (样式继承, 标签覆盖, 忽略标签 warning); 烧录结果抽帧目视.

---

## P4: Studio 界面

状态: 未开始. 设备: API 24 AVD (最低版本布局), API 35 AVD, Sony XQ-AT72 API 31, Xiaomi Pad API 35 (大屏), OnePlus API 35 (ColorOS).

### P4.1 应用骨架, 首页, 任务队列, 设置, 发行历史 (D37)

- [ ] (插件) Compose 应用骨架 (导航, 主题, 阿拉伯语 RTL 与 FSI / PDI), 首页 (D37 目录的媒体列表, 缩略图, 详情页含 "用 MediaInfo 查看" 入口), 任务队列页 (进度, 取消, 失败原因), 设置页 (D37 各项 + 启动器图标模式 + "所有文件访问" 引导), 发行历史与更新检查 (照 Maple Image).
- 验收: API 24 / 35 AVD 与两台真机界面可用; 亮暗主题与阿拉伯语各截一次图.

### P4.2 工具箱向导 (D35)

- [ ] (插件) 每个加工 API 一个向导页 (P2.2-P2.5 全部操作), 共用文件选择器 (SAF + 首页目录), 参数表单由 `OptionSchemas` 驱动, 结果页 (输出路径, 媒体信息, 打开 / 分享, 生成的代码预览).
- 验收: 每个向导走通一次并与脚本调用结果一致; 表单字段与 d.ts 选项逐项对应 (单元测试比对 schema).

### P4.3 时间线剪辑器 (D10, D34)

- [ ] (插件) spike: `CompositionPlayer` 在 API 28 / 33 / 35 真机的实时预览可用性, 结论回填附录 D Q3.
- [ ] (插件) 多轨时间线 UI (视频 / 音频 / 叠加 / 字幕轨, 缩略图与波形, 缩放, 吸附), 片段操作 (拖入, 裁剪, 分割, 删除, 移动, 变速, 音量), 转场选择与时长, 叠加编辑 (文字 / 图片, 位置拖动), 字幕编辑 (列表, 时间, 样式, SRT / ASS 导入, SRT 导出), 预览播放, 工程保存 / 打开 (`.slate.json`), 导出 (调用 `compose`, 进入任务队列).
- 验收: 用 3 段素材 + 音乐 + 字幕做一个 30 秒工程并导出, 与脚本 `video.compose` 同一 JSON 的输出一致; Xiaomi Pad 大屏与手机竖屏各验一次; 撤销 / 重做 10 步以内可用.

### P4.4 代码生成与宿主入口 (D11, D36)

- [ ] (插件) `CodeGenerator` (向导选项 -> `video.*` / `audio.*` 代码; 时间线 -> `video.compose({...})` 代码), 复制按钮, "在 AutoJs6 中打开" (P1.4 入口, 宿主不可用时隐藏).
- 验收: 单元测试: 生成的代码经 Rhino 解析无语法错误且选项对象与输入相等; 真机上从向导到宿主编辑器新建文件走通一次.

---

## P5: 文档, 声明, 四仓库同步, 样例与发布

状态: 未开始. 设备: 附录 F 全部可用设备各跑一次冒烟.

### P5.1 文档 (AutoJs6-Documentation) (D40)

- [ ] (文档) `api/video.md` 与 `api/audio.md` (10 种语言, 半角标点, 照 `speech.md` 版式: 方法, options, 返回对象, 错误码, 示例, 版本标记 `6.8.0`), 时间线 JSON 与转场 / 字幕说明, FFmpeg 透传的 1.1.0 预告与 Garage 插件迁移说明; 插件文档页 `plugins/threeSlateStudio.md` (安装, 权限, 界面, 大库); changelog JSON 10 语言.
- 验收: 文档站本地构建通过; 示例脚本在真机跑过.

### P5.2 声明 (AutoJs6-TypeScript-Declarations, Ace Editor, Offline Docs)

- [ ] (文档) `video.d.ts` / `audio.d.ts` (与插件 `OptionSchemas` 同源, 单元测试比对), Ace 补全与高亮, Offline Docs 打包.
- 验收: d.ts 通过 `tsc` 检查; Ace 补全在宿主编辑器里能提示 `video.` 的方法.

### P5.3 README, changelog, 样例与兼容矩阵

- [ ] (插件) README (功能, 安装, 权限, 脚本示例, 界面截图, FFmpeg 大库节, 许可证), `CHANGELOG.md` 与 10 语言 changelog JSON, `samples/` 定稿 (P1.3 草稿), 兼容矩阵 (宿主最低版本, Android 版本与格式支持表).
- [ ] (宿主) 宿主 `samples/` 同步样例脚本.
- 验收: 兼容矩阵中每一格有附录 F 的证据编号.

### P5.4 1.0.0 发布 (D23)

- [ ] (发布) 发布门禁 1-5 全部通过; `REQUIRES_HOST_VERSION` 回填; tag `v1.0.0`, Release 附 APK 与源码归档; (索引) 官方索引登记 (release-manifests 与 catalog-icons); (插件) 版本号推进到 1.1.0-dev.
- 验收: 从官方索引安装的 1.0.0 在两台真机跑通 `samples/`.

---

## P6: 1.1.0 FFmpeg + libass 大库

状态: 未开始 (D4, D5, D13, D24). 设备: Samsung SM-A566B API 36 (16 KB) 必跑, 其余同 P2.

### P6.1 FFmpeg 自建或选型 (附录 D Q5)

- [ ] (插件) 决定自建 (LGPL 配置, `--enable-mediacodec --enable-jni`, libass + FreeType + HarfBuzz + fribidi, 4 ABI, NDK r26c 补丁, 16 KB 对齐) 或采用社区分支; 构建脚本与 `locks/` 记录 (源码 tag, 配置, SHA-256); 许可审查 (无 GPL 组件, 专利说明).
- 验收: 4 个 ABI 的 `.so` 产出, `PT_LOAD` 对齐 16384, 在 Samsung 16 KB 设备加载成功.

### P6.2 Shared-Assets `media-libs` 类别

- [ ] (资产) 新类别 `media-libs/` (照 `speech-models`: 目录 JSON + 分离签名, 按 ABI 的大库包, 发布工作流, 标签 `media-libs-vN`); 大库包含 `NOTICES` 与源码对应关系.
- 验收: `media-libs-v1` 发布, 目录签名校验通过.

### P6.3 插件大库下载, 校验与加载

- [ ] (插件) `LibraryManager` (目录刷新, 按 ABI 下载到私有目录, SHA-256 校验, 原子激活, 删除), 设置页与首页的安装入口, `dlopen` 加载与 JNI 桥 (`FfmpegRunner`: 参数数组, 进度解析, 超时, 取消, 文件白名单, 禁用网络协议).
- 验收: 断网 / 校验失败 / 中途取消各一个用例; 未装大库时所有依赖大库的操作报 `LIBRARY_MISSING`.

### P6.4 透传, 新格式与完整 ASS

- [ ] (插件) `video.ffmpeg(args, options)` (宿主侧同步 + Async), MP3 / WebM / MKV 输出接入 `convert` / `transcode`, ASS 改走 libass 渲染 (`AssRenderer`), 文档与 d.ts 更新.
- 验收: Garage 插件文档里的典型命令在 `video.ffmpeg` 下跑通; ASS 特效样本渲染正确.

### P6.5 1.1.0 发布

- [ ] (发布) 门禁 1-5 (含 16 KB 设备), tag `v1.1.0`, 索引登记.

---

## P7: 1.2.0 录屏增强

状态: 未开始 (D2, D13). 设备: Sony API 31 / 33, Xiaomi Pad API 35, OnePlus API 35 (ColorOS 后台弹窗), Samsung API 36.

### P7.1 插件自持投影录屏

- [ ] (插件) 插件自己的 MediaProjection 授权与 `mediaProjection` 前台服务, 编码 / 封装复用宿主 `core/record` 的实现 (复制并保持同步说明), 契约新增 `startRecording` / `stopRecording` 等操作; 脚本 `video.record(path, {engine: "plugin"})` 选择插件路线 (默认仍为宿主); 宿主后台时经 `allowActivityStarts` 弹授权.
- 验收: 四台真机后台与熄屏录屏通过; 与宿主截图会话互不干扰.

### P7.2 悬浮控制条与 Studio 录屏入口

- [ ] (插件) 悬浮窗控制条 (开始 / 暂停 / 停止 / 计时 / 麦克风开关), Studio 首页录屏入口与录屏设置 (分辨率, 帧率, 音频源, 倒计时); 悬浮窗权限引导.
- 验收: 控制条在 ColorOS 与 HyperOS 可拖动且不被杀; 录制结果出现在首页列表.

### P7.3 Shizuku 免确认录屏

- [ ] (插件) 已装 Shizuku 且授权时用 shell `screenrecord` (或 `MediaProjection` 的 shell 授权路径) 免系统确认; 不可用时回落 P7.1; 设置页开关与说明.
- 验收: 两台真机免确认录屏成功; 未授权时回落正确.

### P7.4 1.2.0 发布

- [ ] (发布) 门禁 1-5, tag `v1.2.0`, 索引登记.

---

## 附录 A. 目录结构 (1.0.0 目标)

```text
AutoJs6-Plugin-Three-Slate-Studio/
  ROADMAP.md  README.md  CHANGELOG.md  LICENSE  THIRD_PARTY_NOTICES.md  .gitattributes  .editorconfig
  version.properties  settings.gradle.kts  build.gradle.kts  gradle/libs.versions.toml  build-logic/
  libs/common-plugin-api.aar  libs/media-studio-api.aar  locks/host-api-aars.lock
  .python/icons/three-slate-ic-launcher-{light,dark}.png  .python/generate_icon_studio.py  .python/icon_studio_runtime.py  .python/generate_media_fixtures.py
  .icons/recipe.json  .icons/assets/
  .github/workflows/{build,android-test,release}.yml
  samples/  scripts/device-regression.js  docs/development/
  app/src/main/java/io/github/supermonster003/autojs6/plugin/three/slate/studio/  (5.4 节)
  app/src/main/res/values*/strings.xml (10 语言)  app/src/main/res/mipmap*/ (Icon Studio 产物)
  app/src/test/  app/src/androidTest/
```

## 附录 B. 脚本 API 草案

### B.1 `video` 方法

| 方法 | 实现方 | 说明 |
| --- | --- | --- |
| `video.isAvailable()`, `video.version()` | 宿主 | 插件是否可用, 插件与契约版本 |
| `video.record(path?, options?)` | 宿主 | 开始录屏, 返回 `ScreenRecording` (`id`, `path`, `stop()`, `cancel()`, `status()`, `capture()`) |
| `video.probe(src)` | 插件 | 媒体信息 |
| `video.trim(src, {start, end?, duration?}, output?)` | 插件 | 按时间裁剪 |
| `video.concat(sources[], options?, output?)` | 插件 | 顺序拼接 |
| `video.speed(src, {factor, keepPitch?}, output?)` | 插件 | 变速 0.25-8 |
| `video.transform(src, {width?, height?, scale?, rotate?, crop?, flip?}, output?)` | 插件 | 缩放 / 旋转 / 裁切 / 翻转 |
| `video.overlay(src, overlays[], output?)` | 插件 | 文字 / 图片叠加 |
| `video.frames(src, {at?, times?, every?, count?, width?, format?}, outputDir?)` | 插件 | 抽帧, 返回路径数组 |
| `video.frame(src, at, options?)` | 插件 | 单帧, 返回 `Image` |
| `video.thumbnail(src, options?)` | 插件 | 缩略图 |
| `video.toGif(src, {start?, end?, fps?, width?, loop?, dither?}, output?)` | 插件 | 转 GIF |
| `video.extractAudio(src, {format?}, output?)`, `video.removeAudio(src, output?)` | 插件 | 抽音频 / 去音轨 |
| `video.transcode(src, {codec?, width?, height?, bitRate?, frameRate?, audio?}, output?)` | 插件 | 转码 |
| `video.compose(timeline, output?, options?)` | 插件 | 多轨合成 (B.4) |
| `video.burnSubtitles(src, subtitles, style?, output?)` | 插件 | 烧录字幕 |
| `video.subtitles.parse(path)`, `video.subtitles.write(path, cues, format?)` | 插件 | 字幕文件读写 |
| `video.tasks()`, `video.cancelAll()` | 宿主 | 本脚本的进行中任务 |
| `video.ffmpeg(args, options?)` | 插件 (1.1.0) | 命令透传 |

每个加工方法都有 `xxxAsync` 形态; `options.onProgress(progress)` 的 `progress` 为 `{percent, stage, etaMillis, taskId}`. 省略 `output` 时按 D14 生成路径.

### B.2 `audio` 方法

| 方法 | 实现方 | 说明 |
| --- | --- | --- |
| `audio.record(path?, options?)` | 宿主 | 开始录音 (`source`: mic / playback / mixed, `format`: m4a / wav, `sampleRate`, `channels`, `bitRate`, `maxDuration`, `onStop`), 返回 `AudioRecording` (`stop()`, `cancel()`, `status()`) |
| `audio.probe(src)` | 插件 | 音频信息 |
| `audio.trim(src, {start, end?, duration?}, output?)`, `audio.concat(sources[], output?)` | 插件 | 裁剪 / 拼接 |
| `audio.mix(inputs[], output?, options?)` | 插件 | `inputs[]` 为 `{src, volume?, at?}` |
| `audio.volume(src, {gain?, normalize?, fadeIn?, fadeOut?}, output?)` | 插件 | 音量与淡入淡出 |
| `audio.convert(src, {format, sampleRate?, channels?, bitRate?}, output?)` | 插件 | 转格式 (m4a / wav / flac / ogg; 1.1.0 加 mp3) |
| `audio.speed(src, {factor, keepPitch?}, output?)` | 插件 | 变速 |
| `audio.tasks()`, `audio.cancelAll()` | 宿主 | 与 `video.tasks()` 同一任务表 |

### B.3 返回对象与错误

- 加工结果: `{path, durationMillis, width?, height?, sizeBytes, mime, codec?, warnings: string[]}`; 抽帧返回 `string[]` 或 `Image`.
- 录制结果 (`stop()` 返回): `{path, durationMillis, width, height, sizeBytes, audio}`.
- 错误 `MediaStudioError`: `code` 取附录 C.3 的错误码, `message`, `taskId?`.

### B.4 时间线 JSON 草案 (D33)

```json
{
  "width": 1080, "height": 1920, "frameRate": 30, "background": "#000000",
  "video": [
    {"clips": [
      {"src": "/sdcard/Movies/a.mp4", "start": 0, "end": 8000, "at": 0, "speed": 1.0, "volume": 1.0,
       "transform": {"scale": 1.0, "rotate": 0, "crop": null},
       "transition": {"type": "dissolve", "duration": 500}},
      {"src": "/sdcard/Movies/b.mp4", "start": 2000, "end": 10000, "at": 7500}
    ]}
  ],
  "audio": [
    {"clips": [{"src": "/sdcard/Music/bgm.m4a", "at": 0, "end": 15500, "volume": 0.4, "fadeIn": 1000, "fadeOut": 2000}]}
  ],
  "overlays": [
    {"type": "text", "text": "Hello", "from": 1000, "to": 4000, "x": "50%", "y": "10%", "fontSize": 48, "color": "#FFFFFF", "stroke": "#000000"},
    {"type": "image", "src": "/sdcard/Pictures/logo.png", "from": 0, "to": 15500, "x": "90%", "y": "90%", "scale": 0.3, "alpha": 0.8}
  ],
  "subtitles": {"src": "/sdcard/Movies/a.srt", "style": {"fontSize": 40, "color": "#FFFFFF", "stroke": "#000000", "align": "bottom"}}
}
```

`transition` 写在后一段上 (与前一段的重叠), `type` 取 D30 的 7 种; `at` 省略时紧接前一段; 时间单位毫秒.

### B.5 示例

```javascript
// 录屏 10 秒并转 GIF
let rec = video.record(null, {audio: "none", frameRate: 30});
sleep(10000);
let clip = rec.stop();
let gif = video.toGif(clip.path, {fps: 10, width: 480});
toastLog(gif.path);

// 裁剪 + 水印 + 拼接, 带进度
let a = video.trim("/sdcard/Movies/a.mp4", {start: 0, end: 5000});
let b = video.overlay("/sdcard/Movies/b.mp4", [{type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48}], null);
let out = video.concat([a.path, b.path], {onProgress: p => console.log(p.stage, p.percent)});

// UI 模式用 Async
video.transcodeAsync("/sdcard/Movies/big.mp4", {codec: "h265", height: 720}).then(r => toast(r.path));
```

## 附录 C. 契约草案

### C.1 `plugin-api/media-studio-api`

```text
package org.autojs.plugin.mediastudio.api
interface IMediaStudioPlugin {
    Bundle getInfo();                                             // 契约版本, 插件版本, 能力列表 (ops, 编码器, 格式), 大库状态 (1.1.0)
    int submit(in Bundle request, IMediaStudioCallback callback); // request: op, inputs[], output, options (Bundle), hostToken; 返回 taskId, 失败抛 code
    boolean cancel(int taskId);
    Bundle probe(String path);
    Bundle listTasks(String hostToken);
}
oneway interface IMediaStudioCallback {
    void onProgress(int taskId, in Bundle progress);  // percent, stage, etaMillis
    void onComplete(int taskId, in Bundle result);    // 附录 B.3 结果
    void onError(int taskId, in Bundle error);        // code, message
}
```

### C.2 操作名 (`MediaStudioOps`)

`probe`, `trim`, `concat`, `speed`, `transform`, `overlay`, `frames`, `frame`, `thumbnail`, `toGif`, `extractAudio`, `removeAudio`, `transcode`, `compose`, `burnSubtitles`, `subtitlesParse`, `subtitlesWrite`, `audioProbe`, `audioTrim`, `audioConcat`, `audioMix`, `audioVolume`, `audioConvert`, `audioSpeed`; 1.1.0: `ffmpeg`; 1.2.0: `recordStart`, `recordStop`, `recordCancel`, `recordStatus`.

### C.3 错误码

`PLUGIN_UNAVAILABLE`, `PLUGIN_DISABLED`, `PLUGIN_INCOMPATIBLE`, `INVALID_OPTIONS`, `LIMIT_EXCEEDED`, `TASK_LIMIT`, `UNSUPPORTED_FORMAT`, `CODEC_ERROR`, `IO_ERROR`, `STORAGE_PERMISSION_DENIED`, `TASK_CANCELLED`, `TASK_NOT_FOUND`, `LIBRARY_MISSING` (1.1.0), `PERMISSION_DENIED` (1.2.0 投影 / 悬浮窗), `INTERNAL_ERROR`.

## 附录 D. 待决事项

格式: 编号 | 事项 | 选项 (推荐项标 "(推荐)") | 拍板时点 | 状态.

| 编号 | 事项 | 选项 | 拍板时点 | 状态 |
| --- | --- | --- | --- | --- |
| Q1 | 内录 / 混合音频的声道数 | (a) 立体声 48 kHz (推荐, 内录多为立体声, 文件约大 1 倍) / (b) 沿用 Node 的单声道 | P1.2 | 需拍板 |
| Q2 | GIF 编码器 | (a) 自带纯 Kotlin (推荐, D32) / (b) 第三方 Apache-2.0 或 MIT 库 | P2.4 | 需拍板 |
| Q3 | 时间线预览引擎 | (a) media3 `CompositionPlayer` (推荐, 实验 API, 由 spike 决定) / (b) 自写预览 | P4.3 spike 后 | 待 spike |
| Q4 | 向导目录分类 | (a) `TOOLS` (推荐, 现有分类) / (b) 新增 `MEDIA` 分类 (需宿主改向导目录与 10 语言标题) | P1.5 | 需拍板 |
| Q5 | 1.1.0 FFmpeg 来源 | (a) 自建 (推荐, 许可与 16 KB 可控, 需交叉编译链) / (b) 社区分支 `dev.ffmpegkit-maintained` (省构建, 许可审查与补丁跟进由我们承担) | P6.1 | 需拍板 |
| Q6 | 单任务上限 | (a) 无硬上限, 超过 2 GB 或 60 分钟返回 warning (推荐, D29) / (b) 硬上限 4 GB | P2.1 | 需拍板 |
| Q7 | H.265 默认 | (a) 默认 H.264, H.265 可选 (推荐, 兼容性) / (b) 设备支持时默认 H.265 | P2.2 | 需拍板 |

## 附录 E. 证据等级

| 等级 | 含义 |
| --- | --- |
| E1 | 单元测试 (JVM) |
| E2 | 仪器测试 (AVD, 与 CI 同款配方) |
| E3 | 真机脚本 (`scripts/device-regression.js` 或宿主 `samples/`), 只记通过 / 失败与关键数值 |
| E4 | 需维护者在手机上操作的项 (授权对话框, 悬浮窗, 启动器图标), 由维护者确认 |
| E5 | 外部文档或源码阅读 (Android / media3 文档, 宿主源码), 需注明出处 |

## 附录 F. 基线快照与设备矩阵

基线 (2026-10-22):

- 宿主 AutoJs6 `master` `63d1cb259`, `VERSION_NAME` 6.8.0 (未发布), `VERSION_BUILD` 4183, versionCode 5835 (偏移 1652), minSdk 24, compile / target 37; Node 录屏内核 `bd522f8a7` / `e8f8edb7a` (2026-10-17); Shizuku 13.1.5 已依赖.
- 插件平台: `PLATFORM_VERSIONS_PLUGIN_VERSION` 1.9.1; 3-Maple Image: Compose BOM 2026.09.00, navigation 2.10.2, `MANAGE_EXTERNAL_STORAGE`; 3-Terra Player: media3 1.10.1; 3-Macaw Speech 1.0.0 发布时 `REQUIRES_HOST_VERSION` 5456 (宿主未公开, D23 先例); MediaInfo 2.1.4; Icon Studio 内核 1.4.0.
- 外部: media3 1.10.1; FFmpeg 7.x (1.1.0 时再定); libass 0.17.x.

设备矩阵 (`COMMON_AGENTS.md`):

| 设备 | API | 用途 |
| --- | --- | --- |
| Sony G8441 (`BH900ASK9E`) | 28 | 低端性能基线, 无内录, GL 效果可用性 |
| Sony XQ-AT72 (`QV710AF65F`) | 31 | 录屏 / 内录, 界面 |
| Sony XQ-DQ72 (`QV770340J7`) | 33 | 硬件编码器, 内录, 主力联调 (注意 debug 宿主在此机的 ART JIT ANR 记忆) |
| Xiaomi Pad 23046RP50C (`968e9f18`) | 35 | HyperOS, 大屏时间线, 存储授权需手动 |
| OnePlus OPD2413 (`63751f63`) | 35 | ColorOS 后台弹窗 / 前台服务 |
| Redmi 22120RN86C (`bek749scrwv4wo8h`) | 33 | 旧宿主隐藏验证 (不兼容提示) |
| Samsung SM-A566B / SM-F971B (远程租用) | 36 / 37 | 16 KB 页 (1.1.0 起必跑) |
| AVD API 24 / 35 / 37 (`E:\.android\avd\`) | 24 / 35 / 37 | 最低版本布局, CI 同款仪器测试, 16 KB |

性能基线表 (P2.6 回填): 设备 | 硬件编码器 | 1080p 60 秒转码耗时 | H.265 | 备注.

## 附录 G. 已评估暂缓

| 事项 | 结论 |
| --- | --- |
| 宿主持投影, 插件编码 (跨进程 Surface + PCM 管道) | 一次授权但实现与排错成本最高, 且录屏期间宿主截图需额外分发; 被 D2 的两条路线取代 |
| 宿主传 FD 的零存储权限方案 | Media3 输出 FD 需自定义 Muxer 工厂, 界面选文件仍需媒体权限; 维护者选 D9 |
| 纯 Media3 不做 FFmpeg | 永远做不了 MP3 / WebM / MKV / 滤镜; 维护者选 D4 |
| 内置 Shine 做 MP3 | 质量低于 LAME 且多一个 LGPL 声明; MP3 随 1.1.0 FFmpeg (D15) |
| 1.0.0 打包 libass 或独立大库 | 会让 1.0.0 引入原生库与 ABI 拆分; 随 1.1.0 大库 (D24) |
| 扩展宿主 `media` 模块 | 与媒体库扫描 / 播放语义混在一起; 维护者选新模块 (D6) |
| 探测硬依赖 MediaInfo 插件 | 多一条转调逻辑与测试矩阵; 松耦合 (D18) |
| 写临时文件再 VIEW 打开 (新建脚本) | 文件落盘且路径写死; 维护者选 `CREATE_SCRIPT` (D19) |
| 等宿主 6.8.0 公开再发 1.0.0 | 发布时点不可控; 照 Macaw 先例不等 (D23) |
| 在线 / AI 字幕, 相机, 直播, 画中画 | 不在本插件范围 (第 3 节) |
