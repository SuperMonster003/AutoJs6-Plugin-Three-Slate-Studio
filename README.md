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

> 当前为开发预览版: 插件可以被 AutoJs6 插件中心识别, 但加工功能, Studio 界面与 `video` / `audio` 模块的加工方法尚未交付. 下文描述的是各版本的设计目标, 进度见开发路线图.

******

### 功能亮点

******

- 视频剪辑: 按时间裁剪, 拼接, 变速 (0.25 到 8 倍), 缩放, 旋转, 裁切与翻转, 文字或图片水印, 抽取或去除音轨, 转码 (默认 H.264, 设备支持时可选 H.265).
- 抽帧与 GIF: 单帧, 帧序列与缩略图, 可保存为图片文件或返回 `Image` 对象; 内置编码器生成 GIF 动图.
- 音频: 裁剪, 拼接, 多路混音, 音量调整与淡入淡出, 变速, 转换为 M4A, WAV, FLAC 或 OGG.
- 多轨时间线: 视频, 音频与叠加多轨, 7 种转场 (切换, 淡入淡出, 叠化, 推移, 擦除, 缩放, 模糊), 字幕支持 SRT 导入导出与 ASS 基础子集, 导出时烧录进画面.
- 代码生成: 每个工具界面与时间线导出都会生成等价的 `video.*` / `audio.*` 代码, 可直接复制, 也可在 AutoJs6 中新建为脚本.
- Studio 录音: 在插件界面中录制麦克风, 设备内部声音 (Android 10 及以上) 或两者混合.
- FFmpeg 大库 (1.1.0): 按需下载, 提供 MP3, WebM 与 MKV 输出, `video.ffmpeg` 命令透传与完整的 ASS 渲染.

******

### 使用方法

******

1. 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) 下载 APK 并安装. 所有 ABI 共用一个安装包.
2. 打开插件, 按提示授予所有文件访问权限, 以便读写你选择的路径.
3. 如需在脚本中使用 `video` 与 `audio` 的加工方法, 请把 AutoJs6 升级到内部版本 6641 (6.8.1) 或更高, 并在插件中心启用本插件.

> 插件支持 Android 7.0 (API 24) 及以上设备. 唯一的原生库来自 Jetpack Compose, 已按 16 KB 内存页对齐, 因此所有 ABI 共用一个安装包.

******

### 脚本 API

******

脚本通过全局模块 `video` 与 `audio` (也可写作 `$video` 与 `$audio`) 使用这些功能 (1.0.0 设计草案, 以发布版本的文档为准):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }], null);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });

let gif = video.toGif(joined.path, { fps: 10, width: 480 });
toastLog(gif.path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }], null);

let recording = video.record(null, { audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- 每个加工方法都有阻塞形态与返回 Promise 的 `xxxAsync` 形态; `options.onProgress(progress)` 报告 `percent`, `stage` 与 `etaMillis`, 停止脚本即取消其任务.
- 省略 `output` 时, 视频输出到 `Movies/3-Slate Studio/`, 音频输出到 `Music/3-Slate Studio/`, 文件名由源文件名, 操作与时间组成; 结果带实际路径与媒体信息, 并登记到媒体库.
- `video.compose(timeline)` 渲染以对象描述的多轨时间线 (轨道, 片段, 转场, 叠加, 字幕), 与时间线剪辑器导出的格式相同.
- `video.tasks()` / `audio.tasks()` 列出并可取消本脚本的进行中任务; 失败时抛出 `MediaStudioError`, 其 `code` 为稳定的错误码, 例如 `INVALID_OPTIONS` 或 `STORAGE_PERMISSION_DENIED`.

******

### 常见问题

******

#### 不装 AutoJs6 能用吗?

可以. 工具界面, 时间线剪辑器与 Studio 录音都能单独使用, 只有在脚本中加工媒体时才需要 AutoJs6.

#### 为什么插件要申请所有文件访问权限?

脚本传入的是 `/sdcard/DCIM/...` 这样的普通文件路径, 插件与 3-Maple Image 一样直接读写这些路径. 未授权时加工会报告 `STORAGE_PERMISSION_DENIED`, 插件界面会说明如何授权.

#### 录屏需要安装插件吗?

不需要. `video.record` 与 `audio.record` 内置于 AutoJs6 (内部版本 6641 或更高); 插件提供加工方法与自己的 Studio 录音.

******

### 权限与安全

******

插件在设计上尽量收窄权限面:

- 受保护的入口: 能力服务与激活入口都受 AutoJs6 插件权限 (`org.autojs.permission.PLUGIN`) 保护, 其他应用无法直接调用.
- 存储: 所有文件访问权限只用于脚本与 Studio 界面指定的路径; 未指定路径的输出只写入 `Movies` 与 `Music` 下的插件目录.
- 录音: 只有 Studio 录音进行中才采集麦克风与设备内部声音, 并显示前台通知; 采集设备内部声音还需要系统的屏幕捕获确认.
- 网络: 1.0.0 只用于检查更新; 自 1.1.0 起, FFmpeg 大库在你请求时下载, 加载前校验 SHA-256.

******

### FFmpeg 大库

******

自 1.1.0 起, 插件可以按需下载按 LGPL-2.1-or-later 构建的 FFmpeg 大库. 大库不打包进 APK: 只在你请求时按设备 ABI 下载并动态加载, 随附自己的许可声明与对应源码的获取位置. 不下载大库时, 1.0.0 的全部功能照常可用.

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

_2026/11/05_

- `提示` 开发预览: 插件可以被 AutoJs6 插件中心识别, 加工功能, Studio 界面与 video / audio 脚本模块的加工方法尚未交付
- `提示` 需要 AutoJs6 6.8.1 (6641) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
- `新增` 插件骨架: Wake 激活协议, 插件中心身份服务与媒体加工能力服务 (契约版本 2)
- `新增` 首页提供所有文件访问权限的授权入口
- `新增` 所有 ABI 共用一个 APK: 唯一的原生库来自 Jetpack Compose, 已按 16 KB 内存页对齐
- `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
- `依赖` 附加 Jetpack Compose BOM 版本 2026.09.00 (插件界面)
- `依赖` 附加 Navigation Compose 版本 2.10.2
- `依赖` 附加 Jetpack Media3 版本 1.10.1 (Transformer, Effect, Muxer, ExoPlayer 与 UI, 用于加工与预览)
- `依赖` 附加 AndroidX Core KTX 版本 1.18.0

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
