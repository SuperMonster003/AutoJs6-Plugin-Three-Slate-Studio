<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>视频与音频加工, 多轨时间线剪辑, 转场与字幕, 界面操作可生成脚本代码</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言 (Languages)

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### 简介

******

3-Slate Studio 是 AutoJs6 的视频与音频插件, 也是不装 AutoJs6 也能单独使用的剪辑应用. 它可以裁剪, 拼接, 变速, 缩放, 旋转, 裁切视频并加水印, 抽帧, 转 GIF 与转码; 可以裁剪, 拼接, 混音, 淡入淡出与转换音频; 多轨时间线剪辑器把片段与转场, 叠加和字幕合成为一个视频.

加工在插件内用 Jetpack Media3 Transformer 与设备的硬件编解码器完成, AutoJs6 宿主因此保持精简. AutoJs6 脚本通过全局模块 `video` 与 `audio` 加工媒体; 录屏 (`video.record`) 与录音 (`audio.record`) 内置于 AutoJs6, 不装插件也能使用. 插件的每个工具界面都会显示等价的脚本代码, 可以复制, 也可以在 AutoJs6 中新建为脚本.

> 1.0.0 是首个正式版本. 在脚本中加工媒体需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本; 插件界面, 时间线剪辑器与 Studio 录音不依赖 AutoJs6.

******

### 功能亮点

******

- 视频剪辑: 按时间裁剪, 拼接, 变速 (0.25 到 8 倍), 缩放, 旋转, 裁切与翻转, 文字或图片水印, 抽取或去除音轨, 转码 (默认 H.264, 设备有 H.265 编码器时可选 H.265).
- 抽帧与 GIF: 单帧, 帧序列与缩略图, 可保存为 PNG 或 JPG 文件, 也可返回 `Image` 对象; 内置编码器生成 GIF 动图.
- 音频: 裁剪, 拼接, 多路混音 (相加会削波时自动缩放), 音量调整与淡入淡出, 变速, 转换为 M4A, WAV, FLAC 或 OGG (OGG 需要 Android 10 及以上).
- 多轨时间线剪辑器: 视频, 音频, 叠加与字幕轨, 7 种转场 (切换, 淡入淡出, 叠化, 推移, 擦除, 缩放, 模糊), 实时预览, 撤销与重做, 工程保存为 `.slate.json`; 字幕支持 SRT 导入导出与 ASS 基础子集, 导出时烧录进画面.
- 工具箱: 每个加工功能都有向导页, 表单给出默认值与取值范围; 任务在队列中运行, 显示进度与失败原因, 长任务带前台通知.
- 代码生成: 每个向导, 时间线导出与录音都会生成等价的 `video.*` / `audio.*` 脚本, 可以复制, 也可以在 AutoJs6 中新建为脚本.
- Studio 录音: 在插件界面中录制麦克风, 设备内部声音 (Android 10 及以上) 或两者混合, 保存为 M4A 或 WAV, 可以暂停, 熄屏时继续录制.
- FFmpeg 大库 (计划于 1.1.0): 按需下载, 增加 MP3, WebM 与 MKV 等格式以及完整的 ASS 字幕渲染.

******

### 界面截图

******

以下截图来自 Android 12 与 13 手机, 浅色主题, 简体中文界面.

| 媒体库 | 工具箱 | 时间线剪辑器 | Studio 录音 |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/library.png?raw=true" alt="library" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/toolbox.png?raw=true" alt="toolbox" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/timeline.png?raw=true" alt="timeline" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/recorder.png?raw=true" alt="recorder" width="200" /> |

******

### 使用方法

******

1. 在 AutoJs6 的插件中心安装 3-Slate Studio, 也可以从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) 下载 APK 安装. 所有 ABI 共用一个安装包.
2. 打开插件, 按提示授予所有文件访问权限, 以便读写你选择的路径.
3. 如需在脚本中使用 `video` 与 `audio` 的加工方法, 请使用 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本, 并在插件中心启用本插件.

> 插件支持 Android 7.0 (API 24) 及以上设备. 唯一的原生库来自 Jetpack Compose, 已按 16 KB 内存页对齐, 因此所有 ABI 共用一个安装包.

******

### 脚本 API

******

脚本通过全局模块 `video` 与 `audio` (也可写作 `$video` 与 `$audio`) 使用这些功能 (全部方法与选项见 AutoJs6 文档的 [video](https://docs.autojs6.com/#/video) 与 [audio](https://docs.autojs6.com/#/audio) 章节):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }]);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });
toastLog(video.toGif(joined.path, { fps: 10, width: 480 }).path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }]);

images.requestScreenCapture();
let recording = video.record({ audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.compose({
    video: [{ clips: [
        { src: "/sdcard/Movies/a.mp4", end: 8000 },
        { src: "/sdcard/Movies/b.mp4", start: 2000, end: 10000, transition: { type: "dissolve", duration: 500 } },
    ] }],
    audio: [{ clips: [{ src: "/sdcard/Music/bgm.m4a", end: 15500, volume: 0.4, fadeIn: 1000, fadeOut: 2000 }] }],
    overlays: [{ type: "text", text: "AutoJs6", from: 1000, to: 4000, x: "50%", y: "10%", fontSize: 48 }],
});

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- 每个加工方法都有阻塞形态与返回 Promise 的 `xxxAsync` 形态; `options.onProgress(progress)` 报告 `percent`, `stage` 与 `etaMillis`, 停止脚本即取消其任务.
- 省略 `output` 时, 视频输出到 `Movies/3-Slate Studio/`, 音频输出到 `Music/3-Slate Studio/`, 抽帧, 缩略图与 GIF 输出到 `Pictures/3-Slate Studio/`, 文件名由源文件名, 操作与时间组成; 结果带实际路径与媒体信息, 并登记到媒体库.
- `video.compose(timeline)` 渲染以对象描述的多轨时间线 (至多 8 条视频轨与 16 条音频轨, 片段, 转场, 叠加, 字幕), 与时间线剪辑器导出的格式相同.
- `video.record` 与 `audio.record` 由 AutoJs6 录制, 省略路径时分别保存到 `Movies/AutoJs6/` 与 `Music/AutoJs6/`; 录屏前要先调用 `images.requestScreenCapture`.
- `video.tasks()` / `audio.tasks()` 列出并可取消本脚本的进行中任务; 失败时抛出 `MediaStudioError`, 其 `code` 为稳定的错误码, 例如 `INVALID_OPTIONS` 或 `STORAGE_PERMISSION_DENIED`.

******

### 示例脚本

******

仓库的 [samples](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/tree/master/samples) 目录有 4 个可以直接运行的示例, AutoJs6 自带的示例代码中也有同样的脚本 (3-Slate Studio 目录):

- 录屏 10 秒转 GIF: 用 `video.record` 录制无声屏幕, 再用 `video.toGif` 转为每秒 10 帧, 宽 480 像素的 GIF (首次运行时需要确认屏幕捕获).
- 录音转 FLAC: 用 `audio.record` 录制麦克风, 再用 `audio.convert` 转为无损 FLAC (首次运行时需要授予录音权限).
- 裁剪加水印再拼接: `video.probe` 读取素材信息, `video.trim` 截取前 5 秒, `video.overlay` 加文字水印, `video.concat` 拼接并报告进度.
- 时间线合成: `video.compose` 把两段视频 (叠化转场), 背景音乐 (淡入淡出) 与文字叠加合成为一段竖屏视频.

后两个示例默认读取 `/sdcard/Movies/a.mp4`, `/sdcard/Movies/b.mp4` 与 `/sdcard/Music/bgm.m4a`, 请先放好素材或改成你自己的路径.

******

### 兼容性

******

插件支持 Android 7.0 (API 24) 及以上, 不装 AutoJs6 也能单独使用. 在插件中心启用本插件, 在脚本中使用 `video` 与 `audio` 的加工方法需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本 (F10); 这两个模块可在 Rhino, Node.js 与 Bun 引擎中使用.

表中括号里的编号指向 [开发路线图附录 F](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md) 的证据条目; "未测" 表示 1.0.0 发布前没有在该版本或格式上验证, 不代表不能使用.

**按 Android 版本**

| Android | 验证设备 | 加工 | 插件界面 | Studio 录音 | 示例脚本 |
| --- | --- | --- | --- | --- | --- |
| 7.0 (API 24) | Android 模拟器 | 通过 (F5) | 通过 (F4, F6) | 未测 | 未测 |
| 7.1 - 8.1 (API 25 - 27) | - | 未测 | 未测 | 未测 | 未测 |
| 9 (API 28) | Sony G8441 | 通过 (F3) | 录音页通过 (F8) | 通过, 仅麦克风 (F8) | 未测 |
| 10 - 11 (API 29 - 30) | - | 未测 | 未测 | 未测 | 未测 |
| 12 (API 31) | Sony XQ-AT72 | 通过 (F3, F5) | 通过 (F4, F5, F7) | 通过, 含内录 (F8) | 未测 |
| 12L (API 32) | - | 未测 | 未测 | 未测 | 未测 |
| 13 (API 33) | Sony XQ-DQ72, Redmi 22120RN86C | 通过 (F1, F3) | 通过 (F6, F8) | 通过, 含内录 (F8) | 通过 (F9) |
| 14 (API 34) | - | 未测 | 未测 | 未测 | 未测 |
| 15 (API 35) | Xiaomi Pad 23046RP50C, OnePlus OPD2413, Android 模拟器 | 通过 (F1, F2, F3, F5) | 通过 (F4, F5, F6) | 通过, 含内录 (F8) | 未测 |

加工: 25 项脚本操作或 20 个工具箱向导; 插件界面: 媒体库, 工具箱, 任务, 设置与时间线剪辑器; 示例脚本: 上文的 4 个示例.

**按格式**

| 格式 | 作为输入 | 作为输出 | 说明 |
| --- | --- | --- | --- |
| MP4: H.264 视频 + AAC 音频 | 通过 (F2, F3) | 通过 (F2, F3) | 视频的默认输出格式 |
| MP4: H.265 / HEVC 视频 | 未测 | 通过 (F1, F2, F3) | 设备有 H.265 编码器时才能选择, 实测的 6 台手机与平板都有 |
| M4A (AAC) | 通过 (F2, F3) | 通过 (F2, F3, F8) | 音频的默认输出格式, 也是 Studio 录音的默认格式 |
| WAV (16 位 PCM) | 通过 (F2) | 通过 (F2, F3) | 无损, 文件较大 |
| FLAC | 未测 | 通过 (F2, F3) | 无损压缩 |
| OGG (Opus) | 未测 | 通过 (F2, F3) | 需要 Android 10 及以上 (系统从 Android 10 起才能写出 OGG), 实测为 Android 12 - 15 |
| GIF 动图 | 未测 | 通过 (F2, F3) | 内置编码器, 每秒 1 - 50 帧 |
| PNG, JPG 图片 | 通过 (F2, F3) | 通过 (F2, F3) | 输入为叠加用的图片, 输出为抽帧与缩略图 |
| SRT 字幕 | 通过 (F2, F3, F6) | 通过 (F2) | 可导入时间线剪辑器, 也可烧录进画面 |
| ASS 字幕 (基础子集) | 通过 (F2) | 通过 (F2, F3) | 不支持的标签会被忽略并给出警告; 完整的 ASS 渲染计划于 1.1.0 |
| MP3, AMR 等其他音频 | 未测 | 1.0.0 不提供 | 能否读取取决于设备的系统解码器; MP3 输出计划随 1.1.0 的 FFmpeg 大库提供 |
| WebM, MKV | 未测 | 1.0.0 不提供 | 计划随 1.1.0 的 FFmpeg 大库提供 |

******

### 常见问题

******

#### 不装 AutoJs6 能用吗?

可以. 媒体库, 工具箱, 时间线剪辑器与 Studio 录音都能单独使用, 只有在脚本中加工媒体时才需要 AutoJs6.

#### 为什么插件要申请所有文件访问权限?

脚本传入的是 `/sdcard/DCIM/...` 这样的普通文件路径, 插件与 3-Maple Image 一样直接读写这些路径. 未授权时加工会报告 `STORAGE_PERMISSION_DENIED`, 插件界面会说明如何授权. `video.frame` 虽然返回 `Image` 对象, 也要经 `Movies/3-Slate Studio/` 中的临时图片交给 AutoJs6, 因此同样需要该权限.

#### 录屏需要安装插件吗?

不需要. `video.record` 与 `audio.record` 内置于 AutoJs6 (6.8.1 或更高版本); 插件提供加工方法与自己的 Studio 录音.

#### 我的 AutoJs6 版本较早, 可以使用吗?

在脚本中使用本插件需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本, 更早的 AutoJs6 还没有 `video` 与 `audio` 模块. 插件本身仍可作为独立的剪辑应用使用.

#### 时间线导出报告 `CODEC_ERROR` 并提示减少轨道, 怎么办?

带重叠转场 (叠化, 推移, 擦除, 缩放, 模糊) 的视频轨在转场期间要同时解码两段素材, 而设备的编解码器数量有限. 实测的三台 Sony 手机能导出 6 条带转场的视频轨加 16 条音频轨, 8 + 16 会以 `CODEC_ERROR` 结束; 请减少视频轨或重叠转场.

******

### 权限与安全

******

插件申请的权限与用途:

- 所有文件访问 (`MANAGE_EXTERNAL_STORAGE`, Android 10 及以下为存储读写权限): 读写脚本与界面指定的文件路径, 并把未指定路径的输出写入 `Movies`, `Music` 与 `Pictures` 下的 `3-Slate Studio` 目录.
- 读取媒体 (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, Android 13 及以上): 在首页的媒体库中列出设备上的视频与音频.
- 录音 (`RECORD_AUDIO`): 只用于 Studio 录音; 脚本中的 `audio.record` 与 `video.record` 由 AutoJs6 录制, 使用 AutoJs6 自己的录音权限.
- 屏幕捕获 (系统的投屏确认, 不是清单中的权限): Studio 录音采集设备内部声音 (内录或混合) 时需要, 开始录音时由系统询问.
- 通知 (`POST_NOTIFICATIONS`, Android 13 及以上): 显示加工进度与录音通知 (带暂停与停止按钮); 不授予时不显示这些通知.
- 前台服务与唤醒锁 (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `WAKE_LOCK`): 让长时间的加工在后台继续, 熄屏时录音继续.
- 网络 (`INTERNET`, `ACCESS_NETWORK_STATE`): 1.0.0 只用于检查更新 (读取官方插件索引), 打开首页时最多每 12 小时检查一次, 可在设置页关闭.
- AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`): 与 AutoJs6 通信.

插件在设计上尽量收窄权限面:

- 受保护的入口: 能力服务与激活入口都受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 其他应用无法直接调用.
- 存储: 所有文件访问权限只用于脚本与 Studio 界面指定的路径; 未指定路径的输出只写入 `Movies`, `Music` 与 `Pictures` 下的 `3-Slate Studio` 目录.
- 录音: 只有 Studio 录音进行中才采集麦克风与设备内部声音, 并显示前台通知; 采集设备内部声音还需要系统的屏幕捕获确认.
- 网络: 1.0.0 只用于检查更新; 计划中的 FFmpeg 大库 (1.1.0) 只在你请求时下载, 加载前校验 SHA-256.

******

### FFmpeg 大库

******

FFmpeg 大库计划于 1.1.0 提供, 按 LGPL-2.1-or-later 构建. 大库不打包进 APK: 只在你请求时按设备 ABI 下载并动态加载, 随附自己的许可声明与对应源码的获取位置. 不下载大库时, 1.0.0 的全部功能照常可用. 大库支持的格式与脚本接口以 1.1.0 发布时的说明为准.

> FFmpeg 支持的部分编解码器 (例如 H.265 / HEVC 与 AAC) 在某些国家或地区可能受专利保护. 大库仅为方便使用而提供, 不附带任何专利许可; 使用前请确认适用于你的规则.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者, 宿主通过这些标识发现插件并完成能力协商:

```text
application id: io.github.supermonster003.autojs6.plugin.three.slate.studio
plugin id: three-slate-studio
engine: media-studio
variant: default
info service: org.autojs.plugin.INFO (category three-slate-studio)
capability service: org.autojs.plugin.MEDIA_STUDIO (category media-studio)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libandroidx.graphics.path.so of Jetpack Compose only (arm64-v8a, armeabi-v7a, x86_64, x86; 16 KB aligned); a single APK for every ABI
```

******

### 开发路线图

******

功能规划, 设计决策与完成情况以可勾选清单维护在 ROADMAP.md 中. 未勾选条目表示规划意向, 不代表当前版本能力.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/11/08_

- `提示` 在脚本中使用 video / audio 的加工方法需要 AutoJs6 6.8.1 (内部版本号 6641) 或更高版本; 插件界面, 时间线剪辑器与 Studio 录音不依赖 AutoJs6
- `提示` 脚本 API 参阅 AutoJs6 文档的 [video](https://docs.autojs6.com/#/video) 与 [audio](https://docs.autojs6.com/#/audio) 章节
- `新增` 视频加工: 裁剪, 拼接, 变速 (0.25 - 8 倍), 缩放, 旋转, 裁切与翻转, 文字或图片水印, 抽取或去除音轨, 转码 (H.264, 设备支持时可选 H.265)
- `新增` 抽帧, 缩略图与 GIF: 保存为 PNG / JPG 文件或返回 Image 对象, 内置 GIF 编码器 (调色板, 抖动, 循环)
- `新增` 音频加工: 裁剪, 拼接, 混音 (会削波时自动缩放), 音量与淡入淡出, 变速, 转换为 M4A / WAV / FLAC / OGG (OGG 需要 Android 10+)
- `新增` 多轨合成 video.compose: 至多 8 条视频轨与 16 条音频轨, 7 种转场, 文字 / 图片叠加与字幕
- `新增` 字幕: SRT 读写与 ASS 基础子集, 可用 video.burnSubtitles 烧录进画面, 用 video.subtitles.parse / write 读写字幕文件
- `新增` AutoJs6 脚本模块 video / audio (需要 AutoJs6 6.8.1, 支持 Rhino, Node.js 与 Bun): 每个加工方法都有 xxxAsync 形态与进度回调, 停止脚本即取消其任务
- `新增` Studio 界面: 媒体库, 工具箱, 任务队列与设置页; 外观跟随 AutoJs6, 也可单独选择语言, 夜间模式与主题色; 设置页引导授予所有文件访问与电池无限制
- `新增` 工具箱向导: 每个加工功能一个向导页, 表单带默认值与取值范围, 结果页显示输出, 媒体信息与等价脚本
- `新增` 时间线剪辑器: 多轨编辑, 转场, 叠加与字幕编辑, 实时预览, 撤销 / 重做, 工程保存为 .slate.json, 导出进入任务队列
- `新增` 代码生成: 向导, 时间线与录音页都给出等价的完整脚本, 可复制或在 AutoJs6 中打开为新脚本
- `新增` Studio 录音: 麦克风, 设备内部声音 (Android 10+) 或两者混合, 保存为 M4A / WAV, 支持暂停 / 继续, 熄屏录音与通知栏控制
- `新增` 长任务在前台服务中运行并显示进度通知; 设置页可选择硬件编码器偏好与同时运行的任务数 (1 - 2)
- `新增` 省略输出路径时, 视频, 音频与图片分别保存到 Movies, Music 与 Pictures 下的 3-Slate Studio 目录, 并登记到媒体库
- `新增` 更新检查: 读取官方插件索引, 打开首页时最多每 12 小时检查一次, 可忽略某个版本
- `新增` 所有 ABI 共用一个 APK: 唯一的原生库来自 Jetpack Compose, 已按 16 KB 内存页对齐
- `新增` 10 种语言的界面, README, 插件中心说明与更新日志
- `依赖` 附加 Jetpack Media3 版本 1.10.1 (Transformer, Effect, Muxer, ExoPlayer 与 UI, 用于加工与预览)
- `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件界面)
- `依赖` 附加 AndroidX Activity Compose 版本 1.13.0
- `依赖` 附加 Navigation Compose 版本 2.10.2
- `依赖` 附加 AndroidX Core KTX 版本 1.18.0
- `依赖` 附加 Material Color Utilities (MaterialKolor) 版本 4.1.1 (插件界面的主题色)

##### 更多发行历史可参阅

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建

******

本节面向希望从源码构建插件的开发者.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### 许可

******

项目代码使用 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE). 第三方组件与数据的许可见第三方声明.

- [第三方声明 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
