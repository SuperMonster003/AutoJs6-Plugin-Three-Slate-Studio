# 3-Slate Studio 插件路线图

更新: 2026-10-22 (立项). 本文件是 `AutoJs6-Plugin-Three-Slate-Studio` (显示名 `3-Slate Studio`, 录屏 + 媒体加工方向, 插件选题分析 2026-10 的第 3 优先级候选) 的唯一进度与决策记录. 立项基线: 宿主 AutoJs6 `master` `63d1cb259` (2026-10-22, `VERSION_NAME` 6.8.0 未发布, `VERSION_BUILD` 4183, versionCode 5835), 宿主 Node 桥接录屏内核 `bd522f8a7` / `e8f8edb7a` (2026-10-17), 3-Macaw Speech `ROADMAP.md` 作为版式与流程模板. 2026-11-05: 维护者拍板 Q1 / Q4 / Q6 (均选推荐项) 并补充 Studio 录音需求 (Q8, D41, P4.5); 宿主基线改为公开历史重写后的 6.8.1 (附录 F).

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

D25-D40 据第 1 节派生, D41 据维护者 2026-11-05 补充需求 (附录 D Q8) 派生; 进入对应阶段前可推翻 (推翻点见各条), 之后视同固定.

| 编号 | 决策 | 含义 | 推翻点 |
| --- | --- | --- | --- |
| D25 | 工具链基线 | 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 的占位符表与 3-Maple Image 的当前依赖线: compileSdk / targetSdk 37, minSdk 24, Kotlin 2.x, Compose BOM 2026.09.00, navigation 2.10.2, media3 1.10.1 (`media3-transformer`, `media3-effect`, `media3-muxer`, `media3-exoplayer`, `media3-ui`), `PLATFORM_VERSIONS_PLUGIN_VERSION` 1.9.1; 版本号起 1.0.0, `VERSION_BUILD` 在骨架阶段为提交数 + 1. 1.0.0 APK 为 universal 单包 (不拆 ABI); 原生库仅 Compose 自带的 graphics-path (`androidx.graphics:graphics-path` 的 `libandroidx.graphics.path.so`, 4 个 ABI, 附录 D Q9), 16 KB 页校验在 1.0.0 确认这 4 个文件 STORED 且 16 KB 对齐, 依赖里不得再出现其他 `.so`. | P0.1 |
| D26 | 宿主录屏 / 录音内核泛化 | 把 `engine/NodeScreenRecording.kt`, `ScreenRecordingAudio.kt`, `ScreenRecordingFrames.kt`, `ScreenRecordingMuxer` 与 Node 的纯音频录制从 `engine/` 抽到 `core/record/` (类名 `ScreenRecorder`, `AudioRecorder`), Node 桥接与 Rhino `video.record` / `audio.record` 共用; 行为与 Node 版一致 (H.264 MP4, 宽高须为偶数且同时给出, `frameRate` 1-120 默认 30, `bitRate` 默认 8 Mbps, `audio` none / mic / playback / mixed, AAC 48 kHz 128 kbps, playback / mixed 需 API 29+), Rhino 侧增加 `maxDuration` 与 `onStop`. 同一宿主进程同时只有一个录屏; 录屏期间 `images.captureScreen` 由帧分发回读 (宿主已实现). 内录声道数见附录 D Q1 (已决: Rhino `audio.record` / `video.record` 的 playback / mixed 默认 2 声道 48 kHz, `channels` 可改 1, AAC 默认 192 kbps; 麦克风与 Node 桥接保持单声道 128 kbps). | P1.2 |
| D27 | 宿主绑定与能力声明 | `MediaStudioPluginHost` 基于 `core/plugin/AidlPluginHost.kt`, 绑定 flags 同 `SpeechPluginHost` (含 API 34 `BIND_ALLOW_ACTIVITY_STARTS`, 以便插件在宿主后台时弹出 "所有文件访问" 授权页与 MediaStore 写入确认); 脚本能力声明: `video.record` 需 `screen_capture`, 带音频的录屏与 `audio.record` 需 `media.recording` + `RECORD_AUDIO`, 加工类方法需 `media`; 裸脚本能力声明与 MCP / Node 的声明保持一致. | P1.3 |
| D28 | 契约请求模型 | `IMediaStudioPlugin.submit(request, callback)` 返回任务 ID, 请求与结果用 `Bundle` (不走 JSON 字符串; 选项像 `JSON.stringify` 一样序列化, 函数被忽略), `IMediaStudioCallback.onProgress / onComplete / onError`; `cancel(taskId)`, `probe(request)` (`path`), `listTasks(request)` (`hostToken`), `getInfo()` 返回 `PluginInfo`, 能力表由 `getCapabilities()` 返回; 宿主回调 Binder 死亡即取消其全部任务并删除未完成的输出文件; 同一宿主 Binder token 最多 4 个并发任务 (超出 `TASK_LIMIT`), 全插件最多 2 个同时编码 (其余排队). 定义见附录 C (按 P1.1 实际 AIDL 定稿, 附录 D Q20). | P1.1 |
| D29 | 加工管线 | 单输入单输出的操作 (裁剪, 变速, 变换, 叠加, 转码, 去音轨, 抽音频) 走 `Transformer` + `EditedMediaItem` + `Effects`; 拼接与多轨走 `Composition` (多 `EditedMediaItemSequence`); 裁剪无需重编码时启用 `experimentalSetTrimOptimizationEnabled`; 默认视频编码 H.264 (`h265` 可选, 设备不支持时报 `UNSUPPORTED_FORMAT`), 默认码率按分辨率估算 (1080p 8 Mbps); 音频处理用 `media3-effect` 的 `AudioProcessor` (音量 / 变速 / 声道) 与 `Composition` 多序列混音; 超过 2 GB 或 60 分钟的单输入返回 `warnings[]` (`OUTPUT_LARGE` / `DURATION_LONG`) 但不拒绝, 无硬上限 (附录 D Q6 已决). | P2.1 |
| D30 | 转场 | 7 种: `cut`, `fade` (经黑场), `dissolve` (叠化), `slide` (推移, 方向参数), `wipe` (擦除, 方向参数), `zoom` (缩放), `blur` (模糊叠化). 除 `cut` / `fade` 外均为重叠区间双序列合成 (`VideoCompositorSettings` + 自写 GLSL 效果, 两段各延长重叠时长); 转场时长 100-3000 ms 默认 500 ms; 时间线 JSON 与界面共用同一转场表. | P3.2 |
| D31 | 字幕 | SRT 完整解析 / 导出 (UTF-8, 含 BOM 与 CRLF 容错); ASS 1.0.0 基础子集: `[V4+ Styles]` 的字体 / 字号 / 主色 / 描边色 / 粗斜 / 描边 / 阴影 / 对齐 / 边距, Dialogue 的时间 / 图层 / 换行, 内联标签只认 b, i, c, fs, an, pos (ASS 写法为反斜杠加标签名), 其余标签 (卡拉 OK, 动画, 绘图) 忽略并返回 `warnings[]`; 烧录用 `media3-effect` `TextOverlay` 逐帧渲染 (Android `StaticLayout`), 字体用系统字体或脚本指定的 TTF 路径. 1.1.0 大库装好后 ASS 自动改走 libass. | P3.3 |
| D32 | GIF | 自带纯 Kotlin 编码器: 全局或逐帧调色板 (中位切分, 最多 256 色), 可选 Floyd-Steinberg 抖动, LZW 编码, 循环与帧延时; 默认 10 fps, 宽度 480, 单次上限 600 帧 (超出报 `LIMIT_EXCEEDED`, 建议先裁剪或降帧率). 不引入第三方 GIF 库 (附录 D Q2 可改). | P2.4 |
| D33 | 时间线 JSON | `video.compose(timeline)` 的 `timeline` 为对象: `width` / `height` / `frameRate` / `background`, `video[]` (轨道数组, 每轨 `clips[]`: `src`, `start`, `end`, `at`, `speed`, `volume`, `transform`, `transition`), `audio[]` (轨道数组, `clips[]`: `src`, `start`, `end`, `at`, `volume`, `fadeIn`, `fadeOut`), `overlays[]` (文字 / 图片, 含时间段与位置), `subtitles` (`src` 或 `cues[]` 与 `style`); 轨道顺序即 z 顺序; 界面工程文件 `.slate.json` 就是同一 JSON 加界面状态 (缩放, 选中). 草案见附录 B.4. | P3.1 |
| D34 | 预览引擎 | 时间线预览优先用 media3 `CompositionPlayer` (实验 API) 直接播放 `Composition`; P4.3 开工先 spike: 在 API 28 / 33 / 35 真机上转场与叠加能实时预览则采用, 否则退到自写预览 (ExoPlayer 逐片段播放 + Compose 叠层绘制转场近似), 结论回填附录 D Q3. 已定 (2026-11-07, spike 结论): 退到自写预览 (`preview/TimelinePreview.kt`, 每车道一个 ExoPlayer + 共享时钟, 转场按导出公式在 Compose 近似), 数据见附录 D Q3. | P4.3 |
| D35 | 工具箱向导 | 每个脚本 API 一个表单页 (选文件, 填参数, 预览结果, 执行入队), 表单字段即 API 选项, 执行后结果页显示输出路径, 媒体信息, 生成的代码 (D11); 向导与脚本共用插件内同一执行器, 任务队列页显示两者的任务. | P4.2 |
| D36 | 代码生成与宿主入口 | 代码生成器以 API 选项对象为输入输出 JS 源码 (字符串转义, 路径用原值), 与 d.ts 同源维护; "在 AutoJs6 中打开" 调用 D19 的 `CREATE_SCRIPT`, 宿主未装或版本过低时只提供复制. | P4.4 |
| D37 | Studio 首页与存储 | 首页列出 `Movies/3-Slate Studio`, `Movies/AutoJs6`, `Music/3-Slate Studio`, `Music/AutoJs6` 与用户添加的目录 (MediaStore 查询, 不扫全盘); 文件操作 (重命名, 删除, 分享) 限于这些目录; 设置项: 默认输出目录, 默认编码 (H.264 / H.265), 默认码率策略, 硬件编码器偏好 (自动 / 优先硬件 / 优先软件), 任务并发数 (1 - 2), 启动器图标模式 (黑白图标规范第 1 节的四个选项); 编码器偏好与并发数对 Studio 任务与脚本任务都生效 (附录 D Q50 (b), P4.4 补上). | P4.1 |
| D38 | 插件中心与向导目录 | `ThreeSlateStudioInfoService` (action `org.autojs.plugin.INFO`, category `three-slate-studio`), 能力服务受 `org.autojs.permission.PLUGIN` 保护; 向导目录 `PluginInstallWizardCatalog` 放 `TOOLS` 分类 `official("three.slate.studio")`, 回退标题 `Three Slate Studio` (附录 D Q4 已决: 沿用 `TOOLS`). | P1.5 |
| D39 | 许可证与声明 | 源码 MPL-2.0 (`LICENSE` 完整文本); 1.0.0 依赖 Apache-2.0 为主 (media3, Compose, Kotlin), checker-qual 为 MIT (guava 带入的 `org.checkerframework:checker-qual`, 声明附 MIT 全文, 附录 D Q10), `THIRD_PARTY_NOTICES.md` 由脚本生成; 1.1.0 FFmpeg 大库按 LGPL-2.1-or-later 独立分发 (不打进 APK, 动态加载), 大库自身的 `NOTICES` 随大库包分发, 专利风险说明写进 README "FFmpeg 大库" 节. | P0.1 |
| D40 | 文档与声明同步 | 文档站新增 `video.md` 与 `audio.md` (10 种语言, 半角标点), 宿主录屏 / 录音也写在这两页 (不写进 `images.md`); d.ts 新增 `video.d.ts` / `audio.d.ts`; Ace 补全与 Offline Docs 同步; 四仓库均只落本地提交, 推送由维护者决定. | P5.1 |
| D41 | Studio 录音 | 插件自持录音内核 (维护者 2026-11-05 补充, 附录 D Q8): 麦克风用 `AudioRecord`; 内录 / 混合 (API 29+) 用 `AudioPlaybackCaptureConfiguration` + 插件自己的 MediaProjection 授权 + `mediaProjection` 类型前台服务; 内核照宿主 `core/record/AudioRecorder` (P1.2 产出) 复制, 在文件头注明同步来源 (宿主提交与文件路径, 与 P7.1 约定一致); 选项与默认值同脚本 `audio.record` (附录 B.2, 含 Q1 的内录 / 混合默认值); 默认输出 `Music/3-Slate Studio/` (D14); 1.2.0 P7.1 复用这套投影授权与前台服务. 实施 (2026-11-07, 提交 `a311f30`): 内核副本放在包 `record/` (附录 A 写作 `recording/`), 来源宿主 `3ef5cec633` (P1.2; 宿主 `063b9f0b8a` 时这些文件未再改) 的 `core/record/AudioRecorder.kt`, `RecordingOptions.kt` (只取音频部分), `Pcm16.kt`, `RecordingMuxer.kt`, `WavFileWriter.kt`, `RecordingForegroundLease.kt`, 文件头注明来源, 插件改动以 `Plugin:` 标出 (插件自己的 MediaProjection, 暂停 / 继续, 电平, API 26 以下 `renameTo`); 同步表与 P7 录屏副本的来源 (`RecordingVideoSizes.kt`, `95b3d8ab42` / `0d5e37d97d`, Q33 (a)) 在 `record/README.md`. | P4.5 |

---

## 3. 范围与非目标

范围 (1.0.0):

- 脚本 API `video` / `audio` 全集 (附录 B): 宿主实现的录屏与录音, 插件实现的全部加工, 同步 + Async 两形态, 进度与取消, 任务列表.
- 宿主契约 `media-studio-api`, 绑定与能力声明, 插件中心注册, 向导条目, `CREATE_SCRIPT` 入口, 官方索引登记.
- 插件加工核心: 裁剪, 拼接, 变速, 变换, 叠加, 抽帧, GIF, 抽音频, 去音轨, 转码, 音频加工, 探测.
- 多轨合成: 时间线 JSON, 7 种转场, SRT / ASS (基础子集) 字幕烧录.
- 独立界面: 首页与任务队列, 工具箱向导, 多轨时间线剪辑器 (预览, 转场, 字幕, 叠加), Studio 录音 (麦克风 + 内录, D41), 代码生成与宿主入口, 设置, 发行历史, 更新检查, Icon Studio 图标与四种启动器模式.
- 文档, d.ts, Ace, Offline Docs, README, changelog, 宿主样例脚本, 兼容矩阵, 1.0.0 发布.

非目标 (不做或排到 1.1.0 之后, 评估记录见附录 G):

- 1.0.0 不含任何原生库: FFmpeg, libass, MP3 编码, WebM / MKV 输出均为 1.1.0 (D4, D13, D24).
- 插件自持投影的录屏, 悬浮控制条, Shizuku 免确认均为 1.2.0 (D2, D13); 1.0.0 的 Studio 录音只把投影授权用于内录 / 混合音频 (D41).
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
  ui/          home, tasks, toolbox/<op>, timeline (editor, preview, tracks), record (P4.5), settings, releases, update, launcher icon
  bigrun/      (1.1.0) LibraryManager, FfmpegRunner, AssRenderer
  recording/   (1.0.0 P4.5, D41) AudioRecorder (宿主内核副本), ProjectionPermission, RecordingService; (1.2.0) ProjectionRecorder, FloatingControls, ShizukuRecorder
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
| `{REQUIRES_HOST_VERSION}` | 基线 6640 (宿主 master `340a0b511e`, 6.8.1 未发布, 附录 F); P0.1 先写临时值 6641 (基线后的第一个本地构建号), P1.5 改为含契约的本地构建号, P5.4 回填为含全部宿主条目的本地构建号 (D23) |
| 向导目录 | `PluginInstallWizardCatalog` `TOOLS` 分类, `official("three.slate.studio")`, 回退标题 `Three Slate Studio` (附录 D Q4 已决) |
| `plugin_description` (zh-Hans) | `视频与音频加工, 多轨时间线剪辑, 转场与字幕, 界面操作可生成脚本代码` (句尾无点号, 不含 "AutoJs6"; P0.1 定稿, 10 语言见 `values*/strings.xml`) |
| GitHub 仓库描述 | `Video and audio processing, multi-track timeline editing and screen recording companion for AutoJs6 scripts` |
| 发布文件名 | `autojs6-plugin-three-slate-studio-v{VERSION_NAME}-{CRC32}.apk` (1.0.0 universal 单包, D25) |
| 索引清单 | `release-manifests/io.github.supermonster003.autojs6.plugin.three.slate.studio/<versionCode>.json` |
| 图标 | `.python/icons/three-slate-ic-launcher-light.png` / `-dark.png` (D16), Icon Studio 合成 (P0.2) |
| 权限 | `org.autojs.permission.PLUGIN`, `MANAGE_EXTERNAL_STORAGE` (+ API 32 及以下的 `READ/WRITE_EXTERNAL_STORAGE`), `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (API 33+, 首页列表), `POST_NOTIFICATIONS` (任务进度通知), `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` (长任务前台服务), `RECORD_AUDIO` + `FOREGROUND_SERVICE_MICROPHONE` + `FOREGROUND_SERVICE_MEDIA_PROJECTION` (Studio 录音, D41; P0.1 预留权限, 前台服务组件到 P2.1 / P4.5 再加), `INTERNET` + `ACCESS_NETWORK_STATE` (更新检查, 1.1.0 大库下载); 1.2.0 再加悬浮窗相关权限 |
| 许可证 | 源码 MPL-2.0; 依赖 Apache-2.0; 1.1.0 FFmpeg 大库 LGPL-2.1-or-later 独立分发 (D39) |

---

## 6. 阶段总览

| 阶段 | 版本 | 主题 | 状态 |
| --- | --- | --- | --- |
| P0 | 1.0.0 | 仓库骨架: 构建链与身份, Icon Studio 图标, 测试与 CI | 已完成 (2026-11-05; 剩真机图标目视 E4 待维护者, Q9 - Q11 已按推荐先行待确认) |
| P1 | 宿主 6.8.1 (本地构建号, D23) | 契约 `media-studio-api`, 录屏 / 录音内核泛化, 脚本模块 `video` / `audio`, `CREATE_SCRIPT` 入口, 注册与协议文档 | P1.1-P1.7 已完成 (2026-11-06), 宿主与 Node / Bun 仓库本地提交未推送 |
| P2 | 1.0.0 | 插件加工核心: 执行器与任务模型, 视频基础加工, 叠加, 抽帧与 GIF, 音频加工, 设备矩阵 | 已完成 (2026-11-07; P2.1 - P2.5 于 2026-11-06, P2.6 设备矩阵 6 台于 2026-11-07; Q36 - Q38 返工于 2026-11-07) |
| P3 | 1.0.0 | 多轨合成: 时间线 JSON 与 `video.compose`, 转场, 字幕 | 已完成 (2026-11-07; P3.1 - P3.2 于 2026-11-06, P3.3 字幕于 2026-11-07 维护者目视通过; Q36 - Q38 返工于 2026-11-07) |
| P4 | 1.0.0 | Studio 界面: 应用骨架, 工具箱向导, 时间线剪辑器, 代码生成与宿主入口, Studio 录音 | 已完成 (2026-11-07; P4.1 - P4.2 于 2026-11-07; frames 的脚本一侧经宿主 Q53 修复后复测 20 / 20; P4.4 于 2026-11-07 (含 Q50 (b), Q51 (b)); P4.3 于 2026-11-07 (Q3 (b) 自写预览); P4.5 于 2026-11-07 (Sony API 28 / 31 / 33 与 OnePlus ColorOS 真机通过)) |
| P5 | 1.0.0 | 文档, 声明, 四仓库同步, 样例, 兼容矩阵, 发布与索引登记 | 进行中 (P5.1 与 P5.3 于 2026-11-08) |
| P6 | 1.1.0 | FFmpeg + libass 大库: 自建或选型, Shared-Assets 类别, 下载 / 校验 / 加载, 命令透传, 新格式, 完整 ASS, 发布 | 未开始 |
| P7 | 1.2.0 | 录屏增强: 插件自持投影, 悬浮控制条与 Studio 录屏入口, Shizuku 免确认, 发布 | 未开始 |

依赖顺序:

```text
P0 (骨架)                                  P1 (宿主) 与 P0 并行: 不同仓库, P1.1 契约 AAR 产出后 P0.1 把它放进 libs/
 -> P2 (加工核心)                          依赖 P0.1, P0.3 与 P1.1 的契约常量; 真机脚本测试依赖 P1.3
     -> P3 (多轨合成)                      依赖 P2.1-P2.3 (管线, 叠加) 与 P2.5 (混音)
         -> P4 (界面)                      P4.1 / P4.2 依赖 P2; P4.3 依赖 P3; P4.4 依赖 P1.4 与 P4.2 / P4.3; P4.5 依赖 P4.1 与 P1.2
             -> P5 (文档, 发布)            依赖 P1-P4 全部通过
                 -> P6 (1.1.0) -> P7 (1.2.0)   P6 与 P7 互不依赖, 维护者按 D13 顺序安排
```

可并行的批次建议 (调度会话按 D17 发卡片):

- 批次 1: P0.1 + P0.3 (插件骨架与 CI, 一个子会话); P1.1 + P1.2 (宿主契约与录制内核泛化, 一个子会话); P0.2 (图标, 可与 P0.1 合并).
- 批次 2: P1.3 + P1.4 + P1.5 (宿主模块与入口); P2.1 + P2.2 (插件执行器与视频基础加工).
- 批次 3: P2.3 + P2.4 (叠加, 抽帧与 GIF); P2.5 (音频加工); 两者可并行.
- 批次 4: P3.1 + P3.2 (时间线与转场); P3.3 (字幕) 可并行; P2.6 设备矩阵在 P2 全部完成后.
- 批次 5: P4.1 + P4.2 (应用骨架与向导); 之后 P4.3 (时间线剪辑器), P4.4 (代码生成), P4.5 (Studio 录音) 并行.
- 批次 6: P5.1 + P5.2 (文档与声明) 与 P5.3 (README, 样例, 兼容矩阵) 并行; P5.4 发布.

发布门禁 (每个阶段的 "(发布)" 条目都指向本清单):

1. 插件 `./gradlew test connectedDebugAndroidTest` 通过 (API 35 AVD), 宿主涉及模块的单元测试通过.
2. 真机脚本回归: 附录 F 设备矩阵中的 Sony API 28 / 31 / 33, Xiaomi Pad API 35 四台通过, OnePlus API 35 (ColorOS) 至少跑一次; Studio 录音在两台真机 (含 API 29+ 内录) 通过 (D41, P4.5); Samsung 16 KB 设备在引入原生库的版本 (1.1.0 起) 必跑.
3. 10 种语言资源与文档只用半角标点 (`COMMON_AGENTS.md` 规则), `prepush_check.py` 通过, `THIRD_PARTY_NOTICES.md` 与依赖一致.
4. 发布 APK 为 release 签名, 用宿主插件中心安装并跑 `samples/` 冒烟脚本; versionCode, 文件名 CRC32 与索引清单一致.
5. changelog 10 种语言同步: 子会话以简体中文为源一次写全 10 种语言并完成翻译 (工作区规则 `.claude/rules/all-languages.md`, 2026-11-05 起), 维护者审阅时可改任何语言.

---

## P0: 仓库骨架

状态: 已完成 (2026-11-05, 子会话 3-slate-p0); 剩 P0.2 验收的真机图标目视 (E4, 待维护者). 设备: 无 (构建与单元测试); P0.3 的仪器测试用 API 35 AVD.

### P0.1 构建链与身份 (D25, D39)

- [x] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 建立 Gradle 工程 (`settings.gradle.kts`, `build-logic`, `gradle/libs.versions.toml`, `version.properties`, `app/`), 填入 5.5 节身份; `libs/common-plugin-api.aar` 与 `libs/media-studio-api.aar` (P1.1 产出, 未产出前用 P1.1 的常量草案本地构建 AAR 顶替并在 `locks/host-api-aars.lock` 标注临时) 入库. 证据 (E1, 2026-11-05): 提交 `15033b3`; 平台插件 1.9.1, AGP 9.3.3, Kotlin 2.3.20, Gradle 9.5.0, Temurin 21.0.12.1+1; `libs/` 直接用 P1.1 的正式 AAR (`media-studio-api` 1.0.0, 宿主本地提交 `ddc91edf4f`, 没有用临时 AAR) 与 `common-plugin-api` 1.1.1 (宿主 `1676e7489d`), `HostApiAarLockTest` 5 例核对 SHA-256.
- [x] (插件) 依赖线: Compose BOM 2026.09.00, navigation 2.10.2, media3 1.10.1 全套 (`transformer`, `effect`, `muxer`, `exoplayer`, `ui`, `common`), 无原生库; `./gradlew assembleDebug` 产出 universal APK; 检查依赖 AAR 内无 `.so` (有则记录并评估 16 KB). 证据 (E1 + E5, 2026-11-05): 提交 `15033b3`; `locks/runtime-dependencies.lock` 锁定 88 个运行时模块, `verifyRuntimeDependencyLock` 接入 `check` 与 `preReleaseBuild`; 依赖里唯一的原生库是 Compose UI graphics 带入的 `androidx.graphics:graphics-path` 1.0.1 (`libandroidx.graphics.path.so`, 4 个 ABI), 与 D25 "无原生库" 不符, 登记附录 D Q9 (已按推荐保留, 构建与 CI 校验 4 个文件均 STORED 且 16 KB 对齐); universal release APK 1,143,250 B.
- [x] (插件) `ThreeSlateStudioApplication`, `ThreeSlateStudioInfoService`, `ThreeSlateStudioService` 空实现 (返回 `getInfo` 与 `NOT_IMPLEMENTED`), `ThreeSlateStudioContract` (ID, engine, 契约版本), Manifest (权限, 服务, `MANAGE_EXTERNAL_STORAGE` 授权页; 为 D41 预留 `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION` 权限, 前台服务组件到 P2.1 / P4.5 再加), 10 种语言 `strings.xml` (`app_name` 不可翻译, `plugin_description` 定稿). 证据 (E1 + E2, 2026-11-05): 提交 `15033b3`; `ManifestContractTest` 9 例, `PluginRuntimeInfoTest` 5 例, `StringResourceParityTest` 6 例, `ApplicationTextPunctuationTest` 2 例; 仪器测试 `ThreeSlateStudioContractTest` 7 例在 AVD_API_35_16K 通过 (WAKE 激活契约, 身份服务 `getInfo`, 契约服务 `getInfo` / `getCapabilities`, `submit` 对全部 `MediaStudioOps` 抛 `INVALID_OPTIONS: NOT_IMPLEMENTED: <op>`, 未知操作与空回调抛 `IllegalArgumentException`, `probe` / `cancel` / `listTasks` 骨架应答, 重复绑定身份一致); 契约服务按 P1.1 实际 AIDL (看板 Q20) 实现.
- [x] (插件) `LICENSE` (MPL-2.0), `README.md` 骨架 (含 "FFmpeg 大库" 节的占位与专利说明), `THIRD_PARTY_NOTICES.md` 生成脚本, `.gitattributes` (已入库), `.editorconfig`, `prepush_check.py` 接入. 证据 (E1, 2026-11-05): 提交 `15033b3`; `generate_third_party_notices.py --check` 输出 `NOTICES_OK host=2 maven=88`, `ThirdPartyNoticesTest` 3 例; guava 带入的 `org.checkerframework:checker-qual` 3.43.0 为 MIT, 与 D39 "依赖均为 Apache-2.0" 不符, 登记附录 D Q10 (已按推荐在声明里附 MIT 全文); `generate_markdown.py --check` 与 `check_repository.py` 通过, `prepush_check.py` 6 / 6 通过.
- 验收: `./gradlew assembleDebug test` 通过; APK 安装到 API 35 AVD 后插件中心能识别 `three-slate-studio` (身份服务) 并显示未启用; 提交前 `VERSION_BUILD` = 提交数 + 1.
- 结果 (2026-11-05): 通过. `assembleDebug testDebugUnitTest` 通过 (单元测试 51 例); AVD_API_35_16K (API 35, x86_64, 16 KB 页, 只读启动) 装宿主 debug 6.8.1 (versionCode 6640) 后, 插件中心列出 "3-Slate Studio 1.0.0 (2)", 开关为关闭; 骨架提交 `VERSION_BUILD` = 2 = 提交数 + 1.

### P0.2 图标与启动器模式 (D16)

- [x] (插件) 用 Icon Studio (`D:/webstorm-projects/AutoJs6-Icon-Studio`, 内核 1.4.0) 从 `.python/icons/three-slate-ic-launcher-light.png` / `-dark.png` 生成全套资源 (`ic_plugin_center`, `ic_launcher`, `ic_launcher_system*`, monochrome, 四个 launcher alias, `.icons/recipe.json`, `.python/icon_studio_runtime.py` / `generate_icon_studio.py`), 视觉尺寸 V = 0.52, 安全圆断言通过. 证据 (E1, 2026-11-05): 提交 `15033b3`; `IconResourcesTest` 4 例 (14 个 PNG 均为 432x432 且四角透明; `ic_plugin_center` 亮 / 暗 V = 0.52 ± 0.006, 半径 <= 216, 灰阶且亮暗 alpha 相同; 前景与 monochrome 层在 132 px 安全圆内; alias 与 adaptive XML 指向), `generate_icon_studio.py --check` 通过.
- [x] (插件) 设置页的启动器图标四选项 (自适应自动 / 固定亮 / 固定暗 / 透明) 与规范提示文案 (可先在 P4.1 的设置页落地, 本条只要求资源与组件 alias 就绪). 证据 (E1 + E2, 2026-11-05): 提交 `15033b3`; 四个 alias (`AdaptiveAutoIconAlias` 默认启用, `AdaptiveLightIconAlias` / `AdaptiveDarkIconAlias` / `TransparentIconAlias` 默认停用) 与资源就绪, 仪器测试 `launcherResolvesTheAutomaticIconAliasOnly` 通过; 设置页四选项与提示文案随 P4.1 设置页落地.
- 验收: 工程测试中的图标尺寸 / 透明度 / 安全圆断言通过; API 35 AVD 与一台真机的启动器与插件中心亮暗主题下图标可辨.
- 结果 (2026-11-05): 断言通过; AVD_API_35_16K 亮 / 暗主题下启动器与插件中心的图标都可辨 (插件中心对未启用插件统一半去饱和, 宿主 `colorFilterWithDesaturateOrNull(isOn, 0.5F)`, 属预期); 启动器图标在亮暗两种主题下与 3-Macaw Speech 表现一致. 真机目视 (E4) 待维护者: 装 debug 包后看启动器与插件中心的亮暗图标.

### P0.3 测试与 CI

- [x] (插件) 单元测试骨架 (选项校验, 输出路径, 代码生成器), 仪器测试骨架 (服务绑定, `getInfo`), 测试夹具生成脚本 `.python/generate_media_fixtures.py` (用 MediaCodec 在测试运行时生成 3 秒彩条 MP4 与正弦波 M4A / WAV, 不入库二进制). 证据 (E1 + E2, 2026-11-05): 提交 `15033b3`; 单元测试 `RequestValidationTest` 7 例, `OutputPathsTest` 6 例, `CodeGeneratorTest` 4 例 (lint 指出 `OutputPaths` 用的 `java.time` 在 API 24 / 25 不可用, 已改 `SimpleDateFormat`); 仪器测试 8 例在 AVD_API_35_16K 通过 (`ThreeSlateStudioContractTest` 7, `MediaFixturesTest` 1: MediaCodec 生成 320x240 30 fps 3 秒 8 色彩条 H.264 MP4, 440 Hz AAC M4A 与 PCM16 WAV, 首帧每条彩条颜色误差 <= 48); `generate_media_fixtures.py --serial emulator-5610` 输出 `FIXTURES_OK` (MP4 3.000 s, M4A 2.995 s, WAV 3.000 s), 拉取到被忽略的 `app/build/media-fixtures/` 并删除设备副本.
- [x] (插件) GitHub Actions: `build.yml` (assemble + test), `android-test.yml` (API 35 AVD 仪器测试, 与 CI 同款 AVD 配方), `release.yml` (tag 触发, 产出 `autojs6-plugin-three-slate-studio-v{VERSION_NAME}-{CRC32}.apk` 与索引清单草稿). 证据 (E1, 2026-11-05): 提交 `15033b3`; 5 个工作流: `build.yml` (仓库契约, 文档 / 声明检查, 依赖锁, 单元测试, debug / androidTest / release 打包, lint, release 包原生库检查), `android-test.yml` (API 35 x86_64 google_apis pixel_7, `reactivecircus/android-emulator-runner` v2.38.0), `release.yml` (tag `v*`: 核对 tag 与 `VERSION_NAME`, 签名, `appendDigestToReleasedFiles`, apksigner, `SHA256SUMS`, `.python/release_manifest.py` 写索引清单草稿, 不自动建 GitHub Release, 见 Q11), `markdown.yml`, `icon-studio.yml`; `release_manifest.py` 用本地 release 包试跑 `MANIFEST_OK` (字段与顺序同官方索引 Macaw `81.json`, `supportedAbis` 4 个 ABI), tag 不符时报 `MANIFEST_ERROR`.
- 验收: 本地 `./gradlew test connectedDebugAndroidTest` 通过; 工作流文件经 `prepush_check.py` 检查 (远端运行待首次推送).
- 结果 (2026-11-05): 通过. `testDebugUnitTest` 51 例与 `connectedDebugAndroidTest` 8 例 (AVD_API_35_16K, `ANDROID_SERIAL` 限定单台) 通过; 本地 AVD 是 16 KB 页变体, CI 用 4 KB 页的 google_apis 镜像; `prepush_check.py` 6 / 6 通过; 远端工作流待首次推送.

---

## P1: 宿主契约, 录制内核与脚本模块

状态: 已完成 (P1.1 / P1.2 2026-11-05, P1.3 - P1.7 2026-11-06; P1.6 按附录 D Q22 B / Q30 A). 全部为宿主与 Node / Bun Runtime 仓库本地提交 (不推送). 设备: Sony XQ-AT72 API 31, XQ-DQ72 API 33 (录屏与内录), G8441 API 28 (无内录), Xiaomi Pad API 35.

### P1.1 契约模块 `plugin-api/media-studio-api` (D20, D28)

- [x] (宿主) 新建 `plugin-api/media-studio-api` (照 `speech-api`): `MediaStudioPluginActions.MEDIA_STUDIO`, `MediaStudioPluginIds` (官方 ID / 包名 / `ENGINE = "media-studio"` / `CATEGORY`), `MediaStudioContract.CONTRACT_VERSION = 1`, `MediaStudioCapabilityKeys`, `MediaStudioLimits` (并发, 文件大小警告阈值, GIF 帧数), `MediaStudioRequestKeys` / `ResultKeys` / `ProgressKeys` / `ErrorCodes`, `MediaStudioOps` (操作名常量表, 与附录 B 一一对应); AIDL `IMediaStudioPlugin` (`getInfo` 居首), `IMediaStudioCallback`; 定义见附录 C. 证据 (E1, 2026-11-05, 3-slate-p1-1-2): 宿主本地提交 `ddc91edf4f` (未推送); 包名 `org.autojs.plugin.mediastudio.api`, 契约版本 1; AIDL 与草案的差异 (`getInfo` 返回 `PluginInfo`, `getCapabilities` 居第 2, `probe` / `listTasks` 收 `Bundle`) 按附录 D Q20 保持, 附录 C 已按实际定义改写; `MediaStudioOps` 24 个处理操作 + `ffmpeg` / `record*`, `SCRIPT_METHODS` 把附录 B 的 25 个脚本方法一一映射到操作.
- [x] (宿主) `maven-central.gradle.kts` 登记, AAR 产出到插件 `libs/`, `locks/host-api-aars.lock` 记录版本与 SHA-256. 证据 (E1, 2026-11-05): 同上提交; Maven 坐标预定 `io.github.supermonster003:autojs6-media-studio-api:1.0.0` (未发布); AAR 由 P0.1 入插件 `libs/`, `HostApiAarLockTest` 核对 SHA-256.
- 验收: 宿主 `:plugin-api:media-studio-api:assemble` 通过; AIDL 编译进宿主 app; 常量表与附录 B 的方法名逐项对得上. 结果 (2026-11-05): 通过; 宿主单测 `AidlOrderTest` 1 项 + `ContractTest` 9 项通过, AIDL 已编进宿主 APK; 插件侧经 Binder 的契约仪器测试见 P2.1.

### P1.2 录屏 / 录音内核泛化 (D26)

- [x] (宿主) 把 `engine/NodeScreenRecording.kt`, `ScreenRecordingAudio.kt`, `ScreenRecordingFrames.kt`, `ScreenRecordingMuxer` 与 Node 纯音频录制抽到 `core/record/` (`ScreenRecorder`, `AudioRecorder`, `RecordingMuxer`, `RecordingOptions`), Node 桥接改为调用新内核, 行为不变 (对照 `docs/dev/runtime-node-recording-2026-10-17.md` 的用例复跑). 证据 (E1 + E3, 2026-11-05, 3-slate-p1-1-2): 宿主本地提交 `3ef5cec633` (内核 8 个类, 包 `org.autojs.autojs.core.record`; Node / Bun 桥接经 `NodeRecordingJson` 转换, 选项, 消息, 默认值与结果字段不变; Node 纯音频录音的目标文件改为 stop 时移入, 见看板 Q21 已决) 与 `a92372b65c` (设备脚本 `PROJECT_MEDIA` 检查 + 证据 `docs/dev/record-kernel-2026-11-05.md`), 均未推送.
- [x] (宿主) 新增 Rhino 侧的 `maxDuration` (到时自动 stop 并触发 `onStop`), `onStop` 回调, `capture()` 取当前帧; 录音内核支持 WAV (PCM16) 输出与 M4A; 内录声道数按附录 D Q1. 证据 (E1 + E3, 2026-11-05): 同上提交; 内录 / 混合默认 2 声道 48 kHz, MP4 / M4A / WAV 在两台真机上都是 2 声道 48000 Hz; `maxDuration` 1500 ms 时 `onStop` 原因 `max-duration`, 录制中 `capture()` 取得含夹具颜色的位图.
- 验收: Node 桥接的录屏 / 录音用例在 API 31 / 33 真机复跑通过; 新内核单元测试 (选项校验, 偶数宽高, 输出已存在) 通过. 结果 (2026-11-05): 通过; XQ-AT72 (API 31) 与 XQ-DQ72 (API 33) 各 8 / 8 PASS (Node 录制 7 项 + 内核 1 项, XQ-DQ72 未遇 ART JIT ANR); 宿主单测 `RecordingOptionsTest` 10, `Pcm16Test` 5, `WavFileWriterTest` 3, `NodeRecordingJsonTest` 6 通过.

### P1.3 脚本模块 `video` / `audio` (D6, D8, D27)

- [x] (宿主) `runtime/api/VideoModule`, `AudioModule` 与 JS 包装 (`modules/__video__.js` / `__audio__.js` 或同等位置): 录制方法自实现; 加工方法校验选项后经 `MediaStudioPluginHost` 转调插件; 同步方法禁止 UI 线程 (`INVALID_OPTIONS`), `xxxAsync` 返回 Promise; `onProgress` 回调经回调 Binder 投递到脚本线程; 停止脚本取消全部任务; `tasks()` / `cancelAll()`; 默认输出路径 (D14) 由宿主填充; 插件缺失 / 禁用 / 不兼容按 `PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` / `PLUGIN_INCOMPATIBLE` 抛 `MediaStudioError` (`code`, `message`, `taskId`). 证据 (E1 + E3, 2026-11-06, 3-slate-p1-3-5): 宿主本地提交 `3abe772fa0` (未推送); 包 `runtime/api/modules/mediastudio` (`Video`, `Audio`, `MediaStudioCalls`, `MediaRecordings`, `VideoSubtitles`, `MediaStudioJsErrors`), `$video` / `$audio` 与 `video` / `audio` 为同一对象, 仅 Rhino (Node / Bun 见 P1.6); 录制走 P1.2 的 `core/record` 内核; 必需参数之后按类型映射 (字符串 = 输出路径, null / undefined = 默认输出, 对象 = 选项, 函数 = `onProgress`, 附录 D Q27); 宿主只对插件已冻结的操作预检选项 (`MediaStudioOptionRules.CHECKED_OPS`, 附录 D Q24); `video.frame` 经隐藏临时图片传帧 (附录 D Q25); 错误映射 `MediaStudioErrorMapper` (无插件时 `tasks()` 抛 `PLUGIN_UNAVAILABLE`, `cancelAll()` 返回 0).
- [x] (宿主) 能力声明: `screen_capture`, `media.recording` + `RECORD_AUDIO`, `media`; 裸脚本 / MCP / Node 的能力声明一致; 绑定 flags 与 `allowActivityStarts` (D27). 证据 (E1, 2026-11-06): 同上提交; `video.record` = `screen_capture` (录声音时另加 `media`, `media.recording`), `audio.record` = `media` + `media.recording`, 加工方法与 `tasks` / `cancelAll` = `media`; api-coverage 登记 (Node / Bun 由 P1.6 改为 portable).
- [x] (宿主) 样例脚本 `samples/` 草稿: 录屏 10 秒并转 GIF, 录音并转 FLAC, 裁剪 + 水印 + 拼接, 时间线合成 (P5.3 定稿). 证据 (E1, 2026-11-06): 同上提交, 4 个示例.
- 验收: 宿主 debug 构建装到 API 31 / 33 真机, 用插件 P2.1 的 `probe` 与 `trim` 走通脚本 -> 宿主 -> 插件 -> 回调全链路; 插件未安装时错误码正确; UI 线程调用同步方法被拒. 结果 (2026-11-06, 3-slate-p1-3-5, 宿主 6641 debug): 通过 (XQ-DQ72 当时未连接, 由 Xiaomi Pad API 35 代替). 968e9f18 (插件 1.0.0 (4)) 设备脚本 `tools/mediastudio/device_mediastudio_check.py` api + 插件中心 + create 23 / 0: `probe` 640x360 h264 / aac, `trim` 3008 ms, `trimAsync` 2005 ms, `tasks()` 1 项 completed, `cancelAll()` 0; XQ-AT72 QV710AF65F (API 31, 无插件) api 12 / 0: `probe` / `trim` / `probeAsync` / `version` / `tasks` 为 `PLUGIN_UNAVAILABLE` 的 `MediaStudioError`, UI 线程同步调用 `INVALID_OPTIONS`; 宿主单测 (mediastudio 4 个类, ScriptModule Arity / Installs / Order) 通过.

### P1.4 `CREATE_SCRIPT` 入口 (D19)

- [x] (宿主) `CreateScriptActivity` (action `org.autojs.autojs.action.CREATE_SCRIPT`, 受 `org.autojs.permission.PLUGIN` 保护, extras `code`, `suggestedName`, `source`), 打开编辑器并新建未保存文件 (文件名建议 = `suggestedName`, 冲突时加序号); 代码超过 512 KB 时拒绝并提示. 证据 (E1 + E3, 2026-11-06, 3-slate-p1-3-5): 宿主本地提交 `5fc200c3ab` (未推送); 常量在 common-plugin-api 的 `AutoJs6CreateScriptContract`; 上限两条: 512 KiB UTF-8 (`MAX_CODE_BYTES`) 与 200000 字符 (`MAX_CODE_CHARS`), 超限返回 `CODE_TOO_LARGE` 并提示 (968e9f18 实测启动 parcel 503988 B 送达, 523988 B 起 `TransactionTooLargeException` 且系统直接杀掉宿主进程; 大代码改经 content URI 见 P1.7, 附录 D Q28); 打开为未保存的新文件, 重名加 " (1)", " (2)" ...; 每个被接受的请求各开一个新的编辑器任务 (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK`, 真机发现只用 NEW_TASK 时第二份代码被静默丢弃, 已修).
- 验收: 用 `am start` 与插件 P4.4 各验一次; 无 PLUGIN 权限的应用调用被拒. 结果 (2026-11-06): `am start` 部分通过 (968e9f18 create 用例与 MULTIPLE_TASK 构建复测 9 / 0, XQ-AT72 create 7 / 0, 含无权限调用被拒与超限拒绝); 宿主单测 `CreateScriptRequestTest` 通过; 插件 P4.4 一侧 (2026-11-07): 插件 "在 AutoJs6 中打开" 内联发送通过 (XQ-AT72, 见 P4.4 结果).

### P1.5 注册, 协议文档, changelog 与版本回填 (D38)

- [x] (宿主) 插件中心登记 `three-slate-studio` (官方 ID, 包名, 最低插件版本), `PluginInstallWizardCatalog` `TOOLS` 条目, 协议文档 `docs/dev/media-studio-plugin-protocol.md` (契约, 能力声明, 错误码, 录制内核泛化说明), `.changelog/lang_zh-Hans.json` 条目 (通俗文案). 证据 (E1 + E3, 2026-11-06, 3-slate-p1-3-5): 宿主本地提交 `9fc8dd2012` (未推送); 向导分类 `TOOLS` (附录 D Q4); 协议文档含绑定, AIDL, 能力表, 脚本 API 与操作, 默认输出, 录制, CREATE_SCRIPT, 上限, 错误码与设备结果; 设备脚本 `tools/mediastudio/device_mediastudio_check.py`; changelog 6.8.1 10 种语言 4 条 (`check_translations` CHANGELOG_TRANSLATIONS_OK, `generate_markdown --check` MARKDOWN_OK).
- [x] (宿主) 插件 `REQUIRES_HOST_VERSION` 临时值 = 含 P1.1-P1.4 的本地构建号, P5.4 回填. 证据 (E1, 2026-11-06): 临时值 6641 (与宿主契约常量 `MediaStudioPluginIds.REQUIRED_HOST_VERSION_CODE` 一致, P1.3 - P1.5 的验收构建即 6641 debug); 插件 `app/build.gradle.kts`, `ThreeSlateStudioContract.REQUIRED_HOST_VERSION` 与 `.readme/common.json` 本来就是 6641, 批次 3 插件卡片 3-slate-p2-3-5 只更新注释; P5.4 回填含全部宿主条目的公开构建号 (D23).
- 验收: 插件中心能发现, 安装, 启用插件并显示描述与图标; 协议文档与 AIDL 一致. 结果 (2026-11-06): 通过; 968e9f18 插件中心显示 "3-Slate Studio 1.0.0 (4)" 已启用 (含描述与图标); 插件中心登记测试与向导目录 53 项单测通过.

### P1.6 Node / Bun 桥接 `video` / `audio` (附录 D Q22 B)

- [x] (宿主) 按插件引擎桥接惯例把 `video` / `audio` 两个模块暴露给 Node 与 Bun, API 与 Rhino 一致 (录制, 加工方法与 `xxxAsync`, `onProgress`, `tasks()` / `cancelAll()`, `MediaStudioError`), 能力声明与 Rhino 一致; api-coverage 策略由 `media-studio-rhino-only` (not-portable) 改为 portable. 需改 Node / Bun Runtime 插件仓库时只本地提交并登记待决.
- 验收: Node 与 Bun 脚本在真机上跑 `record` / `probe` / `trim` 与 `onProgress` / Promise 形态, 结果与 Rhino 一致; 宿主相关单测通过.
- 证据 (E1 + E3 + E4, 2026-11-06, 3-slate-p1-6-7): 宿主本地提交 `734c5a785f` (Node 模块 `media_studio` = `RuntimeMediaStudioBridge`, 参数映射 `MediaStudioScriptArgs` 同 Rhino, Bun 新授权 `mediastudio.control` 默认关闭 + 设置开关 11 语言), `3c26d999e9` (协议文档与 changelog 10 语言), `0aecdd6c01` (api-coverage: runtime-policy 的 `video` / `audio` 改 `mediastudio.control`, 60 个 pending 格改 covered, MATRIX.md), `f86bcb1da2` (设备脚本 `tools/mediastudio/device_mediastudio_check.py` 增 `--runtimes` / `--record` / `--record-size`); 门面 (附录 D Q30 A, 同一卡片续做): Node Runtime `0e81048e` (`autojs6:video` / `autojs6:audio`, 主线程 + Worker, d.ts, HOST-API 文档, changelog 10 语言), Bun Runtime `f0d85db` (`host.video` / `host.audio`, 录制按附录 D Q32 A 复用 screen.record 授权, 文档, changelog 10 语言); 均未推送. 隔离克隆 JVM 570 例 0 失败; Node 仓库 Node 测试与 Gradle 单测通过, Bun 仓库 136 例中 135 通过 (唯一失败为既有计时抖动, 与本条无关).
- 结果 (2026-11-06): 通过. Redmi 22120RN86C (`bek749scrwv4wo8h`, Android 13, 临时宿主 6641 + Node 381 + Bun 353), `device_mediastudio_check.py --runtimes node,bun`: 不装插件 32 PASS / 0 FAIL (插件方法以 `PLUGIN_UNAVAILABLE` 拒绝且为 `MediaStudioError`); 装插件 debug 6 后 39 PASS / 0 FAIL (probe 8023 ms 640x360 h264 / aac, trim 的 Promise 与 2 次 onProgress, trimAsync, audio.probe, tasks, cancelAll); 录屏 `--record --record-size 720x1648` (维护者确认投影对话框) 30 PASS / 0 FAIL, Node 5246 ms 299340 B, Bun 5337 ms 299646 B, onStop 各 1 次. 默认尺寸录屏在该机失败 (720x1650 不被编码器接受), 登记附录 D Q33, 由宿主卡片 3-slate-q33 修复. 测后装回原宿主 6640 / Node 291 / Bun 349 (md5 一致).

### P1.7 `CREATE_SCRIPT` 大代码经 content URI (附录 D Q28 C)

- [x] (宿主) 契约 `AutoJs6CreateScriptContract` 增 content URI 字段 (调用方经 FileProvider 授予读权限); 内联 `code` 保留 512 KiB / 200000 字符上限; 宿主读流并设上限, 无授权, 读取失败或超上限时拒绝并提示 (多语言提示); 协议文档同步. 证据 (E1 + E2, 2026-11-06, 3-slate-p1-6-7): 宿主本地提交 `271949817a` (`codeUri` 只收 `content://`, 拒绝宿主自身 provider (含 `userId@` 前缀), 读取上限 4 MiB (附录 D Q31) / 10 s, 复制到私有缓存后按文件开新编辑器, 超过 512 KiB 走大文件模式; 4 个错误码提示 11 语言) 与 `3c26d999e9` (协议文档 "URI mode", changelog 10 语言); `CreateScriptRequestTest` 11 例 + `CreateScriptUriRequestTest` 7 例通过.
- 验收: 单元测试 (URI 与内联互斥, 上限, 编码); 仪器测试或临时测试应用发送 content URI 打开编辑器; 无授权或超上限时被拒. 结果 (2026-11-06): 通过. 小米平板 968e9f18 (临时 6641 debug 宿主) 由宿主脚本自发 `CREATE_SCRIPT`, 设备脚本 `tools/mediastudio/device_mediastudio_check.py` create-uri 18 / 0 (含内联回归 4 例): 小文件 (含中文) 与 600 KiB 文件各开新编辑器, 副本 MD5 一致; 5 MiB 为 `CODE_TOO_LARGE`; 未授权 URI 与宿主自身 provider 为 `CODE_URI_DENIED`; 不存在的 id 为 `CODE_URI_UNREADABLE`; `file://` 与 `code` + `codeUri` 同给为 `INVALID_REQUEST`; 拒绝不留副本. 未覆盖: 真实发送方的 FileProvider 授权路径, 留插件 P4.4 (配方见共享笔记 `3-slate-p1-6-7-handoff.md`). 插件 P4.4 (2026-11-07): 真实发送方 (插件 FileProvider + 读授权) 发送 481684 字符的脚本, 宿主接受且私有副本 MD5 一致 (XQ-AT72, 见 P4.4 结果).

---

## P2: 插件加工核心

状态: 已完成 (P2.1 - P2.5 于 2026-11-06, P2.6 于 2026-11-07; 附录 D Q37 / Q38 返工于 2026-11-07, 见 P2.6). 宿主选项预检已同步到全部 18 个已实现操作 (附录 D Q24, 宿主本地提交 `764b63b765`). 设备: API 35 AVD (仪器测试), Sony XQ-DQ72 API 33 与 Xiaomi Pad API 35 (硬件编码器), G8441 API 28 (低端机性能基线).

### P2.1 执行器, 任务模型与探测 (D28, D29, D14)

- [x] (插件) `ThreeSlateStudioService` 实现 `submit` / `cancel` / `probe` / `listTasks`; `TaskRunner` (队列, 并发 2 编码, 每宿主 token 4 任务, 宿主 Binder 死亡即取消并删除未完成输出); 长任务 (> 10 秒) 启动 `dataSync` 前台服务与进度通知; `OutputPaths` (默认目录, 命名, 冲突加序号), `MediaStoreRegistrar`; `MANAGE_EXTERNAL_STORAGE` 检查与引导 Activity; `probe` 返回时长 / 宽高 / 旋转 / 帧率 / 码率 / 编码 / 音轨 / 大小. 证据 (E1 + E2 + E3, 2026-11-06, 3-slate-p2-1-2): 提交 `a0a90f2`; `service/MediaStudioEngine` (契约 Bundle, 回调 Binder 死亡按 hostToken 取消), `core/TaskRunner` (先进先出, 同时 2 个任务, 每 token 最多 4 个未完成任务, 另保留最近 16 个已结束任务供 `listTasks`), `service/ProcessingForegroundService` (`dataSync`, 运行满 10 秒启动, 队列空后自行停止; Android 12+ 后台不许启动时只记日志, 任务照常进行), `core/OutputPaths` (D14 目录与 `<主名>-<op>-<时间戳>`, 冲突加 `-1` ...; 先写同目录 `.part-<taskId>` 临时文件), `core/MediaStoreRegistrar` (插件私有目录不登记), `ui/storage/StorageAccessActivity`, `core/MediaProbe` (FLAC 容器的平台提取器报 `audio/raw`, 按 `fLaC` 文件头改报 `flac`); 能力表新增 `mediaStudioStoragePermission` 与 `mediaStudioWarnings` (无 H.265 编码器, 共享存储不足 1 GiB, 系统限制插件后台时各一条; 后台受限见附录 D Q13).
- [x] (插件) 选项校验层 (与宿主同一规则二次校验), 错误码表 (附录 C.3), `warnings[]` 机制. 证据 (E1 + E2, 2026-11-06): 提交 `a0a90f2`; `core/RequestValidation` (外层字段) 与 `core/RequestPlanner` + `core/OperationOptions` (逐操作选项), 检查顺序与逐字消息写在共享笔记 `3-slate-p2-notes.md` 供宿主 P1.3 照抄; Binder 上 `INVALID_OPTIONS` / `UNSUPPORTED_FORMAT` / `LIMIT_EXCEEDED` 抛 `IllegalArgumentException`, 其余抛 `IllegalStateException`, 消息为 `CODE: detail`; `warnings[]` 先放裸码 `OUTPUT_LARGE` / `DURATION_LONG` (Q6) 再放自由文本.
- 验收: 单元测试覆盖队列 / 并发 / 取消 / 输出命名; 仪器测试经 Binder 跑 `probe` 与一个 `trim`; 宿主 P1.3 真机联调通过. 结果 (2026-11-06): JVM 单测 108 例通过 (新增 `TaskRunnerTest` 15, `RequestPlannerTest` 14, `OperationOptionsTest` 12, `FrameGeometryTest` 9, `OutputPathsTest` 8, `RequestValidationTest` 7, `MediaEstimatesTest` 4 等); AVD_API_35_16K 仪器测试 `ThreeSlateStudioContractTest` 7 例 + `MediaOperationsTest` 12 例 (经 Binder 的 `probe`, `trim`, 拒绝码, 取消后无残留文件, 每 token 4 个任务上限) 通过; 前台服务: XQ-DQ72 (API 33) 上运行 13 秒的转码已在前台, 取消后约 0.6 秒服务停止; 小米平板 (HyperOS) 默认限制新装应用后台, 系统静默忽略 `startForeground()`, 任务照常完成但无通知 (附录 F, Q13); 宿主 P1.3 真机联调未执行 (属宿主卡片 3-slate-p1-3-5, 本卡片只交付插件).

### P2.2 视频基础加工 (D29)

- [x] (插件) `trim` (免重编码优化优先, 失败回退重编码), `concat` (分辨率不一致时按首段或 options 缩放), `speed` (`keepPitch` 用音频变速处理器), `transform` (缩放 / 旋转 / 裁切 / 翻转), `transcode` (H.264 / H.265, 分辨率, 码率, 帧率, 音频参数), `removeAudio`, `extractAudio` (M4A / WAV / FLAC / OGG, 复用 P2.5 编码器). 证据 (E1 + E2 + E3, 2026-11-06, 3-slate-p2-1-2): 提交 `a0a90f2`; `pipeline/TransformerPipeline` (media3 Transformer 1.10.1; `trim` 先开 media3 剪辑优化直通, 只重编码到首个关键帧, 导出失败时用 H.264 与估算码率重来并在 `warnings[]` 记一句; `concat` 按首段显示尺寸或 `width` / `height` + `fit` (contain / cover / stretch) 缩放, 部分段无音轨时补静音; `speed` 的 `keepPitch` (默认 true) 用 Sonic 音高 1 / 倍数 抵消; `transform` 顺序为 crop -> rotate (顺时针, 90 的倍数) -> flip -> 缩放; `transcode` 的 `frameRate` 只降不升, 不低于输入时保留原帧率并记 warning; `removeAudio` 视频轨原样复制; `extractAudio` M4A 在未给音频选项时 AAC 直通), `pipeline/AudioPipeline` (WAV 自写 44 字节头, FLAC 用平台编码器并补写总样本数, OGG 为 Opus + `MediaMuxer` OGG, API 29+; 声道混合与 Sonic 重采样; P2.5 的 `audio.*` 复用这些 sink), `pipeline/OperationJob` (探测 -> 写同目录隐藏 `.part` 文件 -> 改名 -> 登记 MediaStore). 编码按附录 D Q7 (a): 默认 H.264, `codec: "h265"` 可选, 设备没有 H.265 编码器时 `UNSUPPORTED_FORMAT`; 默认码率 1080p30 H.264 8 Mbps, H.265 取 0.6 倍 (4.8 Mbps, 附录 D Q12). 选项, 默认值与检查顺序逐条写在共享笔记 `3-slate-p2-notes.md`.
- 验收: 每个操作在 3 秒彩条夹具上有仪器测试 (输出可探测, 时长 / 尺寸 / 轨道符合预期); 真机上 1080p 60 秒素材的转码耗时与输出码率记录到附录 F. 结果 (2026-11-06): 通过. AVD_API_35_16K 上 `MediaOperationsTest` 12 例覆盖全部 7 个操作 (含 2 倍速时 `keepPitch` 开 / 关的 440 Hz 音调分别测得约 440 / 880 Hz, 裁切 + 顺时针旋转后的宽高与色条顺序, 水平翻转, `transcode` 的缩放 / 码率 / 降帧率 / 音频参数与 h265, `extractAudio` 的 4 种容器与声道 / 采样率) 与拒绝码 / 取消 / token 上限; `./gradlew test connectedDebugAndroidTest` (固定 `ANDROID_SERIAL=emulator-5610`) 单测 108 例 + 仪器 21 例通过 (基准用例默认跳过); 真机 XQ-DQ72 与小米平板经 `.python/benchmark_transcode.py` 各跑 1080p 60 秒 H.264 / H.265, 耗时与码率见附录 F; 未覆盖: 带旋转元数据的真机素材, 自然内容 (摄像头拍摄) 的码率, 留 P2.6.

### P2.3 叠加 (D29)

- [x] (插件) `overlay`: 文字 (字体 / 字号 / 颜色 / 描边 / 阴影 / 对齐 / 位置 / 时间段 / 透明度, 系统字体或 TTF 路径) 与图片 (路径, 缩放, 位置, 时间段, 透明度), 多个叠加一次完成; 坐标支持像素与百分比. 证据 (2026-11-06, 3-slate-p2-3-5): 提交 `f3751f5`; `core/OverlayOptions` (`overlays[]` 1 - 64 项, `type` text / image; 文字 `text` 1 - 1000 字符, `font` 为 sans-serif / serif / monospace 或绝对字体路径, `fontSize` 默认 48, `bold` / `italic`, `color` 默认白, `stroke` / `strokeWidth` (给任一即描边, 默认黑, 宽度为字号 / 16), `shadow` / `shadowRadius` / `shadowDx` / `shadowDy` (给任一即加阴影, 默认黑, 半径 4, 偏移 2), `align` 默认 center; 图片 `src` 绝对路径, `scale` 与 `width` / `height` 互斥, 只给一边保持宽高比; 共用 `x` / `y` (数字为像素, `"25%"` 为帧宽 / 高百分比, 默认 50%), `anchor` (9 个方位, 默认 center), `from` (含) / `to` (不含), `alpha` 0 - 1; 颜色为 `#RGB` / `#RRGGBB` / `#AARRGGBB` / 颜色名 / ARGB 整数), `pipeline/OverlayRenderer` (全部叠加画进一张帧尺寸透明位图, 可见集合变化时才重画, 经 media3 `BitmapOverlay` 合成; 字体与图片在导出前加载, 图片不可解码为 `UNSUPPORTED_FORMAT`; 叠加起点晚于输入结尾时在 `warnings[]` 记一句). 字体文件与图片像输入一样检查存在, 可读与存储权限. 选项规则写在共享笔记 `3-slate-p2b-notes.md`.
- 验收: 仪器测试对输出抽帧并比对叠加区域的像素差; 真机目视一次. 结果 (2026-11-06): 通过. `OverlayOperationsTest` 3 例 (图片的像素 / 百分比定位与锚点, 时间段, 半透明, 宽度百分比缩放逐像素核对颜色; 文字区域内像素变化 > 15%, 区域外 < 2%; 系统字体文件 + 左下锚点 + 阴影; 缺图 / 缺文字 / 非图片文件 / 起点晚于结尾的拒绝与提示) 在 AVD_API_35_16K 与 XQ-AT72 (API 31) 上通过; 真机首跑发现半透明叠加的 alpha 被乘两次 (黑底 50% 蓝为 0x39, 应为 0x80; media3 叠加着色器按非预乘 alpha 混合, Canvas 位图是预乘的), 改为把颜色拷进非预乘位图后交给 media3, 该断言容差收紧为 32. 真机目视: XQ-AT72 输出帧上红块, 缩放蓝块, 半透明深蓝, 红字黑描边粗体与左下带阴影小字均位置正确.

### P2.4 抽帧与 GIF (D32)

- [x] (插件) `frames` (按时间点 / 间隔 / 数量, PNG / JPG, 宽度, 输出目录) 与 `frame` (单帧返回 `Image`), `thumbnail`; `toGif` (起止, fps, 宽度, 循环, 抖动, 上限 600 帧), 自带 `GifEncoder` (中位切分调色板 + LZW). 证据 (E1 + E2, 2026-11-06, 3-slate-p2-3-5): 提交 `f3751f5`; `core/OperationOptions` + `core/FrameTimes` (`frames`: `at` (一个时间点) 或 `times` (多个, 按给出顺序), 或在 `start` / `end` (不含) 内按 `every` (毫秒间隔) 或 `count` (每帧取所在时段中点) 取帧, 两者同给时取间隔序列的前 `count` 帧; 单次至多 1000 帧, 超出为 `LIMIT_EXCEEDED` (附录 D Q15); 图片 `format` 为 png / jpg / jpeg, `quality` 1 - 100 只用于 jpg, `width` 16 - 8192 (等比); `frames` / `frame` 默认 png, jpg 质量 90, 原宽; `frame` 必须给 `at`; `thumbnail` 的 `at` 默认为时长的 1 / 3, 默认 jpg 质量 85, 宽 min(320, 原宽); `toGif` 的 `start` (默认 0) / `end` (不含, 默认到结尾, 超出时截到结尾), `fps` 1 - 50 (默认 10), `width` (默认 min(480, 原宽)), `loop` (省略或 true 为无限循环, false 不写循环扩展只播一次, 整数为重复次数), `dither` (默认 false); 已知范围时帧数在 submit 检查, 否则探测后检查; 时间点晚于输入结尾为 `INVALID_OPTIONS`), `core/GifEncoder` (GIF89a; 每帧局部颜色表, 帧内不超过 256 色时用原色, 否则对 15 位直方图做中位切分, 可选 Floyd-Steinberg 抖动; 可变码宽 LZW, 码表满时发清除码; 帧延时按累计值取整, 总和等于片段时长), `pipeline/StillOperations` (`MediaMetadataRetriever` 按 `OPTION_CLOSEST` 取帧, 失败时退回 `OPTION_PREVIOUS_SYNC`; 按旋转元数据摆正; `frames` 每帧先写同目录隐藏 `.part` 文件再改名, 目标已有同名文件为 `IO_ERROR`, 失败或取消时删掉本任务写的帧, 文件夹由本任务新建时一并删除; 全部帧一次登记 MediaStore), `core/OutputPaths` (图片, GIF 与帧文件夹默认 `/sdcard/Pictures/3-Slate Studio/`, 附录 D Q14; 帧文件名 `<源名>-<n>.<ext>`, n 从 1 起, 至少 4 位). `frame` 按附录 D Q25 (a): 宿主把输出设为隐藏文件 `.frame-<uuid>.<ext>` 并关闭 `registerMedia`, 解码为 `Image` 后删除. 选项规则与结果字段写在共享笔记 `3-slate-p2b-notes.md`.
- 验收: GIF 输出能被 `MediaMetadataRetriever` / 浏览器解码且帧数与时长正确; 单元测试覆盖调色板与 LZW 边界 (256 色, 单色, 超长帧). 结果 (2026-11-06): 通过. `MediaMetadataRetriever` 只读视频与音频, 不能解码 GIF, 改用平台的 GIF 解码器核对: `FrameOperationsTest` 7 例在 AVD_API_35_16K 通过 (`frames` 的 PNG / JPG 编号文件与彩条颜色, 同一文件夹追加, 重跑为 `IO_ERROR`; `times` 顺序, 晚于结尾与超过 1000 帧的拒绝; `frame` / `thumbnail` 的尺寸与像素, 纯音频输入为 `UNSUPPORTED_FORMAT`; 旋转 90 度的素材抽出的帧已摆正; 取消后帧与文件夹都删除; `toGif` 0.5 - 2.5 秒 10 fps 宽 160 为 20 帧, 每帧 10 cs, 循环 0, 共 2000 ms; 30 fps + 不循环 + 抖动为 90 帧, 延时 3 / 4 cs 交替, 合计 300 cs, 无 NETSCAPE 块; 两个 GIF 经 `android.graphics.Movie` (时长), `ImageDecoder` (`AnimatedImageDrawable`) 与 `BitmapFactory` (首帧) 解码; 已知范围超过 600 帧在 submit 时为 `LIMIT_EXCEEDED`); 拉回 PC 的两个 GIF: Pillow 读出 160x120 / 20 帧 / 2000 ms / loop 0 与 320x240 / 90 帧 / 3000 ms / 无 loop, 浏览器 (WebCodecs `ImageDecoder` + `<img>`) 读出 20 帧 / 2000 ms / 无限循环与 90 帧 / 3000 ms / 播放一次, 尺寸一致; JVM 单测 `GifEncoderTest` 9 例, 用 JDK ImageIO 的 GIF 读取器解码核对 (256 色逐像素原样保留; 单色帧用最小的 2 项颜色表; 600x400 随机 256 色的大帧远超 4096 个码, 码表清除后仍逐像素一致, 最长延时 65535 cs; 多帧的帧数与时长; 延时累计取整; 循环扩展有无; 4096 色渐变经中位切分平均误差 < 5; 抖动; 非法帧拒绝) 与 `FrameSamplingTest` 5 例, 全部单测 144 例通过. 性能提示: 每次 `OPTION_CLOSEST` 都从前一个关键帧解码, 帧数多且关键帧间隔长时耗时近似线性增长 (留 P2.6 记录基线).

### P2.5 音频加工 (D15)

- [x] (插件) `audio.trim`, `concat`, `mix` (多路, 各自音量与偏移), `volume` (增益 / 归一化 / 淡入淡出), `convert` (M4A / WAV / FLAC / OGG-Opus, 采样率, 声道, 码率), `speed`; `AudioPipeline` 用 media3 解码 + `AudioProcessor` + 平台编码器, OGG 用 `MediaMuxer` OGG (API 29+, 低版本报 `UNSUPPORTED_FORMAT`). 证据 (E1 + E2, 2026-11-06, 3-slate-p2-3-5): 提交 `f3751f5`; `core/OperationOptions` (6 个操作共用 `format` (m4a / wav / flac / ogg, 省略时由输出扩展名补上, 都没有时 m4a, 附录 D Q16 (a); `audioConvert` 必须能确定格式), `bitRate` (8 - 512 kbps, 无损 wav / flac 不适用), `sampleRate` (ogg 只收 Opus 采样率), `channels` 1 / 2; `audioTrim` 的 `start` / `end` / `duration` 同 `trim`; `audioMix` 2 - 16 个输入 (超出 `LIMIT_EXCEEDED`, 附录 D Q18 (a)), `tracks[]` 每个输入一项 `volume` 0 - 16 (默认 1) 与 `at` (毫秒偏移, 默认 0); `audioVolume` 的 `gain` 0 - 16 (线性, 附录 D Q19 (a)) 与 `normalize` (`true` = -1 dBFS, 或 -60 - 0 dBFS 的目标峰值) 互斥, `fadeIn` / `fadeOut` 毫秒, 至少给一项; `audioSpeed` 的 `factor` 0.25 - 8 与 `keepPitch` (默认 true)), `core/AudioTargets` (未指定时的输出格式: wav 保留源声道, 其他至多 2 声道; ogg 48 kHz, m4a 取 AAC 支持的不低于源的采样率, wav / flac 保留源采样率; 多输入按最高采样率与最多声道; AAC 单声道 128 / 立体声 192 kbps, Opus 64 / 96 kbps), `pipeline/AudioOperations` (每个输入解码为同一目标格式的 16 位 PCM, 经 P2.2 的 `PcmSinks` 编码; `audioMix` 直接相加, 削波时在 `warnings[]` 报告比例并建议调低音量, 附录 D Q17 (a), 2026-11-06 由 3-slate-p3-1-2 按 Q17 (c) 改为两遍缩放, 见附录 B.2; `audioVolume` 的归一化与淡出先读一遍输入求峰值与时长; `audioSpeed` 用 Sonic; `extractAudio` 输出 wav / flac / ogg 也改走这条路径, m4a 仍走 media3 Transformer). 输出与编码器检查: 能力表 `mediaStudioAudioOutputFormats` 列出可用容器 (ogg 需 API 29+), 缺编码器时 `UNSUPPORTED_FORMAT`. 选项规则写在共享笔记 `3-slate-p2b-notes.md`; 宿主选项预检同步见附录 D Q24 (宿主本地提交 `764b63b765`).
- 验收: 每种输出格式有仪器测试 (可解码, 时长与声道正确); 混音结果在真机听一次. 结果 (2026-11-06): 通过. AVD_API_35_16K 上 `AudioOperationsTest` 6 例: `audioConvert` 写出 m4a / wav / flac / ogg 的单声道与立体声, 经 `MediaExtractor` + `MediaCodec` 解码核对声道与 3000 ms (容差 150 ms), ogg 为 48 kHz, 重采样到 16 kHz 后 440 Hz 音调仍在 (3 %); 无音轨的视频为 `UNSUPPORTED_FORMAT`; `audioTrim` 的 wav 从第 22050 个样本逐样本一致, m4a 2000 ms, 超出结尾截到结尾, `start` 不早于结尾为 `INVALID_OPTIONS`; `audioConcat` 单声道 44.1 kHz + 立体声 48 kHz 拼成立体声 48 kHz 6000 ms, 三种来源拼成 m4a 9000 ms; `audioMix` 两路各 0.5 倍且第二路偏移 1000 ms 时 4000 ms, 各段峰值符合预期且无削波, 2 倍音量相加时 `warnings[]` 报削波; `audioVolume` 的 0.5 倍增益, 归一化到 -6 dBFS, 1 秒淡入淡出各点电平, flac 4 倍增益报削波; `audioSpeed` 2 倍保持音调约 440 Hz, 不保持约 880 Hz, 0.5 倍 m4a 6000 ms. 试听: `.python/listening_sample.py` 经 `ListeningSampleTest` 在 Sony XQ-AT72 (API 31) 上把 16 秒的三声部 (琶音 44.1 kHz 单声道 WAV, 4 秒进入的低音 22.05 kHz 单声道 FLAC, 鼓 48 kHz 立体声 M4A) 用 `audioMix` 混成 m4a (峰值 -7.05 dBFS, 0 个削波样本), 再用 `audioVolume` 归一化到 -1 dBFS 并淡出 3 秒 (峰值 -1.01 dBFS); 维护者 2026-11-06 在真机试听确认: 无破音或咔哒声, 低音在 4 秒合拍进入, 鼓与琶音对齐, 无失真. 全部单测 156 例通过.

### P2.6 设备矩阵与性能基线

- [x] (测试) 附录 F 设备矩阵跑一遍 P2 的回归脚本 (插件仓库 `scripts/device-regression.js`, 只输出每项通过 / 失败与耗时); 记录各机硬件编码器名称, 1080p 转码倍速, H.265 可用性. 进度 (E3, 2026-11-07, 3-slate-p3-3): 脚本 `scripts/device-regression.js` (宿主 Rhino 把 P2 / P3 的 25 个操作各跑一次, 另列编码器, 测 1080p 60 秒 H.264 / H.265 转码倍速; `extreme` 另跑 8 视频轨 + 16 音频轨, 失败时逐级减少) 与驱动 `.python/device_regression.py` (ffmpeg 生成素材, 推到插件私有目录, 输出放 `/sdcard/Download/slate-regression`, 运行期间经 appops / pm 授予插件存储权限并事后还原, 删除推送文件与 MediaStore 记录) 已入库 (提交见下方 "提交" 行). 6 台都已跑 (宿主为隔离克隆 debug 6641, 插件 debug 1.0.0 (10), 测后装回原宿主核对 md5): G8441 26 / 27, XQ-AT72 26 / 27, XQ-DQ72 26 / 27, Redmi 23 / 25, 小米平板 25 / 25, OnePlus 25 / 25, 结果见附录 F. 失败项: 三台 Sony 的 8 视频轨 + 16 音频轨极限用例 (6 + 16 都通过, 附录 D Q37), Redmi 1080p 输出 1920x1088 (Q38); 首轮暴露的 `video.frame` 需要全部文件访问 (Q39). XQ-DQ72 / XQ-AT72 因被 chime-p5 占用晚于其他四台补跑 (维护者 2026-11-07 确认可用); 两台宿主的插件中心都把 3-Slate Studio 关着 (此前会话留下的状态, 回归全部 `PLUGIN_DISABLED`), 由维护者打开开关后重跑.
- 验收: 结果表回填附录 F; 失败项登记为条目或待决. 结果 (2026-11-07): 通过. 附录 F 性能基线表 6 台已回填, 失败项登记为附录 D Q37 / Q38 / Q39 (需拍板, 未先行).
- 返工 (2026-11-07, 3-slate-q36-q38, 维护者 08:05 拍板 Q37 (a) / Q38 (a) / Q39 (a)): `compose` 资源不足时返回可操作提示 (Q37, 附录 B.4); 编码器先按请求尺寸配置 (Q38, `pipeline/RequestedSizeEncoderFactory.kt`); 回归脚本按精确尺寸检查输出 (`trim` 为源尺寸, `transform` 360x640, 其余 640x360), 错误截断放宽到 600 字符, 驱动新增 `--cues 2000,6000` (Q36 大字幕解析, 并列出插件与宿主缓存残留). 复测 (宿主为隔离克隆 debug 6641 含 `ca938a9d89` / `094844ef86`, 插件 debug 1.0.0 (12)): Redmi 25 / 25 (此前 23 / 25), 小米平板 27 / 27 (含 `--cues`), XQ-DQ72 26 / 27 与 G8441 26 / 27 (8 + 16 都以 Q37 提示失败, 6 + 16 通过), 见附录 F; XQ-AT72 被 3-Chime 占用, 未复跑.

- 提交: 插件 `8fb7bc8` (回归脚本与驱动, 4 台结果, VERSION_BUILD 11) 与其后一笔 (驱动经 `/data/local/tmp` 中转推送, 插件不可用时提前结束, 两台 Sony 结果, VERSION_BUILD 12).

---

## P3: 多轨合成

状态: 已完成 (P3.1 - P3.2 于 2026-11-06, P3.3 于 2026-11-07; 附录 D Q36 返工 (契约版本 2) 于 2026-11-07, 见 P3.3). 设备: API 35 AVD, Sony XQ-DQ72 API 33, Xiaomi Pad API 35, G8441 API 28 (GL 效果在低端 GPU 的可用性).

### P3.1 时间线 JSON 与 `video.compose` (D33)

- [x] (插件) 时间线模型与校验 (轨道, 片段, 时间重叠规则, z 顺序), `CompositionBuilder` 把时间线编成 media3 `Composition` (视频轨 -> 序列, 音频轨 -> 音频序列, 叠加 -> `OverlayEffect`, 片段变换 -> `Effects`); `compose` 操作接入 `TaskRunner`; 工程文件 `.slate.json` 读写. 证据 (E1 + E2, 2026-11-06, 3-slate-p3-1-2): 提交见下方 "提交" 行; `core/Timeline.kt` (`TimelineOptions.parse` 提交时校验, 字段路径消息, 轨道上限 Q34 (先行 4 / 8, 2026-11-06 按维护者改选 (c) 改为 8 / 16, 见 P3.3 提交); `TimelineLayout.resolve` 探测后定位: 同轨不重叠, 重叠转场的 `at`, 转场长度, 默认尺寸), `pipeline/ComposeOperation.kt` (序列 0 为透明时钟图片, 视频轨自上而下, 有重叠转场的轨道拆成两个车道, 车道空档与结尾用 gap 并在合成器设置里隐藏; 非黑背景为拉伸的纯色图片; 时间线叠加物为合成级 `OverlayEffect`; 音频经 `PcmMixer` 两遍混成临时 WAV 再作为音频序列编码), `core/SlateProject.kt` (`.slate.json` = 时间线对象 + `version` + `ui`, 读取时同样校验). 规则全文在附录 B.4 与共享笔记 `3-slate-p3-notes.md`; 宿主预检按附录 D Q24 同步 (宿主本地提交见 "提交" 行).
- 验收: 仪器测试: 两段视频 + 一段音频 + 一个文字叠加的时间线导出成功, 时长 / 轨道 / 叠加位置正确; JSON 校验的错误信息指出字段路径. 结果 (2026-11-06): 通过. AVD_API_35_16K 上 `ComposeOperationsTest` 11 例中的 P3.1 部分: 彩条 2 s + 蓝色片段 2 s + 偏移 500 ms 的音调音频 + 文字叠加 (`x` 50 %, `y` 20 %, 1 - 3 s) 导出 320x240, 4000 ms (容差 100 ms), 视频与音频各一轨, 两段画面颜色正确, 文字只出现在 y 约 48 处且 3 s 后消失, 音频在 500 ms 前与 3.5 s 后静音; 上层轨道缩放后盖住下层, 下层结束后只剩上层, 无轨道处显示 `background`; 提交时校验与定位错误的消息带字段路径 (`unknown option timeline.video[0].clips[0].volumes`, 任务中的 `timeline.video[0].clips[1].at (1000) must be 2500 ...`), 失败不留半成品文件. JVM `TimelineTest` 9 例 (默认值与来源, 字段路径消息, 定位与转场重叠, 需要媒体的定位错误, 尺寸 / 时长 / 4 与 8 轨上限, GPU 回退替换, 混音来源淡化, 着色器状态, `.slate.json` 读写), 全部单测 166 例通过; 全量 `connectedDebugAndroidTest` 100 例 0 失败 (6 例按门控跳过).

### P3.2 转场 (D30)

- [x] (插件) 7 种转场的 GLSL 效果与重叠区间双序列合成; 转场时长校验; 时间线 `transition` 字段与界面转场表同源. 证据 (E1 + E2, 2026-11-06, 3-slate-p3-1-2): `core/Timeline.kt` 的 `TransitionType` (7 种的重叠 / 方向 / 着色器属性与 GPU 回退, 界面转场表以它为源), `pipeline/TransitionEffect.kt` (GLSL ES 1.00: 亮度 (fade 经黑场), 擦除遮罩 (wipe), 两遍 15 点高斯模糊 (blur, 最强半径为短边 5 %), 时间按片段自身的第一帧起算), `pipeline/ComposeOperation.kt` 的 `ComposeCompositorSettings` (dissolve / zoom / blur 的上层透明度, zoom 缩放 0.5 -> 1 与 1 -> 1.5, slide 位移). GPU 回退: 导出以帧处理错误失败时先把 wipe / blur 换成 dissolve, fade 换成 cut 重导一次, 仍失败则全部换成 cut, 每次在 `warnings[]` 写一条 (`the GPU of this device failed on the wipe, blur transitions (...); they were exported as dissolve (fade as cut)`).
- 验收: 每种转场在仪器测试里导出 2 秒样本并抽取中间帧比对 (两源各占一定比例); 真机 (含 API 28) 目视一次, 记录 GPU 不支持时的回退 (退为 `dissolve` 或 `cut` 并返回 warning). 结果 (2026-11-06): 通过 (仪器测试通过, 维护者在 G8441 上目视 9 个样本正常). AVD_API_35_16K 上 `ComposeOperationsTest` 的 P3.2 部分: 红 / 蓝片段以 1 s 转场接成 2000 ms 样本 (时长容差 100 ms), 转场中点 1 s 处 cut 前后为红 / 蓝, fade 经黑且两侧半亮, dissolve / zoom / blur 中点两源混色, slide (left) 左半为离场红色右半为入场蓝色, wipe (up) 上半红下半蓝, zoom 角落只有离场片段; `failShadersForTest` 模拟 GPU 失败时 wipe 导出为 dissolve 并带回退 warning (JVM `TimelineTest` 核对回退表与位置不变; 第二级 "全部换成 cut" 只有代码路径, 没有单独的用例). 样本: `.python/transition_samples.py` (`TransitionSampleTest`, 9 个样本, 彩条 2 s + 反序彩条 2 s, 转场 1 s) 在 AVD 与 Sony G8441 (API 28, `BH900ASK9E`) 上 9 / 9 PASS, 均无 GPU 回退; AVD 样本按 p = 0.25 / 0.5 / 0.75 抽帧自查方向与混合正确 (模糊转场初版 7x7 采样过弱, 改为两遍高斯后复查). 维护者 2026-11-06 在 G8441 上目视确认 9 个样本正常, 之后样本已删除.

- 提交: 插件 `26c60f7` (P3.1, P3.2, Q17 (c)); 宿主 `025ec4dffe` (Q24 同步 compose 预检, 本地, 未推送).

### P3.3 字幕 (D31)

- [x] (插件) SRT 解析 / 导出, ASS 基础子集解析 (样式表 + 允许的内联标签, 其余忽略并 warning), 字幕渲染器 (`StaticLayout` 到 `TextOverlay`), `burnSubtitles` 操作与时间线 `subtitles` 字段; `video.subtitles.parse` / `write` 暴露给脚本. 证据 (E1 + E2, 2026-11-06, 3-slate-p3-3): 提交见下方 "提交" 行; `core/Subtitles.kt` (读取至多 16 MiB, UTF-8 / UTF-16 BOM, 非法 UTF-8 替换并 warning, 按扩展名再按内容判格式; SRT 容忍缺序号 / 缺空行 / 句点毫秒, 文本内空行去掉, 无文本或结尾不晚于开头的 cue 跳过并 warning, 重叠保留; ASS 读 `[Script Info]` 的 PlayRes (缺省按 4:3 推, 都缺为 384x288) 与 WrapStyle, `[V4+ Styles]` / `[V4 Styles]` (SSA 对齐换算), 样式子集为字体 / 字号 / 主色 / 边框色 / 背景色 / 粗斜体 / BorderStyle (3 为底框) / Outline / Shadow / Alignment / 边距, 其余非默认值 warning; 内联标签只认 `\b` `\i` `\c` `\1c` `\fs` `\an` `\pos`, `\N` / `\n` / `\h`, `\p` 绘图不显示, 其他标签汇总为一条 warning; SRT 标记 `<b> <i> <u> <font color>`; 写出 SRT (CRLF, UTF-8) 与 ASS (1920x1080, Default 样式同烧录默认值)), `core/SubtitleOptions.kt` (cue 数组 1 - 10000 条, 文本 1 - 1000 字符; 样式键 = 文字叠加键 + `background` / `align` / `marginV` / `marginH`; ASS 样式按 PlayRes 缩放, SRT 默认字号为画面高 / 18, 白字黑边, 底部居中, 边距 5 %), `pipeline/SubtitlePainter.kt` (`StaticLayout` + span 画进 `OverlayRenderer` 与叠加物共用的非预乘位图, 先描边后填充, 逐行底框, 阴影; 同位置的并存 cue 堆叠: 底部早的在下, 顶部早的在上). 与条目原文的差异: 未用 media3 `TextOverlay` (每个 cue 一个纹理, 且有 P2.3 发现的预乘 alpha 问题), 改为与叠加物同一张位图. `burnSubtitles` / `subtitlesParse` (原先结果超过契约事件上限 256 KiB 时 `LIMIT_EXCEEDED`; 2026-11-07 起按契约版本 2 经只读文件描述符返回, 不限条数, 附录 D Q36 (b)) / `subtitlesWrite`, 时间线 `subtitles` (画在叠加物之上); 能力表 `mediaStudioSubtitleFormats` = srt, ass. 宿主按 Q24 只同步 `burnSubtitles` 与 `timeline.subtitles` 的预检 (`subtitlesParse` / `subtitlesWrite` 见 Q35), 宿主脚本模块在 P1.3 已预留, 未改.
- 验收: 单元测试覆盖 SRT (BOM, CRLF, 空行, 重叠时间) 与 ASS 子集 (样式继承, 标签覆盖, 忽略标签 warning); 烧录结果抽帧目视. 结果 (2026-11-07): 通过. JVM 单测 176 例 0 失败 (新增 `SubtitlesTest` 9 例: BOM + CRLF + 缺序号 + 空行 + 重叠时间, 无效 cue 跳过并 warning, ASS 样式继承与未知样式退回 Default, `\b` / `\i` / `\c` / `\fs` / `\an` / `\pos` 覆盖与恢复, 忽略标签汇总 warning (含行号), SSA 对齐换算, 写出往返; `TimelineTest` 加时间线字幕与 8 / 16 轨上限); AVD_API_35_16K `connectedDebugAndroidTest` 54 例 0 失败 (3 例基准 / 样本用例默认跳过), 其中 `SubtitleOperationsTest` 4 例抽帧比对像素: SRT cue 只在各自时间窗出现且并存时堆叠, ASS 的 BorderStyle 3 蓝色底框与 `\pos` 定位的绿字, cue 数组与时间线字幕, 经 Binder 写出再解析; 宿主隔离克隆 (6641) `org.autojs.autojs.core.plugin.mediastudio.*` 单测 51 例 0 失败. 真机目视 (E4, 2026-11-07): 小米平板上把 1080p 10 秒片段烧录成 4 个样本 (SRT 默认样式与重叠堆叠, `{\an8}` 置顶, 斜体 / 红字; ASS 半透明蓝底框, `\c` / `\b` / `\fs` 与 `\N`, `\an7\pos(200,300)`, `\k` 被忽略并 warning; cue 数组 + 黄字半透明底框; `compose` 时间线字幕盖在文字叠加之上), 关键帧拼图存于共享笔记 `3-slate-p3-3-visual/`, 维护者目视正常, 样本已删.

- 提交: 插件 `fa78933` (P3.3, Q34 (c), VERSION_BUILD 10); 宿主 `630ba2a200` (Q24 同步 `burnSubtitles` / `timeline.subtitles` 预检与 Q34 (c) 上限, 协议文档, 本地, 未推送); P2.6 脚本与结果见 P2.6 的 "提交" 行. Q36 - Q38 返工 (2026-11-07, 3-slate-q36-q38): 宿主 `ca938a9d89` (契约版本 2, `subtitlesParse` 经文件描述符返回 cue) 与 `094844ef86` (`video.frame` 不支持的格式按选项报错, Q39 附带), 本地, 未推送; 插件为 `ca938a9d89` 之后的一笔本地提交 (新 AAR, `SubtitleCueFiles`, `CodecResources`, `RequestedSizeEncoderFactory`, 回归脚本, changelog 契约版本号, 本 ROADMAP), 未推送.

---

## P4: Studio 界面

状态: 已完成 (P4.1 - P4.5 于 2026-11-07, 见各条结果). 设备: API 24 AVD (最低版本布局), API 35 AVD, Sony XQ-AT72 API 31, Xiaomi Pad API 35 (大屏), OnePlus API 35 (ColorOS); P4.5 另需 Sony XQ-DQ72 API 33 与 G8441 API 28.

### P4.1 应用骨架, 首页, 任务队列, 设置, 发行历史 (D37)

- [x] (插件) Compose 应用骨架 (导航, 主题, 阿拉伯语 RTL 与 FSI / PDI), 首页 (D37 目录的媒体列表, 缩略图, 详情页含 "用 MediaInfo 查看" 入口), 任务队列页 (进度, 取消, 失败原因), 设置页 (D37 各项 + 启动器图标模式 + "所有文件访问" 引导 + "电池无限制" 引导 (附录 D Q13: 系统限制插件后台时, 能力表 `mediaStudioWarnings` 已报警告, 设置页给出跳转)), 发行历史与更新检查 (照 Maple Image). 证据 (E1 + E2, 2026-11-07): 提交 `a1c7479` (实现, VERSION_BUILD 15) 与其后的 `fix(ui)` 提交 (设备检查中的界面修正与本节回填, VERSION_BUILD 16); 界面套件照 3-Prism Camera 的 Compose 套件 (`StandaloneActivity`, 外观跟随 AutoJs6 并可自选语言 / 夜间模式 / 主题色, MaterialKolor 4.1.1 调色, 发行历史页与官方插件索引更新检查; 3-Maple Image 只有发行历史对话框, 没有更新检查); 首页 600 dp 以下为底部导航栏, 600 dp 起为侧边导航栏; 新增文案 282 条 x 11 个 values 目录; JVM 214 例 0 失败 (新增 `OptionSchemasTest`, `WizardFormTest`, `AppearanceTest`, `AppUpdateIndexTest`, 更新 `ManifestContractTest`); `lintDebug` 只余他卡既有的 `core/Subtitles.kt:427` 字节序标记错误 (`fa78933` 引入, 不属本卡). D37 的 "硬件编码器偏好" 与 "任务并发数" 按附录 D Q50 (b) 由 P4.4 补上 (提交 `035c879`); 脚本任务的失败原因按 Q51 (b) 由 P4.4 补上 (执行器快照保存错误码与详情, 队列页对脚本任务也显示); "用 MediaInfo 查看" 入口按 Q52 (a) 在 MediaInfo 提供查看器后才出现.
- 验收: API 24 / 35 AVD 与两台真机界面可用; 亮暗主题与阿拉伯语各截一次图. 结果 (2026-11-07): 通过. `.python/device_ui_check.py` (从启动器打开首页, 按测试标签只点按插件自己的页面: 媒体库, 详情, 工具箱, 修剪向导, 任务, 设置; 每页检查进程与崩溃): AVD_API_24 6 / 6, AVD_API_35_16K 亮 / 暗 / 阿拉伯语 18 / 18, XQ-AT72 (API 31) 6 / 6, 小米平板 23046RP50C (API 35 大屏, 侧边导航栏) 暗色与阿拉伯语 12 / 12 加亮色 4 页 (亮色任务页已打开, 脚本因空状态缺标签误判, 已补标签; 其后屏幕自动锁定未复跑), OnePlus OPD2413 (API 35, ColorOS, 首装由维护者确认) 6 / 6, 均无崩溃; 截图在共享笔记 `3-slate-p4-visual/`. 检查中修正: 阿拉伯语里分辨率 / 码率 / 帧率被双向算法重排 (改用 LRI / PDI 隔离), 详情页路径开头的斜杠跑到行尾 (FSI / PDI), 修改时间用了进程默认区域 (改用插件语言), 详情页标题按字符折行 (单行中间省略), 时间与像素字段的范围提示显示 9007199254740992 (上限不小于 `Int.MAX_VALUE` 时改为 "至少 N", 新文案 `wizard_min_hint`). 未执行: G8441 停在安全锁屏 (非本条要求).

### P4.2 工具箱向导 (D35)

- [x] (插件) 每个加工 API 一个向导页 (P2.2-P2.5 全部操作), 共用文件选择器 (SAF + 首页目录), 参数表单由 `OptionSchemas` 驱动, 结果页 (输出路径, 媒体信息, 打开 / 分享, 生成的代码预览). 证据 (E1 + E2, 2026-11-07): 提交同 P4.1; 20 个向导 (P2.2 - P2.5 的 18 个操作, `burnSubtitles`, 字幕格式转换 = `subtitlesParse` + `subtitlesWrite`; 时间线留 P4.3), 表单的字段类型, 默认值, 范围, 枚举, 互斥组与条件字段都来自 `codegen/OptionSchemas`, 提交前用插件校验器 `OperationOptions.parse` 预检, 与脚本同一执行器与队列 (origin `studio`); 码率策略 (自动 / 高画质 / 小文件) 按输入估算后写进请求与等价代码; 结果页显示输出, 媒体信息, 打开 / 分享与等价脚本调用 (`codegen/ScriptCalls`, 完整代码生成与宿主入口为 P4.4). `OptionSchemasTest` 以边界值逐字段喂给校验器, schema 渲染出的选项声明入库为 `docs/development/media-studio-options.d.ts` 并由测试核对 (P5.2 用它写 Rhino 的 `video` / `audio` 声明; Node d.ts 与插件选项的差异记在共享笔记 `3-slate-p4-notes.md`, 留 P5.2); `WizardFormTest` 7 例 (最少输入能被校验器接受且代码与 `ScriptCalls` 一致, 文本转换, 逐字段错误, 条件字段, 每路输入的音轨, 默认编码预填, 每个键与选项都有文案).
- 验收: 每个向导走通一次并与脚本调用结果一致; 表单字段与 d.ts 选项逐项对应 (单元测试比对 schema). 结果 (2026-11-07): 19 / 20 一致, frames 的脚本一侧被宿主预检拒绝 (附录 D Q53; 宿主修复后复测 20 / 20, 见本条末尾). `.python/device_wizard_check.py` + 仪器测试 `WizardDeviceTest` + `scripts/device-wizard-compare.js`: 每个向导按页面的提交方式 (`WizardForm` -> `StudioTasks.submit`) 跑一次, 另有修剪向导经页面完整走通 (工具箱 -> 向导 -> 从媒体库选择 -> 填写 -> 提交 -> 结果页), 再把报告中的等价调用改写输出路径后经宿主运行, 两份输出用同一组探测比对. AVD_API_24 与 AVD_API_35_16K: 仪器测试 2 / 2 (20 个向导全部完成, 无宿主, 未比对脚本); XQ-AT72 (API 31, 隔离克隆宿主 6641, 宿主 HEAD `8cbea453d5` 含契约 v2 `ca938a9d89`, 测后装回原宿主 md5 fe59a68e): 仪器测试 2 / 2, 19 个向导与脚本输出的尺寸, 时长, 编码一致 (trim 640x360 1509 ms, concat 320x180 6037 ms, transform 320x180, transcode 480x270, toGif 160x90, audioConvert pcm 5015 ms, subtitlesConvert 文本逐字相同等); frames 向导输出 3 帧 640x360, 等价脚本 `video.frames(..., { times: [500, 1500, 2500] }, ...)` 被宿主报 `options.times must be an array of numbers` (宿主问题, Q53 修复后用同一脚本复测). Q53 复测 (2026-11-07, 3-slate-p4-4-q50-q53): XQ-AT72 (隔离克隆宿主 6641 = 宿主 HEAD `063b9f0b8a` + Q53 修复 `549ff67974` 的同一改动) `device_wizard_check.py` 20 / 20 一致, frames 向导与等价脚本 `video.frames(..., { times: [500, 1500, 2500] }, ...)` 各输出 3 帧 640x360; 测后装回原宿主 md5 fe59a68e.

### P4.3 时间线剪辑器 (D10, D34)

- [x] (插件) spike: `CompositionPlayer` 在 API 28 / 33 / 35 真机的实时预览可用性, 结论回填附录 D Q3. 结论 (2026-11-07): (b) 自写预览, 各机数据见附录 D Q3; spike 工具 `.python/preview_spike.py` 与 androidTest `preview/` (`PreviewSpikeTest`, 照导出组装的 `PreviewComposition` / `PreviewAudio`) 保留, media3 升级后可复跑.
- [x] (插件) 多轨时间线 UI (视频 / 音频 / 叠加 / 字幕轨, 缩略图与波形, 缩放, 吸附), 片段操作 (拖入, 裁剪, 分割, 删除, 移动, 变速, 音量), 转场选择与时长, 叠加编辑 (文字 / 图片, 位置拖动), 字幕编辑 (列表, 时间, 样式, SRT / ASS 导入, SRT 导出), 预览播放, 工程保存 / 打开 (`.slate.json`), 导出 (调用 `compose`, 进入任务队列). 实现 (2026-11-07): `ui/timeline/` (`TimelineActivity` 编辑 / 结果 / 详情三路由, `EditorModel`, `EditorScreen`, 画布 `TimelineView`: 播放头居中, 拖动选中项移动 / 两端裁剪 / 吸附到边缘, 播放头与 0, 跨视频轨放下, 双指缩放; `Inspector`; 缩略图与波形 `TimelineMedia`), `project/` (`EditorProject` 波纹 / 空档模型, `EditOps`, `EditHistory` 撤销 50 步且一次拖动合并为一步, `ProjectJson` 读写 `.slate.json` 与生成附录 B.4 时间线 JSON, `ProjectStore`), `preview/TimelinePreview.kt` (Q3 (b)), `codegen/timeline/TimelineCalls.kt` (结果页的等价 `video.compose(timeline, output, options)`); 字幕导入沿用 `core/Subtitles.kt`; 导出前用核心校验 (轨道上限, 超过实测 28 个来源时显示 6 + 16 资源提示), 经 `StudioTasks.submit` 以 `compose` 进入任务队列 (时间线放进请求的 `timeline` Bundle). 工程有全部文件访问权限时存 `Movies/3-Slate Studio/Projects` (D37 默认目录下), 否则存应用私有 `files/projects`; 入口为首页顶栏图标与工具箱 "时间线剪辑" 分组 (新建 / 打开工程). 阿拉伯语下时间线, 预览与播放控件保持从左到右 (附录 D Q60).
- 验收: 用 3 段素材 + 音乐 + 字幕做一个 30 秒工程并导出, 与脚本 `video.compose` 同一 JSON 的输出一致; Xiaomi Pad 大屏与手机竖屏各验一次; 撤销 / 重做 10 步以内可用. 结果 (2026-11-07): 通过. 仪器测试 `TimelineDeviceTest.timelineProject` 在界面上做 30 秒工程 (3 段 1280x720 素材各裁到 10.5 s, 溶解与推移转场各 750 ms, 音乐裁到 30 s 并淡入 1 s / 淡出 1.5 s / 音量 0.6, 导入 8 条 SRT 字幕, 文字叠加与图片叠加), 撤销 / 重做各 12 步, `.slate.json` 保存与往返, 预览播放, 经导出对话框进入任务队列; 驱动 `.python/device_timeline_check.py` 再经宿主 (宿主 HEAD 的隔离克隆构建的 6641 debug, md5 600487fb) 用 `scripts/device-timeline-compare.js` 运行结果页的等价 `video.compose` 脚本并比对两份输出: XQ-DQ72 (API 33, 手机竖屏) 两份都是 1280x720, 30010 ms, h264 / aac 44100 Hz 单声道, 2 轨 (audio, video), 8501449 字节 (完全相同; 剪辑器 8214 ms, 脚本 7866 ms); 小米平板 968e9f18 (API 35, 大屏) 两份都是 1280x720, 30000 ms, h264 / aac 44100 Hz 单声道, 2 轨, 6753759 / 6753512 字节 (剪辑器 8917 ms, 脚本 8102 ms); 两台预览播放 2.6 - 3.0 s 推进. 阿拉伯语 (XQ-DQ72, 不导出) 通过, 时间线, 预览与播放控件保持从左到右 (附录 D Q60). AVD_API_24 (只读启动) `timelineBasics` (分割, 撤销 / 重做, 转场, 播放, 缩放, 添加 cue, 工程列表) 通过, logcat 无崩溃. 单元测试 `EditorProjectTest` (工程读写, 撤销 / 重做, 时间线 JSON 生成与校验) 与 `TimelinePreviewTest` (预览转场公式). 截图在共享笔记 `3-slate-p4-visual/p4-3/` (小米平板与 XQ-DQ72 中文 5 张, XQ-DQ72 阿拉伯语 3 张, AVD_API_24 1 张). 各机测后宿主与插件原包装回并核对 md5. 提交 `76254e2` (插件 VERSION_BUILD 21); 本地 testDebugUnitTest 254 / 254, lintDebug 0 错误, assembleDebugAndroidTest, check_repository, generate_markdown / 图标 / 第三方声明 --check 均通过.

### P4.4 代码生成与宿主入口 (D11, D36)

- [x] (插件) `CodeGenerator` (向导选项 -> `video.*` / `audio.*` 代码; 时间线 -> `video.compose({...})` 代码), 复制按钮, "在 AutoJs6 中打开" (P1.4 入口, 宿主不可用时隐藏; 超过内联上限 (512 KiB 或 200000 字符) 的代码经 FileProvider content URI 发送, P1.7, 附录 D Q28 C). 证据 (E1 + E2, 2026-11-07): 提交 `035c879` (VERSION_BUILD 19, 3-slate-p4-4-q50-q53). `codegen/CodeGenerator`: `wizardScript` 把向导请求写成可运行的完整脚本 (两行头注释 + `let result = video.xxx(...)` / `audio.xxx(...)` + `console.log(result.path)`; `frame` 保存并回收图片, 字幕格式转换先 `video.subtitles.parse` 再 `video.subtitles.write`, `frames` 输出路径列表; P4.3 剪辑器的 compose 请求 (op `compose`, 时间线在 `options.timeline`) 即 `composeScript`), `composeCall` / `composeScript` (时间线 -> `video.compose(timeline[, output[, options]])`, 供 P4.3), `recordScript` (`audio.record` + `sleep` + `stop()`, 供 P4.5); 超过 100 列的调用每个参数一行, 宽对象与数组分行. 结果页显示完整脚本, 带复制与 "在 AutoJs6 中打开" (`ui/toolbox/OpenInAutoJs6.kt`, P4.3 / P4.5 共用): 宿主未装或 versionCode 低于 6641 时隐藏; `codegen/AutoJs6Scripts` 按 512 KiB UTF-8 与 200000 字符判断, 超出时写进 `cache/create-script/`, 经 `${applicationId}.files` FileProvider 以 content URI 加读授权发送 (超过 4 MiB 提示过大, 不发送), 宿主应答后删除, 超过 1 小时的残留在下次发送时清理; 宿主契约字面量在插件内重复 (暂存的 common-plugin-api 1.1.1 尚无 `AutoJs6CreateScriptContract`), 由单测核对. Rhino 测试依赖: 宿主 `:app:slimRhinoJar` 的产物 (宿主 `549ff67974`, sha256 `3e4d74bc...`, 2,653,406 B) 暂存为 `libs/org-mozilla-rhino-2_0_0-SNAPSHOT-slim.jar`, 由 `locks/rhino-jar.lock` 校验, 只在 testImplementation, 不进 APK.
- 验收: 单元测试: 生成的代码经 Rhino 解析无语法错误且选项对象与输入相等; 真机上从向导到宿主编辑器新建文件走通一次. 结果 (2026-11-07): 通过. JVM: `GeneratedScriptRhinoTest` 6 例用与宿主相同的 Rhino (解释模式, ES6) 编译并运行生成的脚本, 以桩函数记录调用: 20 个向导全字段与仅必填字段 (含省略输出路径) 各一遍, frames 的 times / every 等互斥项, 12 个叠加的折行脚本, compose 4 例 (含 256 段), record 3 例, 记录到的选项对象与输入逐项相等 (字符串含引号, 反斜杠, 换行, 制表, U+2028 / U+2029, NUL, 中文与 emoji); `CodeGeneratorTest`, `AutoJs6ScriptsTest` (内联 / URI / 过大的阈值, 6640 / 6641); 插件 JVM 共 244 例 0 失败, lintDebug 0 错误. AVD_API_35_16K 仪器测试 59 例 0 失败 (7 例按设计跳过). 真机 XQ-AT72 (API 31, 隔离克隆宿主 6641 含 Q53 修复, 测后装回原宿主 md5 fe59a68e, 插件测前未装测后卸载): `.python/device_wizard_check.py --open-in-autojs6` 驱动仪器测试 `OpenInAutoJs6DeviceTest`: 修剪向导经页面走到结果页, 页面所示脚本即发送内容, 按 "在 AutoJs6 中打开" 内联发送 340 字符, 宿主 `CREATE_SCRIPT accepted` 并在编辑器打开未保存的新文件 video-trim.js; 1200 段时间线脚本 481684 字符 / 486484 字节经同一按钮以 content URI 发送, 宿主接受, 编辑器到前台, 宿主私有副本的 MD5 与发送内容一致; 同一轮 20 / 20 向导与脚本一致.

### P4.5 Studio 录音 (D41)

- [x] (插件) 录音页: 音源 mic / playback / mixed (后两者仅 API 29+ 显示), 格式 M4A / WAV, 采样率 / 声道 / 码率 (默认值同 `audio.record`, 附录 B.2 与 Q1), 电平表, 计时, 暂停 / 恢复 / 停止, 最长时长. 证据 (E1 + E2, 2026-11-07): 提交 `a311f30` (实现, VERSION_BUILD 18) 与其后的 `docs(roadmap)` 提交 (等价脚本改用 P4.4 的 `CodeGenerator.recordScript` 并加 "在 AutoJs6 中打开", 本节回填, VERSION_BUILD 24); `ui/record/RecordScreen` (路由 `record`; 入口: 首页顶栏麦克风按钮, 工具箱顶部 "录制" 段, 录音通知); 音源按 API 级别给出 (`RecordSettings.sources`, API 28 只有麦克风); 采样率 8000 - 48000 Hz 六档, 单 / 立体声, 码率 64 - 320 kbps (WAV 不显示), 最长时长 不限 / 1 / 5 / 10 / 30 / 60 分钟, 未改动的项显示该音源的默认值 (内核副本 `RecordingOptions`, 同 `audio.record` 与 Q1: 麦克风 48 kHz 单声道 128 kbps, 内录 / 混合 48 kHz 立体声 192 kbps); 电平表为每个数据块的峰值, 计时不含暂停; 授权顺序: 麦克风 -> 通知 (API 33+, 只问一次) -> 所有文件访问 -> 投屏同意 (内录 / 混合); 系统限制插件后台时显示警告行, 点开复用 P4.1 的 "电池无限制" 引导 (Q13); 选项存于 `RecordPreferences`.
- [x] (插件) 投影授权流程与 `mediaProjection` / `microphone` 类型前台服务 (通知含停止按钮, 熄屏可继续); 录音内核照宿主 `core/record/AudioRecorder` (P1.2 产出) 复制, 文件头注明同步来源. P4.5 / P7 的录屏内核副本 (D41) 省略宽高时按附录 D Q33 (a) 的同一规则取默认尺寸 (按编码器 `VideoCapabilities` 向下取对齐倍数, 超上限等比缩小), 与宿主 3-slate-q33 的实现同步: 照抄宿主 `core/record/RecordingVideoSizes.kt` (提交 `95b3d8ab42` / `0d5e37d97d`; 对齐后的全尺寸先试, 不行再按两个方向的上限等比缩小并逐步减小宽度, 一律以 `areSizeAndRateSupported` 为准, 多个编码器取面积最大), 文件头注明同步来源. 证据 (E1 + E2, 2026-11-07): 提交 `a311f30`; 内核副本见 D41 实施注; `record/RecordingService` 在取投影之前先进前台 (API 34+ 的要求), 类型为 microphone (API 30+) 加内录 / 混合时的 mediaProjection (API 29+), 持有部分唤醒锁, 通知每秒刷新时长与音源并带暂停 / 继续与停止按钮, 点按回到录音页; 投影被系统停止时保存已录内容; `record/StudioRecorder` 保存状态供页面与通知读取; 录屏副本 (P7) 尚未复制, 来源写在 `record/README.md`. Manifest 加 `WAKE_LOCK` 与 `.record.RecordingService` (`ManifestContractTest` 新增一例).
- [x] (插件) 产物进首页列表并可 "送进工具箱" 继续加工; 代码生成 (等价 `audio.record` 代码, D11). 证据 (E1 + E2, 2026-11-07): 输出 `Music/3-Slate Studio/slate-record-<yyyyMMdd-HHmmss>.<ext>` (D14 规则, 无单一来源时源名为 slate, 重名加 `-<n>`) 或设置页的输出目录, 保存后登记 MediaStore, 出现在首页媒体库; 结果区与媒体库每行的 "送进工具箱" 按媒体类型列出向导 (音频 6 个), 打开 P4.2 向导并预填输入; 等价脚本 `codegen/record/RecordCalls` -> `CodeGenerator.recordScript` (只写 `source` 与不同于该音源默认值的选项, 等待时长取已录时长向上取整到 100 ms, 默认输出目录时省略路径, 因为脚本的默认输出为 `Music/AutoJs6/`), 可复制, 宿主可用时可 "在 AutoJs6 中打开".
- 验收: Sony API 31 / 33 真机麦克风与内录各录一段并能被 `probe`; G8441 API 28 内录选项隐藏且麦克风可用; OnePlus ColorOS 后台授权弹窗一次; 单元测试覆盖选项校验与输出命名. 依赖 P4.1 与 P1.2, 可与 P4.3 / P4.4 并行. 结果 (2026-11-07): 通过. `.python/device_record_check.py` (装本卡 debug 与测试 APK, 经录音页开始录音, 安装 / 麦克风 / 通知 / 投屏对话框由维护者点; 每个用例录 3.5 s, 暂停 1 s, 再录 2 s 后停止并探测保存的文件; 内录用例由测试应用以 USAGE_MEDIA 播放 440 Hz 测试音; 测后删除录音与 MediaStore 记录, 还原插件, 权限与 appops) + 仪器测试 `RecordingDeviceTest`: XQ-AT72 (API 31) 麦克风 5802 ms (48 kHz 单声道 AAC), 内录 5845 ms (48 kHz 立体声, 测试音峰值 0.300); XQ-DQ72 (API 33) 麦克风 5760 ms, 内录 5674 ms (峰值 0.300), 录音中熄屏 30 秒 36266 ms (期望约 35500 ms, 熄屏期间不中断); G8441 (API 28) 页面只给麦克风 (内录 / 混合隐藏), 麦克风 5888 ms; OnePlus OPD2413 (API 35, ColorOS) 麦克风两次 5824 / 5781 ms, 内录 5888 ms (Android 14+ 先进前台再取投影), 维护者确认 ColorOS 后台 / 自启动弹窗一次都没出现 (只有安装, 麦克风, 通知与投屏对话框), 内录期间 ColorOS 显示 "屏幕共享保护已开启" 通知; AVD_API_35_16K (只读启动) 仪器测试 page / microphone 通过 (经页面开始, 前台服务, 暂停 / 继续, 录音 2.6 - 3.7 s, probe 误差 300 ms 以内, 服务随后结束), 驱动麦克风 5738 ms. 全部用例前台服务在运行, 暂停不计入时长 (经过时间比录音时长多 1.3 - 2.6 s, 含 1 s 暂停), probe 时长与录音时长相差不超过 43 ms. 单元测试: `RecordSettingsTest` 11 例 (默认值与 Q1, 覆盖, 拒绝范围外的值, 页面选项都通过内核校验, 各 API 的音源, D14 输出命名, 自定义目录与重名, 等价脚本, WAV 头, PCM 辅助), `ManifestContractTest` 新增录音服务一例; 隔离克隆 (HEAD + 本卡) `testDebugUnitTest` 全部通过, `lintDebug` 0 错误. 截图: 共享笔记 `3-slate-p4-visual/p4-5/` (AVD 与四台真机的录音中 / 暂停 / 已保存; 锁屏画面含其他应用通知, 不保留).

---

## P5: 文档, 声明, 四仓库同步, 样例与发布

状态: 进行中 (P5.1 与 P5.3 于 2026-11-08 完成, P5.2 部分完成, 见各条). 设备: 附录 F 全部可用设备各跑一次冒烟.

### P5.1 文档 (AutoJs6-Documentation) (D40)

- [x] (文档) `api/video.md` 与 `api/audio.md` (10 种语言, 半角标点, 照 `speech.md` 版式: 方法, options, 返回对象, 错误码, 示例, 版本标记 `6.8.1`), 时间线 JSON 与转场 / 字幕说明, FFmpeg 透传的 1.1.0 预告与 Garage 插件迁移说明; 插件文档页 `plugins/threeSlateStudio.md` (安装, 权限, 界面, 大库); changelog JSON 10 语言. 版本标记原写 `6.8.0`, 2026-11-08 改为 `6.8.1`: `video` / `audio` 随宿主 6.8.1 (内部版本 6641) 提供, 与 P1 及其他 6.8.1 插件模块页一致. 证据 (E3 + E5, 2026-11-08): 文档站提交 `465f7a9` (内容) 与 `0db65c9` (从 HEAD 重新生成离线 HTML / JSON / 搜索索引), 仅本地 (3-slate-p5-1). `api/video.md`: 录屏 `video.record` (宿主 `core/record/`, D40 不写进 `images.md`), 15 个加工方法及其 Async 形态, `video.subtitles` 的 parse / write, `tasks`, `cancelAll` 与 `MediaStudioError`; 页首 "准备工作" (插件, 所有文件访问; Q39 (a): `video.frame` 与默认输出一样需要该权限), 参数约定, 默认输出 (Q14 命名), 任务 / 进度与取消, 时间线 (附录 B.4 与 `core/Timeline.kt`: 顶层键, 轨道与片段键, 时钟顺序, 7 种转场与参数, 定位规则, GPU 回退, Q34 / Q37 的轨道上限与实测), 字幕 (cue, SRT, ASS 子集, 默认样式与样式键, `core/Subtitles.kt`), Node.js 与 Bun (`autojs6:video` / `host.video`, 授权), FFmpeg 命令透传 (计划中, 1.1.0, 只预告不写 API) 与 Garage 等第三方 FFmpeg 插件的写法对照; 选项, 返回对象, 错误代码与限制. `api/audio.md`: 录音 `audio.record` 与 probe / trim / concat / mix / volume / convert / speed 及其 Async 形态, 与 video 页共用的约定以链接引用. 事实以宿主 `docs/dev/media-studio-plugin-protocol.md`, 插件 `docs/development/media-studio-options.d.ts` (`codegen/OptionSchemas`) 与宿主 mediastudio 模块源码核对; 运行时支持块由文档站 `generator/runtime-support.js` 按宿主矩阵 (2026-11-06) 生成. 插件页放在 `api/threeSlateStudio.md` (文档站没有 `api/plugins/` 子目录, 其他插件页都是顶层页, 附录 D Q90): 安装与启用, 权限表, 界面 (媒体库, 工具箱, 任务, 时间线剪辑器, 录音, 设置; 截图 5 张取自共享笔记 `3-slate-p4-visual/`), 代码生成, FFmpeg 大库 (计划中) 与许可. "10 种语言" 落在文档站的 changelog 生成源 `.changelog/lang_*.json` (与生成的 README / CHANGELOG): 文档站 api 页只有简体中文, 与其他模块页相同 (附录 D Q91). 侧栏三项, `plugins.md` 模块表一行, `progress.md` 一条, `api/changelog.md` 一条新增与两条更新. 积压一并落实: `console.restoredEntryCount` (`console.md`, api-coverage feedback-680; 脚本中的 console 读取始终为 0), Node.js `images.requestScreenCapture` 的 30 秒桥接等待上限 (`runtimeApis.md` "Node.js 录屏与音频" 一节与 video 页 "Node.js 与 Bun" 一节).
- 验收: 文档站本地构建通过; 示例脚本在真机跑过. 结果 (2026-11-08): 构建与检查通过: `generate_markdown.py --check` (10 语言 21 个生成物), `check_markdown.bat`, `.python/tests` 17 例, `generator/normalize-markdown.py --check` (196 个文件 0 改动), `runtime-support.js --check` (106 页 0 问题), `test-runtime-support.js`, `test_version_sync.py`, `auto-generate.py --check` (196 个模块为最新); 离线 HTML 中新页与改动页的 3213 个链接全部解析到锚点 (只有 `runtimeApis` 原有的 `dataTypes#promise` 15 处缺锚点, 非本卡). 真机 (E3): 新增 `.python/device_docs_examples.py` (读文档站两页全部 28 个 js 代码块, 示例路径映射到 `/sdcard/Download/slate-docs`, ffmpeg 生成示例用到的 17 个输入文件, 每块在各自的函数中运行, 不抛异常且无 console.error 即通过, Async 示例等 toast 到达; 请求截图会话的示例只在 `--interactive` 时运行; 测后按运行前后的目录比较删除默认目录中的输出与 MediaStore 记录) 在小米平板 23046RP50C (API 35; 宿主 6641 为 P5.3 隔离克隆 HEAD `e0eda70330` 的 debug 包, 只复制; 插件为 HEAD `d75ab37` 隔离克隆的 debug 包, versionCode 24): 26 / 26 PASS (`audio.record` 麦克风示例经 pm 临时授予宿主麦克风权限, 5034 ms 48 kHz; `transcodeAsync` 的 toast 到达; frames 10 张; frame 1280x720; compose 15500 ms; 默认输出命名与页面一致, 如 `slate-compose-<时间>.mp4`, `a-audioConvert-<时间>.flac`; `video.frame` 的隐藏临时文件未残留); 两个请求截图会话的示例 (video 页 `video.record`, audio 页内录) 要维护者在手机上确认对话框, 待补跑. 冷启动的宿主会让启动页随后打开的 MainActivity 盖住先到的运行意图, 脚本要等 RunIntentActivity 再次回到前台才运行, 驱动脚本改为等 MainActivity 到前台再发, 30 秒内未开始则重发 (从强行停止的宿主起跑复核 4 / 4). 测后装回原宿主 6551 与插件 debug 4 (md5 一致), 撤销宿主麦克风权限并清除 user-set 标志, 临时目录, 输出与 MediaStore 记录已删.

### P5.2 声明 (AutoJs6-TypeScript-Declarations, Ace Editor, Offline Docs)

- [ ] (文档) `video.d.ts` / `audio.d.ts` (与插件 `OptionSchemas` 同源, 单元测试比对), Ace 补全与高亮, Offline Docs 打包.
- 验收: d.ts 通过 `tsc` 检查; Ace 补全在宿主编辑器里能提示 `video.` 的方法. 结果 (2026-11-08, 部分; Offline Docs 打包等 P5.1 文档站提交后做, 届时勾选): d.ts 与 Ace 两项通过. (E1) TypeScript-Declarations `aj6-int-video.d.ts` / `aj6-int-audio.d.ts` (`c00b698`, 运行时声明同步 `b4e4930`), `npm test` 通过 (含 `tsc` 编译 smoke); 同源测试 `tools/verify-media-studio-declarations.mjs` 比对插件 `docs/development/media-studio-options.d.ts` (副本与兄弟仓库一致) 的 148 个选项, 20 个方法与 29 个时间线键 (另对照 `core/Timeline.kt`), 以及 Node 声明 53 个类型 356 个键, 删 dither, 去掉 flip 的 both, 插件新增选项, factor 改可选等变异都会失败. Node / Bun 声明修正 P1.6 的不一致 (frames 无 webp, frame / thumbnail 无 height, toGif 的 dither 只收布尔值, flip 有 both, record 默认尺寸按编码器能力) 并补上 P2 - P4 选项: Node `be5b8697`, Bun `dbb5a24` (`sync-media-studio --check`, `tsc` 与 `media-studio.test` 3 / 3). Ace `76134aa` + 宿主 `422f730331` (补全源), `verifyAutoJs6LspRuntime` 通过, 离线补全器 `video.` 37 项 / `audio.` 20 项. (E3) XQ-AT72 宿主 6641 + Ace 2.50.0 debug, `tools/device/ace_completion_check.py` 7 / 7: `video.` / `audio.` / `video.subtitles.` 的成员与 trim / record / mix 的选项字段都有提示, 带类型说明 (截图在共享笔记 `3-slate-p5-visual/p5-2/`). Ace 没有按模块区分的高亮规则, video / audio 与其他模块一样按 JS 语法着色.

### P5.3 README, changelog, 样例与兼容矩阵

- [x] (插件) README (功能, 安装, 权限, 脚本示例, 界面截图, FFmpeg 大库节, 许可证), `CHANGELOG.md` 与 10 语言 changelog JSON, `samples/` 定稿 (P1.3 草稿), 兼容矩阵 (宿主最低版本, Android 版本与格式支持表). 证据 (E1 + E3 + E4, 2026-11-08, 3-slate-p5-3): 提交见看板 "子会话结果". README: `.readme/lang_*.json` 10 种语言 (每种 62 键) 与模板新增截图 (4 张, `docs/images/screenshots/` 的媒体库 / 工具箱 / 时间线 / 录音, 取自共享笔记 `3-slate-p4-visual/`), 示例脚本, 兼容性 (两张表), 权限与用途 (8 项) 各节, 改写状态说明, 功能, 用法, 脚本示例 (附录 B 的 `video.*` / `audio.*` 与时间线 `video.compose`), 常见问题与 FFmpeg 大库节 (只写 1.1.0 计划, 许可与按需下载, 不写接口); `.python/generate_markdown.py` 新增 `samples` / `permission_points` 列表与两张表的占位, 校验表头与每行列数一致且单元格不含竖线. changelog: 1.0.0 条目取代开发预览条目, 10 种语言各为提示 2 / 新增 16 / 依赖 6 (依赖照其他插件写 "附加 ..."; 阿拉伯语品牌加 FSI / PDI), 日期写 2026/11/08, P5.4 发布时按实际日期改; 本仓库没有根目录 `CHANGELOG.md`, 生成的更新日志在 `app/src/main/assets/doc/CHANGELOG*.md` (AGENTS.md). `samples/`: P1.3 的 4 个草稿按插件 1.0.0 的选项 (`docs/development/media-studio-options.d.ts`, `core/Timeline.kt` 的键与重叠规则, `core/OutputPaths.kt` 的默认文件名, 契约错误码) 逐项核对, 无需改动, 原样放入. 兼容矩阵: README "兼容性" 一节, 宿主最低版本 6641 (F10), 按 Android 版本 (9 档 x 加工 / 界面 / 录音 / 示例) 与按格式 (12 行 x 输入 / 输出) 两张表, 证据编号见附录 F "兼容矩阵证据编号".
- [x] (宿主) 宿主 `samples/` 同步样例脚本. 证据 (E3, 2026-11-08): 宿主 `app/src/main/assets-app/sample/3-Slate Studio/` 的 4 个脚本与插件 `samples/` 逐字节相同 (定稿未改内容, 宿主最后一次改动仍是 P1.3 的 `3abe772fa0`), 宿主无需新提交; 真机运行的就是这 4 个文件.
- 验收: 兼容矩阵中每一格有附录 F 的证据编号. 结果 (2026-11-08): 通过. 两张表每格为附录 F 的 F1 - F10 或 "未测" (10 种语言同一组编号, `generate_markdown.py --check` 为 MARKDOWN_OK 36). 示例真机 (F9): `.python/device_samples_check.py` (prepare / run / restore 三步; 素材由 ffmpeg 生成, 样例经 `engines.execScriptFile` 带参数原样运行, 输出拉回 PC 用 ffmpeg 核对, 结束后删默认目录新文件与 MediaStore 记录) 在 Redmi 22120RN86C (API 33; 小米平板当时被 P5.1 占用) 4 / 4 PASS: 裁剪加水印再拼接 17000 ms, 1280x720 h264 / aac; 时间线合成 15500 ms, h264 / aac, 显示 1080x1920 (编码为 1920x1080 加 -90 度旋转矩阵, media3 为编码器兼容的做法, 播放器按竖屏显示); 录音转 FLAC 5060 ms, 48 kHz 单声道 (宿主已有麦克风权限, 未弹对话框); 录屏 10 秒转 GIF 10100 ms, 宽 480, 101 帧 (屏幕捕获对话框由维护者点, E4). 宿主为 HEAD `e0eda70330` 的隔离克隆 debug 6641 (arm64 md5 d724a818), 插件为工作树 debug (versionCode 24, md5 74ac5932); 测后宿主装回 6640 原包 (md5 92f0eb09 一致), 插件卸载 (测前未装). 其余: `generate_third_party_notices.py --check` NOTICES_OK (host 2, maven 90, 与依赖锁一致); `prepush_check.py` 6 / 6 (含 `testDebugUnitTest`; 未提交改动只给提示), 另以 `--no-daemon` 运行 `assembleDebug testDebugUnitTest lintDebug`: 单元测试 254 例 0 失败, lint 0 错误 (本卡未改 Kotlin).

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
| `video.compose(timeline, output?, options?)` | 插件 | 多轨合成 (B.4): 至多 8 条视频轨与 16 条音频轨 (附录 D Q34 (c); 同时解码的实测上限与资源不足时的提示见 B.4, Q37 (a)), 7 种转场 (D30); `options` 只有 `codec` / `bitRate`; 合成音轨与 `audio.mix` 同样两遍缩放 (Q17 (c)) |
| `video.burnSubtitles(src, subtitles, style?, output?)` | 插件 | 烧录字幕 (P3.3): `subtitles` 为 SRT / ASS 文件路径或 cue 数组 (`{start, end, text}`, 毫秒, 文本可含 SRT 标记, 至多 10000 条); `style` 为文字叠加的样式键加 `background` (每行底框), `align` (锚点, 默认 bottom), `marginV` / `marginH` (像素或百分比, 默认 5 %), 另收 `codec` / `bitRate`; SRT 默认字号为画面高 / 18, 白字黑边; ASS 按自身样式 (基础子集, D31) 渲染, `style` 中给出的键覆盖每个样式 |
| `video.subtitles.parse(path)`, `video.subtitles.write(path, cues, format?)` | 插件 | 字幕文件读写 (P3.3): `parse` 返回 `{start, end, text}` 数组 (纯文本, 标记去掉; 契约版本 2 经文件送达, 不受 256 KiB 事件上限限制, 不限条数, 宿主至多读 64 MiB 的 cue JSON, 附录 D Q36 (b)); `write` 按扩展名或 `format` 写 SRT (CRLF, UTF-8) 或 ASS (1920x1080, 与烧录默认值一致的 Default 样式) |
| `video.tasks()`, `video.cancelAll()` | 宿主 | 本脚本的进行中任务 |
| `video.ffmpeg(args, options?)` | 插件 (1.1.0) | 命令透传 |

每个加工方法都有 `xxxAsync` 形态; `options.onProgress(progress)` 的 `progress` 为 `{percent, stage, etaMillis, taskId}`. 省略 `output` 时按 D14 生成路径.

### B.2 `audio` 方法

| 方法 | 实现方 | 说明 |
| --- | --- | --- |
| `audio.record(path?, options?)` | 宿主 | 开始录音 (`source`: mic / playback / mixed, `format`: m4a / wav, `sampleRate`, `channels`, `bitRate`, `maxDuration`, `onStop`), 返回 `AudioRecording` (`stop()`, `cancel()`, `status()`) |
| `audio.probe(src)` | 插件 | 音频信息 |
| `audio.trim(src, {start, end?, duration?}, output?)`, `audio.concat(sources[], output?)` | 插件 | 裁剪 / 拼接 |
| `audio.mix(inputs[], output?, options?)` | 插件 | `inputs[]` 为 `{src, volume?, at?}`; 两遍处理 (附录 D Q17 (c)): 相加后会削波时整体缩放到 -1 dBFS, `warnings[]` 报峰值与缩放量 (例如 `the mix peaked at +6.02 dBFS; it was scaled by -7.02 dB to -1 dBFS to avoid clipping`), 不会削波时保持原电平, 不放大 |
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
       "transform": {"scale": 1.0, "rotate": 0, "crop": null}},
      {"src": "/sdcard/Movies/b.mp4", "start": 2000, "end": 10000, "at": 7500,
       "transition": {"type": "dissolve", "duration": 500}}
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

定稿规则 (P3.1 / P3.2, 2026-11-06, 详见共享笔记 `3-slate-p3-notes.md`, 宿主按附录 D Q24 照抄提交时的规则):

- 顶层: `width` / `height` 16 - 8192 取偶 (省略时取第一条视频轨第一段裁切与旋转后的尺寸), `frameRate` 整数 1 - 120 (默认 30), `background` 颜色 (默认黑), `video[]` 至多 8 轨, `audio[]` 至多 16 轨 (附录 D Q34 (c)), 合计至少 1 个视频片段, 片段总数至多 256; `overlays` 同 `video.overlay` (空数组视为无叠加); `subtitles` 为 `{src | cues, style}` (P3.3, 规则同 `video.burnSubtitles`, style 不收编码键), 画在叠加物之上. 消息都带自 `timeline.` 起的字段路径, 未知键报错.
- 片段: `src` 绝对路径; `start` / `end` 为源时间 (end 不含, 省略为源结尾); `speed` 0.25 - 8 (保持音高), `volume` 0 - 16 线性; `transform` 为 `scale`, `rotate` (90 的倍数), `crop`, `flip`, `fit`; `transition` 为 `type`, `duration` 100 - 3000 ms (默认 500), `direction` (left / right / up / down, 只用于 slide / wipe). 音频片段另有 `fadeIn` / `fadeOut`.
- 定位 (探测之后): 同一轨道片段不重叠, 只有重叠转场 (dissolve / slide / wipe / zoom / blur) 让本段在上一段结尾前 `duration` 开始, 显式 `at` 必须恰好等于该位置; `fade` 经黑场, 不重叠, 两侧各占一半; 转场不得长于前后片段, 首段不能有转场. 轨道顺序即 z 顺序 (`video[0]` 最下), 叠加物在最上. 输出时长为所有片段结尾的最大值.
- GPU 回退: 着色器转场 (fade / wipe / blur) 导出失败时先把 wipe / blur 换成 dissolve, fade 换成 cut 重导一次, 仍失败则全部换成 cut, 每次在 `warnings[]` 说明.
- 同时解码的来源 (附录 D Q37 (a), 2026-11-07): 上限保持 8 / 16 轨, 但带重叠转场的视频轨在转场期间同时解码两段, 最多 8 x 2 + 16 = 32 个来源. 实测三台 Sony (G8441 API 28, XQ-AT72 API 31, XQ-DQ72 API 33) 上 6 条带重叠转场的视频轨 + 16 条音频轨 (28 个来源) 可以导出, 8 + 16 (32 个) 以编码器或解码器初始化失败 (`InsufficientResources`) 结束. 这种失败返回 `CODEC_ERROR` 与可操作的提示, 例如 `the device could not start another video encoder (ERROR_CODE_ENCODER_INIT_FAILED, InsufficientResources): this timeline decodes about 32 sources at once (8 video tracks with overlapping transitions x 2 + 0 other video tracks + 16 audio tracks); use fewer video tracks or fewer overlapping transitions`; 估算 = 带重叠转场的视频轨 x 2 + 其他视频轨 + 音频轨. 判定: 错误码为 `ERROR_CODE_ENCODER_INIT_FAILED` / `ERROR_CODE_DECODER_INIT_FAILED`, 且原因链里有资源不足迹象 (`MediaCodec.CodecException` 1100 / 1101, 或消息含 resource / no memory 字样) 或估算超过实测通过的 28.

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

## 附录 C. 契约 (按 P1.1 实际定义定稿, 2026-11-06; 契约版本 2 于 2026-11-07)

定义来源: 宿主本地提交 `ddc91edf4f` 的 `plugin-api/media-studio-api` 1.0.0 (包 `org.autojs.plugin.mediastudio.api`), 2026-11-07 由宿主本地提交 `ca938a9d89` 改为契约版本 2 (MIN / MAX 均为 2, 附录 D Q36 (b); 版本 1 未发布, 直接取代, 不保留兼容; Maven 版本仍为 1.0.0, 首次发布即带版本 2); 插件经 `libs/media-studio-api.aar` 引用 (SHA-256 `e408474a4ec85f06a27a362c00dc18b025182dd3f08c5dbccf285639379dc258`, 26,881 B, 见 `libs/README.md`), 不复制 AIDL. 与 2026-10-22 草案的差异 (`getInfo` 返回 `PluginInfo`, `getCapabilities` 居第 2, `probe` / `listTasks` 收 `Bundle`) 按附录 D Q20 保持.

### C.1 `plugin-api/media-studio-api`

```text
package org.autojs.plugin.mediastudio.api
interface IMediaStudioPlugin {
    PluginInfo getInfo();                                         // common-plugin-api 的 PluginInfo, capabilities 为静态能力表 (C.4)
    Bundle getCapabilities();                                     // 实时能力表: 静态键 + 存储授权状态 + warnings (C.4)
    int submit(in Bundle request, IMediaStudioCallback callback); // 返回 taskId; 拒绝时抛 IllegalArgumentException (INVALID_OPTIONS / UNSUPPORTED_FORMAT / LIMIT_EXCEEDED) 或 IllegalStateException (其余), 消息 "CODE: detail"
    boolean cancel(int taskId);                                   // 未知或已结束返回 false
    Bundle probe(in Bundle request);                              // op (缺省 probe, 可为 audioProbe), path; 失败返回含 errorCode / errorMessage 的 Bundle, 不抛异常
    Bundle listTasks(in Bundle request);                          // hostToken; 返回 tasks (Bundle[])
}
oneway interface IMediaStudioCallback {
    void onProgress(int taskId, in Bundle progress);  // percent, stage, etaMillis, taskId
    void onComplete(int taskId, in Bundle result);    // 附录 B.3 结果, 键见下; subtitlesParse 的 cuesFile 为文件描述符, 由宿主关闭
    void onError(int taskId, in Bundle error);        // errorCode, errorMessage (只含 detail, 不含码前缀)
}
```

- 动作与身份: `MediaStudioPluginActions.MEDIA_STUDIO` = `org.autojs.plugin.MEDIA_STUDIO` (服务 category `media-studio`, 受 `org.autojs.permission.PLUGIN` 保护), 身份服务 `org.autojs.plugin.INFO`; `MediaStudioPluginIds`: 官方 ID `three-slate-studio`, 包名 `io.github.supermonster003.autojs6.plugin.three.slate.studio`, `ENGINE` `media-studio`, `VARIANT_DEFAULT` `default`.
- 请求键 (`MediaStudioRequestKeys`): `op`, `inputs` (String[]), `output`, `options` (Bundle), `timeline` (P3), `hostToken`, `origin` (`script` / `studio`), `path` (probe), `registerMedia` (缺省 true).
- 结果键 (`MediaStudioResultKeys`): `path`, `paths`, `durationMillis`, `width`, `height`, `sizeBytes`, `mime`, `codec`, `warnings` (String[]), `cuesFile` (`subtitlesParse`, 契约版本 2: 只读 `ParcelFileDescriptor`, 内容为 UTF-8 JSON 数组 `[{"start": 毫秒, "end": 毫秒, "text": "..."}]`, 键名常量 `CUE_START` / `CUE_END` / `CUE_TEXT`; 插件在自身缓存目录写出后立即打开并删除文件, `onComplete` 发出后关闭自己的描述符; 宿主在 IO 线程读出, 至多 `MAX_CUES_FILE_BYTES` 64 MiB, 读后关闭, 迟到 / 重复 / 无人接收的结果也关闭; 两侧都不需要存储权限, 宿主不写文件), `cues` (宿主由 `cuesFile` 换成的列表, 交给脚本, 不在 Binder 上传送); 探测另有 `rotation`, `frameRate`, `bitRate`, `videoCodec`, `audioCodec`, `sampleRate`, `channels`, `hasVideo`, `hasAudio`, `tracks`, `language`; `listTasks` 的 `tasks` 每项含 `taskId`, `op`, `state`, `percent`, `stage`, `output`, `origin`, `submittedAt`. 结果与错误 Bundle 都带 `contractVersion`.
- 进度键 (`MediaStudioProgressKeys`): `percent` (0 - 100 不递减), `stage`, `etaMillis` (未知为 -1), `taskId`; 同一任务两次进度至少间隔 200 ms.
- `MediaStudioContract`: 任务状态 `queued` / `running` / `completed` / `failed` / `cancelled`; 阶段 `queued` / `probing` / `decoding` / `encoding` / `muxing` / `writing`; 模块 `video` / `audio`; `CALL_TIMEOUT_MS` 10000, `PROBE_TIMEOUT_MS` 30000 (宿主同步调用的等待上限).
- `MediaStudioLimits`: 同时编码 2, 每 token 未完成任务 4, 输入 256 个, hostToken 128 字符, 错误消息 4096 字符, 单个事件 Bundle 256 KiB (文件描述符不计入), cue 文件 64 MiB (`MAX_CUES_FILE_BYTES`), 警告阈值 2 GiB / 60 分钟, 变速 0.25 - 8, GIF 最多 600 帧 (默认 10 fps, 宽 480), 转场 100 - 3000 ms (默认 500).

### C.2 操作名 (`MediaStudioOps`)

`probe`, `trim`, `concat`, `speed`, `transform`, `overlay`, `frames`, `frame`, `thumbnail`, `toGif`, `extractAudio`, `removeAudio`, `transcode`, `compose`, `burnSubtitles`, `subtitlesParse`, `subtitlesWrite`, `audioProbe`, `audioTrim`, `audioConcat`, `audioMix`, `audioVolume`, `audioConvert`, `audioSpeed`; 1.1.0: `ffmpeg`; 1.2.0: `recordStart`, `recordStop`, `recordCancel`, `recordStatus`.

### C.3 错误码

`PLUGIN_UNAVAILABLE`, `PLUGIN_DISABLED`, `PLUGIN_INCOMPATIBLE`, `INVALID_OPTIONS`, `LIMIT_EXCEEDED`, `TASK_LIMIT`, `UNSUPPORTED_FORMAT`, `CODEC_ERROR`, `IO_ERROR`, `STORAGE_PERMISSION_DENIED`, `TASK_CANCELLED`, `TASK_NOT_FOUND`, `LIBRARY_MISSING` (1.1.0), `PERMISSION_DENIED` (1.2.0 投影 / 悬浮窗), `INTERNAL_ERROR`.

警告码 (结果的 `warnings[]`, 不中断任务, 附录 D Q6): `OUTPUT_LARGE` (输出或单输入超过 2 GB), `DURATION_LONG` (超过 60 分钟).

## 附录 D. 待决事项

格式: 编号 | 事项 | 选项 (推荐项标 "(推荐)") | 拍板时点 | 状态.

| 编号 | 事项 | 选项 | 拍板时点 | 状态 |
| --- | --- | --- | --- | --- |
| Q1 | 内录 / 混合音频的声道数 | (a) 立体声 48 kHz (推荐, 内录多为立体声, 文件约大 1 倍) / (b) 沿用 Node 的单声道 | P1.2 | 已决 (2026-11-05, 维护者选 a): Rhino `audio.record` / `video.record` 的内录 (playback) 与混合 (mixed) 默认 2 声道 48 kHz (`channels` 可改 1), AAC 默认码率 192 kbps; 麦克风与 Node 桥接的默认值保持单声道 128 kbps 不变 |
| Q2 | GIF 编码器 | (a) 自带纯 Kotlin (推荐, D32) / (b) 第三方 Apache-2.0 或 MIT 库 | P2.4 | 已决 (2026-11-06, 维护者选 a): 自带纯 Kotlin `GifEncoder` (中位切分调色板 + 可选 Floyd-Steinberg 抖动 + LZW), 不加新依赖 (P2.4 实施) |
| Q3 | 时间线预览引擎 | (a) media3 `CompositionPlayer` (推荐, 实验 API, 由 spike 决定) / (b) 自写预览 | P4.3 spike 后 | 已决 (2026-11-07, 维护者确认 spike 结论 (b)): 自写预览, 在 D34 预留的退路内, 不改其他决策. spike 数据: media3 1.10.1 `CompositionPlayer` (`MultipleInputVideoGraph`) 按导出同样的 `Composition` (透明时钟序列, 重叠转场的双车道, 叠加, 预混音) 播放到 1280x720 `ImageReader`, 只有 "单车道, 纯 cut, 不带时钟序列" 能播完; 带时钟序列约 118 帧后反复缓冲, 第二个车道 (带空档的序列) 一出画面视频输出就冻结, 与 SoC 强弱无关. 小米平板 968e9f18 (API 35): cuts+noclock 970 / 960 帧, 30.4 fps, 帧间隔 p95 48 / 最大 313 ms, dissolve+noclock 99 帧后冻结, main 卡在 13.4 s; XQ-DQ72 (API 33): cuts+noclock 1015 / 960 帧, 31.7 fps, p95 41.5 / 最大 151 ms, dissolve+noclock 102 帧后冻结, main 118 帧后缓冲 378 次 (位置 10.9 s); XQ-AT72 (API 31, XQ-DQ72 当时未连接而先测): cuts+noclock 997 帧, 31.2 fps, dissolve+noclock 101 帧后冻结, main 缓冲 430 次; G8441 (API 28, 待其他会话释放后补测): cuts+noclock 982 / 960 帧, 30.7 fps, p95 41.6 / 最大 319 ms, dissolve+noclock 101 帧后冻结, main 115 帧后缓冲 400 次 (位置 16.0 s). 自写预览 `preview/TimelinePreview.kt`: 每个车道一个 ExoPlayer (视频轨与导出一样按奇偶拆两车道) + 由 Choreographer 驱动的共享时钟 (起播缓冲时停钟至多 1 s), 转场按 `ComposeCompositorSettings` / `TransitionEffect` 同样的公式在 Compose 近似 (上层车道 alpha, slide 平移, zoom 缩放, fade 黑色遮罩, wipe 裁切, blur 在 API 31+ 用 RenderEffect), 叠加与字幕用 Compose 绘制; 单元测试 `TimelinePreviewTest` |
| Q4 | 向导目录分类 | (a) `TOOLS` (推荐, 现有分类) / (b) 新增 `MEDIA` 分类 (需宿主改向导目录与 10 语言标题) | P1.5 | 已决 (2026-11-05, 维护者选 a): `TOOLS` 现有分类 |
| Q5 | 1.1.0 FFmpeg 来源 | (a) 自建 (推荐, 许可与 16 KB 可控, 需交叉编译链) / (b) 社区分支 `dev.ffmpegkit-maintained` (省构建, 许可审查与补丁跟进由我们承担) | P6.1 | 需拍板 |
| Q6 | 单任务上限 | (a) 无硬上限, 超过 2 GB 或 60 分钟返回 warning (推荐, D29) / (b) 硬上限 4 GB | P2.1 | 已决 (2026-11-05, 维护者选 a): 无硬上限, 超 2 GB 或 60 分钟在 `warnings[]` 返回 `OUTPUT_LARGE` / `DURATION_LONG` (附录 C.3) |
| Q7 | H.265 默认 | (a) 默认 H.264, H.265 可选 (推荐, 兼容性) / (b) 设备支持时默认 H.265 | P2.2 | 已决 (2026-11-05, 维护者确认推荐项 a): 默认 H.264, `codec: "h265"` 可选, 无 H.265 编码器时 `UNSUPPORTED_FORMAT` (P2.2 已实现) |
| Q8 | Studio 录音归属 (维护者 2026-11-05 补充: Studio 应用本身支持麦克风 + 内录) | (a) 进 1.0.0, 插件自持录音内核 (推荐, D41, 条目 P4.5) / (b) 随 1.2.0 录屏增强 (P7) / (c) 不做, 只靠脚本 `audio.record` | P4.5 | 已决 (2026-11-05, 维护者选 a): 进 1.0.0, 见 D41 与 P4.5 |
| Q9 | Compose 带入原生库: `androidx.graphics:graphics-path` 1.0.1 为 4 个 ABI 打包 `libandroidx.graphics.path.so`, 与 D25 "1.0.0 APK 无原生库" 不符 (影响 D25, P0.1, README / changelog 的原生库说明, P5.4 索引 `supportedAbis`) | (a) 保留 Compose, 构建与 CI 校验 4 个文件 STORED 且 16 KB 对齐, 索引 `supportedAbis` 列 4 个 ABI, D25 改为 "仅 Compose 自带的 graphics-path" (推荐, 已先行; 改选返工: 无) / (b) 界面改用 View, 去掉 Compose (P4 全部界面重写, 返工大) | P0.1 | 已决 (2026-11-05, 维护者确认推荐项 / 保持现状 a): 保留 Compose, D25 已改为 "仅 Compose 自带的 graphics-path" |
| Q10 | D39 写 "1.0.0 依赖均为 Apache-2.0", 实际 guava 带入的 `org.checkerframework:checker-qual` 3.43.0 为 MIT (影响 D39, `THIRD_PARTY_NOTICES.md`) | (a) 声明里列出 MIT 并附全文, D39 改为 "Apache-2.0 为主, checker-qual 为 MIT" (推荐, 已先行; 改选返工: 无) / (b) 从运行时依赖排除 checker-qual (仅注解, 改依赖排除与锁文件, 返工小, 有缺类风险) | P0.1 | 已决 (2026-11-05, 维护者确认推荐项 / 保持现状 a): 声明列出 MIT 并附全文, D39 已改 |
| Q11 | `release.yml` 只把 APK, `SHA256SUMS` 与索引清单草稿存为 workflow artifact, 不自动建 GitHub Release (影响 P0.3, P5.4) | (a) 保持, 建 Release 与索引登记在 P5.4 手动做 (推荐, 已先行; 改选返工: 无) / (b) 照 ImGui 由 tag 工作流自动建 Release 并上传附件 (加一个发布步骤与 `contents: write`, 返工小) | P5.4 | 已决 (2026-11-07, 维护者选: A 保持手动建 Release); 2026-11-05 已由 3-slate-p0 先行, 无返工 |
| Q12 | H.265 默认码率 (影响 P2.2 `transcode` / `concat` / `speed` / `transform` 的默认值, 附录 B.1, 脚本文档) | (a) H.264 估算值的 0.6 倍, 1080p30 为 4.8 Mbps (推荐, 已先行; 同画质下 HEVC 约省 40%; 改选返工: 无) / (b) 与 H.264 相同 (1080p30 8 Mbps, 改 `BitRates.estimateVideo` 一行与单测 / 文档, 返工小) | P2.2 | 已决 (2026-11-06, 维护者选 a): 保持 H.264 估算值的 0.6 倍 (1080p30 4.8 Mbps), 已实施. 3-slate-p2-1-2 当时的附录 F 基准: 平滑内容时 H.264 只有 1.57 - 4.15 Mbps (目标 8), H.265 为 2.15 - 4.78 Mbps (目标 4.8), 反而比 H.264 大; 每帧跳位的最坏内容时两台设备的硬件编码器都超出目标 2 - 5 倍; 0.6 倍的目标没有让 H.265 输出稳定变小 |
| Q13 | HyperOS 默认限制新装应用后台 (`RUN_ANY_IN_BACKGROUND ignore`), 系统静默忽略 `startForeground()`: 长任务照常完成, 但没有前台服务与进度通知, 熄屏或内存紧张时可能被杀 (影响 P2.1 前台服务, P4.1 设置页引导) | (a) 保持: 能力表 `mediaStudioWarnings` 报后台受限 (API 28+ `isBackgroundRestricted`), P4.1 设置页加 "电池无限制" 引导 (推荐, 警告已先行; 改选返工: 无) / (b) 另加首次运行即弹的电池设置引导页 (新 Activity + 10 语言文案, 返工小) | P4.1 | 已决 (2026-11-06, 维护者选 a): 保持能力表警告, P4.1 设置页加 "电池无限制" 引导 (已写进 P4.1 条目) |
| Q14 | 图片类输出 (`frames` / `frame` / `thumbnail` / `toGif`) 省略输出路径时的默认位置 (D14 只规定了视频与音频) | (a) `/sdcard/Pictures/3-Slate Studio/`, 名称 `<源名>-<操作>-<时间戳>.<ext>`, `frames` 为同名文件夹 (推荐, 已先行; Pictures 是系统的图片标准目录, 相册按图片归类; 改选返工: 无) / (b) 与视频相同放 `/sdcard/Movies/3-Slate Studio/` (改 `OutputKind` 三处与单测, 宿主默认输出同步改, 返工小) | P2.4 | 已决 (2026-11-06 19:45, 维护者选 a): 保持 `/sdcard/Pictures/3-Slate Studio/`, 已实施 |
| Q15 | `frames` 单次抽帧上限 (D32 与契约只规定了 GIF 的 600 帧) | (a) 1000 帧, 超出报 `LIMIT_EXCEEDED` 并提示加大 `every` 或设 `count` (推荐, 已先行; 1080p 自然画面 PNG 每帧约 2 - 4 MB, 1000 帧约 2 - 4 GB; 改选返工: 无) / (b) 不设上限, 只按 Q6 在超过 2 GB 时返回 warning (删一处检查与单测, 返工小) / (c) 与 GIF 相同 600 帧 (改一个常量) | P2.4 | 已决 (2026-11-06 19:45, 维护者选 a): 保持 1000 帧上限, 已实施 |
| Q16 | 音频操作 (`audioTrim` 等 6 个) 既没有 `options.format` 也没有输出扩展名 (省略输出路径) 时的输出格式 | (a) m4a (AAC), 与默认输出 `Music/3-Slate Studio/<源名>-<操作>-<时间戳>.m4a` 一致 (推荐, 已先行; 所有设备都有 AAC 编码器, 体积小; 改选返工: 无) / (b) 沿用第一个输入的扩展名 (输入为 mp3 / mp4 等无法写出的容器时仍要回退, 改 `audioOutput` 与宿主默认输出, 返工小) | P2.5 | 已决 (2026-11-06 19:45, 维护者选 a): 保持 m4a, 已实施 |
| Q17 | `audioMix` 相加超出 16 位范围时的处理 | (a) 直接相加后硬削波, `warnings[]` 报削波样本比例并建议调低 `tracks[].volume` (推荐, 已先行; 与常见混音器的默认一致, 不改动未削波的电平; 改选返工: 无) / (b) 各路自动除以输入数 (永不削波, 但两路时整体低 6 dB, 改一处与单测) / (c) 两遍: 先求峰值再整体缩放到 -1 dBFS (读两遍输入, 耗时约翻倍) | P2.5 | 已决 (2026-11-06 19:45, 维护者选 c, 非推荐): 两遍处理, 先求相加后的峰值, 超过 -1 dBFS 时整体缩放到 -1 dBFS, 未超出时不缩放 (不放大, 保持原电平); `warnings[]` 改报实际缩放量; P3.1 多轨合成的混音共用同一实现. 硬削波 (a) 已由 3-slate-p3-1-2 返工替换, 规则见附录 B.2 与 P2.5 条目 |
| Q18 | `audioMix` 的输入数上限 (契约 `MAX_INPUTS` 为 256) | (a) 16 路, 每路一个解码器同时运行, 超出 `LIMIT_EXCEEDED` (推荐, 已先行; 设备硬件解码器实例通常在 16 - 32 个, 超出会随机失败; 改选返工: 无) / (b) 至多 256 路, 超过 16 路时先逐路解码到临时 PCM 文件再相加 (磁盘约 10 MB / 分钟 / 路, 返工中: 约半天, 含宿主预检) | P2.5 | 已决 (2026-11-06 19:45, 维护者选 a): 保持 16 路, 已实施 |
| Q19 | `audioVolume` 的 `gain` 与 `audioMix` 的 `tracks[].volume` 的单位 | (a) 线性倍数 0 - 16 (1 为原样, 0.5 约 -6 dB) (推荐, 已先行; 与附录 B.4 时间线 `volume` (示例 1.0 / 0.4) 的写法一致; 改选返工: 无) / (b) 分贝 (-60 - +24 dB, 改选项名为 `gainDb` 或换算, 宿主同步改, 返工小) | P2.5 | 已决 (2026-11-06 19:45, 维护者选 a): 保持线性倍数 0 - 16, 已实施 |
| Q20 | 媒体工作室契约 AIDL 与附录 C 草案的差异: 实际为 `PluginInfo getInfo()`, `getCapabilities()` 居第 2, `probe` / `listTasks` 收 `Bundle` | (a) 保持实际定义, 按 speech / alarm 惯例 (推荐) / (b) 改回草案签名 (宿主契约返工) | P1.1 | 已决 (2026-11-05, 维护者确认推荐项 / 保持现状 a): 附录 C 已按实际定义改写 (2026-11-06) |
| Q21 | Node 纯音频录音 (recorder 模块) 改用内核 `AudioRecorder` 后的两处行为变化: 目标文件改为 stop 时由同目录临时文件移入 (以前 start 即创建并截断目标); MediaRecorder 出错原因由 `recorder-error-<what>-<extra>` 统一为 `recorder-error` | (a) 保持 (推荐, 失败或取消不留文件, 与 P1.3 Rhino 录音一致) / (b) 恢复旧行为 | P1.2 | 已决 (2026-11-05, 维护者选 a): 保持, P1.5 changelog 写明 |
| Q22 | `video` / `audio` 1.0.0 是否只登记 Rhino 模块 (api-coverage 策略 `media-studio-rhino-only` 标 not-portable) | (a) 保持, 插件引擎桥接以后按需另立条目 (推荐) / (b) 1.0.0 同时桥接 Node 与 Bun | P1.3 | 已决 (2026-11-06, 维护者选 b, 非推荐): 1.0.0 同时桥接 Node 与 Bun, 新增宿主条目 P1.6, api-coverage 策略改 portable (批次 3 宿主卡片 3-slate-p1-6-7) |
| Q23 | `video.record` 复用脚本的截图会话 (先 `images.requestScreenCapture`; 录屏期间 `images.captureScreen` 读录屏当前画面) | (a) 保持 (推荐, 一次授权, 截图与录屏不抢投影) / (b) 录屏单独申请 MediaProjection | P1.3 | 已决 (2026-11-06, 维护者选 a): 保持复用截图会话 |
| Q24 | 宿主只对插件已冻结的操作做选项预检 (`MediaStudioOptionRules.CHECKED_OPS`), 其余操作先由插件单独校验 | (a) 保持, P2.3 - P3.3 各卡片实现新操作时把规则抄进宿主并单独本地提交 (推荐) / (b) 宿主现在就按附录 B 草案校验全部操作 | P1.3 | 已决 (2026-11-06, 维护者选 a) |
| Q25 | `video.frame` 的传帧方式 | (a) 插件把帧写成默认输出目录里的隐藏临时图片 `.frame-<uuid>.<format>` (不登记媒体库), 宿主解码为 `Image` 后删除 (推荐, 不受 Binder 大小限制) / (b) 经 Binder 直接传 Bitmap 字节 (受 1 MB 事务限制) | P1.3, P2.4 | 已决 (2026-11-06, 维护者选 a) |
| Q26 | `video` / `audio` 的文档站, d.ts 与 Ace 补全的时点 | (a) 留到插件 P5.1 / P5.2 (推荐, API 随 P2 / P3 还会扩充; api-coverage `deferredNameGate` 暂时放行) / (b) 宿主卡片现在就写 | P1.3 | 已决 (2026-11-06, 维护者选 a) |
| Q27 | 加工方法必需参数之后的参数映射 | (a) 按类型映射: 字符串 = 输出路径, null / undefined = 默认输出, 对象 = 选项, 函数 = `onProgress` (推荐, 与附录 B.1 写法一致) / (b) 只接受 (输入, 选项) 两参 | P1.3 | 已决 (2026-11-06, 维护者选 a) |
| Q28 | `CREATE_SCRIPT` 的代码上限: Intent 按 UTF-16 传, 只限 512 KB 时 25 万字符以上的 ASCII 代码会让发送方把宿主杀掉 | (a) 加 200000 字符上限, 发送方先自查 (推荐) / (b) 只保留 512 KB, 协议文档提醒 / (c) 大代码改经 content URI 传 (契约增字段, 插件需 FileProvider) | P1.4 | 已决 (2026-11-06, 维护者选 c, 非推荐): 大代码经 content URI, 内联 `code` 保留 512 KiB / 200000 字符上限; 新增宿主条目 P1.7, 插件 P4.4 加 FileProvider |
| Q29 | 附录 B.4 时间线 JSON 中 `transition` 的位置 (示例写在前一段上, 说明写 "写在后一段上") | (a) 按说明写在后一段上, 改示例 (推荐, 与 "at 省略时紧接前一段" 一致, 首段没有转场) / (b) 按示例写在前一段上, 改说明 | P3.1 前 | 已决 (2026-11-06, 维护者选 a): 附录 B.4 示例已改 (2026-11-06, 3-slate-p2-3-5) |
| Q30 | P1.6 的 Node / Bun 门面何时写 (两个运行时插件仓库当时被 3-prism 的门面会话独占, 宿主没有注入门面的通道) | (a) 串行: 先做宿主侧桥接与 P1.7, 仓库空闲后由同一卡片或续接卡写门面, 声明, 测试并做 Node / Bun 真机验收 (推荐) / (b) 门面与真机验收拆为新条目另开卡 / (c) 与 3-prism 门面会话并行改同一仓库 | P1.6 | 已决 (2026-11-06 19:45, 维护者选 a): 门面已由 3-slate-p1-6-7 续做完成 (Node Runtime `0e81048e`, Bun Runtime `f0d85db`), 见 P1.6 证据 |
| Q31 | `CREATE_SCRIPT` URI 模式的读取上限 | (a) 4 MiB UTF-8 (推荐, 内联上限 512 KiB 的 8 倍, 超过 512 KiB 走编辑器大文件模式) / (b) 16 MiB / (c) 与内联相同 512 KiB | P1.7 | 已决 (2026-11-06 19:45, 维护者选 a): 保持 4 MiB, 插件 P4.4 发送前按此自查 |
| Q32 | Bun 里 `video.record` / `audio.record` 用哪组授权 (Bun 新授权 `mediastudio.control` 默认关闭, 只放行加工与任务管理) | (a) 复用 Node 录屏授权: `video.record` 需 screen.capture + screen.record, 麦克风需 screen.record.microphone, 内录需 screen.capture + screen.record.playback (推荐) / (b) 新增 mediastudio.record 授权 / (c) 只要 mediastudio.control 即可录制 | P1.6 | 已决 (2026-11-06 19:45, 维护者选 a): 保持, P5 文档授权表按此写 |
| Q33 | `video.record` 省略 `width` / `height` 时默认用截图尺寸取偶数, 在要求 16 对齐或有宏块上限的编码器上必失败 (Redmi 22120RN86C 屏幕 720x1650, 默认调用报 `INVALID_OPTIONS: No H.264 encoder supports 720x1650`, 显式 720x1648 正常) | (a) 默认尺寸按编码器能力取: 查 `MediaCodecList` 的 `VideoCapabilities`, 向下取到对齐倍数, 超上限时等比缩小; 显式宽高不受支持时仍报 `INVALID_OPTIONS` 并在消息里给出可用尺寸 (推荐) / (b) 默认尺寸固定向下取 16 的倍数, 不查编码器 / (c) 保持, 只在文档写明 | P1.3 | 已决 (2026-11-06 19:45, 维护者选 a): 宿主 `core/record` 一处修改, Rhino / Node / Bun 共用, 由宿主卡片 3-slate-q33 实施; 插件录屏 / 录音内核副本 (D41, P4.5 / P7) 实现时按同一规则取默认尺寸. 已实施 (2026-11-06, 3-slate-q33): 宿主本地提交 `95b3d8ab42` (规则 `core/record/RecordingVideoSizes.kt`, Rhino / Node / Bun 与旧 `screen.record` 共用, 协议文档 "Recording size") 与 `0d5e37d97d` (以编码器 `areSizeAndRateSupported` 为准, 上限按两个方向取宽松者), 未推送; 隔离克隆单测 81 例 0 失败; Redmi 22120RN86C 两轮 `device_mediastudio_check.py --record --runtimes node,bun` 各 41 PASS / 0 FAIL, 第 2 轮 Rhino / Node / Bun 默认尺寸均为 720x1648 (屏幕 720x1650), 显式 718x1648 报 INVALID_OPTIONS 并给出最近可用尺寸 704x1648 |
| Q34 | `compose` 时间线的轨道上限 (契约只限 256 个输入) | (a) 视频轨至多 4 条, 音频轨至多 8 条, 超出 `LIMIT_EXCEEDED` (推荐, 已先行; 重叠转场时每条视频轨同时解码 2 段, 4 x 2 + 8 = 16 个来源同时发声, 与 `audioMix` 的 16 路解码器上限 (Q18) 一致; 改选返工: 无) / (b) 不限轨道数, 改为检查任一时刻同时发声的来源不超过 16 (要在探测后逐时刻统计, 宿主无法预检, 返工小) / (c) 视频 8 条, 音频 16 条 (同时解码可达 32 个, 低端设备易随机失败) | P3.1 | 已决 (2026-11-06 23:10, 维护者选 c, 非推荐): 视频轨至多 8 条, 音频轨至多 16 条, 超出 `LIMIT_EXCEEDED`; 由批次 5 卡片 3-slate-p3-3 返工 (插件 `core/Timeline.kt` 与宿主 `MediaStudioOptionRules` 常量, 单测, 附录 B.1 / B.4, 协议文档上限表). 风险: 重叠转场时同时解码可达 8 x 2 + 16 = 32 个来源, 是 `audioMix` 16 路 (Q18) 的两倍, 低端设备可能因编解码器资源随机失败; P2.6 在 G8441 跑 8 视频轨 + 16 音频轨极限用例, 结果见附录 F |
| Q35 | 宿主是否预检 `subtitlesParse` / `subtitlesWrite` 的选项 (Q24 要求插件冻结的操作把规则抄进宿主; 这两个操作没有默认输出, 输出扩展名为字幕格式, 宿主 `MediaStudioRequests` 目前按视频处理未校验操作的扩展名) | (a) 不预检, 只由插件校验 (推荐, 已先行; 两个操作无编码器, 失败即时返回, 宿主改动需要动 `MediaStudioRequests` / `MediaStudioOutputPaths` 的输出类型, 超出本卡的宿主范围) / (b) 宿主另开卡片: 增加字幕输出类型 (`.srt` / `.ass`, 补 `format`), 预检 cue 数组与 `format`, `subtitlesParse` 不填默认输出 (约 1 小时, 宿主 3 个文件 + 单测) | P3.3 | 已决 (2026-11-07 08:05, 维护者选: (a) 不预检, 只由插件校验; 保持 2026-11-06 先行的现状): `burnSubtitles` 与 `timeline.subtitles` 已抄进宿主, `subtitlesParse` / `subtitlesWrite` 不抄, 无返工 |
| Q36 | `subtitlesParse` 的结果超过契约事件上限 256 KiB (约 1500 条中等长度的 cue, 一部长片的字幕可能超出) | (a) 超出时 `LIMIT_EXCEEDED` 并在消息中建议拆分文件或直接交给 `burnSubtitles` / 时间线 (推荐, 已先行; 不改契约) / (b) 契约 v2: 结果改为插件写出的 JSON 临时文件路径, 宿主读出后删除 (契约, 宿主, 文档与声明都要改, 约半天) / (c) 结果改为紧凑数组 (`starts` long[], `ends` long[], `texts` String[]), 约省 40 %, 仍有上限 (契约字段变化, 宿主映射改, 约 2 小时) | P3.3 | 已决 (2026-11-07 08:05, 维护者选: (b) 契约 v2 临时文件). 已实施 (2026-11-07, 3-slate-q36-q38): 交换方式为结果 Bundle 的 `cuesFile` (只读 `ParcelFileDescriptor`, 不传路径: 宿主在 Android 11+ 读不到插件私有目录, 公共目录又要存储权限). 插件把 UTF-8 JSON 数组 `[{"start", "end", "text"}]` 写进自身缓存 `cache/subtitles-parse/`, 打开只读描述符后立即删除文件, `onComplete` (oneway, 内核在发送时已持有文件引用) 发出后关闭自己的描述符, 引擎启动时清理残留; 宿主在 IO 线程读出 (至多 64 MiB, `MAX_CUES_FILE_BYTES`) 并换成 `cues` 列表, 读后关闭, 迟到 / 重复 / 无人接收的结果也关闭; 两侧都不写共享存储, 不需要权限; 删去 256 KiB 的 `LIMIT_EXCEEDED` 分支, 不限条数 (cue 文件超过 64 MiB 时才 `LIMIT_EXCEEDED`). 契约版本 1 -> 2 (MIN / MAX = 2, 不兼容版本 1, 版本 1 未发布), 宿主本地提交 `ca938a9d89` (契约, `MediaStudioCueFiles`, `MediaStudioPluginHost`, 协议文档), 新 AAR 见附录 C; 插件提交见 P3.3 的 "提交" 行; 插件 changelog 的 "契约版本 1" 改为 2 (10 种语言). 验证: 宿主 mediastudio 单测 59 例 0 失败 (含 `MediaStudioCueFilesTest` 6 例: 键序与类型, 6000 条, 上限, 畸形 JSON, IO 错误, 结果替换), 契约模块 10 例 0 失败; 插件 JVM 184 例 0 失败 (含 `SubtitleCueFilesTest` 4 例), AVD_API_35_16K 仪器 55 例 0 失败 (含 6000 条 cue 经 Binder 逐条一致且缓存无残留); 小米平板 (API 35, 克隆宿主 6641) `device_regression.py --cues 2000,6000` 两个文件的 cue 逐条与生成规则一致 (482 / 768 ms), 插件 `cache/subtitles-parse` 与宿主缓存都无残留 |
| Q37 | `compose` 8 视频轨 + 16 音频轨 (Q34 (c), 8 条视频轨都带溶解转场, 即 16 个视频解码器同时运行) 在三台 Sony 上都失败, 不只是低端机: G8441 (API 28, 骁龙 835) 与 XQ-AT72 (API 31, 骁龙 865) 为编码器初始化 `InsufficientResources` (`ERROR_CODE_ENCODER_INIT_FAILED`, 约 27 / 32 秒后), XQ-DQ72 (API 33, 骁龙 8 Gen 1) 为解码器初始化失败 (`ERROR_CODE_DECODER_INIT_FAILED`, 约 9 秒后); 都以 `CODEC_ERROR` 结束, 无残留文件; 三台的 6 视频轨 + 16 音频轨都通过 (P2.6, 附录 F; 小米平板 / OnePlus / Redmi 未跑极限用例) | (a) 保持静态 8 / 16, 编码器或解码器因资源不足失败时把消息改为可操作的提示 (同时解码的来源过多, 减少轨道或重叠转场), 文档写明实测上限 (三台 Sony 为 6 条带转场的视频轨) (推荐; 插件约 1 小时, 不改契约与宿主, 尊重 Q34 (c) 的决定) / (b) 按设备能力动态收紧: 提交后按 `MediaCodecInfo.getMaxSupportedInstances` 与同时解码路数估算, 超出时 `LIMIT_EXCEEDED` (插件约半天; 宿主无法预检, 估算偏差可能误拒能跑的时间线) / (c) 资源不足时自动降级重试 (重叠转场改为 cut 以减少同时解码路数) 并 warning (插件约半天, 输出效果与请求不同) / (d) 静态上限改为视频 6 轨 (音频仍 16 轨; 插件与宿主常量, 单测, 附录 B / 协议文档约 30 分钟; 不带转场的 7 - 8 条视频轨本可运行, 会被一并拒绝) | P2.6 | 已决 (2026-11-07 08:05, 维护者选: (a) 保持 8 / 16, 资源不足时给出可操作提示). 已实施 (2026-11-07, 3-slate-q36-q38): `core/CodecResources.kt` 与 `ComposeOperation` (判定, 估算与消息见附录 B.4; 协议文档上限表写明实测上限), JVM `CodecResourcesTest` 4 例. 真机: XQ-DQ72 (API 33) `--extreme` 8 + 16 以 `CODEC_ERROR: the device could not start another video encoder (ERROR_CODE_ENCODER_INIT_FAILED, InsufficientResources): this timeline decodes about 32 sources at once (8 video tracks with overlapping transitions x 2 + 0 other video tracks + 16 audio tracks); use fewer video tracks or fewer overlapping transitions` 结束 (此前该机为解码器先失败, 本次为编码器), 6 + 16 通过, 26 / 27; G8441 (API 28, 验收指定机型) `--extreme` 8 + 16 约 28 秒后以同一提示 (`ERROR_CODE_ENCODER_INIT_FAILED, InsufficientResources`, 估算 32 个来源) 结束, 6 + 16 通过, 26 / 27. XQ-AT72 未复跑 (被 3-Chime chime-p5 占用到收尾, 此前为同样的编码器初始化失败) |
| Q38 | 编码器能力表偏保守时 media3 回退改变输出尺寸: Redmi 22120RN86C 的 MTK 编码器 (`c2.mtk.avc.encoder` 等, 对齐 16x16) 与谷歌软件编码器对 1920x1080 / 640x360 的 `isSizeSupported` 都答 false, media3 (`setEnableFallback(true)`) 改用 1920x1088 / 640x368, 画面按比例居中, 上下各加 4 像素黑边; 只改宽度时 (360x640 -> 368x640) 连 warning 都没有 (`TransformationRequest` 只有高度). 用探测脚本直接以 1920x1080 / 640x360 / 360x640 配置这些编码器都能启动, 输出格式也是请求尺寸 | (a) 先按请求尺寸配置编码器, 配置或启动失败才走 media3 回退 (包装 `DefaultEncoderFactory`, 码率与编码格式的回退不变), 回退时 warning 写明前后宽高 (推荐; 插件约 2 - 3 小时含真机验证, 影响全部重编码的视频操作) / (b) 保持 media3 回退 (16 对齐 + 黑边), 只补上宽度变化的 warning (比较输出与请求尺寸, 约 1 小时) / (c) 回退时改为铺满后裁切, 不加黑边 (仍为非标准尺寸或损失 8 行画面, 约 2 小时) | P2.6 | 已决 (2026-11-07 08:05, 维护者选: (a) 先按请求尺寸配置编码器, 失败才走 media3 回退). 已实施 (2026-11-07, 3-slate-q36-q38): `pipeline/RequestedSizeEncoderFactory.kt` 包装 media3 `DefaultEncoderFactory`: 带回退的工厂照旧选编码器, MIME 与码率; 它改了尺寸时释放该编码器, 用不带回退, 只含同名编码器且同码率的工厂按请求尺寸重建, 尺寸一致即用; 仍不行才保留回退尺寸, 并在 `warnings[]` 写明前后宽高 (例如 `the encoder c2.mtk.avc.encoder did not run 1920x1080; the video was encoded at 1920x1088, the frame fitted inside`, 只改宽度时也报); `TransformerPipeline` 的 media3 回退提示只保留编码格式变化. JVM `CodecResourcesTest` 覆盖提示文本. 真机: Redmi 22120RN86C `device_regression.py` 25 / 25 (此前 23 / 25): 1080p H.264 / H.265 输出 1920x1080, `transform` 360x640, `concat` / `speed` / `removeAudio` / `overlay` / `compose` / `burnSubtitles` 640x360, 无尺寸 warning; 复查小米平板 27 / 27 (25 项 + Q36 两项) 与 XQ-DQ72 26 / 27 (只差 8 + 16 极限用例, 见 Q37); 回归脚本现在按精确尺寸检查这些输出 |
| Q39 | `video.frame` 不写文件却需要插件有全部文件访问权限: 宿主把临时图片放在公共 `Movies/3-Slate Studio/.frame-<uuid>.png` (P1.3 `Video.kt`), 插件没有该权限时 `STORAGE_PERMISSION_DENIED` (P2.6 首轮在 G8441 / 小米平板 / Redmi 复现; 宿主在 Android 11+ 读不到插件私有目录, 不能改放那里) | (a) 保持, 文档 (P5.1) 写明 `video.frame` 与默认输出一样需要全部文件访问 (推荐; 无代码改动, 插件首次使用本就引导授权) / (b) 契约 v2: 宿主在自身缓存目录打开文件, 把 `ParcelFileDescriptor` 随请求交给插件写入, 无需任何存储权限 (契约, 宿主, 插件与文档都要改, 约半天) | P5.1 | 已决 (2026-11-07 08:05, 维护者选: (a) 保持, P5.1 文档写明 `video.frame` 与默认输出一样需要全部文件访问): 无代码改动, 由 P5.1 落实. 附带修正 (2026-11-07, 3-slate-q36-q38, 宿主本地提交 `094844ef86`): 不支持的 `options.format` 原先报出隐藏临时文件名 `.frame-<uuid>.webp` 的扩展名错误, 改为 `video.frame writes png, jpg or jpeg images, got options.format webp` (Rhino 与 Node / Bun 共用 `MediaStudioRequests.frameExtension`, 单测覆盖) |
| Q50 | D37 设置项中的 "硬件编码器偏好" 与 "任务并发数" (执行器的并发上限是契约常量 `MediaStudioLimits.MAX_CONCURRENT_ENCODES` = 2, 编码器由 media3 `DefaultEncoderFactory` 与 Q38 的 `RequestedSizeEncoderFactory` 选择; 两项都要改 core / pipeline, 批次 6 时另一张卡正在改编码器工厂, 本卡不改 core) | (a) 1.0.0 不提供这两项, 设置页只列默认输出目录, 默认编码, 码率策略, 媒体库目录 (推荐, 已先行; 改选返工: 无) / (b) 1.0.0 补上两项: 编码器偏好 (自动 / 优先硬件 / 优先软件) 经 media3 `EncoderSelector`, 并发数 1 - 2 经 `TaskRunner` (插件 core + pipeline + 设置页 + 11 种语言文案, 约半天; 并发越高越易遇到 Q37 的编解码器资源不足) / (c) 只补编码器偏好 (约 2 小时) | P4.1 | 已决 (2026-11-07 10:05, 维护者选: (b) 补上两项, 非推荐): `core/ProcessingSettings` (存于 Studio 偏好文件; 编码器偏好 auto / hardware / software, 并发数 1 - 2, 默认 2, 契约上限 `MAX_CONCURRENT_ENCODES` 仍为 2); 编码器偏好经 media3 `EncoderSelector` 接入 `RequestedSizeEncoderFactory` (偏好的一类排前, 其余仍作回退, 偏好不会让导出失败), 并发数经 `TaskRunner` 的并发数提供者 (改设置后 `limitsChanged()` 立即生效, 运行中的任务不受影响); 对 Studio 任务与脚本任务都生效; 设置页两行与两个对话框, 并发说明写明并发越高越易遇到 Q37 的编解码器资源不足 (CODEC_ERROR); 17 条文案 x 11 个目录; 单测 `ProcessingSettingsTest` 2 例与 `TaskRunnerTest` 并发 1 例; 提交 `035c879` |
| Q51 | 任务队列页的 "失败原因" 只有 Studio 自己提交的任务有: 脚本提交的任务失败原因经回调交给宿主, 插件 `TaskRunner` 的任务快照只有状态, 不保存错误码与消息 | (a) 1.0.0 保持: 脚本任务在队列里只显示 "失败" 状态, 原因在脚本侧 (推荐, 已先行; 改选返工: 无) / (b) `TaskRunner` 快照保存最近的错误码与消息 (只给 Studio 进程内读取, 不进契约), 队列页对脚本任务也显示 (插件 core 约 1 小时 + 单测) | P4.1 | 已决 (2026-11-07 10:05, 维护者选: (b), 非推荐): `TaskRunner.TaskSnapshot` 增 `errorCode` / `errorMessage` (最近一次失败的错误码与详情, 只在 Studio 进程内读取, 不进契约), `StudioTasks` 的脚本任务行带出, `TasksScreen` 对脚本任务也显示失败原因; 单测 `TaskRunnerTest` 1 例 (CODEC_ERROR, INTERNAL_ERROR, TASK_CANCELLED, 完成时为空); 提交 `035c879` |
| Q52 | 详情页的 "用 MediaInfo 查看" 入口: MediaInfo 2.1.4 (`io.github.supermonster003.autojs6.plugin.mediainfo`) 只有唤醒 Activity 与契约服务, 没有接受 `ACTION_VIEW` 的查看界面, 入口无处可跳 | (a) 入口按 `resolveActivity` 判断, MediaInfo 将来提供查看器 (接受 `ACTION_VIEW` + 媒体 URI) 时自动出现, 目前不显示; Manifest `queries` 已声明该包 (推荐, 已先行; 改选返工: 无) / (b) 给 MediaInfo 插件加一个查看器 Activity (另一个仓库, 约半天, 需单独立项与发布) / (c) 去掉该入口 (改 P4.1 条目与 D18 措辞) | P4.1 | 已决 (2026-11-07 10:05, 维护者选: (a) 保持现状): `DetailScreen` 按 (a) 实现 (3-slate-p4-1-2), 不再改动 |
| Q53 | 脚本 `video.frames` 的 `times` 数组被宿主预检拒绝: 宿主 `MediaStudioValues` 把全为整数的数组规整为 `LongArray` (含小数时为 `DoubleArray`), `MediaStudioOptionRules.numbers()` 只接受 `List`, 报 `INVALID_OPTIONS: options.times must be an array of numbers`, 任务到不了插件 (Rhino / Node / Bun 共用这条路径; P4.2 向导真机比对 `device_wizard_check.py` 中发现, 向导在插件进程内提交, 不受影响) | (a) 宿主卡片修 `numbers()` (连同其他读数字数组的规则) 接受 `LongArray` / `DoubleArray`, 补单测, 再用 `.python/device_wizard_check.py` 复测 frames 一致 (推荐; 宿主 1 个文件约 30 分钟, 不改契约与插件) / (b) 改 `MediaStudioValues`, 数字数组保持 `List`, 写 Bundle 时再转数组 (所有数组选项的写入路径都受影响, 约 1 小时) | P1.3 | 已决 (2026-11-07 10:05, 维护者选: (a)): 宿主 `549ff67974` (只本地提交): `MediaStudioOptionRules.numbers()` 接受 List / Array / LongArray / DoubleArray 及其他基本类型数组, 逐项错误消息不变; 单测 `MediaStudioOptionRulesTest.framesTakeTheNormalizedNumberArrays` 与 `MediaStudioScriptArgsTest` 三例, 协议文档写明数字数组以 long[] / double[] 到达; 隔离克隆 6641 debug 中 mediastudio 包 58 例 0 失败; XQ-AT72 复测 `device_wizard_check.py` 20 / 20 (frames 一致, 见 P4.2 结果) |
| Q60 | 阿拉伯语 (RTL) 下时间线剪辑器的方向: 时间线画布, 预览画面与播放控件 (播放 / 暂停, 时间, 分割与缩放按钮) 镜像与否 | (a) 这三处保持从左到右, 其余界面 (工具栏, 检查器, 对话框) 照常镜像 (推荐; Material 双向规范: 时间轴, 媒体进度与播放控件不镜像, 与导出画面和其他剪辑软件一致, 预览上的叠加位置也与输出相同) / (b) 时间线整体镜像 (时间从右往左, 拖动与裁剪方向随之反转, 预览叠加坐标要换算; 约半天, 需重做 RTL 截图) | P4.3 | 已决 (2026-11-07, 维护者选: (a) 保持); 3-slate-p4-3 已先行: `TimelineView`, `TimelinePreviewFrame` 与播放控件在 `LayoutDirection.Ltr` 下绘制; XQ-DQ72 阿拉伯语截图核对 |
| Q90 | 插件文档页的位置: P5.1 条目写 `plugins/threeSlateStudio.md`, 文档站没有 `api/plugins/` 子目录, 已有的插件页 (`autojs4Runtime.md`, `composeDesigner.md` 等) 都是顶层页, 侧栏与链接按页名引用 | (a) 放顶层 `api/threeSlateStudio.md`, 与其他插件页一致 (推荐, 已先行; 改选返工: 无) / (b) 新建 `api/plugins/` 子目录放插件页 (生成器, 侧栏, 页间链接与 Offline Docs 路径都要核对, 已有插件页是否一起迁移另议; 返工约 1 小时) | P5.1 | 待决 (2026-11-08, 3-slate-p5-1 按 (a) 先行, 文档站提交 `465f7a9`) |
| Q91 | D40 与 P5.1 写 `video.md` / `audio.md` "10 种语言", 文档站 api 页只有简体中文 (所有模块页如此, 没有多语言页面体系), 10 种语言只在 changelog 与 README 的生成源 | (a) 页面用简体中文, 10 种语言落在 changelog JSON 与生成的 README / CHANGELOG, 与其他模块页一致 (推荐, 已先行; 改选返工: 无) / (b) 为两页另写 9 种语言版本 (文档站需新建多语言页面, 侧栏与生成器都要改, 超出本卡, 约数天) | P5.1 | 待决 (2026-11-08, 3-slate-p5-1 按 (a) 先行, 文档站提交 `465f7a9`) |

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

基线更新 (2026-11-05, 取代上文宿主部分):

- 宿主 6.8.0 已于 2026-11-03 公开, 公开历史经过重写, 上文 2026-10-22 基线中的宿主 hash (`63d1cb259`, `bd522f8a7`, `e8f8edb7a`) 已失效, 只作历史.
- 宿主 AutoJs6 当前 `master` `340a0b511e` (2026-11-05), `VERSION_NAME` 6.8.1 (未发布), `VERSION_BUILD` 6640; 宿主的 `VERSION_BUILD` 就是完整的 versionCode (不再有偏移, `COMMON_AGENTS.md` 构建号一节), P1 的宿主改动随 6.8.1.
- `common-plugin-api` 1.1.1 (宿主 `1676e7489d` 的 release AAR, 含设置迁移契约 v1), P0.1 入库; `media-studio-api` 1.0.0 是 P1.1 的正式 AAR (宿主本地提交 `ddc91edf4f`, 契约版本 1, AIDL 与附录 C 草案的差异见看板 Q20), P0.1 直接入库, 未用临时 AAR (见 `libs/README.md` 与 `locks/host-api-aars.lock`).
- 基线更新 (2026-11-07, 附录 D Q36 (b)): `libs/media-studio-api.aar` 换为契约版本 2 的 release AAR (宿主本地提交 `ca938a9d89`, 在 `1f877537e6` + 本改动的隔离克隆中构建; Maven 版本仍为 1.0.0, 首次发布即带版本 2), SHA-256 `e408474a4ec85f06a27a362c00dc18b025182dd3f08c5dbccf285639379dc258`, 26,881 B (`locks/host-api-aars.lock` 与 `libs/README.md` 同步; 取代契约版本 1 的 `66c31dee...`, 26,749 B); 共享笔记目录留有同名副本与 `.sha256`.

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

P2.2 转码基准 (E3, 2026-11-06, 3-slate-p2-1-2): `py .python/benchmark_transcode.py --serial <serial>` (仪器测试 `TranscodeBenchmarkTest`, 仅在 `-e benchmark true` 时运行). 源文件在设备上生成: 1920x1080, 60 秒, 30 fps, 440 Hz AAC 音调; 经 Binder 以默认选项 `transcode` 为 H.264 (目标 8 Mbps) 与 H.265 (目标 4.8 Mbps, Q12), 码率按输出视频样本计算; 再提交三份拼接 (`concat`), 13 秒时检查前台服务, 然后取消.

| 设备 | 源 | H.264 耗时 / 码率 / 大小 | H.265 耗时 / 码率 / 大小 | 13 秒时的前台服务 |
| --- | --- | --- | --- | --- |
| XQ-DQ72 (API 33), 平移纹理 | 2.98 Mbps | 19.8 s (3.0 倍速) / 4.15 Mbps / 32.2 MB | 19.1 s (3.1 倍速) / 4.78 Mbps / 37.0 MB | 前台 (两次转码都已在前台) |
| 小米平板 (API 35), 平移纹理 | 1.48 Mbps | 9.0 s (6.7 倍速) / 1.57 Mbps / 12.9 MB | 8.5 s (7.1 倍速) / 2.15 Mbps / 17.2 MB | 不足 10 秒, 未启动 |
| XQ-DQ72 (API 33), 每帧跳位 (最坏情况, 构建 4) | 26.16 Mbps | 18.6 s (3.2 倍速) / 15.77 Mbps / 119.4 MB | 18.5 s (3.2 倍速) / 13.29 Mbps / 100.8 MB | 前台; 取消后 606 ms 服务停止 |
| 小米平板 (API 35), 每帧跳位 (最坏情况, 构建 4) | 26.33 Mbps | 12.6 s (4.8 倍速) / 24.10 Mbps / 181.9 MB | 13.3 s (4.5 倍速) / 24.31 Mbps / 183.5 MB | 普通运行 (后台受限, 见下); 取消后 517 ms 服务停止 |

- 两台设备都有 H.264 与 H.265 硬件编码器 (能力表 `mediaStudioVideoEncoders` = `h264`, `h265`); 输出均为 1920x1080, 时长 60.00 / 60.05 秒.
- 码率: 平滑内容时编码器远低于目标 (VBR); 每帧跳位时两台设备都超出目标 2 - 5 倍, 生成源用的 MediaCodec (请求 16 Mbps) 同样给出 26 Mbps, 看来是硬件编码器到了量化上限. 自然内容 (摄像头素材) 的码率留 P2.6 记录.
- 小米平板 (HyperOS) 新装应用默认 `RUN_ANY_IN_BACKGROUND ignore`, 系统日志 `Service.startForeground() not allowed due to bg restriction`, 不抛异常: 任务照常完成, 但没有前台服务与通知 (附录 D Q13). 能力表 `mediaStudioWarnings` 此时报后台受限, 基准脚本把这一项记为 `RESTRICTED` 而非失败. XQ-DQ72 无此限制. 小米平板复跑一次 (00:34, 改正警告文本的分隔符后): H.264 13.7 s, H.265 13.5 s, 码率与上表相同, 取消后 230 ms 服务停止, 警告全文输出正确.
- 两台设备都保留 debug APK `1.0.0 (4)` (基准脚本事后卸载测试 APK, 并删除缓存里的源与输出).

性能基线表 (P2.6, E3, 2026-11-07, 3-slate-p3-3; Q36 - Q38 返工后的复测见各行 "返工后" 与表下说明): `py -3.12 .python/device_regression.py --serial <serial> [--extreme]`; 宿主为隔离克隆 debug 6641 (含 `630ba2a200`), 插件 debug 1.0.0 (10). 源为 ffmpeg `testsrc2` 1920x1080 30 fps 60 秒 (H.264 8 Mbps, 440 Hz AAC), 内容与 P2.2 的平移纹理不同, 倍速只在本表内可比; 耗时为脚本侧 (含 Binder 往返) 默认选项转码, 倍速 = 源时长 / 耗时. "回归" 为 25 个操作的通过数, 三台 Sony 另加极限用例 (8 + 16 失败后再跑 6 + 16, 计 27 项).

| 设备 | API | 硬件视频编码器 | 1080p 60 秒 H.264 | H.265 | 回归 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| Sony G8441 (`BH900ASK9E`) | 28 | `OMX.qcom.video.encoder.avc`, `OMX.qcom.video.encoder.hevc` | 40.3 s (1.49 倍速) | 可用, 30.4 s (1.98 倍速) | 26 / 27 | 8 视频轨 + 16 音频轨极限用例失败 (`ERROR_CODE_ENCODER_INIT_FAILED`, 编码器 `InsufficientResources`), 逐级减少后 6 + 16 通过 (附录 D Q37). 返工后 (2026-11-07): 26 / 27, 8 + 16 以 Q37 的可操作提示失败 (估算 32 个来源), 6 + 16 通过; 25 项均按精确尺寸检查通过; H.264 44.4 s (1.35 倍速), H.265 30.3 s (1.98 倍速) |
| Sony XQ-AT72 (`QV710AF65F`) | 31 | `c2.qti.avc.encoder`, `OMX.qcom.video.encoder.avc`, `OMX.qcom.video.encoder.hevc` (另有 `.cq`) | 9.6 s (6.28 倍速) | 可用, 9.6 s (6.25 倍速) | 26 / 27 | 8 + 16 极限用例 `ERROR_CODE_ENCODER_INIT_FAILED`, 6 + 16 通过 (Q37) |
| Sony XQ-DQ72 (`QV770340J7`) | 33 | `c2.qti.avc.encoder`, `OMX.qcom.video.encoder.avc`, `c2.qti.hevc.encoder` (另有 `.cq`), `OMX.qcom.video.encoder.hevc` (另有 `.cq`) | 19.3 s (3.11 倍速) | 可用, 19.1 s (3.14 倍速) | 26 / 27 | 8 + 16 极限用例 `ERROR_CODE_DECODER_INIT_FAILED`, 6 + 16 通过 (Q37); 倍速与 P2.2 基准 (3.0 / 3.1) 一致. 返工后 (2026-11-07): 26 / 27, 8 + 16 以 Q37 的可操作提示失败 (本次为编码器 `ERROR_CODE_ENCODER_INIT_FAILED`, 估算 32 个来源), 6 + 16 通过; H.264 19.2 s (3.12 倍速), H.265 19.0 s (3.16 倍速) |
| Redmi 22120RN86C (`bek749scrwv4wo8h`) | 33 | `c2.mtk.avc.encoder`, `OMX.MTK.VIDEO.ENCODER.AVC`, `c2.mtk.hevc.encoder`, `OMX.MTK.VIDEO.ENCODER.HEVC` | 33.1 s (1.81 倍速) | 可用, 36.1 s (1.66 倍速) | 25 / 25 | 返工后 (2026-11-07, Q38 (a)) 的结果: 1080p H.264 / H.265 输出 1920x1080, `transform` 360x640, `concat` / `speed` / `removeAudio` / `overlay` / `compose` / `burnSubtitles` 640x360, 无尺寸 warning. 首轮 (同日 3-slate-p3-3) 为 23 / 25: 编码器能力表对 1920x1080 / 640x360 答不支持, media3 回退为 1920x1088 / 640x368 / 368x640 带黑边, 两个 1080p 转码未通过; 首轮倍速 34.5 s (1.74) / 38.8 s (1.55) |
| Xiaomi Pad 23046RP50C (`968e9f18`) | 35 | `c2.qti.avc.encoder`, `OMX.qcom.video.encoder.avc`, `c2.qti.hevc.encoder` (另有 `.cq`), `OMX.qcom.video.encoder.hevc` (另有 `.cq`) | 8.3 s (7.22 倍速) | 可用, 8.2 s (7.31 倍速) | 25 / 25 | 返工后 (2026-11-07): 27 / 27 (25 项均按精确尺寸检查, 另加 Q36 的 2000 / 6000 条 cue 解析, 482 / 768 ms, 缓存无残留); H.264 8.3 s (7.20 倍速), H.265 8.2 s (7.29 倍速) |
| OnePlus OPD2413 (`63751f63`) | 35 | 同小米平板, 另有 `c2.qti.hevc.encoder.hdr` | 6.9 s (8.75 倍速) | 可用, 6.6 s (9.13 倍速) | 25 / 25 | ColorOS 首次安装插件要维护者在安装引导页确认 |

- 首轮 (30 秒源) 暴露的脚本问题均已修正后重跑, 上表为修正后的结果: `frames` 返回路径数组; Android 11+ 宿主读不到插件私有目录, 检查一律用返回值; `video.frame` 的临时图片在公共目录, 运行期间授予存储权限 (Q39); 小米平板 (Android 15) 的 shell 删不掉插件写进私有目录的文件 (`run-as` 也进不了 `/storage`), 输出改放 `/sdcard/Download/slate-regression`, 残留已在插件进程内删除; 删除的文件夹 (API 28 上还有文件) 仍留 MediaStore 记录, 驱动改为按路径删除记录. 返工复测 (2026-11-07, 3-slate-q36-q38): 脚本改为按精确尺寸检查保持尺寸的输出 (Q38), 因此 Redmi 首轮未计为失败的 640x368 / 368x640 现在也会被查出; 三台 Sony 中 XQ-AT72 被 3-Chime 占用, 未复跑. 补跑两台 Sony 时又发现: XQ-DQ72 (Android 13) 上 `adb push` 直接写进插件目录会在设置属主时失败 (`remote fchown() failed`, 文件随即被删), 驱动改为先推到 `/data/local/tmp` 再由 shell 复制; 插件在插件中心被关闭时脚本只输出一行原因后结束, 不再逐项报 `PLUGIN_DISABLED`.

兼容矩阵证据编号 (P5.3, 2026-11-08): README "兼容性" 一节两张表的每一格写本表的编号或 "未测". 编号只汇总已有条目的证据, 数值与命令见出处.

| 编号 | 证据 | 等级 | 设备 (API) | 出处 |
| --- | --- | --- | --- | --- |
| F1 | 1080p 60 秒素材经 Binder 默认选项转码为 H.264 与 H.265, 记录耗时, 码率与前台服务 | E3 | XQ-DQ72 (33), 小米平板 23046RP50C (35) | P2.2, 本附录 "P2.2 转码基准" |
| F2 | 插件仪器测试: 全部加工操作在彩条 / 音调夹具上的输出 (MP4 H.264 / H.265, M4A / WAV / FLAC / OGG, GIF, PNG / JPG, 叠加, 时间线与 7 种转场, SRT / ASS 读写与烧录), 与 JVM 单测 | E1 + E2 | AVD_API_35_16K (35) | P2.1 - P2.5, P3.1 - P3.3 各条验收 |
| F3 | 真机回归 `.python/device_regression.py`: 经宿主脚本跑 25 个操作 (含 H.265 转码, 四种音频格式, GIF, 抽帧, PNG 叠加, compose, SRT 烧录, ASS 写出), 按精确尺寸检查 | E3 | G8441 (28), XQ-AT72 (31), XQ-DQ72 (33), Redmi 22120RN86C (33), 小米平板 (35), OnePlus OPD2413 (35) | P2.6, 本附录 "性能基线表" |
| F4 | 界面检查 `.python/device_ui_check.py`: 媒体库, 详情, 工具箱, 修剪向导, 任务, 设置, 每页查进程与崩溃 | E2 + E3 | AVD_API_24 (24), AVD_API_35_16K (35), XQ-AT72 (31), 小米平板 (35), OnePlus OPD2413 (35) | P4.1 验收 |
| F5 | 工具箱 20 个向导各跑一次; 真机上与等价脚本的输出比对 (20 / 20) | E2 + E3 | AVD_API_24 (24), AVD_API_35_16K (35), XQ-AT72 (31) | P4.2 验收 |
| F6 | 时间线剪辑器: 30 秒工程 (3 段素材, 转场, 音乐, 导入 8 条 SRT 字幕, 文字 / 图片叠加) 导出并与脚本 `video.compose` 比对, 撤销 / 重做, 工程往返; API 24 基本操作 | E2 + E3 | XQ-DQ72 (33), 小米平板 (35), AVD_API_24 (24) | P4.3 验收 |
| F7 | 代码生成与 "在 AutoJs6 中打开" (内联与 content URI 两种发送) | E1 + E3 | XQ-AT72 (31) | P4.4 验收 |
| F8 | Studio 录音: 麦克风 / 内录保存为 M4A 并探测, 熄屏录音, ColorOS 授权 (对话框由维护者点) | E2 + E3 + E4 | G8441 (28, 仅麦克风), XQ-AT72 (31), XQ-DQ72 (33), OnePlus OPD2413 (35), AVD_API_35_16K (35) | P4.5 验收 |
| F9 | `samples/` 的 4 个示例经宿主原样运行, 输出拉回 PC 用 ffmpeg 核对 (`.python/device_samples_check.py`) | E3 + E4 | Redmi 22120RN86C (33) | P5.3 验收 |
| F10 | 宿主最低构建 6641: 含契约 `media-studio-api` 与 `video` / `audio` 模块的宿主本地构建 (6.8.1 未发布, P5.4 回填公开构建号, D23) | E1 + E3 | XQ-AT72 (31), 小米平板 (35) | P1.3, P1.5 |

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
